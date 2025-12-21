package com.mehedee.filesync.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mehedee.filesync.data.FileSyncRepository
import com.mehedee.filesync.data.local.entity.FileSyncEntity
import com.mehedee.filesync.utils.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UploadManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileSyncRepository(application)

    private val _uploadingFiles = MutableStateFlow<Map<Int, UploadState>>(emptyMap())
    val uploadingFiles: StateFlow<Map<Int, UploadState>> = _uploadingFiles

    private val uploadJobs = mutableMapOf<Int, Job>()

    fun startUpload(file: FileSyncEntity) {
        val job = viewModelScope.launch {
            try {
                _uploadingFiles.value = _uploadingFiles.value + (file.id to UploadState.Uploading(0))

                repository.updateSyncStatus(file.id, "SYNCING")

                val result = repository.uploadFileWithProgress(file) { uploadedBytes, progress ->
                    viewModelScope.launch {
                        _uploadingFiles.value = _uploadingFiles.value +
                                (file.id to UploadState.Uploading(progress))

                        repository.updateUploadProgress(file.id, uploadedBytes, progress)
                    }
                }

                result.onSuccess { response ->
                    if (response.success) {
                        repository.updateSyncStatus(file.id, "SYNCED")
                        _uploadingFiles.value = _uploadingFiles.value - file.id
                    } else {
                        repository.updateWithError(file.id, response.error ?: "Upload failed")
                        _uploadingFiles.value = _uploadingFiles.value +
                                (file.id to UploadState.Error(response.error ?: "Failed"))
                    }
                }.onFailure { exception ->
                    if (exception.message == "Upload paused") {
                        repository.pauseUpload(file.id)
                        _uploadingFiles.value = _uploadingFiles.value +
                                (file.id to UploadState.Paused)
                    } else {
                        repository.updateWithError(file.id, exception.message ?: "Upload failed")
                        _uploadingFiles.value = _uploadingFiles.value +
                                (file.id to UploadState.Error(exception.message ?: "Failed"))
                    }
                }

            } catch (e: Exception) {
                repository.updateWithError(file.id, e.message ?: "Upload failed")
                _uploadingFiles.value = _uploadingFiles.value +
                        (file.id to UploadState.Error(e.message ?: "Failed"))
            } finally {
                uploadJobs.remove(file.id)
            }
        }

        uploadJobs[file.id] = job
    }

    fun pauseUpload(fileId: Int) {
        uploadJobs[fileId]?.cancel()
        uploadJobs.remove(fileId)
        _uploadingFiles.value = _uploadingFiles.value + (fileId to UploadState.Paused)

        viewModelScope.launch {
            repository.pauseUpload(fileId)
        }
    }

    fun resumeUpload(file: FileSyncEntity) {
        viewModelScope.launch {
            repository.resumeUpload(file.id)
            startUpload(file)
        }
    }

    fun cancelUpload(fileId: Int) {
        uploadJobs[fileId]?.cancel()
        uploadJobs.remove(fileId)
        _uploadingFiles.value = _uploadingFiles.value - fileId

        viewModelScope.launch {
            repository.updateSyncStatus(fileId, "PENDING")
            repository.resetUploadProgress(fileId)
        }
    }

    fun retryUpload(file: FileSyncEntity) {
        viewModelScope.launch {
            repository.incrementRetryCount(file.id)
            repository.resetUploadProgress(file.id)
            startUpload(file)
        }
    }
}

sealed class UploadState {
    data class Uploading(val progress: Int) : UploadState()
    object Paused : UploadState()
    data class Error(val message: String) : UploadState()
}