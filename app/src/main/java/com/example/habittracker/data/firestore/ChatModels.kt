package it.atraj.habittracker.data.firestore

import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String? = null, // Nullable - null if no custom avatar
    val senderPhotoUrl: String? = null,
    val content: String = "",
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val replyTo: String? = null // Message ID being replied to
)

enum class MessageType {
    TEXT,
    EMOJI,
    STICKER,
    IMAGE
}

@Serializable
data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(), // User IDs
    val participantNames: Map<String, String> = emptyMap(),
    val participantAvatars: Map<String, String?> = emptyMap(), // Nullable avatars
    val participantPhotoUrls: Map<String, String?> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageType: String = MessageType.TEXT.name,
    val lastMessageSenderId: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Map<String, Int> = emptyMap(), // userId -> count
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Sticker packs for chat
object StickerPacks {
    val EMOJI_REACTIONS = listOf(
        "👍", "❤️", "😂", "😮", "😢", "😡",
        "🎉", "🔥", "⭐", "💯", "👏", "🙏"
    )
    
    val CELEBRATION = listOf(
        "🎉", "🎊", "🥳", "🎈", "🎁", "🏆",
        "🌟", "✨", "💫", "🎆", "🎇", "🍾"
    )
    
    val MOTIVATION = listOf(
        "💪", "🔥", "⚡", "🚀", "🎯", "💯",
        "👊", "🏋️", "🤸", "🧘", "🏃", "⛰️"
    )
    
    val EMOTIONS = listOf(
        "😊", "😃", "😄", "😁", "😆", "😅",
        "😂", "🤣", "😇", "😍", "🥰", "😘",
        "😋", "😎", "🤩", "🥺", "😢", "😭"
    )
    
    val ANIMALS = listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊",
        "🐻", "🐼", "🐨", "🐯", "🦁", "🐮"
    )
    
    val NATURE = listOf(
        "🌸", "🌺", "🌻", "🌷", "🌹", "🏵️",
        "🌲", "🌳", "🌴", "🌵", "🍀", "🌿"
    )
    
    fun getAllPacks(): Map<String, List<String>> = mapOf(
        "Reactions" to EMOJI_REACTIONS,
        "Celebration" to CELEBRATION,
        "Motivation" to MOTIVATION,
        "Emotions" to EMOTIONS,
        "Animals" to ANIMALS,
        "Nature" to NATURE
    )
}

// Extension functions
fun DocumentSnapshot.toChatMessage(): ChatMessage? {
    return try {
        val data = data ?: return null
        ChatMessage(
            id = id,
            chatId = data["chatId"] as? String ?: "",
            senderId = data["senderId"] as? String ?: "",
            senderName = data["senderName"] as? String ?: "",
            senderAvatar = data["senderAvatar"] as? String ?: "😊",
            senderPhotoUrl = data["senderPhotoUrl"] as? String,
            content = data["content"] as? String ?: "",
            type = MessageType.valueOf(data["type"] as? String ?: MessageType.TEXT.name),
            timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis(),
            isRead = data["isRead"] as? Boolean ?: false,
            replyTo = data["replyTo"] as? String
        )
    } catch (e: Exception) {
        null
    }
}

fun DocumentSnapshot.toChat(): Chat? {
    return try {
        val data = data ?: return null
        Chat(
            id = id,
            participants = (data["participants"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            participantNames = (data["participantNames"] as? Map<*, *>)?.mapNotNull { (k, v) -> 
                (k as? String)?.let { key -> (v as? String)?.let { value -> key to value } }
            }?.toMap() ?: emptyMap(),
            participantAvatars = (data["participantAvatars"] as? Map<*, *>)?.mapNotNull { (k, v) -> 
                (k as? String)?.let { key -> (v as? String)?.let { value -> key to value } }
            }?.toMap() ?: emptyMap(),
            participantPhotoUrls = (data["participantPhotoUrls"] as? Map<*, *>)?.mapNotNull { (k, v) -> 
                (k as? String)?.let { key -> key to (v as? String) }
            }?.toMap() ?: emptyMap(),
            lastMessage = data["lastMessage"] as? String ?: "",
            lastMessageType = data["lastMessageType"] as? String ?: MessageType.TEXT.name,
            lastMessageSenderId = data["lastMessageSenderId"] as? String ?: "",
            lastMessageTimestamp = data["lastMessageTimestamp"] as? Long ?: System.currentTimeMillis(),
            unreadCount = (data["unreadCount"] as? Map<*, *>)?.mapNotNull { (k, v) -> 
                (k as? String)?.let { key -> (v as? Long)?.toInt()?.let { value -> key to value } }
            }?.toMap() ?: emptyMap(),
            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
            updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis()
        )
    } catch (e: Exception) {
        null
    }
}
