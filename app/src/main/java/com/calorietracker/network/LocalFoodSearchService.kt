package com.calorietracker.network

import com.calorietracker.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * Service for searching local food databases
 * Combines results from USDA and Open Food Facts databases
 */
class LocalFoodSearchService(
    private val database: CalorieDatabase
) {
    
    data class CombinedFoodResult(
        val id: String,
        val name: String,
        val brand: String? = null,
        val source: String, // "usda" or "openfoodfacts"
        val calories: Double? = null,
        val protein: Double? = null,
        val carbs: Double? = null,
        val fat: Double? = null,
        val fiber: Double? = null,
        val sugar: Double? = null,
        val sodium: Double? = null,
        val servingInfo: String? = null,
        val barcode: String? = null,
        val imageUrl: String? = null,
        val nutritionGrade: String? = null
    )
    
    /**
     * Search both USDA and Open Food Facts databases
     */
    suspend fun searchFoods(query: String, limit: Int = 20): List<CombinedFoodResult> {
        return withContext(Dispatchers.IO) {
            // Search both databases concurrently
            val usdaDeferred = async { searchUSDAFoods(query, limit / 2) }
            val offDeferred = async { searchOpenFoodFacts(query, limit / 2) }
            
            val usdaResults = usdaDeferred.await()
            val offResults = offDeferred.await()
            
            // Combine and sort results
            val allResults = (usdaResults + offResults)
                .sortedWith(compareBy<CombinedFoodResult> { result ->
                    // Prioritize exact matches and better data quality
                    when {
                        result.name.equals(query, ignoreCase = true) -> 1
                        result.name.startsWith(query, ignoreCase = true) -> 2
                        result.brand?.contains(query, ignoreCase = true) == true -> 3
                        else -> 4
                    }
                }.thenBy { it.name })
                .take(limit)
            
            allResults
        }
    }
    
    /**
     * Search by barcode in Open Food Facts database
     */
    suspend fun searchByBarcode(barcode: String): CombinedFoodResult? {
        return withContext(Dispatchers.IO) {
            val offItem = database.openFoodFactsDao().getFoodByBarcode(barcode)
            offItem?.let { convertOpenFoodFactsItem(it) }
        }
    }
    
    /**
     * Get food suggestions by category
     */
    suspend fun getFoodsByCategory(category: String, limit: Int = 30): List<CombinedFoodResult> {
        return withContext(Dispatchers.IO) {
            // Search USDA by food category
            val usdaDeferred = async {
                database.usdaFoodItemDao().getFoodsByCategory(category, limit / 2)
                    .map { convertUSDAItem(it) }
            }
            
            // Search Open Food Facts by category
            val offDeferred = async {
                database.openFoodFactsDao().getFoodsByCategory(category, limit / 2)
                    .map { convertOpenFoodFactsItem(it) }
            }
            
            val usdaResults = usdaDeferred.await()
            val offResults = offDeferred.await()
            
            (usdaResults + offResults).take(limit)
        }
    }
    
    /**
     * Get popular/common foods for quick access
     */
    suspend fun getPopularFoods(limit: Int = 20): List<CombinedFoodResult> {
        return withContext(Dispatchers.IO) {
            // Get common food items from both databases
            val commonQueries = listOf("apple", "banana", "chicken", "rice", "bread", "milk", "egg")
            val results = mutableListOf<CombinedFoodResult>()
            
            for (query in commonQueries) {
                if (results.size >= limit) break
                val searchResults = searchFoods(query, 3)
                results.addAll(searchResults)
            }
            
            results.distinctBy { it.name }.take(limit)
        }
    }
    
    /**
     * Get database statistics
     */
    suspend fun getDatabaseStats(): DatabaseStats {
        return withContext(Dispatchers.IO) {
            val usdaCount = database.usdaFoodItemDao().getCount()
            val offCount = database.openFoodFactsDao().getCount()
            
            DatabaseStats(
                usdaItemCount = usdaCount,
                openFoodFactsCount = offCount,
                totalItems = usdaCount + offCount
            )
        }
    }
    
    private suspend fun searchUSDAFoods(query: String, limit: Int): List<CombinedFoodResult> {
        return database.usdaFoodItemDao().searchFoodsDetailed(query, limit)
            .map { convertUSDAItem(it) }
    }
    
    private suspend fun searchOpenFoodFacts(query: String, limit: Int): List<CombinedFoodResult> {
        return database.openFoodFactsDao().searchFoodsDetailed(query, limit)
            .map { convertOpenFoodFactsItem(it) }
    }
    
    private fun convertUSDAItem(item: USDAFoodItem): CombinedFoodResult {
        return CombinedFoodResult(
            id = "usda_${item.fdcId}",
            name = item.description,
            brand = item.brandOwner ?: item.brandName,
            source = "usda",
            calories = item.calories,
            protein = item.protein,
            carbs = item.carbohydrates,
            fat = item.fat,
            fiber = item.fiber,
            sugar = item.sugar,
            sodium = item.sodium,
            servingInfo = when {
                item.householdServingFullText != null -> item.householdServingFullText
                item.servingSize != null && item.servingSizeUnit != null -> 
                    "${item.servingSize} ${item.servingSizeUnit}"
                else -> "Per 100g"
            }
        )
    }
    
    private fun convertOpenFoodFactsItem(item: OpenFoodFactsItem): CombinedFoodResult {
        return CombinedFoodResult(
            id = "off_${item.id}",
            name = item.productName,
            brand = item.brands,
            source = "openfoodfacts",
            calories = item.energyKcal,
            protein = item.proteins,
            carbs = item.carbohydrates,
            fat = item.fat,
            fiber = item.fiber,
            sugar = item.sugars,
            sodium = item.sodium,
            servingInfo = item.servingSize ?: "Per 100g",
            barcode = item.barcode,
            imageUrl = item.imageUrl,
            nutritionGrade = item.nutritionGrade
        )
    }
    
    data class DatabaseStats(
        val usdaItemCount: Int,
        val openFoodFactsCount: Int,
        val totalItems: Int
    )
}