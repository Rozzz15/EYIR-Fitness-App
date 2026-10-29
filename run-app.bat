@echo off
REM ============================================================
REM  EYIR Fitness App - build, install and launch on emulator
REM  Works from cmd, PowerShell (.\run-app.bat) or double-click
REM ============================================================
setlocal

set "PROJECT=%~dp0"
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
set "APK=%PROJECT%app\build\outputs\apk\debug\app-debug.apk"

echo.
echo [1/4] Building debug APK...
call "%PROJECT%gradlew.bat" assembleDebug --console=plain -q
if errorlevel 1 (
    echo BUILD FAILED - see errors above.
    pause
    exit /b 1
)

echo.
echo [2/4] Checking for emulator/device...
if not exist "%ADB%" (
    echo adb not found at: %ADB%
    echo Make sure Android Studio is installed.
    pause
    exit /b 1
)
"%ADB%" devices | findstr /R "emulator- device$" >nul
if errorlevel 1 (
    echo No device found. Start your emulator from Android Studio first:
    echo   Device Manager ^> play button on a virtual device
    pause
    exit /b 1
)

echo.
echo [3/4] Installing APK...
"%ADB%" install -r "%APK%"
if errorlevel 1 (
    echo INSTALL FAILED.
    pause
    exit /b 1
)

echo.
echo [4/4] Launching app...
"%ADB%" shell am start -n com.example.eyirfitnessapp/.MainActivity

echo.
echo Done! Check your emulator.
timeout /t 3 >nul
endlocal
