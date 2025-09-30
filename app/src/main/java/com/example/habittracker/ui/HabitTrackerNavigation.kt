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
                onHabitDetailsClick = { habitId -> navController.navigate("habit_details/$habitId") }
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
    var isCompletedToday by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(habitId) {
        try {
            habit = viewModel.getHabitById(habitId)
            progress = viewModel.getHabitProgress(habitId)
            
            // Check if completed today
            val today = java.time.LocalDate.now()
            isCompletedToday = progress?.completedDates?.contains(today) ?: false
            
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            // Handle error
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
            HabitDetailsScreen(
                habit = currentHabit,
                progress = currentProgress,
                isCompletedToday = isCompletedToday,
                onBackClick = onBackClick,
                onMarkCompleted = {
                    viewModel.markHabitCompleted(habitId)
                    isCompletedToday = true
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