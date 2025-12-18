package com.mehedee.filesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mehedee.filesync.ui.screens.FileSelectionScreen
import com.mehedee.filesync.ui.screens.HomeScreen
import com.mehedee.filesync.ui.screens.SyncHistoryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FileSyncApp()
                }
            }
        }
    }
}

@Composable
fun FileSyncApp() {
    var currentScreen by remember { mutableStateOf("home") }

    when (currentScreen) {
        "home" -> HomeScreen(
            onSelectFilesClick = { currentScreen = "file_selection" },
            onViewHistoryClick = { currentScreen = "sync_history" }
        )
        "file_selection" -> FileSelectionScreen(
            onBackClick = { currentScreen = "home" }
        )
        "sync_history" -> SyncHistoryScreen(  // Add this
            onBackClick = { currentScreen = "home" }
        )
    }
}