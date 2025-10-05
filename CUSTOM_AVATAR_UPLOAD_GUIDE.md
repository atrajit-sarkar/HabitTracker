# 🎨 Custom Avatar Upload Feature - Complete Guide

## Overview

This feature allows users to upload custom profile avatars that are stored in a GitHub repository and displayed across the app. It maintains the existing default avatar functionality while adding user-specific custom avatar uploads.

## Architecture

### Repository Structure
```
habit-tracker-avatar-repo/
├── avatars/
│   ├── default/          # (Optional) Pre-existing default avatars
│   └── users/           # User-uploaded avatars
│       ├── {userId1}/
│       │   ├── avatar_1697123456789.png
│       │   └── avatar_1697234567890.png
│       └── {userId2}/
│           └── avatar_1697345678901.png
```

### Components

1. **GitHubAvatarUploader** - Handles uploading/deleting images to GitHub via API
2. **AvatarManager** - Manages default and custom avatars, coordinates uploads
3. **EnhancedAvatarPickerDialog** - UI for selecting/uploading avatars
4. **AvatarPickerViewModel** - ViewModel for avatar picker logic
5. **AvatarConfig** - Configuration and token management
6. **AvatarModule** - Dagger Hilt dependency injection

## Features

✅ **Default Avatars** - 8 pre-existing avatars from main repository
✅ **Custom Upload** - Users can upload their own images
✅ **User-Specific Directories** - Each user has their own folder
✅ **Delete Custom Avatars** - Users can delete their uploaded avatars
✅ **Grid View** - Visual selection with badges for custom avatars
✅ **Progress Indicators** - Upload progress and loading states
✅ **Error Handling** - Comprehensive error messages
✅ **Image Compression** - Automatic resizing to 512x512 max
✅ **Secure Storage** - Images stored in GitHub with access control

## Manual Setup Required

### 1. Create GitHub Repository

1. Go to https://github.com/gongobongofounder
2. Navigate to the repository: `habit-tracker-avatar-repo`
3. Create the following folder structure:
   ```
   avatars/
   └── users/
       └── .gitkeep
   ```

### 2. Generate GitHub Personal Access Token

1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Click "Generate new token (classic)"
3. Token name: `Habit Tracker Avatar Upload`
4. Select scopes:
   - ✅ `repo` (Full control of private repositories)
