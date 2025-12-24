package com.semanticsearch.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<Document>>
    
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    suspend fun getAllDocumentsList(): List<Document>
    
    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): Document?
    
    @Query("SELECT * FROM documents WHERE id IN (:ids)")
    suspend fun getDocumentsByIds(ids: List<Long>): List<Document>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<Document>): List<Long>
    
    @Update
    suspend fun updateDocument(document: Document)
    
    @Delete
    suspend fun deleteDocument(document: Document)
    
    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("DELETE FROM documents WHERE id IN (:ids)")
    suspend fun deleteDocumentsByIds(ids: List<Long>)
    
    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getDocumentCount(): Int
    
    @Query("DELETE FROM documents")
    suspend fun deleteAllDocuments()
}
