# In-App Update System with GitHub Releases

## Overview
Professional in-app update system that automatically checks for new releases from GitHub and displays a beautiful changelog dialog with download functionality.

## ✨ Features

### 🎯 Core Functionality
- ✅ **Automatic Update Checking** - Checks GitHub releases on app launch
- ✅ **Smart Interval System** - Checks every 24 hours (configurable)
- ✅ **Version Comparison** - Semantic versioning support (e.g., 1.2.3)
- ✅ **Skip Version** - Users can skip specific versions
- ✅ **Direct Download** - Downloads APK directly in-app
- ✅ **Progress Tracking** - Real-time download progress (0-100%)
- ✅ **Auto Install** - Triggers Android package installer
- ✅ **Fallback to Browser** - Opens GitHub releases if download fails

### 🎨 UI/UX Features
- ✅ **Material Design 3** - Beautiful, modern UI
- ✅ **Gradient Headers** - Eye-catching visual design
- ✅ **Animated Icons** - Rotating download icon during download
- ✅ **Changelog Display** - Full release notes with scrolling
- ✅ **Version Comparison** - Visual "Current → New" display
- ✅ **File Size Display** - Shows download size (MB/KB)
- ✅ **Release Date** - Shows when update was published
- ✅ **Progress Bar** - Linear progress indicator during download
- ✅ **Multiple Actions** - Update Now, Skip, Later buttons
- ✅ **Mandatory Updates** - Support for critical updates (optional)

### 🔒 Smart Features
- ✅ **Non-Intrusive** - Dismissible unless marked as mandatory
- ✅ **Network Aware** - Handles connection failures gracefully
- ✅ **Cache Management** - Cleans up old APK files
- ✅ **FileProvider** - Secure APK installation on Android 7+
- ✅ **Background Prevention** - Blocks back button during download
- ✅ **Error Handling** - Comprehensive error recovery

## 📁 File Structure

```
app/src/main/java/com/example/habittracker/update/
├── UpdateManager.kt           # Core update logic (320 lines)
└── UpdateDialog.kt            # UI components (450 lines)

app/src/main/res/xml/
└── file_paths.xml             # FileProvider configuration

app/src/main/AndroidManifest.xml  # Permissions + FileProvider
```

## 🔧 Implementation Details

### 1. UpdateManager.kt
**Purpose**: Handles all update logic and GitHub API integration

**Key Methods**:
```kotlin
// Check if it's time to check for updates (24h interval)
fun shouldCheckForUpdate(): Boolean

// Get current app version
fun getCurrentVersion(): String
fun getCurrentVersionCode(): Long

// Check GitHub for latest release
suspend fun checkForUpdate(): UpdateInfo?

// Download APK with progress callback
suspend fun downloadAndInstall(
    downloadUrl: String,
    onProgress: (Int) -> Unit
): Result<File>

// Install downloaded APK
fun installApk(apkFile: File)

// Open GitHub releases page in browser
fun openReleasesPage()

// Skip version management
fun skipVersion(version: String)
fun isVersionSkipped(version: String): Boolean
```

**GitHub API Integration**:
- Endpoint: `https://api.github.com/repos/atrajit-sarkar/HabitTracker/releases/latest`
- Parses: version, release notes, download URL, file size, date
- Finds: First `.apk` file in release assets
- Timeout: 30 seconds connect/read

**Version Comparison**:
```kotlin
compareVersions("1.2.3", "1.2.4") // Returns -1 (v1 < v2)
compareVersions("1.2.4", "1.2.4") // Returns 0 (equal)
compareVersions("1.2.5", "1.2.4") // Returns 1 (v1 > v2)
```

### 2. UpdateDialog.kt
**Purpose**: Professional UI for update prompts

**Components**:
- **UpdateDialog** - Main dialog container
- **UpdateIcon** - Animated icon (rotates during download)
- **VersionInfoCard** - Current → New version display
- **InfoChip** - File size and date chips
- **DownloadProgressCard** - Progress bar with percentage
- **ChangelogSection** - Scrollable release notes
- **ActionButtons** - Update/Skip/Later buttons

