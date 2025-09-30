package com.example.habittracker.data

import com.example.habittracker.data.local.Habit
import com.example.habittracker.data.local.HabitCompletion
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    suspend fun getHabitById(id: Long): Habit
    suspend fun insertHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habit: Habit)
    suspend fun markCompletedToday(habitId: Long)
    suspend fun getHabitCompletions(habitId: Long): List<HabitCompletion>
}
