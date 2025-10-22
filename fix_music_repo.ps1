# Fix music.json in GitHub repository by removing old custom_music entries

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Cleaning music.json in GitHub Repository" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Read GitHub token from keystore.properties
$keystoreFile = "keystore.properties"
if (-not (Test-Path $keystoreFile)) {
    Write-Host "ERROR: keystore.properties not found!" -ForegroundColor Red
    exit 1
}

$token = ""
Get-Content $keystoreFile | ForEach-Object {
    if ($_ -match "GITHUB_TOKEN_MUSIC_REPO\s*=\s*(.+)") {
        $token = $matches[1].Trim()
    }
}

if ([string]::IsNullOrEmpty($token)) {
    Write-Host "ERROR: GITHUB_TOKEN_MUSIC_REPO not found in keystore.properties!" -ForegroundColor Red
    exit 1
}

Write-Host "✓ GitHub token found" -ForegroundColor Green

# GitHub API settings
$owner = "gongobongofounder"
$repo = "HabitTracker-Music"
$branch = "main"
$filePath = "music.json"

# Headers for GitHub API
$headers = @{
    "Authorization" = "token $token"
    "Accept" = "application/vnd.github.v3+json"
}

Write-Host "Fetching current music.json..." -ForegroundColor Yellow

# Get current file content and SHA
$getUrl = "https://api.github.com/repos/$owner/$repo/contents/$filePath"
try {
    $response = Invoke-RestMethod -Uri $getUrl -Headers $headers -Method Get
    $currentSha = $response.sha
    
    # Decode content
    $contentBytes = [System.Convert]::FromBase64String($response.content)
    $currentJson = [System.Text.Encoding]::UTF8.GetString($contentBytes)
    $musicData = $currentJson | ConvertFrom-Json
    
    Write-Host "✓ Current songs: $($musicData.music.Count)" -ForegroundColor Green
    
    # Filter out old custom_music entries
    $originalCount = $musicData.music.Count
    $cleanedSongs = @()
    $removedSongs = @()
    
    foreach ($song in $musicData.music) {
        if ($song.url -like "*custom_music/*") {
            $removedSongs += $song
            Write-Host "  ✗ Removing: $($song.title)" -ForegroundColor Red
            Write-Host "    URL: $($song.url)" -ForegroundColor Gray
        } else {
            $cleanedSongs += $song
        }
    }
    
    if ($removedSongs.Count -eq 0) {
        Write-Host "`n`✓ No old entries found! music.json is clean." -ForegroundColor Green
        exit 0
    }
    
    # Update music data
    $musicData.music = $cleanedSongs
    $musicData.lastUpdated = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    
    Write-Host "`nRemoving $($removedSongs.Count) old entry(ies)..." -ForegroundColor Yellow
    
    # Convert back to JSON
    $updatedJson = $musicData | ConvertTo-Json -Depth 10 -Compress:$false
    
    # Encode to Base64
    $updatedBytes = [System.Text.Encoding]::UTF8.GetBytes($updatedJson)
    $base64Content = [System.Convert]::ToBase64String($updatedBytes)
    
    # Prepare update request
    $updateBody = @{
        message = "Remove old custom_music references - $($removedSongs.Count) song(s)"
        content = $base64Content
        sha = $currentSha
        branch = $branch
    } | ConvertTo-Json
    
    # Update file on GitHub
    Write-Host "Updating music.json on GitHub..." -ForegroundColor Yellow
    $updateResponse = Invoke-RestMethod -Uri $getUrl -Headers $headers -Method Put -Body $updateBody -ContentType "application/json"
    
    Write-Host "`n`========================================" -ForegroundColor Green
    Write-Host "✓ SUCCESS!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Removed: $($removedSongs.Count) song(s)" -ForegroundColor Yellow
    Write-Host "Remaining: $($cleanedSongs.Count) song(s)" -ForegroundColor Cyan
    Write-Host "`n`Removed songs:" -ForegroundColor Gray
    foreach ($song in $removedSongs) {
        Write-Host "  • $($song.title) (ID: $($song.id))" -ForegroundColor Gray
    }
    Write-Host "Commit: $($updateResponse.commit.sha.Substring(0,7))" -ForegroundColor Cyan
    Write-Host "`n`✓ music.json cleaned successfully!" -ForegroundColor Green
    
 } catch {
    Write-Host "`n`ERROR: Failed to update music.json" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
    exit 1
}
