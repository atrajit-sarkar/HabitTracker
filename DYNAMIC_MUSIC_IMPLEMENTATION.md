# Dynamic Music Loading Implementation

## Overview
The app now dynamically fetches background music metadata from a GitHub repository, enabling remote music management without app updates.

## Architecture

### 1. Data Layer
- **MusicModels.kt**: Data classes for music metadata
  - `MusicResponse`: Root response with version and music list
  - `MusicMetadata`: Individual track information
  - `CachedMusicData`: Wrapper with caching timestamp

### 2. Repository Layer
- **MusicRepositoryService.kt**: Fetches and caches music metadata
  - Fetches from: `https://raw.githubusercontent.com/gongobongofounder/HabitTracker-Music/main/music.json`
  - Three-tier caching: Memory → File → Network
  - Cache validity: 24 hours
  - Fallback to expired cache on network failure

### 3. Manager Layer
- **DynamicMusicManager.kt**: Business logic for music management
  - Manages music list state with Kotlin Flows
  - Provides music filtering (by ID, category)
  - Schedules periodic background updates (every 12 hours)
  - WorkManager integration for background sync

### 4. UI Layer
- **MusicSettingsViewModel.kt**: ViewModel for music settings screen
  - Exposes music list state to UI
  - Handles refresh and update operations
  - Manages loading/error states

- **MusicSettingsScreen.kt**: Updated to use dynamic music
  - Integrates with `MusicSettingsViewModel`
  - Shows refresh button in toolbar
  - Displays loading/error states
  - Maintains backward compatibility with existing download system

### 5. Bridge Layer
- **MusicSystemBridge.kt**: Compatibility layer
  - Bridges between dynamic system and legacy enum-based system
  - Converts `MusicMetadata` ↔ `MusicTrack` enum
  - Ensures smooth transition

## Features

### ✅ Implemented
1. **Dynamic Music Fetching**: Loads music list from GitHub repository
2. **Three-Tier Caching**: Memory → File → Network for optimal performance
3. **Offline Support**: Falls back to cached data when offline
4. **Background Updates**: Periodic checks for new music (every 12 hours)
5. **Pull-to-Refresh**: Manual refresh button in toolbar
6. **Error Handling**: Graceful fallback to cached data on errors
7. **Loading States**: Progress indicators for better UX
8. **Version Tracking**: Detects music metadata changes via version field

### 🔄 Caching Strategy
```
1. Check memory cache (instant)
   ↓ (if invalid/missing)
2. Check file cache (fast)
   ↓ (if invalid/missing)
3. Fetch from network (slow)
   ↓
4. Update both caches
```

### 📦 Cache Management
- **Location**: `context.cacheDir/music_cache.json`
- **Validity**: 24 hours
- **Update Checks**: Every 12 hours (configurable)
- **Manual Refresh**: Available via UI button
- **Clear Cache**: `musicSettingsViewModel.clearCacheAndReload()`

## Music Repository Structure

### Repository URL
```
https://github.com/gongobongofounder/HabitTracker-Music
```

### metadata File (music.json)
```json
{
  "version": "1.0.0",
  "lastUpdated": "2024-01-15",
  "music": [
    {
      "id": "AMBIENT_1",
      "title": "Peaceful Calm",
      "artist": "Ambient Artist",
      "filename": "ambient_calm.mp3",
      "category": "Ambient",
      "duration": 180,
      "sizeBytes": 4200000
    },
    ...
  ]
}
```

### Music Files Location
```
https://raw.githubusercontent.com/gongobongofounder/HabitTracker-Music/main/music/{filename}
```

## Usage

### In the App

#### Get Music List
```kotlin
val musicState by musicSettingsViewModel.uiState.collectAsStateWithLifecycle()
val tracks = musicState.musicList
```

#### Refresh Music
```kotlin
musicSettingsViewModel.refreshMusicList()
```

#### Get Download URL
```kotlin
val url = musicSettingsViewModel.getDownloadUrl(metadata)
// Returns: https://raw.githubusercontent.com/.../music/ambient_calm.mp3
```

#### Check for Updates
```kotlin
musicSettingsViewModel.checkForUpdates()
```

### Background Updates

WorkManager automatically checks for updates every 12 hours when:
- Device is connected to network
- Battery is not low

To manually trigger:
```kotlin
dynamicMusicManager.checkAndUpdateIfNeeded()
```

## Integration with Existing System

### Backward Compatibility
The system maintains compatibility with the existing `BackgroundMusicManager` enum-based system:

```kotlin
// Dynamic metadata → Enum
val musicTrack = MusicSystemBridge.metadataToEnum(metadata)

// Enum → ID string
val trackId = MusicSystemBridge.enumToId(MusicTrack.AMBIENT_1)
```

