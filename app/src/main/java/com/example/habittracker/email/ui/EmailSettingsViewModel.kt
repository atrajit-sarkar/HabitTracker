package it.atraj.habittracker.email.ui

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.atraj.habittracker.auth.AuthRepository
import it.atraj.habittracker.data.HabitRepository
import it.atraj.habittracker.data.local.Habit
import it.atraj.habittracker.email.EmailNotificationService
import it.atraj.habittracker.email.EmailResult
import it.atraj.habittracker.email.SecureEmailStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

enum class StatusType {
    SUCCESS, ERROR, INFO
}

data class EmailSettingsUiState(
    val isEnabled: Boolean = false,
    val email: String = "",
    val emailError: String? = null,
    val isSendingTest: Boolean = false,
    val statusMessage: String? = null,
    val statusType: StatusType = StatusType.INFO
)

@HiltViewModel
class EmailSettingsViewModel @Inject constructor(
    private val secureEmailStorage: SecureEmailStorage,
    private val emailService: EmailNotificationService,
    private val habitRepository: HabitRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailSettingsUiState())
    val uiState: StateFlow<EmailSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val isEnabled = secureEmailStorage.emailNotificationsEnabled
            val email = secureEmailStorage.userEmail ?: ""
            
            _uiState.update {
                it.copy(
                    isEnabled = isEnabled,
                    email = email
                )
            }
        }
    }

    fun setEmailEnabled(enabled: Boolean) {
        secureEmailStorage.emailNotificationsEnabled = enabled
        _uiState.update { it.copy(isEnabled = enabled) }
        
        if (enabled && _uiState.value.email.isBlank()) {
            _uiState.update {
                it.copy(
                    statusMessage = "Please enter your email address below",
                    statusType = StatusType.INFO
                )
            }
        }
    }

    fun setEmail(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                emailError = null
            )
        }
        
        // Validate and save email
        if (email.isNotBlank()) {
            if (isValidEmail(email)) {
                secureEmailStorage.userEmail = email
                _uiState.update { it.copy(emailError = null) }
            } else {
                _uiState.update { it.copy(emailError = "Invalid email address") }
            }
        }
    }

    fun sendTestEmail() {
        val email = _uiState.value.email
        
        if (!isValidEmail(email)) {
            _uiState.update {
                it.copy(
                    emailError = "Please enter a valid email address",
                    statusMessage = "Invalid email address",
                    statusType = StatusType.ERROR
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingTest = true, statusMessage = null) }
            
            try {
                // Create a test habit for the email
                val testHabit = createTestHabit()
                val userName = authRepository.currentUserSync?.displayName
                
                // Send test email
                val result = emailService.sendHabitReminderEmail(testHabit, userName)
                
                _uiState.update {
                    when (result) {
                        is EmailResult.Success -> it.copy(
                            isSendingTest = false,
                            statusMessage = "✅ Test email sent successfully! Check your inbox.",
                            statusType = StatusType.SUCCESS
                        )
                        is EmailResult.NotConfigured -> it.copy(
                            isSendingTest = false,
                            statusMessage = "⚠️ Email service is not configured properly.",
                            statusType = StatusType.ERROR
                        )
                        is EmailResult.NoRecipient -> it.copy(
                            isSendingTest = false,
                            statusMessage = "⚠️ No recipient email address found.",
                            statusType = StatusType.ERROR
                        )
                        is EmailResult.Error -> it.copy(
                            isSendingTest = false,
                            statusMessage = "❌ Failed to send: ${result.message}",
                            statusType = StatusType.ERROR
                        )
                    }
                }
                
                Log.d(TAG, "Test email result: $result")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send test email", e)
                _uiState.update {
                    it.copy(
                        isSendingTest = false,
                        statusMessage = "❌ Error: ${e.message ?: "Unknown error"}",
                        statusType = StatusType.ERROR
                    )
                }
            }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun createTestHabit(): Habit {
        val now = LocalTime.now()
        return Habit(
            id = 1L, // Dummy ID for test
            title = "Test Habit Reminder",
            description = "This is a test email to verify your email notifications are working correctly!",
            reminderHour = now.hour,
            reminderMinute = now.minute,
            reminderEnabled = true
        )
    }

    companion object {
        private const val TAG = "EmailSettingsViewModel"
    }
}

