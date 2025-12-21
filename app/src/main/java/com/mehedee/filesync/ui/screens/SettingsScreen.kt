package com.mehedee.filesync.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsState()
    val syncInterval by viewModel.syncInterval.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show connection status messages
    LaunchedEffect(connectionStatus) {
        when (connectionStatus) {
            is ConnectionStatus.Success -> {
                val message = (connectionStatus as ConnectionStatus.Success).message
                snackbarHostState.showSnackbar("✓ $message")
                viewModel.resetConnectionStatus()
            }
            is ConnectionStatus.Error -> {
                val message = (connectionStatus as ConnectionStatus.Error).message
                snackbarHostState.showSnackbar("✗ $message")
                viewModel.resetConnectionStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server Configuration Section
            Text(
                "Server Configuration",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { viewModel.updateServerUrl(it) },
                        label = { Text("Server IP Address") },
                        placeholder = { Text("192.168.1.100") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = serverPort,
                        onValueChange = { viewModel.updateServerPort(it) },
                        label = { Text("Server Port") },
                        placeholder = { Text("5000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        "Full URL: ${viewModel.getFullServerUrl()}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { viewModel.testConnection() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = connectionStatus !is ConnectionStatus.Testing
                    ) {
                        if (connectionStatus is ConnectionStatus.Testing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing...")
                        } else {
                            Text("Test Connection")
                        }
                    }
                }
            }

            // Auto Sync Section
            Text(
                "Auto Sync",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Enable Auto Sync",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Automatically sync files in background",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = { viewModel.toggleAutoSync() }
                        )
                    }

                    if (autoSyncEnabled) {
                        Divider()

                        Text(
                            "Sync Interval: $syncInterval minutes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Slider(
                            value = syncInterval.toFloat(),
                            onValueChange = { viewModel.updateSyncInterval(it.toInt()) },
                            valueRange = 15f..120f,
                            steps = 6,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("15 min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("30 min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("60 min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("120 min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    viewModel.saveSettings()
                    onBackClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Settings", fontSize = 18.sp)
            }

            // Info Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "💡 Tips",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Make sure your PC and phone are on the same Wi-Fi network\n" +
                                "• Server must be running on your PC\n" +
                                "• Use 'Test Connection' to verify settings",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}