package com.example.habittracker.auth

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
                            Log.e(TAG, "Error listening to avatar changes", error)
                            trySend(firebaseUser.toUser(null))
                            return@addSnapshotListener
                        }
                        
                        val customAvatar = snapshot?.getString(CUSTOM_AVATAR_FIELD)
                        Log.d(TAG, "Avatar snapshot updated: $customAvatar")
                        trySend(firebaseUser.toUser(customAvatar))
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
        get() = firebaseAuth.currentUser?.toUser(null)
    
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
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign up failed")
        }
    }
    
    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            AuthResult.Success
        } catch (e: Exception) {
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
    
    private fun FirebaseUser.toUser(customAvatar: String?): User {
        return User(
            uid = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl?.toString(),
            customAvatar = customAvatar
        )
    }
}