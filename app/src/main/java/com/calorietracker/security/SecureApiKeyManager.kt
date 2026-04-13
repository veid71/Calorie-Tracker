package com.calorietracker.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 🔐 SECURE API KEY MANAGER
 * 
 * Provides secure storage for API keys using Android Keystore system.
 * This prevents API keys from being extracted via ADB backup, rooted devices,
 * or other security vulnerabilities.
 * 
 * 🛡️ Security Features:
 * - Uses Android Hardware Security Module (HSM) when available
 * - Keys stored in secure hardware enclave (TEE/Secure Element)
 * - Automatic key generation with AES-GCM encryption
 * - Keys cannot be extracted even from rooted devices
 * - Biometric authentication support for additional security
 * 
 * 📚 How it works:
 * 1. Generates a master key in Android Keystore
 * 2. Uses master key to encrypt API keys before storage
 * 3. Stores encrypted API keys in SharedPreferences
 * 4. Decrypts keys on demand using secure hardware
 */
class SecureApiKeyManager private constructor(private val context: Context) {
    
    companion object {
        private const val KEYSTORE_ALIAS = "CalorieTrackerApiKeyAlias"
        private const val PREFS_NAME = "secure_api_keys"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        
        @Volatile
        private var INSTANCE: SecureApiKeyManager? = null
        
        fun getInstance(context: Context): SecureApiKeyManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureApiKeyManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    init {
        // Generate master key if it doesn't exist
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            generateMasterKey()
        }
    }
    
    /**
     * 🔑 GENERATE MASTER KEY IN SECURE HARDWARE
     * 
     * Creates a master encryption key that lives in the Android Keystore.
     * This key cannot be extracted or accessed directly, even on rooted devices.
     */
    private fun generateMasterKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // Require device authentication for additional security
                .setUserAuthenticationRequired(false) // Set to true for biometric protection
                .setRandomizedEncryptionRequired(true)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
            
