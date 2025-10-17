package it.atraj.habittracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import it.atraj.habittracker.service.OverdueHabitIconManager
import javax.inject.Inject

@AndroidEntryPoint
class HabitCompletionReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var iconManager: OverdueHabitIconManager
    
    companion object {
        private const val TAG = "HabitCompletionReceiver"
        const val ACTION_HABIT_COMPLETED = "it.atraj.habittracker.HABIT_COMPLETED"
        
        fun sendHabitCompletedBroadcast(context: Context) {
            val intent = Intent(ACTION_HABIT_COMPLETED)
            context.sendBroadcast(intent)
            Log.d(TAG, "Sent habit completed broadcast")
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_HABIT_COMPLETED) {
            Log.d(TAG, "Received habit completed broadcast, updating icon with cache clear")
            iconManager.onHabitCompleted()
        }
    }
}