@echo off
echo Building Tabs Per Project Plugin...
echo.

REM Clean previous builds
echo Cleaning previous builds...
call gradlew clean

REM Build the plugin
echo Building plugin...
call gradlew buildPlugin

if %ERRORLEVEL% == 0 (
    echo.
    echo Build successful!
    echo Plugin JAR can be found in: build\distributions\
    echo.
    echo To install in your IDE:
    echo 1. Open Settings/Preferences
    echo 2. Go to Plugins
    echo 3. Click the gear icon and select "Install Plugin from Disk..."
    echo 4. Select the ZIP file from build\distributions\
) else (
    echo.
    echo Build failed! Check the error messages above.
)

pause
