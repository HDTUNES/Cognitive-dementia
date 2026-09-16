package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PersonalBaseline
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalBaselineDao {
    @Query("SELECT * FROM personal_baseline WHERE id = 1 LIMIT 1")
    fun getBaseline(): Flow<PersonalBaseline?>

    @Query("SELECT * FROM personal_baseline WHERE id = 1 LIMIT 1")
    suspend fun getBaselineSync(): PersonalBaseline?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(baseline: PersonalBaseline)

    @Query("DELETE FROM personal_baseline")
    suspend fun clear()
}
