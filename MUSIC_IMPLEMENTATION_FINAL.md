# Background Music - Final Implementation Steps

## ✅ COMPLETED
1. **BackgroundMusicManager.kt** - Full music service created
2. **User.kt** - Added musicEnabled, musicTrack, musicVolume fields
3. **AuthRepository.kt** - Added `updateMusicPreferences()` and Firebase sync
4. **AuthViewModel.kt** - Added `updateMusicPreferences()` method
5. **Assets directory** created with README

## 🔨 REMAINING STEPS (Quick Implementation)

### Step 1: Add Music Files (Required)
Download 5 small MP3 files (2-3 min each, ~1-2MB total) from Pixabay/YouTube Audio Library
Place in: `app/src/main/assets/`
- ambient_calm.mp3
- ambient_focus.mp3  
- ambient_nature.mp3
- lofi_chill.mp3
- piano_soft.mp3

### Step 2: Add Music Settings Row to ProfileScreen.kt
Insert BEFORE the "Sign Out" row (around line 1167):

```kotlin
HorizontalDivider(
    modifier = Modifier.padding(horizontal = 20.dp),
    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
)

// Background Music
val musicTrackName = remember(state.user?.musicTrack) {
    it.atraj.habittracker.music.BackgroundMusicManager.MusicTrack.values()
        .find { it.name == (state.user?.musicTrack ?: "NONE") }
        ?.displayName ?: "No Music"
}

ProfileSettingRow(
    icon = Icons.Default.MusicNote,
    title = "Background Music",
    subtitle = if (state.user?.musicEnabled == true) "Playing: $musicTrackName" else "Disabled",
    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
    gradientColors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
    ),
    iconTint = MaterialTheme.colorScheme.primary,
    textColor = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick = { showMusicDialog = true }
)
```

### Step 3: Add Dialog State to ProfileScreen (around line 262)
```kotlin
var showMusicDialog by remember { mutableStateOf(false) }
```

### Step 4: Add Music Dialog at end of ProfileScreen (around line 1375)
```kotlin
// Music Settings Dialog
if (showMusicDialog) {
    MusicSettingsDialog(
        currentEnabled = state.user?.musicEnabled ?: false,
        currentTrack = state.user?.musicTrack ?: "NONE",
        currentVolume = state.user?.musicVolume ?: 0.3f,
        onDismiss = { showMusicDialog = false },
        onSave = { enabled, track, volume ->
            viewModel.updateMusicPreferences(enabled, track, volume) {
                showMusicDialog = false
            }
        }
    )
}
```

### Step 5: Create MusicSettingsDialog Composable in ProfileScreen.kt
Add at the end of the file:

```kotlin
@Composable
private fun MusicSettingsDialog(
    currentEnabled: Boolean,
    currentTrack: String,
    currentVolume: Float,
    onDismiss: () -> Unit,
    onSave: (Boolean, String, Float) -> Unit
) {
    var enabled by remember { mutableStateOf(currentEnabled) }
    var selectedTrack by remember { mutableStateOf(currentTrack) }
    var volume by remember { mutableStateOf(currentVolume) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Background Music", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Enable/Disable Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Music")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                
                if (enabled) {
                    // Music Track Selection
                    Text("Select Track", style = MaterialTheme.typography.labelLarge)
                    it.atraj.habittracker.music.BackgroundMusicManager.MusicTrack.values().forEach { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTrack = track.name }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTrack == track.name,
                                onClick = { selectedTrack = track.name }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(track.displayName)
                        }
                    }
                    
                    // Volume Slider
                    Text("Volume: ${(volume * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        valueRange = 0f..1f
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(enabled, selectedTrack, volume) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### Step 6: Integrate with MainActivity.kt
Add these at class level:

```kotlin
@Inject lateinit var musicManager: BackgroundMusicManager
```

In `onCreate()` after setContent:
```kotlin
// Initialize music from user preferences
lifecycleScope.launch {
    authRepository.currentUser.collect { user ->
        user?.let {
            val track = BackgroundMusicManager.MusicTrack.values()
                .find { t -> t.name == user.musicTrack } 
                ?: BackgroundMusicManager.MusicTrack.NONE
            musicManager.initialize(track, user.musicVolume, user.musicEnabled)
        }
    }
}
```

Override lifecycle methods:
```kotlin
override fun onResume() {
    super.onResume()
    musicManager.resumeMusic()
}

override fun onPause() {
    super.onPause()
    musicManager.pauseMusic()
}

override fun onDestroy() {
    musicManager.stopMusic()
    super.onDestroy()
}
```

## TESTING
1. Build and install app
2. Go to Profile → Background Music
3. Enable music, select track, adjust volume
4. Save and verify music plays
5. Test pause/resume with home button
6. Test persistence after app restart

## DONE! 🎵
Music will now play in the background, sync to Firebase, and persist across sessions.
