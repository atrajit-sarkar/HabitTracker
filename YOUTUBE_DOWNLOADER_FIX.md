# YouTube Downloader Feature - HttpTimeout Fix

## Problem
The YouTube Downloader feature was crashing with:
```
java.lang.NoClassDefFoundError: Failed resolution of: Lio/ktor/client/plugins/HttpTimeout;
```

## Root Cause
The NewPipeExtractor-KMP library requires Ktor HTTP client dependencies, but not all necessary Ktor modules were included in the project, particularly the `HttpTimeout` class.

## Solution Applied

### 1. Updated Ktor Version
**File**: `gradle/libs.versions.toml`
- Changed Ktor version from `2.3.12` to `3.0.0`
```toml
ktor = "3.0.0"
```

### 2. Switched to OkHttp Engine (Critical Fix)
The initial implementation used `ktor-client-cio` which had SSL certificate validation issues on Android (CertificateException). Switched to `ktor-client-okhttp` which properly handles Android's SSL certificate store.

**File**: `gradle/libs.versions.toml`

Added the following library declarations:
```toml
# Ktor Client (required by NewPipe KMP)
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-client-logging = { group = "io.ktor", name = "ktor-client-logging", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
```

**File**: `app/build.gradle.kts`

Added the following implementation dependencies:
```kotlin
// Ktor Client (required by NewPipe KMP)
implementation(libs.ktor.client.core)
implementation(libs.ktor.client.okhttp)  // Using OkHttp instead of CIO for Android SSL compatibility
implementation(libs.ktor.client.content.negotiation)
implementation(libs.ktor.client.logging)
implementation(libs.ktor.serialization.kotlinx.json)
```

### 3. ProGuard Rules
**File**: `app/proguard-rules.pro`

ProGuard rules were already in place (added in previous fix attempt):
```pro
# Ktor Client
-keep class io.ktor.** { *; }
-keep interface io.ktor.** { *; }
-keepnames class io.ktor.** { *; }
-keep class io.ktor.client.plugins.** { *; }
-keep class io.ktor.client.engine.** { *; }
-keep class io.ktor.client.request.** { *; }
-dontwarn io.ktor.**
```

## Build Commands
```powershell
# Clean build to ensure all dependencies are included
.\gradlew clean assembleDebug

# Install on device
.\gradlew installDebug
```

## Dependencies Summary
The YouTube Downloader now requires:
- **NewPipeExtractor-KMP v1.0**: YouTube metadata extraction
- **Ktor Client v3.0.0**: HTTP client framework with modules:
  - `ktor-client-core`: Core HTTP client functionality
  - `ktor-client-okhttp`: OkHttp engine (Android-compatible SSL handling)
  - `ktor-client-content-negotiation`: Content type negotiation
  - `ktor-client-logging`: Logging plugin (includes HttpTimeout)
  - `ktor-serialization-kotlinx-json`: JSON serialization

## Important Notes
- **Engine Choice**: `ktor-client-okhttp` is mandatory for Android as `ktor-client-cio` has SSL certificate validation issues
- The CIO engine fails with: `java.security.cert.CertificateException: Domain specific configurations require that hostname aware checkServerTrusted(X509Certificate[], String, String) is used`
- OkHttp properly integrates with Android's certificate store and handles SSL/TLS correctly

## Testing
After installation, test the feature by:
1. Open app → Profile screen
2. Tap "YouTube Downloader" button
3. Paste a YouTube URL (e.g., `https://youtube.com/shorts/m_TUAe7plL0`)
4. Verify metadata extraction works without crashes
5. Test MP3 and MP4 download functionality

## Notes
- The `ktor-client-logging` module is crucial as it contains the `HttpTimeout` class required by the CIO engine
- Ktor 3.0.0 provides better stability and compatibility
- Clean builds are necessary when adding new dependencies to ensure they're properly included in the APK
- SLF4J warnings in logs are expected (NOP implementation is fine for production)

## Additional Fixes Applied

### 3. Flow Context Violation Fix
**Problem**: `IllegalStateException: Flow invariant is violated` when downloading
- Flow was collected on Main dispatcher but emissions happened on IO dispatcher
- MediaDownloader used `withContext(Dispatchers.IO)` inside flow builder

**Solution**: 
- Removed `withContext(Dispatchers.IO)` from inside the flow
- Added `.flowOn(Dispatchers.IO)` at the end of the flow builder
- This ensures proper coroutine context handling

**File**: `MediaDownloader.kt`
```kotlin
fun downloadFile(...): Flow<DownloadState> = flow {
    // Download logic without withContext
    okHttpClient.newCall(request).execute().use { response ->
        // ... download code ...
    }
}.flowOn(Dispatchers.IO)  // Handle context at flow level
```

### 4. Video Stream Extraction
**Problem**: "No video stream available" error for YouTube Shorts
- NewPipe-Extractor-KMP API doesn't expose `videoStreams` or `videoOnlyStreams` directly
- Original code tried to access non-existent properties

**Solution**:
- Use HLS and DASH URLs which are reliably available
- These adaptive streaming URLs work for both regular videos and Shorts
- Added logging to debug stream availability

**File**: `YouTubeExtractor.kt`
```kotlin
// Use HLS/DASH URLs instead of direct video streams
val videoStreams = mutableListOf<VideoStreamInfo>()

if (!streamInfo.hlsUrl.isNullOrEmpty()) {
    videoStreams.add(VideoStreamInfo(url = streamInfo.hlsUrl, format = "HLS", ...))
}

if (!streamInfo.dashMpdUrl.isNullOrEmpty()) {
    videoStreams.add(VideoStreamInfo(url = streamInfo.dashMpdUrl, format = "DASH", ...))
}
```

## Status
✅ Build successful
✅ Installed on device (RMX3750 - Android 15)
✅ SSL certificate errors fixed (OkHttp engine)
✅ Flow context violations fixed (flowOn)
✅ Video stream extraction working (HLS/DASH URLs)
✅ Ready for full testing
