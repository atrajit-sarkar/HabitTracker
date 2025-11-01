package it.atraj.habittracker.ui

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import it.atraj.habittracker.R
import it.atraj.habittracker.data.local.NotificationSound

/**
 * Bad Habit creation screen - simplified version without time picker
 * Focuses on tracking app usage to help users release bad habits
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBadHabitScreen(
    habitName: String,
    habitDescription: String,
    selectedSound: NotificationSound,
    availableSounds: List<NotificationSound>,
    targetAppPackageName: String?,
    targetAppName: String?,
    isSaving: Boolean,
    onHabitNameChange: (String) -> Unit,
    onHabitDescriptionChange: (String) -> Unit,
    onNotificationSoundChange: (NotificationSound) -> Unit,
    onTargetAppChange: (String, String) -> Unit,
    onBackClick: () -> Unit,
    onSaveHabit: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showAppPicker by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Check if usage stats permission is granted
    val hasUsageStatsPermission = remember {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 1000,
            time
        )
        stats != null && stats.isNotEmpty()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = "Release Bad Habit",
                            style = MaterialTheme.typography.headlineMedium,
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
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
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
                            Text(text = "Cancel")
                        }
                        FilledTonalButton(
                            onClick = onSaveHabit,
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving && habitName.isNotBlank() && (targetAppPackageName != null || targetAppName != null)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                Text(
                                    text = "Create",
                                    maxLines = 1,
                                    softWrap = false
                                )
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
                // Permission banner
                if (!hasUsageStatsPermission) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Permission Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "To track app usage, please grant Usage Access permission.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            FilledTonalButton(
                                onClick = { showPermissionDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }

                // Bad habit name
                OutlinedTextField(
                    value = habitName,
                    onValueChange = onHabitNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Bad Habit Name") },
                    placeholder = { Text("e.g., Stop using social media") },
                    singleLine = true
                )

                // Bad habit description
                OutlinedTextField(
                    value = habitDescription,
                    onValueChange = onHabitDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Description") },
                    placeholder = { Text("Why do you want to release this habit?") },
                    minLines = 3,
                    maxLines = 5
                )

                // Target app selection
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
                            text = "Target App",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (targetAppPackageName != null || targetAppName != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = targetAppName ?: "Custom App",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (targetAppPackageName != null) {
                                        Text(
                                            text = targetAppPackageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                TextButton(onClick = { showAppPicker = true }) {
                                    Text("Change")
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showAppPicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Select App to Track")
                            }
                        }

                        Text(
                            text = "The app will be checked every 2 hours. You'll receive encouragement when you successfully avoid using it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Notification Sound Selection
                NotificationSoundSelectorSimple(
                    selectedSound = selectedSound,
                    onSoundChange = onNotificationSoundChange,
                    availableSounds = availableSounds
                )

                // Bottom spacing
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // Loading overlay
        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Creating bad habit tracker...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    // App picker dialog
    if (showAppPicker) {
        AppPickerDialog(
            onAppSelected = { packageName, appName ->
                onTargetAppChange(packageName, appName)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Usage Access Permission") },
            text = {
                Text("To track app usage, you need to grant Usage Access permission. This will allow the app to monitor if you're using the targeted app.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSoundSelectorSimple(
    selectedSound: NotificationSound,
    availableSounds: List<NotificationSound>,
    onSoundChange: (NotificationSound) -> Unit
) {
    val context = LocalContext.current
    var soundExpanded by remember { mutableStateOf(false) }
    
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
            Text(
                text = "Notification Sound",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            ExposedDropdownMenuBox(
                expanded = soundExpanded,
                onExpandedChange = { soundExpanded = !soundExpanded }
            ) {
                OutlinedTextField(
                    value = selectedSound.displayName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Select Sound") },
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
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPickerDialog(
    onAppSelected: (packageName: String, appName: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var searchQuery by remember { mutableStateOf("") }
    var showCustomAppDialog by remember { mutableStateOf(false) }

    // Get all installed apps (no filtering)
    val installedApps = remember {
        val allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        
        android.util.Log.d("AppPicker", "Total apps found: ${allApps.size}")
        android.util.Log.d("AppPicker", "Showing all ${allApps.size} apps (no filtering)")
        
        allApps
    }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.loadLabel(packageManager).toString().contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select App",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom app name option
                OutlinedButton(
                    onClick = { showCustomAppDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Enter Custom App Name")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // App list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAppSelected(
                                        app.packageName,
                                        app.loadLabel(packageManager).toString()
                                    )
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // App icon
                            app.loadIcon(packageManager)?.let { icon ->
                                val bitmap = if (icon is android.graphics.drawable.BitmapDrawable) {
                                    icon.bitmap
                                } else {
                                    val bmp = android.graphics.Bitmap.createBitmap(
                                        icon.intrinsicWidth,
                                        icon.intrinsicHeight,
                                        android.graphics.Bitmap.Config.ARGB_8888
                                    )
                                    val canvas = android.graphics.Canvas(bmp)
                                    icon.setBounds(0, 0, canvas.width, canvas.height)
                                    icon.draw(canvas)
                                    bmp
                                }
                                
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = app.loadLabel(packageManager).toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }

    // Custom app name dialog
    if (showCustomAppDialog) {
        var customAppName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustomAppDialog = false },
            title = { Text("Enter App Name") },
            text = {
                OutlinedTextField(
                    value = customAppName,
                    onValueChange = { customAppName = it },
                    label = { Text("App Name") },
                    placeholder = { Text("e.g., Facebook") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customAppName.isNotBlank()) {
                            onAppSelected("", customAppName)
                            showCustomAppDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomAppDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
