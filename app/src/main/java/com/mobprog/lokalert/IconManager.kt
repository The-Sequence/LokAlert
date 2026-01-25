package com.mobprog.lokalert

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Manages dynamic app icon switching between Material3 and iOS6 skeuomorphic styles.
 * Uses activity-alias to switch the launcher icon when the user changes design language.
 * 
 * MainActivity is NOT a launcher activity in the manifest. Only the aliases are launcher activities.
 * This ensures only ONE icon appears in the launcher at any time.
 */
object IconManager {
    
    private const val TAG = "IconManager"
    private const val MATERIAL_ALIAS = "com.mobprog.lokalert.MainActivityMaterial"
    private const val IOS6_ALIAS = "com.mobprog.lokalert.MainActivityiOS6"
    
    /**
     * Switches the app icon based on the design language.
     * 
     * IMPORTANT: This should ONLY be called when the app is dead/closed!
     * Calling this while app is running will cause force close because
     * disabling the current launcher alias terminates the app.
     * 
     * @param context Application context
     * @param designLanguage 0 for Material3, 1 for iOS6
     */
    fun setAppIcon(context: Context, designLanguage: Int) {
        val packageManager = context.packageManager
        val useIOS6 = designLanguage == 1
        
        Log.d(TAG, "setAppIcon called - target: ${if (useIOS6) "iOS6" else "Material3"}")
        
        try {
            if (useIOS6) {
                // Enable iOS6 first, then disable Material
                Log.d(TAG, "  → Enabling iOS6 alias...")
                setComponentEnabled(packageManager, context.packageName, IOS6_ALIAS, true)
                
                // IMPORTANT: Longer delay to ensure system registers the new enabled alias
                // before we disable the old one
                Thread.sleep(300)
                
                Log.d(TAG, "  → Disabling Material alias...")
                setComponentEnabled(packageManager, context.packageName, MATERIAL_ALIAS, false)
            } else {
                // Enable Material first, then disable iOS6
                Log.d(TAG, "  → Enabling Material alias...")
                setComponentEnabled(packageManager, context.packageName, MATERIAL_ALIAS, true)
                
                // IMPORTANT: Longer delay to ensure system registers the new enabled alias
                Thread.sleep(300)
                
                Log.d(TAG, "  → Disabling iOS6 alias...")
                setComponentEnabled(packageManager, context.packageName, IOS6_ALIAS, false)
            }
            
            Log.d(TAG, "✓ Icon changed to ${if (useIOS6) "iOS6" else "Material3"}")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error changing icon", e)
        }
    }
    
    /**
     * Checks if the correct icon is already enabled.
     * Does NOT change anything - just returns true if correct, false if needs change.
     */
    fun isCorrectIconEnabled(context: Context, designLanguage: Int): Boolean {
        val packageManager = context.packageManager
        
        return try {
            val useIOS6 = designLanguage == 1
            val materialEnabled = isComponentEnabled(packageManager, context.packageName, MATERIAL_ALIAS)
            val ios6Enabled = isComponentEnabled(packageManager, context.packageName, IOS6_ALIAS)
            
            if (useIOS6) {
                ios6Enabled && !materialEnabled
            } else {
                materialEnabled && !ios6Enabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking icon state", e)
            false
        }
    }
    
    /**
     * Safe initialization - only changes icon on first install when neither alias is enabled.
     * Should be called on app startup.
     */
    fun initializeIconIfNeeded(context: Context, designLanguage: Int) {
        val packageManager = context.packageManager
        
        try {
            val materialEnabled = isComponentEnabled(packageManager, context.packageName, MATERIAL_ALIAS)
            val ios6Enabled = isComponentEnabled(packageManager, context.packageName, IOS6_ALIAS)
            
            // Only do initial setup if neither alias is enabled (shouldn't happen with new manifest)
            if (!materialEnabled && !ios6Enabled) {
                Log.d(TAG, "No alias enabled, setting up initial icon")
                val useIOS6 = designLanguage == 1
                
                // Enable the correct alias
                if (useIOS6) {
                    setComponentEnabled(packageManager, context.packageName, IOS6_ALIAS, true)
                } else {
                    setComponentEnabled(packageManager, context.packageName, MATERIAL_ALIAS, true)
                }
                
                Log.d(TAG, "Initial icon set to ${if (useIOS6) "iOS6" else "Material3"}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing icon", e)
        }
    }
    
    /**
     * Legacy method for compatibility - delegates to initializeIconIfNeeded
     */
    fun initializeIcon(context: Context, designLanguage: Int) {
        initializeIconIfNeeded(context, designLanguage)
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
