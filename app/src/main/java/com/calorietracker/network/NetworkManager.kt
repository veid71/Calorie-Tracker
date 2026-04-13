package com.calorietracker.network

/**
 * ANDROID NETWORKING FUNDAMENTALS FOR BEGINNERS:
 * 
 * This file demonstrates core Android networking concepts using Retrofit,
 * OkHttp, and modern Kotlin coroutines. Perfect for learning how Android
 * apps communicate with web APIs and handle network operations.
 * 
 * **NETWORKING STACK OVERVIEW:**
 * 
 * 1. **HTTP Layer (OkHttp):**
 *    - Handles low-level HTTP communication
 *    - Manages connections, timeouts, retries
 *    - Like the "postal service" for your app
 * 
 * 2. **API Interface Layer (Retrofit):**
 *    - Converts Kotlin functions into HTTP requests
 *    - Automatically handles JSON parsing
 *    - Like a "translator" between your app and web APIs
 * 
 * 3. **Data Layer (Gson):**
 *    - Converts JSON text to Kotlin objects and vice versa
 *    - Handles null safety and type conversion
 *    - Like a "filing system" for organizing API data
 * 
 * 4. **Threading Layer (Coroutines):**
 *    - Keeps UI responsive during slow network operations
 *    - Handles background threads automatically
 *    - Like having "assistants" do the heavy lifting
 */

import android.content.Context
import android.net.ConnectivityManager
import com.calorietracker.BuildConfig
import android.net.NetworkCapabilities
import android.util.Log
import com.calorietracker.api.NutritionApiService
import com.calorietracker.api.OpenFoodFactsService
import com.calorietracker.api.OpenFoodFactsResponse
import com.calorietracker.api.OpenFoodFactsSearchResponse
import com.calorietracker.database.FoodItem
import com.calorietracker.database.BarcodeCache
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.database.CacheStats
import com.calorietracker.database.OpenFoodFactsItem
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * NetworkManager: Central hub for all API communications in the CalorieTracker app
 * 
 * **DESIGN PATTERNS DEMONSTRATED:**
 * 
 * **Repository Pattern:**
 * This class acts as a "repository" - a single place that manages data from multiple sources:
 * - Hides complexity of different APIs from the rest of the app
 * - Provides a clean, consistent interface for food data
 * - Handles caching, error recovery, and data transformation
 * 
 * **Service Locator Pattern:**
 * Creates and manages API service instances:
 * - One place to configure all network clients
 * - Consistent settings (timeouts, logging) across all APIs
 * - Easy to modify network behavior for entire app
 * 
 * **Dependency Injection:**
 * Takes Context as parameter instead of creating it internally:
 * - Makes the class testable (can inject mock context)
 * - Follows Android best practices
 * - Allows different contexts for different use cases
 */
class NetworkManager(private val context: Context) {
    
    // **API SERVICE INSTANCES:**
    // These are Retrofit interfaces that convert Kotlin functions to HTTP requests
    private val nutritionApiService: NutritionApiService  // Edamam API for food search
    private val openFoodFactsService: OpenFoodFactsService  // Open Food Facts for barcodes
    
    // **LOCAL DATA STORAGE:**
    private val database: CalorieDatabase = CalorieDatabase.getDatabase(context)
    
    // **JSON PARSING:**
    private val gson = Gson()  // Google's JSON parser library
    
    // **HTTP CLIENT:**
    // OkHttp handles the actual network communication
    private val httpClient: OkHttpClient
    
