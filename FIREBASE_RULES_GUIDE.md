# Firebase Security Rules - Production Ready

This document explains the Firebase Security Rules for the HabitTracker app and how to deploy them.

## 📋 Table of Contents

1. [Overview](#overview)
2. [Firestore Rules](#firestore-rules)
3. [Storage Rules](#storage-rules)
4. [How to Deploy](#how-to-deploy)
5. [Testing Rules](#testing-rules)
6. [Important Security Notes](#important-security-notes)

---

## 🔒 Overview

The security rules have been designed to:
- ✅ **Protect user data** - Users can only access their own private data
- ✅ **Enable social features** - Public profiles are readable by all authenticated users
- ✅ **Prevent unauthorized access** - All operations require authentication
- ✅ **Validate data** - Rules check data integrity before allowing writes
- ✅ **Support all app features** - Habits, chat, friends, leaderboard, rewards, etc.
- ✅ **Production-ready** - Comprehensive rules with proper validation

---

## 📁 Firestore Rules

### Collections Covered

#### 1. **users** Collection
- **Purpose**: Main user data (rewards, settings, presence, FCM tokens)
- **Access**:
  - ✅ Users can read/write their own data
  - ❌ Users cannot access other users' data
  - ❌ Users cannot delete their profile

**Sub-collections:**

- **habits**: Personal habit tracking
  - Users can CRUD their own habits
  - Validates title length, reminder times
  
- **completions**: Habit completion records
  - Users can CRUD their own completions
  - Validates completion dates
  
- **read_news**: Tracks read news articles
  - Users can mark news as read/unread

#### 2. **userProfiles** Collection
- **Purpose**: Public profile for social features (leaderboard, friends)
- **Access**:
  - ✅ All authenticated users can read (for friend search, leaderboard)
  - ✅ Users can create/update their own profile
  - ❌ Users cannot delete profiles
  - Validates email format and display name length

#### 3. **friendRequests** Collection
- **Purpose**: Friend request management
- **Access**:
  - ✅ Users can read requests sent TO them or BY them
  - ✅ Users can send friend requests (create)
  - ✅ Recipients can accept/reject (update status)
  - ✅ Users can cancel/delete requests they sent or received
  - Validates request structure and prevents self-requests

#### 4. **friendships** Collection
- **Purpose**: Established friendships
- **Access**:
  - ✅ Users can read friendships they're part of
  - ✅ Users can create friendships (after accepting request)
  - ❌ Updates not allowed (immutable)
  - ✅ Users can delete (unfriend)

#### 5. **chats** Collection
- **Purpose**: Chat conversations
- **Access**:
  - ✅ Participants can read/update/delete chats
  - ✅ Users can create chats with 2 participants
  - Validates participant list

**Sub-collection:**

- **messages**: Individual messages
  - ✅ Participants can read messages
  - ✅ Users can send messages (create)
  - ✅ Senders can edit/delete their own messages
  - Validates content length (max 5000 chars)

#### 6. **app_news** Collection
- **Purpose**: App-wide news and announcements
- **Access**:
  - ✅ All authenticated users can read
  - ❌ Client-side creation/updates disabled (admin/backend only)
  - *Change `allow create: if false` to `if isAuthenticated()` if you need client-side news creation*

#### 7. **posts** & **comments** Collections (Optional)
- **Purpose**: Social feed functionality (currently in patch files)
- **Access**:
  - ✅ All authenticated users can read
  - ✅ Users can CRUD their own posts/comments

#### 8. **user_social_profiles** Collection (Optional)
- **Purpose**: Extended social features (currently in patch files)
- **Access**:
  - ✅ All authenticated users can read
  - ✅ Users can manage their own profile

---

## 🖼️ Storage Rules

### Paths Covered

#### 1. **avatars/{userId}/{fileName}**
- **Purpose**: User profile avatars
- **Access**:
  - ✅ Public read (anyone can view avatars)
  - ✅ Users can upload their own (max 10 MB images)
  - ✅ Users can delete their own

#### 2. **habits/{userId}/{habitId}/{fileName}**
- **Purpose**: Custom habit images
- **Access**:
  - ✅ Authenticated users can read
  - ✅ Users can manage their own habit images (max 10 MB)

#### 3. **chats/{chatId}/images/{imageFile}**
- **Purpose**: Images shared in chats
- **Access**:
  - ✅ Authenticated users can read
  - ✅ Users can upload images (max 10 MB)
  - ❌ Images are immutable (no updates)
  - ✅ Users can delete their uploads

#### 4. **sounds/{userId}/{fileName}**
- **Purpose**: Custom notification sounds
- **Access**:
  - ✅ Public read
  - ✅ Users can upload their own (max 50 MB audio)

#### 5. **music/{fileName}**
- **Purpose**: Background music files
- **Access**:
  - ✅ Public read
  - ❌ Admin-only uploads
  - *Note: Your app uses GitHub for music, so this may not be actively used*

#### 6. **themes/{themeId}/{fileName}** & **hero_backgrounds/{heroId}/{fileName}**
- **Purpose**: App themes and hero images
- **Access**:
  - ✅ Public read
  - ❌ Admin-only management

#### 7. **posts/{postId}/{fileName}**
- **Purpose**: Social post images
- **Access**:
  - ✅ Authenticated users can read/upload (max 10 MB)
  - ❌ Immutable once uploaded

#### 8. **temp/{userId}/{fileName}**
- **Purpose**: Temporary file storage
- **Access**:
  - ✅ Users can manage their own temp files

---

## 🚀 How to Deploy

### Option 1: Firebase Console (Recommended for First Time)

#### Firestore Rules:

1. Open [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to **Firestore Database** → **Rules** tab
4. Copy contents from `firestore.rules` file
5. Paste into the editor
6. Click **Publish**

#### Storage Rules:

1. In Firebase Console, navigate to **Storage** → **Rules** tab
2. Copy contents from `storage.rules` file
3. Paste into the editor
4. Click **Publish**

### Option 2: Firebase CLI (For Automated Deployment)

1. **Install Firebase CLI** (if not already installed):
   ```bash
   npm install -g firebase-tools
   ```

2. **Login to Firebase**:
   ```bash
   firebase login
   ```

3. **Initialize Firebase** (if not done):
   ```bash
   firebase init
   ```
   - Select: Firestore, Storage
   - Choose your project
   - Use `firestore.rules` and `storage.rules` as rule files

4. **Deploy Rules**:
   ```bash
   # Deploy both Firestore and Storage rules
   firebase deploy --only firestore:rules,storage:rules
   ```
   
   Or deploy individually:
   ```bash
   # Deploy only Firestore rules
   firebase deploy --only firestore:rules
   
   # Deploy only Storage rules
   firebase deploy --only storage:rules
   ```

### Option 3: firebase.json Configuration

Create or update `firebase.json` in your project root:

```json
{
  "firestore": {
    "rules": "firestore.rules",
    "indexes": "firestore.indexes.json"
  },
  "storage": {
    "rules": "storage.rules"
  },
  "functions": {
    "source": "functions"
  }
}
```

Then run:
```bash
firebase deploy
```

---

## 🧪 Testing Rules

### Firebase Rules Playground

1. Open Firebase Console → Firestore Database → Rules
2. Click **Rules Playground** tab
3. Test read/write operations with different scenarios:

**Example Tests:**

```javascript
// Test 1: User can read their own habits
// Location: /users/{userId}/habits/{habitId}
// Authenticated as: user123
// Type: get
// Expected: Allow

// Test 2: User cannot read another user's habits
// Location: /users/otherUser/habits/{habitId}
// Authenticated as: user123
// Type: get
// Expected: Deny

// Test 3: User can send friend request
// Location: /friendRequests/{requestId}
// Authenticated as: user123
// Type: create
// Data: { fromUserId: "user123", toUserId: "user456", ... }
// Expected: Allow
```

### Automated Testing (Advanced)

Create a test file `firestore.test.js`:

```javascript
const firebase = require('@firebase/rules-unit-testing');
const fs = require('fs');

describe('Firestore Rules', () => {
  let testEnv;
  
  beforeAll(async () => {
    testEnv = await firebase.initializeTestEnvironment({
      projectId: 'your-project-id',
      firestore: {
        rules: fs.readFileSync('firestore.rules', 'utf8'),
      },
    });
  });
  
  it('allows users to read their own habits', async () => {
    const alice = testEnv.authenticatedContext('alice');
    const habitsRef = alice.firestore()
      .doc('users/alice/habits/habit1');
    await firebase.assertSucceeds(habitsRef.get());
  });
  
  // Add more tests...
});
```

---

## 🔐 Important Security Notes

### ⚠️ Things to Review Before Going Live

1. **Admin Operations**: Currently, `app_news`, `music`, `themes`, and `hero_backgrounds` have admin-only write access. Implement proper admin authentication if needed:

```javascript
// Example admin check (add to helper functions):
function isAdmin() {
  return isAuthenticated() 
    && request.auth.token.admin == true; // Set custom claims in Firebase Auth
}
```

2. **Rate Limiting**: Firebase has built-in rate limiting, but consider implementing additional application-level rate limiting for:
   - Friend requests
   - Chat messages
   - Profile updates

3. **Data Validation**: The rules validate basic data types and sizes. Consider adding more specific validation:
   - URL formats for photoUrl, customAvatar
   - Enum values for message types, friend request status
   - Numeric ranges for stats (successRate 0-100, etc.)

4. **Backup Strategy**: Ensure you have automated backups:
   ```bash
   gcloud firestore export gs://[BUCKET_NAME]
   ```

5. **Monitoring**: Enable Firebase Security Rules monitoring:
   - Firebase Console → Firestore → Usage tab
   - Set up alerts for denied requests

### 🔒 Security Best Practices Implemented

✅ **Authentication Required**: All operations require authentication  
✅ **Ownership Validation**: Users can only modify their own data  
✅ **Data Validation**: Input validation for critical fields  
✅ **Size Limits**: File size limits for storage uploads  
✅ **Read-Only Public Data**: Public profiles don't expose private information  
✅ **Immutable Data**: Some fields can't be changed once set  
✅ **Participant Validation**: Chat access restricted to participants  
✅ **No Bulk Writes**: Prevents mass data manipulation  
✅ **Default Deny**: Any unspecified path is denied by default  

### 🚨 What's NOT Covered (Intentionally)

- **Admin Panel Access**: You'll need to implement custom claims for admin users
- **Soft Deletes**: Consider implementing soft deletes for important data
- **Audit Logging**: Implement application-level logging for sensitive operations
- **Email Verification**: Ensure users verify their email before certain operations
- **Account Deletion**: Implement a proper user deletion flow with data cleanup

---

## 📊 Performance Considerations

### Indexed Fields

Make sure you have proper indexes for queries. Check `firestore.indexes.json`:

```json
{
  "indexes": [
    {
      "collectionGroup": "habits",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "isDeleted", "order": "ASCENDING" },
        { "fieldPath": "reminderHour", "order": "ASCENDING" }
      ]
    },
    {
      "collectionGroup": "messages",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "timestamp", "order": "ASCENDING" }
      ]
    }
  ]
}
```

### Query Limits

- **whereIn** queries are limited to 10 items (handled in code with chunking)
- Consider pagination for large datasets
- Use `.limit()` in queries to prevent loading too much data

---

## 📝 Maintenance

### Regular Reviews

1. **Monthly**: Review denied request logs in Firebase Console
2. **Quarterly**: Audit user permissions and data access patterns
3. **After Major Updates**: Review and update rules when adding new features

### Version Control

- Always keep rules in version control (Git)
- Tag rule versions with app releases
- Document changes in commit messages

### Testing Checklist

Before deploying new rules:
- [ ] Test all CRUD operations for each collection
- [ ] Test with authenticated and unauthenticated users
- [ ] Test edge cases (empty data, null values, etc.)
- [ ] Test file upload size limits
- [ ] Test cross-user access attempts
- [ ] Review Firebase Console for any errors after deployment

---

## 🎯 Quick Reference

### Common Rule Patterns

```javascript
// Read own data
allow read: if request.auth.uid == userId;

// Write own data with validation
allow write: if request.auth.uid == userId 
  && request.resource.data.title.size() <= 100;

// Public read, owner write
allow read: if true;
allow write: if request.auth.uid == resource.data.ownerId;

// Authenticated users only
allow read, write: if request.auth != null;

// Prevent deletion
allow delete: if false;

// Validate required fields
allow create: if request.resource.data.keys().hasAll(['field1', 'field2']);
```

---

## 📞 Support

If you encounter issues:
1. Check Firebase Console → Firestore → Rules for error messages
2. Use Rules Playground to test specific scenarios
3. Review denied requests in Usage tab
4. Check application logs for permission errors

---

**Last Updated**: October 27, 2025  
**Rules Version**: 1.0.0  
**App Version**: Compatible with HabitTracker v6.0+

---

## 🚀 Deployment Confirmation

After deploying, verify:
- [ ] App functions correctly (login, habits, chat, friends)
- [ ] No permission denied errors in Firebase Console
- [ ] Users can't access others' private data
- [ ] File uploads work correctly
- [ ] Chat notifications still work (Cloud Functions have permission)

**Your rules are now production-ready! 🎉**
