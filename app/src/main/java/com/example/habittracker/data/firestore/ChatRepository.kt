package it.atraj.habittracker.data.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "ChatRepository"
        private const val CHATS_COLLECTION = "chats"
        private const val MESSAGES_COLLECTION = "messages"
    }

    // Get or create chat between two users
    suspend fun getOrCreateChat(
        userId1: String,
        userName1: String,
        userAvatar1: String,
        userPhotoUrl1: String?,
        userId2: String,
        userName2: String,
        userAvatar2: String,
        userPhotoUrl2: String?
    ): Result<Chat> {
        return try {
            // Check if chat already exists
            val existingChat = firestore.collection(CHATS_COLLECTION)
                .whereArrayContains("participants", userId1)
                .get()
                .await()
                .documents
                .mapNotNull { it.toChat() }
                .firstOrNull { chat -> chat.participants.contains(userId2) }

            if (existingChat != null) {
                return Result.success(existingChat)
            }

            // Create new chat
            val chatId = firestore.collection(CHATS_COLLECTION).document().id
            val chat = Chat(
                id = chatId,
                participants = listOf(userId1, userId2),
                participantNames = mapOf(
                    userId1 to userName1,
                    userId2 to userName2
                ),
                participantAvatars = mapOf(
                    userId1 to userAvatar1,
                    userId2 to userAvatar2
                ),
                participantPhotoUrls = mapOf(
                    userId1 to userPhotoUrl1,
                    userId2 to userPhotoUrl2
                ),
                unreadCount = mapOf(userId1 to 0, userId2 to 0)
            )

            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .set(chat)
                .await()

            Result.success(chat)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting/creating chat: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Observe user's chats
    fun observeUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = firestore.collection(CHATS_COLLECTION)
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing chats: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { it.toChat() } ?: emptyList()
                trySend(chats)
            }

        awaitClose { listener.remove() }
    }

    // Observe messages in a chat
    fun observeChatMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing messages: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { it.toChatMessage() } ?: emptyList()
                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    // Send message
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        senderAvatar: String,
        senderPhotoUrl: String?,
        content: String,
        type: MessageType = MessageType.TEXT,
        replyTo: String? = null
    ): Result<Unit> {
        return try {
            val messageId = firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .document().id

            val message = ChatMessage(
                id = messageId,
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                senderPhotoUrl = senderPhotoUrl,
                content = content,
                type = type,
                replyTo = replyTo
            )

            // Send message
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .document(messageId)
                .set(message)
                .await()

            // Update chat's last message
            val chat = firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .get()
                .await()
                .toChat()

            if (chat != null) {
                val updatedUnreadCount = chat.unreadCount.toMutableMap()
                chat.participants.forEach { participantId ->
                    if (participantId != senderId) {
                        updatedUnreadCount[participantId] = (updatedUnreadCount[participantId] ?: 0) + 1
                    }
                }

                firestore.collection(CHATS_COLLECTION)
                    .document(chatId)
                    .update(
                        mapOf(
                            "lastMessage" to content,
                            "lastMessageType" to type.name,
                            "lastMessageSenderId" to senderId,
                            "lastMessageTimestamp" to System.currentTimeMillis(),
                            "unreadCount" to updatedUnreadCount,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
                
                // NOTE: FCM notifications are sent automatically via Firebase Cloud Functions
                // When this message is written to Firestore, a trigger will fire and send
                // the notification to the recipient. No client-side action needed.
                Log.d(TAG, "Message sent successfully. Cloud Function will send notification.")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Mark messages as read
    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("unreadCount.$userId", 0)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Delete message
    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> {
        return try {
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .document(messageId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message: ${e.message}", e)
            Result.failure(e)
        }
    }
}
