package it.atraj.habittracker.social.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import it.atraj.habittracker.data.firestore.FriendRepository
import it.atraj.habittracker.social.data.model.Comment
import it.atraj.habittracker.social.data.model.Post
import it.atraj.habittracker.social.data.model.PostImage
import it.atraj.habittracker.social.data.repository.PostsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

data class BrowsePostsUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)

data class CreatePostUiState(
    val content: String = "",
    val selectedImages: List<Uri> = emptyList(),
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val error: String? = null,
    val success: Boolean = false
)

data class PostDetailsUiState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val postsRepository: PostsRepository,
    private val friendRepository: FriendRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    companion object {
        private const val TAG = "PostsViewModel"
    }
    
    private val _browsePosts = MutableStateFlow(BrowsePostsUiState())
    val browsePosts: StateFlow<BrowsePostsUiState> = _browsePosts.asStateFlow()
    
    private val _createPost = MutableStateFlow(CreatePostUiState())
    val createPost: StateFlow<CreatePostUiState> = _createPost.asStateFlow()
    
    private val _postDetails = MutableStateFlow(PostDetailsUiState())
    val postDetails: StateFlow<PostDetailsUiState> = _postDetails.asStateFlow()
    
    private var observeCommentsJob: Job? = null
    private var observePostJob: Job? = null
    
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""
    
    init {
        observeFriendsPosts()
    }
    
    private fun observeFriendsPosts() {
        viewModelScope.launch {
            _browsePosts.value = _browsePosts.value.copy(isLoading = true, error = null)
            
            try {
                postsRepository.observeFriendsPosts(currentUserId)
                    .collect { posts ->
                        _browsePosts.value = _browsePosts.value.copy(
                            posts = posts,
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
            } catch (e: Exception) {
                _browsePosts.value = _browsePosts.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun loadFriendsPosts() {
        viewModelScope.launch {
            _browsePosts.value = _browsePosts.value.copy(isLoading = true, error = null)
            
            val result = postsRepository.getFriendsPosts(currentUserId)
            
            result.fold(
                onSuccess = { posts ->
                    _browsePosts.value = _browsePosts.value.copy(
                        posts = posts,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _browsePosts.value = _browsePosts.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun refresh() {
        _browsePosts.value = _browsePosts.value.copy(isRefreshing = true)
        // The real-time listener will update automatically
    }
    
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            val result = postsRepository.toggleLike(postId, currentUserId)
            
            result.onSuccess { isLiked ->
                // Real-time listeners will automatically update the UI
                Log.d(TAG, "Like toggled successfully: isLiked=$isLiked")
            }
            
            result.onFailure { error ->
                Log.e(TAG, "Failed to toggle like", error)
            }
        }
    }
    
    fun loadPostDetails(postId: String) {
        viewModelScope.launch {
            _postDetails.value = PostDetailsUiState(isLoading = true)
            
            // Find post in current list or fetch it
            val post = _browsePosts.value.posts.find { it.id == postId }
            
            if (post != null) {
                _postDetails.value = _postDetails.value.copy(post = post)
            }
            
            // Observe real-time comments
            try {
                postsRepository.observeComments(postId).collect { comments ->
                    _postDetails.value = _postDetails.value.copy(
                        comments = comments,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _postDetails.value = _postDetails.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun observePostForComments(postId: String) {
        observePostJob?.cancel()
        observePostJob = viewModelScope.launch {
            // Observe the post itself for comment count updates
            postsRepository.observePost(postId).collect { updatedPost ->
                if (updatedPost != null) {
                    // Update post in browse list
                    val updatedPosts = _browsePosts.value.posts.map { post ->
                        if (post.id == postId) updatedPost else post
                    }
                    _browsePosts.value = _browsePosts.value.copy(posts = updatedPosts)
                    
                    // Update post details if viewing
                    if (_postDetails.value.post?.id == postId) {
                        _postDetails.value = _postDetails.value.copy(post = updatedPost)
                    }
                }
            }
        }
    }
    
    fun observeComments(postId: String) {
        observeCommentsJob?.cancel()
        observeCommentsJob = viewModelScope.launch {
            _postDetails.value = _postDetails.value.copy(isLoading = true)
            
            try {
                postsRepository.observeComments(postId).collect { comments ->
                    Log.d(TAG, "Received ${comments.size} comments for post $postId")
                    _postDetails.value = _postDetails.value.copy(
                        comments = comments,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing comments", e)
                _postDetails.value = _postDetails.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    @Deprecated("Use loadPostDetails for real-time updates")
    fun loadPostDetailsOld(postId: String) {
        viewModelScope.launch {
            _postDetails.value = PostDetailsUiState(isLoading = true)
            
            // Find post in current list or fetch it
            val post = _browsePosts.value.posts.find { it.id == postId }
            
            if (post != null) {
                _postDetails.value = _postDetails.value.copy(post = post)
            }
            
            // Load comments
            val result = postsRepository.getComments(postId)
            
            result.fold(
                onSuccess = { comments ->
                    _postDetails.value = _postDetails.value.copy(
                        comments = comments,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _postDetails.value = _postDetails.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            val result = postsRepository.addComment(postId, content)
            
            result.onSuccess { comment ->
                // Real-time listeners will automatically update the UI
                // No need to manually update state
                Log.d(TAG, "Comment added successfully: ${comment.id}")
            }
            
            result.onFailure { error ->
                Log.e(TAG, "Failed to add comment", error)
            }
        }
    }
    
    // Create Post functions
    fun updateContent(content: String) {
        _createPost.value = _createPost.value.copy(content = content)
    }
    
    fun addImage(uri: Uri) {
        val currentImages = _createPost.value.selectedImages
        if (currentImages.size < 10) { // Max 10 images
            _createPost.value = _createPost.value.copy(
                selectedImages = currentImages + uri
            )
        }
    }
    
    fun removeImage(uri: Uri) {
        val updatedImages = _createPost.value.selectedImages.filter { it != uri }
        _createPost.value = _createPost.value.copy(selectedImages = updatedImages)
    }
    
    fun createPost(context: Context) {
        viewModelScope.launch {
            Log.d(TAG, "createPost started")
            _createPost.value = _createPost.value.copy(
                isUploading = true,
                uploadProgress = 0f,
                error = null
            )
            
            try {
                Log.d(TAG, "Getting friends list for user: $currentUserId")
                // Get friends list - use first() to get the current value
                val friends = friendRepository.getFriends(currentUserId).first()
                val friendIds = friends.map { it.userId }
                Log.d(TAG, "Loaded ${friendIds.size} friends")
                
                // Upload images to GitHub
                val imageUrls = mutableListOf<String>()
                val images = _createPost.value.selectedImages
                Log.d(TAG, "Number of images to upload: ${images.size}")
                
                if (images.isEmpty()) {
                    Log.d(TAG, "Creating text-only post")
                    // No images, just create post with text
                    val result = postsRepository.createPost(
                        content = _createPost.value.content,
                        imageUrls = emptyList(),
                        friendIds = friendIds
                    )
                    
                    result.fold(
                        onSuccess = {
                            Log.d(TAG, "Post created successfully")
                            _createPost.value = CreatePostUiState(success = true)
                            loadFriendsPosts() // Refresh feed
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Failed to create post", error)
                            _createPost.value = _createPost.value.copy(
                                isUploading = false,
                                error = error.message ?: "Failed to create post"
                            )
                        }
                    )
                } else {
                    Log.d(TAG, "Uploading images...")
                    // Upload images first
                    images.forEachIndexed { index, uri ->
                        Log.d(TAG, "Uploading image ${index + 1}/${images.size}: $uri")
                        val result = postsRepository.uploadImageFromUri(uri, currentUserId, context)
                        result.fold(
                            onSuccess = { postImage ->
                                imageUrls.add(postImage.rawUrl)
                                val progress = (index + 1).toFloat() / images.size
                                Log.d(TAG, "Image uploaded successfully. Progress: $progress")
                                _createPost.value = _createPost.value.copy(
                                    uploadProgress = progress
                                )
                            },
                            onFailure = { error ->
                                Log.e(TAG, "Failed to upload image ${index + 1}", error)
                                throw error
                            }
                        )
                    }
                    
                    Log.d(TAG, "All images uploaded. Creating post...")
                    // Create post in Firebase
                    val result = postsRepository.createPost(
                        content = _createPost.value.content,
                        imageUrls = imageUrls,
                        friendIds = friendIds
                    )
                    
                    result.fold(
                        onSuccess = {
                            Log.d(TAG, "Post created successfully with images")
                            _createPost.value = CreatePostUiState(success = true)
                            loadFriendsPosts() // Refresh feed
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Failed to create post", error)
                            _createPost.value = _createPost.value.copy(
                                isUploading = false,
                                error = error.message ?: "Failed to create post"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in createPost", e)
                _createPost.value = _createPost.value.copy(
                    isUploading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    fun resetCreatePost() {
        _createPost.value = CreatePostUiState()
    }
}
