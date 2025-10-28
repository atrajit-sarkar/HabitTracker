package it.atraj.habittracker.data.firestore

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
        var listener1: ListenerRegistration? = null
        var listener2: ListenerRegistration? = null
        val profileListeners = mutableMapOf<String, ListenerRegistration>()
        
        try {
            val allFriendships = mutableSetOf<Friendship>()
            
            // Helper function to update profiles
            fun updateProfiles(friendIds: List<String>, listeners: MutableMap<String, ListenerRegistration>) {
                // Remove old profile listeners for friends no longer in list
                val removedFriends = listeners.keys - friendIds.toSet()
                removedFriends.forEach { friendId ->
                    listeners[friendId]?.remove()
                    listeners.remove(friendId)
                }
                
                // Fetch friend profiles with real-time updates
                if (friendIds.isEmpty()) {
                    trySend(emptyList())
                } else {
                    val profiles = mutableMapOf<String, UserPublicProfile>()
                    
                    // Set up real-time listener for each friend's profile
                    friendIds.forEach { friendId ->
                        if (!listeners.containsKey(friendId)) {
                            listeners[friendId] = firestore.collection(USER_PROFILES_COLLECTION)
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
            
            // Listen to friendships where user is user1Id
            listener1 = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("user1Id", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to friendships (user1): ${error.message}", error)
                        return@addSnapshotListener
                    }
                    
                    val friendships = snapshot?.documents?.mapNotNull { it.toFriendship() } ?: emptyList()
                    
                    // Add to set (will update existing)
                    allFriendships.removeAll { it.user1Id == userId || it.user2Id == userId }
                    allFriendships.addAll(friendships)
                    
                    // Get friend user IDs from combined set
                    val friendIds = allFriendships.mapNotNull { friendship ->
                        when (userId) {
                            friendship.user1Id -> friendship.user2Id
                            friendship.user2Id -> friendship.user1Id
                            else -> null
                        }
                    }
                    
                    updateProfiles(friendIds, profileListeners)
                }
            
            // Listen to friendships where user is user2Id
            listener2 = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("user2Id", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to friendships (user2): ${error.message}", error)
                        return@addSnapshotListener
                    }
                    
                    val friendships = snapshot?.documents?.mapNotNull { it.toFriendship() } ?: emptyList()
                    
                    // Add to set (will update existing)
                    allFriendships.removeAll { it.user1Id == userId || it.user2Id == userId }
                    allFriendships.addAll(friendships)
                    
                    // Get friend user IDs from combined set
                    val friendIds = allFriendships.mapNotNull { friendship ->
                        when (userId) {
                            friendship.user1Id -> friendship.user2Id
                            friendship.user2Id -> friendship.user1Id
                            else -> null
                        }
                    }
                    
                    updateProfiles(friendIds, profileListeners)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up friendships listener: ${e.message}", e)
            trySend(emptyList())
        }
        
        awaitClose {
            listener1?.remove()
            listener2?.remove()
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
        customAvatar: String?, // Nullable - null means no custom avatar set
        successRate: Int,
        totalHabits: Int,
        totalCompletions: Int,
        currentStreak: Int,
        leaderboardScore: Int
    ): Result<Unit> {
        return try {
            // Fetch existing profile to preserve displayName and photoUrl if new ones are empty
            val existingProfile = firestore.collection(USER_PROFILES_COLLECTION)
                .document(userId)
                .get()
                .await()
                .toUserPublicProfile()
            
            // Preserve existing displayName if new one is empty, blank, or default "User"
            val finalDisplayName = if (displayName.isBlank() || displayName == "User") {
                existingProfile?.displayName?.takeIf { it.isNotBlank() } ?: displayName
            } else {
                displayName
            }
            
            // Preserve existing photoUrl if new one is null
            val finalPhotoUrl = photoUrl ?: existingProfile?.photoUrl
            
            // Preserve existing customAvatar if new one is null
            val finalCustomAvatar = customAvatar ?: existingProfile?.customAvatar
            
            Log.d(TAG, "updateUserPublicProfile: Updating profile for $userId")
            Log.d(TAG, "  - displayName: '$displayName' -> '$finalDisplayName' (existing: '${existingProfile?.displayName}')")
            Log.d(TAG, "  - photoUrl: $photoUrl -> $finalPhotoUrl")
            Log.d(TAG, "  - customAvatar: $customAvatar -> $finalCustomAvatar")
            
            val profile = UserPublicProfile(
                userId = userId,
                email = email,
                displayName = finalDisplayName,
                photoUrl = finalPhotoUrl,
                customAvatar = finalCustomAvatar,
                successRate = successRate,
                totalHabits = totalHabits,
                totalCompletions = totalCompletions,
                currentStreak = currentStreak,
                leaderboardScore = leaderboardScore
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

    /**
     * Update ONLY user stats without touching displayName, photoUrl, or customAvatar
     * This is safer for periodic stats updates as it preserves user profile data
     */
    suspend fun updateUserStats(
        userId: String,
        successRate: Int,
        totalHabits: Int,
        totalCompletions: Int,
        currentStreak: Int,
        leaderboardScore: Int,
        completedThisWeek: Int
    ): Result<Unit> {
        return try {
            val profileDoc = firestore.collection(USER_PROFILES_COLLECTION).document(userId)
            
            // Check if profile exists
            val snapshot = profileDoc.get().await()
            
            if (!snapshot.exists()) {
                Log.w(TAG, "updateUserStats: Profile doesn't exist for $userId, cannot update stats")
                return Result.failure(Exception("Profile does not exist"))
            }
            
            // Only update stats fields, preserving all other data
            val updates = hashMapOf<String, Any>(
                "successRate" to successRate,
                "totalHabits" to totalHabits,
                "totalCompletions" to totalCompletions,
                "currentStreak" to currentStreak,
                "leaderboardScore" to leaderboardScore,
                "completedThisWeek" to completedThisWeek,
                "updatedAt" to System.currentTimeMillis()
            )
            
            profileDoc.update(updates).await()
            
            Log.d(TAG, "updateUserStats: Stats updated for $userId - SR: $successRate%, Habits: $totalHabits, Score: $leaderboardScore, ThisWeek: $completedThisWeek")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user stats: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Update only avatar/photoUrl in user profile (partial update)
    suspend fun updateUserAvatar(
        userId: String,
        photoUrl: String?,
        customAvatar: String?
    ): Result<Unit> {
        return try {
            // First check if the profile exists
            val profileDoc = firestore.collection(USER_PROFILES_COLLECTION)
                .document(userId)
            
            val snapshot = profileDoc.get().await()
            
            if (snapshot.exists()) {
                // Profile exists, do a partial update
                val updates = mutableMapOf<String, Any?>()
                updates["photoUrl"] = photoUrl
                updates["customAvatar"] = customAvatar // Can be null, URL, or emoji
                updates["updatedAt"] = System.currentTimeMillis()
                
                profileDoc.update(updates).await()
                Log.d(TAG, "User avatar updated in existing profile - photoUrl: $photoUrl, customAvatar: $customAvatar")
            } else {
                // Profile doesn't exist yet, get user info from auth
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val email = currentUser?.email ?: ""
                val displayName = currentUser?.displayName ?: ""
                
                // Create minimal profile
                val profile = hashMapOf(
                    "userId" to userId,
                    "email" to email,
                    "displayName" to displayName,
                    "photoUrl" to photoUrl,
                    "customAvatar" to customAvatar,
                    "successRate" to 0,
                    "totalHabits" to 0,
                    "totalCompletions" to 0,
                    "currentStreak" to 0,
                    "leaderboardScore" to 0,
                    "completedThisWeek" to 0,
                    "updatedAt" to System.currentTimeMillis()
                )
                profileDoc.set(profile).await()
                Log.d(TAG, "User avatar saved in new profile - photoUrl: $photoUrl, customAvatar: $customAvatar")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user avatar: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Update only display name in user profile (partial update)
    suspend fun updateUserDisplayName(
        userId: String,
        displayName: String
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "displayName" to displayName,
                "updatedAt" to System.currentTimeMillis()
            )
            
            firestore.collection(USER_PROFILES_COLLECTION)
                .document(userId)
                .update(updates)
                .await()
            
            Log.d(TAG, "User display name updated in public profile: $displayName")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user display name: ${e.message}", e)
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
    }    // Real-time friend profile observer
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
            // Get friendships where user is user1Id
            val snapshot1 = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("user1Id", userId)
                .get()
                .await()

            // Get friendships where user is user2Id
            val snapshot2 = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("user2Id", userId)
                .get()
                .await()

            val friendships = (snapshot1.documents + snapshot2.documents).mapNotNull { it.toFriendship() }
            
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

            // Get profiles and sort by leaderboard score (higher is better)
            val profiles = mutableListOf<UserPublicProfile>()
            
            // Firestore has a limit of 10 for whereIn, so we batch
            friendIds.chunked(10).forEach { chunk ->
                val snapshot = firestore.collection(USER_PROFILES_COLLECTION)
                    .whereIn("__name__", chunk)
                    .get()
                    .await()
                
                profiles.addAll(snapshot.documents.mapNotNull { it.toUserPublicProfile() })
            }

            profiles.sortedByDescending { it.leaderboardScore }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting leaderboard: ${e.message}", e)
            emptyList()
        }
    }

    // Real-time leaderboard with live stats updates
    fun observeLeaderboard(userId: String): Flow<List<UserPublicProfile>> = callbackFlow {
        var listener1: ListenerRegistration? = null
        var listener2: ListenerRegistration? = null
        val profileListeners = mutableMapOf<String, ListenerRegistration>()
        
        try {
            val allFriendships = mutableSetOf<Friendship>()
            
            // Helper function to update leaderboard
            fun updateLeaderboard() {
                // Get friend user IDs
                val friendIds = allFriendships.mapNotNull { friendship ->
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
                                        // Send sorted list whenever any profile changes (sorted by leaderboard score)
                                        val sortedProfiles = profiles.values.sortedByDescending { it.leaderboardScore }
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
                                val sortedProfiles = profiles.values.sortedByDescending { it.leaderboardScore }
                                trySend(sortedProfiles)
                            }
                    }
                }
            }
            
            // Listen to friendships where user is user1Id
            listener1 = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("user1Id", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to friendships (user1) for leaderboard: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    
                    val friendships = snapshot?.documents?.mapNotNull { it.toFriendship() } ?: emptyList()
                    
                    // Update set with new friendships
                    allFriendships.removeAll { it.user1Id == userId }
                    allFriendships.addAll(friendships)
                    
                    updateLeaderboard()
                }
            
            // Listen to friendships where user is user2Id
            listener2 = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("user2Id", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to friendships (user2) for leaderboard: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    
                    val friendships = snapshot?.documents?.mapNotNull { it.toFriendship() } ?: emptyList()
                    
                    // Update set with new friendships
                    allFriendships.removeAll { it.user2Id == userId }
                    allFriendships.addAll(friendships)
                    
                    updateLeaderboard()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up leaderboard listener: ${e.message}", e)
            trySend(emptyList())
        }
        
        awaitClose {
            listener1?.remove()
            listener2?.remove()
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

    // Get friends count (real-time)
    fun getFriendsCount(userId: String): Flow<Int> = callbackFlow {
        var listener1: ListenerRegistration? = null
        var listener2: ListenerRegistration? = null
        
        try {
            var count1 = 0
            var count2 = 0
            
            // Listen to friendships where user is user1Id
            listener1 = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("user1Id", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to friendships count (user1): ${error.message}", error)
                        return@addSnapshotListener
                    }
                    
                    count1 = snapshot?.size() ?: 0
                    trySend(count1 + count2)
                }
            
            // Listen to friendships where user is user2Id
            listener2 = firestore.collection(FRIENDSHIPS_COLLECTION)
                .whereEqualTo("user2Id", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to friendships count (user2): ${error.message}", error)
                        return@addSnapshotListener
                    }
                    
                    count2 = snapshot?.size() ?: 0
                    trySend(count1 + count2)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up friends count listener: ${e.message}", e)
            trySend(0)
        }
        
        awaitClose {
            listener1?.remove()
            listener2?.remove()
        }
    }
}
