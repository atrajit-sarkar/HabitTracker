# 🎨 Glittering Profile Photo - Customization Guide

## 🔧 Quick Customization Reference

### Change Animation Speed

#### Rotation Speed
```kotlin
// Current: 3 seconds per rotation
val rotationAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
        animation = tween(3000, easing = LinearEasing), // ← Change this
        repeatMode = RepeatMode.Restart
    )
)

// Options:
tween(2000)  // Faster (2s)
tween(5000)  // Slower (5s)
tween(10000) // Very slow (10s)
```

#### Pulse Speed
```kotlin
// Current: 1.5 seconds pulse
val scale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.1f,
    animationSpec = infiniteRepeatable(
        animation = tween(1500, easing = FastOutSlowInEasing), // ← Change this
        repeatMode = RepeatMode.Reverse
    )
)

// Options:
tween(1000)  // Faster pulse (1s)
tween(2000)  // Slower pulse (2s)
tween(500)   // Rapid pulse (0.5s)
```

#### Sparkle Speed
```kotlin
// Sparkle 1: 2 seconds
animation = tween(2000, easing = LinearEasing)

// Sparkle 2: 2.5 seconds
animation = tween(2500, easing = LinearEasing)

// Sparkle 3: 2.2 seconds
animation = tween(2200, easing = LinearEasing)

// Make all same speed:
tween(2000) // All sparkles synchronized
```

---

## 🌈 Change Color Schemes

### Gold Theme (Default)
```kotlin
val gradientColors = listOf(
    Color(0xFFFFD700), // Gold
    Color(0xFFFFE55C), // Light gold
    Color(0xFFFFFFFF), // White
    Color(0xFFFFE55C), // Light gold
    Color(0xFFFFD700), // Gold
    Color(0xFFFFA500), // Orange
    Color(0xFFFFD700)  // Gold
)
```

### Blue Ocean Theme
```kotlin
val gradientColors = listOf(
    Color(0xFF4FC3F7), // Light blue
    Color(0xFF29B6F6), // Blue
    Color(0xFFFFFFFF), // White
    Color(0xFF29B6F6), // Blue
    Color(0xFF4FC3F7), // Light blue
    Color(0xFF0288D1), // Dark blue
    Color(0xFF4FC3F7)  // Light blue
)
```

### Purple Galaxy Theme
```kotlin
val gradientColors = listOf(
    Color(0xFFBA68C8), // Light purple
    Color(0xFFAB47BC), // Purple
    Color(0xFFFFFFFF), // White
    Color(0xFFAB47BC), // Purple
    Color(0xFFBA68C8), // Light purple
    Color(0xFF8E24AA), // Dark purple
    Color(0xFFBA68C8)  // Light purple
)
```

### Emerald Green Theme
```kotlin
val gradientColors = listOf(
    Color(0xFF66BB6A), // Light green
    Color(0xFF4CAF50), // Green
    Color(0xFFFFFFFF), // White
    Color(0xFF4CAF50), // Green
    Color(0xFF66BB6A), // Light green
    Color(0xFF388E3C), // Dark green
    Color(0xFF66BB6A)  // Light green
)
```

### Rose Pink Theme
```kotlin
val gradientColors = listOf(
    Color(0xFFF06292), // Light pink
    Color(0xFFEC407A), // Pink
    Color(0xFFFFFFFF), // White
    Color(0xFFEC407A), // Pink
    Color(0xFFF06292), // Light pink
    Color(0xFFE91E63), // Deep pink
    Color(0xFFF06292)  // Light pink
)
```

### Rainbow Theme
```kotlin
val gradientColors = listOf(
    Color(0xFFFF0000), // Red
    Color(0xFFFF7F00), // Orange
    Color(0xFFFFFF00), // Yellow
    Color(0xFF00FF00), // Green
    Color(0xFF0000FF), // Blue
    Color(0xFF4B0082), // Indigo
    Color(0xFF9400D3)  // Violet
)
```

---

## ✨ Adjust Sparkle Effects

### Sparkle Count

