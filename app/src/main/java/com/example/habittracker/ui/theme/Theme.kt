package it.atraj.habittracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Default color schemes
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// ========================================
// 🎃 HALLOWEEN THEME
// ========================================
private val HalloweenLightColorScheme = lightColorScheme(
    primary = HalloweenOrangeDark,        // Darker for better contrast
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC5),
    onPrimaryContainer = Color(0xFF2D1600),
    
    secondary = HalloweenPurpleDark,      // Darker for better contrast
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5C9FF),
    onSecondaryContainer = Color(0xFF2D1A3D),
    
    tertiary = HalloweenGreenDark,        // Darker for better contrast
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA8F5BB),
    onTertiaryContainer = Color(0xFF00210B),
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFFFEDD8),
    onSurfaceVariant = Color(0xFF4F4539),
    
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF857569),
    outlineVariant = Color(0xFFD8C2B6)
)

private val HalloweenDarkColorScheme = darkColorScheme(
    primary = HalloweenOrangeLight,       // Brighter for dark mode
    onPrimary = Color(0xFF4D1F00),
    primaryContainer = HalloweenOrangeDark,
    onPrimaryContainer = Color(0xFFFFDCC5),
    
    secondary = HalloweenPurpleLight,     // Brighter for dark mode
    onSecondary = Color(0xFF3D2647),
    secondaryContainer = HalloweenPurpleDark,
    onSecondaryContainer = Color(0xFFE5C9FF),
    
    tertiary = HalloweenGreenLight,       // Brighter for dark mode
    onTertiary = Color(0xFF003919),
    tertiaryContainer = HalloweenGreenDark,
    onTertiaryContainer = Color(0xFFA8F5BB),
    
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// ========================================
// 🥚 EASTER THEME
// ========================================
private val EasterLightColorScheme = lightColorScheme(
    primary = Color(0xFFD81B60),          // Darker pink for better contrast
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E9),
    onPrimaryContainer = Color(0xFF3D0026),
    
    secondary = Color(0xFF0277BD),        // Darker blue for better contrast
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE5FF),
    onSecondaryContainer = Color(0xFF001D33),
    
    tertiary = Color(0xFFF9A825),         // Darker yellow for better contrast
    onTertiary = Color(0xFF3D3500),
    tertiaryContainer = Color(0xFFFFFFE5),
    onTertiaryContainer = Color(0xFF2B2400),
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFFFF0E6),
    onSurfaceVariant = Color(0xFF524539),
    
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF857569),
    outlineVariant = Color(0xFFD8C2B6)
)

private val EasterDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB3D9),          // Bright pastel pink for dark mode
    onPrimary = Color(0xFF5C0039),
    primaryContainer = Color(0xFF810048),
    onPrimaryContainer = Color(0xFFFFD9E9),
    
    secondary = Color(0xFF90CAF9),        // Bright pastel blue for dark mode
    onSecondary = Color(0xFF003D5C),
    secondaryContainer = Color(0xFF01579B),
    onSecondaryContainer = Color(0xFFCCE5FF),
    
    tertiary = Color(0xFFFFF59D),         // Bright pastel yellow for dark mode
    onTertiary = Color(0xFF3D3500),
    tertiaryContainer = Color(0xFFF57F17),
    onTertiaryContainer = Color(0xFFFFFFE5),
    
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// ========================================
// 🔴 ITACHI UCHIHA THEME
// ========================================
private val ItachiLightColorScheme = lightColorScheme(
    primary = ItachiRedDark,              // Darker red for better contrast
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    
    secondary = ItachiGreyDark,           // Darker grey for better contrast
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF1C1C1C),
    
    tertiary = Color(0xFF546E7A),         // Better contrast grey-blue
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFD8DC),
    onTertiaryContainer = Color(0xFF263238),
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

