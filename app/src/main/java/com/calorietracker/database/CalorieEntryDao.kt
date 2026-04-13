package com.calorietracker.database

// 🧰 ROOM DATABASE AND ANDROID TOOLS
import androidx.room.*           // Database annotation tools (@Dao, @Query, @Insert, etc.)
import androidx.lifecycle.LiveData // Reactive data that automatically updates UI

/**
 * 🗄️ CALORIE ENTRY DAO - FOOD DIARY DATABASE OPERATIONS
 * 
 * Hey future programmer! This is a Data Access Object (DAO) for food entries.
 * Think of this like a specialized librarian who knows exactly how to find, add, update,
 * and organize all the food entries in our digital food diary.
 * 
 * 📚 What is a DAO?
 * DAO stands for "Data Access Object" - it's a design pattern that creates a safe,
 * organized way to work with database tables. Instead of writing raw SQL everywhere
 * in our app (which is error-prone and messy), we define all our database operations
 * here in one place.
 * 
 * 🎯 Why Use DAOs?
 * - Type Safety: Room checks our SQL queries at compile time (catches errors early)
 * - Clean Code: All database operations are centralized and well-organized
 * - Automatic Threading: Room handles background threads for us
 * - LiveData Integration: Automatically updates UI when data changes
 * - Easy Testing: We can mock the DAO for unit tests
 * 
 * 🔄 LiveData vs Suspend Functions:
 * - LiveData functions: Return reactive data that auto-updates UI (like a subscription)
 * - Suspend functions: Return data once for immediate use in background tasks
 * 
 * 📊 Types of Operations:
 * - Queries: Read data from the database (@Query)
 * - Insert: Add new entries to the database (@Insert)
 * - Update: Modify existing entries (@Update)
 * - Delete: Remove entries from the database (@Delete)
 * 
 * 🗓️ Date Format:
 * All dates use "yyyy-MM-dd" format (like "2024-08-31") for consistent sorting and filtering
 */
@Dao  // 🏷️ Tells Room this interface contains database operations
interface CalorieEntryDao {
    
    // 📋 RETRIEVE FOOD ENTRIES (LiveData - Auto-updating for UI)
    
    /**
     * 📅 GET ALL FOOD ENTRIES FOR A SPECIFIC DATE (LiveData)
     * 
     * This gets all the food entries for one day and returns them as LiveData.
     * LiveData is magical - when new food is added or existing food is updated,
     * any UI components observing this data will automatically refresh!
     * 
     * The results are ordered by timestamp (newest first), so the most recent
     * food entries appear at the top of the list.
     * 
     * @param date The date to get entries for (format: "yyyy-MM-dd" like "2024-08-31")
     * @return LiveData containing a list of food entries for that date
     */
    @Query("SELECT * FROM calorie_entries WHERE date = :date ORDER BY timestamp DESC")
    fun getEntriesForDate(date: String): LiveData<List<CalorieEntry>>
    
    /**
     * 📊 GET FOOD ENTRIES FOR DATE RANGE (LiveData)
     * 
     * This gets all food entries between two dates (inclusive) for trend analysis.
     * Perfect for showing weekly or monthly views of eating patterns.
     * 
     * Results are ordered by date (newest first), then by time within each date.
     * 
     * @param startDate First date to include (format: "yyyy-MM-dd")
     * @param endDate Last date to include (format: "yyyy-MM-dd")
     * @return LiveData containing food entries in the date range
     */
    @Query("SELECT * FROM calorie_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, timestamp DESC")
    fun getEntriesForDateRange(startDate: String, endDate: String): LiveData<List<CalorieEntry>>
    
    // 📋 RETRIEVE FOOD ENTRIES (Suspend - One-time fetch for calculations)
    
