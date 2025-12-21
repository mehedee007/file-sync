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
import androidx.compose.runtime.livedata.observeAsState
import androidx.work.WorkInfo
import com.mehedee.filesync.utils.WorkManagerHelper
import com.mehedee.filesync.utils.PreferencesHelper

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.mehedee.filesync.utils.NotificationHelper
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSelectFilesClick: () -> Unit = {},
    onViewHistoryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: SyncHistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uploadService = remember { FileUploadService(context) }
    val files by viewModel.files.collectAsState()
    val pendingCount = files.count { it.syncStatus == "PENDING" }
    val syncedCount = files.count { it.syncStatus == "SYNCED" }
    val totalCount = files.size
    val autoSyncEnabled = remember { PreferencesHelper.isAutoSyncEnabled(context) }
    val workInfo = WorkManagerHelper.getSyncWorkInfo(context).observeAsState()
    val isBackgroundSyncing = workInfo.value?.any { it.state == WorkInfo.State.RUNNING } == true
    // Request notification permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notifications enabled", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
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
                    // Background Sync Status Card
                    if (autoSyncEnabled) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBackgroundSyncing)
                                    MaterialTheme.colorScheme.tertiaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isBackgroundSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Background sync running...", fontSize = 14.sp)
                                    } else {
                                        Text("🔄 Auto sync enabled", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons
            // Sync Now Button
            if (pendingCount > 0) {
                val fileViewModel: FileSelectionViewModel = viewModel()
                val saveStatus by fileViewModel.saveStatus.collectAsState()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Immediate Sync
                    Button(
                        onClick = { fileViewModel.startSync() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        enabled = saveStatus !is SaveStatus.Saving && !isBackgroundSyncing
                    ) {
                        if (saveStatus is SaveStatus.Saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        } else {
                            Text("Sync Now", fontSize = 16.sp)
                        }
                    }

                    // Background Sync
                    OutlinedButton(
                        onClick = {
                            WorkManagerHelper.syncNow(context)
                            Toast.makeText(context, "Background sync started", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        enabled = !isBackgroundSyncing
                    ) {
                        Text("Sync in BG", fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
            OutlinedButton(
                onClick = onSettingsClick,  // Change this
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Settings", fontSize = 18.sp)
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