package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserSession
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<UserSession>>

    @Query("SELECT * FROM user_sessions ORDER BY startedAt DESC")
    suspend fun getAllSessionsSync(): List<UserSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: UserSession): Long

    @Update
    suspend fun update(session: UserSession)

    @Query("DELETE FROM user_sessions")
    suspend fun clear()
}
