package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HydrationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HydrationDao {
    @Query("SELECT * FROM hydration_records WHERE dateString = :date ORDER BY timestamp ASC")
    fun getRecordsForDate(date: String): Flow<List<HydrationRecord>>

    @Query("SELECT * FROM hydration_records WHERE dateString = :date ORDER BY timestamp ASC")
    suspend fun getRecordsForDateSync(date: String): List<HydrationRecord>

    @Query("SELECT * FROM hydration_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<HydrationRecord>>

    @Query("SELECT * FROM hydration_records ORDER BY timestamp DESC")
    suspend fun getAllRecordsSync(): List<HydrationRecord>

    @Query("SELECT * FROM hydration_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRecord(): HydrationRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HydrationRecord): Long

    @Query("DELETE FROM hydration_records")
    suspend fun clear()
}
