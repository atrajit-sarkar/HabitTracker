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
    val freezeDaysUsedForCurrentGap: Int = 0 // How many freeze days already used for this gap
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