5. Generate token and **COPY IT IMMEDIATELY** (you won't see it again)

### 3. Initialize the Feature in Your App

Add this code to your `MainActivity.kt` or application initialization:

```kotlin
// In MainActivity.onCreate() or Application.onCreate()
@Inject
lateinit var avatarConfig: it.atraj.habittracker.avatar.AvatarConfig

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize with your GitHub token
    // IMPORTANT: In production, store this securely (not hardcoded!)
    val githubToken = "ghp_YOUR_TOKEN_HERE"
    avatarConfig.initialize(githubToken)
}
```

### 4. Secure Token Storage (Production)

For production, use Android's encrypted SharedPreferences or Keystore:

```kotlin
// Example with EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

val sharedPreferences = EncryptedSharedPreferences.create(
    "secure_prefs",
    masterKeyAlias,
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

// Store token securely
sharedPreferences.edit()
    .putString("github_token", "ghp_YOUR_TOKEN_HERE")
    .apply()

// Initialize with secure token
val token = sharedPreferences.getString("github_token", null)
if (token != null) {
    avatarConfig.initialize(token)
}
```

### 5. Add Permissions (Already in Manifest)

Ensure these permissions are in your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

### 6. Update Build.gradle (If Needed)

If you need encrypted preferences:

```kotlin
// In app/build.gradle.kts
dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

## How It Works

### User Flow

1. **Open Profile Screen** → Tap on profile avatar
2. **Avatar Picker Opens** showing:
   - Default avatars (8 pre-existing)
   - User's custom uploaded avatars (with star badge)
3. **Upload New Avatar**:
   - Tap upload icon (top right)
   - Select image from gallery
   - Image is compressed and uploaded to GitHub
   - New avatar appears in grid instantly
4. **Select Avatar** → Tap any avatar to use it
5. **Delete Custom Avatar** → Tap delete icon on custom avatars

### Technical Flow

```
User selects image
    ↓
AvatarPickerViewModel.uploadCustomAvatar()
    ↓
AvatarManager.uploadCustomAvatar()
    ↓
GitHubAvatarUploader.uploadAvatar()
    ↓
1. Load & compress image (max 512x512)
2. Convert to Base64
3. Upload to GitHub via API:
   PUT /repos/{owner}/{repo}/contents/avatars/users/{userId}/avatar_{timestamp}.png
4. Get download URL
    ↓
AuthRepository.updateCustomAvatar(url)
    ↓
Save to Firestore: users/{userId}/customAvatar = url
    ↓
UI updates automatically (StateFlow)
```

### Image Processing

- **Max Dimensions**: 512x512 pixels
- **Format**: PNG
- **Compression**: 85% quality
- **Max Size**: 1MB (enforced by compression)
- **Filename**: `avatar_{timestamp}.png`
- **Storage**: GitHub raw URLs for CDN-like performance

## API Endpoints Used

### Upload Avatar
```
PUT https://api.github.com/repos/{owner}/{repo}/contents/{path}
Authorization: token {github_token}
Content-Type: application/json

{
  "message": "Upload custom avatar",
  "content": "{base64_encoded_image}"
}
```

### Delete Avatar
```
DELETE https://api.github.com/repos/{owner}/{repo}/contents/{path}
Authorization: token {github_token}
Content-Type: application/json

{
  "message": "Delete custom avatar",
  "sha": "{file_sha}"
}
```

### List User Avatars
```
GET https://api.github.com/repos/{owner}/{repo}/contents/avatars/users/{userId}
Authorization: token {github_token}
```

## Configuration Options

### Change Repository
Update these constants in `GitHubAvatarUploader.kt`:

```kotlin
private const val GITHUB_OWNER = "gongobongofounder"
private const val GITHUB_REPO = "habit-tracker-avatar-repo"
```

### Change Image Settings
Update these constants in `GitHubAvatarUploader.kt`:

```kotlin
private const val MAX_IMAGE_SIZE = 1024 * 1024  // 1MB
private const val COMPRESSION_QUALITY = 85       // 0-100
private const val MAX_DIMENSION = 512            // pixels
```

### Change Default Avatars
Update `getDefaultAvatars()` in `AvatarManager.kt`:

```kotlin
fun getDefaultAvatars(): List<String> {
    return listOf(
        "https://raw.githubusercontent.com/...",
        // Add more default avatars
    )
}
```

## Testing

### Test Upload Feature

1. Run the app
2. Sign in with a test account
3. Go to Profile screen
4. Tap on avatar
5. Tap upload icon
6. Select an image
7. Verify:
   - Upload progress shows
   - New avatar appears in grid with star badge
   - Avatar can be selected
   - Avatar appears across all screens

### Test Delete Feature

1. Long-press on a custom avatar
2. Tap delete icon
3. Confirm deletion
4. Verify:
   - Avatar removed from grid
   - File deleted from GitHub (check repo)

### Test Default Avatars

1. Select a default avatar
2. Verify it displays correctly
3. No delete option should appear for default avatars

## Security Considerations

⚠️ **IMPORTANT**: Never commit GitHub tokens to version control!

### Best Practices:

1. **Use Environment Variables** (for CI/CD):
```kotlin
val token = BuildConfig.GITHUB_TOKEN
```

2. **Use Encrypted SharedPreferences** (for local storage)
3. **Use Android Keystore** (for maximum security)
4. **Rotate Tokens Regularly**
5. **Use Repository-Specific Tokens** (limit scope)

### Token Permissions

Minimum required: `repo` scope for private repositories
- Can read/write repository contents
- Cannot access other repositories
- Can be revoked anytime

## Troubleshooting

### Upload Fails

**Error**: "Failed to upload to GitHub"
**Solutions**:
- Check GitHub token is valid
- Verify token has `repo` permissions
- Check internet connection
- Verify repository exists and is accessible

### Image Too Large

**Error**: "Failed to load image"
**Solution**: Image compression should handle this automatically, but ensure source image is reasonable size (<10MB)

### Token Not Initialized

**Error**: "GitHub token not initialized"
**Solution**: Call `avatarConfig.initialize(token)` in `MainActivity.onCreate()`

### Avatar Not Appearing

**Issue**: Upload succeeds but avatar doesn't show
**Solutions**:
- Check Firestore is updated (`users/{userId}/customAvatar`)
- Verify URL is accessible (try opening in browser)
- Check image format is PNG
- Clear app cache and restart

## Files Created

1. `app/src/main/java/.../avatar/GitHubAvatarUploader.kt` - GitHub API integration
2. `app/src/main/java/.../avatar/AvatarManager.kt` - Avatar management
3. `app/src/main/java/.../avatar/AvatarConfig.kt` - Configuration
4. `app/src/main/java/.../avatar/ui/EnhancedAvatarPickerDialog.kt` - UI component
5. `app/src/main/java/.../avatar/ui/AvatarPickerViewModel.kt` - ViewModel
6. `app/src/main/java/.../avatar/di/AvatarModule.kt` - Dependency injection

## Files Modified

1. `app/src/main/java/.../auth/ui/ProfileScreen.kt` - Uses new dialog
2. `app/src/main/java/.../MainActivity.kt` - Auto-initializes feature

## Future Enhancements

- [ ] Add image cropping UI
- [ ] Support multiple image formats (JPEG, WebP)
- [ ] Add image filters/effects
- [ ] Avatar history/rollback
- [ ] Batch delete functionality
- [ ] Admin dashboard for avatar moderation
- [ ] AI-generated avatar suggestions
- [ ] Avatar templates/stickers

## Support

For issues or questions:
1. Check GitHub repository issues
2. Review logs: `adb logcat | grep -E "GitHubAvatarUploader|AvatarManager|AvatarConfig"`
3. Verify GitHub API status: https://www.githubstatus.com/

## Summary of Manual Steps

1. ✅ Create GitHub repository `habit-tracker-avatar-repo`
2. ✅ Create folder structure: `avatars/users/`
3. ✅ Generate GitHub Personal Access Token with `repo` scope
4. ⚠️ **YOU NEED TO DO**: Store token securely in your app
5. ⚠️ **YOU NEED TO DO**: Initialize `avatarConfig` with token in `MainActivity`
6. ✅ Test upload/delete functionality
7. ✅ Verify avatars appear across all screens

## Token Initialization Template

Add this to your `MainActivity.kt`:

```kotlin
@Inject
lateinit var avatarConfig: it.atraj.habittracker.avatar.AvatarConfig

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ... other initialization ...
    
    // TODO: Replace with your actual GitHub token
    // For production, load from secure storage!
    val githubToken = "ghp_YOUR_GITHUB_TOKEN_HERE"
    avatarConfig.initialize(githubToken)
}
```

---

**Ready to use!** Once you complete the manual steps above, users can upload and manage custom avatars! 🎉
