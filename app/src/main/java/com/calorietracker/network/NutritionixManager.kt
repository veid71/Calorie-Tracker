package com.calorietracker.network

import android.content.Context
import android.util.Log
import com.calorietracker.database.FoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Manager for Nutritionix API integration
 * Handles restaurant chains, branded foods, and natural language food parsing
 */
class NutritionixManager(private val context: Context) {
    
    companion object {
        private const val TAG = "NutritionixManager"
        
        // API credentials loaded from Android Keystore secure storage
        private fun getAppId(context: Context): String {
            val secureManager = com.calorietracker.security.SecureApiKeyManager.getInstance(context)
            val secureKey = secureManager.getApiKey("nutritionix_app_id")
            if (!secureKey.isNullOrBlank()) return secureKey
            return try {
                val field = Class.forName("${context.packageName}.BuildConfig")
                    .getDeclaredField("NUTRITIONIX_APP_ID")
                field.get(null) as? String ?: ""
            } catch (e: Exception) {
                ""
            }
        }

        private fun getAppKey(context: Context): String {
            val secureManager = com.calorietracker.security.SecureApiKeyManager.getInstance(context)
            val secureKey = secureManager.getApiKey("nutritionix_app_key")
            if (!secureKey.isNullOrBlank()) return secureKey
            return try {
                val field = Class.forName("${context.packageName}.BuildConfig")
                    .getDeclaredField("NUTRITIONIX_APP_KEY")
                field.get(null) as? String ?: ""
            } catch (e: Exception) {
                ""
            }
        }
        
        // Popular restaurant chains and brands Nutritionix covers
        private val POPULAR_BRANDS = listOf(
            "McDonald's", "Starbucks", "Subway", "KFC", "Pizza Hut", "Domino's",
            "Taco Bell", "Burger King", "Wendy's", "Chipotle", "Panera Bread",
            "Dunkin' Donuts", "Chick-fil-A", "Papa John's", "Olive Garden"
        )
    }
    
    private val nutritionixService: NutritionixService by lazy {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(NutritionixService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NutritionixService::class.java)
    }
    
    /**
     * Search for foods using instant search (good for autocomplete)
     */
    suspend fun searchFoods(query: String, limit: Int = 10): List<FoodItem> {
        return withContext(Dispatchers.IO) {
            try {
                if (query.length < 2) return@withContext emptyList()
                
                Log.d(TAG, "Searching Nutritionix for: $query")
                
                val appId = getAppId(context)
                val appKey = getAppKey(context)
                
                if (appId.isEmpty() || appKey.isEmpty()) {
                    Log.w(TAG, "Nutritionix API credentials not configured")
                    return@withContext emptyList()
                }
                
                val response = nutritionixService.searchInstant(
                    appId = appId,
                    appKey = appKey,
                    query = query,
                    detailed = true
                )
                
                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    val results = mutableListOf<FoodItem>()
                    
                    // Add branded foods (restaurant chains, packaged products)
                    searchResponse?.branded?.take(limit / 2)?.forEach { brandedFood ->
                        results.add(convertBrandedFoodToFoodItem(brandedFood))
                    }
                    
                    // Add common foods
                    searchResponse?.common?.take(limit / 2)?.forEach { commonFood ->
                        results.add(convertCommonFoodToFoodItem(commonFood))
                    }
                    
                    Log.d(TAG, "Found ${results.size} foods from Nutritionix")
                    results.take(limit)
                } else {
                    Log.e(TAG, "Nutritionix search failed: ${response.code()}")
                    emptyList()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error searching Nutritionix", e)
                emptyList()
            }
        }
    }
    
    /**
     * Parse natural language food descriptions
     * Example: \"2 slices pepperoni pizza\", \"large Big Mac meal\"
     */
    suspend fun parseNaturalLanguage(query: String): List<FoodItem> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Parsing natural language: $query")
                
                val request = NutritionixNaturalRequest(
                    query = query,
                    timezone = "US/Eastern",
                    locale = "en_US"
                )
                
                val appId = getAppId(context)
                val appKey = getAppKey(context)
                
                if (appId.isEmpty() || appKey.isEmpty()) {
                    Log.w(TAG, "Nutritionix API credentials not configured")
                    return@withContext emptyList()
                }
                
                val response = nutritionixService.searchNatural(
                    appId = appId,
                    appKey = appKey,
                    request = request
                )
                
                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    val results = searchResponse?.foods?.map { food ->
                        convertNutritionixFoodToFoodItem(food)
                    } ?: emptyList()
                    
