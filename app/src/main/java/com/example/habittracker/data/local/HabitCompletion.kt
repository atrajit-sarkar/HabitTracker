package com.example.habittracker.data.local

import androidx.room.*
import java.time.LocalDate

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "completedDate"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId"), Index("completedDate")]
)
data class HabitCompletion(
    val habitId: Long,
    val completedDate: LocalDate,
    val completedAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)