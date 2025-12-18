package com.mehedee.filesync.data

import android.content.Context
import com.mehedee.filesync.data.local.FileSyncDatabase
import com.mehedee.filesync.data.local.entity.FileSyncEntity
import com.mehedee.filesync.utils.FileInfo
import com.mehedee.filesync.utils.FilePickerHelper
import kotlinx.coroutines.flow.Flow

class FileSyncRepository(context: Context) {

    private val database = FileSyncDatabase.getDatabase(context)
    private val dao = database.fileSyncDao()
    private val appContext = context.applicationContext

    // Get all files
    fun getAllFiles(): Flow<List<FileSyncEntity>> {
        return dao.getAllFiles()
    }

    // Get files by status
    fun getFilesByStatus(status: String): Flow<List<FileSyncEntity>> {
        return dao.getFilesByStatus(status)
    }

    // Get pending files
    suspend fun getPendingFiles(): List<FileSyncEntity> {
        return dao.getPendingFiles()
    }

    // Add files to sync queue
    suspend fun addFilesToSyncQueue(fileInfoList: List<FileInfo>) {
        val entities = fileInfoList.map { fileInfo ->
            val fileHash = FilePickerHelper.calculateFileHash(appContext, fileInfo.uri)

            FileSyncEntity(
                filePath = fileInfo.uri.toString(),
                fileName = fileInfo.name,
                fileSize = fileInfo.size,
                fileHash = fileHash,
                lastModified = System.currentTimeMillis(),
                syncStatus = "PENDING",
                lastSyncTime = null,
                errorMessage = null
            )
        }
        dao.insertFiles(entities)
    }

    // Update sync status
    suspend fun updateSyncStatus(fileId: Int, status: String) {
        dao.updateSyncStatus(fileId, status, System.currentTimeMillis())
    }

    // Update with error
    suspend fun updateWithError(fileId: Int, error: String) {
        dao.updateWithError(fileId, error)
    }

    // Delete file
    suspend fun deleteFile(file: FileSyncEntity) {
        dao.deleteFile(file)
    }

    // Clear all
    suspend fun clearAll() {
        dao.clearAll()
    }
}