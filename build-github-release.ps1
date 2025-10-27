# ========================================
# Build GitHub Release Version
# ========================================
# This script builds the GitHub flavor with in-app update enabled
# Output: HabitTracker-v7.0.7-github.apk
# Location: app/build/outputs/apk/github/release/
# ========================================

Write-Host ""
Write-Host "=================================" -ForegroundColor Cyan
Write-Host "  Building GitHub Release v7.0.7" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "✓ Flavor: github" -ForegroundColor Green
Write-Host "✓ Version: 7.0.7-github" -ForegroundColor Green
Write-Host "✓ In-app updates: ENABLED" -ForegroundColor Green
Write-Host "✓ Update source: GitHub Releases" -ForegroundColor Green
Write-Host ""

# Clean previous builds
Write-Host "🧹 Cleaning previous builds..." -ForegroundColor Yellow
.\gradlew.bat clean

# Build GitHub flavor release
Write-Host ""
Write-Host "🔨 Building GitHub release APK..." -ForegroundColor Yellow
.\gradlew.bat assembleGithubRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=================================" -ForegroundColor Green
    Write-Host "  ✅ BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "=================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "📦 Output location:" -ForegroundColor Cyan
    Write-Host "   app\build\outputs\apk\github\release\" -ForegroundColor White
    Write-Host ""
    Write-Host "📋 Next steps:" -ForegroundColor Yellow
    Write-Host "   1. Test the APK on a device" -ForegroundColor White
    Write-Host "   2. Create GitHub release at:" -ForegroundColor White
    Write-Host "      https://github.com/atrajit-sarkar/HabitTracker/releases/new" -ForegroundColor Cyan
    Write-Host "   3. Upload APK to the release" -ForegroundColor White
    Write-Host "   4. Users will get in-app update notifications" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "=================================" -ForegroundColor Red
    Write-Host "  ❌ BUILD FAILED!" -ForegroundColor Red
    Write-Host "=================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Check the error messages above for details." -ForegroundColor Yellow
    Write-Host ""
}
