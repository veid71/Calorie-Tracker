package com.calorietracker.nutrition

/**
 * This class contains the official daily nutrition recommendations for adults
 * These are based on government health guidelines (like the FDA in the US)
 * Think of this as a nutrition facts label that tells you how much of each nutrient you need per day
 */
object NutritionRecommendations {
    
    /**
     * Daily nutrition recommendations for different regions
     * Each region has slightly different recommendations based on their health authorities
     */
    
    // United States recommendations (based on FDA Daily Values)
    val US_RECOMMENDATIONS = mapOf(
        "calories" to 2000.0,      // Average adult needs about 2000 calories per day
        "protein" to 50.0,         // 50 grams of protein (builds and repairs muscles)
        "carbs" to 300.0,          // 300 grams of carbohydrates (main energy source)
        "fat" to 65.0,             // 65 grams of fat (needed for vitamins and energy)
        "fiber" to 25.0,           // 25 grams of fiber (helps digestion)
        "sugar" to 50.0,           // 50 grams of added sugar (LIMIT - less is better)
        "sodium" to 2300.0         // 2300 mg of sodium (LIMIT - too much causes high blood pressure)
    )
    
    // United Kingdom recommendations (based on NHS guidelines)
    val UK_RECOMMENDATIONS = mapOf(
        "calories" to 2000.0,      // Same calorie target as US
        "protein" to 55.0,         // Slightly higher protein recommendation
        "carbs" to 260.0,          // Slightly lower carbs than US
        "fat" to 70.0,             // Slightly higher fat allowance
        "fiber" to 30.0,           // Higher fiber target (very important for health)
        "sugar" to 30.0,           // Stricter sugar limit (much better for health)
        "sodium" to 2300.0         // Same sodium limit as US
    )
    
    // Canada recommendations (based on Health Canada)
    val CANADA_RECOMMENDATIONS = mapOf(
        "calories" to 2000.0,      // Standard calorie target
        "protein" to 50.0,         // Same as US
        "carbs" to 300.0,          // Same as US  
        "fat" to 65.0,             // Same as US
        "fiber" to 25.0,           // Same as US
        "sugar" to 50.0,           // Same as US
        "sodium" to 2300.0         // Same as US
    )
    
    // Australia recommendations (based on Australian Government Department of Health)
    val AUSTRALIA_RECOMMENDATIONS = mapOf(
        "calories" to 2000.0,      // Standard calorie target
        "protein" to 50.0,         // Standard protein target
        "carbs" to 310.0,          // Slightly higher carbs
        "fat" to 70.0,             // Slightly higher fat allowance
        "fiber" to 30.0,           // Higher fiber like UK
        "sugar" to 45.0,           // Moderate sugar limit
        "sodium" to 2000.0         // Stricter sodium limit (better for heart health)
    )
    
    /**
     * Get the nutrition recommendations for a specific region
     * If we don't have recommendations for that region, we use US as the default
     */
    fun getRecommendationsForRegion(region: String): Map<String, Double> {
        return when (region.uppercase()) {
            "US", "USA", "UNITED_STATES" -> US_RECOMMENDATIONS
            "UK", "UNITED_KINGDOM", "BRITAIN" -> UK_RECOMMENDATIONS  
            "CA", "CANADA" -> CANADA_RECOMMENDATIONS
            "AU", "AUSTRALIA" -> AUSTRALIA_RECOMMENDATIONS
            else -> US_RECOMMENDATIONS // Default to US if region not found
        }
    }
    
    /**
     * Get a list of all available regions that have nutrition data
     */
    fun getAvailableRegions(): List<Pair<String, String>> {
        return listOf(
            "US" to "United States",
            "UK" to "United Kingdom", 
            "CA" to "Canada",
            "AU" to "Australia"
        )
    }
    
    /**
     * Calculate what percentage of daily recommended value the user has consumed
     * For example: if user ate 25g protein and recommendation is 50g, this returns 50%
     */
    fun calculatePercentageOfRecommended(consumed: Double, nutrient: String, region: String): Double {
        val recommendations = getRecommendationsForRegion(region)
        val recommendedAmount = recommendations[nutrient.lowercase()] ?: return 0.0
        
        // Avoid division by zero
        if (recommendedAmount == 0.0) return 0.0
        
        return (consumed / recommendedAmount) * 100.0
    }
    
    /**
     * Check if the user has exceeded the recommended limit for nutrients that should be limited
     * Sugar and sodium are nutrients where "more" is not better
     */
    fun isOverRecommendedLimit(consumed: Double, nutrient: String, region: String): Boolean {
        val limitNutrients = listOf("sugar", "sodium") // These are nutrients we want to limit
        
        if (!limitNutrients.contains(nutrient.lowercase())) {
            return false // For other nutrients, there's no "over limit"
        }
        
        val recommendations = getRecommendationsForRegion(region)
        val recommendedLimit = recommendations[nutrient.lowercase()] ?: return false
        
        return consumed > recommendedLimit
    }
}