# FCM Notification Setup - Server-Side Required

## Why Notifications Aren't Working

Currently, notifications are **NOT being sent** because:

1. **Client-Side FCM Sending is Blocked**: Firebase doesn't allow apps to send notifications directly to other users for security reasons
2. **Server Key Required**: You need a Firebase Server Key, which should NEVER be embedded in the app
3. **We Only Have Client SDK**: The app can only receive notifications, not send them

## Current Implementation Status

✅ **Receiving Notifications**: `ChatMessagingService` is ready and working
✅ **Saving FCM Tokens**: Tokens are saved to Firestore users collection
✅ **Inline Reply**: `ChatReplyReceiver` can handle replies
✅ **Navigation**: Tapping notifications opens correct chat
❌ **Sending Notifications**: Requires server-side implementation

## Solutions (Choose One)

### Solution 1: Firebase Cloud Functions (Recommended) ⭐

**Cost**: Free tier: 125K invocations/month, 40K GB-seconds/month

**Steps**:

1. Install Firebase CLI:
   ```bash
   npm install -g firebase-tools
   firebase login
   firebase init functions
   ```

2. Create Cloud Function (`functions/index.js`):
   ```javascript
   const functions = require('firebase-functions');
   const admin = require('firebase-admin');
   admin.initializeApp();

   exports.sendChatNotification = functions.firestore
       .document('chats/{chatId}/messages/{messageId}')
       .onCreate(async (snap, context) => {
           const message = snap.data();
           const chatId = context.params.chatId;
           
           // Get chat details
           const chatDoc = await admin.firestore()
               .collection('chats')
               .doc(chatId)
               .get();
           
           const chat = chatDoc.data();
           
           // Send notification to all participants except sender
           const promises = chat.participants
               .filter(participantId => participantId !== message.senderId)
               .map(async (recipientId) => {
                   // Get recipient's FCM token
                   const userDoc = await admin.firestore()
                       .collection('users')
                       .doc(recipientId)
                       .get();
                   
                   const fcmToken = userDoc.data()?.fcmToken;
                   if (!fcmToken) return;
                   
                   // Send notification
                   return admin.messaging().send({
                       token: fcmToken,
                       data: {
                           friendId: message.senderId,
                           friendName: message.senderName,
                           friendAvatar: message.senderAvatar,
                           friendPhotoUrl: message.senderPhotoUrl || '',
                           messageContent: message.content,
                           messageType: message.type
                       },
                       android: {
                           priority: 'high'
                       }
                   });
               });
           
           return Promise.all(promises);
       });
   ```

3. Deploy:
   ```bash
   firebase deploy --only functions
   ```

**Pros**:
- ✅ Automatic - triggers on new messages
- ✅ Secure - server key never exposed
- ✅ Scalable
- ✅ Free for most apps

**Cons**:
- ❌ Requires Firebase Blaze (pay-as-you-go) plan
- ❌ Need Node.js knowledge

---

### Solution 2: Simple Backend API (Alternative)

Create a simple Express.js server:

```javascript
const express = require('express');
const admin = require('firebase-admin');

admin.initializeApp({
  credential: admin.credential.cert(require('./serviceAccountKey.json'))
});

const app = express();
app.use(express.json());

app.post('/send-notification', async (req, res) => {
  const { recipientToken, data } = req.body;
  
  try {
    await admin.messaging().send({
      token: recipientToken,
      data: data,
      android: { priority: 'high' }
    });
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.listen(3000);
```

Deploy to Heroku, Railway, or Render.

---

### Solution 3: Quick Test Without Server (Temporary)

For **testing only**, you can manually trigger notifications using Firebase Console:

1. Go to Firebase Console → Cloud Messaging
2. Click "Send your first message"
3. Enter:
   - **Notification title**: Friend name
   - **Notification text**: Message content
4. Click "Send test message"
5. Paste your device's FCM token

**Get your FCM token**:
Add this to `MainActivity.onCreate()`:
```kotlin
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        Log.d("FCM_TOKEN", "Token: ${task.result}")
    }
}
```

Check logcat for: `D/FCM_TOKEN: Token: ...`

---

## What I Recommend

### For Development/Testing:
Use **Solution 3** (manual testing) to verify everything works

### For Production:
Use **Solution 1** (Cloud Functions) - it's the most reliable and automatic

## Update App Code to Remove Client-Side Sending

Since client-side sending doesn't work anyway, let's remove that code and rely on Cloud Functions:

