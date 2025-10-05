package it.atraj.habittracker.ui

// import androidx.compose.ui.platform.LocalLifecycleOwner // Already imported via androidx.lifecycle.compose.LocalLifecycleOwner
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import it.atraj.habittracker.R
import it.atraj.habittracker.auth.User
import it.atraj.habittracker.data.local.HabitAvatar
import it.atraj.habittracker.data.local.HabitAvatarType
import it.atraj.habittracker.data.local.HabitFrequency
import it.atraj.habittracker.data.local.NotificationSound
import it.atraj.habittracker.ui.DeleteHabitConfirmationDialog
import it.atraj.habittracker.ui.dialogs.FirstLaunchNotificationDialog
import it.atraj.habittracker.util.clickableOnce
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitHomeRoute(
    state: HabitScreenState,
    user: User?,
    onAddHabitClick: () -> Unit,
    onToggleReminder: (Long, Boolean) -> Unit,
    onMarkHabitCompleted: (Long) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    onHabitDetailsClick: (Long) -> Unit,
    onTrashClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNotificationGuideClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationPermissionState = rememberNotificationPermissionState()
    var notificationCardDismissed by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    
    // First launch dialog state
    val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    var showFirstLaunchDialog by rememberSaveable { 
        mutableStateOf(!prefs.getBoolean("notification_guide_shown", false)) 
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionState.value = isGranted
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationPermissionState.value = checkNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shouldShowPermissionCard = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        state.habits.any { it.isReminderEnabled } &&
        !notificationPermissionState.value &&
        !notificationCardDismissed

    // Show first launch dialog after a short delay
    LaunchedEffect(Unit) {
        if (showFirstLaunchDialog) {
            kotlinx.coroutines.delay(2000) // Wait 2 seconds after app opens
        }
    }

    // First launch dialog
    if (showFirstLaunchDialog) {
        FirstLaunchNotificationDialog(
            onDismiss = {
                showFirstLaunchDialog = false
                prefs.edit().putBoolean("notification_guide_shown", true).apply()
            },
            onOpenGuide = {
                showFirstLaunchDialog = false
                prefs.edit().putBoolean("notification_guide_shown", true).apply()
                onNotificationGuideClick()
            }
        )
    }

    HabitHomeScreen(
        state = state,
        user = user,
        snackbarHostState = snackbarHostState,
        onAddHabitClick = onAddHabitClick,
        onToggleReminder = onToggleReminder,
        onMarkHabitCompleted = onMarkHabitCompleted,
        onDeleteHabit = onDeleteHabit,
        onHabitDetailsClick = onHabitDetailsClick,
        onTrashClick = onTrashClick,
        onProfileClick = onProfileClick,
        notificationPermissionVisible = shouldShowPermissionCard,
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onDismissPermissionCard = { notificationCardDismissed = true }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitHomeScreen(
    state: HabitScreenState,
    user: User?,
    snackbarHostState: SnackbarHostState,
    onAddHabitClick: () -> Unit,
    onToggleReminder: (Long, Boolean) -> Unit,
    onMarkHabitCompleted: (Long) -> Unit,
    onDeleteHabit: (Long) -> Unit,
    onHabitDetailsClick: (Long) -> Unit,
    onTrashClick: () -> Unit,
    onProfileClick: () -> Unit,
    notificationPermissionVisible: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onDismissPermissionCard: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var isDeletingHabit by remember { mutableStateOf(false) }
    val habitCountBeforeDelete = remember { mutableStateOf(state.habits.size) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onTrashClick = onTrashClick,
                onProfileClick = onProfileClick,
                onCloseDrawer = { 
                    scope.launch { drawerState.close() }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.home_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { 
                            scope.launch { drawerState.open() } 
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    // Profile Picture that navigates to profile screen
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                CircleShape
                            )
                            .clickableOnce { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        // Determine if we should show profile photo or custom avatar
                        val showProfilePhoto = user?.photoUrl != null && user.customAvatar == null
                        val currentAvatar = user?.customAvatar ?: "https://raw.githubusercontent.com/atrajit-sarkar/HabitTracker/main/Avatars/avatar_1_professional.png"
                        val context = LocalContext.current
                        
                        if (showProfilePhoto && user?.photoUrl != null) {
                            // Load Google profile photo in high quality
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(user.photoUrl)
                                    .size(Size.ORIGINAL) // Load original high-quality image
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile picture",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else if (currentAvatar.startsWith("https://")) {
                            // Custom avatar from GitHub (image URL)
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(currentAvatar)
                                    .size(Size.ORIGINAL) // Load original high-quality image
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Custom avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Fallback
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default avatar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddHabitClick,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.add_habit),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground())
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                AnimatedVisibility(visible = notificationPermissionVisible, enter = fadeIn(), exit = fadeOut()) {
                    NotificationPermissionCard(
                        onAllow = onRequestNotificationPermission,
                        onDismiss = onDismissPermissionCard
                    )
                }
            }

            if (state.habits.isEmpty() && !state.isLoading) {
                item {
                    EmptyState()
                }
            }

            items(state.habits, key = { it.id }) { habit ->
                HabitCard(
                    habit = habit,
                    onToggleReminder = { enabled -> onToggleReminder(habit.id, enabled) },
                    onMarkCompleted = { onMarkHabitCompleted(habit.id) },
                    onDelete = { 
                        habitCountBeforeDelete.value = state.habits.size
                        isDeletingHabit = true
                        onDeleteHabit(habit.id)
                    },
                    onSeeDetails = { onHabitDetailsClick(habit.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // For FAB overlap
            }
        }
    }
    }
    
    // Global loading overlay for delete operations
    if (isDeletingHabit) {
        LoadingSandClockOverlay()
    }
    
    // Auto-dismiss when habit count changes (deletion completed)
    LaunchedEffect(state.habits.size) {
        if (isDeletingHabit && state.habits.size < habitCountBeforeDelete.value) {
            isDeletingHabit = false
        }
    }
}

@Composable
private fun DrawerContent(
    onTrashClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    // Responsive drawer width: max 280dp or 75% of screen width (whichever is smaller)
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val maxDrawerWidth = 280.dp
    val drawerWidth = minOf(maxDrawerWidth, screenWidthDp * 0.75f)
    
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(drawerWidth) // Responsive width
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp) // Add proper top padding
        ) {
            // Modern header with subtle gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp) // Reduced padding
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Menu",
                                style = MaterialTheme.typography.headlineSmall.copy( // Smaller heading
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "Manage your habits",
                                style = MaterialTheme.typography.bodySmall, // Smaller subtitle
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 2.dp), // Reduced spacing
                                maxLines = 1
                            )
                        }
                        
                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            
            // Navigation items with modern styling
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp) // Reduced from 16dp
            ) {
                Text(
                    text = "ACTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp) // Reduced padding
                )
                
                // Profile Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableOnce {
                            onCloseDrawer()
                            onProfileClick()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp), // Reduced from 16dp
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp) // Reduced from 40dp
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp) // Reduced from 20dp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp)) // Reduced from 16dp
                        
                        Column(modifier = Modifier.weight(1f)) { // Added weight to prevent overflow
                            Text(
                                text = "Profile",
                                style = MaterialTheme.typography.titleSmall.copy( // Smaller size
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "Account settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp) // Reduced from 20dp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp)) // Reduced from 8dp
                
                // Trash Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableOnce {
                            onCloseDrawer()
                            onTrashClick()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp), // Reduced from 16dp
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp) // Reduced from 40dp
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp) // Reduced from 20dp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp)) // Reduced from 16dp
                        
                        Column(modifier = Modifier.weight(1f)) { // Added weight to prevent overflow
                            Text(
                                text = "Trash",
                                style = MaterialTheme.typography.titleSmall.copy( // Smaller size
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "View deleted habits",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp) // Reduced from 20dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun gradientBackground(): Brush = Brush.verticalGradient(
    colors = listOf(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )
)

@Composable
private fun HabitCard(
    habit: HabitCardUi, // Assuming HabitCardUi contains all necessary fields like reminderTime
    onToggleReminder: (Boolean) -> Unit,
    onMarkCompleted: () -> Unit,
    onDelete: () -> Unit,
    onSeeDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showTitleDialog by remember { mutableStateOf(false) }
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var isTitleTruncated by remember { mutableStateOf(false) }
    var isDescriptionTruncated by remember { mutableStateOf(false) }
    var showCompletionAnimation by remember { mutableStateOf(false) }
    var showFanfareAnimation by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    
    val palette = remember(habit.id) { cardPaletteFor(habit.id) }
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val reminderText = if (habit.isReminderEnabled) {
        stringResource(id = R.string.reminder_on, timeFormatter.format(habit.reminderTime))
    } else {
        stringResource(id = R.string.reminder_off)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(palette.brush, shape = RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar display
                    AvatarDisplay(
                        avatar = habit.avatar,
                        size = 48.dp
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = habit.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { textLayoutResult ->
                                    isTitleTruncated = textLayoutResult.hasVisualOverflow
                                },
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isTitleTruncated) {
                                IconButton(
                                    onClick = { showTitleDialog = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Show full title",
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (habit.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = habit.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.82f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    onTextLayout = { textLayoutResult ->
                                        isDescriptionTruncated = textLayoutResult.hasVisualOverflow
                                    },
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isDescriptionTruncated) {
                                    IconButton(
                                        onClick = { showDescriptionDialog = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Show full description",
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.delete),
                            tint = Color.White
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = reminderText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = habit.isReminderEnabled,
                        onCheckedChange = onToggleReminder,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color.White.copy(alpha = 0.4f),
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                            uncheckedThumbColor = Color.White
                        )
                    )
                }

                if (habit.isCompletedToday) {
                    // Lottie animation composition
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.Asset("man_with_task_list.json")
                    )
                    
                    // Calculate target frame: 132 out of 241 total frames
                    val targetProgress = 132f / 241f // ≈ 0.5477
                    
                    val rawAnimationProgress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = 1, // Play only once
                        isPlaying = showCompletionAnimation,
                        speed = 1f,
                        restartOnPlay = false
                    )
                    
                    // Clamp progress to stop at frame 132
                    val animationProgress = if (rawAnimationProgress > targetProgress) {
                        targetProgress
                    } else {
                        rawAnimationProgress
                    }
                    
                    // Stop animation when target frame is reached
                    LaunchedEffect(rawAnimationProgress) {
                        if (rawAnimationProgress >= targetProgress) {
                            showCompletionAnimation = false
                        }
                    }
                    
                    // Trigger animation when habit becomes completed
                    LaunchedEffect(habit.isCompletedToday) {
                        if (habit.isCompletedToday && !showCompletionAnimation) {
                            showCompletionAnimation = true
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Completed chip
                        AssistChip(
                            onClick = { /* No action needed or perhaps show details */ },
                            label = {
                                Text(
                                    text = stringResource(id = R.string.habit_completed_today),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color.White.copy(alpha = 0.18f),
                                labelColor = Color.White,
                                leadingIconContentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Lottie animation
                        LottieAnimation(
                            composition = composition,
                            progress = { animationProgress },
                            modifier = Modifier.size(60.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            FilledTonalButton(
                                onClick = {
                                    showFanfareAnimation = true
                                    onMarkCompleted()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp)),
                                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White,
                                    contentColor = palette.accent
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = stringResource(R.string.done), fontWeight = FontWeight.SemiBold)
                            }
                            
                            // Fanfare animation overlay
                            if (showFanfareAnimation) {
                                val fanfareComposition by rememberLottieComposition(
                                    LottieCompositionSpec.Asset("Fanfare.json")
                                )
                                
                                val fanfareProgress by animateLottieCompositionAsState(
                                    composition = fanfareComposition,
                                    iterations = 1,
                                    isPlaying = showFanfareAnimation,
                                    speed = 1.2f,
                                    restartOnPlay = false
                                )
                                
                                // Stop animation when complete
                                LaunchedEffect(fanfareProgress) {
                                    if (fanfareProgress >= 0.99f) {
                                        showFanfareAnimation = false
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LottieAnimation(
                                        composition = fanfareComposition,
                                        progress = { fanfareProgress },
                                        modifier = Modifier.size(120.dp)
                                    )
                                }
                            }
                        }
                        
                        OutlinedButton(
                            onClick = onSeeDetails,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp)),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.details), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Add "See Details" button for completed habits too
                if (habit.isCompletedToday) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onSeeDetails,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.see_details), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
    
    // Title dialog
    if (showTitleDialog) {
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            icon = {
                Icon(imageVector = Icons.Default.Info, contentDescription = null)
            },
            title = {
                Text(text = "Habit Title")
            },
            text = {
                Text(text = habit.title)
            },
            confirmButton = {
                TextButton(onClick = { showTitleDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Description dialog
    if (showDescriptionDialog) {
        AlertDialog(
            onDismissRequest = { showDescriptionDialog = false },
            icon = {
                Icon(imageVector = Icons.Default.Info, contentDescription = null)
            },
            title = {
                Text(text = "Description")
            },
            text = {
                Text(text = habit.description)
            },
            confirmButton = {
                TextButton(onClick = { showDescriptionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        DeleteHabitConfirmationDialog(
            habitTitle = habit.title,
            onConfirm = {
                showDeleteConfirmation = false
                onDelete()
            },
            onDismiss = {
                showDeleteConfirmation = false
            }
        )
    }
}

@Composable
private fun NotificationPermissionCard(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(id = R.string.notification_permission_rationale),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onAllow) {
                    Text(text = stringResource(id = R.string.notification_permission_allow))
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.notification_permission_not_now))
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.empty_state_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(id = R.string.empty_state_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHabitSheet(
    state: AddHabitState,
    onHabitNameChange: (String) -> Unit,
    onHabitDescriptionChange: (String) -> Unit,
    onHabitReminderToggleChange: (Boolean) -> Unit,
    onHabitTimeChange: (Int, Int) -> Unit,
    onHabitFrequencyChange: (HabitFrequency) -> Unit,
    onHabitDayOfWeekChange: (Int) -> Unit,
    onHabitDayOfMonthChange: (Int) -> Unit,
    onHabitMonthOfYearChange: (Int) -> Unit,
    onAvatarChange: (HabitAvatar) -> Unit,
    onNotificationSoundChange: (NotificationSound) -> Unit,
    onDismiss: () -> Unit,
    onSaveHabit: () -> Unit
) {
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    val timePickerState = rememberTimePickerState(
        initialHour = state.hour, 
        initialMinute = state.minute,
        is24Hour = is24Hour
    )
    val scrollState = rememberScrollState() // Added scroll state

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        onHabitTimeChange(timePickerState.hour, timePickerState.minute)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(scrollState), // Applied verticalScroll
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.add_habit),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        OutlinedTextField(
            value = state.title,
            onValueChange = onHabitNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.habit_name_label)) },
            isError = state.nameError != null,
            singleLine = true,
            supportingText = {
                state.nameError?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = onHabitDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.habit_description_label)) },
            minLines = 2,
            maxLines = 4
        )
        
        // Avatar Selection
        AvatarSelector(
            selectedAvatar = state.avatar,
            onAvatarChange = onAvatarChange
        )
        
        // Frequency Selection
        FrequencySelector(
            frequency = state.frequency,
            dayOfWeek = state.dayOfWeek,
            dayOfMonth = state.dayOfMonth,
            monthOfYear = state.monthOfYear,
            onFrequencyChange = onHabitFrequencyChange,
            onDayOfWeekChange = onHabitDayOfWeekChange,
            onDayOfMonthChange = onHabitDayOfMonthChange,
            onMonthOfYearChange = onHabitMonthOfYearChange
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.reminder_toggle_label),
                style = MaterialTheme.typography.titleMedium
            )
            Switch(
                checked = state.reminderEnabled,
                onCheckedChange = onHabitReminderToggleChange
            )
        }

        AnimatedVisibility(visible = state.reminderEnabled) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimePicker(
                    state = timePickerState, 
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Notification Sound Selection
                NotificationSoundSelector(
                    selectedSound = state.notificationSound,
                    availableSounds = state.availableSounds,
                    onSoundChange = onNotificationSoundChange
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp), 
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                onClick = onDismiss, 
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(id = R.string.cancel))
            }
            FilledTonalButton(
                onClick = onSaveHabit,
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving && state.title.isNotBlank() 
            ) {
                if (state.isSaving) {
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
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(text = stringResource(id = R.string.save))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencySelector(
    frequency: HabitFrequency,
    dayOfWeek: Int,
    dayOfMonth: Int,
    monthOfYear: Int,
    onFrequencyChange: (HabitFrequency) -> Unit,
    onDayOfWeekChange: (Int) -> Unit,
    onDayOfMonthChange: (Int) -> Unit,
    onMonthOfYearChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.frequency_label),
            style = MaterialTheme.typography.titleMedium
        )
        
        // Frequency dropdown
        var frequencyExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = frequencyExpanded,
            onExpandedChange = { frequencyExpanded = !frequencyExpanded }
        ) {
            OutlinedTextField(
                value = getFrequencyText(frequency),
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = frequencyExpanded,
                onDismissRequest = { frequencyExpanded = false }
            ) {
                HabitFrequency.values().forEach { freq ->
                    DropdownMenuItem(
                        text = { Text(getFrequencyText(freq)) },
                        onClick = {
                            onFrequencyChange(freq)
                            frequencyExpanded = false
                        }
                    )
                }
            }
        }
        
        // Additional options based on frequency
        when (frequency) {
            HabitFrequency.WEEKLY -> {
                WeeklySelector(
                    selectedDay = dayOfWeek,
                    onDayChange = onDayOfWeekChange
                )
            }
            HabitFrequency.MONTHLY -> {
                MonthlySelector(
                    selectedDay = dayOfMonth,
                    onDayChange = onDayOfMonthChange
                )
            }
            HabitFrequency.YEARLY -> {
                YearlySelector(
                    selectedMonth = monthOfYear,
                    selectedDay = dayOfMonth,
                    onMonthChange = onMonthOfYearChange,
                    onDayChange = onDayOfMonthChange
                )
            }
            HabitFrequency.DAILY -> {
                // No additional options needed
            }
        }
    }
}

@Composable
private fun getFrequencyText(frequency: HabitFrequency): String {
    return when (frequency) {
        HabitFrequency.DAILY -> stringResource(id = R.string.frequency_daily)
        HabitFrequency.WEEKLY -> stringResource(id = R.string.frequency_weekly)
        HabitFrequency.MONTHLY -> stringResource(id = R.string.frequency_monthly)
        HabitFrequency.YEARLY -> stringResource(id = R.string.frequency_yearly)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeeklySelector(
    selectedDay: Int,
    onDayChange: (Int) -> Unit
) {
    val dayNames = listOf(
        stringResource(id = R.string.monday),
        stringResource(id = R.string.tuesday),
        stringResource(id = R.string.wednesday),
        stringResource(id = R.string.thursday),
        stringResource(id = R.string.friday),
        stringResource(id = R.string.saturday),
        stringResource(id = R.string.sunday)
    )
    
    var dayExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = dayExpanded,
        onExpandedChange = { dayExpanded = !dayExpanded }
    ) {
        OutlinedTextField(
            value = dayNames[selectedDay - 1],
            onValueChange = { },
            readOnly = true,
            label = { Text(stringResource(id = R.string.day_of_week_label)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = dayExpanded,
            onDismissRequest = { dayExpanded = false }
        ) {
            dayNames.forEachIndexed { index, dayName ->
                DropdownMenuItem(
                    text = { Text(dayName) },
                    onClick = {
                        onDayChange(index + 1)
                        dayExpanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlySelector(
    selectedDay: Int,
    onDayChange: (Int) -> Unit
) {
    var dayExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = dayExpanded,
        onExpandedChange = { dayExpanded = !dayExpanded }
    ) {
        OutlinedTextField(
            value = selectedDay.toString(),
            onValueChange = { },
            readOnly = true,
            label = { Text(stringResource(id = R.string.day_of_month_label)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = dayExpanded,
            onDismissRequest = { dayExpanded = false }
        ) {
            (1..31).forEach { day ->
                DropdownMenuItem(
                    text = { Text(day.toString()) },
                    onClick = {
                        onDayChange(day)
                        dayExpanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearlySelector(
    selectedMonth: Int,
    selectedDay: Int,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit
) {
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Month selector
        var monthExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = monthExpanded,
            onExpandedChange = { monthExpanded = !monthExpanded }
        ) {
            OutlinedTextField(
                value = monthNames[selectedMonth - 1],
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(id = R.string.month_of_year_label)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = monthExpanded,
                onDismissRequest = { monthExpanded = false }
            ) {
                monthNames.forEachIndexed { index, monthName ->
                    DropdownMenuItem(
                        text = { Text(monthName) },
                        onClick = {
                            onMonthChange(index + 1)
                            monthExpanded = false
                        }
                    )
                }
            }
        }
        
        // Day selector
        MonthlySelector(
            selectedDay = selectedDay,
            onDayChange = onDayChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSoundSelector(
    selectedSound: NotificationSound,
    availableSounds: List<NotificationSound>,
    onSoundChange: (NotificationSound) -> Unit
) {
    val context = LocalContext.current
    var soundExpanded by remember { mutableStateOf(false) }
    var soundPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    
    // Cleanup media player when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            soundPlayer?.release()
            soundPlayer = null
        }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.notification_sound_label),
                style = MaterialTheme.typography.titleMedium
            )
            
            // Preview button for selected sound
            if (selectedSound.id != NotificationSound.DEFAULT_ID) {
                IconButton(
                    onClick = {
                        try {
                            soundPlayer?.release()
                            soundPlayer = null
                            
                            val uri = NotificationSound.getActualUri(context, selectedSound)
                            if (uri != null) {
                                soundPlayer = android.media.MediaPlayer().apply {
                                    setDataSource(context, uri)
                                    setAudioAttributes(
                                        android.media.AudioAttributes.Builder()
                                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                                            .build()
                                    )
                                    prepare()
                                    start()
                                    setOnCompletionListener { player ->
                                        player.release()
                                        soundPlayer = null
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SoundPreview", "Error playing sound: ${e.message}")
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Preview sound"
                    )
                }
            }
        }
        
        ExposedDropdownMenuBox(
            expanded = soundExpanded,
            onExpandedChange = { soundExpanded = !soundExpanded }
        ) {
            OutlinedTextField(
                value = selectedSound.displayName,
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(id = R.string.notification_sound_hint)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = soundExpanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = soundExpanded,
                onDismissRequest = { soundExpanded = false },
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                if (availableSounds.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Loading sounds...") },
                        onClick = { }
                    )
                } else {
                    availableSounds.forEach { sound ->
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sound.displayName)
                                    if (sound.id == selectedSound.id) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onSoundChange(sound)
                                soundExpanded = false
                                
                                // Stop any playing sound
                                soundPlayer?.release()
                                soundPlayer = null
                            },
                            trailingIcon = if (sound.id != NotificationSound.DEFAULT_ID && sound.id != NotificationSound.SYSTEM_DEFAULT_ID) {
                                {
                                    IconButton(
                                        onClick = {
                                            try {
                                                soundPlayer?.release()
                                                soundPlayer = null
                                                
                                                val uri = NotificationSound.getActualUri(context, sound)
                                                if (uri != null) {
                                                    soundPlayer = android.media.MediaPlayer().apply {
                                                        setDataSource(context, uri)
                                                        setAudioAttributes(
                                                            android.media.AudioAttributes.Builder()
                                                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                                                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                                                                .build()
                                                        )
                                                        prepare()
                                                        start()
                                                        setOnCompletionListener { player ->
                                                            player.release()
                                                            soundPlayer = null
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("SoundPreview", "Error playing sound: ${e.message}")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Preview",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarSelector(
    selectedAvatar: HabitAvatar,
    onAvatarChange: (HabitAvatar) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.avatar_label),
            style = MaterialTheme.typography.titleMedium
        )
        
        // Current avatar display
        AvatarDisplay(
            avatar = selectedAvatar,
            size = 56.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        // Emoji selection grid
        Text(
            text = stringResource(id = R.string.choose_emoji_label),
            style = MaterialTheme.typography.bodyMedium
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(120.dp)
        ) {
            items(HabitAvatar.POPULAR_EMOJIS) { emoji ->
                EmojiItem(
                    emoji = emoji,
                    isSelected = selectedAvatar.value == emoji && selectedAvatar.type == HabitAvatarType.EMOJI,
                    onClick = {
                        onAvatarChange(
                            selectedAvatar.copy(
                                type = HabitAvatarType.EMOJI,
                                value = emoji
                            )
                        )
                    }
                )
            }
        }
        
        // Background color selection
        Text(
            text = stringResource(id = R.string.background_color_label),
            style = MaterialTheme.typography.bodyMedium
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(HabitAvatar.BACKGROUND_COLORS) { color ->
                ColorItem(
                    color = color,
                    isSelected = selectedAvatar.backgroundColor == color,
                    onClick = {
                        onAvatarChange(selectedAvatar.copy(backgroundColor = color))
                    }
                )
            }
        }
    }
}

@Composable
private fun AvatarDisplay(
    avatar: HabitAvatar,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(
                color = Color(android.graphics.Color.parseColor(avatar.backgroundColor)),
                shape = CircleShape
            )
    ) {
        when (avatar.type) {
            HabitAvatarType.EMOJI -> {
                Text(
                    text = avatar.value,
                    fontSize = (size.value * 0.6f).sp,
                    textAlign = TextAlign.Center
                )
            }
            HabitAvatarType.DEFAULT_ICON -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size((size.value * 0.6f).dp)
                )
            }
            HabitAvatarType.CUSTOM_IMAGE -> {
                // For future implementation of custom images
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size((size.value * 0.6f).dp)
                )
            }
        }
    }
}

@Composable
private fun EmojiItem(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                       else MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickableOnce(debounceTime = 300L) { onClick() }
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ColorItem(
    color: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                color = Color(android.graphics.Color.parseColor(color)),
                shape = CircleShape
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickableOnce(debounceTime = 300L) { onClick() }
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(16.dp)
            )
        }
    }
}

@Composable
private fun rememberNotificationPermissionState(): MutableState<Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val state = remember { mutableStateOf(checkNotificationPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.value = checkNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return state
}

private fun checkNotificationPermission(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

internal data class CardPalette(val brush: Brush, val accent: Color)

internal fun cardPaletteFor(habitId: Long): CardPalette {
    val palettes = listOf(
        listOf(Color(0xFF6650A4), Color(0xFF9575CD)),
        listOf(Color(0xFF006C62), Color(0xFF00BFA6)),
        listOf(Color(0xFF7B1FA2), Color(0xFFE040FB)),
        listOf(Color(0xFF3949AB), Color(0xFF5C6BC0)),
        listOf(Color(0xFF00838F), Color(0xFF00ACC1))
    )
    val paletteCount = palettes.size.toLong()
    val index = ((habitId % paletteCount) + paletteCount) % paletteCount
    val colors = palettes[index.toInt()]
    return CardPalette(
        brush = Brush.linearGradient(colors),
        accent = colors.last()
    )
}

// Ensure your AddHabitState data class in the ViewModel (or wherever it's defined) includes:
// val title: String
// val description: String
// val reminderEnabled: Boolean
// val hour: Int
// val minute: Int
// val nameError: String? (or similar for validation feedback)
// val isSaving: Boolean

/*
data class AddHabitState(
    val title: String = "",
    val description: String = "",
    val reminderEnabled: Boolean = false,
    val hour: Int = java.time.LocalTime.now().hour,
    val minute: Int = java.time.LocalTime.now().minute,
    val nameError: String? = null,
    val isSaving: Boolean = false
)
*/

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
