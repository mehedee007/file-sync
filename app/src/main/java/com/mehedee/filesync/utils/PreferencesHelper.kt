package com.mehedee.filesync.utils

import android.content.Context
import android.content.SharedPreferences

object PreferencesHelper {

    private const val PREF_NAME = "FileSync_Preferences"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_SERVER_PORT = "server_port"
    private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
    private const val KEY_SYNC_INTERVAL = "sync_interval"

    private const val DEFAULT_SERVER_URL = "192.168.100.147"
    private const val DEFAULT_SERVER_PORT = "5000"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Server URL
    fun saveServerUrl(context: Context, url: String) {
        getPreferences(context).edit().putString(KEY_SERVER_URL, url).apply()
    }

    fun getServerUrl(context: Context): String {
        return getPreferences(context).getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    // Server Port
    fun saveServerPort(context: Context, port: String) {
        getPreferences(context).edit().putString(KEY_SERVER_PORT, port).apply()
    }

    fun getServerPort(context: Context): String {
        return getPreferences(context).getString(KEY_SERVER_PORT, DEFAULT_SERVER_PORT) ?: DEFAULT_SERVER_PORT
    }

    // Full Server URL
    fun getFullServerUrl(context: Context): String {
        val url = getServerUrl(context)
        val port = getServerPort(context)
        return "http://$url:$port/"
    }

    // Auto Sync
    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
    }

    fun isAutoSyncEnabled(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_AUTO_SYNC_ENABLED, false)
    }

    // Sync Interval (in minutes)
    fun setSyncInterval(context: Context, minutes: Int) {
        getPreferences(context).edit().putInt(KEY_SYNC_INTERVAL, minutes).apply()
    }

    fun getSyncInterval(context: Context): Int {
        return getPreferences(context).getInt(KEY_SYNC_INTERVAL, 30) // Default: 30 minutes
    }
}