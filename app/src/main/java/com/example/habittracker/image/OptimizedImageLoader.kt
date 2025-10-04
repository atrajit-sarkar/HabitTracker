package com.example.habittracker.image

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.example.habittracker.BuildConfig
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
            .components {
                // Add GIF/Animated WebP decoder
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Use 25% of available RAM
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // Use 2% of available disk
                    .build()
            }
            // Enable aggressive caching for offline-first experience
            .respectCacheHeaders(false)
            .crossfade(true)
            .crossfade(300)
            // Bitmap configuration for quality vs memory
            .bitmapConfig(Bitmap.Config.RGB_565) // Lower memory usage
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}
