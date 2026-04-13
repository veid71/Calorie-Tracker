package com.calorietracker.repository

import android.util.Log

// 🧰 IMPORT ALL OUR TOOLS
import android.content.Context                          // Gives us access to Android system features
import androidx.lifecycle.LiveData                     // Special data that automatically updates UI
import com.calorietracker.database.*                   // All our database tables and entities
import com.calorietracker.network.NetworkManager       // Handles internet connections and API calls
import com.calorietracker.fitness.HealthConnectManager // Talks to fitness trackers and smartwatches
import com.calorietracker.fitness.FitnessData          // Stores workout information
import com.calorietracker.utils.CalorieCalculator      // Helper for calorie math
import java.text.SimpleDateFormat                      // Formats dates for storage and display
import java.time.LocalDate                             // Modern date handling
import java.time.format.DateTimeFormatter              // More date formatting tools
import java.util.*                                     // Date and time utilities

/**
 * 🏗️ CALORIE REPOSITORY - THE BRAIN OF OUR APP
 * 
 * Hey young programmer! This is the most important class in our entire app.
 * Think of it like the "manager" of a restaurant who coordinates everything:
 * 
 * 🍽️ What does this manager do?
 * 1. Takes orders from the app screens (like "save this food entry")
 * 2. Decides where to get the data (local database, internet, fitness tracker)
 * 3. Combines information from different sources
 * 4. Makes sure everything is saved properly
 * 5. Sends updates back to the app screens
 * 
 * 🧠 Why is this called a "Repository"?
 * In programming, a repository is like a librarian who knows where everything is stored.
 * Instead of every part of our app having to know about databases, internet APIs, etc.,
 * they just ask the repository: "Hey, can you get me today's food entries?"
 * 
 * 🔄 Data Sources this manager coordinates:
 * - 💾 Local Database: Our app's private storage (works offline!)
 * - 🌐 Internet APIs: Open Food Facts, USDA database (needs internet)
 * - ⌚ Fitness Trackers: Smartwatches via Health Connect (optional)
 * 
 * 🎯 Key Benefits:
 * - One place to get all nutrition data (no confusion about where things are)
 * - Automatically handles when internet is down (offline-first design)
 * - Smart caching so the app is fast
 * - Combines workout calories with food calories automatically
 * 
 * Think of this like having one super-smart assistant who handles all your data needs!
 */