**Visual States**:
1. **Initial State**: Shows update info with 3 action buttons
2. **Downloading State**: Shows progress bar, hides buttons
3. **Mandatory State**: Only shows "Update Now" (no dismiss)

### 3. MainActivity Integration

**Initialization**:
```kotlin
private lateinit var updateManager: UpdateManager

override fun onCreate(savedInstanceState: Bundle?) {
    updateManager = UpdateManager(this)
    // ... rest of code
}
```

**Update Check Flow**:
```kotlin
LaunchedEffect(Unit) {
    if (updateManager.shouldCheckForUpdate()) {
        val update = updateManager.checkForUpdate()
        if (update != null && update.isUpdateAvailable) {
            if (!updateManager.isVersionSkipped(update.latestVersion)) {
                updateInfo = update
                showUpdateDialog = true
            }
        }
    }
}
```

**Download & Install Flow**:
```kotlin
onUpdate = {
    isDownloading = true
    MainScope().launch {
        val result = updateManager.downloadAndInstall(
            downloadUrl = updateInfo!!.downloadUrl,
            onProgress = { progress ->
                downloadProgress = progress // 0-100
            }
        )
        
        result.onSuccess { apkFile ->
            isDownloading = false
            showUpdateDialog = false
            updateManager.installApk(apkFile)
        }.onFailure { error ->
            isDownloading = false
            updateManager.openReleasesPage() // Fallback
            showUpdateDialog = false
        }
    }
}
```

## 📋 Configuration

### Permissions (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

### FileProvider (AndroidManifest.xml)
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### Dependencies (build.gradle.kts)
```kotlin
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

### Repository Configuration (UpdateManager.kt)
```kotlin
companion object {
    private const val GITHUB_REPO_OWNER = "atrajit-sarkar"
    private const val GITHUB_REPO_NAME = "HabitTracker"
    private const val CHECK_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
}
```

## 🚀 Usage Guide

### For Developers

#### 1. **Create a GitHub Release**
```bash
# Tag your version
git tag -a v1.2.0 -m "Version 1.2.0"
git push origin v1.2.0

# Create release on GitHub
# 1. Go to: https://github.com/atrajit-sarkar/HabitTracker/releases/new
# 2. Select tag: v1.2.0
# 3. Release title: "Version 1.2.0 - Feature Update"
# 4. Description (changelog):
```

**Example Changelog Format**:
```markdown
## 🎉 What's New in v1.2.0

### ✨ New Features
- 🎯 Added habit statistics and analytics
- 📊 Beautiful bar charts for progress tracking
- 🔔 Enhanced notification system with reliability improvements

### 🐛 Bug Fixes
- Fixed notification not appearing when device is idle
- Resolved dark mode theme inconsistencies
- Fixed profile picture update issues

### 🔧 Improvements
- Optimized database queries for better performance
- Enhanced UI animations and transitions
- Improved offline mode functionality

### 📱 Technical Updates
- Updated to Android SDK 36
- Migrated to Kotlin 2.0
- Added WorkManager for background tasks
```

#### 2. **Upload APK to Release**
```bash
# Build release APK
./gradlew assembleRelease

# APK location:
# app/build/outputs/apk/release/app-release.apk

