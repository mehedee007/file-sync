package com.mehedee.filesync.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mehedee.filesync.data.remote.FileUploadService
import com.mehedee.filesync.utils.PreferencesHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.mehedee.filesync.utils.WorkManagerHelper

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _serverUrl = MutableStateFlow(PreferencesHelper.getServerUrl(context))
    val serverUrl: StateFlow<String> = _serverUrl

    private val _serverPort = MutableStateFlow(PreferencesHelper.getServerPort(context))
    val serverPort: StateFlow<String> = _serverPort

    private val _autoSyncEnabled = MutableStateFlow(PreferencesHelper.isAutoSyncEnabled(context))
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled

    private val _syncInterval = MutableStateFlow(PreferencesHelper.getSyncInterval(context))
    val syncInterval: StateFlow<Int> = _syncInterval

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    fun updateServerUrl(url: String) {
        _serverUrl.value = url
    }

    fun updateServerPort(port: String) {
        _serverPort.value = port
    }

    fun saveSettings() {
        PreferencesHelper.saveServerUrl(context, _serverUrl.value)
        PreferencesHelper.saveServerPort(context, _serverPort.value)
        PreferencesHelper.setAutoSyncEnabled(context, _autoSyncEnabled.value)
        PreferencesHelper.setSyncInterval(context, _syncInterval.value)
    }


    fun toggleAutoSync() {
        _autoSyncEnabled.value = !_autoSyncEnabled.value
        PreferencesHelper.setAutoSyncEnabled(context, _autoSyncEnabled.value)

        // Schedule or cancel background sync
        if (_autoSyncEnabled.value) {
            WorkManagerHelper.schedulePeriodicSync(context)
        } else {
            WorkManagerHelper.cancelPeriodicSync(context)
        }
    }

    fun updateSyncInterval(minutes: Int) {
        _syncInterval.value = minutes
        PreferencesHelper.setSyncInterval(context, minutes)

        // Reschedule if auto sync is enabled
        if (_autoSyncEnabled.value) {
            WorkManagerHelper.schedulePeriodicSync(context)
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.Testing

            // Save settings before testing
            saveSettings()

            val uploadService = FileUploadService(context)
            val result = uploadService.checkServerConnection()

            result.onSuccess {
                _connectionStatus.value = ConnectionStatus.Success(it.message ?: "Connected")
            }.onFailure {
                _connectionStatus.value = ConnectionStatus.Error(it.message ?: "Connection failed")
            }
        }
    }

    fun resetConnectionStatus() {
        _connectionStatus.value = ConnectionStatus.Idle
    }

    fun getFullServerUrl(): String {
        return "http://${_serverUrl.value}:${_serverPort.value}/"
    }


}

sealed class ConnectionStatus {
    object Idle : ConnectionStatus()
    object Testing : ConnectionStatus()
    data class Success(val message: String) : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}