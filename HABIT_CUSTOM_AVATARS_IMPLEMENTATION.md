# Habit Custom Image Avatars Implementation Guide

## Overview
This guide explains how to add custom image support to habit avatars, allowing users to upload custom images in addition to emojis. The images will also appear in notifications.

## Status
✅ **Created**: `HabitAvatarPickerDialog.kt` - New dialog component with tabs for Emojis and Images

## Remaining Steps

### 1. Update AddHabitScreen.kt

Replace the current `AvatarSelector` composable with a button that opens the new dialog:

**Find this section (around line 356):**
```kotlin
// Avatar selection
AvatarSelector(
    selectedAvatar = state.avatar,
    onAvatarChange = onAvatarChange
)
```

**Replace with:**
```kotlin
// Avatar selection with custom image support
var showAvatarPicker by remember { mutableStateOf(false) }

Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    )
) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.avatar_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        // Current avatar display (clickable to open picker)
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { showAvatarPicker = true }
        ) {
            AvatarDisplay(
                avatar = state.avatar,
                size = 80.dp
            )
            // Edit indicator
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Change avatar",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
        
        TextButton(
            onClick = { showAvatarPicker = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Change Avatar")
        }
    }
}

// Avatar picker dialog
if (showAvatarPicker) {
    HabitAvatarPickerDialog(
        currentAvatar = state.avatar,
        onAvatarSelected = { newAvatar ->
            onAvatarChange(newAvatar)
        },
        onDismiss = { showAvatarPicker = false }
    )
}
```

### 2. Update AvatarDisplay in AddHabitScreen.kt

**Find the `AvatarDisplay` composable (around line 397-442):**

Replace the `CUSTOM_IMAGE` case to load actual images:

```kotlin
@Composable
private fun AvatarDisplay(
    avatar: HabitAvatar,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(
                color = Color(android.graphics.Color.parseColor(avatar.backgroundColor)),
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = CircleShape
            )
    ) {
        when (avatar.type) {
            HabitAvatarType.EMOJI -> {
                Text(
                    text = avatar.value,
                    fontSize = (size.value * 0.5f).sp,
                    textAlign = TextAlign.Center
                )
            }
            HabitAvatarType.DEFAULT_ICON -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size((size.value * 0.5f).dp)
                )
            }
            HabitAvatarType.CUSTOM_IMAGE -> {
                // Load custom image from URL using Coil
                val token = it.atraj.habittracker.avatar.SecureTokenStorage.getToken(context)
                val requestBuilder = ImageRequest.Builder(context)
                    .data(avatar.value)
                    .crossfade(true)
                
                if (token != null && avatar.value.contains("githubusercontent.com")) {
                    requestBuilder.addHeader("Authorization", "token $token")
                }
                
                AsyncImage(
                    model = requestBuilder.build(),
                    contentDescription = "Custom habit avatar",
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
    }
}
```

**Add imports at the top of AddHabitScreen.kt:**
```kotlin
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.Edit
```

### 3. Update HomeScreen.kt Avatar Display

Find all instances of habit avatar rendering (search for `HabitAvatarType`) and update to load custom images.

**Example location (around line 1330):**
```kotlin
when (habit.avatar.type) {
    HabitAvatarType.EMOJI -> {
        Text(text = habit.avatar.value, fontSize = 32.sp)
    }
    HabitAvatarType.DEFAULT_ICON -> {
        Icon(...)
    }
    HabitAvatarType.CUSTOM_IMAGE -> {
        val context = LocalContext.current
        val token = it.atraj.habittracker.avatar.SecureTokenStorage.getToken(context)
        val requestBuilder = ImageRequest.Builder(context)
            .data(habit.avatar.value)
            .crossfade(true)
        
        if (token != null && habit.avatar.value.contains("githubusercontent.com")) {
            requestBuilder.addHeader("Authorization", "token $token")
        }
        
        AsyncImage(
            model = requestBuilder.build(),
            contentDescription = "Habit avatar",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}
```

### 4. Update TrashScreen.kt Avatar Display

Same as HomeScreen - find and update avatar display logic to load custom images.

### 5. Update HabitDetailsScreen.kt Avatar Display

Same pattern - update avatar rendering to support custom images.

### 6. Update Notifications (IMPORTANT!)

**File: `app/src/main/java/com/example/habittracker/notification/HabitReminderService.kt`**