#### Add More Sparkles (6 total)
```kotlin
// Add 3 more sparkle animations
val sparkle4Angle by infiniteTransition.animateFloat(
    initialValue = 60f,
    targetValue = 420f,
    animationSpec = infiniteRepeatable(
        animation = tween(1800, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    ),
    label = "sparkle4"
)

val sparkle5Angle by infiniteTransition.animateFloat(
    initialValue = 180f,
    targetValue = 540f,
    animationSpec = infiniteRepeatable(
        animation = tween(2300, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    ),
    label = "sparkle5"
)

val sparkle6Angle by infiniteTransition.animateFloat(
    initialValue = 300f,
    targetValue = 660f,
    animationSpec = infiniteRepeatable(
        animation = tween(2100, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    ),
    label = "sparkle6"
)

// Draw them
drawSparkle(sparkle4Angle, sparkleDistance)
drawSparkle(sparkle5Angle, sparkleDistance)
drawSparkle(sparkle6Angle, sparkleDistance)
```

#### Remove Sparkles (1 only)
```kotlin
// Comment out sparkle2 and sparkle3
// val sparkle2Angle by ...
// val sparkle3Angle by ...

// Only draw sparkle1
drawSparkle(sparkle1Angle, sparkleDistance)
```

### Sparkle Size

#### Larger Sparkles
```kotlin
// Change sparkle size from 6dp to 10dp
val sparkleSize = 10.dp.toPx()  // Increased from 6dp
drawCircle(
    color = Color.White,
    radius = sparkleSize,
    center = Offset(sparkleX, sparkleY),
    alpha = alpha
)
drawCircle(
    color = Color(0xFFFFD700),
    radius = sparkleSize * 0.6f,
    center = Offset(sparkleX, sparkleY),
    alpha = alpha * 0.8f
)
```

#### Smaller Sparkles
```kotlin
val sparkleSize = 4.dp.toPx()  // Decreased from 6dp
```

### Sparkle Distance

#### Move Sparkles Closer
```kotlin
// Closer to profile photo
val sparkleDistance = radius + 8.dp.toPx()  // Reduced from 12dp
```

#### Move Sparkles Further
```kotlin
// Further from profile photo
val sparkleDistance = radius + 16.dp.toPx()  // Increased from 12dp
```

---

## 📏 Adjust Sizes

### Profile Photo Size

#### Larger Photo
```kotlin
Box(
    modifier = modifier
        .size(150.dp) // Increased from 120dp (outer container)
        // ...
) {
    Box(
        modifier = Modifier
            .size(130.dp) // Increased from 100dp (inner photo)
            // ...
    )
}
```

#### Smaller Photo
```kotlin
Box(
    modifier = modifier
        .size(100.dp) // Decreased from 120dp
        // ...
) {
    Box(
        modifier = Modifier
            .size(80.dp) // Decreased from 100dp
            // ...
    )
}
```

### Ring Thickness

#### Thicker Rings
```kotlin
// Outer ring
style = Stroke(width = 6.dp.toPx())  // Increased from 4dp

// Middle ring
style = Stroke(width = 4.dp.toPx())  // Increased from 2dp

// Inner border
.border(
    6.dp,  // Increased from 4dp
    // ...
)
```

#### Thinner Rings
```kotlin
// Outer ring
style = Stroke(width = 2.dp.toPx())  // Decreased from 4dp

// Middle ring
style = Stroke(width = 1.dp.toPx())  // Decreased from 2dp

// Inner border
.border(
    2.dp,  // Decreased from 4dp
    // ...
)
```

---

## 💫 Modify Animation Intensity

### Pulse Intensity

#### Stronger Pulse
```kotlin
val scale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.3f,  // Increased from 1.1f
    animationSpec = infiniteRepeatable(
        animation = tween(1500, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
)
```

#### Subtle Pulse
```kotlin
val scale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.05f,  // Decreased from 1.1f
    animationSpec = infiniteRepeatable(
        animation = tween(1500, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
)
```

### Sparkle Twinkle

#### More Twinkling
```kotlin
val alpha by infiniteTransition.animateFloat(
    initialValue = 0.1f,  // Lower minimum (more contrast)
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(800, easing = FastOutSlowInEasing),  // Faster
        repeatMode = RepeatMode.Reverse
    )
)
```

#### Less Twinkling
```kotlin
val alpha by infiniteTransition.animateFloat(
    initialValue = 0.6f,  // Higher minimum (less contrast)
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(2000, easing = FastOutSlowInEasing),  // Slower
        repeatMode = RepeatMode.Reverse
    )
)
```

---

## 🎭 Opacity Adjustments

### Overall Brightness

