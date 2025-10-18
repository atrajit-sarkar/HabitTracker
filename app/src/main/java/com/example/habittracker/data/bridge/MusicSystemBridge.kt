package it.atraj.habittracker.data.bridge

import it.atraj.habittracker.data.model.MusicMetadata
import it.atraj.habittracker.music.BackgroundMusicManager

/**
 * Bridge class to help transition from enum-based music system to dynamic music system
 */
object MusicSystemBridge {
    
    /**
     * Convert MusicMetadata to the legacy MusicTrack enum
     * Returns null if no matching enum is found
     */
    fun metadataToEnum(metadata: MusicMetadata): BackgroundMusicManager.MusicTrack? {
        return try {
            // Try to match by ID
            BackgroundMusicManager.MusicTrack.valueOf(metadata.id.uppercase())
        } catch (e: Exception) {
            // If ID doesn't match enum, try to find by filename
            val tracks = BackgroundMusicManager.MusicTrack.values()
            tracks.find { it.resourceName == metadata.filename }
        }
    }
    
    /**
     * Convert enum to track ID string
     */
    fun enumToId(track: BackgroundMusicManager.MusicTrack): String {
        return track.name
    }
    
    /**
     * Get downloadable filename from metadata
     */
    fun getFileName(metadata: MusicMetadata): String {
        return metadata.filename
    }
    
    /**
     * Check if a track ID matches the NONE track
     */
    fun isNoneTrack(trackId: String): Boolean {
        return trackId.equals("NONE", ignoreCase = true)
    }
    
    /**
     * Get the NONE track ID
     */
    fun getNoneTrackId(): String {
        return "NONE"
    }
}
