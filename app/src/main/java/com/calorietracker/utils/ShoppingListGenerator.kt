package com.calorietracker.utils

import com.calorietracker.database.MealPlan
import com.calorietracker.database.ShoppingListItem
import com.calorietracker.repository.CalorieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for generating shopping lists from meal plans
 * Handles ingredient parsing, quantity consolidation, and smart categorization
 */
class ShoppingListGenerator(private val repository: CalorieRepository) {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Comprehensive ingredient categorization
    private val categoryMap = mapOf(
        // Produce
        "produce" to listOf("apple", "banana", "orange", "lettuce", "spinach", "tomato", "onion", "garlic", "carrot", 
                          "broccoli", "bell pepper", "cucumber", "celery", "potato", "avocado", "lemon", "lime"),
        
        // Dairy & Eggs
        "dairy" to listOf("milk", "cheese", "butter", "yogurt", "cream", "egg", "eggs", "sour cream"),
        
        // Meat & Seafood
        "meat" to listOf("chicken", "beef", "pork", "turkey", "fish", "salmon", "tuna", "shrimp", "bacon", "ham"),
        
        // Pantry Staples
        "pantry" to listOf("rice", "pasta", "bread", "flour", "sugar", "salt", "pepper", "oil", "vinegar", "honey"),
        
        // Frozen Foods
        "frozen" to listOf("frozen", "ice cream", "frozen vegetables", "frozen fruit"),
        
        // Beverages
        "beverages" to listOf("water", "juice", "soda", "coffee", "tea", "wine", "beer"),
        
        // Bakery
        "bakery" to listOf("bread", "bagel", "muffin", "cake", "cookies"),
        
        // Canned Goods
        "canned" to listOf("canned", "beans", "soup", "sauce", "broth"),
        
        // Condiments & Sauces
        "condiments" to listOf("ketchup", "mustard", "mayo", "hot sauce", "soy sauce", "salsa"),
        
        // Snacks
        "snacks" to listOf("chips", "crackers", "nuts", "granola", "popcorn")
    )

    /**
     * Generate shopping list from current week's meal plans
     */
    suspend fun generateFromCurrentWeek(): List<ShoppingListItem> = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val weekUtils = WeekUtils()
        val weekDates = weekUtils.getWeekDates(calendar)
        
        val startDate = dateFormatter.format(weekDates.first())
        val endDate = dateFormatter.format(weekDates.last())
        
