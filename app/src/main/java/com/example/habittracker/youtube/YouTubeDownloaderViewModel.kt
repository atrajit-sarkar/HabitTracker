package it.atraj.habittracker.youtube

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for YouTube video downloader screen
 * Handles video metadata extraction and download management
 */
@HiltViewModel
class YouTubeDownloaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    data class UiState(
        val youtubeUrl: String = "",
        val isValidatingUrl: Boolean = false,
        val isDownloading: Boolean = false,
        val videoMetadata: YouTubeExtractor.VideoMetadata? = null,
        val downloadProgress: MediaDownloader.DownloadProgress? = null,
        val selectedAudioStream: YouTubeExtractor.AudioStreamInfo? = null,
        val errorMessage: String? = null,
        val successMessage: String? = null,
        val downloadFolder: String = ""
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val youtubeExtractor = YouTubeExtractor()
    private val mediaDownloader = MediaDownloader(context)
    
    private var currentDownloadJob: Job? = null
    
    // SharedPreferences for storing download folder preference
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("youtube_downloader_prefs", Context.MODE_PRIVATE)
    }
    
    private val defaultDownloadsDir: File by lazy {
        // For Android 10+ (API 29+), use public Downloads directory
        // For older versions, use app-specific directory
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use public Downloads/HabitTracker folder
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HabitTracker").apply {
                if (!exists()) {
                    val created = mkdirs()
                    Log.d(TAG, "Public downloads directory created: $created, path: $absolutePath")
                } else {
                    Log.d(TAG, "Public downloads directory exists: $absolutePath")
                }
            }
        } else {
            // Fallback to app-specific directory for older Android versions
            File(context.getExternalFilesDir(null), "YouTubeDownloads").apply {
                if (!exists()) {
                    val created = mkdirs()
                    Log.d(TAG, "App-specific downloads directory created: $created, path: $absolutePath")
                } else {
                    Log.d(TAG, "App-specific downloads directory exists: $absolutePath")
                }
            }
        }
    }
    
    init {
        // Load saved download folder or use default
        val savedFolder = prefs.getString(PREF_DOWNLOAD_FOLDER, defaultDownloadsDir.absolutePath) 
            ?: defaultDownloadsDir.absolutePath
        _uiState.value = _uiState.value.copy(downloadFolder = savedFolder)
    }
    
    companion object {
        private const val TAG = "YouTubeDownloaderVM"
        private const val PREF_DOWNLOAD_FOLDER = "download_folder"
    }
    
    /**
     * Update the YouTube URL
     */
    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(
            youtubeUrl = url,
            errorMessage = null
        )
    }
    
    /**
     * Select an audio stream for download
     */
    fun selectAudioStream(audioStream: YouTubeExtractor.AudioStreamInfo) {
        _uiState.value = _uiState.value.copy(selectedAudioStream = audioStream)
    }
    
    /**
     * Update download folder and save preference
     */
    fun updateDownloadFolder(folderPath: String) {
        val folder = File(folderPath)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        
        // Save to SharedPreferences
        prefs.edit().putString(PREF_DOWNLOAD_FOLDER, folderPath).apply()
        
        // Update UI state
        _uiState.value = _uiState.value.copy(
            downloadFolder = folderPath,
            successMessage = "Download folder updated: ${folder.name}"
        )
        
        Log.d(TAG, "Download folder updated to: $folderPath")
    }
    
    /**
     * Reset to default download folder
     */
    fun resetToDefaultFolder() {
        updateDownloadFolder(defaultDownloadsDir.absolutePath)
    }
    
    /**
     * Get list of available download folders
     */
    fun getAvailableFolders(): List<Pair<String, String>> {
        val folders = mutableListOf<Pair<String, String>>()
        
        // Add public Downloads folder (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val publicDownloads = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HabitTracker")
            folders.add("Downloads/HabitTracker" to publicDownloads.absolutePath)
            
            val musicFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "HabitTracker")
            folders.add("Music/HabitTracker" to musicFolder.absolutePath)
            
            val documentsFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "HabitTracker")
            folders.add("Documents/HabitTracker" to documentsFolder.absolutePath)
        }
        
        // Add app-specific directory (always available, no permissions needed)
        val appSpecific = File(context.getExternalFilesDir(null), "YouTubeDownloads")
        folders.add("App Data (Private)" to appSpecific.absolutePath)
        
        return folders
    }
    
    /**
     * Validate YouTube URL and extract video metadata
     */
    fun validateAndExtractMetadata() {
        val url = _uiState.value.youtubeUrl.trim()
        
        if (url.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter a YouTube URL"
            )
            return
        }
        
        if (!YouTubeExtractor.isValidYouTubeUrl(url)) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Invalid YouTube URL. Please enter a valid YouTube link."
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isValidatingUrl = true,
                errorMessage = null,
                videoMetadata = null
            )
            
            try {
                Log.d(TAG, "Extracting metadata for URL: $url")
                
                val result = youtubeExtractor.extractVideoInfo(url)
                
                result.onSuccess { metadata ->
                    Log.d(TAG, "Successfully extracted metadata: ${metadata.title}")
                    
                    // Auto-select best audio stream
                    val bestAudio = youtubeExtractor.getBestAudioStream(metadata)
                    
                    _uiState.value = _uiState.value.copy(
                        isValidatingUrl = false,
                        videoMetadata = metadata,
                        selectedAudioStream = bestAudio,
                        errorMessage = null
                    )
                }
                
                result.onFailure { exception ->
                    Log.e(TAG, "Failed to extract metadata", exception)
                    _uiState.value = _uiState.value.copy(
                        isValidatingUrl = false,
                        errorMessage = "Failed to extract video info: ${exception.message}"
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during metadata extraction", e)
                _uiState.value = _uiState.value.copy(
                    isValidatingUrl = false,
                    errorMessage = "Unexpected error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Start downloading the video/audio
     */
    fun startDownload() {
        val metadata = _uiState.value.videoMetadata
        
        if (metadata == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "No video metadata available. Please validate URL first."
            )
            return
        }
        
        // Cancel any existing download
        currentDownloadJob?.cancel()
        
        currentDownloadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                errorMessage = null,
                successMessage = null,
                downloadProgress = null
            )
            
            try {
                // Get selected audio stream or use best one
                val audioStream = _uiState.value.selectedAudioStream 
                    ?: youtubeExtractor.getBestAudioStream(metadata)
                    ?: throw Exception("No audio stream available")
                
                val downloadUrl = audioStream.url
                
                // Generate safe filename - use only letters and numbers
                val safeTitle = metadata.title
                    .filter { it.isLetterOrDigit() }  // Keep ONLY alphanumeric characters
                    .take(25)  // Very short to avoid any issues
                    .ifEmpty { "audio" }  // Fallback if title has no alphanumeric chars
                
                val extension = "m4a"  // YouTube audio streams are usually m4a
                
                val fileName = "${safeTitle}${System.currentTimeMillis()}.$extension"  // No separator
                
                Log.d(TAG, "Starting download: $fileName")
                Log.d(TAG, "Selected stream: ${audioStream.quality} - ${audioStream.format}")
                Log.d(TAG, "Download URL: ${downloadUrl.take(100)}...")
                
                // Get download directory from preference
                val downloadDir = File(_uiState.value.downloadFolder).apply {
                    if (!exists()) {
                        mkdirs()
                        Log.d(TAG, "Created download directory: $absolutePath")
                    }
                }
                
                // Start download with progress tracking
                mediaDownloader.downloadFile(
                    url = downloadUrl,
                    fileName = fileName,
                    destinationDir = downloadDir
                ).collect { downloadState ->
                    when (downloadState) {
                        is MediaDownloader.DownloadState.Idle -> {
                            Log.d(TAG, "Download idle")
                        }
                        
                        is MediaDownloader.DownloadState.Downloading -> {
                            _uiState.value = _uiState.value.copy(
                                downloadProgress = downloadState.progress
                            )
                            
                            Log.d(
                                TAG,
                                "Download progress: ${downloadState.progress.percentage}% " +
                                "(${mediaDownloader.formatBytes(downloadState.progress.bytesDownloaded)} / " +
                                "${mediaDownloader.formatBytes(downloadState.progress.totalBytes)})"
                            )
                        }
                        
                        is MediaDownloader.DownloadState.Success -> {
                            Log.d(TAG, "Download successful: ${downloadState.file.absolutePath}")
                            
                            _uiState.value = _uiState.value.copy(
                                isDownloading = false,
                                successMessage = "Download completed successfully!\n" +
                                    "File saved to: ${downloadState.file.name}",
                                downloadProgress = null
                            )
                        }
                        
                        is MediaDownloader.DownloadState.Error -> {
                            Log.e(TAG, "Download error: ${downloadState.message}")
                            
                            _uiState.value = _uiState.value.copy(
                                isDownloading = false,
                                errorMessage = downloadState.message,
                                downloadProgress = null
                            )
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    errorMessage = "Download failed: ${e.message}",
                    downloadProgress = null
                )
            }
        }
    }
    
    /**
     * Cancel the current download
     */
    fun cancelDownload() {
        currentDownloadJob?.cancel()
        currentDownloadJob = null
        
        mediaDownloader.cancelDownload()
        
        _uiState.value = _uiState.value.copy(
            isDownloading = false,
            downloadProgress = null
        )
        
        Log.d(TAG, "Download cancelled by user")
    }
    
    /**
     * Clear error and success messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
    
    /**
     * Reset the UI state
     */
    fun reset() {
        currentDownloadJob?.cancel()
        currentDownloadJob = null
        
        _uiState.value = UiState()
    }
    
    override fun onCleared() {
        super.onCleared()
        currentDownloadJob?.cancel()
    }
}