#### Brighter Effect (for dark themes)
```kotlin
// Outer ring
alpha = 0.8f,  // Increased from 0.6f

// Middle ring
alpha = alpha * 0.6f,  // Increased from 0.4f

// Sparkles - already using animated alpha, so good!
```

#### Dimmer Effect (for light themes)
```kotlin
// Outer ring
alpha = 0.4f,  // Decreased from 0.6f

// Middle ring
alpha = alpha * 0.3f,  // Decreased from 0.4f
```

---

## 🔄 Animation Direction

### Reverse Rotation
```kotlin
// Counter-clockwise rotation
val rotationAngle by infiniteTransition.animateFloat(
    initialValue = 360f,  // Start at 360
    targetValue = 0f,     // Go to 0
    animationSpec = infiniteRepeatable(
        animation = tween(3000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )
)
```

### Oscillating Rotation (back and forth)
```kotlin
val rotationAngle by infiniteTransition.animateFloat(
    initialValue = -30f,   // Start at -30°
    targetValue = 30f,     // Go to +30°
    animationSpec = infiniteRepeatable(
        animation = tween(2000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse  // Changed to Reverse
    )
)
```

---

## 🎨 Theme-Aware Colors

### Use Material Theme Colors
```kotlin
val gradientColors = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.primaryContainer,
    MaterialTheme.colorScheme.secondary,
    MaterialTheme.colorScheme.secondaryContainer,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.tertiaryContainer,
    MaterialTheme.colorScheme.primary
)
```

### Adapt to Light/Dark Mode
```kotlin
val isDarkMode = isSystemInDarkTheme()

val gradientColors = if (isDarkMode) {
    // Softer colors for dark mode
    listOf(
        Color(0xFFFFD700).copy(alpha = 0.7f),
        Color(0xFFFFE55C).copy(alpha = 0.7f),
        Color(0xFFFFFFFF).copy(alpha = 0.5f),
        // ...
    )
} else {
    // Vibrant colors for light mode
    listOf(
        Color(0xFFFFD700),
        Color(0xFFFFE55C),
        Color(0xFFFFFFFF),
        // ...
    )
}
```

---

## ⚙️ Performance Optimizations

### Reduce Animation Complexity

#### Lower Frame Rate (for low-end devices)
```kotlin
// Add throttling
LaunchedEffect(Unit) {
    while (true) {
        delay(33) // ~30fps instead of 60fps
        // Force recomposition at lower rate
    }
}
```

#### Disable on Low-End Devices
```kotlin
@Composable
fun GlitteringProfilePhoto(
    // ...
    enableAnimations: Boolean = true  // Add parameter
) {
    if (!enableAnimations) {
        // Show static version
        Box(/* static implementation */)
        return
    }
    
    // Regular animated version
    // ...
}
```

---

## 🎯 Preset Configurations

### Subtle Mode (minimal distraction)
```kotlin
// Slow rotation
tween(5000)

// Gentle pulse
targetValue = 1.05f

// Few sparkles
// Only use sparkle1

// Dim opacity
alpha = 0.4f
```

### Party Mode (maximum effect!)
```kotlin
// Fast rotation
tween(1000)

// Strong pulse
targetValue = 1.3f

// Many sparkles (6+)
// All sparkles visible

// Bright colors
alpha = 0.9f

// Rainbow colors!
val gradientColors = /* rainbow theme */
```

### Professional Mode (elegant)
```kotlin
// Medium rotation
tween(4000)

// Subtle pulse
targetValue = 1.08f

// 3 sparkles (balanced)
// Standard sparkles

// Muted colors
listOf(
    Color(0xFF90A4AE), // Blue grey
    Color(0xFF607D8B),
    Color(0xFFFFFFFF),
    // ...
)
```

---

## 🛠️ Advanced Customizations

### Add Second Gradient Layer
```kotlin
// After existing gradient
drawCircle(
    brush = Brush.radialGradient(
        colors = listOf(
            Color.Transparent,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        radius = radius
    ),
    radius = radius,
    center = Offset(centerX, centerY)
)
```

### Add Glow Effect
```kotlin
// Draw multiple overlapping circles with decreasing alpha
for (i in 1..5) {
    drawCircle(
        brush = brush,
        radius = radius + (i * 2.dp.toPx()),
        center = Offset(centerX, centerY),
        alpha = 0.6f / i,  // Decreasing alpha
        style = Stroke(width = 2.dp.toPx())
    )
}
```

