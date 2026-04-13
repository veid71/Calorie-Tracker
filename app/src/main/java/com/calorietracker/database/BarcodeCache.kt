package com.calorietracker.database

// 🧰 ROOM DATABASE TOOLS
import androidx.room.Entity      // Tells Android this class represents a database table
import androidx.room.PrimaryKey  // Marks which field is the unique ID

/**
 * 📷 BARCODE CACHE - OFFLINE FOOD DATABASE FOR PREVIOUSLY SCANNED ITEMS
 * 
 * Hey future programmer! This class creates an offline food database by caching barcode scan results.
 * Think of this like having a personal food dictionary that remembers every product you've ever scanned.
 * 
 * 🎯 Why Do We Need This?
 * When you scan a barcode, we look up the food information from online APIs (like Open Food Facts).
 * But what happens when:
 * - You're on a plane with no internet?
 * - You're in a basement with poor cell service?
 * - The API is temporarily down?
 * - You want to scan the same product again?
 * 
 * The cache solves all these problems by storing a local copy of every food you've scanned!
 * 
 * 🔄 How The Cache Works:
 * 1. User scans a barcode (like "012345678901")
 * 2. App checks if this barcode is in the cache first
 * 3. If found in cache → return instantly (super fast!)
 * 4. If not in cache → look up online, then save result to cache for next time
 * 5. Next time user scans same product → cache hit! (works offline)
 * 
 * 📦 What Food Information Do We Cache?
 * - Basic info: name, brand, serving size
 * - Complete nutrition facts: calories, protein, carbs, fat, fiber, sugar, sodium
 * - Metadata: where we got this info, when we cached it, when last accessed
 * 
 * ⚡ Cache Performance Benefits:
 * - Instant results for previously scanned items
 * - Reduced API calls (saves bandwidth and API limits)
 * - Works completely offline for cached items
 * - Consistent user experience even with poor internet
 * 
 * 🗂️ Cache Sources:
 * We track where each food's data came from so we know how reliable it is:
 * - "open_food_facts": From Open Food Facts database (user-contributed)
 * - "edamam": From Edamam API (professionally curated)
 * - "usda": From USDA Food Database (government data)
 * - "nutritionix": From Nutritionix API (restaurant chains)
 * 
 * 🧹 Cache Maintenance:
 * - Track when items were last accessed to clean up unused entries
 * - Remove very old cached items to prevent database bloat
 * - Update cached items periodically to get latest nutrition info
 * 
 * @property barcode The barcode number that uniquely identifies this product (like "012345678901")
 * @property name The product name (like "Coca-Cola Classic")
 * @property brand The brand name (like "Coca-Cola" - can be unknown/null)
 * @property servingSize The serving size description (like "1 can (355ml)" - can be unknown)
 * @property caloriesPerServing Calories per serving (like 140 for a can of Coke)
 * @property proteinPerServing Protein in grams per serving (like 0.0 for soda - can be unknown)
 * @property carbsPerServing Carbohydrates in grams per serving (like 39.0 for Coke - can be unknown)
 * @property fatPerServing Fat in grams per serving (like 0.0 for soda - can be unknown)
 * @property fiberPerServing Fiber in grams per serving (like 0.0 for soda - can be unknown)
 * @property sugarPerServing Sugar in grams per serving (like 39.0 for Coke - can be unknown)
 * @property sodiumPerServing Sodium in milligrams per serving (like 45.0 for Coke - can be unknown)
 * @property cacheSource Which API provided this data (like "open_food_facts", "edamam", "usda")
 * @property cachedAt When we first saved this to our cache (computer timestamp)
 * @property lastAccessed When someone last looked up this barcode (for cleanup maintenance)
 */
// 🏷️ DATABASE TABLE SETUP
// @Entity tells Android "this class should become a table in our database"
// tableName = "barcode_cache" means the table will be called "barcode_cache"
@Entity(tableName = "barcode_cache")
data class BarcodeCache(
    // 🔑 PRIMARY KEY - The unique ID for this cached food item
    // We use barcode as the primary key because each barcode should only appear once in our cache
    // If we cache the same barcode again, it replaces the old cached data (updating the info)
    @PrimaryKey 
    val barcode: String, // 📷 The barcode number (like "012345678901")
    
    // 🍎 BASIC FOOD INFORMATION
    val name: String, // 🏷️ Product name (like "Coca-Cola Classic" or "Oreo Cookies Original")
    val brand: String? = null, // 🏢 Brand name (like "Coca-Cola" or "Nabisco" - may be unknown)
    val servingSize: String? = null, // 📏 Serving description (like "1 can (355ml)" or "2 cookies" - may be unknown)
    
    // 🔥 MAIN NUTRITION DATA
    val caloriesPerServing: Int, // ⚡ Energy per serving (like 140 calories for a can of Coke)
    
    // 💪 DETAILED MACRONUTRIENTS - The "big three" nutrients (may be unknown for some products)
    val proteinPerServing: Double? = null, // 💪 Protein in grams (like 0.0 for soda, 2.0 for cookies)
    val carbsPerServing: Double? = null,   // 🍞 Carbohydrates in grams (like 39.0 for Coke, 25.0 for cookies)
    val fatPerServing: Double? = null,     // 🥑 Fat in grams (like 0.0 for soda, 7.0 for cookies)
    
    // 🌿 ADDITIONAL NUTRITION DETAILS - Smaller amounts but important for health
    val fiberPerServing: Double? = null,   // 🌾 Dietary fiber in grams (like 0.0 for soda, 1.0 for cookies)
    val sugarPerServing: Double? = null,   // 🍯 Sugar in grams (like 39.0 for Coke, 14.0 for cookies)
    val sodiumPerServing: Double? = null,  // 🧂 Sodium in milligrams (like 45.0 for Coke, 150.0 for cookies)
    
    // 📋 CACHE METADATA - Information about the cached data itself
    val cacheSource: String, // 🌐 Which API provided this data ("open_food_facts", "edamam", "usda", etc.)
    val cachedAt: Long = System.currentTimeMillis(), // 📅 When we first cached this item (computer timestamp)
    val lastAccessed: Long = System.currentTimeMillis() // 👆 When someone last looked up this barcode (for maintenance)
)

