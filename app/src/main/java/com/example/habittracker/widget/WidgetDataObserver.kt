package it.atraj.habittracker.widget

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import it.atraj.habittracker.data.HabitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes habit data changes and automatically updates the widget
 * This ensures the widget always reflects the current state without manual triggers
 */
@Singleton
class WidgetDataObserver @Inject constructor(
    private val habitRepository: HabitRepository,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isObserving = false
    
    companion object {
        private const val TAG = "WidgetDataObserver"
    }
    
    /**
     * Start observing habit data changes and update widget when needed
     */
    fun startObserving() {
        if (isObserving) {
            Log.d(TAG, "Already observing habit data")
            return
        }
        
        isObserving = true
        Log.d(TAG, "Starting to observe habit data for widget updates")
        
        scope.launch {
            // Observe all habits and trigger widget update when data changes
            habitRepository.observeHabits()
                .map { habits ->
                    // Create a signature of the current state to detect meaningful changes
                    habits.filter { !it.isDeleted }
                        .map { habit ->
                            // Track: habitId, lastCompletedDate, reminderEnabled, deleted status
                            "${habit.id}_${habit.lastCompletedDate}_${habit.reminderEnabled}"
                        }
                        .sorted()
                        .joinToString("|")
                }
                .distinctUntilChanged() // Only trigger when the signature actually changes
                .collect { signature ->
                    Log.d(TAG, "Habit data changed, updating widget (signature: ${signature.take(50)}...)")
                    HabitWidgetProvider.requestUpdate(context)
                }
        }
    }
    
    /**
     * Stop observing habit data changes
     */
    fun stopObserving() {
        // Note: Since we're using a SupervisorJob, we don't cancel it
        // The observation will continue for the lifetime of the app
        isObserving = false
        Log.d(TAG, "Stopped observing habit data")
    }
}
