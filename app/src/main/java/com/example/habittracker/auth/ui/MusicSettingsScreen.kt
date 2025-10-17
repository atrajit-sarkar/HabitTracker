package it.atraj.habittracker.auth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.atraj.habittracker.music.BackgroundMusicManager
import it.atraj.habittracker.music.MusicDownloadManager
import kotlinx.coroutines.launch

data class MusicTrackData(
    val id: String,
    val name: String,
    val fileName: String,
    val artist: String = "",
    val category: String = "Ambient"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSettingsScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? it.atraj.habittracker.MainActivity
    
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
    var selectedTrack by remember { mutableStateOf(state.user?.musicTrack ?: \"NONE\") }
    var volume by remember { mutableFloatStateOf(state.user?.musicVolume ?: 0.3f) }
    
    val downloadStates = remember { mutableStateMapOf<String, Pair<Boolean, Int>>() }
    val scope = rememberCoroutineScope()
    
    val tracks = remember {
        listOf(
            MusicTrackData("NONE", "No Music", "", "", "None"),
            MusicTrackData("AMBIENT_1", "Peaceful Calm", "ambient_calm.mp3", "Ambient", "Ambient"),
            MusicTrackData("AMBIENT_2", "Focus Flow", "ambient_focus.mp3", "Ambient", "Ambient"),
            MusicTrackData("AMBIENT_3", "Nature Sounds", "ambient_nature.mp3", "Nature", "Ambient"),
            MusicTrackData("LOFI_1", "Lo-Fi Chill", "lofi_chill.mp3", "Lo-Fi", "Lo-Fi"),
            MusicTrackData("PIANO_1", "Soft Piano", "piano_soft.mp3", "Piano", "Classical"),
            MusicTrackData("ROMANTIC_1", "Casa Rosa", "romantic_casa_rosa.mp3", "Guera Encantadora", "Romantic"),
            MusicTrackData("HINDI_1", "Love Slowed", "hindi_love_slowed.mp3", "Hindi", "Romantic"),
            MusicTrackData("JAPANESE_1", "Waguri Edit", "japanese_waguri_edit.mp3", "Waguri", "Japanese"),
            MusicTrackData("JAPANESE_2", "Shounen Ki", "japanese_shounen_ki.mp3", "Tetsuya Takeda", "Japanese")
        )
    }
    
    // Auto-save when settings change
    LaunchedEffect(enabled, selectedTrack, volume) {
        if (state.user != null) {
            viewModel.updateMusicSettings(enabled, selectedTrack, volume)
            
            // Apply immediately to music manager
            musicManager?.let { manager ->
                val musicTrack = try {
                    BackgroundMusicManager.MusicTrack.valueOf(selectedTrack)
                } catch (e: Exception) {
                    BackgroundMusicManager.MusicTrack.NONE
                }
                manager.setEnabled(enabled)
                manager.changeSong(musicTrack)
                manager.setVolume(volume)
            }
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
            
            // Volume Control
            AnimatedVisibility(visible = enabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Volume",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(volume * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = volume,
                            onValueChange = { volume = it },
                            valueRange = 0f..1f,
                            steps = 19
                        )
                    }
                }
            }
            
            // Music Tracks Section
            Text(
                text = "Available Tracks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            tracks.forEach { track ->
                MusicTrackCard(
                    track = track,
                    isSelected = selectedTrack == track.id,
                    isDownloaded = track.fileName.isEmpty() || 
                        downloadManager?.isMusicDownloaded(track.fileName) == true,
                    downloadState = downloadStates[track.fileName] ?: (false to 0),
                    onSelect = { 
                        if (track.id == "NONE" || downloadManager?.isMusicDownloaded(track.fileName) == true) {
                            selectedTrack = track.id
                        }
                    },
                    onDownload = {
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
                        downloadManager?.deleteMusicFile(track.fileName)
                        downloadStates.remove(track.fileName)
                        if (selectedTrack == track.id) {
                            selectedTrack = "NONE"
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MusicTrackCard(
    track: MusicTrackData,
    isSelected: Boolean,
    isDownloaded: Boolean,
    downloadState: Pair<Boolean, Int>,
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
                if (isDownloading) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "$progress",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                } else if (isDownloaded) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(28.dp)
                        )
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
