package it.atraj.habittracker.music

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val musicDir: File by lazy {
        File(context.filesDir, "music").apply {
            if (!exists()) mkdirs()
        }
    }
    
    // GitHub raw URLs for music files (stored in /songs folder)
    private val musicUrls = mapOf(
        "ambient_calm.mp3" to "https://github.com/atrajit-sarkar/HabitTracker/raw/main/songs/ambient_calm.mp3",
        "ambient_focus.mp3" to "https://github.com/atrajit-sarkar/HabitTracker/raw/main/songs/ambient_focus.mp3",
        "ambient_nature.mp3" to "https://github.com/atrajit-sarkar/HabitTracker/raw/main/songs/ambient_nature.mp3",
        "lofi_chill.mp3" to "https://github.com/atrajit-sarkar/HabitTracker/raw/main/songs/lofi_chill.mp3",
        "piano_soft.mp3" to "https://github.com/atrajit-sarkar/HabitTracker/raw/main/songs/piano_soft.mp3",
        "romantic_casa_rosa.mp3" to "https://github.com/atrajit-sarkar/HabitTracker/raw/main/songs/romantic_casa_rosa.mp3",
        "hindi_love_slowed.mp3" to "https://github.com/atrajit-sarkar/HabitTracker/raw/main/songs/hindi_love_slowed.mp3",
        "japanese_waguri_edit.mp3" to "https://github.com/atrajit-sarkar/HabitTracker/raw/main/songs/japanese_waguri_edit.mp3",
        "japanese_shounen_ki.mp3" to "https://github.com/atrajit-sarkar/HabitTracker/raw/main/songs/japanese_shounen_ki.mp3",
        "clair_obscur_lumiere.mp3" to "https://github.com/atrajit-sarkar/HabitTracker/raw/main/songs/clair_obscur_lumiere.mp3",
        "cyberpunk_stay_at_house.mp3" to "https://github.com/atrajit-sarkar/HabitTracker/raw/main/songs/cyberpunk_stay_at_house.mp3"
    )
    
    /**
     * Check if a music file is already downloaded
     */
    fun isMusicDownloaded(fileName: String): Boolean {
        val file = File(musicDir, fileName)
        return file.exists() && file.length() > 0
    }
    
    /**
     * Get the local file for a music track
     */
    fun getMusicFile(fileName: String): File {
        return File(musicDir, fileName)
    }
    
    /**
     * Download a music file from GitHub with progress callback
     * @param fileName The name of the music file to download
     * @param onProgress Callback for download progress (0-100)
     * @return Result with success/error
     */
    suspend fun downloadMusic(
        fileName: String,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val url = musicUrls[fileName]
                ?: return@withContext Result.failure(Exception("Unknown music file: $fileName"))
            
            val destinationFile = File(musicDir, fileName)
            
            // If already exists and valid, return it
            if (destinationFile.exists() && destinationFile.length() > 0) {
                Log.d("MusicDownload", "File already exists: $fileName")
                onProgress(100)
                return@withContext Result.success(destinationFile)
            }
            
            Log.d("MusicDownload", "Starting download: $fileName from $url")
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.connect()
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    Exception("HTTP error: ${connection.responseCode}")
                )
            }
            
            val fileLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(destinationFile)
            
            val buffer = ByteArray(8192)
            var total = 0L
            var count: Int
            
            while (inputStream.read(buffer).also { count = it } != -1) {
                total += count
                outputStream.write(buffer, 0, count)
                
                // Calculate and report progress
                if (fileLength > 0) {
                    val progress = (total * 100 / fileLength).toInt()
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
            }
            
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            
            Log.d("MusicDownload", "Download complete: $fileName (${destinationFile.length()} bytes)")
            
            withContext(Dispatchers.Main) {
                onProgress(100)
            }
            
            Result.success(destinationFile)
        } catch (e: Exception) {
            Log.e("MusicDownload", "Download failed: $fileName", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a downloaded music file
     */
    fun deleteMusicFile(fileName: String): Boolean {
        val file = File(musicDir, fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
    
    /**
     * Get total size of downloaded music files
     */
    fun getTotalDownloadedSize(): Long {
        return musicDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
    
    /**
     * Clear all downloaded music
     */
    fun clearAllMusic() {
        musicDir.listFiles()?.forEach { it.delete() }
    }
}
