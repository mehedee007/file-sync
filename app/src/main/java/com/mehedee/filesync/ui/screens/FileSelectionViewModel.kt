package com.mehedee.filesync.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mehedee.filesync.data.FileSyncRepository
import com.mehedee.filesync.data.local.entity.FileSyncEntity
import com.mehedee.filesync.utils.FileInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FileSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileSyncRepository(application)

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus

    private val _allFiles = MutableStateFlow<List<FileSyncEntity>>(emptyList())
    val allFiles: StateFlow<List<FileSyncEntity>> = _allFiles

    init {
        loadAllFiles()
    }

    private fun loadAllFiles() {
        viewModelScope.launch {
            repository.getAllFiles().collect { files ->
                _allFiles.value = files
            }
        }
    }

    fun saveFilesToDatabase(files: List<FileInfo>) {
        viewModelScope.launch {
            try {
                _saveStatus.value = SaveStatus.Saving
                repository.addFilesToSyncQueue(files)
                _saveStatus.value = SaveStatus.Success(files.size)
            } catch (e: Exception) {
                _saveStatus.value = SaveStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetSaveStatus() {
        _saveStatus.value = SaveStatus.Idle
    }

    fun startSync() {
        viewModelScope.launch {
            try {
                _saveStatus.value = SaveStatus.Saving

                val pendingFiles = repository.getPendingFiles()

                if (pendingFiles.isEmpty()) {
                    _saveStatus.value = SaveStatus.Error("No files to sync")
                    return@launch
                }

                // Upload files one by one
                var successCount = 0
                var failCount = 0

                for (file in pendingFiles) {
                    repository.updateSyncStatus(file.id, "SYNCING")

                    val result = repository.uploadFileToServer(getApplication(), file)

                    result.onSuccess { response ->
                        if (response.success) {
                            repository.updateSyncStatus(file.id, "SYNCED")
                            successCount++
                        } else {
                            repository.updateWithError(file.id, response.error ?: "Upload failed")
                            failCount++
                        }
                    }.onFailure { exception ->
                        repository.updateWithError(file.id, exception.message ?: "Upload failed")
                        failCount++
                    }
                }

                if (failCount == 0) {
                    _saveStatus.value = SaveStatus.Success(successCount)
                } else {
                    _saveStatus.value = SaveStatus.Error("$successCount succeeded, $failCount failed")
                }

            } catch (e: Exception) {
                _saveStatus.value = SaveStatus.Error(e.message ?: "Sync failed")
            }
        }
    }
}

sealed class SaveStatus {
    object Idle : SaveStatus()
    object Saving : SaveStatus()
    data class Success(val count: Int) : SaveStatus()
    data class Error(val message: String) : SaveStatus()
}