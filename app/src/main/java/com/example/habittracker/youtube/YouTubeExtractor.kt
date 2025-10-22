package it.atraj.habittracker.youtube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.extractor.ServiceList
import com.yushosei.newpipe.extractor.stream.StreamInfo
import com.yushosei.newpipe.util.DefaultDownloaderImpl

/**
 * Utility class for extracting YouTube video information and download streams
 * using NewPipe Extractor library
 */
class YouTubeExtractor {
    
    data class VideoMetadata(
        val title: String,
        val uploader: String,
        val duration: Long, // in seconds
        val thumbnailUrl: String,
        val viewCount: Long,
        val audioStreams: List<AudioStreamInfo>,
        val videoStreams: List<VideoStreamInfo>
    )
    
    data class AudioStreamInfo(
        val url: String,
        val format: String,
        val averageBitrate: Int,
        val quality: String
    )
    
    data class VideoStreamInfo(
        val url: String,
        val format: String,
        val resolution: String,
        val fps: Int
    )
    
    companion object {
        private const val TAG = "YouTubeExtractor"
        
        private var isInitialized = false
        
        // Initialize NewPipe once
        suspend fun initialize() {
            if (!isInitialized) {
                try {
                    NewPipe.init(DefaultDownloaderImpl.initDefault())
                    isInitialized = true
                    Log.d(TAG, "NewPipe initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize NewPipe", e)
                }
            }
        }
        
        /**
         * Extract video ID from various YouTube URL formats
         */
        fun extractVideoId(url: String): String? {
            return try {
                when {
                    // Standard watch URL: https://www.youtube.com/watch?v=VIDEO_ID
                    url.contains("youtube.com/watch?v=") -> {
                        url.substringAfter("watch?v=")
                            .substringBefore("&")
                            .substringBefore("#")
                    }
                    // Short URL: https://youtu.be/VIDEO_ID
                    url.contains("youtu.be/") -> {
                        url.substringAfter("youtu.be/")
                            .substringBefore("?")
                            .substringBefore("#")
                    }
                    // Shorts: https://www.youtube.com/shorts/VIDEO_ID
                    url.contains("youtube.com/shorts/") -> {
                        url.substringAfter("shorts/")
                            .substringBefore("?")
                            .substringBefore("#")
                    }
                    // Mobile URL: https://m.youtube.com/watch?v=VIDEO_ID
                    url.contains("m.youtube.com/watch?v=") -> {
                        url.substringAfter("watch?v=")
                            .substringBefore("&")
                            .substringBefore("#")
                    }
                    // Embed URL: https://www.youtube.com/embed/VIDEO_ID
                    url.contains("youtube.com/embed/") -> {
                        url.substringAfter("embed/")
                            .substringBefore("?")
                            .substringBefore("#")
                    }
                    else -> null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract video ID from URL: $url", e)
                null
            }
        }
        
        /**
         * Validate if URL is a valid YouTube URL
         */
        fun isValidYouTubeUrl(url: String): Boolean {
            return url.contains("youtube.com") || url.contains("youtu.be")
        }
    }
    
    /**
     * Extract video metadata and available streams from YouTube URL
     */
    suspend fun extractVideoInfo(url: String): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        try {
            // Ensure NewPipe is initialized
            initialize()
            
            Log.d(TAG, "Extracting video info for URL: $url")
            
            // Get YouTube service (note: KMP version uses ServiceList.Youtube not YouTube)
            val youtubeService = ServiceList.YouTube
            
            // Extract stream info
            val streamInfo = StreamInfo.getInfo(youtubeService, url)
            
            Log.d(TAG, "Video title: ${streamInfo.name}")
            Log.d(TAG, "Duration: ${streamInfo.duration} seconds")
            Log.d(TAG, "Audio streams: ${streamInfo.audioStreams.size}")
            
            // Extract audio streams
            val audioStreams = streamInfo.audioStreams.map { stream ->
                // NewPipe streams can have bitrate in different fields
                val bitrate = stream.averageBitrate.takeIf { it > 0 } ?: stream.bitrate ?: 0
                val formatName = stream.format?.name ?: stream.format?.suffix ?: "unknown"
                
                Log.d(TAG, "Audio stream - Format: $formatName, Bitrate: $bitrate, AvgBitrate: ${stream.averageBitrate}")
                
                AudioStreamInfo(
                    url = stream.content,
                    format = formatName,
                    averageBitrate = bitrate,
                    quality = if (bitrate > 0) "${bitrate / 1000}kbps" else "Unknown quality"
                )
            }
            
            // Note: NewPipe-KMP v1.0 doesn't expose direct video stream URLs
            // Only HLS/DASH manifests are available, which require special players
            // Direct video file downloads are not supported in this version
            val videoStreams = mutableListOf<VideoStreamInfo>()
            
            Log.d(TAG, "Note: NewPipe-KMP doesn't provide direct video stream URLs")
            Log.d(TAG, "Only audio streams available: ${audioStreams.size}")
            
            // Get best thumbnail
            val thumbnailUrl = streamInfo.thumbnails.firstOrNull()?.url ?: ""
            
            val metadata = VideoMetadata(
                title = streamInfo.name ?: "Unknown",
                uploader = streamInfo.uploaderName,
                duration = streamInfo.duration,
                thumbnailUrl = thumbnailUrl,
                viewCount = 0, // KMP version doesn't expose viewCount easily
                audioStreams = audioStreams,
                videoStreams = videoStreams
            )
            
            Result.success(metadata)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract video info", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get the best audio stream (highest bitrate)
     */
    fun getBestAudioStream(metadata: VideoMetadata): AudioStreamInfo? {
        return metadata.audioStreams.maxByOrNull { it.averageBitrate }
    }
    
    /**
     * Get the best video stream (highest resolution and non-HLS/DASH format)
     * Note: NewPipe-KMP v1.0 doesn't expose direct video streams, only audio
     */
    fun getBestVideoStream(metadata: VideoMetadata): VideoStreamInfo? {
        if (metadata.videoStreams.isEmpty()) {
            Log.d(TAG, "No video streams available - NewPipe-KMP limitation")
            return null
        }
        
        // All streams will be empty as NewPipe-KMP doesn't expose video streams
        return null
    }
    
    /**
     * Get the best video stream for a given resolution preference
     */
    fun getBestVideoStream(
        metadata: VideoMetadata, 
        preferredResolution: String = "720p"
    ): VideoStreamInfo? {
        // Try to find preferred resolution
        val preferred = metadata.videoStreams.find { 
            it.resolution.contains(preferredResolution, ignoreCase = true) 
        }
        
        if (preferred != null) return preferred
        
        // Fallback to highest resolution
        return metadata.videoStreams.maxByOrNull { stream ->
            stream.resolution.filter { it.isDigit() }.toIntOrNull() ?: 0
        }
    }
    
    /**
     * Format duration in seconds to MM:SS or HH:MM:SS
     */
    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%d:%02d", minutes, secs)
        }
    }
    
    /**
     * Format view count to human-readable format (e.g., 1.2M views)
     */
    fun formatViewCount(count: Long): String {
        return when {
            count >= 1_000_000_000 -> String.format("%.1fB views", count / 1_000_000_000.0)
            count >= 1_000_000 -> String.format("%.1fM views", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK views", count / 1_000.0)
            else -> "$count views"
        }
    }
}
