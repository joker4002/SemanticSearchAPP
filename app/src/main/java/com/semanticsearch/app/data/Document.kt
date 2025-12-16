package com.semanticsearch.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "documents")
@TypeConverters(VectorConverter::class)
data class Document(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val embedding: FloatArray,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Document
        if (id != other.id) return false
        if (title != other.title) return false
        if (content != other.content) return false
        if (!embedding.contentEquals(other.embedding)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}

class VectorConverter {
    private val gson = Gson()
    
    @TypeConverter
    fun fromFloatArray(value: FloatArray): String {
        return gson.toJson(value.toList())
    }
    
    @TypeConverter
    fun toFloatArray(value: String): FloatArray {
        val listType = object : TypeToken<List<Float>>() {}.type
        val list: List<Float> = gson.fromJson(value, listType)
        return list.toFloatArray()
    }
}

data class SearchResult(
    val document: Document,
    val similarity: Float
)
