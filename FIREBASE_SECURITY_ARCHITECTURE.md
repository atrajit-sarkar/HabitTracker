# 🔐 Firebase Security Architecture - Visual Guide

## 📊 Security Model Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                    FIREBASE AUTHENTICATION                          │
│                   (All access requires login)                       │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│                       FIRESTORE DATABASE                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────────────┐  ┌────────────────────┐                   │
│  │  PRIVATE DATA      │  │  PUBLIC DATA       │                   │
│  │  (Owner Only)      │  │  (Authenticated)   │                   │
│  ├────────────────────┤  ├────────────────────┤                   │
│  │ • users/           │  │ • userProfiles/    │                   │
│  │   - habits/        │  │ • app_news/        │                   │
│  │   - completions/   │  │                    │                   │
│  │   - read_news/     │  │                    │                   │
│  │                    │  │                    │                   │
│  │ Only YOU can read  │  │ All users can read │                   │
│  │ and write          │  │ Only YOU can write │                   │
│  └────────────────────┘  └────────────────────┘                   │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │           CONDITIONAL ACCESS (Shared Data)                   │  │
│  ├──────────────────────────────────────────────────────────────┤  │
│  │ • friendRequests/ - Sender OR Recipient can access          │  │
│  │ • friendships/    - Both friends can access                 │  │
│  │ • chats/          - Participants only                        │  │
│  │   - messages/     - Participants only                        │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│                      FIREBASE STORAGE                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌────────────────────┐  ┌────────────────────┐                   │
│  │  PUBLIC READ       │  │  AUTHENTICATED     │                   │
│  ├────────────────────┤  ├────────────────────┤                   │
│  │ • avatars/         │  │ • chats/*/images/  │                   │
│  │ • sounds/          │  │ • habits/*/images/ │                   │
│  │ • music/           │  │ • posts/*/images/  │                   │
│  │ • themes/          │  │                    │                   │
│  │ • hero_backgrounds/│  │ Write: Owner only  │                   │
│  │                    │  │                    │                   │
│  │ Anyone can view    │  │ Auth users can see │                   │
│  │ Owner can upload   │  │ Owner can upload   │                   │
│  └────────────────────┘  └────────────────────┘                   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Data Flow & Access Patterns

### 1️⃣ Private User Data Flow

```
User Login
    ↓
┌──────────────────────────────────────────┐
│         Authenticated User               │
│         (userId: abc123)                 │
└──────────────────────────────────────────┘
    ↓
✅ CAN ACCESS:
┌──────────────────────────────────────────┐
│  /users/abc123                           │
│  /users/abc123/habits/*                  │
│  /users/abc123/completions/*             │
│  /users/abc123/read_news/*               │
└──────────────────────────────────────────┘

❌ CANNOT ACCESS:
┌──────────────────────────────────────────┐
│  /users/xyz789                           │
│  /users/xyz789/habits/*                  │
│  /users/xyz789/completions/*             │
└──────────────────────────────────────────┘
```

### 2️⃣ Social Features Flow

```
User (Alice: user123) wants to view leaderboard
    ↓
┌──────────────────────────────────────────┐
│  Query: /userProfiles/                   │
└──────────────────────────────────────────┘
    ↓
✅ CAN READ (Public Profiles):
┌──────────────────────────────────────────┐
│  /userProfiles/user123  (Alice - self)   │
│  /userProfiles/user456  (Bob - friend)   │
│  /userProfiles/user789  (Carol - friend) │
│                                          │
│  Fields visible:                         │
│  - displayName, email                    │
│  - successRate, totalHabits              │
│  - currentStreak, leaderboardScore       │
│  - customAvatar, photoUrl                │
└──────────────────────────────────────────┘

❌ CANNOT READ (Private Data):
┌──────────────────────────────────────────┐
│  /users/user456/habits/*                 │
│  /users/user789/completions/*            │
└──────────────────────────────────────────┘
```

### 3️⃣ Friend Request Flow

```
Step 1: Send Request
Alice (user123) → Bob (user456)
    ↓
✅ CREATE:
┌──────────────────────────────────────────┐
│  /friendRequests/{requestId}             │
│  {                                       │
│    fromUserId: "user123",                │
│    toUserId: "user456",                  │
│    status: "PENDING"                     │
│  }                                       │
└──────────────────────────────────────────┘

Step 2: Bob Accepts
    ↓
✅ UPDATE (Bob only):
┌──────────────────────────────────────────┐
│  /friendRequests/{requestId}             │
│  { status: "ACCEPTED" }                  │
└──────────────────────────────────────────┘
    ↓
✅ CREATE:
┌──────────────────────────────────────────┐
│  /friendships/{friendshipId}             │
│  {                                       │
│    user1Id: "user123",                   │
│    user2Id: "user456"                    │
│  }                                       │
└──────────────────────────────────────────┘
```

### 4️⃣ Chat Flow

```
Alice & Bob are friends
    ↓
✅ CREATE CHAT:
┌──────────────────────────────────────────┐
│  /chats/{chatId}                         │
│  {                                       │
│    participants: ["user123", "user456"]  │
│  }                                       │
└──────────────────────────────────────────┘
    ↓
Alice sends message
    ↓
✅ CREATE MESSAGE:
┌──────────────────────────────────────────┐
│  /chats/{chatId}/messages/{msgId}        │
│  {                                       │
│    senderId: "user123",                  │
│    content: "Hello!",                    │
│    timestamp: 1234567890                 │
│  }                                       │
└──────────────────────────────────────────┘
    ↓
Cloud Function triggers
    ↓
✅ SEND NOTIFICATION to Bob
```

---

## 🎯 Permission Matrix

| Operation | Owner | Friend | Authenticated | Public |
|-----------|-------|--------|---------------|--------|
| **Read own habits** | ✅ | ❌ | ❌ | ❌ |
| **Write own habits** | ✅ | ❌ | ❌ | ❌ |
| **Read userProfiles** | ✅ | ✅ | ✅ | ❌ |
| **Write own profile** | ✅ | ❌ | ❌ | ❌ |
| **Send friend request** | ✅ | - | ✅ | ❌ |
| **Accept friend request** | ✅ | - | ❌ | ❌ |
| **Read friend list** | ✅ | ❌ | ❌ | ❌ |
| **Read chat (participant)** | ✅ | ✅ | ❌ | ❌ |
| **Send message (participant)** | ✅ | ✅ | ❌ | ❌ |
| **Read app news** | ✅ | ✅ | ✅ | ❌ |
| **Upload avatar** | ✅ | ❌ | ❌ | ❌ |
| **View avatars** | ✅ | ✅ | ✅ | ✅ |

---

## 🔍 Validation Rules

### Input Validation Flow

```
Client sends data
    ↓
┌──────────────────────────────────────────┐
│     Firebase Security Rules              │
│     ┌──────────────────────────┐         │
│     │  1. Authentication Check │         │
│     │     ↓ Is user signed in? │         │
│     └──────────────────────────┘         │
│              ↓                            │
│     ┌──────────────────────────┐         │
│     │  2. Ownership Check      │         │
│     │     ↓ Does user own data?│         │
│     └──────────────────────────┘         │
│              ↓                            │
│     ┌──────────────────────────┐         │
│     │  3. Data Validation      │         │
│     │     ↓ Valid format?      │         │
│     │     ↓ Required fields?   │         │
│     │     ↓ Valid ranges?      │         │
│     └──────────────────────────┘         │
│              ↓                            │
│     ┌──────────────────────────┐         │
│     │  4. Business Logic       │         │
│     │     ↓ Valid state?       │         │
│     │     ↓ Allowed transition?│         │
│     └──────────────────────────┘         │
└──────────────────────────────────────────┘
    ↓
✅ Allow   OR   ❌ Deny
```

### Example: Creating a Habit

```
Input:
{
  title: "Exercise Daily",
  reminderHour: 7,
  reminderMinute: 30,
  frequency: "DAILY"
}
    ↓
Validation Checks:
✅ User authenticated?           → YES
✅ Correct user collection?      → YES (users/{myId}/habits)
✅ Has required fields?          → YES (title, reminderHour, etc.)
✅ Title length 1-100?           → YES (15 chars)
✅ Reminder hour 0-23?           → YES (7)
✅ Reminder minute 0-59?         → YES (30)
    ↓
Result: ✅ ALLOWED - Habit created
```

### Example: Accessing Other User's Data

```
Request:
GET /users/xyz789/habits/habit1
From: user abc123
    ↓
Validation Checks:
✅ User authenticated?           → YES
❌ User owns this data?          → NO (abc123 ≠ xyz789)
    ↓
Result: ❌ DENIED - Permission denied
```

---

## 🛡️ Security Layers

```
┌─────────────────────────────────────────────────────────┐
│  Layer 1: Network Security                              │
│  • HTTPS only                                           │
│  • Firebase SDK encryption                              │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│  Layer 2: Authentication                                │
│  • Firebase Auth (Google, Email, etc.)                  │
│  • Session tokens                                       │
│  • Token validation                                     │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│  Layer 3: Authorization (Security Rules)                │
│  • User ownership checks                                │
│  • Participant validation                               │
│  • Read/write permissions                               │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│  Layer 4: Data Validation                               │
│  • Required fields                                      │
│  • Type validation                                      │
│  • Range checks                                         │
│  • Format validation                                    │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│  Layer 5: Business Logic                                │
│  • State transitions                                    │
│  • Relationship integrity                               │
│  • Immutability rules                                   │
└─────────────────────────────────────────────────────────┘
```

---

## 📈 Scale & Performance

### Query Patterns

```
Efficient ✅
┌──────────────────────────────────────┐
│  userProfiles                        │
│    .where('userId', 'in', friendIds) │
│    .limit(10)                        │
└──────────────────────────────────────┘

Inefficient ❌
┌──────────────────────────────────────┐
│  userProfiles                        │
│    .get() // Fetches ALL profiles    │
└──────────────────────────────────────┘
```

### Caching Strategy

```
┌──────────────────────────────────────┐
│  Client App                          │
│  ├─ Local Cache (SQLite)             │
│  ├─ Memory Cache                     │
│  └─ Real-time Listeners              │
└──────────────────────────────────────┘
         ↕
    (Sync when needed)
         ↕
┌──────────────────────────────────────┐
│  Firebase (Cloud)                    │
│  ├─ Firestore Database               │
│  └─ Storage                          │
└──────────────────────────────────────┘
```

---

## 🔐 Default Deny Rule

```
Everything not explicitly allowed is DENIED

┌─────────────────────────────────────────┐
│  Defined Rules (Allowed)                │
│  • users/{userId} - owner access        │
│  • userProfiles - authenticated read    │
│  • chats - participant access           │
│  • etc...                               │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  Undefined Paths (Denied)               │
│  • /unknown_collection/*                │
│  • /future_feature/*                    │
│  • /{anything_not_defined}/*            │
└─────────────────────────────────────────┘
         ↓
    ❌ DENIED
```

---

## 📊 Monitoring Dashboard View

```
Firebase Console → Firestore → Usage

┌────────────────────────────────────────────────┐
│  Today's Activity                              │
├────────────────────────────────────────────────┤
│  Reads:     125,430  ✅                        │
│  Writes:     42,160  ✅                        │
│  Deletes:       234  ✅                        │
│  Denied:         12  ⚠️  ← Monitor this!      │
└────────────────────────────────────────────────┘

┌────────────────────────────────────────────────┐
│  Denied Requests (Last 24h)                    │
├────────────────────────────────────────────────┤
│  • /users/xyz/habits → permission_denied       │
│  • /chats/abc123 → not_participant             │
│  • /admin_panel → path_not_allowed             │
└────────────────────────────────────────────────┘
```

---

## 🎓 Key Takeaways

### ✅ What's Protected
- Private user data (habits, completions, settings)
- Personal information (email, rewards, etc.)
- Direct message content (chat privacy)
- File uploads (ownership validation)

### ✅ What's Shared Safely
- Public profiles (for leaderboard/friends)
- Friend relationships (both parties)
- Chat conversations (participants only)
- App news (all users)

### ✅ What's Validated
- User authentication (required for all)
- Data ownership (users own their data)
- Data types and formats (strings, numbers, etc.)
- File types and sizes (images, audio)
- Required fields (no incomplete data)

---

**For complete details, see:**
- `firestore.rules` - The actual rules
- `storage.rules` - Storage rules
- `FIREBASE_RULES_GUIDE.md` - Detailed guide
- `FIREBASE_RULES_QUICK_REF.md` - Quick reference

---

**🎉 Your data is secure!**