    // Edamam API credentials loaded securely at runtime.
    // To configure: enter keys via Settings → API Keys, or set EDAMAM_APP_ID / EDAMAM_APP_KEY
    // in local.properties before building (never commit those values to source control).
    private fun getEdamamAppId(): String {
        val secureManager = com.calorietracker.security.SecureApiKeyManager.getInstance(context)
        val userAppId = secureManager.getApiKey("edamam_app_id")
        return if (!userAppId.isNullOrBlank()) {
            userAppId
        } else {
            try {
                val field = Class.forName("${context.packageName}.BuildConfig")
                    .getDeclaredField("EDAMAM_APP_ID")
                field.get(null) as? String ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }

    private fun getEdamamAppKey(): String {
        val secureManager = com.calorietracker.security.SecureApiKeyManager.getInstance(context)
        val userAppKey = secureManager.getApiKey("edamam_app_key")
        return if (!userAppKey.isNullOrBlank()) {
            userAppKey
        } else {
            try {
                val field = Class.forName("${context.packageName}.BuildConfig")
                    .getDeclaredField("EDAMAM_APP_KEY")
                field.get(null) as? String ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }
    
    /**
     * INITIALIZATION BLOCK - NETWORK CLIENT SETUP:
     * 
     * This init block runs when NetworkManager is created and sets up our HTTP client.
     * Think of it as "configuring your internet connection settings".
     */
    init {
        /**
         * HTTP LOGGING INTERCEPTOR:
         * 
         * **What is an Interceptor?**
         * Like a security guard who inspects every package going in/out:
         * - Sees every HTTP request your app makes
         * - Can modify requests (add headers, authentication)
         * - Logs responses for debugging
         * - Can cache responses or handle errors
         * 
         * **Logging Levels:**
         * - NONE: No logging (production apps)
         * - BASIC: Just URL and response code
         * - HEADERS: Include all HTTP headers  
         * - BODY: Full request/response content (what we use for debugging)
         */
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Only log in debug builds; release builds log nothing to prevent API key leakage
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        
        /**
         * OKHTTP CLIENT CONFIGURATION:
         * 
         * **Builder Pattern:**
         * OkHttp uses the "Builder" pattern for configuration:
         * - Start with OkHttpClient.Builder()
         * - Add features one by one (.addInterceptor, .connectTimeout, etc.)
         * - Call .build() to create the final client
         * 
         * **Timeout Settings:**
         * Network operations can hang forever, so we set timeouts:
         * - connectTimeout: How long to wait to establish connection (30 sec)
         * - readTimeout: How long to wait for server response (30 sec)
         * - Without timeouts, app might freeze waiting for slow servers
         */
        httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)  // Add logging for debugging
            .connectTimeout(30, TimeUnit.SECONDS)  // 30 sec to connect
            .readTimeout(30, TimeUnit.SECONDS)     // 30 sec to read response
            .build()  // Create the configured client
        
        /**
         * RETROFIT SETUP FOR EDAMAM API:
         * 
         * **What is Retrofit?**
         * Retrofit is like a "universal translator" for web APIs:
         * - You write normal Kotlin functions
         * - Retrofit converts them to HTTP requests automatically
         * - JSON responses become Kotlin objects automatically
         * - Handles errors and parsing behind the scenes
         * 
         * **Base URL Concept:**
         * APIs have a "base URL" that's the same for all requests:
         * - Base: "https://api.edamam.com/api/food-database/v2/"
         * - Full URL: base + endpoint (like "/parser?q=chicken")
         * - This lets us change the base URL easily (dev vs production servers)
         * 
         * **Converter Factory:**
         * GsonConverterFactory automatically converts:
         * - Kotlin objects → JSON (when sending data)
         * - JSON → Kotlin objects (when receiving data)
         * - Handles null safety and type checking
         * 
         * **HTTP Client:**
         * We pass our configured OkHttpClient so Retrofit uses our:
         * - Timeout settings
         * - Logging interceptor
         * - Any other custom network configuration
         */
        // Set up Edamam API (for text-based food search)
        val edamamRetrofit = Retrofit.Builder()
            .baseUrl("https://api.edamam.com/api/food-database/v2/")  // API base address
            .client(httpClient)  // Use our configured HTTP client
            .addConverterFactory(GsonConverterFactory.create())  // JSON ↔ Kotlin conversion
            .build()  // Create the Retrofit instance
        
        // Set up Open Food Facts API (for barcode scanning)
        val openFoodFactsRetrofit = Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        nutritionApiService = edamamRetrofit.create(NutritionApiService::class.java)
        openFoodFactsService = openFoodFactsRetrofit.create(OpenFoodFactsService::class.java)
    }
    
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
    
    /**
     * Search for food by barcode using cache-first hybrid approach
     * 
     * **CACHING STRATEGY EXPLAINED:**
     * 
     * **Why Cache Data?**
     * Network requests are slow and require internet:
     * - API call: 1-5 seconds, needs internet connection
     * - Cache lookup: 0.01 seconds, works offline
     * - User experience: Instant results vs waiting
     * 
     * **Cache-First Strategy ("Stale While Revalidate"):**
     * 1. Check local cache first (instant, may be old data)
     * 2. If cache miss, make API request (slow, fresh data)
     * 3. Save API response to cache for next time
     * 4. Return cached data immediately, update in background
     * 
     * **Offline-First Design:**
     * App works even without internet:
     * - Previously scanned items load from cache
     * - New items require internet but get cached
     * - User doesn't need to worry about connectivity
     * 
     * **Multi-API Fallback Strategy:**
     * Different APIs have different strengths:
     * - Open Food Facts: Best for consumer product barcodes
     * - Edamam: Better for restaurant/generic foods
     * - Try most likely to succeed first, fallback to others
     * 
     * **Error Handling Philosophy:**
     * Network operations fail often, so we:
     * - Never crash the app on network errors
     * - Always return a safe default (null or empty list)
     * - Log errors for debugging but don't show scary messages to users
     * - Provide graceful degradation (cache when API fails)
     */
    suspend fun searchFoodByBarcode(barcode: String): FoodItem? {
        Log.d("NetworkManager", "Starting offline-first barcode search for: $barcode")
        
        // Step 1: Check barcode cache first
        val cachedItem = database.barcodeCacheDao().getCachedItem(barcode)
        if (cachedItem != null) {
            Log.d("NetworkManager", "Found cached item: ${cachedItem.name} (cached from ${cachedItem.cacheSource})")
            // Update last accessed time
            database.barcodeCacheDao().updateLastAccessed(barcode)
            // Convert cached item to FoodItem
            return convertCacheToFoodItem(cachedItem)
        }
        
        // Step 2: Check offline OpenFoodFacts database
        val offlineItem = database.openFoodFactsDao().getFoodByBarcode(barcode)
        if (offlineItem != null) {
            Log.d("NetworkManager", "Found item in offline OpenFoodFacts database: ${offlineItem.productName}")
            // Convert OpenFoodFacts item to FoodItem
            val foodItem = convertOpenFoodFactsItemToFoodItem(offlineItem)
            // Cache it for faster future access
            cacheBarcodeLookup(foodItem, "offline_openfoodfacts")
            return foodItem
        }
        
        // Step 3: Not in cache or offline DB, check if network is available
        if (!isNetworkAvailable()) {
            Log.w("NetworkManager", "No network available and barcode not found offline: $barcode")
            return null
        }
        
        Log.d("NetworkManager", "Item not found offline, searching online for barcode: $barcode")
        
        // Step 4: Try Open Food Facts API (best for barcodes)
        val openFoodFactsResult = searchOpenFoodFacts(barcode)
        if (openFoodFactsResult != null) {
            Log.d("NetworkManager", "Found product in Open Food Facts: ${openFoodFactsResult.name}")
            // Cache the result for future offline access
            cacheBarcodeLookup(openFoodFactsResult, "open_food_facts")
            return openFoodFactsResult
        }
        
        // Step 5: Try Edamam as fallback (rarely works for barcodes, but keeping for completeness)
        Log.d("NetworkManager", "Open Food Facts failed, trying Edamam fallback")
        return try {
            val response = nutritionApiService.getFoodByBarcode(barcode, getEdamamAppId(), getEdamamAppKey())
            if (response.isSuccessful && response.body() != null) {
                val foodHint = response.body()?.hints?.firstOrNull()
                foodHint?.let { 
                    Log.d("NetworkManager", "Found product in Edamam: ${it.food.label}")
                    val foodItem = convertToFoodItem(it, barcode)
                    // Cache the result
                    cacheBarcodeLookup(foodItem, "edamam")
                    foodItem
                }
            } else {
                Log.w("NetworkManager", "Edamam barcode search failed: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("NetworkManager", "Exception during Edamam barcode search", e)
            null
        }
    }
    
    /**
     * Search Open Food Facts database for barcode
     * This is where most consumer products will be found
     */
    private suspend fun searchOpenFoodFacts(barcode: String): FoodItem? {
        return try {
            Log.d("NetworkManager", "Searching Open Food Facts for barcode: $barcode")
            val response = openFoodFactsService.getProductByBarcode(barcode)
            
            Log.d("NetworkManager", "Open Food Facts response: ${response.code()}")
            
            val body = if (response.isSuccessful) response.body() else null
            if (body != null) {
                Log.d("NetworkManager", "Open Food Facts status: ${body.status}, product: ${body.product?.product_name}")
                if (body.status == 1 && body.product != null) {
                    convertOpenFoodFactsToFoodItem(body, barcode)
                } else {
                    Log.w("NetworkManager", "Product not found in Open Food Facts database")
                    null
                }
            } else {
                Log.w("NetworkManager", "Open Food Facts API call failed: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("NetworkManager", "Exception during Open Food Facts search", e)
            null
        }
    }
    
    suspend fun searchFoodByName(query: String): List<FoodItem> {
        if (!isNetworkAvailable()) return emptyList()
        
        return try {
            val response = nutritionApiService.searchFood(query, getEdamamAppId(), getEdamamAppKey())
            if (response.isSuccessful && response.body() != null) {
                response.body()?.hints?.mapNotNull { hint ->
                    // For search results, we'll use the food ID as barcode
                    convertToFoodItem(hint, hint.food.foodId)
                } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Convert Open Food Facts product data to our FoodItem format
     * This handles the conversion from Open Food Facts format to our database format
     */
    private fun convertOpenFoodFactsToFoodItem(response: OpenFoodFactsResponse, barcode: String): FoodItem? {
        val product = response.product ?: return null
        return convertOpenFoodFactsToFoodItem(product, barcode)
    }
    
    private fun convertOpenFoodFactsToFoodItem(product: com.calorietracker.api.OpenFoodFactsProduct, barcode: String?): FoodItem {
        val nutriments = product.nutriments
        
        // Get product name (try English first, then any language)
        val productName = product.product_name_en?.takeIf { it.isNotBlank() } 
            ?: product.product_name?.takeIf { it.isNotBlank() } 
            ?: "Unknown Product"
        
        // Get brand (clean up the brands string)
        val brand = product.brands?.split(",")?.firstOrNull()?.trim() ?: "Generic"
        
        // Determine serving size - Open Food Facts uses various formats
        val servingInfo = when {
            product.serving_size?.isNotBlank() == true -> product.serving_size.orEmpty()
            product.quantity?.isNotBlank() == true -> "1 package (${product.quantity})"
            else -> "100g"
        }

        // Calculate nutrition per serving
        // Open Food Facts typically provides per 100g, so we need to convert if serving size is different
        val servingMultiplier = calculateServingMultiplier(product.serving_size, nutriments)

        val kcalServing = nutriments?.`energy-kcal_serving`
        val kcal100g = nutriments?.`energy-kcal_100g`
        val calories = when {
            kcalServing != null -> kcalServing.toInt()
            kcal100g != null -> (kcal100g * servingMultiplier).toInt()
            else -> 0
        }

        // Get macronutrients (convert from per 100g if needed)
        val protein = nutriments?.proteins_serving ?: (nutriments?.proteins_100g?.times(servingMultiplier))
        val carbs = nutriments?.carbohydrates_serving ?: (nutriments?.carbohydrates_100g?.times(servingMultiplier))
        val fat = nutriments?.fat_serving ?: (nutriments?.fat_100g?.times(servingMultiplier))
        val fiber = nutriments?.fiber_serving ?: (nutriments?.fiber_100g?.times(servingMultiplier))
        val sugar = nutriments?.sugars_serving ?: (nutriments?.sugars_100g?.times(servingMultiplier))

        // Convert sodium from grams to milligrams (Open Food Facts uses grams, we use mg)
        val sodiumServing = nutriments?.sodium_serving
        val sodium100g = nutriments?.sodium_100g
        val sodium = when {
            sodiumServing != null -> sodiumServing * 1000 // Convert g to mg
            sodium100g != null -> sodium100g * servingMultiplier * 1000 // Convert g to mg
            else -> null
        }
        
        Log.d("NetworkManager", "Converted Open Food Facts product: $productName ($brand) - ${calories} cal")
        
        return FoodItem(
            barcode = barcode ?: "NAME_${System.currentTimeMillis()}_${(0..999).random()}",
            name = productName,
            brand = brand,
            servingSize = servingInfo,
            caloriesPerServing = calories,
            proteinPerServing = protein,
            carbsPerServing = carbs,
            fatPerServing = fat,
            fiberPerServing = fiber,
            sugarPerServing = sugar,
            sodiumPerServing = sodium
        )
    }
    
    /**
     * Calculate serving multiplier to convert from per 100g to per serving
     * This is needed because Open Food Facts typically provides nutrition per 100g
     */
    private fun calculateServingMultiplier(servingSize: String?, nutriments: com.calorietracker.api.OpenFoodFactsNutriments?): Double {
        // If we have per-serving data, use multiplier of 1 (no conversion needed)
        if (nutriments?.`energy-kcal_serving` != null) {
            return 1.0
        }
        
        // Try to extract serving size from the serving size string
        servingSize?.let { size ->
            // Look for patterns like "355 ml", "12 fl oz", "250g", etc.
            val numbers = Regex("""(\d+(?:\.\d+)?)""").findAll(size).map { it.value.toDoubleOrNull() ?: 0.0 }.toList()
            if (numbers.isNotEmpty()) {
                val amount = numbers.first()
                return when {
                    size.contains("ml", true) || size.contains("milliliter", true) -> amount / 100.0 // ml to 100ml
                    size.contains("l", true) && !size.contains("ml", true) -> amount * 10.0 // liters to 100ml
                    size.contains("fl oz", true) -> (amount * 29.5735) / 100.0 // fl oz to 100ml
                    size.contains("g", true) && !size.contains("mg", true) -> amount / 100.0 // grams to 100g
                    size.contains("oz", true) && !size.contains("fl oz", true) -> (amount * 28.3495) / 100.0 // oz to 100g
                    else -> 1.0 // Default to no conversion
                }
            }
        }
        
        // Default: assume serving = 100g (no conversion needed)
        return 1.0
    }
    
    private fun convertToFoodItem(hint: com.calorietracker.api.FoodHint, barcode: String?): FoodItem {
        val food = hint.food
        val nutrients = food.nutrients
        
        return FoodItem(
            barcode = barcode ?: "NAME_${System.currentTimeMillis()}_${(0..999).random()}",
            name = food.label,
            brand = food.brand,
            servingSize = food.servingSize?.let { "${it} ${food.servingSizeUnit ?: "g"}" },
            caloriesPerServing = nutrients.ENERC_KCAL?.toInt() ?: 0,
            proteinPerServing = nutrients.PROCNT,
            carbsPerServing = nutrients.CHOCDF,
            fatPerServing = nutrients.FAT,
            fiberPerServing = nutrients.FIBTG,
            sugarPerServing = nutrients.SUGAR,
            sodiumPerServing = nutrients.NA
        )
    }
    
    /**
     * Get a list of common foods to store on the device for offline use
     * This is called when the user first opens the app or wants to update their food database
     * The region parameter determines which country's common foods to download
     */
    fun getCommonFoodItems(region: String = "US"): List<FoodItem> {
        // Choose which foods to load based on the user's region
        // Different countries eat different common foods
        return when (region.uppercase()) {
            "US", "USA" -> getUSCommonFoods()
            "UK", "UNITED_KINGDOM" -> getUKCommonFoods()
            "CA", "CANADA" -> getCanadianCommonFoods()
            "AU", "AUSTRALIA" -> getAustralianCommonFoods()
            else -> getUSCommonFoods() // Default to US foods if region not recognized
        }
    }
    
    /**
     * Get common foods typically eaten in the United States
     * These are foods you'd commonly find in American grocery stores and restaurants
     */
    private fun getUSCommonFoods(): List<FoodItem> {
        return listOf(
            // Fruits
            FoodItem("0000000000001", "Apple", "Generic", "1 medium (182g)", 95, 0.5, 25.1, 0.3, 4.4, 18.9, 2.0),
            FoodItem("0000000000002", "Banana", "Generic", "1 medium (118g)", 105, 1.3, 27.0, 0.4, 3.1, 14.4, 1.0),
            FoodItem("0000000000003", "Orange", "Generic", "1 medium (154g)", 62, 1.2, 15.4, 0.2, 3.1, 12.2, 0.0),
            FoodItem("0000000000004", "Grapes", "Generic", "1 cup (151g)", 104, 1.1, 27.3, 0.2, 1.4, 23.4, 3.0),
            FoodItem("0000000000005", "Strawberries", "Generic", "1 cup (152g)", 49, 1.0, 11.7, 0.5, 3.0, 7.4, 1.5),
            FoodItem("0000000000006", "Blueberries", "Generic", "1 cup (148g)", 84, 1.1, 21.5, 0.5, 3.6, 14.7, 1.5),
            FoodItem("0000000000007", "Watermelon", "Generic", "1 cup cubed (152g)", 46, 0.9, 11.5, 0.2, 0.6, 9.4, 1.5),
            FoodItem("0000000000008", "Pineapple", "Generic", "1 cup chunks (165g)", 82, 0.9, 21.6, 0.2, 2.3, 16.3, 1.7),
            
            // Proteins
            FoodItem("0000000000010", "Chicken Breast", "Generic", "100g", 165, 31.0, 0.0, 3.6, 0.0, 0.0, 74.0),
            FoodItem("0000000000011", "Chicken Thigh", "Generic", "100g", 209, 26.0, 0.0, 10.9, 0.0, 0.0, 84.0),
            FoodItem("0000000000012", "Ground Beef (85% lean)", "Generic", "100g", 250, 25.0, 0.0, 17.0, 0.0, 0.0, 78.0),
            FoodItem("0000000000013", "Salmon", "Generic", "100g", 208, 25.4, 0.0, 12.4, 0.0, 0.0, 59.0),
            FoodItem("0000000000014", "Tuna (Canned in Water)", "Generic", "100g", 116, 25.5, 0.0, 0.8, 0.0, 0.0, 247.0),
            FoodItem("0000000000015", "Shrimp", "Generic", "100g", 99, 24.0, 0.2, 0.3, 0.0, 0.0, 111.0),
            FoodItem("0000000000016", "Egg", "Generic", "1 large (50g)", 70, 6.3, 0.4, 4.8, 0.0, 0.2, 71.0),
            FoodItem("0000000000017", "Turkey Breast", "Generic", "100g", 135, 30.1, 0.0, 1.0, 0.0, 0.0, 64.0),
            FoodItem("0000000000018", "Pork Tenderloin", "Generic", "100g", 143, 26.0, 0.0, 3.5, 0.0, 0.0, 62.0),
            
            // Dairy
            FoodItem("0000000000020", "Milk (2%)", "Generic", "1 cup (244g)", 122, 8.1, 11.7, 4.8, 0.0, 11.7, 115.0),
            FoodItem("0000000000021", "Milk (Whole)", "Generic", "1 cup (244g)", 150, 7.9, 11.7, 7.9, 0.0, 11.7, 105.0),
            FoodItem("0000000000022", "Milk (Skim)", "Generic", "1 cup (245g)", 83, 8.3, 12.2, 0.2, 0.0, 12.2, 103.0),
            FoodItem("0000000000023", "Yogurt (Greek, Plain)", "Generic", "170g", 100, 17.3, 6.1, 0.7, 0.0, 6.1, 56.0),
            FoodItem("0000000000024", "Cheese (Cheddar)", "Generic", "28g", 113, 7.0, 0.4, 9.3, 0.0, 0.1, 174.0),
            FoodItem("0000000000025", "Cottage Cheese", "Generic", "1 cup (225g)", 163, 28.0, 6.2, 2.3, 0.0, 6.1, 918.0),
            FoodItem("0000000000026", "Butter", "Generic", "1 tbsp (14g)", 102, 0.1, 0.0, 11.5, 0.0, 0.0, 82.0),
            
            // Grains & Carbs
            FoodItem("0000000000030", "Rice (White, Cooked)", "Generic", "1 cup (158g)", 205, 4.3, 44.5, 0.4, 0.6, 0.1, 1.6),
            FoodItem("0000000000031", "Rice (Brown, Cooked)", "Generic", "1 cup (195g)", 216, 5.0, 44.8, 1.8, 3.5, 0.7, 10.0),
            FoodItem("0000000000032", "Bread (White)", "Generic", "1 slice (25g)", 67, 1.9, 12.7, 0.8, 0.8, 1.2, 127.4),
            FoodItem("0000000000033", "Bread (Whole Wheat)", "Generic", "1 slice (28g)", 81, 3.6, 13.8, 1.9, 1.9, 1.4, 144.0),
            FoodItem("0000000000034", "Pasta (Cooked)", "Generic", "1 cup (140g)", 220, 8.1, 43.2, 1.1, 2.5, 0.8, 1.0),
            FoodItem("0000000000035", "Quinoa (Cooked)", "Generic", "1 cup (185g)", 222, 8.1, 39.4, 3.6, 5.2, 0.9, 13.0),
            FoodItem("0000000000036", "Oatmeal", "Generic", "1 cup cooked (234g)", 154, 5.9, 27.4, 3.2, 4.0, 0.6, 2.0),
            FoodItem("0000000000037", "Bagel", "Generic", "1 medium (95g)", 277, 10.9, 54.4, 1.7, 2.3, 5.1, 443.0),
            
            // Vegetables
            FoodItem("0000000000040", "Broccoli", "Generic", "1 cup chopped (91g)", 25, 3.0, 5.1, 0.3, 2.3, 1.5, 33.0),
            FoodItem("0000000000041", "Spinach", "Generic", "1 cup (30g)", 7, 0.9, 1.1, 0.1, 0.7, 0.1, 24.0),
            FoodItem("0000000000042", "Carrots", "Generic", "1 medium (61g)", 25, 0.5, 5.8, 0.1, 1.7, 2.9, 42.0),
            FoodItem("0000000000043", "Bell Pepper", "Generic", "1 cup chopped (149g)", 30, 1.0, 7.0, 0.3, 2.5, 4.2, 4.0),
            FoodItem("0000000000044", "Tomato", "Generic", "1 medium (123g)", 22, 1.1, 4.8, 0.2, 1.5, 3.2, 6.0),
            FoodItem("0000000000045", "Cucumber", "Generic", "1 cup sliced (119g)", 16, 0.7, 4.0, 0.1, 0.5, 1.8, 2.0),
            FoodItem("0000000000046", "Lettuce", "Generic", "1 cup shredded (47g)", 5, 0.5, 1.0, 0.1, 0.6, 0.4, 4.0),
            FoodItem("0000000000047", "Onion", "Generic", "1 medium (110g)", 44, 1.2, 10.3, 0.1, 1.9, 4.7, 4.0),
            FoodItem("0000000000048", "Potato", "Generic", "1 medium (173g)", 161, 4.3, 36.6, 0.2, 3.8, 1.3, 8.0),
            FoodItem("0000000000049", "Sweet Potato", "Generic", "1 medium (112g)", 112, 2.0, 26.0, 0.1, 3.9, 5.4, 6.0),
            
            // Nuts & Seeds
            FoodItem("0000000000050", "Almonds", "Generic", "28g (23 nuts)", 164, 6.0, 6.1, 14.2, 3.5, 1.2, 1.0),
            FoodItem("0000000000051", "Walnuts", "Generic", "28g (14 halves)", 185, 4.3, 3.9, 18.5, 1.9, 0.7, 1.0),
            FoodItem("0000000000052", "Peanuts", "Generic", "28g", 161, 7.3, 4.6, 14.0, 2.4, 1.3, 5.0),
            FoodItem("0000000000053", "Cashews", "Generic", "28g", 157, 5.2, 8.6, 12.4, 0.9, 1.7, 3.0),
            FoodItem("0000000000054", "Sunflower Seeds", "Generic", "28g", 165, 5.8, 6.8, 14.1, 2.4, 0.8, 1.0),
            
            // Snacks & Processed Foods
            FoodItem("0000000000060", "Crackers (Saltine)", "Generic", "5 crackers (15g)", 64, 1.4, 10.6, 1.7, 0.4, 0.2, 135.0),
            FoodItem("0000000000061", "Chips (Potato)", "Generic", "28g", 152, 2.0, 15.0, 10.0, 1.3, 0.1, 149.0),
            FoodItem("0000000000062", "Pretzels", "Generic", "28g", 108, 2.8, 22.5, 0.8, 0.9, 0.5, 486.0),
            FoodItem("0000000000063", "Granola Bar", "Generic", "1 bar (24g)", 118, 2.5, 15.8, 4.9, 1.3, 6.0, 72.0),
            
            // Beverages
            FoodItem("0000000000070", "Coffee (Black)", "Generic", "1 cup (237g)", 2, 0.3, 0.0, 0.0, 0.0, 0.0, 5.0),
            FoodItem("0000000000071", "Tea (Green)", "Generic", "1 cup (245g)", 2, 0.5, 0.0, 0.0, 0.0, 0.0, 2.0),
            FoodItem("0000000000072", "Orange Juice", "Generic", "1 cup (248g)", 112, 1.7, 25.8, 0.5, 0.5, 20.8, 2.0),
            FoodItem("0000000000073", "Soda (Cola)", "Generic", "12 oz (355ml)", 140, 0.0, 39.0, 0.0, 0.0, 39.0, 45.0),
            
            // Condiments & Oils
            FoodItem("0000000000080", "Olive Oil", "Generic", "1 tbsp (13.5g)", 119, 0.0, 0.0, 13.5, 0.0, 0.0, 0.3),
            FoodItem("0000000000081", "Mayonnaise", "Generic", "1 tbsp (13.8g)", 94, 0.1, 0.1, 10.3, 0.0, 0.1, 105.0),
            FoodItem("0000000000082", "Ketchup", "Generic", "1 tbsp (17g)", 19, 0.2, 4.7, 0.0, 0.1, 3.7, 154.0),
            FoodItem("0000000000083", "Mustard", "Generic", "1 tsp (5g)", 3, 0.2, 0.3, 0.2, 0.2, 0.1, 57.0),
            
            // Beans & Legumes
            FoodItem("0000000000090", "Black Beans", "Generic", "1 cup cooked (172g)", 227, 15.2, 40.8, 0.9, 15.0, 0.6, 2.0),
            FoodItem("0000000000091", "Chickpeas", "Generic", "1 cup cooked (164g)", 269, 14.5, 45.0, 4.2, 12.5, 7.9, 11.0),
            FoodItem("0000000000092", "Lentils", "Generic", "1 cup cooked (198g)", 230, 17.9, 39.9, 0.8, 15.6, 3.6, 4.0),
            FoodItem("0000000000093", "Kidney Beans", "Generic", "1 cup cooked (177g)", 225, 15.3, 40.4, 0.9, 11.3, 0.6, 2.0),
            
            // Fast Food Approximations
            FoodItem("0000000000100", "Burger (Medium)", "Generic", "1 burger (150g)", 540, 25.0, 40.0, 31.0, 2.0, 4.0, 1040.0),
            FoodItem("0000000000101", "French Fries", "Generic", "Medium serving (115g)", 365, 4.0, 48.0, 17.0, 4.0, 0.3, 246.0),
            FoodItem("0000000000102", "Pizza Slice", "Generic", "1 slice (107g)", 285, 12.0, 36.0, 10.4, 2.3, 3.8, 640.0),
            FoodItem("0000000000103", "Chicken Nuggets", "Generic", "6 pieces (90g)", 270, 18.0, 13.0, 17.0, 1.0, 0.0, 540.0)
        )
    }
    
    /**
     * Get common foods typically eaten in the United Kingdom
     * These include traditional British foods and common UK grocery items
     */
    private fun getUKCommonFoods(): List<FoodItem> {
        return listOf(
            // UK Fruits (similar to US but some different varieties)
            FoodItem("1000000000001", "Apple (British)", "Generic", "1 medium (182g)", 95, 0.5, 25.1, 0.3, 4.4, 18.9, 2.0),
            FoodItem("1000000000002", "Banana", "Generic", "1 medium (118g)", 105, 1.3, 27.0, 0.4, 3.1, 14.4, 1.0),
            FoodItem("1000000000003", "Orange", "Generic", "1 medium (154g)", 62, 1.2, 15.4, 0.2, 3.1, 12.2, 0.0),
            
            // Traditional British Foods
            FoodItem("1000000000010", "Fish and Chips", "Generic", "1 serving (300g)", 585, 32.0, 45.0, 31.0, 3.0, 2.0, 950.0),
            FoodItem("1000000000011", "Baked Beans", "Heinz", "1/2 cup (130g)", 155, 7.7, 29.0, 0.6, 7.7, 10.4, 871.0),
            FoodItem("1000000000012", "Full English Breakfast", "Generic", "1 serving (400g)", 807, 39.0, 18.0, 65.0, 4.0, 3.0, 1680.0),
            FoodItem("1000000000013", "Shepherd's Pie", "Generic", "1 serving (300g)", 282, 18.0, 24.0, 13.0, 4.0, 6.0, 682.0),
            FoodItem("1000000000014", "Bangers and Mash", "Generic", "1 serving (350g)", 580, 22.0, 54.0, 29.0, 5.0, 4.0, 1200.0),
            FoodItem("1000000000015", "Sunday Roast Beef", "Generic", "1 serving (200g)", 380, 35.0, 8.0, 22.0, 2.0, 3.0, 420.0),
            
            // UK Dairy
            FoodItem("1000000000020", "Full Fat Milk", "Generic", "1 cup (244g)", 150, 7.9, 11.7, 7.9, 0.0, 11.7, 105.0),
            FoodItem("1000000000021", "Cheddar Cheese (Mature)", "Generic", "28g", 120, 7.5, 0.1, 10.0, 0.0, 0.1, 180.0),
            FoodItem("1000000000022", "Digestive Biscuits", "McVitie's", "2 biscuits (33g)", 141, 2.1, 20.9, 5.9, 1.4, 9.9, 220.0),
            
            // UK Breakfast Items
            FoodItem("1000000000030", "Porridge Oats", "Generic", "1 bowl (40g dry)", 379, 11.2, 60.0, 8.4, 9.0, 1.0, 6.0),
            FoodItem("1000000000031", "White Bread (Sliced)", "Hovis", "1 slice (36g)", 77, 2.7, 15.5, 0.6, 2.3, 1.8, 170.0),
            FoodItem("1000000000032", "Brown Bread", "Generic", "1 slice (36g)", 79, 3.5, 13.8, 1.2, 2.5, 1.4, 150.0),
            
            // UK Vegetables
            FoodItem("1000000000040", "Mushy Peas", "Generic", "1/2 cup (80g)", 99, 6.8, 16.0, 0.8, 5.1, 1.8, 380.0),
            FoodItem("1000000000041", "Brussels Sprouts", "Generic", "1 cup (88g)", 38, 3.0, 8.0, 0.3, 3.3, 1.9, 22.0),
            FoodItem("1000000000042", "Jacket Potato", "Generic", "1 medium (173g)", 161, 4.3, 36.6, 0.2, 3.8, 1.3, 8.0),
            
            // UK Snacks and Sweets
            FoodItem("1000000000050", "Chocolate Digestives", "McVitie's", "2 biscuits (33g)", 167, 2.0, 21.0, 8.3, 1.4, 11.9, 103.0),
            FoodItem("1000000000051", "Jaffa Cakes", "McVitie's", "2 cakes (24g)", 92, 1.0, 17.9, 1.2, 0.6, 14.2, 94.0),
            FoodItem("1000000000052", "Cadbury Dairy Milk", "Cadbury", "4 squares (25g)", 134, 2.0, 14.0, 7.3, 0.4, 14.0, 24.0),
            
            // UK Beverages
            FoodItem("1000000000060", "Tea (with milk)", "Generic", "1 cup (250g)", 37, 1.8, 3.5, 1.8, 0.0, 3.5, 11.0),
            FoodItem("1000000000061", "Ribena", "Ribena", "250ml", 95, 0.0, 22.5, 0.0, 0.0, 22.5, 15.0)
        )
    }
    
    /**
     * Get common foods typically eaten in Canada
     * Mix of British influences, American foods, and uniquely Canadian items
     */
    private fun getCanadianCommonFoods(): List<FoodItem> {
        return listOf(
            // Canadian Fruits (similar to US)
            FoodItem("2000000000001", "Apple", "Generic", "1 medium (182g)", 95, 0.5, 25.1, 0.3, 4.4, 18.9, 2.0),
            FoodItem("2000000000002", "Banana", "Generic", "1 medium (118g)", 105, 1.3, 27.0, 0.4, 3.1, 14.4, 1.0),
            FoodItem("2000000000003", "Blueberries (Wild)", "Generic", "1 cup (148g)", 84, 1.1, 21.5, 0.5, 3.6, 14.7, 1.5),
            
            // Canadian Specialties
            FoodItem("2000000000010", "Poutine", "Generic", "1 serving (300g)", 740, 20.0, 93.0, 27.0, 5.0, 2.0, 1680.0),
            FoodItem("2000000000011", "Maple Syrup", "Canadian", "2 tbsp (40g)", 108, 0.0, 28.0, 0.0, 0.0, 25.4, 2.4),
            FoodItem("2000000000012", "Tourtière", "Generic", "1 slice (150g)", 451, 18.0, 32.0, 26.0, 2.0, 1.0, 890.0),
            FoodItem("2000000000013", "Butter Tarts", "Generic", "1 tart (60g)", 267, 2.8, 39.0, 11.0, 0.8, 32.0, 147.0),
            FoodItem("2000000000014", "Canadian Bacon", "Generic", "2 slices (60g)", 89, 11.7, 1.0, 4.0, 0.0, 0.0, 719.0),
            
            // Canadian Dairy
            FoodItem("2000000000020", "2% Milk", "Generic", "1 cup (244g)", 122, 8.1, 11.7, 4.8, 0.0, 11.7, 115.0),
            FoodItem("2000000000021", "Canadian Cheddar", "Generic", "28g", 113, 7.0, 0.4, 9.3, 0.0, 0.1, 174.0),
            
            // Canadian Grains
            FoodItem("2000000000030", "Tim Hortons Donut (Glazed)", "Tim Hortons", "1 donut (75g)", 260, 4.0, 31.0, 13.0, 1.0, 13.0, 340.0),
            FoodItem("2000000000031", "Everything Bagel", "Generic", "1 bagel (95g)", 277, 10.9, 54.4, 1.7, 2.3, 5.1, 443.0),
            
            // Canadian Proteins
            FoodItem("2000000000040", "Atlantic Salmon", "Generic", "100g", 208, 25.4, 0.0, 12.4, 0.0, 0.0, 59.0),
            FoodItem("2000000000041", "Canadian Turkey", "Generic", "100g", 135, 30.1, 0.0, 1.0, 0.0, 0.0, 64.0)
        )
    }
    
    /**
     * Get common foods typically eaten in Australia
     * Mix of British influences, unique Australian foods, and fresh local produce
     */
    private fun getAustralianCommonFoods(): List<FoodItem> {
        return listOf(
            // Australian Fruits
            FoodItem("3000000000001", "Green Apple (Granny Smith)", "Generic", "1 medium (182g)", 95, 0.5, 25.1, 0.3, 4.4, 18.9, 2.0),
            FoodItem("3000000000002", "Banana", "Generic", "1 medium (118g)", 105, 1.3, 27.0, 0.4, 3.1, 14.4, 1.0),
            FoodItem("3000000000003", "Mango", "Generic", "1 cup sliced (165g)", 107, 1.4, 28.0, 0.5, 3.0, 22.5, 2.0),
            FoodItem("3000000000004", "Kiwi Fruit", "Generic", "1 medium (69g)", 42, 0.8, 10.1, 0.4, 2.1, 6.2, 2.0),
            
            // Australian Specialties  
            FoodItem("3000000000010", "Meat Pie", "Generic", "1 pie (175g)", 550, 16.0, 45.0, 32.0, 3.0, 4.0, 1200.0),
            FoodItem("3000000000011", "Sausage Roll", "Generic", "1 roll (120g)", 445, 12.0, 28.0, 31.0, 2.0, 2.0, 980.0),
            FoodItem("3000000000012", "Vegemite on Toast", "Generic", "2 slices (60g)", 195, 8.9, 25.4, 6.6, 3.8, 2.4, 1380.0),
            FoodItem("3000000000013", "Lamington", "Generic", "1 piece (65g)", 265, 3.2, 35.0, 12.0, 2.1, 25.0, 165.0),
            FoodItem("3000000000014", "Pavlova", "Generic", "1 slice (80g)", 210, 3.0, 48.0, 0.5, 0.2, 45.0, 45.0),
            FoodItem("3000000000015", "Anzac Biscuit", "Generic", "1 biscuit (20g)", 95, 1.2, 13.8, 4.0, 1.0, 6.5, 85.0),
            
            // Australian Proteins
            FoodItem("3000000000020", "Barramundi", "Generic", "100g", 136, 25.0, 0.0, 3.6, 0.0, 0.0, 65.0),
            FoodItem("3000000000021", "Kangaroo Meat", "Generic", "100g", 120, 24.0, 0.0, 1.8, 0.0, 0.0, 55.0),
            FoodItem("3000000000022", "Prawns", "Generic", "100g", 99, 24.0, 0.2, 0.3, 0.0, 0.0, 111.0),
            
            // Australian Dairy
            FoodItem("3000000000030", "Full Cream Milk", "Generic", "1 cup (244g)", 150, 7.9, 11.7, 7.9, 0.0, 11.7, 105.0),
            FoodItem("3000000000031", "Tim Tam", "Arnott's", "2 biscuits (35g)", 180, 2.0, 23.0, 9.0, 1.5, 16.0, 60.0),
            
            // Australian Vegetables
            FoodItem("3000000000040", "Sweet Potato", "Generic", "1 medium (112g)", 112, 2.0, 26.0, 0.1, 3.9, 5.4, 6.0),
            FoodItem("3000000000041", "Beetroot", "Generic", "1 cup sliced (136g)", 58, 2.2, 13.0, 0.2, 3.8, 9.2, 106.0),
            
            // Australian Beverages
            FoodItem("3000000000050", "Flat White Coffee", "Generic", "1 cup (240ml)", 120, 6.4, 8.7, 6.2, 0.0, 8.7, 95.0),
            FoodItem("3000000000051", "Milo (with milk)", "Nestlé", "1 cup (250ml)", 180, 9.0, 24.0, 5.5, 0.5, 22.0, 140.0)
        )
    }
    
    /**
     * Convert cached barcode item back to FoodItem format
     */
    private fun convertCacheToFoodItem(cachedItem: BarcodeCache): FoodItem {
        return FoodItem(
            barcode = cachedItem.barcode,
            name = cachedItem.name,
            brand = cachedItem.brand,
            servingSize = cachedItem.servingSize,
            caloriesPerServing = cachedItem.caloriesPerServing,
            proteinPerServing = cachedItem.proteinPerServing,
            carbsPerServing = cachedItem.carbsPerServing,
            fatPerServing = cachedItem.fatPerServing,
            fiberPerServing = cachedItem.fiberPerServing,
            sugarPerServing = cachedItem.sugarPerServing,
            sodiumPerServing = cachedItem.sodiumPerServing,
            lastUpdated = cachedItem.cachedAt
        )
    }
    
    /**
     * Cache a barcode lookup result for future offline access
     */
    private suspend fun cacheBarcodeLookup(foodItem: FoodItem, source: String) {
        try {
            val cacheItem = BarcodeCache(
                barcode = foodItem.barcode,
                name = foodItem.name,
                brand = foodItem.brand,
                servingSize = foodItem.servingSize,
                caloriesPerServing = foodItem.caloriesPerServing,
                proteinPerServing = foodItem.proteinPerServing,
                carbsPerServing = foodItem.carbsPerServing,
                fatPerServing = foodItem.fatPerServing,
                fiberPerServing = foodItem.fiberPerServing,
                sugarPerServing = foodItem.sugarPerServing,
                sodiumPerServing = foodItem.sodiumPerServing,
                cacheSource = source
            )
            
            database.barcodeCacheDao().cacheItem(cacheItem)
            Log.d("NetworkManager", "Cached barcode ${foodItem.barcode} from $source: ${foodItem.name}")
        } catch (e: Exception) {
            Log.e("NetworkManager", "Failed to cache barcode lookup", e)
        }
    }
    
    /**
     * Get cache statistics for debugging and user information
     */
    suspend fun getCacheStats(): List<CacheStats> {
        return try {
            database.barcodeCacheDao().getCacheStats()
        } catch (e: Exception) {
            Log.e("NetworkManager", "Failed to get cache stats", e)
            emptyList()
        }
    }
    
    /**
     * Clear old cached items (for maintenance)
     * Call this periodically to prevent cache from growing too large
     */
    suspend fun cleanOldCache(maxAgeInDays: Int = 30) {
        try {
            val cutoffTime = System.currentTimeMillis() - (maxAgeInDays * 24 * 60 * 60 * 1000L)
            database.barcodeCacheDao().deleteOldCachedItems(cutoffTime)
            Log.d("NetworkManager", "Cleaned cache items older than $maxAgeInDays days")
        } catch (e: Exception) {
            Log.e("NetworkManager", "Failed to clean old cache", e)
        }
    }
    
    /**
     * Search for food items by name using Open Food Facts and Edamam APIs
     * Returns a list of matching food items for dropdown suggestions
     */
    suspend fun searchFoodByName(query: String, limit: Int = 10): List<FoodItem> {
        Log.d("NetworkManager", "Searching for food by name: $query")

        val results = mutableListOf<FoodItem>()

        try {
            // Step 1: Search local OFFs database first (fast, offline, ranked)
            val localOFFs = database.openFoodFactsDao().searchFoodsDetailed(query, limit)
            if (localOFFs.isNotEmpty()) {
                Log.d("NetworkManager", "Found ${localOFFs.size} local OFFs results for: $query")
                results.addAll(localOFFs.map { convertOpenFoodFactsItemToFoodItem(it) })
            }

            // Step 2: Supplement with online results if we need more and have network
            if (results.size < limit && isNetworkAvailable()) {
                val remainingLimit = limit - results.size

                val onlineResults = searchOpenFoodFactsByName(query, remainingLimit)
                // Avoid duplicates by barcode
                val existingBarcodes = results.map { it.barcode }.toSet()
                results.addAll(onlineResults.filter { it.barcode !in existingBarcodes })

                if (results.size < limit) {
                    val edamamResults = searchEdamamByName(query, limit - results.size)
                    val barcodes = results.map { it.barcode }.toSet()
                    results.addAll(edamamResults.filter { it.barcode !in barcodes })
                }
            }

            Log.d("NetworkManager", "Found ${results.size} food items for query: $query")
            return results.take(limit)

        } catch (e: Exception) {
            Log.e("NetworkManager", "Error searching food by name: ${e.message}")
            return emptyList()
        }
    }
    
    /**
     * Search Open Food Facts by product name
     */
    private suspend fun searchOpenFoodFactsByName(query: String, limit: Int): List<FoodItem> {
        return try {
            val response = withContext(Dispatchers.IO) {
                val url = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=${query.replace(" ", "+")}&search_simple=1&action=process&json=1&page_size=$limit"
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute()
            }
            
            if (response.isSuccessful) {
                val jsonResponse = response.body?.string()
                Log.d("NetworkManager", "Open Food Facts name search response: ${jsonResponse?.take(200)}...")
                
                val parsedResponse = gson.fromJson(jsonResponse, OpenFoodFactsSearchResponse::class.java)
                
                parsedResponse.products?.mapNotNull { product ->
                    convertOpenFoodFactsToFoodItem(product, null)
                } ?: emptyList()
            } else {
                Log.w("NetworkManager", "Open Food Facts name search failed: ${response.code}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("NetworkManager", "Error in Open Food Facts name search: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Search Edamam by food name
     */
    private suspend fun searchEdamamByName(query: String, limit: Int): List<FoodItem> {
        return try {
            val response = nutritionApiService.searchFood(query, getEdamamAppId(), getEdamamAppKey())

            if (response.isSuccessful && response.body() != null) {
                val hints = response.body()?.hints ?: emptyList()
                Log.d("NetworkManager", "Edamam name search found ${hints.size} results")
                
                hints.take(limit).map { hint ->
                    convertToFoodItem(hint, null) // No barcode for name searches
                }
            } else {
                Log.w("NetworkManager", "Edamam name search failed: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("NetworkManager", "Error in Edamam name search: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Convert OpenFoodFactsItem to FoodItem for unified interface
     */
    private fun convertOpenFoodFactsItemToFoodItem(offItem: OpenFoodFactsItem): FoodItem {
        // Use energyKcal if available, otherwise convert from energyKj, fallback to 0
        val calories = when {
            offItem.energyKcal != null -> offItem.energyKcal.toInt()
            offItem.energyKj != null -> (offItem.energyKj / 4.184).toInt()
            else -> 0
        }
        
        return FoodItem(
            barcode = offItem.barcode,
            name = offItem.productName,
            brand = offItem.brands ?: "Unknown",
            servingSize = "100g", // OpenFoodFacts uses per 100g
            caloriesPerServing = calories,
            proteinPerServing = offItem.proteins ?: 0.0,
            carbsPerServing = offItem.carbohydrates ?: 0.0,
            fatPerServing = offItem.fat ?: 0.0,
            fiberPerServing = offItem.fiber ?: 0.0,
            sugarPerServing = offItem.sugars ?: 0.0,
            sodiumPerServing = offItem.sodium ?: ((offItem.salt ?: 0.0) * 400.0) // Use sodium directly or convert salt
        )
    }
}