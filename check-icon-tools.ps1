# Icon Optimization Tools Checker
Write-Host "========================================"  -ForegroundColor Cyan
Write-Host "  Icon Optimization Tools Check        " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check for tools
$hasPngquant = Get-Command pngquant -ErrorAction SilentlyContinue
$hasMagick = Get-Command magick -ErrorAction SilentlyContinue

Write-Host "Checking for optimization tools..." -ForegroundColor Yellow
Write-Host ""

if ($hasPngquant) {
    Write-Host "[OK] pngquant is installed" -ForegroundColor Green
    Write-Host "     You can run: .\optimize-icons.ps1" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host "[X] pngquant is NOT installed" -ForegroundColor Red
    Write-Host "    Install: choco install pngquant" -ForegroundColor Yellow
    Write-Host ""
}

if ($hasMagick) {
    Write-Host "[OK] ImageMagick is installed" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "[X] ImageMagick is NOT installed" -ForegroundColor Red
    Write-Host "    Install: choco install imagemagick" -ForegroundColor Yellow
    Write-Host ""
}

# Show current sizes
Write-Host "Current icon sizes:" -ForegroundColor Cyan
$resPath = "app\src\main\res"
$densities = @('mipmap-mdpi', 'mipmap-hdpi', 'mipmap-xhdpi', 'mipmap-xxhdpi', 'mipmap-xxxhdpi')
$totalSize = 0

foreach ($density in $densities) {
    $path = Join-Path $resPath $density
    if (Test-Path $path) {
        $size = (Get-ChildItem -Path $path -Filter "*.png" -File | Measure-Object -Property Length -Sum).Sum
        $totalSize += $size
        Write-Host "  $density : $([math]::Round($size/1MB, 2)) MB" -ForegroundColor White
    }
}

Write-Host ""
Write-Host "Total PNG size: $([math]::Round($totalSize/1MB, 2)) MB" -ForegroundColor Yellow
Write-Host "Potential savings: ~$([math]::Round($totalSize * 0.6 / 1MB, 2)) MB (60% est.)" -ForegroundColor Green
Write-Host ""

# Recommendations
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "       NEXT STEPS                      " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($hasPngquant) {
    Write-Host "READY! Run: .\optimize-icons.ps1" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "Install pngquant first:" -ForegroundColor Yellow
    Write-Host "  choco install pngquant -y" -ForegroundColor White
    Write-Host ""
    Write-Host "Then run: .\optimize-icons.ps1" -ForegroundColor White
    Write-Host ""
    Write-Host "Alternative: Use TinyPNG online" -ForegroundColor Yellow
    Write-Host "  https://tinypng.com/" -ForegroundColor White
    Write-Host ""
}

Write-Host "See ICON_OPTIMIZATION_GUIDE.md for details" -ForegroundColor Cyan
Write-Host ""
