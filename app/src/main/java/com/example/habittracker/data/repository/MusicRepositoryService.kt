package it.atraj.habittracker.data.repository

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import it.atraj.habittracker.data.model.CachedMusicData
import it.atraj.habittracker.data.model.MusicResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching music metadata from GitHub
 */
@Singleton
class MusicRepositoryService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MusicRepository"
        private const val REPO_BASE_URL = "https://raw.githubusercontent.com/gongobongofounder/HabitTracker-Music/main/"
        private const val METADATA_URL = "${REPO_BASE_URL}music.json"
        private const val CACHE_FILE_NAME = "music_cache.json"
        private const val CACHE_VALIDITY_HOURS = 24
        private const val CHECK_UPDATE_INTERVAL_HOURS = 12
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val cacheFile: File by lazy {
        File(context.cacheDir, CACHE_FILE_NAME)
    }
    
    private var memoryCache: CachedMusicData? = null
    private var lastCheckTime: Long = 0
    
    /**
     * Get music metadata - from cache if valid, otherwise from network
     */
    suspend fun getMusicMetadata(forceRefresh: Boolean = false): Result<MusicResponse> = withContext(Dispatchers.IO) {
        try {
            // Check memory cache first
            if (!forceRefresh && memoryCache != null && memoryCache!!.isValid(CACHE_VALIDITY_HOURS)) {
                Log.d(TAG, "Returning music from memory cache")
                return@withContext Result.success(memoryCache!!.response)
            }
            
            // Check file cache
            if (!forceRefresh) {
                val cached = loadFromCache()
                if (cached != null && cached.isValid(CACHE_VALIDITY_HOURS)) {
                    Log.d(TAG, "Returning music from file cache")
                    memoryCache = cached
                    return@withContext Result.success(cached.response)
                }
            }
            
            // Fetch from network
            Log.d(TAG, "Fetching music from network")
            fetchFromNetwork()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting music metadata", e)
            
            // Fallback to cache even if expired
            val cached = memoryCache ?: loadFromCache()
            if (cached != null) {
                Log.d(TAG, "Returning expired cache as fallback")
                Result.success(cached.response)
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check if updates are available
     */
    suspend fun checkForUpdates(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            
            // Don't check too frequently
            if (currentTime - lastCheckTime < CHECK_UPDATE_INTERVAL_HOURS * 60 * 60 * 1000) {
                Log.d(TAG, "Skipping update check - too soon")
                return@withContext Result.success(false)
            }
            
            lastCheckTime = currentTime
            
            val cached = memoryCache ?: loadFromCache()
            if (cached == null) {
                Log.d(TAG, "No cache available, update needed")
                return@withContext Result.success(true)
            }
            
            // Fetch latest version info
            val latestResult = fetchFromNetwork()
            if (latestResult.isFailure) {
                return@withContext Result.success(false)
            }
            
            val latest = latestResult.getOrNull()!!
            val hasUpdate = latest.version != cached.response.version ||
                           latest.lastUpdated != cached.response.lastUpdated
            
            Log.d(TAG, "Update check: hasUpdate=$hasUpdate, " +
                      "cached=${cached.response.version}, latest=${latest.version}")
            
            Result.success(hasUpdate)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            Result.failure(e)
        }
    }
    
    /**
     * Force refresh music metadata
     */
    suspend fun refreshMusicMetadata(): Result<MusicResponse> {
        return getMusicMetadata(forceRefresh = true)
    }
    
    /**
     * Get cached music synchronously (for quick access)
     */
    fun getCachedMusicSync(): MusicResponse? {
        return memoryCache?.response ?: loadFromCache()?.response
    }
    
    /**
     * Clear all caches
     */
    fun clearCache() {
        memoryCache = null
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
        Log.d(TAG, "Cache cleared")
    }
    
    // Private helper methods
    
    private fun fetchFromNetwork(): Result<MusicResponse> {
        try {
            val request = Request.Builder()
                .url(METADATA_URL)
                .addHeader("Cache-Control", "no-cache")
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("HTTP error: ${response.code}")
            }
            
            val body = response.body?.string() ?: throw Exception("Empty response body")
            
            val adapter = moshi.adapter(MusicResponse::class.java)
            val musicResponse = adapter.fromJson(body) ?: throw Exception("Failed to parse JSON")
            
            // Cache the result
            val cachedData = CachedMusicData(musicResponse)
            saveToCache(cachedData)
            memoryCache = cachedData
            
            Log.d(TAG, "Successfully fetched ${musicResponse.music.size} tracks from network")
            return Result.success(musicResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Network fetch failed", e)
            return Result.failure(e)
        }
    }
    
    private fun loadFromCache(): CachedMusicData? {
        return try {
            if (!cacheFile.exists()) {
                Log.d(TAG, "Cache file doesn't exist")
                return null
            }
            
            val json = cacheFile.readText()
            val adapter = moshi.adapter(CachedMusicData::class.java)
            val cached = adapter.fromJson(json)
            
            if (cached != null) {
                Log.d(TAG, "Loaded ${cached.response.music.size} tracks from cache")
            }
            
            cached
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cache", e)
            null
        }
    }
    
    private fun saveToCache(data: CachedMusicData) {
        try {
            val adapter = moshi.adapter(CachedMusicData::class.java)
            val json = adapter.toJson(data)
            cacheFile.writeText(json)
            Log.d(TAG, "Saved music to cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cache", e)
        }
    }
}
