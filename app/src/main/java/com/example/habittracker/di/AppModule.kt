package com.example.habittracker.di

import android.content.Context
import coil.ImageLoader
import com.example.habittracker.data.HabitRepository
import com.example.habittracker.data.firestore.FirestoreHabitRepository
import com.example.habittracker.image.OptimizedImageLoader
import com.example.habittracker.notification.HabitReminderScheduler
import com.example.habittracker.notification.HabitReminderSchedulerImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    abstract fun bindHabitRepository(impl: FirestoreHabitRepository): HabitRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance().apply {
            // Enable offline persistence for offline-first experience
            firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(100 * 1024 * 1024) // 100MB cache
                .build()
        }
    }

    @Provides
    @Singleton
    fun provideImageLoader(optimizedImageLoader: OptimizedImageLoader): ImageLoader {
        return optimizedImageLoader.imageLoader
    }

    @Provides
    @Singleton
    fun provideHabitReminderScheduler(
        @ApplicationContext context: Context
    ): HabitReminderScheduler = HabitReminderSchedulerImpl(context)
}
