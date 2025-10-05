package it.atraj.habittracker.ui.social

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.atraj.habittracker.auth.User
import it.atraj.habittracker.data.firestore.FriendRepository
import it.atraj.habittracker.data.firestore.FriendRequest
import it.atraj.habittracker.data.firestore.UserPublicProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SocialUiState(
    val currentUser: User? = null,
    val friends: List<UserPublicProfile> = emptyList(),
    val pendingRequests: List<FriendRequest> = emptyList(),
    val searchResult: UserPublicProfile? = null,
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val isLoadingLeaderboard: Boolean = false,
    val actionInProgress: Boolean = false,
    val actionMessage: String? = null,
    val actionError: String? = null
)

data class LeaderboardEntry(
    val profile: UserPublicProfile,
    val rank: Int,
    val isCurrentUser: Boolean = false
)

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val friendRepository: FriendRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "SocialViewModel"
    }

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    fun setCurrentUser(user: User) {
        currentUserId = user.uid
        _uiState.update { it.copy(currentUser = user) }
        
        // Update user's public profile
        updatePublicProfile(user)
        
        // Load friends and pending requests
        loadFriends()
        loadPendingRequests()
    }

    private fun updatePublicProfile(user: User) {
        viewModelScope.launch {
            val currentUser = _uiState.value.currentUser ?: return@launch
            
            Log.d(TAG, "updatePublicProfile: Fetching existing profile for ${user.uid}")
            
            // Get existing profile to preserve stats
            val existingProfile = friendRepository.getFriendProfile(user.uid)
            
            if (existingProfile != null) {
                Log.d(TAG, "updatePublicProfile: Found existing profile - SR: ${existingProfile.successRate}%, Habits: ${existingProfile.totalHabits}, Score: ${existingProfile.leaderboardScore}")
            } else {
                Log.d(TAG, "updatePublicProfile: No existing profile found, will create new one")
            }
            
            // Update user's public profile, preserving existing stats
            friendRepository.updateUserPublicProfile(
                userId = user.uid,
                email = user.email ?: "",
                displayName = user.effectiveDisplayName,
                photoUrl = user.photoUrl,
                customAvatar = user.customAvatar, // null if no custom avatar set
                // Preserve existing stats or use 0 if profile doesn't exist yet
                successRate = existingProfile?.successRate ?: 0,
                totalHabits = existingProfile?.totalHabits ?: 0,
                totalCompletions = existingProfile?.totalCompletions ?: 0,
                currentStreak = existingProfile?.currentStreak ?: 0,
                leaderboardScore = existingProfile?.leaderboardScore ?: 0
            )
            
            Log.d(TAG, "updatePublicProfile: Profile updated in Firestore")
        }
    }

    fun updateUserStats(successRate: Int, totalHabits: Int, totalCompletions: Int, currentStreak: Int, leaderboardScore: Int) {
        viewModelScope.launch {
            val user = _uiState.value.currentUser ?: return@launch
            
            friendRepository.updateUserPublicProfile(
                userId = user.uid,
                email = user.email ?: "",
                displayName = user.effectiveDisplayName,
                photoUrl = user.photoUrl,
                customAvatar = user.customAvatar, // null if no custom avatar set
                successRate = successRate,
                totalHabits = totalHabits,
                totalCompletions = totalCompletions,
                currentStreak = currentStreak,
                leaderboardScore = leaderboardScore
            )
        }
    }

    private fun loadFriends() {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            try {
                friendRepository.getFriends(userId).collect { friends ->
                    _uiState.update { it.copy(friends = friends) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Expected when ViewModel is cleared
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error loading friends: ${e.message}", e)
            }
        }
    }

    private fun loadPendingRequests() {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            try {
                friendRepository.getPendingFriendRequests(userId).collect { requests ->
                    _uiState.update { it.copy(pendingRequests = requests) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Expected when ViewModel is cleared
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error loading pending requests: ${e.message}", e)
            }
        }
    }

    fun searchUserByEmail(email: String) {
        if (email.isBlank()) {
            _uiState.update { 
                it.copy(
                    searchResult = null,
                    searchError = null,
                    isSearching = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            
            try {
                val result = friendRepository.searchUserByEmail(email.trim())
                
                if (result == null) {
                    _uiState.update { 
                        it.copy(
                            searchResult = null,
                            searchError = "No user found with this email",
                            isSearching = false
                        )
                    }
                } else if (result.userId == currentUserId) {
                    _uiState.update { 
                        it.copy(
                            searchResult = null,
                            searchError = "You cannot add yourself as a friend",
                            isSearching = false
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            searchResult = result,
                            searchError = null,
                            isSearching = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching user: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        searchResult = null,
                        searchError = "Error searching for user",
                        isSearching = false
                    )
                }
            }
        }
    }

    fun sendFriendRequest(toUser: UserPublicProfile) {
        val currentUser = _uiState.value.currentUser ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionError = null) }
            
            val result = friendRepository.sendFriendRequest(
                fromUserId = currentUser.uid,
                fromUserEmail = currentUser.email ?: "",
                fromUserName = currentUser.effectiveDisplayName,
                fromUserAvatar = currentUser.customAvatar ?: "", // Empty string if no custom avatar
                toUserId = toUser.userId,
                toUserEmail = toUser.email
            )
            
            if (result.isSuccess) {
                _uiState.update { 
                    it.copy(
                        actionInProgress = false,
                        actionMessage = "Friend request sent!",
                        searchResult = null
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        actionInProgress = false,
                        actionError = result.exceptionOrNull()?.message ?: "Failed to send friend request"
                    )
                }
            }
        }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionError = null) }
            
            val result = friendRepository.acceptFriendRequest(request.id)
            
            if (result.isSuccess) {
                _uiState.update { 
                    it.copy(
                        actionInProgress = false,
                        actionMessage = "Friend request accepted!"
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        actionInProgress = false,
                        actionError = "Failed to accept friend request"
                    )
                }
            }
        }
    }

    fun rejectFriendRequest(request: FriendRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionError = null) }
            
            val result = friendRepository.rejectFriendRequest(request.id)
            
            if (result.isSuccess) {
                _uiState.update { 
                    it.copy(
                        actionInProgress = false,
                        actionMessage = "Friend request rejected"
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        actionInProgress = false,
                        actionError = "Failed to reject friend request"
                    )
                }
            }
        }
    }

    fun removeFriend(friendId: String) {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionError = null) }
            
            val result = friendRepository.removeFriend(userId, friendId)
            
            if (result.isSuccess) {
                _uiState.update { 
                    it.copy(
                        actionInProgress = false,
                        actionMessage = "Friend removed"
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        actionInProgress = false,
                        actionError = "Failed to remove friend"
                    )
                }
            }
        }
    }

    fun loadLeaderboard() {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLeaderboard = true) }
            
            try {
                // Use real-time observer instead of one-time fetch
                friendRepository.observeLeaderboard(userId).collect { profiles ->
                    val entries = profiles.mapIndexed { index, profile ->
                        LeaderboardEntry(
                            profile = profile,
                            rank = index + 1,
                            isCurrentUser = profile.userId == userId
                        )
                    }
                    
                    _uiState.update { 
                        it.copy(
                            leaderboard = entries,
                            isLoadingLeaderboard = false
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // This is expected when navigating away - don't log as error
                throw e // Re-throw to properly cancel the coroutine
            } catch (e: Exception) {
                Log.e(TAG, "Error loading leaderboard: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        leaderboard = emptyList(),
                        isLoadingLeaderboard = false
                    )
                }
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null, actionError = null) }
    }

    fun clearSearchResult() {
        _uiState.update { it.copy(searchResult = null, searchError = null) }
    }
}
