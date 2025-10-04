package com.example.habittracker.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance manager that adapts app behavior based on device capabilities
 */
@Singleton
class PerformanceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    enum class PerformanceTier {
        HIGH,    // Modern devices with good specs
        MEDIUM,  // Mid-range devices
        LOW      // Older/low-end devices (Android 11 and older, or low RAM)
    }
    
    val performanceTier: PerformanceTier by lazy {
        calculatePerformanceTier()
    }
    
    private fun calculatePerformanceTier(): PerformanceTier {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRamGb = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val sdkVersion = Build.VERSION.SDK_INT
        
        return when {
            // Android 11 or older, or less than 4GB RAM = LOW
            sdkVersion <= Build.VERSION_CODES.R || totalRamGb < 4.0 -> PerformanceTier.LOW
            
            // Android 12-13 with 4-6GB RAM = MEDIUM
            sdkVersion <= Build.VERSION_CODES.TIRAMISU && totalRamGb < 6.0 -> PerformanceTier.MEDIUM
            
            // Android 14+ with 6GB+ RAM = HIGH
            else -> PerformanceTier.HIGH
        }
    }
    
    // Adaptive animation settings
    fun getAnimationSpeed(): Float = when (performanceTier) {
        PerformanceTier.HIGH -> 2f
        PerformanceTier.MEDIUM -> 1.5f
        PerformanceTier.LOW -> 1f
    }
    
    fun getAnimationSize(): Int = when (performanceTier) {
        PerformanceTier.HIGH -> 300
        PerformanceTier.MEDIUM -> 250
        PerformanceTier.LOW -> 200
    }
    
    // Pagination settings for lazy loading
    fun getInitialLoadSize(): Int = when (performanceTier) {
        PerformanceTier.HIGH -> 20
        PerformanceTier.MEDIUM -> 15
        PerformanceTier.LOW -> 10
    }
    
    fun getPageSize(): Int = when (performanceTier) {
        PerformanceTier.HIGH -> 15
        PerformanceTier.MEDIUM -> 10
        PerformanceTier.LOW -> 5
    }
    
    // Image loading quality
    fun shouldUseHighQualityImages(): Boolean = performanceTier == PerformanceTier.HIGH
    
    // Enable/disable expensive animations
    fun shouldEnableParticleEffects(): Boolean = performanceTier != PerformanceTier.LOW
    
    fun shouldEnableBlurEffects(): Boolean = performanceTier == PerformanceTier.HIGH
    
    // Logging for debugging
    fun logPerformanceInfo() {
        android.util.Log.d("PerformanceManager", """
            Performance Tier: $performanceTier
            Android Version: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Animation Speed: ${getAnimationSpeed()}x
            Animation Size: ${getAnimationSize()}dp
            Initial Load Size: ${getInitialLoadSize()} items
            Page Size: ${getPageSize()} items
        """.trimIndent())
    }
}