                    Log.d(TAG, "Parsed ${results.size} foods from natural language")
                    results
                } else {
                    Log.e(TAG, "Natural language parsing failed: ${response.code()}")
                    emptyList()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing natural language", e)
                emptyList()
            }
        }
    }
    
    /**
     * Get detailed information for a specific branded food item
     */
    suspend fun getFoodDetails(itemId: String): FoodItem? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting details for item: $itemId")
                
                val appId = getAppId(context)
                val appKey = getAppKey(context)
                
                if (appId.isEmpty() || appKey.isEmpty()) {
                    Log.w(TAG, "Nutritionix API credentials not configured")
                    return@withContext null
                }
                
                val response = nutritionixService.getItemDetails(
                    appId = appId,
                    appKey = appKey,
                    itemId = itemId
                )
                
                if (response.isSuccessful) {
                    val itemResponse = response.body()
                    val food = itemResponse?.foods?.firstOrNull()
                    
                    if (food != null) {
                        Log.d(TAG, "Got details for: ${food.food_name}")
                        convertNutritionixFoodToFoodItem(food)
                    } else {
                        Log.w(TAG, "No food details found for item: $itemId")
                        null
                    }
                } else {
                    Log.e(TAG, "Get food details failed: ${response.code()}")
                    null
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting food details", e)
                null
            }
        }
    }
    
    /**
     * Search within a specific restaurant chain or brand
     */
    suspend fun searchBrand(brandName: String, foodQuery: String = "", limit: Int = 10): List<FoodItem> {
        return withContext(Dispatchers.IO) {
            try {
                // First, search for the brand to get brand ID
                val appId = getAppId(context)
                val appKey = getAppKey(context)
                
                if (appId.isEmpty() || appKey.isEmpty()) {
                    Log.w(TAG, "Nutritionix API credentials not configured")
                    return@withContext emptyList()
                }
                
                val instantResponse = nutritionixService.searchInstant(
                    appId = appId,
                    appKey = appKey,
                    query = brandName,
                    detailed = false
                )
                
                if (!instantResponse.isSuccessful) {
                    return@withContext emptyList()
                }
                
                // Find the brand ID from branded foods
                val brandId = instantResponse.body()?.branded
                    ?.firstOrNull { it.brand_name_item_name?.contains(brandName, ignoreCase = true) == true }
                    ?.nix_brand_id
                
                if (brandId == null) {
                    Log.w(TAG, "Brand ID not found for: $brandName")
                    return@withContext emptyList()
                }
                
                Log.d(TAG, "Searching brand $brandName (ID: $brandId) for: $foodQuery")
                
                val response = nutritionixService.searchBrand(
                    appId = appId,
                    appKey = appKey,
                    brandId = brandId,
                    query = foodQuery.takeIf { it.isNotBlank() },
                    limit = limit
                )
                
                if (response.isSuccessful) {
                    val results = response.body()?.foods?.map { food ->
                        convertNutritionixFoodToFoodItem(food)
                    } ?: emptyList()
                    
                    Log.d(TAG, "Found ${results.size} items from $brandName")
                    results
                } else {
                    Log.e(TAG, "Brand search failed: ${response.code()}")
                    emptyList()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error searching brand", e)
                emptyList()
            }
        }
    }
    
    /**
     * Get list of popular restaurant brands available in Nutritionix
     */
    fun getPopularBrands(): List<String> = POPULAR_BRANDS
    
    /**
     * Check if API credentials are configured
     */
    fun isConfigured(): Boolean {
        val appId = getAppId(context)
        val appKey = getAppKey(context)
        return appId.isNotEmpty() && appKey.isNotEmpty()
    }
    
    /**
     * Convert Nutritionix branded food to our FoodItem format
     */
    private fun convertBrandedFoodToFoodItem(brandedFood: NutritionixBrandedFood): FoodItem {
        val displayName = buildString {
            brandedFood.brand_name_item_name?.let { append(it) }
                ?: run {
                    brandedFood.nix_item_name?.let { append(it) }
                    ?: append(brandedFood.food_name)
                }
        }
        
        return FoodItem(
            name = "$displayName (Nutritionix)",
            barcode = brandedFood.nix_item_id ?: generateFakeBarcode(displayName),
            caloriesPerServing = brandedFood.nf_calories?.toInt() ?: 0,
            proteinPerServing = brandedFood.nf_protein,
            carbsPerServing = brandedFood.nf_total_carbohydrate,
            fatPerServing = brandedFood.nf_total_fat,
            fiberPerServing = brandedFood.nf_dietary_fiber,
            sugarPerServing = brandedFood.nf_sugars,
            sodiumPerServing = brandedFood.nf_sodium,
            servingSize = brandedFood.serving_qty?.toString() ?: "1",
            brand = extractBrandName(brandedFood.brand_name_item_name)
        )
    }
    
    /**
     * Convert Nutritionix common food to our FoodItem format
     */
    private fun convertCommonFoodToFoodItem(commonFood: NutritionixCommonFood): FoodItem {
        return FoodItem(
            name = "${commonFood.food_name} (Nutritionix)",
            barcode = generateFakeBarcode(commonFood.food_name),
            caloriesPerServing = 0, // Will need to be fetched via natural language API
            servingSize = commonFood.serving_qty?.toString() ?: "1"
        )
    }
    
    /**
     * Convert Nutritionix detailed food to our FoodItem format
     */
    private fun convertNutritionixFoodToFoodItem(food: NutritionixFood): FoodItem {
        val displayName = buildString {
            food.brand_name?.let { append("$it ") }
            append(food.food_name)
        }
        
        return FoodItem(
            name = "$displayName (Nutritionix)",
            barcode = food.nix_item_id ?: food.upc ?: generateFakeBarcode(displayName),
            caloriesPerServing = food.nf_calories?.toInt() ?: 0,
            proteinPerServing = food.nf_protein,
            carbsPerServing = food.nf_total_carbohydrate,
            fatPerServing = food.nf_total_fat,
            fiberPerServing = food.nf_dietary_fiber,
            sugarPerServing = food.nf_sugars,
            sodiumPerServing = food.nf_sodium,
            servingSize = food.serving_qty?.toString() ?: "1",
            brand = food.brand_name ?: food.nix_brand_name
        )
    }
    
    /**
     * Extract brand name from combined brand + item name
     */
    private fun extractBrandName(brandNameItemName: String?): String? {
        if (brandNameItemName == null) return null
        
        // Common patterns: "McDonald's Big Mac", "Starbucks Frappuccino"
        for (brand in POPULAR_BRANDS) {
            if (brandNameItemName.startsWith(brand, ignoreCase = true)) {
                return brand
            }
        }
        
        // Try to extract brand from first part before common separators
        return brandNameItemName.split(" - ", ", ", " : ").firstOrNull()
    }
    
    /**
     * Generate a fake barcode for items without UPC/EAN
     */
    private fun generateFakeBarcode(name: String): String {
        return "NUTX${name.hashCode().toString().takeLast(8)}"
    }
}