package com.example.habittracker.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.auth.AuthRepository
import com.example.habittracker.auth.User
import com.example.habittracker.data.HabitRepository
import com.example.habittracker.data.local.Habit
import com.example.habittracker.data.local.HabitAvatar
import com.example.habittracker.data.local.HabitFrequency
import com.example.habittracker.data.local.NotificationSound
import com.example.habittracker.notification.HabitReminderScheduler
import com.example.habittracker.notification.HabitReminderService
import com.example.habittracker.ui.social.ProfileStatsUpdater
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
    private val profileStatsUpdater: ProfileStatsUpdater
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitScreenState(isLoading = true))
    val uiState: StateFlow<HabitScreenState> = _uiState.asStateFlow()
    
    // Cache available sounds
    private var availableSounds: List<NotificationSound> = emptyList()
    
    // Track current user for stats updates
    private var currentUser: User? = null

    init {
        // Observe current user for stats updates
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                currentUser = user
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
        
        viewModelScope.launch {
            habitRepository.observeHabits().collectLatest { habits ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        habits = habits.map(::mapToUi).sortedBy { it.reminderTime }
                    )
                }
                
                // Sync notification channels after habits are loaded
                // This ensures all active habits have their channels in the system
                if (habits.isNotEmpty()) {
                    HabitReminderService.syncAllHabitChannels(context, habits)
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

    fun saveHabit() {
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
        viewModelScope.launch {
            _uiState.update { it.copy(addHabitState = it.addHabitState.copy(isSaving = true)) }
            val addForm = _uiState.value.addHabitState
            
            // Log the notification sound being saved
            android.util.Log.d("HabitViewModel", "Saving habit with notification sound: ${addForm.notificationSound.displayName} (ID: ${addForm.notificationSound.id}, URI: ${addForm.notificationSound.uri})")
            
            val habit = Habit(
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
            
            android.util.Log.d("HabitViewModel", "Habit object created: soundId=${habit.notificationSoundId}, soundName=${habit.notificationSoundName}, soundUri=${habit.notificationSoundUri}")
            
            val savedHabit = withContext(Dispatchers.IO) {
                val id = habitRepository.insertHabit(habit)
                habit.copy(id = id)
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
                    snackbarMessage = state.habitSavedMessage(),
                    addHabitState = AddHabitState(),
                    isAddSheetVisible = false
                )
            }
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
            habitRepository.markCompletedToday(habitId)
            // Update stats after completion
            updateUserStatsAsync()
        }
    }

    fun markHabitCompletedForDate(habitId: Long, date: java.time.LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            habitRepository.markCompletedForDate(habitId, date)
            // Update stats after completion
            updateUserStatsAsync()
        }
    }
    
    /**
     * Update user's public profile stats based on current habits
     */
    private suspend fun updateUserStatsAsync() {
        val user = currentUser ?: return
        // Get current habits from the UI state
        val habits = _uiState.value.habits.map { ui ->
            // Convert UiHabit back to Habit for stats calculation
            // This is safe because ProfileStatsUpdater only needs basic habit info
            getHabitById(ui.id)
        }
        profileStatsUpdater.updateUserStats(user, habits)
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            val habit = withContext(Dispatchers.IO) {
                habitRepository.getHabitById(habitId)
            }
            withContext(Dispatchers.IO) {
                habitRepository.moveToTrash(habitId)
            }
            reminderScheduler.cancel(habit.id)
            
            // Note: We don't delete the channel yet because user might restore from trash
            // Channel will be deleted on permanent deletion
            
            _uiState.update { state ->
                state.copy(snackbarMessage = "\"${habit.title}\" moved to trash")
            }
        }
    }

    fun restoreHabit(habitId: Long) {
        viewModelScope.launch {
            val habit = withContext(Dispatchers.IO) {
                habitRepository.getHabitById(habitId)
            }
            withContext(Dispatchers.IO) {
                habitRepository.restoreFromTrash(habitId)
            }
            // Reschedule notifications if reminder is enabled
            if (habit.reminderEnabled) {
                reminderScheduler.schedule(habit)
            }
            _uiState.update { state ->
                state.copy(snackbarMessage = "\"${habit.title}\" restored")
            }
        }
    }

    fun permanentlyDeleteHabit(habitId: Long) {
        viewModelScope.launch {
            val habit = withContext(Dispatchers.IO) {
                habitRepository.getHabitById(habitId)
            }
            withContext(Dispatchers.IO) {
                habitRepository.permanentlyDeleteHabit(habitId)
            }
            
            // Delete the notification channel to clean up system settings
            HabitReminderService.deleteHabitChannel(context, habitId)
            
            _uiState.update { state ->
                state.copy(snackbarMessage = "\"${habit.title}\" permanently deleted")
            }
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
        return HabitCardUi(
            id = habit.id,
            title = habit.title,
            description = habit.description,
            reminderTime = reminderTime,
            isReminderEnabled = habit.reminderEnabled,
            isCompletedToday = habit.lastCompletedDate == LocalDate.now(),
            frequency = habit.frequency,
            frequencyText = frequencyText,
            avatar = habit.avatar
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
            
            // Calculate current streak
            val currentStreak = calculateCurrentStreak(completedDates)
            
            // Calculate longest streak
            val longestStreak = calculateLongestStreak(completedDates)
            
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
                completedDates = completedDates
            )
        }
    }

    private fun calculateCurrentStreak(completedDates: Set<LocalDate>): Int {
        if (completedDates.isEmpty()) return 0

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        
        // Check if there's a recent completion (today or yesterday)
        val hasRecentCompletion = today in completedDates || yesterday in completedDates
        
        if (!hasRecentCompletion) {
            // No recent completion - apply penalty system
            // Find the most recent completion
            val mostRecent = completedDates.max()
            val daysSinceLastCompletion = ChronoUnit.DAYS.between(mostRecent, today).toInt()
            
            // Calculate what the streak would have been
            var consecutiveStreak = 1
            var cursor = mostRecent.minusDays(1)
            while (cursor in completedDates) {
                consecutiveStreak++
                cursor = cursor.minusDays(1)
            }
            
            // Apply penalty: -1 for each missed day, but don't go below 0
            val penalty = daysSinceLastCompletion - 1 // -1 because first day after is acceptable
            return maxOf(0, consecutiveStreak - penalty)
        }
        
        // Has recent completion - calculate normal streak from most recent date
        val startDate = if (today in completedDates) today else yesterday
        var streak = 1
        var cursor = startDate.minusDays(1)
        while (cursor in completedDates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun calculateLongestStreak(completedDates: Set<LocalDate>): Int {
        if (completedDates.isEmpty()) return 0
        
        val sortedDates = completedDates.sorted()
        var longestStreak = 1
        var currentStreak = 1
        
        for (i in 1 until sortedDates.size) {
            val currentDate = sortedDates[i]
            val previousDate = sortedDates[i - 1]
            
            if (ChronoUnit.DAYS.between(previousDate, currentDate) == 1L) {
                currentStreak++
                longestStreak = maxOf(longestStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        
        return longestStreak
    }

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
}
