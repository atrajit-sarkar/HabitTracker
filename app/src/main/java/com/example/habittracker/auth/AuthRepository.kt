package it.atraj.habittracker.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthRepository"
private const val USERS_COLLECTION = "users"
private const val CUSTOM_AVATAR_FIELD = "customAvatar"
private const val CUSTOM_DISPLAY_NAME_FIELD = "customDisplayName"

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    
    val currentUser: Flow<User?> = callbackFlow {
        var userDocListener: com.google.firebase.firestore.ListenerRegistration? = null
        
        val authListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            
            // Remove previous Firestore listener if exists
            userDocListener?.remove()
            
            if (firebaseUser != null) {
                // Set up real-time Firestore listener for avatar changes
                userDocListener = firestore.collection(USERS_COLLECTION)
                    .document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Error listening to user data changes", error)
                            trySend(firebaseUser.toUser(null, null))
                            return@addSnapshotListener
                        }
                        
                        val customAvatar = snapshot?.getString(CUSTOM_AVATAR_FIELD)
                        val customDisplayName = snapshot?.getString(CUSTOM_DISPLAY_NAME_FIELD)
                        Log.d(TAG, "User data snapshot updated - Avatar: $customAvatar, Name: $customDisplayName")
                        trySend(firebaseUser.toUser(customAvatar, customDisplayName))
                    }
            } else {
                trySend(null)
            }
        }
        
        firebaseAuth.addAuthStateListener(authListener)
        
        awaitClose {
            userDocListener?.remove()
            firebaseAuth.removeAuthStateListener(authListener)
        }
    }
    
    val currentUserSync: User?
        get() = firebaseAuth.currentUser?.toUser(null, null)
    
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign in failed")
        }
    }
    
    suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            
            // For email/password users, initialize with empty name (they'll set it later)
            val userId = authResult.user?.uid
            if (userId != null) {
                val userDoc = firestore.collection(USERS_COLLECTION).document(userId)
                userDoc.set(mapOf(
                    CUSTOM_DISPLAY_NAME_FIELD to "" // Empty name for email users to set later
                ), com.google.firebase.firestore.SetOptions.merge()).await()
                Log.d(TAG, "Initialized user document for email user")
            }
            
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign up failed")
        }
    }
    
    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            Log.d(TAG, "Starting Google sign-in with ID token")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            
            Log.d(TAG, "Google sign-in successful for user: ${authResult.user?.uid}")
            
            // For Google users, save their Google display name as custom name on first sign-in
            val userId = authResult.user?.uid
            val googleDisplayName = authResult.user?.displayName
            
            if (userId != null && googleDisplayName != null) {
                val userDoc = firestore.collection(USERS_COLLECTION).document(userId)
                val snapshot = userDoc.get().await()
                
                // Only set name if it's the first time (document doesn't exist or has no name)
                if (!snapshot.exists() || !snapshot.contains(CUSTOM_DISPLAY_NAME_FIELD)) {
                    userDoc.set(mapOf(
                        CUSTOM_DISPLAY_NAME_FIELD to googleDisplayName
                    ), com.google.firebase.firestore.SetOptions.merge()).await()
                    Log.d(TAG, "Initialized user document for Google user with name: $googleDisplayName")
                } else {
                    Log.d(TAG, "User document already exists for Google user")
                }
            }
            
            AuthResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            AuthResult.Error(e.message ?: "Google sign in failed")
        }
    }
    
    suspend fun signOut() {
        firebaseAuth.signOut()
    }
    
    suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Password reset failed")
        }
    }
    
    suspend fun updateCustomAvatar(avatar: String?): AuthResult {
        return try {
            val userId = firebaseAuth.currentUser?.uid 
                ?: return AuthResult.Error("No user signed in")
            
            val userDoc = firestore.collection(USERS_COLLECTION).document(userId)
            
            if (avatar != null) {
                userDoc.set(mapOf(CUSTOM_AVATAR_FIELD to avatar), 
                    com.google.firebase.firestore.SetOptions.merge()).await()
                Log.d(TAG, "Custom avatar updated to: $avatar")
            } else {
                // Reset to default by removing the field
                userDoc.update(CUSTOM_AVATAR_FIELD, com.google.firebase.firestore.FieldValue.delete()).await()
                Log.d(TAG, "Custom avatar reset to default")
            }
            
            AuthResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update custom avatar", e)
            AuthResult.Error(e.message ?: "Failed to update avatar")
        }
    }
    
    suspend fun updateDisplayName(name: String): AuthResult {
        return try {
            val userId = firebaseAuth.currentUser?.uid 
                ?: return AuthResult.Error("No user signed in")
            
            if (name.isBlank()) {
                return AuthResult.Error("Name cannot be empty")
            }
            
            val userDoc = firestore.collection(USERS_COLLECTION).document(userId)
            userDoc.set(mapOf(CUSTOM_DISPLAY_NAME_FIELD to name.trim()), 
                com.google.firebase.firestore.SetOptions.merge()).await()
            Log.d(TAG, "Display name updated to: $name")
            
            AuthResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update display name", e)
            AuthResult.Error(e.message ?: "Failed to update name")
        }
    }
    
    private fun FirebaseUser.toUser(customAvatar: String?, customDisplayName: String?): User {
        return User(
            uid = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl?.toString(),
            customAvatar = customAvatar,
            customDisplayName = customDisplayName
        )
    }
}
