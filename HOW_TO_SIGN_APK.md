# How to Sign and Install APK

## The Issue
You **cannot install an unsigned APK**. Android requires all APKs to be signed with a certificate.

## Quick Solution: Use Debug APK Instead

The **debug APK is automatically signed** with a debug certificate and can be installed immediately:

### Build and Install Debug APK
```bash
cd e:\CodingWorld\AndroidAppDev\HabitTracker
.\gradlew installDebug
```

**Note**: Debug APK is larger (79.57 MB) but includes all the optimizations when running.

---

## Proper Solution: Sign the Release APK

### Option A: Configure Signing in build.gradle.kts (Recommended)

Add this to `app/build.gradle.kts`:

```kotlin
android {
    // ... existing config
    
    signingConfigs {
        create("release") {
            // For testing - DO NOT use in production!
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

Then rebuild:
```bash
.\gradlew assembleRelease
.\gradlew installRelease
```

### Option B: Sign Manually with Existing Keystore

If you have a keystore for production:

```bash
# Sign APK
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 `
  -keystore C:\path\to\your-keystore.jks `
  app\build\outputs\apk\release\app-release-unsigned.apk `
  your-key-alias

# Align APK
zipalign -v 4 `
  app\build\outputs\apk\release\app-release-unsigned.apk `
  app\build\outputs\apk\release\app-release-signed.apk

# Install
adb install -r app\build\outputs\apk\release\app-release-signed.apk
```

### Option C: Create Production Keystore

```bash
# Generate keystore (ONE TIME ONLY - KEEP THIS SAFE!)
keytool -genkey -v `
  -keystore habit-tracker-release.jks `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -alias habit-tracker-key

# Follow prompts to set password and details
# IMPORTANT: Save the password and keystore file securely!
```

Then use Option B to sign with this keystore.

---

## Recommended for Now: Use Debug Build

Since you're testing, use the debug build which is automatically signed:

```bash
# Build and install debug APK (already optimized with all features)
.\gradlew installDebug
```

**Pros:**
- ✅ Automatically signed
- ✅ Can be installed immediately
- ✅ All optimizations are active
- ✅ Performance is the same

**Cons:**
- ❌ Larger size (79.57 MB vs 28.17 MB)
- ❌ Debug symbols included (but won't affect user experience)

---

## For Play Store Distribution

You MUST use a production keystore:

1. **Never use debug keystore for production**
2. **Create a production keystore** (Option C above)
3. **Keep keystore file safe** - if you lose it, you can never update your app!
4. **Use App Bundle** instead of APK for better size optimization

```bash
# Build App Bundle (signed)
.\gradlew bundleRelease
# Result: app-release.aab (~25 MB, user downloads ~20 MB)
```

---

## Summary

| Build Type | Size | Signed | Can Install | Use Case |
|------------|------|--------|-------------|----------|
| Debug | 79.57 MB | ✅ Yes | ✅ Yes | Testing |
| Release (unsigned) | 28.17 MB | ❌ No | ❌ No | N/A |
| Release (signed) | 28.17 MB | ✅ Yes | ✅ Yes | Distribution |
| App Bundle | ~25 MB | ✅ Yes | ✅ Yes (Play Store) | Play Store |

**For now**: Use `.\gradlew installDebug` to test all your optimizations!