        generateFromDateRange(startDate, endDate, "This Week")
    }

    /**
     * Generate shopping list from next week's meal plans
     */
    suspend fun generateFromNextWeek(): List<ShoppingListItem> = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        
        val weekUtils = WeekUtils()
        val weekDates = weekUtils.getWeekDates(calendar)
        
        val startDate = dateFormatter.format(weekDates.first())
        val endDate = dateFormatter.format(weekDates.last())
        
        generateFromDateRange(startDate, endDate, "Next Week")
    }

    /**
     * Generate shopping list from specific date range
     */
    suspend fun generateFromDateRange(startDate: String, endDate: String, tripName: String = "Meal Planning"): List<ShoppingListItem> = withContext(Dispatchers.IO) {
        val mealPlans = repository.getMealPlansForDateRange(startDate, endDate)
            .filter { it.ingredients != null && it.ingredients.isNotBlank() }
        
        if (mealPlans.isEmpty()) {
            return@withContext emptyList()
        }
        
        val consolidatedItems = consolidateIngredients(mealPlans, tripName)
        return@withContext consolidatedItems
    }

    /**
     * Generate shopping list from specific meal plans
     */
    suspend fun generateFromMealPlans(mealPlans: List<MealPlan>, tripName: String = "Selected Meals"): List<ShoppingListItem> = withContext(Dispatchers.IO) {
        val mealsWithIngredients = mealPlans.filter { it.ingredients != null && it.ingredients.isNotBlank() }
        return@withContext consolidateIngredients(mealsWithIngredients, tripName)
    }

    /**
     * Consolidate ingredients from multiple meal plans, handling quantities and duplicates
     */
    private fun consolidateIngredients(mealPlans: List<MealPlan>, tripName: String): List<ShoppingListItem> {
        val ingredientMap = mutableMapOf<String, ConsolidatedIngredient>()
        
        mealPlans.forEach { mealPlan ->
            val ingredients = parseIngredientsFromMealPlan(mealPlan)
            
            ingredients.forEach { ingredient ->
                val key = ingredient.name.lowercase().trim()
                
                if (ingredientMap.containsKey(key)) {
                    // Merge with existing ingredient
                    val existing = ingredientMap[key] ?: return@forEach
                    ingredientMap[key] = existing.copy(
                        totalQuantity = combineQuantities(existing.totalQuantity, ingredient.quantity),
                        mealPlanIds = existing.mealPlanIds + mealPlan.id,
                        servings = existing.servings + mealPlan.servings,
                        notes = combineNotes(existing.notes, ingredient.notes)
                    )
                } else {
                    // Add new ingredient
                    ingredientMap[key] = ConsolidatedIngredient(
                        name = ingredient.name,
                        totalQuantity = ingredient.quantity,
                        unit = ingredient.unit,
                        category = categorizeIngredient(ingredient.name),
                        mealPlanIds = listOf(mealPlan.id),
                        servings = mealPlan.servings,
                        estimatedCost = estimateIngredientCost(ingredient.name, ingredient.quantity),
                        notes = ingredient.notes
                    )
                }
            }
        }
        
        // Convert to shopping list items
        return ingredientMap.values.mapIndexed { index, consolidated ->
            ShoppingListItem(
                itemName = consolidated.name,
                quantity = consolidated.totalQuantity,
                unit = consolidated.unit,
                category = consolidated.category,
                notes = consolidated.notes,
                estimatedCost = consolidated.estimatedCost,
                shoppingTrip = tripName,
                servings = consolidated.servings,
                sortOrder = index
            )
        }.sortedWith(compareBy<ShoppingListItem> { it.category }.thenBy { it.itemName })
    }

    /**
     * Parse ingredients from a meal plan's ingredients JSON string
     */
    private fun parseIngredientsFromMealPlan(mealPlan: MealPlan): List<ParsedIngredient> {
        val ingredients = mutableListOf<ParsedIngredient>()
        
        try {
            // Try to parse as JSON array first
            val jsonArray = JSONArray(mealPlan.ingredients)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                ingredients.add(
                    ParsedIngredient(
                        name = item.getString("name"),
                        quantity = item.optString("quantity", "1"),
                        unit = item.optString("unit", ""),
                        notes = item.optString("notes", "")
                    )
                )
            }
        } catch (e: JSONException) {
            // Fall back to parsing as simple text
            val raw = mealPlan.ingredients ?: return ingredients
            ingredients.addAll(parseIngredientsFromText(raw))
        }
        
        return ingredients
    }

    /**
     * Parse ingredients from plain text (fallback method)
     */
    private fun parseIngredientsFromText(ingredientsText: String): List<ParsedIngredient> {
        return ingredientsText.split(",", "\n", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { ingredient ->
                val parts = ingredient.split(" ", limit = 3)
                when {
                    parts.size >= 3 && parts[0].matches(Regex("\\d+(\\.\\d+)?")) -> {
                        ParsedIngredient(
                            name = parts.drop(2).joinToString(" "),
                            quantity = parts[0],
                            unit = parts[1]
                        )
                    }
                    parts.size >= 2 && parts[0].matches(Regex("\\d+(\\.\\d+)?")) -> {
                        ParsedIngredient(
                            name = parts[1],
                            quantity = parts[0],
                            unit = ""
                        )
                    }
                    else -> {
                        ParsedIngredient(
                            name = ingredient,
                            quantity = "1",
                            unit = ""
                        )
                    }
                }
            }
    }

    /**
     * Categorize ingredient based on name
     */
    private fun categorizeIngredient(ingredientName: String): String {
        val lowerName = ingredientName.lowercase()
        
        categoryMap.forEach { (category, keywords) ->
            if (keywords.any { keyword -> lowerName.contains(keyword) }) {
                return category.replaceFirstChar { it.titlecaseChar() }
            }
        }
        
        return "Other"
    }

    /**
     * Combine quantities from multiple meal plans
     */
    private fun combineQuantities(quantity1: String, quantity2: String): String {
        // Simple quantity combination - can be enhanced with unit conversion
        val num1 = quantity1.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 1.0
        val num2 = quantity2.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 1.0
        
        val combined = num1 + num2
        
        // Preserve unit from first quantity
        val unit = quantity1.replace(Regex("[\\d.]"), "").trim()
        
        return if (unit.isNotBlank()) {
            "${combined.toInt()} $unit"
        } else {
            combined.toInt().toString()
        }
    }

    /**
     * Combine notes from multiple sources
     */
    private fun combineNotes(notes1: String?, notes2: String?): String? {
        return when {
            notes1.isNullOrBlank() && notes2.isNullOrBlank() -> null
            notes1.isNullOrBlank() -> notes2
            notes2.isNullOrBlank() -> notes1
            notes1 == notes2 -> notes1
            else -> "$notes1; $notes2"
        }
    }

    /**
     * Estimate cost for an ingredient (simplified implementation)
     */
    private fun estimateIngredientCost(ingredientName: String, quantity: String): Double {
        // Basic cost estimation - can be enhanced with real price data
        val lowerName = ingredientName.lowercase()
        val quantityNum = quantity.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 1.0
        
        val baseCost = when {
            lowerName.contains("meat") || lowerName.contains("chicken") || lowerName.contains("beef") -> 6.0
            lowerName.contains("fish") || lowerName.contains("salmon") -> 8.0
            lowerName.contains("cheese") -> 4.0
            lowerName.contains("milk") -> 3.0
            lowerName.contains("egg") -> 2.0
            categoryMap["produce"]?.any { lowerName.contains(it) } == true -> 1.5
            else -> 2.0
        }
        
        return baseCost * quantityNum.coerceAtMost(10.0) // Cap at reasonable quantity
    }

    /**
     * Data classes for ingredient parsing and consolidation
     */
    data class ParsedIngredient(
        val name: String,
        val quantity: String,
        val unit: String = "",
        val notes: String? = null
    )

    data class ConsolidatedIngredient(
        val name: String,
        val totalQuantity: String,
        val unit: String?,
        val category: String,
        val mealPlanIds: List<Long>,
        val servings: Int,
        val estimatedCost: Double?,
        val notes: String?
    )
}