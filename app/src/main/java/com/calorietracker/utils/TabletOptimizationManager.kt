package com.calorietracker.utils

// 🧰 ANDROID SYSTEM TOOLS
import android.content.Context               // Android system access
import android.content.SharedPreferences    // Local storage for user preferences
import android.content.res.Configuration    // Device configuration info
import android.util.DisplayMetrics          // Screen size measurements
import androidx.appcompat.app.AppCompatActivity // Base activity class

/**
 * 📱 TABLET OPTIMIZATION MANAGER - SMART LAYOUT DETECTION
 * 
 * Hey young programmer! This automatically detects if the user has a tablet or phone
 * and optimizes the app layout accordingly.
 * 
 * 📏 What's the difference?
 * - 📱 PHONE: Small screen, single-column layout, stacked elements
 * - 📟 TABLET: Large screen, multi-column layout, side-by-side panels
 * 
 * 🎯 How does detection work?
 * 1. Check screen size in inches (diagonal measurement)
 * 2. Check screen density (how many pixels per inch)
 * 3. Check orientation (portrait vs landscape)
 * 4. Consider user's manual preference override
 * 
 * 📊 Screen Size Categories:
 * - Small: < 5 inches (definitely phone)
 * - Normal: 5-7 inches (large phone or small tablet)
 * - Large: 7-10 inches (tablet)
 * - XLarge: > 10 inches (large tablet or TV)
 * 
 * 🎨 Layout Optimizations:
 * - Tablet: Two-column nutrition entry, side-by-side macro charts
 * - Phone: Single-column, stacked vertically for easy scrolling
 * - Auto-detect: Automatically choose best layout
 * - Manual override: User can force phone or tablet layout
 * 
 * 💡 Smart Features:
 * - Remembers user's preference between app sessions
 * - Detects orientation changes (portrait ↔ landscape)
 * - Provides settings toggle for manual control
 * - Optimizes button sizes and spacing for touch targets
 */
object TabletOptimizationManager {
    
    // 💾 PREFERENCE STORAGE KEYS
    private const val PREFS_NAME = "tablet_optimization_prefs"
    private const val KEY_USE_TABLET_LAYOUT = "use_tablet_layout"
    private const val KEY_AUTO_DETECT = "auto_detect_layout"
    private const val KEY_USER_OVERRIDE = "user_override_layout"
    
    // 📏 SCREEN SIZE THRESHOLDS (in inches)
    private const val TABLET_MIN_SIZE_INCHES = 7.0    // 7+ inches = tablet
    private const val LARGE_TABLET_SIZE_INCHES = 10.0 // 10+ inches = large tablet
    
