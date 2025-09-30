package com.example.habittracker.data.local

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
    val notificationSound: NotificationSound = NotificationSound.DEFAULT,
    val avatar: HabitAvatar = HabitAvatar.DEFAULT,
    val lastCompletedDate: LocalDate? = null,
    val createdAt: Instant = Instant.now(),
    val isDeleted: Boolean = false,
    val deletedAt: Instant? = null
)
