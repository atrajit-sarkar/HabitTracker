# Dynamic Music Loading - Architecture Diagram

## System Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                         GITHUB REPOSITORY                         │
│  https://github.com/gongobongofounder/HabitTracker-Music         │
│                                                                    │
│  ├── music.json (metadata)                                        │
│  └── music/                                                       │
│      ├── ambient_calm.mp3                                        │
│      ├── lofi_chill.mp3                                          │
│      └── ... (11 tracks total)                                   │
└────────────────────────────┬───────────────────────────────────┘
                             │ HTTPS GET
                             ↓
┌──────────────────────────────────────────────────────────────────┐
│                    MUSIC REPOSITORY SERVICE                       │
│                   MusicRepositoryService.kt                       │
│                                                                    │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │
│  │ Memory Cache   │→ │  File Cache    │→ │ Network Fetch  │    │
│  │ (Instant)      │  │ (Fast)         │  │ (Slow)         │    │
│  └────────────────┘  └────────────────┘  └────────────────┘    │
│                                                                    │
│  • OkHttp for network requests                                   │
│  • Moshi for JSON parsing                                        │
│  • 24-hour cache validity                                        │
│  • Graceful error fallback                                       │
└────────────────────────────┬───────────────────────────────────┘
                             │
                             ↓
┌──────────────────────────────────────────────────────────────────┐
│                    DYNAMIC MUSIC MANAGER                          │
│                   DynamicMusicManager.kt                          │
│                                                                    │
│  • Kotlin Flow state management                                  │
│  • Music list filtering (ID, category)                           │
│  • Update detection logic                                        │
│  • WorkManager scheduling                                        │
│                                                                    │
│  ┌──────────────────────────────────────────────┐               │
│  │  Background Update Worker (Every 12 hours)    │               │
│  │  • Network required                           │               │
│  │  • Battery not low                            │               │
│  │  • Exponential backoff on failure             │               │
│  └──────────────────────────────────────────────┘               │
└────────────────────────────┬───────────────────────────────────┘
                             │
                             ↓
┌──────────────────────────────────────────────────────────────────┐
│                    MUSIC SETTINGS VIEWMODEL                       │
│                   MusicSettingsViewModel.kt                       │
│                                                                    │
│  • Exposes UI state (music list, loading, errors)                │
│  • Handles user actions (refresh, clear cache)                   │
│  • Lifecycle-aware                                               │
└────────────────────────────┬───────────────────────────────────┘
                             │
                             ↓
┌──────────────────────────────────────────────────────────────────┐
│                      MUSIC SETTINGS SCREEN                        │
│                   MusicSettingsScreen.kt                          │
│                                                                    │
│  ┌────────────────────────────────────────────────────┐         │
│  │ TopBar: [← Back] Background Music [🔄 Refresh]     │         │
│  ├────────────────────────────────────────────────────┤         │
│  │ ⚠️ Error Card (if error)                           │         │
│  │ ⏳ Loading Card (if loading)                        │         │
│  ├────────────────────────────────────────────────────┤         │
│  │ 🎵 Enable/Disable Switch                           │         │
│  │ 🔊 Volume Slider                                   │         │
│  ├────────────────────────────────────────────────────┤         │
│  │ Available Tracks:                                  │         │
│  │ ○ No Music                                         │         │
│  │ ○ Peaceful Calm [Download] [Delete]               │         │
│  │ ○ Lo-Fi Chill [Downloaded] [Delete]               │         │
│  │ ○ ... (dynamic list from repository)              │         │
│  └────────────────────────────────────────────────────┘         │
└──────────────────────────────────────────────────────────────────┘
```

## Data Flow Sequence

```
1. APP LAUNCH
   ├─→ DynamicMusicManager.initialize()
   ├─→ Load from memory cache (if available)
   ├─→ Display cached music instantly
   └─→ Fetch fresh data in background
   
2. USER OPENS MUSIC SETTINGS
   ├─→ MusicSettingsScreen renders
   ├─→ Observe musicState from ViewModel
   ├─→ Display tracks list
   └─→ Check for updates (if >12 hours)
   
3. USER TAPS REFRESH
   ├─→ musicSettingsViewModel.refreshMusicList()
   ├─→ Show loading indicator
   ├─→ Force fetch from network
   ├─→ Update cache
   └─→ Update UI with new data
   
4. BACKGROUND UPDATE (Every 12 hours)
   ├─→ MusicUpdateWorker triggered
   ├─→ Check if version changed
   ├─→ If updated: fetch and cache
   └─→ Silent update (no UI notification)
   
5. OFFLINE MODE
   ├─→ Network fetch fails
   ├─→ Fall back to file cache
   ├─→ If cache expired: use anyway
   └─→ Show cached music with stale data
```

## Caching Strategy

```
┌─────────────────────────────────────────┐
│         getMusicMetadata()              │
│              Request                    │
└──────────────┬──────────────────────────┘
               ↓
      ┌────────────────┐
      │ Memory Cache?  │
      └────┬───────┬───┘
           │ YES   │ NO
           ↓       ↓
      [Return]  ┌────────────────┐
                │  File Cache?   │
                └────┬───────┬───┘
                     │ YES   │ NO
                     ↓       ↓
                [Return]  ┌──────────────┐
                          │ Network Fetch│
                          └──────┬───────┘
                                 ↓
                          ┌──────────────┐
                          │ Update Caches│
                          └──────┬───────┘
                                 ↓
                            [Return]
```

## Component Interactions

```
┌──────────────────┐
│  MainActivity    │
│                  │
│  ├─ AuthViewModel│
│  ├─ BackgroundMusicManager
│  └─ MusicDownloadManager
└─────────┬────────┘
          │
          ↓
