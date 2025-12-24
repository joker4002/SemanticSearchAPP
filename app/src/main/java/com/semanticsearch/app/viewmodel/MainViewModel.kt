package com.semanticsearch.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.semanticsearch.app.data.Document
import com.semanticsearch.app.data.SampleData
import com.semanticsearch.app.data.SearchResult
import com.semanticsearch.app.indexing.FileIndexingService
import com.semanticsearch.app.repository.DocumentRepository
import com.semanticsearch.app.visualization.EmbeddingPoint
import com.semanticsearch.app.visualization.Pca2DReducer
import com.semanticsearch.app.R
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: DocumentRepository,
    private val fileIndexingService: FileIndexingService
) : ViewModel() {

    private val pca2DReducer = Pca2DReducer()
    
    // UI状态
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    // 所有文档
    val allDocuments: StateFlow<List<Document>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _embeddingPoints = MutableStateFlow<List<EmbeddingPoint>>(emptyList())
    val embeddingPoints: StateFlow<List<EmbeddingPoint>> = _embeddingPoints.asStateFlow()
    
    // 搜索结果
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()
    
    // 搜索查询
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    init {
        // 监听搜索查询变化，实现防抖搜索
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank()) {
                        performSearch(query)
                    } else {
                        _searchResults.value = emptyList()
                    }
                }
        }

        viewModelScope.launch {
            allDocuments.collect { docs ->
                refreshEmbeddingVisualization(docs)
            }
        }
    }

    private suspend fun refreshEmbeddingVisualization(documents: List<Document>) {
        try {
            _embeddingPoints.value = pca2DReducer.reduce(documents)
        } catch (_: Throwable) {
            _embeddingPoints.value = emptyList()
        }
    }

    fun refreshEmbeddingVisualization() {
        viewModelScope.launch {
            refreshEmbeddingVisualization(allDocuments.value)
        }
    }
    
    /**
     * 更新搜索查询
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * 执行搜索
     */
    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true) }
        
        try {
            val results = repository.semanticSearch(query, topK = 20, minSimilarity = 0.05f)
            _searchResults.value = results
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        } finally {
            _uiState.update { it.copy(isSearching = false) }
        }
    }
    
    /**
     * 添加文档
     */
    fun addDocument(title: String, content: String) {
        if (title.isBlank() || content.isBlank()) {
            _uiState.update {
                it.copy(
                    error = null,
                    errorResId = R.string.error_title_content_empty,
                    errorArgs = emptyList()
                )
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val id = repository.addDocument(title, content)
                _uiState.update { it.copy(highlightedDocumentIds = setOf(id)) }
                _uiState.update {
                    it.copy(
                        showAddDialog = false,
                        message = null,
                        messageResId = R.string.doc_add_success,
                        messageArgs = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = null,
                        errorResId = R.string.doc_add_failed,
                        errorArgs = listOf(e.message ?: "")
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    /**
     * 更新文档
     */
    fun updateDocument(id: Long, title: String, content: String) {
        if (title.isBlank() || content.isBlank()) {
            _uiState.update {
                it.copy(
                    error = null,
                    errorResId = R.string.error_title_content_empty,
                    errorArgs = emptyList()
                )
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                repository.updateDocument(id, title, content)
                _uiState.update { it.copy(highlightedDocumentIds = setOf(id)) }
                _uiState.update {
                    it.copy(
                        editingDocument = null,
                        message = null,
                        messageResId = R.string.doc_update_success,
                        messageArgs = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = null,
                        errorResId = R.string.doc_update_failed,
                        errorArgs = listOf(e.message ?: "")
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    /**
     * 删除文档
     */
    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            try {
                repository.deleteDocument(document)
                _uiState.update { it.copy(highlightedDocumentIds = emptySet()) }
                _uiState.update {
                    it.copy(
                        message = null,
                        messageResId = R.string.doc_deleted,
                        messageArgs = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = null,
                        errorResId = R.string.doc_delete_failed,
                        errorArgs = listOf(e.message ?: "")
                    )
                }
            }
        }
    }
    
    /**
     * 显示添加对话框
     */
    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }
    
    /**
     * 隐藏添加对话框
     */
    fun hideAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }
    
    /**
     * 开始编辑文档
     */
    fun startEditDocument(document: Document) {
        _uiState.update { it.copy(editingDocument = document) }
    }
    
    /**
     * 取消编辑
     */
    fun cancelEdit() {
        _uiState.update { it.copy(editingDocument = null) }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null, errorResId = null, errorArgs = emptyList()) }
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null, messageResId = null, messageArgs = emptyList()) }
    }
    
    /**
     * 切换当前页面
     */
    fun setCurrentScreen(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }
    
    /**
     * 加载示例数据
     */
    fun loadSampleData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                SampleData.sampleDocuments.forEach { (title, content) ->
                    repository.addDocument(title, content)
                }
                _uiState.update {
                    it.copy(
                        message = null,
                        messageResId = R.string.sample_loaded,
                        messageArgs = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = null,
                        errorResId = R.string.sample_load_failed,
                        errorArgs = listOf(e.message ?: "")
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun syncKnowledgeBaseFolder(treeUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = fileIndexingService.syncFolder(treeUri)
                _uiState.update { it.copy(highlightedDocumentIds = result.upsertedDocumentIds.toSet()) }
                _uiState.update {
                    it.copy(
                        message = null,
                        messageResId = R.string.sync_complete,
                        messageArgs = listOf(result.addedFiles, result.updatedFiles, result.removedFiles)
                    )
                }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        error = null,
                        errorResId = R.string.sync_failed,
                        errorArgs = listOf(e.message ?: "")
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingDocument: Document? = null,
    val currentScreen: Screen = Screen.Search,
    val highlightedDocumentIds: Set<Long> = emptySet(),
    val error: String? = null,
    val message: String? = null,
    val messageResId: Int? = null,
    val messageArgs: List<Any> = emptyList(),
    val errorResId: Int? = null,
    val errorArgs: List<Any> = emptyList()
)

enum class Screen {
    Search,
    KnowledgeBase,
    Visualization
}

class MainViewModelFactory(
    private val repository: DocumentRepository,
    private val fileIndexingService: FileIndexingService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, fileIndexingService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
