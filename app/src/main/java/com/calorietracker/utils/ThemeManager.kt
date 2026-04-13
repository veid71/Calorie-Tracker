package com.calorietracker.utils

// 🧰 ANDROID TOOLS FOR THEMES
import android.content.Context                    // Access to Android system
import android.content.SharedPreferences         // Local storage for user preferences
import androidx.appcompat.app.AppCompatDelegate  // Controls app's light/dark theme

/**
 * 🎨 THEME MANAGER - CONTROLS LIGHT AND DARK MODE
 * 
 * Hey young programmer! This is like a light switch for our entire app.
 * 
 * 💡 What does this do?
 * 1. Remembers if the user likes light mode or dark mode
 * 2. Switches the entire app's appearance instantly
 * 3. Can follow the phone's system setting automatically
 * 4. Saves the user's choice so it persists between app sessions
 * 
 * 🌙 Light vs Dark Mode:
 * - Light Mode: White backgrounds, dark text (good for daytime)
 * - Dark Mode: Dark backgrounds, light text (easier on eyes at night)
 * - System Mode: Automatically follows your phone's setting
 * 
 * 💾 How do we remember the user's choice?
 * We use SharedPreferences - it's like a tiny database for simple settings.
 * Think of it like a notebook where we write down "User prefers dark mode: YES"
 * 
 * 🎯 This is an "object" class (singleton pattern):
 * There's only ever one ThemeManager in the entire app, and everyone shares it.
 * Like having one master light switch that controls the whole house.
 */
object ThemeManager {
    private const val PREFS_NAME = "theme_preferences"
    private const val KEY_DARK_MODE = "dark_mode_enabled"
    private const val KEY_FOLLOW_SYSTEM = "follow_system_theme"
    
    /**
     * Apply the saved theme preference
     */
    fun applyTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false)
        val followSystem = prefs.getBoolean(KEY_FOLLOW_SYSTEM, true)
        
        when {
            followSystem -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            isDarkModeEnabled -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    
    /**
     * Set dark mode preference
     */
    fun setDarkMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .putBoolean(KEY_FOLLOW_SYSTEM, false) // Disable system follow when manually set
            .apply()
        
        // Apply immediately
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    
    /**
     * Get current dark mode preference
     */
    fun isDarkModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val followSystem = prefs.getBoolean(KEY_FOLLOW_SYSTEM, true)
        
        return if (followSystem) {
            // If following system, check current system theme
            val currentNightMode = AppCompatDelegate.getDefaultNightMode()
            when (currentNightMode) {
                AppCompatDelegate.MODE_NIGHT_YES -> true
                AppCompatDelegate.MODE_NIGHT_NO -> false
                else -> {
                    // MODE_NIGHT_FOLLOW_SYSTEM, check actual system setting
                    val configuration = context.resources.configuration
                    val systemNightMode = configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    systemNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            }
        } else {
            // If not following system, return manual preference
            prefs.getBoolean(KEY_DARK_MODE, false)
        }
    }
    
    /**
     * Check if following system theme
     */
    fun isFollowingSystem(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FOLLOW_SYSTEM, true)
    }
    
    /**
     * Set to follow system theme
     */
    fun setFollowSystem(context: Context, follow: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_FOLLOW_SYSTEM, follow)
            .apply()
        
        if (follow) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}