# Script to convert default icons from JPG to PNG for all Android densities
# Requires .NET Framework (built into Windows)

Add-Type -AssemblyName System.Drawing

function Resize-Image {
    param(
        [string]$InputPath,
        [string]$OutputPath,
        [int]$Width,
        [int]$Height
    )
    
    $img = [System.Drawing.Image]::FromFile($InputPath)
    $bitmap = New-Object System.Drawing.Bitmap($Width, $Height)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    
    # Use high-quality rendering
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    
    $graphics.DrawImage($img, 0, 0, $Width, $Height)
    
    # Ensure output directory exists
    $outputDir = Split-Path -Parent $OutputPath
    if (!(Test-Path $outputDir)) {
        New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    }
    
    $bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    
    $graphics.Dispose()
    $bitmap.Dispose()
    $img.Dispose()
}

$baseDir = "E:\CodingWorld\AndroidAppDev\HabitTracker"
$iconsDir = "$baseDir\icons"
$resDir = "$baseDir\app\src\main\res"

# Define icon densities and sizes
$densities = @{
    "mdpi" = 48
    "hdpi" = 72
    "xhdpi" = 96
    "xxhdpi" = 144
    "xxxhdpi" = 192
}

# Define icons to convert
$icons = @(
    @{Source = "default.jpg"; Name = "ic_launcher_default"},
    @{Source = "default-warning.jpg"; Name = "ic_launcher_warning_default"},
    @{Source = "default-angry.jpg"; Name = "ic_launcher_angry_default"}
)

Write-Host "Converting default icons to PNG for all densities..." -ForegroundColor Green

foreach ($icon in $icons) {
    $sourcePath = Join-Path $iconsDir $icon.Source
    
    if (-not (Test-Path $sourcePath)) {
        Write-Host "Warning: Source file not found: $sourcePath" -ForegroundColor Yellow
        continue
    }
    
    Write-Host "`nProcessing $($icon.Source)..." -ForegroundColor Cyan
    
    foreach ($density in $densities.GetEnumerator()) {
        $size = $density.Value
        $mipmapDir = Join-Path $resDir "mipmap-$($density.Key)"
        $outputPath = Join-Path $mipmapDir "$($icon.Name).png"
        
        Write-Host "  Creating $($density.Key) (${size}x${size}): $($icon.Name).png"
        
        try {
            Resize-Image -InputPath $sourcePath -OutputPath $outputPath -Width $size -Height $size
            Write-Host "    Success" -ForegroundColor Green
        } catch {
            Write-Host "    Error: $_" -ForegroundColor Red
        }
    }
}

Write-Host "`nIcon conversion complete!" -ForegroundColor Green