    /**
     * 📱 DETECT IF DEVICE IS A TABLET
     * 
     * Analyzes screen size and density to determine device type.
     * 
     * @param context Android context for accessing system info
     * @return True if device appears to be a tablet
     */
    fun isTabletDevice(context: Context): Boolean {
        // 📐 GET SCREEN MEASUREMENTS
        val displayMetrics = context.resources.displayMetrics
        val screenWidthInches = displayMetrics.widthPixels / displayMetrics.xdpi
        val screenHeightInches = displayMetrics.heightPixels / displayMetrics.ydpi
        val screenSizeInches = kotlin.math.sqrt(
            (screenWidthInches * screenWidthInches) + (screenHeightInches * screenHeightInches)
        )
        
        // 📊 CHECK CONFIGURATION
        val configuration = context.resources.configuration
        val isLargeScreen = (configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
        
        // 🎯 TABLET DETECTION LOGIC
        return screenSizeInches >= TABLET_MIN_SIZE_INCHES || isLargeScreen
    }
    
    /**
     * 🎨 SHOULD USE TABLET LAYOUT?
     * 
     * Determines whether to use tablet-optimized layout based on device detection
     * and user preferences.
     * 
     * @param context Android context
     * @return True if should use tablet layout
     */
    fun shouldUseTabletLayout(context: Context): Boolean {
        val prefs = getPreferences(context)
        val autoDetect = prefs.getBoolean(KEY_AUTO_DETECT, true)
        
        return if (autoDetect) {
            // 🤖 AUTO-DETECTION MODE
            isTabletDevice(context)
        } else {
            // 👤 USER MANUAL OVERRIDE
            prefs.getBoolean(KEY_USE_TABLET_LAYOUT, false)
        }
    }
    
    /**
     * ⚙️ SET TABLET LAYOUT PREFERENCE
     * 
     * Allow user to manually override automatic detection.
     * 
     * @param context Android context
     * @param useTabletLayout Force tablet layout?
     * @param autoDetect Should auto-detect, or use manual setting?
     */
    fun setTabletLayoutPreference(context: Context, useTabletLayout: Boolean, autoDetect: Boolean = false) {
        getPreferences(context).edit()
            .putBoolean(KEY_USE_TABLET_LAYOUT, useTabletLayout)
            .putBoolean(KEY_AUTO_DETECT, autoDetect)
            .putBoolean(KEY_USER_OVERRIDE, !autoDetect)
            .apply()
    }
    
    /**
     * 🔄 TOGGLE AUTO-DETECTION
     * 
     * Switch between automatic device detection and manual layout choice.
     */
    fun setAutoDetection(context: Context, enabled: Boolean) {
        getPreferences(context).edit()
            .putBoolean(KEY_AUTO_DETECT, enabled)
            .putBoolean(KEY_USER_OVERRIDE, !enabled)
            .apply()
    }
    
    /**
     * 📊 GET CURRENT LAYOUT PREFERENCES
     * 
     * Returns current tablet layout settings for displaying in settings screen.
     */
    fun getCurrentPreferences(context: Context): TabletLayoutPreferences {
        val prefs = getPreferences(context)
        return TabletLayoutPreferences(
            useTabletLayout = prefs.getBoolean(KEY_USE_TABLET_LAYOUT, false),
            autoDetect = prefs.getBoolean(KEY_AUTO_DETECT, true),
            isTabletDevice = isTabletDevice(context),
            currentLayoutMode = if (shouldUseTabletLayout(context)) "Tablet" else "Phone"
        )
    }
    
    /**
     * 📐 GET DEVICE SCREEN INFO
     * 
     * Returns detailed information about the device screen for debugging.
     */
    fun getDeviceScreenInfo(context: Context): DeviceScreenInfo {
        val displayMetrics = context.resources.displayMetrics
        val configuration = context.resources.configuration
        
        val screenWidthInches = displayMetrics.widthPixels / displayMetrics.xdpi
        val screenHeightInches = displayMetrics.heightPixels / displayMetrics.ydpi
        val screenSizeInches = kotlin.math.sqrt(
            (screenWidthInches * screenWidthInches) + (screenHeightInches * screenHeightInches)
        )
        
        val densityCategory = when (displayMetrics.densityDpi) {
            in 0..120 -> "ldpi"
            in 121..160 -> "mdpi"
            in 161..240 -> "hdpi"
            in 241..320 -> "xhdpi"
            in 321..480 -> "xxhdpi"
            else -> "xxxhdpi"
        }
        
        val sizeCategory = when (configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
            Configuration.SCREENLAYOUT_SIZE_SMALL -> "small"
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> "normal"
            Configuration.SCREENLAYOUT_SIZE_LARGE -> "large"
            Configuration.SCREENLAYOUT_SIZE_XLARGE -> "xlarge"
            else -> "undefined"
        }
        
        return DeviceScreenInfo(
            screenSizeInches = screenSizeInches.toDouble(),
            widthPixels = displayMetrics.widthPixels,
            heightPixels = displayMetrics.heightPixels,
            densityDpi = displayMetrics.densityDpi,
            densityCategory = densityCategory,
            sizeCategory = sizeCategory,
            isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        )
    }
    
    /**
     * 🎨 APPLY TABLET OPTIMIZATIONS TO ACTIVITY
     * 
     * Call this in activity's onCreate() to apply appropriate layout optimizations.
     */
    fun applyTabletOptimizations(activity: AppCompatActivity) {
        if (shouldUseTabletLayout(activity)) {
            // 📟 TABLET OPTIMIZATIONS
            
            // Set larger text sizes for better readability on big screens
            // Enable multi-pane layouts where available
            // Optimize touch targets for tablet use
            
            // For now, we'll handle this through alternative layout files
            // Android automatically selects layouts from layout-large, layout-xlarge folders
        }
    }
    
    /**
     * 💾 GET SHARED PREFERENCES
     * 
     * Helper to access our tablet optimization preferences.
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}

/**
 * ⚙️ TABLET LAYOUT PREFERENCES
 * 
 * Data class containing current tablet layout settings.
 */
data class TabletLayoutPreferences(
    val useTabletLayout: Boolean,    // 📟 Currently using tablet layout?
    val autoDetect: Boolean,         // 🤖 Auto-detecting device type?
    val isTabletDevice: Boolean,     // 📱 Hardware detected as tablet?
    val currentLayoutMode: String    // 🎨 "Phone" or "Tablet" layout active
)

/**
 * 📱 DEVICE SCREEN INFO
 * 
 * Detailed information about device screen for debugging and optimization.
 */
data class DeviceScreenInfo(
    val screenSizeInches: Double,    // 📏 Diagonal screen size in inches
    val widthPixels: Int,           // 📐 Screen width in pixels
    val heightPixels: Int,          // 📐 Screen height in pixels  
    val densityDpi: Int,           // 🔍 Pixels per inch
    val densityCategory: String,    // 🏷️ "mdpi", "hdpi", "xhdpi", etc.
    val sizeCategory: String,       // 📱 "small", "normal", "large", "xlarge"
    val isLandscape: Boolean        // 🔄 Currently in landscape orientation?
)