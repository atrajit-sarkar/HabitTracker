# 🚀 Quick Setup - What YOU Need to Do

## ✅ Code Implementation: DONE
All code has been created and integrated into your app.

## ⚠️ Manual Steps Required (DO THIS NOW)

### Step 1: GitHub Repository Setup (5 minutes)

1. **Go to GitHub**: https://github.com/gongobongofounder/habit-tracker-avatar-repo

2. **Create folder structure**:
   - Click "Add file" → "Create new file"
   - Type: `avatars/users/.gitkeep`
   - Click "Commit new file"

### Step 2: Get GitHub Token (2 minutes)

1. **Go to**: https://github.com/settings/tokens
2. **Click**: "Generate new token (classic)"
3. **Settings**:
   - Name: `Habit Tracker Avatar Upload`
   - Expiration: Choose (90 days recommended)
   - Scopes: Check ✅ `repo` (full control)
4. **Click**: "Generate token"
5. **COPY THE TOKEN IMMEDIATELY** (you'll need it next!)

### Step 3: Add Token to Your App (1 minute)

Open `MainActivity.kt` and find where you injected `avatarConfig`. 

The code is already there, you just need to **add your token**:

```kotlin
// Around line 98-100 in MainActivity.kt
// You'll see this code:
// avatarConfig.autoInitialize()

// BEFORE that, add this:
avatarConfig.initialize("YOUR_GITHUB_TOKEN_HERE")
```

**Full example**:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // ... other code ...
    
    // Add your GitHub token here:
    avatarConfig.initialize("ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
    
    // Then this line will work:
    avatarConfig.autoInitialize()
    
    // ... rest of code ...
}
```

⚠️ **SECURITY WARNING**: 
- Never commit this token to GitHub!
- For testing, hardcode is OK
- For production, use encrypted storage (see full guide)

### Step 4: Test It! (2 minutes)

1. Run the app
2. Go to Profile screen
3. Tap on your avatar
4. You should see:
   - Upload icon (top right)
   - Default avatars (8 images)
5. Tap upload icon
6. Select an image
7. Wait for upload
8. Your custom avatar appears with a star badge!

## That's It! 🎉

### Quick Checklist:
- [ ] Created `avatars/users/` folder in GitHub repo
- [ ] Generated GitHub Personal Access Token
- [ ] Added token to `MainActivity.kt` with `avatarConfig.initialize()`
- [ ] Tested upload feature

### Troubleshooting:

**"GitHub token not initialized"**
→ Did you call `avatarConfig.initialize("your_token")` in MainActivity?

**"Failed to upload to GitHub"**
→ Check your token has `repo` permissions
→ Verify repository exists: https://github.com/gongobongofounder/habit-tracker-avatar-repo

**Upload button doesn't work**
→ Check internet connection
→ Check Logcat for errors: `adb logcat | grep Avatar`

### Where to Add the Token?

**File**: `e:\CodingWorld\AndroidAppDev\HabitTracker\app\src\main\java\com\example\habittracker\MainActivity.kt`

**Location**: In `onCreate()` method, around line 98-100

**Before** (current):
```kotlin
// Auto-initialize avatar upload feature if configured
avatarConfig.autoInitialize()
```

**After** (what you need):
```kotlin
// Initialize avatar upload with GitHub token
avatarConfig.initialize("ghp_YOUR_ACTUAL_TOKEN_HERE")

// Auto-initialize avatar upload feature if configured
avatarConfig.autoInitialize()
```

### Need Full Documentation?
See: `CUSTOM_AVATAR_UPLOAD_GUIDE.md` for complete details, security best practices, and advanced configuration.

---

**Time to complete**: ~10 minutes
**Difficulty**: Easy ⭐

Once you complete these steps, users can upload custom profile pictures! 📸
