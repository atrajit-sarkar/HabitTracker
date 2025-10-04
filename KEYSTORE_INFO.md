# Production Keystore Information

## IMPORTANT: Keep This Information Safe! 🔐

This keystore is used to sign your production APK. **If you lose this, you can NEVER update your app on Play Store!**

### Keystore Details:
```
File: habit-tracker-release.jks
Location: E:\CodingWorld\AndroidAppDev\HabitTracker\habit-tracker-release.jks

Keystore Password: HabitTracker2025!
Key Alias: habit-tracker-key
Key Password: HabitTracker2025!

Algorithm: RSA
Key Size: 2048 bits
Validity: 10000 days (~27 years)
```

### Certificate Information:
```
Owner: CN=Habit Tracker, OU=Development, O=Habit Tracker, L=Unknown, ST=Unknown, C=US
```

---

## ⚠️ CRITICAL: Backup Instructions

1. **Copy the keystore file** to a secure location:
   ```
   Copy-Item habit-tracker-release.jks -Destination "C:\YourSecureBackup\"
   ```

2. **Store passwords securely** (password manager recommended)

3. **Never commit to Git**:
   - Already in `.gitignore`
   - Verify with: `git status`

4. **Share with team securely** (encrypted drive, password manager)

---

## How It's Used

The keystore is configured in `build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("habit-tracker-release.jks")
        storePassword = "HabitTracker2025!"
        keyAlias = "habit-tracker-key"
        keyPassword = "HabitTracker2025!"
    }
}
```

---

## Building Signed APK

```bash
# Build signed release APK (28.17 MB)
.\gradlew assembleRelease

# Build signed App Bundle for Play Store
.\gradlew bundleRelease
```

---

## Verify Keystore

To verify the keystore was created correctly:

```bash
keytool -list -v -keystore habit-tracker-release.jks -storepass "HabitTracker2025!"
```

---

## If Keystore is Lost

**YOU CANNOT RECOVER IT!**

Options:
1. Cannot update existing app on Play Store
2. Must publish as NEW app with different package name
3. Users lose all data/purchases

**Always keep backups in multiple secure locations!**

---

## Production Security Best Practices

### DO:
✅ Keep keystore in secure, backed-up location
✅ Use strong passwords
✅ Restrict access to keystore
✅ Use environment variables for CI/CD
✅ Encrypt backups

### DON'T:
❌ Commit keystore to version control
❌ Share via email/chat
❌ Use same keystore for multiple apps
❌ Store passwords in plain text
❌ Lose the keystore (no recovery possible!)

---

## For Continuous Integration (CI/CD)

Store keystore and passwords as:
- GitHub Secrets
- GitLab CI/CD Variables
- Environment variables

Example:
```bash
export KEYSTORE_PASSWORD="HabitTracker2025!"
export KEY_ALIAS="habit-tracker-key"
export KEY_PASSWORD="HabitTracker2025!"
```

---

**REMEMBER: This keystore is the ONLY way to sign updates to your app. Keep it safe!**
