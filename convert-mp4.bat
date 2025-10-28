@echo off
REM Quick MP4 to Lottie Converter
REM Usage: convert-mp4.bat <input-file> <output-name> [fps] [speed]

if "%1"=="" (
    echo Usage: convert-mp4.bat ^<input-file^> ^<output-name^> [fps] [speed]
    echo.
    echo Examples:
    echo   convert-mp4.bat my-video.mp4 my_animation
    echo   convert-mp4.bat my-video.mp4 my_animation 15 1.0
    echo.
    exit /b 1
)

set INPUT=%1
set OUTPUT=%2
set FPS=%3
set SPEED=%4

if "%FPS%"=="" set FPS=12
if "%SPEED%"=="" set SPEED=0.75

echo Converting %INPUT% to %OUTPUT%.json...
echo Settings: FPS=%FPS%, Speed=%SPEED%x
echo.

powershell -ExecutionPolicy Bypass -Command "& '%~dp0animation-tools\convert-mp4.ps1' -InputFile '%INPUT%' -OutputName '%OUTPUT%' -TargetFps %FPS% -Speed %SPEED% -BackgroundMethod ai"
