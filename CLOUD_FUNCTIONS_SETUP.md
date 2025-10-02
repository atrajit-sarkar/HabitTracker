# Firebase Cloud Functions Setup Guide

## Prerequisites

1. **Node.js** (v18 or higher): [Download](https://nodejs.org/)
2. **Firebase CLI**: Install globally
   ```bash
   npm install -g firebase-tools
   ```
3. **Firebase Project**: Your existing HabitTracker project

## Setup Steps

### 1. Login to Firebase

```bash
firebase login
```

### 2. Initialize Firebase Functions

```bash
cd E:\CodingWorld\AndroidAppDev\HabitTracker
firebase init functions
```

**Select**:
- Use an existing project: HabitTracker
- Language: JavaScript
- ESLint: No (optional)
- Install dependencies: Yes

### 3. Copy Function Code

The `functions/index.js` file is already created with the notification logic.

### 4. Install Dependencies

```bash
cd functions
npm install
```

### 5. Deploy to Firebase

```bash
firebase deploy --only functions
```

**Expected output**:
```
✔  functions[sendChatNotification(us-central1)] Successful create operation.
Function URL: https://us-central1-habittracker-xxxxx.cloudfunctions.net/sendChatNotification
```

## How It Works

1. **User sends message** → Message written to Firestore
2. **Cloud Function triggers** → Detects new message in `chats/{chatId}/messages/{messageId}`
3. **Function gets recipient** → Looks up chat participants
4. **Function gets FCM token** → Retrieves token from `users/{userId}/fcmToken`
5. **Notification sent** → Uses Firebase Admin SDK to send FCM message
6. **User receives notification** → `ChatMessagingService` handles it in the app

## Verify It's Working

### Check Function Logs

```bash
firebase functions:log
```

### Test the Function

1. Open your app on two devices (or one device + emulator)
2. Send a message from Device A
3. Check Device B receives notification

### Troubleshoot

**No notification received?**

1. Check logs: `firebase functions:log`
2. Verify FCM token saved:
   - Open Firebase Console → Firestore
   - Navigate to `users/{userId}`
   - Confirm `fcmToken` field exists

3. Check logcat on sender device:
   ```
   adb logcat | grep "FCM_TOKEN"
   ```
   Copy the token and test manually in Firebase Console

## Cost

**Free Tier (Spark Plan)**:
- 125,000 function invocations/month
- 40,000 GB-seconds compute time
- 2,000 GB network egress

**Blaze Plan (Pay-as-you-go)**:
- Same free tier, then pay only for extra usage
- ~$0.40 per million invocations
- First 2 million invocations always free

For a chat app with moderate usage, you'll likely stay within free tier.

## Alternative: Test Without Cloud Functions

If you want to test notifications immediately without deploying Cloud Functions:

### Method 1: Firebase Console (Manual)

1. Get your FCM token from logcat:
   ```
   D/FCM_TOKEN: Token: xxxxxxxxxxxxx
   ```

2. Go to [Firebase Console](https://console.firebase.google.com/) → Cloud Messaging

3. Click "Send your first message"

4. Fill in:
   - **Title**: `New Message`
   - **Text**: `You have a new message`
   - Click "Send test message"
   - Paste your FCM token

### Method 2: Use Postman/cURL (Advanced)

Get your Firebase Server Key from Firebase Console → Project Settings → Cloud Messaging

```bash
curl -X POST https://fcm.googleapis.com/fcm/send \
  -H "Authorization: key=YOUR_SERVER_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "RECIPIENT_FCM_TOKEN",
    "data": {
      "friendId": "sender123",
      "friendName": "John Doe",
      "friendAvatar": "😊",
      "friendPhotoUrl": "",
      "messageContent": "Hello!",
      "messageType": "TEXT"
    },
    "android": {
      "priority": "high"
    }
  }'
```

**⚠️ NEVER commit your server key to git!**

## Recommended Approach

1. **For Development**: Use manual testing (Firebase Console) 
2. **For Production**: Deploy Cloud Functions (automatic and secure)

## Next Steps

After deploying Cloud Functions:

1. ✅ Send a message from User A
2. ✅ User B automatically receives notification
3. ✅ Tap notification → Opens chat
4. ✅ Reply from notification → Message sent

Everything else is already implemented in the app!
