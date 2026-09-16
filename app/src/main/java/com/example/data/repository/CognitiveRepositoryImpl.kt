package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.HydrationRecord
import com.example.data.model.PersonalBaseline
import com.example.data.model.PersonalCognitiveStateRecord
import com.example.data.model.User
import com.example.data.model.UserPreferences
import com.example.data.model.UserProfile
import com.example.data.model.UserSession
import com.example.processor.PersonalCognitiveState
import com.example.processor.PersonalCognitiveStateProcessor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CognitiveRepositoryImpl(
    private val database: AppDatabase
) : CognitiveRepository {

    private val profileDao = database.userProfileDao()
    private val baselineDao = database.personalBaselineDao()
    private val stateDao = database.personalCognitiveStateDao()
    private val hydrationDao = database.hydrationDao()
    private val sessionDao = database.userSessionDao()
    private val preferencesDao = database.userPreferencesDao()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override val userProfile: Flow<UserProfile?> = profileDao.getUserProfile().distinctUntilChanged()

    override val userPreferences: Flow<UserPreferences> = preferencesDao.getPreferences()
        .map { it ?: UserPreferences() }
        .distinctUntilChanged()

    override val personalBaseline: Flow<PersonalBaseline?> = baselineDao.getBaseline().distinctUntilChanged()

    override val sessions: Flow<List<UserSession>> = sessionDao.getAllSessions()

    override val personalCognitiveState: Flow<PersonalCognitiveState> = combine(
        sessions,
        personalBaseline,
        hydrationDao.getAllRecords()
    ) { allSessions, baseline, hydrationList ->
        val (state, updatedBaseline) = PersonalCognitiveStateProcessor.processCognitiveState(
            allSessions = allSessions,
            existingBaseline = baseline,
            hydrationRecords = hydrationList
        )
        state
    }.distinctUntilChanged()

    private fun getTodayDateString(timestamp: Long = System.currentTimeMillis()): String {
        return dateFormat.format(Date(timestamp))
    }

    override fun getTodayHydrationRecords(): Flow<List<HydrationRecord>> {
        val today = getTodayDateString()
        return hydrationDao.getRecordsForDate(today)
    }

    override fun getAllHydrationRecords(): Flow<List<HydrationRecord>> {
        return hydrationDao.getAllRecords()
    }

    override suspend fun saveUserProfile(name: String, dateOfBirth: String) {
        val existing = profileDao.getUserProfileSync()
        val profile = UserProfile(
            id = 1,
            name = name.trim(),
            dateOfBirth = dateOfBirth.trim(),
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        profileDao.insertOrUpdate(profile)

        // Ensure baseline architecture record is initialized if not present
        // (NOTE: This remains internal data structure only and is never shown in UI)
        baselineDao.insertOrUpdate(
            PersonalBaseline(
                id = 1,
                established = false,
                createdAt = null
            )
        )
    }

    override suspend fun updatePreferences(preferences: UserPreferences) {
        preferencesDao.insertOrUpdate(preferences)
    }

    override suspend fun setDarkMode(isDark: Boolean) {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferences()
        preferencesDao.insertOrUpdate(current.copy(isDarkMode = isDark))
    }

    override suspend fun setReminderIntervalHours(intervalHours: Int) {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferences()
        preferencesDao.insertOrUpdate(current.copy(reminderIntervalHours = intervalHours.coerceIn(1, 6)))
    }

    override suspend fun recordHydrationEvent(timestamp: Long): Long {
        val dateString = getTodayDateString(timestamp)
        val record = HydrationRecord(
            timestamp = timestamp,
            dateString = dateString,
            type = HydrationRecord.TYPE_HYDRATION,
            status = HydrationRecord.STATUS_COMPLETED
        )
        return hydrationDao.insert(record)
    }

    override suspend fun recordMissedReminderEvent(timestamp: Long): Long {
        val dateString = getTodayDateString(timestamp)
        val record = HydrationRecord(
            timestamp = timestamp,
            dateString = dateString,
            type = HydrationRecord.TYPE_HYDRATION,
            status = HydrationRecord.STATUS_MISSED
        )
        return hydrationDao.insert(record)
    }

    /**
     * Inspects scheduled reminder slots for today. If an expected reminder time has elapsed
     * without any confirmed drinking event within the slot's active window, records it as 'missed'.
     * Guarantees that completed drinking and missed reminders remain distinguishable.
     */
    override suspend fun checkAndProcessMissedReminders(currentTimestamp: Long) {
        val prefs = preferencesDao.getPreferencesSync() ?: UserPreferences()
        val todayStr = getTodayDateString(currentTimestamp)
        val existingRecords = hydrationDao.getRecordsForDateSync(todayStr)

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimestamp
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val startHour = prefs.reminderStartHour
        val endHour = prefs.reminderEndHour
        val interval = prefs.reminderIntervalHours.coerceAtLeast(1)

        val slotToleranceMs = 45 * 60 * 1000L // 45 minutes window

        for (hour in startHour..endHour step interval) {
            val slotCal = Calendar.getInstance()
            slotCal.timeInMillis = currentTimestamp
            slotCal.set(Calendar.HOUR_OF_DAY, hour)
            slotCal.set(Calendar.MINUTE, 0)
            slotCal.set(Calendar.SECOND, 0)
            slotCal.set(Calendar.MILLISECOND, 0)
            val slotTimestamp = slotCal.timeInMillis

            // Only check slots that are strictly in the past (past grace period)
            if (currentTimestamp > slotTimestamp + (30 * 60 * 1000L)) {
                // Check if any record (completed or missed) already covers this slot
                val hasRecordInSlot = existingRecords.any { record ->
                    Math.abs(record.timestamp - slotTimestamp) < slotToleranceMs
                }

                if (!hasRecordInSlot) {
                    val missedRecord = HydrationRecord(
                        timestamp = slotTimestamp,
                        dateString = todayStr,
                        type = HydrationRecord.TYPE_HYDRATION,
                        status = HydrationRecord.STATUS_MISSED
                    )
                    hydrationDao.insert(missedRecord)
                }
            }
        }
    }

    override suspend fun recordSession(
        sessionType: String,
        startedAt: Long,
        completedAt: Long,
        cyclesCompleted: Int,
        isCompleted: Boolean,
        metadata: String
    ): Long {
        val session = UserSession(
            sessionType = sessionType,
            startedAt = startedAt,
            completedAt = completedAt,
            cyclesCompleted = cyclesCompleted,
            isCompleted = isCompleted,
            metadata = metadata
        )
        val sessionId = sessionDao.insert(session)

        // After every completed session:
        // 1. Raw trial data is already saved.
        // 2. Calculate current-session metrics & compare with baseline & recent.
        // 3. Update personal cognitive state in database.
        try {
            val allSessions = sessionDao.getAllSessionsSync()
            val existingBaseline = baselineDao.getBaselineSync()
            val hydrationRecords = hydrationDao.getAllRecordsSync()

            val (state, updatedBaseline) = PersonalCognitiveStateProcessor.processCognitiveState(
                allSessions = allSessions,
                existingBaseline = existingBaseline,
                hydrationRecords = hydrationRecords
            )

            // If baseline was newly established, persist it
            if (updatedBaseline.established && (existingBaseline == null || !existingBaseline.established)) {
                baselineDao.insertOrUpdate(updatedBaseline)
            }

            // Persist the updated state record
            stateDao.insertOrUpdate(
                PersonalCognitiveStateRecord(
                    id = 1,
                    updatedAt = System.currentTimeMillis(),
                    totalSessionsProcessed = state.totalCompletedSessions,
                    baselineEstablished = state.baselineEstablished,
                    stateJson = state.toGeminiContextJson()
                )
            )
        } catch (_: Exception) {
            // Processing should be robust and never fail the raw session save
        }

        return sessionId
    }

    override suspend fun recalculateCognitiveState(): PersonalCognitiveState {
        val allSessions = sessionDao.getAllSessionsSync()
        val existingBaseline = baselineDao.getBaselineSync()
        val hydrationRecords = hydrationDao.getAllRecordsSync()

        val (state, updatedBaseline) = PersonalCognitiveStateProcessor.processCognitiveState(
            allSessions = allSessions,
            existingBaseline = existingBaseline,
            hydrationRecords = hydrationRecords
        )

        if (updatedBaseline.established && (existingBaseline == null || !existingBaseline.established)) {
            baselineDao.insertOrUpdate(updatedBaseline)
        }

        stateDao.insertOrUpdate(
            PersonalCognitiveStateRecord(
                id = 1,
                updatedAt = System.currentTimeMillis(),
                totalSessionsProcessed = state.totalCompletedSessions,
                baselineEstablished = state.baselineEstablished,
                stateJson = state.toGeminiContextJson()
            )
        )

        return state
    }

    override suspend fun getFullUserAggregate(): User {
        val profile = profileDao.getUserProfileSync()
        val prefs = preferencesDao.getPreferencesSync() ?: UserPreferences()
        val todayRecords = hydrationDao.getRecordsForDateSync(getTodayDateString())
        return User(
            profile = profile,
            baseline = PersonalBaseline(established = false, createdAt = null),
            sessions = emptyList(),
            hydration = todayRecords,
            preferences = prefs
        )
    }

    override suspend fun resetAllData() {
        profileDao.clear()
        baselineDao.clear()
        stateDao.clear()
        hydrationDao.clear()
        sessionDao.clear()
        preferencesDao.clear()
    }
}
