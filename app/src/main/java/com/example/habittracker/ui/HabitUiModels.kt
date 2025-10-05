package it.atraj.habittracker.ui

import it.atraj.habittracker.data.local.Habit
import it.atraj.habittracker.data.local.HabitAvatar
import it.atraj.habittracker.data.local.HabitFrequency
import it.atraj.habittracker.data.local.NotificationSound
import java.time.LocalTime

data class HabitCardUi(
    val id: Long,
    val title: String,
    val description: String,
    val reminderTime: LocalTime,
    val isReminderEnabled: Boolean,
    val isCompletedToday: Boolean,
    val frequency: HabitFrequency,
    val frequencyText: String,
    val avatar: HabitAvatar
)

data class AddHabitState(
    val title: String = "",
    val description: String = "",
    val hour: Int = 8,
    val minute: Int = 0,
    val reminderEnabled: Boolean = true,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val dayOfWeek: Int = 1, // Monday
    val dayOfMonth: Int = 1,
    val monthOfYear: Int = 1, // January
    val notificationSound: NotificationSound = NotificationSound.DEFAULT,
    val availableSounds: List<NotificationSound> = emptyList(), // List of all available sounds
    val avatar: HabitAvatar = HabitAvatar.DEFAULT,
    val nameError: String? = null,
    val isSaving: Boolean = false
) {
    val time: LocalTime get() = LocalTime.of(hour, minute)
}

data class HabitScreenState(
    val habits: List<HabitCardUi> = emptyList(),
    val deletedHabits: List<Habit> = emptyList(),
    val isLoading: Boolean = false,
    val isAddSheetVisible: Boolean = false,
    val addHabitState: AddHabitState = AddHabitState(),
    val snackbarMessage: String? = null
)
