package it.atraj.habittracker.social.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import it.atraj.habittracker.data.firestore.FriendRepository
import it.atraj.habittracker.social.data.model.Post
import it.atraj.habittracker.social.data.model.UserSocialProfile
import it.atraj.habittracker.social.data.repository.PostsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SocialProfileUiState(
    val profile: UserSocialProfile? = null,
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class SocialProfileViewModel @Inject constructor(
    private val postsRepository: PostsRepository,
    private val friendRepository: FriendRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SocialProfileUiState())
    val uiState: StateFlow<SocialProfileUiState> = _uiState.asStateFlow()
    
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""
    
    init {
        loadProfile()
        observePosts()
        observeFriendsCount()
    }
    
    private fun observeFriendsCount() {
        viewModelScope.launch {
            try {
                friendRepository.getFriends(currentUserId).collect { friends ->
                    val friendCount = friends.size
                    
                    // Update profile with actual friend count
                    _uiState.value.profile?.let { profile ->
                        if (profile.friendCount != friendCount) {
                            val updatedProfile = profile.copy(friendCount = friendCount)
                            _uiState.value = _uiState.value.copy(profile = updatedProfile)
                            
                            // Also update in Firestore
                            postsRepository.updateUserProfile(updatedProfile)
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    private fun observePosts(userId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val targetUserId = userId ?: currentUserId
            
            try {
                postsRepository.observeUserPosts(targetUserId).collect { posts ->
                    _uiState.value = _uiState.value.copy(
                        posts = posts,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun observeUserPosts(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                postsRepository.observeUserPosts(userId).collect { posts ->
                    _uiState.value = _uiState.value.copy(
                        posts = posts,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun loadProfile(userId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val targetUserId = userId ?: currentUserId
            val result = postsRepository.getUserProfile(targetUserId)
            
            result.fold(
                onSuccess = { profile ->
                    _uiState.value = _uiState.value.copy(
                        profile = profile,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun loadPosts(userId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val targetUserId = userId ?: currentUserId
            val result = postsRepository.getUserPosts(targetUserId)
            
            result.fold(
                onSuccess = { posts ->
                    _uiState.value = _uiState.value.copy(
                        posts = posts,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadProfile()
        // Posts will auto-update via real-time listener
    }
    
    fun updateBio(newBio: String) {
        viewModelScope.launch {
            val currentProfile = _uiState.value.profile ?: return@launch
            val updatedProfile = currentProfile.copy(bio = newBio)
            
            val result = postsRepository.updateUserProfile(updatedProfile)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(profile = updatedProfile)
            }
        }
    }
    
    fun deletePost(postId: String) {
        viewModelScope.launch {
            val result = postsRepository.deletePost(postId)
            result.onSuccess {
                // Remove post from UI
                val updatedPosts = _uiState.value.posts.filter { it.id != postId }
                _uiState.value = _uiState.value.copy(posts = updatedPosts)
            }
        }
    }
}
