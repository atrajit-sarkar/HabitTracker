# Get SHA-1 Fingerprint for Firebase
# Run this in PowerShell to get your debug SHA-1 fingerprint

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Getting SHA-1 Fingerprint for Firebase" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$debugKeystore = "$env:USERPROFILE\.android\debug.keystore"

if (Test-Path $debugKeystore) {
    Write-Host "Found debug keystore at: $debugKeystore" -ForegroundColor Green
    Write-Host ""
    Write-Host "Running keytool..." -ForegroundColor Yellow
    Write-Host ""
    
    keytool -list -v -keystore "$debugKeystore" -alias androiddebugkey -storepass android -keypass android | Select-String -Pattern "SHA1:"
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Instructions:" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "1. Copy the SHA1 fingerprint above (without 'SHA1:' prefix)" -ForegroundColor White
    Write-Host "2. Go to Firebase Console: https://console.firebase.google.com" -ForegroundColor White
    Write-Host "3. Select your project: habit-tracker-56079" -ForegroundColor White
    Write-Host "4. Go to Project Settings (gear icon)" -ForegroundColor White
    Write-Host "5. Scroll to 'Your apps' section" -ForegroundColor White
    Write-Host "6. Find your Android app (com.example.habittracker)" -ForegroundColor White
    Write-Host "7. Click 'Add fingerprint'" -ForegroundColor White
    Write-Host "8. Paste the SHA-1 fingerprint" -ForegroundColor White
    Write-Host "9. Click Save" -ForegroundColor White
    Write-Host "10. Download new google-services.json if prompted" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host "Debug keystore not found at: $debugKeystore" -ForegroundColor Red
    Write-Host "This is unusual. Check if Android Studio is properly installed." -ForegroundColor Yellow
}

Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
