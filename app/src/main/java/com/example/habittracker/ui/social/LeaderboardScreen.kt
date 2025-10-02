package com.example.habittracker.ui.social

import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.habittracker.auth.ui.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    socialViewModel: SocialViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val socialState by socialViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var previousRank by remember { mutableStateOf<Int?>(null) }
    var showRankImproved by remember { mutableStateOf(false) }

    // Set current user and load leaderboard
    LaunchedEffect(authState.user) {
        authState.user?.let { 
            socialViewModel.setCurrentUser(it)
            socialViewModel.loadLeaderboard()
        }
    }

    // Check for rank improvement
    LaunchedEffect(socialState.leaderboard) {
        val currentUserEntry = socialState.leaderboard.find { it.isCurrentUser }
        if (currentUserEntry != null) {
            if (previousRank != null && currentUserEntry.rank < previousRank!!) {
                // Rank improved!
                showRankImproved = true
                
                // Play success sound (optional - will fail silently if sound doesn't exist)
                try {
                    // Note: Add a success_sound.mp3 file to res/raw/ folder for sound feedback
                    // val mediaPlayer = MediaPlayer.create(context, R.raw.success_sound)
                    // mediaPlayer?.start()
                    // mediaPlayer?.setOnCompletionListener { it.release() }
                } catch (e: Exception) {
                    // Sound not available - continue without it
                }
                
                delay(3000)
                showRankImproved = false
            }
            previousRank = currentUserEntry.rank
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Leaderboard",
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
                    IconButton(
                        onClick = { socialViewModel.loadLeaderboard() },
                        enabled = !socialState.isLoadingLeaderboard
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (socialState.isLoadingLeaderboard) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading leaderboard...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (socialState.leaderboard.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No Leaderboard Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Add friends to see rankings based on habit completion rates",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    item {
                        LeaderboardHeader()
                    }

                    // Top 3
                    item {
                        if (socialState.leaderboard.size >= 3) {
                            TopThreeSection(
                                entries = socialState.leaderboard.take(3)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Rest of the leaderboard
                    itemsIndexed(
                        items = if (socialState.leaderboard.size > 3) 
                            socialState.leaderboard.drop(3) 
                        else 
                            socialState.leaderboard,
                        key = { _, entry -> entry.profile.userId }
                    ) { index, entry ->
                        LeaderboardEntryCard(
                            entry = entry,
                            animationDelay = index * 50
                        )
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Rank improved banner
            AnimatedVisibility(
                visible = showRankImproved,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = paddingValues.calculateTopPadding() + 16.dp)
            ) {
                RankImprovedBanner()
            }
        }
    }
}

@Composable
fun LeaderboardHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Rankings are based on your habit success rate. Complete more habits to climb up!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TopThreeSection(entries: List<LeaderboardEntry>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd place
        if (entries.size >= 2) {
            TopThreeCard(
                entry = entries[1],
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 20.dp)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // 1st place (larger)
        if (entries.isNotEmpty()) {
            TopThreeCard(
                entry = entries[0],
                modifier = Modifier.weight(1f),
                isFirst = true
            )
        }

        // 3rd place
        if (entries.size >= 3) {
            TopThreeCard(
                entry = entries[2],
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 40.dp)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun TopThreeCard(
    entry: LeaderboardEntry,
    modifier: Modifier = Modifier,
    isFirst: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(entry.rank * 100L)
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val medalColor = when (entry.rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isCurrentUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Medal/Rank
            Box(
                modifier = Modifier
                    .size(if (isFirst) 48.dp else 40.dp)
                    .clip(CircleShape)
                    .background(medalColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.rank.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = if (isFirst) 24.sp else 20.sp
                )
            }

            // Avatar - show photo if available, otherwise emoji
            if (entry.profile.photoUrl != null && entry.profile.customAvatar == "😊") {
                // Google profile picture
                AsyncImage(
                    model = entry.profile.photoUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(if (isFirst) 56.dp else 48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                // Custom emoji avatar
                Box(
                    modifier = Modifier
                        .size(if (isFirst) 56.dp else 48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.profile.customAvatar,
                        fontSize = if (isFirst) 28.sp else 24.sp
                    )
                }
            }

            // Name
            Text(
                text = if (entry.isCurrentUser) "You" else entry.profile.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            // Success rate
            Text(
                text = "${entry.profile.successRate}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LeaderboardEntryCard(
    entry: LeaderboardEntry,
    animationDelay: Int
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }

    val offsetX by animateDpAsState(
        targetValue = if (visible) 0.dp else 50.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "offset"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetX)
            .alpha(alpha),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isCurrentUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rank
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (entry.isCurrentUser)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.rank.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.isCurrentUser)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.surface
                )
            }

            // Avatar - show photo if available, otherwise emoji
            if (entry.profile.photoUrl != null && entry.profile.customAvatar == "😊") {
                // Google profile picture
                AsyncImage(
                    model = entry.profile.photoUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                // Custom emoji avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.profile.customAvatar,
                        fontSize = 24.sp
                    )
                }
            }

            // User info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (entry.isCurrentUser) "You" else entry.profile.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${entry.profile.totalCompletions}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "${entry.profile.currentStreak}d",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Success rate
            Text(
                text = "${entry.profile.successRate}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun RankImprovedBanner() {
    val infiniteTransition = rememberInfiniteTransition(label = "shine")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.8f + shimmer * 0.2f),
                            Color(0xFFFFA500).copy(alpha = 0.8f + shimmer * 0.2f),
                            Color(0xFFFFD700).copy(alpha = 0.8f + shimmer * 0.2f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "🎉 Rank Improved!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "You climbed up the leaderboard!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