### Add Star Shapes Instead of Circles
```kotlin
fun drawStar(angle: Float, distance: Float) {
    val radians = Math.toRadians(angle.toDouble())
    val sparkleX = centerX + (distance * cos(radians)).toFloat()
    val sparkleY = centerY + (distance * sin(radians)).toFloat()
    
    val path = Path().apply {
        // Draw 5-pointed star
        moveTo(sparkleX, sparkleY - 6.dp.toPx())
        lineTo(sparkleX + 2.dp.toPx(), sparkleY + 2.dp.toPx())
        lineTo(sparkleX + 7.dp.toPx(), sparkleY + 2.dp.toPx())
        lineTo(sparkleX + 3.dp.toPx(), sparkleY + 5.dp.toPx())
        lineTo(sparkleX + 4.dp.toPx(), sparkleY + 10.dp.toPx())
        lineTo(sparkleX, sparkleY + 7.dp.toPx())
        lineTo(sparkleX - 4.dp.toPx(), sparkleY + 10.dp.toPx())
        lineTo(sparkleX - 3.dp.toPx(), sparkleY + 5.dp.toPx())
        lineTo(sparkleX - 7.dp.toPx(), sparkleY + 2.dp.toPx())
        lineTo(sparkleX - 2.dp.toPx(), sparkleY + 2.dp.toPx())
        close()
    }
    
    drawPath(
        path = path,
        color = Color(0xFFFFD700),
        alpha = alpha
    )
}
```

---

## 📱 Responsive Design

### Tablet-Specific Adjustments
```kotlin
val configuration = LocalConfiguration.current
val isTablet = configuration.screenWidthDp >= 600

val photoSize = if (isTablet) 150.dp else 120.dp
val innerSize = if (isTablet) 130.dp else 100.dp
```

### Accessibility Considerations
```kotlin
// Respect reduced motion preference
val motionPreference = LocalAccessibilityManager.current?.isReduceMotionEnabled

if (motionPreference == true) {
    // Show static version or very slow animations
    tween(10000)  // Very slow
} else {
    // Normal animations
    tween(3000)
}
```

---

## 🎊 Special Effects

### Birthday Mode
```kotlin
// Confetti-like sparkles
val sparkleColors = listOf(
    Color.Red, Color.Blue, Color.Green,
    Color.Yellow, Color.Magenta, Color.Cyan
)

fun drawSparkle(angle: Float, distance: Float) {
    val color = sparkleColors.random()
    // Draw with random colors
}
```

### Achievement Unlock Effect
```kotlin
// Burst animation when avatar changed
var justChanged by remember { mutableStateOf(false) }

LaunchedEffect(currentAvatar) {
    justChanged = true
    delay(2000)
    justChanged = false
}

if (justChanged) {
    // Show extra sparkles or flash effect
}
```

---

## 📋 Quick Copy-Paste Presets

### Preset 1: Calm Ocean
```kotlin
// Slow, blue, peaceful
val gradientColors = listOf(
    Color(0xFF4FC3F7), Color(0xFF29B6F6), Color(0xFFFFFFFF),
    Color(0xFF29B6F6), Color(0xFF4FC3F7), Color(0xFF0288D1), Color(0xFF4FC3F7)
)
tween(5000)  // Slow rotation
targetValue = 1.05f  // Gentle pulse
```

### Preset 2: Vibrant Energy
```kotlin
// Fast, rainbow, exciting
val gradientColors = listOf(
    Color(0xFFFF0000), Color(0xFFFF7F00), Color(0xFFFFFF00),
    Color(0xFF00FF00), Color(0xFF0000FF), Color(0xFF4B0082), Color(0xFF9400D3)
)
tween(1500)  // Fast rotation
targetValue = 1.15f  // Strong pulse
```

### Preset 3: Elegant Gold (Default)
```kotlin
// Medium, gold, balanced
val gradientColors = listOf(
    Color(0xFFFFD700), Color(0xFFFFE55C), Color(0xFFFFFFFF),
    Color(0xFFFFE55C), Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFFD700)
)
tween(3000)  // Medium rotation
targetValue = 1.1f  // Balanced pulse
```

---

**Tip**: After making changes, always test in both light and dark modes!

**Performance**: Keep an eye on FPS if you add many sparkles or complex effects.

**User Experience**: More animation isn't always better - aim for delight, not distraction!
