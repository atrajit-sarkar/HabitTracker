# Monitor HabitTracker restore operations
Write-Host "🔍 Monitoring HabitTracker restore operations..." -ForegroundColor Cyan
Write-Host "📱 Perform these steps in the app:" -ForegroundColor Yellow
Write-Host "   1. Delete a habit (moves to trash)" -ForegroundColor Yellow
Write-Host "   2. Restore the habit from trash" -ForegroundColor Yellow
Write-Host ""
Write-Host "Press Ctrl+C to stop monitoring" -ForegroundColor Gray
Write-Host "----------------------------------------" -ForegroundColor Gray
Write-Host ""

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Clear logcat first
& $adb logcat -c

# Monitor logs with highlighting
& $adb logcat | ForEach-Object {
    if ($_ -match "🔄.*Restoring") {
        Write-Host $_ -ForegroundColor Magenta
    }
    elseif ($_ -match "✅.*restored.*Firestore") {
        Write-Host $_ -ForegroundColor Green
    }
    elseif ($_ -match "📦.*Fresh habit data") {
        Write-Host $_ -ForegroundColor Cyan
    }
    elseif ($_ -match "⏰.*Alarm rescheduled") {
        Write-Host $_ -ForegroundColor Yellow
    }
    elseif ($_ -match "🔕.*Reminder disabled") {
        Write-Host $_ -ForegroundColor Gray
    }
    elseif ($_ -match "HabitViewModel.*delete|HabitViewModel.*restore|HabitViewModel.*alarm|reminderScheduler") {
        Write-Host $_ -ForegroundColor White
    }
}