┌─────────────────────────────┐
│ MusicSettingsScreen         │
│  (Composable)               │
│                             │
│  ViewModel Injection:       │
│  ├─ AuthViewModel           │
│  └─ MusicSettingsViewModel ←┼──┐
└─────────────────────────────┘  │
                                 │
┌─────────────────────────────┐  │
│ MusicSettingsViewModel      │←─┘
│  (Hilt ViewModel)           │
│                             │
│  Dependencies:              │
│  └─ DynamicMusicManager ────┼──┐
└─────────────────────────────┘  │
                                 │
┌─────────────────────────────┐  │
│ DynamicMusicManager         │←─┘
│  (Singleton)                │
│                             │
│  Dependencies:              │
│  ├─ Context                 │
│  └─ MusicRepositoryService ─┼──┐
└─────────────────────────────┘  │
                                 │
┌─────────────────────────────┐  │
│ MusicRepositoryService      │←─┘
│  (Singleton)                │
│                             │
│  Dependencies:              │
│  ├─ OkHttpClient            │
│  └─ Moshi                   │
└─────────────────────────────┘
```

## State Management Flow

```
┌──────────────────────────────────────────┐
│    DynamicMusicManager (State Source)    │
│                                          │
│  musicList: StateFlow<List<Metadata>>   │
│  isLoading: StateFlow<Boolean>          │
│  error: StateFlow<String?>              │
└──────────────┬───────────────────────────┘
               │ Observes
               ↓
┌──────────────────────────────────────────┐
│   MusicSettingsViewModel (State Bridge)  │
│                                          │
│  uiState: StateFlow<UiState>            │
│  ├─ musicList                           │
│  ├─ isLoading                           │
│  ├─ error                               │
│  └─ isRefreshing                        │
└──────────────┬───────────────────────────┘
               │ collectAsStateWithLifecycle()
               ↓
┌──────────────────────────────────────────┐
│   MusicSettingsScreen (UI)              │
│                                          │
│  val musicState by uiState              │
│  ├─ Render music list                   │
│  ├─ Show loading indicator              │
│  ├─ Display errors                      │
│  └─ Handle user actions                 │
└──────────────────────────────────────────┘
```

## Background Update Flow

```
WorkManager Schedule
      ↓
Every 12 hours
      ↓
┌─────────────────────┐
│ MusicUpdateWorker   │
└──────────┬──────────┘
           ↓
┌─────────────────────────────────┐
│ Check Conditions                │
│ ✓ Network available             │
│ ✓ Battery not low               │
└──────────┬──────────────────────┘
           ↓
┌─────────────────────────────────┐
│ Fetch Latest Metadata           │
└──────────┬──────────────────────┘
           ↓
┌─────────────────────────────────┐
│ Compare Versions                │
│ cached.version vs latest.version│
└──────────┬──────────────────────┘
           ↓
     ┌─────┴─────┐
     │           │
   Changed    Unchanged
     │           │
     ↓           ↓
  Update      [Skip]
  Cache
     │
     ↓
  Success!
```

## Error Handling Flow

```
Network Fetch Failed
      ↓
┌────────────────────┐
│ Check Memory Cache │
└─────┬──────────────┘
      │
      ↓ (if null)
┌────────────────────┐
│  Check File Cache  │
└─────┬──────────────┘
      │
      ↓
  ┌───┴────┐
  │        │
Found    Not Found
  │        │
  ↓        ↓
Use     Return
Expired  Error
Cache     +
(Stale)  Empty
  │       List
  ↓
Success
(with warning)
```

## Benefits Visualization

```
┌─────────────────────────────────────────────────────┐
│              BEFORE (Static Enum)                   │
├─────────────────────────────────────────────────────┤
│ • Hardcoded music list                              │
│ • Requires app update to add music                  │
│ • All music bundled in APK                          │
│ • Large APK size                                    │
│ • No remote management                              │
└─────────────────────────────────────────────────────┘
                       ⬇️ UPGRADE
┌─────────────────────────────────────────────────────┐
│              AFTER (Dynamic Loading)                 │
├─────────────────────────────────────────────────────┤
│ ✅ Dynamic music list from GitHub                    │
│ ✅ Instant updates without app release               │
│ ✅ Music downloaded on-demand                        │
│ ✅ Smaller APK size                                  │
│ ✅ Remote configuration & version control            │
│ ✅ Offline support with caching                      │
│ ✅ Automatic background updates                      │
│ ✅ Scalable music library                            │
└─────────────────────────────────────────────────────┘
```

## Performance Metrics

```
┌─────────────────────────────────────────────────────┐
│                  LOAD TIMES                         │
├─────────────────────────────────────────────────────┤
│ Memory Cache:    <1 ms   ████ (Instant)            │
│ File Cache:      ~5 ms   ████ (Very Fast)          │
│ Network Fetch:   ~500 ms ████████ (Moderate)       │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│              CACHE HIT RATES (Expected)             │
├─────────────────────────────────────────────────────┤
│ Memory:  70%  ███████████████████████              │
│ File:    25%  ████████                             │
│ Network:  5%  █                                    │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│                  DATA USAGE                         │
├─────────────────────────────────────────────────────┤
│ Metadata (JSON):  ~10 KB per fetch                  │
│ Music Files:      ~4 MB per track (on-demand)       │
│ Cache Storage:    ~15 KB (metadata only)            │
└─────────────────────────────────────────────────────┘
```

---

This architecture ensures:
- ⚡ **Fast Performance**: Multi-tier caching
- 📴 **Offline Support**: Graceful fallback
- 🔄 **Auto Updates**: Background sync
- 📱 **Low Impact**: Minimal battery/network usage
- 🎯 **Scalable**: Easy to add new music
- 🛡️ **Reliable**: Robust error handling
