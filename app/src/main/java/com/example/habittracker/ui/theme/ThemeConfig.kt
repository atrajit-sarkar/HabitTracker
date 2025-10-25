package it.atraj.habittracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * CompositionLocal for accessing theme configuration
 */
val LocalThemeConfig = staticCompositionLocalOf { 
    getThemeConfig(AppTheme.DEFAULT) 
}

/**
 * Theme-specific configuration for icons, shapes, and customizations
 */
data class ThemeConfig(
    val shapes: Shapes,
    val icons: ThemeIcons,
    val fontFamily: FontFamily = FontFamily.Default,
    val animationStyle: AnimationStyle = AnimationStyle.SMOOTH,
    val cardElevation: Int = 4,
    val buttonStyle: ButtonStyle = ButtonStyle.ROUNDED
)

/**
 * Custom icons for different themes
 */
data class ThemeIcons(
    val check: ImageVector = Icons.Default.CheckCircle,
    val add: ImageVector = Icons.Default.Add,
    val fire: ImageVector = Icons.Default.Whatshot,
    val star: ImageVector = Icons.Default.Star,
    val settings: ImageVector = Icons.Default.Settings,
    val calendar: ImageVector = Icons.Default.CalendarToday,
    val notification: ImageVector = Icons.Default.Notifications,
    val favorite: ImageVector = Icons.Default.Favorite,
    val trophy: ImageVector = Icons.Default.EmojiEvents,
    val shield: ImageVector = Icons.Default.Security,
    val sparkle: ImageVector = Icons.Default.AutoAwesome,
    val moon: ImageVector = Icons.Default.DarkMode,
    val skull: ImageVector = Icons.Default.BugReport, // Closest to skull
    val flower: ImageVector = Icons.Default.LocalFlorist,
    val sword: ImageVector = Icons.Default.Hardware, // Closest to sword
    val lightning: ImageVector = Icons.Default.ElectricBolt,
    val diamond: ImageVector = Icons.Default.Diamond,
    val weapon: ImageVector = Icons.Default.AutoFixHigh, // Closest to weapon
    val magic: ImageVector = Icons.Default.AutoAwesome
)

enum class AnimationStyle {
    SMOOTH,     // Default smooth animations
    BOUNCY,     // Extra bouncy animations
    QUICK,      // Fast snappy animations
    ELEGANT     // Slow elegant animations
}

enum class ButtonStyle {
    ROUNDED,    // Standard rounded corners
    SHARP,      // Sharp square corners
    PILL,       // Fully rounded pill shape
    CUT         // Cut corners (diamond-like)
}

/**
 * Get theme configuration for each app theme
 */
