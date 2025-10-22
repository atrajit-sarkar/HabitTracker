package it.atraj.habittracker.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import it.atraj.habittracker.BuildConfig
import it.atraj.habittracker.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for interacting with GitHub API to manage music repository
 */
@Singleton
class GitHubMusicService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GitHubMusicService"
        private const val GITHUB_API_BASE = "https://api.github.com"
        private const val GITHUB_RAW_BASE = "https://raw.githubusercontent.com"
        private const val REPO_OWNER = "gongobongofounder"
        private const val REPO_NAME = "HabitTracker-Music"
        private const val BRANCH = "main"
        
        // Paths in the repository
        private const val OFFICIAL_SONGS_PATH = "music/official"
        private const val USER_SONGS_PATH = "music/users"
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }
    
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    
    /**
     * Get GitHub token from BuildConfig
     */
    private fun getGitHubToken(): String {
        return try {
            BuildConfig.GITHUB_TOKEN_MUSIC_REPO
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get GitHub token from BuildConfig", e)
            ""
        }
    }
    
    /**
     * List official song categories
     */
    suspend fun listOfficialCategories(): Result<List<MusicCategory>> = withContext(Dispatchers.IO) {
        try {
            // Fetch music.json from the repository
            val musicResponse = fetchMusicJson()
            
            // Filter official songs (those without uploadedBy field)
            val officialSongs = musicResponse.music.filter { it.isOfficial }
            
            // Group by category and count songs
            val categories = officialSongs
                .groupBy { it.category.replaceFirstChar { char -> char.uppercase() } }
                .map { (categoryName, songs) ->
                    MusicCategory(
                        name = categoryName,
                        path = "music/official/$categoryName",
                        songCount = songs.size
                    )
                }
                .sortedBy { it.name }
            
            Result.success(categories)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing official categories", e)
            Result.failure(e)
        }
    }
    
    /**
     * List user folders
     */
    suspend fun listUserFolders(currentUserId: String): Result<List<UserFolder>> = withContext(Dispatchers.IO) {
        try {
            // Fetch music.json from the repository
            val musicResponse = fetchMusicJson()
            
            // Filter user-uploaded songs
            val userSongs = musicResponse.music.filter { it.isUserUploaded }
            
            // Group by uploader
            val folders = userSongs
                .groupBy { it.uploadedBy ?: "unknown" }
                .map { (userId, songs) ->
                    val uploaderName = songs.firstOrNull()?.uploaderName ?: userId
                    val displayName = if (userId == currentUserId) "You" else uploaderName
                    
                    // Group user's songs by category
                    val categories = songs
                        .groupBy { it.category.replaceFirstChar { char -> char.uppercase() } }
                        .map { (categoryName, categorySongs) ->
                            MusicCategory(
                                name = categoryName,
                                path = "music/users/$userId/$categoryName",
                                songCount = categorySongs.size
                            )
                        }
                        .sortedBy { it.name }
                    
                    UserFolder(
                        username = userId,
                        displayName = displayName,
                        path = "music/users/$userId",
                        categories = categories
                    )
                }
                .sortedByDescending { it.displayName == "You" } // Current user first
            
            Result.success(folders)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing user folders", e)
            Result.failure(e)
        }
    }
    
    /**
     * List songs in a category
     */
    suspend fun listSongsInCategory(categoryPath: String): Result<List<MusicMetadata>> = withContext(Dispatchers.IO) {
        try {
            // Fetch music.json from the repository
            val musicResponse = fetchMusicJson()
            
            // Determine if this is official or user-uploaded based on path
            val isOfficial = categoryPath.contains("official")
            val categoryName = categoryPath.substringAfterLast("/").lowercase()
            
            val songs = if (isOfficial) {
                // Filter official songs by category
                musicResponse.music.filter { 
                    it.isOfficial && it.category.lowercase() == categoryName 
                }
            } else {
                // Extract userId from path: music/users/{userId}/{category}
                val pathParts = categoryPath.split("/")
                val userId = if (pathParts.size >= 3) pathParts[2] else null
                
                // Filter user songs by userId and category
                musicResponse.music.filter { 
                    it.isUserUploaded && 
                    it.uploadedBy == userId && 
                    it.category.lowercase() == categoryName 
                }
            }
            
            Result.success(songs)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing songs in category: $categoryPath", e)
            Result.failure(e)
        }
    }
    
    /**
     * Upload a song to user's category
     */
    suspend fun uploadSong(
        userId: String,
        songData: SongUploadData
    ): Result<GitHubUploadResponse> = withContext(Dispatchers.IO) {
        try {
            val token = getGitHubToken()
            if (token.isEmpty()) {
                return@withContext Result.failure(Exception("GitHub token not configured"))
            }
            
            // Sanitize filename
            val sanitizedFileName = songData.fileName
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .replace(Regex("_+"), "_")
            
            // Create path: music/users/{userId}/{category}/{filename}
            val filePath = "$USER_SONGS_PATH/$userId/${songData.category}/$sanitizedFileName"
            
            // Encode file data to Base64
            val base64Content = Base64.encodeToString(songData.fileData, Base64.NO_WRAP)
            
            // Create upload request
            val uploadRequest = GitHubUploadRequest(
                message = "Upload song: ${songData.title} by ${songData.artist}",
                content = base64Content,
                branch = BRANCH
            )
            
            val requestBody = json.encodeToString(
                GitHubUploadRequest.serializer(),
                uploadRequest
            ).toRequestBody(jsonMediaType)
            
            val request = Request.Builder()
                .url("$GITHUB_API_BASE/repos/$REPO_OWNER/$REPO_NAME/contents/$filePath")
                .header("Authorization", "token $token")
                .header("Accept", "application/vnd.github.v3+json")
                .put(requestBody)
                .build()
            
            Log.d(TAG, "Uploading song to: $filePath")
            
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "Upload failed: ${response.code} - $responseBody")
                    return@withContext Result.failure(
                        Exception("Upload failed: ${response.code} - ${response.message}")
                    )
                }
                
                val uploadResponse = json.decodeFromString(
                    GitHubUploadResponse.serializer(),
                    responseBody
                )
                
                Log.d(TAG, "Song uploaded successfully: ${uploadResponse.content.download_url}")
                
                // Update music.json to include the new song
                updateMusicMetadata(userId, songData, uploadResponse.content.download_url ?: "")
                
                Result.success(uploadResponse)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading song", e)
            Result.failure(e)
        }
    }
    
    /**
     * Fetch music.json from the repository
     */
    private suspend fun fetchMusicJson(): MusicResponse = withContext(Dispatchers.IO) {
        val token = getGitHubToken()
        
        val request = Request.Builder()
            .url("$GITHUB_API_BASE/repos/$REPO_OWNER/$REPO_NAME/contents/music.json?ref=$BRANCH")
            .apply {
                if (token.isNotEmpty()) {
                    header("Authorization", "token $token")
                }
            }
            .header("Accept", "application/vnd.github.v3+json")
            .get()
            .build()
        
        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch music.json: ${response.code} - ${response.message}")
            }
            
            // Parse the GitHub API response which wraps the content
            val contentResponse = json.decodeFromString<GitHubFileContent>(responseBody)
            
            // Download the actual file content
            val contentUrl = contentResponse.download_url ?: throw Exception("No download URL for music.json")
            
            val contentRequest = Request.Builder()
                .url(contentUrl)
                .get()
                .build()
            
            httpClient.newCall(contentRequest).execute().use { contentResp ->
                val musicJson = contentResp.body?.string() ?: throw Exception("Empty music.json")
                
                if (!contentResp.isSuccessful) {
                    throw Exception("Failed to download music.json content: ${contentResp.code}")
                }
                
                // Parse music.json
                json.decodeFromString<MusicResponse>(musicJson)
            }
        }
    }
    
    /**
     * List directory contents using GitHub API
     */
    private suspend fun listDirectoryContents(path: String): List<GitHubTreeItem> = withContext(Dispatchers.IO) {
        val token = getGitHubToken()
        
        val request = Request.Builder()
            .url("$GITHUB_API_BASE/repos/$REPO_OWNER/$REPO_NAME/contents/$path?ref=$BRANCH")
            .apply {
                if (token.isNotEmpty()) {
                    header("Authorization", "token $token")
                }
            }
            .header("Accept", "application/vnd.github.v3+json")
            .get()
            .build()
        
        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            
            if (!response.isSuccessful) {
                // If directory doesn't exist, return empty list
                if (response.code == 404) {
                    return@withContext emptyList()
                }
                throw Exception("Failed to list directory: ${response.code} - ${response.message}")
            }
            
            // Parse as array of GitHubFileContent
            json.decodeFromString<List<GitHubFileContent>>(responseBody).map { file ->
                GitHubTreeItem(
                    path = file.path,
                    mode = "100644",
                    type = if (file.download_url == null) "tree" else "blob",
                    sha = file.sha,
                    size = file.size,
                    url = file.url
                )
            }
        }
    }
    
    /**
     * Update music.json with new song metadata
     */
    private suspend fun updateMusicMetadata(
        userId: String,
        songData: SongUploadData,
        downloadUrl: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = getGitHubToken()
            if (token.isEmpty()) {
                return@withContext Result.failure(Exception("GitHub token not configured"))
            }
            
            Log.d(TAG, "Fetching current music.json...")
            
            // 1. Fetch current music.json
            val musicJsonUrl = "$GITHUB_RAW_BASE/$REPO_OWNER/$REPO_NAME/$BRANCH/music.json"
            val fetchRequest = Request.Builder()
                .url(musicJsonUrl)
                .get()
                .build()
            
            val currentMusicData = httpClient.newCall(fetchRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch music.json: ${response.code}")
                    return@withContext Result.failure(Exception("Failed to fetch music.json"))
                }
                
                val jsonString = response.body?.string() ?: "{\"songs\":[]}"
                json.decodeFromString<MusicJsonData>(jsonString)
            }
            
            Log.d(TAG, "Current music.json has ${currentMusicData.songs.size} songs")
            
            // 2. Create new song metadata
            val timestamp = System.currentTimeMillis()
            val newSong = MusicMetadata(
                id = "${timestamp}_${userId}:${songData.title}",
                title = songData.title,
                artist = songData.artist,
                category = songData.category.lowercase(),
                url = downloadUrl,
                duration = songData.duration,
                tags = songData.tags,
                uploadedBy = userId,
                uploaderName = songData.uploaderName,
                source = "MANUAL"
            )
            
            // 3. Add new song to the list
            val updatedSongs = currentMusicData.songs + newSong
            val updatedMusicData = MusicJsonData(songs = updatedSongs)
            
            Log.d(TAG, "Updated music.json will have ${updatedSongs.size} songs")
            
            // 4. Get current file SHA (required for updating)
            val getFileUrl = "$GITHUB_API_BASE/repos/$REPO_OWNER/$REPO_NAME/contents/music.json"
            val getFileRequest = Request.Builder()
                .url(getFileUrl)
                .header("Authorization", "token $token")
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build()
            
            val fileSha = httpClient.newCall(getFileRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to get file SHA: ${response.code}")
                    return@withContext Result.failure(Exception("Failed to get file SHA"))
                }
                
                val responseBody = response.body?.string() ?: ""
                val fileInfo = json.decodeFromString<GitHubFileContent>(responseBody)
                fileInfo.sha
            }
            
            Log.d(TAG, "Got file SHA: $fileSha")
            
            // 5. Encode updated JSON to Base64
            val updatedJsonString = json.encodeToString(kotlinx.serialization.serializer(), updatedMusicData)
            val base64Content = Base64.encodeToString(updatedJsonString.toByteArray(), Base64.NO_WRAP)
            
            // 6. Update music.json on GitHub
            val updateRequest = GitHubUploadRequest(
                message = "Add song: ${songData.title} by ${songData.artist}",
                content = base64Content,
                branch = BRANCH,
                sha = fileSha
            )
            
            val requestBody = json.encodeToString(
                GitHubUploadRequest.serializer(),
                updateRequest
            ).toRequestBody(jsonMediaType)
            
            val putRequest = Request.Builder()
                .url(getFileUrl)
                .header("Authorization", "token $token")
                .header("Accept", "application/vnd.github.v3+json")
                .put(requestBody)
                .build()
            
            httpClient.newCall(putRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Failed to update music.json: ${response.code} - $errorBody")
                    return@withContext Result.failure(
                        Exception("Failed to update music.json: ${response.code}")
                    )
                }
                
                Log.d(TAG, "✅ Successfully updated music.json")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating music metadata", e)
            Result.failure(e)
        }
    }
    
    /**
     * Internal data class for music.json structure
     */
    @Serializable
    private data class MusicJsonData(
        val songs: List<MusicMetadata>
    )
}
