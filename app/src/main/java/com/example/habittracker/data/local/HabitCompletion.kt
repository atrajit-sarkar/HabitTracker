package com.example.habittracker.data.local

import java.time.LocalDate

data class HabitCompletion(
    val habitId: Long,
    val completedDate: LocalDate,
    val completedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)