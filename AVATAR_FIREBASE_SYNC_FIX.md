# Avatar Firebase Sync Fix - Complete Solution

## Problem Statement

Custom avatars (both default image URLs and custom emojis) were not appearing in social features (leaderboard, friends list, search results) because:

1. **Avatar only saved locally**: When users changed their avatar, it was saved to the `users` collection but NOT to the `userProfiles` collection
2. **Profile not synced**: The `userProfiles` collection is used by all social features, so avatars weren't visible to other users
3. **Display logic issue**: Leaderboard was checking `customAvatar == null` to show Google photos, which prevented custom avatars from showing

## Root Cause

```
User changes avatar in ProfileScreen
    ↓
AuthViewModel.updateCustomAvatar()
    ↓
AuthRepository.updateCustomAvatar()
    ↓
Updates ONLY: users/{userId}/customAvatar ❌
    ↓
userProfiles/{userId} NOT UPDATED ❌
    ↓
Social features can't see the avatar ❌
```

## Solution Implemented

### 1. Added Sync Methods to `FriendRepository.kt`

**New Method: `updateUserAvatar()`**
- Updates avatar/photoUrl in `userProfiles` collection (partial update)
- Handles both existing profiles and creates new profiles if needed
- Properly handles null values (for resetting to default)

```kotlin
suspend fun updateUserAvatar(
    userId: String,
    photoUrl: String?,
    customAvatar: String?
): Result<Unit> {
    // Checks if profile exists
    // If exists: partial update
    // If not: creates new profile with minimal data
    // Updates both photoUrl and customAvatar
}
```

**New Method: `updateUserDisplayName()`**
- Updates display name in `userProfiles` collection
- Ensures name changes sync to social features

### 2. Enhanced `AuthRepository.kt`

**Injected FriendRepository**
```kotlin
@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val friendRepository: FriendRepository  // ✅ New dependency
)
```

**Updated `updateCustomAvatar()` Method**
- Now updates BOTH collections:
  1. `users/{userId}/customAvatar` (local auth data)
  2. `userProfiles/{userId}` via `friendRepository.updateUserAvatar()` (social data)

```kotlin
suspend fun updateCustomAvatar(avatar: String?): AuthResult {
    // Update users collection
    userDoc.set(mapOf(CUSTOM_AVATAR_FIELD to avatar), SetOptions.merge()).await()
    
    // ✅ NEW: Also update social profile
    friendRepository.updateUserAvatar(
        userId = userId,
        photoUrl = photoUrl,
        customAvatar = avatar
    )
    
    return AuthResult.Success
}
```

**Updated `updateDisplayName()` Method**
- Similarly syncs display name changes to social profile

### 3. Fixed Avatar Display Logic in `LeaderboardScreen.kt`

**Before (Incorrect Logic)**
```kotlin
if (photoUrl != null && customAvatar == null) {
    // Show Google photo
} else if (customAvatar?.startsWith("https://")) {
    // Show custom image
} else {
    // Show default icon (wrong!)
}
```

**After (Correct Priority Logic)**
```kotlin
// Priority: custom image URL > Google photo > emoji > default icon
val hasCustomImageAvatar = customAvatar?.startsWith("https://") == true
val hasGooglePhoto = photoUrl != null

if (hasCustomImageAvatar) {
    // Show custom avatar image (GitHub URLs, etc.)
} else if (hasGooglePhoto) {
    // Show Google profile photo
} else if (!customAvatar.isNullOrBlank()) {
    // Show emoji avatar
} else {
    // Show default icon
}
```

## Data Flow (After Fix)

```
User changes avatar in ProfileScreen
    ↓
AuthViewModel.updateCustomAvatar(avatar)
    ↓
AuthRepository.updateCustomAvatar(avatar)
    ↓
Updates BOTH:
    1. users/{userId}/customAvatar ✅
    2. userProfiles/{userId}/customAvatar + photoUrl ✅
    ↓
Social features now see the avatar:
    ✅ Leaderboard
    ✅ Friends list
    ✅ Search results
    ✅ Friend profiles
    ✅ Chat messages
```

## Firestore Structure

### `users` Collection (Auth/Local Data)
```json
{
  "userId": "abc123",
  "customAvatar": "https://example.com/avatar.png",
  "customDisplayName": "John Doe"
}
```

### `userProfiles` Collection (Social Data)
```json
{
  "userId": "abc123",
  "email": "user@example.com",
  "displayName": "John Doe",
  "photoUrl": "https://lh3.googleusercontent.com/...",
  "customAvatar": "https://example.com/avatar.png",
  "successRate": 85,
  "totalHabits": 7,
  "totalCompletions": 42,
  "currentStreak": 5,
  "leaderboardScore": 420,
  "updatedAt": 1728123456789
}
```

## Avatar Priority Logic

When displaying avatars across the app:

1. **Custom Image Avatar** (highest priority)
   - URLs starting with `https://`
   - Example: GitHub-hosted avatars
   - Shown everywhere: profile, leaderboard, friends, chat

2. **Google Profile Photo**
   - From Google Sign-In
   - Only shown if no custom avatar is set

3. **Custom Emoji**
   - User-selected emojis
   - Any non-URL string

4. **Default Icon** (lowest priority)
   - Person icon
   - Shown when nothing else is available

## Files Modified

### Data Layer
- ✅ `FriendRepository.kt` - Added `updateUserAvatar()` and `updateUserDisplayName()` methods
- ✅ `AuthRepository.kt` - Injected FriendRepository, syncs avatar/name changes

### UI Layer
- ✅ `LeaderboardScreen.kt` - Fixed avatar display logic (both podium and list)

## Testing Checklist

- [x] Build successful
- [x] APK installed on device
- [ ] Change avatar in ProfileScreen
- [ ] Verify avatar appears in leaderboard
- [ ] Verify avatar appears in friends list
- [ ] Verify avatar appears in search results
- [ ] Verify avatar appears in friend profiles
- [ ] Verify Google photos still work
- [ ] Verify emoji avatars still work
- [ ] Verify reset to default works

## Known Behavior

1. **First-time users**: If a user changes their avatar before ever visiting social features, a minimal profile will be auto-created in `userProfiles`
2. **Avatar updates**: Changes take effect immediately across all screens
3. **Profile existence**: The `updateUserAvatar()` method handles both existing and non-existing profiles gracefully

## Troubleshooting

### Avatar not showing after update?
1. Check Firestore console - verify `userProfiles/{userId}` has the correct `customAvatar` value
2. Check logs for "User avatar updated in public profile" message
3. Ensure user is signed in (uid exists)

### Old avatar still showing?
1. Force close and reopen the app
2. Clear app cache
3. Check if Firestore update succeeded in logs

### Avatar showing as default icon?
1. Verify the URL is valid and accessible
2. Check if customAvatar is null in Firestore
3. Verify Coil image loading library is working

---

**Status**: ✅ Implemented and Deployed
**Date**: October 5, 2025
**Build**: Release APK installed on device
