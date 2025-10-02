# Quick Start: Enable Chat Notifications

## 3 Simple Steps to Enable Notifications

### Step 1: Install Firebase CLI
```bash
npm install -g firebase-tools
```

### Step 2: Deploy Cloud Functions
```bash
cd E:\CodingWorld\AndroidAppDev\HabitTracker
firebase login
firebase init functions
firebase deploy --only functions
```

### Step 3: Test
- Send a message from one device
- Receive notification on another device
- ✅ Done!

---

## Alternative: Test Now (Without Cloud Functions)

### Get Your FCM Token:
1. Install and open app
2. Check logcat output:
   ```
   adb logcat | findstr "FCM_TOKEN"
   ```
3. Copy the token shown

### Send Test Notification:
1. Go to https://console.firebase.google.com
2. Select HabitTracker project
3. Cloud Messaging → Send your first message
4. Click "Send test message"
5. Paste your FCM token
6. Click "Test"

---

## Quick Reference

**Cloud Functions File**: `functions/index.js` (already created)  
**Setup Guide**: `CLOUD_FUNCTIONS_SETUP.md`  
**Full Docs**: `NOTIFICATION_SETUP_COMPLETE.md`  

**Need Help?**
- Check function logs: `firebase functions:log`
- Check app logs: `adb logcat | grep HabitTracker`
- Verify token saved: Firebase Console → Firestore → users

---

**That's it!** Your chat notifications are ready to go. 🚀
