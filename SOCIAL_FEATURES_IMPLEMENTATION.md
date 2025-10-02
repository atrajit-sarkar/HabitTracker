# Social Features Implementation - Feature #3

This document describes the implementation of the social features for the Habit Tracker app.

## Overview

The social features allow users to:
1. Search for other users by email address
2. Send and receive friend requests
3. View friends list with pending requests
4. View friend profiles (read-only)
5. Compete on a leaderboard based on habit success rate
6. View friend dashboards (stats only, no edit capabilities)

## Architecture

### Data Layer

#### Models (`data/firestore/FirestoreFriendModels.kt`)
- **FriendRequest**: Stores friend request information
  - From/To user details (ID, email, name, avatar)
  - Status (PENDING, ACCEPTED, REJECTED)
  - Timestamps
  
- **Friendship**: Stores active friendships
  - Two user IDs linked together
  - Creation timestamp
  
- **UserPublicProfile**: Public profile information for leaderboard
  - User basic info (email, display name, avatar)
  - Stats (success rate, total habits, completions, streak)
  - Updated regularly

#### Repository (`data/firestore/FriendRepository.kt`)
Handles all Firestore operations:
- `searchUserByEmail()`: Find users by email
- `sendFriendRequest()`: Send friend request with validation
- `acceptFriendRequest()`: Accept request and create friendship
- `rejectFriendRequest()`: Reject pending request
- `getFriends()`: Get real-time friends list (Flow)
- `getPendingFriendRequests()`: Get real-time pending requests (Flow)
- `getLeaderboard()`: Get sorted leaderboard
- `updateUserPublicProfile()`: Update user stats for leaderboard
- `removeFriend()`: Remove friendship

### Presentation Layer

#### ViewModel (`ui/social/SocialViewModel.kt`)
Manages social features state:
- Current user management
- Friends list (real-time updates)
- Pending requests (real-time updates)
- Search functionality
- Leaderboard data
- Action handling (send/accept/reject requests)

#### UI Screens

1. **SearchUsersScreen** (`ui/social/SearchUsersScreen.kt`)
   - Search users by email
   - Display search results with user stats
   - Send friend requests
   - Input validation and error handling

2. **FriendsListScreen** (`ui/social/FriendsListScreen.kt`)
   - Two-page swipe navigation:
     - **Friends page**: List of current friends
     - **Requests page**: Pending friend requests
   - Custom tab indicator with count badges
   - Friend count formatting (99+, 1k+ for large numbers)
   - Accept/Reject actions for requests
   - Navigation to friend profiles

3. **LeaderboardScreen** (`ui/social/LeaderboardScreen.kt`)
   - Ranked list based on success rate
   - Top 3 podium display with animations
   - Current user highlighting
   - Rank improvement detection
   - Animated entry transitions
   - Sound feedback for rank improvements (optional)
   - Real-time updates

4. **FriendProfileScreen** (`ui/social/FriendProfileScreen.kt`)
   - View-only friend profile
   - Display stats and achievements
   - No edit capabilities (user-personalized features hidden)
   - Info banner explaining read-only mode

### Navigation Updates

Updated `HabitTrackerNavigation.kt` to include new routes:
- `/searchUsers`: Search for users
- `/friendsList`: View friends and pending requests
- `/leaderboard`: View leaderboard
- `/friendProfile/{friendId}`: View specific friend's profile

### Profile Screen Integration

Updated `ProfileScreen.kt` to add social features section:
- Search button
- Friends list button
- Leaderboard card (prominent display)
- New navigation callbacks

## Firestore Collections

### `friendRequests`
```
{
  fromUserId: string
  fromUserEmail: string
  fromUserName: string
  fromUserAvatar: string
  toUserId: string
  toUserEmail: string
  status: "PENDING" | "ACCEPTED" | "REJECTED"
  createdAt: timestamp
  updatedAt: timestamp
}
```

### `friendships`
```
{
  user1Id: string
  user2Id: string
  createdAt: timestamp
}
```

### `userProfiles`
```
{
  userId: string (document ID)
  email: string
  displayName: string
  customAvatar: string
  successRate: number (0-100)
  totalHabits: number
  totalCompletions: number
  currentStreak: number
  updatedAt: timestamp
}
```

## Key Features

### Real-time Updates
- Friends list updates automatically when requests are accepted
- Pending requests appear instantly
- Leaderboard can be manually refreshed

### Count Formatting
Smart count display for large numbers:
- 0-99: Show exact number
- 100+: Show "99+"
- 1000+: Show "1k+"

### Animations
- Fade in/out transitions
- Slide animations for entries
- Scale animations for top 3 podium
- Shimmer effect for rank improvement banner
- Spring physics for smooth animations

### Validation
- Cannot add yourself as friend
- Cannot send duplicate requests
- Cannot send request if already friends
- Detects reverse pending requests

### Error Handling
- Graceful fallbacks for missing data
- User-friendly error messages
- Try-catch blocks for Firestore operations
- Optional sound playback (fails silently)

## User Experience

### Friend Request Flow
1. User searches for friend by email
2. System displays user profile with stats
3. User sends friend request
4. Recipient sees request in "Requests" tab
5. Recipient accepts/rejects
6. If accepted, both users appear in each other's friends list

### Leaderboard Updates
1. User stats updated when habits are completed
2. Success rate calculated from completion percentage
3. Leaderboard sorted by success rate (highest first)
4. Rank improvements trigger celebration banner
5. Optional sound feedback (if sound file exists)

## Privacy & Security

- Only email search (users must know exact email)
- Public profiles show limited info (stats only)
- No access to friend's personal data
- Cannot edit friend's habits or settings
- Friend removal is unilateral

## Future Enhancements

Possible additions:
- Activity feed showing friends' achievements
- Private/public profile toggle
- Friend suggestions based on mutual friends
- Chat or messaging
- Shared challenges or goals
- Push notifications for friend requests
- Friend streak comparisons
- Weekly/monthly leaderboard snapshots

## Testing Checklist

- [ ] Search users by email
- [ ] Send friend request
- [ ] Receive friend request notification
- [ ] Accept friend request
- [ ] Reject friend request
- [ ] View friends list
- [ ] Navigate to friend profile
- [ ] View leaderboard
- [ ] Check rank updates
- [ ] Remove friend
- [ ] Handle network errors gracefully
- [ ] Test with 0, 1, 10, 100+ friends
- [ ] Verify real-time updates
- [ ] Test count formatting (99+, 1k+)

## Known Limitations

1. Firestore `whereIn` queries limited to 10 items (batched)
2. Sound playback requires sound file in res/raw/
3. No offline support for social features
4. No friend request notifications (would need FCM)
5. Success rate calculation depends on user updating profile

## Dependencies

All dependencies already included in the project:
- Firebase Firestore
- Jetpack Compose
- Hilt for DI
- Material3 for UI components
- Compose Navigation

## Notes

- Success sound is optional - see `SUCCESS_SOUND_INFO.md`
- Profile updates should be triggered when habits are completed
- Consider adding a background service to update profiles periodically
- Leaderboard refresh is manual to avoid excessive Firestore reads
