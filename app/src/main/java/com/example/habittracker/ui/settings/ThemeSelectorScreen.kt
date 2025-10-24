package it.atraj.habittracker.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import it.atraj.habittracker.R
import it.atraj.habittracker.ui.HabitViewModel
import it.atraj.habittracker.ui.theme.AppTheme
import it.atraj.habittracker.ui.theme.rememberThemeManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorScreen(
    onBackClick: () -> Unit,
    onThemeSelected: (AppTheme) -> Unit,
    viewModel: HabitViewModel = hiltViewModel()
) {
    val themeManager = rememberThemeManager()
    var selectedTheme by remember { mutableStateOf(themeManager.getCurrentTheme()) }
    val purchasedThemes by viewModel.purchasedThemes.collectAsState()
    val userRewards by viewModel.userRewards.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showPurchaseDialog by remember { mutableStateOf<AppTheme?>(null) }
    var showInsufficientDiamondsDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "App Theme",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose your style",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "🎨 Available Themes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(AppTheme.values().toList()) { theme ->
                val isUnlocked = theme.price == 0 || purchasedThemes.contains(theme.name)
                
                ThemeCard(
                    theme = theme,
                    isSelected = theme == selectedTheme,
                    isUnlocked = isUnlocked,
                    currentDiamonds = userRewards.diamonds,
                    onClick = {
                        if (isUnlocked) {
                            selectedTheme = theme
                            themeManager.setTheme(theme)
                            onThemeSelected(theme)
                        } else {
                            // Show purchase dialog
                            if (userRewards.diamonds >= theme.price) {
                                showPurchaseDialog = theme
                            } else {
                                showInsufficientDiamondsDialog = true
                            }
                        }
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Column {
                            Text(
                                text = "Theme applies instantly!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Your selected theme changes the entire app's look and feel.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Purchase confirmation dialog
    showPurchaseDialog?.let { theme ->
        AlertDialog(
            onDismissRequest = { showPurchaseDialog = null },
            icon = {
                Text(text = theme.emoji, style = MaterialTheme.typography.headlineLarge)
            },
            title = {
                Text(text = "Purchase ${theme.displayName}?")
            },
            text = {
                Column {
                    Text("This will unlock the ${theme.displayName} theme for 💎 ${theme.price} diamonds.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your balance: 💎 ${userRewards.diamonds} diamonds",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val success = viewModel.purchaseTheme(theme.name, theme.price)
                            if (success) {
                                selectedTheme = theme
                                themeManager.setTheme(theme)
                                onThemeSelected(theme)
                            }
                            showPurchaseDialog = null
                        }
                    }
                ) {
                    Text("Purchase")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Insufficient diamonds dialog
    if (showInsufficientDiamondsDialog) {
        AlertDialog(
            onDismissRequest = { showInsufficientDiamondsDialog = false },
            icon = {
                Text(text = "💎", style = MaterialTheme.typography.headlineLarge)
            },
            title = {
                Text(text = "Not Enough Diamonds")
            },
            text = {
                Text("You need more diamonds to unlock this theme. Complete habits to earn diamonds!")
            },
            confirmButton = {
                TextButton(onClick = { showInsufficientDiamondsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    isUnlocked: Boolean,
    currentDiamonds: Int,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else if (!isUnlocked)
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        else 
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        animationSpec = tween(300),
        label = "borderColor"
    )
    
    val (colors, description) = getThemePreview(theme)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Theme preview with color circles or character images
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(colors = colors)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Show character images for Itachi and All Might themes
                    when (theme) {
                        AppTheme.ITACHI -> {
                            Image(
                                painter = painterResource(id = R.drawable.itachi_theme),
                                contentDescription = "Itachi Uchiha",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        AppTheme.ALL_MIGHT -> {
                            Image(
                                painter = painterResource(id = R.drawable.allmight_theme),
                                contentDescription = "All Might",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        AppTheme.COD_MW -> {
                            Image(
                                painter = painterResource(id = R.drawable.call_of_duty_theme),
                                contentDescription = "Call of Duty Modern Warfare",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        AppTheme.GENSHIN -> {
                            Image(
                                painter = painterResource(id = R.drawable.genshin_impact_theme),
                                contentDescription = "Genshin Impact",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            Text(
                                text = theme.emoji,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
                
                Column {
                    Text(
                        text = theme.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    
                    // Color preview dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        colors.take(3).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                }
            }
            
            // Right side: Lock icon with price OR check icon for selected
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                if (!isUnlocked) {
                    // Show lock icon and price for locked themes
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💎",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = theme.price.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (currentDiamonds >= theme.price) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else if (isSelected) {
                    // Check icon for selected theme
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Get preview colors and description for each theme
 */
private fun getThemePreview(theme: AppTheme): Pair<List<Color>, String> {
    return when (theme) {
        AppTheme.DEFAULT -> listOf(
            Color(0xFF6650a4),
            Color(0xFF625b71),
            Color(0xFF7D5260)
        ) to "Material You design"
        
        AppTheme.HALLOWEEN -> listOf(
            Color(0xFFFF6B35),
            Color(0xFF8B4FBF),
            Color(0xFF4CAF50)
        ) to "Spooky orange & purple"
        
        AppTheme.EASTER -> listOf(
            Color(0xFFFFB3D9),
            Color(0xFFADD8E6),
            Color(0xFFFFF9C4)
        ) to "Soft pastel colors"
        
        AppTheme.ITACHI -> listOf(
            Color(0xFFE53935),
            Color(0xFF424242),
            Color(0xFFECEFF1)
        ) to "Sharingan red & black"
        
        AppTheme.ALL_MIGHT -> listOf(
            Color(0xFF2196F3),
            Color(0xFFFFD700),
            Color(0xFFFF1744)
        ) to "Heroic blue & gold"
        
        AppTheme.SAKURA -> listOf(
            Color(0xFFFFB7C5),
            Color(0xFFE1BEE7),
            Color(0xFFA5D6A7)
        ) to "Cherry blossom pink"
        
        AppTheme.COD_MW -> listOf(
            Color(0xFF8D7B68),
            Color(0xFF556B2F),
            Color(0xFFFF6B35)
        ) to "Military tactical theme"
        
        AppTheme.GENSHIN -> listOf(
            Color(0xFF4FC3F7),
            Color(0xFFFFB300),
            Color(0xFF9C27B0)
        ) to "Celestial adventure"
    }
}
