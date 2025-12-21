package com.mehedee.filesync.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mehedee.filesync.data.local.entity.FileSyncEntity
import com.mehedee.filesync.utils.FilePickerHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncHistoryScreen(
    onBackClick: () -> Unit,
    viewModel: SyncHistoryViewModel = viewModel()
) {
    val files by viewModel.files.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync History") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←", fontSize = 24.sp)
                    }
                },
                actions = {
                    if (files.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear All",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterStatus == "ALL",
                    onClick = { viewModel.setFilter("ALL") },
                    label = { Text("All (${files.size})") }
                )
                FilterChip(
                    selected = filterStatus == "PENDING",
                    onClick = { viewModel.setFilter("PENDING") },
                    label = { Text("Pending (${files.count { it.syncStatus == "PENDING" }})") }
                )
                FilterChip(
                    selected = filterStatus == "SYNCED",
                    onClick = { viewModel.setFilter("SYNCED") },
                    label = { Text("Synced (${files.count { it.syncStatus == "SYNCED" }})") }
                )
                FilterChip(
                    selected = filterStatus == "FAILED",
                    onClick = { viewModel.setFilter("FAILED") },
                    label = { Text("Failed (${files.count { it.syncStatus == "FAILED" }})") }
                )
            }

            Divider()

            // File List
            if (files.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No files in sync queue",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Add files to get started",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val filteredFiles = viewModel.getFilteredFiles()

                if (filteredFiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No files with status: $filterStatus",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredFiles) { file ->
                            SyncFileCard(
                                file = file,
                                onDelete = { viewModel.deleteFile(file) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete All Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Clear All Files?") },
            text = { Text("This will remove all files from the sync queue. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SyncFileCard(
    file: FileSyncEntity,
    onDelete: () -> Unit,
    uploadManagerViewModel: UploadManagerViewModel = viewModel()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val uploadingFiles by uploadManagerViewModel.uploadingFiles.collectAsState()
    val uploadState = uploadingFiles[file.id]

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        file.fileName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        FilePickerHelper.formatFileSize(file.fileSize),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Upload Progress
            when (uploadState) {
                is UploadState.Uploading -> {
                    LinearProgressIndicator(
                        progress = uploadState.progress / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Uploading: ${uploadState.progress}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { uploadManagerViewModel.pauseUpload(file.id) }) {
                            Text("Pause", fontSize = 12.sp)
                        }
                    }
                }
                is UploadState.Paused -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "⏸ Paused at ${file.uploadProgress}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Row {
                            TextButton(onClick = { uploadManagerViewModel.resumeUpload(file) }) {
                                Text("Resume", fontSize = 12.sp)
                            }
                            TextButton(onClick = { uploadManagerViewModel.cancelUpload(file.id) }) {
                                Text("Cancel", fontSize = 12.sp)
                            }
                        }
                    }
                }
                is UploadState.Error -> {
                    Text(
                        "❌ Error: ${uploadState.message}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = { uploadManagerViewModel.retryUpload(file) }) {
                        Text("Retry", fontSize = 12.sp)
                    }
                }
                null -> {
                    // Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusBadge(file.syncStatus)

                        if (file.uploadProgress > 0 && file.syncStatus == "PENDING") {
                            Text(
                                "(${file.uploadProgress}% completed)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            formatDate(file.lastModified),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Upload controls for PENDING/PAUSED files
                    if (file.syncStatus == "PENDING" || file.syncStatus == "PAUSED") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (file.syncStatus == "PAUSED") {
                                TextButton(onClick = { uploadManagerViewModel.resumeUpload(file) }) {
                                    Text("Resume Upload")
                                }
                            } else {
                                TextButton(onClick = { uploadManagerViewModel.startUpload(file) }) {
                                    Text("Start Upload")
                                }
                            }
                        }
                    }
                }
            }

            // Error message if failed
            if (file.syncStatus == "FAILED" && file.errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Error: ${file.errorMessage}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
                if (file.retryCount > 0) {
                    Text(
                        "Retry attempts: ${file.retryCount}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Last sync time if synced
            if (file.syncStatus == "SYNCED" && file.lastSyncTime != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Synced: ${formatDate(file.lastSyncTime)}",
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete File?") },
            text = { Text("Remove '${file.fileName}' from sync queue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "PENDING" -> Pair(Color(0xFFFFA726), "Pending")
        "SYNCING" -> Pair(Color(0xFF42A5F5), "Syncing")
        "PAUSED" -> Pair(Color(0xFFAB47BC), "Paused")  // Add this
        "SYNCED" -> Pair(Color(0xFF4CAF50), "Synced")
        "FAILED" -> Pair(Color(0xFFEF5350), "Failed")
        else -> Pair(Color.Gray, status)
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}