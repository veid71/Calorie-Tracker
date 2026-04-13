package com.calorietracker.cache

import android.content.Context
import android.util.Log
import com.calorietracker.database.*
import com.calorietracker.network.NetworkManager
import com.calorietracker.network.NutritionixManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Comprehensive offline caching system for complete offline functionality
 * Handles local storage, cache validation, and intelligent sync strategies
 */
class OfflineCacheManager(
    private val context: Context,
    private val database: CalorieDatabase
) {
    
    companion object {
        private const val TAG = "OfflineCacheManager"
        private const val CACHE_VERSION = 1
        
        // Cache expiration times
        private const val FOOD_CACHE_HOURS = 24 * 7 // 1 week
        private const val BARCODE_CACHE_HOURS = 24 * 30 // 1 month
        private const val SEARCH_CACHE_HOURS = 24 // 1 day
        private const val BULK_DATA_CACHE_DAYS = 30 // 1 month
        
        // Cache size limits
        private const val MAX_SEARCH_CACHE_ENTRIES = 10000
        private const val MAX_FOOD_CACHE_ENTRIES = 50000
        private const val MAX_BARCODE_CACHE_ENTRIES = 25000
    }
    
    private val cacheMetadataDao = database.cacheMetadataDao()
    private val foodItemDao = database.foodItemDao()
    private val barcodeQueueDao = database.barcodeQueueDao()
    private val searchCacheDao = database.searchCacheDao()
    private val offlineFoodDao = database.offlineFoodDao()
    private val networkManager = NetworkManager(context)
    private val nutritionixManager = NutritionixManager(context)
    
    /**
     * Initialize offline cache system
     * Sets up cache directories and performs initial validation
     */
    suspend fun initializeCache(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing offline cache system")
                
                // Create cache directories
                val cacheDir = File(context.cacheDir, "food_cache")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                
                // Validate cache integrity
                validateCacheIntegrity()
                
                // Clean up old cache entries
                performCacheMaintenance()
                
                // Update cache metadata
                updateCacheMetadata("system_init", "Cache system initialized successfully")
                
                Log.d(TAG, "Cache initialization completed")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing cache", e)
                false
            }
        }
    }
    
    /**
     * Cache a food search result for offline use
     */
    suspend fun cacheFoodSearch(query: String, results: List<FoodItem>) {
        withContext(Dispatchers.IO) {
            try {
                // Store individual food items in offline food cache
                results.forEach { foodItem ->
                    val offlineFood = OfflineFood(
                        barcode = foodItem.barcode,
                        name = foodItem.name,
                        calories = foodItem.caloriesPerServing,
                        protein = foodItem.proteinPerServing,
                        carbs = foodItem.carbsPerServing,
                        fat = foodItem.fatPerServing,
                        fiber = foodItem.fiberPerServing,
                        sugar = foodItem.sugarPerServing,
                        sodium = foodItem.sodiumPerServing,
                        servingSize = foodItem.servingSize,
                        servingUnit = null, // Not available in current FoodItem
                        brand = foodItem.brand,
                        categories = null, // Not available in current FoodItem
                        source = determineSource(foodItem),
                        lastUpdated = System.currentTimeMillis()
                    )
                    
                    offlineFoodDao.insertOfflineFood(offlineFood)
                }
                
                // Store search result mapping
                val searchCache = SearchCache(
                    query = query.lowercase().trim(),
                    resultCount = results.size,
                    resultBarcodes = results.map { it.barcode }.joinToString(","),
                    timestamp = System.currentTimeMillis()
                )
                
                searchCacheDao.insertSearchCache(searchCache)
                
                Log.d(TAG, "Cached ${results.size} foods for query: $query")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error caching food search", e)
            }
        }
    }
    
    /**
     * Get cached food search results
     */
    suspend fun getCachedFoodSearch(query: String): List<FoodItem>? {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedQuery = query.lowercase().trim()
                val searchCache = searchCacheDao.getSearchCache(normalizedQuery)
                
                if (searchCache == null || isCacheExpired(searchCache.timestamp, SEARCH_CACHE_HOURS)) {
                    return@withContext null
                }
                
                // Get the cached food items
                val barcodes = searchCache.resultBarcodes.split(",")
                val foods = barcodes.mapNotNull { barcode ->
                    offlineFoodDao.getOfflineFoodByBarcode(barcode)?.toFoodItem()
                }
                
                if (foods.size == searchCache.resultCount) {
                    Log.d(TAG, "Retrieved ${foods.size} cached foods for query: $query")
                    foods
                } else {
                    Log.w(TAG, "Cache inconsistency for query: $query")
                    null
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting cached search", e)
                null
            }
        }
    }
    
    /**
     * Cache a barcode lookup result
     */
    suspend fun cacheBarcodeResult(barcode: String, foodItem: FoodItem?) {
        withContext(Dispatchers.IO) {
            try {
                if (foodItem != null) {
                    // Store the food item
                    val offlineFood = OfflineFood(
                        barcode = barcode,
                        name = foodItem.name,
                        calories = foodItem.caloriesPerServing,
                        protein = foodItem.proteinPerServing,
                        carbs = foodItem.carbsPerServing,
                        fat = foodItem.fatPerServing,
                        fiber = foodItem.fiberPerServing,
                        sugar = foodItem.sugarPerServing,
                        sodium = foodItem.sodiumPerServing,
                        servingSize = foodItem.servingSize,
                        servingUnit = null,
                        brand = foodItem.brand,
                        categories = null,
                        source = determineSource(foodItem),
                        lastUpdated = System.currentTimeMillis()
                    )
                    
                    offlineFoodDao.insertOfflineFood(offlineFood)
                    
                    // Also store in barcode cache
                    val barcodeCache = BarcodeCache(
                        barcode = barcode,
                        name = foodItem.name,
                        caloriesPerServing = foodItem.caloriesPerServing,
                        proteinPerServing = foodItem.proteinPerServing,
                        carbsPerServing = foodItem.carbsPerServing,
                        fatPerServing = foodItem.fatPerServing,
                        fiberPerServing = foodItem.fiberPerServing,
                        sugarPerServing = foodItem.sugarPerServing,
                        sodiumPerServing = foodItem.sodiumPerServing,
                        brand = foodItem.brand,
                        servingSize = foodItem.servingSize,
                        cacheSource = determineSource(foodItem),
                        cachedAt = System.currentTimeMillis(),
                        lastAccessed = System.currentTimeMillis()
                    )
                    
                    database.barcodeCacheDao().insertBarcodeCache(barcodeCache)
                }
                
                Log.d(TAG, "Cached barcode result: $barcode")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error caching barcode result", e)
            }
        }
    }
    
    /**
     * Get cached barcode result
     */
    suspend fun getCachedBarcodeResult(barcode: String): FoodItem? {
        return withContext(Dispatchers.IO) {
            try {
                // First check offline food cache
                val offlineFood = offlineFoodDao.getOfflineFoodByBarcode(barcode)
                if (offlineFood != null && !isCacheExpired(offlineFood.lastUpdated, BARCODE_CACHE_HOURS)) {
                    return@withContext offlineFood.toFoodItem()
                }
                
                // Fallback to barcode cache
                val barcodeCache = database.barcodeCacheDao().getBarcodeCache(barcode)
                if (barcodeCache != null && !isCacheExpired(barcodeCache.cachedAt, BARCODE_CACHE_HOURS)) {
                    return@withContext barcodeCache.toFoodItem()
                }
                
                null
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting cached barcode", e)
                null
            }
        }
    }
    
    /**
     * Pre-cache popular foods for better offline experience
     */
    suspend fun preCachePopularFoods(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Pre-caching popular foods")
                
                val popularQueries = listOf(
                    // Common foods
                    "apple", "banana", "rice", "chicken breast", "bread", "milk", "eggs",
                    "salmon", "broccoli", "pasta", "cheese", "yogurt", "oatmeal",
                    
                    // Popular brands/restaurants
                    "McDonald's Big Mac", "Starbucks coffee", "Subway sandwich",
                    "Pizza Hut pizza", "KFC chicken", "Coca Cola", "Pepsi",
                    "Cheerios", "Oreos", "Lay's chips"
                )
                
                var successCount = 0
                for (query in popularQueries) {
                    try {
                        // Try to get from network and cache
                        val results = networkManager.searchFoodByName(query, 5)
                        if (results.isNotEmpty()) {
                            cacheFoodSearch(query, results)
                            successCount++
                        }
                        
                        // Also try Nutritionix if available
                        if (nutritionixManager.isConfigured()) {
                            val nutritionixResults = nutritionixManager.searchFoods(query, 3)
                            if (nutritionixResults.isNotEmpty()) {
                                cacheFoodSearch("$query nutritionix", nutritionixResults)
                            }
                        }
                        
                        // Add small delay to avoid overwhelming APIs
                        kotlinx.coroutines.delay(100)
                        
                    } catch (e: Exception) {
                        Log.w(TAG, "Error pre-caching query: $query", e)
                    }
                }
                
                Log.d(TAG, "Pre-cached $successCount popular food queries")
                updateCacheMetadata("pre_cache", "Pre-cached $successCount popular foods")
                
                successCount > 0
                
            } catch (e: Exception) {
                Log.e(TAG, "Error pre-caching popular foods", e)
                false
            }
        }
    }
    
    /**
     * Get comprehensive offline statistics
     */
    suspend fun getOfflineStats(): OfflineStats {
        return withContext(Dispatchers.IO) {
            try {
                val totalCachedFoods = offlineFoodDao.getTotalFoodCount()
                val totalBarcodes = database.barcodeCacheDao().getTotalBarcodeCount()
                val totalSearches = searchCacheDao.getTotalSearchCount()
                val cacheSize = calculateCacheSize()
                val lastUpdate = cacheMetadataDao.getLatestCacheUpdate()?.timestamp ?: 0
                
                OfflineStats(
                    totalCachedFoods = totalCachedFoods,
                    totalCachedBarcodes = totalBarcodes,
                    totalCachedSearches = totalSearches,
                    cacheSizeMB = cacheSize / (1024 * 1024),
                    lastUpdateTime = lastUpdate,
                    isFullyOfflineCapable = totalCachedFoods > 1000
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting offline stats", e)
                OfflineStats()
            }
        }
    }
    
    /**
     * Clear all cache data
     */
    suspend fun clearAllCache(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Clearing all cache data")
                
                offlineFoodDao.deleteAllOfflineFoods()
                searchCacheDao.deleteAllSearchCache()
                database.barcodeCacheDao().deleteAllBarcodeCache()
                cacheMetadataDao.deleteAllCacheMetadata()
                
                // Clear cache files
                val cacheDir = File(context.cacheDir, "food_cache")
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                    cacheDir.mkdirs()
                }
                
                updateCacheMetadata("cache_clear", "All cache data cleared")
                Log.d(TAG, "Cache cleared successfully")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cache", e)
                false
            }
        }
    }
    
    /**
     * Validate cache integrity and fix any issues
     */
    private suspend fun validateCacheIntegrity() {
        try {
            // Remove orphaned search cache entries
            val orphanedSearches = searchCacheDao.getOrphanedSearches()
            if (orphanedSearches.isNotEmpty()) {
                Log.d(TAG, "Removing ${orphanedSearches.size} orphaned search cache entries")
                orphanedSearches.forEach { searchCacheDao.deleteSearchCache(it.query) }
            }
            
            // Remove duplicate food entries
            val duplicateFoods = offlineFoodDao.getDuplicateFoods()
            Log.d(TAG, "Found ${duplicateFoods.size} duplicate food entries")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error validating cache integrity", e)
        }
    }
    
    /**
     * Perform regular cache maintenance
     */
    private suspend fun performCacheMaintenance() {
        try {
            val now = System.currentTimeMillis()
            
            // Remove expired search cache
            val expiredSearchTime = now - (SEARCH_CACHE_HOURS * 3600 * 1000)
            searchCacheDao.deleteExpiredSearchCache(expiredSearchTime)
            
            // Remove expired barcode cache
            val expiredBarcodeTime = now - (BARCODE_CACHE_HOURS * 3600 * 1000)
            database.barcodeCacheDao().deleteExpiredBarcodeCache(expiredBarcodeTime)
            
            // Remove old offline foods if we're over the limit
            val totalFoods = offlineFoodDao.getTotalFoodCount()
            if (totalFoods > MAX_FOOD_CACHE_ENTRIES) {
                val excessCount = totalFoods - MAX_FOOD_CACHE_ENTRIES
                offlineFoodDao.deleteOldestFoods(excessCount)
                Log.d(TAG, "Removed $excessCount old cached foods")
            }
            
            Log.d(TAG, "Cache maintenance completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error performing cache maintenance", e)
        }
    }
    
    /**
     * Calculate total cache size in bytes
     */
    private suspend fun calculateCacheSize(): Long {
        return try {
            val cacheDir = File(context.cacheDir, "food_cache")
            if (cacheDir.exists()) {
                cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Check if cache entry is expired
     */
    private fun isCacheExpired(timestamp: Long, maxAgeHours: Int): Boolean {
        val ageHours = (System.currentTimeMillis() - timestamp) / (3600 * 1000)
        return ageHours > maxAgeHours
    }
    
    /**
     * Determine the source of a food item
     */
    private fun determineSource(foodItem: FoodItem): String {
        return when {
            foodItem.name.contains("(saved)", ignoreCase = true) -> "Local Database"
            foodItem.name.contains("Nutritionix", ignoreCase = true) -> "Nutritionix"
            foodItem.name.contains("USDA", ignoreCase = true) -> "USDA"
            else -> "Open Food Facts"
        }
    }
    
    /**
     * Update cache metadata
     */
    private suspend fun updateCacheMetadata(operation: String, details: String) {
        try {
            val metadata = CacheMetadata(
                operation = operation,
                timestamp = System.currentTimeMillis(),
                details = details,
                cacheVersion = CACHE_VERSION
            )
            cacheMetadataDao.insertCacheMetadata(metadata)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cache metadata", e)
        }
    }
}

/**
 * Data class for offline statistics
 */
data class OfflineStats(
    val totalCachedFoods: Int = 0,
    val totalCachedBarcodes: Int = 0,
    val totalCachedSearches: Int = 0,
    val cacheSizeMB: Long = 0,
    val lastUpdateTime: Long = 0,
    val isFullyOfflineCapable: Boolean = false
)

/**
 * Extension function to convert OfflineFood to FoodItem
 */
private fun OfflineFood.toFoodItem(): FoodItem {
    return FoodItem(
        name = this.name,
        barcode = this.barcode,
        caloriesPerServing = this.calories,
        proteinPerServing = this.protein,
        carbsPerServing = this.carbs,
        fatPerServing = this.fat,
        fiberPerServing = this.fiber,
        sugarPerServing = this.sugar,
        sodiumPerServing = this.sodium,
        brand = this.brand,
        servingSize = this.servingSize
    )
}

/**
 * Extension function to convert BarcodeCache to FoodItem
 */
private fun BarcodeCache.toFoodItem(): FoodItem {
    return FoodItem(
        name = this.name,
        barcode = this.barcode,
        caloriesPerServing = this.caloriesPerServing,
        proteinPerServing = this.proteinPerServing,
        carbsPerServing = this.carbsPerServing,
        fatPerServing = this.fatPerServing,
        fiberPerServing = this.fiberPerServing,
        sugarPerServing = this.sugarPerServing,
        sodiumPerServing = this.sodiumPerServing,
        brand = this.brand,
        servingSize = this.servingSize
    )
}