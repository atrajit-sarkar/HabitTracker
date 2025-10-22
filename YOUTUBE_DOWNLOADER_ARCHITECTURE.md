# YouTube Downloader - Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         USER INTERFACE (Compose)                         │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │            YouTubeDownloaderScreen.kt                          │   │
│  │                                                                │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │   │
│  │  │ URL Input    │  │ Video Preview│  │ Format Select│       │   │
│  │  │ Card         │  │ Card         │  │ Card         │       │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘       │   │
│  │                                                                │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │   │
│  │  │ Download     │  │ Progress     │  │ Success/Error│       │   │
│  │  │ Button       │  │ Card         │  │ Message      │       │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘       │   │
│  │                                                                │   │
│  └────────────────────────────────────────────────────────────────┘   │
│                                ↕                                        │
│                    StateFlow<UiState>                                   │
│                                ↕                                        │
└─────────────────────────────────────────────────────────────────────────┘
                                ↕
┌─────────────────────────────────────────────────────────────────────────┐
│                      VIEW MODEL (Business Logic)                         │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────┐   │
│  │         YouTubeDownloaderViewModel.kt (@HiltViewModel)         │   │
│  │                                                                │   │
│  │  State Management:                                            │   │
│  │  • youtubeUrl: String                                         │   │
│  │  • isValidatingUrl: Boolean                                   │   │
│  │  • isDownloading: Boolean                                     │   │
│  │  • videoMetadata: VideoMetadata?                              │   │
│  │  • downloadProgress: DownloadProgress?                        │   │
│  │  • downloadFormat: DownloadFormat (MP3/MP4)                   │   │
│  │  • errorMessage: String?                                      │   │
│  │  • successMessage: String?                                    │   │
│  │                                                                │   │
│  │  Methods:                                                      │   │
│  │  • updateUrl(url: String)                                     │   │
│  │  • validateAndExtractMetadata()                               │   │
│  │  • setDownloadFormat(format: DownloadFormat)                  │   │
│  │  • startDownload()                                            │   │
│  │  • cancelDownload()                                           │   │
│  │  • clearMessages()                                            │   │
│  │                                                                │   │
│  └────────────────────────────────────────────────────────────────┘   │
│                    ↓                              ↓                      │
└─────────────────────────────────────────────────────────────────────────┘
                    ↓                              ↓
    ┌───────────────────────────┐    ┌───────────────────────────┐
    │  YouTubeExtractor.kt      │    │  MediaDownloader.kt       │
    │                           │    │                           │
    │  Responsibilities:        │    │  Responsibilities:        │
    │  • Parse YouTube URLs     │    │  • Download files         │
    │  • Extract video ID       │    │  • Track progress         │
    │  • Fetch metadata         │    │  • Calculate speed        │
    │  • Get stream URLs        │    │  • Handle cancellation    │
    │  • Format duration        │    │  • Error handling         │
    │  • Format view count      │    │  • Save to disk           │
    │                           │    │                           │
    │  Uses: NewPipe Extractor  │    │  Uses: OkHttp             │
    └───────────────────────────┘    └───────────────────────────┘
                    ↓                              ↓
    ┌───────────────────────────┐    ┌───────────────────────────┐
    │  NewPipe Library          │    │  OkHttp Client            │
    │  (External Dependency)    │    │  (HTTP Downloads)         │
    │                           │    │                           │
    │  • YouTube API scraping   │    │  • Network requests       │
    │  • Stream extraction      │    │  • Connection pooling     │
    │  • No API key required    │    │  • Timeout handling       │
    └───────────────────────────┘    └───────────────────────────┘
                    ↓                              ↓
    ┌───────────────────────────────────────────────────────────┐
    │                   YOUTUBE SERVERS                          │
    │                                                            │
    │  • Video metadata                                         │
    │  • Stream URLs                                            │
    │  • Thumbnails                                             │
    │  • Audio/Video files                                      │
    └───────────────────────────────────────────────────────────┘
                    ↓
    ┌───────────────────────────────────────────────────────────┐
    │               DEVICE STORAGE                               │
    │                                                            │
    │  /Android/data/it.atraj.habittracker/files/               │
    │      └── YouTubeDownloads/                                │
    │          ├── Song_Title_123456789.m4a                     │
    │          ├── Video_Title_987654321.mp4                    │
    │          └── ...                                          │
    └───────────────────────────────────────────────────────────┘
