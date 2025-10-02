# 🔗 Chat Feature Integration Guide

## Quick Integration Steps

### Step 1: Add Chat Button to Friend Profile Screen

Open `FriendProfileScreen.kt` and add a "Message" button in the actions row:

```kotlin
// Around line 200, in the actions row with Follow button
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
    // Existing Follow/Unfollow button
    // ... existing code ...
    
    // Add Message button
    Button(
        onClick = { 
            onMessageClick(
                friendId = friendProfile.userId,
                friendName = friendProfile.displayName,
                friendAvatar = friendProfile.customAvatar,
                friendPhotoUrl = friendProfile.photoUrl
            )
        },
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("Message")
    }
}
```

Update the composable signature:
```kotlin
@Composable
fun FriendProfileScreen(
    friendId: String,
    authViewModel: AuthViewModel = hiltViewModel(),
    socialViewModel: SocialViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onMessageClick: (String, String, String, String?) -> Unit  // Add this
) {
```

### Step 2: Add Navigation Routes

Open your main `Navigation.kt` (or wherever you define routes) and add:

```kotlin
// Add these constants at the top
object Route {
    const val CHAT_LIST = "chatList"
    const val CHAT = "chat/{friendId}/{friendName}/{friendAvatar}/{friendPhotoUrl}"
    
    fun chat(friendId: String, friendName: String, friendAvatar: String, friendPhotoUrl: String?) =
        "chat/$friendId/$friendName/$friendAvatar/${friendPhotoUrl ?: "null"}"
}

// In your NavHost:
NavHost(
    navController = navController,
    startDestination = "home"
) {
    // ... existing routes ...
    
    // Chat list screen
    composable(Route.CHAT_LIST) {
        ChatListScreen(
            onBackClick = { navController.popBackStack() },
            onChatClick = { chat ->
                val currentUserId = /* get current user ID */
                val otherUserId = chat.participants.first { it != currentUserId }
                val otherUserName = chat.participantNames[otherUserId] ?: "Unknown"
                val otherUserAvatar = chat.participantAvatars[otherUserId] ?: "😊"
                val otherUserPhotoUrl = chat.participantPhotoUrls[otherUserId]
                
                navController.navigate(
                    Route.chat(otherUserId, otherUserName, otherUserAvatar, otherUserPhotoUrl)
                )
            }
        )
    }
    
    // Individual chat screen
    composable(
        route = Route.CHAT,
        arguments = listOf(
            navArgument("friendId") { type = NavType.StringType },
            navArgument("friendName") { type = NavType.StringType },
            navArgument("friendAvatar") { type = NavType.StringType },
            navArgument("friendPhotoUrl") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
        val friendName = backStackEntry.arguments?.getString("friendName") ?: ""
        val friendAvatar = backStackEntry.arguments?.getString("friendAvatar") ?: "😊"
        val friendPhotoUrl = backStackEntry.arguments?.getString("friendPhotoUrl")
            ?.takeIf { it != "null" }
        
        ChatScreen(
            friendId = friendId,
            friendName = friendName,
            friendAvatar = friendAvatar,
            friendPhotoUrl = friendPhotoUrl,
            onBackClick = { navController.popBackStack() }
        )
    }
    
    // Update Friend Profile route
    composable(
        route = "friendProfile/{friendId}",
        arguments = listOf(navArgument("friendId") { type = NavType.StringType })
    ) { backStackEntry ->
        FriendProfileScreen(
            friendId = backStackEntry.arguments?.getString("friendId") ?: "",
            onBackClick = { navController.popBackStack() },
            onMessageClick = { friendId, friendName, friendAvatar, friendPhotoUrl ->
                navController.navigate(
                    Route.chat(friendId, friendName, friendAvatar, friendPhotoUrl)
                )
            }
        )
    }
}
```

### Step 3: Add Chat Icon to Bottom Navigation (Optional)

If you have bottom navigation, add a chat icon:

```kotlin
NavigationBar {
    // ... existing items ...
    
    NavigationBarItem(
        icon = { 
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        Badge { Text(unreadCount.toString()) }
                    }
                }
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Messages")
            }
        },
        label = { Text("Messages") },
        selected = currentRoute == Route.CHAT_LIST,
        onClick = { navController.navigate(Route.CHAT_LIST) }
    )
}
```

### Step 4: Add Message FAB to Friends List (Optional)

In `FriendsListScreen.kt`, add a floating action button:

```kotlin
Scaffold(
    topBar = { /* ... */ },
    floatingActionButton = {
        FloatingActionButton(
            onClick = { navController.navigate(Route.CHAT_LIST) }
        ) {
            Icon(Icons.Default.Chat, contentDescription = "Messages")
        }
    }
) { /* ... */ }
```

### Step 5: Test the Feature

1. **Build and install** the APK
2. **Go to a friend's profile**
3. **Tap "Message"** button
4. **Send a text message** - type and tap send
5. **Try stickers** - tap 😊 button, select a pack, tap a sticker
6. **Try emojis** - tap 😀 button, select an emoji
7. **Check chat list** - navigate to chat list to see all conversations

## Testing Checklist

- ✅ Send text message
- ✅ Send emoji (large display)
- ✅ Send sticker (large display)
- ✅ Switch between sticker packs
- ✅ Insert emoji into text
- ✅ Message appears instantly on both devices
- ✅ Unread counter updates
- ✅ Last message preview shows in list
- ✅ Timestamps format correctly
- ✅ Scroll to bottom on new message
- ✅ Empty state shows helpful message

## Quick Fixes

### If messages don't appear:
1. Check Firestore rules allow read/write to `chats` collection
2. Ensure both users are authenticated
3. Check console for Firebase errors

### If stickers don't show:
- Make sure you're clicking on the sticker (not just opening the picker)
- Check the message type is being sent correctly

### If navigation fails:
- Verify route parameters are being passed correctly
- Check navController is in scope
- Ensure `friendPhotoUrl` handles null values

## Firestore Security Rules

Add these rules to your `firestore.rules`:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Chat rules
    match /chats/{chatId} {
      // Allow read if user is a participant
      allow read: if request.auth != null && 
                    request.auth.uid in resource.data.participants;
      
      // Allow create if user is one of the participants
      allow create: if request.auth != null && 
                      request.auth.uid in request.resource.data.participants;
      
      // Allow update if user is a participant
      allow update: if request.auth != null && 
                      request.auth.uid in resource.data.participants;
      
      // Messages subcollection
      match /messages/{messageId} {
        // Allow read if user is chat participant
        allow read: if request.auth != null && 
                      request.auth.uid in get(/databases/$(database)/documents/chats/$(chatId)).data.participants;
        
        // Allow create if user is sender and is chat participant
        allow create: if request.auth != null && 
                        request.auth.uid == request.resource.data.senderId &&
                        request.auth.uid in get(/databases/$(database)/documents/chats/$(chatId)).data.participants;
        
        // Allow delete if user is sender
        allow delete: if request.auth != null && 
                        request.auth.uid == resource.data.senderId;
      }
    }
  }
}
```

## Done! 🎉

Your chat feature is now fully integrated! Users can:
- ✅ Send text messages
- ✅ Send emojis and stickers
- ✅ See real-time updates
- ✅ Track unread messages
- ✅ View all conversations in one place

The chat system follows WhatsApp-style UX patterns, making it instantly familiar to users.
