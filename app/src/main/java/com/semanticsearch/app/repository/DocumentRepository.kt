package com.semanticsearch.app.repository

import com.semanticsearch.app.data.Document
import com.semanticsearch.app.data.DocumentDao
import com.semanticsearch.app.data.SearchResult
import com.semanticsearch.app.embedding.EmbeddingEngine
import com.semanticsearch.app.search.VectorSearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DocumentRepository(
    private val documentDao: DocumentDao,
    private val embeddingEngine: EmbeddingEngine,
    private val vectorSearchEngine: VectorSearchEngine
) {
    
    val allDocuments: Flow<List<Document>> = documentDao.getAllDocuments()
    
    /**
     * 加载所有向量到搜索引擎
     */
    suspend fun loadVectorsIntoSearchEngine() = withContext(Dispatchers.IO) {
        val documents = documentDao.getAllDocumentsList()
        vectorSearchEngine.addVectors(documents)
    }
    
    /**
     * 添加新文档
     */
    suspend fun addDocument(title: String, content: String): Long = withContext(Dispatchers.IO) {
        // 生成嵌入向量
        val combinedText = "$title $content"
        val embedding = embeddingEngine.generateEmbedding(combinedText)
        
        val document = Document(
            title = title,
            content = content,
            embedding = embedding
        )
        
        val id = documentDao.insertDocument(document)
        
        // 添加到搜索引擎
        val savedDocument = document.copy(id = id)
        vectorSearchEngine.addVector(id, embedding, savedDocument)
        
        id
    }
    
    /**
     * 更新文档
     */
    suspend fun updateDocument(id: Long, title: String, content: String) = withContext(Dispatchers.IO) {
        val existingDoc = documentDao.getDocumentById(id) ?: return@withContext
        
        // 重新生成嵌入向量
        val combinedText = "$title $content"
        val embedding = embeddingEngine.generateEmbedding(combinedText)
        
        val updatedDocument = existingDoc.copy(
            title = title,
            content = content,
            embedding = embedding,
            updatedAt = System.currentTimeMillis()
        )
        
        documentDao.updateDocument(updatedDocument)
        
        // 更新搜索引擎
        vectorSearchEngine.updateVector(id, embedding, updatedDocument)
    }
    
    /**
     * 删除文档
     */
    suspend fun deleteDocument(document: Document) = withContext(Dispatchers.IO) {
        documentDao.deleteDocument(document)
        vectorSearchEngine.removeVector(document.id)
    }
    
    /**
     * 删除文档通过ID
     */
    suspend fun deleteDocumentById(id: Long) = withContext(Dispatchers.IO) {
        documentDao.deleteDocumentById(id)
        vectorSearchEngine.removeVector(id)
    }
    
    /**
     * 语义搜索
     */
    suspend fun semanticSearch(
        query: String,
        topK: Int = 10,
        minSimilarity: Float = 0.1f
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext emptyList()
        }
        
        // 生成查询向量
        val queryVector = embeddingEngine.generateEmbedding(query)
        
        // 使用缓存搜索
        vectorSearchEngine.searchWithCache(queryVector, topK, minSimilarity)
    }
    
    /**
     * 获取文档数量
     */
    suspend fun getDocumentCount(): Int = withContext(Dispatchers.IO) {
        documentDao.getDocumentCount()
    }
    
    /**
     * 清空所有文档
     */
    suspend fun deleteAllDocuments() = withContext(Dispatchers.IO) {
        documentDao.deleteAllDocuments()
        vectorSearchEngine.clear()
    }
    
    /**
     * 获取单个文档
     */
    suspend fun getDocumentById(id: Long): Document? = withContext(Dispatchers.IO) {
        documentDao.getDocumentById(id)
    }
}
