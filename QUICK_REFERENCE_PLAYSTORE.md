# 🚀 Quick Reference - Play Store Publishing

## 📋 5-Minute Setup Checklist

### 1️⃣ Change Package Name (CRITICAL)
```
File: app/build.gradle.kts
Change: com.example.habittracker → com.yourdomain.habittracker
Lines: 22 and 26
```

### 2️⃣ Update Firebase
- Firebase Console → Update package name
- Download new google-services.json
- Replace app/google-services.json

### 3️⃣ Add Release SHA-1
```powershell
.\gradlew signingReport
```
Copy SHA-1 → Firebase Console → Add fingerprint

### 4️⃣ Build Release
```powershell
.\gradlew clean
.\gradlew bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`

### 5️⃣ Create Assets
- [ ] Privacy policy (host online)
- [ ] Feature graphic (1024x500)
- [ ] 4+ screenshots (1080x2340)

### 6️⃣ Play Console Setup
1. Pay $25 → Create account
2. Create app
3. Fill store listing
4. Complete content rating
5. Fill data safety
6. Upload AAB
7. Submit for review

---

## 📦 Files You Created

| File | Purpose |
|------|---------|
| `PLAY_STORE_SETUP_GUIDE.md` | Complete detailed guide |
| `MANUAL_STEPS_REQUIRED.md` | Step-by-step checklist |
| `PRIVACY_POLICY_TEMPLATE.md` | Privacy policy template |
| `check-playstore-readiness.ps1` | Pre-flight check script |

---

## 🔧 Essential Commands

```powershell
# Check readiness
.\check-playstore-readiness.ps1

# Clean build
.\gradlew clean

# Build release AAB (for Play Store)
.\gradlew bundleRelease

# Build release APK (for testing)
.\gradlew assembleRelease

# Check signing config
.\gradlew signingReport

# Install release APK on device
adb install app/build/outputs/apk/release/app-release.apk
```

---

## ⚠️ Common Issues

**Issue**: "App not installed"
**Fix**: Uninstall debug version first

**Issue**: Google Sign-In not working in release
**Fix**: Add release SHA-1 to Firebase Console

**Issue**: App rejected - "Invalid package name"
**Fix**: Change from com.example.* to your domain

**Issue**: Build fails with signing error
**Fix**: Check keystore.properties has correct values

---

## 📍 Important Locations

**Release AAB**: `app/build/outputs/bundle/release/app-release.aab`
**Release APK**: `app/build/outputs/apk/release/app-release.apk`
**Keystore**: `habit-tracker-release.jks`
**Keystore Config**: `keystore.properties`

---

## 🌐 Important Links

**Play Console**: https://play.google.com/console
**Firebase Console**: https://console.firebase.google.com/
**Privacy Generator**: https://app-privacy-policy-generator.firebaseapp.com/
**Developer Policies**: https://play.google.com/about/developer-content-policy/

---

## 📊 Current App Info

- **Version**: 4.0.0 (code: 8)
- **Package**: com.example.habittracker ⚠️ CHANGE THIS
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36
- **Keystore**: ✅ Ready

---

## 🎯 Timeline Estimate

| Task | Time |
|------|------|
| Change package & Firebase | 30 min |
| Build & test release | 30 min |
| Create privacy policy | 30 min |
| Create graphics/screenshots | 1-2 hours |
| Play Console setup | 1-2 hours |
| Google review | 2-7 days |
| **TOTAL** | **1-2 days + review** |

---

## ✅ Before You Submit

- [ ] Package name changed
- [ ] Firebase updated
- [ ] Release SHA-1 added
- [ ] AAB built successfully
- [ ] Release tested on device
- [ ] Privacy policy hosted
- [ ] Graphics created
- [ ] Screenshots captured
- [ ] Play Console account created
- [ ] Store listing complete
- [ ] Content rating done
- [ ] Data safety filled
- [ ] Release notes written

---

**Start here**: `MANUAL_STEPS_REQUIRED.md`

**Need details?**: `PLAY_STORE_SETUP_GUIDE.md`

**Questions?**: Check Google Play Console Help Center
