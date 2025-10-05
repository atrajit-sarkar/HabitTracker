package it.atraj.habittracker.data.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object UserPresenceManager {
    private const val USERS_COLLECTION = "users"
    private const val ONLINE_THRESHOLD_MS = 2 * 60 * 1000L // 2 minutes
    
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // Update user online status
    suspend fun setOnlineStatus(isOnline: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        
        try {
            val data = mapOf(
                "isOnline" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Get user online status
    suspend fun getUserOnlineStatus(userId: String): Pair<Boolean, Long> {
        return try {
            val doc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            val isOnline = doc.getBoolean("isOnline") ?: false
            val lastSeen = doc.getLong("lastSeen") ?: 0L
            
            // Check if last seen is within threshold
            val currentTime = System.currentTimeMillis()
            val isActuallyOnline = isOnline && (currentTime - lastSeen < ONLINE_THRESHOLD_MS)
            
            Pair(isActuallyOnline, lastSeen)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, 0L)
        }
    }

    // Observe user online status
    fun observeUserOnlineStatus(userId: String, onStatusChanged: (Boolean, Long) -> Unit) {
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                
                val isOnline = snapshot.getBoolean("isOnline") ?: false
                val lastSeen = snapshot.getLong("lastSeen") ?: 0L
                
                val currentTime = System.currentTimeMillis()
                val isActuallyOnline = isOnline && (currentTime - lastSeen < ONLINE_THRESHOLD_MS)
                
                onStatusChanged(isActuallyOnline, lastSeen)
            }
    }

    // Save FCM token
    suspend fun saveFcmToken(token: String) {
        val userId = auth.currentUser?.uid ?: return
        
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Get user FCM token
    suspend fun getUserFcmToken(userId: String): String? {
        return try {
            val doc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            doc.getString("fcmToken")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
