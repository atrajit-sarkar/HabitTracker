package com.example.habittracker.di

import android.content.Context
import androidx.room.Room
import com.example.habittracker.data.HabitRepository
import com.example.habittracker.data.HabitRepositoryImpl
import com.example.habittracker.data.local.HabitDao
import com.example.habittracker.data.local.HabitDatabase
import com.example.habittracker.notification.HabitReminderScheduler
import com.example.habittracker.notification.HabitReminderSchedulerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HabitDatabase =
        Room.databaseBuilder(context, HabitDatabase::class.java, HabitDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideHabitDao(db: HabitDatabase): HabitDao = db.habitDao()

    @Provides
    @Singleton
    fun provideHabitReminderScheduler(
        @ApplicationContext context: Context
    ): HabitReminderScheduler = HabitReminderSchedulerImpl(context)
}
