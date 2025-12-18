package com.mehedee.filesync.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mehedee.filesync.utils.FileInfo
import com.mehedee.filesync.utils.FilePickerHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSelectionScreen(
    onBackClick: () -> Unit,
    viewModel: FileSelectionViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedFiles by remember { mutableStateOf<List<FileInfo>>(emptyList()) }
    val saveStatus by viewModel.saveStatus.collectAsState()

    // Show snackbar for save status
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveStatus) {
        when (saveStatus) {
            is SaveStatus.Success -> {
                val count = (saveStatus as SaveStatus.Success).count
                snackbarHostState.showSnackbar("$count files added to sync queue!")
                selectedFiles = emptyList() // Clear selection
                viewModel.resetSaveStatus()
            }
            is SaveStatus.Error -> {
                val message = (saveStatus as SaveStatus.Error).message
                snackbarHostState.showSnackbar("Error: $message")
                viewModel.resetSaveStatus()
            }
            else -> {}
        }
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val newFiles = uris.mapNotNull { uri ->
            FilePickerHelper.getFileInfo(context, uri)
        }
        selectedFiles = selectedFiles + newFiles
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Files") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←", fontSize = 24.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch("*/*") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Files")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Selected files count
            Text(
                "Selected Files: ${selectedFiles.size}",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (selectedFiles.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No files selected",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap + to add files",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // File list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedFiles) { fileInfo ->
                        FileItemCard(
                            fileInfo = fileInfo,
                            onRemove = {
                                selectedFiles = selectedFiles.filter { it.uri != fileInfo.uri }
                            }
                        )
                    }
                }

                // Sync button
                Button(
                    onClick = {
                        viewModel.saveFilesToDatabase(selectedFiles)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(top = 16.dp),
                    enabled = selectedFiles.isNotEmpty() && saveStatus !is SaveStatus.Saving
                ) {
                    if (saveStatus is SaveStatus.Saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Add to Sync Queue", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FileItemCard(
    fileInfo: FileInfo,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    fileInfo.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    FilePickerHelper.formatFileSize(fileInfo.size),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}