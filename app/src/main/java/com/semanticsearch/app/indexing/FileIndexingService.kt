package com.semanticsearch.app.indexing

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.semanticsearch.app.data.Document
import com.semanticsearch.app.embedding.EmbeddingEngine
import com.semanticsearch.app.repository.DocumentRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipInputStream
import java.security.MessageDigest

data class SyncResult(
    val addedFiles: Int,
    val updatedFiles: Int,
    val removedFiles: Int,
    val addedDocuments: Int,
    val removedDocuments: Int,
    val upsertedDocumentIds: List<Long>,
    val removedDocumentIds: List<Long>
)

class FileIndexingService(
    private val context: Context,
    private val repository: DocumentRepository,
    private val embeddingEngine: EmbeddingEngine,
    private val manifestStore: ManifestStore = ManifestStore(context)
) {

    companion object {
        private val pdfBoxInitialized = AtomicBoolean(false)
    }
    suspend fun syncFolder(
        rootDir: File,
        allowedExtensions: Set<String> = setOf("txt", "md", "pdf", "docx")
    ): SyncResult = withContext(Dispatchers.IO) {
        val manifest = manifestStore.load()

        val scannedFiles = rootDir
            .walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                val ext = file.extension.lowercase()
                ext.isNotBlank() && ext in allowedExtensions
            }
            .toList()

        val scannedPaths = scannedFiles.map { it.absolutePath }.toSet()
        val manifestPaths = manifest.entries.keys.toSet()

        var addedFiles = 0
        var updatedFiles = 0
        var removedFiles = 0
        var addedDocuments = 0
        var removedDocuments = 0
        val upsertedDocumentIds = mutableListOf<Long>()
        val removedDocumentIds = mutableListOf<Long>()

        val deletedPaths = manifestPaths - scannedPaths
        for (path in deletedPaths) {
            val entry = manifest.entries[path] ?: continue
            repository.deleteDocumentsByIds(entry.documentIds)
            removedDocuments += entry.documentIds.size
            removedDocumentIds.addAll(entry.documentIds)
            manifest.entries.remove(path)
            removedFiles += 1
        }

        for (file in scannedFiles) {
            val path = file.absolutePath
            val mtime = file.lastModified()
            val sha256 = sha256(file)

            val existing = manifest.entries[path]
            val isNew = existing == null
            val isChanged = existing != null && existing.sha256 != sha256

            if (!isNew && !isChanged) {
                continue
            }

            val ext = file.extension.lowercase()
            val extractedText = readDocumentText(file, ext)
            val chunks = chunkText(extractedText)
            if (chunks.isEmpty()) {
                continue
            }

            if (isChanged) {
                repository.deleteDocumentsByIds(existing!!.documentIds)
                removedDocuments += existing.documentIds.size
                removedDocumentIds.addAll(existing.documentIds)
            }

            val documents = mutableListOf<Document>()

            for ((index, chunk) in chunks.withIndex()) {
                val title = if (chunks.size == 1) file.name else "${file.name} #${index + 1}"
                val combinedText = "$title $chunk"
                val embedding = embeddingEngine.generateEmbedding(combinedText)
                documents.add(
                    Document(
                        title = title,
                        content = chunk,
                        embedding = embedding
                    )
                )
            }

            val ids = repository.addDocuments(documents)
            addedDocuments += ids.size
            upsertedDocumentIds.addAll(ids)

            manifest.entries[path] = ManifestEntry(
                path = path,
                sha256 = sha256,
                mtime = mtime,
                documentIds = ids
            )

            if (isNew) {
                addedFiles += 1
            } else {
                updatedFiles += 1
            }
        }

        manifestStore.save(manifest)

        SyncResult(
            addedFiles = addedFiles,
            updatedFiles = updatedFiles,
            removedFiles = removedFiles,
            addedDocuments = addedDocuments,
            removedDocuments = removedDocuments,
            upsertedDocumentIds = upsertedDocumentIds,
            removedDocumentIds = removedDocumentIds
        )
    }

    suspend fun syncFolder(
        treeUri: Uri,
        allowedExtensions: Set<String> = setOf("txt", "md", "pdf", "docx")
    ): SyncResult = withContext(Dispatchers.IO) {
        val manifest = manifestStore.load()

        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext SyncResult(0, 0, 0, 0, 0, emptyList(), emptyList())

        val prefix = treeUri.toString() + "|"
        val scannedFiles = mutableListOf<Pair<String, DocumentFile>>()
        collectDocumentFiles(root, "", treeUri.toString(), allowedExtensions, scannedFiles)

        val scannedKeys = scannedFiles.map { it.first }.toSet()
        val manifestKeysForRoot = manifest.entries.keys.filter { it.startsWith(prefix) }.toSet()

        var addedFiles = 0
        var updatedFiles = 0
        var removedFiles = 0
        var addedDocuments = 0
        var removedDocuments = 0
        val upsertedDocumentIds = mutableListOf<Long>()
        val removedDocumentIds = mutableListOf<Long>()

        val deletedKeys = manifestKeysForRoot - scannedKeys
        for (key in deletedKeys) {
            val entry = manifest.entries[key] ?: continue
            repository.deleteDocumentsByIds(entry.documentIds)
            removedDocuments += entry.documentIds.size
            removedDocumentIds.addAll(entry.documentIds)
            manifest.entries.remove(key)
            removedFiles += 1
        }

        for ((key, docFile) in scannedFiles) {
            val mtime = docFile.lastModified()
            val sha256 = sha256(docFile.uri)

            val existing = manifest.entries[key]
            val isNew = existing == null
            val isChanged = existing != null && existing.sha256 != sha256

            if (!isNew && !isChanged) {
                continue
            }
            val fileName = docFile.name ?: key
            val ext = fileName.substringAfterLast('.', "").lowercase()
            val extractedText = readDocumentText(docFile.uri, ext)
            val chunks = chunkText(extractedText)
            if (chunks.isEmpty()) {
                continue
            }

            if (isChanged) {
                repository.deleteDocumentsByIds(existing!!.documentIds)
                removedDocuments += existing.documentIds.size
                removedDocumentIds.addAll(existing.documentIds)
            }

            val documents = mutableListOf<Document>()

            for ((index, chunk) in chunks.withIndex()) {
                val title = if (chunks.size == 1) fileName else "${fileName} #${index + 1}"
                val combinedText = "$title $chunk"
                val embedding = embeddingEngine.generateEmbedding(combinedText)
                documents.add(
                    Document(
                        title = title,
                        content = chunk,
                        embedding = embedding
                    )
                )
            }

            val ids = repository.addDocuments(documents)
            addedDocuments += ids.size

            manifest.entries[key] = ManifestEntry(
                path = key,
                sha256 = sha256,
                mtime = mtime,
                documentIds = ids
            )

            if (isNew) {
                addedFiles += 1
            } else {
                updatedFiles += 1
            }
        }

        manifestStore.save(manifest)

        SyncResult(
            addedFiles = addedFiles,
            updatedFiles = updatedFiles,
            removedFiles = removedFiles,
            addedDocuments = addedDocuments,
            removedDocuments = removedDocuments,
            upsertedDocumentIds = upsertedDocumentIds,
            removedDocumentIds = removedDocumentIds
        )
    }

    private fun readFileText(file: File): String {
        return file.readText(Charsets.UTF_8)
    }

    private fun readDocumentText(file: File, extension: String): String {
        return when (extension) {
            "txt", "md" -> readFileText(file)
            "pdf" -> readPdfText(file)
            "docx" -> readDocxText(file)
            else -> ""
        }
    }

    private fun readUriText(uri: Uri): String {
        val resolver = context.contentResolver
        resolver.openInputStream(uri)?.use { input ->
            return input.readBytes().toString(Charsets.UTF_8)
        }
        return ""
    }

    private fun readDocumentText(uri: Uri, extension: String): String {
        return when (extension) {
            "txt", "md" -> readUriText(uri)
            "pdf" -> readPdfText(uri)
            "docx" -> readDocxText(uri)
            else -> ""
        }
    }

    private fun readPdfText(file: File): String {
        return try {
            ensurePdfBoxInitialized()
            file.inputStream().use { input ->
                PDDocument.load(input).use { document ->
                    val stripper = PDFTextStripper()
                    return stripper.getText(document)
                }
            }
        } catch (_: Throwable) {
            ""
        }
    }

    private fun readDocxText(file: File): String {
        return try {
            file.inputStream().use { input ->
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.name == "word/document.xml") {
                            val xml = zip.readBytes().toString(Charsets.UTF_8)
                            return extractTextFromDocxXml(xml)
                        }
                    }
                }
            }
            ""
        } catch (_: Throwable) {
            ""
        }
    }

    private fun readPdfText(uri: Uri): String {
        return try {
            ensurePdfBoxInitialized()
            val resolver = context.contentResolver
            resolver.openInputStream(uri)?.use { input ->
                PDDocument.load(input).use { document ->
                    val stripper = PDFTextStripper()
                    return stripper.getText(document)
                }
            }
            ""
        } catch (_: Throwable) {
            ""
        }
    }

    private fun readDocxText(uri: Uri): String {
        return try {
            val resolver = context.contentResolver
            resolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.name == "word/document.xml") {
                            val xml = zip.readBytes().toString(Charsets.UTF_8)
                            return extractTextFromDocxXml(xml)
                        }
                    }
                }
            }
            ""
        } catch (_: Throwable) {
            ""
        }
    }

    private fun ensurePdfBoxInitialized() {
        if (pdfBoxInitialized.get()) {
            return
        }

        synchronized(pdfBoxInitialized) {
            if (pdfBoxInitialized.get()) {
                return
            }
            PDFBoxResourceLoader.init(context.applicationContext)
            pdfBoxInitialized.set(true)
        }
    }

    private fun extractTextFromDocxXml(xml: String): String {
        if (xml.isBlank()) return ""

        val withNewlines = xml
            .replace(Regex("</w:p>"), "\n")
            .replace(Regex("</w:tr>"), "\n")

        val textRuns = Regex("<w:t[^>]*>(.*?)</w:t>", setOf(RegexOption.DOT_MATCHES_ALL))
            .findAll(withNewlines)
            .map { match ->
                match.groupValues.getOrNull(1).orEmpty()
            }
            .toList()

        return textRuns
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun chunkText(text: String, maxChars: Int = 2000): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return emptyList()
        }
        if (trimmed.length <= maxChars) {
            return listOf(trimmed)
        }

        val result = mutableListOf<String>()
        var start = 0
        while (start < trimmed.length) {
            val end = minOf(start + maxChars, trimmed.length)
            val chunk = trimmed.substring(start, end).trim()
            if (chunk.isNotEmpty()) {
                result.add(chunk)
            }
            start = end
        }
        return result
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { byte ->
            (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
    }

    private fun sha256(uri: Uri): String {
        val md = MessageDigest.getInstance("SHA-256")
        val resolver = context.contentResolver
        resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { byte ->
            (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
    }

    private fun collectDocumentFiles(
        dir: DocumentFile,
        relativeDir: String,
        treeUriString: String,
        allowedExtensions: Set<String>,
        out: MutableList<Pair<String, DocumentFile>>
    ) {
        val children = dir.listFiles()
        for (child in children) {
            val name = child.name ?: continue
            if (child.isDirectory) {
                collectDocumentFiles(
                    child,
                    relativeDir = if (relativeDir.isEmpty()) "$name/" else "$relativeDir$name/",
                    treeUriString = treeUriString,
                    allowedExtensions = allowedExtensions,
                    out = out
                )
                continue
            }

            if (!child.isFile) {
                continue
            }

            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext.isBlank() || ext !in allowedExtensions) {
                continue
            }

            val key = "$treeUriString|$relativeDir$name"
            out.add(key to child)
        }
    }
}
