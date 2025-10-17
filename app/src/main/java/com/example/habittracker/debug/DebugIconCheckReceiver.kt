package it.atraj.habittracker.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import it.atraj.habittracker.service.OverdueHabitIconManager
import javax.inject.Inject

/**
 * Temporary debug receiver - triggers a forced overdue icon check when a specific
 * broadcast action is sent via adb. Not intended for production long-term.
 */
@AndroidEntryPoint
class DebugIconCheckReceiver : BroadcastReceiver() {
    @Inject
    lateinit var iconManager: OverdueHabitIconManager

    companion object {
        const val TAG = "DebugIconCheckReceiver"
        const val ACTION_FORCE_ICON_CHECK = "it.atraj.habittracker.ACTION_FORCE_ICON_CHECK"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            Log.d(TAG, "Received debug force icon check broadcast")
            iconManager.forceCheckAndUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to run forced icon check", e)
        }
    }
}
