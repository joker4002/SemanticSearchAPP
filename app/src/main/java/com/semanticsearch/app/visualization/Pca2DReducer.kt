package com.semanticsearch.app.visualization

import com.semanticsearch.app.data.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ejml.simple.SimpleMatrix
import kotlin.math.max
import kotlin.math.min

data class EmbeddingPoint(
    val id: Long,
    val x: Float,
    val y: Float,
    val title: String,
    val snippet: String
)

class Pca2DReducer {
    suspend fun reduce(documents: List<Document>): List<EmbeddingPoint> = withContext(Dispatchers.Default) {
        if (documents.isEmpty()) {
            return@withContext emptyList()
        }

        val dim = documents.first().embedding.size
        val n = documents.size

        val data = Array(n) { DoubleArray(dim) }
        for (i in 0 until n) {
            val emb = documents[i].embedding
            if (emb.size != dim) {
                return@withContext emptyList()
            }
            for (j in 0 until dim) {
                data[i][j] = emb[j].toDouble()
            }
        }

        val means = DoubleArray(dim)
        for (j in 0 until dim) {
            var sum = 0.0
            for (i in 0 until n) sum += data[i][j]
            means[j] = sum / max(1, n).toDouble()
        }

        val centered = SimpleMatrix(n, dim)
        for (i in 0 until n) {
            for (j in 0 until dim) {
                centered.set(i, j, data[i][j] - means[j])
            }
        }

        if (n == 1) {
            return@withContext listOf(
                EmbeddingPoint(
                    id = documents[0].id,
                    x = 0.5f,
                    y = 0.5f,
                    title = documents[0].title,
                    snippet = documents[0].content
                )
            )
        }

        val svd = centered.svd()
        val v = svd.v
        val k = min(2, v.numCols())
        val v2 = v.extractMatrix(0, v.numRows(), 0, k)
        val projected = centered.mult(v2)

        var minX = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY

        val raw = Array(n) { DoubleArray(2) }
        for (i in 0 until n) {
            val x = projected.get(i, 0)
            val y = if (k > 1) projected.get(i, 1) else 0.0
            raw[i][0] = x
            raw[i][1] = y
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
        }

        val dx = (maxX - minX).takeIf { it != 0.0 } ?: 1.0
        val dy = (maxY - minY).takeIf { it != 0.0 } ?: 1.0

        documents.mapIndexed { idx, doc ->
            val nx = ((raw[idx][0] - minX) / dx).toFloat()
            val ny = ((raw[idx][1] - minY) / dy).toFloat()
            EmbeddingPoint(
                id = doc.id,
                x = nx,
                y = ny,
                title = doc.title,
                snippet = doc.content
            )
        }
    }
}