### Download Integration
The existing `MusicDownloadManager` continues to work:
- Uses `filename` from `MusicMetadata`
- Downloads from GitHub repository URLs
- Caches files locally as before

## Dependencies

### Required Libraries
```gradle
// Already in project
implementation "com.squareup.okhttp3:okhttp:4.x.x"
implementation "com.squareup.moshi:moshi:1.x.x"
implementation "com.squareup.moshi:moshi-kotlin:1.x.x"
implementation "androidx.work:work-runtime-ktx:2.x.x"

// Hilt for dependency injection
implementation "com.google.dagger:hilt-android:2.x.x"
kapt "com.google.dagger:hilt-compiler:2.x.x"
```

## Network Configuration

### Permissions
Already in AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Timeout Settings
- Connect timeout: 30 seconds
- Read timeout: 30 seconds
- Retry policy: Exponential backoff (WorkManager)

## Testing

### Manual Testing
1. **Initial Load**: Open music settings → Should load from cache/network
2. **Refresh**: Tap refresh button → Should fetch latest
3. **Offline**: Disable network → Should show cached music
4. **Error Handling**: Invalid repo URL → Should show error + cached fallback
5. **Updates**: Change music.json version → Should detect update

### Debug Logs
Look for tags:
- `MusicRepository`: Network operations
- `DynamicMusicManager`: Manager operations
- `MusicUpdateWorker`: Background updates

## Future Enhancements

### Planned Features
- [ ] Download progress tracking for repository fetches
- [ ] Diff-based updates (only download changed tracks)
- [ ] Multiple repository support
- [ ] User-selectable music quality (low/high bitrate)
- [ ] Music preview before download
- [ ] Automatic cleanup of removed tracks
- [ ] Analytics for popular tracks

### Configuration Options
Consider making these configurable:
- Cache validity duration
- Update check interval
- Repository URL (for custom servers)
- Network requirements (WiFi-only option)

## Troubleshooting

### Music not loading
1. Check network connectivity
2. Verify repository URL is accessible
3. Check logs for specific errors
4. Try manual refresh
5. Clear cache and reload

### Cache issues
```kotlin
// Clear cache
musicSettingsViewModel.clearCacheAndReload()
```

### Update not detecting changes
- Verify `version` or `lastUpdated` changed in music.json
- Check last update time (12-hour interval)
- Manually refresh to force check

## Performance Considerations

### Memory Usage
- Memory cache: ~10-50 KB (metadata only, not audio files)
- File cache: Same size as memory cache
- Negligible impact on app memory

### Network Usage
- Initial load: ~5-15 KB (music.json)
- Update checks: Same as initial load
- Only fetches metadata, not audio files
- Audio files downloaded separately via MusicDownloadManager

### Battery Impact
- Minimal: Background checks only every 12 hours
- Constrained by WorkManager policies
- Only runs on network + battery not low

## Security Considerations

### Data Integrity
- No sensitive data in music.json
- Public GitHub repository (read-only access)
- HTTPS for all network requests
- No authentication required

### Privacy
- No user data sent to repository
- No tracking or analytics in metadata fetches
- Local caching only

## Maintenance

### Updating Music List
1. Edit `music-repository/music.json`
2. Add/remove/modify tracks
3. Update `version` field
4. Commit and push to GitHub
5. App will automatically detect and fetch updates

### Adding New Music
1. Add MP3 file to `music-repository/music/`
2. Add metadata entry to `music.json`
3. Increment version number
4. Push changes

### Removing Music
1. Remove entry from `music.json`
2. Increment version number
3. Push changes
4. (Optional) Delete file from repository

## Success Metrics

The implementation successfully achieves:
- ✅ Dynamic music loading from GitHub
- ✅ Offline-first architecture with caching
- ✅ Backward compatibility with existing system
- ✅ Minimal performance impact
- ✅ User-friendly UI with loading states
- ✅ Automatic background updates
- ✅ Robust error handling

## Migration Path

For complete migration from enum-based to fully dynamic system:
1. ✅ Create dynamic loading infrastructure (DONE)
2. ✅ Add caching layer (DONE)
3. ✅ Update UI to use dynamic list (DONE)
4. 🔄 Test thoroughly with real users
5. ⏳ Optionally remove enum system in future version
6. ⏳ Migrate legacy settings to dynamic IDs

## Summary

The dynamic music loading system is now **fully integrated** and ready for use. It provides:
- Remote music management via GitHub
- Offline support with intelligent caching
- Seamless integration with existing features
- Automatic updates without app releases
- Better scalability for growing music library

Users will see no disruption, and the music library can now be updated instantly by pushing changes to the GitHub repository.
