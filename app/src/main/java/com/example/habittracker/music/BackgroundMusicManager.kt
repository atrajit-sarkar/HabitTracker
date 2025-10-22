package it.atraj.habittracker.music

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundMusicManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: MusicDownloadManager
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentSong: MusicTrack = MusicTrack.AMBIENT_1
    private var currentDynamicFileName: String? = null // For dynamic tracks not in enum
    private var volume: Float = 0.3f // Default 30% volume
    private var isEnabled: Boolean = false
    
    enum class MusicTrack(val displayName: String, val resourceName: String) {
        NONE("No Music", ""),
        AMBIENT_1("Peaceful Ambient", "ambient_calm.mp3"),
        AMBIENT_2("Focus Flow", "ambient_focus.mp3"),
        AMBIENT_3("Nature Sounds", "ambient_nature.mp3"),
        LOFI_1("Lo-Fi Beats", "lofi_chill.mp3"),
        PIANO_1("Piano Melody", "piano_soft.mp3"),
        ROMANTIC_1("Casa Rosa", "romantic_casa_rosa.mp3"),
        HINDI_1("Love Slowed", "hindi_love_slowed.mp3"),
        JAPANESE_1("Waguri Edit", "japanese_waguri_edit.mp3"),
        JAPANESE_2("Shounen Ki", "japanese_shounen_ki.mp3"),
        CLAIR_OBSCUR("Lumière", "clair_obscur_lumiere.mp3"),
        CYBERPUNK("Stay At Your House", "cyberpunk_stay_at_house.mp3")
    }
    
    fun initialize(song: MusicTrack, volumeLevel: Float, enabled: Boolean) {
        this.currentSong = song
        this.volume = volumeLevel.coerceIn(0f, 1f)
        this.isEnabled = enabled
        
        if (enabled && song != MusicTrack.NONE) {
            startMusic()
        }
    }
    
    fun startMusic() {
        if (!isEnabled) return
        
        // Determine which file to play
        val fileName = currentDynamicFileName ?: currentSong.resourceName
        
        Log.d("BackgroundMusic", "startMusic() called - isEnabled: $isEnabled, fileName: $fileName, currentSong: ${currentSong.name}, currentDynamicFileName: $currentDynamicFileName")
        
        // Only skip if we have no valid filename (not just if currentSong is NONE, because dynamic tracks use NONE)
        if (fileName.isEmpty()) {
            Log.w("BackgroundMusic", "Not starting - fileName is empty")
            return
        }
        
        try {
            // Stop existing player
            stopMusic()
            
            // Check if music is downloaded locally
            val musicFile = downloadManager.getMusicFile(fileName)
            Log.d("BackgroundMusic", "Music file path: ${musicFile.absolutePath}")
            Log.d("BackgroundMusic", "File exists: ${musicFile.exists()}, size: ${musicFile.length()}")
            
            if (!musicFile.exists() || musicFile.length() == 0L) {
                Log.w("BackgroundMusic", "⚠️ Music file not downloaded: $fileName")
                return
            }
            
            Log.d("BackgroundMusic", "Creating MediaPlayer for: $fileName")
            
            mediaPlayer = MediaPlayer().apply {
                // Use FileInputStream and FileDescriptor for better compatibility with Android 15+
                try {
                    val fis = java.io.FileInputStream(musicFile)
                    setDataSource(fis.fd)
                    fis.close()
                } catch (e: Exception) {
                    Log.e("BackgroundMusic", "Failed to set data source with FileDescriptor, trying absolute path", e)
                    setDataSource(musicFile.absolutePath)
                }
                
                isLooping = true
                setVolume(volume, volume)
                Log.d("BackgroundMusic", "Preparing MediaPlayer...")
                prepare()
                Log.d("BackgroundMusic", "Starting MediaPlayer...")
                start()
            }
            
            Log.d("BackgroundMusic", "✅ Music started successfully: $fileName (isPlaying: ${mediaPlayer?.isPlaying})")
        } catch (e: Exception) {
            Log.e("BackgroundMusic", "❌ Failed to start music: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    fun stopMusic() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("BackgroundMusic", "Error stopping music", e)
        } finally {
            mediaPlayer = null
        }
    }
    
    fun pauseMusic() {
        mediaPlayer?.pause()
    }
    
    fun resumeMusic() {
        // Resume if music is enabled and we have either an enum track or a dynamic track
        val hasValidTrack = (currentSong != MusicTrack.NONE) || (currentDynamicFileName != null && currentDynamicFileName!!.isNotEmpty())
        
        if (isEnabled && hasValidTrack) {
            try {
                mediaPlayer?.let { player ->
                    if (!player.isPlaying) {
                        player.start()
                        Log.d("BackgroundMusic", "Music resumed - Song: ${currentSong.name}, Dynamic: $currentDynamicFileName")
                    }
                }
            } catch (e: Exception) {
                Log.e("BackgroundMusic", "Error resuming music", e)
                // Try to restart the music if resume fails
                startMusic()
            }
        }
    }
    
    fun setVolume(newVolume: Float) {
        volume = newVolume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(volume, volume)
    }
    
    fun changeSong(newSong: MusicTrack) {
        if (currentSong != newSong || newSong == MusicTrack.NONE) {
            // Always stop the current song first to prevent overlap
            stopMusic()
            
            currentSong = newSong
            currentDynamicFileName = null // Clear dynamic file when using enum
            
            Log.d("BackgroundMusic", "changeSong - newSong: ${newSong.name}, isEnabled: $isEnabled")
            
            if (isEnabled && newSong != MusicTrack.NONE) {
                startMusic()
            } else if (newSong == MusicTrack.NONE) {
                Log.d("BackgroundMusic", "NONE selected - music stopped")
            }
        }
    }
    
    /**
     * Play a dynamic track by filename (for tracks not in the enum)
     */
    fun playDynamicTrack(fileName: String) {
        Log.d("BackgroundMusic", "playDynamicTrack called - fileName: $fileName, isEnabled: $isEnabled")
        
        // Always stop the current song first
        stopMusic()
        
        currentSong = MusicTrack.NONE // Set to NONE when playing dynamic track
        currentDynamicFileName = fileName
        
        // Check if file exists
        val musicFile = downloadManager.getMusicFile(fileName)
        val fileExists = musicFile.exists() && musicFile.length() > 0L
        Log.d("BackgroundMusic", "File check - path: ${musicFile.absolutePath}, exists: ${musicFile.exists()}, size: ${musicFile.length()}")
        
        if (!fileExists) {
            Log.w("BackgroundMusic", "⚠️ Cannot play dynamic track - file not downloaded: $fileName")
            return
        }
        
        if (isEnabled && fileName.isNotEmpty()) {
            startMusic()
            Log.d("BackgroundMusic", "✅ Playing dynamic track: $fileName")
        } else {
            Log.w("BackgroundMusic", "⚠️ Not playing - isEnabled: $isEnabled, fileName.isNotEmpty: ${fileName.isNotEmpty()}")
        }
    }
    
    /**
     * Get the currently playing filename (works for both enum and dynamic tracks)
     */
    fun getCurrentFileName(): String {
        return currentDynamicFileName ?: currentSong.resourceName
    }
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (enabled) {
            startMusic()
        } else {
            stopMusic()
        }
    }
    
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
    
    fun getCurrentSong(): MusicTrack = currentSong
    
    fun getVolume(): Float = volume
    
    fun isEnabled(): Boolean = isEnabled
}
