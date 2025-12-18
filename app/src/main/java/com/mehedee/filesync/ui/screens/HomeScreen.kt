package com.mehedee.filesync.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.mehedee.filesync.data.remote.FileUploadService

import androidx.lifecycle.viewmodel.compose.viewModel
//import com.mehedee.filesync.ui.screens.SyncHistoryViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSelectFilesClick: () -> Unit = {},
    onViewHistoryClick: () -> Unit = {},
    viewModel: SyncHistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uploadService = remember { FileUploadService(context) }
    val files by viewModel.files.collectAsState()
    val pendingCount = files.count { it.syncStatus == "PENDING" }
    val syncedCount = files.count { it.syncStatus == "SYNCED" }
    val totalCount = files.size
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "FileSync",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Sync Status",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "Ready to sync",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))



                    // Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Files", totalCount.toString())
                        StatItem("Synced", syncedCount.toString())
                        StatItem("Pending", pendingCount.toString())
                    }
                }
            }

            // Action Buttons
            // Sync Now Button
            if (pendingCount > 0) {
                val fileViewModel: FileSelectionViewModel = viewModel()
                val saveStatus by fileViewModel.saveStatus.collectAsState()

                Button(
                    onClick = { fileViewModel.startSync() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    enabled = saveStatus !is SaveStatus.Saving
                ) {
                    if (saveStatus is SaveStatus.Saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing...", fontSize = 18.sp)
                    } else {
                        Text("Sync Now ($pendingCount files)", fontSize = 18.sp)
                    }
                }

                // Show result
                LaunchedEffect(saveStatus) {
                    when (saveStatus) {
                        is SaveStatus.Success -> {
                            val count = (saveStatus as SaveStatus.Success).count
                            Toast.makeText(context, "✓ $count files synced!", Toast.LENGTH_SHORT).show()
                            fileViewModel.resetSaveStatus()
                        }
                        is SaveStatus.Error -> {
                            val message = (saveStatus as SaveStatus.Error).message
                            Toast.makeText(context, "✗ $message", Toast.LENGTH_LONG).show()
                            fileViewModel.resetSaveStatus()
                        }
                        else -> {}
                    }
                }
            }
            Button(
                onClick  = onSelectFilesClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 12.dp)
            ) {
                Text("Select Files", fontSize = 18.sp)
            }

            OutlinedButton(
                onClick = onViewHistoryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 12.dp)
            ) {
                Text("View Sync History", fontSize = 18.sp)
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val result = uploadService.checkServerConnection()
                        result.onSuccess {
                            Toast.makeText(context, "✓ Server Connected!", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(context, "✗ Connection Failed: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Test Server Connection", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}