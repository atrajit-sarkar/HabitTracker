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
        if (!isEnabled || currentSong == MusicTrack.NONE) return
        
        try {
            // Stop existing player
            stopMusic()
            
            // Check if music is downloaded locally
            val musicFile = downloadManager.getMusicFile(currentSong.resourceName)
            if (!musicFile.exists() || musicFile.length() == 0L) {
                Log.w("BackgroundMusic", "Music file not downloaded: ${currentSong.resourceName}")
                return
            }
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(musicFile.absolutePath)
                isLooping = true
                setVolume(volume, volume)
                prepare()
                start()
            }
            
            Log.d("BackgroundMusic", "Music started: ${currentSong.displayName}")
        } catch (e: Exception) {
            Log.e("BackgroundMusic", "Failed to start music", e)
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
        if (isEnabled && currentSong != MusicTrack.NONE) {
            mediaPlayer?.start()
        }
    }
    
    fun setVolume(newVolume: Float) {
        volume = newVolume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(volume, volume)
    }
    
    fun changeSong(newSong: MusicTrack) {
        if (currentSong != newSong) {
            // Always stop the current song first to prevent overlap
            stopMusic()
            
            currentSong = newSong
            if (isEnabled && newSong != MusicTrack.NONE) {
                startMusic()
            }
        }
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
