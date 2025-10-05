package it.atraj.habittracker.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * Prevents multiple rapid clicks by ignoring clicks within the debounce time window.
 * Default debounce time is 500ms to prevent double-clicks on navigation actions.
 */
fun Modifier.clickableOnce(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    debounceTime: Long = 500L,
    onClick: () -> Unit
): Modifier = composed {
    val clickFlow = remember { 
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        ) 
    }
    
    LaunchedEffect(Unit) {
        clickFlow.collectLatest {
            onClick()
            kotlinx.coroutines.delay(debounceTime)
        }
    }
    
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        onClick = { clickFlow.tryEmit(Unit) }
    )
}

/**
 * Prevents multiple rapid clicks with custom interaction source.
 */
fun Modifier.clickableOnce(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource,
    debounceTime: Long = 500L,
    onClick: () -> Unit
): Modifier = composed {
    val clickFlow = remember { 
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        ) 
    }
    
    LaunchedEffect(Unit) {
        clickFlow.collectLatest {
            onClick()
            kotlinx.coroutines.delay(debounceTime)
        }
    }
    
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        onClick = { clickFlow.tryEmit(Unit) }
    )
}

/**
 * Composable function wrapper for handling single-click navigation.
 * Prevents navigation from being triggered multiple times.
 */
@Composable
fun rememberNavigationHandler(
    debounceTime: Long = 500L,
    action: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    return remember {
        {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= debounceTime) {
                lastClickTime = currentTime
                action()
            }
        }
    }
}

/**
 * Composable function for handling button clicks with throttling.
 * Use this for non-navigation actions that should not be spammed.
 */
@Composable
fun rememberThrottledAction(
    throttleTime: Long = 300L,
    action: () -> Unit
): () -> Unit {
    var lastActionTime by remember { mutableLongStateOf(0L) }
    
    return remember {
        {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastActionTime >= throttleTime) {
                lastActionTime = currentTime
                action()
            }
        }
    }
}
