# ========================================
# Build Play Store App Bundle (AAB)
# ========================================
# This script builds the Android App Bundle for Play Store
# AAB format is REQUIRED by Google Play Store (not APK)
# Output: HabitTracker-v7.0.0.aab
# Location: app/build/outputs/bundle/playstoreRelease/
# ========================================

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Building Play Store Bundle v7.0.0" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "✓ Format: Android App Bundle (.aab)" -ForegroundColor Green
Write-Host "✓ Flavor: playstore" -ForegroundColor Green
Write-Host "✓ Version: 7.0.0" -ForegroundColor Green
Write-Host "✓ In-app updates: DISABLED" -ForegroundColor Green
Write-Host "✓ Permissions: Play Store compliant" -ForegroundColor Green
Write-Host ""
Write-Host "📦 This AAB file is what you upload to Play Console!" -ForegroundColor Yellow
Write-Host ""

# Clean previous builds
Write-Host "🧹 Cleaning previous builds..." -ForegroundColor Yellow
.\gradlew.bat clean

# Build Play Store bundle (AAB)
Write-Host ""
Write-Host "🔨 Building Android App Bundle (.aab)..." -ForegroundColor Yellow
Write-Host "   This may take 1-2 minutes..." -ForegroundColor Gray
Write-Host ""
.\gradlew.bat bundlePlaystoreRelease

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  ✅ BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "📦 Bundle location:" -ForegroundColor Cyan
    Write-Host "   app\build\outputs\bundle\playstoreRelease\" -ForegroundColor White
    Write-Host ""
    Write-Host "📝 Bundle filename:" -ForegroundColor Cyan
    Write-Host "   app-playstore-release.aab" -ForegroundColor White
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host "  📋 NEXT STEPS FOR PLAY STORE UPLOAD" -ForegroundColor Cyan
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. 🧪 Test the bundle locally (optional):" -ForegroundColor Yellow
    Write-Host "   Use bundletool to test:" -ForegroundColor Gray
    Write-Host "   https://developer.android.com/tools/bundletool" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "2. 📸 Prepare Play Store assets:" -ForegroundColor Yellow
    Write-Host "   • Screenshots (phone, tablet, TV if applicable)" -ForegroundColor White
    Write-Host "   • Feature graphic (1024 x 500 px)" -ForegroundColor White
    Write-Host "   • App icon (512 x 512 px)" -ForegroundColor White
    Write-Host "   • Description, what's new, etc." -ForegroundColor White
    Write-Host ""
    Write-Host "3. 🌐 Go to Play Console:" -ForegroundColor Yellow
    Write-Host "   https://play.google.com/console" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "4. 📤 Upload this AAB file:" -ForegroundColor Yellow
    Write-Host "   app\build\outputs\bundle\playstoreRelease\app-playstore-release.aab" -ForegroundColor White
    Write-Host ""
    Write-Host "5. 📋 Follow the complete guide:" -ForegroundColor Yellow
    Write-Host "   See: PLAY_STORE_UPLOAD_GUIDE.md" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
    Write-Host ""
    Write-Host "💡 Important Notes:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "   ✅ AAB format is REQUIRED by Play Store" -ForegroundColor Green
    Write-Host "   ✅ This build has NO GitHub update system" -ForegroundColor Green
    Write-Host "   ✅ All permissions are Play Store compliant" -ForegroundColor Green
    Write-Host "   ✅ Bundle is signed and ready to upload" -ForegroundColor Green
    Write-Host ""
    Write-Host "   ⚠️  Keep this AAB file safe for future reference" -ForegroundColor Yellow
    Write-Host "   ⚠️  First upload may take 24-48 hours for review" -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "  ❌ BUILD FAILED!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Check the error messages above." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Common issues:" -ForegroundColor Yellow
    Write-Host "  • Missing keystore.properties file" -ForegroundColor White
    Write-Host "  • Incorrect signing configuration" -ForegroundColor White
    Write-Host "  • Build errors in code" -ForegroundColor White
    Write-Host ""
    Write-Host "💡 Try: .\gradlew.bat bundlePlaystoreRelease --stacktrace" -ForegroundColor Cyan
    Write-Host ""
}
