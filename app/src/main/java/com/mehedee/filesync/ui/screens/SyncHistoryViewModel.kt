package com.mehedee.filesync.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mehedee.filesync.data.FileSyncRepository
import com.mehedee.filesync.data.local.entity.FileSyncEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SyncHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileSyncRepository(application)

    private val _files = MutableStateFlow<List<FileSyncEntity>>(emptyList())
    val files: StateFlow<List<FileSyncEntity>> = _files

    private val _filterStatus = MutableStateFlow("ALL")
    val filterStatus: StateFlow<String> = _filterStatus

    init {
        loadFiles()
    }

    private fun loadFiles() {
        viewModelScope.launch {
            repository.getAllFiles().collect { fileList ->
                _files.value = fileList
            }
        }
    }

    fun setFilter(status: String) {
        _filterStatus.value = status
        viewModelScope.launch {
            if (status == "ALL") {
                repository.getAllFiles().collect { fileList ->
                    _files.value = fileList
                }
            } else {
                repository.getFilesByStatus(status).collect { fileList ->
                    _files.value = fileList
                }
            }
        }
    }

    fun deleteFile(file: FileSyncEntity) {
        viewModelScope.launch {
            repository.deleteFile(file)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun getFilteredFiles(): List<FileSyncEntity> {
        return if (_filterStatus.value == "ALL") {
            _files.value
        } else {
            _files.value.filter { it.syncStatus == _filterStatus.value }
        }
    }
}