package com.calorietracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.calorietracker.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecipeCreateViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = CalorieDatabase.getDatabase(application)
    private val recipeDao = database.recipeDao()
    private val recipeIngredientDao = database.recipeIngredientDao()
    private val usdaFoodDao = database.usdaFoodItemDao()
    private val foodItemDao = database.foodItemDao()
    
    private val _ingredients = MutableLiveData<List<RecipeIngredient>>(emptyList())
    val ingredients: LiveData<List<RecipeIngredient>> = _ingredients
    
    private val _currentRecipe = MutableLiveData<Recipe?>()
    val currentRecipe: LiveData<Recipe?> = _currentRecipe
    
    private val _foodSearchResults = MutableLiveData<List<FoodSearchResult>>()
    val foodSearchResults: LiveData<List<FoodSearchResult>> = _foodSearchResults
    
    private val _selectedFood = MutableLiveData<FoodSearchResult?>()
    val selectedFood: LiveData<FoodSearchResult?> = _selectedFood
    
    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    /**
     * Load existing recipe for editing
     */
    fun loadRecipe(recipeId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recipe = recipeDao.getRecipeById(recipeId)
                val ingredients = recipeIngredientDao.getIngredientsForRecipeList(recipeId)
                
                _currentRecipe.postValue(recipe)
                _ingredients.postValue(ingredients)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    /**
     * Search for food items to add as ingredients
     */
    fun searchFood(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = mutableListOf<FoodSearchResult>()
                
                // Search USDA database
                val usdaResults = usdaFoodDao.searchFoods(query, 5)
                results.addAll(usdaResults.map { usda ->
                    FoodSearchResult(
                        name = usda.description,
                        calories = usda.calories.toInt(),
                        protein = usda.protein,
                        carbs = usda.carbohydrates,
                        fat = usda.fat,
                        unit = "100g",
                        source = "USDA"
                    )
                })
                
                // Search local food database
                val localResults = foodItemDao.searchFoodsByName(query, 5)
                results.addAll(localResults.map { food ->
                    FoodSearchResult(
                        name = food.name,
                        calories = food.caloriesPerServing,
                        protein = food.proteinPerServing ?: 0.0,
                        carbs = food.carbsPerServing ?: 0.0,
                        fat = food.fatPerServing ?: 0.0,
                        unit = food.servingSize,
                        source = "Local"
                    )
                })
                
                _foodSearchResults.postValue(results)
            } catch (e: Exception) {
                _foodSearchResults.postValue(emptyList())
            }
        }
    }
    
    /**
     * Add ingredient to the current recipe
     */
    fun addIngredient(ingredientName: String, quantity: Double, unit: String) {
        // Find matching food item for nutrition data
        val searchResults = _foodSearchResults.value ?: emptyList()
        val matchingFood = searchResults.find { it.name.contains(ingredientName, ignoreCase = true) }
        
        val ingredient = RecipeIngredient(
            recipeId = _currentRecipe.value?.id ?: 0,
            ingredientName = ingredientName,
            quantity = quantity,
            unit = unit,
            calories = matchingFood?.let { (it.calories * quantity / 100).toInt() } ?: 0,
            protein = matchingFood?.let { it.protein * quantity / 100 } ?: 0.0,
            carbs = matchingFood?.let { it.carbs * quantity / 100 } ?: 0.0,
            fat = matchingFood?.let { it.fat * quantity / 100 } ?: 0.0,
            fiber = 0.0,
            sugar = 0.0,
            sodium = 0.0,
            order = (_ingredients.value?.size ?: 0) + 1
        )
        
        val currentIngredients = _ingredients.value?.toMutableList() ?: mutableListOf()
        currentIngredients.add(ingredient)
        _ingredients.value = currentIngredients
    }
    
    /**
     * Remove ingredient from the recipe
     */
    fun removeIngredient(ingredient: RecipeIngredient) {
        val currentIngredients = _ingredients.value?.toMutableList() ?: mutableListOf()
        currentIngredients.remove(ingredient)
        _ingredients.value = currentIngredients
    }
    
    /**
     * Save a recipe (create new or update existing)
     */
    fun saveRecipe(recipe: Recipe) {
        val currentIngredients = _ingredients.value ?: emptyList()
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.postValue(true)
                
                if (recipe.id == 0L) {
                    // Create new recipe
                    val recipeId = recipeDao.insertRecipe(recipe)
                    
                    // Update ingredients with the recipe ID and insert them
                    val ingredientsWithRecipeId = currentIngredients.map { ingredient ->
                        ingredient.copy(recipeId = recipeId)
                    }
                    
                    recipeIngredientDao.insertIngredients(ingredientsWithRecipeId)
                } else {
                    // Update existing recipe
                    recipeDao.updateRecipe(recipe)
                    
                    // Delete existing ingredients and insert new ones
                    recipeIngredientDao.deleteIngredientsForRecipe(recipe.id)
                    
                    val ingredientsWithRecipeId = currentIngredients.map { ingredient ->
                        ingredient.copy(recipeId = recipe.id)
                    }
                    
                    recipeIngredientDao.insertIngredients(ingredientsWithRecipeId)
                }
                
                _saveResult.postValue(true)
                
            } catch (e: Exception) {
                _saveResult.postValue(false)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}

/**
 * Data class for food search results
 */
data class FoodSearchResult(
    val name: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val unit: String?,
    val source: String
)