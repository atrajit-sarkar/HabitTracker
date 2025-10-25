package it.atraj.habittracker.social.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.atraj.habittracker.social.data.repository.PostsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SocialModule {
    
    @Provides
    @Singleton
    fun providePostsRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): PostsRepository {
        return PostsRepository(firestore, auth)
    }
}
