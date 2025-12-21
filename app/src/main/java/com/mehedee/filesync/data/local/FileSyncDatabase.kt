package com.mehedee.filesync.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mehedee.filesync.data.local.entity.FileSyncEntity

@Database(
    entities = [FileSyncEntity::class],
    version = 2,
    exportSchema = false
)
abstract class FileSyncDatabase : RoomDatabase() {

    abstract fun fileSyncDao(): FileSyncDao

    companion object {
        @Volatile
        private var INSTANCE: FileSyncDatabase? = null

        fun getDatabase(context: Context): FileSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FileSyncDatabase::class.java,
                    "file_sync_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}