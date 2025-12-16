package com.semanticsearch.app.search

import com.semanticsearch.app.data.Document
import com.semanticsearch.app.data.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

/**
 * 向量搜索引擎
 * 实现高效的K-NN (K-最近邻) 搜索算法
 * 支持余弦相似度和欧氏距离计算
 */
class VectorSearchEngine {
    
    // 内存中的向量索引: documentId -> embedding
    private val vectorIndex = ConcurrentHashMap<Long, FloatArray>()
    
    // 文档缓存: documentId -> document
    private val documentCache = ConcurrentHashMap<Long, Document>()
    
    /**
     * 添加文档向量到索引
     */
    fun addVector(documentId: Long, embedding: FloatArray, document: Document? = null) {
        vectorIndex[documentId] = embedding
        document?.let { documentCache[documentId] = it }
    }
    
    /**
     * 批量添加向量
     */
    fun addVectors(documents: List<Document>) {
        for (doc in documents) {
            vectorIndex[doc.id] = doc.embedding
            documentCache[doc.id] = doc
        }
    }
    
    /**
     * 移除向量
     */
    fun removeVector(documentId: Long) {
        vectorIndex.remove(documentId)
        documentCache.remove(documentId)
    }
    
    /**
     * 更新向量
     */
    fun updateVector(documentId: Long, embedding: FloatArray, document: Document? = null) {
        vectorIndex[documentId] = embedding
        document?.let { documentCache[documentId] = it }
    }
    
    /**
     * 清空索引
     */
    fun clear() {
        vectorIndex.clear()
        documentCache.clear()
    }
    
    /**
     * K-NN搜索 - 返回最相似的K个文档ID及其相似度
     */
    suspend fun searchKNN(
        queryVector: FloatArray,
        k: Int = 10,
        minSimilarity: Float = 0.0f
    ): List<Pair<Long, Float>> = withContext(Dispatchers.Default) {
        
        if (vectorIndex.isEmpty()) {
            return@withContext emptyList()
        }
        
        // 计算所有向量的相似度
        val similarities = vectorIndex.map { (id, vector) ->
            id to cosineSimilarity(queryVector, vector)
        }
        
        // 过滤并排序
        similarities
            .filter { it.second >= minSimilarity }
            .sortedByDescending { it.second }
            .take(k)
    }
    
    /**
     * 语义搜索 - 返回SearchResult列表
     */
    suspend fun semanticSearch(
        queryVector: FloatArray,
        documents: List<Document>,
        k: Int = 10,
        minSimilarity: Float = 0.0f
    ): List<SearchResult> = withContext(Dispatchers.Default) {
        
        if (documents.isEmpty()) {
            return@withContext emptyList()
        }
        
        // 计算相似度
        val results = documents.map { doc ->
            val similarity = cosineSimilarity(queryVector, doc.embedding)
            SearchResult(doc, similarity)
        }
        
        // 过滤并排序
        results
            .filter { it.similarity >= minSimilarity }
            .sortedByDescending { it.similarity }
            .take(k)
    }
    
    /**
     * 使用缓存进行快速搜索
     */
    suspend fun searchWithCache(
        queryVector: FloatArray,
        k: Int = 10,
        minSimilarity: Float = 0.0f
    ): List<SearchResult> = withContext(Dispatchers.Default) {
        
        val knnResults = searchKNN(queryVector, k, minSimilarity)
        
        knnResults.mapNotNull { (id, similarity) ->
            documentCache[id]?.let { doc ->
                SearchResult(doc, similarity)
            }
        }
    }
    
    /**
     * 余弦相似度计算
     * cos(A, B) = (A · B) / (||A|| * ||B||)
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) dotProduct / denominator else 0f
    }
    
    /**
     * 欧氏距离计算
     */
    private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return Float.MAX_VALUE
        
        var sum = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }
    
    /**
     * 曼哈顿距离计算
     */
    private fun manhattanDistance(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return Float.MAX_VALUE
        
        var sum = 0f
        for (i in a.indices) {
            sum += kotlin.math.abs(a[i] - b[i])
        }
        return sum
    }
    
    /**
     * 获取索引大小
     */
    fun size(): Int = vectorIndex.size
    
    /**
     * 检查索引是否为空
     */
    fun isEmpty(): Boolean = vectorIndex.isEmpty()
    
    /**
     * 获取所有文档ID
     */
    fun getAllDocumentIds(): Set<Long> = vectorIndex.keys.toSet()
}
