package it.atraj.habittracker.auth.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import it.atraj.habittracker.auth.AuthRepository
import it.atraj.habittracker.auth.AuthResult
import it.atraj.habittracker.auth.GoogleSignInHelper
import it.atraj.habittracker.auth.User
import it.atraj.habittracker.data.HabitRepository
import it.atraj.habittracker.notification.HabitReminderScheduler
import it.atraj.habittracker.notification.HabitReminderService
import it.atraj.habittracker.notification.OverdueNotificationScheduler
import it.atraj.habittracker.notification.OverdueNotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = true, // Start as loading while checking auth state
    val user: User? = null,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val errorMessage: String? = null,
    val isSignUpMode: Boolean = false,
    val showForgotPassword: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val googleSignInHelper: GoogleSignInHelper,
    private val habitRepository: HabitRepository,
    private val reminderScheduler: HabitReminderScheduler,
    private val overdueScheduler: OverdueNotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(user = user, isLoading = false) }
            }
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, errorMessage = null) }
    }

    fun toggleSignUpMode() {
        _uiState.update { 
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                errorMessage = null,
                password = "",
                confirmPassword = ""
            ) 
        }
    }

    fun toggleForgotPassword() {
        _uiState.update { 
            it.copy(
                showForgotPassword = !it.showForgotPassword,
                errorMessage = null
            ) 
        }
    }

    fun signInWithEmail() {
        val currentState = _uiState.value
        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = authRepository.signInWithEmail(currentState.email, currentState.password)
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    errorMessage = if (result is AuthResult.Error) result.message else null
                )
            }
        }
    }

    fun signUpWithEmail() {
        val currentState = _uiState.value
        if (currentState.email.isBlank() || currentState.password.isBlank() || currentState.confirmPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all fields") }
            return
        }

        if (currentState.password != currentState.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        if (currentState.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = authRepository.signUpWithEmail(currentState.email, currentState.password)
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    errorMessage = if (result is AuthResult.Error) result.message else null
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = authRepository.signInWithGoogle(idToken)
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    errorMessage = if (result is AuthResult.Error) result.message else null
                )
            }
        }
    }

    fun sendPasswordResetEmail() {
        val currentState = _uiState.value
        if (currentState.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = authRepository.sendPasswordResetEmail(currentState.email)
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    errorMessage = if (result is AuthResult.Error) result.message else "Password reset email sent!",
                    showForgotPassword = false
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // Cancel all scheduled alarms and delete notification channels before signing out
            withContext(Dispatchers.IO) {
                try {
                    val habits = habitRepository.getAllHabits()
                    val habitIds = habits.map { it.id }
                    var cancelledRemindersCount = 0
                    var cancelledOverdueCount = 0
                    
                    // Cancel all regular reminder alarms and overdue alarms
                    habits.forEach { habit ->
                        try {
                            reminderScheduler.cancel(habit.id)
                            cancelledRemindersCount++
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Failed to cancel reminder alarm for habit ${habit.id}", e)
                        }
                        
                        try {
                            overdueScheduler.cancelOverdueChecks(habit.id)
                            cancelledOverdueCount++
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Failed to cancel overdue alarms for habit ${habit.id}", e)
                        }
                    }
                    Log.d("AuthViewModel", "Cancelled $cancelledRemindersCount reminder alarms and $cancelledOverdueCount overdue alarms before logout")
                    
                    // Delete all notification channels (both regular and overdue)
                    if (habitIds.isNotEmpty()) {
                        HabitReminderService.deleteMultipleHabitChannels(context, habitIds)
                        Log.d("AuthViewModel", "Deleted ${habitIds.size} regular notification channels before logout")
                        
                        // Dismiss and delete overdue notifications and channels
                        habitIds.forEach { habitId ->
                            try {
                                OverdueNotificationService.dismissAllOverdueNotifications(context, habitId)
                                OverdueNotificationService.deleteOverdueChannel(context, habitId)
                            } catch (e: Exception) {
                                Log.e("AuthViewModel", "Failed to cleanup overdue notifications for habit $habitId", e)
                            }
                        }
                        Log.d("AuthViewModel", "Dismissed and deleted ${habitIds.size} overdue notification channels before logout")
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error cleaning up alarms/channels during logout", e)
                }
            }
            
            authRepository.signOut()
            googleSignInHelper.signOut()
        }
    }

    fun updateCustomAvatar(avatar: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.updateCustomAvatar(avatar)
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    errorMessage = if (result is AuthResult.Error) result.message else null
                )
            }
        }
    }
    
    fun updateDisplayName(name: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.updateDisplayName(name)
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    errorMessage = if (result is AuthResult.Error) result.message else null
                )
            }
            if (result is AuthResult.Success) {
                onSuccess()
            }
        }
    }
    
    fun updateMusicPreferences(enabled: Boolean, track: String, volume: Float, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = authRepository.updateMusicPreferences(enabled, track, volume)
            if (result is AuthResult.Success) {
                onSuccess()
            } else if (result is AuthResult.Error) {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
        }
    }
    
    fun updateMusicSettings(enabled: Boolean, track: String, volume: Float) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.updateMusicPreferences(enabled, track, volume)
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    errorMessage = if (result is AuthResult.Error) result.message else null
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun setGoogleSignInError(message: String) {
        _uiState.update { it.copy(errorMessage = message, isLoading = false) }
    }
}
