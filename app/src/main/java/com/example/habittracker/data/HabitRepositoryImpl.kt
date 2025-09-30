package com.example.habittracker.data

import com.example.habittracker.data.local.Habit
import com.example.habittracker.data.local.HabitDao
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao
) : HabitRepository {

    override fun observeHabits(): Flow<List<Habit>> = habitDao.observeHabits()

    override suspend fun getHabitById(id: Long): Habit? = habitDao.getHabitById(id)

    override suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabit(habit)

    override suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    override suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    override suspend fun markCompletedToday(habitId: Long) {
        val existing = habitDao.getHabitById(habitId) ?: return
        val updated = existing.copy(lastCompletedDate = LocalDate.now())
        habitDao.updateHabit(updated)
    }
}
