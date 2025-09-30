package com.example.habittracker.data

import com.example.habittracker.data.local.Habit
import com.example.habittracker.data.local.HabitCompletion
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeDeletedHabits(): Flow<List<Habit>>
    suspend fun getHabitById(id: Long): Habit
    suspend fun insertHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habit: Habit)
    suspend fun moveToTrash(habitId: Long)
    suspend fun restoreFromTrash(habitId: Long)
    suspend fun permanentlyDeleteHabit(habitId: Long)
    suspend fun emptyTrash()
    suspend fun cleanupOldDeletedHabits()
    suspend fun markCompletedToday(habitId: Long)
    suspend fun markCompletedForDate(habitId: Long, date: java.time.LocalDate)
    suspend fun getHabitCompletions(habitId: Long): List<HabitCompletion>
}
