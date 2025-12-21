package com.mehedee.filesync

import android.app.Application
import com.mehedee.filesync.utils.NotificationHelper
import com.mehedee.filesync.utils.PreferencesHelper
import com.mehedee.filesync.utils.WorkManagerHelper

class FileSyncApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Schedule background sync if enabled
        if (PreferencesHelper.isAutoSyncEnabled(this)) {
            WorkManagerHelper.schedulePeriodicSync(this)
        }
    }
}