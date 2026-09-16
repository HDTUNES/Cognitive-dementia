package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BreathingPreparationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PracticeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WelcomeSetupScreen
import com.example.ui.screens.caregiver.CaregiverDashboardScreen
import com.example.ui.screens.experiment.MemoryExperimentScreen
import com.example.ui.screens.experiment.ReactionExperimentScreen
import com.example.ui.theme.CognitiveAssistantTheme
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = viewModel(
                factory = AppViewModel.provideFactory(application)
            )
            val preferences by appViewModel.userPreferences.collectAsStateWithLifecycle()
            val userProfile by appViewModel.userProfile.collectAsStateWithLifecycle()
            val currentScreen by appViewModel.currentScreen.collectAsStateWithLifecycle()
            val todayRecords by appViewModel.todayRecords.collectAsStateWithLifecycle()
            val cognitiveState by appViewModel.cognitiveState.collectAsStateWithLifecycle()
            val notification by appViewModel.uiNotification.collectAsStateWithLifecycle()
            val isInitialized by appViewModel.isInitialized.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }
            var isEditingProfileInSettings by remember { mutableStateOf(false) }

            LaunchedEffect(notification) {
                notification?.let {
                    snackbarHostState.showSnackbar(it.message)
                    appViewModel.clearNotification()
                }
            }

            CognitiveAssistantTheme(darkTheme = preferences.isDarkMode) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (!isInitialized) {
                            // Waiting for initial state
                        } else if (isEditingProfileInSettings) {
                            BackHandler {
                                isEditingProfileInSettings = false
                            }
                            WelcomeSetupScreen(
                                initialName = userProfile?.name ?: "",
                                initialDob = userProfile?.dateOfBirth ?: "",
                                isDarkMode = preferences.isDarkMode,
                                onToggleDarkMode = { appViewModel.toggleDarkMode() },
                                onSaveProfile = { name, dob ->
                                    appViewModel.saveProfile(name, dob)
                                    isEditingProfileInSettings = false
                                },
                                isEditing = true,
                                onCancelEdit = { isEditingProfileInSettings = false }
                            )
                        } else {
                            when (currentScreen) {
                                AppDestination.SETUP -> {
                                    WelcomeSetupScreen(
                                        initialName = userProfile?.name ?: "",
                                        initialDob = userProfile?.dateOfBirth ?: "",
                                        isDarkMode = preferences.isDarkMode,
                                        onToggleDarkMode = { appViewModel.toggleDarkMode() },
                                        onSaveProfile = { name, dob ->
                                            appViewModel.saveProfile(name, dob)
                                        }
                                    )
                                }

                                AppDestination.BREATHING -> {
                                    BreathingPreparationScreen(
                                        onComplete = {
                                            appViewModel.onBreathingCompleted()
                                        }
                                    )
                                }

                                AppDestination.HOME -> {
                                    HomeScreen(
                                        userProfile = userProfile,
                                        isDarkMode = preferences.isDarkMode,
                                        onToggleDarkMode = { appViewModel.toggleDarkMode() },
                                        todayRecords = todayRecords,
                                        nextReminderTime = appViewModel.getNextReminderTimeString(),
                                        onDrankWater = { appViewModel.recordDrankWater() },
                                        onSimulateMissedReminder = { appViewModel.simulateMissedReminder() },
                                        onOpenPractice = { appViewModel.navigateTo(AppDestination.PRACTICE) },
                                        onOpenSettings = { appViewModel.navigateTo(AppDestination.SETTINGS) },
                                        onOpenCaregiverDashboard = { appViewModel.openCaregiverDashboard() }
                                    )
                                }

                                AppDestination.PRACTICE, AppDestination.PRACTICE_PLACEHOLDER -> {
                                    BackHandler {
                                        appViewModel.navigateTo(AppDestination.HOME)
                                    }
                                    PracticeScreen(
                                        onStartPractice = {
                                            appViewModel.startPractice()
                                        },
                                        onStartReactionExperiment = {
                                            appViewModel.startReactionExperiment()
                                        },
                                        onBackToHome = { appViewModel.navigateTo(AppDestination.HOME) }
                                    )
                                }

                                AppDestination.PRACTICE_BREATHING -> {
                                    BackHandler {
                                        appViewModel.navigateTo(AppDestination.PRACTICE)
                                    }
                                    BreathingPreparationScreen(
                                        title = "Prepare Your Attention",
                                        subtitle = "A brief 3-breath routine to settle and focus your attention before beginning the exercise.",
                                        continueButtonText = "Begin Practice",
                                        skipButtonText = "Skip to Practice",
                                        onComplete = {
                                            appViewModel.onPracticeBreathingCompleted()
                                        }
                                    )
                                }

                                AppDestination.MEMORY_EXPERIMENT -> {
                                    BackHandler {
                                        appViewModel.navigateTo(AppDestination.PRACTICE)
                                    }
                                    MemoryExperimentScreen(
                                        onSaveSession = { result ->
                                            appViewModel.saveMemoryExperimentSession(result)
                                        },
                                        onFinishAndReturnHome = {
                                            appViewModel.onExperimentFinished()
                                        }
                                    )
                                }

                                AppDestination.REACTION_EXPERIMENT -> {
                                    BackHandler {
                                        appViewModel.navigateTo(AppDestination.PRACTICE)
                                    }
                                    ReactionExperimentScreen(
                                        startingDifficultySeconds = appViewModel.getReactionAdaptiveDifficulty(),
                                        onSaveSession = { result ->
                                            appViewModel.saveReactionExperimentSession(result)
                                        },
                                        onFinishAndReturnHome = {
                                            appViewModel.onExperimentFinished()
                                        }
                                    )
                                }

                                AppDestination.SETTINGS -> {
                                    BackHandler {
                                        appViewModel.navigateTo(AppDestination.HOME)
                                    }
                                    SettingsScreen(
                                        userProfile = userProfile,
                                        userPreferences = preferences,
                                        isDarkMode = preferences.isDarkMode,
                                        onToggleDarkMode = { appViewModel.toggleDarkMode() },
                                        onEditProfile = { isEditingProfileInSettings = true },
                                        onChangeReminderInterval = { hours ->
                                            appViewModel.updateReminderInterval(hours)
                                        },
                                        onStartBreathing = {
                                            appViewModel.navigateTo(AppDestination.BREATHING)
                                        },
                                        onResetAllData = {
                                            appViewModel.resetAllDataForTesting()
                                        },
                                        onBack = { appViewModel.navigateTo(AppDestination.HOME) },
                                        onOpenCaregiverDashboard = { appViewModel.openCaregiverDashboard() }
                                    )
                                }

                                AppDestination.CAREGIVER_DASHBOARD -> {
                                    BackHandler {
                                        appViewModel.navigateTo(AppDestination.HOME)
                                    }
                                    CaregiverDashboardScreen(
                                        cognitiveState = cognitiveState,
                                        onRecalculateState = {
                                            appViewModel.recalculateCognitiveState()
                                        },
                                        onBackToHome = {
                                            appViewModel.navigateTo(AppDestination.HOME)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Retained for greeting preview and tests
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
