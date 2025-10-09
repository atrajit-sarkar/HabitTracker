# Avatar Selector Cleanup - HomeScreen Integration

## Overview
Removed the old `AvatarSelector` composable from `HomeScreen.kt` and integrated the new `HabitAvatarPickerDialog` that supports custom images uploaded to GitHub.

## Changes Made

### 1. Updated AddHabitSheet in HomeScreen.kt

**Before:**
- Used the old `AvatarSelector` composable with inline emoji grid and color selection
- Limited to emojis and background colors only
- No support for custom uploaded images

**After:**
- Replaced with `HabitAvatarPickerDialog` integration (consistent with AddHabitScreen.kt)
- Shows current avatar with an edit indicator
- Clicking opens a dialog with tabs for Emojis and Images
- Supports custom image uploads to GitHub

**Code Structure:**
```kotlin
// Avatar Selection with custom image support
var showAvatarPicker by remember { mutableStateOf(false) }

Card(...) {
    Column(...) {
        Text("Avatar")
        
        // Current avatar display (clickable to open picker)
        Box(clickable) {
            AvatarDisplay(avatar = state.avatar, size = 64.dp)
            // Edit indicator badge
            Surface(CircleShape) { Icon(Edit) }
        }
        
        TextButton("Change Avatar")
    }
}

// Avatar picker dialog
if (showAvatarPicker) {
    HabitAvatarPickerDialog(
        currentAvatar = state.avatar,
        onAvatarSelected = { onAvatarChange(it) },
        onDismiss = { showAvatarPicker = false }
    )
}
```

### 2. Removed Old AvatarSelector Composable

**Removed:**
```kotlin
@Composable
private fun AvatarSelector(
    selectedAvatar: HabitAvatar,
    onAvatarChange: (HabitAvatar) -> Unit
) {
    // Old implementation with LazyVerticalGrid for emojis
    // and LazyRow for colors inline
}
```

**Reason for Removal:**
- Duplicated functionality now in `HabitAvatarPickerDialog`
- Didn't support custom images
- Less flexible UI (no dialog/modal presentation)
- Not reusable across different screens

### 3. Kept Reusable Helper Composables

The following composables were kept as they're used by both HomeScreen.kt and AddHabitScreen.kt:

- `AvatarDisplay` - Displays avatar with emoji/icon/image
- `EmojiItem` - Individual emoji selection button
- `ColorItem` - Individual color selection button

### 4. Fixed Import Issue

**Fixed:**
```kotlin
// Before (incorrect package)
import it.atraj.habittracker.avatar.AvatarPickerViewModel

// After (correct package)
import it.atraj.habittracker.avatar.ui.AvatarPickerViewModel
```

## Benefits

### 1. **Code Consistency**
- Both `HomeScreen.kt` and `AddHabitScreen.kt` now use the same avatar picker
- Consistent user experience across the app
- Easier to maintain - changes only need to be made in one place

### 2. **Enhanced Functionality**
- Support for custom image uploads
- Tabbed interface (Emojis vs Images)
- Better visual feedback with edit indicator
- Modal dialog presentation

### 3. **Better Architecture**
- Separation of concerns (dialog is a separate composable)
- Uses ViewModel for avatar management
- Supports GitHub avatar storage via `AvatarManager`

### 4. **Cleaner Code**
- Removed ~70 lines of duplicate code
- More maintainable codebase
- Better composable reusability

## User Experience

### Old Flow:
1. Open habit creation sheet
2. Scroll down to avatar section
3. Select emoji and color inline (takes up screen space)
4. Limited to predefined emojis only

### New Flow:
1. Open habit creation sheet
2. See current avatar with "Change Avatar" button
3. Click to open dedicated avatar picker dialog
4. Choose between Emojis tab or Images tab
5. Upload custom images from gallery
6. Select and auto-close dialog

## Technical Details

### HabitAvatarPickerDialog Features:
- **Hilt ViewModel Integration**: Uses `@HiltViewModel` for dependency injection
- **Coroutine Support**: Async avatar upload with loading states
- **Error Handling**: Shows error messages for upload failures
- **State Management**: Uses StateFlow for reactive UI updates
- **Image Caching**: Coil library for efficient image loading
- **GitHub Storage**: Stores avatars in private GitHub repo

### Avatar Flow:
1. User selects image from gallery
2. `AvatarPickerViewModel.uploadCustomAvatar(uri)` is called
3. `AvatarManager` uploads to GitHub and gets public URL
4. Avatar is saved to local DB with GitHub URL
5. URL is used to load image with Coil (with auth token if needed)

## Testing Verification

✅ **Build Success**: App compiles without errors
✅ **Installation**: Successfully installed on device
✅ **Import Fixed**: Correct package path for `AvatarPickerViewModel`

## Files Modified

1. **app/src/main/java/com/example/habittracker/ui/HomeScreen.kt**
   - Updated avatar selection in `AddHabitSheet`
   - Removed old `AvatarSelector` composable
   - Added dialog state management

2. **app/src/main/java/com/example/habittracker/ui/HabitAvatarPickerDialog.kt**
   - Fixed import for `AvatarPickerViewModel`

## Next Steps

### Testing Checklist:
- [ ] Test emoji selection in habit creation sheet
- [ ] Test custom image upload in habit creation sheet
- [ ] Verify avatar displays correctly after selection
- [ ] Test avatar persistence across app restarts
- [ ] Verify GitHub upload works with network connection
- [ ] Test error handling for failed uploads

### Future Enhancements:
- Add image cropping before upload
- Support for avatar search/categories
- Animated emoji picker
- Avatar pack downloads
- Community-shared avatars

## Migration Guide

If you're working on other screens that use avatar selection:

1. **Remove inline avatar selectors**
2. **Add state management**:
   ```kotlin
   var showAvatarPicker by remember { mutableStateOf(false) }
   ```
3. **Display current avatar with click handler**:
   ```kotlin
   Box(modifier = Modifier.clickable { showAvatarPicker = true }) {
       AvatarDisplay(avatar, size)
   }
   ```
4. **Show dialog conditionally**:
   ```kotlin
   if (showAvatarPicker) {
       HabitAvatarPickerDialog(
           currentAvatar = avatar,
           onAvatarSelected = { /* handle */ },
           onDismiss = { showAvatarPicker = false }
       )
   }
   ```

## Conclusion

This cleanup successfully unified the avatar selection experience across the app, eliminated duplicate code, and added support for custom image uploads. The codebase is now cleaner, more maintainable, and provides a better user experience.

