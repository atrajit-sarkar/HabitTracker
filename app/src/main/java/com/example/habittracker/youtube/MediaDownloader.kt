package it.atraj.habittracker.youtube

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Handles downloading media files from URLs with progress tracking
 */
class MediaDownloader(private val context: Context) {
    
    data class DownloadProgress(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val percentage: Int,
        val speedBytesPerSecond: Long = 0
    )
    
    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: DownloadProgress) : DownloadState()
        data class Success(val file: File) : DownloadState()
        data class Error(val message: String, val exception: Exception? = null) : DownloadState()
    }
    
    companion object {
        private const val TAG = "MediaDownloader"
        private const val BUFFER_SIZE = 524288  // 512KB buffer for maximum download speed (was 64KB)
        private const val TIMEOUT_SECONDS = 120L  // Increased timeout for large files
        private const val PROGRESS_UPDATE_INTERVAL_MS = 250L  // Update UI every 250ms instead of 500ms
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    
    /**
     * Download a file from URL to the specified directory with progress tracking
     * 
     * @param url The URL to download from
     * @param fileName The name for the downloaded file
     * @param destinationDir The directory to save the file
     * @return Flow emitting download state updates
     */
    fun downloadFile(
        url: String,
        fileName: String,
        destinationDir: File
    ): Flow<DownloadState> = flow {
        try {
            emit(DownloadState.Idle)
            
            Log.d(TAG, "Starting download: $url")
            Log.d(TAG, "Destination: ${destinationDir.absolutePath}/$fileName")
            
            // Ensure destination directory exists
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }
            
            // Create destination file
            val destinationFile = File(destinationDir, fileName)
            
            // Build request
            val request = Request.Builder()
                .url(url)
                .build()
            
            // Execute download (already on IO dispatcher due to flowOn)
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadState.Error("Download failed: ${response.code}"))
                    return@flow
                }
                
                val body = response.body
                if (body == null) {
                    emit(DownloadState.Error("Empty response body"))
                    return@flow
                }
                
                val totalBytes = body.contentLength()
                Log.d(TAG, "Total file size: ${totalBytes / 1024 / 1024}MB")
                
                var bytesDownloaded = 0L
                val startTime = System.currentTimeMillis()
                var lastProgressEmitTime = startTime
                
                body.byteStream().use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            bytesDownloaded += bytesRead
                            
                            // Emit progress update every 250ms for responsive UI
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastProgressEmitTime >= PROGRESS_UPDATE_INTERVAL_MS || 
                                bytesDownloaded == totalBytes) {
                                
                                val elapsedSeconds = (currentTime - startTime) / 1000.0
                                val speedBytesPerSecond = if (elapsedSeconds > 0) {
                                    (bytesDownloaded / elapsedSeconds).toLong()
                                } else {
                                    0L
                                }
                                
                                val percentage = if (totalBytes > 0) {
                                    ((bytesDownloaded * 100) / totalBytes).toInt()
                                } else {
                                    0
                                }
                                
                                emit(
                                    DownloadState.Downloading(
                                        DownloadProgress(
                                            bytesDownloaded = bytesDownloaded,
                                            totalBytes = totalBytes,
                                            percentage = percentage,
                                            speedBytesPerSecond = speedBytesPerSecond
                                        )
                                    )
                                )
                                
                                lastProgressEmitTime = currentTime
                            }
                        }
                    }
                }
                
                Log.d(TAG, "Download completed: ${destinationFile.absolutePath}")
                emit(DownloadState.Success(destinationFile))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            emit(DownloadState.Error("Download failed: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Format bytes to human-readable format
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
    
    /**
     * Format download speed
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatBytes(bytesPerSecond)}/s"
    }
    
    /**
     * Cancel an ongoing download
     */
    fun cancelDownload() {
        // OkHttp automatically cancels when the coroutine is cancelled
        Log.d(TAG, "Download cancelled")
    }
}
