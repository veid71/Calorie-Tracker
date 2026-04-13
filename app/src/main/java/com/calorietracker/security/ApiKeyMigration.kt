package com.calorietracker.security

import android.content.Context
import android.content.SharedPreferences

/**
 * 🔄 API KEY MIGRATION UTILITY
 * 
 * Migrates existing plain text API keys from SharedPreferences 
 * to secure Android Keystore storage, then cleans up the old keys.
 * 
 * This is a one-time migration that happens automatically when
 * the app updates to the secure storage version.
 */
object ApiKeyMigration {
    
    private const val LEGACY_PREFS_NAME = "calorie_tracker_api"
    private const val MIGRATION_KEY = "api_keys_migrated_to_secure_storage"
    
    /**
     * 🔄 MIGRATE ALL API KEYS TO SECURE STORAGE
     * 
     * Safely migrates existing API keys from plain text SharedPreferences
     * to encrypted Android Keystore storage.
     * 
     * @param context Application context
     * @return Migration result with details
     */
    fun migrateApiKeys(context: Context): MigrationResult {
        val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        
        // Check if migration already completed
        if (legacyPrefs.getBoolean(MIGRATION_KEY, false)) {
            android.util.Log.d("ApiKeyMigration", "✅ API keys already migrated to secure storage")
            return MigrationResult(
                success = true,
                keysFound = 0,
                keysMigrated = 0,
                message = "Already migrated"
            )
        }
        
        val secureManager = SecureApiKeyManager.getInstance(context)
        val keysToMigrate = mapOf(
            "usda_api_key" to "USDA API Key",
            "edamam_app_id" to "Edamam App ID", 
            "edamam_app_key" to "Edamam App Key",
            "nutritionix_app_id" to "Nutritionix App ID",
            "nutritionix_app_key" to "Nutritionix App Key"
        )
        
        var keysFound = 0
        var keysMigrated = 0
        val migrationErrors = mutableListOf<String>()
        
        try {
            // Migrate each API key
            keysToMigrate.forEach { (keyName, displayName) ->
                val plainTextKey = legacyPrefs.getString(keyName, null)
                
                if (!plainTextKey.isNullOrBlank()) {
                    keysFound++
                    android.util.Log.d("ApiKeyMigration", "🔍 Found $displayName in legacy storage")
                    
                    // Store in secure storage
                    val stored = secureManager.storeApiKey(keyName, plainTextKey)
                    if (stored) {
                        keysMigrated++
                        android.util.Log.d("ApiKeyMigration", "🔐 Migrated $displayName to secure storage")
                    } else {
                        migrationErrors.add("Failed to migrate $displayName")
                        android.util.Log.e("ApiKeyMigration", "❌ Failed to migrate $displayName")
                    }
                }
            }
            
            // If migration successful, clean up old keys and mark as completed
            if (migrationErrors.isEmpty() && keysFound > 0) {
                cleanupLegacyKeys(legacyPrefs, keysToMigrate.keys)
                markMigrationComplete(legacyPrefs)
                
                android.util.Log.i("ApiKeyMigration", "✅ Successfully migrated $keysMigrated API keys to secure storage")
            } else if (keysFound == 0) {
                // No keys to migrate, mark as complete
                markMigrationComplete(legacyPrefs)
                android.util.Log.d("ApiKeyMigration", "ℹ️ No API keys found to migrate")
            }
            
            return MigrationResult(
                success = migrationErrors.isEmpty(),
                keysFound = keysFound,
                keysMigrated = keysMigrated,
                message = if (migrationErrors.isEmpty()) {
                    "Successfully migrated $keysMigrated API keys"
                } else {
                    "Migration completed with errors: ${migrationErrors.joinToString(", ")}"
                },
                errors = migrationErrors
            )
            
        } catch (e: Exception) {
            android.util.Log.e("ApiKeyMigration", "❌ API key migration failed", e)
            return MigrationResult(
                success = false,
                keysFound = keysFound,
                keysMigrated = keysMigrated,
                message = "Migration failed: ${e.message}",
                errors = listOf(e.message ?: "Unknown error")
            )
        }
    }
    
    /**
     * 🧹 CLEAN UP LEGACY API KEYS
     * 
     * Removes plain text API keys from SharedPreferences after successful migration.
     */
    private fun cleanupLegacyKeys(prefs: SharedPreferences, keyNames: Set<String>) {
        val editor = prefs.edit()
        keyNames.forEach { keyName ->
            editor.remove(keyName)
            android.util.Log.d("ApiKeyMigration", "🗑️ Removed legacy key: $keyName")
        }
        editor.apply()
    }
    
    /**
     * ✅ MARK MIGRATION AS COMPLETE
     */
    private fun markMigrationComplete(prefs: SharedPreferences) {
        prefs.edit()
            .putBoolean(MIGRATION_KEY, true)
            .putLong("migration_completed_timestamp", System.currentTimeMillis())
            .apply()
    }
    
    /**
     * 🔍 CHECK IF MIGRATION IS NEEDED
     * 
     * Returns true if there are legacy API keys that need migration.
     */
    fun isMigrationNeeded(context: Context): Boolean {
        val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        
        // If already migrated, no need to check
        if (legacyPrefs.getBoolean(MIGRATION_KEY, false)) {
            return false
        }
        
        // Check if any legacy keys exist
        val legacyKeys = listOf("usda_api_key", "edamam_app_id", "edamam_app_key", "nutritionix_app_id", "nutritionix_app_key")
        return legacyKeys.any { keyName ->
            !legacyPrefs.getString(keyName, null).isNullOrBlank()
        }
    }
    
    /**
     * 📊 MIGRATION RESULT DATA CLASS
     */
    data class MigrationResult(
        val success: Boolean,
        val keysFound: Int,
        val keysMigrated: Int,
        val message: String,
        val errors: List<String> = emptyList()
    )
}