package it.atraj.habittracker.avatar

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import it.atraj.habittracker.auth.AuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages avatar operations including default avatars and custom uploads
 */
@Singleton
class AvatarManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val githubUploader: GitHubAvatarUploader,
    private val firebaseAuth: FirebaseAuth
) {
    
    companion object {
        private const val TAG = "AvatarManager"
        private const val GITHUB_OWNER = "atrajit-sarkar"
        private const val GITHUB_REPO = "HabitTracker"
        private const val GITHUB_BRANCH = "main"
    }
    
    /**
     * Get list of default avatar URLs
     */
    fun getDefaultAvatars(): List<String> {
        return listOf(
            "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/$GITHUB_BRANCH/Avatars/avatar_1_professional.png",
            "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/$GITHUB_BRANCH/Avatars/avatar_2_casual.png",
            "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/$GITHUB_BRANCH/Avatars/avatar_3_creative.png",
            "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/$GITHUB_BRANCH/Avatars/avatar_4_modern.png",
            "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/$GITHUB_BRANCH/Avatars/avatar_5_artistic.png",
            "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/$GITHUB_BRANCH/Avatars/avatar_6_gemini_1.png",
            "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/$GITHUB_BRANCH/Avatars/avatar_7_gemini_2.png",
            "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/$GITHUB_BRANCH/Avatars/avatar_8_gemini_3.png"
        )
    }
    
    /**
     * Upload a custom avatar for the current user
     * 
     * @param imageUri URI of the selected image
     * @param folder Folder to upload to ("profile" for profile avatars, "habits" for habit avatars)
     * @return Result containing the avatar URL or error
     */
    suspend fun uploadCustomAvatar(imageUri: Uri, folder: String = "profile"): AvatarResult {
        return try {
            // Get current user ID from Firebase Auth
            val userId = firebaseAuth.currentUser?.uid
            if (userId == null) {
                return AvatarResult.Error("No user signed in")
            }
            
            // Upload to GitHub
            when (val uploadResult = githubUploader.uploadAvatar(context, userId, imageUri, folder)) {
                is UploadResult.Success -> {
                    // Only update user profile if uploading profile avatar
                    if (folder == "profile") {
                        val updateResult = authRepository.updateCustomAvatar(uploadResult.avatarUrl)
                        if (updateResult is it.atraj.habittracker.auth.AuthResult.Success) {
                            AvatarResult.Success(uploadResult.avatarUrl)
                        } else {
                            AvatarResult.Error("Failed to update user profile")
                        }
                    } else {
                        // For habit avatars, just return success without updating profile
                        AvatarResult.Success(uploadResult.avatarUrl)
                    }
                }
                is UploadResult.Error -> {
                    AvatarResult.Error(uploadResult.message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload custom avatar", e)
            AvatarResult.Error(e.message ?: "Upload failed")
        }
    }
    
    /**
     * Set a default avatar for the current user
     * 
     * @param avatarUrl URL of the default avatar
     * @return Result indicating success or failure
     */
    suspend fun setDefaultAvatar(avatarUrl: String): AvatarResult {
        return try {
            val updateResult = authRepository.updateCustomAvatar(avatarUrl)
            if (updateResult is it.atraj.habittracker.auth.AuthResult.Success) {
                AvatarResult.Success(avatarUrl)
            } else {
                AvatarResult.Error("Failed to update avatar")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set default avatar", e)
            AvatarResult.Error(e.message ?: "Failed to set avatar")
        }
    }
    
    /**
     * Delete the current user's custom uploaded avatar
     * 
     * @param avatarUrl URL of the avatar to delete
     * @return true if deleted successfully
     */
    suspend fun deleteCustomAvatar(avatarUrl: String): Boolean {
        return try {
            // Get current user ID from Firebase Auth
            val userId = firebaseAuth.currentUser?.uid
            if (userId == null) {
                Log.e(TAG, "No user signed in")
                return false
            }
            
            // Only delete if it's a custom uploaded avatar (not a default one)
            if (isCustomUploadedAvatar(avatarUrl)) {
                githubUploader.deleteAvatar(userId, avatarUrl)
            } else {
                Log.w(TAG, "Cannot delete default avatar")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete custom avatar", e)
            false
        }
    }
    
    /**
     * Get all avatars for the current user (both default and custom)
     * 
     * @param folder Folder to get avatars from ("profile" or "habits")
     * @return Combined list of default and custom uploaded avatars
     */
    suspend fun getAllAvatars(folder: String = "profile"): List<AvatarItem> {
        val userId = firebaseAuth.currentUser?.uid
        val defaultAvatars = getDefaultAvatars().map { 
            AvatarItem(it, AvatarType.DEFAULT) 
        }
        
        if (userId != null) {
            val customAvatars = githubUploader.listUserAvatars(userId, folder).map {
                AvatarItem(it, AvatarType.CUSTOM)
            }
            return defaultAvatars + customAvatars
        }
        
        return defaultAvatars
    }
    
    /**
     * Reset avatar to default (Google photo or default avatar)
     */
    suspend fun resetAvatar(): AvatarResult {
        return try {
            val updateResult = authRepository.updateCustomAvatar(null)
            if (updateResult is it.atraj.habittracker.auth.AuthResult.Success) {
                AvatarResult.Success(null)
            } else {
                AvatarResult.Error("Failed to reset avatar")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset avatar", e)
            AvatarResult.Error(e.message ?: "Failed to reset avatar")
        }
    }
    
    /**
     * Check if an avatar URL is a custom uploaded avatar
     */
    private fun isCustomUploadedAvatar(avatarUrl: String): Boolean {
        return avatarUrl.contains("/avatars/profile/") || 
               avatarUrl.contains("/avatars/habits/") || 
               avatarUrl.contains("/avatars/users/")
    }
    
    /**
     * Initialize GitHub uploader with token
     */
    fun initializeGitHubUploader(token: String) {
        githubUploader.initialize(token)
    }
}

/**
 * Result of avatar operations
 */
sealed class AvatarResult {
    data class Success(val avatarUrl: String?) : AvatarResult()
    data class Error(val message: String) : AvatarResult()
}

/**
 * Represents an avatar item
 */
data class AvatarItem(
    val url: String,
    val type: AvatarType
)

/**
 * Type of avatar
 */
enum class AvatarType {
    DEFAULT,  // Pre-existing default avatars
    CUSTOM    // User uploaded avatars
}
