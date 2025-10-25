package it.atraj.habittracker.social.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import it.atraj.habittracker.BuildConfig
import it.atraj.habittracker.social.data.model.Comment
import it.atraj.habittracker.social.data.model.Post
import it.atraj.habittracker.social.data.model.PostImage
import it.atraj.habittracker.social.data.model.UserSocialProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val postsCollection = firestore.collection("posts")
    private val commentsCollection = firestore.collection("comments")
    private val profilesCollection = firestore.collection("user_social_profiles")
    
    private val client = OkHttpClient()
    private val githubToken = BuildConfig.GITHUB_TOKEN
    private val githubOwner = "gongobongofounder"
    private val githubRepo = "habit-tracker-posts-repo"
    
    companion object {
        private const val TAG = "PostsRepository"
    }
    
    /**
     * Upload image to GitHub repository
     */
    suspend fun uploadImageToGitHub(imageFile: File, userId: String): Result<PostImage> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting image upload for file: ${imageFile.name}, size: ${imageFile.length()} bytes")
            
            val timestamp = System.currentTimeMillis()
            val fileName = "${userId}_${timestamp}_${imageFile.name}"
            val path = "posts/$userId/$fileName"
            
            // Read file and encode to Base64
            Log.d(TAG, "Reading and encoding file to Base64...")
            val fileBytes = imageFile.readBytes()
            val base64Content = Base64.getEncoder().encodeToString(fileBytes)
            Log.d(TAG, "Base64 encoding complete, size: ${base64Content.length} chars")
            
            // Create GitHub API request
            val url = "https://api.github.com/repos/$githubOwner/$githubRepo/contents/$path"
            Log.d(TAG, "Uploading to GitHub URL: $url")
            
            val jsonBody = JSONObject().apply {
                put("message", "Upload post image: $fileName")
                put("content", base64Content)
            }
            
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "token $githubToken")
                .header("Accept", "application/vnd.github.v3+json")
                .put(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            Log.d(TAG, "Executing GitHub API request...")
            val response = client.newCall(request).execute()
            Log.d(TAG, "GitHub API response code: ${response.code}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "{}")
                val content = jsonResponse.optJSONObject("content")
                val downloadUrl = content?.optString("download_url") ?: ""
                
                // Use raw.githubusercontent.com URL format which is more reliable
                // Format: https://raw.githubusercontent.com/{owner}/{repo}/main/{path}
                val rawUrl = "https://raw.githubusercontent.com/$githubOwner/$githubRepo/main/$path"
                
                // Add timestamp as cache buster to ensure fresh images
                val urlWithCacheBuster = "$rawUrl?t=$timestamp"
                
                val postImage = PostImage(
                    fileName = fileName,
                    githubPath = path,
                    rawUrl = urlWithCacheBuster,
                    uploadedAt = timestamp
                )
                
                Log.d(TAG, "Image uploaded successfully: $urlWithCacheBuster")
                Result.success(postImage)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Failed to upload image: $errorBody")
                Result.failure(Exception("Failed to upload image: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading image", e)
            Result.failure(e)
        }
    }
    
    /**
     * Upload image from URI to GitHub
     */
    suspend fun uploadImageFromUri(imageUri: Uri, userId: String, context: android.content.Context): Result<PostImage> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting upload from URI: $imageUri")
            
            // Create temporary file from URI
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("Cannot open image"))
            
            Log.d(TAG, "Creating temporary file...")
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            
            Log.d(TAG, "Temporary file created: ${tempFile.absolutePath}, size: ${tempFile.length()} bytes")
            
            val result = uploadImageToGitHub(tempFile, userId)
            tempFile.delete()
            
            // Add small delay to ensure GitHub processes the file
            if (result.isSuccess) {
                kotlinx.coroutines.delay(500) // 500ms delay
            }
            
            Log.d(TAG, "Upload result: ${if (result.isSuccess) "SUCCESS" else "FAILURE"}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading image from URI", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create a new post
     */
    suspend fun createPost(
        content: String,
        imageUrls: List<String>,
        friendIds: List<String>
    ): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext Result.failure(Exception("User not authenticated"))
            
            val post = Post(
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Anonymous",
                userAvatar = "", // Get from user profile
                userPhotoUrl = currentUser.photoUrl?.toString(),
                content = content,
                imageUrls = imageUrls,
                isPublic = false,
                viewerIds = friendIds,
                likeCount = 0,
                commentCount = 0
            )
            
            val docRef = postsCollection.add(post).await()
            val createdPost = post.copy(id = docRef.id)
            
            // Update user's post count
            updateUserPostCount(currentUser.uid, 1)
            
            Log.d(TAG, "Post created successfully: ${docRef.id}")
            Result.success(createdPost)
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating post", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get posts from friends (friends only visibility)
     */
    suspend fun getFriendsPosts(userId: String, limit: Int = 50): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val posts = postsCollection
                .whereArrayContains("viewerIds", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .toObjects(Post::class.java)
            
            Result.success(posts)
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting friends posts", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get posts by specific user
     */
    suspend fun getUserPosts(userId: String, limit: Int = 50): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val posts = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .toObjects(Post::class.java)
            
            Result.success(posts)
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting user posts", e)
            Result.failure(e)
        }
    }
    
    /**
     * Like or unlike a post
     */
    suspend fun toggleLike(postId: String, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val postRef = postsCollection.document(postId)
            val post = postRef.get().await().toObject(Post::class.java)
                ?: return@withContext Result.failure(Exception("Post not found"))
            
            val isLiked = post.likes.contains(userId)
            
            if (isLiked) {
                // Unlike
                postRef.update(
                    mapOf(
                        "likes" to FieldValue.arrayRemove(userId),
                        "likeCount" to FieldValue.increment(-1)
                    )
                ).await()
            } else {
                // Like
                postRef.update(
                    mapOf(
                        "likes" to FieldValue.arrayUnion(userId),
                        "likeCount" to FieldValue.increment(1)
                    )
                ).await()
            }
            
            Result.success(!isLiked)
        } catch (e: Exception) {
            Log.e(TAG, "Exception toggling like", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add comment to post
     */
    suspend fun addComment(postId: String, content: String): Result<Comment> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext Result.failure(Exception("User not authenticated"))
            
            val comment = Comment(
                postId = postId,
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Anonymous",
                userAvatar = "",
                userPhotoUrl = currentUser.photoUrl?.toString(),
                content = content
            )
            
            val docRef = commentsCollection.add(comment).await()
            
            // Increment comment count on post
            postsCollection.document(postId)
                .update("commentCount", FieldValue.increment(1))
                .await()
            
            val createdComment = comment.copy(id = docRef.id)
            Result.success(createdComment)
        } catch (e: Exception) {
            Log.e(TAG, "Exception adding comment", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get comments for a post
     */
    suspend fun getComments(postId: String): Result<List<Comment>> = withContext(Dispatchers.IO) {
        try {
            val comments = commentsCollection
                .whereEqualTo("postId", postId)
                .get()
                .await()
                .toObjects(Comment::class.java)
                .sortedBy { it.timestamp }
            
            Result.success(comments)
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting comments", e)
            Result.failure(e)
        }
    }
    
    /**
     * Real-time Flow for friends' posts
     */
    fun observeFriendsPosts(userId: String, limit: Int = 50): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .whereArrayContains("viewerIds", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing friends posts", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val posts = snapshot.toObjects(Post::class.java)
                    trySend(posts)
                }
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Real-time Flow for user's own posts
     */
    fun observeUserPosts(userId: String, limit: Int = 50): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing user posts", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val posts = snapshot.toObjects(Post::class.java)
                    trySend(posts)
                }
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Real-time Flow for comments on a post
     */
    fun observeComments(postId: String): Flow<List<Comment>> = callbackFlow {
        Log.d(TAG, "Starting to observe comments for post: $postId")
        val listener = commentsCollection
            .whereEqualTo("postId", postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing comments: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val comments = snapshot.toObjects(Comment::class.java)
                        .sortedBy { it.timestamp } // Sort in memory instead
                    Log.d(TAG, "Received ${comments.size} comments from Firestore for post $postId")
                    trySend(comments).isSuccess
                }
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Real-time Flow for a single post (for comment count updates)
     */
    fun observePost(postId: String): Flow<Post?> = callbackFlow {
        val listener = postsCollection
            .document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing post", error)
                    return@addSnapshotListener
                }
                
                val post = snapshot?.toObject(Post::class.java)
                trySend(post)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get comments for a post (keeping old method for compatibility)
     */
    @Deprecated("Use observeComments() for real-time updates")
    suspend fun getCommentsOld(postId: String): Result<List<Comment>> = withContext(Dispatchers.IO) {
        try {
            val comments = commentsCollection
                .whereEqualTo("postId", postId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(Comment::class.java)
            
            Result.success(comments)
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting comments", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get or create user social profile
     */
    suspend fun getUserProfile(userId: String): Result<UserSocialProfile> = withContext(Dispatchers.IO) {
        try {
            // Always fetch latest user data from users collection
            val userDoc = firestore.collection("users").document(userId).get().await()
            val customAvatar = userDoc.getString("customAvatar") ?: ""
            val customDisplayName = userDoc.getString("customDisplayName")
            val currentUser = auth.currentUser
            
            // Determine if customAvatar is an emoji (not a URL)
            val isEmojiAvatar = customAvatar.isNotEmpty() && !customAvatar.startsWith("http")
            
            val doc = profilesCollection.document(userId).get().await()
            
            if (doc.exists()) {
                val profile = doc.toObject(UserSocialProfile::class.java)
                    ?: return@withContext Result.failure(Exception("Failed to parse profile"))
                
                // Update profile with latest user data if changed
                val updatedUserName = customDisplayName ?: currentUser?.displayName ?: "User"
                val updatedAvatar = if (isEmojiAvatar) customAvatar else ""
                val updatedPhotoUrl = if (!isEmojiAvatar) (currentUser?.photoUrl?.toString() ?: "") else ""
                
                if (profile.avatar != updatedAvatar || profile.userName != updatedUserName || profile.photoUrl != updatedPhotoUrl) {
                    val updatedProfile = profile.copy(
                        avatar = updatedAvatar,
                        userName = updatedUserName,
                        photoUrl = if (updatedPhotoUrl.isEmpty()) null else updatedPhotoUrl
                    )
                    // Update in Firestore
                    profilesCollection.document(userId).set(updatedProfile).await()
                    Result.success(updatedProfile)
                } else {
                    Result.success(profile)
                }
            } else {
                // Create default profile
                val profile = UserSocialProfile(
                    userId = userId,
                    userName = customDisplayName ?: currentUser?.displayName ?: "User",
                    bio = "",
                    avatar = if (isEmojiAvatar) customAvatar else "",
                    photoUrl = if (!isEmojiAvatar) currentUser?.photoUrl?.toString() else null
                )
                
                profilesCollection.document(userId).set(profile).await()
                Result.success(profile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting user profile", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update user social profile
     */
    suspend fun updateUserProfile(profile: UserSocialProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            profilesCollection.document(profile.userId)
                .set(profile)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating user profile", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update user's post count
     */
    private suspend fun updateUserPostCount(userId: String, increment: Int) {
        try {
            profilesCollection.document(userId)
                .update("postCount", FieldValue.increment(increment.toLong()))
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating post count", e)
        }
    }
    
    /**
     * Delete post
     */
    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser ?: return@withContext Result.failure(Exception("User not authenticated"))
            
            val post = postsCollection.document(postId).get().await().toObject(Post::class.java)
                ?: return@withContext Result.failure(Exception("Post not found"))
            
            // Only allow deletion by post owner
            if (post.userId != currentUser.uid) {
                return@withContext Result.failure(Exception("Not authorized to delete this post"))
            }
            
            // Delete post document
            postsCollection.document(postId).delete().await()
            
            // Delete associated comments
            val comments = commentsCollection.whereEqualTo("postId", postId).get().await()
            comments.documents.forEach { it.reference.delete() }
            
            // Update user's post count
            updateUserPostCount(currentUser.uid, -1)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception deleting post", e)
            Result.failure(e)
        }
    }
}
