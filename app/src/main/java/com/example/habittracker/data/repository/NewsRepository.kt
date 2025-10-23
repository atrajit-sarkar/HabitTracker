package it.atraj.habittracker.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import it.atraj.habittracker.data.local.AppNews
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val TAG = "NewsRepository"
    private val newsCollection = firestore.collection("app_news")
    
    /**
     * Get all news messages ordered by timestamp (newest first)
     */
    fun getNewsFlow(): Flow<List<AppNews>> = callbackFlow {
        val listener = newsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching news", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val newsList = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        AppNews.fromMap(doc.id, doc.data ?: emptyMap())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing news document", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(newsList)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get count of unread news messages
     */
    suspend fun getUnreadCount(): Int {
        return try {
            val userId = auth.currentUser?.uid ?: return 0
            val readNewsIds = getReadNewsIds(userId)
            
            val snapshot = newsCollection.get().await()
            val totalNews = snapshot.documents.size
            val unreadCount = totalNews - readNewsIds.size
            
            maxOf(0, unreadCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread count", e)
            0
        }
    }
    
    /**
     * Mark news as read for current user
     */
    suspend fun markAsRead(newsId: String) {
        try {
            val userId = auth.currentUser?.uid ?: return
            
            firestore.collection("users")
                .document(userId)
                .collection("read_news")
                .document(newsId)
                .set(mapOf(
                    "readAt" to System.currentTimeMillis(),
                    "newsId" to newsId
                ))
                .await()
                
            Log.d(TAG, "Marked news $newsId as read")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking news as read", e)
        }
    }
    
    /**
     * Get list of read news IDs for user
     */
    private suspend fun getReadNewsIds(userId: String): List<String> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("read_news")
                .get()
                .await()
            
            snapshot.documents.map { it.id }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting read news IDs", e)
            emptyList()
        }
    }
    
    /**
     * Check if user has read specific news
     */
    suspend fun isNewsRead(newsId: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val readNewsIds = getReadNewsIds(userId)
            newsId in readNewsIds
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if news is read", e)
            false
        }
    }
    
    /**
     * Mark all news as read
     */
    suspend fun markAllAsRead(newsList: List<AppNews>) {
        try {
            val userId = auth.currentUser?.uid ?: return
            
            newsList.forEach { news ->
                firestore.collection("users")
                    .document(userId)
                    .collection("read_news")
                    .document(news.id)
                    .set(mapOf(
                        "readAt" to System.currentTimeMillis(),
                        "newsId" to news.id
                    ))
                    .await()
            }
            
            Log.d(TAG, "Marked all news as read")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all news as read", e)
        }
    }
}