```

---

## Data Flow Diagram

```
┌──────────┐
│  USER    │
└────┬─────┘
     │
     │ 1. Pastes YouTube URL
     ↓
┌─────────────────────┐
│  URL Input Field    │
└─────────┬───────────┘
          │
          │ 2. Enters URL, clicks "Fetch Video Info"
          ↓
┌────────────────────────┐
│  ViewModel             │ ──→ updateUrl(url)
│  validateAndExtract()  │
└─────────┬──────────────┘
          │
          │ 3. Calls extractor
          ↓
┌────────────────────────┐
│  YouTubeExtractor      │
│  extractVideoInfo()    │
└─────────┬──────────────┘
          │
          │ 4. Fetches from YouTube
          ↓
┌────────────────────────┐
│  NewPipe Library       │ ──→ HTTP Request
└─────────┬──────────────┘
          │
          │ 5. Returns metadata
          ↓
┌────────────────────────┐
│  ViewModel             │ ──→ Updates UiState
│  videoMetadata = ...   │
└─────────┬──────────────┘
          │
          │ 6. Emit state to UI
          ↓
┌────────────────────────┐
│  YouTubeDownloader     │
│  Screen                │ ──→ Shows preview card
└─────────┬──────────────┘
          │
          │ 7. User selects format, clicks Download
          ↓
┌────────────────────────┐
│  ViewModel             │
│  startDownload()       │
└─────────┬──────────────┘
          │
          │ 8. Gets stream URL
          ↓
┌────────────────────────┐
│  MediaDownloader       │
│  downloadFile()        │
└─────────┬──────────────┘
          │
          │ 9. Downloads with OkHttp
          ↓
┌────────────────────────┐
│  OkHttp                │ ──→ HTTP Download
└─────────┬──────────────┘
          │
          │ 10. Progress updates (Flow)
          ↓
┌────────────────────────┐
│  ViewModel             │ ──→ Updates progress
│  downloadProgress = .. │
└─────────┬──────────────┘
          │
          │ 11. Emit progress to UI
          ↓
┌────────────────────────┐
│  Progress Card         │ ──→ Shows percentage, speed
└─────────┬──────────────┘
          │
          │ 12. Download completes
          ↓
┌────────────────────────┐
│  File saved to disk    │ ──→ /YouTubeDownloads/
└─────────┬──────────────┘
          │
          │ 13. Success message
          ↓
┌────────────────────────┐
│  Success Card          │ ──→ "Download complete!"
└────────────────────────┘
```

---

## Navigation Flow

```
┌──────────────────┐
│   Home Screen    │
└────────┬─────────┘
         │
         │ Tap Profile
         ↓
┌──────────────────┐
│  Profile Screen  │
└────────┬─────────┘
         │
         │ Scroll to Settings
         ↓
┌──────────────────────────────────┐
│  Settings Section                │
│  • Background Music              │
│  • App Icon                      │
│  • YouTube Downloader ← HERE!    │  ← New Button
│  • Sign Out                      │
└────────┬─────────────────────────┘
         │
         │ Tap YouTube Downloader
         ↓
┌────────────────────────────────────┐
│  YouTubeDownloaderScreen          │
│                                    │
│  ┌──────────────────────────────┐ │
│  │  Paste URL                   │ │
│  └──────────────────────────────┘ │
│                                    │
│  [Fetch Video Info Button]        │
│                                    │
│  ┌──────────────────────────────┐ │
│  │  Video Preview               │ │
│  │  • Thumbnail                 │ │
│  │  • Title                     │ │
│  │  • Duration                  │ │
│  └──────────────────────────────┘ │
│                                    │
│  ┌────────────┐ ┌────────────┐   │
│  │    MP3     │ │    MP4     │   │
│  └────────────┘ └────────────┘   │
│                                    │
│  [Download Button]                │
│                                    │
│  ┌──────────────────────────────┐ │
│  │  Progress: 45%               │ │
│  │  ████████░░░░░░░░░░          │ │
│  │  2.1 MB / 4.7 MB             │ │
│  │  Speed: 1.2 MB/s             │ │
│  └──────────────────────────────┘ │
│                                    │
└────────┬───────────────────────────┘
         │
         │ Tap Back
         ↓
