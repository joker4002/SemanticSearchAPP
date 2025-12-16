package com.semanticsearch.app.embedding

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.math.sqrt

/**
 * 嵌入向量生成引擎
 * 使用基于字符级别的哈希向量化方法，适合移动端轻量级场景
 * 支持中英文混合文本的语义表示
 */
class EmbeddingEngine(private val context: Context) {
    
    companion object {
        const val EMBEDDING_DIM = 256  // 向量维度
        private const val NGRAM_SIZE = 3  // N-gram大小
    }
    
    private var isInitialized = false
    private val stopWords = setOf(
        "的", "了", "是", "在", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
        "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
        "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
        "may", "might", "must", "shall", "can", "need", "dare", "ought", "used",
        "to", "of", "in", "for", "on", "with", "at", "by", "from", "as", "into",
        "through", "during", "before", "after", "above", "below", "between", "under"
    )
    
    suspend fun initialize() = withContext(Dispatchers.Default) {
        isInitialized = true
    }
    
    fun isReady(): Boolean = isInitialized
    
    /**
     * 生成文本的嵌入向量
     */
    suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        val cleanedText = preprocessText(text)
        val tokens = tokenize(cleanedText)
        val ngrams = generateNgrams(tokens, NGRAM_SIZE)
        
        // 初始化向量
        val embedding = FloatArray(EMBEDDING_DIM)
        
        // 使用哈希技巧生成向量
        for (ngram in ngrams) {
            val hash = hashString(ngram)
            val index = (hash and 0x7FFFFFFF) % EMBEDDING_DIM
            val sign = if ((hash shr 31) and 1 == 0) 1f else -1f
            embedding[index] += sign * getWeight(ngram)
        }
        
        // 添加字符级特征
        addCharacterFeatures(cleanedText, embedding)
        
        // L2归一化
        normalize(embedding)
        
        embedding
    }
    
    /**
     * 批量生成嵌入向量
     */
    suspend fun generateEmbeddings(texts: List<String>): List<FloatArray> = 
        withContext(Dispatchers.Default) {
            texts.map { generateEmbedding(it) }
        }
    
    private fun preprocessText(text: String): String {
        return text.lowercase()
            .replace(Regex("[\\p{Punct}\\s]+"), " ")
            .trim()
    }
    
    private fun tokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val currentToken = StringBuilder()
        
        for (char in text) {
            when {
                char.isWhitespace() -> {
                    if (currentToken.isNotEmpty()) {
                        tokens.add(currentToken.toString())
                        currentToken.clear()
                    }
                }
                isChinese(char) -> {
                    if (currentToken.isNotEmpty()) {
                        tokens.add(currentToken.toString())
                        currentToken.clear()
                    }
                    tokens.add(char.toString())
                }
                else -> {
                    currentToken.append(char)
                }
            }
        }
        
        if (currentToken.isNotEmpty()) {
            tokens.add(currentToken.toString())
        }
        
        return tokens.filter { it !in stopWords && it.isNotBlank() }
    }
    
    private fun isChinese(char: Char): Boolean {
        val ub = Character.UnicodeBlock.of(char)
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
               ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
               ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
               ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
               ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
               ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS ||
               ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
    }
    
    private fun generateNgrams(tokens: List<String>, n: Int): List<String> {
        val ngrams = mutableListOf<String>()
        
        // 单个token
        ngrams.addAll(tokens)
        
        // 2-gram 到 n-gram
        for (size in 2..n) {
            for (i in 0..tokens.size - size) {
                ngrams.add(tokens.subList(i, i + size).joinToString("_"))
            }
        }
        
        // 字符级n-gram (对于中文特别有效)
        for (token in tokens) {
            if (token.length >= 2) {
                for (i in 0..token.length - 2) {
                    ngrams.add("c_${token.substring(i, minOf(i + 2, token.length))}")
                }
            }
        }
        
        return ngrams
    }
    
    private fun hashString(str: String): Int {
        val bytes = str.toByteArray(StandardCharsets.UTF_8)
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return ((digest[0].toInt() and 0xFF) shl 24) or
               ((digest[1].toInt() and 0xFF) shl 16) or
               ((digest[2].toInt() and 0xFF) shl 8) or
               (digest[3].toInt() and 0xFF)
    }
    
    private fun getWeight(ngram: String): Float {
        // 基于n-gram类型的权重
        return when {
            ngram.startsWith("c_") -> 0.5f  // 字符级特征权重较低
            ngram.contains("_") -> 1.5f  // 多词n-gram权重较高
            else -> 1.0f
        }
    }
    
    private fun addCharacterFeatures(text: String, embedding: FloatArray) {
        // 添加一些简单的统计特征
        val length = text.length.toFloat()
        val chineseRatio = text.count { isChinese(it) }.toFloat() / maxOf(length, 1f)
        val digitRatio = text.count { it.isDigit() }.toFloat() / maxOf(length, 1f)
        
        // 将这些特征添加到向量的特定位置
        if (EMBEDDING_DIM > 3) {
            embedding[EMBEDDING_DIM - 3] = (length / 1000f).coerceIn(0f, 1f)
            embedding[EMBEDDING_DIM - 2] = chineseRatio
            embedding[EMBEDDING_DIM - 1] = digitRatio
        }
    }
    
    private fun normalize(vector: FloatArray) {
        var norm = 0f
        for (v in vector) {
            norm += v * v
        }
        norm = sqrt(norm)
        
        if (norm > 0) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
    }
    
    fun close() {
        isInitialized = false
    }
}
