package com.example.habittracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isDeleted = 0 ORDER BY reminderHour, reminderMinute")
    fun observeHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeDeletedHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: Long): Habit

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("UPDATE habits SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :habitId")
    suspend fun moveToTrash(habitId: Long, deletedAt: java.time.Instant)

    @Query("UPDATE habits SET isDeleted = 0, deletedAt = NULL WHERE id = :habitId")
    suspend fun restoreFromTrash(habitId: Long)

    @Query("DELETE FROM habits WHERE isDeleted = 1 AND deletedAt < :cutoffTime")
    suspend fun permanentlyDeleteOldHabits(cutoffTime: java.time.Instant)

    @Query("DELETE FROM habits WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion)

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completedDate DESC")
    suspend fun getHabitCompletions(habitId: Long): List<HabitCompletion>

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND completedDate = :date")
    suspend fun removeCompletion(habitId: Long, date: java.time.LocalDate)
}
