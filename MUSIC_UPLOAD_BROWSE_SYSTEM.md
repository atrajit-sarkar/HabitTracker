# Music Repository Upload & Browse System

## Overview
A comprehensive music management system that allows users to browse official songs and user-uploaded songs, with the ability to upload their own music to a GitHub repository.

## Architecture

### 1. Data Models (`GitHubModels.kt`)
- **GitHubUploadRequest**: Request payload for uploading files to GitHub
- **GitHubUploadResponse**: Response from GitHub upload API
- **GitHubFileContent**: Metadata for files in GitHub
- **GitHubTreeItem**: Directory/file items in GitHub repo
- **MusicCategory**: Represents a category folder with song count
- **UserFolder**: Represents a user's upload folder
- **SongUploadData**: Data structure for song uploads

### 2. GitHub API Service (`GitHubMusicService.kt`)
Handles all GitHub API interactions:
- **listOfficialCategories()**: Fetches official song categories from `music/official/`
- **listUserFolders()**: Fetches user-uploaded folders from `music/users/`
- **listSongsInCategory()**: Lists all songs in a specific category
- **uploadSong()**: Uploads a new song to the user's category folder

**Repository Structure:**
```
HabitTracker-Music/
├── music/
│   ├── official/
│   │   ├── ambient/
│   │   ├── classical/
│   │   ├── electronic/
│   │   └── ... (other categories)
│   └── users/
│       ├── {userId}/
│       │   ├── ambient/
│       │   ├── focus/
│       │   └── ... (user's categories)
│       └── ... (other users)
└── music.json (metadata file)
```

### 3. User Interface Screens

#### a. MusicBrowserRootScreen
**Purpose**: Entry point for music browsing
**Features**:
- Two main sections: "Official Songs" and "User Uploaded Songs"
- Floating action button to upload songs
- Beautiful gradient card design with icons

**Navigation**:
- Official Songs → Official Categories Browser
- User Uploaded Songs → User Folders List
- FAB → Upload Screen

#### b. MusicCategoryBrowserScreen
**Purpose**: Browse categories/folders
**Features**:
- Shows category folders with song counts
- Different views for official vs user-uploaded
- User folders show username (current user labeled as "You")
- Error handling with retry option
- Loading states

**Modes**:
1. **Official Mode**: Shows official category folders
2. **User Folders Mode**: Shows all user folders
3. **User Categories Mode**: Shows specific user's categories

#### c. MusicSongListScreen
**Purpose**: Display songs in a category
**Features**:
- Grid layout (2 columns) with animated cards
- Download/delete functionality for each song
- Song metadata display (title, artist, category)
- Integration with MusicPlayerScreen for playback
- Progress indicators for downloads
- Same UI as current background music page

**Song Card Features**:
- Gradient icon background
- Category tag
- Download button (if not downloaded)
- Delete button (if downloaded)
- Download progress indicator
- Visual feedback on selection

#### d. SongUploadScreen
**Purpose**: Upload songs to GitHub repository
**Features**:
- File picker for MP3 files
- Song details form (title, artist, category)
- Category selection dialog
- Upload progress indicator
- Auto-fill title from filename
- Validation before upload

**Upload Process**:
1. User selects MP3 file
2. Fills in song details
3. Selects category
4. Uploads to `music/users/{userId}/{category}/`
5. Updates music metadata

### 4. ViewModel (`MusicBrowserViewModel.kt`)
Manages state and business logic:
- **loadOfficialCategories()**: Loads official categories
- **loadUserFolders()**: Loads user upload folders
- **loadSongsInCategory()**: Loads songs in a category
- **setCurrentUserId()**: Sets current user ID
- **clearError()**: Clears error state

**State Management**:
```kotlin
data class MusicBrowserUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val officialCategories: List<MusicCategory> = emptyList(),
    val userFolders: List<UserFolder> = emptyList(),
    val currentSongs: List<MusicMetadata> = emptyList(),
    val currentUserId: String = ""
)
```

## Navigation Flow

```
Settings → Music Settings (music_settings)
    ↓
MusicBrowserRootScreen
    ├── Official Songs → music_category_browser/official
    │       ↓
    │   Category List → music_song_list/{path}/{name}
    │       ↓
    │   Song Grid → MusicPlayerScreen (overlay)
    │
    ├── User Uploaded Songs → music_category_browser/user
    │       ↓
    │   User Folders List → User Categories
    │       ↓
    │   Category List → music_song_list/{path}/{name}
    │       ↓
    │   Song Grid → MusicPlayerScreen (overlay)
    │
    └── Upload FAB → music_upload
            ↓
        SongUploadScreen
```

## Configuration

### Build Configuration
Updated `app/build.gradle.kts` to include GitHub token:
```kotlin
val githubTokenMusicRepo = keystoreProperties.getProperty("GITHUB_TOKEN_MUSIC_REPO") ?: ""
buildConfigField("String", "GITHUB_TOKEN_MUSIC_REPO", "\"$githubTokenMusicRepo\"")
```

