package com.example.habittracker.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.auth.AuthRepository
import com.example.habittracker.auth.AuthResult
import com.example.habittracker.auth.GoogleSignInHelper
import com.example.habittracker.auth.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val authRepository: AuthRepository,
    private val googleSignInHelper: GoogleSignInHelper
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}