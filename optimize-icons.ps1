# Icon Optimization Script for HabitTracker
# Optimizes PNG icons to reduce app size while maintaining quality

Write-Host "========================================"  -ForegroundColor Cyan
Write-Host "  HabitTracker Icon Optimization Tool  " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check for pngquant (system or local)
$pngquantPath = Get-Command pngquant -ErrorAction SilentlyContinue
$localPngquant = ".\pngquant\pngquant\pngquant.exe"

if ($pngquantPath) {
    $pngquantExe = "pngquant"
    Write-Host "[OK] Using system pngquant" -ForegroundColor Green
} elseif (Test-Path $localPngquant) {
    $pngquantExe = $localPngquant
    Write-Host "[OK] Using local pngquant: $localPngquant" -ForegroundColor Green
} else {
    Write-Host "[ERROR] pngquant not found!" -ForegroundColor Red
    Write-Host "Please run the download script or install manually." -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Get version
$version = & $pngquantExe --version 2>&1
Write-Host "pngquant version: $version" -ForegroundColor Cyan
Write-Host ""

$resPath = "app\src\main\res"
$densities = @('mipmap-mdpi', 'mipmap-hdpi', 'mipmap-xhdpi', 'mipmap-xxhdpi', 'mipmap-xxxhdpi')

# Calculate initial size
Write-Host "Calculating initial size..." -ForegroundColor Yellow
$initialSize = 0
foreach ($density in $densities) {
    $path = Join-Path $resPath $density
    if (Test-Path $path) {
        $files = Get-ChildItem -Path $path -Filter "*.png" -File
        $size = ($files | Measure-Object -Property Length -Sum).Sum
        $initialSize += $size
    }
}

Write-Host "Initial total size: $([math]::Round($initialSize/1MB, 2)) MB" -ForegroundColor Cyan
Write-Host ""

# Create backup
$backupPath = "icon_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
Write-Host "Creating backup: $backupPath" -ForegroundColor Yellow
New-Item -ItemType Directory -Path $backupPath -Force | Out-Null

foreach ($density in $densities) {
    $sourcePath = Join-Path $resPath $density
    $backupDensityPath = Join-Path $backupPath $density
    if (Test-Path $sourcePath) {
        Copy-Item -Path $sourcePath -Destination $backupDensityPath -Recurse -Force
    }
}

Write-Host "Backup completed!" -ForegroundColor Green
Write-Host ""

# Optimize icons
Write-Host "Optimizing icons..." -ForegroundColor Yellow
Write-Host ""
$optimizedCount = 0
$skippedCount = 0
$errorCount = 0

foreach ($density in $densities) {
    $path = Join-Path $resPath $density
    if (-not (Test-Path $path)) {
        continue
    }
    
    Write-Host "Processing $density..." -ForegroundColor Cyan
    $pngFiles = Get-ChildItem -Path $path -Filter "*.png" -File
    
    foreach ($file in $pngFiles) {
        $tempOutput = "$($file.FullName).temp.png"
        
        try {
            # Run pngquant
            $result = & $pngquantExe --quality=85-100 --skip-if-larger --strip --speed 1 --force --output $tempOutput $file.FullName 2>&1
            
            if (Test-Path $tempOutput) {
                $originalSize = $file.Length
                $newSize = (Get-Item $tempOutput).Length
                
                if ($newSize -lt $originalSize) {
                    Move-Item -Path $tempOutput -Destination $file.FullName -Force
                    $saved = $originalSize - $newSize
                    Write-Host "  [OK] $($file.Name): Saved $([math]::Round($saved/1KB, 1)) KB" -ForegroundColor Green
                    $optimizedCount++
                } else {
                    Remove-Item -Path $tempOutput -Force -ErrorAction SilentlyContinue
                    Write-Host "  [--] $($file.Name): Already optimal" -ForegroundColor Gray
                    $skippedCount++
                }
            } else {
                Write-Host "  [--] $($file.Name): Skipped (no improvement)" -ForegroundColor Gray
                $skippedCount++
            }
        } catch {
            Write-Host "  [ERR] $($file.Name): $($_.Exception.Message)" -ForegroundColor Red
            $errorCount++
            if (Test-Path $tempOutput) {
                Remove-Item -Path $tempOutput -Force -ErrorAction SilentlyContinue
            }
        }
    }
    Write-Host ""
}

# Calculate final size
Write-Host "Calculating final size..." -ForegroundColor Yellow
$finalSize = 0
foreach ($density in $densities) {
    $path = Join-Path $resPath $density
    if (Test-Path $path) {
        $files = Get-ChildItem -Path $path -Filter "*.png" -File
        $size = ($files | Measure-Object -Property Length -Sum).Sum
        $finalSize += $size
    }
}

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "        OPTIMIZATION COMPLETE          " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Initial size:    $([math]::Round($initialSize/1MB, 2)) MB" -ForegroundColor White
Write-Host "Final size:      $([math]::Round($finalSize/1MB, 2)) MB" -ForegroundColor White
Write-Host "Space saved:     $([math]::Round(($initialSize - $finalSize)/1MB, 2)) MB" -ForegroundColor Green
Write-Host "Reduction:       $([math]::Round((($initialSize - $finalSize) / $initialSize) * 100, 1))%" -ForegroundColor Green
Write-Host ""
Write-Host "Files optimized: $optimizedCount" -ForegroundColor White
Write-Host "Files skipped:   $skippedCount" -ForegroundColor White
Write-Host "Errors:          $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "White" })
Write-Host ""
Write-Host "Backup location: $backupPath" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($errorCount -eq 0) {
    Write-Host "SUCCESS! Optimization completed!" -ForegroundColor Green
} else {
    Write-Host "Completed with some errors." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Size breakdown by folder:" -ForegroundColor Cyan
foreach ($density in $densities) {
    $path = Join-Path $resPath $density
    if (Test-Path $path) {
        $files = Get-ChildItem -Path $path -Filter "*.png" -File
        $size = ($files | Measure-Object -Property Length -Sum).Sum
        Write-Host "  $density : $([math]::Round($size/1MB, 2)) MB" -ForegroundColor White
    }
}
Write-Host ""
Write-Host "To restore backup: Copy-Item $backupPath\mipmap-* -Destination app\src\main\res\ -Recurse -Force" -ForegroundColor Yellow
Write-Host ""
