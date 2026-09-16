package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.HydrationRecord
import com.example.data.model.PersonalBaseline
import com.example.data.model.PersonalCognitiveStateRecord
import com.example.data.model.UserProfile
import com.example.data.model.UserPreferences
import com.example.data.model.UserSession

@Database(
    entities = [
        UserProfile::class,
        PersonalBaseline::class,
        PersonalCognitiveStateRecord::class,
        HydrationRecord::class,
        UserSession::class,
        UserPreferences::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun personalBaselineDao(): PersonalBaselineDao
    abstract fun personalCognitiveStateDao(): PersonalCognitiveStateDao
    abstract fun hydrationDao(): HydrationDao
    abstract fun userSessionDao(): UserSessionDao
    abstract fun userPreferencesDao(): UserPreferencesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cognitive_assistant.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
