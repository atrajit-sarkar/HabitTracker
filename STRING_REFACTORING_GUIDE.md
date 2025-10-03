# String Refactoring Guide

## Summary
This guide documents the refactoring of all hardcoded strings in the HabitTracker app to use string resources from `strings.xml` for better maintainability, localization support, and code organization.

## Changes Made

### 1. Updated strings.xml
- Added **180+ new string resources** covering all major UI components
- Organized strings by feature (Dialogs, Screens, Social, Chat, Updates, etc.)
- Added proper formatting for plurals and dynamic content using `%1$s`, `%1$d` placeholders

### 2. Refactored Files (Completed)

#### ✅ DeleteHabitConfirmationDialog.kt
**Before:**
```kotlin
Text(text = "Delete Habit?")
Text(text = "Are you sure you want to delete \"$habitTitle\"?...")
Text("Delete")
Text("Cancel")
```

**After:**
```kotlin
Text(text = stringResource(R.string.delete_habit_title))
Text(text = stringResource(R.string.delete_habit_message, habitTitle))
Text(stringResource(R.string.delete))
Text(stringResource(R.string.cancel))
```

#### ✅ NameDialogs.kt
- Added imports: `stringResource`, `LocalContext`, `R`
- Refactored both `EditNameDialog` and `SetNameDialog`
- All error messages now use context.getString() for proper string resource access

**Key Pattern:**
```kotlin
val context = LocalContext.current
// In error handling:
error = context.getString(R.string.name_cannot_be_empty)
```

#### ✅ FirstLaunchNotificationDialog.kt
- All UI text now uses `stringResource(R.string.xxx)`
- Maintains emoji and formatting

#### ✅ HomeScreen.kt (Partial)
- Button text refactored ("Done", "Details", "See Details")

---

## Remaining Files to Refactor

### HIGH PRIORITY (User-facing dialogs and screens)

#### 1. TrashScreen.kt
**Strings to replace:**
```kotlin
"Trash" → R.string.trash
"${deletedHabits.size} item${if (deletedHabits.size == 1) "" else "s"}" 
  → stringResource(R.string.trash_items_count, count, if (count == 1) "" else "s")
"Items will be automatically deleted after 30 days" → R.string.items_auto_delete_after_30_days
"Trash is Empty" → R.string.trash_is_empty
"Deleted habits will appear here" → R.string.deleted_habits_appear_here
"You can restore them within 30 days" → R.string.restore_within_30_days
"Deleted on $it" → stringResource(R.string.deleted_on, it)
"Restore" → R.string.restore
"Delete" → R.string.delete
"Empty Trash?" → R.string.empty_trash_title
"This will permanently delete all habits..." → R.string.empty_trash_message
"Empty Trash" → R.string.empty_trash
"Delete Permanently?" → R.string.delete_permanently_title
"This habit will be permanently deleted..." → R.string.delete_permanently_message
"Delete Forever" → R.string.delete_forever
"Cancel" → R.string.cancel
"IMG" → R.string.img
```

#### 2. StatisticsScreen.kt
**Strings to replace:**
```kotlin
"Analyzing your habits..." → R.string.analyzing_habits
"Quick Statistics" → R.string.quick_statistics
"Total" → R.string.total
"Habits" → R.string.habits
"Streak" → R.string.streak
"Days" → R.string.days
"Completed" → R.string.completed
"Times" → R.string.times
"Last 7 Days" → R.string.last_7_days
"30-Day Trend" → R.string.30_day_trend
"Habit Frequency" → R.string.habit_frequency
"Top Performers" → R.string.top_performers
"Completion Comparison" → R.string.completion_comparison
"Success by Weekday" → R.string.success_by_weekday
"Detailed Breakdown" → R.string.detailed_breakdown
"Your Progress" → R.string.your_progress
"No data available" → R.string.no_data_available
"No trend data available" → R.string.no_trend_data_available
"No habits to compare" → R.string.no_habits_to_compare
"${comparison.completionCount} times" → stringResource(R.string.times_completed, count)
"No weekday data available" → R.string.no_weekday_data_available
"Performance Score" → R.string.performance_score
"/ 100" → R.string.out_of_100
"$count habit${if (count != 1) "s" else ""}" → stringResource(R.string.habit_count, count, ...)
"No habits to display" → R.string.no_habits_to_display
"${habit.completionCount} completions" → stringResource(R.string.completions_count, count)
```

#### 3. Social Features

**SearchUsersScreen.kt:**
```kotlin
"Find friends by their email address" → R.string.find_friends_by_email
"Email Address" → R.string.email_address
"Tips" → R.string.tips
(search tips text) → R.string.search_tips
```

