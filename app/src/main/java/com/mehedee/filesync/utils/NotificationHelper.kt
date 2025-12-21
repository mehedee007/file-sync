package com.mehedee.filesync.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mehedee.filesync.MainActivity
import com.mehedee.filesync.R

object NotificationHelper {

    private const val CHANNEL_ID = "file_sync_channel"
    private const val CHANNEL_NAME = "File Sync"
    private const val NOTIFICATION_ID_SYNC = 1001
    private const val NOTIFICATION_ID_COMPLETE = 1002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "File synchronization progress"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSyncProgressNotification(
        context: Context,
        fileName: String,
        progress: Int,
        currentFile: Int,
        totalFiles: Int
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Syncing files ($currentFile/$totalFiles)")
            .setContentText("Uploading: $fileName")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SYNC, notification)
        } catch (e: SecurityException) {
            // Permission not granted, silently fail
        }
    }

    fun showSyncCompleteNotification(
        context: Context,
        successCount: Int,
        failCount: Int
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (failCount == 0) {
            "✓ Sync Complete"
        } else {
            "⚠ Sync Completed with Errors"
        }

        val message = if (failCount == 0) {
            "$successCount files synced successfully"
        } else {
            "$successCount succeeded, $failCount failed"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(
                if (failCount == 0) android.R.drawable.stat_sys_upload_done
                else android.R.drawable.stat_notify_error
            )
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_COMPLETE, notification)
        } catch (e: SecurityException) {
            // Permission not granted, silently fail
        }
    }

    fun cancelSyncNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SYNC)
        } catch (e: SecurityException) {
            // Permission not granted, silently fail
        }
    }

    fun showUploadErrorNotification(context: Context, fileName: String, error: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Upload Failed")
            .setContentText("$fileName: $error")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_COMPLETE + fileName.hashCode(),
                notification
            )
        } catch (e: SecurityException) {
            // Permission not granted, silently fail
        }
    }
}