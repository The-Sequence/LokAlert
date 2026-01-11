#!/bin/zsh

# LokAlert Real Screenshot Capture Script
# Properly navigates through all onboarding screens including permission dialogs

PACKAGE="com.mobprog.lokalert"
ACTIVITY="com.mobprog.lokalert.MainActivity"
PROJECT_DIR="/Users/apple/AndroidStudioProjects/LokAlert"

# ============================================================================
# UTILITY FUNCTIONS
# ============================================================================

get_device_type() {
    local size=$(adb shell wm size | grep "Physical size" | cut -d: -f2 | tr -d ' ')
    local width=$(echo $size | cut -dx -f1)
    local height=$(echo $size | cut -dx -f2)
    
    if [ "$width" -gt 1800 ] && [ "$height" -gt 1400 ]; then
        echo "tablet"
    else
        echo "phone"
    fi
}

take_screenshot() {
    local name=$1
    local folder=$2
    sleep 1.5
    adb shell screencap -p /sdcard/screenshot.png
    adb pull /sdcard/screenshot.png "$folder/${name}.png" 2>/dev/null
    adb shell rm /sdcard/screenshot.png
    echo "  ✅ Captured: ${name}.png"
}

dump_ui() {
    adb shell uiautomator dump /sdcard/ui.xml 2>/dev/null
    adb shell cat /sdcard/ui.xml 2>/dev/null
}

# Find element bounds by text and tap center
tap_by_text() {
    local text=$1
    local ui=$(dump_ui)
    
    local bounds=$(echo "$ui" | grep -o "text=\"$text\"[^>]*bounds=\"\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]\"" | grep -o "bounds=\"\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]\"" | head -1)
    
    if [ -n "$bounds" ]; then
        local coords=$(echo "$bounds" | sed 's/bounds="//;s/"$//')
        local x1=$(echo "$coords" | sed 's/\[\([0-9]*\).*/\1/')
        local y1=$(echo "$coords" | sed 's/\[[0-9]*,\([0-9]*\).*/\1/')
        local x2=$(echo "$coords" | sed 's/.*\[\([0-9]*\),[0-9]*\]$/\1/')
        local y2=$(echo "$coords" | sed 's/.*,\([0-9]*\)\]$/\1/')
        
        local tap_x=$(( (x1 + x2) / 2 ))
        local tap_y=$(( (y1 + y2) / 2 ))
        
        echo "  → Tap '$text' at ($tap_x, $tap_y)"
        adb shell input tap $tap_x $tap_y
        sleep 1
        return 0
    fi
    echo "  ⚠ '$text' not found"
    return 1
}

# Tap checkbox element
tap_checkbox() {
    local ui=$(dump_ui)
    
    local bounds=$(echo "$ui" | grep -o 'class="android.widget.CheckBox"[^>]*bounds="[^"]*"' | grep -o 'bounds="[^"]*"' | head -1)
    
    if [ -n "$bounds" ]; then
        local coords=$(echo "$bounds" | sed 's/bounds="//;s/"$//')
        local x1=$(echo "$coords" | sed 's/\[\([0-9]*\).*/\1/')
        local y1=$(echo "$coords" | sed 's/\[[0-9]*,\([0-9]*\).*/\1/')
        local x2=$(echo "$coords" | sed 's/.*\[\([0-9]*\),[0-9]*\]$/\1/')
        local y2=$(echo "$coords" | sed 's/.*,\([0-9]*\)\]$/\1/')
        
        local tap_x=$(( (x1 + x2) / 2 ))
        local tap_y=$(( (y1 + y2) / 2 ))
        
        echo "  → Tap checkbox at ($tap_x, $tap_y)"
        adb shell input tap $tap_x $tap_y
        sleep 1
        return 0
    fi
    echo "  ⚠ Checkbox not found"
    return 1
}

# Check if text exists in current UI
has_text() {
    local text=$1
    local ui=$(dump_ui)
    echo "$ui" | grep -q "text=\"$text\""
}

# Handle system permission dialog (Allow/Deny)
handle_permission_dialog() {
    sleep 1.5
    local ui=$(dump_ui)
    
    # Check for system permission dialog - different patterns
    if echo "$ui" | grep -q 'text="Allow"' || echo "$ui" | grep -q 'text="While using the app"' || echo "$ui" | grep -q 'text="Only this time"'; then
        echo "  → System permission dialog detected"
        
        # Try "While using the app" first (for location)
        if tap_by_text "While using the app" 2>/dev/null; then
            sleep 1
            return 0
        fi
        
        # Try "Allow" button
        if tap_by_text "Allow" 2>/dev/null; then
            sleep 1
            return 0
        fi
        
        # Try "ALLOW" (uppercase)
        if tap_by_text "ALLOW" 2>/dev/null; then
            sleep 1
            return 0
        fi
    fi
    return 1
}

