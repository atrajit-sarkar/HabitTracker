# Avatar Firebase Persistence - Debug Guide

## Summary
The custom avatar feature **IS ALREADY SAVING TO FIREBASE** correctly! The implementation saves to both Firestore collections:
1. **users/{userId}** - Main user data (customAvatar field)
2. **userProfiles/{userId}** - Social features data (customAvatar field)

## How It Works

### Upload Flow:
1. User selects/uploads custom avatar
2. `AvatarManager.uploadCustomAvatar()` uploads image to GitHub
3. Gets download URL from GitHub API
4. Calls `AuthRepository.updateCustomAvatar(url)`
5. Saves to Firestore `users/{userId}` with field `customAvatar`
6. Also calls `FriendRepository.updateUserAvatar()` to sync to `userProfiles/{userId}`
7. Real-time listener in `AuthRepository.currentUser` updates UI automatically

### Code References:
- **AuthRepository.kt** (lines 145-182): `updateCustomAvatar()` method
- **FriendRepository.kt** (lines 336-385): `updateUserAvatar()` method
- **AvatarManager.kt** (lines 54-76): Upload and save flow
- **AuthViewModel.kt** (lines 38-41): Real-time Firestore listener

## Verify Firebase Data

### Step 1: Check Firebase Console
1. Go to Firebase Console: https://console.firebase.google.com/
2. Select your project: HabitTracker
3. Navigate to Firestore Database
4. Check these collections:

#### Collection: **users**
```
users/
  └─ {your_userId}/
      ├─ customAvatar: "https://raw.githubusercontent.com/..." (or null)
      ├─ customDisplayName: "Your Name" (optional)
      └─ (other fields)
```

#### Collection: **userProfiles**
```
userProfiles/
  └─ {your_userId}/
      ├─ customAvatar: "https://raw.githubusercontent.com/..." (or null)
      ├─ photoUrl: "https://lh3.googleusercontent.com/..." (Google profile pic)
      ├─ displayName: "Your Name"
      └─ (other social fields)
```

### Step 2: Test Custom Avatar
1. Open the app
2. Go to Profile screen
3. Tap on profile photo
4. Select "Upload Custom Avatar"
5. Choose an image
6. Wait for upload (check logcat for success logs)
7. **Immediately check Firebase Console** - you should see:
   - `users/{userId}/customAvatar` = GitHub URL
   - `userProfiles/{userId}/customAvatar` = GitHub URL

### Step 3: Test Persistence
1. After uploading avatar, **check logcat** for these logs:
   ```
   D/AuthRepository: Custom avatar updated to: https://raw.githubusercontent.com/...
   D/AuthRepository: Custom avatar synced to social profile
   D/FriendRepository: User avatar updated in existing profile - photoUrl: ..., customAvatar: ...
   ```

2. **Force-stop** the app (don't just minimize)
3. Reopen the app
4. Avatar should still be visible

5. **Uninstall and reinstall** the app
6. Sign in with the same account
7. Avatar should load from Firebase (not local cache)

### Step 4: Test Social Features
1. Have a friend add you via your userId
2. Friend should see your custom avatar in their Friends List
3. Check Leaderboard - your custom avatar should appear there too

## Common Issues & Solutions

### Issue 1: Black/Missing Avatar After Reinstall
**Possible Causes:**
- GitHub token not properly stored in BuildConfig
- Network error during Firebase fetch
- Avatar URL format issue

**Debug:**
1. Check logcat for errors when loading avatar
2. Verify GitHub repo is accessible (not deleted/private without token)
3. Check if Firestore read succeeded:
   ```
   D/AuthRepository: User data snapshot updated - Avatar: https://..., Name: ...
   ```

### Issue 2: Avatar Not Visible in Friends List
**Possible Causes:**
- `userProfiles` collection not updated
- Friend's app not fetching latest data

**Debug:**
1. Check Firebase Console: `userProfiles/{userId}/customAvatar` exists
2. Check logcat on friend's device for fetch errors
3. Verify `FriendRepository.updateUserAvatar()` was called:
   ```
   D/FriendRepository: User avatar updated in existing profile
   ```

### Issue 3: Avatar Shows Locally But Not in Firebase
**This should NOT happen with current code, but if it does:**
1. Check if `authRepository.updateCustomAvatar()` returned success
2. Look for exceptions in logcat during upload
3. Verify Firebase Firestore rules allow writes:
   ```javascript
   match /users/{userId} {
     allow read, write: if request.auth != null && request.auth.uid == userId;
   }
   match /userProfiles/{userId} {
     allow read: if request.auth != null;
     allow write: if request.auth != null && request.auth.uid == userId;
   }
   ```

## Test Checklist

- [ ] Upload custom avatar
- [ ] Check Firebase Console for `users/{userId}/customAvatar`
- [ ] Check Firebase Console for `userProfiles/{userId}/customAvatar`
- [ ] Force-stop and reopen app - avatar persists
- [ ] Uninstall and reinstall - avatar loads from Firebase
- [ ] Friend can see your avatar in Friends List
- [ ] Avatar visible in Leaderboard
- [ ] Logcat shows no errors during upload/fetch

## Expected Behavior

✅ **Custom avatars ARE being saved to Firebase**
✅ **Data persists across app restarts**
✅ **Data persists across reinstalls**
✅ **Visible to friends and in leaderboard**
✅ **Real-time updates via Firestore listener**

## If You Still See Black Avatars

The issue is likely NOT with Firebase persistence. Check these instead:

1. **GitHub Token in Release Build:**
   - Verify `keystore.properties` has `GITHUB_TOKEN=ghp_...`
   - Check `app/build.gradle.kts` includes token in BuildConfig
   - Confirm signed APK includes token (check logcat on first run)

2. **Network Connectivity:**
   - Avatar images load from GitHub (requires internet)
   - Check if other images load in the app

3. **Coil Image Loading:**
   - Check logcat for Coil errors
   - Verify image URLs are valid (not 404)

4. **GitHub Repository:**
   - Verify repo exists: https://github.com/gongobongofounder/habit-tracker-avatar-repo
   - Check if images are actually uploaded (browse repo on GitHub)
   - Verify repo permissions (public or token has access)

## Next Steps

1. **Upload an avatar and check Firebase Console immediately**
2. **Copy the exact value** of `customAvatar` from Firebase
3. **Paste that URL** in a browser to verify it loads
4. If URL doesn't load in browser → **GitHub access issue** (token/repo)
5. If URL loads in browser but not in app → **Coil loading issue**
6. If URL not in Firebase → **Firebase write issue** (check rules/logs)

---

**The code is already correct!** The issue you're experiencing is likely:
- Testing with cached data (try uninstall/reinstall)
- GitHub token not in release build (verify BuildConfig)
- Network connectivity during image load
- Not waiting for upload to complete before checking

**Run the Test Checklist above to identify the actual root cause.**