class CalorieRepository(
    private val database: CalorieDatabase,  // 💾 Our local database (like a digital filing cabinet)
    private val context: Context? = null    // 📱 Android context (gives us access to system features)
) {
    
    // 🗂️ DATABASE ACCESS OBJECTS (DAOs) - These are like specialized file clerks
    // Each DAO knows how to work with one specific type of data in our database
    // Think of each one like a different department in a library:
    
    private val calorieEntryDao = database.calorieEntryDao()     // 🍎 Food entries department
    private val foodItemDao = database.foodItemDao()           // 🗃️ Food database department  
    private val dailyGoalDao = database.dailyGoalDao()         // 🎯 Daily goals department
    private val barcodeQueueDao = database.barcodeQueueDao()   // 📷 Barcode scanning queue department
    private val nutritionGoalsDao = database.nutritionGoalsDao() // 📊 Nutrition targets department
    private val workoutCaloriesDao = database.workoutCaloriesDao() // 💪 Workout data department
    private val weightEntryDao = database.weightEntryDao()     // ⚖️ Weight tracking department
    private val weightGoalDao = database.weightGoalDao()       // 🎯 Weight goals department
    private val favoriteMealDao = database.favoriteMealDao()   // ⭐ Favorite foods department
    private val barcodeHistoryDao = database.barcodeHistoryDao() // 📷 Scan history department
    private val progressPhotoDao = database.progressPhotoDao() // 📸 Progress photos department
    private val mealPlanDao = database.mealPlanDao()           // 📅 Meal planning department
    private val shoppingListDao = database.shoppingListDao()   // 🛒 Shopping lists department
    
    // 🌐 EXTERNAL SERVICE MANAGERS - These talk to the outside world
    private val networkManager = context?.let { NetworkManager(it) }           // 🌐 Internet API manager
    private val healthConnectManager = context?.let { HealthConnectManager(it) } // ⌚ Fitness tracker manager
    
    // 📋 BASIC FOOD ENTRY OPERATIONS
    // These are the fundamental things users do with food entries
    
    /**
     * 📅 GET TODAY'S FOOD ENTRIES
     * 
     * This function gets all the food you've logged today and presents it as "LiveData".
     * LiveData is magical - when you add new food, the UI automatically updates!
     * Think of it like a bulletin board that automatically shows new announcements.
     */
    fun getTodaysEntries(): LiveData<List<CalorieEntry>> {
        val today = getTodayDateString()  // Get today's date like "2024-08-31"
        return calorieEntryDao.getEntriesForDate(today)  // Ask the database for today's food
    }
    
    /**
     * ➕ ADD A NEW FOOD ENTRY
     * 
     * When someone logs a new food (like "I just ate an apple"), this saves it to the database.
     * The "suspend" keyword means this might take a moment, so we do it in the background.
     * 
     * @param entry The food entry to save (contains all the nutrition info)
     * @return The unique ID number assigned to this entry
     */
    suspend fun addCalorieEntry(entry: CalorieEntry): Long {
        return calorieEntryDao.insertEntry(entry)
    }
    
    /**
     * ✏️ UPDATE AN EXISTING FOOD ENTRY
     * 
     * When someone fixes a food entry (like changing that 14,000 calorie Coke to 140 calories),
     * this saves the corrected information back to the database.
     * 
     * @param entry The food entry with updated information
     */
    suspend fun updateCalorieEntry(entry: CalorieEntry) {
        calorieEntryDao.updateEntry(entry)
    }
    
    /**
     * 🗑️ DELETE A FOOD ENTRY
     * 
     * When someone wants to remove a food entry completely, this erases it from the database.
     * Once deleted, it's gone forever (like putting paper in the trash).
     * 
     * @param entry The food entry to remove
     */
    suspend fun deleteCalorieEntry(entry: CalorieEntry) {
        calorieEntryDao.deleteEntry(entry)
    }
    
    /**
     * 🧮 CALCULATE TODAY'S TOTAL CALORIES
     * 
     * This adds up all the calories from every food you've logged today.
     * For example: breakfast (300) + lunch (500) + snack (100) = 900 total calories
     * 
     * @return Total calories consumed today
     */
    suspend fun getTodaysTotalCalories(): Int {
        val today = getTodayDateString()  // Get today's date
        return calorieEntryDao.getTotalCaloriesForDate(today) ?: 0  // Add up all calories, or 0 if none
    }
    
    suspend fun getFoodItemByBarcode(barcode: String): FoodItem? {
        // First check local database
        var foodItem = foodItemDao.getFoodItemByBarcode(barcode)
        
        // If not found locally and network is available, try to fetch from API
        if (foodItem == null && networkManager != null) {
            foodItem = networkManager.searchFoodByBarcode(barcode)
            // Save to local database for offline use
            foodItem?.let { addFoodItem(it) }
        }
        
        return foodItem
    }
    
    suspend fun addFoodItem(foodItem: FoodItem) {
        foodItemDao.insertFoodItem(foodItem)
    }
    
    fun getDailyGoal(): LiveData<DailyGoal?> {
        return dailyGoalDao.getDailyGoal()
    }
    
    suspend fun setDailyGoal(calorieGoal: Int) {
        val goal = DailyGoal(calorieGoal = calorieGoal)
        dailyGoalDao.insertOrUpdateDailyGoal(goal)
    }
    
    suspend fun getDailyGoalSync(): DailyGoal? {
        return dailyGoalDao.getDailyGoalSync()
    }
    
    fun getEntriesForDateRange(startDate: String, endDate: String): LiveData<List<CalorieEntry>> {
        return calorieEntryDao.getEntriesForDateRange(startDate, endDate)
    }
    
    suspend fun getDailyCalorieSummary(startDate: String, endDate: String): List<DailyCalorieSummary> {
        return calorieEntryDao.getDailyCalorieSummary(startDate, endDate)
    }
    
    suspend fun searchFoodOnline(barcode: String): FoodItem? {
        return networkManager?.searchFoodByBarcode(barcode)
    }
    
    suspend fun searchFoodByName(query: String, limit: Int = 10): List<FoodItem> {
        val results = mutableListOf<FoodItem>()
        
        try {
            // First, search locally saved foods in SQLite database
            val localResults = searchLocalFoodsByName(query, limit)
            results.addAll(localResults.map { it.copy(name = "${it.name} (saved)") })
            
            // Then search online if we don't have enough results and network is available
            if (results.size < limit) {
                val networkAvailable = isNetworkAvailable()
                
                if (networkAvailable) {
                    val remainingLimit = limit - results.size
                    
                    val onlineResults = networkManager?.searchFoodByName(query, remainingLimit) ?: emptyList()
                    
                    // Log first few online results for debugging
                    onlineResults.take(3).forEachIndexed { index, foodItem ->
                    }
                    
                    results.addAll(onlineResults)
                } else {
                }
            } else {
            }
            
            val finalResults = results.take(limit)
            return finalResults
            
        } catch (e: Exception) {
            Log.e("CalorieRepository", "Unexpected error", e)
            return emptyList()
        }
    }
    
    /**
     * Search locally saved food items by name
     */
    suspend fun searchLocalFoodsByName(query: String, limit: Int = 10): List<FoodItem> {
        return foodItemDao.searchFoodsByName("%$query%", limit)
    }
    
    /**
     * Save a food item to SQLite database for offline use
     */
    suspend fun saveFoodItemToLocal(foodItem: FoodItem): Boolean {
        return try {
            // Check if this food already exists (by name to avoid duplicates)
            val existingFood = foodItemDao.getFoodItemByName(foodItem.name)
            if (existingFood == null) {
                foodItemDao.insertFoodItem(foodItem)
                true
            } else {
                // Food already exists, update it with newest nutrition data
                foodItemDao.updateFoodItem(foodItem.copy(barcode = existingFood.barcode))
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get count of saved foods in database
     */
    suspend fun getSavedFoodCount(): Int {
        return foodItemDao.getFoodItemCount()
    }
    
    fun isNetworkAvailable(): Boolean {
        return networkManager?.isNetworkAvailable() ?: false
    }
    
    suspend fun queueBarcodeForSync(barcode: String) {
        val queueItem = BarcodeQueue(barcode = barcode)
        barcodeQueueDao.insertBarcodeQueue(queueItem)
    }
    
    suspend fun syncQueuedBarcodes() {
        if (!isNetworkAvailable()) return
        
        val unsyncedBarcodes = barcodeQueueDao.getUnsyncedBarcodes()
        for (queueItem in unsyncedBarcodes) {
            try {
                val foodItem = networkManager?.searchFoodByBarcode(queueItem.barcode)
                if (foodItem != null) {
                    addFoodItem(foodItem)
                }
                barcodeQueueDao.markAsSynced(queueItem.barcode)
            } catch (e: Exception) {
                // Keep in queue for next sync attempt
            }
        }
        
        // Clean up old synced items (older than 30 days)
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        barcodeQueueDao.deleteSyncedOlderThan(thirtyDaysAgo)
    }
    
    // ===== NUTRITION GOALS METHODS =====
    // These methods help manage the user's daily nutrition targets and preferences
    
    /**
     * Get the user's nutrition goals (this automatically updates the UI when goals change)
     */
    fun getNutritionGoals(): LiveData<NutritionGoals?> {
        return nutritionGoalsDao.getNutritionGoals()
    }
    
    /**
     * Get the user's nutrition goals right now (for background tasks)
     */
    suspend fun getNutritionGoalsSync(): NutritionGoals? {
        return nutritionGoalsDao.getNutritionGoalsSync()
    }
    
    /**
     * Save or update the user's nutrition goals
     */
    suspend fun updateNutritionGoals(goals: NutritionGoals) {
        nutritionGoalsDao.insertOrUpdateNutritionGoals(goals)
    }
    
    /**
     * Update just the user's selected region (like changing from US to UK)
     */
    suspend fun updateUserRegion(region: String) {
        nutritionGoalsDao.updateRegion(region)
    }
    
    /**
     * Update just the calorie goal (most common change)
     */
    suspend fun updateCalorieGoal(calorieGoal: Int) {
        nutritionGoalsDao.updateCalorieGoal(calorieGoal)
    }
    
    /**
     * Calculate today's total nutrition intake for all nutrients
     * This adds up all the food the user ate today
     */
    suspend fun getTodaysNutritionTotals(): Map<String, Double> {
        val today = getTodayDateString()
        val entries = calorieEntryDao.getEntriesForDateSync(today)
        
        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0
        var totalFiber = 0.0
        var totalSugar = 0.0
        var totalSodium = 0.0
        
        // Add up all the nutrition from each food entry
        for (entry in entries) {
            totalCalories += entry.calories
            totalProtein += entry.protein ?: 0.0
            totalCarbs += entry.carbs ?: 0.0
            totalFat += entry.fat ?: 0.0
            totalFiber += entry.fiber ?: 0.0
            totalSugar += entry.sugar ?: 0.0
            totalSodium += entry.sodium ?: 0.0
        }
        
        // Return all the totals as a map (like a dictionary)
        return mapOf(
            "calories" to totalCalories,
            "protein" to totalProtein,
            "carbs" to totalCarbs,
            "fat" to totalFat,
            "fiber" to totalFiber,
            "sugar" to totalSugar,
            "sodium" to totalSodium
        )
    }
    
    // ===== FITNESS & WORKOUT CALORIES METHODS =====
    // These methods handle integration with Health Connect for workout data from OnePlus Watch 3
    
    /**
     * Check if Health Connect is available and we have permissions
     */
    suspend fun isHealthConnectAvailable(): Boolean {
        return healthConnectManager?.isHealthConnectAvailable() == true
    }
    
    /**
     * Check if we have all required Health Connect permissions
     */
    suspend fun hasHealthConnectPermissions(): Boolean {
        return healthConnectManager?.hasRequiredPermissions() == true
    }
    
    /**
     * Sync today's workout data from Health Connect and save to local database
     */
    suspend fun syncTodaysWorkoutData(): Boolean {
        return try {
            val today = LocalDate.now()
            syncWorkoutDataForDate(today)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Sync workout data for a specific date from Health Connect
     */
    suspend fun syncWorkoutDataForDate(date: LocalDate): Boolean {
        return try {
            
            val healthConnect = healthConnectManager
            if (healthConnect == null) {
                return false
            }
            
            // Check if Health Connect is ready with comprehensive diagnostics
            val isReady = healthConnect.isHealthConnectReady()
            if (!isReady) {
                return false
            }
            
            val fitnessData = healthConnect.getFitnessDataForDate(date)
            
            val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            
            // Check if we have existing test workout data before overwriting
            val existingData = getTodaysWorkoutCalories()
            val hasTestData = existingData != null && existingData.source?.contains("Test") == true && existingData.activeCaloriesBurned > 0
            
            if (hasTestData && fitnessData.activeCaloriesBurned == 0) {
                return true // Don't overwrite test data with empty Health Connect data
            }
            
            val workoutCalories = WorkoutCalories(
                date = dateString,
                activeCaloriesBurned = fitnessData.activeCaloriesBurned,
                totalCaloriesBurned = fitnessData.totalCaloriesBurned,
                exerciseMinutes = fitnessData.exerciseMinutes,
                exerciseType = fitnessData.primaryExerciseType,
                lastSyncTime = System.currentTimeMillis(),
                source = "Health Connect"
            )
            
            
            workoutCaloriesDao.insertWorkoutCalories(workoutCalories)
            
            // Verify the data was saved
            val savedData = getTodaysWorkoutCalories()
            
            true
        } catch (e: Exception) {
            Log.e("CalorieRepository", "Unexpected error", e)
            false
        }
    }
    
    /**
     * Get workout calories for today (cached from database)
     */
    suspend fun getTodaysWorkoutCalories(): WorkoutCalories? {
        val today = getTodayDateString()
        val result = workoutCaloriesDao.getWorkoutCaloriesForDate(today)
        return result
    }
    
    /**
     * Get workout calories for today as LiveData (for UI updates)
     */
    fun getTodaysWorkoutCaloriesLive(): LiveData<WorkoutCalories?> {
        val today = getTodayDateString()
        return workoutCaloriesDao.getWorkoutCaloriesForDateLive(today)
    }
    
    /**
     * Get adjusted calorie goal including workout bonus calories
     * This is the main method that shows how many extra calories you can eat based on workouts
     */
    suspend fun getAdjustedCalorieGoal(): Int {
        val baseGoal = getNutritionGoalsSync()?.calorieGoal ?: 2000
        val workoutCalories = getTodaysWorkoutCalories()?.activeCaloriesBurned ?: 0
        
        // Add 70% of active calories burned as bonus (conservative approach)
        val bonusCalories = (workoutCalories * 0.7).toInt()
        return baseGoal + bonusCalories
    }
    
    /**
     * Get workout calories for a date range (for analytics)
     */
    suspend fun getWorkoutCaloriesForDateRange(startDate: String, endDate: String): List<WorkoutCalories> {
        return workoutCaloriesDao.getWorkoutCaloriesForDateRange(startDate, endDate)
    }
    
    /**
     * Get workout calories for a date range as LiveData (for analytics charts)
     */
    fun getWorkoutCaloriesForDateRangeLive(startDate: String, endDate: String): LiveData<List<WorkoutCalories>> {
        return workoutCaloriesDao.getWorkoutCaloriesForDateRangeLive(startDate, endDate)
    }
    
    /**
     * Calculate total calories available to eat today (food goal + workout bonus)
     */
    suspend fun getTotalCaloriesAvailableToday(): Int {
        return getAdjustedCalorieGoal()
    }
    
    /**
     * Calculate remaining calories available to eat today
     */
    suspend fun getRemainingCaloriesToday(): Int {
        val totalAvailable = getTotalCaloriesAvailableToday()
        val consumed = getTodaysTotalCalories()
        return maxOf(0, totalAvailable - consumed)
    }
    
    /**
     * Get comprehensive daily summary including workout data
     */
    suspend fun getTodaysSummary(): DailySummary {
        
        val nutritionGoals = getNutritionGoalsSync()
        val baseCalorieGoal = nutritionGoals?.calorieGoal ?: 2000
        
        val consumedCalories = getTodaysTotalCalories()
        
        val workoutData = getTodaysWorkoutCalories()
        
        val workoutCalories = workoutData?.activeCaloriesBurned ?: 0
        
        val adjustedGoal = getAdjustedCalorieGoal()
        
        // Double-check the calculation manually
        val manualBonusCalories = (workoutCalories * 0.7).toInt()
        val manualAdjustedGoal = baseCalorieGoal + manualBonusCalories
        
        val summary = DailySummary(
            baseCalorieGoal = baseCalorieGoal,
            workoutCaloriesBurned = workoutCalories,
            adjustedCalorieGoal = adjustedGoal,
            consumedCalories = consumedCalories,
            remainingCalories = maxOf(0, adjustedGoal - consumedCalories),
            exerciseMinutes = workoutData?.exerciseMinutes ?: 0,
            primaryExerciseType = workoutData?.exerciseType
        )
        
        return summary
    }
    
    // ===== WEIGHT TRACKING METHODS =====
    
    suspend fun addWeightEntry(weight: Double, date: String = getTodayDateString(), notes: String? = null) {
        val weightEntry = WeightEntry(
            date = date,
            weight = weight,
            notes = notes
        )
        weightEntryDao.insertWeight(weightEntry)
    }
    
    suspend fun getLatestWeight(): WeightEntry? {
        return weightEntryDao.getLatestWeight()
    }
    
    fun getAllWeightEntries() = weightEntryDao.getAllWeightEntries()
    
    suspend fun getWeightEntriesInRange(startDate: String, endDate: String): List<WeightEntry> {
        return weightEntryDao.getWeightEntriesInRange(startDate, endDate)
    }
    
    // ===== WEIGHT GOAL METHODS =====
    
    suspend fun setWeightGoal(weightGoal: WeightGoal) {
        weightGoalDao.insertWeightGoal(weightGoal)
    }
    
    suspend fun getCurrentWeightGoal(): WeightGoal? {
        return weightGoalDao.getCurrentWeightGoalSync()
    }
    
    fun getCurrentWeightGoalLive() = weightGoalDao.getCurrentWeightGoal()
    
    suspend fun updateWeightGoal(weightGoal: WeightGoal) {
        weightGoalDao.updateWeightGoal(weightGoal)
    }
    
    suspend fun deleteWeightGoal() {
        weightGoalDao.deleteCurrentWeightGoal()
    }
    
    suspend fun getCalorieRecommendationFromWeightGoal(): CalorieCalculator.CalorieRecommendation? {
        val weightGoal = getCurrentWeightGoal() ?: return null
        val nutritionGoals = getNutritionGoalsSync()
        val useMetricUnits = nutritionGoals?.useMetricUnits ?: false
        
        return CalorieCalculator.calculateCalorieRecommendation(weightGoal, useMetricUnits)
    }
    
    private fun getTodayDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }
    
    /**
     * DEBUG/TEST METHOD: Add sample workout data for testing calorie adjustments
     * This method creates fake workout data to test if the calorie adjustment feature works
     */
    suspend fun addTestWorkoutData(activeCalories: Int = 350, exerciseMinutes: Int = 45, exerciseType: String = "Running") {
        val today = getTodayDateString()
        val testWorkoutData = WorkoutCalories(
            date = today,
            activeCaloriesBurned = activeCalories,
            totalCaloriesBurned = activeCalories + 1200, // Add some BMR calories
            exerciseMinutes = exerciseMinutes,
            exerciseType = exerciseType,
            lastSyncTime = System.currentTimeMillis(),
            source = "Test Data"
        )
        
        workoutCaloriesDao.insertWorkoutCalories(testWorkoutData)
    }
    
    /**
     * DEBUG/TEST METHOD: Clear workout data for testing
     */
    suspend fun clearTodaysWorkoutData() {
        val today = getTodayDateString()
        val emptyWorkoutData = WorkoutCalories(
            date = today,
            activeCaloriesBurned = 0,
            totalCaloriesBurned = 0,
            exerciseMinutes = 0,
            exerciseType = null,
            lastSyncTime = System.currentTimeMillis(),
            source = "Cleared"
        )
        
        workoutCaloriesDao.insertWorkoutCalories(emptyWorkoutData)
    }
    
    // =================== FAVORITE MEALS METHODS ===================
    
    /**
     * Get top favorite meals ordered by usage frequency
     */
    suspend fun getTopFavorites(limit: Int = 10): List<FavoriteMeal> {
        return favoriteMealDao.getTopFavorites(limit)
    }
    
    /**
     * Get favorite meals as LiveData for UI observation
     */
    fun getTopFavoritesLive(limit: Int = 10) = favoriteMealDao.getTopFavoritesFlow(limit)
    
    /**
     * Get recent favorite meals
     */
    suspend fun getRecentFavorites(limit: Int = 5): List<FavoriteMeal> {
        return favoriteMealDao.getRecentFavorites(limit)
    }
    
    /**
     * Search through favorite meals
     */
    suspend fun searchFavorites(query: String): List<FavoriteMeal> {
        return favoriteMealDao.searchFavorites(query)
    }
    
    /**
     * Add or update a favorite meal
     * If the food already exists in favorites, increment its usage count
     * Otherwise, create a new favorite entry
     */
    suspend fun addToFavorites(entry: CalorieEntry, category: String? = null): FavoriteMeal {
        // Check if this food is already in favorites
        val existing = if (entry.barcode != null) {
            favoriteMealDao.getFavoriteByBarcode(entry.barcode)
        } else {
            favoriteMealDao.getFavoriteByName(entry.foodName, null)
        }
        
        return if (existing != null) {
            // Update existing favorite
            favoriteMealDao.incrementUsage(existing.id)
            existing.copy(
                timesUsed = existing.timesUsed + 1,
                lastUsed = System.currentTimeMillis()
            )
        } else {
            // Create new favorite
            val favorite = FavoriteMeal(
                foodName = entry.foodName,
                calories = entry.calories,
                protein = entry.protein,
                carbs = entry.carbs,
                fat = entry.fat,
                fiber = entry.fiber,
                sugar = entry.sugar,
                sodium = entry.sodium,
                barcode = entry.barcode,
                category = category,
                timesUsed = 1,
                lastUsed = System.currentTimeMillis(),
                dateAdded = System.currentTimeMillis()
            )
            
            val id = favoriteMealDao.insertFavorite(favorite)
            favorite.copy(id = id)
        }
    }
    
    /**
     * Add food directly to favorites from food search results
     */
    suspend fun addFoodItemToFavorites(foodItem: FoodItem, category: String? = null): FavoriteMeal {
        val existing = favoriteMealDao.getFavoriteByBarcode(foodItem.barcode)
        
        return if (existing != null) {
            favoriteMealDao.incrementUsage(existing.id)
            existing
        } else {
            val favorite = FavoriteMeal(
                foodName = foodItem.name,
                brand = foodItem.brand,
                servingSize = foodItem.servingSize,
                calories = foodItem.caloriesPerServing,
                protein = foodItem.proteinPerServing,
                carbs = foodItem.carbsPerServing,
                fat = foodItem.fatPerServing,
                fiber = foodItem.fiberPerServing,
                sugar = foodItem.sugarPerServing,
                sodium = foodItem.sodiumPerServing,
                barcode = foodItem.barcode,
                category = category
            )
            
            val id = favoriteMealDao.insertFavorite(favorite)
            favorite.copy(id = id)
        }
    }
    
    /**
     * Remove a meal from favorites
     */
    suspend fun removeFromFavorites(favorite: FavoriteMeal) {
        favoriteMealDao.deleteFavorite(favorite)
    }
    
    /**
     * Quick-add a favorite meal as a calorie entry
     */
    suspend fun quickAddFavorite(favorite: FavoriteMeal, date: String? = null): CalorieEntry {
        val entryDate = date ?: getTodayDateString()
        
        val entry = CalorieEntry(
            foodName = favorite.foodName,
            calories = favorite.calories,
            protein = favorite.protein,
            carbs = favorite.carbs,
            fat = favorite.fat,
            fiber = favorite.fiber,
            sugar = favorite.sugar,
            sodium = favorite.sodium,
            date = entryDate,
            barcode = favorite.barcode
        )
        
        // Add the entry
        addCalorieEntry(entry)
        
        // Update favorite usage statistics
        favoriteMealDao.incrementUsage(favorite.id)
        
        return entry
    }
    
    /**
     * Get favorites by category
     */
    suspend fun getFavoritesByCategory(category: String): List<FavoriteMeal> {
        return favoriteMealDao.getFavoritesByCategory(category)
    }
    
    /**
     * Get all available categories
     */
    suspend fun getFavoriteCategories(): List<String> {
        return favoriteMealDao.getAllCategories()
    }
    
    /**
     * Clean up old unused favorites (maintenance method)
     */
    suspend fun cleanupOldFavorites() {
        val ninetyDaysAgo = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L)
        favoriteMealDao.cleanupUnusedFavorites(ninetyDaysAgo)
    }
    
    // =================== STREAK TRACKING ===================

    /**
     * Calculate current logging streak from existing CalorieEntry dates.
     * A streak is consecutive days (ending today or yesterday) with at least one entry.
     */
    suspend fun calculateCurrentStreak(): Int {
        val dates = calorieEntryDao.getAllDistinctDates() // descending order
        if (dates.isEmpty()) return 0

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()

        // Accept streak that ends today OR yesterday (user hasn't logged today yet)
        val today = sdf.format(calendar.time)
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val yesterday = sdf.format(calendar.time)

        if (dates[0] != today && dates[0] != yesterday) return 0

        // Walk backwards counting consecutive days
        var streak = 1
        val dateSet = dates.toHashSet()
        val check = java.util.Calendar.getInstance()
        sdf.parse(dates[0])?.let { check.time = it }

        for (i in 1..365) {
            check.add(java.util.Calendar.DAY_OF_YEAR, -1)
            if (dateSet.contains(sdf.format(check.time))) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    // =================== BARCODE SEARCH WITH HISTORY ===================
    
    /**
     * Search for food by barcode with automatic history recording
     * This is the main method that should be used for barcode scanning
     */
    suspend fun searchFoodByBarcodeWithHistory(barcode: String): FoodItem? {
        return try {
            // Use NetworkManager to search for the food
            val foodItem = networkManager?.searchFoodByBarcode(barcode)
            
            if (foodItem != null) {
                // Determine the source based on where it was found
                val source = when {
                    // This is a simplified source detection - NetworkManager could be enhanced to return source info
                    foodItem.name.contains("(saved)") -> "cache"
                    else -> "online_api"
                }
                
                // Record successful scan in history
                recordBarcodeHistory(foodItem, source)
                
                foodItem
            } else {
                // Record failed scan
                recordFailedBarcodeScan(barcode)
                null
            }
        } catch (e: Exception) {
            // Record failed scan on exception
            recordFailedBarcodeScan(barcode)
            null
        }
    }
    
    // =================== BARCODE HISTORY METHODS ===================
    
    /**
     * Get recent barcode scanning history
     */
    suspend fun getRecentBarcodeHistory(limit: Int = 20): List<BarcodeHistory> {
        return barcodeHistoryDao.getRecentHistory(limit)
    }
    
    /**
     * Get recent barcode history as LiveData for UI observation
     */
    fun getRecentBarcodeHistoryLive(limit: Int = 20) = barcodeHistoryDao.getRecentHistoryFlow(limit)
    
    /**
     * Search barcode history
     */
    suspend fun searchBarcodeHistory(query: String): List<BarcodeHistory> {
        return barcodeHistoryDao.searchHistory(query)
    }
    
    /**
     * Get most frequently scanned successful items
     */
    suspend fun getMostScannedBarcodes(limit: Int = 10): List<BarcodeHistory> {
        return barcodeHistoryDao.getMostScannedSuccessful(limit)
    }
    
    /**
     * Get failed barcode scans for debugging
     */
    suspend fun getFailedBarcodeScans(limit: Int = 10): List<BarcodeHistory> {
        return barcodeHistoryDao.getFailedScans(limit)
    }
    
    /**
     * Record a successful barcode scan in history
     */
    suspend fun recordBarcodeHistory(foodItem: FoodItem, source: String): BarcodeHistory {
        val existing = barcodeHistoryDao.getByBarcode(foodItem.barcode)
        
        return if (existing != null) {
            // Update existing history entry
            barcodeHistoryDao.incrementScanCount(foodItem.barcode)
            existing.copy(
                timesScanned = existing.timesScanned + 1,
                lastScanned = System.currentTimeMillis(),
                source = source // Update source to latest
            )
        } else {
            // Create new history entry
            val history = BarcodeHistory(
                barcode = foodItem.barcode,
                foodName = foodItem.name,
                brand = foodItem.brand,
                calories = foodItem.caloriesPerServing,
                servingSize = foodItem.servingSize,
                protein = foodItem.proteinPerServing,
                carbs = foodItem.carbsPerServing,
                fat = foodItem.fatPerServing,
                fiber = foodItem.fiberPerServing,
                sugar = foodItem.sugarPerServing,
                sodium = foodItem.sodiumPerServing,
                source = source,
                wasSuccessful = true
            )
            
            val id = barcodeHistoryDao.insertHistory(history)
            history.copy(id = id)
        }
    }
    
    /**
     * Record a failed barcode scan for analytics
     */
    suspend fun recordFailedBarcodeScan(barcode: String) {
        val existing = barcodeHistoryDao.getByBarcode(barcode)
        
        if (existing != null) {
            barcodeHistoryDao.incrementScanCount(barcode)
        } else {
            val history = BarcodeHistory(
                barcode = barcode,
                foodName = "Unknown Product",
                calories = 0,
                wasSuccessful = false,
                source = "failed_scan"
            )
            
            barcodeHistoryDao.insertHistory(history)
        }
    }
    
    /**
     * Quick-add from barcode history
     */
    suspend fun quickAddFromHistory(history: BarcodeHistory, date: String? = null): CalorieEntry {
        val entryDate = date ?: getTodayDateString()
        
        val entry = CalorieEntry(
            foodName = history.foodName,
            calories = history.calories,
            protein = history.protein,
            carbs = history.carbs,
            fat = history.fat,
            fiber = history.fiber,
            sugar = history.sugar,
            sodium = history.sodium,
            date = entryDate,
            barcode = history.barcode
        )
        
        // Add the entry
        addCalorieEntry(entry)
        
        // Update history usage
        barcodeHistoryDao.incrementScanCount(history.barcode)
        
        return entry
    }
    
    /**
     * Get barcode scan statistics
     */
    suspend fun getBarcodeStats(): BarcodeStats {
        val total = barcodeHistoryDao.getHistoryCount()
        val successful = barcodeHistoryDao.getSuccessfulScanCount()
        val failed = barcodeHistoryDao.getFailedScanCount()
        val sourceStats = barcodeHistoryDao.getScanSourceStats()
        
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        val dailyStats = barcodeHistoryDao.getDailyScanCounts(thirtyDaysAgo)
        
        return BarcodeStats(
            totalScans = total,
            successfulScans = successful,
            failedScans = failed,
            successRate = if (total > 0) (successful.toFloat() / total.toFloat()) * 100f else 0f,
            sourceBreakdown = sourceStats,
            dailyActivity = dailyStats
        )
    }
    
    /**
     * Clean up old barcode history
     */
    suspend fun cleanupBarcodeHistory() {
        barcodeHistoryDao.cleanupOldHistory()
    }
    
    /**
     * Remove specific barcode from history
     */
    suspend fun removeBarcodeFromHistory(history: BarcodeHistory) {
        barcodeHistoryDao.deleteHistory(history)
    }
    
    // =================== PROGRESS PHOTOS METHODS ===================
    
    /**
     * Add a new progress photo
     */
    suspend fun addProgressPhoto(photo: ProgressPhoto): Long {
        return progressPhotoDao.insertPhoto(photo)
    }
    
    /**
     * Get recent progress photos
     */
    suspend fun getRecentProgressPhotos(limit: Int = 20): List<ProgressPhoto> {
        return progressPhotoDao.getRecentPhotos(limit)
    }
    
    /**
     * Get recent progress photos as Flow for UI observation
     */
    fun getRecentProgressPhotosFlow(limit: Int = 20) = progressPhotoDao.getRecentPhotosFlow(limit)
    
    /**
     * Get all visible progress photos as LiveData
     */
    fun getAllProgressPhotosLive() = progressPhotoDao.getAllVisiblePhotos()
    
    /**
     * Get progress photos in date range
     */
    suspend fun getProgressPhotosInDateRange(startDate: String, endDate: String): List<ProgressPhoto> {
        return progressPhotoDao.getPhotosInDateRange(startDate, endDate)
    }
    
    /**
     * Get progress photos by type (before, after, milestone, etc.)
     */
    suspend fun getProgressPhotosByType(type: String): List<ProgressPhoto> {
        return progressPhotoDao.getPhotosByType(type)
    }
    
    /**
     * Search progress photos by notes
     */
    suspend fun searchProgressPhotosByNotes(query: String): List<ProgressPhoto> {
        return progressPhotoDao.searchPhotosByNotes(query)
    }
    
    /**
     * Get progress photo by ID
     */
    suspend fun getProgressPhotoById(id: Long): ProgressPhoto? {
        return progressPhotoDao.getPhotoById(id)
    }
    
    /**
     * Update progress photo
     */
    suspend fun updateProgressPhoto(photo: ProgressPhoto) {
        progressPhotoDao.updatePhoto(photo)
    }
    
    /**
     * Hide progress photo (soft delete)
     */
    suspend fun hideProgressPhoto(id: Long) {
        progressPhotoDao.hidePhoto(id)
    }
    
    /**
     * Show previously hidden progress photo
     */
    suspend fun showProgressPhoto(id: Long) {
        progressPhotoDao.showPhoto(id)
    }
    
    /**
     * Delete progress photo permanently
     */
    suspend fun deleteProgressPhoto(photo: ProgressPhoto) {
        progressPhotoDao.deletePhoto(photo)
    }
    
    /**
     * Get progress photo count for today
     */
    suspend fun getProgressPhotoCountForToday(): Int {
        val today = getTodayDateString()
        return progressPhotoDao.getPhotoCountForDate(today)
    }
    
    /**
     * Get total visible progress photo count
     */
    suspend fun getTotalProgressPhotoCount(): Int {
        return progressPhotoDao.getVisiblePhotoCount()
    }
    
    /**
     * Get progress photos that include weight data
     */
    suspend fun getProgressPhotosWithWeight(): List<ProgressPhoto> {
        return progressPhotoDao.getPhotosWithWeight()
    }
    
    /**
     * Get average mood rating from progress photos in last 30 days
     */
    suspend fun getRecentAverageMoodRating(): Double? {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        return progressPhotoDao.getAverageMoodSince(thirtyDaysAgo)
    }
    
    /**
     * Get photo count statistics by date
     */
    suspend fun getPhotoCountsByDate(): List<PhotoDateCount> {
        return progressPhotoDao.getPhotoCountsByDate()
    }
    
    /**
     * Get all photo types used by user
     */
    suspend fun getAllPhotoTypes(): List<String> {
        return progressPhotoDao.getAllPhotoTypes()
    }
    
    /**
     * Clean up old hidden progress photos
     */
    suspend fun cleanupOldProgressPhotos() {
        val ninetyDaysAgo = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L)
        progressPhotoDao.cleanupHiddenPhotos(ninetyDaysAgo)
    }
    
    // =================== MEAL PLANNING METHODS ===================
    
    /**
     * Add a new meal plan
     */
    suspend fun addMealPlan(mealPlan: MealPlan): Long {
        return mealPlanDao.insertMealPlan(mealPlan)
    }
    
    /**
     * Update an existing meal plan
     */
    suspend fun updateMealPlan(mealPlan: MealPlan) {
        mealPlanDao.updateMealPlan(mealPlan)
    }
    
    /**
     * Delete a meal plan
     */
    suspend fun deleteMealPlan(mealPlan: MealPlan) {
        mealPlanDao.deleteMealPlan(mealPlan)
    }
    
    /**
     * Get meal plans for a specific date
     */
    suspend fun getMealPlansForDate(date: String): List<MealPlan> {
        return mealPlanDao.getMealPlansForDate(date)
    }
    
    /**
     * Get meal plans for a specific date as Flow
     */
    fun getMealPlansForDateFlow(date: String) = mealPlanDao.getMealPlansForDateFlow(date)
    
    /**
     * Get meal plans for a date range
     */
    suspend fun getMealPlansForDateRange(startDate: String, endDate: String): List<MealPlan> {
        return mealPlanDao.getMealPlansForDateRange(startDate, endDate)
    }
    
    /**
     * Get meal plans for a date range as Flow
     */
    fun getMealPlansForDateRangeFlow(startDate: String, endDate: String) = mealPlanDao.getMealPlansForDateRangeFlow(startDate, endDate)
    
    /**
     * Get meal plans for a specific week
     */
    suspend fun getMealPlansForWeek(week: String): List<MealPlan> {
        return mealPlanDao.getMealPlansForWeek(week)
    }
    
    /**
     * Get meal plans for a specific week as Flow
     */
    fun getMealPlansForWeekFlow(week: String) = mealPlanDao.getMealPlansForWeekFlow(week)
    
    /**
     * Get meal plans for a specific date and meal type
     */
    suspend fun getMealPlansForDateAndType(date: String, mealType: String): List<MealPlan> {
        return mealPlanDao.getMealPlansForDateAndType(date, mealType)
    }
    
    /**
     * Search meal plans by name or description
     */
    suspend fun searchMealPlans(query: String): List<MealPlan> {
        return mealPlanDao.searchMealPlans(query)
    }
    
    /**
     * Get meal plan by ID
     */
    suspend fun getMealPlanById(id: Long): MealPlan? {
        return mealPlanDao.getMealPlanById(id)
    }
    
    /**
     * Mark meal as completed or not completed
     */
    suspend fun markMealAsCompleted(id: Long, completed: Boolean, completedAt: Long?) {
        mealPlanDao.markMealAsCompleted(id, completed, completedAt)
    }
    
    /**
     * Get completed meals in date range
     */
    suspend fun getCompletedMealsInRange(startDate: String, endDate: String): List<MealPlan> {
        return mealPlanDao.getCompletedMealsInRange(startDate, endDate)
    }
    
    /**
     * Update shopping list status for meal
     */
    suspend fun updateShoppingListStatus(id: Long, added: Boolean, dateAdded: Long?) {
        mealPlanDao.updateShoppingListStatus(id, added, dateAdded)
    }
    
    /**
     * Get meals added to shopping list
     */
    suspend fun getMealsAddedToShoppingList(): List<MealPlan> {
        return mealPlanDao.getMealsAddedToShoppingList()
    }
    
    /**
     * Get meals not in shopping list that have ingredients
     */
    suspend fun getMealsNotInShoppingList(): List<MealPlan> {
        return mealPlanDao.getMealsNotInShoppingList()
    }
    
    /**
     * Get meal plan count for date range
     */
    suspend fun getMealPlanCountForRange(startDate: String, endDate: String): Int {
        return mealPlanDao.getMealPlanCountForRange(startDate, endDate)
    }
    
    /**
     * Get completed meal count for date range
     */
    suspend fun getCompletedMealCountForRange(startDate: String, endDate: String): Int {
        return mealPlanDao.getCompletedMealCountForRange(startDate, endDate)
    }
    
    /**
     * Get average calories for planned meals in date range
     */
    suspend fun getAverageCaloriesForRange(startDate: String, endDate: String): Double? {
        return mealPlanDao.getAverageCaloriesForRange(startDate, endDate)
    }
    
    /**
     * Get all unique meal names for suggestions
     */
    suspend fun getAllUniqueMealNames(): List<String> {
        return mealPlanDao.getAllUniqueMealNames()
    }
    
    /**
     * Get most popular meals for quick adding
     */
    suspend fun getMostPopularMeals(limit: Int = 10): List<MealNameCount> {
        return mealPlanDao.getMostPopularMeals(limit)
    }
    
    /**
     * Get recent meal plans for suggestions
     */
    suspend fun getRecentMealPlans(limit: Int = 20): List<MealPlan> {
        return mealPlanDao.getRecentMealPlans(limit)
    }
    
    /**
     * Batch insert meal plans (for weekly templates)
     */
    suspend fun insertMealPlans(mealPlans: List<MealPlan>) {
        mealPlanDao.insertMealPlans(mealPlans)
    }
    
    /**
     * Delete meal plans for a specific date
     */
    suspend fun deleteMealPlansForDate(date: String) {
        mealPlanDao.deleteMealPlansForDate(date)
    }
    
    /**
     * Update sort order for meal plan
     */
    suspend fun updateMealSortOrder(id: Long, newOrder: Int) {
        mealPlanDao.updateSortOrder(id, newOrder)
    }
    
    /**
     * Clean up old uncompleted meal plans
     */
    suspend fun cleanupOldMealPlans() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        mealPlanDao.cleanupOldUncompletedPlans(thirtyDaysAgo)
    }
    
    /**
     * Generate weekly meal plan template
     */
    suspend fun generateWeeklyMealPlanTemplate(startDate: String): List<MealPlan> {
        val weekDates = generateWeekDatesFromStart(startDate)
        val mealPlans = mutableListOf<MealPlan>()
        
        // Create basic meal structure for each day
        weekDates.forEach { date ->
            listOf("breakfast", "lunch", "dinner").forEach { mealType ->
                mealPlans.add(
                    MealPlan(
                        date = date,
                        mealType = mealType,
                        mealName = "Plan your ${mealType}",
                        description = "Drag and drop or tap to add meals",
                        estimatedCalories = 0,
                        planWeek = getWeekIdentifier(date)
                    )
                )
            }
        }
        
        return mealPlans
    }
    
    private fun generateWeekDatesFromStart(startDate: String): List<String> {
        val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()
        
        try {
            calendar.time = dateFormatter.parse(startDate) ?: java.util.Date()
        } catch (e: Exception) {
            calendar.time = java.util.Date()
        }
        
        val dates = mutableListOf<String>()
        for (i in 0 until 7) {
            dates.add(dateFormatter.format(calendar.time))
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        
        return dates
    }
    
    private fun getWeekIdentifier(dateString: String): String {
        val weekFormatter = java.text.SimpleDateFormat("yyyy-'W'ww", java.util.Locale.getDefault())
        val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        
        return try {
            val date = dateFormatter.parse(dateString) ?: java.util.Date()
            weekFormatter.format(date)
        } catch (e: Exception) {
            weekFormatter.format(java.util.Date())
        }
    }
    
    // =================== SHOPPING LIST METHODS ===================
    
    /**
     * Get active shopping list (unchecked items)
     */
    fun getActiveShoppingListFlow() = shoppingListDao.getActiveShoppingListFlow()
    
    /**
     * Get active shopping list as LiveData
     */
    fun getActiveShoppingList() = shoppingListDao.getActiveShoppingList()
    
    /**
     * Get active shopping list synchronously
     */
    suspend fun getActiveShoppingListSync(): List<ShoppingListItem> {
        return shoppingListDao.getActiveShoppingListSync()
    }
    
    /**
     * Get all shopping items (including checked)
     */
    fun getAllShoppingItems() = shoppingListDao.getAllShoppingItems()
    
    /**
     * Add shopping list item
     */
    suspend fun insertShoppingItem(item: ShoppingListItem): Long {
        return shoppingListDao.insertShoppingItem(item)
    }
    
    /**
     * Add multiple shopping list items
     */
    suspend fun insertShoppingItems(items: List<ShoppingListItem>) {
        shoppingListDao.insertShoppingItems(items)
    }
    
    /**
     * Update shopping list item
     */
    suspend fun updateShoppingItem(item: ShoppingListItem) {
        shoppingListDao.updateShoppingItem(item)
    }
    
    /**
     * Delete shopping list item
     */
    suspend fun deleteShoppingItem(item: ShoppingListItem) {
        shoppingListDao.deleteShoppingItem(item)
    }
    
    /**
     * Update item checked status
     */
    suspend fun updateShoppingItemCheckedStatus(id: Long, checked: Boolean, checkedAt: Long?, updatedAt: Long) {
        shoppingListDao.updateItemCheckedStatus(id, checked, checkedAt, updatedAt)
    }
    
    /**
     * Uncheck all shopping items
     */
    suspend fun uncheckAllShoppingItems(updatedAt: Long) {
        shoppingListDao.uncheckAllItems(updatedAt)
    }
    
    /**
     * Get shopping items by category
     */
    suspend fun getShoppingItemsByCategory(category: String): List<ShoppingListItem> {
        return shoppingListDao.getItemsByCategory(category)
    }
    
    /**
     * Get shopping items by trip
     */
    suspend fun getShoppingItemsByTrip(tripName: String): List<ShoppingListItem> {
        return shoppingListDao.getItemsByTrip(tripName)
    }
    
    /**
     * Search shopping items
     */
    suspend fun searchShoppingItems(query: String): List<ShoppingListItem> {
        return shoppingListDao.searchShoppingItems(query)
    }
    
    /**
     * Get all shopping categories
     */
    suspend fun getAllShoppingCategories(): List<String> {
        return shoppingListDao.getAllCategories()
    }
    
    /**
     * Get all shopping trips
     */
    suspend fun getAllShoppingTrips(): List<String> {
        return shoppingListDao.getAllShoppingTrips()
    }
    
    /**
     * Update shopping item category
     */
    suspend fun updateShoppingItemCategory(id: Long, newCategory: String, updatedAt: Long) {
        shoppingListDao.updateItemCategory(id, newCategory, updatedAt)
    }
    
    /**
     * Update staple status
     */
    suspend fun updateStapleStatus(id: Long, isStaple: Boolean, updatedAt: Long) {
        shoppingListDao.updateStapleStatus(id, isStaple, updatedAt)
    }
    
    /**
     * Update recurring status
     */
    suspend fun updateRecurringStatus(id: Long, isRecurring: Boolean, updatedAt: Long) {
        shoppingListDao.updateRecurringStatus(id, isRecurring, updatedAt)
    }
    
    /**
     * Get active item count
     */
    suspend fun getActiveShoppingItemCount(): Int {
        return shoppingListDao.getActiveItemCount()
    }
    
    /**
     * Get estimated total cost
     */
    suspend fun getEstimatedTotalCost(): Double? {
        return shoppingListDao.getEstimatedTotalCost()
    }
    
    /**
     * Get most frequent shopping items
     */
    suspend fun getMostFrequentShoppingItems(limit: Int = 20): List<ItemFrequency> {
        return shoppingListDao.getMostFrequentItems(limit)
    }
    
    /**
     * Get staple items
     */
    suspend fun getStapleItems(): List<ShoppingListItem> {
        return shoppingListDao.getStapleItems()
    }
    
    /**
     * Get recurring items
     */
    suspend fun getRecurringItems(): List<ShoppingListItem> {
        return shoppingListDao.getRecurringItems()
    }
    
    /**
     * Clean up old checked items
     */
    suspend fun cleanupOldCheckedItems(cutoffTime: Long) {
        shoppingListDao.cleanupOldCheckedItems(cutoffTime)
    }
    
    /**
     * Get shopping items from meal plan
     */
    suspend fun getShoppingItemsFromMealPlan(mealPlanId: Long): List<ShoppingListItem> {
        return shoppingListDao.getItemsFromMealPlan(mealPlanId)
    }
    
    /**
     * Delete shopping items from meal plan
     */
    suspend fun deleteShoppingItemsFromMealPlan(mealPlanId: Long) {
        shoppingListDao.deleteItemsFromMealPlan(mealPlanId)
    }
    
    // =================== TREND ANALYSIS METHODS ===================
    
    /**
     * 📊 GET ENTRIES IN DATE RANGE
     * 
     * Retrieve all food entries between two timestamps for trend analysis.
     */
    suspend fun getEntriesInDateRange(startDate: Long, endDate: Long): List<CalorieEntry> {
        val startDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startDate))
        val endDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(endDate))
        return calorieEntryDao.getEntriesInDateRange(startDateString, endDateString)
    }
    
    /**
     * ⚖️ GET WEIGHT ENTRIES IN DATE RANGE
     * 
     * Retrieve weight measurements between two timestamps for weight trends.
     */
    suspend fun getWeightEntriesInRange(startDate: Long, endDate: Long): List<WeightEntry> {
        val startDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startDate))
        val endDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(endDate))
        return weightEntryDao.getWeightEntriesInRange(startDateString, endDateString)
    }
    
    /**
     * 🎯 GET CALORIE GOAL
     * 
     * Get the user's current daily calorie goal.
     */
    suspend fun getCalorieGoal(): Int? {
        return getNutritionGoalsSync()?.calorieGoal
    }
    
    /**
     * 📊 GET NUTRITION GOALS (SYNC)
     * 
     * Get nutrition goals synchronously for calculations.
     */
    suspend fun getNutritionGoalsAsync(): NutritionGoals? {
        return getNutritionGoalsSync()
    }
}

/**
 * Data class representing comprehensive daily summary including workout data
 */
data class DailySummary(
    val baseCalorieGoal: Int,
    val workoutCaloriesBurned: Int,
    val adjustedCalorieGoal: Int,
    val consumedCalories: Int,
    val remainingCalories: Int,
    val exerciseMinutes: Int,
    val primaryExerciseType: String?
)

/**
 * Data class representing barcode scanning statistics
 */
data class BarcodeStats(
    val totalScans: Int,
    val successfulScans: Int,
    val failedScans: Int,
    val successRate: Float, // Percentage
    val sourceBreakdown: List<ScanSourceStats>,
    val dailyActivity: List<DailyScanStats>
)