    /**
     * 📅 GET ALL FOOD ENTRIES FOR A SPECIFIC DATE (Suspend)
     * 
     * Same as getEntriesForDate() but returns data immediately instead of LiveData.
     * Use this in background tasks when you need the data right now for calculations,
     * not for UI that needs to stay updated.
     * 
     * @param date The date to get entries for (format: "yyyy-MM-dd")
     * @return List of food entries for that date (one-time fetch)
     */
    @Query("SELECT * FROM calorie_entries WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getEntriesForDateSync(date: String): List<CalorieEntry>
    
    /**
     * 📊 GET FOOD ENTRIES FOR DATE RANGE (Suspend)
     * 
     * Same as getEntriesForDateRange() but for immediate use in calculations.
     * 
     * @param startDate First date to include
     * @param endDate Last date to include  
     * @return List of food entries in the date range
     */
    @Query("SELECT * FROM calorie_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, timestamp DESC")
    suspend fun getEntriesInDateRange(startDate: String, endDate: String): List<CalorieEntry>
    
    // 🧮 NUTRITION CALCULATIONS (Adding up nutrients across multiple food entries)
    
    /**
     * 🔥 GET TOTAL CALORIES FOR A DAY
     * 
     * Adds up all the calories from every food entry on a specific date.
     * For example: breakfast (300) + lunch (500) + snack (150) + dinner (650) = 1600 total
     * 
     * Returns null if no entries exist for that date (so we can detect empty days).
     * 
     * @param date The date to calculate calories for
     * @return Total calories for the day, or null if no entries
     */
    @Query("SELECT SUM(calories) FROM calorie_entries WHERE date = :date")
    suspend fun getTotalCaloriesForDate(date: String): Int?
    
    /**
     * 💪 GET TOTAL PROTEIN FOR A DAY
     * 
     * Adds up all protein from every food entry on a specific date.
     * Only includes entries that have protein data (ignores null values).
     * 
     * @param date The date to calculate protein for
     * @return Total protein in grams, or null if no entries with protein data
     */
    @Query("SELECT SUM(protein) FROM calorie_entries WHERE date = :date")
    suspend fun getTotalProteinForDate(date: String): Double?
    
    /**
     * 🍞 GET TOTAL CARBOHYDRATES FOR A DAY
     * 
     * Adds up all carbs from every food entry on a specific date.
     * 
     * @param date The date to calculate carbs for
     * @return Total carbohydrates in grams, or null if no entries with carb data
     */
    @Query("SELECT SUM(carbs) FROM calorie_entries WHERE date = :date")
    suspend fun getTotalCarbsForDate(date: String): Double?
    
    /**
     * 🥑 GET TOTAL FAT FOR A DAY
     * 
     * Adds up all fat from every food entry on a specific date.
     * 
     * @param date The date to calculate fat for
     * @return Total fat in grams, or null if no entries with fat data
     */
    @Query("SELECT SUM(fat) FROM calorie_entries WHERE date = :date")
    suspend fun getTotalFatForDate(date: String): Double?
    
    /**
     * 📊 GET DAILY CALORIE SUMMARY FOR DATE RANGE
     * 
     * Creates a summary showing total calories for each day in a date range.
     * This is perfect for creating charts and graphs showing eating patterns over time.
     * 
     * The SQL GROUP BY groups all entries for each date together, then SUM() adds up
     * the calories for each group. Results are sorted by date for chronological order.
     * 
     * Example result:
     * - 2024-08-29: 1850 calories
     * - 2024-08-30: 2100 calories  
     * - 2024-08-31: 1950 calories
     * 
     * @param startDate First date to include
     * @param endDate Last date to include
     * @return List of daily calorie summaries
     */
    @Query("SELECT date, SUM(calories) as totalCalories FROM calorie_entries WHERE date BETWEEN :startDate AND :endDate GROUP BY date ORDER BY date")
    suspend fun getDailyCalorieSummary(startDate: String, endDate: String): List<DailyCalorieSummary>
    
    // ✍️ MODIFY DATABASE (Create, Update, Delete operations)
    
    /**
     * ➕ ADD NEW FOOD ENTRY
     * 
     * Adds a new food entry to the database. Room automatically generates a unique ID
     * for the entry and returns it so we know which ID was assigned.
     * 
     * The suspend keyword means this operation might take a moment (writing to disk)
     * so it runs in the background to avoid freezing the UI.
     * 
     * @param entry The food entry to add to the database
     * @return The unique ID assigned to this entry
     */
    @Insert
    suspend fun insertEntry(entry: CalorieEntry): Long
    
    /**
     * ✏️ UPDATE EXISTING FOOD ENTRY
     * 
     * Modifies an existing food entry in the database. Room uses the entry's ID
     * to find which row to update, then replaces all the data.
     * 
     * This is useful when users want to fix mistakes (like changing that
     * 14,000 calorie Coke to 140 calories).
     * 
     * @param entry The food entry with updated information
     */
    @Update
    suspend fun updateEntry(entry: CalorieEntry)
    
    /**
     * 🗑️ DELETE SPECIFIC FOOD ENTRY
     * 
     * Permanently removes a specific food entry from the database.
     * Room uses the entry's ID to find and delete the correct row.
     * 
     * Once deleted, the entry is gone forever (no undo).
     * 
     * @param entry The food entry to remove
     */
    @Delete
    suspend fun deleteEntry(entry: CalorieEntry)
    
    /**
     * 🗑️ DELETE ALL ENTRIES FOR A SPECIFIC DATE
     * 
     * Removes all food entries for a specific date. This is like clearing
     * an entire day from your food diary.
     * 
     * Use this carefully - it deletes everything for that day!
     * 
     * @param date The date to clear all entries for
     */
    @Query("DELETE FROM calorie_entries WHERE date = :date")
    suspend fun deleteEntriesForDate(date: String)

    @Query("SELECT DISTINCT date FROM calorie_entries ORDER BY date DESC")
    suspend fun getAllDistinctDates(): List<String>
}

/**
 * 📊 DAILY CALORIE SUMMARY - Data class for daily totals
 * 
 * This data class holds the results from getDailyCalorieSummary().
 * It represents the total calories consumed on a specific date.
 * 
 * Think of this like a line item in a summary report:
 * "On August 31st, 2024, you consumed 1850 total calories"
 * 
 * @property date The date this summary is for (like "2024-08-31")
 * @property totalCalories The total calories consumed on that date
 */
data class DailyCalorieSummary(
    val date: String,        // 📅 The date (format: "yyyy-MM-dd")
    val totalCalories: Int   // 🔥 Total calories for that day
)

/**
 * 🧮 HELPFUL EXTENSION FUNCTIONS FOR CALORIE SUMMARIES
 * 
 * These functions add useful capabilities to work with daily calorie summaries,
 * making it easier to analyze eating patterns over time.
 */

/**
 * 📈 CALCULATE AVERAGE CALORIES FROM SUMMARIES
 * 
 * Calculates the average daily calories across multiple days.
 * Useful for seeing long-term eating patterns.
 */
fun List<DailyCalorieSummary>.getAverageCalories(): Double {
    return if (isEmpty()) 0.0 else sumOf { it.totalCalories }.toDouble() / size
}

/**
 * 🏆 FIND HIGHEST CALORIE DAY
 * 
 * Finds the day with the most calories consumed.
 */
fun List<DailyCalorieSummary>.getHighestCalorieDay(): DailyCalorieSummary? {
    return maxByOrNull { it.totalCalories }
}

/**
 * 🥬 FIND LOWEST CALORIE DAY
 * 
 * Finds the day with the fewest calories consumed.
 */
fun List<DailyCalorieSummary>.getLowestCalorieDay(): DailyCalorieSummary? {
    return minByOrNull { it.totalCalories }
}

/**
 * 🎯 COUNT DAYS WITHIN CALORIE GOAL
 * 
 * Counts how many days the user stayed within their calorie goal range.
 */
fun List<DailyCalorieSummary>.countDaysWithinGoal(calorieGoal: Int, tolerance: Int = 100): Int {
    return count { summary ->
        kotlin.math.abs(summary.totalCalories - calorieGoal) <= tolerance
    }
}