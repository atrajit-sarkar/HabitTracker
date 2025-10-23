package it.atraj.habittracker.data.local

import java.time.Instant

/**
 * App news/message data model
 */
data class AppNews(
    val id: String = "",
    val title: String = "",
    val content: String = "",  // Markdown content
    val timestamp: Long = System.currentTimeMillis(),
    val priority: String = "normal",  // low, normal, high, urgent
    val isRead: Boolean = false,
    val type: String = "news",
    val author: String = "Developer",
    val version: String = "1.0"
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "title" to title,
            "content" to content,
            "timestamp" to timestamp,
            "priority" to priority,
            "isRead" to isRead,
            "type" to type,
            "author" to author,
            "version" to version
        )
    }
    
    companion object {
        fun fromMap(id: String, map: Map<String, Any>): AppNews {
            return AppNews(
                id = id,
                title = map["title"] as? String ?: "",
                content = map["content"] as? String ?: "",
                timestamp = (map["timestamp"] as? com.google.firebase.Timestamp)?.seconds?.times(1000) 
                    ?: (map["timestamp"] as? Long ?: System.currentTimeMillis()),
                priority = map["priority"] as? String ?: "normal",
                isRead = map["isRead"] as? Boolean ?: false,
                type = map["type"] as? String ?: "news",
                author = map["author"] as? String ?: "Developer",
                version = map["version"] as? String ?: "1.0"
            )
        }
    }
}
