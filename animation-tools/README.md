# 🎨 Animation Tools

Organized tools for converting GIF and MP4 files to Lottie animations for the Habit Tracker app.

## 📁 Folder Structure

```
animation-tools/
├── input/              # Place your GIF/MP4 files here
├── output/             # Generated Lottie JSON files
├── scripts/            # Python converter scripts
│   ├── gif_to_lottie.py
│   └── mp4_to_lottie.py
├── convert-gif.ps1     # Easy GIF converter
├── convert-mp4.ps1     # Easy MP4 converter
└── README.md           # This file
```

## 🚀 Quick Start

### 1. Setup (One-time)

Make sure Python virtual environment is set up:
```powershell
# Already done if you've run the converters before
# Virtual environment is at: ../.venv
```

Required packages (already installed):
- Pillow
- numpy
- opencv-python
- rembg
- onnxruntime

### 2. Convert GIF to Lottie

```powershell
# Place your GIF in the input/ folder
.\convert-gif.ps1 -InputFile "my-animation.gif" -OutputName "my_animation"
```

**Options:**
- `-InputFile`: Your GIF filename (in input/ folder) or absolute path
- `-OutputName`: Name for the output JSON (without .json extension)
- `-MaxSize`: Maximum dimension in pixels (default: 256)
- `-TargetFps`: Target frames per second (default: 25)
- `-RemoveBackground`: true/false (default: true)

### 3. Convert MP4 to Lottie

```powershell
# Place your MP4 in the input/ folder
.\convert-mp4.ps1 -InputFile "my-video.mp4" -OutputName "my_animation"
```

**Options:**
- `-InputFile`: Your MP4 filename (in input/ folder) or absolute path
- `-OutputName`: Name for the output JSON (without .json extension)
- `-MaxSize`: Maximum dimension in pixels (default: 256)
- `-TargetFps`: Target frames per second (default: 12 for smooth 1x speed)
- `-BackgroundMethod`: 'ai', 'simple', or 'none' (default: 'ai')
  - **'ai'**: Best quality, uses AI to detect subject vs background (recommended)
  - **'simple'**: Fast, removes black/white backgrounds only
  - **'none'**: No background removal
- `-Speed`: Animation speed for Kotlin code (default: 0.75 for slower playback)

**Examples:**

```powershell
# Basic conversion with AI background removal
.\convert-mp4.ps1 -InputFile "dance.mp4" -OutputName "dance_anim"

# Faster animation (1x speed)
.\convert-mp4.ps1 -InputFile "dance.mp4" -OutputName "dance_fast" -Speed 1.0

# Higher FPS for smoother animation
.\convert-mp4.ps1 -InputFile "dance.mp4" -OutputName "dance_smooth" -TargetFps 20

# Simple background removal (faster processing)
.\convert-mp4.ps1 -InputFile "dance.mp4" -OutputName "dance_simple" -BackgroundMethod simple

# No background removal
.\convert-mp4.ps1 -InputFile "dance.mp4" -OutputName "dance_nobg" -BackgroundMethod none
```

## 📝 Using in Your App

After conversion, the script will ask if you want to copy to app assets. Say 'y' to automatically copy to:
```
app/src/main/assets/[OutputName].json
```

Then use in your Kotlin code:

```kotlin
@Composable
private fun MyAnimationOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.Asset("my_animation.json")
        )
        
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            isPlaying = true,
            speed = 0.75f, // Use the Speed value from conversion
            restartOnPlay = true
        )
        
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(250.dp)
        )
    }
}
```

## 🎯 Tips for Best Results

### For MP4 Videos:

1. **Background Color**: Best results with solid colored backgrounds
   - Black backgrounds work well with AI removal
   - Green/Blue screens work great (chroma key)
   
2. **Video Quality**: 
   - Keep videos short (1-3 seconds for looping animations)
   - Higher resolution = larger file size
   - Use `MaxSize=256` for good balance of quality/size

3. **Frame Rate**:
   - 12 FPS = smooth, normal speed
   - 20 FPS = very smooth, slightly faster
   - Lower FPS = smaller file size

4. **Speed Adjustment**:
   - 0.75x = slower, more visible (recommended)
   - 1.0x = normal speed
   - 0.5x = half speed (very slow)

### For GIF Files:

1. **File Size**: GIFs typically create larger Lottie files than MP4s
2. **Background**: Transparent GIFs work best
3. **Frame Rate**: 25 FPS is good for most GIFs

## 🔧 Advanced Usage

### Direct Python Script Usage

If you prefer to use the Python scripts directly:

```powershell
# GIF Conversion
E:\.venv\Scripts\python.exe scripts\gif_to_lottie.py input\my.gif output\my.json

# MP4 Conversion with custom settings
E:\.venv\Scripts\python.exe -c "
from scripts.mp4_to_lottie import convert_mp4_to_lottie
convert_mp4_to_lottie(
    'input/my.mp4',
    'output/my.json',
    remove_bg=True,
    max_size=256,
    target_fps=12,
    skip_frames=1,
    duplicate_threshold=0.02,
    bg_method='ai'
)
"
```

## 📊 File Size Guide

Typical Lottie file sizes:
- **Small** (< 100 KB): 256px, 10-15 frames, good compression
- **Medium** (100-200 KB): 256px, 15-25 frames
- **Large** (> 200 KB): 512px or 30+ frames

Smaller is better for app performance!

## ❓ Troubleshooting

**Background not removed properly?**
- Try `ai` method instead of `simple`
- For MP4s, use a solid colored background when recording
- Black clothing may be removed with black backgrounds - use green/blue screen

**Animation too fast/slow?**
- Adjust `-TargetFps` (lower = slower, higher = faster)
- Adjust `-Speed` in Kotlin code (0.5 = half speed, 2.0 = double speed)

**File size too large?**
- Reduce `-MaxSize` (try 128 or 192)
- Lower `-TargetFps`
- Keep video duration short

**Python errors?**
- Make sure virtual environment is activated
- Check that all packages are installed: `pip list`

## 📚 Resources

- [Lottie Documentation](https://airbnb.io/lottie/)
- [rembg (Background Removal)](https://github.com/danielgatis/rembg)
- [Lottie Android](https://github.com/airbnb/lottie-android)

## 🎬 Current Animations

- `do_a_habit.json` - Main habit completion animation
  - Source: `gifassests/do-a-habit.mp4`
  - Settings: 256px, 12 FPS, AI background removal, 0.75x speed
  - Size: ~115 KB
  - Duration: ~1 second per loop
