package it.atraj.habittracker.social.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a social post in the Habit Tracker app
 * Images are stored in GitHub, text content in Firebase
 */
data class Post(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "", // Emoji or avatar URL
    val userPhotoUrl: String? = null, // User's profile photo
    
    // Content
    val content: String = "", // Markdown formatted content
    val imageUrls: List<String> = emptyList(), // GitHub raw URLs for images
    
    // Metadata
    @ServerTimestamp
    val timestamp: Date? = null,
    val likes: List<String> = emptyList(), // List of user IDs who liked
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    
    // Visibility
    val isPublic: Boolean = false, // false = friends only
    val viewerIds: List<String> = emptyList() // User IDs who can view (friends)
)

/**
 * Represents a comment on a post
 */
data class Comment(
    @DocumentId
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val userPhotoUrl: String? = null,
    
    val content: String = "", // Plain text comment
    
    @ServerTimestamp
    val timestamp: Date? = null,
    
    val likes: List<String> = emptyList(),
    val likeCount: Int = 0
)

/**
 * User social profile information
 */
data class UserSocialProfile(
    @DocumentId
    val userId: String = "",
    val userName: String = "",
    val bio: String = "",
    val avatar: String = "",
    val photoUrl: String? = null,
    
    // Social stats
    val friendCount: Int = 0,
    val postCount: Int = 0,
    val totalLikes: Int = 0,
    
    // Privacy
    val isProfilePublic: Boolean = false,
    
    @ServerTimestamp
    val lastUpdated: Date? = null
)

/**
 * GitHub post image metadata
 */
data class PostImage(
    val fileName: String,
    val githubPath: String, // Path in GitHub repo
    val rawUrl: String, // Direct URL to download
    val uploadedAt: Long = System.currentTimeMillis()
)