# Upload to GitHub release as an asset
```

#### 3. **Version Configuration**
Update `build.gradle.kts`:
```kotlin
defaultConfig {
    versionCode = 2        // Increment for each release
    versionName = "1.2.0"  // Semantic versioning
}
```

### For Users

#### Automatic Update Flow
1. **App Launch** → Checks for updates (every 24 hours)
2. **Update Found** → Beautiful dialog appears
3. **User Choice**:
   - **Update Now** → Downloads APK → Installs automatically
   - **Skip** → Won't show this version again
   - **Later** → Shows again in 24 hours

#### Manual Update Check
Users can also check manually by visiting:
- Profile → About → Check for Updates (if you add this feature)

## 🎨 Visual States

### 1. Update Available Dialog
```
┌─────────────────────────────────────┐
│   [Gradient Header with Icon]       │
│   Update Available                  │
│   Version 1.2.0 - Feature Update    │
├─────────────────────────────────────┤
│   Current: 1.1.0  →  New: 1.2.0    │
│   📦 15.2 MB   📅 Jan 15, 2025     │
├─────────────────────────────────────┤
│   What's New                        │
│   ┌───────────────────────────────┐ │
│   │ • Feature 1                   │ │
│   │ • Feature 2                   │ │
│   │ • Bug fixes                   │ │
│   └───────────────────────────────┘ │
├─────────────────────────────────────┤
│   [  Update Now (Primary Button)  ] │
│   [ Skip ]  [ Later ]               │
└─────────────────────────────────────┘
```

### 2. Downloading State
```
┌─────────────────────────────────────┐
│   [Rotating Download Icon]          │
│   Downloading...                    │
│   Version 1.2.0                     │
├─────────────────────────────────────┤
│   Downloading...            67%     │
│   ████████████░░░░░░░░░░░░         │
│   Please don't close the app        │
└─────────────────────────────────────┘
```

### 3. Mandatory Update
```
┌─────────────────────────────────────┐
│   [Gradient Header with Icon]       │
│   Required Update                   │
│   Critical Security Update          │
├─────────────────────────────────────┤
│   Current: 1.1.0  →  New: 1.2.0    │
├─────────────────────────────────────┤
│   What's New                        │
│   This update includes critical     │
│   security fixes. Please update now.│
├─────────────────────────────────────┤
│   [ Update Now (Required) ]         │
│   (No dismiss option)               │
└─────────────────────────────────────┘
```

## 🔒 Security Considerations

### 1. **FileProvider Security**
- Uses `FileProvider` for secure APK access
- Grants temporary URI permissions
- APK stored in cache directory (auto-cleaned by system)

### 2. **Network Security**
- Uses HTTPS for GitHub API
- 30-second timeouts to prevent hanging
- Graceful failure handling

### 3. **User Control**
- Users can skip updates (except mandatory)
- Can dismiss dialog (except mandatory)
- No forced downloads without user consent

## 🐛 Error Handling

### Common Scenarios
1. **No Internet**: Silent fail, tries again in 24h
2. **GitHub API Down**: Silent fail, falls back to browser
3. **Download Failed**: Opens browser to GitHub releases
4. **No APK in Release**: Logs error, doesn't show dialog
5. **Invalid JSON**: Catches exception, silent fail

### Logging
All operations are logged with tag `UpdateManager`:
```kotlin
Log.d("UpdateManager", "Checking for updates from GitHub...")
Log.d("UpdateManager", "Update available: $latestVersion")
Log.e("UpdateManager", "Failed to check for updates: ${response.code}")
```

## 🎯 Advanced Features

### 1. Mandatory Updates
```kotlin
UpdateDialog(
    updateInfo = updateInfo,
    isMandatory = true,  // ← Set to true for critical updates
    // ... other params
)
```

**Behavior**:
- No "Skip" or "Later" buttons
- Can't dismiss dialog
- Must update to continue

### 2. Custom Check Interval
```kotlin
// In UpdateManager.kt
private const val CHECK_INTERVAL = 12 * 60 * 60 * 1000L // 12 hours
```

### 3. Pre-release Support
```kotlin
// UpdateInfo includes isPrerelease flag
if (updateInfo.isPrerelease) {
    // Handle beta/pre-release versions
    // Maybe show "Beta Update" badge
}
```

### 4. Multi-APK Support
If release has multiple APKs (e.g., different architectures):
```kotlin
// Modify UpdateManager to filter by name pattern
val assetName = asset.optString("name", "")
if (assetName.matches(Regex(".*-universal-release\\.apk"))) {
    // Only download universal APK
}
```

## 📊 Testing Checklist

### Before Release
- [ ] Update `versionCode` and `versionName` in `build.gradle.kts`
- [ ] Build release APK (`./gradlew assembleRelease`)
- [ ] Create GitHub release with proper changelog
- [ ] Upload APK to release assets
- [ ] Test update dialog appears
- [ ] Test download progress works
- [ ] Test APK installs correctly
- [ ] Test "Skip" functionality
- [ ] Test "Later" functionality
- [ ] Test with no internet connection
- [ ] Test with slow network
- [ ] Test mandatory update mode

### Testing Update Flow
1. Install old version (e.g., v1.0.0)
2. Create new release on GitHub (e.g., v1.1.0)
3. Open app → Should show update dialog
4. Click "Update Now" → Should download
5. Progress bar should show 0% → 100%
6. Should open Android installer
7. Install → Should replace old version

## 🚨 Common Issues & Solutions

### Issue 1: Dialog Doesn't Appear
**Solution**: Check logs for:
```kotlin
Log.d("UpdateManager", "Checking for updates...")
```
Verify: Internet connection, 24h interval passed, version not skipped

### Issue 2: APK Won't Install
**Solution**: Ensure:
- `REQUEST_INSTALL_PACKAGES` permission in manifest
- FileProvider configured correctly
- APK is signed (use release build, not debug)

### Issue 3: Download Fails
**Solution**: Check:
- APK file exists in GitHub release assets
- File is named with `.apk` extension
- Network connection is stable
- Falls back to browser automatically

### Issue 4: Version Comparison Wrong
**Solution**: Use semantic versioning:
- ✅ Good: `1.0.0`, `1.2.3`, `2.0.0`
- ❌ Bad: `v1.0`, `1.0.0-beta`, `latest`

## 📈 Future Enhancements

### Planned Features
1. **Delta Updates** - Only download changed files
2. **In-App Changelog View** - Show all past updates
3. **Update Notification** - Push notification for updates
4. **Auto-Update Option** - User preference for auto-download
5. **Update Schedule** - Choose when to check (daily/weekly)
6. **Beta Channel** - Opt-in to pre-release versions
7. **Update Size Warning** - Warn about large downloads
8. **WiFi-Only Option** - Only download on WiFi

### Analytics Integration
Track update metrics:
- Update check frequency
- Update acceptance rate
- Skip/Later ratio
- Average time to update
- Download success rate

## 🎓 Best Practices

### For Developers
1. **Semantic Versioning**: Use MAJOR.MINOR.PATCH (e.g., 1.2.3)
2. **Detailed Changelogs**: Write user-friendly release notes
3. **Test Before Release**: Always test update flow
4. **Sign APKs**: Use release signing for production
5. **Tag Properly**: Use `v` prefix for tags (e.g., v1.2.0)

### For Release Notes
1. **Group Changes**: Features, Fixes, Improvements
2. **Use Emojis**: Makes changelog more engaging
3. **Be Specific**: "Fixed login crash" not "Bug fixes"
4. **Prioritize**: Most important changes first
5. **Credit Users**: Thank bug reporters

### For Version Management
1. **Increment Wisely**:
   - MAJOR: Breaking changes
   - MINOR: New features
   - PATCH: Bug fixes only
2. **Never Skip Versions**: Don't jump from 1.0 to 2.0
3. **Document Breaking Changes**: Warn users about major updates

## 🔗 Related Files

- `app/build.gradle.kts` - Version configuration
- `gradle/libs.versions.toml` - OkHttp dependency
- `app/src/main/AndroidManifest.xml` - Permissions
- `MainActivity.kt` - Integration point

## 📞 Support

If users encounter update issues:
1. Check internet connection
2. Try manual download from GitHub
3. Clear app cache
4. Reinstall app from GitHub releases

---

**Created**: October 2025
**Status**: ✅ Production Ready
**Version**: 1.0.0
