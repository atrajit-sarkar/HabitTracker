# 🔥 Mangekyo Sharingan Animations for Habit Tracker

## Overview
Professional rotating Mangekyo Sharingan eye animations created using Python and optimized as Lottie JSON files for use in the Habit Tracker Android app.

## Generated Files

### Animation Files
| File | Description | Size | Pattern |
|------|-------------|------|---------|
| `mangekyo_itachi.json` | Itachi's 3-blade pinwheel pattern | 4.24 KB | Rotating clockwise |
| `mangekyo_sasuke.json` | Sasuke's 6-pointed star pattern | 5.87 KB | Rotating clockwise |
| `mangekyo_kakashi.json` | Kakashi's Kamui spiral vortex | 4.93 KB | Rotating clockwise |
| `mangekyo_obito.json` | Obito's reverse Kamui spiral | 4.96 KB | Rotating counter-clockwise |
| `sharingan_animation.json` | Classic Sharingan with tomoe | 2.39 KB | Standard 3-tomoe rotation |

### Preview Files
- `sharingan_preview.gif` - Animated GIF preview (131 KB)

**Total optimized size: ~22 KB** (Perfect for mobile apps!)

## 🎨 Visual Features

### All Animations Include:
- ✅ Smooth 360° rotation at 30 FPS
- ✅ Professional red gradient iris (#C41E3A)
- ✅ Black rotating pattern elements
- ✅ Central pupil with realistic highlight
- ✅ Outer black ring for definition
- ✅ Transparent background (RGBA support)
- ✅ Infinite loop capability
- ✅ Optimized for mobile performance

## 📱 Integration into Habit Tracker App

### Step 1: Add Lottie Dependency

Add to `app/build.gradle`:
```gradle
dependencies {
    implementation 'com.airbnb.android:lottie:6.1.0'
}
```

### Step 2: Copy Animation Files

Copy the JSON files to your assets folder:
```
app/src/main/assets/animations/
├── mangekyo_itachi.json
├── mangekyo_sasuke.json
├── mangekyo_kakashi.json
├── mangekyo_obito.json
└── sharingan_animation.json
```

### Step 3: Use in XML Layout

```xml
<com.airbnb.lottie.LottieAnimationView
    android:id="@+id/sharinganAnimation"
    android:layout_width="100dp"
    android:layout_height="100dp"
    app:lottie_fileName="animations/mangekyo_itachi.json"
    app:lottie_loop="true"
    app:lottie_autoPlay="true"
    app:lottie_speed="1.0" />
```

### Step 4: Control Programmatically (Kotlin)

```kotlin
// In your Activity or Fragment
val animationView: LottieAnimationView = findViewById(R.id.sharinganAnimation)

// Start animation
animationView.playAnimation()

// Pause animation
animationView.pauseAnimation()

// Change animation
animationView.setAnimation("animations/mangekyo_sasuke.json")
animationView.playAnimation()

// Set speed (0.5 = half speed, 2.0 = double speed)
animationView.speed = 1.5f

// Add animation listener
animationView.addAnimatorListener(object : Animator.AnimatorListener {
    override fun onAnimationStart(animation: Animator) {
        // Animation started
    }
    override fun onAnimationEnd(animation: Animator) {
        // Animation ended (won't trigger if looping)
    }
    override fun onAnimationCancel(animation: Animator) {}
    override fun onAnimationRepeat(animation: Animator) {}
})
```

## 🎯 Use Cases in Habit Tracker

### 1. **Achievement Unlock Animation**
When user reaches a milestone streak:
```kotlin
fun showAchievementAnimation() {
    binding.achievementEye.apply {
        setAnimation("animations/mangekyo_itachi.json")
        playAnimation()
        postDelayed({ visibility = View.GONE }, 3000)
    }
}
```

### 2. **Power-Up/Streak Freeze Indicator**
Show different patterns for different power-ups:
```kotlin
fun showPowerUpAnimation(powerUpType: String) {
    val animation = when(powerUpType) {
        "STREAK_FREEZE" -> "animations/mangekyo_kakashi.json"
        "DOUBLE_XP" -> "animations/mangekyo_sasuke.json"
        "LEGENDARY" -> "animations/mangekyo_itachi.json"
        else -> "animations/sharingan_animation.json"
    }
    binding.powerUpIndicator.setAnimation(animation)
    binding.powerUpIndicator.playAnimation()
}
```

### 3. **Loading Indicator**
Use as a unique loading animation:
```kotlin
fun showLoading(show: Boolean) {
    if (show) {
        binding.loadingEye.visibility = View.VISIBLE
        binding.loadingEye.setAnimation("animations/mangekyo_obito.json")
        binding.loadingEye.playAnimation()
    } else {
        binding.loadingEye.cancelAnimation()
        binding.loadingEye.visibility = View.GONE
    }
}
```

### 4. **Profile Avatar Enhancement**
Animated border around user avatar when on a hot streak:
```kotlin
fun setStreakAnimation(streakDays: Int) {
    when {
        streakDays >= 100 -> {
            binding.avatarAnimation.setAnimation("animations/mangekyo_itachi.json")
            binding.avatarAnimation.playAnimation()
        }
        streakDays >= 50 -> {
            binding.avatarAnimation.setAnimation("animations/mangekyo_sasuke.json")
            binding.avatarAnimation.playAnimation()
        }
        streakDays >= 30 -> {
            binding.avatarAnimation.setAnimation("animations/sharingan_animation.json")
            binding.avatarAnimation.playAnimation()
        }
        else -> {
            binding.avatarAnimation.cancelAnimation()
        }
    }
}
```

### 5. **Focus Mode Indicator**
When user enters deep focus mode:
```xml
<FrameLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <!-- Your content -->
    
    <com.airbnb.lottie.LottieAnimationView
        android:id="@+id/focusModeEye"
        android:layout_width="60dp"
        android:layout_height="60dp"
        android:layout_gravity="top|end"
        android:layout_margin="16dp"
        android:alpha="0.7"
        app:lottie_fileName="animations/mangekyo_kakashi.json"
        app:lottie_loop="true"
        app:lottie_speed="0.8" />
</FrameLayout>
```

## 🎨 Customization Options

### Changing Speed
```kotlin
// Slower, more dramatic
animationView.speed = 0.5f

// Faster, more intense
animationView.speed = 2.0f
```

### Changing Size
```xml
<!-- Small (icon size) -->
android:layout_width="48dp"
android:layout_height="48dp"

<!-- Medium (button size) -->
android:layout_width="100dp"
android:layout_height="100dp"

<!-- Large (full screen) -->
android:layout_width="300dp"
android:layout_height="300dp"
```

### Adding Effects
```kotlin
// Fade in/out
animationView.alpha = 0f
animationView.animate()
    .alpha(1f)
    .setDuration(500)
    .start()

// Scale pulse effect
animationView.animate()
    .scaleX(1.2f)
    .scaleY(1.2f)
    .setDuration(200)
    .withEndAction {
        animationView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .start()
    }
    .start()
```

## 🔧 Advanced Customization

### Creating Custom Patterns

To create new patterns, modify `create_advanced_sharingan.py`:

```python
def create_custom_pattern(self, total_frames):
    """Create your custom pattern"""
    layers = []
    
    # Add your custom shapes here
    # Example: 8-pointed star
    for i in range(8):
        angle = i * 45
        # ... create layer with your shape
        layers.append(layer)
    
    return layers
```

Then add to the patterns dictionary in `main()`:
```python
patterns = {
    "custom": "My Custom Pattern"
}
```

Run the script again:
```bash
python create_advanced_sharingan.py
```

## 📊 Performance Optimization

### Best Practices:
1. **Use hardware acceleration**: Lottie animations are hardware accelerated by default
2. **Limit concurrent animations**: Show only 2-3 animations at once
3. **Pause when not visible**: 
   ```kotlin
   override fun onPause() {
       super.onPause()
       animationView.pauseAnimation()
   }
   
   override fun onResume() {
       super.onResume()
       animationView.resumeAnimation()
   }
   ```
4. **Cache animations**: Lottie automatically caches loaded animations
5. **Use appropriate sizes**: Don't scale up small animations excessively

## 🎭 Animation Comparison

| Pattern | Best For | Personality | Speed Recommendation |
|---------|----------|-------------|---------------------|
| **Itachi** (3-blade) | Major achievements | Powerful, legendary | 1.0x - 1.2x |
| **Sasuke** (6-star) | Streak milestones | Sharp, focused | 1.2x - 1.5x |
| **Kakashi** (Kamui) | Loading, transitions | Mysterious, flowing | 0.8x - 1.0x |
| **Obito** (Reverse Kamui) | Time-based events | Dynamic, counter | 0.8x - 1.0x |
| **Classic** (3-tomoe) | General use | Traditional, balanced | 1.0x |

## 🐛 Troubleshooting

### Animation not showing?
1. Check file path: `app/src/main/assets/animations/`
2. Verify Lottie dependency is added
3. Check if view size is > 0dp
4. Ensure `app:lottie_autoPlay="true"` or call `playAnimation()`

### Animation lagging?
1. Reduce animation size (width/height)
2. Slow down speed: `animationView.speed = 0.8f`
3. Check if too many animations running simultaneously
4. Use `app:lottie_renderMode="HARDWARE"` for better performance

### File not found error?
```kotlin
// Check if file exists
val assetManager = context.assets
try {
    val inputStream = assetManager.open("animations/mangekyo_itachi.json")
    inputStream.close()
    // File exists
} catch (e: IOException) {
    Log.e("Animation", "File not found: ${e.message}")
}
```

## 📦 File Structure

```
HabitTracker/
├── create_sharingan_animation.py          # Basic Sharingan generator
├── create_advanced_sharingan.py           # Advanced Mangekyo generator
├── mangekyo_itachi.json                   # ✅ Generated
├── mangekyo_sasuke.json                   # ✅ Generated
├── mangekyo_kakashi.json                  # ✅ Generated
├── mangekyo_obito.json                    # ✅ Generated
├── sharingan_animation.json               # ✅ Generated
├── sharingan_preview.gif                  # ✅ Preview
└── MANGEKYO_SHARINGAN_ANIMATIONS.md      # This file
```

## 🚀 Quick Integration Script

To quickly integrate into your app, run:

```powershell
# Copy animations to app assets
New-Item -ItemType Directory -Force -Path "app/src/main/assets/animations"
Copy-Item "mangekyo_*.json" "app/src/main/assets/animations/"
Copy-Item "sharingan_animation.json" "app/src/main/assets/animations/"
```

Or use the provided helper script:
```powershell
.\integrate_sharingan_animations.ps1
```

## 📝 License & Credits

These animations were created using Python (matplotlib, numpy, pillow) and converted to Lottie JSON format. They are inspired by the Naruto anime series and designed for use in the Habit Tracker app.

**Created by:** Copilot + Python Animation Generator  
**Date:** October 27, 2025  
**Version:** 1.0  

---

**Need different patterns or customization? Run the Python scripts with different parameters or modify the pattern creation functions!** 🎨🔥
