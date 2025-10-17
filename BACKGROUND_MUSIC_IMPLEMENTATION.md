# Background Music Feature - Implementation Guide

## Status: Partially Implemented

### ✅ Completed
1. **BackgroundMusicManager** - Created service to manage music playback
2. **User Model Updated** - Added musicEnabled, musicTrack, musicVolume fields
3. **Assets Directory** - Created with README for music files

### 🔄 To Complete

#### 1. Add Music Files
Download royalty-free ambient music and place in `app/src/main/assets/`:
- ambient_calm.mp3
- ambient_focus.mp3
- ambient_nature.mp3
- lofi_chill.mp3
- piano_soft.mp3

Sources: Pixabay Music, YouTube Audio Library, FreePD

#### 2. Update AuthRepository
Add methods to save/load music preferences:
```kotlin
suspend fun updateMusicPreferences(enabled: Boolean, track: String, volume: Float)
```

#### 3. Update AuthViewModel
Add state and methods for music preferences

#### 4. Add Music Settings UI to ProfileScreen
Add a new row in Profile Settings card:
```kotlin
ProfileSettingRow(
    icon = Icons.Default.MusicNote,
    title = "Background Music",
    subtitle = if (musicEnabled) "Playing: ${trackName}" else "Off",
    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
    onClick = { showMusicDialog = true }
)
```

#### 5. Create MusicSettingsDialog
Dialog with:
- Enable/Disable toggle
- Music track selection (dropdown/list)
- Volume slider
- Preview button

#### 6. Integrate with MainActivity
```kotlin
@Inject lateinit var musicManager: BackgroundMusicManager

override fun onCreate() {
    // Initialize music from user preferences
}

override fun onResume() {
    musicManager.resumeMusic()
}

override fun onPause() {
    musicManager.pauseMusic()
}

override fun onDestroy() {
    musicManager.stopMusic()
}
```

#### 7. Firebase Sync
Update user document in Firestore when music preferences change:
```kotlin
firestore.collection("users").document(uid).update(
    mapOf(
        "musicEnabled" to enabled,
        "musicTrack" to track.name,
        "musicVolume" to volume
    )
)
```

### Testing
1. Test with and without music files present
2. Test volume changes
3. Test track switching
4. Test enable/disable
5. Test app lifecycle (pause/resume)
6. Test Firebase sync

### Notes
- Music continues playing between screen navigations
- Music pauses when app goes to background
- Music stops on app close
- Graceful failure if music files missing
- Default volume: 30%
