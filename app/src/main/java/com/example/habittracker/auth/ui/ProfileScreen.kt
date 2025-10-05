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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.airbnb.lottie.compose.*
import it.atraj.habittracker.R
import it.atraj.habittracker.ui.HabitViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlitteringProfilePhoto(
    modifier: Modifier = Modifier,
    showProfilePhoto: Boolean,
    photoUrl: String?,
    currentAvatar: String,
    avatarLoaded: Boolean,
    onClick: () -> Unit
) {
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "glitter_animation")
    
    // Rotating gradient animation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Pulsing scale animation for shimmer effect
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Opacity animation for sparkles
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    // Multiple sparkle positions
    val sparkle1Angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle1"
    )
    
    val sparkle2Angle by infiniteTransition.animateFloat(
        initialValue = 120f,
        targetValue = 480f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle2"
    )
    
    val sparkle3Angle by infiniteTransition.animateFloat(
        initialValue = 240f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle3"
    )
    
    Box(
        modifier = modifier
            .size(120.dp) // Increased size to accommodate glitter effect
            .drawBehind {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = size.minDimension / 2 - 10.dp.toPx()
                
                // Draw rotating gradient border
                val gradientColors = listOf(
                    Color(0xFFFFD700), // Gold
                    Color(0xFFFFE55C), // Light gold
                    Color(0xFFFFFFFF), // White
                    Color(0xFFFFE55C), // Light gold
                    Color(0xFFFFD700), // Gold
                    Color(0xFFFFA500), // Orange
                    Color(0xFFFFD700)  // Gold
                )
                
                val brush = Brush.sweepGradient(
                    colors = gradientColors,
                    center = Offset(centerX, centerY)
                )
                
                // Draw outer glowing ring
                drawCircle(
                    brush = brush,
                    radius = radius + 8.dp.toPx(),
                    center = Offset(centerX, centerY),
                    alpha = 0.6f,
                    style = Stroke(width = 4.dp.toPx())
                )
                
                // Draw middle pulsing ring
                drawCircle(
                    brush = brush,
                    radius = (radius + 8.dp.toPx()) * scale,
                    center = Offset(centerX, centerY),
                    alpha = alpha * 0.4f,
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Draw sparkles at different positions
                fun drawSparkle(angle: Float, distance: Float) {
                    val radians = Math.toRadians(angle.toDouble())
                    val sparkleX = centerX + (distance * cos(radians)).toFloat()
                    val sparkleY = centerY + (distance * sin(radians)).toFloat()
                    
                    // Draw sparkle as a small star
                    val sparkleSize = 6.dp.toPx()
                    drawCircle(
                        color = Color.White,
                        radius = sparkleSize,
                        center = Offset(sparkleX, sparkleY),
                        alpha = alpha
                    )
                    drawCircle(
                        color = Color(0xFFFFD700),
                        radius = sparkleSize * 0.6f,
                        center = Offset(sparkleX, sparkleY),
                        alpha = alpha * 0.8f
                    )
                }
                
                // Draw multiple sparkles
                val sparkleDistance = radius + 12.dp.toPx()
                drawSparkle(sparkle1Angle, sparkleDistance)
                drawSparkle(sparkle2Angle, sparkleDistance)
                drawSparkle(sparkle3Angle, sparkleDistance)
            }
            .rotate(rotationAngle)
            .clickableOnce(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Inner profile photo container
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    4.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                        )
                    ),
                    CircleShape
                )
                .rotate(-rotationAngle), // Counter-rotate to keep photo upright
            contentAlignment = Alignment.Center
        ) {
            // Only show avatar content when data is loaded
            if (avatarLoaded) {
                Crossfade(
                    targetState = Pair(showProfilePhoto, currentAvatar),
                    label = "avatar_crossfade"
                ) { (isPhoto, emoji) ->
                    if (isPhoto) {
                        // Load Google profile photo with Coil in high quality
                        val context = androidx.compose.ui.platform.LocalContext.current
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
                    } else {
                        // Custom avatar (both Google and Email users can use this)
                        Text(
                            text = emoji,
                            fontSize = 48.sp,
                            modifier = Modifier.padding(8.dp)
                        )
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
    onLanguageSettingsClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val habitState by habitViewModel.uiState.collectAsStateWithLifecycle()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showResetAvatarDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showSetNameDialog by remember { mutableStateOf(false) }
    var showAnimationPicker by remember { mutableStateOf(false) }
    
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

    // Get the current avatar to display
    val currentAvatar = state.user?.customAvatar ?: "😊"
    
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
    val completedThisWeek = habitState.habits.count { it.isCompletedToday }
    val completionPercentage = if (activeHabits > 0) 
        (completedThisWeek * 100) / activeHabits else 0

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
                .verticalScroll(rememberScrollState())
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
                    // Background gradient layer (bottom)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    )
                                )
                            )
                    )
                    
                    // Animation overlay layer (middle) - fills entire card
                    if (selectedAnimation != "none") {
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
                            else -> 1f // Full opacity for other animations
                        }
                        
                        animationFile?.let { file ->
                            val composition by rememberLottieComposition(
                                LottieCompositionSpec.Asset(file)
                            )
                            
                            val progress by animateLottieCompositionAsState(
                                composition = composition,
                                iterations = LottieConstants.IterateForever,
                                isPlaying = true,
                                speed = 1f, // Normal speed for smoother playback
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
                            onClick = { showAvatarPicker = true }
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

            // Notification Setup Guide Card - Highlighted
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .clickableOnce { onNotificationGuideClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Language Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clickableOnce { onLanguageSettingsClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Check for Updates Card - App Settings
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clickableOnce { onCheckForUpdates() },
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Edit Name (all users)
                    ProfileActionItem(
                        icon = Icons.Default.Edit,
                        title = "Edit Name",
                        subtitle = "Change your display name",
                        onClick = { showEditNameDialog = true }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    // Change Avatar (all users)
                    ProfileActionItem(
                        icon = Icons.Default.Face,
                        title = "Change Avatar",
                        subtitle = "Select a custom emoji avatar",
                        onClick = { showAvatarPicker = true }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    // Profile Animation (new feature)
                    ProfileActionItem(
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
                        onClick = { showAnimationPicker = true }
                    )
                    
                    // Reset Avatar (show only if user has custom avatar set)
                    if (state.user?.customAvatar != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        ProfileActionItem(
                            icon = Icons.Default.Refresh,
                            title = "Reset Avatar",
                            subtitle = if (state.user?.photoUrl != null) 
                                "Return to Google profile picture" 
                            else 
                                "Return to default emoji",
                            onClick = { showResetAvatarDialog = true },
                            iconTint = MaterialTheme.colorScheme.secondary,
                            titleColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Sign Out
                    ProfileActionItem(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        title = "Sign Out",
                        subtitle = "Sign out of your account",
                        onClick = { showSignOutDialog = true },
                        iconTint = MaterialTheme.colorScheme.error,
                        titleColor = MaterialTheme.colorScheme.error
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

    // Avatar Picker Dialog
    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentAvatar = currentAvatar,
            onAvatarSelected = { avatar ->
                viewModel.updateCustomAvatar(avatar)
                showAvatarPicker = false
            },
            onDismiss = { showAvatarPicker = false }
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
    val avatarEmojis = listOf(
        "😊", "😎", "🤗", "🥳", "🤓", 
        "😇", "🤠", "🥰", "😄", "🙂",
        "🦸", "🧑‍💼", "👨‍🎓", "👩‍🎓", "🧑‍🚀",
        "🦊", "🐱", "🐶", "🐼", "🐨",
        "🌟", "⭐", "✨", "💫", "🎯"
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
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(avatarEmojis) { emoji ->
                    val isSelected = emoji == currentAvatar
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickableOnce(debounceTime = 300L) {
                                onAvatarSelected(emoji)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 32.sp
                        )
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
