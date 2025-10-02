package com.example.habittracker.data.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FriendRepository"
        private const val FRIEND_REQUESTS_COLLECTION = "friendRequests"
        private const val FRIENDSHIPS_COLLECTION = "friendships"
        private const val USER_PROFILES_COLLECTION = "userProfiles"
    }

    // Search users by email
    suspend fun searchUserByEmail(email: String): UserPublicProfile? {
        return try {
            val snapshot = firestore.collection(USER_PROFILES_COLLECTION)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.toUserPublicProfile()
        } catch (e: Exception) {
            Log.e(TAG, "Error searching user by email: ${e.message}", e)
            null
        }
    }

    // Send friend request
    suspend fun sendFriendRequest(
        fromUserId: String,
        fromUserEmail: String,
        fromUserName: String,
        fromUserAvatar: String,
        toUserId: String,
        toUserEmail: String
    ): Result<String> {
        return try {
            // Check if already friends
            val existingFriendship = checkFriendshipExists(fromUserId, toUserId)
            if (existingFriendship) {
                return Result.failure(Exception("Already friends with this user"))
            }

            // Check if there's already a pending request
            val existingRequest = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", fromUserId)
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()

            if (!existingRequest.isEmpty) {
                return Result.failure(Exception("Friend request already sent"))
            }

            // Check if there's a reverse pending request
            val reverseRequest = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", toUserId)
                .whereEqualTo("toUserId", fromUserId)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()

            if (!reverseRequest.isEmpty) {
                return Result.failure(Exception("This user has already sent you a friend request"))
            }

            val request = FriendRequest(
                fromUserId = fromUserId,
                fromUserEmail = fromUserEmail,
                fromUserName = fromUserName,
                fromUserAvatar = fromUserAvatar,
                toUserId = toUserId,
                toUserEmail = toUserEmail,
                status = "PENDING"
            )

            val docRef = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .add(request)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Get pending friend requests (received)
    fun getPendingFriendRequests(userId: String): Flow<List<FriendRequest>> = callbackFlow {
        var listenerRegistration: ListenerRegistration? = null
        
        try {
            listenerRegistration = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to friend requests: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    
                    val requests = snapshot?.documents?.mapNotNull { it.toFriendRequest() } ?: emptyList()
                    trySend(requests)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up friend requests listener: ${e.message}", e)
            trySend(emptyList())
        }
        
        awaitClose {
            listenerRegistration?.remove()
        }
    }

    // Accept friend request
    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            // Get the request
            val requestDoc = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .document(requestId)
                .get()
                .await()

            val request = requestDoc.toFriendRequest()
                ?: return Result.failure(Exception("Friend request not found"))

            // Update request status
            firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .document(requestId)
                .update(
                    mapOf(
                        "status" to "ACCEPTED",
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            // Create friendship
            val friendship = Friendship(
                user1Id = request.fromUserId,
                user2Id = request.toUserId
            )

            firestore.collection(FRIENDSHIPS_COLLECTION)
                .add(friendship)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Reject friend request
    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .document(requestId)
                .update(
                    mapOf(
                        "status" to "REJECTED",
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting friend request: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Get friends list
    fun getFriends(userId: String): Flow<List<UserPublicProfile>> = callbackFlow {
        var friendshipsListener: ListenerRegistration? = null
        val profileListeners = mutableMapOf<String, ListenerRegistration>()
        
        try {
            // Listen to friendships collection
            friendshipsListener = firestore.collection(FRIENDSHIPS_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to friendships: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    
                    val friendships = snapshot?.documents?.mapNotNull { it.toFriendship() } ?: emptyList()
                    
                    // Get friend user IDs
                    val friendIds = friendships.mapNotNull { friendship ->
                        when (userId) {
                            friendship.user1Id -> friendship.user2Id
                            friendship.user2Id -> friendship.user1Id
                            else -> null
                        }
                    }
                    
                    // Remove old profile listeners for friends no longer in list
                    val removedFriends = profileListeners.keys - friendIds.toSet()
                    removedFriends.forEach { friendId ->
                        profileListeners[friendId]?.remove()
                        profileListeners.remove(friendId)
                    }
                    
                    // Fetch friend profiles with real-time updates
                    if (friendIds.isEmpty()) {
                        trySend(emptyList())
                    } else {
                        val profiles = mutableMapOf<String, UserPublicProfile>()
                        
                        // Set up real-time listener for each friend's profile
                        friendIds.forEach { friendId ->
                            if (!profileListeners.containsKey(friendId)) {
                                profileListeners[friendId] = firestore.collection(USER_PROFILES_COLLECTION)
                                    .document(friendId)
                                    .addSnapshotListener { profileDoc, profileError ->
                                        if (profileError != null) {
                                            Log.e(TAG, "Error listening to profile $friendId: ${profileError.message}")
                                            return@addSnapshotListener
                                        }
                                        
                                        profileDoc?.toUserPublicProfile()?.let { profile ->
                                            profiles[friendId] = profile
                                            // Send updated list whenever any profile changes
                                            trySend(profiles.values.toList())
                                        }
                                    }
                            }
                        }
                        
                        // Initial fetch for profiles we don't have yet
                        friendIds.chunked(10).forEach { chunk ->
                            firestore.collection(USER_PROFILES_COLLECTION)
                                .whereIn("__name__", chunk)
                                .get()
                                .addOnSuccessListener { profileSnapshot ->
                                    profileSnapshot.documents.forEach { doc ->
                                        doc.toUserPublicProfile()?.let { profile ->
                                            profiles[profile.userId] = profile
                                        }
                                    }
                                    trySend(profiles.values.toList())
                                }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up friendships listener: ${e.message}", e)
            trySend(emptyList())
        }
        
        awaitClose {
            friendshipsListener?.remove()
            profileListeners.values.forEach { it.remove() }
            profileListeners.clear()
        }
    }

    // Check if friendship exists
    private suspend fun checkFriendshipExists(userId1: String, userId2: String): Boolean {
        return try {
            val snapshot1 = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("user1Id", userId1)
                .whereEqualTo("user2Id", userId2)
                .get()
                .await()

            val snapshot2 = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("user1Id", userId2)
                .whereEqualTo("user2Id", userId1)
                .get()
                .await()

            !snapshot1.isEmpty || !snapshot2.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error checking friendship: ${e.message}", e)
            false
        }
    }

    // Update user public profile
    suspend fun updateUserPublicProfile(
        userId: String,
        email: String,
        displayName: String,
        photoUrl: String?,
        customAvatar: String,
        successRate: Int,
        totalHabits: Int,
        totalCompletions: Int,
        currentStreak: Int
    ): Result<Unit> {
        return try {
            val profile = UserPublicProfile(
                userId = userId,
                email = email,
                displayName = displayName,
                photoUrl = photoUrl,
                customAvatar = customAvatar,
                successRate = successRate,
                totalHabits = totalHabits,
                totalCompletions = totalCompletions,
                currentStreak = currentStreak
            )

            firestore.collection(USER_PROFILES_COLLECTION)
                .document(userId)
                .set(profile)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Get friend's public profile
    suspend fun getFriendProfile(friendId: String): UserPublicProfile? {
        return try {
            val snapshot = firestore.collection(USER_PROFILES_COLLECTION)
                .document(friendId)
                .get()
                .await()

            snapshot.toUserPublicProfile()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting friend profile: ${e.message}", e)
            null
        }
    }

    // Real-time friend profile observer
    fun observeFriendProfile(friendId: String): Flow<UserPublicProfile?> = callbackFlow {
        var listener: ListenerRegistration? = null
        
        try {
            listener = firestore.collection(USER_PROFILES_COLLECTION)
                .document(friendId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error observing friend profile: ${error.message}", error)
                        trySend(null)
                        return@addSnapshotListener
                    }
                    
                    val profile = snapshot?.toUserPublicProfile()
                    trySend(profile)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up friend profile listener: ${e.message}", e)
            trySend(null)
        }
        
        awaitClose {
            listener?.remove()
        }
    }

    // Get leaderboard (friends sorted by success rate)
    suspend fun getLeaderboard(userId: String): List<UserPublicProfile> {
        return try {
            // Get friends
            val friendshipsSnapshot = firestore.collection(FRIENDSHIPS_COLLECTION)
                .get()
                .await()

            val friendships = friendshipsSnapshot.documents.mapNotNull { it.toFriendship() }
            
            val friendIds = friendships.mapNotNull { friendship ->
                when (userId) {
                    friendship.user1Id -> friendship.user2Id
                    friendship.user2Id -> friendship.user1Id
                    else -> null
                }
            }.toMutableList()

            // Add current user
            friendIds.add(userId)

            if (friendIds.isEmpty()) {
                return emptyList()
            }

            // Get profiles and sort by success rate
            val profiles = mutableListOf<UserPublicProfile>()
            
            // Firestore has a limit of 10 for whereIn, so we batch
            friendIds.chunked(10).forEach { chunk ->
                val snapshot = firestore.collection(USER_PROFILES_COLLECTION)
                    .whereIn("__name__", chunk)
                    .get()
                    .await()
                
                profiles.addAll(snapshot.documents.mapNotNull { it.toUserPublicProfile() })
            }

            profiles.sortedByDescending { it.successRate }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting leaderboard: ${e.message}", e)
            emptyList()
        }
    }

    // Real-time leaderboard with live stats updates
    fun observeLeaderboard(userId: String): Flow<List<UserPublicProfile>> = callbackFlow {
        var friendshipsListener: ListenerRegistration? = null
        val profileListeners = mutableMapOf<String, ListenerRegistration>()
        
        try {
            // Listen to friendships collection
            friendshipsListener = firestore.collection(FRIENDSHIPS_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to friendships for leaderboard: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    
                    val friendships = snapshot?.documents?.mapNotNull { it.toFriendship() } ?: emptyList()
                    
                    // Get friend user IDs
                    val friendIds = friendships.mapNotNull { friendship ->
                        when (userId) {
                            friendship.user1Id -> friendship.user2Id
                            friendship.user2Id -> friendship.user1Id
                            else -> null
                        }
                    }.toMutableList()
                    
                    // Add current user to leaderboard
                    friendIds.add(userId)
                    
                    // Remove old profile listeners for users no longer in leaderboard
                    val removedUsers = profileListeners.keys - friendIds.toSet()
                    removedUsers.forEach { removedUserId ->
                        profileListeners[removedUserId]?.remove()
                        profileListeners.remove(removedUserId)
                    }
                    
                    // Fetch profiles with real-time updates
                    if (friendIds.isEmpty()) {
                        trySend(emptyList())
                    } else {
                        val profiles = mutableMapOf<String, UserPublicProfile>()
                        
                        // Set up real-time listener for each user's profile
                        friendIds.forEach { leaderboardUserId ->
                            if (!profileListeners.containsKey(leaderboardUserId)) {
                                profileListeners[leaderboardUserId] = firestore.collection(USER_PROFILES_COLLECTION)
                                    .document(leaderboardUserId)
                                    .addSnapshotListener { profileDoc, profileError ->
                                        if (profileError != null) {
                                            Log.e(TAG, "Error listening to profile $leaderboardUserId: ${profileError.message}")
                                            return@addSnapshotListener
                                        }
                                        
                                        profileDoc?.toUserPublicProfile()?.let { profile ->
                                            profiles[leaderboardUserId] = profile
                                            // Send sorted list whenever any profile changes
                                            val sortedProfiles = profiles.values.sortedByDescending { it.successRate }
                                            trySend(sortedProfiles)
                                        }
                                    }
                            }
                        }
                        
                        // Initial fetch for profiles we don't have yet
                        friendIds.chunked(10).forEach { chunk ->
                            firestore.collection(USER_PROFILES_COLLECTION)
                                .whereIn("__name__", chunk)
                                .get()
                                .addOnSuccessListener { profileSnapshot ->
                                    profileSnapshot.documents.forEach { doc ->
                                        doc.toUserPublicProfile()?.let { profile ->
                                            profiles[profile.userId] = profile
                                        }
                                    }
                                    val sortedProfiles = profiles.values.sortedByDescending { it.successRate }
                                    trySend(sortedProfiles)
                                }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up leaderboard listener: ${e.message}", e)
            trySend(emptyList())
        }
        
        awaitClose {
            friendshipsListener?.remove()
            profileListeners.values.forEach { it.remove() }
            profileListeners.clear()
        }
    }

    // Remove friend
    suspend fun removeFriend(userId: String, friendId: String): Result<Unit> {
        return try {
            val snapshot1 = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("user1Id", userId)
                .whereEqualTo("user2Id", friendId)
                .get()
                .await()

            val snapshot2 = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("user1Id", friendId)
                .whereEqualTo("user2Id", userId)
                .get()
                .await()

            snapshot1.documents.forEach { it.reference.delete().await() }
            snapshot2.documents.forEach { it.reference.delete().await() }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing friend: ${e.message}", e)
            Result.failure(e)
        }
    }
}
