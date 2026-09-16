package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.HydrationRecord
import com.example.data.model.UserProfile
import com.example.data.model.UserPreferences
import com.example.data.model.UserSession
import com.example.experiment.memory.MemoryExperimentSessionResult
import com.example.experiment.reaction.ReactionExperimentSessionResult
import com.example.data.repository.CognitiveRepository
import com.example.data.repository.CognitiveRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AppDestination {
    SETUP,
    BREATHING,
    HOME,
    PRACTICE,
    PRACTICE_BREATHING,
    MEMORY_EXPERIMENT,
    REACTION_EXPERIMENT,
    PRACTICE_PLACEHOLDER,
    SETTINGS,
    CAREGIVER_DASHBOARD
}

data class UiNotification(
    val message: String,
    val isError: Boolean = false
)

class AppViewModel(
    application: Application,
    private val repository: CognitiveRepository
) : AndroidViewModel(application) {

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userPreferences: StateFlow<UserPreferences> = repository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val todayRecords: StateFlow<List<HydrationRecord>> = repository.getTodayHydrationRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<UserSession>> = repository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cognitiveState: StateFlow<com.example.processor.PersonalCognitiveState> = repository.personalCognitiveState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.processor.PersonalCognitiveState())

    private val _currentScreen = MutableStateFlow(AppDestination.SETUP)
    val currentScreen: StateFlow<AppDestination> = _currentScreen.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _uiNotification = MutableStateFlow<UiNotification?>(null)
    val uiNotification: StateFlow<UiNotification?> = _uiNotification.asStateFlow()

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    init {
        viewModelScope.launch {
            // Check if user has already completed setup
            val profile = repository.userProfile.first()
            if (profile != null) {
                // Profile exists: show calming 3-breath preparation routine before entering Home
                _currentScreen.value = AppDestination.BREATHING
            } else {
                _currentScreen.value = AppDestination.SETUP
            }
            _isInitialized.value = true

            // Check and mark any missed reminder intervals today
            repository.checkAndProcessMissedReminders()
        }
    }

    fun navigateTo(destination: AppDestination) {
        _currentScreen.value = destination
    }

    fun saveProfile(name: String, dateOfBirth: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiNotification.value = UiNotification("Please enter your name", isError = true)
                return@launch
            }
            if (dateOfBirth.isBlank()) {
                _uiNotification.value = UiNotification("Please enter your date of birth", isError = true)
                return@launch
            }
            repository.saveUserProfile(name, dateOfBirth)
            // After setup, proceed to calming 3-breath preparation
            _currentScreen.value = AppDestination.BREATHING
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val currentMode = userPreferences.value.isDarkMode
            repository.setDarkMode(!currentMode)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkMode(enabled)
        }
    }

    fun updateReminderInterval(intervalHours: Int) {
        viewModelScope.launch {
            repository.setReminderIntervalHours(intervalHours)
            _uiNotification.value = UiNotification("Reminders set to every $intervalHours hours")
        }
    }

    fun recordDrankWater() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.recordHydrationEvent(now)
            _uiNotification.value = UiNotification("Recorded water intake at ${timeFormat.format(Date(now))}")
        }
    }

    /**
     * Allows explicit verification and testing of the missed reminder mechanism.
     * Marks an expected reminder interval as missed.
     */
    fun simulateMissedReminder() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.recordMissedReminderEvent(now)
            _uiNotification.value = UiNotification("Reminder marked as missed")
        }
    }

    fun onBreathingCompleted() {
        viewModelScope.launch {
            repository.recordSession(
                sessionType = UserSession.TYPE_PREPARATION,
                startedAt = System.currentTimeMillis() - 18000,
                completedAt = System.currentTimeMillis(),
                cyclesCompleted = 3,
                isCompleted = true
            )
            _currentScreen.value = AppDestination.HOME
        }
    }

    fun startPractice() {
        _currentScreen.value = AppDestination.PRACTICE_BREATHING
    }

    fun onPracticeBreathingCompleted() {
        viewModelScope.launch {
            repository.recordSession(
                sessionType = UserSession.TYPE_PREPARATION,
                startedAt = System.currentTimeMillis() - 18000,
                completedAt = System.currentTimeMillis(),
                cyclesCompleted = 3,
                isCompleted = true
            )
            _currentScreen.value = AppDestination.MEMORY_EXPERIMENT
        }
    }

    fun saveMemoryExperimentSession(result: MemoryExperimentSessionResult) {
        viewModelScope.launch {
            repository.recordSession(
                sessionType = UserSession.TYPE_MEMORY_RECOGNITION,
                startedAt = result.startedAt,
                completedAt = result.completedAt,
                cyclesCompleted = result.measurements.completedTrials,
                isCompleted = true,
                metadata = result.metadataJson
            )
        }
    }

    fun startReactionExperiment() {
        _currentScreen.value = AppDestination.REACTION_EXPERIMENT
    }

    fun saveReactionExperimentSession(result: ReactionExperimentSessionResult) {
        viewModelScope.launch {
            repository.recordSession(
                sessionType = UserSession.TYPE_REACTION_COORDINATION,
                startedAt = result.startedAt,
                completedAt = result.completedAt,
                cyclesCompleted = result.measurements.completedTrials,
                isCompleted = true,
                metadata = result.metadataJson
            )
        }
    }

    fun getReactionAdaptiveDifficulty(): Double {
        val lastReactionSession = allSessions.value
            .filter { it.sessionType == UserSession.TYPE_REACTION_COORDINATION && it.isCompleted }
            .maxByOrNull { it.startedAt }

        if (lastReactionSession != null && !lastReactionSession.metadata.isNullOrBlank()) {
            try {
                val json = org.json.JSONObject(lastReactionSession.metadata)
                if (json.has("metrics")) {
                    val m = json.getJSONObject("metrics")
                    val diff = m.optDouble("adaptive_difficulty_seconds", 2.0)
                    if (!diff.isNaN() && diff > 0.0) return diff
                }
            } catch (_: Exception) {
                // Ignore parsing errors and return default
            }
        }
        return 2.0
    }

    fun onExperimentFinished() {
        _currentScreen.value = AppDestination.HOME
    }

    fun openCaregiverDashboard() {
        _currentScreen.value = AppDestination.CAREGIVER_DASHBOARD
    }

    fun recalculateCognitiveState() {
        viewModelScope.launch {
            repository.recalculateCognitiveState()
        }
    }

    fun clearNotification() {
        _uiNotification.value = null
    }

    fun resetAllDataForTesting() {
        viewModelScope.launch {
            repository.resetAllData()
            _currentScreen.value = AppDestination.SETUP
            _uiNotification.value = UiNotification("App reset to initial state")
        }
    }

    /**
     * Calculates the next expected reminder time today based on user's preference.
     */
    fun getNextReminderTimeString(): String {
        val prefs = userPreferences.value
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        for (hour in prefs.reminderStartHour..prefs.reminderEndHour step prefs.reminderIntervalHours) {
            if (hour > currentHour) {
                val nextCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, 0)
                }
                return timeFormat.format(nextCal.time)
            }
        }
        return "Tomorrow at ${prefs.reminderStartHour}:00 AM"
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(application)
                    val repo = CognitiveRepositoryImpl(db)
                    return AppViewModel(application, repo) as T
                }
            }
    }
}
