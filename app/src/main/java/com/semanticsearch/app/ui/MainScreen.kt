package com.semanticsearch.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.semanticsearch.app.R
import com.semanticsearch.app.ui.components.AddDocumentDialog
import com.semanticsearch.app.ui.components.EditDocumentDialog
import com.semanticsearch.app.ui.screens.EmbeddingVisualizationScreen
import com.semanticsearch.app.ui.screens.KnowledgeBaseScreen
import com.semanticsearch.app.ui.screens.SearchScreen
import com.semanticsearch.app.viewmodel.MainViewModel
import com.semanticsearch.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allDocuments by viewModel.allDocuments.collectAsState()
    val embeddingPoints by viewModel.embeddingPoints.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }

    var showLanguageDialog by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: SecurityException) {
            }
            viewModel.syncKnowledgeBaseFolder(uri)
        }
    }
    
    // 显示错误或消息
    LaunchedEffect(
        uiState.error,
        uiState.errorResId,
        uiState.errorArgs,
        uiState.message,
        uiState.messageResId,
        uiState.messageArgs
    ) {
        uiState.errorResId?.let { resId ->
            val text = if (uiState.errorArgs.isEmpty()) {
                context.getString(resId)
            } else {
                context.getString(resId, *uiState.errorArgs.toTypedArray())
            }
            snackbarHostState.showSnackbar(text)
            viewModel.clearError()
        } ?: uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }

        uiState.messageResId?.let { resId ->
            val text = if (uiState.messageArgs.isEmpty()) {
                context.getString(resId)
            } else {
                context.getString(resId, *uiState.messageArgs.toTypedArray())
            }
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessage()
        } ?: uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showLanguageDialog = true }) {
                        Icon(Icons.Default.Translate, contentDescription = stringResource(id = R.string.language))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (uiState.currentScreen == Screen.Search) 
                                Icons.Filled.Search else Icons.Outlined.Search,
                            contentDescription = stringResource(id = R.string.search)
                        )
                    },
                    label = { Text(stringResource(id = R.string.search)) },
                    selected = uiState.currentScreen == Screen.Search,
                    onClick = { viewModel.setCurrentScreen(Screen.Search) }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (uiState.currentScreen == Screen.KnowledgeBase) 
                                Icons.Filled.LibraryBooks else Icons.Outlined.LibraryBooks,
                            contentDescription = stringResource(id = R.string.knowledge_base)
                        )
                    },
                    label = { Text(stringResource(id = R.string.knowledge_base)) },
                    selected = uiState.currentScreen == Screen.KnowledgeBase,
                    onClick = { viewModel.setCurrentScreen(Screen.KnowledgeBase) }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (uiState.currentScreen == Screen.Visualization)
                                Icons.Filled.AutoGraph else Icons.Outlined.AutoGraph,
                            contentDescription = stringResource(id = R.string.visualization)
                        )
                    },
                    label = { Text(stringResource(id = R.string.visualization)) },
                    selected = uiState.currentScreen == Screen.Visualization,
                    onClick = { viewModel.setCurrentScreen(Screen.Visualization) }
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
                        onLoadSampleData = viewModel::loadSampleData,
                        onSyncFolderClick = { folderPickerLauncher.launch(null) }
                    )
                }
                Screen.Visualization -> {
                    EmbeddingVisualizationScreen(
                        points = embeddingPoints,
                        highlightedIds = uiState.highlightedDocumentIds,
                        onRefresh = viewModel::refreshEmbeddingVisualization
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

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(id = R.string.select_language)) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh"))
                            showLanguageDialog = false
                        }
                    ) { Text(stringResource(id = R.string.language_zh)) }
                    TextButton(
                        onClick = {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                            showLanguageDialog = false
                        }
                    ) { Text(stringResource(id = R.string.language_en)) }
                    TextButton(
                        onClick = {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("fr"))
                            showLanguageDialog = false
                        }
                    ) { Text(stringResource(id = R.string.language_fr)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(id = R.string.close))
                }
            }
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
