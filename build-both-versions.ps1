# ========================================
# Build BOTH Versions
# ========================================
# This script builds both GitHub and Play Store versions
# Useful when you want to maintain both distribution channels
# ========================================

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  Building BOTH Versions v7.0.0" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

# Clean previous builds
Write-Host "🧹 Cleaning previous builds..." -ForegroundColor Yellow
.\gradlew.bat clean

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "  Building GitHub Version" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

.\gradlew.bat assembleGithubRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ GitHub version built successfully!" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "❌ GitHub version build failed!" -ForegroundColor Red
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "  Building Play Store Version" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

.\gradlew.bat assemblePlaystoreRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Play Store version built successfully!" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "❌ Play Store version build failed!" -ForegroundColor Red
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  ✅ ALL BUILDS SUCCESSFUL!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📦 Build outputs:" -ForegroundColor Cyan
Write-Host ""
Write-Host "   GitHub version (with in-app updates):" -ForegroundColor Yellow
Write-Host "   └─ app\build\outputs\apk\github\release\" -ForegroundColor White
Write-Host "      Version: 7.0.0-github" -ForegroundColor Gray
Write-Host ""
Write-Host "   Play Store version (no in-app updates):" -ForegroundColor Yellow
Write-Host "   └─ app\build\outputs\apk\playstore\release\" -ForegroundColor White
Write-Host "      Version: 7.0.0" -ForegroundColor Gray
Write-Host ""
Write-Host "📋 Distribution guide:" -ForegroundColor Cyan
Write-Host ""
Write-Host "   GitHub Release:" -ForegroundColor Yellow
Write-Host "   • Upload github flavor APK to GitHub releases" -ForegroundColor White
Write-Host "   • Users get automatic update notifications" -ForegroundColor White
Write-Host ""
Write-Host "   Play Store:" -ForegroundColor Yellow
Write-Host "   • Upload playstore flavor APK to Play Console" -ForegroundColor White
Write-Host "   • Google Play handles all updates" -ForegroundColor White
Write-Host ""
