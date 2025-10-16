package it.atraj.habittracker.data.firestore

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import it.atraj.habittracker.auth.AuthRepository
import it.atraj.habittracker.data.local.UserRewards
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRewardsRepository"
private const val USERS_COLLECTION = "users"
private const val DIAMONDS_FIELD = "diamonds"
private const val FREEZE_DAYS_FIELD = "freeze_days"

@Singleton
class UserRewardsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) {
    
    /**
     * Observe user rewards in real-time
     */
    fun observeUserRewards(): Flow<UserRewards> =
        authRepository.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf(UserRewards())
            } else {
                callbackFlow {
                    val listener = firestore.collection(USERS_COLLECTION)
                        .document(user.uid)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.e(TAG, "Error observing user rewards", error)
                                trySend(UserRewards())
                                return@addSnapshotListener
                            }
                            
                            val diamonds = (snapshot?.get(DIAMONDS_FIELD) as? Long)?.toInt() ?: 0
                            val freezeDays = (snapshot?.get(FREEZE_DAYS_FIELD) as? Long)?.toInt() ?: 0
                            
                            trySend(UserRewards(diamonds = diamonds, freezeDays = freezeDays))
                        }
                    
                    awaitClose { listener.remove() }
                }
            }
        }
    
    /**
     * Get current user rewards (synchronous)
     */
    suspend fun getUserRewards(): UserRewards {
        val userId = authRepository.currentUserSync?.uid ?: return UserRewards()
        
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            val diamonds = (snapshot.get(DIAMONDS_FIELD) as? Long)?.toInt() ?: 0
            val freezeDays = (snapshot.get(FREEZE_DAYS_FIELD) as? Long)?.toInt() ?: 0
            
            UserRewards(diamonds = diamonds, freezeDays = freezeDays)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user rewards", e)
            UserRewards()
        }
    }
    
    /**
     * Add diamonds to user account
     */
    suspend fun addDiamonds(amount: Int): Boolean {
        val userId = authRepository.currentUserSync?.uid ?: return false
        
        return try {
            firestore.runTransaction { transaction ->
                val docRef = firestore.collection(USERS_COLLECTION).document(userId)
                val snapshot = transaction.get(docRef)
                
                val currentDiamonds = (snapshot.get(DIAMONDS_FIELD) as? Long)?.toInt() ?: 0
                val newDiamonds = currentDiamonds + amount
                
                transaction.update(docRef, DIAMONDS_FIELD, newDiamonds)
            }.await()
            
            Log.d(TAG, "Added $amount diamonds to user account")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding diamonds", e)
            false
        }
    }
    
    /**
     * Purchase freeze days with diamonds
     * Returns true if purchase successful, false if not enough diamonds
     */
    suspend fun purchaseFreezeDays(days: Int, cost: Int): Boolean {
        val userId = authRepository.currentUserSync?.uid ?: return false
        
        return try {
            firestore.runTransaction { transaction ->
                val docRef = firestore.collection(USERS_COLLECTION).document(userId)
                val snapshot = transaction.get(docRef)
                
                val currentDiamonds = (snapshot.get(DIAMONDS_FIELD) as? Long)?.toInt() ?: 0
                val currentFreezeDays = (snapshot.get(FREEZE_DAYS_FIELD) as? Long)?.toInt() ?: 0
                
                // Check if user has enough diamonds
                if (currentDiamonds < cost) {
                    throw IllegalStateException("Not enough diamonds")
                }
                
                val newDiamonds = currentDiamonds - cost
                val newFreezeDays = currentFreezeDays + days
                
                transaction.update(docRef, mapOf(
                    DIAMONDS_FIELD to newDiamonds,
                    FREEZE_DAYS_FIELD to newFreezeDays
                ))
            }.await()
            
            Log.d(TAG, "Purchased $days freeze days for $cost diamonds")
            true
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Purchase failed: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error purchasing freeze days", e)
            false
        }
    }
    
    /**
     * Use one freeze day from the pool
     * Returns true if freeze day was available and used
     */
    suspend fun useFreezeDayIfAvailable(): Boolean {
        val userId = authRepository.currentUserSync?.uid ?: return false
        
        return try {
            firestore.runTransaction { transaction ->
                val docRef = firestore.collection(USERS_COLLECTION).document(userId)
                val snapshot = transaction.get(docRef)
                
                val currentFreezeDays = (snapshot.get(FREEZE_DAYS_FIELD) as? Long)?.toInt() ?: 0
                
                // Check if freeze day is available
                if (currentFreezeDays <= 0) {
                    throw IllegalStateException("No freeze days available")
                }
                
                val newFreezeDays = currentFreezeDays - 1
                transaction.update(docRef, FREEZE_DAYS_FIELD, newFreezeDays)
            }.await()
            
            Log.d(TAG, "Used one freeze day")
            true
        } catch (e: IllegalStateException) {
            Log.d(TAG, "No freeze days available")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error using freeze day", e)
            false
        }
    }
    
    /**
     * Initialize user rewards if not already set
     */
    suspend fun initializeUserRewards() {
        val userId = authRepository.currentUserSync?.uid ?: return
        
        try {
            val docRef = firestore.collection(USERS_COLLECTION).document(userId)
            val snapshot = docRef.get().await()
            
            // Only initialize if fields don't exist
            if (!snapshot.contains(DIAMONDS_FIELD)) {
                docRef.update(DIAMONDS_FIELD, 0).await()
            }
            if (!snapshot.contains(FREEZE_DAYS_FIELD)) {
                docRef.update(FREEZE_DAYS_FIELD, 0).await()
            }
            
            Log.d(TAG, "Initialized user rewards")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing user rewards", e)
        }
    }
}
