package com.calorietracker.network

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.calorietracker.database.*
import com.calorietracker.notifications.DatabaseDownloadNotificationManager
import com.calorietracker.utils.FileLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream

/**
 * Central manager for downloading and updating food databases from multiple sources.
 * 
 * **NETWORKING FUNDAMENTALS FOR BEGINNERS:**
 * 
 * **What is an API?**
 * An API (Application Programming Interface) is like a waiter in a restaurant:
 * - Your app is the customer making requests
 * - The API is the waiter who takes your order
 * - The server (USDA, Open Food Facts) is the kitchen that prepares data
 * - The waiter brings back the food data to your app
 * 
 * **REST API Concepts:**
 * - HTTP Requests: Like sending letters to different addresses (URLs)
 * - GET requests: "Please give me data" (like asking for a menu)
 * - POST requests: "Please save this data" (like placing an order)
 * - Response codes: 200 = success, 404 = not found, 500 = server error
 * 
 * **Why Use Multiple APIs?**
 * Different APIs have different strengths:
 * - USDA: Government nutrition data (very accurate, scientific)
 * - Open Food Facts: Barcode database (community-driven, good coverage)
 * - Edamam: Restaurant foods (commercial database)
 * - Nutritionix: Branded products (comprehensive commercial data)
 * 
 * **ANDROID NETWORKING CONCEPTS:**
 * 
 * **Retrofit Library:**
 * Retrofit is like a smart translator for API calls:
 * - Converts Kotlin function calls into HTTP requests
 * - Automatically converts JSON responses to Kotlin objects
 * - Handles network errors and retries automatically
 * 
 * **Coroutines & Async Programming:**
 * Network calls are slow (1-10 seconds), so we use coroutines:
 * - suspend functions: Can pause and resume without freezing UI
 * - Dispatchers.IO: Special thread pool for network/file operations
 * - withContext(): Switch between UI thread and background threads
 * 
 * **Caching Strategy:**
 * We use a "cache-first" approach for better performance:
 * 1. Check local database first (instant, works offline)
 * 2. If not found, make API request (slower, needs internet)
 * 3. Save API response to cache for next time
 * 4. This makes the app faster and works offline
 * 
 * **JSON Data Parsing:**
 * APIs return data in JSON format (like a text-based filing system):
 * - Gson library converts JSON text to Kotlin objects automatically
 * - Data classes define the structure we expect
 * - Null safety prevents crashes from missing data
 * 
 * **BULK DATA DOWNLOAD CONCEPTS:**
 * 
 * **Why Bulk Downloads?**
 * Instead of requesting one food at a time:
 * - Download entire database once (400MB ZIP file)
 * - Extract and parse CSV files locally
 * - Store everything in SQLite database
 * - Much faster searches, works completely offline
 * 
 * **ZIP File Processing:**
 * Large datasets come compressed:
 * - ZipInputStream reads compressed data
 * - BufferedReader processes large files efficiently
 * - CSV parsing converts text rows to database records
 * - Stream processing prevents memory overflow
 * 
 * This class coordinates the complex process of downloading, parsing, and storing
 * food databases from various APIs and data sources. It handles:
 * 
 * **Supported Data Sources:**
 * - USDA FoodData Central (113,886+ items from 2025 dataset)
 * - Open Food Facts (community-driven barcode database)
 * - Edamam Food Database (comprehensive nutrient data)
 * - Nutritionix (restaurant and branded foods)
 * 
 * **Key Features:**
 * - Bulk CSV downloads for offline-first architecture
 * - Background downloads with progress notifications
 * - API key management (user keys + build config fallbacks)
 * - Intelligent error handling and retry mechanisms
 * - ZIP file processing for large datasets
 * - Database migration-safe imports
 * 
 * **Download Process:**
 * 1. Check API availability and authentication
 * 2. Download ZIP archives (can be 400MB+)
 * 3. Parse CSV files (sr_legacy_food.csv format)
 * 4. Import to SQLite with batch operations
 * 5. Update food database status tracking
 * 6. Show completion notification to user
 * 
 * **Security:**
 * - API keys stored in SharedPreferences (user-provided)
 * - BuildConfig fallbacks for developer keys
 * - No hardcoded API keys in source code
 * - Safe reflection for accessing BuildConfig fields
 * 
 * @param context Android context for notifications and storage
 * @param database Room database instance for food data storage
 */
