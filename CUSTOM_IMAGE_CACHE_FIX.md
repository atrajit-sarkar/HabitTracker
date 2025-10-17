# Custom Image Cache Fix - GitHub Token Expiration

## Problem

When habits have custom images hosted on GitHub private repos, the URLs contain temporary access tokens:
```
https://raw.githubusercontent.com/.../avatar.png?token=ABC123
```

These tokens expire after a period of time. When they expire:
1. ❌ The image URL changes (new token parameter)
2. ❌ Coil treats it as a different image (URL-based caching)
3. ❌ Cache miss occurs, even though the image is the same
4. ❌ Notifications show fallback 📷 emoji instead of the cached image

## Root Cause

The app was using the **full URL (including token)** as the cache key:
- Day 1: `avatar.png?token=ABC123` → cached ✅
- Day 2: `avatar.png?token=XYZ789` → cache miss ❌ (different URL!)

## Solution

Use **habit ID as a stable cache key** instead of the URL:

### 1. Notification Avatar Loading
**File**: `HabitReminderService.kt`

```kotlin
// OLD - Uses URL as cache key (breaks when token changes)
.diskCacheKey(avatar.value)
.memoryCacheKey(avatar.value)

// NEW - Uses habit ID as stable cache key
val cacheKey = "habit_avatar_$habitId"
.diskCacheKey(cacheKey)
.memoryCacheKey(cacheKey)
```

### 2. UI Avatar Loading
**File**: `HomeScreen.kt`

```kotlin
// OLD - Uses URL hash as cache key
.memoryCacheKey("avatar_${avatar.value.hashCode()}")
.diskCacheKey("avatar_${avatar.value.hashCode()}")

// NEW - Uses habit ID when available
val cacheKey = habitId?.let { "habit_avatar_$it" } ?: "avatar_${avatar.value.hashCode()}"
.memoryCacheKey(cacheKey)
.diskCacheKey(cacheKey)
```

### 3. Updated AvatarDisplay signature
```kotlin
@Composable
private fun AvatarDisplay(
    avatar: HabitAvatar,
    size: Dp,
    modifier: Modifier = Modifier,
    habitId: Long? = null // NEW: Stable cache key
)
```

## How It Works

1. **App loads habit with custom image**:
   - URL: `avatar.png?token=ABC123`
   - Cache key: `habit_avatar_123456` (based on habit ID)
   - Image downloaded and cached ✅

2. **Token expires, URL changes in Firebase**:
   - New URL: `avatar.png?token=XYZ789`
   - Same cache key: `habit_avatar_123456`
   - Image loaded from cache instantly ✅

3. **Notification fires**:
   - Looks up cache using: `habit_avatar_123456`
   - Finds cached image even though URL changed ✅
   - Shows correct custom image, not fallback emoji ✅

## Benefits

1. ✅ **Survives token expiration** - Cache persists across URL changes
2. ✅ **No network calls in notifications** - Fast, cache-only lookups
3. ✅ **No UI freezing** - Notifications remain async-safe
4. ✅ **Consistent across app** - Same cache key used everywhere
5. ✅ **Works with private repos** - GitHub token changes don't break cache

## Testing

To verify the fix works:

1. Create a habit with a custom GitHub image
2. Wait for the GitHub token to expire (or change the URL manually in Firebase)
3. Trigger a notification
4. ✅ Should show the custom image, not 📷 fallback

## Related Changes

- **Restore fix**: Now fetches fresh habit data AFTER restore operation
- **Alarm rescheduling**: Updated to use fresh data when restoring habits
- Both ensure the latest GitHub URLs with fresh tokens are loaded into the app

## Notes

- Cache keys are consistent: `habit_avatar_{habitId}`
- Falls back to URL hash for non-habit avatars (profile pictures, etc.)
- No changes needed to preloader - it will naturally cache with new keys when images are loaded
