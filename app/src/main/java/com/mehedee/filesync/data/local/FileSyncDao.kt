package com.mehedee.filesync.data.local

import androidx.room.*
import com.mehedee.filesync.data.local.entity.FileSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileSyncDao {

    // Insert a new file record
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileSyncEntity)

    // Insert multiple files
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileSyncEntity>)

    // Get all files
    @Query("SELECT * FROM file_sync ORDER BY lastModified DESC")
    fun getAllFiles(): Flow<List<FileSyncEntity>>

    // Get files by sync status
    @Query("SELECT * FROM file_sync WHERE syncStatus = :status")
    fun getFilesByStatus(status: String): Flow<List<FileSyncEntity>>

    // Get pending files (need to be synced)
    @Query("SELECT * FROM file_sync WHERE syncStatus = 'PENDING'")
    suspend fun getPendingFiles(): List<FileSyncEntity>

    // Update sync status
    @Query("UPDATE file_sync SET syncStatus = :status, lastSyncTime = :syncTime WHERE id = :fileId")
    suspend fun updateSyncStatus(fileId: Int, status: String, syncTime: Long)

    // Update with error
    @Query("UPDATE file_sync SET syncStatus = 'FAILED', errorMessage = :error WHERE id = :fileId")
    suspend fun updateWithError(fileId: Int, error: String)

    // Delete a file record
    @Delete
    suspend fun deleteFile(file: FileSyncEntity)

    // Clear all records
    @Query("DELETE FROM file_sync")
    suspend fun clearAll()

    // Update upload progress
    @Query("UPDATE file_sync SET uploadedBytes = :uploadedBytes, uploadProgress = :progress WHERE id = :fileId")
    suspend fun updateUploadProgress(fileId: Int, uploadedBytes: Long, progress: Int)

    // Pause upload
    @Query("UPDATE file_sync SET syncStatus = 'PAUSED' WHERE id = :fileId")
    suspend fun pauseUpload(fileId: Int)

    // Resume upload (set back to PENDING)
    @Query("UPDATE file_sync SET syncStatus = 'PENDING' WHERE id = :fileId")
    suspend fun resumeUpload(fileId: Int)

    // Get paused files
    @Query("SELECT * FROM file_sync WHERE syncStatus = 'PAUSED'")
    suspend fun getPausedFiles(): List<FileSyncEntity>

    // Increment retry count
    @Query("UPDATE file_sync SET retryCount = retryCount + 1 WHERE id = :fileId")
    suspend fun incrementRetryCount(fileId: Int)

    // Reset upload progress
    @Query("UPDATE file_sync SET uploadedBytes = 0, uploadProgress = 0 WHERE id = :fileId")
    suspend fun resetUploadProgress(fileId: Int)
}