/**
 * 🧮 HELPFUL EXTENSION FUNCTIONS FOR CACHE ANALYSIS
 * 
 * These functions add useful capabilities to our cached food items,
 * helping with cache maintenance and data quality assessment.
 */

/**
 * 🕐 CHECK IF CACHE ENTRY IS RECENT
 * 
 * Determines if this cached item is relatively fresh or if it might need updating.
 * Food nutrition information doesn't change often, but occasionally manufacturers
 * reformulate products or change serving sizes.
 * 
 * @param maxAgeInDays Maximum age in days before considering entry "old" (default 90 days)
 * @return True if cached recently, false if it's getting old
 */
fun BarcodeCache.isRecent(maxAgeInDays: Int = 90): Boolean {
    val maxAgeInMillis = maxAgeInDays * 24 * 60 * 60 * 1000L
    val currentTime = System.currentTimeMillis()
    return (currentTime - cachedAt) < maxAgeInMillis
}

/**
 * 📊 CHECK IF NUTRITION DATA IS COMPLETE
 * 
 * Determines if this cached entry has complete macronutrient information.
 * Some food databases have incomplete data, so we track data quality.
 * 
 * @return True if we have protein, carbs, and fat data; false if any are missing
 */
fun BarcodeCache.hasCompleteMacros(): Boolean = 
    proteinPerServing != null && carbsPerServing != null && fatPerServing != null

/**
 * 📈 CHECK IF ITEM IS FREQUENTLY ACCESSED
 * 
 * Determines if this is a "popular" item in the user's diet based on access frequency.
 * Frequently accessed items are prioritized for cache retention during cleanup.
 * 
 * @param recentDays Number of recent days to consider (default 30)
 * @return True if accessed recently, false if it hasn't been used lately
 */
fun BarcodeCache.isFrequentlyAccessed(recentDays: Int = 30): Boolean {
    val recentPeriod = recentDays * 24 * 60 * 60 * 1000L
    val currentTime = System.currentTimeMillis()
    return (currentTime - lastAccessed) < recentPeriod
}

/**
 * 🏷️ GET DISPLAY NAME WITH BRAND
 * 
 * Creates a user-friendly display name that includes brand information when available.
 * 
 * @return Formatted name like "Coca-Cola Classic" or "Oreo Cookies Original (Nabisco)"
 */
fun BarcodeCache.getDisplayName(): String {
    return if (brand != null && !name.lowercase().contains(brand.lowercase())) {
        "$name ($brand)"
    } else {
        name
    }
}

/**
 * ⭐ GET CACHE RELIABILITY SCORE
 * 
 * Provides a reliability score for this cached data based on source and age.
 * Higher scores indicate more reliable nutrition information.
 * 
 * @return Reliability score from 1-5 (5 = most reliable)
 */
fun BarcodeCache.getReliabilityScore(): Int {
    var score = when (cacheSource.lowercase()) {
        "usda" -> 5          // Government data - highest reliability
        "edamam" -> 4        // Professional curation - high reliability  
        "nutritionix" -> 4   // Restaurant data - high reliability
        "open_food_facts" -> 3 // User-contributed - moderate reliability
        else -> 2            // Unknown source - lower reliability
    }
    
    // Reduce score if data is very old (over 1 year)
    if (!isRecent(365)) {
        score = maxOf(1, score - 1)
    }
    
    // Reduce score if nutrition data is incomplete
    if (!hasCompleteMacros()) {
        score = maxOf(1, score - 1)
    }
    
    return score
}

/**
 * 📋 GET FORMATTED AGE STRING
 * 
 * Creates a human-readable string showing how long ago this was cached.
 * 
 * @return Age string like "2 days ago" or "3 weeks ago"
 */
fun BarcodeCache.getFormattedAge(): String {
    val currentTime = System.currentTimeMillis()
    val ageInMillis = currentTime - cachedAt
    val ageInDays = ageInMillis / (24 * 60 * 60 * 1000)
    
    return when {
        ageInDays < 1 -> "Today"
        ageInDays < 7 -> "${ageInDays} day${if (ageInDays == 1L) "" else "s"} ago"
        ageInDays < 30 -> "${ageInDays / 7} week${if (ageInDays < 14) "" else "s"} ago"
        ageInDays < 365 -> "${ageInDays / 30} month${if (ageInDays < 60) "" else "s"} ago"
        else -> "${ageInDays / 365} year${if (ageInDays < 730) "" else "s"} ago"
    }
}