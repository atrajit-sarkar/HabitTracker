package it.atraj.habittracker.music.ui

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.atraj.habittracker.auth.ui.AuthViewModel
import it.atraj.habittracker.auth.ui.MusicPlayerScreen
import it.atraj.habittracker.auth.ui.MusicTrackData
import it.atraj.habittracker.auth.ui.toMusicTrackData
import it.atraj.habittracker.music.BackgroundMusicManager
import it.atraj.habittracker.music.MusicDownloadManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Screen to display songs in a category
 * Full-featured UI with music controls, enable/disable toggle, and volume control
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSongListScreen(
    categoryPath: String,
    categoryName: String,
    viewModel: MusicBrowserViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    musicManager: BackgroundMusicManager?,
    downloadManager: MusicDownloadManager?,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var showMusicPlayer by remember { mutableStateOf(false) }
    var selectedMusicPlayerTrack by remember { mutableStateOf<MusicTrackData?>(null) }
    
    // Music controls state
    var enabled by remember { mutableStateOf(authState.user?.musicEnabled ?: false) }
    var selectedTrack by remember { mutableStateOf(authState.user?.musicTrack ?: "NONE") }
    var volume by remember { mutableFloatStateOf(authState.user?.musicVolume ?: 0.3f) }
    var isUserAdjustingVolume by remember { mutableStateOf(false) }
    
    val downloadStates = remember { mutableStateMapOf<String, Pair<Boolean, Int>>() }
    val deletingStates = remember { mutableStateMapOf<String, Boolean>() }
    val deletedFiles = remember { mutableStateSetOf<String>() }
    val scope = rememberCoroutineScope()
    var saveJob by remember { mutableStateOf<Job?>(null) }
    
    // Sync state with user data (one-way sync only - don't trigger music changes here)
    LaunchedEffect(authState.user?.musicEnabled, authState.user?.musicTrack, authState.user?.musicVolume) {
        authState.user?.let { user ->
            enabled = user.musicEnabled
            selectedTrack = user.musicTrack
            if (!isUserAdjustingVolume) {
                volume = user.musicVolume
            }
        }
    }
    
    // Cleanup when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            saveJob?.cancel()
            isUserAdjustingVolume = false
        }
    }
    
    // Load songs
    LaunchedEffect(categoryPath) {
        viewModel.loadSongsInCategory(categoryPath)
    }
    
    // Convert songs to track data
    val tracks = remember(state.currentSongs) {
        state.currentSongs.map { it.toMusicTrackData() }
    }
    
    // Function to handle music changes (only called when user interacts)
    fun handleMusicChange(newEnabled: Boolean, newTrack: String, newVolume: Float) {
        authViewModel.updateMusicSettings(newEnabled, newTrack, newVolume)
        
        Log.d("MusicSongList", "User changed settings - enabled: $newEnabled, track: $newTrack, volume: $newVolume")
        
        // Apply immediately to music manager
        musicManager?.let { manager ->
            manager.setEnabled(newEnabled)
            manager.setVolume(newVolume)
            
            if (newTrack == "NONE") {
                manager.stopMusic()
                manager.changeSong(BackgroundMusicManager.MusicTrack.NONE)
                Log.d("MusicSongList", "Changed to NONE track - music stopped")
            } else {
                val enumTrack = try {
                    BackgroundMusicManager.MusicTrack.valueOf(newTrack)
                } catch (e: Exception) {
                    null
                }
                
                if (enumTrack != null) {
                    manager.changeSong(enumTrack)
                    Log.d("MusicSongList", "Playing enum track: ${enumTrack.name}")
                } else {
                    // For dynamic tracks
                    val selectedMetadata = state.currentSongs.find { it.id == newTrack }
                    if (selectedMetadata != null) {
                        val isDownloaded = downloadManager?.isMusicDownloaded(selectedMetadata.filename) ?: false
                        if (isDownloaded) {
                            manager.playDynamicTrack(selectedMetadata.filename)
                            Log.d("MusicSongList", "Playing dynamic track: ${selectedMetadata.filename}")
                        } else {
                            manager.changeSong(BackgroundMusicManager.MusicTrack.NONE)
                        }
                    }
                }
            }
        }
    }
    
    // Volume auto-save with debounce
    LaunchedEffect(volume) {
        if (authState.user != null && isUserAdjustingVolume) {
            saveJob?.cancel()
            saveJob = scope.launch {
                delay(500)
                authViewModel.updateMusicSettings(enabled, selectedTrack, volume)
                musicManager?.setVolume(volume)
                isUserAdjustingVolume = false
                Log.d("MusicSongList", "Volume saved: $volume")
            }
        }
    }
    
    // Animated background
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )
    
    val enabledScale by animateFloatAsState(
        targetValue = if (enabled) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "enabledScale"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            categoryName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "${tracks.size} songs available",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f + 0.1f * gradientOffset),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadSongsInCategory(categoryPath) }) {
                            Text("Retry")
                        }
                    }
                }
                
                tracks.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No songs in this category",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Music Controls Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Enable/Disable Toggle
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (enabled)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 4.dp else 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (enabled)
                                                        Brush.linearGradient(
                                                            colors = listOf(
                                                                MaterialTheme.colorScheme.primary,
                                                                MaterialTheme.colorScheme.tertiary
                                                            )
                                                        )
                                                    else
                                                        Brush.linearGradient(
                                                            colors = listOf(
                                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                                            )
                                                        )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (enabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                                tint = Color.White
                                            )
                                        }
                                        
                                        Column {
                                            Text(
                                                text = "Background Music",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (enabled)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (enabled)
                                                                MaterialTheme.colorScheme.primary
                                                            else
                                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                        )
                                                        .scale(if (enabled) enabledScale else 1f)
                                                )
                                                Text(
                                                    text = if (enabled) "Playing" else "Disabled",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (enabled)
                                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                    else
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                    
                                    Switch(
                                        checked = enabled,
                                        onCheckedChange = { 
                                            enabled = it
                                            handleMusicChange(it, selectedTrack, volume)
                                        }
                                    )
                                }
                            }
                            
                            // Volume Control
                            AnimatedVisibility(
                                visible = enabled,
                                enter = fadeIn(tween(300)) + expandVertically(),
                                exit = fadeOut(tween(200)) + shrinkVertically()
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.linearGradient(
                                                                colors = listOf(
                                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                                                )
                                                            )
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = when {
                                                            volume == 0f -> Icons.Default.VolumeOff
                                                            volume < 0.5f -> Icons.Default.VolumeDown
                                                            else -> Icons.Default.VolumeUp
                                                        },
                                                        contentDescription = "Volume",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                
                                                Column {
                                                    Text(
                                                        text = "Volume",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = when {
                                                            volume == 0f -> "Muted"
                                                            volume < 0.3f -> "Low"
                                                            volume < 0.7f -> "Medium"
                                                            else -> "High"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                            
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "${(volume * 100).toInt()}%",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                        
                                        Slider(
                                            value = volume,
                                            onValueChange = {
                                                volume = it
                                                isUserAdjustingVolume = true
                                                musicManager?.setVolume(it)
                                            },
                                            valueRange = 0f..1f,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Songs Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(600.dp)
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(tracks, key = { it.id }) { track ->
                                val isDownloaded = track.fileName.isEmpty() ||
                                    (!deletedFiles.contains(track.fileName) &&
                                     downloadManager?.isMusicDownloaded(track.fileName) == true)
                                
                                CompactMusicCard(
                                    track = track,
                                    isSelected = selectedTrack == track.id,
                                    isDownloaded = isDownloaded,
                                    downloadState = downloadStates[track.fileName] ?: (false to 0),
                                    isDeleting = deletingStates[track.fileName] ?: false,
                                    onSelect = {
                                        if (track.id == "NONE" || isDownloaded) {
                                            selectedTrack = track.id
                                            val newEnabled = if (!enabled) true else enabled
                                            enabled = newEnabled
                                            handleMusicChange(newEnabled, track.id, volume)
                                        }
                                    },
                                    onPlayerClick = {
                                        if (track.id != "NONE" && isDownloaded) {
                                            selectedMusicPlayerTrack = track
                                            showMusicPlayer = true
                                        }
                                    },
                                    onDownload = {
                                        deletedFiles.remove(track.fileName)
                                        downloadStates[track.fileName] = true to 0
                                        scope.launch {
                                            downloadManager?.downloadMusic(track.fileName) { progress ->
                                                downloadStates[track.fileName] = true to progress
                                            }?.onSuccess {
                                                downloadStates[track.fileName] = false to 100
                                            }?.onFailure {
                                                downloadStates.remove(track.fileName)
                                            }
                                        }
                                    },
                                    onDelete = {
                                        scope.launch {
                                            deletingStates[track.fileName] = true
                                            try {
                                                downloadManager?.deleteMusicFile(track.fileName)
                                                deletedFiles.add(track.fileName)
                                                downloadStates.remove(track.fileName)
                                                if (selectedTrack == track.id) {
                                                    selectedTrack = "NONE"
                                                    handleMusicChange(enabled, "NONE", volume)
                                                }
                                            } finally {
                                                deletingStates.remove(track.fileName)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Music Player Overlay
        if (showMusicPlayer && selectedMusicPlayerTrack != null) {
            MusicPlayerScreen(
                track = selectedMusicPlayerTrack!!,
                isPlaying = enabled && selectedTrack == selectedMusicPlayerTrack!!.id,
                currentVolume = volume,
                musicManager = musicManager,
                onBackClick = { showMusicPlayer = false },
                onVolumeChange = { newVolume -> 
                    volume = newVolume
                    isUserAdjustingVolume = true
                },
                onPlayPauseClick = { 
                    selectedTrack = selectedMusicPlayerTrack!!.id
                    val newEnabled = if (!enabled) true else !enabled
                    enabled = newEnabled
                    handleMusicChange(newEnabled, selectedMusicPlayerTrack!!.id, volume)
                },
                allTracks = tracks,
                onTrackChange = { newTrack ->
                    selectedMusicPlayerTrack = newTrack
                    selectedTrack = newTrack.id
                    handleMusicChange(enabled, newTrack.id, volume)
                }
            )
        }
    }
}

@Composable
private fun CompactMusicCard(
    track: MusicTrackData,
    isSelected: Boolean,
    isDownloaded: Boolean,
    downloadState: Pair<Boolean, Int>,
    isDeleting: Boolean,
    onSelect: () -> Unit,
    onPlayerClick: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val (isDownloading, progress) = downloadState
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Enhanced animations
    val scale by animateFloatAsState(
        targetValue = when {
            isDownloading && progress == 0 -> 1.02f
            isSelected -> 0.98f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    val elevation by animateDpAsState(
        targetValue = when {
            isDownloading -> 8.dp
            isSelected -> 12.dp
            else -> 3.dp
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_elevation"
    )

    // Shimmer and pulse effects for downloading
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    // Pulsing scale for initiating download
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = track.id == "NONE" || isDownloaded) { onSelect() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Album art / Icon area - Enhanced
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glow effect for selected
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .alpha(0.3f)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    // Background gradient with shimmer and pulse
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .scale(if (isDownloading && progress == 0) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                if (isDownloading) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = shimmerAlpha),
                                            MaterialTheme.colorScheme.primary.copy(alpha = shimmerAlpha)
                                        )
                                    )
                                } else if (isSelected) {
                                    Brush.sweepGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary,
                                            MaterialTheme.colorScheme.tertiary,
                                            MaterialTheme.colorScheme.primary
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else if (isDownloading) {
                            Box(contentAlignment = Alignment.Center) {
                                AnimatedContent(
                                    targetState = progress == 0,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(300)) togetherWith
                                        fadeOut(animationSpec = tween(300))
                                    },
                                    label = "download_progress"
                                ) { isInitiating ->
                                    if (isInitiating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(34.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(
                                                progress = { progress / 100f },
                                                modifier = Modifier.size(34.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "$progress%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 8.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Icon(
                                imageVector = if (track.id == "NONE")
                                    Icons.Default.MusicOff
                                else
                                    Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                        }
                    }

                    // Category badge
                    if (track.category.isNotEmpty() && track.id != "NONE") {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .alpha(0.9f),
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = track.category,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1
                            )
                        }
                    }

                    // Selected indicator
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Track info - Compact
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp,
                        color = if (track.id == "NONE" || isDownloaded)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    if (track.artist.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Status text
                    if (!isDownloading && !isDeleting && !isDownloaded && track.fileName.isNotEmpty()) {
                        Text(
                            text = "Tap to download",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (track.fileName.isNotEmpty()) {
                        if (isDownloaded && !isDeleting) {
                            // Player button
                            IconButton(
                                onClick = onPlayerClick,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Open player",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Delete button
                            IconButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (!isDownloading && !isDeleting) {
                            // Download button - Compact
                            FilledTonalButton(
                                onClick = onDownload,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp),
                                shape = RoundedCornerShape(7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Download",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Delete Track?")
            },
            text = {
                Text("Delete \"${track.name}\" from your device? You can download it again later.")
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