Find the notification building code and update to load custom images:

```kotlin
// Add this import at the top
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.runBlocking

// In the notification building function:
private fun buildNotification(habit: Habit): Notification {
    val builder = NotificationCompat.Builder(this, getChannelId(habit.id))
        .setContentTitle(habit.title)
        .setContentText(habit.description.ifEmpty { "Time to complete your habit!" })
        .setSmallIcon(R.drawable.ic_notification)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
    
    // Load custom avatar image for notification
    when (habit.avatar.type) {
        HabitAvatarType.EMOJI -> {
            // For emoji, create a text bitmap
            val bitmap = createEmojiB bitmap(habit.avatar.value, habit.avatar.backgroundColor)
            builder.setLargeIcon(bitmap)
        }
        HabitAvatarType.CUSTOM_IMAGE -> {
            // Load image from URL
            try {
                val bitmap = runBlocking {
                    loadImageFromUrl(habit.avatar.value)
                }
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                }
            } catch (e: Exception) {
                android.util.Log.e("HabitReminder", "Failed to load avatar image", e)
            }
        }
        else -> {
            // Default icon - no large icon
        }
    }
    
    return builder.build()
}

private suspend fun loadImageFromUrl(url: String): android.graphics.Bitmap? {
    return try {
        val loader = ImageLoader(this)
        val request = ImageRequest.Builder(this)
            .data(url)
            .allowHardware(false) // Disable hardware bitmaps for notifications
            .build()
        
        val result = (loader.execute(request) as? SuccessResult)?.drawable
        if (result is android.graphics.drawable.BitmapDrawable) {
            result.bitmap
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

private fun createEmojiBitmap(emoji: String, bgColorHex: String): android.graphics.Bitmap {
    val size = 128
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    // Draw background circle
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor(bgColorHex)
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    
    // Draw emoji text
    val textPaint = android.graphics.Paint().apply {
        textSize = size * 0.6f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    
    val textBounds = android.graphics.Rect()
    textPaint.getTextBounds(emoji, 0, emoji.length, textBounds)
    val y = size / 2f - textBounds.exactCenterY()
    
    canvas.drawText(emoji, size / 2f, y, textPaint)
    
    return bitmap
}
```

## Testing Checklist

After implementing all changes:

- [ ] Build successfully
- [ ] Create habit with emoji avatar → Shows in habit list
- [ ] Create habit with custom image → Upload works
- [ ] Custom image shows correctly in habit card
- [ ] Custom image shows in habit details
- [ ] Edit habit and change avatar type
- [ ] Notification shows emoji avatar correctly
- [ ] Notification shows custom image avatar correctly
- [ ] Custom images load from GitHub (with token)
- [ ] Trash screen shows custom avatars
- [ ] Restore habit keeps custom avatar

## File Summary

### Created Files
1. ✅ `HabitAvatarPickerDialog.kt` - Main picker dialog with emoji and image tabs

### Files to Modify
1. ⏳ `AddHabitScreen.kt` - Update avatar selector and display
2. ⏳ `HomeScreen.kt` - Update habit card avatar rendering  
3. ⏳ `TrashScreen.kt` - Update trash item avatar rendering
4. ⏳ `HabitDetailsScreen.kt` - Update detail avatar rendering
5. ⏳ `HabitReminderService.kt` - Update notification icon loading

## Key Points

1. **CUSTOM_IMAGE type already exists** - No need to modify data models
2. **Reuse existing avatar upload system** - Same ViewModel and repository as profile avatars
3. **GitHub token support** - Already implemented for private repositories
4. **Coil for image loading** - AsyncImage handles caching automatically
5. **Notifications need special handling** - Must load image synchronously using runBlocking

## Benefits

✅ Users can personalize habits with custom images
✅ Emojis still available for quick selection
✅ Images appear in notifications for better recognition
✅ Reuses existing infrastructure (no new backend needed)
✅ Supports both public and private GitHub avatar repos

## Implementation Time Estimate

- AddHabitScreen updates: 15 minutes
- HomeScreen/TrashScreen/DetailsScreen updates: 20 minutes  
- Notification updates: 30 minutes
- Testing: 15 minutes
- **Total: ~80 minutes**

Start with AddHabitScreen to get the picker working, then update displays, and finally tackle notifications!

