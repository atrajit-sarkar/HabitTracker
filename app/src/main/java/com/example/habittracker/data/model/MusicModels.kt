package it.atraj.habittracker.data.model

import kotlinx.serialization.Serializable

/**
 * Response model for music.json from GitHub repository
 */
@Serializable
data class MusicResponse(
    val version: String,
    val lastUpdated: String,
    val music: List<MusicMetadata>
)

/**
 * Individual music track metadata
 */
@Serializable
data class MusicMetadata(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Int,
    val url: String,
    val category: String,
    val tags: List<String>,
    val uploadedBy: String? = null,  // User ID who uploaded (null for official songs)
    val uploaderName: String? = null, // Display name of uploader
    val source: String? = null        // "MANUAL" for user uploads, null for official
) {
    /**
     * Extract filename from URL for backwards compatibility
     */
    val filename: String
        get() = url.substringAfterLast("/")
    
    /**
     * Check if this is a user-uploaded song
     */
    val isUserUploaded: Boolean
        get() = uploadedBy != null || source == "MANUAL"
    
    /**
     * Check if this is an official song
     */
    val isOfficial: Boolean
        get() = !isUserUploaded
}

/**
 * Cached music data with timestamp
 */
@Serializable
data class CachedMusicData(
    val response: MusicResponse,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun isValid(validityHours: Int = 24): Boolean {
        val ageHours = (System.currentTimeMillis() - cachedAt) / (1000 * 60 * 60)
        return ageHours < validityHours
    }
}
