package it.atraj.habittracker.music.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.atraj.habittracker.data.model.MusicCategory
import it.atraj.habittracker.data.model.MusicMetadata
import it.atraj.habittracker.data.model.UserFolder
import it.atraj.habittracker.data.repository.GitHubMusicService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicBrowserUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val officialCategories: List<MusicCategory> = emptyList(),
    val userFolders: List<UserFolder> = emptyList(),
    val currentSongs: List<MusicMetadata> = emptyList(),
    val currentUserId: String = ""
)

@HiltViewModel
class MusicBrowserViewModel @Inject constructor(
    private val gitHubMusicService: GitHubMusicService
) : ViewModel() {
    
    companion object {
        private const val TAG = "MusicBrowserViewModel"
    }
    
    private val _uiState = MutableStateFlow(MusicBrowserUiState())
    val uiState: StateFlow<MusicBrowserUiState> = _uiState.asStateFlow()
    
    /**
     * Set current user ID
     */
    fun setCurrentUserId(userId: String) {
        _uiState.value = _uiState.value.copy(currentUserId = userId)
    }
    
    /**
     * Load official song categories
     */
    fun loadOfficialCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            gitHubMusicService.listOfficialCategories()
                .onSuccess { categories ->
                    Log.d(TAG, "Loaded ${categories.size} official categories")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        officialCategories = categories
                    )
                }
                .onFailure { exception ->
                    Log.e(TAG, "Failed to load official categories", exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load categories"
                    )
                }
        }
    }
    
    /**
     * Load user folders
     */
    fun loadUserFolders() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val currentUserId = _uiState.value.currentUserId.ifEmpty { "unknown" }
            
            gitHubMusicService.listUserFolders(currentUserId)
                .onSuccess { folders ->
                    Log.d(TAG, "Loaded ${folders.size} user folders")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userFolders = folders
                    )
                }
                .onFailure { exception ->
                    Log.e(TAG, "Failed to load user folders", exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load user folders"
                    )
                }
        }
    }
    
    /**
     * Load songs in a category
     */
    fun loadSongsInCategory(categoryPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            gitHubMusicService.listSongsInCategory(categoryPath)
                .onSuccess { songs ->
                    Log.d(TAG, "Loaded ${songs.size} songs in category: $categoryPath")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentSongs = songs
                    )
                }
                .onFailure { exception ->
                    Log.e(TAG, "Failed to load songs", exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load songs"
                    )
                }
        }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Upload a song to GitHub
     */
    suspend fun uploadSong(
        userId: String,
        songData: it.atraj.habittracker.data.model.SongUploadData
    ): Result<it.atraj.habittracker.data.model.GitHubUploadResponse> {
        return try {
            Log.d(TAG, "Starting upload for song: ${songData.title}")
            gitHubMusicService.uploadSong(userId, songData)
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            Result.failure(e)
        }
    }
}
