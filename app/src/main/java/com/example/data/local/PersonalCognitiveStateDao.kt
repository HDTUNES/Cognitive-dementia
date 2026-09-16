package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PersonalCognitiveStateRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalCognitiveStateDao {
    @Query("SELECT * FROM personal_cognitive_state WHERE id = 1 LIMIT 1")
    fun getStateRecord(): Flow<PersonalCognitiveStateRecord?>

    @Query("SELECT * FROM personal_cognitive_state WHERE id = 1 LIMIT 1")
    suspend fun getStateRecordSync(): PersonalCognitiveStateRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: PersonalCognitiveStateRecord)

    @Query("DELETE FROM personal_cognitive_state")
    suspend fun clear()
}
