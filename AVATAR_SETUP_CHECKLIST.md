# ✅ Custom Avatar Upload - Implementation Checklist

## 📋 Complete Setup Checklist

### Phase 1: GitHub Repository Setup (5 minutes)

- [ ] **Access GitHub Repository**
  - Go to: https://github.com/gongobongofounder/habit-tracker-avatar-repo
  - Ensure you have write access
  
- [ ] **Create Folder Structure**
  - [ ] Create folder: `avatars/`
  - [ ] Create subfolder: `avatars/users/`
  - [ ] Add `.gitkeep` file in `users/` folder
  
  ```
  Quick way:
  1. Click "Add file" → "Create new file"
  2. Type: avatars/users/.gitkeep
  3. Commit
  ```

- [ ] **Verify Repository Structure**
  ```
  habit-tracker-avatar-repo/
  ├── README.md
  └── avatars/
      └── users/
          └── .gitkeep
  ```

### Phase 2: GitHub Token Generation (3 minutes)

- [ ] **Navigate to Token Settings**
  - Go to: https://github.com/settings/tokens
  - Or: GitHub → Settings → Developer settings → Personal access tokens
  
- [ ] **Generate New Token**
  - [ ] Click "Generate new token (classic)"
  - [ ] Token name: `Habit Tracker Avatar Upload`
  - [ ] Expiration: 90 days (recommended) or No expiration
  - [ ] Scopes: Check ✅ `repo` (Full control of private repositories)
  
- [ ] **Save Token Securely**
  - [ ] Copy token immediately (format: `ghp_...`)
  - [ ] Store in password manager
  - [ ] DO NOT commit to Git!
  - [ ] DO NOT share publicly!

### Phase 3: Code Integration (2 minutes)

- [ ] **Open MainActivity.kt**
  - File: `app/src/main/java/com/example/habittracker/MainActivity.kt`
  
- [ ] **Add Token Initialization**
  
  **OPTION A: Simple (Development/Testing)**
  ```kotlin
  // In onCreate() method, around line 98-100
  // Add this BEFORE avatarConfig.autoInitialize()
  
  avatarConfig.initialize("ghp_YOUR_TOKEN_HERE")
  ```
  
  **OPTION B: Secure (Production)**
  ```kotlin
  // First time setup (do once):
  SecureTokenStorage.storeToken(this, "ghp_YOUR_TOKEN_HERE")
  
  // Then in onCreate():
  val token = SecureTokenStorage.getToken(this)
  if (token != null) {
      avatarConfig.initialize(token)
  }
  ```

- [ ] **Verify Code Structure**
  ```kotlin
  @Inject
  lateinit var avatarConfig: it.atraj.habittracker.avatar.AvatarConfig
  
  override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      
      // ... other code ...
      
      // Initialize avatar feature
      avatarConfig.initialize("ghp_YOUR_TOKEN")  // ← Add this line
      avatarConfig.autoInitialize()
      
      // ... rest of code ...
  }
  ```

### Phase 4: Build & Test (5 minutes)

- [ ] **Build the App**
  ```bash
  ./gradlew assembleDebug
  ```
  - [ ] Build succeeds without errors
  - [ ] No compilation issues
  
- [ ] **Install on Device**
  ```bash
  ./gradlew installDebug
  ```
  - [ ] App installs successfully
  - [ ] App launches without crashing

- [ ] **Test Basic Navigation**
  - [ ] Open app
  - [ ] Sign in (if needed)
  - [ ] Navigate to Profile screen
  - [ ] Tap on avatar
  - [ ] Avatar picker dialog opens

### Phase 5: Feature Testing (10 minutes)

#### Test Upload Functionality

- [ ] **Upload Icon Visible**
  - [ ] Upload icon appears in top-right of dialog
  - [ ] Icon is clickable
  
- [ ] **Select Image**
  - [ ] Tap upload icon
  - [ ] Image picker opens
  - [ ] Select a photo from gallery
  
- [ ] **Upload Process**
  - [ ] Progress indicator appears
  - [ ] "Uploading..." text shows
  - [ ] Wait for completion (2-5 seconds)
  
- [ ] **Upload Success**
  - [ ] New avatar appears in grid
  - [ ] Avatar has star (⭐) badge
  - [ ] Avatar is selectable
  - [ ] No error messages
  
- [ ] **Avatar Display**
  - [ ] Select the new avatar
  - [ ] Dialog closes
  - [ ] Avatar appears in profile
  - [ ] Avatar visible in top bar (HomeScreen)

#### Test Default Avatars

- [ ] **View Default Avatars**
  - [ ] 8 default avatars visible
  - [ ] Avatars load correctly
  - [ ] No star badges on defaults
  
- [ ] **Select Default Avatar**
  - [ ] Tap any default avatar
  - [ ] Checkmark appears
  - [ ] Avatar updates in profile

#### Test Delete Functionality

- [ ] **Custom Avatar Options**
  - [ ] Delete icon (🗑️) visible on custom avatars
  - [ ] NO delete icon on default avatars
  
- [ ] **Delete Process**
  - [ ] Tap delete icon on custom avatar
  - [ ] Confirmation dialog appears
  - [ ] Confirm deletion
  - [ ] Avatar removed from grid
  
- [ ] **Verify GitHub**
  - [ ] Check GitHub repository
  - [ ] File should be deleted from `avatars/users/{userId}/`

#### Test Error Handling

- [ ] **No Internet Test**
  - [ ] Turn off internet
  - [ ] Try to upload
  - [ ] Error message appears
  - [ ] Error is user-friendly
  
