package it.atraj.habittracker.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import it.atraj.habittracker.MainActivity
import it.atraj.habittracker.data.local.HabitAvatar
import it.atraj.habittracker.data.local.HabitAvatarType
import it.atraj.habittracker.ui.theme.HabitTrackerTheme
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Screen showing all overdue habits with urgency-based animations
 * The longer a habit is overdue, the more urgent the animation
 */
@AndroidEntryPoint
class OverdueHabitsActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            HabitTrackerTheme {
                OverdueHabitsScreen(
                    onNavigateBack = { finish() },
                    onHabitClick = { habitId ->
                        // Use launcher intent to ensure we get the correct activity alias
                        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                            action = "it.atraj.habittracker.OPEN_HABIT_DETAILS"
                            putExtra("habitId", habitId)
                            putExtra("openHabitDetails", true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        } ?: Intent(this, it.atraj.habittracker.MainActivity::class.java).apply {
                            // Fallback to direct intent
                            action = "it.atraj.habittracker.OPEN_HABIT_DETAILS"
                            putExtra("habitId", habitId)
                            putExtra("openHabitDetails", true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverdueHabitsScreen(
    viewModel: HabitViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onHabitClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Get overdue habits - filter from the HabitCardUi list
    val overdueHabits = remember(uiState.habits) {
        uiState.habits.filter { it.isOverdue }.sortedBy { it.title }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "⚠️ Overdue Habits (${overdueHabits.size})",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        if (overdueHabits.isEmpty()) {
            // No overdue habits - show empty state
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
                    Text(
                        "🎉 All caught up!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        "No overdue habits right now",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = overdueHabits,
                    key = { it.id }
                ) { habit ->
                    OverdueHabitCard(
                        habit = habit,
                        onClick = { onHabitClick(habit.id) }
                    )
                }
            }
        }
    }
}

// Simple palette for overdue habit cards
private data class OverdueCardPalette(
    val accent: Color
)

// Helper function to generate consistent colors for overdue habit cards
private fun overdueCardPaletteFor(habitId: Long): OverdueCardPalette {
    val colors = listOf(
        Color(0xFF6650A4),
        Color(0xFF006C62),
        Color(0xFF7B1FA2),
        Color(0xFF3949AB),
        Color(0xFF00838F)
    )
    // Use abs() to ensure positive index, handle negative habit IDs properly
    val index = kotlin.math.abs(habitId % colors.size).toInt()
    return OverdueCardPalette(accent = colors[index])
}

@Composable
fun OverdueHabitCard(
    habit: HabitCardUi,
    onClick: () -> Unit
) {
    val today = LocalDate.now()
    val habitDateTime = LocalDateTime.of(today, habit.reminderTime)
    val now = LocalDateTime.now()
    val hoursOverdue = Duration.between(habitDateTime, now).toHours()
    
    // Calculate urgency level (0-3)
    // 0-2 hours: Low urgency
    // 2-6 hours: Medium urgency
    // 6-12 hours: High urgency
    // 12+ hours: Critical urgency
    val urgencyLevel = when {
        hoursOverdue < 2 -> 0  // Low
        hoursOverdue < 6 -> 1  // Medium
        hoursOverdue < 12 -> 2 // High
        else -> 3              // Critical
    }
    
    // Urgency colors
    val urgencyColor = when (urgencyLevel) {
        0 -> Color(0xFFFCD34D) // Yellow - low urgency
        1 -> Color(0xFFFB923C) // Orange - medium urgency
        2 -> Color(0xFFEF4444) // Red - high urgency
        else -> Color(0xFF991B1B) // Dark red - critical urgency
    }
    
    val urgencyText = when (urgencyLevel) {
        0 -> "Recently overdue"
        1 -> "Overdue for ${hoursOverdue}h"
        2 -> "Very overdue (${hoursOverdue}h)!"
        else -> "CRITICAL (${hoursOverdue}h)!!"
    }
    
    // Shining animation - faster and more intense for higher urgency
    val infiniteTransition = rememberInfiniteTransition(label = "shine")
    
    // Scale animation (pulsing effect)
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (urgencyLevel >= 2) 1.05f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (urgencyLevel) {
                    0 -> 2000  // Slow pulse
                    1 -> 1500  // Medium pulse
                    2 -> 1000  // Fast pulse
                    else -> 700 // Very fast pulse
                },
                easing = EaseInOutCubic
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Shimmer effect (color animation)
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (urgencyLevel >= 2) 0.9f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (urgencyLevel) {
                    0 -> 2000
                    1 -> 1500
                    2 -> 1000
                    else -> 700
                },
                easing = EaseInOutCubic
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    
    val palette = overdueCardPaletteFor(habit.id)
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.accent),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (urgencyLevel >= 2) 8.dp else 4.dp
        )
    ) {
        Box {
            // Animated shining border overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                urgencyColor.copy(alpha = shimmerAlpha * 0.3f),
                                Color.Transparent,
                                urgencyColor.copy(alpha = shimmerAlpha * 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Urgency badge
                Surface(
                    color = urgencyColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = urgencyText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar
                    AvatarDisplay(
                        avatar = habit.avatar,
                        size = 48.dp,
                        habitId = habit.id
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (habit.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = habit.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Habit info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Scheduled: ${timeFormatter.format(habit.reminderTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    
                    // Frequency info
                    Text(
                        text = habit.frequencyText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                
                // Action hint
                Text(
                    text = "Tap to view details and mark as done",
                    style = MaterialTheme.typography.bodySmall,
                    color = urgencyColor.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun AvatarDisplay(
    avatar: HabitAvatar,
    size: Dp,
    modifier: Modifier = Modifier,
    habitId: Long? = null // Stable cache key for custom images
) {
    val context = LocalContext.current
    
    val bgColor = remember(avatar.backgroundColor) {
        Color(android.graphics.Color.parseColor(avatar.backgroundColor))
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(
                color = bgColor,
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
                    imageVector = Icons.Default.Alarm, // Default icon
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size((size.value * 0.6f).dp)
                )
            }
            HabitAvatarType.CUSTOM_IMAGE -> {
                val token = it.atraj.habittracker.avatar.SecureTokenStorage.getToken(context)
                
                // Use habit ID as stable cache key
                val cacheKey = habitId?.let { "habit_avatar_$it" } ?: "avatar_${avatar.value.hashCode()}"
                
                val requestBuilder = ImageRequest.Builder(context)
                    .data(avatar.value)
                    .crossfade(false)
                    .size(100)
                    .memoryCacheKey(cacheKey)
                    .diskCacheKey(cacheKey)
                    .allowHardware(true)
                
                if (token != null && avatar.value.contains("githubusercontent.com")) {
                    requestBuilder.addHeader("Authorization", "token $token")
                }
                
                AsyncImage(
                    model = requestBuilder.build(),
                    contentDescription = "Custom habit avatar",
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
