package com.example.habittracker.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.auth.User
import com.example.habittracker.data.firestore.Chat
import com.example.habittracker.data.firestore.ChatMessage
import com.example.habittracker.data.firestore.ChatRepository
import com.example.habittracker.data.firestore.MessageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val currentUser: User? = null,
    val chats: List<Chat> = emptyList(),
    val isLoadingChats: Boolean = false,
    val currentChat: Chat? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isLoadingMessages: Boolean = false,
    val isSendingMessage: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun setCurrentUser(user: User) {
        _uiState.update { it.copy(currentUser = user) }
        loadUserChats(user.uid)
    }

    private fun loadUserChats(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChats = true) }
            
            try {
                chatRepository.observeUserChats(userId).collect { chats ->
                    _uiState.update { 
                        it.copy(
                            chats = chats,
                            isLoadingChats = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chats: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoadingChats = false,
                        error = "Failed to load chats"
                    )
                }
            }
        }
    }

    fun openOrCreateChat(
        friendId: String,
        friendName: String,
        friendAvatar: String,
        friendPhotoUrl: String?
    ) {
        val user = _uiState.value.currentUser ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMessages = true) }
            
            try {
                val result = chatRepository.getOrCreateChat(
                    userId1 = user.uid,
                    userName1 = user.effectiveDisplayName,
                    userAvatar1 = user.customAvatar ?: "😊",
                    userPhotoUrl1 = user.photoUrl,
                    userId2 = friendId,
                    userName2 = friendName,
                    userAvatar2 = friendAvatar,
                    userPhotoUrl2 = friendPhotoUrl
                )
                
                result.onSuccess { chat ->
                    _uiState.update { it.copy(currentChat = chat) }
                    loadChatMessages(chat.id)
                    markMessagesAsRead(chat.id)
                }.onFailure { error ->
                    Log.e(TAG, "Error opening chat: ${error.message}", error)
                    _uiState.update { 
                        it.copy(
                            isLoadingMessages = false,
                            error = "Failed to open chat"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error opening chat: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoadingMessages = false,
                        error = "Failed to open chat"
                    )
                }
            }
        }
    }

    fun openExistingChat(chat: Chat) {
        _uiState.update { it.copy(currentChat = chat) }
        loadChatMessages(chat.id)
        markMessagesAsRead(chat.id)
    }

    private fun loadChatMessages(chatId: String) {
        viewModelScope.launch {
            try {
                chatRepository.observeChatMessages(chatId).collect { messages ->
                    _uiState.update { 
                        it.copy(
                            messages = messages,
                            isLoadingMessages = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading messages: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoadingMessages = false,
                        error = "Failed to load messages"
                    )
                }
            }
        }
    }

    fun sendMessage(
        content: String,
        type: MessageType = MessageType.TEXT,
        replyTo: String? = null
    ) {
        val user = _uiState.value.currentUser ?: return
        val chat = _uiState.value.currentChat ?: return
        
        if (content.isBlank() && type == MessageType.TEXT) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingMessage = true) }
            
            try {
                val result = chatRepository.sendMessage(
                    chatId = chat.id,
                    senderId = user.uid,
                    senderName = user.effectiveDisplayName,
                    senderAvatar = user.customAvatar ?: "😊",
                    senderPhotoUrl = user.photoUrl,
                    content = content,
                    type = type,
                    replyTo = replyTo
                )
                
                result.onSuccess {
                    _uiState.update { it.copy(isSendingMessage = false) }
                }.onFailure { error ->
                    Log.e(TAG, "Error sending message: ${error.message}", error)
                    _uiState.update { 
                        it.copy(
                            isSendingMessage = false,
                            error = "Failed to send message"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isSendingMessage = false,
                        error = "Failed to send message"
                    )
                }
            }
        }
    }

    private fun markMessagesAsRead(chatId: String) {
        val userId = _uiState.value.currentUser?.uid ?: return
        
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(chatId, userId)
        }
    }

    fun closeChat() {
        _uiState.update { 
            it.copy(
                currentChat = null,
                messages = emptyList()
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
