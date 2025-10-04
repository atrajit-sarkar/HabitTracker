# 🔧 Google Sign-In Not Working - Release APK Fix

## 🔍 Problem Identified

You just created a **NEW production keystore** (`habit-tracker-release.jks`) and installed the **signed release APK**. Google Sign-In is not working because **Firebase doesn't recognize your new release keystore's SHA-1 fingerprint**.

---

## 📊 Your SHA-1 Fingerprints

You have **TWO different keystores** with different SHA-1 fingerprints:

### 1. Debug Keystore (Working)
- **Keystore:** `C:\Users\atraj\.android\debug.keystore`
- **SHA-1:** `15:E0:7A:B4:00:E7:EA:E1:BB:D5:20:DA:15:DC:13:9A:44:D2:8D:21`
- **Status:** ✅ Already added to Firebase (Google Sign-In works with debug builds)

### 2. Release Keystore (NEW - Not Working)
- **Keystore:** `habit-tracker-release.jks`
- **SHA-1:** `83:12:B1:6E:B1:21:B6:1C:20:AD:63:98:01:DF:0C:44:15:7C:F6:FC`
- **Status:** ❌ NOT added to Firebase (Google Sign-In fails with release builds)

---

## ✅ Solution: Add Release SHA-1 to Firebase

### Step 1: Go to Firebase Console
Open: https://console.firebase.google.com/project/habit-tracker-56079/settings/general/

### Step 2: Find Your Android App
Scroll down to **"Your apps"** section and find your Android app

### Step 3: Add the Release SHA-1 Fingerprint
1. Click **"Add fingerprint"** button
2. Paste this SHA-1:
   ```
   83:12:B1:6E:B1:21:B6:1C:20:AD:63:98:01:DF:0C:44:15:7C:F6:FC
   ```
3. Click **"Save"**

### Step 4: Download Updated google-services.json (Optional)
Firebase will automatically update the configuration. You can optionally:
1. Click **"Download google-services.json"**
2. Replace the file in `app/google-services.json`
3. Rebuild the app

**Note:** This is optional as the SHA-1 is validated server-side by Google.

### Step 5: Wait for Propagation
Google's servers need 5-10 minutes to propagate the new SHA-1 fingerprint.

### Step 6: Test Again
After waiting, test Google Sign-In again. No need to rebuild, just:
1. Open the app (already installed)
2. Try signing in with Google
3. Should work now! ✅

---

## 🧪 Quick Test Commands

### Verify SHA-1 Fingerprints
```powershell
# Get SHA-1 for all variants
.\gradlew signingReport
```

### Reinstall Release APK (if needed)
```powershell
# Uninstall current
adb uninstall com.example.habittracker

# Reinstall release
.\gradlew installRelease
```

---

## 📝 Why This Happened

1. **Before:** You were testing with **debug builds** which use the debug keystore
2. **Now:** You installed the **signed release APK** which uses the NEW production keystore
3. **Result:** Firebase doesn't recognize the new keystore's SHA-1, so Google Sign-In fails

This is **completely normal** when you create a new production keystore. Every Android app keystore has a unique SHA-1 fingerprint that must be registered with Firebase for Google Sign-In to work.

---

## 🔐 Security Note

**Both SHA-1 fingerprints are safe to add to Firebase:**
- Debug SHA-1: For development/testing
- Release SHA-1: For production/Play Store

You should have **BOTH** registered in Firebase so Google Sign-In works in:
- ✅ Debug builds (during development)
- ✅ Release builds (production/Play Store)

---

## ✅ After Adding SHA-1

Once you add the release SHA-1 to Firebase:

1. **Google Sign-In will work** in release builds
2. **No code changes needed** - everything is already configured correctly
3. **No rebuild needed** - just wait 5-10 minutes after adding the SHA-1
4. **Works on all devices** that install the release APK

---

## 🆘 If Still Not Working After 10 Minutes

### Check Firebase Console
1. Verify the SHA-1 is shown in Firebase Console
2. Make sure there are no typos in the SHA-1
3. Should show BOTH SHA-1 fingerprints:
   - `15:E0:7A:B4:00:E7:EA:E1:BB:D5:20:DA:15:DC:13:9A:44:D2:8D:21` (debug)
   - `83:12:B1:6E:B1:21:B6:1C:20:AD:63:98:01:DF:0C:44:15:7C:F6:FC` (release)

### Force Reinstall
```powershell
# Completely remove app
adb uninstall com.example.habittracker

# Reinstall fresh
.\gradlew installRelease
```

### Check Logs (if you have adb)
```powershell
adb logcat -s AuthScreen:D AuthRepository:D
```

Look for errors like:
- `DEVELOPER_ERROR` = SHA-1 not added or not propagated yet
- `NETWORK_ERROR` = Check internet connection
- `statusCode=10` = SHA-1 fingerprint mismatch

---

## 📋 Summary Checklist

- [ ] Add release SHA-1 to Firebase Console: `83:12:B1:6E:B1:21:B6:1C:20:AD:63:98:01:DF:0C:44:15:7C:F6:FC`
- [ ] Save in Firebase Console
- [ ] Wait 5-10 minutes for propagation
- [ ] Test Google Sign-In on device
- [ ] Verify both debug and release SHA-1s are in Firebase

---

## 🎯 Quick Link

**Add SHA-1 Now:** https://console.firebase.google.com/project/habit-tracker-56079/settings/general/

Copy this SHA-1:
```
83:12:B1:6E:B1:21:B6:1C:20:AD:63:98:01:DF:0C:44:15:7C:F6:FC
```

---

## ✨ No Firestore Changes Needed

**Answer to your question:** No, you don't need to add anything new in Firestore. The issue is with **Firebase Authentication configuration (SHA-1 fingerprint)**, not Firestore data. Your current Firestore setup is perfect:

1. ✅ `users` collection exists
2. ✅ User documents are created on sign-in
3. ✅ Custom avatar and display name fields working
4. ✅ Security rules (if any) are fine

The only thing needed is **adding the release SHA-1 to Firebase Console**.
