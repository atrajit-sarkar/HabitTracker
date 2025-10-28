package it.atraj.habittracker.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.atraj.habittracker.auth.AuthRepository
import it.atraj.habittracker.auth.User
import it.atraj.habittracker.data.HabitRepository
import it.atraj.habittracker.data.StreakCalculator
import it.atraj.habittracker.data.firestore.UserRewardsRepository
import it.atraj.habittracker.data.local.Habit
import it.atraj.habittracker.data.local.HabitAvatar
import it.atraj.habittracker.data.local.HabitFrequency
import it.atraj.habittracker.data.local.NotificationSound
import it.atraj.habittracker.data.local.UserRewards
import it.atraj.habittracker.notification.HabitReminderScheduler
import it.atraj.habittracker.notification.HabitReminderService
import it.atraj.habittracker.ui.social.ProfileStatsUpdater
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import it.atraj.habittracker.image.HabitImagePreloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.temporal.ChronoUnit
import java.time.ZoneOffset

@HiltViewModel
class HabitViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: HabitReminderScheduler,
    private val authRepository: AuthRepository,
    private val profileStatsUpdater: ProfileStatsUpdater,
    private val userRewardsRepository: UserRewardsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitScreenState(isLoading = true))
    val uiState: StateFlow<HabitScreenState> = _uiState.asStateFlow()
    
    // Cache available sounds
    private var availableSounds: List<NotificationSound> = emptyList()
    
    // Track current user for stats updates
    private var currentUser: User? = null
    
    // User rewards state
    private val _userRewards = MutableStateFlow(UserRewards())
    val userRewards: StateFlow<UserRewards> = _userRewards.asStateFlow()
    
    // Purchased themes state
    private val _purchasedThemes = MutableStateFlow<List<String>>(emptyList()) // Start with empty list
    val purchasedThemes: StateFlow<List<String>> = _purchasedThemes.asStateFlow()
    
    // Flag to track if themes have been loaded
    private val _themesLoaded = MutableStateFlow(false)
    val themesLoaded: StateFlow<Boolean> = _themesLoaded.asStateFlow()
    
    // Purchased hero backgrounds state
    private val _purchasedHeroBackgrounds = MutableStateFlow<List<String>>(listOf("itachi"))
    val purchasedHeroBackgrounds: StateFlow<List<String>> = _purchasedHeroBackgrounds.asStateFlow()

    init {
        // Observe current user for stats updates
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                currentUser = user
                // Initialize user rewards when user signs in
                if (user != null) {
                    userRewardsRepository.initializeUserRewards()
                }
            }
        }
        
        // Observe user rewards
        viewModelScope.launch {
            userRewardsRepository.observeUserRewards().collectLatest { rewards ->
                _userRewards.value = rewards
            }
        }
        
        // Load purchased themes eagerly
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                if (user != null) {
                    val themes = userRewardsRepository.getPurchasedThemes()
                    _purchasedThemes.value = themes
                    _themesLoaded.value = true
                } else {
                    _purchasedThemes.value = emptyList()
                    _themesLoaded.value = false
                }
            }
        }
        
        // Load purchased hero backgrounds
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                if (user != null) {
                    val backgrounds = userRewardsRepository.getPurchasedHeroBackgrounds()
                    _purchasedHeroBackgrounds.value = backgrounds
                }
            }
        }
        
        // Reschedule all reminders on app startup
        // This ensures reminders work after app reinstall
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val habits = habitRepository.getAllHabits()
                var rescheduled = 0
                habits.filter { !it.isDeleted && it.reminderEnabled }.forEach { habit ->
                    try {
                        reminderScheduler.schedule(habit)
                        rescheduled++
                    } catch (e: Exception) {
                        android.util.Log.e("HabitViewModel", "Failed to reschedule ${habit.title}: ${e.message}")
                    }
                }
                android.util.Log.d("HabitViewModel", "Rescheduled $rescheduled reminders on app startup")
            } catch (e: Exception) {
                android.util.Log.e("HabitViewModel", "Error rescheduling reminders on startup: ${e.message}")
            }
        }
        
        // Load available notification sounds
        viewModelScope.launch(Dispatchers.IO) {
            availableSounds = NotificationSound.getAllAvailableSounds(context)
            _uiState.update { state ->
                state.copy(
                    addHabitState = state.addHabitState.copy(
                        availableSounds = availableSounds
                    )
                )
            }
        }
        
        // Track current date to detect day changes and recalculate streaks
        var lastKnownDate = LocalDate.now()
        var isFirstLoad = true
        
        viewModelScope.launch {
            habitRepository.observeHabits().collectLatest { habits ->
                // Check if date has changed since last update
                val currentDate = LocalDate.now()
                val dateChanged = currentDate != lastKnownDate
                lastKnownDate = currentDate
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        habits = habits.map(::mapToUi).sortedBy { it.reminderTime }
                    )
                }
                
                // Recalculate streaks on app open or date change
                if (isFirstLoad || dateChanged) {
                    if (dateChanged) {
                        android.util.Log.d("HabitViewModel", "Date changed detected, recalculating all streaks")
                    } else {
                        android.util.Log.d("HabitViewModel", "App opened, recalculating all streaks")
                    }
                    
                    // Recalculate streaks for all non-deleted habits
                    viewModelScope.launch(Dispatchers.IO) {
                        recalculateAllStreaks()
                    }
                    
                    isFirstLoad = false
                }
                
        // Sync notification channels after habits are loaded
                // This ensures all active habits have their channels in the system
                if (habits.isNotEmpty()) {
                    HabitReminderService.syncAllHabitChannels(context, habits)
                    
                    // Preload custom images in background (non-blocking, one-time)
                    // This ensures notifications have cached images even after cache clear
                    scheduleImagePreload()
                }
            }
        }
        
        viewModelScope.launch {
            habitRepository.observeDeletedHabits().collectLatest { deletedHabits ->
                _uiState.update { state ->
                    state.copy(deletedHabits = deletedHabits)
                }
            }
        }
        
        // Schedule cleanup of old deleted habits
        viewModelScope.launch {
            // First, get the IDs of habits that will be cleaned up
            val deletedHabitsToCleanup = withContext(Dispatchers.IO) {
                // Get all deleted habits older than 30 days
                val allDeleted = _uiState.value.deletedHabits
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
                allDeleted.filter { habit ->
                    habit.deletedAt != null && habit.deletedAt.toEpochMilli() < thirtyDaysAgo
                }.map { it.id }
            }
            
            // Cleanup from repository
            withContext(Dispatchers.IO) {
                habitRepository.cleanupOldDeletedHabits()
            }
            
            // Delete notification channels for cleaned up habits
            if (deletedHabitsToCleanup.isNotEmpty()) {
                HabitReminderService.deleteMultipleHabitChannels(context, deletedHabitsToCleanup)
                android.util.Log.d("HabitViewModel", "Cleaned up ${deletedHabitsToCleanup.size} notification channels for old deleted habits")
            }
        }
    }

    /**
     * Refresh habits UI to recalculate completion states.
     * Call this when the app resumes or when date might have changed.
     */
    fun refreshHabitsUI() {
        viewModelScope.launch(Dispatchers.IO) {
            val habits = habitRepository.getAllHabits()
            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    state.copy(
                        habits = habits.map(::mapToUi).sortedBy { it.reminderTime }
                    )
                }
            }
            android.util.Log.d("HabitViewModel", "Habits UI refreshed - completion states recalculated")
        }
    }
    
    /**
     * Schedule background image preload for custom habit avatars.
     * Non-blocking, runs once, ensures notifications have cached images.
     */
    private fun scheduleImagePreload() {
        val workRequest = OneTimeWorkRequestBuilder<HabitImagePreloadWorker>().build()
        
        // Use KEEP policy - if already running/scheduled, don't start another
        WorkManager.getInstance(context).enqueueUniqueWork(
            HabitImagePreloadWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
        
        android.util.Log.d("HabitViewModel", "Scheduled background image preload")
    }

    fun showAddHabitSheet() {
        _uiState.update { it.copy(
            isAddSheetVisible = true,
            addHabitState = AddHabitState(availableSounds = availableSounds)
        ) }
    }

    fun hideAddHabitSheet() {
        _uiState.update { it.copy(isAddSheetVisible = false, addHabitState = AddHabitState(availableSounds = availableSounds)) }
    }

    fun resetAddHabitState() {
        _uiState.update { it.copy(addHabitState = AddHabitState(availableSounds = availableSounds)) }
    }

    fun onHabitNameChange(value: String) {
        _uiState.update { state ->
            state.copy(
                addHabitState = state.addHabitState.copy(
                    title = value,
                    nameError = null
                )
            )
        }
    }

    fun onHabitDescriptionChange(value: String) {
        _uiState.update { state ->
            state.copy(
                addHabitState = state.addHabitState.copy(description = value)
            )
        }
    }

    fun onHabitReminderToggle(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                addHabitState = state.addHabitState.copy(reminderEnabled = enabled)
            )
        }
    }

    fun onHabitTimeChange(hour: Int, minute: Int) {
        _uiState.update { state ->
            state.copy(
                addHabitState = state.addHabitState.copy(hour = hour, minute = minute)
            )
        }
    }

    fun onHabitFrequencyChange(frequency: HabitFrequency) {
        _uiState.update { state ->
            state.copy(
                addHabitState = state.addHabitState.copy(frequency = frequency)
            )
        }
    }

    fun onHabitDayOfWeekChange(dayOfWeek: Int) {
        _uiState.update { state ->
            state.copy(
                addHabitState = state.addHabitState.copy(dayOfWeek = dayOfWeek)
            )
        }
    }

    fun onHabitDayOfMonthChange(dayOfMonth: Int) {
        _uiState.update { state ->
            state.copy(
                addHabitState = state.addHabitState.copy(dayOfMonth = dayOfMonth)
            )
        }
    }

    fun onHabitMonthOfYearChange(monthOfYear: Int) {
        _uiState.update { state ->
            state.copy(
                addHabitState = state.addHabitState.copy(monthOfYear = monthOfYear)
            )
        }
    }

    fun onNotificationSoundChange(sound: NotificationSound) {
        android.util.Log.d("HabitViewModel", "Notification sound changed to: ${sound.displayName} (ID: ${sound.id}, URI: ${sound.uri})")
        _uiState.update { state ->
            state.copy(
                addHabitState = state.addHabitState.copy(notificationSound = sound)
            )
        }
    }

    fun onAvatarChange(avatar: HabitAvatar) {
        _uiState.update { state ->
            state.copy(
                addHabitState = state.addHabitState.copy(avatar = avatar)
            )
        }
    }

    suspend fun saveHabit() {
        val currentState = _uiState.value
        if (currentState.addHabitState.isSaving) return
        val title = currentState.addHabitState.title.trim()
        if (title.isBlank()) {
            _uiState.update { state ->
                state.copy(
                    addHabitState = state.addHabitState.copy(
                        nameError = state.addHabitState.nameError
                            ?: state.habitsValidationError()
                    )
                )
            }
            return
        }
        
        _uiState.update { it.copy(addHabitState = it.addHabitState.copy(isSaving = true)) }
        val addForm = _uiState.value.addHabitState
        
        // Log the notification sound being saved
        android.util.Log.d("HabitViewModel", "Saving habit with notification sound: ${addForm.notificationSound.displayName} (ID: ${addForm.notificationSound.id}, URI: ${addForm.notificationSound.uri})")
        
        val habit = if (addForm.isEditMode && addForm.editingHabitId != null) {
            // Edit mode: get existing habit and update it
            android.util.Log.d("HabitViewModel", "EDIT MODE: Updating habit ID=${addForm.editingHabitId}")
            val existingHabit = withContext(Dispatchers.IO) {
                habitRepository.getHabitById(addForm.editingHabitId)
            }
            android.util.Log.d("HabitViewModel", "Existing habit loaded: ${existingHabit.title}")
            android.util.Log.d("HabitViewModel", "New values - Hour: ${addForm.hour}, Minute: ${addForm.minute}, Avatar: ${addForm.avatar}, Sound: ${addForm.notificationSound.displayName}")
            
            existingHabit.copy(
                title = title,
                description = addForm.description.trim(),
                reminderHour = addForm.hour,
                reminderMinute = addForm.minute,
                reminderEnabled = addForm.reminderEnabled,
                frequency = addForm.frequency,
                dayOfWeek = if (addForm.frequency == HabitFrequency.WEEKLY) addForm.dayOfWeek else null,
                dayOfMonth = if (addForm.frequency == HabitFrequency.MONTHLY) addForm.dayOfMonth else null,
                monthOfYear = if (addForm.frequency == HabitFrequency.YEARLY) addForm.monthOfYear else null,
                notificationSoundId = addForm.notificationSound.id,
                notificationSoundName = addForm.notificationSound.displayName,
                notificationSoundUri = addForm.notificationSound.uri,
                avatar = addForm.avatar
            )
        } else {
            // Create mode: new habit
            android.util.Log.d("HabitViewModel", "CREATE MODE: Creating new habit")
            Habit(
                title = title,
                description = addForm.description.trim(),
                reminderHour = addForm.hour,
                reminderMinute = addForm.minute,
                reminderEnabled = addForm.reminderEnabled,
                frequency = addForm.frequency,
                dayOfWeek = if (addForm.frequency == HabitFrequency.WEEKLY) addForm.dayOfWeek else null,
                dayOfMonth = if (addForm.frequency == HabitFrequency.MONTHLY) addForm.dayOfMonth else null,
                monthOfYear = if (addForm.frequency == HabitFrequency.YEARLY) addForm.monthOfYear else null,
                notificationSoundId = addForm.notificationSound.id,
                notificationSoundName = addForm.notificationSound.displayName,
                notificationSoundUri = addForm.notificationSound.uri,
                avatar = addForm.avatar
            )
        }
        
        android.util.Log.d("HabitViewModel", "Habit object created: soundId=${habit.notificationSoundId}, soundName=${habit.notificationSoundName}, soundUri=${habit.notificationSoundUri}")
        android.util.Log.d("HabitViewModel", "Habit details - ID: ${habit.id}, Title: ${habit.title}, Hour: ${habit.reminderHour}, Minute: ${habit.reminderMinute}")
        android.util.Log.d("HabitViewModel", "Habit avatar: ${habit.avatar}, Frequency: ${habit.frequency}")
        
        val savedHabit = withContext(Dispatchers.IO) {
            if (addForm.isEditMode && addForm.editingHabitId != null) {
                android.util.Log.d("HabitViewModel", "Calling habitRepository.updateHabit() for ID=${habit.id}")
                try {
                    habitRepository.updateHabit(habit)
                    android.util.Log.d("HabitViewModel", "✅ habitRepository.updateHabit() completed successfully!")
                } catch (e: Exception) {
                    android.util.Log.e("HabitViewModel", "❌ Error updating habit in repository", e)
                    throw e
                }
                habit
            } else {
                android.util.Log.d("HabitViewModel", "Calling habitRepository.insertHabit()")
                val id = habitRepository.insertHabit(habit)
                android.util.Log.d("HabitViewModel", "✅ Habit inserted with ID: $id")
                habit.copy(id = id)
            }
        }
        
        android.util.Log.d("HabitViewModel", "Habit saved to database with ID: ${savedHabit.id}, calling updateHabitChannel...")
        
        // Force update notification channel with the new/changed sound
        HabitReminderService.updateHabitChannel(context, savedHabit)
        
        if (savedHabit.reminderEnabled) {
            reminderScheduler.schedule(savedHabit)
        } else {
            reminderScheduler.cancel(savedHabit.id)
        }
        _uiState.update { state ->
            state.copy(
                snackbarMessage = if (addForm.isEditMode) "Habit updated" else state.habitSavedMessage(),
                addHabitState = AddHabitState(availableSounds = availableSounds),
                isAddSheetVisible = false
            )
        }
        
        // Update user's stats on leaderboard in background (non-blocking)
        // Launch in separate coroutine to avoid blocking UI
        viewModelScope.launch(Dispatchers.IO) {
            updateUserStatsAsync()
        }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun toggleReminder(habitId: Long, enabled: Boolean) {
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) {
                val habit = habitRepository.getHabitById(habitId) ?: return@withContext null
                val newHabit = habit.copy(reminderEnabled = enabled)
                habitRepository.updateHabit(newHabit)
                newHabit
            } ?: return@launch
            
            // Update notification channel to ensure sound is correct
            HabitReminderService.updateHabitChannel(context, updated)
            
            if (enabled) {
                reminderScheduler.schedule(updated)
            } else {
                reminderScheduler.cancel(updated.id)
            }
        }
    }

    fun markHabitCompleted(habitId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // Mark as completed
            habitRepository.markCompletedToday(habitId)
            
            // Calculate and update streak
            updateHabitStreak(habitId)
            
            // Notify icon manager to check for overdue habits
            it.atraj.habittracker.receiver.HabitCompletionReceiver.sendHabitCompletedBroadcast(context)
        }
        // Update stats after completion in separate coroutine (non-blocking)
        viewModelScope.launch(Dispatchers.IO) {
            updateUserStatsAsync()
        }
    }

    fun markHabitCompletedForDate(habitId: Long, date: java.time.LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            habitRepository.markCompletedForDate(habitId, date)
            
            // Notify icon manager to check for overdue habits
            it.atraj.habittracker.receiver.HabitCompletionReceiver.sendHabitCompletedBroadcast(context)
        }
        // Update stats after completion in separate coroutine (non-blocking)
        viewModelScope.launch(Dispatchers.IO) {
            updateUserStatsAsync()
        }
    }
    
    /**
     * Update user's public profile stats based on current habits.
     * Now fetches habits directly from Firestore for accurate stats.
     */
    private suspend fun updateUserStatsAsync() {
        val user = currentUser ?: run {
            android.util.Log.d("HabitViewModel", "updateUserStatsAsync: No current user")
            return
        }
        
        android.util.Log.d("HabitViewModel", "updateUserStatsAsync: Starting for user ${user.uid}")
        android.util.Log.d("HabitViewModel", "updateUserStatsAsync: Fetching fresh habits from Firestore")
        
        // Delay to ensure Firestore snapshot listener has time to update
        // Increased to 800ms to allow completion data and streak calculations to sync
        kotlinx.coroutines.delay(800)
        
        // Use the new method that fetches habits directly from Firestore
        // This ensures we always have the latest data, even if UI state isn't loaded yet
        profileStatsUpdater.updateUserStats(user)
        
        android.util.Log.d("HabitViewModel", "updateUserStatsAsync: Stats update complete")
    }
    
    /**
     * Recalculate streaks for all habits
     * Called on app open and daily at midnight
     */
    private suspend fun recalculateAllStreaks() {
        try {
            val habits = habitRepository.getAllHabits()
            val nonDeletedHabits = habits.filter { !it.isDeleted }
            val currentDate = LocalDate.now()
            
            android.util.Log.d("HabitViewModel", "Recalculating streaks for ${nonDeletedHabits.size} habits")
            
            var totalDiamondsEarned = 0
            var totalFreezeDaysUsed = 0
            
            nonDeletedHabits.forEach { habit ->
                try {
                    // CRITICAL FIX: Skip if streak was already calculated today
                    // This prevents duplicate freeze day deductions when opening habit details
                    if (habit.lastStreakUpdate == currentDate) {
                        android.util.Log.d("HabitViewModel", 
                            "Skipping ${habit.title}: already calculated today")
                        return@forEach
                    }
                    
                    val completions = habitRepository.getHabitCompletions(habit.id)
                    val availableFreezeDays = _userRewards.value.freezeDays
                    
                    // Calculate streak
                    val result = StreakCalculator.calculateStreak(
                        habit = habit,
                        completions = completions,
                        currentDate = currentDate,
                        availableFreezeDays = availableFreezeDays
                    )
                    
                    // Track diamonds and freeze usage
                    totalDiamondsEarned += result.diamondsEarned
                    totalFreezeDaysUsed += result.freezeDaysUsed
                    
                    // Update habit only if streak changed
                    if (result.newStreak != habit.streak || 
                        result.diamondsEarned > 0 || 
                        result.freezeDaysUsed > 0) {
                        
                        val newHighest = maxOf(habit.highestStreakAchieved, result.newStreak)
                        val updatedHabit = habit.copy(
                            streak = result.newStreak,
                            highestStreakAchieved = newHighest,
                            lastStreakUpdate = currentDate
                        )
                        
                        habitRepository.updateHabit(updatedHabit)
                        android.util.Log.d("HabitViewModel", 
                            "Updated ${habit.title}: streak=${result.newStreak}, " +
                            "diamonds=${result.diamondsEarned}, freeze=${result.freezeDaysUsed}")
                    } else {
                        // Even if no changes, update lastStreakUpdate to prevent recalculation
                        val updatedHabit = habit.copy(lastStreakUpdate = currentDate)
                        habitRepository.updateHabit(updatedHabit)
                        android.util.Log.d("HabitViewModel", 
                            "No changes for ${habit.title}, but marked as calculated today")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HabitViewModel", 
                        "Error recalculating streak for ${habit.title}", e)
                }
            }
            
            // Apply freeze days usage (batch)
            if (totalFreezeDaysUsed > 0) {
                repeat(totalFreezeDaysUsed) {
                    userRewardsRepository.useFreezeDayIfAvailable()
                }
                android.util.Log.d("HabitViewModel", "Used $totalFreezeDaysUsed freeze days total")
            }
            
            // Award diamonds (batch)
            if (totalDiamondsEarned > 0) {
                userRewardsRepository.addDiamonds(totalDiamondsEarned)
                android.util.Log.d("HabitViewModel", "Awarded $totalDiamondsEarned diamonds total")
            }
            
            android.util.Log.d("HabitViewModel", "Streak recalculation complete")
            
        } catch (e: Exception) {
            android.util.Log.e("HabitViewModel", "Error in recalculateAllStreaks", e)
        }
    }
    
    /**
     * Calculate and update streak for a habit
     * Called when marking a habit as completed
     */
    private suspend fun updateHabitStreak(habitId: Long) {
        try {
            val habit = habitRepository.getHabitById(habitId)
            val completions = habitRepository.getHabitCompletions(habitId)
            val currentDate = LocalDate.now()
            
            android.util.Log.d("HabitViewModel", "Calculating streak for ${habit.title}")
            
            // SAFETY CHECK: If already calculated today AND no new completion, skip
            // This prevents duplicate freeze deductions when habit is already up-to-date
            val hasNewCompletion = completions.any { it.completedDate == currentDate }
            if (habit.lastStreakUpdate == currentDate && !hasNewCompletion) {
                android.util.Log.d("HabitViewModel", 
                    "Skipping ${habit.title}: already calculated today with no new completion")
                return
            }
            
            val availableFreezeDays = _userRewards.value.freezeDays
            
            // Calculate streak
            val result = StreakCalculator.calculateStreak(
                habit = habit,
                completions = completions,
                currentDate = currentDate,
                availableFreezeDays = availableFreezeDays
            )
            
            android.util.Log.d("HabitViewModel", 
                "Streak result: newStreak=${result.newStreak}, diamonds=${result.diamondsEarned}, " +
                "freezeUsed=${result.freezeDaysUsed}, grace=${result.graceUsed}")
            
            // Use freeze days if needed
            if (result.freezeDaysUsed > 0) {
                repeat(result.freezeDaysUsed) {
                    userRewardsRepository.useFreezeDayIfAvailable()
                }
            }
            
            // Award diamonds for milestones
            if (result.diamondsEarned > 0) {
                userRewardsRepository.addDiamonds(result.diamondsEarned)
                android.util.Log.d("HabitViewModel", "Awarded ${result.diamondsEarned} diamonds!")
            }
            
            // Update habit with new streak
            val newHighest = maxOf(habit.highestStreakAchieved, result.newStreak)
            val updatedHabit = habit.copy(
                streak = result.newStreak,
                highestStreakAchieved = newHighest,
                lastStreakUpdate = currentDate
            )
            
            habitRepository.updateHabit(updatedHabit)
            android.util.Log.d("HabitViewModel", "Updated habit streak to ${result.newStreak}")
            
        } catch (e: Exception) {
            android.util.Log.e("HabitViewModel", "Error updating habit streak", e)
        }
    }
    
    /**
     * Purchase freeze days with diamonds
     */
    suspend fun purchaseFreezeDays(days: Int, cost: Int): Boolean {
        return try {
            userRewardsRepository.purchaseFreezeDays(days, cost)
        } catch (e: Exception) {
            android.util.Log.e("HabitViewModel", "Error purchasing freeze days", e)
            false
        }
    }
    
    /**
     * Purchase a theme with diamonds
     */
    suspend fun purchaseTheme(themeId: String, cost: Int): Boolean {
        return try {
            val success = userRewardsRepository.purchaseTheme(themeId, cost)
            if (success) {
                // Refresh purchased themes list
                val themes = userRewardsRepository.getPurchasedThemes()
                _purchasedThemes.value = themes
            }
            success
        } catch (e: Exception) {
            android.util.Log.e("HabitViewModel", "Error purchasing theme", e)
            false
        }
    }
    
    /**
     * Purchase a hero background with diamonds
     */
    suspend fun purchaseHeroBackground(heroId: String, cost: Int): Boolean {
        return try {
            val success = userRewardsRepository.purchaseHeroBackground(heroId, cost)
            if (success) {
                // Refresh purchased hero backgrounds list
                val backgrounds = userRewardsRepository.getPurchasedHeroBackgrounds()
                _purchasedHeroBackgrounds.value = backgrounds
            }
            success
        } catch (e: Exception) {
            android.util.Log.e("HabitViewModel", "Error purchasing hero background", e)
            false
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            // Indicate deletion in progress so UI overlay stays visible
            _uiState.update { it.copy(isDeleting = true) }
            val habit = withContext(Dispatchers.IO) {
                habitRepository.getHabitById(habitId)
            }
            withContext(Dispatchers.IO) {
                habitRepository.moveToTrash(habitId)
            }
            reminderScheduler.cancel(habit.id)
            
            // Note: We don't delete the channel yet because user might restore from trash
            // Channel will be deleted on permanent deletion
            
            // Clear deleting flag immediately after deletion completes
            _uiState.update { state ->
                state.copy(
                    snackbarMessage = "\"${habit.title}\" moved to trash",
                    isDeleting = false  // Clear immediately for instant UI response
                )
            }
        }
        
        // Update user's stats on leaderboard in background (non-blocking, separate coroutine)
        viewModelScope.launch(Dispatchers.IO) {
            updateUserStatsAsync()
        }
    }

    fun restoreHabit(habitId: Long) {
        viewModelScope.launch {
            android.util.Log.d("HabitViewModel", "🔄 Restoring habit ID: $habitId")
            
            withContext(Dispatchers.IO) {
                habitRepository.restoreFromTrash(habitId)
            }
            android.util.Log.d("HabitViewModel", "✅ Habit restored in Firestore, now fetching fresh data...")
            
            // Fetch habit AFTER restore to get updated state
            val habit = withContext(Dispatchers.IO) {
                habitRepository.getHabitById(habitId)
            }
            
            android.util.Log.d("HabitViewModel", "📦 Fresh habit data: title='${habit.title}', avatar=${habit.avatar}")
            
            // Reschedule notifications if reminder is enabled
            if (habit.reminderEnabled) {
                reminderScheduler.schedule(habit)
                android.util.Log.d("HabitViewModel", "⏰ Alarm rescheduled for '${habit.title}'")
            } else {
                android.util.Log.d("HabitViewModel", "🔕 Reminder disabled, no alarm scheduled")
            }
            
            // Preload image to ensure fresh cache with updated URL
            scheduleImagePreload()
            
            _uiState.update { state ->
                state.copy(snackbarMessage = "\"${habit.title}\" restored")
            }
        }
        
        // Update user's stats on leaderboard in background (non-blocking, separate coroutine)
        viewModelScope.launch(Dispatchers.IO) {
            updateUserStatsAsync()
        }
    }

    fun permanentlyDeleteHabit(habitId: Long) {
        viewModelScope.launch {
            val habit = withContext(Dispatchers.IO) {
                habitRepository.getHabitById(habitId)
            }
            
            // Cancel any scheduled alarms
            reminderScheduler.cancel(habitId)
            
            withContext(Dispatchers.IO) {
                habitRepository.permanentlyDeleteHabit(habitId)
            }
            
            // Delete the notification channel to clean up system settings
            HabitReminderService.deleteHabitChannel(context, habitId)
            
            _uiState.update { state ->
                state.copy(snackbarMessage = "\"${habit.title}\" permanently deleted")
            }
        }
        
        // Update user's stats on leaderboard in background (non-blocking, separate coroutine)
        viewModelScope.launch(Dispatchers.IO) {
            updateUserStatsAsync()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            // Get the IDs of deleted habits before emptying trash
            val deletedHabitIds = _uiState.value.deletedHabits.map { it.id }
            
            withContext(Dispatchers.IO) {
                habitRepository.emptyTrash()
            }
            
            // Delete all notification channels for the deleted habits
            if (deletedHabitIds.isNotEmpty()) {
                HabitReminderService.deleteMultipleHabitChannels(context, deletedHabitIds)
            }
            
            _uiState.update { state ->
                state.copy(snackbarMessage = "Trash emptied")
            }
        }
        
        // Update user's stats on leaderboard in background (non-blocking, separate coroutine)
        viewModelScope.launch(Dispatchers.IO) {
            updateUserStatsAsync()
        }
    }

    private fun HabitScreenState.habitsValidationError(): String =
        if (habits.isEmpty()) "Name is required" else "Pick a name to remember the habit"

    private fun HabitScreenState.habitSavedMessage(): String =
        "Habit saved"

    private fun HabitScreenState.habitDeletedMessage(title: String): String =
        "Removed \"$title\""

    private fun mapToUi(habit: Habit): HabitCardUi {
        val reminderTime = LocalTime.of(habit.reminderHour, habit.reminderMinute)
        val frequencyText = buildFrequencyText(habit)
        val today = LocalDate.now()
        val isCompleted = habit.lastCompletedDate == today
        
        // Debug logging for completion status
        android.util.Log.d("HabitViewModel", 
            "Habit: ${habit.title}, lastCompletedDate: ${habit.lastCompletedDate}, today: $today, isCompleted: $isCompleted")
        
        // Check if this habit is selected
        val isSelected = _uiState.value.selectedHabitIds.contains(habit.id)
        
        return HabitCardUi(
            id = habit.id,
            title = habit.title,
            description = habit.description,
            reminderTime = reminderTime,
            isReminderEnabled = habit.reminderEnabled,
            isCompletedToday = isCompleted,
            frequency = habit.frequency,
            frequencyText = frequencyText,
            avatar = habit.avatar,
            isSelected = isSelected
        )
    }

    private fun buildFrequencyText(habit: Habit): String {
        return when (habit.frequency) {
            HabitFrequency.DAILY -> "Daily"
            HabitFrequency.WEEKLY -> {
                val dayName = getDayName(habit.dayOfWeek ?: 1)
                "Weekly on $dayName"
            }
            HabitFrequency.MONTHLY -> {
                val day = habit.dayOfMonth ?: 1
                val suffix = when (day % 10) {
                    1 -> if (day == 11) "th" else "st"
                    2 -> if (day == 12) "th" else "nd"
                    3 -> if (day == 13) "th" else "rd"
                    else -> "th"
                }
                "Monthly on the $day$suffix"
            }
            HabitFrequency.YEARLY -> {
                val month = getMonthName(habit.monthOfYear ?: 1)
                val day = habit.dayOfMonth ?: 1
                "Yearly on $month $day"
            }
        }
    }

    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Monday"
        }
    }

    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> "January"
        }
    }

    suspend fun getHabitProgress(habitId: Long): HabitProgress {
        return withContext(Dispatchers.IO) {
            val habit = habitRepository.getHabitById(habitId)
            val completions = habitRepository.getHabitCompletions(habitId)
            val completedDates = completions.map { it.completedDate }.toSet()
            
            // Use habit.streak (calculated by StreakCalculator with grace/freeze)
            val currentStreak = habit.streak
            
            // Calculate longest streak
            val longestStreak = maxOf(habit.highestStreakAchieved, currentStreak)
            
            // Calculate total completions
            val totalCompletions = completions.size
            
            // Calculate success rate
            val daysSinceCreation = ChronoUnit.DAYS.between(
                habit.createdAt.atZone(ZoneOffset.UTC).toLocalDate(), 
                LocalDate.now()
            ).toInt() + 1
            val completionRate = if (daysSinceCreation > 0) {
                totalCompletions.toFloat() / daysSinceCreation
            } else 0f
            
            HabitProgress(
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                totalCompletions = totalCompletions,
                completionRate = completionRate.coerceAtMost(1.0f),
                completedDates = completedDates,
                completions = completions
            )
        }
    }

    // OLD STREAK CALCULATION FUNCTIONS REMOVED
    // Now using habit.streak from StreakCalculator throughout the app

    suspend fun getHabitById(habitId: Long): Habit {
        return withContext(Dispatchers.IO) {
            habitRepository.getHabitById(habitId)
        }
    }
    
    /**
     * Manually trigger user stats update.
     * Call this when viewing profile or when you want to refresh stats.
     */
    fun refreshUserStats() {
        viewModelScope.launch(Dispatchers.IO) {
            updateUserStatsAsync()
        }
    }
    
    // Selection Mode Functions
    fun toggleHabitSelection(habitId: Long) {
        _uiState.update { state ->
            val newSelectedIds = if (habitId in state.selectedHabitIds) {
                state.selectedHabitIds - habitId
            } else {
                state.selectedHabitIds + habitId
            }
            
            // Exit selection mode if no habits are selected
            val isSelectionMode = newSelectedIds.isNotEmpty()
            
            // Update habits with selection state
            val updatedHabits = state.habits.map { habit ->
                habit.copy(isSelected = habit.id in newSelectedIds)
            }
            
            state.copy(
                selectedHabitIds = newSelectedIds,
                isSelectionMode = isSelectionMode,
                habits = updatedHabits
            )
        }
    }
    
    fun startSelectionMode(habitId: Long) {
        _uiState.update { state ->
            val updatedHabits = state.habits.map { habit ->
                habit.copy(isSelected = habit.id == habitId)
            }
            
            state.copy(
                isSelectionMode = true,
                selectedHabitIds = setOf(habitId),
                habits = updatedHabits
            )
        }
    }
    
    fun exitSelectionMode() {
        _uiState.update { state ->
            val updatedHabits = state.habits.map { habit ->
                habit.copy(isSelected = false)
            }
            
            state.copy(
                isSelectionMode = false,
                selectedHabitIds = emptySet(),
                habits = updatedHabits
            )
        }
    }
    
    fun deleteSelectedHabits() {
        viewModelScope.launch {
            // Indicate deletion in progress so UI can show overlay until all deletions finish
            _uiState.update { it.copy(isDeleting = true) }
            val selectedIds = _uiState.value.selectedHabitIds.toList()
            val habitTitles = mutableListOf<String>()
            
            selectedIds.forEach { habitId ->
                val habit = withContext(Dispatchers.IO) {
                    habitRepository.getHabitById(habitId)
                }
                habitTitles.add(habit.title)
                withContext(Dispatchers.IO) {
                    habitRepository.moveToTrash(habitId)
                }
                reminderScheduler.cancel(habit.id)
            }
            
            val message = if (selectedIds.size == 1) {
                "\"${habitTitles.first()}\" moved to trash"
            } else {
                "${selectedIds.size} habits moved to trash"
            }
            
            // Clear deleting flag immediately after deletion completes
            _uiState.update { state ->
                state.copy(
                    snackbarMessage = message,
                    isSelectionMode = false,
                    selectedHabitIds = emptySet(),
                    isDeleting = false  // Clear immediately for instant UI response
                )
            }

            // Update user's stats on leaderboard in background (non-blocking)
            updateUserStatsAsync()
        }
    }
    
    fun loadHabitForEdit(habitId: Long) {
        viewModelScope.launch {
            val habit = withContext(Dispatchers.IO) {
                habitRepository.getHabitById(habitId)
            }
            
            android.util.Log.d("HabitViewModel", "Loading habit for edit: ID=$habitId")
            android.util.Log.d("HabitViewModel", "Habit data: title=${habit.title}, desc=${habit.description}")
            android.util.Log.d("HabitViewModel", "Time: ${habit.reminderHour}:${habit.reminderMinute}, enabled=${habit.reminderEnabled}")
            android.util.Log.d("HabitViewModel", "Frequency: ${habit.frequency}, dayOfWeek=${habit.dayOfWeek}, dayOfMonth=${habit.dayOfMonth}, monthOfYear=${habit.monthOfYear}")
            android.util.Log.d("HabitViewModel", "Avatar: ${habit.avatar}")
            android.util.Log.d("HabitViewModel", "Sound: id=${habit.notificationSoundId}, name=${habit.notificationSoundName}, uri=${habit.notificationSoundUri}")
            
            // Find the notification sound - try multiple matching strategies
            val notificationSound = when {
                // First, try exact ID match
                habit.notificationSoundId == NotificationSound.DEFAULT_ID -> {
                    android.util.Log.d("HabitViewModel", "Using default sound (default ID)")
                    NotificationSound.DEFAULT
                }
                // Try to find by ID in available sounds
                availableSounds.any { it.id == habit.notificationSoundId } -> {
                    val sound = availableSounds.first { it.id == habit.notificationSoundId }
                    android.util.Log.d("HabitViewModel", "Found sound by ID: ${sound.displayName}")
                    sound
                }
                // Try to find by URI (more reliable for system sounds)
                habit.notificationSoundUri.isNotEmpty() && availableSounds.any { it.uri == habit.notificationSoundUri } -> {
                    val sound = availableSounds.first { it.uri == habit.notificationSoundUri }
                    android.util.Log.d("HabitViewModel", "Found sound by URI: ${sound.displayName}")
                    sound
                }
                // Try to find by display name (fallback)
                availableSounds.any { it.displayName == habit.notificationSoundName } -> {
                    val sound = availableSounds.first { it.displayName == habit.notificationSoundName }
                    android.util.Log.d("HabitViewModel", "Found sound by name: ${sound.displayName}")
                    sound
                }
                // If all else fails, recreate the sound object from stored data
                habit.notificationSoundId != NotificationSound.DEFAULT_ID -> {
                    android.util.Log.d("HabitViewModel", "Recreating sound from stored data: ${habit.notificationSoundName}")
                    NotificationSound(
                        id = habit.notificationSoundId,
                        displayName = habit.notificationSoundName,
                        uri = habit.notificationSoundUri
                    )
                }
                // Last resort: use default
                else -> {
                    android.util.Log.d("HabitViewModel", "No match found, using default sound")
                    NotificationSound.DEFAULT
                }
            }
            
            android.util.Log.d("HabitViewModel", "Selected notification sound: ${notificationSound.displayName} (ID: ${notificationSound.id})")
            
            _uiState.update { state ->
                state.copy(
                    addHabitState = AddHabitState(
                        title = habit.title,
                        description = habit.description,
                        hour = habit.reminderHour,
                        minute = habit.reminderMinute,
                        reminderEnabled = habit.reminderEnabled,
                        frequency = habit.frequency,
                        dayOfWeek = habit.dayOfWeek ?: 1,
                        dayOfMonth = habit.dayOfMonth ?: 1,
                        monthOfYear = habit.monthOfYear ?: 1,
                        notificationSound = notificationSound,
                        availableSounds = availableSounds,
                        avatar = habit.avatar,
                        isEditMode = true,
                        editingHabitId = habitId
                    )
                )
            }
            
            android.util.Log.d("HabitViewModel", "Edit state updated successfully")
        }
    }
}
