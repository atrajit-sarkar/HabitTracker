package it.atraj.habittracker.image

import android.content.Context
import coil.ImageLoader
import coil.request.ImageRequest
import it.atraj.habittracker.data.local.HabitAvatar
import it.atraj.habittracker.data.local.HabitAvatarType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Preloads avatar images in the background for smooth scrolling
 */
@Singleton
class AvatarImagePreloader @Inject constructor(
    private val imageLoader: ImageLoader,
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Preload avatar images in background
     */
    fun preloadAvatars(avatars: List<HabitAvatar>) {
        scope.launch {
            avatars.forEach { avatar ->
                if (avatar.type == HabitAvatarType.CUSTOM_IMAGE && avatar.value.startsWith("http")) {
                    preloadImage(avatar.value)
                }
            }
        }
    }
    
    private suspend fun preloadImage(url: String) {
        withContext(Dispatchers.IO) {
            try {
                val token = it.atraj.habittracker.avatar.SecureTokenStorage.getToken(context)
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(100) // Same size as in the UI
                    .memoryCacheKey("avatar_${url.hashCode()}")
                    .diskCacheKey("avatar_${url.hashCode()}")
                    .apply {
                        if (token != null && url.contains("githubusercontent.com")) {
                            addHeader("Authorization", "token $token")
                        }
                    }
                    .build()
                
                // Execute to cache the image
                imageLoader.execute(request)
            } catch (e: Exception) {
                // Silently fail - image will load on demand
            }
        }
    }
}

