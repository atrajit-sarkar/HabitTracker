package it.atraj.habittracker.avatar.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import it.atraj.habittracker.auth.AuthRepository
import it.atraj.habittracker.avatar.AvatarManager
import it.atraj.habittracker.avatar.GitHubAvatarUploader
import javax.inject.Singleton

/**
 * Dagger Hilt module for avatar-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AvatarModule {
    
    @Provides
    @Singleton
    fun provideGitHubAvatarUploader(): GitHubAvatarUploader {
        return GitHubAvatarUploader()
    }
    
    @Provides
    @Singleton
    fun provideAvatarManager(
        @ApplicationContext context: Context,
        authRepository: AuthRepository,
        githubUploader: GitHubAvatarUploader,
        firebaseAuth: FirebaseAuth
    ): AvatarManager {
        return AvatarManager(context, authRepository, githubUploader, firebaseAuth)
    }
}
