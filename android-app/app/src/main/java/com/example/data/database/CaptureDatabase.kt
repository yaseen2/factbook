package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CaptureEntity::class], version = 1, exportSchema = false)
abstract class CaptureDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao

    companion object {
        @Volatile
        private var INSTANCE: CaptureDatabase? = null

        fun getDatabase(context: Context): CaptureDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CaptureDatabase::class.java,
                    "factbook_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
