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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRewardsRepository"
private const val USERS_COLLECTION = "users"
private const val DIAMONDS_FIELD = "diamonds"
private const val FREEZE_DAYS_FIELD = "freeze_days"
private const val FIRST_FREEZE_PURCHASE_DATE_FIELD = "first_freeze_purchase_date"
private const val PURCHASED_THEMES_FIELD = "purchased_themes"
private const val PURCHASED_HERO_BACKGROUNDS_FIELD = "purchased_hero_backgrounds"

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
                            
                            // Parse first freeze purchase date
                            val firstFreezePurchaseDate = try {
                                (snapshot?.get(FIRST_FREEZE_PURCHASE_DATE_FIELD) as? com.google.firebase.Timestamp)
                                    ?.toDate()
                                    ?.toInstant()
                                    ?.atZone(ZoneId.systemDefault())
                                    ?.toLocalDate()
                            } catch (e: Exception) {
                                null
                            }
                            
                            trySend(UserRewards(
                                diamonds = diamonds, 
                                freezeDays = freezeDays,
                                firstFreezePurchaseDate = firstFreezePurchaseDate
                            ))
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
            
            // Parse first freeze purchase date
            val firstFreezePurchaseDate = try {
                (snapshot.get(FIRST_FREEZE_PURCHASE_DATE_FIELD) as? com.google.firebase.Timestamp)
                    ?.toDate()
                    ?.toInstant()
                    ?.atZone(ZoneId.systemDefault())
                    ?.toLocalDate()
            } catch (e: Exception) {
                null
            }
            
            UserRewards(
                diamonds = diamonds, 
                freezeDays = freezeDays,
                firstFreezePurchaseDate = firstFreezePurchaseDate
            )
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
                
                val updates = mutableMapOf<String, Any>(
                    DIAMONDS_FIELD to newDiamonds,
                    FREEZE_DAYS_FIELD to newFreezeDays
                )
                
                // Set first purchase date if this is the first time purchasing
                if (!snapshot.contains(FIRST_FREEZE_PURCHASE_DATE_FIELD) || snapshot.get(FIRST_FREEZE_PURCHASE_DATE_FIELD) == null) {
                    val now = com.google.firebase.Timestamp.now()
                    updates[FIRST_FREEZE_PURCHASE_DATE_FIELD] = now
                    Log.d(TAG, "Setting first freeze purchase date to $now")
                }
                
                transaction.update(docRef, updates)
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
            
            // Initialize purchased themes with DEFAULT theme
            if (!snapshot.contains(PURCHASED_THEMES_FIELD)) {
                docRef.update(PURCHASED_THEMES_FIELD, listOf("DEFAULT")).await()
            }
            
            // MIGRATION: For existing users who have freeze days but no purchase date
            // Set the purchase date to a past date (e.g., account creation or a reasonable fallback)
            // This ensures backward compatibility
            val freezeDays = (snapshot.get(FREEZE_DAYS_FIELD) as? Long)?.toInt() ?: 0
            val hasPurchaseDate = snapshot.contains(FIRST_FREEZE_PURCHASE_DATE_FIELD) && 
                                  snapshot.get(FIRST_FREEZE_PURCHASE_DATE_FIELD) != null
            
            if (freezeDays > 0 && !hasPurchaseDate) {
                // Set to today's date for legacy users (August 24, 2025)
                // This ensures their existing freeze days work on all dates from today onwards
                val legacyDate = LocalDate.of(2025, 8, 24)
                val timestamp = com.google.firebase.Timestamp(
                    legacyDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(),
                    0
                )
                docRef.update(FIRST_FREEZE_PURCHASE_DATE_FIELD, timestamp).await()
                Log.d(TAG, "Migrated legacy user with $freezeDays freeze days - set purchase date to $legacyDate")
            }
            
            Log.d(TAG, "Initialized user rewards")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing user rewards", e)
        }
    }
    
    /**
     * Get list of purchased themes for current user
     */
    suspend fun getPurchasedThemes(): List<String> {
        val userId = authRepository.currentUserSync?.uid ?: return listOf("DEFAULT")
        
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val themes = snapshot.get(PURCHASED_THEMES_FIELD) as? List<String> ?: listOf("DEFAULT")
            
            // Ensure DEFAULT is always included
            if (!themes.contains("DEFAULT")) {
                themes + "DEFAULT"
            } else {
                themes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting purchased themes", e)
            listOf("DEFAULT")
        }
    }
    
    /**
     * Purchase a theme with diamonds
     * Returns true if purchase successful, false if not enough diamonds or already purchased
     */
    suspend fun purchaseTheme(themeId: String, cost: Int): Boolean {
        val userId = authRepository.currentUserSync?.uid ?: return false
        
        return try {
            firestore.runTransaction { transaction ->
                val docRef = firestore.collection(USERS_COLLECTION).document(userId)
                val snapshot = transaction.get(docRef)
                
                // Check current diamonds
                val currentDiamonds = (snapshot.get(DIAMONDS_FIELD) as? Long)?.toInt() ?: 0
                
                if (currentDiamonds < cost) {
                    throw IllegalStateException("Not enough diamonds")
                }
                
                // Check if already purchased
                @Suppress("UNCHECKED_CAST")
                val purchasedThemes = (snapshot.get(PURCHASED_THEMES_FIELD) as? List<String>) 
                    ?: listOf("DEFAULT")
                
                if (purchasedThemes.contains(themeId)) {
                    throw IllegalStateException("Theme already purchased")
                }
                
                // Deduct diamonds and add theme
                val newDiamonds = currentDiamonds - cost
                val newPurchasedThemes = purchasedThemes + themeId
                
                transaction.update(docRef, DIAMONDS_FIELD, newDiamonds)
                transaction.update(docRef, PURCHASED_THEMES_FIELD, newPurchasedThemes)
            }.await()
            
            Log.d(TAG, "Purchased theme $themeId for $cost diamonds")
            true
        } catch (e: IllegalStateException) {
            Log.d(TAG, "Theme purchase failed: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error purchasing theme", e)
            false
        }
    }
    
    /**
     * Get list of purchased hero backgrounds for current user
     */
    suspend fun getPurchasedHeroBackgrounds(): List<String> {
        val userId = authRepository.currentUserSync?.uid ?: return listOf("itachi")
        
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val backgrounds = snapshot.get(PURCHASED_HERO_BACKGROUNDS_FIELD) as? List<String> ?: listOf("itachi")
            
            // Ensure itachi is always included (free default)
            if (!backgrounds.contains("itachi")) {
                backgrounds + "itachi"
            } else {
                backgrounds
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting purchased hero backgrounds", e)
            listOf("itachi")
        }
    }
    
    /**
     * Purchase a hero background with diamonds
     * Returns true if purchase successful, false if not enough diamonds or already purchased
     */
    suspend fun purchaseHeroBackground(heroId: String, cost: Int): Boolean {
        val userId = authRepository.currentUserSync?.uid ?: return false
        
        return try {
            firestore.runTransaction { transaction ->
                val docRef = firestore.collection(USERS_COLLECTION).document(userId)
                val snapshot = transaction.get(docRef)
                
                // Check current diamonds
                val currentDiamonds = (snapshot.get(DIAMONDS_FIELD) as? Long)?.toInt() ?: 0
                
                if (currentDiamonds < cost) {
                    throw IllegalStateException("Not enough diamonds")
                }
                
                // Check if already purchased
                @Suppress("UNCHECKED_CAST")
                val purchasedBackgrounds = (snapshot.get(PURCHASED_HERO_BACKGROUNDS_FIELD) as? List<String>) 
                    ?: listOf("itachi")
                
                if (purchasedBackgrounds.contains(heroId)) {
                    throw IllegalStateException("Hero background already purchased")
                }
                
                // Deduct diamonds and add hero background
                val newDiamonds = currentDiamonds - cost
                val newPurchasedBackgrounds = purchasedBackgrounds + heroId
                
                transaction.update(docRef, DIAMONDS_FIELD, newDiamonds)
                transaction.update(docRef, PURCHASED_HERO_BACKGROUNDS_FIELD, newPurchasedBackgrounds)
            }.await()
            
            Log.d(TAG, "Purchased hero background $heroId for $cost diamonds")
            true
        } catch (e: IllegalStateException) {
            Log.d(TAG, "Hero background purchase failed: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error purchasing hero background", e)
            false
        }
    }
}
