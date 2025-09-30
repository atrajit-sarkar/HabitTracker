package com.example.habittracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        fontWeight = FontWeight.Bold
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

            // Habit Title and Description
            Text(
                text = habit.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (habit.description.isNotBlank()) {
                Text(
                    text = habit.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            // Current Streak
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.current_streak_days, progress.currentStreak),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Selected date information
            Text(
                text = stringResource(R.string.selected_date, selectedDateText),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )

            // Complete Button
            if (isSelectedDateCompleted) {
                AssistChip(
                    onClick = { },
                    enabled = false,
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
                FilledTonalButton(
                    onClick = onMarkCompleted,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canMarkSelectedDate
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.mark_as_completed),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressStatsSection(progress: HabitProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.progress_overview),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        val statCards = remember(progress) {
            listOf(
                StatCard(
                    title = "Current Streak",
                    value = progress.currentStreak.toString(),
                    subtitle = "days",
                    icon = Icons.Default.LocalFireDepartment,
                    color = androidx.compose.ui.graphics.Color.Blue
                ),
                StatCard(
                    title = "Longest Streak",
                    value = progress.longestStreak.toString(),
                    subtitle = "days",
                    icon = Icons.Default.Star,
                    color = androidx.compose.ui.graphics.Color.Green
                ),
                StatCard(
                    title = "Total Completions",
                    value = progress.totalCompletions.toString(),
                    subtitle = "times",
                    icon = Icons.Default.CheckCircle,
                    color = androidx.compose.ui.graphics.Color.Magenta
                ),
                StatCard(
                    title = "Success Rate",
                    value = "${(progress.completionRate * 100).toInt()}%",
                    subtitle = "completed",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = androidx.compose.ui.graphics.Color.Blue
                )
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCardItem(
                    statCard = statCards[0],
                    modifier = Modifier.weight(1f)
                )
                StatCardItem(
                    statCard = statCards[1],
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCardItem(
                    statCard = statCards[2],
                    modifier = Modifier.weight(1f)
                )
                StatCardItem(
                    statCard = statCards[3],
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
private fun StatCardItem(
    statCard: StatCard,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = statCard.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = statCard.icon,
                contentDescription = null,
                tint = statCard.color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = statCard.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = statCard.color
            )
            Text(
                text = statCard.title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
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
                    habitCreationDate = java.time.LocalDate.ofInstant(habit.createdAt, java.time.ZoneOffset.UTC),
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
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
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

    Box(
        modifier = dayModifier,
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
                        .format(java.time.LocalDate.ofInstant(habit.createdAt, java.time.ZoneOffset.UTC)),
                    icon = Icons.Default.Today
                )

                InfoRow(
                    label = stringResource(R.string.notification_sound),
                    value = habit.notificationSound.displayName,
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