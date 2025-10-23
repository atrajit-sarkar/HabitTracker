package it.atraj.habittracker.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import it.atraj.habittracker.R
import it.atraj.habittracker.data.local.Habit
import it.atraj.habittracker.data.local.HabitAvatar
import it.atraj.habittracker.data.local.HabitAvatarType
import it.atraj.habittracker.data.local.HabitFrequency
import androidx.core.graphics.toColorInt
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import it.atraj.habittracker.data.local.HabitCompletion
import it.atraj.habittracker.data.StreakCalculator

data class HabitProgress(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int,
    val completionRate: Float,
    val completedDates: Set<LocalDate>,
    val completions: List<HabitCompletion> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailsScreen(
    habit: Habit,
    progress: HabitProgress,
    selectedDate: LocalDate,
    isSelectedDateCompleted: Boolean,
    userRewards: it.atraj.habittracker.data.local.UserRewards,
    onBackClick: () -> Unit,
    onSelectDate: (LocalDate) -> Unit = { _ -> },
    onMarkCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = habit.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Section with Avatar and Quick Stats
            HeroSection(
                habit = habit,
                progress = progress,
                selectedDate = selectedDate,
                isSelectedDateCompleted = isSelectedDateCompleted,
                onMarkCompleted = onMarkCompleted
            )

            // Progress Stats Cards
            ProgressStatsSection(habit = habit, progress = progress)
            
            // Rewards and Streak Info Section
            StreakRewardsSection(
                habit = habit,
                userRewards = userRewards
            )

            // Calendar View
            CalendarSection(
                habit = habit,
                completedDates = progress.completedDates,
                selectedDate = selectedDate,
                onDateSelected = onSelectDate,
                progress = progress,
                userRewards = userRewards
            )

            // Habit Information
            HabitInfoSection(habit = habit)

            // Bottom spacing
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroSection(
    habit: Habit,
    progress: HabitProgress,
    selectedDate: LocalDate,
    isSelectedDateCompleted: Boolean,
    onMarkCompleted: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL) }
    val selectedDateText = remember(selectedDate) { dateFormatter.format(selectedDate) }
    val isToday = selectedDate == LocalDate.now()
    val canMarkSelectedDate = !isSelectedDateCompleted && !selectedDate.isAfter(LocalDate.now())
    
    var showTitleDialog by remember { mutableStateOf(false) }
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var isTitleTruncated by remember { mutableStateOf(false) }
    var isDescriptionTruncated by remember { mutableStateOf(false) }
    var showFanfareAnimation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar
            HabitAvatarDisplay(
                avatar = habit.avatar,
                size = 80.dp
            )

            // Habit Title and Description with Info Buttons
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
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
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Show full title",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (habit.description.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = habit.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { textLayoutResult ->
                            isDescriptionTruncated = textLayoutResult.hasVisualOverflow
                        },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isDescriptionTruncated) {
                        IconButton(
                            onClick = { showDescriptionDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Show full description",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Current Streak with Animated Fire (using habit.streak for consistency)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedFireIcon(
                    isActive = isSelectedDateCompleted && isToday,
                    streakCount = habit.streak
                )
                Text(
                    text = stringResource(R.string.current_streak_days, habit.streak),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            
            // Streak Rules Explanation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Streak Rules",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "• First missed day = 🧊 Free grace (no penalty)\n• Additional missed days = ❄️ Use freeze days or -1 streak per day\n• Streak updates automatically daily\n• Earn 💎 20 diamonds every 10 days, +100 at 100-day milestones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }

            // Selected date information
            Text(
                text = stringResource(R.string.selected_date, selectedDateText),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )

            // Complete Button with Animation
            if (isSelectedDateCompleted) {
                // Success animation
                val successScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "success_scale"
                )
                
                AssistChip(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.graphicsLayer {
                        scaleX = successScale
                        scaleY = successScale
                    },
                    label = {
                        Text(
                            text = if (isToday) {
                                stringResource(R.string.completed_today)
                            } else {
                                stringResource(R.string.completed_on, selectedDateText)
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        labelColor = MaterialTheme.colorScheme.primary,
                        leadingIconContentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        disabledLabelColor = MaterialTheme.colorScheme.primary,
                        disabledLeadingIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            } else {
                // Pulsing call-to-action for incomplete days
                val infiniteTransition = rememberInfiniteTransition(label = "button_pulse")
                val buttonAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "button_alpha"
                )
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = {
                            showFanfareAnimation = true
                            onMarkCompleted()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = if (canMarkSelectedDate) buttonAlpha else 0.6f },
                        enabled = canMarkSelectedDate
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.mark_as_completed),
                            fontWeight = FontWeight.SemiBold
                        )
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
                                modifier = Modifier.size(150.dp)
                            )
                        }
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
}

@Composable
private fun ProgressStatsSection(habit: Habit, progress: HabitProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Enhanced Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Animated gradient icon
            val infiniteTransition = rememberInfiniteTransition(label = "header_glow")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow_alpha"
            )
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                text = stringResource(R.string.progress_overview),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Main Stats in 2x2 Grid with Beautiful Cards
        val isDark = isSystemInDarkTheme()
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnhancedStatCard(
                    title = "Current Streak",
                    value = habit.streak.toString(),
                    subtitle = "days",
                    icon = Icons.Default.LocalFireDepartment,
                    gradient = listOf(
                        Color(0xFFFF6B35),
                        Color(0xFFFF8E53)
                    ),
                    modifier = Modifier.weight(1f),
                    useLottieAnimation = true,
                    lottieAsset = if (habit.streak == 0) "fireblack.json" else "Fire.json"
                )
                EnhancedStatCard(
                    title = "Longest Streak",
                    value = progress.longestStreak.toString(),
                    subtitle = "days",
                    icon = Icons.Default.Star,
                    gradient = listOf(
                        Color(0xFFFFD700),
                        Color(0xFFFFA500)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnhancedStatCard(
                    title = "Total Completions",
                    value = progress.totalCompletions.toString(),
                    subtitle = "times",
                    icon = Icons.Default.CheckCircle,
                    gradient = listOf(
                        Color(0xFF9C27B0),
                        Color(0xFFE91E63)
                    ),
                    modifier = Modifier.weight(1f)
                )
                EnhancedStatCard(
                    title = "Success Rate",
                    value = "${(progress.completionRate * 100).toInt()}%",
                    subtitle = "completed",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    gradient = listOf(
                        Color(0xFF2196F3),
                        Color(0xFF03DAC6)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

data class StatCard(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun EnhancedStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    useLottieAnimation: Boolean = false,
    lottieAsset: String? = null
) {
    val isDark = isSystemInDarkTheme()
    
    // Enhanced animations
    val infiniteTransition = rememberInfiniteTransition(label = "enhanced_stat_card")
    
    // Subtle float animation
    val cardElevation by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card_elevation"
    )
    
    // Icon rotation for visual interest
    val iconRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_rotation"
    )
    
    // Shimmer effect
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    
    // Lottie animation setup
    val lottieComposition by rememberLottieComposition(
        spec = if (useLottieAnimation && lottieAsset != null) {
            LottieCompositionSpec.Asset(lottieAsset)
        } else {
            // Provide a dummy spec that won't load anything
            LottieCompositionSpec.Asset("")
        }
    )
    
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = LottieConstants.IterateForever,
        isPlaying = useLottieAnimation && lottieComposition != null,
        speed = 1f,
        restartOnPlay = true
    )
    
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(
                elevation = cardElevation.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = gradient[0].copy(alpha = 0.3f),
                spotColor = gradient[1].copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Gradient background overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = gradient.map { it.copy(alpha = 0.1f) },
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset.Infinite
                        )
                    )
            )
            
            // Shimmer overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0f),
                                Color.White.copy(alpha = shimmerAlpha * 0.1f),
                                Color.White.copy(alpha = 0f)
                            ),
                            start = androidx.compose.ui.geometry.Offset(-100f, -100f),
                            end = androidx.compose.ui.geometry.Offset(100f, 100f)
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Animated Icon with gradient
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = gradient.map { it.copy(alpha = 0.2f) }
                            ),
                            shape = CircleShape
                        )
                        .graphicsLayer {
                            rotationZ = if (useLottieAnimation) 0f else iconRotation
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (useLottieAnimation && lottieComposition != null) {
                        // Use Lottie animation
                        LottieAnimation(
                            composition = lottieComposition,
                            progress = { lottieProgress },
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        // Use regular icon
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = gradient[0],
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Value with enhanced typography
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = gradient[0],
                    textAlign = TextAlign.Center
                )
                
                // Title and subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakRewardsSection(
    habit: Habit,
    userRewards: it.atraj.habittracker.data.local.UserRewards
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Section Header (Clickable to expand/collapse)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "rewards_glow")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow_alpha"
            )
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = glowAlpha),
                                Color(0xFFFFD700).copy(alpha = 0.2f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💎",
                    fontSize = 18.sp
                )
            }
            
            Text(
                text = "Streak Rewards & System",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Expandable content
        if (isExpanded) {
        // Rewards Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current Streak with Fire Animation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Current Streak",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${habit.streak} days",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )
                }
                
                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                
                // Total Diamonds (calculated from streak milestones)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "💎",
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Diamonds from Habit",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = calculateDiamondsEarned(habit.highestStreakAchieved).toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
                
                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                
                // Available Freeze Days (Shared Pool)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "❄️",
                            fontSize = 20.sp
                        )
                        Column {
                            Text(
                                text = "Freeze Days Available",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Shared across all habits",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Text(
                        text = "${userRewards.freezeDays}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF03DAC6)
                    )
                }
            }
        }
        
        // Visual Legend
        StreakLegendCard()
        } // End of isExpanded
    }
}

