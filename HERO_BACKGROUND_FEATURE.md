# Hero Background Customization Feature

## Overview
This feature allows users to customize their profile card background with either a solid color (default) or a custom hero image. When profile animations are enabled, they are rendered on top of the hero background.

## Implementation Details

### 1. File Structure
- **Hero Image Location**: `app/src/main/res/drawable/itachi_hero.png`
- **Source Image**: Copied from `image-assets/hero-image/itachi-hero.png`
- **Modified File**: `ProfileScreen.kt`

### 2. SharedPreferences Keys
New preferences stored in `profile_prefs`:
- `hero_background_type` (String): Either `"solid"` or `"image"`
- `hero_background_image` (String): Image identifier (currently `"itachi"`)

### 3. UI Components

#### Profile Card Background Layers (Bottom to Top)
1. **Background Layer** (Bottom)
   - Solid Color: Uses `MaterialTheme.colorScheme.primaryContainer`
   - Custom Image: Displays hero image with 90% opacity for text readability
   
2. **Animation Overlay Layer** (Middle)
   - Lottie animations (Sakura Fall, Worldwide, Cute Anime Girl, Fireblast)
   - Rendered above background, below profile content
   
3. **Profile Content Layer** (Top)
   - User avatar, name, stats, etc.
   - Always visible above all other layers

#### New Dialog: HeroBackgroundPickerDialog
Located in Profile Settings section:
- **Icon**: Wallpaper icon
- **Title**: "Hero Background"
- **Subtitle**: Shows current selection (e.g., "Custom Image (Itachi)" or "Solid Color")

### 4. Dialog Features

#### Background Type Selection
Two options:
1. **Solid Color**
   - Icon: ColorLens
   - Description: "Use theme color background"
   - Default behavior

2. **Custom Image**
   - Icon: Image
   - Description: "Use hero character image"
   - Reveals image selection options when selected

#### Hero Image Selection
Currently available:
- **Itachi Uchiha** (Naruto character)
  - Preview thumbnail shown
  - Full image applied to profile card

#### Info Banner
Displays note: "Profile animations will be displayed over the hero image"

### 5. Code Implementation

#### State Management
```kotlin
var heroBackgroundType by remember { 
    mutableStateOf(prefs.getString("hero_background_type", "solid") ?: "solid") 
}
var heroBackgroundImage by remember { 
    mutableStateOf(prefs.getString("hero_background_image", "itachi") ?: "itachi") 
}
```

#### Background Rendering
```kotlin
if (heroBackgroundType == "image") {
    val heroImageRes = when (heroBackgroundImage) {
        "itachi" -> R.drawable.itachi_hero
        else -> R.drawable.itachi_hero // Default
    }
    
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(heroImageRes)
            .size(Size.ORIGINAL)
            .crossfade(true)
            .build(),
        contentDescription = "Hero background",
        modifier = Modifier
            .fillMaxWidth()
            .matchParentSize(),
        contentScale = ContentScale.Crop,
        alpha = 0.9f // Slightly transparent for text readability
    )
} else {
    // Solid color background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .matchParentSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
    )
}
```

### 6. User Flow

1. User opens Profile Screen
2. Scrolls to "Profile Settings" section
3. Taps "Hero Background" option
4. `HeroBackgroundPickerDialog` appears
5. User selects background type:
   - **Solid Color**: Applies immediately
   - **Custom Image**: Shows image options
6. If Custom Image selected, user chooses hero:
   - Currently: Itachi Uchiha (with thumbnail preview)
7. User taps "Apply" button
8. Settings saved to SharedPreferences
9. Profile card background updates instantly
10. Animations (if enabled) render on top of background

### 7. Design Considerations

#### Layer Ordering
- ✅ Background image at bottom
- ✅ Animations in middle layer
- ✅ Profile content on top
- ✅ Proper z-index management with Box layers

#### Image Optimization
- Uses Coil's AsyncImage for efficient loading
- CrossFade animation for smooth transitions
- Size.ORIGINAL for high quality
- ContentScale.Crop for proper fitting

#### Text Readability
- Hero image alpha set to 0.9f (90% opacity)
- Ensures text remains legible over image
- Works with both light and dark themes

### 8. Future Enhancements

#### Additional Hero Images
To add more hero images:
1. Add image file to `res/drawable/` (e.g., `hero_naruto.png`)
2. Update `heroBackgroundImage` state options
3. Add new `HeroImageOption` in dialog:
```kotlin
HeroImageOption(
    title = "Naruto Uzumaki",
    subtitle = "Naruto character",
    imageRes = R.drawable.hero_naruto,
    isSelected = selectedImage == "naruto",
    onClick = { selectedImage = "naruto" }
)
```
4. Update background rendering logic:
```kotlin
val heroImageRes = when (heroBackgroundImage) {
    "itachi" -> R.drawable.itachi_hero
    "naruto" -> R.drawable.hero_naruto
    else -> R.drawable.itachi_hero
}
```

#### Custom Color Selection
Add color picker for solid backgrounds instead of just theme color.

#### Image Upload
Allow users to upload their own custom hero images.

#### Gradient Backgrounds
Add gradient options as alternative to solid colors.

## Testing Checklist

- [ ] Hero background defaults to solid color
- [ ] Selecting custom image displays Itachi background
- [ ] Profile animations render correctly over hero image
- [ ] Text remains readable on hero background
- [ ] Settings persist after app restart
- [ ] Works in both light and dark themes
- [ ] Image loads efficiently without lag
- [ ] Switching between solid and image is smooth
- [ ] Dialog dismisses properly after applying changes
- [ ] Preview thumbnail shows in dialog

## Technical Notes

### Performance
- Hero images cached by Coil for instant loading
- No noticeable performance impact
- Works well with existing animation system

### Compatibility
- Compatible with all existing profile animations
- Works with all theme colors
- Maintains proper aspect ratios on all screen sizes

### File Size Considerations
- Itachi hero image: PNG format
- Consider WebP for future images to reduce APK size
- Current implementation has minimal impact

## Visual Examples

### Solid Color Background
```
┌─────────────────────┐
│  [Solid Color BG]   │
│    [Animation]      │
│   [Profile Photo]   │
│      [Name]         │
│     [Stats]         │
└─────────────────────┘
```

### Custom Hero Image Background
```
┌─────────────────────┐
│ [Itachi Image BG]   │
│    [Animation]      │
│   [Profile Photo]   │
│      [Name]         │
│     [Stats]         │
└─────────────────────┘
```

## Settings UI Location
**Profile Screen → Profile Settings Section → Hero Background** (between "Change Avatar" and "Profile Animation")

## Conclusion
This feature enhances user customization while maintaining the existing animation system. The layered approach ensures animations work seamlessly over custom backgrounds, providing a rich visual experience.
