package com.semanticsearch.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.semanticsearch.app.ui.components.AddDocumentDialog
import com.semanticsearch.app.ui.components.EditDocumentDialog
import com.semanticsearch.app.ui.screens.KnowledgeBaseScreen
import com.semanticsearch.app.ui.screens.SearchScreen
import com.semanticsearch.app.viewmodel.MainViewModel
import com.semanticsearch.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allDocuments by viewModel.allDocuments.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 显示错误或消息
    LaunchedEffect(uiState.error, uiState.message) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (uiState.currentScreen == Screen.Search) 
                                Icons.Filled.Search else Icons.Outlined.Search,
                            contentDescription = "搜索"
                        )
                    },
                    label = { Text("搜索") },
                    selected = uiState.currentScreen == Screen.Search,
                    onClick = { viewModel.setCurrentScreen(Screen.Search) }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (uiState.currentScreen == Screen.KnowledgeBase) 
                                Icons.Filled.LibraryBooks else Icons.Outlined.LibraryBooks,
                            contentDescription = "知识库"
                        )
                    },
                    label = { Text("知识库") },
                    selected = uiState.currentScreen == Screen.KnowledgeBase,
                    onClick = { viewModel.setCurrentScreen(Screen.KnowledgeBase) }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (uiState.currentScreen) {
                Screen.Search -> {
                    SearchScreen(
                        searchQuery = searchQuery,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        searchResults = searchResults,
                        isSearching = uiState.isSearching,
                        onDocumentClick = { viewModel.startEditDocument(it.document) }
                    )
                }
                Screen.KnowledgeBase -> {
                    KnowledgeBaseScreen(
                        documents = allDocuments,
                        onAddClick = viewModel::showAddDialog,
                        onEditClick = viewModel::startEditDocument,
                        onDeleteClick = viewModel::deleteDocument,
                        onLoadSampleData = viewModel::loadSampleData
                    )
                }
            }
        }
    }
    
    // 添加文档对话框
    if (uiState.showAddDialog) {
        AddDocumentDialog(
            onDismiss = viewModel::hideAddDialog,
            onConfirm = { title, content ->
                viewModel.addDocument(title, content)
            },
            isLoading = uiState.isLoading
        )
    }
    
    // 编辑文档对话框
    uiState.editingDocument?.let { document ->
        EditDocumentDialog(
            document = document,
            onDismiss = viewModel::cancelEdit,
            onConfirm = { title, content ->
                viewModel.updateDocument(document.id, title, content)
            },
            isLoading = uiState.isLoading
        )
    }
}
