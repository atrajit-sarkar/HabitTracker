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
                    .maxSizeBytes(100 * 1024 * 1024) // Increased to 100MB for post images
                    .build()
            }
            // Network retry configuration for GitHub raw URLs
            .okHttpClient {
                okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .addInterceptor { chain ->
                        var request = chain.request()
                        var response = chain.proceed(request)
                        var tryCount = 0
                        
                        // Retry up to 3 times on failure
                        while (!response.isSuccessful && tryCount < 3) {
                            tryCount++
                            Thread.sleep(1000L * tryCount) // Progressive backoff
                            response.close()
                            response = chain.proceed(request)
                        }
                        
                        response
                    }
                    .build()
            }
            // AGGRESSIVE caching for smooth scrolling
            .respectCacheHeaders(false) // Always use cache, ignore GitHub headers
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