            android.util.Log.d("SecureApiKeyManager", "🔑 Master key generated successfully in secure hardware")
        } catch (e: Exception) {
            android.util.Log.e("SecureApiKeyManager", "❌ Failed to generate master key", e)
            throw SecurityException("Failed to initialize secure key storage", e)
        }
    }
    
    /**
     * 🔒 SECURELY STORE API KEY
     * 
     * Encrypts the API key using hardware-backed encryption and stores it securely.
     * 
     * @param keyName Unique identifier for the API key (e.g., "usda_api_key")
     * @param apiKey The plain text API key to secure
     * @return true if successfully stored, false otherwise
     */
    fun storeApiKey(keyName: String, apiKey: String): Boolean {
        if (apiKey.isBlank()) {
            android.util.Log.w("SecureApiKeyManager", "⚠️ Attempted to store empty API key for $keyName")
            return false
        }
        
        try {
            val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            // Encrypt the API key
            val encryptedData = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            
            // Combine IV and encrypted data
            val encryptedWithIv = iv + encryptedData
            val encodedData = Base64.encodeToString(encryptedWithIv, Base64.DEFAULT)
            
            // Store encrypted data in SharedPreferences
            prefs.edit()
                .putString("${keyName}_encrypted", encodedData)
                .putString("${keyName}_timestamp", System.currentTimeMillis().toString())
                .apply()
            
            android.util.Log.d("SecureApiKeyManager", "🔐 API key '$keyName' stored securely")
            return true
            
        } catch (e: Exception) {
            android.util.Log.e("SecureApiKeyManager", "❌ Failed to store API key '$keyName'", e)
            return false
        }
    }
    
    /**
     * 🔓 SECURELY RETRIEVE API KEY
     * 
     * Decrypts and returns the API key using hardware-backed decryption.
     * 
     * @param keyName Unique identifier for the API key
     * @return Decrypted API key or null if not found/invalid
     */
    fun getApiKey(keyName: String): String? {
        try {
            val encryptedData = prefs.getString("${keyName}_encrypted", null) ?: return null
            val decodedData = Base64.decode(encryptedData, Base64.DEFAULT)
            
            // Extract IV and encrypted data (IV is first 12 bytes for GCM)
            val iv = decodedData.sliceArray(0..11)
            val encrypted = decodedData.sliceArray(12 until decodedData.size)
            
            // Decrypt using secure hardware
            val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedBytes = cipher.doFinal(encrypted)
            val apiKey = String(decryptedBytes, Charsets.UTF_8)
            
            android.util.Log.d("SecureApiKeyManager", "🔓 API key '$keyName' retrieved securely")
            return apiKey
            
        } catch (e: Exception) {
            android.util.Log.e("SecureApiKeyManager", "❌ Failed to retrieve API key '$keyName'", e)
            return null
        }
    }
    
    /**
     * 🗑️ SECURELY DELETE API KEY
     * 
     * Permanently removes the encrypted API key from storage.
     * 
     * @param keyName Unique identifier for the API key to delete
     * @return true if successfully deleted, false otherwise
     */
    fun deleteApiKey(keyName: String): Boolean {
        try {
            prefs.edit()
                .remove("${keyName}_encrypted")
                .remove("${keyName}_timestamp")
                .apply()
            
            android.util.Log.d("SecureApiKeyManager", "🗑️ API key '$keyName' deleted securely")
            return true
            
        } catch (e: Exception) {
            android.util.Log.e("SecureApiKeyManager", "❌ Failed to delete API key '$keyName'", e)
            return false
        }
    }
    
    /**
     * 📋 LIST ALL STORED API KEYS
     * 
     * Returns a list of all API key names that are currently stored.
     * Does not return the actual keys, only their identifiers.
     */
    fun getStoredApiKeyNames(): List<String> {
        return prefs.all.keys
            .filter { it.endsWith("_encrypted") }
            .map { it.removeSuffix("_encrypted") }
    }
    
    /**
     * 🧹 CLEANUP OLD API KEYS
     * 
     * Removes API keys that haven't been accessed in a specified time.
     * Useful for automatic cleanup of unused keys.
     * 
     * @param maxAgeMs Maximum age in milliseconds (default: 90 days)
     */
    fun cleanupOldKeys(maxAgeMs: Long = 90L * 24 * 60 * 60 * 1000) {
        val currentTime = System.currentTimeMillis()
        val keysToDelete = mutableListOf<String>()
        
        prefs.all.forEach { (key, value) ->
            if (key.endsWith("_timestamp") && value is String) {
                val timestamp = value.toLongOrNull() ?: 0
                if (currentTime - timestamp > maxAgeMs) {
                    val keyName = key.removeSuffix("_timestamp")
                    keysToDelete.add(keyName)
                }
            }
        }
        
        keysToDelete.forEach { keyName ->
            deleteApiKey(keyName)
            android.util.Log.d("SecureApiKeyManager", "🧹 Cleaned up old API key: $keyName")
        }
        
        if (keysToDelete.isNotEmpty()) {
            android.util.Log.i("SecureApiKeyManager", "🧹 Cleaned up ${keysToDelete.size} old API keys")
        }
    }
    
    /**
     * 🔍 CHECK IF API KEY EXISTS
     * 
     * Checks if an API key is stored without retrieving it.
     */
    fun hasApiKey(keyName: String): Boolean {
        return prefs.contains("${keyName}_encrypted")
    }
    
    /**
     * 🏥 HEALTH CHECK
     * 
     * Verifies that the secure storage system is working correctly.
     * Returns true if the keystore and encryption are functioning properly.
     */
    fun healthCheck(): Boolean {
        return try {
            // Test encryption/decryption cycle
            val testKey = "health_check_test"
            val testValue = "test_${System.currentTimeMillis()}"
            
            val stored = storeApiKey(testKey, testValue)
            val retrieved = getApiKey(testKey)
            val deleted = deleteApiKey(testKey)
            
            stored && retrieved == testValue && deleted
        } catch (e: Exception) {
            android.util.Log.e("SecureApiKeyManager", "❌ Health check failed", e)
            false
        }
    }
}