fun getThemeConfig(theme: AppTheme): ThemeConfig {
    return when (theme) {
        AppTheme.DEFAULT -> ThemeConfig(
            shapes = Shapes(
                small = RoundedCornerShape(8.dp),
                medium = RoundedCornerShape(12.dp),
                large = RoundedCornerShape(16.dp)
            ),
            icons = ThemeIcons(
                check = Icons.Default.CheckCircle,
                fire = Icons.Default.Whatshot,
                star = Icons.Default.Star,
                trophy = Icons.Default.EmojiEvents
            ),
            fontFamily = FontFamily.Default,
            animationStyle = AnimationStyle.SMOOTH,
            cardElevation = 4,
            buttonStyle = ButtonStyle.ROUNDED
        )
        
        AppTheme.HALLOWEEN -> ThemeConfig(
            shapes = Shapes(
                small = RoundedCornerShape(4.dp),
                medium = RoundedCornerShape(8.dp),
                large = RoundedCornerShape(12.dp)
            ),
            icons = ThemeIcons(
                check = Icons.Default.CheckCircle,
                fire = Icons.Default.Whatshot,
                star = Icons.Default.Star,
                trophy = Icons.Default.Nightlight, // Moon for spooky feel
                favorite = Icons.Default.FavoriteBorder,
                sparkle = Icons.Default.AutoAwesome,
                moon = Icons.Default.DarkMode,
                skull = Icons.Default.BugReport,
                diamond = Icons.Default.Hexagon, // Different diamond shape
                add = Icons.Default.AddCircle // Circular add for Halloween
            ),
            fontFamily = FontFamily.Monospace,
            animationStyle = AnimationStyle.BOUNCY,
            cardElevation = 8,
            buttonStyle = ButtonStyle.SHARP
        )
        
        AppTheme.EASTER -> ThemeConfig(
            shapes = Shapes(
                small = RoundedCornerShape(16.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(24.dp)
            ),
            icons = ThemeIcons(
                check = Icons.Default.CheckCircle,
                fire = Icons.Default.LocalFlorist, // Flower instead of fire!
                star = Icons.Default.Star,
                trophy = Icons.Default.Spa,
                favorite = Icons.Default.Favorite,
                sparkle = Icons.Default.AutoAwesome,
                flower = Icons.Default.LocalFlorist,
                diamond = Icons.Default.Egg, // Egg for Easter!
                add = Icons.Default.AddCircleOutline // Outlined for softness
            ),
            fontFamily = FontFamily.SansSerif,
            animationStyle = AnimationStyle.ELEGANT,
            cardElevation = 2,
            buttonStyle = ButtonStyle.PILL
        )
        
        AppTheme.ITACHI -> ThemeConfig(
            shapes = Shapes(
                small = RoundedCornerShape(2.dp),
                medium = RoundedCornerShape(4.dp),
                large = RoundedCornerShape(8.dp)
            ),
            icons = ThemeIcons(
                check = Icons.Default.CheckCircle,
                fire = Icons.Default.Whatshot,
                star = Icons.Default.Star,
                trophy = Icons.Default.Security,
                shield = Icons.Default.Security,
                sword = Icons.Default.Hardware,
                lightning = Icons.Default.ElectricBolt
            ),
            fontFamily = FontFamily.Monospace,
            animationStyle = AnimationStyle.QUICK,
            cardElevation = 6,
            buttonStyle = ButtonStyle.SHARP
        )
        
        AppTheme.ALL_MIGHT -> ThemeConfig(
            shapes = Shapes(
                small = RoundedCornerShape(8.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(24.dp)
            ),
            icons = ThemeIcons(
                check = Icons.Default.CheckCircle,
                fire = Icons.Default.Whatshot,
                star = Icons.Default.Star,
                trophy = Icons.Default.EmojiEvents,
                shield = Icons.Default.Security,
                lightning = Icons.Default.ElectricBolt,
                sparkle = Icons.Default.AutoAwesome
            ),
            fontFamily = FontFamily.SansSerif,
            animationStyle = AnimationStyle.BOUNCY,
            cardElevation = 8,
            buttonStyle = ButtonStyle.ROUNDED
        )
        
        AppTheme.SAKURA -> ThemeConfig(
            shapes = Shapes(
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(20.dp)
            ),
            icons = ThemeIcons(
                check = Icons.Default.CheckCircle,
                fire = Icons.Default.LocalFlorist, // Flower instead of fire
                star = Icons.Default.Star,
                trophy = Icons.Default.LocalFlorist,
                favorite = Icons.Default.Favorite,
                flower = Icons.Default.LocalFlorist,
                sparkle = Icons.Default.AutoAwesome,
                diamond = Icons.Default.Spa // Zen/spa icon for diamonds
            ),
            fontFamily = FontFamily.Serif,
            animationStyle = AnimationStyle.ELEGANT,
            cardElevation = 3,
            buttonStyle = ButtonStyle.PILL
        )
        
        AppTheme.COD_MW -> ThemeConfig(
            shapes = Shapes(
                small = RoundedCornerShape(0.dp),
                medium = RoundedCornerShape(2.dp),
                large = RoundedCornerShape(4.dp)
            ),
            icons = ThemeIcons(
                check = Icons.Default.CheckCircle,
                fire = Icons.Default.Whatshot,
                star = Icons.Default.Star,
                trophy = Icons.Default.MilitaryTech,
                shield = Icons.Default.Security,
                weapon = Icons.Default.AutoFixHigh,
                sword = Icons.Default.Hardware,
                diamond = Icons.Default.Shield, // Shield for military diamonds
                add = Icons.Default.Add // Simple add
            ),
            fontFamily = FontFamily.Monospace,
            animationStyle = AnimationStyle.QUICK,
            cardElevation = 2,
            buttonStyle = ButtonStyle.SHARP
        )
        
        AppTheme.GENSHIN -> ThemeConfig(
            shapes = Shapes(
                small = RoundedCornerShape(8.dp),
                medium = RoundedCornerShape(12.dp),
                large = RoundedCornerShape(16.dp)
            ),
            icons = ThemeIcons(
                check = Icons.Default.CheckCircle,
                fire = Icons.Default.Whatshot,
                star = Icons.Default.Star,
                trophy = Icons.Default.EmojiEvents,
                sparkle = Icons.Default.AutoAwesome,
                magic = Icons.Default.AutoAwesome,
                diamond = Icons.Default.Diamond,
                lightning = Icons.Default.ElectricBolt
            ),
            fontFamily = FontFamily.SansSerif,
            animationStyle = AnimationStyle.SMOOTH,
            cardElevation = 6,
            buttonStyle = ButtonStyle.ROUNDED
        )
    }
}

/**
 * Get animation duration based on animation style
 */
fun AnimationStyle.getDuration(): Int {
    return when (this) {
        AnimationStyle.SMOOTH -> 300
        AnimationStyle.BOUNCY -> 400
        AnimationStyle.QUICK -> 150
        AnimationStyle.ELEGANT -> 500
    }
}

/**
 * Get spring stiffness based on animation style
 */
fun AnimationStyle.getStiffness(): Float {
    return when (this) {
        AnimationStyle.SMOOTH -> 300f
        AnimationStyle.BOUNCY -> 200f
        AnimationStyle.QUICK -> 500f
        AnimationStyle.ELEGANT -> 150f
    }
}

/**
 * Get spring damping based on animation style
 */
fun AnimationStyle.getDamping(): Float {
    return when (this) {
        AnimationStyle.SMOOTH -> 0.7f
        AnimationStyle.BOUNCY -> 0.5f
        AnimationStyle.QUICK -> 0.9f
        AnimationStyle.ELEGANT -> 0.8f
    }
}
