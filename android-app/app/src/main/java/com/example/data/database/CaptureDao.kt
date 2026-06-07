package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {
    @Query("SELECT * FROM captures ORDER BY timestamp DESC")
    fun getAllCaptures(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedCaptures(): List<CaptureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapture(capture: CaptureEntity): Long

    @Update
    suspend fun updateCapture(capture: CaptureEntity)

    @Delete
    suspend fun deleteCapture(capture: CaptureEntity)

    @Query("DELETE FROM captures WHERE id = :id")
    suspend fun deleteCaptureById(id: Int)
}
