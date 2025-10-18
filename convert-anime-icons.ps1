# Script to convert anime icons to Android launcher icons
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

# Icon sizes for different densities
$densities = @{
    "mdpi" = 48
    "hdpi" = 72
    "xhdpi" = 96
    "xxhdpi" = 144
    "xxxhdpi" = 192
}

$sourceDir = "E:\CodingWorld\AndroidAppDev\HabitTracker\icons"
$resDir = "E:\CodingWorld\AndroidAppDev\HabitTracker\app\src\main\res"

# Process anime.png
Write-Host "Processing anime.png..."
foreach ($density in $densities.Keys) {
    $size = $densities[$density]
    $outputPath = "$resDir\mipmap-$density\ic_launcher_anime.png"
    Write-Host "  Creating $density ($size x $size px)..."
    Resize-Image -InputPath "$sourceDir\anime.png" -OutputPath $outputPath -Width $size -Height $size
}

# Process angry-anime.png
Write-Host "`nProcessing angry-anime.png..."
foreach ($density in $densities.Keys) {
    $size = $densities[$density]
    $outputPath = "$resDir\mipmap-$density\ic_launcher_angry_anime.png"
    Write-Host "  Creating $density ($size x $size px)..."
    Resize-Image -InputPath "$sourceDir\angry-anime.png" -OutputPath $outputPath -Width $size -Height $size
}

# Process warning-anime.png
Write-Host "`nProcessing warning-anime.png..."
foreach ($density in $densities.Keys) {
    $size = $densities[$density]
    $outputPath = "$resDir\mipmap-$density\ic_launcher_warning_anime.png"
    Write-Host "  Creating $density ($size x $size px)..."
    Resize-Image -InputPath "$sourceDir\warning-anime.png" -OutputPath $outputPath -Width $size -Height $size
}

# Process sitama.png
Write-Host "`nProcessing sitama.png..."
foreach ($density in $densities.Keys) {
    $size = $densities[$density]
    $outputPath = "$resDir\mipmap-$density\ic_launcher_sitama.png"
    Write-Host "  Creating $density ($size x $size px)..."
    Resize-Image -InputPath "$sourceDir\sitama.png" -OutputPath $outputPath -Width $size -Height $size
}

# Process angry-sitama.png
Write-Host "`nProcessing angry-sitama.png..."
foreach ($density in $densities.Keys) {
    $size = $densities[$density]
    $outputPath = "$resDir\mipmap-$density\ic_launcher_angry_sitama.png"
    Write-Host "  Creating $density ($size x $size px)..."
    Resize-Image -InputPath "$sourceDir\angry-sitama.png" -OutputPath $outputPath -Width $size -Height $size
}

# Process warning-sitama.png
Write-Host "`nProcessing warning-sitama.png..."
foreach ($density in $densities.Keys) {
    $size = $densities[$density]
    $outputPath = "$resDir\mipmap-$density\ic_launcher_warning_sitama.png"
    Write-Host "  Creating $density ($size x $size px)..."
    Resize-Image -InputPath "$sourceDir\warning-sitama.png" -OutputPath $outputPath -Width $size -Height $size
}

Write-Host "`nIcon conversion complete!"
Write-Host "All anime and sitama icons have been generated for all density folders."

# Process bird.png (user-provided themed icon)
Write-Host "`nProcessing bird.png..."
foreach ($density in $densities.Keys) {
    $size = $densities[$density]
    $outputPath = "$resDir\mipmap-$density\ic_launcher_bird.png"
    Write-Host "  Creating $density ($size x $size px)..."
    Resize-Image -InputPath "$sourceDir\bird.png" -OutputPath $outputPath -Width $size -Height $size
}

# Process angry-bird.png
Write-Host "`nProcessing angry-bird.png..."
foreach ($density in $densities.Keys) {
    $size = $densities[$density]
    $outputPath = "$resDir\mipmap-$density\ic_launcher_angry_bird.png"
    Write-Host "  Creating $density ($size x $size px)..."
    Resize-Image -InputPath "$sourceDir\angry-bird.png" -OutputPath $outputPath -Width $size -Height $size
}

# Process warning-bird.png
Write-Host "`nProcessing warning-bird.png..."
foreach ($density in $densities.Keys) {
    $size = $densities[$density]
    $outputPath = "$resDir\mipmap-$density\ic_launcher_warning_bird.png"
    Write-Host "  Creating $density ($size x $size px)..."
    Resize-Image -InputPath "$sourceDir\warning-bird.png" -OutputPath $outputPath -Width $size -Height $size
}

Write-Host "`nBird icons generated for all density folders."
