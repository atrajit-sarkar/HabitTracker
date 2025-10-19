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
    val tags: List<String>
) {
    /**
     * Extract filename from URL for backwards compatibility
     */
    val filename: String
        get() = url.substringAfterLast("/")
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
