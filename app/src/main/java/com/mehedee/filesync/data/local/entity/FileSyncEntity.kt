package com.mehedee.filesync.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "file_sync")
data class FileSyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val filePath: String,          // Local path of the file
    val fileName: String,           // Name of the file
    val fileSize: Long,             // Size in bytes
    val fileHash: String,           // SHA-256 hash for integrity
    val lastModified: Long,         // Timestamp when file was modified
    val syncStatus: String,         // "PENDING", "SYNCING", "PAUSED", "SYNCED", "FAILED"
    val lastSyncTime: Long? = null, // When it was last synced
    val errorMessage: String? = null, // Error if sync failed
    val uploadedBytes: Long = 0,    // Bytes uploaded so far
    val uploadProgress: Int = 0,    // Progress percentage (0-100)
    val retryCount: Int = 0         // Number of retry attempts
)