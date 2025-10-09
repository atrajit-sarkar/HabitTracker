package it.atraj.habittracker.image

import android.content.Context
import android.graphics.Bitmap
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import it.atraj.habittracker.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimized ImageLoader configuration with WebP support and caching
 */
@Singleton
class OptimizedImageLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.30) // Increased to 30% for better caching
                    .strongReferencesEnabled(true) // Keep strong references
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // Fixed 50MB for avatar images
                    .build()
            }
            // AGGRESSIVE caching for smooth scrolling
            .respectCacheHeaders(false) // Always use cache
            .crossfade(150) // Faster crossfade
            // Use ARGB_8888 for better quality with custom images
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .allowHardware(true) // Use hardware bitmaps for performance
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}
