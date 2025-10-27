# 🚀 Firebase Rules Deployment Checklist

## Pre-Deployment

- [ ] Read `FIREBASE_RULES_GUIDE.md` completely
- [ ] Backup existing rules from Firebase Console
- [ ] Review all changes in `firestore.rules` and `storage.rules`
- [ ] Test rules locally using Firebase Emulator (optional but recommended)

## Deployment Steps

### Option 1: Firebase Console (Easiest)

#### Firestore Rules
1. - [ ] Open [Firebase Console](https://console.firebase.google.com/)
2. - [ ] Select your project: **HabitTracker**
3. - [ ] Go to **Firestore Database** → **Rules** tab
4. - [ ] **IMPORTANT**: Click "View current rules" and save a copy (backup)
5. - [ ] Open `firestore.rules` file from this project
6. - [ ] Copy all content (Ctrl+A, Ctrl+C)
7. - [ ] Paste into Firebase Console editor
8. - [ ] Click **Publish**
9. - [ ] Wait for confirmation message
10. - [ ] Verify: Rules should show "Published" status with timestamp

#### Storage Rules
1. - [ ] In Firebase Console, go to **Storage** → **Rules** tab
2. - [ ] **IMPORTANT**: Save current rules as backup
3. - [ ] Open `storage.rules` file from this project
4. - [ ] Copy all content (Ctrl+A, Ctrl+C)
5. - [ ] Paste into Firebase Console editor
6. - [ ] Click **Publish**
7. - [ ] Wait for confirmation message
8. - [ ] Verify: Rules should show "Published" status with timestamp

### Option 2: Firebase CLI (Advanced)

1. - [ ] Install Firebase CLI: `npm install -g firebase-tools`
2. - [ ] Login: `firebase login`
3. - [ ] Navigate to project directory
4. - [ ] Run: `firebase deploy --only firestore:rules,storage:rules`
5. - [ ] Wait for deployment to complete
6. - [ ] Check for any errors in output

## Post-Deployment Verification

### Immediate Checks (Within 5 minutes)

- [ ] **Test App Login**: Launch app and sign in with test account
- [ ] **Test Habits**: 
  - [ ] Create a new habit
  - [ ] View existing habits
  - [ ] Mark a habit complete
  - [ ] Edit a habit
- [ ] **Test Chat**:
  - [ ] Open a chat with a friend
  - [ ] Send a message
  - [ ] Receive a message
- [ ] **Test Friends**:
  - [ ] View friends list
  - [ ] View leaderboard
  - [ ] Search for a user (if applicable)
- [ ] **Test Profile**:
  - [ ] View your profile
  - [ ] Update avatar (if applicable)
  - [ ] Check rewards/diamonds

### Firebase Console Checks

- [ ] Go to **Firestore Database** → **Usage** tab
- [ ] Check for any spikes in "Denied Requests"
- [ ] Go to **Storage** → **Usage** tab
- [ ] Check for any permission errors
- [ ] Review logs for any "permission denied" errors

### Advanced Verification (Optional)

- [ ] Test with multiple user accounts
- [ ] Try accessing another user's data (should fail)
- [ ] Test file uploads (avatar, chat images)
- [ ] Test friend requests flow completely
- [ ] Test chat with new conversation

## Rollback Plan (If Issues Occur)

If you encounter critical issues:

### Quick Rollback via Console

1. - [ ] Go to Firebase Console → Firestore → Rules
2. - [ ] Click on "History" or find your backup
3. - [ ] Restore previous version
4. - [ ] Click "Publish"
5. - [ ] Repeat for Storage rules

### Rollback via CLI

```bash
# Restore from backup files
firebase deploy --only firestore:rules,storage:rules
```

## Common Issues & Solutions

### Issue: "Permission Denied" errors after deployment

**Solution**:
1. Check Firebase Console → Usage tab for specific denied requests
2. Verify user is authenticated (signed in)
3. Check if the affected feature uses correct collection/path names
4. Review the specific rule for that path in `firestore.rules`

### Issue: Chat notifications stopped working

**Solution**:
1. Ensure Cloud Functions still have access
2. Check that `users` collection is readable for FCM token retrieval
3. Verify chat message creation rules allow Cloud Functions

### Issue: File uploads failing

**Solution**:
1. Check Storage rules are deployed
2. Verify file size limits (images: 10MB, audio: 50MB)
3. Ensure file types match rules (image/*, audio/*)
4. Check user is authenticated

### Issue: Friend requests not working

**Solution**:
1. Verify `friendRequests` collection rules
2. Check `friendships` collection rules
3. Ensure `userProfiles` is readable by authenticated users
4. Test friend search functionality

## Monitoring (First 24 Hours)

Set up monitoring to catch issues early:

- [ ] **Hour 1**: Check Firebase Console every 15 minutes
- [ ] **Hour 2-4**: Check every 30 minutes
- [ ] **Hour 4-24**: Check every 2-4 hours
- [ ] Monitor app crash reports (if using Firebase Crashlytics)
- [ ] Watch user feedback channels

### What to Monitor:

1. **Firestore Usage Tab**:
   - Denied requests count
   - Read/Write operations
   - Error patterns

2. **Storage Usage Tab**:
   - Failed uploads
   - Permission errors
   - Bandwidth usage

3. **App Performance**:
   - User reports of "permission denied"
   - Features not loading
   - Slower than usual operations

## Success Criteria

✅ **Deployment Successful If**:

- No increase in "permission denied" errors
- All app features work normally
- Users can login and use the app
- Chat messages send/receive correctly
- Friend requests work
- Habits CRUD operations work
- File uploads succeed
- No user complaints for 24 hours

## Documentation

After successful deployment:

- [ ] Update deployment date in `FIREBASE_RULES_GUIDE.md`
- [ ] Document any custom changes made to rules
- [ ] Note any issues encountered and solutions
- [ ] Update app version compatibility notes
- [ ] Commit rules to Git with descriptive message

Example Git commit:
```bash
git add firestore.rules storage.rules FIREBASE_RULES_GUIDE.md
git commit -m "Deploy production Firebase rules v1.0.0 - Covers all collections and storage paths"
git push
```

## Contact/Support

If you need help:
1. Review `FIREBASE_RULES_GUIDE.md` for detailed explanations
2. Check Firebase documentation: https://firebase.google.com/docs/rules
3. Test rules using Firebase Rules Playground
4. Review Firebase Console error logs

---

## Emergency Contact

**Firebase Project**: HabitTracker  
**Last Rules Update**: October 27, 2025  
**Rules Version**: 1.0.0  

### Quick Links:
- [Firebase Console](https://console.firebase.google.com/)
- [Firestore Rules](https://console.firebase.google.com/project/_/firestore/rules)
- [Storage Rules](https://console.firebase.google.com/project/_/storage/rules)
- [Firebase Documentation](https://firebase.google.com/docs)

---

**🎉 You're ready to deploy! Good luck!**

Remember: Always keep a backup of your current rules before deploying new ones!
