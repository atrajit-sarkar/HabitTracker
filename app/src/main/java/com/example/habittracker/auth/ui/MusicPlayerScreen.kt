package it.atraj.habittracker.auth.ui

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.atraj.habittracker.music.BackgroundMusicManager
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    track: MusicTrackData,
    isPlaying: Boolean,
    currentVolume: Float,
    musicManager: BackgroundMusicManager?,
    onBackClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPlayPauseClick: () -> Unit
) {
    var showVolumeControl by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(100f) }
    var isUserSeeking by remember { mutableStateOf(false) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "music_animation")
    
    // Update progress periodically when playing
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(500)
            if (!isUserSeeking) {
                musicManager?.let { manager ->
                    try {
                        val mediaPlayer = manager.javaClass.getDeclaredField("mediaPlayer").apply {
                            isAccessible = true
                        }.get(manager) as? android.media.MediaPlayer
                        
                        mediaPlayer?.let {
                            if (it.isPlaying) {
                                currentPosition = it.currentPosition.toFloat()
                                duration = it.duration.toFloat().coerceAtLeast(1f)
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore if we can't access the player
                    }
                }
            }
        }
    }
    
    // Pulsating animation for playing state
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Rotation animation for vinyl effect
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Now Playing", 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Animated gradient background
            AnimatedGradientBackground(isPlaying)
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                
                // Album art / Music visual
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glow effect
                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.95f)
                                .scale(scale)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                    
                    // Main album art card
                    Card(
                        modifier = Modifier
                            .fillMaxSize(0.9f)
                            .scale(if (isPlaying) scale else 1f),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Music icon with gradient
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp),
                                    tint = Color.White
                                )
                            }
                            
                            // Category badge
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = track.category,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
                
                // Waveform visualization
                AnimatedWaveform(isPlaying)
                
                // Track info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    if (track.artist.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress bar with timestamps - Professional Design
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Progress slider with custom styling
                        Slider(
                            value = if (duration > 0) currentPosition else 0f,
                            onValueChange = { newValue ->
                                isUserSeeking = true
                                currentPosition = newValue
                            },
                            onValueChangeFinished = {
                                isUserSeeking = false
                                musicManager?.let { manager ->
                                    try {
                                        val mediaPlayer = manager.javaClass.getDeclaredField("mediaPlayer").apply {
                                            isAccessible = true
                                        }.get(manager) as? android.media.MediaPlayer
                                        
                                        mediaPlayer?.seekTo(currentPosition.toInt())
                                    } catch (e: Exception) {
                                        Log.e("MusicPlayer", "Failed to seek", e)
                                    }
                                }
                            },
                            valueRange = 0f..duration.coerceAtLeast(1f),
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            thumb = {
                                // Custom thumb with larger size
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary
                                                )
                                            )
                                        )
                                        .then(
                                            if (isUserSeeking) {
                                                Modifier.scale(1.3f)
                                            } else {
                                                Modifier
                                            }
                                        )
                                )
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Time display with better styling
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Current time with icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isPlaying) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        )
                                )
                                Text(
                                    text = formatTime(currentPosition.toLong()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Duration
                            Text(
                                text = formatTime(duration.toLong()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Volume control overlay
                AnimatedVisibility(
                    visible = showVolumeControl,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Close button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Volume Control",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { showVolumeControl = false }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Large volume icon
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        currentVolume == 0f -> Icons.Default.VolumeOff
                                        currentVolume < 0.5f -> Icons.Default.VolumeDown
                                        else -> Icons.Default.VolumeUp
                                    },
                                    contentDescription = "Volume",
                                    modifier = Modifier.size(50.dp),
                                    tint = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            // Volume percentage display
                            Text(
                                text = "${(currentVolume * 100).toInt()}%",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Volume slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Slider(
                                    value = currentVolume,
                                    onValueChange = onVolumeChange,
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                                
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                // Control buttons (hide when volume control is open)
                AnimatedVisibility(
                    visible = !showVolumeControl,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Volume button
                        FilledTonalIconButton(
                            onClick = { showVolumeControl = true },
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = when {
                                    currentVolume == 0f -> Icons.Default.VolumeOff
                                    currentVolume < 0.5f -> Icons.Default.VolumeDown
                                    else -> Icons.Default.VolumeUp
                                },
                                contentDescription = "Volume",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        
                        // Play/Pause button
                        FloatingActionButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.size(80.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 12.dp
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        // Info button
                        FilledTonalIconButton(
                            onClick = { /* Show track info */ },
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Track info",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                // Bottom spacer for scrolling
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AnimatedGradientBackground(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (isPlaying) 0.1f else 0.05f)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f * offset),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f * (1 - offset)),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    )
                )
            )
    )
}

@Composable
fun AnimatedWaveform(isPlaying: Boolean) {
    val barCount = 40
    val bars = remember { List(barCount) { Random.nextFloat() } }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        bars.forEachIndexed { index, initialHeight ->
            WaveformBar(
                index = index,
                isPlaying = isPlaying,
                initialHeight = initialHeight
            )
        }
    }
}

@Composable
fun WaveformBar(
    index: Int,
    isPlaying: Boolean,
    initialHeight: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bar_$index")
    
    // Each bar has a slightly different phase for wave effect
    val height by infiniteTransition.animateFloat(
        initialValue = initialHeight,
        targetValue = if (isPlaying) Random.nextFloat() else 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 300 + (index * 10),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "height_$index"
    )
    
    val actualHeight = if (isPlaying) height else 0.1f
    
    Box(
        modifier = Modifier
            .width(3.dp)
            .fillMaxHeight(actualHeight.coerceIn(0.1f, 1f))
            .clip(RoundedCornerShape(2.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
    )
}

/**
 * Format milliseconds to MM:SS format
 */
fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
