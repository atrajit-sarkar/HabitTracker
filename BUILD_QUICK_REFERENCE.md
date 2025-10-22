# 🎯 Quick Build Reference - v7.0.0

## One-Line Build Commands

### GitHub Version (with in-app updates)
```powershell
.\build-github-release.ps1
```
📦 Output: `app\build\outputs\apk\github\release\app-github-release.apk`

### Play Store Version (clean, no in-app updates)
```powershell
.\build-playstore-release.ps1
```
📦 Output: `app\build\outputs\apk\playstore\release\app-playstore-release.apk`

### Both Versions
```powershell
.\build-both-versions.ps1
```

---

## Key Differences

| Feature | GitHub | Play Store |
|---------|--------|------------|
| Version | 7.0.0-github | 7.0.0 |
| Update Check Button | ✅ Yes | ❌ No |
| Auto Update Check | ✅ Every 24h | ❌ Disabled |
| Update Dialog | ✅ Shows | ❌ Hidden |
| APK Download | ✅ From GitHub | ❌ N/A |
| Update Source | GitHub Releases | Google Play |

---

## When to Use Each

### Use GitHub Flavor When:
- 🎯 Direct distribution to users
- 🧪 Beta testing
- 🚀 Quick releases without review
- 👥 Community/open-source distribution

### Use Play Store Flavor When:
- 🏪 Official app store release
- ✅ Need Play Store compliance
- 📊 Want Play Store analytics
- 🌍 Wide public distribution

---

## Version Info
- **Version Code:** 20
- **Min SDK:** 29 (Android 10+)
- **Target SDK:** 36
- **Package:** it.atraj.habittracker

---

## Gradle Tasks

```bash
# List all available build tasks
.\gradlew.bat tasks --group=build

# Clean build
.\gradlew.bat clean

# Build specific flavor
.\gradlew.bat assembleGithubRelease
.\gradlew.bat assemblePlaystoreRelease

# Build both
.\gradlew.bat assembleRelease

# Install directly to device
.\gradlew.bat installGithubRelease
.\gradlew.bat installPlaystoreRelease
```

---

## Distribution Checklist

### GitHub Release
1. ✅ Build: `.\build-github-release.ps1`
2. ✅ Test APK
3. ✅ Create release at github.com/releases/new
4. ✅ Upload APK
5. ✅ Add changelog
6. ✅ Mark as latest

### Play Store
1. ✅ Build: `.\build-playstore-release.ps1`
2. ✅ Test APK
3. ✅ Go to Play Console
4. ✅ Create new release
5. ✅ Upload APK
6. ✅ Add release notes
7. ✅ Submit for review

---

## Troubleshooting

### Build fails?
```powershell
.\gradlew.bat clean
.\gradlew.bat assembleGithubRelease --stacktrace
```

### Wrong version number?
Edit `app/build.gradle.kts`:
```kotlin
versionCode = 20
versionName = "7.0.0"
```

### Signing issues?
Check `keystore.properties` exists and has correct paths/passwords.

---

## 📖 Full Documentation
See `DUAL_FLAVOR_BUILD_SYSTEM.md` for complete details.
