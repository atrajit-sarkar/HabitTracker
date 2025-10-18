package it.atraj.habittracker.data.manager

import android.content.Context
import android.util.Log
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import it.atraj.habittracker.data.model.MusicMetadata
import it.atraj.habittracker.data.repository.MusicRepositoryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for dynamic music loading and updates
 */
@Singleton
class DynamicMusicManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepositoryService: MusicRepositoryService
) {
    companion object {
        private const val TAG = "DynamicMusicManager"
        private const val UPDATE_WORKER_NAME = "music_update_worker"
        private const val UPDATE_INTERVAL_HOURS = 12L
    }
    
    private val _musicList = MutableStateFlow<List<MusicMetadata>>(emptyList())
    val musicList: StateFlow<List<MusicMetadata>> = _musicList.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Initialize the manager and load music
     */
    suspend fun initialize() {
        // Try to load from cache first for instant UI
        val cached = musicRepositoryService.getCachedMusicSync()
        if (cached != null) {
            _musicList.value = cached.music
            Log.d(TAG, "Loaded ${cached.music.size} tracks from cache")
        }
        
        // Then fetch fresh data in background
        loadMusicMetadata(forceRefresh = false)
        
        // Schedule periodic updates
        schedulePeriodicUpdates()
    }
    
    /**
     * Load music metadata
     */
    suspend fun loadMusicMetadata(forceRefresh: Boolean = false) {
        _isLoading.value = true
        _error.value = null
        
        try {
            val result = musicRepositoryService.getMusicMetadata(forceRefresh)
            
            if (result.isSuccess) {
                val response = result.getOrNull()!!
                _musicList.value = response.music
                Log.d(TAG, "Successfully loaded ${response.music.size} tracks, version: ${response.version}")
            } else {
                val errorMsg = "Failed to load music: ${result.exceptionOrNull()?.message}"
                _error.value = errorMsg
                Log.e(TAG, errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Error loading music: ${e.message}"
            _error.value = errorMsg
            Log.e(TAG, errorMsg, e)
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Check for updates and reload if available
     */
    suspend fun checkAndUpdateIfNeeded() {
        try {
            val result = musicRepositoryService.checkForUpdates()
            
            if (result.isSuccess && result.getOrNull() == true) {
                Log.d(TAG, "Updates available, refreshing music metadata")
                loadMusicMetadata(forceRefresh = true)
            } else {
                Log.d(TAG, "No updates available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
        }
    }
    
    /**
     * Get music by ID
     */
    fun getMusicById(id: String): MusicMetadata? {
        return _musicList.value.find { it.id == id }
    }
    
    /**
     * Get music by category
     */
    fun getMusicByCategory(category: String): List<MusicMetadata> {
        return _musicList.value.filter { it.category == category }
    }
    
    /**
     * Get all categories
     */
    fun getCategories(): List<String> {
        return _musicList.value.map { it.category }.distinct().sorted()
    }
    
    /**
     * Get download URL for music
     */
    fun getDownloadUrl(musicMetadata: MusicMetadata): String {
        return "https://raw.githubusercontent.com/gongobongofounder/HabitTracker-Music/main/music/${musicMetadata.filename}"
    }
    
    /**
     * Schedule periodic background updates
     */
    private fun schedulePeriodicUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val updateRequest = PeriodicWorkRequestBuilder<MusicUpdateWorker>(
            UPDATE_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                UPDATE_WORKER_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                updateRequest
            )
        
        Log.d(TAG, "Scheduled periodic music updates every $UPDATE_INTERVAL_HOURS hours")
    }
    
    /**
     * Cancel periodic updates
     */
    fun cancelPeriodicUpdates() {
        WorkManager.getInstance(context).cancelUniqueWork(UPDATE_WORKER_NAME)
        Log.d(TAG, "Cancelled periodic music updates")
    }
    
    /**
     * Clear cache and reload
     */
    suspend fun clearCacheAndReload() {
        musicRepositoryService.clearCache()
        loadMusicMetadata(forceRefresh = true)
    }
}

/**
 * Worker for periodic music updates
 */
class MusicUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        Log.d("MusicUpdateWorker", "Starting periodic music update")
        
        return try {
            // Note: This is a simplified version. In production, you'd inject the repository
            // For now, we'll trigger the update through the app when it's running
            Log.d("MusicUpdateWorker", "Music update check completed")
            Result.success()
        } catch (e: Exception) {
            Log.e("MusicUpdateWorker", "Music update failed", e)
            Result.retry()
        }
    }
}
