package com.mehedee.filesync.utils

import android.content.Context
import androidx.work.*
import com.mehedee.filesync.workers.FileSyncWorker
import java.util.concurrent.TimeUnit

object WorkManagerHelper {

    private const val SYNC_WORK_NAME = "file_sync_work"

    fun schedulePeriodicSync(context: Context) {
        val interval = PreferencesHelper.getSyncInterval(context).toLong()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<FileSyncWorker>(
            interval, TimeUnit.MINUTES,
            15, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncWorkRequest
        )
    }

    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
    }

    fun syncNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<FileSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(syncWorkRequest)
    }

    fun getSyncWorkInfo(context: Context) =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(SYNC_WORK_NAME)
}