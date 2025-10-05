# 🔐 How to Add GitHub Token (For Developers)

## ⚠️ IMPORTANT: Token Security

**NEVER commit the GitHub token to the repository!**

The token is stored in encrypted device storage, not in the code. This file explains how to add it manually.

## For New Installations

If you clone this repository and want to enable avatar upload functionality, follow these steps:

### Method 1: Using ADB (Recommended for Testing)

1. **Get your GitHub token** (see "How to Generate Token" below)

2. **Create a helper script** (one-time setup):

Save this as `setup-token.ps1`:
```powershell
# setup-token.ps1
param(
    [Parameter(Mandatory=$true)]
    [string]$Token
)

$packageName = "it.atraj.habittracker"
$command = "am broadcast -a com.habittracker.SET_TOKEN --es token '$Token' -n $packageName/.TokenReceiver"

Write-Host "Setting up GitHub token..." -ForegroundColor Green
adb shell $command
Write-Host "Token configured!" -ForegroundColor Green
Write-Host "Restart the app to activate avatar upload feature." -ForegroundColor Yellow
```

3. **Run the script**:
```powershell
.\setup-token.ps1 -Token "ghp_YOUR_TOKEN_HERE"
```

### Method 2: Using Android Studio Debugger

1. **Set a breakpoint** in `MainActivity.initializeAvatarFeature()`

2. **In Debug Console**, run:
```kotlin
it.atraj.habittracker.avatar.SecureTokenStorage.storeToken(this, "ghp_YOUR_TOKEN_HERE")
```

3. **Resume execution**

### Method 3: Add Temporary Setup Code (Remove After First Run)

⚠️ **Use this only locally, NEVER commit it!**

1. **Add this to `MainActivity.onCreate()` temporarily**:
```kotlin
// TODO: REMOVE BEFORE COMMITTING!
// One-time token setup
if (!it.atraj.habittracker.avatar.SecureTokenStorage.hasToken(this)) {
    it.atraj.habittracker.avatar.SecureTokenStorage.storeToken(
        this, 
        "ghp_YOUR_TOKEN_HERE"
    )
}
```

2. **Run the app once** - token will be stored in encrypted storage

3. **IMMEDIATELY REMOVE the code** - token is now persisted

4. **Verify removal** - make sure no token exists in code before committing!

### Method 4: Settings Screen (Recommended for Production)

Create a developer settings screen in the app:

```kotlin
// In a debug-only settings screen
@Composable
fun DeveloperSettings() {
    var token by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    
    Column {
        Text("GitHub Token Setup (Development Only)")
        
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("GitHub Token") },
            singleLine = true
        )
        
        Button(onClick = {
            val context = LocalContext.current
            SecureTokenStorage.storeToken(context, token)
            showSuccess = true
            token = "" // Clear immediately
        }) {
            Text("Save Token")
        }
        
        if (showSuccess) {
            Text("Token saved successfully!", color = Color.Green)
        }
    }
}
```

## How to Generate Token

1. Go to: https://github.com/settings/tokens

2. Click: **"Generate new token (classic)"**

3. Configure:
   - **Name**: `Habit Tracker Avatar Upload`
   - **Expiration**: 90 days (recommended)
   - **Scopes**: ✅ `repo` (Full control of repositories)

4. Click: **"Generate token"**

5. **COPY THE TOKEN IMMEDIATELY** (you won't see it again!)
   - Format: `ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

## Verify Token is Configured

1. **Check logs** after app launch:
```bash
adb logcat | grep "Avatar upload feature"
```

**Expected output**:
```
MainActivity: ✅ Avatar upload feature initialized
```

**If not configured**:
```
MainActivity: ⚠️ GitHub token not found - avatar upload disabled
```

2. **Test upload**:
   - Go to Profile screen
   - Tap on avatar
   - Upload icon should be visible and functional

## Token Storage Location

- **File**: `/data/data/it.atraj.habittracker/shared_prefs/avatar_secure_prefs.xml`
- **Encryption**: AES256-GCM via Android Keystore
- **Access**: Only this app can decrypt
- **Visibility**: Encrypted gibberish if viewed directly

## Security Checklist

Before committing to Git:

- [ ] ✅ No `ghp_...` tokens in any `.kt` files
- [ ] ✅ No tokens in `.md` documentation files (except examples)
- [ ] ✅ No tokens in `.properties` files
- [ ] ✅ No tokens in build scripts
- [ ] ✅ `.gitignore` includes sensitive files
- [ ] ✅ Ran search: `git grep -i "ghp_"` returns nothing

### Quick Check Command:
```bash
# This should return NOTHING:
git grep -E "ghp_[a-zA-Z0-9]{36}"
```

## For Other Developers

When setting up the project:

1. **Clone the repository**
```bash
git clone https://github.com/atrajit-sarkar/HabitTracker.git
```

2. **Follow Method 1, 2, or 4 above** to add your token

3. **Never share your token** - each developer should have their own

4. **Token is device-specific** - no need to reconfigure per installation on same device

## Troubleshooting

### "Avatar upload disabled"
→ Token not stored yet. Follow setup methods above.

### "Upload fails"
→ Check token has `repo` permissions
→ Verify token hasn't expired
→ Test token: `curl -H "Authorization: token YOUR_TOKEN" https://api.github.com/user`

### "Token not persisting"
→ Check app has storage permissions
→ Verify device has Android 6.0+ (API 23+)
→ Check for errors in Logcat

## Production Deployment

For production builds:

1. **Use build variants** with different configurations
2. **Never include token in source code**
3. **Consider backend service** for uploads (more secure)
4. **Or use OAuth App authentication** instead of personal tokens

## Emergency: Token Exposed

If token is accidentally committed:

1. **Immediately revoke it**:
   - Go to: https://github.com/settings/tokens
   - Find the token
   - Click "Delete"

2. **Generate new token** (follow steps above)

3. **Update all devices** with new token

4. **Remove from Git history**:
```bash
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch path/to/file" \
  --prune-empty --tag-name-filter cat -- --all
```

5. **Force push** (⚠️ coordinates with team first!):
```bash
git push origin --force --all
```

## Summary

✅ **DO**:
- Store token in encrypted device storage
- Use SecureTokenStorage API
- Generate personal tokens for development
- Keep tokens in password manager

❌ **DON'T**:
- Hardcode tokens in source code
- Commit tokens to Git
- Share tokens between developers
- Store tokens in plain text files

---

**Remember**: The token is like a password. Treat it with the same level of security!