### Keystore Properties
Add to `keystore.properties`:
```properties
GITHUB_TOKEN_MUSIC_REPO=your_github_token_here
```

## Features

### Official Songs
- Curated collection of background music
- Organized by categories (Ambient, Classical, Electronic, etc.)
- Maintained by app administrators
- High-quality, tested tracks

### User Uploaded Songs
- Community contributions
- User-specific folders
- Current user's folder labeled as "You"
- Category organization within each user folder
- Same categories as official songs

### Song Management
- **Download**: Download songs for offline playback
- **Delete**: Remove downloaded songs to free space
- **Play**: Stream or play downloaded songs
- **Upload**: Share songs with community

### Upload System
- Secure GitHub API integration
- Automatic file organization
- Metadata management
- Progress tracking
- Validation and error handling

## UI/UX Highlights

### Design Principles
1. **Consistent with existing UI**: Matches current background music screen design
2. **Intuitive navigation**: Clear hierarchy and flow
3. **Visual feedback**: Animations and state indicators
4. **Error resilience**: Graceful error handling with retry options

### Animations
- Scale animations on card press
- Fade transitions for deletion
- Smooth page transitions
- Progress indicators for async operations
- Gradient animations for visual interest

### Accessibility
- Clear icons and labels
- Haptic feedback on interactions
- Loading states with progress indicators
- Error messages with retry options

## Integration Points

### MainActivity
- Provides MusicDownloadManager instance
- Provides BackgroundMusicManager instance
- Handles music playback

### MusicRepositoryService
- Existing service for fetching music metadata
- Compatible with new GitHub structure

### MusicDownloadManager
- Downloads songs to local storage
- Manages cached files
- Provides download status

### BackgroundMusicManager
- Plays music tracks
- Manages playback state
- Controls volume

## Security

### GitHub Token
- Stored in `keystore.properties` (not in git)
- Injected via BuildConfig
- Used for authenticated API calls
- Required for write operations

### Upload Validation
- File type checking (MP3 only)
- Required fields validation
- File size limits (handled by GitHub API)
- User authentication required

## Future Enhancements

### Planned Features
1. **Rating System**: Users can rate songs
2. **Search Functionality**: Search across all songs
3. **Playlist Creation**: Create custom playlists
4. **Social Features**: Like, comment on songs
5. **Moderation**: Admin approval for uploads
6. **Download All**: Bulk download categories
7. **Sort/Filter**: Sort by popularity, date, etc.

### Technical Improvements
1. **Caching**: Cache category lists
2. **Offline Mode**: Better offline support
3. **Compression**: Compress files before upload
4. **Metadata Extraction**: Auto-extract MP3 metadata
5. **Thumbnail Generation**: Generate waveform previews

## Files Created/Modified

### New Files
1. `app/src/main/java/com/example/habittracker/data/model/GitHubModels.kt`
2. `app/src/main/java/com/example/habittracker/data/repository/GitHubMusicService.kt`
3. `app/src/main/java/com/example/habittracker/music/ui/MusicBrowserRootScreen.kt`
4. `app/src/main/java/com/example/habittracker/music/ui/MusicCategoryBrowserScreen.kt`
5. `app/src/main/java/com/example/habittracker/music/ui/MusicSongListScreen.kt`
6. `app/src/main/java/com/example/habittracker/music/ui/MusicBrowserViewModel.kt`
7. `app/src/main/java/com/example/habittracker/music/ui/SongUploadScreen.kt`

### Modified Files
1. `app/build.gradle.kts` - Added GITHUB_TOKEN_MUSIC_REPO to BuildConfig
2. `app/src/main/java/com/example/habittracker/ui/HabitTrackerNavigation.kt` - Added new routes

## Testing Checklist

### Navigation
- [ ] Navigate from Settings → Music Browser Root
- [ ] Navigate to Official Songs
- [ ] Navigate to User Uploaded Songs
- [ ] Navigate to specific categories
- [ ] Navigate to song lists
- [ ] Back navigation works correctly

### Official Songs
- [ ] Categories load correctly
- [ ] Song counts are accurate
- [ ] Songs display in grid
- [ ] Download functionality works
- [ ] Delete functionality works
- [ ] Playback works

### User Uploads
- [ ] User folders load correctly
- [ ] Current user shows as "You"
- [ ] Categories within user folders work
- [ ] Songs display correctly

### Upload
- [ ] File picker opens
- [ ] MP3 files can be selected
- [ ] Form validation works
- [ ] Category selection works
- [ ] Upload progress shows
- [ ] Success/error handling works

### Error Handling
- [ ] Network errors show retry option
- [ ] Empty states display correctly
- [ ] Invalid file handling
- [ ] GitHub API errors handled

## Known Issues
1. Upload functionality needs GitHubMusicService injection in SongUploadScreen
2. music.json update not implemented yet (placeholder)
3. File size limits need to be enforced
4. No duplicate detection yet

## Notes
- All screens use Material Design 3
- Follows existing app architecture patterns
- Compatible with Hilt dependency injection
- Uses Kotlin Coroutines for async operations
- Supports dark/light themes