# Wait for app to return from settings
wait_for_app() {
    local max_wait=10
    local count=0
    while [ $count -lt $max_wait ]; do
        local ui=$(dump_ui)
        if echo "$ui" | grep -q "package=\"$PACKAGE\""; then
            return 0
        fi
        sleep 1
        count=$((count + 1))
    done
    # Force return to app
    adb shell am start -n "${PACKAGE}/${ACTIVITY}" >/dev/null 2>&1
    sleep 2
}

# ============================================================================
# MAIN SCRIPT
# ============================================================================

echo "🚀 LokAlert Real Screenshot Capture"
echo "===================================="

if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected!"
    exit 1
fi

DEVICE_TYPE=$(get_device_type)
SCREENSHOT_DIR="${PROJECT_DIR}/screenshots/${DEVICE_TYPE}"

echo "📱 Device type: ${DEVICE_TYPE}"
echo "📂 Output folder: ${SCREENSHOT_DIR}"
echo ""

mkdir -p "$SCREENSHOT_DIR"
rm -f "${SCREENSHOT_DIR}/"*.png 2>/dev/null || true

echo "🔄 Fresh installing app..."
cd "$PROJECT_DIR"
./gradlew uninstallAll >/dev/null 2>&1
./gradlew installDebug >/dev/null 2>&1

echo ""
echo "📸 Capturing screenshots..."
echo ""

# ============================================================================
# PAGE 0: SPLASH & PRIVACY TERMS
# ============================================================================
echo "━━━ Page 0: Privacy Terms ━━━"
adb shell am start -n "${PACKAGE}/${ACTIVITY}" >/dev/null 2>&1
sleep 5  # Wait for splash screen to finish

take_screenshot "01_Privacy_Terms" "$SCREENSHOT_DIR"

# Accept terms: tap checkbox then Continue
echo "  Accepting privacy terms..."
tap_checkbox
sleep 1
tap_by_text "Continue"
sleep 2

# ============================================================================
# PAGE 1: WELCOME
# ============================================================================
echo ""
echo "━━━ Page 1: Welcome ━━━"
take_screenshot "02_Welcome" "$SCREENSHOT_DIR"

tap_by_text "Continue"
sleep 2

# ============================================================================
# PAGE 2: NOTIFICATIONS & ALARMS
# ============================================================================
echo ""
echo "━━━ Page 2: Notifications & Alarms ━━━"
take_screenshot "03_Notifications_Before" "$SCREENSHOT_DIR"

# Click Allow for Notifications
echo "  Requesting notification permission..."
tap_by_text "Allow"
sleep 1.5
handle_permission_dialog
sleep 1

# Check if there's another Allow button (for Exact Alarms)
local ui=$(dump_ui)
if echo "$ui" | grep -q "Exact Alarms"; then
    echo "  Requesting exact alarms permission..."
    tap_by_text "Allow"
    sleep 2
    # This opens settings, need to enable and go back
    # Look for toggle or switch
    ui=$(dump_ui)
    if echo "$ui" | grep -q "$PACKAGE"; then
        # In alarms settings, look for toggle
        tap_by_text "Allow setting alarms and reminders" 2>/dev/null || \
        tap_by_text "Alarms & reminders" 2>/dev/null
        sleep 1
    fi
    # Go back to app
    adb shell input keyevent KEYCODE_BACK
    sleep 1
    adb shell input keyevent KEYCODE_BACK
    sleep 2
fi

wait_for_app
take_screenshot "04_Notifications_After" "$SCREENSHOT_DIR"

tap_by_text "Continue"
sleep 2

# ============================================================================
# PAGE 3: LOCATION
# ============================================================================
echo ""
echo "━━━ Page 3: Location ━━━"
take_screenshot "05_Location_Before" "$SCREENSHOT_DIR"

echo "  Requesting location permission..."
tap_by_text "Allow"
sleep 1.5
handle_permission_dialog
sleep 1

take_screenshot "06_Location_After" "$SCREENSHOT_DIR"

tap_by_text "Continue"
sleep 2

# ============================================================================
# PAGE 4: OVERLAY
# ============================================================================
echo ""
echo "━━━ Page 4: Overlay ━━━"
take_screenshot "07_Overlay_Before" "$SCREENSHOT_DIR"