private val ItachiDarkColorScheme = darkColorScheme(
    primary = ItachiRedLight,             // Bright red for dark mode
    onPrimary = Color(0xFF690005),
    primaryContainer = ItachiRedDark,
    onPrimaryContainer = Color(0xFFFFDAD6),
    
    secondary = ItachiGreyLight,          // Light grey for dark mode
    onSecondary = Color(0xFF1C1C1C),
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFE0E0E0),
    
    tertiary = ItachiWhite,               // Light for dark mode
    onTertiary = Color(0xFF37474F),
    tertiaryContainer = Color(0xFF546E7A),
    onTertiaryContainer = Color(0xFFECEFF1),
    
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// ========================================
// 💪 ALL MIGHT THEME
// ========================================
private val AllMightLightColorScheme = lightColorScheme(
    primary = AllMightBlueDark,           // Darker blue for better contrast
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    
    secondary = AllMightYellowDark,       // Deep orange-gold for better contrast
    onSecondary = Color.Black,            // Black text on orange-gold (maximum contrast)
    secondaryContainer = Color(0xFFFFE57F),
    onSecondaryContainer = Color(0xFF1C1500),
    
    tertiary = AllMightRedDark,           // Darker red for better contrast
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD6),
    onTertiaryContainer = Color(0xFF410002),
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE3F2FD),
    onSurfaceVariant = Color(0xFF42474E),
    
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6CF)
)

private val AllMightDarkColorScheme = darkColorScheme(
    primary = AllMightBlueLight,          // Bright blue for dark mode
    onPrimary = Color(0xFF003258),
    primaryContainer = AllMightBlueDark,
    onPrimaryContainer = Color(0xFFD1E4FF),
    
    secondary = AllMightYellow,           // Bright gold for dark mode
    onSecondary = Color.Black,            // Black text on bright yellow (maximum contrast)
    secondaryContainer = AllMightYellowDark,
    onSecondaryContainer = Color.White,   // White text on deep orange-gold
    
    tertiary = AllMightRedLight,          // Bright red for dark mode
    onTertiary = Color(0xFF690005),
    tertiaryContainer = AllMightRedDark,
    onTertiaryContainer = Color(0xFFFFDAD6),
    
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// ========================================
// 🌸 SAKURA THEME
// ========================================
private val SakuraLightColorScheme = lightColorScheme(
    primary = SakuraPinkDark,             // Darker pink for better contrast
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E6),
    onPrimaryContainer = Color(0xFF3D0021),
    
    secondary = SakuraPurpleDark,         // Darker purple for better contrast
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E5F5),
    onSecondaryContainer = Color(0xFF33203B),
    
    tertiary = SakuraGreenDark,           // Darker green for better contrast
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8F5D1),
    onTertiaryContainer = Color(0xFF002106),
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFFFE4EE),
    onSurfaceVariant = Color(0xFF524349),
    
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF7D5260),
    outlineVariant = Color(0xFFD0C3C9)
)

private val SakuraDarkColorScheme = darkColorScheme(
    primary = SakuraPinkLight,            // Bright pink for dark mode
    onPrimary = Color(0xFF4A0028),        // Dark text on bright pink (high contrast)
    primaryContainer = SakuraPinkDark,
    onPrimaryContainer = Color(0xFFFFD9E6),
    
    secondary = SakuraPurpleLight,        // Bright purple for dark mode
    onSecondary = Color(0xFF381E42),      // Dark text on bright purple (high contrast)
    secondaryContainer = SakuraPurpleDark,
    onSecondaryContainer = Color(0xFFF3E5F5),
    
    tertiary = SakuraGreenLight,          // Bright green for dark mode
    onTertiary = Color(0xFF00531B),
    tertiaryContainer = SakuraGreenDark,
    onTertiaryContainer = Color(0xFFC8F5D1),
    
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// ========================================
// 🎮 CALL OF DUTY MODERN WARFARE THEME
// ========================================
private val CodMwLightColorScheme = lightColorScheme(
    primary = CodTanDark,                 // Military tan
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DED2),
    onPrimaryContainer = Color(0xFF2D2418),
    
    secondary = CodGreenDark,             // Military green
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E3C4),
    onSecondaryContainer = Color(0xFF1A2410),
    
    tertiary = CodOrangeDark,             // Alert orange
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC5),
    onTertiaryContainer = Color(0xFF2D1600),
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE8E0D8),
    onSurfaceVariant = Color(0xFF4A4539),
    
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF7B7569),
    outlineVariant = Color(0xFFCCC2B6)
)