┌──────────────────┐
│  Profile Screen  │
└──────────────────┘
```

---

## State Management Flow

```
┌─────────────────────────────────────────────────────────┐
│                    UiState (Data Class)                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Initial State:                                         │
│  ┌────────────────────────────────────────┐            │
│  │ youtubeUrl = ""                         │            │
│  │ isValidatingUrl = false                 │            │
│  │ isDownloading = false                   │            │
│  │ videoMetadata = null                    │            │
│  │ downloadProgress = null                 │            │
│  │ downloadFormat = MP3                    │            │
│  │ errorMessage = null                     │            │
│  │ successMessage = null                   │            │
│  └────────────────────────────────────────┘            │
│                                                          │
│  After URL Input:                                       │
│  ┌────────────────────────────────────────┐            │
│  │ youtubeUrl = "https://youtu.be/ABC123"  │            │
│  │ ... (rest unchanged)                    │            │
│  └────────────────────────────────────────┘            │
│                                                          │
│  During Validation:                                     │
│  ┌────────────────────────────────────────┐            │
│  │ isValidatingUrl = true                  │            │
│  │ ... (rest unchanged)                    │            │
│  └────────────────────────────────────────┘            │
│                                                          │
│  After Metadata Fetch:                                  │
│  ┌────────────────────────────────────────┐            │
│  │ isValidatingUrl = false                 │            │
│  │ videoMetadata = VideoMetadata(...)      │            │
│  │ ... (rest unchanged)                    │            │
│  └────────────────────────────────────────┘            │
│                                                          │
│  During Download:                                       │
│  ┌────────────────────────────────────────┐            │
│  │ isDownloading = true                    │            │
│  │ downloadProgress = DownloadProgress(    │            │
│  │     bytesDownloaded = 2100000,          │            │
│  │     totalBytes = 4700000,               │            │
│  │     percentage = 45,                    │            │
│  │     speedBytesPerSecond = 1200000       │            │
│  │ )                                       │            │
│  │ ... (rest unchanged)                    │            │
│  └────────────────────────────────────────┘            │
│                                                          │
│  After Success:                                         │
│  ┌────────────────────────────────────────┐            │
│  │ isDownloading = false                   │            │
│  │ downloadProgress = null                 │            │
│  │ successMessage = "Download complete!"   │            │
│  │ ... (rest unchanged)                    │            │
│  └────────────────────────────────────────┘            │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## Component Interaction Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                      HabitTrackerNavigation                       │
│                                                                   │
│  composable("youtube_downloader") {                              │
│      YouTubeDownloaderScreen(                                    │
│          onBackClick = { navController.popBackStack() }          │
│      )                                                           │
│  }                                                               │
└──────────────────────────────────────────────────────────────────┘
                            │
                            │ Creates
                            ↓
┌──────────────────────────────────────────────────────────────────┐
│                  YouTubeDownloaderScreen                          │
│                                                                   │
│  val viewModel: YouTubeDownloaderViewModel = hiltViewModel()    │
│  val uiState by viewModel.uiState.collectAsStateWithLifecycle() │
│                                                                   │
│  Composable Components:                                          │
│  • InfoBanner()                                                  │
│  • URLInputCard(...)                                            │
│  • ErrorCard(...)                                               │
│  • SuccessCard(...)                                             │
│  • VideoMetadataCard(...)                                       │
│  • FormatSelectionCard(...)                                     │
│  • DownloadProgressCard(...)                                    │
│  • DownloadButton(...)                                          │
└──────────────────────────────────────────────────────────────────┘
                            │
                            │ Observes
                            ↓
┌──────────────────────────────────────────────────────────────────┐
│                YouTubeDownloaderViewModel                         │
│                                                                   │
│  @Inject constructor(                                            │
│      @ApplicationContext context: Context                        │
│  )                                                               │
│                                                                   │
│  private val _uiState = MutableStateFlow(UiState())             │
│  val uiState: StateFlow<UiState> = _uiState.asStateFlow()       │
│                                                                   │
│  private val youtubeExtractor = YouTubeExtractor()              │
│  private val mediaDownloader = MediaDownloader(context)         │
└──────────────────────────────────────────────────────────────────┘
        │                                              │
        │ Uses                                         │ Uses
        ↓                                              ↓
