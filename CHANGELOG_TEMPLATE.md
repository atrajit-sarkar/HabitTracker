# Changelog Template for GitHub Releases

Use this template when creating new releases on GitHub.

---

## Version [X.Y.Z] - [Release Name]
**Release Date**: [Month Day, Year]

### 🎉 Highlights
[1-2 sentence summary of the most important changes]

---

### ✨ New Features
- 🎯 **[Feature Name]**: [Brief description of what it does and why it's useful]
- 📊 **[Feature Name]**: [Brief description]
- 🔔 **[Feature Name]**: [Brief description]

### 🐛 Bug Fixes
- ❌ Fixed: [Description of the bug and how it was fixed]
- ❌ Fixed: [Description]
- ❌ Resolved: [Description]

### 🔧 Improvements
- ⚡ Performance: [What was optimized and expected impact]
- 🎨 UI/UX: [Visual or interaction improvements]
- 🔒 Security: [Security enhancements]

### 🏗️ Technical Updates
- 📱 Updated Android SDK to [version]
- 🛠️ Migrated to [library/framework version]
- ⚙️ Added [technical feature for developers]

### 📝 Known Issues
- ⚠️ [Issue description] - Will be fixed in next release
- ⚠️ [Issue description] - Workaround: [solution]

---

### 📦 Download
- **File Size**: ~XX.X MB
- **Minimum Android**: Android 10 (API 29)
- **Target Android**: Android 14 (API 36)

### 🔗 Links
- [Full Documentation](link)
- [Report Issues](https://github.com/atrajit-sarkar/HabitTracker/issues)
- [View Source Code](https://github.com/atrajit-sarkar/HabitTracker)

---

## 📸 Screenshots
[Optional: Add screenshots of new features]

---

## 🙏 Acknowledgments
- Thanks to [@username] for reporting [bug/feature request]
- Special thanks to all beta testers

---

**Installation Instructions**:
1. Download `app-release.apk` from assets below
2. Enable "Install from Unknown Sources" if needed
3. Open APK and install
4. Enjoy the new features!

**Existing users**: The app will notify you automatically about this update!

---

# EXAMPLE RELEASE:

---

## Version 1.1.0 - In-App Update System
**Release Date**: October 2, 2025

### 🎉 Highlights
This update introduces a professional in-app update system with automatic version checking and beautiful changelog dialogs. Users will now be notified automatically when new updates are available!

---

### ✨ New Features
- 📲 **In-App Update System**: Automatic update checking with beautiful Material Design 3 dialog
- 📋 **Changelog Display**: Full release notes shown in-app with scrollable view
- ⬇️ **Direct Downloads**: Download and install updates without leaving the app
- 📊 **Progress Tracking**: Real-time download progress with percentage indicator
- ⏭️ **Skip/Later Options**: User choice to skip versions or postpone updates
- 🔄 **Auto-Check**: Checks for updates every 24 hours automatically
- 🌐 **Browser Fallback**: Automatically opens GitHub releases if download fails

### 🐛 Bug Fixes
- ❌ Fixed: Notifications not sending when device stays idle for long periods
- ❌ Fixed: Dark mode theme inconsistencies in profile screen
- ❌ Resolved: Profile picture not updating in real-time across screens
- ❌ Fixed: Statistics chart not displaying correctly on some devices

### 🔧 Improvements
- ⚡ Performance: Optimized database queries for 30% faster habit loading
- 🎨 UI/UX: Enhanced notification setup guide with step-by-step instructions
- 🎨 UI/UX: Improved status tracking with progress indicators (X/4 steps)
- 🔋 Battery: Better handling of battery optimization settings
- 📱 Compatibility: Added specific instructions for Realme/Oppo devices
- 🔒 Security: Updated to latest Firebase and security patches

### 🏗️ Technical Updates
- 📱 Updated Android compileSdk to 36
- 🛠️ Updated Kotlin to 2.0.21
- ⚙️ Added OkHttp 4.12.0 for HTTP client
- ⚙️ Implemented FileProvider for secure APK installation
- 🔧 Added comprehensive logging for update process
- 🔧 Improved error handling throughout the app

### 📝 Known Issues
- ⚠️ First-time update check may take a few seconds on slow networks
- ⚠️ Some Xiaomi devices may require manual "Autostart" permission
- ⚠️ Update notification shows after 24 hours interval

---

### 📦 Download
- **File Size**: ~15.2 MB
- **Minimum Android**: Android 10 (API 29)
- **Target Android**: Android 14 (API 36)

### 🔗 Links
- [Setup Documentation](https://github.com/atrajit-sarkar/HabitTracker/blob/main/QUICK_UPDATE_SETUP.md)
- [Report Issues](https://github.com/atrajit-sarkar/HabitTracker/issues)
- [View Source Code](https://github.com/atrajit-sarkar/HabitTracker)

---

## 📸 Screenshots
[Add screenshots of the new update dialog and changelog view]

---

## 🙏 Acknowledgments
- Thanks to all users who reported the notification reliability issues
- Special thanks to beta testers for feedback on the update system

---

**Installation Instructions**:
1. Download `app-release.apk` from assets below
2. Enable "Install from Unknown Sources" in Settings → Security (if prompted)
3. Open the APK file and tap "Install"
4. Grant necessary permissions when prompted
5. Enjoy automatic update notifications going forward!

**Existing users**: This is the last manual install! Future updates will notify you automatically in the app! 🎉

---

## 🔄 Update System Details

### How It Works
1. App checks GitHub releases every 24 hours
2. If new version found, shows beautiful dialog with changelog
3. User can choose: Update Now, Skip, or Later
4. "Update Now" downloads and installs automatically
5. "Skip" never shows this version again
6. "Later" reminds in 24 hours

### User Privacy
- ✅ No tracking or analytics
- ✅ Only checks version number
- ✅ Downloads over HTTPS
- ✅ User choice always respected

### For Developers
This release includes comprehensive documentation:
- `IN_APP_UPDATE_SYSTEM.md` - Full technical documentation
- `QUICK_UPDATE_SETUP.md` - Quick setup guide for releases

---

**Build Info**:
- Version Code: 2
- Version Name: 1.1.0
- Build Date: October 2, 2025
- Build Type: Release
- Signed: Yes

---

**What's Next?**:
- v1.2.0: Habit streaks and achievements system
- v1.3.0: Widget support for home screen
- v2.0.0: Complete redesign with new features

Stay tuned! 🚀
