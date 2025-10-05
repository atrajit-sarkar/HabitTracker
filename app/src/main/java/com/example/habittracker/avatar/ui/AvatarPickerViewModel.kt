package it.atraj.habittracker.avatar.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.atraj.habittracker.avatar.AvatarItem
import it.atraj.habittracker.avatar.AvatarManager
import it.atraj.habittracker.avatar.AvatarResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Enhanced Avatar Picker
 */
@HiltViewModel
class AvatarPickerViewModel @Inject constructor(
    private val avatarManager: AvatarManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AvatarPickerUiState())
    val uiState: StateFlow<AvatarPickerUiState> = _uiState.asStateFlow()
    
    /**
     * Load all available avatars (default + custom)
     */
    fun loadAvatars() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val avatars = avatarManager.getAllAvatars()
                _uiState.update { 
                    it.copy(
                        avatars = avatars,
                        isLoading = false
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load avatars: ${e.message}"
                    ) 
                }
            }
        }
    }
    
    /**
     * Upload a custom avatar
     */
    fun uploadCustomAvatar(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isUploading = true, 
                    errorMessage = null
                ) 
            }
            
            when (val result = avatarManager.uploadCustomAvatar(imageUri)) {
                is AvatarResult.Success -> {
                    // Reload avatars to show the new one
                    loadAvatars()
                    _uiState.update { it.copy(isUploading = false) }
                }
                is AvatarResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isUploading = false,
                            errorMessage = result.message
                        ) 
                    }
                }
            }
        }
    }
    
    /**
     * Delete a custom avatar
     */
    fun deleteCustomAvatar(avatarUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            
            val success = avatarManager.deleteCustomAvatar(avatarUrl)
            
            if (success) {
                // Reload avatars to remove the deleted one
                loadAvatars()
            } else {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to delete avatar") 
                }
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for avatar picker
 */
data class AvatarPickerUiState(
    val avatars: List<AvatarItem> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val errorMessage: String? = null
)
