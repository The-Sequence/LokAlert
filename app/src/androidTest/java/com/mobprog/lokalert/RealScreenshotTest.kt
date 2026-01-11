package com.mobprog.lokalert

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Real screenshot capture test using UiAutomator.
 * This captures actual app screens, not mock previews.
 * 
 * Prerequisites:
 * - App must be installed on the device
 * - For onboarding screens: Clear app data first
 * 
 * Run with:
 * ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.mobprog.lokalert.RealScreenshotTest
 */
@RunWith(AndroidJUnit4::class)
class RealScreenshotTest {

    private lateinit var device: UiDevice
    private lateinit var screenshotsDir: File
    private val packageName = "com.mobprog.lokalert"
    private val timeout = 5000L

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Create screenshots directory
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        screenshotsDir = File(picturesDir, "LokAlert_Screenshots")
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs()
        }
    }

    private fun takeScreenshot(name: String) {
        Thread.sleep(1000) // Wait for UI to settle
        val file = File(screenshotsDir, "${name}.png")
        device.takeScreenshot(file)
        println("Screenshot saved: ${file.absolutePath}")
    }

    private fun launchApp() {
        // Start from home screen
        device.pressHome()
        Thread.sleep(500)

        // Launch the app
        val context = InstrumentationRegistry.getInstrumentation().context
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)

        // Wait for app to launch
        device.wait(Until.hasObject(By.pkg(packageName).depth(0)), timeout)
        Thread.sleep(2000) // Extra wait for animations
    }

    @Test
    fun captureAllScreens() {
        launchApp()
        
        // Check if we're on onboarding or main screen
        Thread.sleep(2000)
        
        // Try to detect onboarding by looking for "Next" or "Get Started" button
        val nextButton = device.findObject(By.text("Next"))
        val getStartedButton = device.findObject(By.text("Get Started"))
        
        if (nextButton != null || getStartedButton != null) {
            // We're on onboarding - capture those screens
            captureOnboardingScreens()
        }
        
        // Now capture main app screens
        captureMainScreens()
    }

    private fun captureOnboardingScreens() {
        // Page 1: Welcome
        takeScreenshot("01_Onboarding_Welcome")
        
        // Click Next to go to page 2
        val nextButton1 = device.findObject(By.text("Next"))
        nextButton1?.click()
        Thread.sleep(1000)
        
        // Page 2: Notifications
        takeScreenshot("02_Onboarding_Notifications")
        
        // Click Next to go to page 3
        val nextButton2 = device.findObject(By.text("Next"))
        nextButton2?.click()
        Thread.sleep(1000)
        
        // Page 3: Location
        takeScreenshot("03_Onboarding_Location")
        
        // Click Get Started to complete onboarding
        val getStartedButton = device.findObject(By.text("Get Started"))
        getStartedButton?.click()
        Thread.sleep(2000)
    }

    private fun captureMainScreens() {
        // Wait for main screen to load
        Thread.sleep(2000)
        
        // Capture Maps Screen (should be default)
        takeScreenshot("04_Maps_Screen")
        
        // Navigate to Locations tab
        val locationsTab = device.findObject(By.text("Locations"))
        locationsTab?.click()
        Thread.sleep(1500)
        
        // Capture Locations Screen
        takeScreenshot("05_Locations_Screen")
        
        // Go back to Maps
        val mapsTab = device.findObject(By.text("Map"))
        mapsTab?.click()
        Thread.sleep(1000)
        
        // Open Settings via menu
        val menuButton = device.findObject(By.desc("Options"))
        if (menuButton != null) {
            menuButton.click()
            Thread.sleep(500)
            
            val settingsOption = device.findObject(By.text("Settings"))
            settingsOption?.click()
            Thread.sleep(1000)
            
            // Capture Settings Screen
            takeScreenshot("06_Settings_Screen")
            
            // Go back
            device.pressBack()
            Thread.sleep(500)
        }
    }
}
