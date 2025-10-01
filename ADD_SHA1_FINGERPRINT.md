# ⚠️ CRITICAL: Add SHA-1 Fingerprint to Firebase

## 🔴 Action Required Immediately

Your Google Sign-In is likely failing because the **SHA-1 fingerprint is not registered** in Firebase Console.

## Your SHA-1 Fingerprint

```
15:E0:7A:B4:00:E7:EA:E1:BB:D5:20:DA:15:DC:13:9A:44:D2:8D:21
```

## Step-by-Step Instructions

### 1. Go to Firebase Console
🔗 https://console.firebase.google.com

### 2. Select Your Project
- Project: **habit-tracker-56079**
- Project Number: **328687985941**

### 3. Open Project Settings
- Click the **⚙️ gear icon** (top left, next to "Project Overview")
- Select **"Project settings"**

### 4. Find Your Android App
- Scroll down to **"Your apps"** section
- You should see your Android app:
  - Package: **com.example.habittracker**
  - App nickname: **Habit Tracker** (or similar)

### 5. Add SHA-1 Fingerprint
- Under your Android app, find the **"SHA certificate fingerprints"** section
- Click **"Add fingerprint"** button
- Paste this SHA-1 fingerprint:
  ```
  15:E0:7A:B4:00:E7:EA:E1:BB:D5:20:DA:15:DC:13:9A:44:D2:8D:21
  ```
- Click **"Save"**

### 6. Wait a Few Minutes
- Google needs to propagate this change (~5 minutes)
- You do NOT need to download a new `google-services.json`
- The SHA-1 is stored server-side

### 7. Test Again
After adding the SHA-1 and waiting 5 minutes:

1. **Uninstall the app** from your device/emulator:
   ```powershell
   adb uninstall com.example.habittracker
   ```

2. **Reinstall the app**:
   ```powershell
   .\gradlew installDebug
   ```

3. **Try Google Sign-In** again

4. **Check Logcat** for success messages:
   ```powershell
   adb logcat -s AuthScreen:D AuthRepository:D
   ```

## Why SHA-1 is Required

### Security Verification
Google uses the SHA-1 fingerprint to verify that sign-in requests are coming from your legitimate app, not a malicious clone.

### How It Works
```
1. Your app requests Google Sign-In
   ↓
2. Google checks the app's signature (SHA-1)
   ↓
3. Google verifies SHA-1 matches Firebase registration
   ↓
4. If match: Provides ID token
   If no match: DEVELOPER_ERROR (10)
```

## Expected Result After Adding SHA-1

### Before (Current):
```
❌ Click "Continue with Google"
❌ Select Google account
❌ Dialog closes
❌ Nothing happens or error message
```

### After (Expected):
```
✅ Click "Continue with Google"
✅ Select Google account
✅ Dialog closes
✅ Successfully signed in!
✅ Redirected to main app screen
✅ Profile shows your Google name
```

## Verification Checklist

After adding SHA-1, verify:

### In Firebase Console:
- [ ] SHA-1 fingerprint added to your Android app
- [ ] Google Sign-In provider is **enabled** in Authentication
- [ ] No red warning icons in Project Settings

### In Your App (Logcat):
- [ ] See: `D/AuthRepository: Starting Google sign-in with ID token`
- [ ] See: `D/AuthRepository: Google sign-in successful for user: <uid>`
- [ ] See: `D/AuthRepository: Initialized user document for Google user`
- [ ] NO errors about DEVELOPER_ERROR

### In Firebase Console (after successful sign-in):
- [ ] New user appears in Authentication → Users
- [ ] User document created in Firestore → users → [uid]
- [ ] Document contains `customDisplayName` field

## Additional SHA Fingerprints

For reference, here are all your fingerprints:

### Debug Keystore:
- **MD5:** `8B:6A:D4:FF:31:17:67:4A:CF:0C:A4:F1:ED:53:B5:5C`
- **SHA-1:** `15:E0:7A:B4:00:E7:EA:E1:BB:D5:20:DA:15:DC:13:9A:44:D2:8D:21`
- **SHA-256:** `6E:81:62:72:13:0E:DF:6E:A9:2A:72:C8:0F:F0:CC:C1:7D:AF:59:B8:EA:9B:FD:F7:C6:C0:05:9F:2B:B3:0F:30`
- **Valid until:** Thursday, April 8, 2055

### Notes:
- Currently only **SHA-1** is required for Google Sign-In
- You may add SHA-256 for additional security (optional)
- For production release, you'll need the SHA-1 from your **release keystore**

## For Production Release

When you create a release build, you'll need to:

1. **Generate a release keystore** (if you haven't already)
2. **Get the SHA-1 from release keystore**
3. **Add that SHA-1 to Firebase** as well
4. **Keep both debug and release SHA-1s registered**

### Get Release SHA-1 Later:
```powershell
.\gradlew signingReport
# or
keytool -list -v -keystore path\to\your\release.keystore -alias your-alias
```

## Common Questions

### Q: Do I need to download a new google-services.json?
**A:** No, the SHA-1 is stored server-side. The existing `google-services.json` is fine.

### Q: How long until the SHA-1 takes effect?
**A:** Usually 2-5 minutes. Sometimes up to 10 minutes.

### Q: Can I add multiple SHA-1 fingerprints?
**A:** Yes! You should add both debug and release fingerprints.

### Q: What if I see "DEVELOPER_ERROR" in logs?
**A:** This specifically means SHA-1 mismatch. Double-check:
1. SHA-1 is correctly entered in Firebase (no typos)
2. You've waited 5+ minutes
3. You've reinstalled the app

### Q: What if sign-in still doesn't work after adding SHA-1?
**A:** Check these in order:
1. Is Google Sign-In **enabled** in Firebase Console?
2. Is the Web Client ID correct in `GoogleSignInHelper.kt`?
3. Do you have internet connection?
4. Check Logcat for specific error messages

## Quick Test Command

After adding SHA-1 and waiting 5 minutes:

```powershell
# Uninstall, reinstall, and watch logs
adb uninstall com.example.habittracker; .\gradlew installDebug; adb logcat -s AuthScreen:D AuthRepository:D
```

Then try signing in with Google and watch the logs!

---

## 🎯 Next Step

**RIGHT NOW:** Go add this SHA-1 fingerprint to Firebase Console!

```
15:E0:7A:B4:00:E7:EA:E1:BB:D5:20:DA:15:DC:13:9A:44:D2:8D:21
```

Firebase Console: https://console.firebase.google.com/project/habit-tracker-56079/settings/general/

After adding it, wait 5 minutes, then reinstall and test! 🚀
