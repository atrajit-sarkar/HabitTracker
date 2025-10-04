# 🔐 Setup Instructions for google-services.json

## ⚠️ Important Security Notice

The `google-services.json` file is **intentionally excluded** from this repository for security reasons. This file contains sensitive Firebase configuration data that should not be publicly shared.

## 📋 Steps to Configure Firebase

If you clone this repository, you'll need to add your own `google-services.json` file:

### 1. Create a Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing one
3. Add an Android app to your project

### 2. Configure Your Android App

- **Package name:** `com.example.habittracker`
- **App nickname:** Habit Tracker (or your choice)
- **Debug signing certificate SHA-1:** Get it using:
  ```bash
  # Windows PowerShell
  .\gradlew signingReport
  
  # Or using keytool directly
  keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
  ```

### 3. Enable Required Firebase Services

In your Firebase Console:

1. **Authentication**
   - Go to Authentication → Sign-in method
   - Enable **Email/Password** provider
   - Enable **Google** provider

2. **Firestore Database**
   - Create a Firestore database
   - Start in production mode
   - Set security rules (see below)

### 4. Download google-services.json

1. In Firebase Console → Project Settings
2. Scroll to "Your apps" section
3. Click "Download google-services.json"
4. Place the file at: `app/google-services.json`

### 5. Verify OAuth Configuration

Your `google-services.json` should contain an OAuth client with `client_type: 3` (Web Client):

```json
{
  "oauth_client": [
    {
      "client_id": "YOUR-PROJECT-ID.apps.googleusercontent.com",
      "client_type": 3
    }
  ]
}
```

Copy this Web Client ID to `app/src/main/java/com/example/habittracker/auth/GoogleSignInHelper.kt`:

```kotlin
.requestIdToken("YOUR-WEB-CLIENT-ID.apps.googleusercontent.com")
```

## 🔒 Firestore Security Rules

Set these rules in Firebase Console → Firestore Database → Rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection - users can only read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Habits collection - users can only access their own habits
    match /habits/{habitId} {
      allow read, write: if request.auth != null && 
                            resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && 
                       request.resource.data.userId == request.auth.uid;
    }
  }
}
```

## 📱 SHA-1 Fingerprint Configuration

**CRITICAL:** For Google Sign-In to work, you MUST add your SHA-1 fingerprint to Firebase Console.

### Get Your SHA-1:
```powershell
.\gradlew signingReport
```

Look for the `SHA1:` line in the output.

### Add to Firebase:
1. Firebase Console → Project Settings
2. Scroll to "Your apps"
3. Click "Add fingerprint"
4. Paste your SHA-1
5. Click "Save"
6. Wait 5 minutes for changes to propagate

### For Production:
When building a release APK, get the SHA-1 from your release keystore and add that too:
```bash
keytool -list -v -keystore path/to/release.keystore -alias your-alias
```

## 🚀 Building the Project

After adding `google-services.json`:

```powershell
# Clean build
.\gradlew clean assembleDebug

# Install on connected device
.\gradlew installDebug

# Run the app
adb shell am start -n com.example.habittracker/.MainActivity
```

## ✅ Verification Checklist

Before running the app, verify:

- [ ] `app/google-services.json` exists
- [ ] Package name matches: `com.example.habittracker`
- [ ] SHA-1 fingerprint added to Firebase Console
- [ ] Firebase Authentication enabled (Email & Google)
- [ ] Firestore database created
- [ ] Security rules configured
- [ ] Web Client ID updated in `GoogleSignInHelper.kt`

## 🐛 Troubleshooting

### Google Sign-In Not Working?

1. **Check SHA-1 is added** to Firebase Console
2. **Verify Web Client ID** in `GoogleSignInHelper.kt` matches `google-services.json`
3. **Enable Google Sign-In** in Firebase Console → Authentication
4. **Wait 5 minutes** after adding SHA-1 for changes to propagate
5. **Reinstall the app** after configuration changes

### See Documentation:
- `GOOGLE_SIGNIN_COMPLETE_FIX.md` - Complete Google Sign-In setup
- `ADD_SHA1_FINGERPRINT.md` - SHA-1 configuration details
- `GOOGLE_SIGNIN_DEBUG.md` - Debugging guide

## 📞 Support

If you encounter issues:

1. Check LogCat for errors:
   ```bash
   adb logcat -s AuthRepository:D AuthScreen:D
   ```

2. Verify Firebase configuration:
   - Authentication enabled
   - SHA-1 added
   - Security rules set

3. Review the documentation files in this repository

---

**Remember:** Never commit `google-services.json` to version control! It's in `.gitignore` for a reason. 🔐
