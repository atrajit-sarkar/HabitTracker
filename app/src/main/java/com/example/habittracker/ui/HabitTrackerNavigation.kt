package com.example.habittracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.habittracker.data.local.Habit
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun HabitTrackerNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = "home"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("home") {
            val viewModel: HabitViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            
            HabitHomeRoute(
                state = state,
                onAddHabitClick = { navController.navigate("add_habit") },
                onToggleReminder = viewModel::toggleReminder,
                onMarkHabitCompleted = viewModel::markHabitCompleted,
                onDeleteHabit = viewModel::deleteHabit,
                onHabitDetailsClick = { habitId -> navController.navigate("habit_details/$habitId") },
                onTrashClick = { navController.navigate("trash") }
            )
        }
        
        composable("add_habit") {
            val viewModel: HabitViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            
            AddHabitScreen(
                state = state.addHabitState,
                onHabitNameChange = viewModel::onHabitNameChange,
                onHabitDescriptionChange = viewModel::onHabitDescriptionChange,
                onHabitReminderToggleChange = viewModel::onHabitReminderToggle,
                onHabitTimeChange = viewModel::onHabitTimeChange,
                onHabitFrequencyChange = viewModel::onHabitFrequencyChange,
                onHabitDayOfWeekChange = viewModel::onHabitDayOfWeekChange,
                onHabitDayOfMonthChange = viewModel::onHabitDayOfMonthChange,
                onHabitMonthOfYearChange = viewModel::onHabitMonthOfYearChange,
                onAvatarChange = viewModel::onAvatarChange,
                onNotificationSoundChange = viewModel::onNotificationSoundChange,
                onBackClick = { 
                    viewModel.resetAddHabitState()
                    navController.popBackStack()
                },
                onSaveHabit = {
                    viewModel.saveHabit()
                    navController.popBackStack()
                }
            )
        }
        
        composable("habit_details/{habitId}") { backStackEntry ->
            val habitId = backStackEntry.arguments?.getString("habitId")?.toLongOrNull() ?: return@composable
            val viewModel: HabitViewModel = hiltViewModel()
            
            HabitDetailsRoute(
                habitId = habitId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable("trash") {
            val viewModel: HabitViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            
            TrashScreen(
                deletedHabits = state.deletedHabits,
                onRestoreHabit = viewModel::restoreHabit,
                onPermanentlyDeleteHabit = viewModel::permanentlyDeleteHabit,
                onEmptyTrash = viewModel::emptyTrash,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun HabitDetailsRoute(
    habitId: Long,
    viewModel: HabitViewModel,
    onBackClick: () -> Unit
) {
    var habit by remember { mutableStateOf<Habit?>(null) }
    var progress by remember { mutableStateOf<HabitProgress?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // Function to refresh progress data
    suspend fun refreshProgress() {
        try {
            val loadedHabit = viewModel.getHabitById(habitId)
            val loadedProgress = viewModel.getHabitProgress(habitId)
            habit = loadedHabit
            progress = loadedProgress

            val creationDate = LocalDate.ofInstant(loadedHabit.createdAt, ZoneOffset.UTC)
            val today = LocalDate.now()
            val adjustedDate = when {
                selectedDate.isBefore(creationDate) -> creationDate
                selectedDate.isAfter(today) -> today
                else -> selectedDate
            }
            if (adjustedDate != selectedDate) {
                selectedDate = adjustedDate
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    // Initial load only shows full screen loader
    LaunchedEffect(habitId) {
        isLoading = true
        refreshProgress()
        isLoading = false
    }

    // Subsequent refreshes (e.g. after marking completion) should not blank the screen
    LaunchedEffect(refreshTrigger) {
        if (!isLoading) {
            refreshProgress()
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        val currentHabit = habit
        val currentProgress = progress
        
        if (currentHabit != null && currentProgress != null) {
            val isSelectedDateCompleted = currentProgress.completedDates.contains(selectedDate)
            HabitDetailsScreen(
                habit = currentHabit,
                progress = currentProgress,
                selectedDate = selectedDate,
                isSelectedDateCompleted = isSelectedDateCompleted,
                onBackClick = onBackClick,
                onSelectDate = { date ->
                    selectedDate = date
                },
                onMarkCompleted = {
                    viewModel.markHabitCompletedForDate(habitId, selectedDate)
                    // Trigger a lightweight refresh without showing the full-screen loader
                    refreshTrigger++
                }
            )
        } else {
            // Error state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Habit not found")
            }
        }
    }
}