package it.atraj.habittracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.airbnb.lottie.compose.*
import it.atraj.habittracker.R
import it.atraj.habittracker.data.local.Habit
import it.atraj.habittracker.data.local.HabitAvatar
import it.atraj.habittracker.data.local.HabitAvatarType
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    deletedHabits: List<Habit>,
    onBackClick: () -> Unit,
    onRestoreHabit: (Long) -> Unit,
    onPermanentlyDeleteHabit: (Long) -> Unit,
    onEmptyTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var showPermanentDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var isEmptyingTrash by remember { mutableStateOf(false) }
    var isDeletingHabit by remember { mutableStateOf(false) }
    var isRestoringHabit by remember { mutableStateOf(false) }
    val habitCountBeforeDelete = remember { mutableStateOf(deletedHabits.size) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.trash),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (deletedHabits.isNotEmpty()) {
                            val itemText = if (deletedHabits.size == 1) "" else "s"
                            Text(
                                text = stringResource(R.string.trash_items_count, deletedHabits.size, itemText),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (deletedHabits.isNotEmpty()) {
                        IconButton(
                            onClick = { showEmptyTrashDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Empty Trash",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Auto-cleanup info card
            if (deletedHabits.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.items_auto_delete_after_30_days),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            // Content
            if (deletedHabits.isEmpty()) {
                EmptyTrashState()
            } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(deletedHabits, key = { it.id }) { habit ->
                    DeletedHabitCard(
                        habit = habit,
                        onRestore = { 
                            habitCountBeforeDelete.value = deletedHabits.size
                            isRestoringHabit = true
                            onRestoreHabit(habit.id) 
                        },
                        onPermanentlyDelete = { showPermanentDeleteDialog = habit.id }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            }
        }
    }

    // Empty Trash Confirmation Dialog
    if (showEmptyTrashDialog) {
        EmptyTrashConfirmationDialog(
            onConfirm = {
                showEmptyTrashDialog = false
                habitCountBeforeDelete.value = deletedHabits.size
                isEmptyingTrash = true
                onEmptyTrash()
            },
            onDismiss = { showEmptyTrashDialog = false }
        )
    }

    // Permanent Delete Confirmation Dialog
    showPermanentDeleteDialog?.let { habitId ->
        PermanentDeleteConfirmationDialog(
            onConfirm = {
                showPermanentDeleteDialog = null
                habitCountBeforeDelete.value = deletedHabits.size
                isDeletingHabit = true
                onPermanentlyDeleteHabit(habitId)
            },
            onDismiss = { showPermanentDeleteDialog = null }
        )
    }
    
    // Loading overlay for empty trash operation
    if (isEmptyingTrash) {
        LoadingSandClockOverlay()
    }
    
    // Loading overlay for permanent delete operation
    if (isDeletingHabit) {
        LoadingSandClockOverlay()
    }
    
    // Loading overlay for restore operation
    if (isRestoringHabit) {
        LoadingSandClockOverlay()
    }
    
    // Auto-dismiss logic
    LaunchedEffect(deletedHabits.size) {
        // For empty trash: wait until ALL habits are deleted (size becomes 0)
        if (isEmptyingTrash && deletedHabits.isEmpty()) {
            isEmptyingTrash = false
        }
        // For single delete: dismiss when count decreases
        if (isDeletingHabit && deletedHabits.size < habitCountBeforeDelete.value) {
            isDeletingHabit = false
        }
        // For restore: dismiss when count decreases (habit moved from trash)
        if (isRestoringHabit && deletedHabits.size < habitCountBeforeDelete.value) {
            isRestoringHabit = false
        }
    }
}

@Composable
private fun EmptyTrashState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Empty state illustration
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(80.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = stringResource(R.string.trash_is_empty),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(R.string.deleted_habits_appear_here),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.restore_within_30_days),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun DeletedHabitCard(
    habit: Habit,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = remember(habit.id) { cardPaletteFor(habit.id) }
    val deletedDate = habit.deletedAt?.let { instant ->
        val localDate = instant.atZone(java.time.ZoneOffset.UTC).toLocalDate()
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(localDate)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(palette.brush, shape = RoundedCornerShape(28.dp))
                .background(
                    Color.Black.copy(alpha = 0.3f), // Overlay for deleted state
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar display with deleted state effect
                    TrashAvatarDisplay(
                        avatar = habit.avatar,
                        size = 48.dp,
                        alpha = 0.8f
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (habit.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = habit.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 3
                            )
                        }
                    }
                }

                // Deleted status info
                deletedDate?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.deleted_on, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Action buttons - same style as home screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onRestore,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp)),
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White,
                            contentColor = palette.accent
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.restore), fontWeight = FontWeight.SemiBold)
                    }
                    
                    OutlinedButton(
                        onClick = onPermanentlyDelete,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.delete), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrashAvatarDisplay(
    avatar: HabitAvatar,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(
                color = Color(avatar.backgroundColor.toColorInt()).copy(alpha = alpha * 0.5f),
                shape = androidx.compose.foundation.shape.CircleShape
            )
    ) {
        when (avatar.type) {
            HabitAvatarType.EMOJI -> {
                Text(
                    text = avatar.value,
                    fontSize = (size.value * 0.5).sp,
                    textAlign = TextAlign.Center,
                    color = Color.Black.copy(alpha = alpha)
                )
            }
            HabitAvatarType.DEFAULT_ICON -> {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = alpha),
                    modifier = Modifier.size(size * 0.6f)
                )
            }
            HabitAvatarType.CUSTOM_IMAGE -> {
                // Optimized image loading with Coil
                val token = it.atraj.habittracker.avatar.SecureTokenStorage.getToken(context)
                val requestBuilder = coil.request.ImageRequest.Builder(context)
                    .data(avatar.value)
                    .crossfade(300) // Smooth crossfade animation
                    .size(size.value.toInt() * 2) // Load 2x size for better quality on high DPI
                    .memoryCacheKey(avatar.value) // Enable memory caching
                    .diskCacheKey(avatar.value) // Enable disk caching
                
                if (token != null && avatar.value.contains("githubusercontent.com")) {
                    requestBuilder.addHeader("Authorization", "token $token")
                }
                
                coil.compose.AsyncImage(
                    model = requestBuilder.build(),
                    contentDescription = "Custom habit avatar",
                    modifier = Modifier
                        .size(size)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .graphicsLayer(alpha = alpha),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun EmptyTrashConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(text = stringResource(R.string.empty_trash_title))
        },
        text = {
            Text(text = stringResource(R.string.empty_trash_message))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.empty_trash))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun PermanentDeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(text = stringResource(R.string.delete_permanently_title))
        },
        text = {
            Text(text = stringResource(R.string.delete_permanently_message))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete_forever))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun LoadingSandClockOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)), // Dimmed background
        contentAlignment = Alignment.Center
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.Asset("loading_sand_clock.json")
        )
        
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            isPlaying = true,
            speed = 1f,
            restartOnPlay = true
        )
        
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(120.dp) // Professional size
        )
    }
}

