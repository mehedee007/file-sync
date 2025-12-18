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
}

sealed class SaveStatus {
    object Idle : SaveStatus()
    object Saving : SaveStatus()
    data class Success(val count: Int) : SaveStatus()
    data class Error(val message: String) : SaveStatus()
}