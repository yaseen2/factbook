package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val sourceUrl: String,
    val sourceTitle: String,
    val remarks: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val errorMessage: String? = null
)
