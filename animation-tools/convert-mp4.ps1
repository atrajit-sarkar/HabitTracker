#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Convert MP4 to Lottie animation
.DESCRIPTION
    Converts an MP4 video to Lottie JSON format with AI-based background removal
.PARAMETER InputFile
    Path to the input MP4 file (relative to input folder or absolute path)
.PARAMETER OutputName
    Name for the output JSON file (without extension)
.PARAMETER RemoveBackground
    Whether to remove background (default: true)
.PARAMETER BackgroundMethod
    Background removal method: 'ai', 'simple', or 'none' (default: ai)
.PARAMETER MaxSize
    Maximum dimension in pixels (default: 256)
.PARAMETER TargetFps
    Target frames per second (default: 12)
.PARAMETER Speed
    Animation speed multiplier in Kotlin code (default: 0.75)
.EXAMPLE
    .\convert-mp4.ps1 -InputFile "my-video.mp4" -OutputName "my_animation"
.EXAMPLE
    .\convert-mp4.ps1 -InputFile "dance.mp4" -OutputName "dance_anim" -TargetFps 15 -Speed 1.0
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$InputFile,
    
    [Parameter(Mandatory=$true)]
    [string]$OutputName,
    
    [bool]$RemoveBackground = $true,
    
    [ValidateSet('ai', 'simple', 'none')]
    [string]$BackgroundMethod = 'ai',
    
    [int]$MaxSize = 256,
    
    [int]$TargetFps = 12,
    
    [double]$Speed = 0.75,
    
    [bool]$SkipDuplicates = $true
)

$RootDir = Split-Path -Parent $PSScriptRoot
$PythonExe = "$RootDir\.venv\Scripts\python.exe"
$OutputDir = "$PSScriptRoot\output"
$InputDir = "$PSScriptRoot\input"

# Check if input file exists
if (Test-Path $InputFile) {
    $InputPath = $InputFile
} elseif (Test-Path "$InputDir\$InputFile") {
    $InputPath = "$InputDir\$InputFile"
} else {
    Write-Host "❌ Error: Input file not found: $InputFile" -ForegroundColor Red
    Write-Host "   Place your MP4 files in: $InputDir" -ForegroundColor Yellow
    exit 1
}

$OutputPath = "$OutputDir\$OutputName.json"

Write-Host "🎬 MP4 to Lottie Converter" -ForegroundColor Cyan
Write-Host "=" * 60
Write-Host "Input:  $InputPath" -ForegroundColor Green
Write-Host "Output: $OutputPath" -ForegroundColor Green
Write-Host "Settings:" -ForegroundColor Yellow
Write-Host "  - Max Size: $MaxSize px"
Write-Host "  - Target FPS: $TargetFps"
Write-Host "  - Background Removal: $BackgroundMethod"
Write-Host "  - Remove BG: $RemoveBackground"
Write-Host "  - Skip Duplicates: $SkipDuplicates"
Write-Host "  - Speed (for Kotlin): ${Speed}x"
Write-Host "=" * 60

# Build Python command - use forward slashes for Python
$ScriptsDir = "$PSScriptRoot\scripts" -replace '\\', '/'
$InputPathPy = $InputPath -replace '\\', '/'
$OutputPathPy = $OutputPath -replace '\\', '/'
$RemoveBgPy = if ($RemoveBackground) { "True" } else { "False" }
$DuplicateThreshold = if ($SkipDuplicates) { "0.02" } else { "0" }

$PythonCmd = @"
import sys
sys.path.insert(0, '$ScriptsDir')
from mp4_to_lottie import convert_mp4_to_lottie
convert_mp4_to_lottie(
    '$InputPathPy', 
    '$OutputPathPy', 
    remove_bg=$RemoveBgPy, 
    max_size=$MaxSize, 
    target_fps=$TargetFps, 
    skip_frames=1, 
    duplicate_threshold=$DuplicateThreshold, 
    bg_method='$BackgroundMethod'
)
"@

# Run the converter
& $PythonExe -c $PythonCmd

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Conversion completed!" -ForegroundColor Green
    Write-Host "📁 Output: $OutputPath" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "📝 Recommended Kotlin settings:" -ForegroundColor Yellow
    Write-Host "   speed = ${Speed}f" -ForegroundColor White
    
    # Ask if user wants to copy to assets
    $CopyToAssets = Read-Host "`nCopy to app assets? (y/n)"
    if ($CopyToAssets -eq "y") {
        $AssetsPath = "$RootDir\app\src\main\assets\$OutputName.json"
        Copy-Item -Path $OutputPath -Destination $AssetsPath -Force
        Write-Host "✅ Copied to: $AssetsPath" -ForegroundColor Green
    }
} else {
    Write-Host "❌ Conversion failed!" -ForegroundColor Red
    exit 1
}
