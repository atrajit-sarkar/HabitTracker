# Integrate Sharingan Animations into Habit Tracker App
# PowerShell script to copy animation files to app assets

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  🔥 Mangekyo Sharingan Animation Integration Script 🔥  " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Define paths
$assetsPath = "app/src/main/assets/animations"
$animationFiles = @(
    "mangekyo_itachi.json",
    "mangekyo_sasuke.json",
    "mangekyo_kakashi.json",
    "mangekyo_obito.json",
    "sharingan_animation.json"
)

# Create assets directory if it doesn't exist
Write-Host "📁 Creating assets directory..." -ForegroundColor Yellow
if (!(Test-Path $assetsPath)) {
    New-Item -ItemType Directory -Force -Path $assetsPath | Out-Null
    Write-Host "   ✓ Created: $assetsPath" -ForegroundColor Green
} else {
    Write-Host "   ✓ Directory already exists: $assetsPath" -ForegroundColor Green
}
Write-Host ""

# Copy animation files
Write-Host "📋 Copying animation files..." -ForegroundColor Yellow
$copiedCount = 0
$skippedCount = 0

foreach ($file in $animationFiles) {
    if (Test-Path $file) {
        Copy-Item $file -Destination $assetsPath -Force
        $fileSize = (Get-Item $file).Length / 1KB
        Write-Host "   ✓ Copied: $file ($("{0:N2}" -f $fileSize) KB)" -ForegroundColor Green
        $copiedCount++
    } else {
        Write-Host "   ⚠ Skipped: $file (not found)" -ForegroundColor Yellow
        $skippedCount++
    }
}
Write-Host ""

# Check build.gradle for Lottie dependency
Write-Host "🔍 Checking Lottie dependency..." -ForegroundColor Yellow
$buildGradlePath = "app/build.gradle.kts"
$buildGradleAltPath = "app/build.gradle"

$gradleFile = ""
if (Test-Path $buildGradlePath) {
    $gradleFile = $buildGradlePath
} elseif (Test-Path $buildGradleAltPath) {
    $gradleFile = $buildGradleAltPath
}

if ($gradleFile -ne "") {
    $gradleContent = Get-Content $gradleFile -Raw
    if ($gradleContent -match "com\.airbnb\.android:lottie") {
        Write-Host "   ✓ Lottie dependency found in $gradleFile" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ Lottie dependency NOT found!" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "   Add this to your dependencies block in $gradleFile:" -ForegroundColor Cyan
        Write-Host "   implementation 'com.airbnb.android:lottie:6.1.0'" -ForegroundColor White
    }
} else {
    Write-Host "   ⚠ build.gradle file not found" -ForegroundColor Yellow
}
Write-Host ""

# Summary
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ✅ Integration Complete!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor White
Write-Host "  • Files copied: $copiedCount" -ForegroundColor White
Write-Host "  • Files skipped: $skippedCount" -ForegroundColor White
Write-Host ""

if ($copiedCount -gt 0) {
    Write-Host "🎯 Next Steps:" -ForegroundColor Cyan
    Write-Host "  1. Sync Gradle if you added Lottie dependency" -ForegroundColor White
    Write-Host "  2. Use animations in your XML layouts:" -ForegroundColor White
    Write-Host ""
    Write-Host "     <com.airbnb.lottie.LottieAnimationView" -ForegroundColor Gray
    Write-Host "         android:layout_width=`"100dp`"" -ForegroundColor Gray
    Write-Host "         android:layout_height=`"100dp`"" -ForegroundColor Gray
    Write-Host "         app:lottie_fileName=`"animations/mangekyo_itachi.json`"" -ForegroundColor Gray
    Write-Host "         app:lottie_loop=`"true`"" -ForegroundColor Gray
    Write-Host "         app:lottie_autoPlay=`"true`" />" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  3. See MANGEKYO_SHARINGAN_ANIMATIONS.md for more examples!" -ForegroundColor White
    Write-Host ""
}

Write-Host "Press any key to continue..." -ForegroundColor DarkGray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
