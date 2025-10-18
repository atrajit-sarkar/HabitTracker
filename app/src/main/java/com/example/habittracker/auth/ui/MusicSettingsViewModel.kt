package it.atraj.habittracker.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.atraj.habittracker.data.manager.DynamicMusicManager
import it.atraj.habittracker.data.model.MusicMetadata
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicSettingsUiState(
    val musicList: List<MusicMetadata> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class MusicSettingsViewModel @Inject constructor(
    private val dynamicMusicManager: DynamicMusicManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MusicSettingsUiState())
    val uiState: StateFlow<MusicSettingsUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            // Initialize dynamic music manager
            dynamicMusicManager.initialize()
            
            // Observe music list changes
            combine(
                dynamicMusicManager.musicList,
                dynamicMusicManager.isLoading,
                dynamicMusicManager.error
            ) { musicList, isLoading, error ->
                MusicSettingsUiState(
                    musicList = musicList,
                    isLoading = isLoading,
                    error = error
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    /**
     * Refresh music metadata from repository
     */
    fun refreshMusicList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            dynamicMusicManager.loadMusicMetadata(forceRefresh = true)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
    
    /**
     * Check for updates
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            dynamicMusicManager.checkAndUpdateIfNeeded()
        }
    }
    
    /**
     * Get music by ID
     */
    fun getMusicById(id: String): MusicMetadata? {
        return dynamicMusicManager.getMusicById(id)
    }
    
    /**
     * Get download URL for music
     */
    fun getDownloadUrl(metadata: MusicMetadata): String {
        return dynamicMusicManager.getDownloadUrl(metadata)
    }
    
    /**
     * Get categories
     */
    fun getCategories(): List<String> {
        return dynamicMusicManager.getCategories()
    }
    
    /**
     * Clear cache and reload
     */
    fun clearCacheAndReload() {
        viewModelScope.launch {
            dynamicMusicManager.clearCacheAndReload()
        }
    }
}
