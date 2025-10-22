# 📝 Changelog - Version 7.0.0

## Release Information

- **Version**: 7.0.0
- **Version Code**: 20
- **Release Date**: October 22, 2025
- **Build System**: Dual-Flavor (GitHub + Play Store)

---

## 🎉 Major Changes

### 1. Dual-Flavor Build System
- **GitHub Flavor** (v7.0.0-github): Includes in-app update system
- **Play Store Flavor** (v7.0.0): Clean build without GitHub updates
- Easy switching between build types with single command
- Automatic flavor detection at runtime

### 2. Version Bump
- Updated from v6.0.5 (versionCode 19) to v7.0.0 (versionCode 20)
- Semantic versioning for major architectural change

---

## ✨ New Features

### Build System
- ✅ **Product Flavors**: Two distinct build variants (github/playstore)
- ✅ **Build Scripts**: One-command PowerShell scripts for each flavor
- ✅ **Build Config Flags**: `IS_GITHUB_VERSION`, `ENABLE_IN_APP_UPDATE`
- ✅ **Automatic Detection**: Runtime flavor detection

### GitHub Flavor Features
- ✅ In-app update checking (24-hour interval)
- ✅ Manual "Check for Updates" button in profile
- ✅ Automatic update notifications
- ✅ APK download with progress bar
- ✅ Changelog display in update dialog
- ✅ Version skip functionality

### Play Store Flavor Features
- ✅ Clean build without update system
- ✅ No GitHub API dependencies for updates
- ✅ Play Store compliant
- ✅ Optimized APK size (unused update code removed)

---

## 🔧 Technical Improvements

### Code Changes
1. **UpdateManager.kt**
   - Added flavor detection
   - Disables update checks for Play Store flavor
   - Returns null for all update queries in Play Store flavor

2. **ProfileScreen.kt**
   - Conditional "Check for Updates" button
   - Only visible in GitHub flavor
   - Cleaner UI in Play Store flavor

3. **build.gradle.kts**
   - Added product flavors configuration
   - Version management centralized
   - Build config fields for runtime detection

### Build Scripts
- `build-github-release.ps1`: GitHub version builder
- `build-playstore-release.ps1`: Play Store version builder
- `build-both-versions.ps1`: Builds both versions

### Documentation
- `DUAL_FLAVOR_BUILD_SYSTEM.md`: Complete technical guide
- `BUILD_QUICK_REFERENCE.md`: Quick command reference
- `BUILD_FLAVORS_VISUAL_COMPARISON.md`: Visual comparison
- `BUILD_SYSTEM_COMPLETE.md`: Implementation summary

---

## 🎯 Benefits

### For Developers
- **Easy builds**: Single command for each flavor
- **No code changes**: Switch flavors without modifying code
- **Clear separation**: Different behaviors for different distributions
- **Well documented**: Comprehensive guides and references

### For Users
- **GitHub users**: Get instant updates via in-app system
- **Play Store users**: Standard Play Store update experience
- **No confusion**: Each version optimized for its platform

### For Distribution
- **Flexibility**: Support both distribution channels
- **Compliance**: Play Store version meets all requirements
- **Speed**: GitHub version allows instant releases
- **Options**: Choose the right flavor for each scenario

---

## 📦 Build Outputs

### GitHub Flavor
- **Location**: `app/build/outputs/apk/github/release/`
- **Filename**: `app-github-release.apk`
- **Version**: 7.0.0-github
- **Features**: In-app updates enabled

### Play Store Flavor
- **Location**: `app/build/outputs/apk/playstore/release/`
- **Filename**: `app-playstore-release.apk`
- **Version**: 7.0.0
- **Features**: Clean Play Store build

---

## 🔄 Migration Guide

### From Previous Version (6.0.5)

**No migration needed for users!** Both flavors are backward compatible.

**For developers:**
1. Pull latest code
2. Use new build scripts instead of old commands
3. Choose appropriate flavor for distribution

