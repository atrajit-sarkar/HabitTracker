package it.atraj.habittracker.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to upload custom avatars to GitHub repository
 * 
 * Repository Structure:
 * - avatars/
 *   - default/ (pre-existing avatars)
 *   - users/
 *     - {userId}/
 *       - avatar_{timestamp}.png
 * 
 * Requires GitHub Personal Access Token with repo permissions
 */
@Singleton
class GitHubAvatarUploader @Inject constructor() {
    
    companion object {
        private const val TAG = "GitHubAvatarUploader"
        
        // GitHub Repository Configuration
        private const val GITHUB_OWNER = "gongobongofounder"
        private const val GITHUB_REPO = "habit-tracker-avatar-repo"
        private const val GITHUB_API_BASE = "https://api.github.com"
        private const val GITHUB_RAW_BASE = "https://raw.githubusercontent.com"
        
        // Maximum image size (1MB)
        private const val MAX_IMAGE_SIZE = 1024 * 1024
        
        // Image compression quality
        private const val COMPRESSION_QUALITY = 85
        
        // Max image dimensions
        private const val MAX_DIMENSION = 512
    }
    
    // This should be stored securely - see documentation for setup
    private var githubToken: String? = null
    
    /**
     * Initialize the uploader with GitHub token
     * Token should have 'repo' permissions
     */
    fun initialize(token: String) {
        githubToken = token
    }
    
    /**
     * Upload a custom avatar image for a user
     * 
     * @param context Android context
     * @param userId Unique user ID
     * @param imageUri URI of the image to upload
     * @return URL of the uploaded avatar or null on failure
     */
    suspend fun uploadAvatar(
        context: Context,
        userId: String,
        imageUri: Uri
    ): UploadResult = withContext(Dispatchers.IO) {
        try {
            if (githubToken == null) {
                return@withContext UploadResult.Error("GitHub token not initialized")
            }
            
            // Step 1: Load and compress image
            val bitmap = loadAndCompressBitmap(context, imageUri)
                ?: return@withContext UploadResult.Error("Failed to load image")
            
            // Step 2: Convert to Base64
            val base64Image = bitmapToBase64(bitmap)
            
            // Step 3: Generate unique filename
            val timestamp = System.currentTimeMillis()
            val filename = "avatar_$timestamp.png"
            val path = "avatars/users/$userId/$filename"
            
            // Step 4: Upload to GitHub
            val uploadUrl = uploadToGitHub(path, base64Image, "Upload custom avatar")
                ?: return@withContext UploadResult.Error("Failed to upload to GitHub")
            
            Log.d(TAG, "Avatar uploaded successfully: $uploadUrl")
            UploadResult.Success(uploadUrl)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload avatar", e)
            UploadResult.Error(e.message ?: "Upload failed")
        }
    }
    
    /**
     * Delete a user's avatar from GitHub
     * 
     * @param userId User ID
     * @param avatarUrl Full URL of the avatar to delete
     * @return true if deleted successfully
     */
    suspend fun deleteAvatar(userId: String, avatarUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (githubToken == null) {
                Log.e(TAG, "GitHub token not initialized")
                return@withContext false
            }
            
            // Extract path from URL
            val path = extractPathFromUrl(avatarUrl) ?: return@withContext false
            
            // Get file SHA (required for deletion)
            val sha = getFileSha(path) ?: return@withContext false
            
            // Delete from GitHub
            deleteFromGitHub(path, sha, "Delete custom avatar")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete avatar", e)
            false
        }
    }
    
    /**
     * List all avatars for a user
     */
    suspend fun listUserAvatars(userId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            if (githubToken == null) {
                Log.e(TAG, "GitHub token not initialized")
                return@withContext emptyList()
            }
            
            val path = "avatars/users/$userId"
            val url = URL("$GITHUB_API_BASE/repos/$GITHUB_OWNER/$GITHUB_REPO/contents/$path")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "token $githubToken")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = org.json.JSONArray(response)
                
                val avatars = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    // For private repos, use download_url (which works with proper authentication)
                    val downloadUrl = item.optString("download_url")
                    if (downloadUrl.isNotEmpty()) {
                        // Return the download URL directly
                        avatars.add(downloadUrl)
                    }
                }
                avatars
            } else if (connection.responseCode == 404) {
                // Directory doesn't exist yet (no avatars uploaded)
                Log.d(TAG, "No avatars directory found for user $userId")
                emptyList()
            } else {
                Log.w(TAG, "Failed to list avatars: ${connection.responseCode}")
                emptyList()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list user avatars", e)
            emptyList()
        }
    }
    
    // Private helper methods
    
    private fun loadAndCompressBitmap(context: Context, imageUri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            // Resize if too large
            if (bitmap.width > MAX_DIMENSION || bitmap.height > MAX_DIMENSION) {
                val scale = MAX_DIMENSION.toFloat() / maxOf(bitmap.width, bitmap.height)
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap", e)
            null
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    
    private fun uploadToGitHub(path: String, content: String, message: String): String? {
        return try {
            val url = URL("$GITHUB_API_BASE/repos/$GITHUB_OWNER/$GITHUB_REPO/contents/$path")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Authorization", "token $githubToken")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.doOutput = true
            
            val jsonBody = JSONObject().apply {
                put("message", message)
                put("content", content)
            }
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            if (connection.responseCode == 201) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                val content = jsonResponse.getJSONObject("content")
                val downloadUrl = content.getString("download_url")
                
                Log.d(TAG, "Upload successful, download URL: $downloadUrl")
                downloadUrl
            } else {
                Log.e(TAG, "Upload failed with code: ${connection.responseCode}")
                Log.e(TAG, "Response: ${connection.errorStream?.bufferedReader()?.readText()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload to GitHub", e)
            null
        }
    }
    
    private fun getFileSha(path: String): String? {
        return try {
            val url = URL("$GITHUB_API_BASE/repos/$GITHUB_OWNER/$GITHUB_REPO/contents/$path")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "token $githubToken")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                json.getString("sha")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file SHA", e)
            null
        }
    }
    
    private fun deleteFromGitHub(path: String, sha: String, message: String): Boolean {
        return try {
            val url = URL("$GITHUB_API_BASE/repos/$GITHUB_OWNER/$GITHUB_REPO/contents/$path")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("Authorization", "token $githubToken")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.doOutput = true
            
            val jsonBody = JSONObject().apply {
                put("message", message)
                put("sha", sha)
            }
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
            }
            
            connection.responseCode == 200
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete from GitHub", e)
            false
        }
    }
    
    private fun extractPathFromUrl(url: String): String? {
        return try {
            // Extract path from: https://raw.githubusercontent.com/{owner}/{repo}/{branch}/{path}
            val parts = url.split("/").drop(6) // Drop protocol, domain, owner, repo, branch
            parts.joinToString("/")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract path from URL", e)
            null
        }
    }
}

/**
 * Result of avatar upload operation
 */
sealed class UploadResult {
    data class Success(val avatarUrl: String) : UploadResult()
    data class Error(val message: String) : UploadResult()
}