private val CodMwDarkColorScheme = darkColorScheme(
    primary = CodTanLight,                // Lighter tan for dark mode
    onPrimary = Color(0xFF3D3427),
    primaryContainer = CodTanDark,
    onPrimaryContainer = Color(0xFFE8DED2),
    
    secondary = CodGreenLight,            // Brighter green for dark mode
    onSecondary = Color(0xFF283A18),
    secondaryContainer = CodGreenDark,
    onSecondaryContainer = Color(0xFFD4E3C4),
    
    tertiary = CodOrangeLight,            // Bright orange for dark mode
    onTertiary = Color(0xFF4D1F00),
    tertiaryContainer = CodOrangeDark,
    onTertiaryContainer = Color(0xFFFFDCC5),
    
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

// ========================================
// ✨ GENSHIN IMPACT THEME
// ========================================
private val GenshinLightColorScheme = lightColorScheme(
    primary = GenshinBlueDark,            // Celestial blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1F5FE),
    onPrimaryContainer = Color(0xFF001D33),
    
    secondary = GenshinGoldDark,          // Golden primogems
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFECB3),
    onSecondaryContainer = Color(0xFF2D2200),
    
    tertiary = GenshinPurpleDark,         // Vision purple
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E5F5),
    onTertiaryContainer = Color(0xFF33203B),
    
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE1F5FE),
    onSurfaceVariant = Color(0xFF42474E),
    
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6CF)
)

private val GenshinDarkColorScheme = darkColorScheme(
    primary = GenshinBlueLight,           // Bright celestial blue
    onPrimary = Color(0xFF003258),
    primaryContainer = GenshinBlueDark,
    onPrimaryContainer = Color(0xFFE1F5FE),
    
    secondary = GenshinGoldLight,         // Bright golden
    onSecondary = Color(0xFF3D2F00),
    secondaryContainer = GenshinGoldDark,
    onSecondaryContainer = Color(0xFFFFECB3),
    
    tertiary = GenshinPurpleLight,        // Bright purple
    onTertiary = Color(0xFF381E42),
    tertiaryContainer = GenshinPurpleDark,
    onTertiaryContainer = Color(0xFFF3E5F5),
    
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

@Composable
fun HabitTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    customTheme: AppTheme = AppTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Custom themes take priority
        customTheme != AppTheme.DEFAULT -> {
            when (customTheme) {
                AppTheme.HALLOWEEN -> if (darkTheme) HalloweenDarkColorScheme else HalloweenLightColorScheme
                AppTheme.EASTER -> if (darkTheme) EasterDarkColorScheme else EasterLightColorScheme
                AppTheme.ITACHI -> if (darkTheme) ItachiDarkColorScheme else ItachiLightColorScheme
                AppTheme.ALL_MIGHT -> if (darkTheme) AllMightDarkColorScheme else AllMightLightColorScheme
                AppTheme.SAKURA -> if (darkTheme) SakuraDarkColorScheme else SakuraLightColorScheme
                AppTheme.COD_MW -> if (darkTheme) CodMwDarkColorScheme else CodMwLightColorScheme
                AppTheme.GENSHIN -> if (darkTheme) GenshinDarkColorScheme else GenshinLightColorScheme
                AppTheme.DEFAULT -> if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Theme enum for selection
enum class AppTheme(val displayName: String, val emoji: String, val price: Int = 0) {
    DEFAULT("Default", "🎨", 0),        // Free
    HALLOWEEN("Halloween", "🎃", 10),
    EASTER("Easter Egg", "🥚", 10),
    ITACHI("Itachi Uchiha", "🔴", 10),
    ALL_MIGHT("All Might", "💪", 10),
    SAKURA("Sakura", "🌸", 10),
    COD_MW("Call of Duty MW", "🎮", 10),
    GENSHIN("Genshin Impact", "✨", 10)
}
