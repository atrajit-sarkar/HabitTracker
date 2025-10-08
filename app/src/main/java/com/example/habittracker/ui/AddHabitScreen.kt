package it.atraj.habittracker.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.atraj.habittracker.R
import it.atraj.habittracker.data.local.HabitAvatar
import it.atraj.habittracker.data.local.HabitAvatarType
import it.atraj.habittracker.data.local.HabitFrequency
import it.atraj.habittracker.data.local.NotificationSound

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
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
    onBackClick: () -> Unit,
    onSaveHabit: () -> Unit
) {
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    val timePickerState = rememberTimePickerState(
        initialHour = state.hour,
        initialMinute = state.minute,
        is24Hour = is24Hour
    )
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        onHabitTimeChange(timePickerState.hour, timePickerState.minute)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = if (state.isEditMode) R.string.edit_habit else R.string.create_new_habit),
                        style = MaterialTheme.typography.headlineMedium,
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
        },
        bottomBar = {
            // Action buttons at bottom with padding to avoid navigation bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    TextButton(
                        onClick = onBackClick,
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            Text(text = stringResource(id = if (state.isEditMode) R.string.update_habit else R.string.create_habit))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Habit name
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

            // Habit description
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

            // Reminder settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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

                    if (state.reminderEnabled) {
                        // Time picker
                        TimePicker(
                            state = timePickerState,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Notification Sound Selection
                        NotificationSoundSelector(
                            selectedSound = state.notificationSound,
                            onSoundChange = onNotificationSoundChange,
                            availableSounds = state.availableSounds
                        )
                    }
                }
            }

            // Bottom spacing to avoid overlap with bottom bar
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AvatarSelector(
    selectedAvatar: HabitAvatar,
    onAvatarChange: (HabitAvatar) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.avatar_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            // Current avatar display
            AvatarDisplay(
                avatar = selectedAvatar,
                size = 64.dp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            // Emoji selection grid using FlowRow for better layout
            Text(
                text = stringResource(id = R.string.choose_emoji_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HabitAvatar.POPULAR_EMOJIS.forEach { emoji ->
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = CircleShape
            )
    ) {
        when (avatar.type) {
            HabitAvatarType.EMOJI -> {
                Text(
                    text = avatar.value,
                    fontSize = (size.value * 0.5f).sp,
                    textAlign = TextAlign.Center
                )
            }
            HabitAvatarType.DEFAULT_ICON -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size((size.value * 0.5f).dp)
                )
            }
            HabitAvatarType.CUSTOM_IMAGE -> {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size((size.value * 0.5f).dp)
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
            .size(48.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                       else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
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
            .size(40.dp)
            .background(
                color = Color(android.graphics.Color.parseColor(color)),
                shape = CircleShape
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable { onClick() }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.frequency_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
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
