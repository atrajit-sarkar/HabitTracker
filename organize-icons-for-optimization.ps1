# Organize Icons for Batch Optimization
# This script copies all PNG icons to a separate folder for easy batch processing with online tools

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Icon Batch Organization Tool         " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$outputDir = "icons_to_optimize_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
Write-Host "Creating organization folder: $outputDir" -ForegroundColor Yellow
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

$resPath = "app\src\main\res"
$densities = @('mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi')
$totalFiles = 0

Write-Host ""
Write-Host "Copying files..." -ForegroundColor Yellow

foreach ($d in $densities) {
    $sourcePath = Join-Path $resPath "mipmap-$d"
    $destPath = Join-Path $outputDir "mipmap-$d"
    
    if (Test-Path $sourcePath) {
        Write-Host "  Processing mipmap-$d..." -ForegroundColor Cyan
        New-Item -ItemType Directory -Path $destPath -Force | Out-Null
        
        $pngFiles = Get-ChildItem -Path $sourcePath -Filter "*.png" -File
        $fileCount = $pngFiles.Count
        $totalFiles += $fileCount
        
        Copy-Item "$sourcePath\*.png" -Destination $destPath
        
        $size = ($pngFiles | Measure-Object -Property Length -Sum).Sum
        Write-Host "    Copied $fileCount files ($([math]::Round($size/1MB, 2)) MB)" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "         ORGANIZATION COMPLETE         " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Total PNG files: $totalFiles" -ForegroundColor White
Write-Host "Output folder: $outputDir" -ForegroundColor Yellow
Write-Host ""

# Create instructions file
$instructionsFile = Join-Path $outputDir "README.txt"
$instructions = @"
ICON OPTIMIZATION INSTRUCTIONS
================================

You have $totalFiles PNG icon files organized in 5 density folders.

OPTION 1 - TinyPNG (Recommended)
----------------------------------
1. Visit: https://tinypng.com/
2. For each folder (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi):
   - Upload all PNG files from that folder (max 20 at a time)
   - Wait for optimization to complete
   - Click "Download All" to get optimized files
   - Extract the zip file
3. After optimizing all folders, copy the optimized files back to:
   app\src\main\res\mipmap-[density]\

OPTION 2 - Squoosh
-------------------
1. Visit: https://squoosh.app/
2. Process files individually with custom quality settings
3. Recommended: Quality 85-90%

OPTION 3 - Compress PNG
------------------------
1. Visit: https://compresspng.com/
2. Upload and download in batches
3. Replace original files

IMPORTANT NOTES:
----------------
- Keep the same folder structure
- Don't rename files
- Maintain file extensions (.png)
- Test your app after replacing files
- Keep this folder as backup until you verify the app works correctly

EXPECTED RESULTS:
-----------------
- File size reduction: 60-70%
- Total space saved: ~7-8 MB
- Quality: Imperceptible difference
- No changes to dimensions or transparency

Original location: app\src\main\res\mipmap-*\
Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
"@

$instructions | Out-File -FilePath $instructionsFile -Encoding UTF8

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "           NEXT STEPS                  " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Open folder: $outputDir" -ForegroundColor White
Write-Host "2. Read instructions: README.txt" -ForegroundColor White
Write-Host "3. Upload each mipmap-* folder to TinyPNG" -ForegroundColor White
Write-Host "4. Download optimized files" -ForegroundColor White
Write-Host "5. Replace files in: app\src\main\res\mipmap-*\" -ForegroundColor White
Write-Host ""
Write-Host "TinyPNG: https://tinypng.com/" -ForegroundColor Yellow
Write-Host ""
Write-Host "This folder serves as your backup too!" -ForegroundColor Green
Write-Host ""

# Open the folder
Write-Host "Opening folder..." -ForegroundColor Yellow
Start-Process $outputDir

Write-Host "Done!" -ForegroundColor Green
Write-Host ""
