package com.mehedee.filesync.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mehedee.filesync.data.FileSyncRepository
import com.mehedee.filesync.utils.NotificationHelper
import com.mehedee.filesync.utils.PreferencesHelper

class FileSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = FileSyncRepository(context)
    private val TAG = "FileSyncWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting auto sync...")

        // Check if auto sync is enabled
        if (!PreferencesHelper.isAutoSyncEnabled(applicationContext)) {
            Log.d(TAG, "Auto sync is disabled, skipping...")
            return Result.success()
        }

        return try {
            // Get all pending files
            val pendingFiles = repository.getPendingFiles()

            if (pendingFiles.isEmpty()) {
                Log.d(TAG, "No pending files to sync")
                return Result.success()
            }

            Log.d(TAG, "Found ${pendingFiles.size} files to sync")

            var successCount = 0
            var failCount = 0
            val totalFiles = pendingFiles.size

            // Upload each file
            for ((index, file) in pendingFiles.withIndex()) {
                try {
                    val currentFile = index + 1

                    // Show progress notification
                    NotificationHelper.showSyncProgressNotification(
                        applicationContext,
                        file.fileName,
                        (currentFile * 100) / totalFiles,
                        currentFile,
                        totalFiles
                    )

                    // Update status to SYNCING
                    repository.updateSyncStatus(file.id, "SYNCING")

                    // Upload file
                    val uploadResult = repository.uploadFileToServer(applicationContext, file)

                    uploadResult.onSuccess { response ->
                        if (response.success) {
                            repository.updateSyncStatus(file.id, "SYNCED")
                            successCount++
                            Log.d(TAG, "✓ Synced: ${file.fileName}")
                        } else {
                            repository.updateWithError(file.id, response.error ?: "Unknown error")
                            failCount++
                            Log.e(TAG, "✗ Failed: ${file.fileName} - ${response.error}")
                        }
                    }.onFailure { exception ->
                        repository.updateWithError(file.id, exception.message ?: "Upload failed")
                        failCount++
                        Log.e(TAG, "✗ Failed: ${file.fileName} - ${exception.message}")
                    }

                } catch (e: Exception) {
                    repository.updateWithError(file.id, e.message ?: "Upload failed")
                    failCount++
                    Log.e(TAG, "✗ Exception: ${file.fileName} - ${e.message}")
                }
            }

            // Cancel progress notification
            NotificationHelper.cancelSyncNotification(applicationContext)

            // Show completion notification
            NotificationHelper.showSyncCompleteNotification(
                applicationContext,
                successCount,
                failCount
            )

            Log.d(TAG, "Auto sync completed: $successCount succeeded, $failCount failed")

            // Return result
            if (failCount == 0) {
                Result.success()
            } else if (successCount > 0) {
                Result.success() // Partial success
            } else {
                Result.retry() // All failed, retry later
            }

        } catch (e: Exception) {
            Log.e(TAG, "Auto sync error: ${e.message}")
            NotificationHelper.cancelSyncNotification(applicationContext)
            e.printStackTrace()
            Result.failure()
        }
    }
}