package it.atraj.habittracker.auth.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
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
import it.atraj.habittracker.data.bridge.MusicSystemBridge
import it.atraj.habittracker.data.model.MusicMetadata
import it.atraj.habittracker.music.BackgroundMusicManager
import it.atraj.habittracker.music.MusicDownloadManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

data class MusicTrackData(
    val id: String,
    val name: String,
    val fileName: String,
    val artist: String = "",
    val category: String = "Ambient"
)

/**
 * Format title to Title Case and clean up
 */
fun String.toDisplayTitle(): String {
    return this
        .split(" ")
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
        .take(40) // Limit length
        .trim()
}

/**
 * Convert MusicMetadata to MusicTrackData for UI
 */
fun MusicMetadata.toMusicTrackData(): MusicTrackData {
    // Try to find matching enum by filename for consistent IDs
    val enumTrack = BackgroundMusicManager.MusicTrack.values().find { 
        it.resourceName == this.filename 
    }
    
    // Use enum display name if available, otherwise format the metadata title
    val displayName = enumTrack?.displayName ?: this.title.toDisplayTitle()
    
    return MusicTrackData(
        id = enumTrack?.name ?: this.id, // Use enum name if found, otherwise use dynamic ID
        name = displayName,
        fileName = this.filename,
        artist = this.artist.toDisplayTitle(),
        category = this.category.replaceFirstChar { it.uppercase() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSettingsScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    musicSettingsViewModel: MusicSettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val musicState by musicSettingsViewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? it.atraj.habittracker.MainActivity
    
    var showMusicPlayer by remember { mutableStateOf(false) }
    var selectedMusicPlayerTrack by remember { mutableStateOf<MusicTrackData?>(null) }
    
    val downloadManager = activity?.let {
        try {
            val field = it::class.java.getDeclaredField("downloadManager")
            field.isAccessible = true
            field.get(it) as? MusicDownloadManager
        } catch (e: Exception) {
            null
        }
    }
    
    val musicManager = activity?.let {
        try {
            val field = it::class.java.getDeclaredField("musicManager")
            field.isAccessible = true
            field.get(it) as? BackgroundMusicManager
        } catch (e: Exception) {
            null
        }
    }
    
    var enabled by remember { mutableStateOf(state.user?.musicEnabled ?: false) }
    var selectedTrack by remember { mutableStateOf(state.user?.musicTrack ?: "NONE") }
    var volume by remember { mutableFloatStateOf(state.user?.musicVolume ?: 0.3f) }
    var isUserAdjustingVolume by remember { mutableStateOf(false) }
    
    // Sync state with user data when it changes (e.g., on screen revisit)
    // But don't sync volume when user is actively adjusting it
    LaunchedEffect(state.user?.musicEnabled, state.user?.musicTrack, state.user?.musicVolume) {
        state.user?.let { user ->
            enabled = user.musicEnabled
            selectedTrack = user.musicTrack
            if (!isUserAdjustingVolume) {
                volume = user.musicVolume
            }
        }
    }
    
    val downloadStates = remember { mutableStateMapOf<String, Pair<Boolean, Int>>() }
    val deletingStates = remember { mutableStateMapOf<String, Boolean>() }
    val deletedFiles = remember { mutableStateSetOf<String>() }
    val scope = rememberCoroutineScope()
    var saveJob by remember { mutableStateOf<Job?>(null) }
    
    // Cleanup when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            saveJob?.cancel()
            isUserAdjustingVolume = false
        }
    }
    
    // Build tracks list - "NONE" track + dynamic music from repository
    val tracks = remember(musicState.musicList) {
        val noneTrack = MusicTrackData("NONE", "No Music", "", "", "None")
        val dynamicTracks = musicState.musicList.map { it.toMusicTrackData() }
        listOf(noneTrack) + dynamicTracks
    }
    
    // Show loading indicator when refreshing
    LaunchedEffect(Unit) {
        // Check for updates on screen open
        musicSettingsViewModel.checkForUpdates()
    }
    
            // Auto-save when settings change (enabled and track immediately, volume debounced)
    LaunchedEffect(enabled, selectedTrack) {
        if (state.user != null && !isUserAdjustingVolume) {
            viewModel.updateMusicSettings(enabled, selectedTrack, volume)
            
            Log.d("MusicSettings", "Settings changed - enabled: $enabled, selectedTrack: $selectedTrack, volume: $volume")
            
            // Apply immediately to music manager
            musicManager?.let { manager ->
                manager.setEnabled(enabled)
                manager.setVolume(volume) // Ensure correct volume is set
                
                if (selectedTrack == "NONE") {
                    // Stop any playing music immediately when NONE is selected
                    manager.stopMusic()
                    manager.changeSong(BackgroundMusicManager.MusicTrack.NONE)
                    Log.d("MusicSettings", "Changed to NONE track - music stopped")
                } else {
                    // First try direct enum match
                    val enumTrack = try {
                        BackgroundMusicManager.MusicTrack.valueOf(selectedTrack)
                    } catch (e: Exception) {
                        null
                    }
                    
                    if (enumTrack != null) {
                        // Use enum-based playback
                        manager.changeSong(enumTrack)
                        Log.d("MusicSettings", "Playing enum track: ${enumTrack.name} (${enumTrack.resourceName})")
                    } else {
                        // Use dynamic track playback by filename
                        val selectedMetadata = musicState.musicList.find { it.id == selectedTrack }
                        Log.d("MusicSettings", "Looking for track ID: $selectedTrack in ${musicState.musicList.size} tracks")
                        
                        if (selectedMetadata != null) {
                            Log.d("MusicSettings", "Found metadata - title: ${selectedMetadata.title}, filename: ${selectedMetadata.filename}")
                            
                            // Check if file is downloaded
                            val isDownloaded = downloadManager?.isMusicDownloaded(selectedMetadata.filename) ?: false
                            Log.d("MusicSettings", "Is file downloaded? $isDownloaded")
                            
                            if (isDownloaded) {
                                manager.playDynamicTrack(selectedMetadata.filename)
                                Log.d("MusicSettings", "✅ Attempting to play dynamic track: ${selectedMetadata.filename}")
                            } else {
                                Log.w("MusicSettings", "⚠️ File not downloaded: ${selectedMetadata.filename}")
                                manager.changeSong(BackgroundMusicManager.MusicTrack.NONE)
                            }
                        } else {
                            manager.changeSong(BackgroundMusicManager.MusicTrack.NONE)
                            Log.w("MusicSettings", "❌ Track not found in music list: $selectedTrack")
                        }
                    }
                }
            } ?: Log.e("MusicSettings", "❌ MusicManager is null!")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Background Music", 
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
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = { musicSettingsViewModel.refreshMusicList() },
                        enabled = !musicState.isRefreshing
                    ) {
                        if (musicState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh music list"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message
            if (musicState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Failed to load music",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = musicState.error ?: "Unknown error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            
            // Loading indicator for initial load
            if (musicState.isLoading && tracks.size <= 1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Loading music tracks...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            
            // Enable/Disable Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Enable Background Music",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (enabled) "Music is playing" else "Music is disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }
            }
            
            // Volume Control - Android System UI Style (Horizontal)
            AnimatedVisibility(visible = enabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Volume Icon
                        Icon(
                            imageVector = when {
                                volume == 0f -> Icons.Default.VolumeOff
                                volume < 0.5f -> Icons.Default.VolumeDown
                                else -> Icons.Default.VolumeUp
                            },
                            contentDescription = "Volume",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        // Slider
                        Slider(
                            value = volume,
                            onValueChange = { newVolume ->
                                volume = newVolume
                                isUserAdjustingVolume = true
                                
                                // Apply volume to music manager immediately for live feedback
                                // Only setVolume, don't restart the song
                                musicManager?.setVolume(newVolume)
                                
                                // Debounce the save to Firebase
                                saveJob?.cancel()
                                saveJob = scope.launch {
                                    delay(300) // Wait 300ms after user stops adjusting
                                    if (state.user != null) {
                                        viewModel.updateMusicSettings(enabled, selectedTrack, newVolume)
                                    }
                                    delay(100) // Extra delay before allowing sync
                                    isUserAdjustingVolume = false
                                }
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Volume Percentage
                        Text(
                            text = "${(volume * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(48.dp)
                        )
                    }
                }
            }
            
            // Music Tracks Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Library",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Track count badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${tracks.count { it.id != "NONE" }} tracks",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            // Grid of music cards
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(((tracks.size / 2 + 1) * 220).dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tracks) { track ->
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
                                    }
                                } finally {
                                    deletingStates.remove(track.fileName)
                                }
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Show music player screen
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
                musicManager?.setVolume(newVolume)
                saveJob?.cancel()
                saveJob = scope.launch {
                    delay(300)
                    if (state.user != null) {
                        viewModel.updateMusicSettings(enabled, selectedTrack, newVolume)
                    }
                    delay(100)
                    isUserAdjustingVolume = false
                }
            },
            onPlayPauseClick = {
                enabled = !enabled
            }
        )
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
    
    // Animation for selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
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
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Album art / Icon area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Background gradient
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
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
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = Color.White
                            )
                        } else if (isDownloading) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { if (progress == 0) 0.5f else progress / 100f },
                                    modifier = Modifier.size(40.dp),
                                    strokeWidth = 3.dp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (progress == 0) "..." else "$progress%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Icon(
                                imageVector = if (track.id == "NONE") 
                                    Icons.Default.MusicOff 
                                else 
                                    Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
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
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = track.category,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
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
                                .size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Track info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (track.id == "NONE" || isDownloaded)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    
                    if (track.artist.isNotEmpty()) {
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Status text
                    if (!isDownloading && !isDeleting && !isDownloaded && track.fileName.isNotEmpty()) {
                        Text(
                            text = "Tap to download",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
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
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Open player",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Delete button
                            IconButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else if (!isDownloading && !isDeleting) {
                            // Download button
                            FilledTonalButton(
                                onClick = onDownload,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Download",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp
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

@Composable
private fun MusicTrackCard(
    track: MusicTrackData,
    isSelected: Boolean,
    isDownloaded: Boolean,
    downloadState: Pair<Boolean, Int>,
    isDeleting: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val (isDownloading, progress) = downloadState
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.tertiaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = track.id == "NONE" || isDownloaded) { onSelect() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio Button
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                enabled = track.id == "NONE" || isDownloaded
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Track Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (track.id == "NONE" || isDownloaded)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                if (track.artist.isNotEmpty()) {
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isDownloading) {
                    Text(
                        text = "Downloading... $progress%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else if (!isDownloaded && track.fileName.isNotEmpty()) {
                    Text(
                        text = "Not downloaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Action Buttons
            if (track.fileName.isNotEmpty()) {
                if (isDeleting) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else if (isDownloading) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // If progress is 0, show indeterminate spinner (unknown content length);
                        // otherwise show determinate progress using a Float value.
                        if (progress == 0) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        } else {
                            CircularProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Text(
                            text = "$progress%",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                } else if (isDownloaded) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isDeleting
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    IconButton(onClick = onDownload) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
            title = { Text("Delete Song?") },
            text = { 
                Text("Are you sure you want to delete \"${track.name}\"? You can download it again later.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
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