**LeaderboardScreen.kt:**
```kotlin
"Loading leaderboard..." → R.string.loading_leaderboard
"No Leaderboard Yet" → R.string.no_leaderboard_yet
"Add friends to see rankings..." → R.string.add_friends_to_see_rankings
(All scoring-related strings) → Use corresponding string resources
"Got It!" → R.string.got_it
```

**FriendsListScreen.kt:**
```kotlin
"Friends" → R.string.friends
"Requests" → R.string.requests
"No Friends Yet" → R.string.no_friends_yet
"Search for users to add as friends" → R.string.search_for_users
"Remove Friend?" → R.string.remove_friend_title
"Are you sure you want to remove..." → stringResource(R.string.remove_friend_message, name)
"Remove" → R.string.remove
"Cancel" → R.string.cancel
"No Pending Requests" → R.string.no_pending_requests
"Friend requests will appear here" → R.string.friend_requests_appear_here
"Reject" → R.string.reject
"Accept" → R.string.accept
```

**FriendProfileScreen.kt:**
```kotlin
"Profile not found" → R.string.profile_not_found
"Message" → R.string.message
"Statistics" → R.string.statistics
"This is a friend's profile..." → R.string.friend_profile_note
```

#### 4. Chat Features

**ChatScreen.kt:**
```kotlin
"Start the conversation!" → R.string.start_conversation
"Send a message or sticker to break the ice 🎉" → R.string.send_message_to_break_ice
"Type a message..." → R.string.type_a_message
```

**ChatListScreen.kt:**
```kotlin
"No Messages Yet" → R.string.no_messages_yet
"Start a conversation with your friends..." → R.string.start_conversation_with_friends
"Photo" → R.string.photo
```

#### 5. Update Dialog

**UpdateDialog.kt:**
```kotlin
"You're running the latest version" → R.string.youre_running_latest_version
"Current Version" → R.string.current_version
"OK" → R.string.ok
"Checking for updates..." → R.string.checking_for_updates
"Please wait a moment" → R.string.please_wait_a_moment
"Current" → R.string.current
"New" → R.string.new_update
"Downloading..." → R.string.downloading
"$progress%" → stringResource(R.string.download_progress, progress)
"Please don't close the app" → R.string.please_dont_close_app
"What's New" → R.string.whats_new
(default update notes) → R.string.default_update_notes
"Skip" → R.string.skip
"Later" → R.string.later
```

**UpdateResultDialog.kt:**
```kotlin
(Similar patterns as UpdateDialog.kt)
```

#### 6. Notification Setup Guide

**NotificationSetupGuideScreen.kt:**
```kotlin
"Notification Setup Guide" → R.string.notification_setup_guide
"Android's battery optimization can prevent..." → R.string.battery_optimization_explanation
"This is the most important step..." → R.string.most_important_step
"Already exempt from battery optimization ✓" → R.string.already_exempt_battery
"Allow Battery Optimization" → R.string.allow_battery_optimization
"Notifications are enabled ✓" → R.string.notifications_enabled
// ... (continue with all strings)
```

#### 7. Profile Screen

**ProfileScreen.kt:**
```kotlin
"Profile" → R.string.profile_title
"Your Statistics" → R.string.your_statistics
"Detailed Analytics" → R.string.detailed_analytics
"Charts, trends & comparisons" → R.string.charts_trends_comparisons
"Social & Friends" → R.string.social_and_friends
"Leaderboard" → R.string.leaderboard
"Compete with friends" → R.string.compete_with_friends
"Notification Setup Guide" → R.string.notification_setup_guide_title
"Ensure reliable reminders" → R.string.ensure_reliable_reminders
"Check for Updates" → R.string.check_for_updates
"Get the latest features" → R.string.get_latest_features
"Habit Tracker" → R.string.app_name
"Build better habits, one day at a time" → R.string.habit_tracker_tagline
"Reset" → R.string.reset
"Are you sure you want to sign out?..." → R.string.sign_out_confirmation
"Sign Out" → R.string.sign_out
"Close" → R.string.close
```

#### 8. Auth Screen

**AuthScreen.kt:**
```kotlin
"Email" → R.string.email
"Password" → R.string.password
"Confirm Password" → R.string.confirm_password
"OR" → R.string.or
"G" (Google icon) → keep as is (it's a visual element)
"Continue with Google" → R.string.continue_with_google
"Back to Sign In" → R.string.back_to_sign_in
"Forgot Password?" → R.string.forgot_password
```

#### 9. Habit Details Screen

**HabitDetailsScreen.kt:**
```kotlin
"Streak Rules" → R.string.streak_rules
(streak rules text) → R.string.streak_rules_text
"IMG" → R.string.img
```

