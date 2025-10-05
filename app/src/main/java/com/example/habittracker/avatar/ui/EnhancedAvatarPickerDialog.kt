package it.atraj.habittracker.avatar.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import it.atraj.habittracker.avatar.AvatarItem
import it.atraj.habittracker.avatar.AvatarType

/**
 * Enhanced Avatar Picker Dialog with upload functionality
 * 
 * Features:
 * - Display default avatars
 * - Show user's custom uploaded avatars
 * - Upload new custom avatars
 * - Delete custom avatars
 * - Select avatar
 */
@Composable
fun EnhancedAvatarPickerDialog(
    currentAvatar: String,
    onAvatarSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: AvatarPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadCustomAvatar(it) }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadAvatars()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Choose Your Avatar",
                    fontWeight = FontWeight.Bold
                )
                
                // Upload button
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    enabled = !uiState.isUploading
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Upload custom avatar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Upload progress
                if (uiState.isUploading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Uploading...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Error message
                uiState.errorMessage?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                // Loading state
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Avatar grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(uiState.avatars) { avatarItem ->
                            // For private repos, URLs have different tokens each time
                            // Compare without query parameters
                            val avatarUrlWithoutParams = avatarItem.url.substringBefore('?')
                            val currentAvatarWithoutParams = currentAvatar.substringBefore('?')
                            val isSelected = avatarUrlWithoutParams == currentAvatarWithoutParams
                            
                            AvatarGridItem(
                                avatarItem = avatarItem,
                                isSelected = isSelected,
                                onSelect = { onAvatarSelected(avatarItem.url) },
                                onDelete = { 
                                    if (avatarItem.type == AvatarType.CUSTOM) {
                                        viewModel.deleteCustomAvatar(avatarItem.url)
                                    }
                                },
                                showDeleteOption = avatarItem.type == AvatarType.CUSTOM
                            )
                        }
                    }
                }
                
                // Help text
                Text(
                    text = "Tap an avatar to select, or upload your own image",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AvatarGridItem(
    avatarItem: AvatarItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    showDeleteOption: Boolean
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.aspectRatio(1f)
    ) {
        // Avatar image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (isSelected) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        Color.Transparent,
                    shape = CircleShape
                )
                .clickable(onClick = onSelect),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(avatarItem.url)
                    .size(Size.ORIGINAL)
                    .crossfade(true)
                    .build(),
                contentDescription = "Avatar option",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            
            // Selection indicator
            if (isSelected) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxSize()
                    )
                }
            }
        }
        
        // Delete button for custom avatars
        if (showDeleteOption && !isSelected) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .clickable { showDeleteDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxSize()
                )
            }
        }
        
        // Badge for custom avatars
        if (avatarItem.type == AvatarType.CUSTOM) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Custom",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .padding(3.dp)
                        .fillMaxSize()
                )
            }
        }
    }
    
    // Delete confirmation dialog
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
                Text("Delete Custom Avatar?")
            },
            text = {
                Text("This will permanently delete this custom avatar from the cloud.")
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
