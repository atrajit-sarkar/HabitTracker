package it.atraj.habittracker.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import it.atraj.habittracker.service.AppIconManager
import it.atraj.habittracker.service.OverdueHabitIconManager
import it.atraj.habittracker.util.IconState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppIconViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appIconManager: AppIconManager,
    private val overdueHabitIconManager: OverdueHabitIconManager
) : ViewModel() {
    
    private val _currentIconId = MutableStateFlow("default")
    val currentIconId: StateFlow<String> = _currentIconId.asStateFlow()
    
    private val _isChangingIcon = MutableStateFlow(false)
    val isChangingIcon: StateFlow<Boolean> = _isChangingIcon.asStateFlow()
    
    private val _isInOverdueMode = MutableStateFlow(false)
    val isInOverdueMode: StateFlow<Boolean> = _isInOverdueMode.asStateFlow()
    
    init {
        loadCurrentIconId()
        checkOverdueMode()
    }
    
    private fun loadCurrentIconId() {
        viewModelScope.launch {
            _currentIconId.value = appIconManager.getCurrentIconId()
        }
    }
    
    private fun checkOverdueMode() {
        viewModelScope.launch {
            val currentIconState = overdueHabitIconManager.getCurrentIconState()
            _isInOverdueMode.value = currentIconState != IconState.DEFAULT
        }
    }
    
    fun changeAppIcon(iconId: String, activityAlias: String) {
        viewModelScope.launch {
            _isChangingIcon.value = true
            
            try {
                // Use scheduled icon change for smooth transition
                // This updates the UI immediately but delays the actual component change
                appIconManager.scheduleIconChange(iconId, activityAlias)
                
                // Update UI state immediately
                _currentIconId.value = iconId
            } catch (e: Exception) {
                // Handle error - could show a toast or error state
            } finally {
                _isChangingIcon.value = false
            }
        }
    }
}