**Build command changes:**
- **Old**: `.\gradlew.bat assembleRelease`
- **New (GitHub)**: `.\build-github-release.ps1` or `.\gradlew.bat assembleGithubRelease`
- **New (Play Store)**: `.\build-playstore-release.ps1` or `.\gradlew.bat assemblePlaystoreRelease`

---

## 🐛 Bug Fixes

- **Fixed**: Update system now properly disabled for Play Store builds
- **Fixed**: Version naming inconsistencies between flavors
- **Fixed**: Profile screen UI adapts to build flavor
- **Optimized**: Unused update code removed in Play Store builds

---

## 📊 Performance

### APK Size
- **GitHub flavor**: Similar to previous version (~same size)
- **Play Store flavor**: Slightly smaller (update code optimized out)

### Build Time
- **GitHub flavor**: ~1-2 minutes (with clean)
- **Play Store flavor**: ~1-2 minutes (with clean)
- **Both flavors**: ~3-4 minutes (sequential build)

### Runtime Performance
- **No overhead**: Flavor detection is compile-time
- **Optimized**: R8 removes unused code paths
- **Clean**: ProGuard rules applied to both flavors

---

## ⚠️ Breaking Changes

### For End Users
- **None**: Both flavors maintain all existing functionality

### For Developers
- **Build commands**: Use new flavor-specific commands
- **Version detection**: Use `BuildConfig.ENABLE_IN_APP_UPDATE` instead of hardcoded checks
- **Testing**: Must test both flavors separately

---

## 🎯 Testing Performed

### GitHub Flavor
- ✅ Build succeeds without errors
- ✅ APK installs correctly
- ✅ Update button visible in profile
- ✅ Version shows as "7.0.0-github"
- ✅ All features work as expected

### Play Store Flavor
- ✅ Build succeeds without errors
- ✅ APK installs correctly
- ✅ Update button hidden
- ✅ Version shows as "7.0.0"
- ✅ All features work as expected

### Both Flavors
- ✅ Can be installed side-by-side for testing
- ✅ No conflicts between flavors
- ✅ Proper signing configuration
- ✅ R8 optimization working

---

## 📝 Known Issues

**None at this time!** 🎉

If you encounter any issues:
1. Check the documentation: `DUAL_FLAVOR_BUILD_SYSTEM.md`
2. Try cleaning: `.\gradlew.bat clean`
3. Report on GitHub: https://github.com/atrajit-sarkar/HabitTracker/issues

---

## 🚀 What's Next

### Planned for v7.1.0
- Monitor feedback from both distribution channels
- Optimize APK sizes further
- Enhance build scripts with additional features
- Consider adding more build variants if needed

### Future Improvements
- Automated build pipeline (GitHub Actions)
- Automatic version bumping
- Release automation
- Beta testing workflow

---

## 📖 Documentation

All documentation available in:
- `DUAL_FLAVOR_BUILD_SYSTEM.md` - Complete guide
- `BUILD_QUICK_REFERENCE.md` - Quick commands
- `BUILD_FLAVORS_VISUAL_COMPARISON.md` - Visual guide
- `BUILD_SYSTEM_COMPLETE.md` - Implementation summary

---

## 🙏 Acknowledgments

- Modern Android build system using Gradle product flavors
- Clean architecture principles for feature separation
- Community feedback on distribution needs

---

## 📞 Support

- **Documentation**: See markdown files in project root
- **Issues**: https://github.com/atrajit-sarkar/HabitTracker/issues
- **Releases**: https://github.com/atrajit-sarkar/HabitTracker/releases

---

## 🎉 Summary

Version 7.0.0 introduces a **professional dual-flavor build system** that allows seamless switching between GitHub and Play Store distributions. Both flavors are production-ready, fully optimized, and come with comprehensive documentation and build scripts.

**GitHub Flavor** (v7.0.0-github):
- For direct distribution
- In-app update system active
- Perfect for beta testing

**Play Store Flavor** (v7.0.0):
- For official store release
- Clean, compliant build
- Standard Play Store updates

Build whichever version you need with a single command! 🚀

---

**Released**: October 22, 2025  
**Download**: https://github.com/atrajit-sarkar/HabitTracker/releases/tag/v7.0.0
