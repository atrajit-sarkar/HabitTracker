package com.example.habittracker.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Manages app updates from GitHub releases
 */
class UpdateManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UpdateManager"
        private const val GITHUB_REPO_OWNER = "atrajit-sarkar"
        private const val GITHUB_REPO_NAME = "HabitTracker"
        private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest"
        private const val PREFS_NAME = "update_prefs"
        private const val KEY_LAST_CHECK = "last_check_time"
        private const val KEY_SKIPPED_VERSION = "skipped_version"
        private const val CHECK_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Check if update check should be performed
     */
    fun shouldCheckForUpdate(): Boolean {
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
        val now = System.currentTimeMillis()
        return (now - lastCheck) > CHECK_INTERVAL
    }
    
    /**
     * Get current app version
     */
    fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Get current version code
     */
    fun getCurrentVersionCode(): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Check for updates from GitHub
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for updates from GitHub...")
            
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to check for updates: ${response.code}")
                return@withContext null
            }
            
            val jsonResponse = response.body?.string() ?: return@withContext null
            val releaseJson = JSONObject(jsonResponse)
            
            // Parse release info
            val latestVersion = releaseJson.optString("tag_name", "").removePrefix("v")
            val releaseNotes = releaseJson.optString("body", "")
            val releaseName = releaseJson.optString("name", "")
            val publishedAt = releaseJson.optString("published_at", "")
            val isPrerelease = releaseJson.optBoolean("prerelease", false)
            
            // Find APK download URL
            val assetsArray = releaseJson.optJSONArray("assets")
            var downloadUrl: String? = null
            var fileSize: Long = 0
            
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        fileSize = asset.optLong("size", 0)
                        break
                    }
                }
            }
            
            if (downloadUrl == null) {
                Log.e(TAG, "No APK found in release")
                return@withContext null
            }
            
            // Update last check time
            prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
            
            val currentVersion = getCurrentVersion()
            val isUpdateAvailable = compareVersions(latestVersion, currentVersion) > 0
            
            if (isUpdateAvailable) {
                Log.d(TAG, "Update available: $latestVersion (current: $currentVersion)")
            } else {
                Log.d(TAG, "App is up to date: $currentVersion")
            }
            
            UpdateInfo(
                latestVersion = latestVersion,
                currentVersion = currentVersion,
                releaseNotes = releaseNotes,
                releaseName = releaseName,
                downloadUrl = downloadUrl,
                fileSize = fileSize,
                publishedAt = publishedAt,
                isPrerelease = isPrerelease,
                isUpdateAvailable = isUpdateAvailable
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            null
        }
    }
    
    /**
     * Compare version strings (e.g., "1.2.3" vs "1.2.4")
     * Returns: -1 if v1 < v2, 0 if equal, 1 if v1 > v2
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").mapNotNull { it.toIntOrNull() }
        val parts2 = v2.split(".").mapNotNull { it.toIntOrNull() }
        
        val maxLength = maxOf(parts1.size, parts2.size)
        
        for (i in 0 until maxLength) {
            val part1 = parts1.getOrNull(i) ?: 0
            val part2 = parts2.getOrNull(i) ?: 0
            
            when {
                part1 < part2 -> return -1
                part1 > part2 -> return 1
            }
        }
        
        return 0
    }
    
    /**
     * Download and install APK
     */
    suspend fun downloadAndInstall(
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading APK from: $downloadUrl")
            
            val request = Request.Builder()
                .url(downloadUrl)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed: ${response.code}"))
            }
            
            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val contentLength = body.contentLength()
            
            // Create temp file
            val apkFile = File(context.cacheDir, "HabitTracker_update.apk")
            if (apkFile.exists()) apkFile.delete()
            
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(apkFile)
            
            var totalBytesRead = 0L
            val buffer = ByteArray(8192)
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                
                if (contentLength > 0) {
                    val progress = ((totalBytesRead * 100) / contentLength).toInt()
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
            }
            
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            
            Log.d(TAG, "APK downloaded successfully: ${apkFile.absolutePath}")
            Result.success(apkFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK", e)
            Result.failure(e)
        }
    }
    
    /**
     * Install downloaded APK
     */
    fun installApk(apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK", e)
        }
    }
    
    /**
     * Open GitHub releases page
     */
    fun openReleasesPage() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://github.com/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening releases page", e)
        }
    }
    
    /**
     * Mark version as skipped
     */
    fun skipVersion(version: String) {
        prefs.edit().putString(KEY_SKIPPED_VERSION, version).apply()
    }
    
    /**
     * Check if version was skipped
     */
    fun isVersionSkipped(version: String): Boolean {
        return prefs.getString(KEY_SKIPPED_VERSION, "") == version
    }
    
    /**
     * Clear skipped version
     */
    fun clearSkippedVersion() {
        prefs.edit().remove(KEY_SKIPPED_VERSION).apply()
    }
}

/**
 * Data class representing update information
 */
data class UpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val releaseNotes: String,
    val releaseName: String,
    val downloadUrl: String,
    val fileSize: Long,
    val publishedAt: String,
    val isPrerelease: Boolean,
    val isUpdateAvailable: Boolean
) {
    fun getFormattedFileSize(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            else -> String.format("%.2f MB", fileSize / (1024.0 * 1024.0))
        }
    }
}