class FoodDatabaseManager(
    private val context: Context,
    private val database: CalorieDatabase
) {
    private val usdaService = USDAFoodService.create()
    private val usdaBulkService = USDABulkDataService.create()
    private val openFoodFactsService = OpenFoodFactsService.create()
    private val edamamService = EdamamFoodService.create()
    private val notificationManager = DatabaseDownloadNotificationManager(context)
    private val csvParser = USDACSVParser()
    private val usdaDownloader = USDADatabaseDownloader(context)
    private val nutritionixManager = NutritionixManager(context)
    private val offlineCacheManager = com.calorietracker.cache.OfflineCacheManager(context, database)
    
    /**
     * API KEY MANAGEMENT CONCEPTS:
     * 
     * **What is an API Key?**
     * An API key is like a membership card for using web services:
     * - It identifies your app to the API server
     * - It tracks how many requests you make
     * - It enforces usage limits (free vs paid tiers)
     * - It can be revoked if misused
     * 
     * **Rate Limiting:**
     * APIs limit how many requests you can make:
     * - DEMO_KEY: 5 requests/hour (too slow for bulk downloads)
     * - Free API key: 1,000 requests/hour (much better)
     * - Paid tiers: Unlimited or very high limits
     * 
     * **Key Storage Security:**
     * We use multiple approaches to protect API keys:
     * 1. SharedPreferences (user enters their own key)
     * 2. BuildConfig (developer key, not in source code)
     * 3. Reflection (safely access BuildConfig without crashes)
     */
    
    // USDA API key - get from https://fdc.nal.usda.gov/api-key-signup.html
    // DEMO_KEY has severe rate limits (5 requests/hour) - not suitable for database downloads
    // Note: These are now functions to always get the latest values
    
    // Edamam API credentials - get from https://developer.edamam.com/
    // Free tier: 5,000 requests/month
    
    private fun getUSDAApiKey(): String {
        // 🔐 USE SECURE API KEY STORAGE
        val secureManager = com.calorietracker.security.SecureApiKeyManager.getInstance(context)
        val userApiKey = secureManager.getApiKey("usda_api_key")
        
        return if (!userApiKey.isNullOrBlank()) {
            userApiKey
        } else {
            // Check BuildConfig for API key (set via gradle.properties)
            try {
                val buildConfigField = Class.forName("${context.packageName}.BuildConfig")
                    .getDeclaredField("USDA_API_KEY")
                buildConfigField.get(null) as? String ?: ""
            } catch (e: Exception) {
                "" // No API key needed for bulk CSV downloads
            }
        }
    }
    
    private fun getEdamamAppId(): String {
        // 🔐 USE SECURE API KEY STORAGE
        val secureManager = com.calorietracker.security.SecureApiKeyManager.getInstance(context)
        val userAppId = secureManager.getApiKey("edamam_app_id")
        
        return if (!userAppId.isNullOrBlank()) {
            userAppId
        } else {
            try {
                val buildConfigField = Class.forName("${context.packageName}.BuildConfig")
                    .getDeclaredField("EDAMAM_APP_ID")
                buildConfigField.get(null) as? String ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }
    
    private fun getEdamamAppKey(): String {
        // 🔐 USE SECURE API KEY STORAGE
        val secureManager = com.calorietracker.security.SecureApiKeyManager.getInstance(context)
        val userAppKey = secureManager.getApiKey("edamam_app_key")
        
        return if (!userAppKey.isNullOrBlank()) {
            userAppKey
        } else {
            try {
                val buildConfigField = Class.forName("${context.packageName}.BuildConfig")
                    .getDeclaredField("EDAMAM_APP_KEY")
                buildConfigField.get(null) as? String ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }
    
    private val _downloadProgress = MutableStateFlow<DownloadProgress>(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    private val isOFFDownloadRunning = java.util.concurrent.atomic.AtomicBoolean(false)

    // Persistent scope not tied to any Activity lifecycle — survives screen-off, navigation, etc.
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var downloadJob: Job? = null
    private var isReceiverRegistered = false
    
    // Broadcast receiver for handling cancel requests from notification
    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.calorietracker.ACTION_CANCEL_DOWNLOAD") {
                Log.d("FoodDatabaseManager", "Received cancel request from notification")
                cancelDownload()
            }
        }
    }
    
    // We'll register the receiver when needed to avoid context issues
    
    data class DownloadProgress(
        val currentDatabase: String = "",
        val currentOperation: String = "",
        val totalItems: Int = 0,
        val downloadedItems: Int = 0,
        val isDownloading: Boolean = false,
        val isComplete: Boolean = false,
        val error: String? = null
    )
    
    /**
     * Check if a valid USDA API key is configured for API searches
     * Note: Bulk CSV downloads don't require an API key
     */
    fun hasValidUSDAApiKey(): Boolean {
        val currentApiKey = getUSDAApiKey()
        return currentApiKey.isNotBlank()
    }
    
    /**
     * Check if bulk downloads are available (they don't require API keys)
     */
    fun canUseBulkDownload(): Boolean {
        return true // Bulk CSV downloads are always available
    }
    
    /**
     * Set USDA API key - now stored securely using Android Keystore
     */
    fun setUSDAApiKey(apiKey: String): Boolean {
        val secureManager = com.calorietracker.security.SecureApiKeyManager.getInstance(context)
        return secureManager.storeApiKey("usda_api_key", apiKey)
    }
    
    /**
     * Set Edamam API credentials - now stored securely using Android Keystore
     */
    fun setEdamamCredentials(appId: String, appKey: String): Boolean {
        val secureManager = com.calorietracker.security.SecureApiKeyManager.getInstance(context)
        val appIdStored = secureManager.storeApiKey("edamam_app_id", appId)
        val appKeyStored = secureManager.storeApiKey("edamam_app_key", appKey)
        return appIdStored && appKeyStored
    }
    
    /**
     * Download both USDA and Open Food Facts databases
     * 
     * **COROUTINES & ASYNC PROGRAMMING EXPLAINED:**
     * 
     * **Why suspend functions?**
     * Network operations are slow (1-10+ seconds), so we use suspend functions:
     * - They can "pause" without freezing the UI thread
     * - The UI stays responsive while downloading happens in background
     * - When download completes, the function "resumes" with the result
     * 
     * **withContext(Dispatchers.IO) explained:**
     * - Dispatchers.IO: Special thread pool for Input/Output operations
     * - Switches from UI thread to background thread for heavy work
     * - Prevents "Application Not Responding" (ANR) errors
     * - Returns result back to UI thread when complete
     * 
     * **Result<T> Pattern:**
     * Instead of throwing exceptions, we return Result objects:
     * - Result.success(data): Operation worked, here's the data
     * - Result.failure(exception): Operation failed, here's why
     * - Caller can check isSuccess/isFailure and handle appropriately
     * 
     * **Error Handling Strategy:**
     * Network operations can fail for many reasons:
     * - No internet connection
     * - Server temporarily down
     * - Rate limits exceeded
     * - Invalid API key
     * We catch exceptions and return descriptive error messages
     */
    suspend fun downloadAllDatabases(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FoodDatabaseManager", "=== Starting downloadAllDatabases ===")
                _downloadProgress.value = _downloadProgress.value.copy(
                    isDownloading = true,
                    isComplete = false,
                    error = null
                )
                
                // Download USDA database first
                Log.d("FoodDatabaseManager", "Starting USDA database download...")
                val usdaResult = downloadUSDABulkData() // Use bulk download directly
                Log.d("FoodDatabaseManager", "USDA download result: ${if (usdaResult.isSuccess) "SUCCESS" else "FAILURE - ${usdaResult.exceptionOrNull()?.message}"}")
                
                if (usdaResult.isFailure) {
                    // If USDA failed, continue with Open Food Facts only
                    Log.w("FoodDatabaseManager", "USDA download failed, continuing with Open Food Facts only")
                    _downloadProgress.value = _downloadProgress.value.copy(
                        currentOperation = "USDA temporarily unavailable, continuing with Open Food Facts..."
                    )
                    // Don't return failure, continue with OFF
                } else {
                    Log.d("FoodDatabaseManager", "USDA download completed successfully, proceeding to Open Food Facts...")
                }
                
                // Download Open Food Facts database
                Log.d("FoodDatabaseManager", "Starting Open Food Facts database download...")
                val offResult = downloadOpenFoodFactsDatabase()
                Log.d("FoodDatabaseManager", "Open Food Facts download result: ${if (offResult.isSuccess) "SUCCESS" else "FAILURE - ${offResult.exceptionOrNull()?.message}"}")
                
                if (offResult.isFailure) {
                    _downloadProgress.value = _downloadProgress.value.copy(
                        isDownloading = false,
                        error = "Open Food Facts download failed: ${offResult.exceptionOrNull()?.message}"
                    )
                    return@withContext offResult
                }
                
                _downloadProgress.value = _downloadProgress.value.copy(
                    isDownloading = false,
                    isComplete = true,
                    currentOperation = "Download completed successfully!"
                )
                
                Log.d("FoodDatabaseManager", "=== downloadAllDatabases completed successfully ===")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("FoodDatabaseManager", "=== downloadAllDatabases failed with exception ===", e)
                _downloadProgress.value = _downloadProgress.value.copy(
                    isDownloading = false,
                    error = e.message
                )
                Result.failure(e)
            }
        }
    }
    
    /**
     * Download USDA FoodData Central database
     */
    private suspend fun downloadUSDADatabase(): Result<Unit> {
        return try {
            FileLogger.addSeparator("USDA Database Download")
            FileLogger.d("FoodDatabaseManager", "=== USDA Download Started ===")
            FileLogger.d("FoodDatabaseManager", "Using API key: ${getUSDAApiKey().take(10)}...")
            
            Log.d("FoodDatabaseManager", "=== USDA Download Started ===")
            Log.d("FoodDatabaseManager", "Using API key: ${getUSDAApiKey().take(10)}...")
            
            // Always try bulk download first (faster and more efficient)
            FileLogger.d("FoodDatabaseManager", "Attempting USDA bulk data download...")
            Log.d("FoodDatabaseManager", "Attempting USDA bulk data download...")
            
            val bulkResult = downloadUSDABulkData()
            if (bulkResult.isSuccess) {
                FileLogger.d("FoodDatabaseManager", "USDA bulk data download successful!")
                Log.d("FoodDatabaseManager", "USDA bulk data download successful!")
                return bulkResult
            }
            
            // If bulk download fails and we have a valid API key, fall back to API search
            if (hasValidUSDAApiKey()) {
                FileLogger.d("FoodDatabaseManager", "Bulk download failed, trying API search as fallback...")
                Log.d("FoodDatabaseManager", "Bulk download failed, trying API search as fallback...")
            } else {
                FileLogger.e("FoodDatabaseManager", "No valid API key and bulk download failed")
                Log.e("FoodDatabaseManager", "No valid API key and bulk download failed")
                return Result.failure(Exception("No valid API key available and bulk download failed"))
            }
            
            _downloadProgress.value = _downloadProgress.value.copy(
                currentDatabase = "USDA",
                currentOperation = "Fetching USDA food list..."
            )
            
            // Update status as downloading
            database.foodDatabaseStatusDao().insertStatus(
                FoodDatabaseStatus(
                    databaseName = "usda",
                    isDownloading = true,
                    totalItems = 0,
                    downloadedItems = 0
                )
            )
            
            Log.d("FoodDatabaseManager", "Database status updated to downloading")
            
            var totalDownloaded = 0
            @Suppress("UNUSED_VARIABLE") var currentPage = 1
            val pageSize = 25 // Standard page size for API searches
            
            // Download Foundation foods (most validated dataset)
            val dataTypes = listOf("Foundation")
            
            Log.d("FoodDatabaseManager", "Data types to download: $dataTypes")
            
            // Try search endpoint as fallback since list endpoint is failing with 500 errors
            FileLogger.addSeparator("Using Search Endpoint Fallback")
            FileLogger.d("FoodDatabaseManager", "List endpoint failing with 500 errors, trying search endpoint")
            
            // Use search with generic terms to get a broad dataset
            val searchTerms = listOf("chicken", "beef", "rice", "bread", "milk", "apple", "carrot", "pasta", "cheese", "fish")
            
            for (searchTerm in searchTerms) {
                FileLogger.addSeparator("Search API Call - $searchTerm")
                FileLogger.d("FoodDatabaseManager", "--- Starting search for: $searchTerm ---")
                _downloadProgress.value = _downloadProgress.value.copy(
                    currentOperation = "Searching USDA foods for '$searchTerm'..."
                )
                
                currentPage = 1
                var hasMorePages = true
                
                // Limit pages for search results
                val maxPages = 10
                FileLogger.d("FoodDatabaseManager", "Max pages for '$searchTerm': $maxPages")
                
                while (hasMorePages && currentPage <= maxPages) {
                    try {
                        FileLogger.addSeparator("Search API Call - $searchTerm Page $currentPage")
                        FileLogger.d("FoodDatabaseManager", "Making search API call for '$searchTerm' page $currentPage")
                        FileLogger.d("FoodDatabaseManager", "Search URL: https://api.nal.usda.gov/fdc/v1/foods/search?api_key=${getUSDAApiKey().take(10)}...&query=$searchTerm&pageSize=$pageSize&pageNumber=$currentPage")
                        
                        Log.d("FoodDatabaseManager", "Making search API call for '$searchTerm' page $currentPage")
                        Log.d("FoodDatabaseManager", "Search URL: https://api.nal.usda.gov/fdc/v1/foods/search?api_key=${getUSDAApiKey().take(10)}...&query=$searchTerm&pageSize=$pageSize&pageNumber=$currentPage")
                        
                        val response = usdaService.searchFoods(
                            apiKey = getUSDAApiKey(),
                            query = searchTerm,
                            pageSize = pageSize,
                            pageNumber = currentPage
                        )
                        
                        FileLogger.d("FoodDatabaseManager", "API call completed. Success: ${response.isSuccessful}, Code: ${response.code()}")
                        Log.d("FoodDatabaseManager", "API call completed. Success: ${response.isSuccessful}, Code: ${response.code()}")
                        
                        // Log raw response for debugging
                        val rawResponse = response.raw().toString()
                        FileLogger.d("FoodDatabaseManager", "Raw response: $rawResponse")
                        Log.d("FoodDatabaseManager", "Raw response: $rawResponse")
                        
                        if (!response.isSuccessful) {
                            val errorBody = response.errorBody()?.string()
                            FileLogger.e("FoodDatabaseManager", "API Error: ${response.code()} - ${response.message()}")
                            FileLogger.e("FoodDatabaseManager", "Error body: $errorBody")
                            FileLogger.e("FoodDatabaseManager", "Request URL: ${response.raw().request.url}")
                            FileLogger.e("FoodDatabaseManager", "Request headers: ${response.raw().request.headers}")
                            
                            Log.e("FoodDatabaseManager", "API Error: ${response.code()} - ${response.message()}")
                            Log.e("FoodDatabaseManager", "Error body: $errorBody")
                            Log.e("FoodDatabaseManager", "Request URL: ${response.raw().request.url}")
                            Log.e("FoodDatabaseManager", "Request headers: ${response.raw().request.headers}")
                        }
                        
                        val searchResult = if (response.isSuccessful) response.body() else null
                        if (searchResult != null) {
                            val foods = searchResult.foods
                            
                            FileLogger.addSeparator("Response Processing")
                            FileLogger.d("FoodDatabaseManager", "USDA Search API response: foods.size=${foods.size}, totalHits=${searchResult.totalHits}")
                            Log.d("FoodDatabaseManager", "USDA Search API response: foods.size=${foods.size}, totalHits=${searchResult.totalHits}")
                            
                            // DEBUG: Log response body as string to see structure
                            try {
                                val responseBody = response.raw().body?.string()
                                FileLogger.writeRaw("Search response body preview: ${responseBody?.take(1000)}...")
                                Log.d("FoodDatabaseManager", "Search response body preview: ${responseBody?.take(500)}...")
                            } catch (e: Exception) {
                                FileLogger.w("FoodDatabaseManager", "Could not read response body for debugging: ${e.message}")
                                Log.w("FoodDatabaseManager", "Could not read response body for debugging: ${e.message}")
                            }
                            
                            if (foods.isEmpty()) {
                                FileLogger.w("FoodDatabaseManager", "No foods in search response for '$searchTerm' page $currentPage")
                                Log.w("FoodDatabaseManager", "No foods in search response for '$searchTerm' page $currentPage")
                                hasMorePages = false
                                continue
                            }
                            
                            // Debug: Log the first food item structure
                            foods.firstOrNull()?.let { firstFood ->
                                Log.d("FoodDatabaseManager", "First food raw data:")
                                Log.d("FoodDatabaseManager", "  fdcId: ${firstFood.fdcId}")
                                Log.d("FoodDatabaseManager", "  description: ${firstFood.description}")
                                Log.d("FoodDatabaseManager", "  foodNutrients count: ${firstFood.foodNutrients?.size}")
                                
                                // Log some key nutrients we're looking for
                                firstFood.foodNutrients?.forEach { nutrient ->
                                    if (nutrient.number in listOf("957", "958", "203", "204", "205")) {
                                        Log.d("FoodDatabaseManager", "  Nutrient ${nutrient.number} (${nutrient.name}): ${nutrient.amount} ${nutrient.unitName}")
                                    }
                                }
                            }
                            
                            // Convert and save to database
                            val usdaFoods = foods.map { food ->
                                convertToUSDAFoodItem(food)
                            }
                            
                            Log.d("FoodDatabaseManager", "Converting ${foods.size} foods, first item: ${foods.firstOrNull()?.description}")
                            Log.d("FoodDatabaseManager", "Converted to ${usdaFoods.size} USDA items, first calories: ${usdaFoods.firstOrNull()?.calories}")
                            
                            // Debug: Check if the conversion worked
                            usdaFoods.firstOrNull()?.let { firstConverted ->
                                Log.d("FoodDatabaseManager", "First converted food:")
                                Log.d("FoodDatabaseManager", "  fdcId: ${firstConverted.fdcId}")
                                Log.d("FoodDatabaseManager", "  description: ${firstConverted.description}")
                                Log.d("FoodDatabaseManager", "  calories: ${firstConverted.calories}")
                                Log.d("FoodDatabaseManager", "  protein: ${firstConverted.protein}")
                                Log.d("FoodDatabaseManager", "  fat: ${firstConverted.fat}")
                                Log.d("FoodDatabaseManager", "  carbohydrates: ${firstConverted.carbohydrates}")
                            }
                            
                            try {
                                database.usdaFoodItemDao().insertFoods(usdaFoods)
                                Log.d("FoodDatabaseManager", "Successfully inserted ${usdaFoods.size} USDA foods to database")
                            } catch (e: Exception) {
                                Log.e("FoodDatabaseManager", "Failed to insert USDA foods to database", e)
                                throw e
                            }
                            
                            totalDownloaded += usdaFoods.size
                            
                            _downloadProgress.value = _downloadProgress.value.copy(
                                downloadedItems = totalDownloaded,
                                totalItems = totalDownloaded, // No total from API, so use downloaded count
                                currentOperation = "Downloaded $totalDownloaded USDA foods (searching '$searchTerm')..."
                            )
                            
                            // Update notification with progress
                            notificationManager.updateProgress(
                                databaseName = "USDA Database",
                                currentItems = totalDownloaded,
                                totalItems = totalDownloaded + 500, // Estimate total as we don't know exact count
                                currentOperation = "Downloaded $totalDownloaded USDA foods..."
                            )
                            
                            // Update status
                            database.foodDatabaseStatusDao().insertStatus(
                                FoodDatabaseStatus(
                                    databaseName = "usda",
                                    isDownloading = true,
                                    totalItems = totalDownloaded,
                                    downloadedItems = totalDownloaded
                                )
                            )
                            
                            // If we get fewer items than requested, we've reached the end
                            if (foods.size < pageSize) {
                                hasMorePages = false
                            }
                            
                            currentPage++
                            
                            // Rate limiting for API requests
                            val delayMs = 25L
                            delay(delayMs)
                            
                        } else {
                            val errorMsg = "USDA API error: ${response.code()} - ${response.message()}"
                            Log.e("FoodDatabaseManager", errorMsg)
                            Log.e("FoodDatabaseManager", "Error body: ${response.errorBody()?.string()}")
                            
                            // Show error in notification for user feedback
                            notificationManager.showDownloadFailed("USDA Database", "API Error: ${response.code()}")
                            
                            _downloadProgress.value = _downloadProgress.value.copy(
                                currentOperation = errorMsg,
                                error = errorMsg
                            )
                            hasMorePages = false
                        }
                    } catch (e: Exception) {
                        FileLogger.addSeparator("Exception Caught")
                        FileLogger.e("FoodDatabaseManager", "Error downloading USDA page $currentPage: ${e.message}", e)
                        FileLogger.e("FoodDatabaseManager", "Exception type: ${e.javaClass.simpleName}")
                        
                        Log.e("FoodDatabaseManager", "Error downloading USDA page $currentPage: ${e.message}", e)
                        Log.e("FoodDatabaseManager", "Exception type: ${e.javaClass.simpleName}")
                        Log.e("FoodDatabaseManager", "Stack trace: ${e.stackTraceToString()}")
                        
                        // Check if it's a JSON parsing error
                        if (e.message?.contains("json", ignoreCase = true) == true ||
                            e.message?.contains("parse", ignoreCase = true) == true ||
                            e.message?.contains("deserializ", ignoreCase = true) == true) {
                            FileLogger.e("FoodDatabaseManager", "JSON parsing error detected - API response structure may have changed")
                            Log.e("FoodDatabaseManager", "JSON parsing error detected - API response structure may have changed")
                        }
                        
                        // Check if it's a rate limit error (common with DEMO_KEY)
                        if (e.message?.contains("429", ignoreCase = true) == true || 
                            e.message?.contains("rate limit", ignoreCase = true) == true) {
                            
                            val errorMsg = "API rate limit exceeded. Please wait before trying again or get a free API key from https://fdc.nal.usda.gov/api-key-signup.html"
                            
                            database.foodDatabaseStatusDao().insertStatus(
                                FoodDatabaseStatus(
                                    databaseName = "usda",
                                    isDownloading = false,
                                    errorMessage = errorMsg
                                )
                            )
                            
                            throw Exception(errorMsg)
                        }
                        
                        hasMorePages = false
                    }
                }
            }
            
            // Mark USDA as complete
            Log.d("FoodDatabaseManager", "USDA download completed with $totalDownloaded total items")
            database.foodDatabaseStatusDao().insertStatus(
                FoodDatabaseStatus(
                    databaseName = "usda",
                    lastDownloadDate = System.currentTimeMillis(),
                    totalItems = totalDownloaded,
                    downloadedItems = totalDownloaded,
                    isDownloading = false,
                    isComplete = true
                )
            )
            
            Log.d("FoodDatabaseManager", "=== USDA Download Completed Successfully ===")
            Result.success(Unit)
        } catch (e: Exception) {
            database.foodDatabaseStatusDao().insertStatus(
                FoodDatabaseStatus(
                    databaseName = "usda",
                    isDownloading = false,
                    errorMessage = e.message
                )
            )
            Result.failure(e)
        }
    }
    
    /**
     * Download Open Food Facts database (comprehensive barcode coverage)
     * 
     * 🚨 ENHANCED FOR COMPREHENSIVE BARCODE SCANNING
     * 
     * **BARCODE SCANNING REQUIREMENTS:**
     * The user wants to scan "all their foods" - this means we need maximum barcode coverage,
     * not just popular categories. Enhanced approach:
     * 
     * 1. **Comprehensive Category Coverage**: Download from ALL major food categories
     * 2. **Geographic Coverage**: Include products from multiple countries (US, UK, France, etc.)
     * 3. **Brand Coverage**: Target major brands and store brands users actually buy
     * 4. **Recent Products**: Focus on recently added/updated products for current market coverage
     * 5. **Increased Limits**: Download more pages per category for deeper coverage
     * 
     * **TARGET GOAL**: 50,000+ products instead of ~5,000 for real-world barcode scanning
     */
    suspend fun downloadOpenFoodFactsDatabase(): Result<Unit> {
        if (!isOFFDownloadRunning.compareAndSet(false, true)) {
            Log.w("FoodDatabaseManager", "OFFs download already in progress — skipping duplicate")
            return Result.failure(IllegalStateException("Download already in progress"))
        }
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK,
            "CalorieTracker:OFFsDownload"
        ).also { it.acquire(6 * 60 * 60 * 1000L) } // max 6 hours
        return try {
            Log.d("FoodDatabaseManager", "=== Starting COMPREHENSIVE Open Food Facts Download ===")
            Log.d("FoodDatabaseManager", "🎯 Goal: Maximum barcode coverage for real-world grocery scanning")
            
            _downloadProgress.value = _downloadProgress.value.copy(
                currentDatabase = "Open Food Facts",
                currentOperation = "Downloading comprehensive barcode database..."
            )
            
            // Update status as downloading
            database.foodDatabaseStatusDao().insertStatus(
                FoodDatabaseStatus(
                    databaseName = "openfoodfacts",
                    isDownloading = true,
                    totalItems = 0,
                    downloadedItems = 0
                )
            )
            
            // Checkpoint: only used to resume after a mid-run crash or cancellation.
            // A deliberate new download (user-triggered) clears the checkpoint first so
            // fresh products are always picked up — the REPLACE conflict strategy handles dedup.
            val prefs = context.getSharedPreferences("offs_download_checkpoint", Context.MODE_PRIVATE)
            val checkpointCatIndex = prefs.getInt("cat_index", -1)
            val checkpointPage    = prefs.getInt("page", 1)
            val isResume = checkpointCatIndex >= 0
            var totalDownloaded   = database.openFoodFactsDao().getCount()
            if (isResume) {
                Log.d("FoodDatabaseManager", "▶️ Resuming from checkpoint: cat $checkpointCatIndex page $checkpointPage ($totalDownloaded products already in DB)")
            } else {
                Log.d("FoodDatabaseManager", "🆕 Fresh download — no checkpoint")
            }

            // ─────────────────────────────────────────────────────────────────
            // Phase 1: Category-based bulk download using the OFFs v2 API.
            //
            // Country-based queries (countriesTags=en:united-states) always
            // return HTTP 503 — OFFs server refuses large country datasets.
            //
            // Instead we iterate through 65 food categories. No country filter
            // needed: products in these categories are the same ones sold in US
            // grocery stores and have the same barcodes worldwide.
            //
            // 100 products/page × 50 pages × 65 categories = up to 325,000 products.
            // Room's conflict strategy deduplicates on barcode — safe to re-run.
            // ─────────────────────────────────────────────────────────────────
            val phase1Categories = listOf(
                // 🥤 Beverages
                "en:beverages", "en:sodas", "en:waters", "en:fruit-juices",
                "en:energy-drinks", "en:sports-drinks", "en:coffees", "en:teas",
                "en:plant-based-milks", "en:milks", "en:fruit-based-beverages",

                // 🍫 Snacks & Confections
                "en:snacks", "en:chips-and-crisps", "en:chocolates", "en:candies",
                "en:cookies", "en:biscuits-and-cakes", "en:cereal-bars",
                "en:protein-bars", "en:gummies",

                // 🧀 Dairy
                "en:dairy-products", "en:cheeses", "en:yogurts",
                "en:ice-creams-and-sorbets", "en:butters", "en:creams",

                // 🍞 Grains & Breads
                "en:breads", "en:pastas", "en:breakfast-cereals",
                "en:crackers", "en:tortillas", "en:flours-and-starch",
                "en:baking-mixes",

                // 🥩 Proteins
                "en:meats-and-their-products", "en:deli-meats",
                "en:seafood", "en:plant-based-foods",

                // 🍕 Frozen & Prepared
                "en:frozen-foods", "en:ready-to-eat-meals",
                "en:soups", "en:pizzas",

                // 🧴 Condiments & Sauces
                "en:condiments", "en:sauces", "en:ketchups", "en:mustards",
                "en:mayonnaises", "en:hot-sauces", "en:salad-dressings",
                "en:pasta-sauces", "en:vinegars",

                // 🥫 Preserved & Canned
                "en:canned-foods", "en:jams-and-marmalades",
                "en:pickled-products",

                // 🥗 Fruits, Vegetables & Nuts
                "en:nuts-and-their-products", "en:dried-fruits", "en:seeds",
                "en:fruits", "en:vegetables",

                // 🍼 Specialty
                "en:baby-foods", "en:organic-foods",
                "en:gluten-free-foods", "en:vegan-foods",

                // 🫒 Oils & Spreads
                "en:oils-and-fats", "en:spreads", "en:dips",

                // 🧂 Seasonings
                "en:spices-and-herbs", "en:sugars", "en:sweeteners",

                // 🍬 Confectionery
                "en:sweets"
            )

            Log.d("FoodDatabaseManager", "📦 Phase 1: category-based download (${phase1Categories.size} categories, up to 50 pages each)")

            for ((catIndex, categoryTag) in phase1Categories.withIndex()) {
                // Skip categories already fully completed in a resumed run
                if (isResume && catIndex < checkpointCatIndex) {
                    Log.d("FoodDatabaseManager", "⏭️ Skipping already-completed category ${catIndex + 1}/${phase1Categories.size}: $categoryTag")
                    continue
                }

                Log.d("FoodDatabaseManager", "📦 Category ${catIndex + 1}/${phase1Categories.size}: $categoryTag")
                var consecutiveEmpty = 0
                // Resume at the saved page for the checkpoint category, otherwise start at 1
                var page = if (isResume && catIndex == checkpointCatIndex) checkpointPage else 1
                var pageRetries = 0
                var consecutive503Pages = 0

                while (page <= 20) {
                    try {
                        _downloadProgress.value = _downloadProgress.value.copy(
                            currentOperation = "[$categoryTag] page $page • $totalDownloaded products total"
                        )
                        val response = openFoodFactsService.searchProductsV2(
                            categoriesTags = categoryTag,
                            pageSize = 200,
                            page = page
                        )
                        when {
                            response.code() == 429 -> {
                                Log.w("FoodDatabaseManager", "Rate limited on $categoryTag page $page — backing off 30s")
                                delay(30_000)
                                // don't increment page — retry same page
                            }
                            response.code() == 503 -> {
                                pageRetries++
                                if (pageRetries >= 4) {
                                    consecutive503Pages++
                                    Log.w("FoodDatabaseManager", "503 persistent on $categoryTag page $page — skipping page ($consecutive503Pages consecutive 503 pages)")
                                    if (consecutive503Pages >= 5) {
                                        Log.w("FoodDatabaseManager", "⚠️ $categoryTag throttled hard — skipping rest of category after $consecutive503Pages 503-pages")
                                        break
                                    }
                                    page++
                                    pageRetries = 0
                                } else {
                                    val backoff = 15_000L * pageRetries
                                    Log.w("FoodDatabaseManager", "503 on $categoryTag page $page (attempt $pageRetries) — backing off ${backoff / 1000}s")
                                    delay(backoff)
                                }
                            }
                            response.isSuccessful -> {
                                // If we just recovered from a 503 streak, give the server
                                // a longer breather before resuming full-speed requests.
                                if (consecutive503Pages > 0) {
                                    delay(5_000)
                                }
                                pageRetries = 0
                                consecutive503Pages = 0
                                val products = response.body()?.products
                                if (!products.isNullOrEmpty()) {
                                    val offFoods = products.mapNotNull { convertToOpenFoodFactsItem(it) }
                                    if (offFoods.isNotEmpty()) {
                                        database.openFoodFactsDao().insertFoods(offFoods)
                                        totalDownloaded += offFoods.size
                                        Log.d("FoodDatabaseManager", "✅ Added ${offFoods.size} from $categoryTag (page $page) • total: $totalDownloaded")
                                        // Save checkpoint so a crash/cancel can resume here.
                                        // Use commit() (synchronous) so the write reaches disk
                                        // immediately — apply() is async and gets lost when the
                                        // process is killed by adb install or the OS freezer.
                                        prefs.edit()
                                            .putInt("cat_index", catIndex)
                                            .putInt("page", page + 1)
                                            .commit()
                                        database.foodDatabaseStatusDao().insertStatus(
                                            FoodDatabaseStatus(
                                                databaseName = "openfoodfacts",
                                                isDownloading = true,
                                                downloadedItems = totalDownloaded
                                            )
                                        )
                                    }
                                    consecutiveEmpty = 0
                                    _downloadProgress.value = _downloadProgress.value.copy(
                                        downloadedItems = totalDownloaded
                                    )
                                } else {
                                    consecutiveEmpty++
                                    if (consecutiveEmpty >= 3) break
                                }
                                page++
                                delay(600)
                            }
                            else -> {
                                pageRetries = 0
                                consecutiveEmpty++
                                if (consecutiveEmpty >= 5) break
                                page++
                                delay(600)
                            }
                        }
                    } catch (e: Exception) {
                        val isNetworkError = e is java.net.UnknownHostException ||
                            e.cause is java.net.UnknownHostException ||
                            e is java.net.ConnectException ||
                            e is java.net.SocketException ||
                            e.cause is java.net.SocketException
                        if (isNetworkError) {
                            Log.w("FoodDatabaseManager", "📡 Network down on $categoryTag page $page — waiting 30s before retry")
                            delay(30_000)
                            // don't advance page — retry same page when network recovers
                        } else {
                            Log.w("FoodDatabaseManager", "Error on $categoryTag page $page: ${e.message}")
                            delay(2000)
                            page++
                            pageRetries = 0
                        }
                    }
                }
                delay(1500) // pause between categories to avoid rate limiting
            }

            
            // 🏪 COMPREHENSIVE BRAND COVERAGE FOR REAL-WORLD BARCODE SCANNING
            // This targets major brands and store brands that users actually buy
            Log.d("FoodDatabaseManager", "🏪 Starting comprehensive brand coverage for barcode scanning...")
            
            val comprehensiveBrandSearches = listOf(
                // 🥤 MAJOR BEVERAGE BRANDS
                "coca-cola", "pepsi", "dr-pepper", "sprite", "fanta", "mountain-dew",
                "red-bull", "monster", "gatorade", "powerade", "vitamin-water",
                
                // 🍫 SNACK & CANDY BRANDS
                "nestle", "mars", "ferrero", "hershey", "cadbury", "lindt", "godiva",
                "oreo", "chips-ahoy", "pringles", "lay's", "doritos", "cheetos",
                "ritz", "nabisco", "pepperidge-farm", "keebler", "frito-lay",
                
                // 🥣 CEREAL & BREAKFAST BRANDS
                "kellogg", "general-mills", "post", "quaker", "nature-valley",
                "cheerios", "frosted-flakes", "lucky-charms", "honey-nut-cheerios",
                
                // 🧀 DAIRY & FOOD BRANDS
                "kraft", "philadelphia", "velveeta", "oscar-mayer", "hillshire-farm",
                "tyson", "perdue", "campbell's", "progresso", "heinz", "hunt's",
                
                // 🛒 MAJOR STORE BRANDS (users scan these frequently)
                "great-value", "kirkland", "365-everyday-value", "trader-joe's",
                "simply-balanced", "market-pantry", "up-up", "equate",
                
                // 🌍 INTERNATIONAL BRANDS (for diverse communities)
                "unilever", "danone", "barilla", "bertolli", "ragu", "prego",
                "old-el-paso", "ortega", "la-choy", "kikkoman", "lee-kum-kee",
                
                // 🥛 HEALTH & ORGANIC BRANDS
                "organic-valley", "horizon", "silk", "almond-breeze", "oat-dream",
                "annie's", "amy's", "cascadian-farm", "earth-balance", "gardein",
                
                // 🍕 FROZEN & CONVENIENCE BRANDS  
                "stouffer's", "lean-cuisine", "healthy-choice", "marie-callender's",
                "hot-pockets", "bagel-bites", "pizza-rolls", "red-baron", "digiorno",
                
                // 🧴 CONDIMENT & SAUCE BRANDS
                "hellmann's", "best-foods", "french's", "grey-poupon", "a1",
                "tabasco", "cholula", "sriracha", "frank's-redhot", "hidden-valley"
            )
            
            // 🔍 ENHANCED BRAND SEARCH WITH DEEPER PAGINATION
            var consecutiveNetworkErrors = 0

            for ((brandIndex, searchTerm) in comprehensiveBrandSearches.withIndex()) {
                var brandAttempt = 0
                var brandDone = false

                while (!brandDone && brandAttempt < 3) {
                    try {
                        _downloadProgress.value = _downloadProgress.value.copy(
                            currentOperation = "Downloading $searchTerm products... (${brandIndex + 1}/${comprehensiveBrandSearches.size})"
                        )

                        Log.d("FoodDatabaseManager", "🔍 Searching brand: $searchTerm (${brandIndex + 1}/${comprehensiveBrandSearches.size})")

                        // Download multiple pages for each brand for better coverage
                        for (page in 1..5) {
                            val response = openFoodFactsService.searchProducts(
                                searchTerms = searchTerm,
                                pageSize = 100,
                                page = page
                            )
                            if (response.code() == 429) {
                                Log.w("FoodDatabaseManager", "Rate limited on brand $searchTerm, backing off")
                                delay(5000)
                                break
                            }
                            val products = if (response.isSuccessful) response.body()?.products else null
                            if (!products.isNullOrEmpty()) {
                                val offFoods = products.mapNotNull { convertToOpenFoodFactsItem(it) }
                                if (offFoods.isNotEmpty()) {
                                    database.openFoodFactsDao().insertFoods(offFoods)
                                    totalDownloaded += offFoods.size
                                    Log.d("FoodDatabaseManager", "✅ Added ${offFoods.size} from $searchTerm (page $page)")
                                } else {
                                    break
                                }
                            } else {
                                break
                            }
                            delay(500)
                        }
                        consecutiveNetworkErrors = 0
                        brandDone = true
                    } catch (e: Exception) {
                        val isNetworkError = e is java.net.UnknownHostException ||
                            e.cause is java.net.UnknownHostException ||
                            e is java.net.ConnectException ||
                            e is java.net.SocketException
                        if (isNetworkError) {
                            brandAttempt++
                            consecutiveNetworkErrors++
                            if (brandAttempt < 3) {
                                Log.w("FoodDatabaseManager", "Network error on brand $searchTerm, retrying in 5s (attempt $brandAttempt)")
                                delay(5_000L)
                            }
                        } else {
                            Log.e("FoodDatabaseManager", "Error downloading brand $searchTerm: ${e.message}")
                            brandDone = true
                        }
                    }
                }
                if (!brandDone) {
                    Log.w("FoodDatabaseManager", "Skipping brand $searchTerm after 3 failed network attempts")
                }
            }
            
            // 🇺🇸 US COVERAGE SUPPLEMENT - extra pages of US products beyond Phase 1
            Log.d("FoodDatabaseManager", "🇺🇸 Supplementing US product coverage...")

            val countrySearches = listOf("en:united-states")
            for (country in countrySearches) {
                try {
                    _downloadProgress.value = _downloadProgress.value.copy(
                        currentOperation = "Downloading $country products for geographic coverage..."
                    )

                    // Use v2 API with countriesTags for proper country filtering
                    for (page in 1..3) {
                        val response = openFoodFactsService.searchProductsV2(
                            countriesTags = country,
                            pageSize = 200,
                            page = page
                        )

                        if (response.code() == 429) {
                            Log.w("FoodDatabaseManager", "Rate limited on country $country, backing off")
                            delay(5000)
                            break
                        }
                        val products = if (response.isSuccessful) response.body()?.products else null
                        if (!products.isNullOrEmpty()) {
                            val offFoods = products.mapNotNull { convertToOpenFoodFactsItem(it) }
                            if (offFoods.isNotEmpty()) {
                                database.openFoodFactsDao().insertFoods(offFoods)
                                totalDownloaded += offFoods.size
                                Log.d("FoodDatabaseManager", "🌍 Added ${offFoods.size} from $country (page $page)")
                            }
                        } else {
                            break
                        }
                        delay(800)
                    }
                } catch (e: Exception) {
                    Log.e("FoodDatabaseManager", "Error downloading products from $country: ${e.message}")
                }
            }
            
            // Mark Open Food Facts as complete
            database.foodDatabaseStatusDao().insertStatus(
                FoodDatabaseStatus(
                    databaseName = "openfoodfacts",
                    lastDownloadDate = System.currentTimeMillis(),
                    totalItems = totalDownloaded,
                    downloadedItems = totalDownloaded,
                    isDownloading = false,
                    isComplete = true
                )
            )
            
            // Clear checkpoint — download finished cleanly; next run starts fresh
            prefs.edit().clear().apply()

            Log.d("FoodDatabaseManager", "=== Open Food Facts Download Completed Successfully ===")
            Log.d("FoodDatabaseManager", "🎯 Total downloaded: $totalDownloaded products")
            Log.d("FoodDatabaseManager", "✅ Your barcode scanning should now work with most grocery store items!")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FoodDatabaseManager", "=== Open Food Facts Download Failed ===", e)
            
            database.foodDatabaseStatusDao().insertStatus(
                FoodDatabaseStatus(
                    databaseName = "openfoodfacts",
                    isDownloading = false,
                    isComplete = false,
                    errorMessage = e.message ?: "Unknown error",
                    totalItems = 0,
                    downloadedItems = 0,
                    lastDownloadDate = null
                )
            )
            
            _downloadProgress.value = _downloadProgress.value.copy(
                currentOperation = "Open Food Facts download failed: ${e.message}",
                error = e.message
            )
            
            Result.failure(e)
        } finally {
            isOFFDownloadRunning.set(false)
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    /**
     * Convert USDA API response to database entity
     */
    private fun convertToUSDAFoodItem(food: USDAFoodBasic): USDAFoodItem {
        // Extract nutrients by number (string identifier)
        val nutrients = food.foodNutrients?.associateBy { it.number } ?: emptyMap()
        
        Log.d("FoodDatabaseManager", "Converting food: ${food.description}")
        Log.d("FoodDatabaseManager", "  Total nutrients: ${nutrients.size}")
        Log.d("FoodDatabaseManager", "  Looking for energy (957): ${nutrients["957"]?.amount}")
        Log.d("FoodDatabaseManager", "  Looking for energy (958): ${nutrients["958"]?.amount}")
        Log.d("FoodDatabaseManager", "  Looking for protein (203): ${nutrients["203"]?.amount}")
        
        return USDAFoodItem(
            fdcId = food.fdcId,
            description = food.description,
            dataType = food.dataType,
            publicationDate = food.publicationDate,
            brandOwner = food.brandOwner,
            brandName = food.brandName,
            ingredients = food.ingredients,
            servingSize = food.servingSize,
            servingSizeUnit = food.servingSizeUnit,
            householdServingFullText = food.householdServingFullText,
            
            // Nutrition per 100g (USDA nutrient numbers)
            calories = nutrients["957"]?.amount ?: nutrients["958"]?.amount ?: 0.0, // Energy (Atwater)
            protein = nutrients["203"]?.amount ?: 0.0, // Protein
            fat = nutrients["204"]?.amount ?: 0.0, // Total lipid (fat)
            carbohydrates = nutrients["205"]?.amount ?: 0.0, // Carbohydrate
            fiber = nutrients["291"]?.amount ?: 0.0, // Fiber
            sugar = nutrients["269.3"]?.amount ?: 0.0, // Total sugars
            sodium = nutrients["307"]?.amount ?: 0.0, // Sodium
            calcium = nutrients["301"]?.amount, // Calcium
            iron = nutrients["303"]?.amount, // Iron
            vitaminC = nutrients["401"]?.amount, // Vitamin C
            cholesterol = nutrients["601"]?.amount, // Cholesterol
            saturatedFat = nutrients["606"]?.amount, // Saturated fatty acids
            transFat = nutrients["605"]?.amount, // Trans fatty acids
            
            foodCategory = food.foodCategory
        )
    }
    
    /**
     * Convert Open Food Facts API response to database entity
     */
    private fun convertToOpenFoodFactsItem(product: OpenFoodFactsProduct): OpenFoodFactsItem? {
        val barcode = product.code ?: return null
        val productName = product.productName ?: return null
        
        return OpenFoodFactsItem(
            id = barcode,
            barcode = barcode,
            productName = productName,
            brands = product.brands,
            categories = product.categories,
            labels = product.labels,
            countries = product.countries,
            ingredients = product.ingredientsText,
            allergens = product.allergens,
            
            servingSize = product.servingSize,
            servingQuantity = product.servingQuantity,
            
            // Nutrition per 100g
            energyKj = product.nutriments?.energyKj100g,
            energyKcal = product.nutriments?.energyKcal100g,
            fat = product.nutriments?.fat100g,
            saturatedFat = product.nutriments?.saturatedFat100g,
            carbohydrates = product.nutriments?.carbohydrates100g,
            sugars = product.nutriments?.sugars100g,
            fiber = product.nutriments?.fiber100g,
            proteins = product.nutriments?.proteins100g,
            salt = product.nutriments?.salt100g,
            sodium = product.nutriments?.sodium100g,
            
            imageUrl = product.imageUrl,
            productUrl = product.url,
            nutritionGrade = product.nutriscoreGrade,
            novaGroup = product.novaGroup,
            completeness = product.completeness
        )
    }
    
    /**
     * Get download status for both databases
     */
    fun getDownloadStatuses(): Flow<List<FoodDatabaseStatus>> {
        return database.foodDatabaseStatusDao().getAllStatusesFlow()
    }
    
    /**
     * Register the cancel receiver when starting downloads
     */
    private fun registerCancelReceiver() {
        if (!isReceiverRegistered) {
            try {
                val filter = IntentFilter("com.calorietracker.ACTION_CANCEL_DOWNLOAD")
                androidx.core.content.ContextCompat.registerReceiver(
                    context, cancelReceiver, filter,
                    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
                )
                isReceiverRegistered = true
                Log.d("FoodDatabaseManager", "Cancel receiver registered")
            } catch (e: Exception) {
                Log.e("FoodDatabaseManager", "Failed to register cancel receiver", e)
            }
        }
    }
    
    /**
     * Unregister the cancel receiver
     */
    private fun unregisterCancelReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(cancelReceiver)
                isReceiverRegistered = false
                Log.d("FoodDatabaseManager", "Cancel receiver unregistered")
            } catch (e: Exception) {
                Log.e("FoodDatabaseManager", "Failed to unregister cancel receiver", e)
            }
        }
    }

    /**
     * Cancel ongoing download (explicit user action — also cancels the persistent scope job)
     */
    fun cancelDownload() {
        Log.d("FoodDatabaseManager", "Cancelling download...")
        downloadJob?.cancel()
        isOFFDownloadRunning.set(false)
        notificationManager.hideNotification()
        unregisterCancelReceiver()
        _downloadProgress.value = DownloadProgress()
    }
    
    fun resetDownloadProgress() {
        Log.d("FoodDatabaseManager", "Resetting download progress...")
        downloadJob?.cancel()
        _downloadProgress.value = DownloadProgress()
        notificationManager.hideNotification()
    }
    
    /**
     * Download only USDA database
     */
    suspend fun downloadUSDADatabaseOnly(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Initialize file logging for this debug session
                FileLogger.initializeLogging(context, "usda_download_debug")
                FileLogger.d("FoodDatabaseManager", "=== Starting USDA-only download ===")
                FileLogger.d("FoodDatabaseManager", "API Key configured: ${if (getUSDAApiKey().isBlank()) "None (using bulk downloads)" else "Yes (${getUSDAApiKey().take(10)}...)"}")
                
                Log.d("FoodDatabaseManager", "=== Starting USDA-only download ===")
                registerCancelReceiver()
                notificationManager.showDownloadStarted("USDA Database")
                
                _downloadProgress.value = _downloadProgress.value.copy(
                    isDownloading = true,
                    isComplete = false,
                    error = null
                )
                
                val result = downloadUSDADatabase()
                
                if (result.isSuccess) {
                    val finalCount = database.usdaFoodItemDao().getCount()
                    notificationManager.showDownloadCompleted("USDA Database", finalCount)
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    notificationManager.showDownloadFailed("USDA Database", errorMessage)
                }
                
                _downloadProgress.value = _downloadProgress.value.copy(
                    isDownloading = false,
                    isComplete = result.isSuccess,
                    currentOperation = if (result.isSuccess) "USDA download completed!" else "USDA download failed"
                )
                
                Log.d("FoodDatabaseManager", "=== USDA-only download result: ${if (result.isSuccess) "SUCCESS" else "FAILURE"} ===")
                
                // Close file logging and show user where to find the logs
                FileLogger.addSeparator("Download Complete")
                FileLogger.d("FoodDatabaseManager", "Final result: ${if (result.isSuccess) "SUCCESS" else "FAILURE"}")
                val logPath = FileLogger.getCurrentLogFilePath()
                FileLogger.d("FoodDatabaseManager", "Debug log saved to: $logPath")
                FileLogger.closeLogging()
                
                Log.d("FoodDatabaseManager", "Debug log saved to: $logPath")
                
                unregisterCancelReceiver()
                result
            } catch (e: Exception) {
                FileLogger.addSeparator("Fatal Exception")
                FileLogger.e("FoodDatabaseManager", "=== USDA-only download failed with exception ===", e)
                val logPath = FileLogger.getCurrentLogFilePath()
                FileLogger.closeLogging()
                
                Log.e("FoodDatabaseManager", "=== USDA-only download failed with exception ===", e)
                Log.d("FoodDatabaseManager", "Debug log saved to: $logPath")
                notificationManager.showDownloadFailed("USDA Database", e.message ?: "Unknown error")
                unregisterCancelReceiver()
                _downloadProgress.value = _downloadProgress.value.copy(
                    isDownloading = false,
                    error = e.message
                )
                Result.failure(e)
            }
        }
    }
    
    /**
     * Download only Open Food Facts database.
     * Runs on a persistent scope so screen-off / Activity destruction won't cancel it.
     */
    suspend fun downloadOpenFoodFactsDatabaseOnly(): Result<Unit> {
        // Launch on downloadScope (not the caller's lifecycle scope)
        // Clear any stale checkpoint so this user-triggered run starts fresh and picks up new products
        context.getSharedPreferences("offs_download_checkpoint", Context.MODE_PRIVATE)
            .edit().clear().apply()

        val deferred = downloadScope.async {
            try {
                Log.d("FoodDatabaseManager", "=== Starting Open Food Facts-only download ===")
                registerCancelReceiver()
                notificationManager.showDownloadStarted("Open Food Facts Database")

                _downloadProgress.value = _downloadProgress.value.copy(
                    isDownloading = true,
                    isComplete = false,
                    error = null
                )

                val result = downloadOpenFoodFactsDatabase()

                if (result.isSuccess) {
                    val finalCount = database.openFoodFactsDao().getCount()
                    notificationManager.showDownloadCompleted("Open Food Facts Database", finalCount)
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    notificationManager.showDownloadFailed("Open Food Facts Database", errorMessage)
                }

                _downloadProgress.value = _downloadProgress.value.copy(
                    isDownloading = false,
                    isComplete = result.isSuccess,
                    currentOperation = if (result.isSuccess) "Open Food Facts download completed!" else "Open Food Facts download failed"
                )

                Log.d("FoodDatabaseManager", "=== Open Food Facts-only download result: ${if (result.isSuccess) "SUCCESS" else "FAILURE"} ===")
                unregisterCancelReceiver()
                result
            } catch (e: Exception) {
                Log.e("FoodDatabaseManager", "=== Open Food Facts-only download failed with exception ===", e)
                notificationManager.showDownloadFailed("Open Food Facts Database", e.message ?: "Unknown error")
                unregisterCancelReceiver()
                _downloadProgress.value = _downloadProgress.value.copy(
                    isDownloading = false,
                    error = e.message
                )
                Result.failure(e)
            }
        }
        downloadJob = deferred

        // Await result — if the calling Activity scope is cancelled the deferred keeps running;
        // the caller just won't receive the Result (progress is observable via downloadProgress flow).
        return try {
            deferred.await()
        } catch (e: CancellationException) {
            Log.d("FoodDatabaseManager", "Caller scope cancelled — download continues in background")
            Result.success(Unit)
        }
    }

    /**
     * Check if databases are downloaded and up to date
     */
    suspend fun areDatabasesReady(): Boolean {
        val usdaStatus = database.foodDatabaseStatusDao().getStatus("usda")
        val offStatus = database.foodDatabaseStatusDao().getStatus("openfoodfacts")
        
        return (usdaStatus?.isComplete == true && usdaStatus.downloadedItems > 0) &&
               (offStatus?.isComplete == true && offStatus.downloadedItems > 0)
    }
    
    // ── Pre-built Database Download ────────────────────────────────────────────

    /** Metadata returned by [checkForPrebuiltDatabase] when an update is available. */
    data class PrebuiltDatabaseInfo(
        val version: String,
        val downloadUrl: String,
        val fileSizeBytes: Long,
        val productCount: Int
    )

    /**
     * Fetches the hosted version JSON and returns update info if a newer pre-built
     * database is available, or null if already up-to-date or the check failed.
     */
    suspend fun checkForPrebuiltDatabase(versionUrl: String): PrebuiltDatabaseInfo? =
        withContext(Dispatchers.IO) {
            try {
                val conn = java.net.URL(versionUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout    = 15_000
                conn.setRequestProperty("User-Agent", "CalorieTrackerApp/3.1 (Android)")
                if (conn.responseCode != 200) {
                    Log.w("FoodDatabaseManager", "Version check HTTP ${conn.responseCode}")
                    return@withContext null
                }
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json        = org.json.JSONObject(body)
                val remoteVer   = json.optString("version", "none")
                val downloadUrl = json.optString("download_url", "")
                val fileSize    = json.optLong("file_size_bytes", 0L)
                val count       = json.optInt("product_count", 0)

                if (downloadUrl.isBlank() || downloadUrl.contains("PLACEHOLDER")) {
                    Log.d("FoodDatabaseManager", "No pre-built database published yet")
                    return@withContext null
                }

                val prefs = context.getSharedPreferences("prebuilt_db_prefs", Context.MODE_PRIVATE)
                val installed = prefs.getString("installed_version", "none") ?: "none"
                if (remoteVer == installed) {
                    Log.d("FoodDatabaseManager", "Pre-built DB up to date ($remoteVer)")
                    return@withContext null
                }

                Log.d("FoodDatabaseManager", "Pre-built DB update: $installed → $remoteVer")
                PrebuiltDatabaseInfo(remoteVer, downloadUrl, fileSize, count)
            } catch (e: Exception) {
                Log.w("FoodDatabaseManager", "Version check failed: ${e.message}")
                null
            }
        }

    /**
     * Downloads the pre-built database .gz from [info.downloadUrl], decompresses it,
     * and imports all rows into Room in batches. Progress is reported via [downloadProgress].
     */
    suspend fun downloadPrebuiltDatabase(info: PrebuiltDatabaseInfo): Result<Unit> =
        withContext(Dispatchers.IO) {
            val gzFile = File(context.cacheDir, "offs_prebuilt.db.gz")
            val dbFile = File(context.cacheDir, "offs_prebuilt.db")
            try {
                _downloadProgress.value = DownloadProgress(
                    currentDatabase  = "openfoodfacts",
                    currentOperation = "Connecting to server...",
                    totalItems       = info.productCount,
                    isDownloading    = true
                )

                // ── 1. Download .gz ────────────────────────────────────────────
                val conn = java.net.URL(info.downloadUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 30_000
                conn.readTimeout    = 300_000
                conn.setRequestProperty("User-Agent", "CalorieTrackerApp/3.1 (Android)")
                if (conn.responseCode != 200) {
                    return@withContext Result.failure(
                        Exception("Server returned HTTP ${conn.responseCode}")
                    )
                }
                val totalBytes = info.fileSizeBytes.takeIf { it > 0 } ?: conn.contentLengthLong
                var receivedBytes = 0L

                FileOutputStream(gzFile).use { out ->
                    conn.inputStream.use { input ->
                        val buf = ByteArray(65536)
                        var n: Int
                        while (input.read(buf).also { n = it } != -1) {
                            out.write(buf, 0, n)
                            receivedBytes += n
                            if (totalBytes > 0) {
                                val mb      = receivedBytes / 1_048_576
                                val totalMb = totalBytes  / 1_048_576
                                _downloadProgress.value = _downloadProgress.value.copy(
                                    currentOperation = "Downloading... ${mb} MB / ${totalMb} MB",
                                    downloadedItems  = (receivedBytes * 100 / totalBytes).toInt()
                                )
                            }
                        }
                    }
                }
                conn.disconnect()
                Log.d("FoodDatabaseManager", "Download complete: ${receivedBytes / 1_048_576} MB")

                // ── 2. Decompress ──────────────────────────────────────────────
                _downloadProgress.value = _downloadProgress.value.copy(
                    currentOperation = "Decompressing..."
                )
                GZIPInputStream(FileInputStream(gzFile)).use { gz ->
                    FileOutputStream(dbFile).use { out -> gz.copyTo(out, bufferSize = 65536) }
                }
                gzFile.delete()

                // ── 3. Import rows from temp SQLite into Room ──────────────────
                _downloadProgress.value = _downloadProgress.value.copy(
                    currentOperation = "Importing products...",
                    downloadedItems  = 0
                )
                var imported = 0
                val batch = mutableListOf<OpenFoodFactsItem>()

                val tempDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath, null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )
                val cursor = tempDb.query("openfoodfacts_items", null, null, null, null, null, null)
                try {
                    fun col(name: String) = cursor.getColumnIndex(name)
                    val cId     = col("id");          val cBarcode = col("barcode")
                    val cName   = col("productName"); val cBrands  = col("brands")
                    val cCats   = col("categories");  val cLabels  = col("labels")
                    val cCntry  = col("countries");   val cIngr    = col("ingredients")
                    val cAlrg   = col("allergens");   val cSrvSz   = col("servingSize")
                    val cSrvQt  = col("servingQuantity")
                    val cEKj    = col("energyKj");    val cEKcal   = col("energyKcal")
                    val cFat    = col("fat");         val cSatFat  = col("saturatedFat")
                    val cCarbs  = col("carbohydrates"); val cSugars = col("sugars")
                    val cFiber  = col("fiber");       val cProt    = col("proteins")
                    val cSalt   = col("salt");        val cSodium  = col("sodium")
                    val cImgUrl = col("imageUrl");    val cProdUrl = col("productUrl")
                    val cGrade  = col("nutritionGrade"); val cNova  = col("novaGroup")
                    val cCompl  = col("completeness")

                    fun dbl(c: Int) = if (c >= 0 && !cursor.isNull(c)) cursor.getDouble(c) else null
                    fun str(c: Int) = if (c >= 0) cursor.getString(c) else null
                    fun int(c: Int) = if (c >= 0 && !cursor.isNull(c)) cursor.getInt(c) else null

                    while (cursor.moveToNext()) {
                        val id   = str(cId)   ?: continue
                        val name = str(cName) ?: continue
                        batch.add(OpenFoodFactsItem(
                            id = id, barcode = str(cBarcode) ?: id, productName = name,
                            brands = str(cBrands), categories = str(cCats), labels = str(cLabels),
                            countries = str(cCntry), ingredients = str(cIngr), allergens = str(cAlrg),
                            servingSize = str(cSrvSz), servingQuantity = dbl(cSrvQt),
                            energyKj = dbl(cEKj), energyKcal = dbl(cEKcal),
                            fat = dbl(cFat), saturatedFat = dbl(cSatFat),
                            carbohydrates = dbl(cCarbs), sugars = dbl(cSugars),
                            fiber = dbl(cFiber), proteins = dbl(cProt),
                            salt = dbl(cSalt), sodium = dbl(cSodium),
                            imageUrl = str(cImgUrl), productUrl = str(cProdUrl),
                            nutritionGrade = str(cGrade), novaGroup = int(cNova),
                            completeness = dbl(cCompl), lastUpdated = System.currentTimeMillis()
                        ))
                        if (batch.size >= 500) {
                            database.openFoodFactsDao().insertFoods(batch)
                            imported += batch.size; batch.clear()
                            _downloadProgress.value = _downloadProgress.value.copy(
                                currentOperation = "Importing... %,d products".format(imported),
                                downloadedItems  = imported
                            )
                        }
                    }
                    if (batch.isNotEmpty()) {
                        database.openFoodFactsDao().insertFoods(batch)
                        imported += batch.size; batch.clear()
                    }
                } finally {
                    cursor.close()
                    tempDb.close()
                }
                dbFile.delete()
                Log.d("FoodDatabaseManager", "Prebuilt import complete: $imported products")

                // ── 4. Persist status & version ────────────────────────────────
                database.foodDatabaseStatusDao().insertStatus(
                    FoodDatabaseStatus(
                        databaseName     = "openfoodfacts",
                        isDownloading    = false,
                        isComplete       = true,
                        downloadedItems  = imported,
                        lastDownloadDate = System.currentTimeMillis(),
                        version          = info.version
                    )
                )
                context.getSharedPreferences("prebuilt_db_prefs", Context.MODE_PRIVATE)
                    .edit().putString("installed_version", info.version).apply()

                _downloadProgress.value = DownloadProgress(
                    currentDatabase  = "openfoodfacts",
                    currentOperation = "Done — %,d products imported".format(imported),
                    totalItems       = imported,
                    downloadedItems  = imported,
                    isDownloading    = false,
                    isComplete       = true
                )
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("FoodDatabaseManager", "Prebuilt DB download failed", e)
                gzFile.delete(); dbFile.delete()
                _downloadProgress.value = _downloadProgress.value.copy(
                    isDownloading = false,
                    error         = "Download failed: ${e.message}"
                )
                Result.failure(e)
            }
        }

    // ── USDA Bulk Download (internal) ──────────────────────────────────────────

    /**
     * Download USDA data using bulk CSV files (alternative when API is down)
     *
     * **BULK DOWNLOAD vs API REQUESTS:**
     * 
     * **Individual API Requests:**
     * - Request one food item at a time
     * - Slow: 25 foods per request, 1000+ requests needed
     * - Rate limited: May take hours to complete
     * - Network dependent: Fails if connection drops
     * 
     * **Bulk CSV Download:**
     * - Download entire database as one large ZIP file (400MB+)
     * - Fast: Single HTTP request, downloads in 1-5 minutes
     * - No rate limits: Get all data at once
     * - Offline processing: Parse locally after download
     * 
     * **CSV Parsing Process:**
     * 1. Download compressed ZIP file over HTTP
     * 2. Extract CSV files using ZipInputStream
     * 3. Parse each row of CSV into database records
     * 4. Insert records in batches for performance
     * 5. Update progress notifications for user feedback
     * 
     * **Memory Management:**
     * Large files (400MB) can crash the app if loaded entirely into memory:
     * - Stream processing: Read file piece by piece
     * - BufferedReader: Efficiently read text line by line
     * - Batch inserts: Insert 1000 records at a time to database
     * - Progress tracking: Update UI without blocking
     */
    private suspend fun downloadUSDABulkData(): Result<Unit> {
        return try {
            FileLogger.addSeparator("USDA Bulk Data Download")
            FileLogger.d("FoodDatabaseManager", "Delegating USDA download to specialized downloader...")
            
            // Configure progress callback to update our progress flow
            usdaDownloader.onProgressUpdate = { progress, message ->
                _downloadProgress.value = _downloadProgress.value.copy(
                    currentDatabase = "USDA",
                    currentOperation = message,
                    downloadedItems = progress // Use progress as downloaded items indicator
                )
                
                // Update notification
                notificationManager.updateProgress("USDA Database", progress, 100, message)
            }
            
            // Show notification that USDA download started
            notificationManager.showDownloadStarted("USDA Database")
            
            _downloadProgress.value = _downloadProgress.value.copy(
                currentDatabase = "USDA",
                currentOperation = "Starting USDA download..."
            )
            
            // Delegate to specialized USDA downloader
            val downloadResult = usdaDownloader.downloadAndProcess()
            
            if (downloadResult.isSuccess) {
                // Get the final count of inserted foods
                val finalCount = database.usdaFoodItemDao().getCount()
                
                FileLogger.d("FoodDatabaseManager", "USDA download successful, final count: $finalCount")
                Log.d("FoodDatabaseManager", "USDA download successful, final count: $finalCount")
                
                // Update status as complete
                database.foodDatabaseStatusDao().insertStatus(
                    FoodDatabaseStatus(
                        databaseName = "usda",
                        lastDownloadDate = System.currentTimeMillis(),
                        totalItems = finalCount,
                        downloadedItems = finalCount,
                        isDownloading = false,
                        isComplete = true,
                        errorMessage = null
                    )
                )
                
                _downloadProgress.value = _downloadProgress.value.copy(
                    downloadedItems = finalCount,
                    currentOperation = "USDA data loaded successfully! ($finalCount foods)"
                )
                
                notificationManager.showDownloadCompleted("USDA Database", finalCount)
                FileLogger.d("FoodDatabaseManager", "USDA bulk data download completed successfully")
                
            } else {
                val error = downloadResult.exceptionOrNull()
                FileLogger.e("FoodDatabaseManager", "USDA download failed", error)
                
                _downloadProgress.value = _downloadProgress.value.copy(
                    isDownloading = false,
                    isComplete = false,
                    error = error?.message ?: "Download failed",
                    totalItems = 0,
                    downloadedItems = 0
                )
                
                // Update database status
                database.foodDatabaseStatusDao().insertStatus(
                    FoodDatabaseStatus(
                        databaseName = "usda",
                        isDownloading = false,
                        isComplete = false,
                        errorMessage = error?.message ?: "Download failed",
                        totalItems = 0,
                        downloadedItems = 0,
                        lastDownloadDate = null
                    )
                )
                
                notificationManager.showDownloadFailed("USDA Database", error?.message ?: "Download failed")
            }
            
            downloadResult
            
        } catch (e: Exception) {
            FileLogger.e("FoodDatabaseManager", "Error in USDA bulk data download", e)
            
            // Show error notification
            notificationManager.showDownloadFailed("USDA Database (Bulk)", e.message ?: "Unknown error")
            
            Result.failure(e)
        }
    }
    
    /**
     * Create sample USDA foods for demonstration when bulk download is used
     * In a full implementation, this would parse the actual CSV data
     */
    private fun createSampleUSDAFoods(): List<USDAFoodItem> {
        return listOf(
            USDAFoodItem(
                fdcId = 167512,
                description = "Chicken, broilers or fryers, breast, meat only, cooked, roasted",
                dataType = "Foundation",
                calories = 165.0,
                protein = 31.02,
                fat = 3.57,
                carbohydrates = 0.0,
                fiber = 0.0,
                sodium = 74.0,
                foodCategory = "Poultry Products"
            ),
            USDAFoodItem(
                fdcId = 169057,
                description = "Eggs, whole, raw, fresh",
                dataType = "Foundation", 
                calories = 155.0,
                protein = 12.56,
                fat = 10.61,
                carbohydrates = 1.12,
                fiber = 0.0,
                sodium = 124.0,
                foodCategory = "Dairy and Egg Products"
            ),
            USDAFoodItem(
                fdcId = 167758,
                description = "Beef, ground, 85% lean meat / 15% fat, raw",
                dataType = "Foundation",
                calories = 215.0,
                protein = 20.0,
                fat = 15.0,
                carbohydrates = 0.0,
                fiber = 0.0,
                sodium = 66.0,
                foodCategory = "Beef Products"
            ),
            USDAFoodItem(
                fdcId = 168916,
                description = "Rice, white, long-grain, regular, cooked",
                dataType = "Foundation",
                calories = 130.0,
                protein = 2.69,
                fat = 0.28,
                carbohydrates = 28.17,
                fiber = 0.4,
                sodium = 1.0,
                foodCategory = "Cereal Grains and Pasta"
            ),
            USDAFoodItem(
                fdcId = 171256,
                description = "Milk, whole, 3.25% milkfat",
                dataType = "Foundation",
                calories = 61.0,
                protein = 3.15,
                fat = 3.25,
                carbohydrates = 4.78,
                fiber = 0.0,
                sodium = 40.0,
                foodCategory = "Dairy and Egg Products"
            )
        )
    }
    
    // ===== HELPER METHODS FOR COMPREHENSIVE FEATURES =====
    
    /**
     * Search local database for foods by name
     */
    private suspend fun searchLocalFoodsByName(query: String, limit: Int): List<FoodItem> {
        return database.foodItemDao().searchFoodsByName("%$query%", limit)
    }
    
    /**
     * Check if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        return true // Simplified for now, could implement actual network check
    }
    
    /**
     * Search USDA foods by query
     */
    suspend fun searchUSDAFoods(@Suppress("UNUSED_PARAMETER") query: String, @Suppress("UNUSED_PARAMETER") limit: Int): List<USDAFoodItem> {
        return try {
            // This would use the existing USDA search functionality
            // For now, return empty list as the full implementation would be complex
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error searching USDA foods", e)
            emptyList()
        }
    }
    
    /**
     * Search Open Food Facts by query
     */
    suspend fun searchOpenFoodFacts(@Suppress("UNUSED_PARAMETER") query: String, @Suppress("UNUSED_PARAMETER") limit: Int): List<FoodItem> {
        return try {
            // This would use the existing Open Food Facts search functionality
            // For now, return empty list as the full implementation would be complex
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error searching Open Food Facts", e)
            emptyList()
        }
    }
    
    /**
     * Search cached/offline foods by query
     */
    suspend fun searchCachedFoods(@Suppress("UNUSED_PARAMETER") query: String, @Suppress("UNUSED_PARAMETER") limit: Int): List<BarcodeCache> {
        return try {
            database.barcodeCacheDao().searchByName("%$query%", limit)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching cached foods", e)
            emptyList()
        }
    }
    
    // ===== COMPREHENSIVE ENHANCED FEATURES =====
    
    companion object {
        private const val TAG = "FoodDatabaseManager"
    }
    
    /**
     * Download USDA database using full CSV parsing for comprehensive coverage
     */
    suspend fun downloadUSDADatabaseFull(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting comprehensive USDA database download with CSV parsing")
                
                // Initialize offline cache if not already done
                offlineCacheManager.initializeCache()
                
                notificationManager.showDownloadStarted("USDA Database (Full)")
                
                _downloadProgress.value = DownloadProgress(
                    isDownloading = true,
                    currentOperation = "Downloading USDA bulk data files..."
                )
                
                // Download and parse USDA bulk ZIP files
                try {
                    val response = usdaBulkService.downloadSurveyFoods()
                    val responseBody = if (response.isSuccessful) response.body() else null
                    if (responseBody != null) {

                        notificationManager.updateProgress("USDA Database", 0, 100, "Processing bulk data files...")

                        // Parse the ZIP file using comprehensive CSV parser
                        val foods = csvParser.parseBulkZipFile(responseBody.byteStream())
                        
                        if (foods.isNotEmpty()) {
                            Log.d(TAG, "Parsed ${foods.size} foods from USDA bulk data")
                            
                            // Store in database in batches for better performance
                            val batchSize = 1000
                            val batches = foods.chunked(batchSize)
                            
                            batches.forEachIndexed { index, batch ->
                                try {
                                    database.usdaFoodItemDao().insertFoods(batch)
                                    
                                    val progress = ((index + 1) * 100 / batches.size)
                                    notificationManager.updateProgress(
                                        "USDA Database", 
                                        index + 1, 
                                        batches.size, 
                                        "Storing batch ${index + 1}/${batches.size}..."
                                    )
                                    
                                    // Also cache for offline use
                                    val offlineFoods = batch.map { it.toOfflineFood() }
                                    database.offlineFoodDao().insertOfflineFoods(offlineFoods)
                                    
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error inserting USDA batch ${index + 1}", e)
                                }
                            }
                            
                            // Update status
                            database.foodDatabaseStatusDao().insertStatus(
                                FoodDatabaseStatus(
                                    databaseName = "usda",
                                    isDownloading = false,
                                    totalItems = foods.size,
                                    downloadedItems = foods.size,
                                    lastUpdated = System.currentTimeMillis()
                                )
                            )
                            
                            notificationManager.showDownloadCompleted("USDA Database", foods.size)
                            Log.d(TAG, "USDA database download completed with ${foods.size} foods")
                            
                        } else {
                            throw Exception("No foods parsed from USDA bulk data")
                        }
                    } else {
                        throw Exception("Failed to download USDA bulk data: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error with bulk download, falling back to API", e)
                    // Fallback to regular API download
                    return@withContext downloadUSDADatabase()
                }
                
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading comprehensive USDA database", e)
                notificationManager.showDownloadFailed("USDA Database", e.message ?: "Unknown error")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Initialize comprehensive offline caching system
     */
    suspend fun initializeOfflineCache(): Boolean {
        return try {
            Log.d(TAG, "Initializing comprehensive offline cache")
            
            val success = offlineCacheManager.initializeCache()
            if (success) {
                // Pre-cache popular foods
                offlineCacheManager.preCachePopularFoods()
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing offline cache", e)
            false
        }
    }
    
    /**
     * Search foods using all available sources with offline fallback
     */
    suspend fun searchFoodsComprehensive(query: String, limit: Int = 10): List<com.calorietracker.database.FoodItem> {
        return withContext(Dispatchers.IO) {
            try {
                // First check offline cache
                val cachedResults = offlineCacheManager.getCachedFoodSearch(query)
                if (cachedResults != null && cachedResults.isNotEmpty()) {
                    Log.d(TAG, "Returning ${cachedResults.size} cached results for: $query")
                    return@withContext cachedResults
                }
                
                val allResults = mutableListOf<com.calorietracker.database.FoodItem>()
                
                // Search local database first
                val localResults = searchLocalFoodsByName(query, limit / 4)
                allResults.addAll(localResults.map { it.copy(name = "${it.name} (local)") })
                
                // If we have network, search online sources
                if (isNetworkAvailable()) {
                    
                    // Search USDA database
                    try {
                        val usdaResults = searchUSDAFoods(query, limit / 4)
                        allResults.addAll(usdaResults.map { it.toFoodItem() })
                    } catch (e: Exception) {
                        Log.w(TAG, "USDA search failed", e)
                    }
                    
                    // Search Open Food Facts
                    try {
                        val offResults = searchOpenFoodFacts(query, limit / 4)
                        allResults.addAll(offResults)
                    } catch (e: Exception) {
                        Log.w(TAG, "Open Food Facts search failed", e)
                    }
                    
                    // Search Nutritionix if configured
                    if (nutritionixManager.isConfigured()) {
                        try {
                            val nutritionixResults = nutritionixManager.searchFoods(query, limit / 4)
                            allResults.addAll(nutritionixResults)
                        } catch (e: Exception) {
                            Log.w(TAG, "Nutritionix search failed", e)
                        }
                    }
                    
                    // Cache the results for offline use
                    if (allResults.isNotEmpty()) {
                        offlineCacheManager.cacheFoodSearch(query, allResults)
                    }
                }
                
                Log.d(TAG, "Comprehensive search returned ${allResults.size} results for: $query")
                allResults.distinctBy { it.barcode }.take(limit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in comprehensive food search", e)
                emptyList()
            }
        }
    }
    
    /**
     * Get comprehensive offline statistics
     */
    suspend fun getOfflineStats(): com.calorietracker.cache.OfflineStats {
        return offlineCacheManager.getOfflineStats()
    }
    
    /**
     * Schedule automatic database updates
     */
    fun scheduleAutomaticUpdates(intervalHours: Long = 168) { // Weekly by default
        com.calorietracker.workers.DatabaseUpdateWorker.schedulePeriodicUpdates(
            context = context,
            intervalHours = intervalHours,
            updateType = com.calorietracker.workers.DatabaseUpdateWorker.UPDATE_ALL
        )
        Log.d(TAG, "Scheduled automatic database updates every $intervalHours hours")
    }
    
    /**
     * Cancel scheduled automatic updates
     */
    fun cancelAutomaticUpdates() {
        com.calorietracker.workers.DatabaseUpdateWorker.cancelScheduledUpdates(context)
        Log.d(TAG, "Cancelled automatic database updates")
    }
    
    /**
     * Parse natural language food queries using Nutritionix
     */
    suspend fun parseNaturalLanguage(query: String): List<com.calorietracker.database.FoodItem> {
        return if (nutritionixManager.isConfigured()) {
            try {
                val results = nutritionixManager.parseNaturalLanguage(query)
                Log.d(TAG, "Parsed natural language query: '$query' -> ${results.size} results")
                results
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing natural language", e)
                emptyList()
            }
        } else {
            Log.w(TAG, "Nutritionix not configured for natural language parsing")
            emptyList()
        }
    }
    
    /**
     * Search within specific restaurant brands
     */
    suspend fun searchRestaurantBrand(brandName: String, foodQuery: String = ""): List<com.calorietracker.database.FoodItem> {
        return if (nutritionixManager.isConfigured()) {
            try {
                nutritionixManager.searchBrand(brandName, foodQuery)
            } catch (e: Exception) {
                Log.e(TAG, "Error searching restaurant brand", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Get list of available restaurant brands
     */
    fun getAvailableRestaurantBrands(): List<String> {
        return nutritionixManager.getPopularBrands()
    }
    
    // Helper extension functions
    private fun USDAFoodItem.toFoodItem(): com.calorietracker.database.FoodItem {
        return com.calorietracker.database.FoodItem(
            name = "${this.description} (USDA)",
            barcode = "USDA${this.fdcId}",
            caloriesPerServing = this.calories.toInt(),
            proteinPerServing = this.protein,
            carbsPerServing = this.carbohydrates,
            fatPerServing = this.fat,
            fiberPerServing = this.fiber,
            sugarPerServing = this.sugar,
            sodiumPerServing = this.sodium,
            servingSize = this.servingSize?.toString() ?: "100",
            brand = this.brandOwner
        )
    }
    
    private fun USDAFoodItem.toOfflineFood(): com.calorietracker.database.OfflineFood {
        return com.calorietracker.database.OfflineFood(
            barcode = "USDA${this.fdcId}",
            name = this.description,
            calories = this.calories.toInt(),
            protein = this.protein,
            carbs = this.carbohydrates,
            fat = this.fat,
            fiber = this.fiber,
            sugar = this.sugar,
            sodium = this.sodium,
            servingSize = this.servingSize?.toString() ?: "100",
            servingUnit = this.servingSizeUnit ?: "g",
            brand = this.brandOwner,
            categories = this.dataType,
            source = "USDA",
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Clean up resources when the manager is no longer needed
     */
    fun cleanup() {
        unregisterCancelReceiver()
        cancelDownload()
        downloadScope.cancel()
    }
}