echo "  Requesting overlay permission..."
tap_by_text "Allow"
sleep 2

# This opens system settings - need to find and toggle the switch
ui=$(dump_ui)
if echo "$ui" | grep -q "Display over other apps\|draw over\|appear on top"; then
    echo "  In overlay settings, enabling..."
    # Look for the app in the list or a toggle
    tap_by_text "$PACKAGE" 2>/dev/null || \
    tap_by_text "LokAlert" 2>/dev/null
    sleep 1
    # Toggle the switch
    tap_by_text "Allow display over other apps" 2>/dev/null
    sleep 1
fi
adb shell input keyevent KEYCODE_BACK
sleep 1
adb shell input keyevent KEYCODE_BACK
sleep 2

wait_for_app
take_screenshot "08_Overlay_After" "$SCREENSHOT_DIR"

tap_by_text "Continue"
sleep 2

# ============================================================================
# PAGE 5: BATTERY
# ============================================================================
echo ""
echo "━━━ Page 5: Battery ━━━"
take_screenshot "09_Battery_Before" "$SCREENSHOT_DIR"

echo "  Requesting battery optimization exemption..."
tap_by_text "Allow"
sleep 2

# Handle battery optimization dialog
ui=$(dump_ui)
if echo "$ui" | grep -q "battery\|Battery\|optimization"; then
    tap_by_text "Allow" 2>/dev/null || \
    tap_by_text "ALLOW" 2>/dev/null
    sleep 1
fi

wait_for_app
take_screenshot "10_Battery_After" "$SCREENSHOT_DIR"

tap_by_text "Continue" 2>/dev/null || tap_by_text "Get Started" 2>/dev/null
sleep 2

# ============================================================================
# CELEBRATION & MAIN APP
# ============================================================================
echo ""
echo "━━━ Celebration & Main App ━━━"

# Check if we're on celebration screen
ui=$(dump_ui)
if echo "$ui" | grep -q "You're All Set\|Ready\|Let's Go\|celebration"; then
    take_screenshot "11_Celebration" "$SCREENSHOT_DIR"
    tap_by_text "Let's Go" 2>/dev/null || \
    tap_by_text "Get Started" 2>/dev/null || \
    tap_by_text "Continue" 2>/dev/null
    sleep 3
fi

# Main Maps Screen
take_screenshot "12_Maps_Screen" "$SCREENSHOT_DIR"

# Navigate to Locations tab
echo ""
echo "━━━ Locations Screen ━━━"
tap_by_text "Locations"
sleep 2
take_screenshot "13_Locations_Screen" "$SCREENSHOT_DIR"

# Try to access Settings
echo ""
echo "━━━ Settings Screen ━━━"
# Look for menu icon (three dots)
ui=$(dump_ui)
if echo "$ui" | grep -q 'content-desc="Options"'; then
    bounds=$(echo "$ui" | grep -o 'content-desc="Options"[^>]*bounds="[^"]*"' | grep -o 'bounds="[^"]*"' | head -1)
    if [ -n "$bounds" ]; then
        coords=$(echo "$bounds" | sed 's/bounds="//;s/"$//')
        x1=$(echo "$coords" | sed 's/\[\([0-9]*\).*/\1/')
        y1=$(echo "$coords" | sed 's/\[[0-9]*,\([0-9]*\).*/\1/')
        x2=$(echo "$coords" | sed 's/.*\[\([0-9]*\),[0-9]*\]$/\1/')
        y2=$(echo "$coords" | sed 's/.*,\([0-9]*\)\]$/\1/')
        tap_x=$(( (x1 + x2) / 2 ))
        tap_y=$(( (y1 + y2) / 2 ))
        echo "  → Tap Options menu at ($tap_x, $tap_y)"
        adb shell input tap $tap_x $tap_y
        sleep 1
        tap_by_text "Settings"
        sleep 2
        take_screenshot "14_Settings_Screen" "$SCREENSHOT_DIR"
    fi
fi

# ============================================================================
# DONE
# ============================================================================
echo ""
echo "===================================="
echo "✅ Screenshots captured for ${DEVICE_TYPE}!"
echo ""
echo "Files in: $SCREENSHOT_DIR"
ls -1 "$SCREENSHOT_DIR"/*.png 2>/dev/null
echo ""
echo "Total: $(ls -1 "$SCREENSHOT_DIR"/*.png 2>/dev/null | wc -l | tr -d ' ') screenshots"
echo "===================================="
