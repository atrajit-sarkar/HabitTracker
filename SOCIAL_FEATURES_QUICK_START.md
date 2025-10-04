# Quick Start Guide - Social Features

## 🚀 Getting Started with Social Features

### Step 1: Deploy Firestore Security Rules

Add these rules to your Firebase Console (Firestore Rules section):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Existing rules...
    
    // Friend Requests
    match /friendRequests/{requestId} {
      allow read: if request.auth != null && 
                  (request.auth.uid == resource.data.toUserId || 
                   request.auth.uid == resource.data.fromUserId);
      allow create: if request.auth != null && 
                    request.auth.uid == request.resource.data.fromUserId;
      allow update: if request.auth != null && 
                    request.auth.uid == resource.data.toUserId;
      allow delete: if request.auth != null && 
                    (request.auth.uid == resource.data.toUserId || 
                     request.auth.uid == resource.data.fromUserId);
    }
    
    // Friendships
    match /friendships/{friendshipId} {
      allow read: if request.auth != null && 
                  (request.auth.uid == resource.data.user1Id || 
                   request.auth.uid == resource.data.user2Id);
      allow create: if request.auth != null;
      allow delete: if request.auth != null && 
                    (request.auth.uid == resource.data.user1Id || 
                     request.auth.uid == resource.data.user2Id);
    }
    
    // User Public Profiles
    match /userProfiles/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### Step 2: Test the Features

1. **Install the app** on 2+ devices or use different accounts
2. **Create accounts** with different email addresses
3. **Complete some habits** to populate stats

### Step 3: Try Each Feature

#### 🔍 Search Users
1. Go to Profile → **Search** button
2. Enter a friend's email address
3. Click **Search**
4. Review their profile
5. Click **Send Friend Request**

#### 👥 Manage Friends
1. Go to Profile → **Friends** button
2. **Requests Tab**: Accept or reject pending requests
3. **Friends Tab**: View your friends list
4. Click on a friend to view their profile

#### 🏆 View Leaderboard
1. Go to Profile → **Leaderboard** card
2. See rankings based on success rate
3. Complete habits to improve your rank
4. Watch for rank improvement celebrations!

### Step 4: Update User Stats (Integration)

The stats need to be updated when habits are completed. Add this to your habit completion logic:

```kotlin
// In HabitViewModel or wherever you mark habits as completed
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val authRepository: AuthRepository,
    private val friendRepository: FriendRepository
) : ViewModel() {

    fun markHabitCompleted(habitId: Long) {
        viewModelScope.launch {
            // Mark habit as completed
            habitRepository.markHabitCompleted(habitId)
            
            // Update user stats for leaderboard
            updateUserStats()
        }
    }
    
    private suspend fun updateUserStats() {
        val user = authRepository.getCurrentUser() ?: return
        val habits = habitRepository.getAllHabits().first()
        
        val activeHabits = habits.filter { !it.isDeleted }
        val today = LocalDate.now()
        val completedToday = activeHabits.count { it.lastCompletedDate == today }
        val successRate = if (activeHabits.isNotEmpty()) {
            (completedToday * 100) / activeHabits.size
        } else {
            0
        }
        
        // Calculate streak (simplified)
        val currentStreak = activeHabits.maxOfOrNull { habit ->
            if (habit.lastCompletedDate == today) 1 else 0
        } ?: 0
        
        friendRepository.updateUserPublicProfile(
            userId = user.uid,
            email = user.email ?: "",
            displayName = user.effectiveDisplayName,
            customAvatar = user.customAvatar ?: "😊",
            successRate = successRate,
            totalHabits = activeHabits.size,
            totalCompletions = completedToday,
            currentStreak = currentStreak
        )
    }
}
```

### Step 5: Add Success Sound (Optional)

1. Find a short success sound (MP3 or OGG, < 2 seconds)
2. Rename it to `success_sound.mp3`
3. Place it in `app/src/main/res/raw/`
4. Rebuild the app

The sound will play when your rank improves on the leaderboard!

---

## 🎮 User Guide

### How to Use Social Features

#### Finding Friends
1. Ask your friend for their email address used in the app
2. Go to **Profile → Search**
3. Enter their exact email
4. Send a friend request

#### Managing Friend Requests
1. Go to **Profile → Friends**
2. Swipe or tap **Requests** tab
3. You'll see pending requests
4. Tap **Accept** or **Reject**

#### Viewing Friends
1. Go to **Profile → Friends**
2. **Friends** tab shows all your friends
3. Tap any friend to view their profile
4. See their stats and achievements

#### Competing on Leaderboard
1. Go to **Profile → Leaderboard**
2. See your rank among friends
3. Complete habits to increase your success rate
4. Watch your rank climb!
5. Get celebrated when you overtake friends! 🎉

---

## 🐛 Troubleshooting

### "No user found with this email"
- Check the email is correct
- Make sure the user has created an account
- The user must have opened the app at least once

### Friend requests not appearing
- Check Firestore rules are deployed
- Verify internet connection
- Try refreshing by going back and returning to the screen

### Leaderboard not updating
- Stats update when habits are completed
- Tap the refresh button (↻) in top-right
- Make sure you've integrated the stats update code

### No sound on rank improvement
- This is optional and requires a sound file
- See `SUCCESS_SOUND_INFO.md` for details
- The feature works fine without sound

---

## 📊 Understanding Stats

### Success Rate
- Percentage of habits completed today
- Formula: `(Completed Today / Total Habits) × 100`
- Updates in real-time as you complete habits

### Current Streak
- Consecutive days a habit has been completed
- Shows the longest streak among all habits
- Resets if a day is missed

### Leaderboard Ranking
- Sorted by success rate (highest first)
- Updated when anyone completes habits
- Tap refresh to see latest rankings

---

## 💡 Tips for Best Experience

1. **Add 2-3 friends** to start competing
2. **Complete habits daily** to maintain your rank
3. **Check leaderboard regularly** to stay motivated
4. **View friend profiles** to see what works for them
5. **Celebrate improvements** - the app will too!

---

## 🎯 What's Next?

The social features are fully functional! Users can now:
- ✅ Connect with friends
- ✅ Share progress
- ✅ Compete in a friendly way
- ✅ Stay motivated together

Enjoy building better habits with your friends! 🚀
