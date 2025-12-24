package com.semanticsearch.app.indexing

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class ManifestEntry(
    val path: String,
    val sha256: String,
    val mtime: Long,
    val documentIds: List<Long>
)

data class IndexManifest(
    val version: Int = 2,
    val entries: MutableMap<String, ManifestEntry> = mutableMapOf()
)

class ManifestStore(
    private val context: Context,
    private val gson: Gson = Gson(),
    private val fileName: String = "index_manifest_v2.json"
) {
    private val manifestFile: File
        get() = File(context.filesDir, fileName)

    fun load(): IndexManifest {
        if (!manifestFile.exists()) {
            return IndexManifest()
        }

        val json = manifestFile.readText(Charsets.UTF_8)
        if (json.isBlank()) {
            return IndexManifest()
        }

        val type = object : TypeToken<IndexManifest>() {}.type
        return gson.fromJson<IndexManifest>(json, type) ?: IndexManifest()
    }

    fun save(manifest: IndexManifest) {
        val json = gson.toJson(manifest)
        manifestFile.writeText(json, Charsets.UTF_8)
    }
}
