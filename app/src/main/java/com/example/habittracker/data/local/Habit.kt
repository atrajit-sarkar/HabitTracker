package it.atraj.habittracker.data.local

import java.time.Instant
import java.time.LocalDate

data class Habit(
    val id: Long = 0L,
    val title: String,
    val description: String,
    val reminderHour: Int,
    val reminderMinute: Int,
    val reminderEnabled: Boolean,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val dayOfWeek: Int? = null, // 1-7 for weekly habits (1 = Monday)
    val dayOfMonth: Int? = null, // 1-31 for monthly habits
    val monthOfYear: Int? = null, // 1-12 for yearly habits
    val notificationSoundId: String = NotificationSound.DEFAULT_ID, // Store sound ID instead of enum
    val notificationSoundName: String = "Default Notification", // Store display name
    val notificationSoundUri: String = "", // Store URI string
    val avatar: HabitAvatar = HabitAvatar.DEFAULT,
    val lastCompletedDate: LocalDate? = null,
    val createdAt: Instant = Instant.now(),
    val isDeleted: Boolean = false,
    val deletedAt: Instant? = null,
    // Streak and rewards fields
    val streak: Int = 0,
    val highestStreakAchieved: Int = 0,
    val lastStreakUpdate: LocalDate? = null, // Track when streak was last calculated
    // Track current gap to prevent re-deducting freeze days for same gap
    val currentGapStartDate: LocalDate? = null, // When current gap started (first missed day)
    val freezeDaysUsedForCurrentGap: Int = 0, // How many freeze days already used for this gap
    // Track ALL dates where freeze was applied (for calendar UI)
    val freezeAppliedDates: Set<LocalDate> = emptySet(), // All dates that had freeze protection applied
    // Bad habit fields
    val isBadHabit: Boolean = false, // Flag to distinguish bad habits from regular habits
    val targetAppPackageName: String? = null, // Package name of the app to track (e.g., "com.facebook.katana")
    val targetAppName: String? = null, // Display name of the app being tracked
    val lastAppUsageCheckDate: LocalDate? = null, // Last date when app usage was checked
    val totalCompletions: Int = 0 // Total number of completions (for bad habits - days app was avoided)
) {
    // Helper function to get NotificationSound object
    fun getNotificationSound(): NotificationSound {
        return NotificationSound(
            id = notificationSoundId,
            displayName = notificationSoundName,
            uri = notificationSoundUri
        )
    }
}
