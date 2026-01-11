package com.mobprog.lokalert

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Manages dynamic app icon switching between Material3 and iOS6 skeuomorphic styles.
 * Uses activity-alias to switch the launcher icon when the user changes design language.
 */
object IconManager {
    
    private const val MAIN_ACTIVITY = "com.mobprog.lokalert.MainActivity"
    private const val MATERIAL_ALIAS = "com.mobprog.lokalert.MainActivityMaterial"
    private const val IOS6_ALIAS = "com.mobprog.lokalert.MainActivityiOS6"
    
    /**
     * Switches the app icon based on the design language.
     * @param context Application context
     * @param designLanguage 0 for Material3, 1 for iOS6
     */
    fun setAppIcon(context: Context, designLanguage: Int) {
        val packageManager = context.packageManager
        
        // Determine which components to enable/disable
        val useIOS6 = designLanguage == 1
        
        try {
            // Disable all launcher activities first
            setComponentEnabled(packageManager, context.packageName, MAIN_ACTIVITY, false)
            setComponentEnabled(packageManager, context.packageName, MATERIAL_ALIAS, false)
            setComponentEnabled(packageManager, context.packageName, IOS6_ALIAS, false)
            
            // Enable the appropriate alias based on design language
            if (useIOS6) {
                setComponentEnabled(packageManager, context.packageName, IOS6_ALIAS, true)
            } else {
                setComponentEnabled(packageManager, context.packageName, MATERIAL_ALIAS, true)
            }
        } catch (e: Exception) {
            // If something goes wrong, ensure the main activity is enabled
            try {
                setComponentEnabled(packageManager, context.packageName, MAIN_ACTIVITY, true)
            } catch (_: Exception) { }
        }
    }
    
    /**
     * Initializes the icon based on saved preferences.
     * Should be called on app startup.
     */
    fun initializeIcon(context: Context, designLanguage: Int) {
        val packageManager = context.packageManager
        
        // Check current state
        val mainEnabled = isComponentEnabled(packageManager, context.packageName, MAIN_ACTIVITY)
        val materialEnabled = isComponentEnabled(packageManager, context.packageName, MATERIAL_ALIAS)
        val ios6Enabled = isComponentEnabled(packageManager, context.packageName, IOS6_ALIAS)
        
        // If using default (main activity enabled), switch to the appropriate alias
        if (mainEnabled && !materialEnabled && !ios6Enabled) {
            setAppIcon(context, designLanguage)
        } else {
            // Verify the correct alias is enabled
            val useIOS6 = designLanguage == 1
            val correctAliasEnabled = if (useIOS6) ios6Enabled else materialEnabled
            
            if (!correctAliasEnabled) {
                setAppIcon(context, designLanguage)
            }
        }
    }
    
    private fun setComponentEnabled(
        packageManager: PackageManager,
        packageName: String,
        className: String,
        enabled: Boolean
    ) {
        val componentName = ComponentName(packageName, className)
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        
        packageManager.setComponentEnabledSetting(
            componentName,
            newState,
            PackageManager.DONT_KILL_APP
        )
    }
    
    private fun isComponentEnabled(
        packageManager: PackageManager,
        packageName: String,
        className: String
    ): Boolean {
        val componentName = ComponentName(packageName, className)
        return when (packageManager.getComponentEnabledSetting(componentName)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> {
                // Check the manifest default
                try {
                    val info = packageManager.getActivityInfo(componentName, 0)
                    info.enabled
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }
            }
            else -> false
        }
    }
}
