#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Convert GIF to Lottie animation
.DESCRIPTION
    Converts a GIF file to Lottie JSON format with background removal
.PARAMETER InputFile
    Path to the input GIF file (relative to input folder or absolute path)
.PARAMETER OutputName
    Name for the output JSON file (without extension)
.PARAMETER RemoveBackground
    Whether to remove background (default: true)
.PARAMETER MaxSize
    Maximum dimension in pixels (default: 256)
.PARAMETER TargetFps
    Target frames per second (default: 25)
.EXAMPLE
    .\convert-gif.ps1 -InputFile "my-animation.gif" -OutputName "my_animation"
#>

param(
    [Parameter(Mandatory=$true)]
    [string]$InputFile,
    
    [Parameter(Mandatory=$true)]
    [string]$OutputName,
    
    [bool]$RemoveBackground = $true,
    
    [int]$MaxSize = 256,
    
    [int]$TargetFps = 25
)

$RootDir = Split-Path -Parent $PSScriptRoot
$PythonExe = "$RootDir\.venv\Scripts\python.exe"
$ScriptPath = "$PSScriptRoot\scripts\gif_to_lottie.py"
$OutputDir = "$PSScriptRoot\output"
$InputDir = "$PSScriptRoot\input"

# Check if input file exists
if (Test-Path $InputFile) {
    $InputPath = $InputFile
} elseif (Test-Path "$InputDir\$InputFile") {
    $InputPath = "$InputDir\$InputFile"
} else {
    Write-Host "❌ Error: Input file not found: $InputFile" -ForegroundColor Red
    Write-Host "   Place your GIF files in: $InputDir" -ForegroundColor Yellow
    exit 1
}

$OutputPath = "$OutputDir\$OutputName.json"

Write-Host "🎨 GIF to Lottie Converter" -ForegroundColor Cyan
Write-Host "=" * 60
Write-Host "Input:  $InputPath" -ForegroundColor Green
Write-Host "Output: $OutputPath" -ForegroundColor Green
Write-Host "Settings: MaxSize=$MaxSize, FPS=$TargetFps, RemoveBg=$RemoveBackground"
Write-Host "=" * 60

# Run the converter
& $PythonExe $ScriptPath $InputPath $OutputPath

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Conversion completed!" -ForegroundColor Green
    Write-Host "📁 Output: $OutputPath" -ForegroundColor Cyan
    
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
