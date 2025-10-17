package it.atraj.habittracker.image

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import it.atraj.habittracker.data.HabitRepository
import it.atraj.habittracker.data.local.HabitAvatarType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker to preload habit custom images into cache.
 * Runs in background, doesn't block UI, and ensures notifications have cached images.
 */
@HiltWorker
class HabitImagePreloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val imageLoader: ImageLoader
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting habit image preload...")
            
            // Get all active habits with custom images
            val habits = habitRepository.getAllHabits()
            val customImageHabits = habits.filter { 
                !it.isDeleted && 
                it.avatar.type == HabitAvatarType.CUSTOM_IMAGE &&
                it.avatar.value.startsWith("http")
            }
            
            if (customImageHabits.isEmpty()) {
                Log.d(TAG, "No custom images to preload")
                return@withContext Result.success()
            }
            
            Log.d(TAG, "Preloading ${customImageHabits.size} custom images...")
            
            var successCount = 0
            var failCount = 0
            
            // Preload each image with stable cache key
            customImageHabits.forEach { habit ->
                try {
                    val cacheKey = "habit_avatar_${habit.id}"
                    val token = it.atraj.habittracker.avatar.SecureTokenStorage.getToken(applicationContext)
                    
                    val requestBuilder = ImageRequest.Builder(applicationContext)
                        .data(habit.avatar.value)
                        .size(128) // Small size for notifications
                        .memoryCacheKey(cacheKey)
                        .diskCacheKey(cacheKey)
                        .diskCachePolicy(CachePolicy.ENABLED) // Allow disk cache write
                        .memoryCachePolicy(CachePolicy.ENABLED) // Allow memory cache write
                        .networkCachePolicy(CachePolicy.ENABLED) // Allow network fetch
                    
                    // Add GitHub auth token if needed
                    if (token != null && habit.avatar.value.contains("githubusercontent.com")) {
                        requestBuilder.addHeader("Authorization", "token $token")
                    }
                    
                    // Execute request to cache the image
                    val result = imageLoader.execute(requestBuilder.build())
                    
                    if (result.drawable != null) {
                        successCount++
                        Log.d(TAG, "✅ Cached image for habit: ${habit.title}")
                    } else {
                        failCount++
                        Log.w(TAG, "⚠️ No drawable returned for habit: ${habit.title}")
                    }
                } catch (e: Exception) {
                    failCount++
                    Log.e(TAG, "❌ Failed to preload image for habit ${habit.title}: ${e.message}")
                }
            }
            
            Log.d(TAG, "Image preload complete: $successCount succeeded, $failCount failed")
            
            // Return success even if some images failed (best effort)
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during image preload: ${e.message}", e)
            Result.retry() // Retry on general failure
        }
    }

    companion object {
        private const val TAG = "HabitImagePreload"
        const val WORK_NAME = "habit_image_preload"
    }
}
