package com.example.habittracker.notification

import android.app.AlertDialog
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationReliabilityHelper {

    private const val ALARM_VERIFICATION_WORK = "alarm_verification_work"
    private const val BATTERY_OPTIMIZATION_PREF = "battery_optimization_requested"
    private const val EXACT_ALARM_PREF = "exact_alarm_permission_requested"

    /**
     * Checks if the app is exempt from battery optimization
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Shows a dialog explaining why battery optimization exemption is needed
     * and directs user to settings if they agree
     */
    fun requestBatteryOptimizationExemption(context: Context) {
        if (isIgnoringBatteryOptimizations(context)) {
            Log.d("NotificationReliability", "Already exempt from battery optimization")
            return
        }

        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(BATTERY_OPTIMIZATION_PREF, false)) {
            Log.d("NotificationReliability", "User already declined battery optimization exemption")
            return
        }

        AlertDialog.Builder(context)
            .setTitle("Enable Reliable Notifications")
            .setMessage(
                "To ensure you receive habit reminders on time, even when your phone " +
                "is idle, please disable battery optimization for HabitTracker.\n\n" +
                "This allows the app to wake your device and show reminders at the scheduled time."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                    prefs.edit().putBoolean(BATTERY_OPTIMIZATION_PREF, true).apply()
                } catch (e: Exception) {
                    Log.e("NotificationReliability", "Error opening battery settings: ${e.message}")
                    // Fallback to general battery optimization settings
                    try {
                        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(fallbackIntent)
                    } catch (e2: Exception) {
                        Log.e("NotificationReliability", "Error opening fallback settings: ${e2.message}")
                    }
                }
            }
            .setNegativeButton("Not Now") { _, _ ->
                prefs.edit().putBoolean(BATTERY_OPTIMIZATION_PREF, true).apply()
            }
            .setNeutralButton("Learn More") { _, _ ->
                // Show more detailed explanation
                showDetailedExplanation(context)
            }
            .setCancelable(true)
            .show()
    }

    /**
     * Check if the app currently has permission to schedule exact alarms (Android 12+)
     */
    fun hasExactAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return true
        return alarmManager.canScheduleExactAlarms()
    }

    /**
     * Request exact alarm permission (Android 12+) if not already granted.
     * We only prompt the user once; subsequent calls will no-op if previously declined.
     */
    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (hasExactAlarmPermission(context)) return

        val prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(EXACT_ALARM_PREF, false)) {
            Log.d("NotificationReliability", "User already prompted for exact alarm permission")
            return
        }

        try {
            // Show lightweight rationale dialog first for better UX
            AlertDialog.Builder(context)
                .setTitle("Enable Precise Reminders")
                .setMessage(
                    "To fire reminders exactly on time while your device is idle, " +
                    "please allow 'Exact alarms'. Without it Android may delay notifications." )
                .setPositiveButton("Open Settings") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("NotificationReliability", "Cannot open exact alarm settings: ${e.message}")
                    } finally {
                        prefs.edit().putBoolean(EXACT_ALARM_PREF, true).apply()
                    }
                }
                .setNegativeButton("Not Now") { _, _ ->
                    prefs.edit().putBoolean(EXACT_ALARM_PREF, true).apply()
                }
                .setNeutralButton("Learn More") { _, _ ->
                    AlertDialog.Builder(context)
                        .setTitle("Why Exact Alarms?")
                        .setMessage("Exact alarms let HabitTracker wake up at the precise minute you set. Without it, the system may batch or delay your habit reminders—especially after long idle periods or at night.")
                        .setPositiveButton("Enable Now") { _, _ ->
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("NotificationReliability", "Cannot open exact alarm settings (nested): ${e.message}")
                            } finally {
                                prefs.edit().putBoolean(EXACT_ALARM_PREF, true).apply()
                            }
                        }
                        .setNegativeButton("Close", null)
                        .show()
                }
                .show()
        } catch (e: Exception) {
            Log.e("NotificationReliability", "Error requesting exact alarm permission: ${e.message}", e)
        }
    }

    private fun showDetailedExplanation(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Why This Permission?")
            .setMessage(
                "Android's battery optimization can prevent apps from running in the background " +
                "to save battery life. This is great for most apps, but it can cause HabitTracker " +
                "to miss sending you reminders.\n\n" +
                "By disabling battery optimization for HabitTracker:\n" +
                "✓ You'll receive reminders on time\n" +
                "✓ Reminders work even when phone is idle\n" +
                "✓ Reminders survive phone restarts\n\n" +
                "Battery impact: Minimal - the app only wakes briefly to show notifications."
            )
            .setPositiveButton("Got It") { dialog, _ ->
                dialog.dismiss()
                requestBatteryOptimizationExemption(context)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Sets up periodic WorkManager job to verify alarms every 24 hours
     * This serves as a backup mechanism if AlarmManager fails
     */
    fun setupAlarmVerification(context: Context) {
        try {
            val workRequest = PeriodicWorkRequestBuilder<AlarmVerificationWorker>(
                24, TimeUnit.HOURS,  // Repeat every 24 hours
                30, TimeUnit.MINUTES  // Flex period: can run within 30 min window
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                ALARM_VERIFICATION_WORK,
                ExistingPeriodicWorkPolicy.KEEP,  // Keep existing if already scheduled
                workRequest
            )

            Log.d("NotificationReliability", "Alarm verification work scheduled successfully")
        } catch (e: Exception) {
            Log.e("NotificationReliability", "Error setting up alarm verification: ${e.message}", e)
        }
    }

    /**
     * Cancels the periodic alarm verification work
     */
    fun cancelAlarmVerification(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(ALARM_VERIFICATION_WORK)
            Log.d("NotificationReliability", "Alarm verification work cancelled")
        } catch (e: Exception) {
            Log.e("NotificationReliability", "Error cancelling alarm verification: ${e.message}", e)
        }
    }

    /**
     * Checks if the device is running on a manufacturer known for aggressive battery optimization
     */
    fun isAggressiveBatteryManagement(): Boolean {
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        return manufacturer in listOf("xiaomi", "oppo", "vivo", "huawei", "oneplus", "samsung")
    }

    /**
     * Gets manufacturer-specific instructions for ensuring notifications work
     */
    fun getManufacturerSpecificInstructions(): String? {
        return when (android.os.Build.MANUFACTURER.lowercase()) {
            "xiaomi" -> "Go to Settings → Apps → HabitTracker → Battery saver → No restrictions\nAlso enable 'Autostart'"
            "oppo", "realme" -> "Go to Settings → Battery → HabitTracker → Allow background activity"
            "huawei" -> "Go to Settings → Apps → HabitTracker → Battery → App launch → Manage manually"
            "oneplus" -> "Go to Settings → Battery → Battery optimization → HabitTracker → Don't optimize"
            "samsung" -> "Go to Settings → Apps → HabitTracker → Battery → Allow background activity\nTurn off 'Put app to sleep'"
            "vivo" -> "Go to Settings → Battery → Background power consumption → HabitTracker → Allow"
            else -> null
        }
    }

    /**
     * Shows manufacturer-specific instructions if on an aggressive device
     */
    fun showManufacturerInstructions(context: Context) {
        val instructions = getManufacturerSpecificInstructions() ?: return

        AlertDialog.Builder(context)
            .setTitle("Additional Steps for ${android.os.Build.MANUFACTURER}")
            .setMessage(
                "Your device manufacturer has additional battery saving features that may prevent notifications.\n\n" +
                "Please also complete these steps:\n\n$instructions"
            )
            .setPositiveButton("OK", null)
            .show()
    }
}