/**
 * Calculate total diamonds earned from a habit based on highest streak
 */
private fun calculateDiamondsEarned(highestStreak: Int): Int {
    var total = 0
    
    // Every 10 days = 20 diamonds
    val tenDayMilestones = highestStreak / 10
    total += tenDayMilestones * 20
    
    // Every 100 days = additional 100 diamonds (on top of the 10-day reward)
    val hundredDayMilestones = highestStreak / 100
    total += hundredDayMilestones * 100
    
    return total
}

@Composable
private fun StreakLegendCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Streak System Guide",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Streak Colors
            Text(
                text = "Calendar Colors:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            LegendItem(
                color = Color(0xFFE53935),
                label = "Red Border",
                description = "Streak = 0 (broken/reset)"
            )
            
            LegendItem(
                color = Color(0xFFFFA726),
                label = "Yellow Border",
                description = "Streak 1-4 days (building momentum)"
            )
            
            LegendItem(
                color = Color(0xFF66BB6A),
                label = "Green Border",
                description = "Streak 5+ days (consistent progress)"
            )
            
            Divider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            
            // Protection Systems
            Text(
                text = "Streak Protection:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🧊",
                    fontSize = 18.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Icy Glass Border",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Default grace day (1 free missed day per gap)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "❄️",
                    fontSize = 18.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Snowy Glass Overlay",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Purchased freeze day protection (shared pool)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Divider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            
            // Rewards Info
            Text(
                text = "Diamond Rewards:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "💎", fontSize = 16.sp)
                Text(
                    text = "20 diamonds every 10-day streak\n💎 +100 diamonds bonus at 100-day milestones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(
                    width = 3.dp,
                    color = color,
                    shape = CircleShape
                )
                .background(
                    color = color.copy(alpha = 0.2f),
                    shape = CircleShape
                )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CalendarSection(
    habit: Habit,
    completedDates: Set<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    progress: HabitProgress,
    userRewards: it.atraj.habittracker.data.local.UserRewards
) {
    var currentMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.calendar_view),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Month Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { currentMonth = currentMonth.minusMonths(1) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = stringResource(R.string.previous_month)
                        )
                    }

                    Text(
                        text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = { 
                            if (currentMonth < YearMonth.now()) {
                                currentMonth = currentMonth.plusMonths(1)
                            }
                        },
                        enabled = currentMonth < YearMonth.now()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.next_month)
                        )
                    }
                }

                // Calendar Grid
                MonthCalendar(
                    month = currentMonth,
                    completedDates = completedDates,
                    habitCreationDate = habit.createdAt.atZone(java.time.ZoneOffset.UTC).toLocalDate(),
                    selectedDate = selectedDate,
                    onDateSelected = { date ->
                        currentMonth = YearMonth.from(date)
                        onDateSelected(date)
                    },
                    progress = progress,
                    userRewards = userRewards
                )
                
                // Helper text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.calendar_helper_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    completedDates: Set<LocalDate>,
    habitCreationDate: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    progress: HabitProgress,
    userRewards: it.atraj.habittracker.data.local.UserRewards
) {
    // Calculate streak at each date for proper coloring
    val streakAtDate = remember(completedDates) {
        calculateStreakAtEachDate(completedDates, habitCreationDate)
    }
    
    val completions = progress.completions
    val firstDayOfMonth = month.atDay(1)
    val lastDayOfMonth = month.atEndOfMonth()
    // Fix for Android 10/11 compatibility - use proper modulo calculation
    val dayOfWeekValue = firstDayOfMonth.dayOfWeek.value
    val firstDayOfWeek = if (dayOfWeekValue == 7) 0 else dayOfWeekValue
    val today = LocalDate.now()
    
    // Get first completion date to know when streak tracking starts
    val firstCompletionDate = completedDates.minOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Day headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Calendar days
        val totalCells = 42 // 6 weeks * 7 days
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(totalCells) { index ->
                val dayNumber = index - firstDayOfWeek + 1
                val date = if (dayNumber in 1..lastDayOfMonth.dayOfMonth) {
                    month.atDay(dayNumber)
                } else null

                // Determine if this is a grace day
                // Grace only shows on PAST dates (not today) - user still has time to complete today
                val isGraceDay = date?.let { d ->
                    if (d !in completedDates && d >= habitCreationDate && d < today) {
                        // Check if there's a completion before this date
                        val previousCompletions = completedDates.filter { it < d }.sorted()
                        if (previousCompletions.isNotEmpty()) {
                            val lastCompletion = previousCompletions.last()
                            // Use StreakCalculator for consistent logic
                            StreakCalculator.isGraceDay(lastCompletion, d, completions)
                        } else false
                    } else false
                } ?: false
                
                // Determine if this is a freeze day
                // Freeze only shows on PAST dates protected by streak freeze
                val isFreezeDay = date?.let { d ->
                    if (d !in completedDates && d >= habitCreationDate && d < today) {
                        val previousCompletions = completedDates.filter { it < d }.sorted()
                        if (previousCompletions.isNotEmpty()) {
                            val lastCompletion = previousCompletions.last()
                            StreakCalculator.isFreezeDay(
                                lastCompletedDate = lastCompletion,
                                date = d,
                                completions = completions,
                                freezeDaysAvailable = userRewards.freezeDays
                            )
                        } else false
                    } else false
                } ?: false
                
                CalendarDay(
                    date = date,
                    isCompleted = date?.let { it in completedDates } ?: false,
                    isToday = date == today,
                    isBeforeHabitCreation = date?.let { it < habitCreationDate } ?: true,
                    isSelected = date == selectedDate,
                    onClick = { 
                        // Allow selecting any past date (including before creation) but still block future
                        if (date != null && !date.isAfter(today)) {
                            onDateSelected(date)
                        }
                    },
                    streakLevel = date?.let { streakAtDate[it] } ?: 0,
                    isGraceDay = isGraceDay,
                    isFreezeDay = isFreezeDay,
                    isBeforeFirstCompletion = date?.let { d -> 
                        firstCompletionDate?.let { d < it } ?: true 
                    } ?: false
                )
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate?,
    isCompleted: Boolean,
    isToday: Boolean,
    isBeforeHabitCreation: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit = {},
    streakLevel: Int = 0,  // 0 = broken/none, 1-4 = building, 5+ = strong
    isGraceDay: Boolean = false,  // Grace day indicator
    isFreezeDay: Boolean = false,  // Freeze day indicator
    isBeforeFirstCompletion: Boolean = false  // Before first completion - no streak tracking yet
) {
    // Allow selecting any past date (incl. before habit creation) for backfill
    val isClickable = date != null && !date.isAfter(LocalDate.now())

    // Background color with special handling for freeze days
    val backgroundColor = when {
        date == null -> Color.Transparent
        isFreezeDay -> Color(0xFFE0F7FA)  // Light frost cyan background for freeze days
        isCompleted -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        isClickable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }

    val textColor = when {
        date == null -> Color.Transparent
        isFreezeDay -> Color(0xFF006064)  // Dark cyan for freeze day icon
        isCompleted -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.primary
        isClickable -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }

    // Streak-based border color (spec colors)
    val streakBorderColor = when {
        streakLevel == 0 -> Color(0xFFE53935) // Red for broken streak
        streakLevel in 1..4 -> Color(0xFFFFA726) // Yellow for building
        streakLevel >= 5 -> Color(0xFF66BB6A) // Green for strong streak
        else -> null
    }
    
    val borderColor = when {
        isSelected && isCompleted -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.primary
        // Don't show red border on today - user still has time to complete
        isToday && !isCompleted -> MaterialTheme.colorScheme.primary
        // Apply streak color only to completed days that are after first completion
        isCompleted && streakBorderColor != null && !isBeforeFirstCompletion -> streakBorderColor
        isBeforeHabitCreation && isClickable -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        else -> null
    }

    var dayModifier = Modifier
        .size(36.dp)
        .background(
            color = backgroundColor,
            shape = CircleShape
        )

    // Grace day gets special icy border (light blue gradient)
    if (isGraceDay) {
        dayModifier = dayModifier.border(
            width = 3.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF87CEEB).copy(alpha = 0.8f),  // Sky blue
                    Color(0xFFB0E0E6).copy(alpha = 0.6f),  // Powder blue
                    Color(0xFF87CEEB).copy(alpha = 0.8f)
                )
            ),
            shape = CircleShape
        )
    } 
    // Freeze day gets frosty border (darker cyan to complement the frost background)
    else if (isFreezeDay) {
        dayModifier = dayModifier.border(
            width = 2.dp,
            color = Color(0xFF00838F),  // Dark cyan border
            shape = CircleShape
        )
    } 
    // Regular border colors (streak-based or selection)
    else if (borderColor != null) {
        dayModifier = dayModifier.border(
            width = 2.dp,
            color = borderColor,
            shape = CircleShape
        )
    }

    if (isClickable) {
        dayModifier = dayModifier.clickable { onClick() }
    }

    // Calendar day entrance animation
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "calendar_day_scale"
    )
    
    Box(
        modifier = dayModifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            when {
                isCompleted -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                isFreezeDay -> {
                    // Show snowflake icon for freeze-protected days
                    Icon(
                        imageVector = Icons.Default.AcUnit,  // Snowflake/frost icon
                        contentDescription = "Freeze day",
                        tint = Color(0xFF00838F),  // Dark cyan
                        modifier = Modifier.size(18.dp)
                    )
                }
                else -> {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Calculate streak value at each date for proper calendar coloring
 */
private fun calculateStreakAtEachDate(
    completedDates: Set<LocalDate>,
    habitCreationDate: LocalDate
): Map<LocalDate, Int> {
    if (completedDates.isEmpty()) return emptyMap()
    
    val sorted = completedDates.sorted()
    val result = mutableMapOf<LocalDate, Int>()
    
    var streak = 0
    var expectedDate = sorted.first()
    
    for (completion in sorted) {
        val gap = ChronoUnit.DAYS.between(expectedDate, completion).toInt()
        
        when {
            gap == 0 -> {
                streak++
                result[completion] = streak
                expectedDate = expectedDate.plusDays(1)
            }
            gap > 0 -> {
                // Apply grace to first missed day
                val penalty = if (gap > 1) gap - 1 else 0
                streak = maxOf(0, streak - penalty)
                streak++
                result[completion] = streak
                expectedDate = completion.plusDays(1)
            }
        }
    }
    
    return result
}

@Composable
private fun HabitInfoSection(habit: Habit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.habit_details),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoRow(
                    label = stringResource(R.string.frequency),
                    value = when (habit.frequency) {
                        HabitFrequency.DAILY -> stringResource(R.string.daily)
                        HabitFrequency.WEEKLY -> stringResource(R.string.weekly)
                        HabitFrequency.MONTHLY -> stringResource(R.string.monthly)
                        HabitFrequency.YEARLY -> stringResource(R.string.yearly)
                    },
                    icon = Icons.Default.CalendarToday
                )

                if (habit.reminderHour >= 0) {
                    InfoRow(
                        label = "Reminder Time",
                        value = String.format("%02d:%02d", habit.reminderHour, habit.reminderMinute),
                        icon = Icons.Default.Alarm
                    )
                }

                InfoRow(
                    label = "Created On",
                    value = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        .format(habit.createdAt.atZone(java.time.ZoneOffset.UTC).toLocalDate()),
                    icon = Icons.Default.Today
                )

                InfoRow(
                    label = stringResource(R.string.notification_sound),
                    value = habit.notificationSoundName,
                    icon = Icons.AutoMirrored.Filled.VolumeUp
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AnimatedFireIcon(
    isActive: Boolean,
    streakCount: Int,
    modifier: Modifier = Modifier
) {
    // Determine which fire animation to use
    val shouldUseBlackFire = !isActive || streakCount == 0
    val fireAsset = if (shouldUseBlackFire) "fireblack.json" else "Fire.json"
    
    // Load Lottie fire animation
    val fireComposition by rememberLottieComposition(
        LottieCompositionSpec.Asset(fireAsset)
    )
    
    val fireProgress by animateLottieCompositionAsState(
        composition = fireComposition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true,
        speed = 1f,
        restartOnPlay = true
    )
    
    // Add grey circular background for black fire visibility in dark mode
    if (shouldUseBlackFire) {
        Box(
            modifier = modifier
                .size(32.dp)
                .background(
                    color = Color(0xFFE0E0E0), // Light grey background
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = fireComposition,
                progress = { fireProgress },
                modifier = Modifier.size(28.dp) // Compact size for single line layout
            )
        }
    } else {
        // Orange fire without background
        Box(
            modifier = modifier.size(28.dp)
        ) {
            LottieAnimation(
                composition = fireComposition,
                progress = { fireProgress },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun HabitAvatarDisplay(
    avatar: HabitAvatar,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(
                color = androidx.compose.ui.graphics.Color(avatar.backgroundColor.toColorInt()),
                shape = CircleShape
            )
    ) {
        when (avatar.type) {
            HabitAvatarType.EMOJI -> {
                Text(
                    text = avatar.value,
                    fontSize = (size.value * 0.6).sp,
                    textAlign = TextAlign.Center
                )
            }
            HabitAvatarType.DEFAULT_ICON -> {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
            HabitAvatarType.CUSTOM_IMAGE -> {
                // Load custom image from URL using Coil
                val token = it.atraj.habittracker.avatar.SecureTokenStorage.getToken(context)
                val requestBuilder = coil.request.ImageRequest.Builder(context)
                    .data(avatar.value)
                    .crossfade(true)
                    .size(coil.size.Size.ORIGINAL)
                
                if (token != null && avatar.value.contains("githubusercontent.com")) {
                    requestBuilder.addHeader("Authorization", "token $token")
                }
                
                coil.compose.AsyncImage(
                    model = requestBuilder.build(),
                    contentDescription = "Custom habit avatar",
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
    }
}