┌─────────────────────────┐              ┌─────────────────────────┐
│   YouTubeExtractor      │              │    MediaDownloader      │
│                         │              │                         │
│  • extractVideoInfo()   │              │  • downloadFile()       │
│  • extractVideoId()     │              │  • formatBytes()        │
│  • getBestAudioStream() │              │  • formatSpeed()        │
│  • getBestVideoStream() │              │  • cancelDownload()     │
└─────────────────────────┘              └─────────────────────────┘
```

---

## Error Handling Flow

```
┌──────────────┐
│  User Action │
└──────┬───────┘
       │
       ↓
┌─────────────────────────┐
│  Try Operation          │
└──────┬──────────────────┘
       │
       ↓
    ┌──┴──┐
    │ OK? │
    └──┬──┘
       │
  ┌────┴────┐
  │         │
 Yes       No
  │         │
  ↓         ↓
┌──────┐  ┌────────────────────┐
│Success│  │  Catch Exception   │
└───┬───┘  └─────────┬──────────┘
    │               │
    │               ↓
    │      ┌────────────────────┐
    │      │  Log Error         │
    │      │  Log.e(TAG, ...)   │
    │      └─────────┬──────────┘
    │               │
    │               ↓
    │      ┌────────────────────┐
    │      │  Update UiState    │
    │      │  errorMessage = .. │
    │      └─────────┬──────────┘
    │               │
    │               ↓
    │      ┌────────────────────┐
    │      │  Show Error Card   │
    │      │  Red container     │
    │      └────────────────────┘
    │
    ↓
┌────────────────────┐
│  Update UiState    │
│  successMessage    │
└─────────┬──────────┘
          │
          ↓
┌────────────────────┐
│  Show Success Card │
│  Green container   │
└────────────────────┘
```

---

## File Download Flow

```
Start Download
      ↓
Get Stream URL from metadata
      ↓
Create OkHttp Request
      ↓
Execute Request
      ↓
┌─────────────────┐
│ Response OK?    │
└────┬────────────┘
     │
  ┌──┴──┐
  │ Yes │
  └──┬──┘
     │
     ↓
Get Content Length (total bytes)
     ↓
Open Input Stream
     ↓
Open Output Stream (file)
     ↓
┌────────────────────────┐
│  Read chunks (8KB)     │ ←┐
└────────┬───────────────┘  │
         │                  │
         ↓                  │
  Update bytes downloaded   │
         │                  │
         ↓                  │
  Calculate percentage      │
         │                  │
         ↓                  │
  Calculate speed           │
         │                  │
         ↓                  │
  Emit progress state       │
         │                  │
         ↓                  │
  ┌──────────┐              │
  │ More data?│─────Yes─────┘
  └─────┬────┘
        │
       No
        │
        ↓
  Close streams
        ↓
  Emit success state
        ↓
  Show success message
        ↓
      Done!
```

---

## Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER                      │
│  • YouTubeDownloaderScreen.kt (UI)                      │
│  • Compose UI components                                │
│  • Material 3 design                                    │
│  • User interactions                                    │
└─────────────────────┬───────────────────────────────────┘
                      │ StateFlow
                      ↓
┌─────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER                          │
│  • YouTubeDownloaderViewModel.kt                        │
│  • Business logic                                       │
│  • State management                                     │
│  • Use cases                                            │
└─────────────────────┬───────────────────────────────────┘
                      │ Methods
                      ↓
┌─────────────────────────────────────────────────────────┐
│                     DATA LAYER                           │
│  • YouTubeExtractor.kt (YouTube operations)             │
│  • MediaDownloader.kt (File operations)                 │
│  • Repository pattern                                   │
│  • External APIs                                        │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP/API
                      ↓
┌─────────────────────────────────────────────────────────┐
│                  EXTERNAL SERVICES                       │
│  • YouTube servers                                      │
│  • NewPipe library                                      │
│  • OkHttp client                                        │
│  • File system                                          │
└─────────────────────────────────────────────────────────┘
```

---

**This completes the architecture documentation!** 🎉
