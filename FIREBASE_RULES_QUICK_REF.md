# 📋 Firebase Rules Quick Reference

## 🔥 Firestore Collections

| Collection | Path | Read Access | Write Access |
|------------|------|-------------|--------------|
| **users** | `/users/{userId}` | Owner only | Owner only |
| ↳ habits | `/users/{userId}/habits/{habitId}` | Owner only | Owner only |
| ↳ completions | `/users/{userId}/completions/{completionId}` | Owner only | Owner only |
| ↳ read_news | `/users/{userId}/read_news/{newsId}` | Owner only | Owner only |
| **userProfiles** | `/userProfiles/{profileId}` | All authenticated | Owner only |
| **friendRequests** | `/friendRequests/{requestId}` | Sender & Recipient | Sender (create), Recipient (update) |
| **friendships** | `/friendships/{friendshipId}` | Both users | Both users (create/delete) |
| **chats** | `/chats/{chatId}` | Participants | Participants |
| ↳ messages | `/chats/{chatId}/messages/{messageId}` | Participants | Sender (own messages) |
| **app_news** | `/app_news/{newsId}` | All authenticated | Admin only (currently disabled) |

## 🗄️ Storage Paths

| Path | Read Access | Write Access | File Type | Max Size |
|------|-------------|--------------|-----------|----------|
| `avatars/{userId}/{fileName}` | Public | Owner only | Images | 10 MB |
| `habits/{userId}/{habitId}/{fileName}` | Authenticated | Owner only | Images | 10 MB |
| `chats/{chatId}/images/{imageFile}` | Authenticated | Authenticated | Images | 10 MB |
| `sounds/{userId}/{fileName}` | Public | Owner only | Audio | 50 MB |
| `music/{fileName}` | Public | Admin only | Audio | - |
| `themes/{themeId}/{fileName}` | Public | Admin only | Images | - |
| `hero_backgrounds/{heroId}/{fileName}` | Public | Admin only | Images | - |
| `posts/{postId}/{fileName}` | Authenticated | Authenticated | Images | 10 MB |
| `temp/{userId}/{fileName}` | Owner only | Owner only | Any | 10 MB |

## 🔐 Key Security Rules

### Authentication
```javascript
// All operations require authentication
✅ Must be signed in
❌ Anonymous access (except public storage reads)
```

### Ownership
```javascript
// Users can only access their own data
✅ /users/{myUserId} → Allowed
❌ /users/{otherUserId} → Denied
```

### Social Features
```javascript
// Public profiles for friends/leaderboard
✅ Read any userProfile → Allowed (if authenticated)
✅ Write own userProfile → Allowed
❌ Write other's userProfile → Denied
```

### Chat System
```javascript
// Participants-only access
✅ Read chat if participant → Allowed
✅ Send message if participant → Allowed
❌ Read chat if not participant → Denied
```

## 📝 Common Validations

### Habits
- ✅ Title: 1-100 characters
- ✅ Reminder hour: 0-23
- ✅ Reminder minute: 0-59
- ✅ Required fields: title, frequency, reminderHour, reminderMinute

### Friend Requests
- ✅ Cannot send to self
- ✅ Valid email format
- ✅ Status: PENDING, ACCEPTED, or REJECTED
- ✅ Name must not be empty

### Chat Messages
- ✅ Content: 1-5000 characters
- ✅ Sender must be authenticated user
- ✅ Must be participant of chat

### User Profiles
- ✅ Email: Valid format
- ✅ Display name: 1-50 characters

## 🚫 What's NOT Allowed

❌ Reading other users' private data  
❌ Deleting user profiles  
❌ Creating news articles from client  
❌ Updating friendships (immutable)  
❌ Accessing chats you're not in  
❌ Sending messages as another user  
❌ Uploading files > size limits  
❌ Uploading non-image/audio files to restricted paths  

## 🔧 Helper Functions

```javascript
isAuthenticated()          // User is signed in
isOwner(userId)           // User owns the resource
isParticipant(list)       // User is in participant list
hasRequiredFields(fields) // Data has required fields
isValidEmail(email)       // Email format is valid
isImage()                 // File is an image
isAudio()                 // File is audio
isValidImageSize()        // Image ≤ 10 MB
isValidAudioSize()        // Audio ≤ 50 MB
```

## 🎯 Access Patterns

### Private Data (Owner Only)
```
users/{userId}
  ├─ habits/{habitId}
  ├─ completions/{completionId}
  └─ read_news/{newsId}
```

### Public Data (Authenticated Read)
```
userProfiles/{profileId}
app_news/{newsId}
```

### Shared Data (Conditional Access)
```
friendRequests/{requestId}     → Sender OR Recipient
friendships/{friendshipId}     → User1 OR User2
chats/{chatId}                 → Participants only
  └─ messages/{messageId}      → Participants only
```

## 📊 Performance Tips

1. **Use indexes** for compound queries
2. **Limit query results** with `.limit()`
3. **Chunk whereIn queries** (max 10 items)
4. **Cache frequently accessed data**
5. **Use real-time listeners** sparingly

## 🔄 Default Rules

```javascript
// Anything not explicitly allowed is DENIED
match /{document=**} {
  allow read, write: if false;
}
```

## 📞 Quick Troubleshooting

| Error | Likely Cause | Solution |
|-------|-------------|----------|
| Permission denied (read) | Not authenticated OR wrong user | Check auth state, verify userId |
| Permission denied (write) | Missing required fields | Check data validation |
| Permission denied (storage) | Wrong file type or size | Check file type and size limits |
| Chat not loading | Not a participant | Verify chat membership |
| Friend request failed | Invalid data | Check email format, no self-requests |

## 🔗 Quick Links

- **Deploy**: `firebase deploy --only firestore:rules,storage:rules`
- **Test**: Firebase Console → Rules → Playground
- **Monitor**: Firebase Console → Usage tab
- **Docs**: [Firebase Rules Guide](https://firebase.google.com/docs/rules)

---

**Version**: 1.0.0  
**Last Updated**: October 27, 2025  
**Status**: ✅ Production Ready

---

## 📱 Copy-Paste Deployment Commands

### Deploy All Rules
```bash
firebase deploy --only firestore:rules,storage:rules
```

### Deploy Firestore Only
```bash
firebase deploy --only firestore:rules
```

### Deploy Storage Only
```bash
firebase deploy --only storage:rules
```

### Test Rules Locally
```bash
firebase emulators:start --only firestore,storage
```

---

**🎉 Rules are ready for production deployment!**

For detailed information, see `FIREBASE_RULES_GUIDE.md`