- [ ] **Large Image Test**
  - [ ] Upload very large image (>5MB)
  - [ ] Image compresses automatically
  - [ ] Upload succeeds

### Phase 6: Cross-Screen Verification (5 minutes)

- [ ] **Profile Screen**
  - [ ] Custom avatar displays correctly
  - [ ] Glittering effect works (if enabled)
  - [ ] Long-press to enlarge works
  
- [ ] **Home Screen Top Bar**
  - [ ] Avatar visible in top-right
  - [ ] Correct avatar shows
  - [ ] Circular shape maintained
  
- [ ] **Friends List**
  - [ ] Your avatar shows in social features
  - [ ] Other users see your new avatar
  
- [ ] **Chat Screen**
  - [ ] Avatar appears in chat header
  - [ ] Avatar in chat list
  
- [ ] **Leaderboard**
  - [ ] Avatar in leaderboard entries
  - [ ] Avatar in podium (if applicable)

### Phase 7: Firestore Verification (3 minutes)

- [ ] **Check Firestore Console**
  - Go to: Firebase Console → Firestore Database
  - Navigate to: `users/{your_uid}`
  
- [ ] **Verify Field**
  - [ ] `customAvatar` field exists
  - [ ] Value is a URL: `https://raw.githubusercontent.com/...`
  - [ ] URL is accessible (test in browser)

### Phase 8: Security Check (2 minutes)

- [ ] **Token Security**
  - [ ] Token NOT in Git commits
  - [ ] Token NOT in logs
  - [ ] Token stored securely
  
- [ ] **Check .gitignore**
  - [ ] Token files ignored (if using files)
  - [ ] No credentials in repository

### Phase 9: Production Preparation (Optional)

If deploying to production:

- [ ] **Use Secure Storage**
  - [ ] Implement `SecureTokenStorage`
  - [ ] Add security-crypto dependency
  - [ ] Test encrypted storage
  
- [ ] **Token Management**
  - [ ] Document token location
  - [ ] Set up token rotation
  - [ ] Create backup token
  
- [ ] **Monitoring**
  - [ ] Set up error tracking
  - [ ] Monitor upload failures
  - [ ] Track usage metrics

### Phase 10: Documentation (1 minute)

- [ ] **Read Documentation**
  - [ ] `AVATAR_SETUP_QUICK_START.md`
  - [ ] `CUSTOM_AVATAR_UPLOAD_GUIDE.md`
  - [ ] `AVATAR_IMPLEMENTATION_SUMMARY.md`
  - [ ] `AVATAR_VISUAL_FLOW.md`

## 🎯 Success Criteria

### Minimum Requirements (Must Have)
- ✅ GitHub repository created with folder structure
- ✅ Personal access token generated
- ✅ Token initialized in MainActivity
- ✅ App builds and runs without errors
- ✅ Upload functionality works
- ✅ Avatars display across all screens

### Ideal Setup (Should Have)
- ✅ Secure token storage implemented
- ✅ Delete functionality tested
- ✅ Error handling verified
- ✅ Cross-screen display confirmed
- ✅ Firestore updates verified

### Production Ready (Nice to Have)
- ✅ Token rotation plan in place
- ✅ Monitoring and logging configured
- ✅ Backup token available
- ✅ User feedback collected

## 📊 Progress Tracker

```
Total Steps: 48
Completed: ___/48

Phase 1 (GitHub Repo):     ___/4
Phase 2 (Token):           ___/5
Phase 3 (Code):            ___/3
Phase 4 (Build):           ___/4
Phase 5 (Testing):         ___/17
Phase 6 (Cross-Screen):    ___/6
Phase 7 (Firestore):       ___/3
Phase 8 (Security):        ___/3
Phase 9 (Production):      ___/3
Phase 10 (Documentation):  ___/1
```

## ⏱️ Time Estimate

- **Quick Setup**: 10 minutes (Phases 1-4)
- **Full Testing**: 20 minutes (Phases 1-6)
- **Production Ready**: 35 minutes (All phases)

## 🆘 Troubleshooting Quick Links

### Common Issues

**Build Errors**
→ Check: `get_errors` tool in VS Code
→ Verify: All dependencies synced

**Upload Fails**
→ Check: GitHub token is valid
→ Verify: Internet connection
→ Test: Token has `repo` permissions

**Avatar Not Showing**
→ Check: Firestore console
→ Verify: URL is accessible
→ Clear: App cache and restart

**Token Not Working**
→ Check: Token copied correctly
→ Verify: No extra spaces
→ Test: Token on GitHub API

### Debug Commands

```bash
# Check logs
adb logcat | grep -E "Avatar|GitHub"

# Check Firestore
# Go to Firebase Console

# Test GitHub API manually
curl -H "Authorization: token ghp_..." \
     https://api.github.com/user
```

## ✨ You're Done When...

1. ✅ You can upload a custom avatar
2. ✅ Avatar appears with star badge
3. ✅ Avatar displays in profile screen
4. ✅ Avatar syncs to Firestore
5. ✅ File exists in GitHub repository
6. ✅ No errors in logs

## 🎉 Congratulations!

Once all checkboxes are marked, you have successfully implemented the custom avatar upload feature!

**Next Steps:**
- Share with users
- Collect feedback
- Monitor usage
- Iterate and improve

---

**Need Help?**
- See: `CUSTOM_AVATAR_UPLOAD_GUIDE.md` for detailed documentation
- Check: Logs with `adb logcat | grep Avatar`
- Review: Error messages in app UI

**Feature Working?** 🎊
Your users can now upload custom profile pictures!
