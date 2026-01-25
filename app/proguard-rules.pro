# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep MainActivity and related activities - these are critical for app launch
-keep class com.mobprog.lokalert.MainActivity { *; }
-keep class com.mobprog.lokalert.AlarmOverlayActivity { *; }
-keep class com.mobprog.lokalert.AlarmActivity { *; }

# Keep services that handle critical functionality
-keep class com.mobprog.lokalert.AppRestartService { *; }
-keep class com.mobprog.lokalert.LocationTrackingService { *; }

# Keep broadcast receivers
-keep class com.mobprog.lokalert.AlarmReceiver { *; }
-keep class com.mobprog.lokalert.SnoozeAlarmReceiver { *; }

# Keep IconManager which handles app icon switching
-keep class com.mobprog.lokalert.IconManager { *; }