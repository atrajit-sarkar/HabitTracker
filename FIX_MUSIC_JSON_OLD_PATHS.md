# Fix music.json - Remove Old custom_music References

## Issue Found
The music.json file contains an old song entry pointing to the deleted `custom_music/` folder:

```json
{
  "id": "custom_1760954723664_eoBwm1gt",
  "url": "https://raw.githubusercontent.com/gongobongofounder/HabitTracker-Music/main/custom_music/1760954723576_Primary:Purple_Desire_-_The_Gr.mp3",
  ...
}
```

Since the `custom_music/` folder was deleted, this song won't be downloadable.

## Current Implementation Status ✅

**Good News:** The fetching logic is already correct!

### How User Songs Are Fetched:
1. **By metadata field**, NOT by URL path
2. Checks `uploadedBy` field to identify user songs
3. Filters in `listUserFolders()` and `listSongsInCategory()` methods

```kotlin
// From GitHubMusicService.kt
val userSongs = musicResponse.music.filter { it.isUserUploaded }

// MusicMetadata.kt
val isUserUploaded: Boolean
    get() = uploadedBy != null || source == "MANUAL"
```

### New Upload Path:
- ✅ Uploads to: `/music/users/{userId}/{category}/{filename}`
- ✅ Updates music.json with proper `uploadedBy`, `uploaderName`, `source` fields
- ✅ Songs appear immediately in user folders by checking metadata

## Solution Options

### Option 1: Manual Cleanup (Recommended)
Remove the old entry from music.json on GitHub:

1. Go to: https://github.com/gongobongofounder/HabitTracker-Music
2. Edit `music.json`
3. Remove the entry with ID `"custom_1760954723664_eoBwm1gt"`
4. Commit the change

### Option 2: Re-upload If Needed
If you want to keep that song:
1. Download the original MP3 file (if you have it)
2. Use the app's upload feature
3. It will be uploaded to the new `/music/users/...` path
4. Will work perfectly with the new system

## Why This Isn't Breaking

✅ **Fetching works fine** - identifies user songs by `uploadedBy` field
✅ **New uploads work fine** - use correct path `/music/users/...`
✅ **Filtering works fine** - doesn't rely on URL path structure
✅ **Display works fine** - groups by user based on metadata

**Only issue:** That one old song's download URL is broken (points to deleted folder)

## Verification

Test the new implementation:
1. Open app
2. Navigate to "User Music" section
3. Check if user folders display correctly (they will, based on `uploadedBy`)
4. Try uploading a new song
5. Verify it appears in your folder
6. Download and play it

The old song will show in the list but fail to download. All new uploads will work perfectly.

## Code Confirmation

### Upload Path (from SongUploadScreen.kt):
```kotlin
// Creates path: music/users/{userId}/{category}/{filename}
val filePath = "$USER_SONGS_PATH/$userId/${songData.category}/$sanitizedFileName"
```

### Fetch Logic (from GitHubMusicService.kt):
```kotlin
// Filters by uploadedBy field, NOT URL
val userSongs = musicResponse.music.filter { it.isUserUploaded }

// Groups by uploader
val folders = userSongs.groupBy { it.uploadedBy ?: "unknown" }
```

### URL Structure:
- **Old (broken)**: `...custom_music/...`
- **New (working)**: `...music/users/{userId}/{category}/...`

## Conclusion

✅ **No code changes needed**
✅ **Fetching already works correctly**
✅ **New uploads use correct path**
✅ **Only action needed:** Remove old entry from music.json (optional)

The implementation is correct and ready to use!
