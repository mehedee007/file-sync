package com.mehedee.filesync.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mehedee.filesync.data.FileSyncRepository

class FileSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = FileSyncRepository(context)

    override suspend fun doWork(): Result {
        return try {
            // Get all pending files
            val pendingFiles = repository.getPendingFiles()

            if (pendingFiles.isEmpty()) {
                return Result.success()
            }

            var successCount = 0
            var failCount = 0

            // Upload each file
            for (file in pendingFiles) {
                // Update status to SYNCING
                repository.updateSyncStatus(file.id, "SYNCING")

                // Upload file
                val uploadResult = repository.uploadFileToServer(applicationContext, file)

                uploadResult.onSuccess { response ->
                    if (response.success) {
                        repository.updateSyncStatus(file.id, "SYNCED")
                        successCount++
                    } else {
                        repository.updateWithError(file.id, response.error ?: "Unknown error")
                        failCount++
                    }
                }.onFailure { exception ->
                    repository.updateWithError(file.id, exception.message ?: "Upload failed")
                    failCount++
                }
            }

            // Return result
            if (failCount == 0) {
                Result.success()
            } else if (successCount > 0) {
                Result.success() // Partial success
            } else {
                Result.retry() // All failed, retry later
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}