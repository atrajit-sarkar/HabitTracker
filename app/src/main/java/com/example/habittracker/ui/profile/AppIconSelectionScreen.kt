package it.atraj.habittracker.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import it.atraj.habittracker.R

data class AppIconOption(
    val id: String,
    val name: String,
    val iconRes: Int,
    val warningIconRes: Int? = null,
    val angryIconRes: Int? = null,
    val alias: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIconSelectionScreen(
    onBackClick: () -> Unit,
    viewModel: AppIconViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentIconId by viewModel.currentIconId.collectAsState()
    val isChangingIcon by viewModel.isChangingIcon.collectAsState()
    val isInOverdueMode by viewModel.isInOverdueMode.collectAsState()
    
    // Define available icon options with their warning and angry versions
    val iconOptions = remember {
        listOf(
            AppIconOption(
                id = "default",
                name = "Default",
                iconRes = R.mipmap.ic_launcher_default,
                warningIconRes = R.mipmap.ic_launcher_warning_default,
                angryIconRes = R.mipmap.ic_launcher_angry_default,
                alias = "it.atraj.habittracker.MainActivity.Default"
            ),
            AppIconOption(
                id = "anime",
                name = "Anime",
                iconRes = R.mipmap.ic_launcher_anime,
                warningIconRes = R.mipmap.ic_launcher_warning_anime,
                angryIconRes = R.mipmap.ic_launcher_angry_anime,
                alias = "it.atraj.habittracker.MainActivity.Anime"
            ),
            AppIconOption(
                id = "bird",
                name = "Bird",
                iconRes = R.mipmap.ic_launcher_bird,
                warningIconRes = R.mipmap.ic_launcher_warning_bird,
                angryIconRes = R.mipmap.ic_launcher_angry_bird,
                alias = "it.atraj.habittracker.MainActivity.Bird"
            ),
            AppIconOption(
                id = "sitama",
                name = "Sitama",
                iconRes = R.mipmap.ic_launcher_sitama,
                warningIconRes = R.mipmap.ic_launcher_warning_sitama,
                angryIconRes = R.mipmap.ic_launcher_angry_sitama,
                alias = "it.atraj.habittracker.MainActivity.Sitama"
            ),
            AppIconOption(
                id = "atrajit",
                name = "Atrajit",
                iconRes = R.mipmap.ic_launcher_atrajit,
                warningIconRes = R.mipmap.ic_launcher_warning_atrajit,
                angryIconRes = R.mipmap.ic_launcher_angry_atrajit,
                alias = "it.atraj.habittracker.MainActivity.Atrajit"
            )
        )
    }

    
    var showConfirmationDialog by remember { mutableStateOf<AppIconOption?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Choose App Icon",
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(16.dp)
        ) {
            // Warning card for overdue mode
            if (isInOverdueMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Icon Change Locked",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "You have overdue habits! Complete all overdue tasks to unlock app icon customization.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Header explanation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Personalize Your App",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Choose your default app icon. This won't affect the warning icons that appear when habits are overdue.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Icon grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(iconOptions) { option ->
                    AppIconCard(
                        option = option,
                        isSelected = currentIconId == option.id,
                        isChanging = isChangingIcon,
                        isDisabled = isInOverdueMode,
                        onClick = {
                            if (!isChangingIcon && !isInOverdueMode && currentIconId != option.id) {
                                showConfirmationDialog = option
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Confirmation dialog
    showConfirmationDialog?.let { option ->
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = null },
            icon = {
                val context = LocalContext.current
                val drawable = remember(option.iconRes) {
                    ContextCompat.getDrawable(context, option.iconRes)
                }
                drawable?.let {
                    Image(
                        bitmap = it.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                }
            },
            title = {
                Text(
                    "Change App Icon?",
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "Your app icon will change to \"${option.name}\". The app may restart to apply the change.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changeAppIcon(option.id, option.alias)
                        showConfirmationDialog = null
                    },
                    enabled = !isChangingIcon
                ) {
                    if (isChangingIcon) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Change Icon")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmationDialog = null },
                    enabled = !isChangingIcon
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AppIconCard(
    option: AppIconOption,
    isSelected: Boolean,
    isChanging: Boolean,
    isDisabled: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isChanging && !isDisabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isDisabled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon preview with 3 versions (default, warning, angry)
            if (option.warningIconRes != null && option.angryIconRes != null) {
                // Show 3-version layout
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Default version (larger)
                    val context = LocalContext.current
                    val defaultDrawable = remember(option.iconRes) {
                        ContextCompat.getDrawable(context, option.iconRes)
                    }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                2.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        defaultDrawable?.let {
                            Image(
                                bitmap = it.toBitmap().asImageBitmap(),
                                contentDescription = "Default",
                                modifier = Modifier.fillMaxSize(),
                                alpha = if (isDisabled) 0.4f else 1f
                            )
                        }
                    }
                    
                    // Warning & Angry versions (smaller)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Warning version
                        val warningDrawable = remember(option.warningIconRes) {
                            ContextCompat.getDrawable(context, option.warningIconRes!!)
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    1.dp,
                                    Color(0xFFFF9800).copy(alpha = 0.6f),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            warningDrawable?.let {
                                Image(
                                    bitmap = it.toBitmap().asImageBitmap(),
                                    contentDescription = "Warning",
                                    modifier = Modifier.fillMaxSize(),
                                    alpha = if (isDisabled) 0.4f else 1f
                                )
                            }
                        }
                        
                        // Angry version
                        val angryDrawable = remember(option.angryIconRes) {
                            ContextCompat.getDrawable(context, option.angryIconRes!!)
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            angryDrawable?.let {
                                Image(
                                    bitmap = it.toBitmap().asImageBitmap(),
                                    contentDescription = "Angry",
                                    modifier = Modifier.fillMaxSize(),
                                    alpha = if (isDisabled) 0.4f else 1f
                                )
                            }
                        }
                    }
                }
            } else {
                // Show single icon (old layout for backwards compatibility)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(
                            if (isSelected) 3.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val context = LocalContext.current
                    val drawable = remember(option.iconRes) {
                        ContextCompat.getDrawable(context, option.iconRes)
                    }
                    drawable?.let {
                        Image(
                            bitmap = it.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            alpha = if (isDisabled) 0.4f else 1f
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                option.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isDisabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                } else if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center
            )
            
            if (isSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}