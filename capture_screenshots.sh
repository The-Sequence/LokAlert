#!/bin/zsh

# LokAlert Screenshot Capture Script
# This script runs the screenshot tests and copies the results to the screenshots folder

echo "🚀 LokAlert Screenshot Capture Tool"
echo "===================================="

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo "❌ Error: ADB not found. Please install Android SDK platform-tools."
    exit 1
fi

# Check if there's a connected device
DEVICES=$(adb devices | grep -v "List" | grep -v "^$" | wc -l | tr -d ' ')
if [ "$DEVICES" -eq "0" ]; then
    echo "❌ Error: No Android device/emulator connected."
    echo "Please connect a device or start an emulator first."
    exit 1
fi

echo "✓ Found $DEVICES connected device(s)"

# Navigate to project root
cd "$(dirname "$0")"
echo "📂 Working directory: $(pwd)"

# Create screenshots directory if it doesn't exist
mkdir -p screenshots

# Run the screenshot tests
echo ""
echo "📸 Running screenshot capture tests..."
echo "This may take a few minutes..."
echo ""

./gradlew connectedAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.mobprog.lokalert.ScreenshotCaptureTest \
    --stacktrace

# Check if tests passed
if [ $? -ne 0 ]; then
    echo ""
    echo "⚠️  Some tests may have failed. Attempting to pull available screenshots..."
fi

# Wait a moment for file system to sync
sleep 2

# Pull screenshots from device
echo ""
echo "📥 Pulling screenshots from device..."

# Clear old screenshots
rm -rf screenshots/*.png 2>/dev/null

# Pull the screenshots
adb pull /sdcard/Pictures/LokAlert_Screenshots/ screenshots/

# Move files up from subdirectory if needed
if [ -d "screenshots/LokAlert_Screenshots" ]; then
    mv screenshots/LokAlert_Screenshots/*.png screenshots/ 2>/dev/null
    rmdir screenshots/LokAlert_Screenshots 2>/dev/null
fi

# Count screenshots
SCREENSHOT_COUNT=$(ls -1 screenshots/*.png 2>/dev/null | wc -l | tr -d ' ')

echo ""
echo "===================================="
if [ "$SCREENSHOT_COUNT" -gt "0" ]; then
    echo "✅ Success! Captured $SCREENSHOT_COUNT screenshots"
    echo ""
    echo "Screenshots saved to: $(pwd)/screenshots/"
    echo ""
    echo "Screenshot files:"
    ls -la screenshots/*.png 2>/dev/null | awk '{print "  - " $NF}'
else
    echo "❌ No screenshots were captured."
    echo "Please check the test output above for errors."
fi
echo "===================================="
