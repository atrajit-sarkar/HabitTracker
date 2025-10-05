package it.atraj.habittracker.avatar

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for GitHub token using Android's EncryptedSharedPreferences
 * 
 * USAGE:
 * 1. Add dependency to build.gradle.kts:
 *    implementation("androidx.security:security-crypto:1.1.0-alpha06")
 * 
 * 2. Store token once (e.g., after user enters it or on first setup):
 *    SecureTokenStorage.storeToken(context, "ghp_your_token_here")
 * 
 * 3. Retrieve and use token:
 *    val token = SecureTokenStorage.getToken(context)
 *    if (token != null) {
 *        avatarConfig.initialize(token)
 *    }
 */
object SecureTokenStorage {
    
    private const val TAG = "SecureTokenStorage"
    private const val PREFS_FILE_NAME = "avatar_secure_prefs"
    private const val KEY_GITHUB_TOKEN = "github_token"
    
    /**
     * Store GitHub token securely
     */
    fun storeToken(context: Context, token: String): Boolean {
        return try {
            val prefs = getEncryptedPrefs(context)
            prefs.edit()
                .putString(KEY_GITHUB_TOKEN, token)
                .apply()
            Log.d(TAG, "Token stored securely")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store token securely", e)
            false
        }
    }
    
    /**
     * Retrieve GitHub token
     */
    fun getToken(context: Context): String? {
        return try {
            val prefs = getEncryptedPrefs(context)
            prefs.getString(KEY_GITHUB_TOKEN, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve token", e)
            null
        }
    }
    
    /**
     * Clear stored token
     */
    fun clearToken(context: Context): Boolean {
        return try {
            val prefs = getEncryptedPrefs(context)
            prefs.edit()
                .remove(KEY_GITHUB_TOKEN)
                .apply()
            Log.d(TAG, "Token cleared")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear token", e)
            false
        }
    }
    
    /**
     * Check if token exists
     */
    fun hasToken(context: Context): Boolean {
        return try {
            val token = getToken(context)
            !token.isNullOrEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check token existence", e)
            false
        }
    }
    
    /**
     * Get or create encrypted SharedPreferences
     */
    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        // Create or retrieve master key
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        // Create encrypted SharedPreferences
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}

/**
 * EXAMPLE USAGE IN MAINACTIVITY:
 * 
 * class MainActivity : ComponentActivity() {
 *     
 *     @Inject
 *     lateinit var avatarConfig: AvatarConfig
 *     
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         
 *         // Initialize avatar feature with secure token
 *         initializeAvatarFeature()
 *     }
 *     
 *     private fun initializeAvatarFeature() {
 *         // Try to load token from secure storage
 *         val token = SecureTokenStorage.getToken(this)
 *         
 *         if (token != null) {
 *             // Token exists, initialize feature
 *             avatarConfig.initialize(token)
 *             Log.d("MainActivity", "Avatar feature initialized with stored token")
 *         } else {
 *             // No token stored, need to set it up
 *             // Option 1: Store it once during setup
 *             val githubToken = "ghp_YOUR_TOKEN_HERE" // Get from secure source
 *             SecureTokenStorage.storeToken(this, githubToken)
 *             avatarConfig.initialize(githubToken)
 *             
 *             // Option 2: Show setup dialog to user
 *             // showTokenSetupDialog()
 *         }
 *     }
 * }
 */

/**
 * EXAMPLE: Token Setup Dialog (Optional)
 * 
 * @Composable
 * fun TokenSetupDialog(
 *     onTokenEntered: (String) -> Unit,
 *     onDismiss: () -> Unit
 * ) {
 *     var token by remember { mutableStateOf("") }
 *     
 *     AlertDialog(
 *         onDismissRequest = onDismiss,
 *         title = { Text("Setup Avatar Upload") },
 *         text = {
 *             Column {
 *                 Text("Enter your GitHub Personal Access Token:")
 *                 Spacer(modifier = Modifier.height(8.dp))
 *                 OutlinedTextField(
 *                     value = token,
 *                     onValueChange = { token = it },
 *                     label = { Text("GitHub Token") },
 *                     singleLine = true
 *                 )
 *             }
 *         },
 *         confirmButton = {
 *             Button(
 *                 onClick = { onTokenEntered(token) },
 *                 enabled = token.isNotEmpty()
 *             ) {
 *                 Text("Save")
 *             }
 *         },
 *         dismissButton = {
 *             TextButton(onClick = onDismiss) {
 *                 Text("Cancel")
 *             }
 *         }
 *     )
 * }
 */
