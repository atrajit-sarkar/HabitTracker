# Play Store Pre-Flight Check Script
# Run this before submitting to Play Store

Write-Host "Play Store Pre-Flight Check" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

$issues = @()
$warnings = @()
$passed = @()

# Check 1: Keystore Properties
Write-Host "Checking keystore configuration..." -ForegroundColor Yellow
if (Test-Path "keystore.properties") {
    $keystoreContent = Get-Content "keystore.properties" -Raw
    if ($keystoreContent -match "YOUR_.*_HERE") {
        $issues += "keystore.properties contains template values"
    } else {
        $passed += "keystore.properties exists and appears configured"
    }
} else {
    $issues += "keystore.properties file not found"
}

# Check 2: Keystore File
Write-Host "Checking keystore file..." -ForegroundColor Yellow
if (Test-Path "habit-tracker-release.jks") {
    $passed += "Release keystore file found"
} else {
    $issues += "habit-tracker-release.jks not found"
}

# Check 3: Application ID
Write-Host "Checking application ID..." -ForegroundColor Yellow
$buildGradle = Get-Content "app\build.gradle.kts" -Raw
if ($buildGradle -match 'applicationId = "com\.example\.habittracker"') {
    $warnings += "Application ID is still com.example.habittracker"
} else {
    $passed += "Application ID has been customized"
}

# Check 4: Version Code and Name
Write-Host "Checking version information..." -ForegroundColor Yellow
if ($buildGradle -match 'versionCode = (\d+)') {
    $versionCode = $matches[1]
    $passed += "Version Code: $versionCode"
}
if ($buildGradle -match 'versionName = "([^"]+)"') {
    $versionName = $matches[1]
    $passed += "Version Name: $versionName"
}

# Check 5: Google Services JSON
Write-Host "Checking Firebase configuration..." -ForegroundColor Yellow
if (Test-Path "app\google-services.json") {
    $passed += "google-services.json exists"
} else {
    $issues += "google-services.json not found"
}

# Check 6: Previous builds
Write-Host "Checking for existing builds..." -ForegroundColor Yellow
if (Test-Path "app\build\outputs\bundle\release\app-release.aab") {
    $aabFile = Get-Item "app\build\outputs\bundle\release\app-release.aab"
    $passed += "Previous AAB found"
} else {
    $warnings += "No release AAB found"
}

Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "SUMMARY" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# Display results
if ($passed.Count -gt 0) {
    Write-Host "PASSED:" -ForegroundColor Green
    foreach ($item in $passed) {
        Write-Host "  $item" -ForegroundColor Green
    }
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "WARNINGS:" -ForegroundColor Yellow
    foreach ($item in $warnings) {
        Write-Host "  $item" -ForegroundColor Yellow
    }
    Write-Host ""
}

if ($issues.Count -gt 0) {
    Write-Host "ISSUES:" -ForegroundColor Red
    foreach ($item in $issues) {
        Write-Host "  $item" -ForegroundColor Red
    }
    Write-Host ""
}

Write-Host "=============================================" -ForegroundColor Cyan

if ($issues.Count -eq 0 -and $warnings.Count -eq 0) {
    Write-Host "All checks passed!" -ForegroundColor Green
} elseif ($issues.Count -eq 0) {
    Write-Host "Minor warnings found. Review before submitting." -ForegroundColor Yellow
} else {
    Write-Host "Please fix the issues above." -ForegroundColor Red
}

Write-Host ""
