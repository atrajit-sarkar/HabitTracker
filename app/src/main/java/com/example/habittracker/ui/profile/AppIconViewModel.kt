package it.atraj.habittracker.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import it.atraj.habittracker.service.AppIconManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppIconViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appIconManager: AppIconManager
) : ViewModel() {
    
    private val _currentIconId = MutableStateFlow("default")
    val currentIconId: StateFlow<String> = _currentIconId.asStateFlow()
    
    private val _isChangingIcon = MutableStateFlow(false)
    val isChangingIcon: StateFlow<Boolean> = _isChangingIcon.asStateFlow()
    
    init {
        loadCurrentIconId()
    }
    
    private fun loadCurrentIconId() {
        viewModelScope.launch {
            _currentIconId.value = appIconManager.getCurrentIconId()
        }
    }
    
    fun changeAppIcon(iconId: String, activityAlias: String) {
        viewModelScope.launch {
            _isChangingIcon.value = true
            
            try {
                val success = appIconManager.changeAppIcon(iconId, activityAlias)
                if (success) {
                    _currentIconId.value = iconId
                }
            } catch (e: Exception) {
                // Handle error - could show a toast or error state
            } finally {
                _isChangingIcon.value = false
            }
        }
    }
}