package com.example.habittracker.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSignInHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    val signInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("328687985941-k0kg0hmcui7jlt3o31dh4a35qbe3is5d.apps.googleusercontent.com") // Web client ID from google-services.json
            .requestEmail()
            .build()
        
        GoogleSignIn.getClient(context, gso)
    }
    
    suspend fun signOut() {
        signInClient.signOut()
    }
}