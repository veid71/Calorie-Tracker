@echo off
REM CalorieTracker Build Script
REM This script ensures reliable builds on Windows

echo Setting up environment...
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo Building CalorieTracker app...
cd /d "%~dp0"
call gradlew.bat clean assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo Build successful!
    echo Copying APK to Downloads...
    copy "app\build\outputs\apk\debug\app-debug.apk" "C:\Users\mattm\Downloads\CalorieTracker-latest.apk"
    if %ERRORLEVEL% EQU 0 (
        echo APK copied successfully to Downloads folder!
    ) else (
        echo Failed to copy APK to Downloads
    )
) else (
    echo Build failed!
)

pause