package it.atraj.habittracker.auth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import it.atraj.habittracker.util.clickableOnce
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.airbnb.lottie.compose.*
import it.atraj.habittracker.R
import it.atraj.habittracker.ui.HabitViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

@Composable
fun GlitteringProfilePhoto(
    modifier: Modifier = Modifier,
    showProfilePhoto: Boolean,
    photoUrl: String?,
    currentAvatar: String,
    avatarLoaded: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    // Optimized: Use derivedStateOf to reduce recompositions
    val shouldAnimate by remember { derivedStateOf { avatarLoaded } }
    
    // Animation states - only animate when loaded
    val infiniteTransition = rememberInfiniteTransition(label = "glitter_animation")
    
    // Simplified rotation animation (reduced from 4 to 1 animation for performance)
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Simplified pulse (reduced complexity)
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(
        modifier = modifier
            .size(120.dp)
            .graphicsLayer {
                // Use graphicsLayer for better performance
                if (shouldAnimate) {
                    rotationZ = rotationAngle
                }
            }
            .drawBehind {
                if (shouldAnimate) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = size.minDimension / 2 - 10.dp.toPx()
                    
                    // Simplified gradient border (reduced colors for performance)
                    val gradientColors = listOf(
                        Color(0xFFFFD700), // Gold
                        Color(0xFFFFE55C), // Light gold
                        Color(0xFFFFFFFF), // White
                        Color(0xFFFFD700)  // Gold
                    )
                    
                    val brush = Brush.sweepGradient(
                        colors = gradientColors,
                        center = Offset(centerX, centerY)
                    )
                    
                    // Single glowing ring (reduced from multiple rings)
                    drawCircle(
                        brush = brush,
                        radius = radius + 8.dp.toPx(),
                        center = Offset(centerX, centerY),
                        alpha = pulseAlpha,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner profile photo container
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    3.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    CircleShape
                )
                .graphicsLayer {
                    // Counter-rotate to keep photo upright
                    if (shouldAnimate) {
                        rotationZ = -rotationAngle
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Only show avatar content when data is loaded
            if (avatarLoaded) {
                Crossfade(
                    targetState = Pair(showProfilePhoto, currentAvatar),
                    label = "avatar_crossfade"
                ) { (isPhoto, avatarValue) ->
                    val context = androidx.compose.ui.platform.LocalContext.current
                    
                    if (isPhoto) {
                        // Load Google profile photo with Coil in high quality
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photoUrl)
                                .size(Size.ORIGINAL) // Load original high-quality image
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else if (avatarValue.startsWith("https://")) {
                        // Custom avatar from GitHub (image URL)
                        // Add GitHub token for private repo authentication
                        val token = it.atraj.habittracker.avatar.SecureTokenStorage.getToken(context)
                        val requestBuilder = ImageRequest.Builder(context)
                            .data(avatarValue)
                            .size(Size.ORIGINAL)
                            .crossfade(true)
                        
                        // Add Authorization header if token is available (for private repos)
                        if (token != null) {
                            requestBuilder.addHeader("Authorization", "token $token")
                        }
                        
                        AsyncImage(
                            model = requestBuilder.build(),
                            contentDescription = "Custom avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback - should not be used anymore, but keeping for safety
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default avatar",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            } else {
                // Show a subtle loading indicator while fetching avatar
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    habitViewModel: HabitViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onSignedOut: () -> Unit,
    onStatisticsClick: () -> Unit = {},
    onSearchUsersClick: () -> Unit = {},
    onFriendsListClick: () -> Unit = {},
    onLeaderboardClick: () -> Unit = {},
    onNotificationGuideClick: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onLanguageSettingsClick: () -> Unit = {},
    onEmailSettingsClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val habitState by habitViewModel.uiState.collectAsStateWithLifecycle()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showResetAvatarDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showSetNameDialog by remember { mutableStateOf(false) }
    var showAnimationPicker by remember { mutableStateOf(false) }
    var showEnlargedPhotoDialog by remember { mutableStateOf(false) }
    var showMusicDialog by remember { mutableStateOf(false) }
    
    // Profile card animation preference (stored in SharedPreferences)
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("profile_prefs", android.content.Context.MODE_PRIVATE) }
    var selectedAnimation by remember { mutableStateOf(prefs.getString("profile_animation", "none") ?: "none") }
    
    // Track if avatar data has been loaded at least once to prevent flash
    var avatarLoaded by remember { mutableStateOf(false) }
    
    // Check if user needs to set name (empty customDisplayName for email users)
    val needsToSetName = state.user?.customDisplayName?.isEmpty() == true
    
    // Show dialog to set name for new email users
    LaunchedEffect(state.user) {
        if (state.user != null) {
            avatarLoaded = true
            if (needsToSetName && !showSetNameDialog) {
                showSetNameDialog = true
            }
            // Refresh user stats when profile is viewed
            habitViewModel.refreshUserStats()
        }
    }

    // Get the current avatar to display (URL string or fallback)
    val currentAvatar = state.user?.customAvatar ?: "https://raw.githubusercontent.com/atrajit-sarkar/HabitTracker/main/Avatars/avatar_1_professional.png"
    
    // Determine if we should show profile photo or custom avatar
    val showProfilePhoto = state.user?.photoUrl != null && state.user?.customAvatar == null

    // Handle sign out navigation
    var hasInitialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(state.user) {
        if (hasInitialized && state.user == null) {
            onSignedOut()
        } else if (state.user != null) {
            hasInitialized = true
        }
    }

    // Calculate stats
    val activeHabits = habitState.habits.size
    val totalCompletions = habitState.habits.count { it.isCompletedToday }
    val completionPercentage = if (activeHabits > 0) 
        (totalCompletions * 100) / activeHabits else 0
    
    // Fetch pre-calculated completedThisWeek from Firebase (optimized like leaderboard)
    var completedThisWeek by remember { mutableStateOf(0) }
    
    LaunchedEffect(state.user?.uid) {
        state.user?.let { user ->
            try {
                // Fetch the user's public profile which contains pre-calculated completedThisWeek
                val profile = it.atraj.habittracker.data.firestore.FriendRepository(
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                ).getFriendProfile(user.uid)
                
                completedThisWeek = profile?.completedThisWeek ?: 0
            } catch (e: Exception) {
                // Silently handle errors
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.SemiBold) },
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
                .verticalScroll(
                    state = rememberScrollState(),
                    flingBehavior = ScrollableDefaults.flingBehavior()
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Background gradient layer (bottom) - Optimized with remember
                    val gradientBrush = remember {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0x26000000), // Use hex colors to avoid theme lookups
                                Color(0x26000000)
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                    
                    // Animation overlay layer (middle) - fills entire card
                    // Optimized: Only load animation composition when selected
                    if (selectedAnimation != "none") {
                        key(selectedAnimation) { // Add key to prevent re-composition issues
                            val animationFile = when (selectedAnimation) {
                                "sakura" -> "sakura_fall.json"
                                "worldwide" -> "worldwide.json"
                                "cute_anime_girl" -> "cute_anime_girl.json"
                                "fireblast" -> "fireblast.json"
                                else -> null
                            }
                            
                            // Set alpha based on animation type for better text visibility
                            val animationAlpha = when (selectedAnimation) {
                                "cute_anime_girl" -> 0.5f // Dimmed for better text readability
                                else -> 0.8f // Slightly reduced for performance
                            }
                            
                            animationFile?.let { file ->
                                val composition by rememberLottieComposition(
                                    LottieCompositionSpec.Asset(file)
                                )
                                
                                // Optimized: Use remembered speed value
                                val animationSpeed = remember { 0.8f }
                                
                                val progress by animateLottieCompositionAsState(
                                    composition = composition,
                                    iterations = LottieConstants.IterateForever,
                                    isPlaying = true,
                                    speed = animationSpeed, // Reduced speed for smoother performance
                                    restartOnPlay = true
                                )
                                
                                LottieAnimation(
                                    composition = composition,
                                    progress = { progress },
                                    modifier = Modifier
                                        .matchParentSize() // Fills entire card
                                        .alpha(animationAlpha)
                                )
                            }
                        }
                    }
                    
                    // Profile content layer (top) - rendered above animation
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profile Picture with glittering animation
                        GlitteringProfilePhoto(
                            showProfilePhoto = showProfilePhoto,
                            photoUrl = state.user?.photoUrl,
                            currentAvatar = currentAvatar,
                            avatarLoaded = avatarLoaded,
                            onClick = { showAvatarPicker = true },
                            onLongPress = { 
                                // Show enlarged photo dialog only if there's an image to show
                                if (showProfilePhoto || currentAvatar.startsWith("https://")) {
                                    showEnlargedPhotoDialog = true
                                }
                            }
                        )

                        // User Info
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = state.user?.effectiveDisplayName ?: "User",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = state.user?.email ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            
                            // Account type badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (state.user?.photoUrl != null) Icons.Default.Person else Icons.Default.Email,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (state.user?.photoUrl != null) "Google Account" else "Email Account",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Stats Cards
            Text(
                text = stringResource(R.string.your_statistics),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active Habits
                StatsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    title = "Active",
                    value = activeHabits.toString(),
                    subtitle = "Habits",
                    color = MaterialTheme.colorScheme.tertiary
                )

                // Total Completions Today
                StatsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Star,
                    title = "Today",
                    value = totalCompletions.toString(),
                    subtitle = "Completed",
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Completion rate
                StatsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Favorite,
                    title = "Rate",
                    value = "$completionPercentage%",
                    subtitle = "Completion",
                    color = MaterialTheme.colorScheme.error
                )

                // This Week
                StatsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DateRange,
                    title = "This Week",
                    value = completedThisWeek.toString(),
                    subtitle = "Completed",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // View Detailed Statistics Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .clickableOnce { onStatisticsClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.detailed_analytics),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.charts_trends_comparisons),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Social Features Section
            Text(
                text = stringResource(R.string.social_and_friends),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            // Social Features Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SocialFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PersonSearch,
                    title = "Search",
                    subtitle = "Find friends",
                    onClick = onSearchUsersClick
                )
                SocialFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Group,
                    title = "Friends",
                    subtitle = "View list",
                    onClick = onFriendsListClick
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .clickableOnce { onLeaderboardClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.leaderboard),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = stringResource(R.string.compete_with_friends),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Account Section
            Text(
                text = stringResource(R.string.account_settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

// Stacked Account Settings Cards
            val feedbackContext = androidx.compose.ui.platform.LocalContext.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Notification Setup Guide
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableOnce { onNotificationGuideClick() }
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                )
                            )
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.notification_setup_guide),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.ensure_reliable_reminders),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open Guide",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Language Settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableOnce { onLanguageSettingsClick() }
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                    )
                                )
                            )
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.language_settings),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.select_language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.language_settings),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Email Notifications
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableOnce { onEmailSettingsClick() }
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                    )
                                )
                            )
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Email Notifications",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Get habit reminders via email",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Email Settings",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Check for Updates
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableOnce { onCheckForUpdates() }
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                )
                            )
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.check_for_updates),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.get_latest_features),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Check Updates",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Send Feedback
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableOnce {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://github.com/atrajit-sarkar/HabitTracker/issues/new/choose")
                                )
                                feedbackContext.startActivity(intent)
                            }
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    )
                                )
                            )
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Feedback,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Send Feedback",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Report bugs or suggest features",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Send Feedback",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Profile Settings Section
            Text(
                text = "Profile Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Edit Name
                    ProfileSettingRow(
                        icon = Icons.Default.Edit,
                        title = "Edit Name",
                        subtitle = "Change your display name",
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        ),
                        iconTint = MaterialTheme.colorScheme.primary,
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = { showEditNameDialog = true }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Change Avatar
                    ProfileSettingRow(
                        icon = Icons.Default.Face,
                        title = "Change Avatar",
                        subtitle = "Select a custom emoji avatar",
                        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        ),
                        iconTint = MaterialTheme.colorScheme.secondary,
                        textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { showAvatarPicker = true }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Profile Animation
                    ProfileSettingRow(
                        icon = Icons.Default.Animation,
                        title = "Profile Animation",
                        subtitle = when (selectedAnimation) {
                            "none" -> "None selected"
                            "sakura" -> "Sakura Fall"
                            "worldwide" -> "Worldwide"
                            "cute_anime_girl" -> "Cute Anime Girl"
                            "fireblast" -> "Fireblast"
                            else -> "None selected"
                        },
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = { showAnimationPicker = true }
                    )
                    
                    // Reset Avatar (show only if user has custom avatar set)
                    if (state.user?.customAvatar != null) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        ProfileSettingRow(
                            icon = Icons.Default.Refresh,
                            title = "Reset Avatar",
                            subtitle = if (state.user?.photoUrl != null) 
                                "Return to Google profile picture" 
                            else 
                                "Return to default emoji",
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                            gradientColors = listOf(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                            iconTint = MaterialTheme.colorScheme.secondary,
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = { showResetAvatarDialog = true }
                        )
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Background Music
                    ProfileSettingRow(
                        icon = Icons.Default.MusicNote,
                        title = "Background Music",
                        subtitle = if (state.user?.musicEnabled == true) "Music enabled" else "Music disabled",
                        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        ),
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = { showMusicDialog = true }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Sign Out
                    ProfileSettingRow(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        title = "Sign Out",
                        subtitle = "Sign out of your account",
                        backgroundColor = MaterialTheme.colorScheme.errorContainer,
                        gradientColors = listOf(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ),
                        iconTint = MaterialTheme.colorScheme.error,
                        textColor = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = { showSignOutDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // App Info Footer
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🎯",
                        fontSize = 36.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.app_name_display),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Avatar Picker Dialog - Now with upload functionality
    if (showAvatarPicker) {
        it.atraj.habittracker.avatar.ui.EnhancedAvatarPickerDialog(
            currentAvatar = currentAvatar,
            onAvatarSelected = { avatar ->
                viewModel.updateCustomAvatar(avatar)
                showAvatarPicker = false
            },
            onDismiss = { showAvatarPicker = false }
        )
    }
    
    // Music Settings Dialog
    if (showMusicDialog) {
        // Get music manager from MainActivity
        val activity = context as? it.atraj.habittracker.MainActivity
        val musicManager = activity?.let { 
            // Access the injected musicManager from MainActivity
            try {
                val field = it::class.java.getDeclaredField("musicManager")
                field.isAccessible = true
                field.get(it) as? it.atraj.habittracker.music.BackgroundMusicManager
            } catch (e: Exception) {
                null
            }
        }
        
        MusicSettingsDialog(
            currentEnabled = state.user?.musicEnabled ?: false,
            currentTrack = state.user?.musicTrack ?: "NONE",
            currentVolume = state.user?.musicVolume ?: 0.3f,
            onDismiss = { showMusicDialog = false },
            onSave = { enabled, track, volume ->
                viewModel.updateMusicSettings(enabled, track, volume)
                showMusicDialog = false
                
                // Apply music settings immediately
                musicManager?.let { manager ->
                    val musicTrack = try {
                        it.atraj.habittracker.music.BackgroundMusicManager.MusicTrack.valueOf(track)
                    } catch (e: Exception) {
                        it.atraj.habittracker.music.BackgroundMusicManager.MusicTrack.NONE
                    }
                    manager.setEnabled(enabled)
                    manager.changeSong(musicTrack)
                    manager.setVolume(volume)
                }
            }
        )
    }
    
    // Reset Avatar Confirmation Dialog
    if (showResetAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showResetAvatarDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            title = { 
                Text(
                    "Reset Avatar",
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    if (state.user?.photoUrl != null) 
                        "Reset to your Google profile picture?" 
                    else 
                        "Reset to the default emoji avatar?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCustomAvatar(null)
                        showResetAvatarDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAvatarDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { 
                Text(
                    "Sign Out",
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text("Are you sure you want to sign out? You'll need to sign in again to access your habits.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Edit Name Dialog
    if (showEditNameDialog) {
        EditNameDialog(
            currentName = state.user?.effectiveDisplayName ?: "",
            onDismiss = { showEditNameDialog = false },
            onSave = { newName ->
                viewModel.updateDisplayName(newName) {
                    showEditNameDialog = false
                }
            }
        )
    }
    
    // Set Name Dialog (for new email users)
    if (showSetNameDialog) {
        SetNameDialog(
            onDismiss = { }, // Cannot dismiss without setting name
            onSave = { newName ->
                viewModel.updateDisplayName(newName) {
                    showSetNameDialog = false
                }
            }
        )
    }
    
    // Animation Picker Dialog
    if (showAnimationPicker) {
        AnimationPickerDialog(
            currentSelection = selectedAnimation,
            onDismiss = { showAnimationPicker = false },
            onSelect = { animation ->
                selectedAnimation = animation
                prefs.edit().putString("profile_animation", animation).apply()
                showAnimationPicker = false
            }
        )
    }
    
    // Enlarged Photo Dialog
    if (showEnlargedPhotoDialog) {
        EnlargedPhotoDialog(
            photoUrl = if (showProfilePhoto) state.user?.photoUrl else currentAvatar,
            onDismiss = { showEnlargedPhotoDialog = false }
        )
    }
}

@Composable
private fun EnlargedPhotoDialog(
    photoUrl: String?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                val context = androidx.compose.ui.platform.LocalContext.current
                
                // Add GitHub token for private repo authentication if it's a GitHub URL
                val token = it.atraj.habittracker.avatar.SecureTokenStorage.getToken(context)
                val requestBuilder = ImageRequest.Builder(context)
                    .data(photoUrl)
                    .size(Size.ORIGINAL)
                    .crossfade(true)
                
                // Add Authorization header if token is available and it's a GitHub URL
                if (token != null && photoUrl.contains("githubusercontent.com")) {
                    requestBuilder.addHeader("Authorization", "token $token")
                }
                
                AsyncImage(
                    model = requestBuilder.build(),
                    contentDescription = "Enlarged profile photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StatsCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ProfileSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    gradientColors: List<Color>,
    iconTint: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableOnce(onClick = onClick)
            .background(backgroundColor)
            .background(
                Brush.horizontalGradient(colors = gradientColors)
            )
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun ProfileActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableOnce(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.15f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun AvatarPickerDialog(
    currentAvatar: String,
    onAvatarSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // GitHub raw URLs for avatar images
    val avatarUrls = listOf(
        "https://raw.githubusercontent.com/atrajit-sarkar/HabitTracker/main/Avatars/avatar_1_professional.png",
        "https://raw.githubusercontent.com/atrajit-sarkar/HabitTracker/main/Avatars/avatar_2_casual.png",
        "https://raw.githubusercontent.com/atrajit-sarkar/HabitTracker/main/Avatars/avatar_3_creative.png",
        "https://raw.githubusercontent.com/atrajit-sarkar/HabitTracker/main/Avatars/avatar_4_modern.png",
        "https://raw.githubusercontent.com/atrajit-sarkar/HabitTracker/main/Avatars/avatar_5_artistic.png",
        "https://raw.githubusercontent.com/atrajit-sarkar/HabitTracker/main/Avatars/avatar_6_gemini_1.png",
        "https://raw.githubusercontent.com/atrajit-sarkar/HabitTracker/main/Avatars/avatar_7_gemini_2.png",
        "https://raw.githubusercontent.com/atrajit-sarkar/HabitTracker/main/Avatars/avatar_8_gemini_3.png"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Choose Your Avatar",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(avatarUrls) { url ->
                    val isSelected = url == currentAvatar
                    val context = androidx.compose.ui.platform.LocalContext.current
                    
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (isSelected) 4.dp else 2.dp,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickableOnce(debounceTime = 300L) {
                                onAvatarSelected(url)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(url)
                                .size(Size.ORIGINAL)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar option",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Show checkmark for selected avatar
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
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
private fun SocialFeatureCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AnimationPickerDialog(
    currentSelection: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Profile Animation",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Choose an animation overlay for your profile card",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // None option
                AnimationOption(
                    title = "None",
                    subtitle = "No animation",
                    icon = Icons.Default.Close,
                    isSelected = currentSelection == "none",
                    onClick = { onSelect("none") }
                )
                
                // Sakura Fall option
                AnimationOption(
                    title = "Sakura Fall",
                    subtitle = "Falling cherry blossoms",
                    icon = Icons.Default.FilterVintage,
                    isSelected = currentSelection == "sakura",
                    onClick = { onSelect("sakura") }
                )
                
                // Worldwide option
                AnimationOption(
                    title = "Worldwide",
                    subtitle = "Global connectivity",
                    icon = Icons.Default.Public,
                    isSelected = currentSelection == "worldwide",
                    onClick = { onSelect("worldwide") }
                )
                
                // Cute Anime Girl option
                AnimationOption(
                    title = "Cute Anime Girl",
                    subtitle = "Animated character",
                    icon = Icons.Default.EmojiEmotions,
                    isSelected = currentSelection == "cute_anime_girl",
                    onClick = { onSelect("cute_anime_girl") }
                )
                
                // Fireblast option
                AnimationOption(
                    title = "Fireblast",
                    subtitle = "Blazing fire effect",
                    icon = Icons.Default.Whatshot,
                    isSelected = currentSelection == "fireblast",
                    onClick = { onSelect("fireblast") }
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
private fun AnimationOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun MusicSettingsDialog(
    currentEnabled: Boolean,
    currentTrack: String,
    currentVolume: Float,
    onDismiss: () -> Unit,
    onSave: (Boolean, String, Float) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? it.atraj.habittracker.MainActivity
    val downloadManager = activity?.let {
        try {
            val field = it::class.java.getDeclaredField("downloadManager")
            field.isAccessible = true
            field.get(it) as? it.atraj.habittracker.music.MusicDownloadManager
        } catch (e: Exception) {
            null
        }
    }
    
    var enabled by remember { mutableStateOf(currentEnabled) }
    var selectedTrack by remember { mutableStateOf(currentTrack) }
    var volume by remember { mutableFloatStateOf(currentVolume) }
    
    // Track download states per file
    val downloadStates = remember { mutableStateMapOf<String, Pair<Boolean, Int>>() } // fileName -> (isDownloading, progress)
    
    val tracks = remember {
        listOf(
            "NONE" to ("No Music" to ""),
            "AMBIENT_1" to ("Peaceful Ambient" to "ambient_calm.mp3"),
            "AMBIENT_2" to ("Focus Flow" to "ambient_focus.mp3"),
            "AMBIENT_3" to ("Nature Sounds" to "ambient_nature.mp3"),
            "LOFI_1" to ("Lo-Fi Beats" to "lofi_chill.mp3"),
            "PIANO_1" to ("Piano Melody" to "piano_soft.mp3")
        )
    }
    
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        },
        title = {
            Text(
                "Background Music Settings",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Enable/Disable Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Music",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }
                
                HorizontalDivider()
                
                // Music Track Selection
                AnimatedVisibility(visible = enabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Music Track",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        tracks.forEach { (trackId, trackInfo) ->
                            val (trackName, fileName) = trackInfo
                            val isDownloaded = fileName.isEmpty() || downloadManager?.isMusicDownloaded(fileName) == true
                            val downloadState = downloadStates[fileName] ?: (false to 0)
                            val (isDownloading, progress) = downloadState
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedTrack == trackId)
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        else
                                            Color.Transparent
                                    )
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedTrack == trackId,
                                    onClick = { 
                                        if (trackId == "NONE" || isDownloaded) {
                                            selectedTrack = trackId
                                        }
                                    },
                                    enabled = trackId == "NONE" || isDownloaded
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(enabled = trackId == "NONE" || isDownloaded) { 
                                            if (trackId == "NONE" || isDownloaded) {
                                                selectedTrack = trackId
                                            }
                                        }
                                ) {
                                    Text(
                                        text = trackName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (trackId == "NONE" || isDownloaded) 
                                            MaterialTheme.colorScheme.onSurface 
                                        else 
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    if (isDownloading) {
                                        Text(
                                            text = "Downloading... $progress%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    } else if (!isDownloaded && fileName.isNotEmpty()) {
                                        Text(
                                            text = "Tap download to use",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                // Download button or status icon
                                if (fileName.isNotEmpty()) {
                                    if (isDownloading) {
                                        Box(
                                            modifier = Modifier.size(40.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                progress = progress / 100f,
                                                modifier = Modifier.size(32.dp),
                                                strokeWidth = 3.dp,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                            Text(
                                                text = "$progress",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp
                                            )
                                        }
                                    } else if (isDownloaded) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Downloaded",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                downloadStates[fileName] = true to 0
                                                scope.launch {
                                                    downloadManager?.downloadMusic(fileName) { downloadProgress ->
                                                        downloadStates[fileName] = true to downloadProgress
                                                    }?.onSuccess {
                                                        downloadStates[fileName] = false to 100
                                                    }?.onFailure {
                                                        downloadStates.remove(fileName)
                                                    }
                                                }
                                            }
                                        ) {
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
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Volume Slider
                        Text(
                            text = "Volume",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(volume * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Slider(
                            value = volume,
                            onValueChange = { volume = it },
                            valueRange = 0f..1f,
                            steps = 19 // 5% increments
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(enabled, selectedTrack, volume) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
