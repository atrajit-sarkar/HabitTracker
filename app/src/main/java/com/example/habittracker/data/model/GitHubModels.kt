package it.atraj.habittracker.data.model

import kotlinx.serialization.Serializable

/**
 * GitHub file content for uploads
 */
@Serializable
data class GitHubUploadRequest(
    val message: String,
    val content: String, // Base64 encoded content
    val branch: String = "main",
    val sha: String? = null // Required for updates
)

/**
 * GitHub upload response
 */
@Serializable
data class GitHubUploadResponse(
    val content: GitHubFileContent,
    val commit: GitHubCommit
)

/**
 * GitHub file content metadata
 */
@Serializable
data class GitHubFileContent(
    val name: String,
    val path: String,
    val sha: String,
    val size: Int,
    val url: String,
    val download_url: String?
)

/**
 * GitHub commit info
 */
@Serializable
data class GitHubCommit(
    val sha: String,
    val message: String
)

/**
 * GitHub tree item for directory listing
 */
@Serializable
data class GitHubTreeItem(
    val path: String,
    val mode: String,
    val type: String, // "blob" or "tree"
    val sha: String,
    val size: Int? = null,
    val url: String
)

/**
 * GitHub tree response
 */
@Serializable
data class GitHubTreeResponse(
    val sha: String,
    val url: String,
    val tree: List<GitHubTreeItem>,
    val truncated: Boolean = false
)

/**
 * Music category structure
 */
data class MusicCategory(
    val name: String,
    val path: String,
    val songCount: Int = 0
)

/**
 * User folder info
 */
data class UserFolder(
    val username: String,
    val displayName: String, // "You" for current user
    val path: String,
    val categories: List<MusicCategory> = emptyList()
)

/**
 * Song upload data
 */
data class SongUploadData(
    val fileName: String,
    val category: String,
    val fileData: ByteArray,
    val title: String,
    val artist: String,
    val duration: Int = 0,
    val tags: List<String> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SongUploadData

        if (fileName != other.fileName) return false
        if (category != other.category) return false
        if (!fileData.contentEquals(other.fileData)) return false
        if (title != other.title) return false
        if (artist != other.artist) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + fileData.contentHashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        return result
    }
}
