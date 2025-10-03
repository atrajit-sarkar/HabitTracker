package com.example.habittracker.ui

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
import com.example.habittracker.R
import com.example.habittracker.data.local.Habit
import com.example.habittracker.data.local.HabitAvatar
import com.example.habittracker.data.local.HabitAvatarType
import com.example.habittracker.data.local.HabitFrequency
import androidx.core.graphics.toColorInt
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

data class HabitProgress(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int,
    val completionRate: Float,
    val completedDates: Set<LocalDate>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailsScreen(
    habit: Habit,
    progress: HabitProgress,
    selectedDate: LocalDate,
    isSelectedDateCompleted: Boolean,
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
            ProgressStatsSection(progress = progress)

            // Calendar View
            CalendarSection(
                habit = habit,
                completedDates = progress.completedDates,
                selectedDate = selectedDate,
                onDateSelected = onSelectDate
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

            // Current Streak with Animated Fire
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedFireIcon(
                    isActive = isSelectedDateCompleted && isToday,
                    streakCount = progress.currentStreak
                )
                Text(
                    text = stringResource(R.string.current_streak_days, progress.currentStreak),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
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
                        text = "• Complete today or yesterday to maintain streak\n• Miss days? Streak reduces by 1 per missed day (motivational penalty)\n• Backfill past days to recover your streak progress",
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
private fun ProgressStatsSection(progress: HabitProgress) {
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
                    value = progress.currentStreak.toString(),
                    subtitle = "days",
                    icon = Icons.Default.LocalFireDepartment,
                    gradient = listOf(
                        Color(0xFFFF6B35),
                        Color(0xFFFF8E53)
                    ),
                    modifier = Modifier.weight(1f)
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
    modifier: Modifier = Modifier
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
                            rotationZ = iconRotation
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = gradient[0],
                        modifier = Modifier.size(20.dp)
                    )
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
private fun CalendarSection(
    habit: Habit,
    completedDates: Set<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit = { _ -> }
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
                    }
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
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = month.atDay(1)
    val lastDayOfMonth = month.atEndOfMonth()
    // Fix for Android 10/11 compatibility - use proper modulo calculation
    val dayOfWeekValue = firstDayOfMonth.dayOfWeek.value
    val firstDayOfWeek = if (dayOfWeekValue == 7) 0 else dayOfWeekValue
    val today = LocalDate.now()

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
                    }
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
    onClick: () -> Unit = {}
) {
    // Allow selecting any past date (incl. before habit creation) for backfill
    val isClickable = date != null && !date.isAfter(LocalDate.now())

    val backgroundColor = when {
        date == null -> Color.Transparent
        isCompleted -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        isClickable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }

    val textColor = when {
        date == null -> Color.Transparent
        isCompleted -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.primary
        isClickable -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }

    val borderColor = when {
        isSelected && isCompleted -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.primary
        isToday && !isCompleted -> MaterialTheme.colorScheme.primary
        isBeforeHabitCreation && isClickable -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        else -> null
    }

    var dayModifier = Modifier
        .size(36.dp)
        .background(
            color = backgroundColor,
            shape = CircleShape
        )

    if (borderColor != null) {
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
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            } else {
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
    val infiniteTransition = rememberInfiniteTransition(label = "fire_animation")
    
    // Flame flickering animation
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )
    
    // Color animation based on active state
    val fireColor by animateColorAsState(
        targetValue = if (isActive) {
            Color(0xFFFF6B35) // Bright orange-red
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) // Faded
        },
        animationSpec = tween(600),
        label = "fire_color"
    )
    
    // Rotation for extra liveliness
    val rotation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fire_rotation"
    )
    
    Icon(
        imageVector = Icons.Default.LocalFireDepartment,
        contentDescription = "Streak Fire",
        tint = fireColor,
        modifier = modifier
            .size(28.dp)
            .graphicsLayer {
                scaleX = if (isActive) flameScale else 1f
                scaleY = if (isActive) flameScale else 1f
                rotationZ = if (isActive) rotation else 0f
            }
    )
}

@Composable
private fun HabitAvatarDisplay(
    avatar: HabitAvatar,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
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
                Text(
                    text = "IMG",
                    color = Color.White,
                    fontSize = (size.value * 0.3).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}