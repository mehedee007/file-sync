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
}