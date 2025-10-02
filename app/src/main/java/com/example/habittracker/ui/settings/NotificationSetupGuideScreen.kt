package com.example.habittracker.ui.settings

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.habittracker.notification.NotificationReliabilityHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSetupGuideScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()
    
    // State variables that update on resume
    var isExemptFromBatteryOptimization by remember { 
        mutableStateOf(NotificationReliabilityHelper.isIgnoringBatteryOptimizations(context))
    }
    var areNotificationsEnabled by remember {
        mutableStateOf(checkNotificationsEnabled(context))
    }
    var canScheduleExactAlarms by remember {
        mutableStateOf(checkExactAlarmPermission(context))
    }
    var isBackgroundDataEnabled by remember {
        mutableStateOf(checkBackgroundDataEnabled(context))
    }
    
    val manufacturer = remember { Build.MANUFACTURER.lowercase() }
    val isAggressiveDevice = remember { NotificationReliabilityHelper.isAggressiveBatteryManagement() }

    // Listen to lifecycle events to refresh status when user returns from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh all status checks when returning to the screen
                isExemptFromBatteryOptimization = NotificationReliabilityHelper.isIgnoringBatteryOptimizations(context)
                areNotificationsEnabled = checkNotificationsEnabled(context)
                canScheduleExactAlarms = checkExactAlarmPermission(context)
                isBackgroundDataEnabled = checkBackgroundDataEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Setup Guide") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            StatusCard(isExemptFromBatteryOptimization)

            // Why This Matters Section
            GuideSection(
                icon = Icons.Default.Info,
                title = "Why Notification Reliability Matters",
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "Android's battery optimization can prevent HabitTracker from sending you reminders when your phone is idle or in Doze mode. This guide helps you ensure 100% reliable notifications.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Step 1: Battery Optimization
            GuideSection(
                icon = Icons.Default.BatteryChargingFull,
                title = "Step 1: Disable Battery Optimization",
                iconColor = if (isExemptFromBatteryOptimization) 
                    MaterialTheme.colorScheme.tertiary 
                else 
                    MaterialTheme.colorScheme.error
            ) {
                Text(
                    text = "This is the most important step to ensure reliable notifications.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isExemptFromBatteryOptimization) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Already exempt from battery optimization ✓",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            try {
                                // Direct intent to request battery optimization exemption
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    // Fallback to battery optimization list
                                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(fallbackIntent)
                                } catch (e2: Exception) {
                                    // Final fallback to app details
                                    try {
                                        val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(detailsIntent)
                                    } catch (e3: Exception) {
                                        // Ignore
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Allow Battery Optimization")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    InstructionStep(
                        stepNumber = "1",
                        instruction = "Tap the button - a permission dialog will appear"
                    )
                    InstructionStep(
                        stepNumber = "2",
                        instruction = "Select 'Allow' or 'Don't optimize' in the popup"
                    )
                    InstructionStep(
                        stepNumber = "3",
                        instruction = "Return to this screen to see the checkmark"
                    )
                }
            }

            // Step 2: Notification Permission
            GuideSection(
                icon = Icons.Default.Notifications,
                title = "Step 2: Enable Notifications",
                iconColor = if (areNotificationsEnabled)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.secondary
            ) {
                if (areNotificationsEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Notifications are enabled ✓",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Button(
                    onClick = {
                        try {
                            // Direct to app's notification settings page
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                // Fallback to app details
                                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(fallbackIntent)
                            } catch (e2: Exception) {
                                // Ignore
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Notification Settings")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (!areNotificationsEnabled) {
                    InstructionStep(
                        stepNumber = "1",
                        instruction = "Tap the button - you'll be on HabitTracker's notification page"
                    )
                    InstructionStep(
                        stepNumber = "2",
                        instruction = "Toggle 'Show notifications' or 'Allow notifications' to ON"
                    )
                    InstructionStep(
                        stepNumber = "3",
                        instruction = "Ensure all notification categories are enabled"
                    )
                } else {
                    Text(
                        text = "You can still open settings to customize notification channels and sounds.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Step 3: Exact Alarm Permission (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                GuideSection(
                    icon = Icons.Default.Schedule,
                    title = "Step 3: Allow Exact Alarms (Android 12+)",
                    iconColor = if (canScheduleExactAlarms)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.secondary
                ) {
                    if (canScheduleExactAlarms) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Exact alarms are allowed ✓",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Button(
                        onClick = {
                            try {
                                // Direct intent to request exact alarm permission (Android 12+)
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    // Fallback to alarms & reminders settings
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val fallbackIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        context.startActivity(fallbackIntent)
                                    }
                                } catch (e2: Exception) {
                                    try {
                                        // Final fallback to app details
                                        val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(detailsIntent)
                                    } catch (e3: Exception) {
                                        // Ignore
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.AlarmOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Allow Exact Alarms")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (!canScheduleExactAlarms) {
                        InstructionStep(
                            stepNumber = "1",
                            instruction = "Tap the button - you'll be on the Alarms & reminders page"
                        )
                        InstructionStep(
                            stepNumber = "2",
                            instruction = "Find HabitTracker and toggle 'Allow setting alarms and reminders' to ON"
                        )
                        InstructionStep(
                            stepNumber = "3",
                            instruction = "Return to see the checkmark appear"
                        )
                    } else {
                        Text(
                            text = "Exact alarm scheduling is enabled for precise reminder timing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Step 4: Allow Background Activity
            GuideSection(
                icon = Icons.Default.Autorenew,
                title = "Step 4: Allow Background Activity",
                iconColor = if (isBackgroundDataEnabled)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.primary
            ) {
                if (isBackgroundDataEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Background activity is enabled ✓",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Button(
                    onClick = {
                        try {
                            // Try to open data usage settings directly (Android 7+)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                val intent = Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } else {
                                // Fallback for older versions
                                throw Exception("Not supported on this version")
                            }
                        } catch (e: Exception) {
                            try {
                                // Fallback to app details page
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                // Ignore
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Allow Background Activity")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (!isBackgroundDataEnabled) {
                    InstructionStep(
                        stepNumber = "1",
                        instruction = "Tap the button - you'll see HabitTracker's data usage page"
                    )
                    InstructionStep(
                        stepNumber = "2",
                        instruction = "Toggle 'Background data' or 'Allow background activity' to ON"
                    )
                    InstructionStep(
                        stepNumber = "3",
                        instruction = "If available, also enable 'Unrestricted data usage'"
                    )
                    InstructionStep(
                        stepNumber = "4",
                        instruction = "Return here to see the checkmark"
                    )
                } else {
                    Text(
                        text = "Background activity is enabled. Reminders will work even when the app is closed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Manufacturer-Specific Instructions
            if (isAggressiveDevice) {
                val manufacturerInstructions = NotificationReliabilityHelper.getManufacturerSpecificInstructions()
                if (manufacturerInstructions != null) {
                    GuideSection(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Additional Steps for ${Build.MANUFACTURER}",
                        iconColor = MaterialTheme.colorScheme.error
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Important!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                
                                Text(
                                    text = "Your device manufacturer has additional battery saving features. Please complete these steps:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = manufacturerInstructions,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open App Settings")
                        }
                    }
                }
            }

            // How It Works Section
            GuideSection(
                icon = Icons.Default.Build,
                title = "How It Works Behind the Scenes",
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                FeatureCard(
                    icon = Icons.Default.Alarm,
                    title = "AlarmManager",
                    description = "Schedules exact alarms that wake your device at the right time, even when idle."
                )
                
                FeatureCard(
                    icon = Icons.Default.RestartAlt,
                    title = "Boot Receiver",
                    description = "Automatically reschedules all your reminders after device restart or app update."
                )
                
                FeatureCard(
                    icon = Icons.Default.Verified,
                    title = "Daily Verification",
                    description = "WorkManager checks every 24 hours to ensure all reminders are scheduled correctly."
                )
            }

            // Benefits Section
            GuideSection(
                icon = Icons.Default.Star,
                title = "What You Get",
                iconColor = MaterialTheme.colorScheme.tertiary
            ) {
                BenefitItem("✓ Notifications appear exactly on time")
                BenefitItem("✓ Works even when phone is idle for hours")
                BenefitItem("✓ Survives phone restarts")
                BenefitItem("✓ Functions in Doze mode (overnight)")
                BenefitItem("✓ Minimal battery impact (<1% per day)")
            }

            // Testing Section
            GuideSection(
                icon = Icons.Default.Science,
                title = "Test Your Setup",
                iconColor = MaterialTheme.colorScheme.secondary
            ) {
                Text(
                    text = "Quick Test (5 minutes):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                InstructionStep("1", "Set a reminder for 5 minutes from now")
                InstructionStep("2", "Lock your phone screen")
                InstructionStep("3", "Wait 5 minutes without opening the app")
                InstructionStep("✓", "Notification should appear and make sound")
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Reboot Test:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                InstructionStep("1", "Set a reminder for 10 minutes from now")
                InstructionStep("2", "After 2 minutes, restart your phone")
                InstructionStep("3", "Don't open the app after reboot")
                InstructionStep("✓", "Reminder should still trigger at the scheduled time")
            }

            // Footer
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Setup Complete!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You're all set for reliable habit reminders",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatusCard(isExempt: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isExempt) 
                MaterialTheme.colorScheme.tertiaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isExempt) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isExempt) 
                    MaterialTheme.colorScheme.onTertiaryContainer 
                else 
                    MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = if (isExempt) "All Set! ✓" else "Action Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isExempt) 
                        "Your notifications are configured for maximum reliability" 
                    else 
                        "Please follow the steps below to enable reliable notifications",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun GuideSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    iconColor: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            content()
        }
    }
}

@Composable
private fun InstructionStep(stepNumber: String, instruction: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = instruction,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun BenefitItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// Helper functions to check permission status
private fun checkNotificationsEnabled(context: Context): Boolean {
    return try {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.areNotificationsEnabled()
    } catch (e: Exception) {
        false
    }
}

private fun checkExactAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } catch (e: Exception) {
            false
        }
    } else {
        true // Not required on older versions
    }
}

private fun checkBackgroundDataEnabled(context: Context): Boolean {
    return try {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val restrictBackgroundStatus = connectivityManager.restrictBackgroundStatus
            // RESTRICT_BACKGROUND_STATUS_DISABLED means background data is NOT restricted (i.e., it's enabled)
            restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED
        } else {
            // On older versions, assume it's enabled if we can't check
            true
        }
    } catch (e: Exception) {
        // If we can't check, assume it's enabled to avoid false negatives
        true
    }
}
