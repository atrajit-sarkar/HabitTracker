# YouTube Downloader - Folder Selection & Permissions Update

## ✅ What's Fixed

### 1. **Storage Permissions Added**
- Added `READ_EXTERNAL_STORAGE` for Android 9 and below
- Added `WRITE_EXTERNAL_STORAGE` for Android 9 and below  
- Added `READ_MEDIA_AUDIO` for Android 13+
- Proper permission handling for all Android versions

### 2. **Folder Selection Working**
- ✅ **Tap the folder card** to open folder selection dialog
- ✅ **Permission request** appears when needed
- ✅ **Multiple folder options** available:
  - `Downloads/HabitTracker` - Public Downloads folder (accessible via file manager)
  - `Music/HabitTracker` - Public Music folder
  - `Documents/HabitTracker` - Public Documents folder
  - `App Data (Private)` - App-specific storage (no permissions needed)

### 3. **Default Download Location**
- **Android 10+**: `Downloads/HabitTracker` (public, accessible)
- **Android 9 and below**: `App Data` (app-specific)
- Files saved to public folders are visible in your file manager!

### 4. **Filename Sanitization**
- Removes ALL special characters (only alphanumeric + underscores)
- Limits filename to 40 characters
- Prevents permission denied errors

## 📱 How to Use

### Step 1: Select Download Folder
1. Open YouTube Downloader screen
2. See the "Download Location" card
3. **Tap on the folder path** (shows dropdown arrow)
4. Grant storage permission when prompted
5. Select your preferred folder from the list
6. Your choice is saved automatically

### Step 2: Download Files
1. Paste YouTube URL
2. Tap "Validate URL"
3. Select format (MP3 for audio)
4. Tap "Download"
5. Files saved to your selected folder

### Step 3: Access Downloaded Files
- **Public folders**: Open your file manager → Downloads/HabitTracker
- **App-specific**: Only accessible within the app

## 🔒 Permission Explained

### Why We Need Storage Permission?
- **To save files** in public folders (Downloads, Music, Documents)
- **To make files accessible** in your file manager
- **App-specific storage** doesn't need permission but files are hidden

### When Permissions Are Requested?
- **First time** you try to change folder location
- **Android 13+**: Requests "Access media files (audio)" permission
- **Android 9 and below**: Requests "Storage" permission
- **Android 10-12**: No permission needed for app-specific storage

## 🎯 Folder Options Explained

| Folder | Location | Accessible? | Permissions Needed? |
|--------|----------|-------------|---------------------|
| **Downloads/HabitTracker** | `/storage/emulated/0/Download/HabitTracker` | ✅ Yes, via file manager | ✅ Yes (Android 13+) |
| **Music/HabitTracker** | `/storage/emulated/0/Music/HabitTracker` | ✅ Yes, via file manager | ✅ Yes (Android 13+) |
| **Documents/HabitTracker** | `/storage/emulated/0/Documents/HabitTracker` | ✅ Yes, via file manager | ✅ Yes (Android 13+) |
| **App Data (Private)** | `/Android/data/it.atraj.habittracker/files` | ❌ No, app only | ❌ No |

## 🛠️ Technical Changes

### Files Modified:

1. **AndroidManifest.xml**
   - Added `READ_EXTERNAL_STORAGE` (Android ≤12)
   - Added `WRITE_EXTERNAL_STORAGE` (Android ≤12)
   - Added `READ_MEDIA_AUDIO` (Android 13+)

2. **YouTubeDownloaderViewModel.kt**
   - Added `getAvailableFolders()` - Lists all folder options
   - Updated default folder to use public Downloads
   - Android version-specific folder selection

3. **YouTubeDownloaderScreen.kt**
   - Added permission request launcher
   - Made folder card clickable
   - Added folder selection dialog
   - Visual indication of selected folder

4. **MediaDownloader.kt**
   - Already using the folder from ViewModel
   - No changes needed

## ⚠️ Current Limitation

**Video downloads are still audio-only** due to NewPipe-KMP API limitation:
- ✅ **Audio downloads (MP3)**: Work perfectly
- ⚠️ **Video downloads (MP4)**: Download audio only (no picture)
- This is a library limitation, not a bug

To get full video downloads with picture, we would need to:
- Integrate ffmpeg library (complex, 5-10 hours work)
- Or use yt-dlp (requires native binary)
- Or implement HLS/DASH segment downloader

## 🎉 What You Can Do Now

1. ✅ Download YouTube audio as MP3
2. ✅ Choose where to save files (4 options)
3. ✅ Access files via file manager (if using public folders)
4. ✅ Share files with other apps
5. ✅ Folder preference saved automatically
6. ✅ No permission errors!

## 📝 Testing Checklist

- [x] Storage permissions request works
- [x] Folder selection dialog opens
- [x] Can select different folders
- [x] Selection is saved
- [x] Files download to selected folder
- [x] Files accessible in file manager (public folders)
- [x] No permission denied errors
- [x] Reset to default works

## 🚀 Next Steps (Optional)

If you want full video downloads with picture, I can help implement:
1. **ffmpeg integration** (recommended but complex)
2. **yt-dlp integration** (requires native binary)
3. **ExoPlayer streaming** (stream instead of download)

---

**Status**: ✅ **WORKING** - Audio downloads with folder selection fully functional!

**Build**: Installed successfully on RMX3750 (Android 15)

**Date**: October 22, 2025
