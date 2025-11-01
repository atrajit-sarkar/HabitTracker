package it.atraj.habittracker.data.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

@Serializable
data class FirestoreHabit(
    val id: String = "",
    // Stable numeric ID derived from id.hashCode() at creation time and stored for lookup
    val numericId: Long? = null,
    val title: String = "",
    val description: String = "",
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val reminderEnabled: Boolean = true,
    val frequency: String = "DAILY",
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val monthOfYear: Int? = null,
    val notificationSound: String = "DEFAULT", // Legacy: Keep for backward compatibility
    val notificationSoundId: String = "default", // New: Sound ID
    val notificationSoundName: String = "Default Notification", // New: Sound display name
    val notificationSoundUri: String = "", // New: Sound URI
    val avatar: FirestoreHabitAvatar = FirestoreHabitAvatar(),
    val lastCompletedDate: Long? = null, // epoch day
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    // Streak and rewards fields
    val streak: Int = 0,
    val highestStreakAchieved: Int = 0,
    val lastStreakUpdate: Long? = null, // epoch day
    // Gap tracking to prevent re-deducting freeze days
    val currentGapStartDate: Long? = null, // epoch day
    val freezeDaysUsedForCurrentGap: Int = 0,
    // Track all dates where freeze was applied (for fast calendar UI)
    val freezeAppliedDates: List<Long> = emptyList(), // epoch days
    // Bad habit fields
    val isBadHabit: Boolean = false,
    val targetAppPackageName: String? = null,
    val targetAppName: String? = null,
    val lastAppUsageCheckDate: Long? = null, // epoch day
    val totalCompletions: Int = 0 // Total completions count (optimized for bad habits)
)

@Serializable
data class FirestoreHabitAvatar(
    val type: String = "DEFAULT_ICON",
    val value: String = "🎯",
    val backgroundColor: String = "#6650A4"
)

@Serializable
data class FirestoreHabitCompletion(
    val id: String = "",
    val habitId: String = "",
    val completedDate: Long = 0, // epoch day
    val completedAt: Long = System.currentTimeMillis()
)

// Extension functions to convert between Firestore and local models
fun DocumentSnapshot.toFirestoreHabit(): FirestoreHabit? {
    return try {
        val data = data ?: return null
        FirestoreHabit(
            id = id,
            numericId = (data["numericId"] as? Long),
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            reminderHour = (data["reminderHour"] as? Long)?.toInt() ?: 8,
            reminderMinute = (data["reminderMinute"] as? Long)?.toInt() ?: 0,
            reminderEnabled = data["reminderEnabled"] as? Boolean ?: true,
            frequency = data["frequency"] as? String ?: "DAILY",
            dayOfWeek = (data["dayOfWeek"] as? Long)?.toInt(),
            dayOfMonth = (data["dayOfMonth"] as? Long)?.toInt(),
            monthOfYear = (data["monthOfYear"] as? Long)?.toInt(),
            notificationSound = data["notificationSound"] as? String ?: "DEFAULT", // Legacy field
            notificationSoundId = data["notificationSoundId"] as? String ?: "default",
            notificationSoundName = data["notificationSoundName"] as? String ?: "Default Notification",
            notificationSoundUri = data["notificationSoundUri"] as? String ?: "",
            avatar = (data["avatar"] as? Map<String, Any>)?.let { avatarMap ->
                FirestoreHabitAvatar(
                    type = avatarMap["type"] as? String ?: "DEFAULT_ICON",
                    value = avatarMap["value"] as? String ?: "🎯",
                    backgroundColor = avatarMap["backgroundColor"] as? String ?: "#6650A4"
                )
            } ?: FirestoreHabitAvatar(),
            lastCompletedDate = data["lastCompletedDate"] as? Long,
            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
            isDeleted = data["isDeleted"] as? Boolean ?: false,
            deletedAt = data["deletedAt"] as? Long,
            streak = (data["streak"] as? Long)?.toInt() ?: 0,
            highestStreakAchieved = (data["highestStreakAchieved"] as? Long)?.toInt() ?: 0,
            lastStreakUpdate = data["lastStreakUpdate"] as? Long,
            currentGapStartDate = data["currentGapStartDate"] as? Long,
            freezeDaysUsedForCurrentGap = (data["freezeDaysUsedForCurrentGap"] as? Long)?.toInt() ?: 0,
            freezeAppliedDates = (data["freezeAppliedDates"] as? List<*>)?.mapNotNull { it as? Long } ?: emptyList(),
            // Support both "isBadHabit" and "badHabit" field names for backward compatibility
            isBadHabit = (data["isBadHabit"] as? Boolean) ?: (data["badHabit"] as? Boolean) ?: false,
            targetAppPackageName = data["targetAppPackageName"] as? String,
            targetAppName = data["targetAppName"] as? String,
            lastAppUsageCheckDate = data["lastAppUsageCheckDate"] as? Long,
            totalCompletions = (data["totalCompletions"] as? Long)?.toInt() ?: 0
        )
    } catch (e: Exception) {
        null
    }
}

fun QuerySnapshot.toFirestoreHabits(): List<FirestoreHabit> {
    return documents.mapNotNull { it.toFirestoreHabit() }
}

fun DocumentSnapshot.toFirestoreHabitCompletion(): FirestoreHabitCompletion? {
    return try {
        val data = data ?: return null
        FirestoreHabitCompletion(
            id = id,
            habitId = data["habitId"] as? String ?: "",
            completedDate = data["completedDate"] as? Long ?: 0,
            completedAt = data["completedAt"] as? Long ?: System.currentTimeMillis()
        )
    } catch (e: Exception) {
        null
    }
}

fun QuerySnapshot.toFirestoreHabitCompletions(): List<FirestoreHabitCompletion> {
    return documents.mapNotNull { it.toFirestoreHabitCompletion() }
}
