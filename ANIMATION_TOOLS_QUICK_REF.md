# 🎨 Animation Tools - Quick Reference

## Location
```
E:\CodingWorld\AndroidAppDev\HabitTracker\animation-tools\
```

## Quick Commands

### Convert MP4 (Recommended Settings)
```powershell
# From project root
& ".\animation-tools\convert-mp4.ps1" -InputFile "your-video.mp4" -OutputName "your_animation"

# With custom speed
& ".\animation-tools\convert-mp4.ps1" -InputFile "your-video.mp4" -OutputName "your_animation" -Speed 0.75

# Faster processing (simple bg removal)
& ".\animation-tools\convert-mp4.ps1" -InputFile "your-video.mp4" -OutputName "your_animation" -BackgroundMethod simple

# No background removal
& ".\animation-tools\convert-mp4.ps1" -InputFile "your-video.mp4" -OutputName "your_animation" -BackgroundMethod none
```

### Convert GIF
```powershell
& ".\animation-tools\convert-gif.ps1" -InputFile "your-animation.gif" -OutputName "your_animation"
```

## Workflow

1. **Place your files**: 
   - Put MP4/GIF files in `animation-tools/input/`
   - Or use absolute paths

2. **Run converter**:
   ```powershell
   & ".\animation-tools\convert-mp4.ps1" -InputFile "my-video.mp4" -OutputName "my_anim"
   ```

3. **Auto-copy to assets**:
   - Script will ask: `Copy to app assets? (y/n)`
   - Type `y` to copy to `app/src/main/assets/`

4. **Use in Kotlin**:
   ```kotlin
   LottieCompositionSpec.Asset("my_anim.json")
   speed = 0.75f  // Use the recommended speed from converter
   ```

## Parameters

### MP4 Converter
| Parameter | Default | Description |
|-----------|---------|-------------|
| `-InputFile` | Required | MP4 filename or path |
| `-OutputName` | Required | Output JSON name (no extension) |
| `-TargetFps` | 12 | Frames per second (lower = slower) |
| `-Speed` | 0.75 | Kotlin playback speed (0.5 = half speed) |
| `-BackgroundMethod` | ai | 'ai', 'simple', or 'none' |
| `-MaxSize` | 256 | Max dimension in pixels |

### GIF Converter
| Parameter | Default | Description |
|-----------|---------|-------------|
| `-InputFile` | Required | GIF filename or path |
| `-OutputName` | Required | Output JSON name (no extension) |
| `-TargetFps` | 25 | Frames per second |
| `-MaxSize` | 256 | Max dimension in pixels |

## Tips

✅ **Best practices:**
- Use AI background removal for best quality
- 12 FPS for smooth 1x speed
- 0.75 speed in Kotlin for slower, more visible animations
- Keep videos 1-3 seconds for looping

⚠️ **Common issues:**
- Black clothes removed? → Video has black background, use AI method
- Animation too fast? → Lower TargetFps or reduce Speed
- File too large? → Reduce MaxSize or TargetFps

## File Structure
```
animation-tools/
├── input/               ← Place your MP4/GIF files here
├── output/              ← Generated JSON files
├── scripts/             ← Python converters (don't touch)
├── convert-mp4.ps1      ← Main MP4 converter
├── convert-gif.ps1      ← Main GIF converter
└── README.md            ← Full documentation
```

## Examples

```powershell
# Slow, smooth animation (recommended)
& ".\animation-tools\convert-mp4.ps1" -InputFile "celebrate.mp4" -OutputName "celebrate_anim" -TargetFps 12 -Speed 0.75

# Fast, energetic animation
& ".\animation-tools\convert-mp4.ps1" -InputFile "jump.mp4" -OutputName "jump_anim" -TargetFps 20 -Speed 1.0

# Ultra smooth, slow animation
& ".\animation-tools\convert-mp4.ps1" -InputFile "meditation.mp4" -OutputName "meditate_anim" -TargetFps 15 -Speed 0.5

# Quick test (no background removal)
& ".\animation-tools\convert-mp4.ps1" -InputFile "test.mp4" -OutputName "test" -BackgroundMethod none
```
