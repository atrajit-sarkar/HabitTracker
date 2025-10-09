package it.atraj.habittracker.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import it.atraj.habittracker.avatar.ui.AvatarPickerViewModel
import it.atraj.habittracker.data.local.HabitAvatar
import it.atraj.habittracker.data.local.HabitAvatarType

/**
 * Enhanced habit avatar picker with emoji and custom image support
 */
@Composable
fun HabitAvatarPickerDialog(
    currentAvatar: HabitAvatar,
    onAvatarSelected: (HabitAvatar) -> Unit,
    onDismiss: () -> Unit,
    viewModel: AvatarPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = Emojis, 1 = Images
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            viewModel.uploadCustomAvatar(it)
        }
    }
    
    // Monitor upload completion
    LaunchedEffect(uiState.avatars) {
        if (!uiState.isUploading && uiState.errorMessage == null && uiState.avatars.isNotEmpty()) {
            // Get the most recent custom avatar (first in list)
            val customAvatar = uiState.avatars.firstOrNull()
            if (customAvatar != null && selectedTab == 1) {
                // Auto-select the uploaded image
                onAvatarSelected(
                    HabitAvatar(
                        type = HabitAvatarType.CUSTOM_IMAGE,
                        value = customAvatar.url,
                        backgroundColor = currentAvatar.backgroundColor
                    )
                )
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadAvatars()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Choose Habit Avatar",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Tab selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Emojis") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Images") }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Emoji tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Emoji grid
                            Text(
                                text = "Choose an Emoji",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                items(HabitAvatar.POPULAR_EMOJIS) { emoji ->
                                    val isSelected = currentAvatar.value == emoji && 
                                                    currentAvatar.type == HabitAvatarType.EMOJI
                                    
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .background(
                                                color = if (isSelected) 
                                                    MaterialTheme.colorScheme.primaryContainer 
                                                else 
                                                    MaterialTheme.colorScheme.surface,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                onAvatarSelected(
                                                    currentAvatar.copy(
                                                        type = HabitAvatarType.EMOJI,
                                                        value = emoji
                                                    )
                                                )
                                            }
                                    ) {
                                        Text(
                                            text = emoji,
                                            fontSize = 24.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            
                            // Background color selection
                            Text(
                                text = "Background Color",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(HabitAvatar.BACKGROUND_COLORS) { color ->
                                    val isSelected = currentAvatar.backgroundColor == color
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = Color(android.graphics.Color.parseColor(color)),
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                onAvatarSelected(currentAvatar.copy(backgroundColor = color))
                                            }
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    1 -> {
                        // Image tab
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Upload button
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isUploading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (uiState.isUploading) "Uploading..." else "Upload Custom Image")
                            }
                            
                            // Upload progress
                            if (uiState.isUploading) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = error,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                            
                            // Custom images grid
                            if (uiState.isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (uiState.avatars.isNotEmpty()) {
                                Text(
                                    text = "Your Custom Images",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.heightIn(max = 300.dp)
                                ) {
                                    items(uiState.avatars) { avatarItem ->
                                        val isSelected = currentAvatar.value == avatarItem.url && 
                                                        currentAvatar.type == HabitAvatarType.CUSTOM_IMAGE
                                        
                                        Box(
                                            modifier = Modifier.aspectRatio(1f)
                                        ) {
                                            // Avatar image
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 0.dp,
                                                        color = if (isSelected) 
                                                            MaterialTheme.colorScheme.primary 
                                                        else 
                                                            Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        onAvatarSelected(
                                                            currentAvatar.copy(
                                                                type = HabitAvatarType.CUSTOM_IMAGE,
                                                                value = avatarItem.url
                                                            )
                                                        )
                                                    }
                                            ) {
                                                val token = it.atraj.habittracker.avatar.SecureTokenStorage.getToken(context)
                                                val requestBuilder = ImageRequest.Builder(context)
                                                    .data(avatarItem.url)
                                                    .crossfade(true)
                                                
                                                if (token != null) {
                                                    requestBuilder.addHeader("Authorization", "token $token")
                                                }
                                                
                                                AsyncImage(
                                                    model = requestBuilder.build(),
                                                    contentDescription = "Custom avatar option",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                            
                                            // Selection indicator - larger with shadow for better visibility
                                            if (isSelected) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(4.dp)
                                                        .size(28.dp),
                                                    shadowElevation = 4.dp
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
                                            
                                            // Star badge for custom images - indicates user-uploaded content
                                            // Only show for CUSTOM type (user-uploaded), not DEFAULT avatars
                                            if (avatarItem.type == it.atraj.habittracker.avatar.AvatarType.CUSTOM) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                                    shape = CircleShape,
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(4.dp)
                                                        .size(24.dp),
                                                    shadowElevation = 2.dp
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "Custom",
                                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                        modifier = Modifier
                                                            .padding(4.dp)
                                                            .fillMaxSize()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No custom images yet.\nTap Upload to add your first image!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

