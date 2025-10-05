package it.atraj.habittracker.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import it.atraj.habittracker.data.firestore.ChatRepository
import it.atraj.habittracker.data.firestore.MessageType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ChatReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var chatRepository: ChatRepository

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(ChatMessagingService.KEY_TEXT_REPLY)?.toString()

        if (replyText.isNullOrBlank()) return

        val friendId = intent.getStringExtra(ChatMessagingService.EXTRA_FRIEND_ID) ?: return
        val friendName = intent.getStringExtra(ChatMessagingService.EXTRA_FRIEND_NAME) ?: return
        val friendAvatar = intent.getStringExtra(ChatMessagingService.EXTRA_FRIEND_AVATAR) ?: "😊"
        val friendPhotoUrl = intent.getStringExtra(ChatMessagingService.EXTRA_FRIEND_PHOTO_URL)
        val notificationId = intent.getIntExtra(ChatMessagingService.EXTRA_NOTIFICATION_ID, 0)

        receiverScope.launch {
            try {
                // Get current user
                val currentUser = FirebaseAuth.getInstance().currentUser ?: return@launch
                
                // Initialize chat repository manually if not injected
                val repository = chatRepository ?: ChatRepository(FirebaseFirestore.getInstance())
                
                // Get or create chat
                val chatResult = repository.getOrCreateChat(
                    userId1 = currentUser.uid,
                    userName1 = currentUser.displayName ?: "Me",
                    userAvatar1 = "😊",
                    userPhotoUrl1 = currentUser.photoUrl?.toString(),
                    userId2 = friendId,
                    userName2 = friendName,
                    userAvatar2 = friendAvatar,
                    userPhotoUrl2 = friendPhotoUrl
                )

                val chat = chatResult.getOrNull()
                if (chat != null) {
                    // Send message
                    repository.sendMessage(
                        chatId = chat.id,
                        senderId = currentUser.uid,
                        senderName = currentUser.displayName ?: "Me",
                        senderAvatar = "😊",
                        senderPhotoUrl = currentUser.photoUrl?.toString(),
                        content = replyText,
                        type = MessageType.TEXT
                    )

                    // Dismiss notification
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(notificationId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
