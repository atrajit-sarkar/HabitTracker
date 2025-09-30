package com.example.habittracker.data

import com.example.habittracker.data.local.Habit
import com.example.habittracker.data.local.HabitDao
import com.example.habittracker.data.local.HabitCompletion
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao
) : HabitRepository {

    override fun observeHabits(): Flow<List<Habit>> = habitDao.observeHabits()

    override fun observeDeletedHabits(): Flow<List<Habit>> = habitDao.observeDeletedHabits()

    override suspend fun getHabitById(id: Long): Habit = habitDao.getHabitById(id)

    override suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabit(habit)

    override suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    override suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    override suspend fun moveToTrash(habitId: Long) {
        habitDao.moveToTrash(habitId, java.time.Instant.now())
    }

    override suspend fun restoreFromTrash(habitId: Long) {
        habitDao.restoreFromTrash(habitId)
    }

    override suspend fun permanentlyDeleteHabit(habitId: Long) {
        val habit = habitDao.getHabitById(habitId)
        habitDao.deleteHabit(habit)
    }

    override suspend fun emptyTrash() {
        habitDao.emptyTrash()
    }

    override suspend fun cleanupOldDeletedHabits() {
        val thirtyDaysAgo = java.time.Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS)
        habitDao.permanentlyDeleteOldHabits(thirtyDaysAgo)
    }

    override suspend fun markCompletedToday(habitId: Long) {
        markCompletedForDate(habitId, LocalDate.now())
    }

    override suspend fun markCompletedForDate(habitId: Long, date: LocalDate) {
        val completion = HabitCompletion(
            habitId = habitId,
            completedDate = date
        )
        habitDao.insertCompletion(completion)
        
        // Update lastCompletedDate if this is the most recent completion
        val existing = habitDao.getHabitById(habitId)
        val newLast = when {
            existing.lastCompletedDate == null -> date
            date.isAfter(existing.lastCompletedDate) -> date
            else -> existing.lastCompletedDate
        }
        if (newLast != existing.lastCompletedDate) {
            habitDao.updateHabit(existing.copy(lastCompletedDate = newLast))
        }
    }

    override suspend fun getHabitCompletions(habitId: Long): List<HabitCompletion> = 
        habitDao.getHabitCompletions(habitId)
}
