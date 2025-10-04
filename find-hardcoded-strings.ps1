# String Refactoring Helper Script
# This PowerShell script helps identify remaining hardcoded strings in Kotlin files

Write-Host "🔍 Scanning for hardcoded strings in Kotlin files..." -ForegroundColor Cyan
Write-Host ""

$projectPath = "app\src\main\java\com\example\habittracker"

# Check if path exists
if (-not (Test-Path $projectPath)) {
    Write-Host "❌ Error: Project path not found!" -ForegroundColor Red
    Write-Host "   Please run this script from the project root directory" -ForegroundColor Yellow
    exit
}

# Find hardcoded strings
Write-Host "📝 Files with hardcoded strings:" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green
Write-Host ""

$files = Get-ChildItem -Path $projectPath -Filter "*.kt" -Recurse

$foundFiles = @()

foreach ($file in $files) {
    $matches = Select-String -Path $file.FullName -Pattern 'Text\([^)]*"[^"]+"|text\s*=\s*"[^"]+"' -AllMatches
    
    if ($matches) {
        $foundFiles += $file
        Write-Host "📄 $($file.Name)" -ForegroundColor Yellow
        Write-Host "   Path: $($file.FullName.Replace((Get-Location).Path + '\', ''))" -ForegroundColor Gray
        Write-Host "   Matches: $($matches.Count)" -ForegroundColor Cyan
        Write-Host ""
    }
}

Write-Host ""
Write-Host "📊 Summary:" -ForegroundColor Magenta
Write-Host "==========" -ForegroundColor Magenta
Write-Host "Total files scanned: $($files.Count)" -ForegroundColor White
Write-Host "Files with hardcoded strings: $($foundFiles.Count)" -ForegroundColor Yellow
Write-Host ""

if ($foundFiles.Count -gt 0) {
    Write-Host "🎯 Priority files to refactor:" -ForegroundColor Green
    Write-Host ""
    
    $priorityOrder = @(
        "TrashScreen.kt",
        "StatisticsScreen.kt",
        "SearchUsersScreen.kt",
        "LeaderboardScreen.kt",
        "FriendsListScreen.kt",
        "FriendProfileScreen.kt",
        "ChatScreen.kt",
        "ChatListScreen.kt",
        "UpdateDialog.kt",
        "NotificationSetupGuideScreen.kt",
        "ProfileScreen.kt",
        "AuthScreen.kt",
        "HabitDetailsScreen.kt",
        "HomeScreen.kt"
    )
    
    $counter = 1
    foreach ($priorityFile in $priorityOrder) {
        if ($foundFiles.Name -contains $priorityFile) {
            Write-Host "  $counter. $priorityFile" -ForegroundColor Cyan
            $counter++
        }
    }
    
    Write-Host ""
    Write-Host "💡 Tip: Start with TrashScreen.kt - it has clear, simple patterns!" -ForegroundColor Green
} else {
    Write-Host "✅ No hardcoded strings found! All files are refactored! 🎉" -ForegroundColor Green
}

Write-Host ""
Write-Host "📖 See STRING_REFACTORING_GUIDE.md for detailed refactoring instructions" -ForegroundColor Gray
Write-Host ""

# Optional: Show sample hardcoded strings from a specific file
$sampleFile = "TrashScreen.kt"
$sampleFilePath = Get-ChildItem -Path $projectPath -Filter $sampleFile -Recurse | Select-Object -First 1

if ($sampleFilePath) {
    Write-Host ""
    Write-Host "📋 Sample hardcoded strings from $sampleFile" -ForegroundColor Magenta
    Write-Host "===========================================" -ForegroundColor Magenta
    Write-Host ""
    
    $sampleMatches = Select-String -Path $sampleFilePath.FullName -Pattern 'Text\([^)]*"([^"]+)"' -AllMatches | Select-Object -First 10
    
    foreach ($match in $sampleMatches) {
        if ($match.Matches.Groups[1].Value) {
            Write-Host "  Line $($match.LineNumber): ""$($match.Matches.Groups[1].Value)""" -ForegroundColor Yellow
        }
    }
    
    Write-Host ""
    Write-Host "  ... and more (showing first 10)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "✨ Happy refactoring! ✨" -ForegroundColor Green
