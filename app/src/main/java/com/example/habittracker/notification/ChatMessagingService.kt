package it.atraj.habittracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import it.atraj.habittracker.MainActivity
import it.atraj.habittracker.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.URL

class ChatMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "chat_messages"
        const val CHANNEL_NAME = "Chat Messages"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val ACTION_REPLY = "it.atraj.habittracker.ACTION_REPLY"
        const val EXTRA_FRIEND_ID = "extra_friend_id"
        const val EXTRA_FRIEND_NAME = "extra_friend_name"
        const val EXTRA_FRIEND_AVATAR = "extra_friend_avatar"
        const val EXTRA_FRIEND_PHOTO_URL = "extra_friend_photo_url"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val friendId = data["friendId"] ?: return
        val friendName = data["friendName"] ?: "Friend"
        val friendAvatar = data["friendAvatar"] ?: "😊"
        val friendPhotoUrl = data["friendPhotoUrl"]
        val messageContent = data["messageContent"] ?: ""
        val messageType = data["messageType"] ?: "TEXT"

        serviceScope.launch {
            showChatNotification(
                friendId = friendId,
                friendName = friendName,
                friendAvatar = friendAvatar,
                friendPhotoUrl = friendPhotoUrl,
                messageContent = messageContent,
                messageType = messageType
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save FCM token to Firestore for this user
        saveFcmTokenToFirestore(token)
    }

    private fun saveFcmTokenToFirestore(token: String) {
        serviceScope.launch {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                
                if (currentUserId != null) {
                    firestore.collection("users").document(currentUserId)
                        .update("fcmToken", token)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun showChatNotification(
        friendId: String,
        friendName: String,
        friendAvatar: String,
        friendPhotoUrl: String?,
        messageContent: String,
        messageType: String
    ) {
        val notificationId = friendId.hashCode()

        // Create intent for opening chat
        val chatIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openChat", true)
            putExtra(EXTRA_FRIEND_ID, friendId)
            putExtra(EXTRA_FRIEND_NAME, friendName)
            putExtra(EXTRA_FRIEND_AVATAR, friendAvatar)
            putExtra(EXTRA_FRIEND_PHOTO_URL, friendPhotoUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            chatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create reply action
        val replyIntent = Intent(this, ChatReplyReceiver::class.java).apply {
            putExtra(EXTRA_FRIEND_ID, friendId)
            putExtra(EXTRA_FRIEND_NAME, friendName)
            putExtra(EXTRA_FRIEND_AVATAR, friendAvatar)
            putExtra(EXTRA_FRIEND_PHOTO_URL, friendPhotoUrl)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply")
            .build()

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        // Load profile picture
        val profileBitmap = friendPhotoUrl?.let { loadBitmapFromUrl(it) }

        // Create person for messaging style
        val person = Person.Builder()
            .setName(friendName)
            .setIcon(
                profileBitmap?.let { IconCompat.createWithBitmap(it) }
                    ?: IconCompat.createWithResource(this, R.drawable.ic_launcher_foreground)
            )
            .build()

        // Format message content based on type
        val displayContent = when (messageType) {
            "STICKER", "EMOJI" -> "$messageContent (sticker)"
            "IMAGE" -> "📷 Photo"
            else -> messageContent
        }

        // Build notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(profileBitmap)
            .setContentTitle(friendName)
            .setContentText(displayContent)
            .setStyle(
                NotificationCompat.MessagingStyle(
                    Person.Builder().setName("You").build()
                )
                    .addMessage(displayContent, System.currentTimeMillis(), person)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(replyAction)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private suspend fun loadBitmapFromUrl(url: String): Bitmap? {
        return try {
            val connection = URL(url).openConnection()
            connection.doInput = true
            connection.connect()
            val input = connection.getInputStream()
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
