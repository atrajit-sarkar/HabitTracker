package it.atraj.habittracker.data.firestore

import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.serialization.Serializable

@Serializable
data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUserEmail: String = "",
    val fromUserName: String = "",
    val fromUserAvatar: String = "",
    val toUserId: String = "",
    val toUserEmail: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class Friendship(
    val id: String = "",
    val user1Id: String = "",
    val user2Id: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class UserPublicProfile(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null, // Google profile picture URL
    val customAvatar: String = "😊",
    val successRate: Int = 0, // Completion percentage
    val totalHabits: Int = 0,
    val totalCompletions: Int = 0,
    val currentStreak: Int = 0,
    val leaderboardScore: Int = 0, // Cumulative score for ranking (higher is better)
    val updatedAt: Long = System.currentTimeMillis()
)

// Extension functions
fun DocumentSnapshot.toFriendRequest(): FriendRequest? {
    return try {
        val data = data ?: return null
        FriendRequest(
            id = id,
            fromUserId = data["fromUserId"] as? String ?: "",
            fromUserEmail = data["fromUserEmail"] as? String ?: "",
            fromUserName = data["fromUserName"] as? String ?: "",
            fromUserAvatar = data["fromUserAvatar"] as? String ?: "",
            toUserId = data["toUserId"] as? String ?: "",
            toUserEmail = data["toUserEmail"] as? String ?: "",
            status = data["status"] as? String ?: "PENDING",
            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
            updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis()
        )
    } catch (e: Exception) {
        null
    }
}

fun DocumentSnapshot.toFriendship(): Friendship? {
    return try {
        val data = data ?: return null
        Friendship(
            id = id,
            user1Id = data["user1Id"] as? String ?: "",
            user2Id = data["user2Id"] as? String ?: "",
            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()
        )
    } catch (e: Exception) {
        null
    }
}

fun DocumentSnapshot.toUserPublicProfile(): UserPublicProfile? {
    return try {
        val data = data ?: return null
        UserPublicProfile(
            userId = id,
            email = data["email"] as? String ?: "",
            displayName = data["displayName"] as? String ?: "",
            photoUrl = data["photoUrl"] as? String,
            customAvatar = data["customAvatar"] as? String ?: "😊",
            successRate = (data["successRate"] as? Long)?.toInt() ?: 0,
            totalHabits = (data["totalHabits"] as? Long)?.toInt() ?: 0,
            totalCompletions = (data["totalCompletions"] as? Long)?.toInt() ?: 0,
            currentStreak = (data["currentStreak"] as? Long)?.toInt() ?: 0,
            leaderboardScore = (data["leaderboardScore"] as? Long)?.toInt() ?: 0,
            updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis()
        )
    } catch (e: Exception) {
        null
    }
}
