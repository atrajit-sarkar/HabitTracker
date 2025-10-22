package it.atraj.habittracker.music.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.atraj.habittracker.auth.ui.AuthViewModel
import it.atraj.habittracker.data.model.SongUploadData
import it.atraj.habittracker.data.repository.GitHubMusicService
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Screen for uploading songs to the music repository
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongUploadScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    musicBrowserViewModel: MusicBrowserViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onUploadSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by authViewModel.uiState.collectAsStateWithLifecycle()
    
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var songTitle by remember { mutableStateOf("") }
    var artistName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Ambient") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    
    val categories = listOf(
        "Ambient",
        "Anime",
        "Classical",
        "Electronic",
        "Focus",
        "Nature",
        "Meditation",
        "Upbeat",
        "Relaxing"
    )
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            // Extract filename from URI
            val fileName = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: "unknown.mp3"
            
            selectedFileName = fileName
            
            // Auto-fill title if empty
            if (songTitle.isEmpty()) {
                songTitle = fileName.removeSuffix(".mp3").replace("_", " ")
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Upload Song",
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Share Your Music",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Upload your favorite background music to share with the community. Supported format: MP3",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // File Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select Audio File",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Button(
                        onClick = { filePickerLauncher.launch("audio/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AudioFile,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose MP3 File")
                    }
                    
                    if (selectedFileName.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = selectedFileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Song Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Song Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = songTitle,
                        onValueChange = { songTitle = it },
                        label = { Text("Song Title") },
                        placeholder = { Text("Enter song title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null
                            )
                        }
                    )
                    
                    OutlinedTextField(
                        value = artistName,
                        onValueChange = { artistName = it },
                        label = { Text("Artist Name") },
                        placeholder = { Text("Enter artist name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null
                            )
                        }
                    )
                    
                    // Category Selection
                    OutlinedButton(
                        onClick = { showCategoryDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Category: $selectedCategory")
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }
            }
            
            // Upload Button
            Button(
                onClick = {
                    if (selectedFileUri == null) {
                        Toast.makeText(context, "Please select a file", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    if (songTitle.isEmpty()) {
                        Toast.makeText(context, "Please enter a song title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    if (artistName.isEmpty()) {
                        Toast.makeText(context, "Please enter an artist name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    val userId = state.user?.email?.substringBefore("@") ?: "anonymous"
                    
                    isUploading = true
                    scope.launch {
                        try {
                            // Read file data
                            val inputStream = context.contentResolver.openInputStream(selectedFileUri!!)
                            val fileData = inputStream?.use { input ->
                                val buffer = ByteArrayOutputStream()
                                val data = ByteArray(1024)
                                var count: Int
                                while (input.read(data).also { count = it } != -1) {
                                    buffer.write(data, 0, count)
                                    uploadProgress = buffer.size() / (inputStream.available() + buffer.size()).toFloat()
                                }
                                buffer.toByteArray()
                            } ?: throw Exception("Failed to read file")
                            
                            // Create upload data
                            val currentUser = state.user
                            if (currentUser == null) {
                                Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                                isUploading = false
                                return@launch
                            }
                            
                            val userId = currentUser.email?.substringBefore("@")?.ifEmpty { currentUser.uid } ?: currentUser.uid
                            val uploaderName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Unknown"
                            
                            val uploadData = SongUploadData(
                                fileName = selectedFileName,
                                category = selectedCategory.lowercase(),
                                fileData = fileData,
                                title = songTitle,
                                artist = artistName,
                                uploaderName = uploaderName,
                                duration = 0,
                                tags = listOf(selectedCategory.lowercase())
                            )
                            
                            // Upload using GitHubMusicService via ViewModel
                            Log.d("SongUpload", "Starting upload for: ${uploadData.title}")
                            
                            val result = musicBrowserViewModel.uploadSong(userId, uploadData)
                            
                            result.onSuccess { response ->
                                Log.d("SongUpload", "Upload successful!")
                                Toast.makeText(
                                    context,
                                    "✅ Song uploaded successfully!\n${songTitle} is now available in the ${selectedCategory} category",
                                    Toast.LENGTH_LONG
                                ).show()
                                
                                isUploading = false
                                uploadProgress = 0f
                                
                                // Reset form
                                selectedFileUri = null
                                selectedFileName = ""
                                songTitle = ""
                                artistName = ""
                                
                                onUploadSuccess()
                            }.onFailure { error ->
                                Log.e("SongUpload", "Upload failed: ${error.message}", error)
                                Toast.makeText(
                                    context,
                                    "❌ Upload failed: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                isUploading = false
                                uploadProgress = 0f
                            }
                        } catch (e: Exception) {
                            Log.e("SongUpload", "Upload failed", e)
                            Toast.makeText(
                                context,
                                "Upload failed: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            isUploading = false
                            uploadProgress = 0f
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUploading && selectedFileUri != null && 
                         songTitle.isNotEmpty() && artistName.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploading... ${(uploadProgress * 100).toInt()}%")
                } else {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Song")
                }
            }
        }
        
        // Category Selection Dialog
        if (showCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showCategoryDialog = false },
                title = { Text("Select Category") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    selectedCategory = category
                                    showCategoryDialog = false
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (category == selectedCategory)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (category == selectedCategory)
                                            FontWeight.Bold
                                        else
                                            FontWeight.Normal
                                    )
                                    
                                    if (category == selectedCategory) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCategoryDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
