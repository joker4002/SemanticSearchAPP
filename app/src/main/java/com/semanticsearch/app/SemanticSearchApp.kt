package com.semanticsearch.app

import android.app.Application
import com.semanticsearch.app.data.AppDatabase
import com.semanticsearch.app.embedding.EmbeddingEngine
import com.semanticsearch.app.indexing.FileIndexingService
import com.semanticsearch.app.repository.DocumentRepository
import com.semanticsearch.app.search.VectorSearchEngine
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SemanticSearchApp : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val embeddingEngine: EmbeddingEngine by lazy { EmbeddingEngine(this) }
    val vectorSearchEngine: VectorSearchEngine by lazy { VectorSearchEngine() }
    val repository: DocumentRepository by lazy { 
        DocumentRepository(database.documentDao(), embeddingEngine, vectorSearchEngine) 
    }

    val fileIndexingService: FileIndexingService by lazy {
        FileIndexingService(this, repository, embeddingEngine)
    }
    
    override fun onCreate() {
        super.onCreate()

        PDFBoxResourceLoader.init(applicationContext)

        applicationScope.launch {
            embeddingEngine.initialize()
            repository.loadVectorsIntoSearchEngine()
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        embeddingEngine.close()
    }
}
