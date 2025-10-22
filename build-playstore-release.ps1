# ========================================
# Build Play Store Version (APK)
# ========================================
# This script builds the Play Store flavor WITHOUT in-app update
# 
# ⚠️  IMPORTANT: For Play Store upload, use AAB (not APK)!
#    Run: .\build-playstore-bundle.ps1 instead
# 
# This APK is for local testing only.
# Output: HabitTracker-v7.0.0.apk (clean version for Play Store)
# Location: app/build/outputs/apk/playstore/release/
# ========================================

Write-Host ""
Write-Host "=================================" -ForegroundColor Cyan
Write-Host "  Building Play Store v7.0.0" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "✓ Flavor: playstore" -ForegroundColor Green
Write-Host "✓ Version: 7.0.0" -ForegroundColor Green
Write-Host "✓ In-app updates: DISABLED" -ForegroundColor Green
Write-Host "✓ Update source: Google Play Store" -ForegroundColor Green
Write-Host ""

# Clean previous builds
Write-Host "🧹 Cleaning previous builds..." -ForegroundColor Yellow
.\gradlew.bat clean

# Build Play Store flavor release
Write-Host ""
Write-Host "🔨 Building Play Store release APK..." -ForegroundColor Yellow
.\gradlew.bat assemblePlaystoreRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=================================" -ForegroundColor Green
    Write-Host "  ✅ BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "=================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "📦 Output location:" -ForegroundColor Cyan
    Write-Host "   app\build\outputs\apk\playstore\release\" -ForegroundColor White
    Write-Host ""
    Write-Host "📋 Next steps for Play Store:" -ForegroundColor Yellow
    Write-Host "   1. Test the APK on a device" -ForegroundColor White
    Write-Host "   2. Go to Play Console:" -ForegroundColor White
    Write-Host "      https://play.google.com/console" -ForegroundColor Cyan
    Write-Host "   3. Create a new release (Production/Beta/Alpha)" -ForegroundColor White
    Write-Host "   4. Upload the APK bundle" -ForegroundColor White
    Write-Host "   5. Fill in release notes" -ForegroundColor White
    Write-Host "   6. Submit for review" -ForegroundColor White
    Write-Host ""
    Write-Host "💡 Tip: Play Store handles updates automatically" -ForegroundColor Yellow
    Write-Host "   No GitHub releases needed for this version!" -ForegroundColor Yellow
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