#### 10. HomeScreen.kt (Remaining)

**Remaining strings:**
```kotlin
"Menu" → R.string.menu
"Manage your habits" → R.string.manage_your_habits
"ACTIONS" → R.string.actions
"Profile" → R.string.profile
"Account settings" → R.string.account_settings
"Trash" → R.string.trash
"View deleted habits" → R.string.view_deleted_habits
"Loading sounds..." → R.string.loading_sounds
```

#### 11. HabitTrackerNavigation.kt

```kotlin
"Error loading habit" → R.string.error_loading_habit
"Retry" → R.string.retry
"Habit not found" → R.string.habit_not_found
```

#### 12. AddHabitScreen.kt

```kotlin
"Loading sounds..." → R.string.loading_sounds
```

---

## Refactoring Pattern

### Step-by-Step Process:

1. **Add import statements:**
```kotlin
import androidx.compose.ui.res.stringResource
import com.example.habittracker.R
import androidx.compose.ui.platform.LocalContext // if needed for error messages
```

2. **Simple text replacement:**
```kotlin
// Before
Text("Some Text")

// After
Text(stringResource(R.string.some_text))
```

3. **Text with parameters:**
```kotlin
// Before
Text("Deleted on $date")

// After
Text(stringResource(R.string.deleted_on, date))
```

4. **Plurals (manual handling):**
```kotlin
// Before
Text("${count} item${if (count == 1) "" else "s"}")

// After
val itemText = if (count == 1) "" else "s"
Text(stringResource(R.string.trash_items_count, count, itemText))
```

5. **Error messages needing context:**
```kotlin
val context = LocalContext.current
// ...
error = context.getString(R.string.error_message)
```

6. **Labels in OutlinedTextField:**
```kotlin
// Before
label = { Text("Email Address") }

// After
label = { Text(stringResource(R.string.email_address)) }
```

---

## Testing Checklist

After refactoring each file:

- [ ] No compilation errors
- [ ] All UI text displays correctly
- [ ] Dynamic text (with parameters) shows proper values
- [ ] Error messages display correctly
- [ ] Dialogs show proper strings
- [ ] No hardcoded strings remain in the file

---

## Benefits of This Refactoring

1. **Localization Ready**: Easy to add multiple languages
2. **Maintainability**: All text in one place
3. **Consistency**: Reuse common strings (Cancel, Delete, etc.)
4. **Type Safety**: Compiler checks string resource existence
5. **Resource Management**: Android handles string lifecycle
6. **Testing**: Easier to test different languages

---

## Tools to Help

### Find Remaining Hardcoded Strings:
```bash
# PowerShell command to find hardcoded strings in Kotlin files
Get-ChildItem -Path "app\src\main\java" -Filter "*.kt" -Recurse | 
  Select-String -Pattern 'Text\([^)]*"[^"]+"|text\s*=\s*"[^"]+"' | 
  Select-Object -First 50
```

### Verify String Resources:
Check that all strings in strings.xml are being used, and vice versa.

---

## Next Steps

1. **Complete HIGH PRIORITY files** listed above
2. **Test each screen** after refactoring
3. **Check for missed strings** using grep/find
4. **Verify no broken references** (Build → Make Project)
5. **Update documentation** if needed

---

## Common Pitfalls to Avoid

❌ **Don't:**
- Forget to import `R` class
- Use `getString()` directly in Composables without context
- Mix string resources and hardcoded strings
- Forget to handle string parameters properly

✅ **Do:**
- Use `stringResource()` in Composables
- Use `LocalContext.current.getString()` for non-Composable contexts
- Keep string keys descriptive and organized
- Test dynamic strings with various values
- Use proper escaping for special characters (`'` → `\'`, `%` → `%%`)

---

## String Resource Naming Convention

Follow these patterns for consistency:

- **Titles:** `screen_name_title` (e.g., `delete_habit_title`)
- **Messages:** `action_message` (e.g., `delete_habit_message`)
- **Buttons:** `action_verb` (e.g., `delete`, `cancel`, `save`)
- **Labels:** `field_name` (e.g., `email_address`, `display_name`)
- **Empty States:** `empty_state_context` (e.g., `trash_is_empty`)
- **Errors:** `error_description` (e.g., `name_cannot_be_empty`)

---

## Complete Refactoring Estimate

- ✅ **Completed:** ~5 files (DeleteHabitConfirmationDialog, NameDialogs, FirstLaunchNotificationDialog, HomeScreen partial)
- 🔄 **Remaining:** ~15-20 major files
- ⏱️ **Estimated Time:** 2-4 hours for complete refactoring

Good luck with the refactoring! 🚀
