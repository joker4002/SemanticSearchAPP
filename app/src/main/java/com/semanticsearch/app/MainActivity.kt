package com.semanticsearch.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semanticsearch.app.ui.MainScreen
import com.semanticsearch.app.ui.theme.SemanticSearchTheme
import com.semanticsearch.app.viewmodel.MainViewModel
import com.semanticsearch.app.viewmodel.MainViewModelFactory

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as SemanticSearchApp
        
        setContent {
            SemanticSearchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModelFactory(app.repository, app.fileIndexingService)
                    )
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
