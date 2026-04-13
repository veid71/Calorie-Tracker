package com.calorietracker.database

// 🧰 ROOM DATABASE AND ANDROID TOOLS
import androidx.lifecycle.LiveData // Reactive data that automatically updates UI
import androidx.room.*           // Database annotation tools (@Dao, @Query, @Insert, etc.)

/**
 * 💪 WORKOUT CALORIES DAO - FITNESS DATA DATABASE OPERATIONS
 * 
 * Hey future programmer! This is the Data Access Object (DAO) for workout/fitness data.
 * Think of this like a specialized gym manager who tracks all your workouts and helps
 * calculate how many extra calories you can eat based on your exercise.
 * 
 * 🏃‍♂️ What Does This DAO Handle?
 * This DAO manages all database operations for workout calories data that comes from:
 * - Health Connect (Android's central health data system)
 * - Smartwatches (like OnePlus Watch 3, Apple Watch, Fitbit, etc.)
 * - Fitness trackers and other connected devices
 * - Manual exercise entries
 * 
 * 💡 Smart Calorie Adjustment System:
 * The core purpose of this data is to adjust daily calorie goals based on exercise.
 * For example:
 * - Base calorie goal: 2000 calories
 * - You burn 400 calories working out
 * - Adjusted goal: 2280 calories (2000 + 70% of workout calories)
 * - This prevents your body from going into "starvation mode" when exercising
 * 
 * 📊 Data Storage Strategy:
 * - One row per date (daily aggregation of all workouts)
 * - If you work out multiple times per day, it combines into daily totals
 * - Data is automatically synced from Health Connect when available
 * - Fallback to manual entry or test data when Health Connect unavailable
 * 
 * 🔄 LiveData vs Suspend Pattern:
 * - LiveData functions: For UI that needs automatic updates when data changes
 * - Suspend functions: For background calculations and one-time data fetching
 * 
 * 🗓️ Date Management:
 * All dates use "yyyy-MM-dd" format for consistent sorting and filtering
 */
@Dao  // 🏷️ Tells Room this interface contains database operations
interface WorkoutCaloriesDao {
    
    // ✍️ STORE WORKOUT DATA (Create and Update operations)
    
    /**
     * 💾 INSERT OR UPDATE WORKOUT DATA FOR A DATE
     * 
     * This is the main method for storing daily workout data in the database.
     * The REPLACE strategy means if data for this date already exists, it gets updated
     * with the new information. This is perfect for daily data that gets refreshed.
     * 
     * Example usage:
     * - Morning sync: Store initial workout data from overnight
     * - Evening sync: Update with complete daily totals
     * - Manual entry: Override automatic data with user corrections
     * 
     * @param workoutCalories Complete workout data for one day
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutCalories(workoutCalories: WorkoutCalories)
    
    // 📋 RETRIEVE SINGLE DAY DATA (For specific date lookups)
    
    /**
     * 🗓️ GET WORKOUT DATA FOR SPECIFIC DATE (Suspend)
     * 
     * Retrieves workout calories data for one specific date. Returns null if no
     * workout data exists for that date (rest day or data not synced yet).
     * 
     * Used for:
     * - Calculating today's adjusted calorie goal
     * - Checking if workout data exists before displaying UI
     * - Background calculations that need immediate results
     * 
     * @param date The date to get workout data for (format: "yyyy-MM-dd")
     * @return Workout data for that date, or null if no data exists
     */
    @Query("SELECT * FROM workout_calories WHERE date = :date")
    suspend fun getWorkoutCaloriesForDate(date: String): WorkoutCalories?
    
    /**
     * 🗓️ GET WORKOUT DATA FOR SPECIFIC DATE (LiveData)
     * 
     * Same as above but returns LiveData that automatically updates the UI when
     * workout data changes. Perfect for the main screen that shows adjusted calorie goals.
     * 
     * The UI will automatically refresh when:
     * - New workout data syncs from Health Connect
     * - User manually enters/edits workout data
     * - Workout data gets updated with more accurate information
     * 
     * @param date The date to observe workout data for
     * @return LiveData containing workout data (updates automatically)
     */
    @Query("SELECT * FROM workout_calories WHERE date = :date")
    fun getWorkoutCaloriesForDateLive(date: String): LiveData<WorkoutCalories?>
    
    // 📊 RETRIEVE DATE RANGE DATA (For analytics and trends)
    
    /**
     * 📈 GET WORKOUT DATA FOR DATE RANGE (Suspend)
     * 
     * Retrieves workout data for multiple days between two dates (inclusive).
     * Results are ordered chronologically (oldest first) for trend analysis.
     * 
     * Used for:
     * - Weekly/monthly workout summaries
     * - Calculating average daily exercise
     * - Identifying workout patterns and rest days
     * - Generating fitness reports
     * 
     * @param startDate First date to include (format: "yyyy-MM-dd")
     * @param endDate Last date to include (format: "yyyy-MM-dd")
     * @return List of workout data in chronological order
     */
    @Query("SELECT * FROM workout_calories WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getWorkoutCaloriesForDateRange(startDate: String, endDate: String): List<WorkoutCalories>
    
    /**
     * 📈 GET WORKOUT DATA FOR DATE RANGE (LiveData)
     * 
     * Same as above but returns LiveData for analytics screens that need live updates.
     * Perfect for charts and graphs that should update when new workout data arrives.
     * 
     * @param startDate First date to include
     * @param endDate Last date to include
     * @return LiveData containing list of workout data (updates automatically)
     */
    @Query("SELECT * FROM workout_calories WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getWorkoutCaloriesForDateRangeLive(startDate: String, endDate: String): LiveData<List<WorkoutCalories>>
    
    // 🧮 CALCULATIONS AND ANALYTICS (Aggregated workout statistics)
    
    /**
     * 🔥 GET TOTAL ACTIVE CALORIES FOR DATE RANGE
     * 
     * Calculates the sum of active calories burned across multiple days.
     * This helps answer questions like "How many calories did I burn this week?"
     * 
     * Active calories = calories from intentional exercise (excludes baseline metabolism)
     * 
     * Example calculation:
     * - Monday: 300 active calories (running)
     * - Tuesday: 0 active calories (rest day)
     * - Wednesday: 450 active calories (gym workout)
     * - Total: 750 active calories for 3 days
     * 
     * @param startDate First date to include in calculation
     * @param endDate Last date to include in calculation
     * @return Total active calories burned, or null if no workout data in range
     */
    @Query("SELECT SUM(activeCaloriesBurned) FROM workout_calories WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalActiveCaloriesForRange(startDate: String, endDate: String): Int?
    
    // 📊 ALL DATA QUERIES (For comprehensive analysis)
    
    /**
     * 📋 GET ALL WORKOUT DATA (LiveData)
     * 
     * Returns all workout data in the database, ordered by date (newest first).
     * Used for comprehensive analytics screens and data export features.
     * 
     * The LiveData automatically updates when new workout data is added,
     * making it perfect for dashboard screens that show overall fitness trends.
     * 
     * @return LiveData containing all workout data (updates automatically)
     */
    @Query("SELECT * FROM workout_calories ORDER BY date DESC")
    fun getAllWorkoutCalories(): LiveData<List<WorkoutCalories>>
    
    /**
     * 📋 GET ALL WORKOUT DATA (Suspend)
     * 
     * Same as above but for immediate use in background calculations.
     * Used for data export, comprehensive analysis, and migration tasks.
     * 
     * @return List of all workout data (one-time fetch)
     */
    @Query("SELECT * FROM workout_calories ORDER BY date DESC")
    suspend fun getAllWorkoutCaloriesSync(): List<WorkoutCalories>
    
    // 🧹 DATA MAINTENANCE (Cleanup and housekeeping operations)
    
    /**
     * 🗑️ DELETE OLD WORKOUT DATA
     * 
     * Removes workout data older than a specified date to prevent database bloat.
     * This is typically used to keep only recent workout data (like last 2 years)
     * while still maintaining reasonable app performance.
     * 
     * Example usage:
     * - Keep only last 730 days (2 years) of workout data
     * - Run monthly cleanup to remove very old entries
     * - Free up storage space on device
     * 
     * @param cutoffDate Delete all workout data older than this date
     */
    @Query("DELETE FROM workout_calories WHERE date < :cutoffDate")
    suspend fun deleteOldWorkoutCalories(cutoffDate: String)
    
    // 📊 METADATA AND STATISTICS (Information about the workout data itself)
    
    /**
     * ⏰ GET MOST RECENT SYNC TIME
     * 
     * Returns the timestamp of the most recent workout data sync.
     * This helps determine when we last successfully retrieved data from Health Connect
     * and whether we need to sync new data.
     * 
     * Used for:
     * - Showing "Last updated: 2 hours ago" in UI
     * - Determining if data is stale and needs refreshing
     * - Debugging sync issues with Health Connect
     * 
     * @return Timestamp of most recent sync, or null if no data exists
     */
    @Query("SELECT MAX(lastSyncTime) FROM workout_calories")
    suspend fun getLastSyncTime(): Long?
    
    /**
     * 📊 GET TOTAL COUNT OF WORKOUT ENTRIES
     * 
     * Returns the number of days with workout data stored in the database.
     * Useful for statistics, debugging, and showing user how much data we have.
     * 
     * Example display: "Tracking workouts for 127 days"
     * 
     * @return Number of workout entries in database
     */
    @Query("SELECT COUNT(*) FROM workout_calories")
    suspend fun getWorkoutCaloriesCount(): Int
}

/**
 * 🧮 HELPFUL EXTENSION FUNCTIONS FOR WORKOUT ANALYSIS
 * 
 * These functions add analytical capabilities to workout data collections,
 * making it easier to generate insights and summaries.
 */

/**
 * 📊 CALCULATE AVERAGE DAILY ACTIVE CALORIES
 * 
 * Calculates the average active calories burned per day across multiple workout entries.
 */
fun List<WorkoutCalories>.getAverageDailyActiveCalories(): Double {
    return if (isEmpty()) 0.0 else sumOf { it.activeCaloriesBurned }.toDouble() / size
}

/**
 * 💪 FIND MOST ACTIVE DAY
 * 
 * Finds the workout entry with the highest active calories burned.
 */
fun List<WorkoutCalories>.getMostActiveDay(): WorkoutCalories? {
    return maxByOrNull { it.activeCaloriesBurned }
}

/**
 * 🏃‍♂️ COUNT WORKOUT DAYS
 * 
 * Counts how many days had significant workout activity (above a threshold).
 */
fun List<WorkoutCalories>.countSignificantWorkoutDays(minActiveCalories: Int = 150): Int {
    return count { it.activeCaloriesBurned >= minActiveCalories }
}

/**
 * ⏱️ CALCULATE TOTAL EXERCISE MINUTES
 * 
 * Sums up total exercise minutes across all workout entries.
 */
fun List<WorkoutCalories>.getTotalExerciseMinutes(): Int {
    return sumOf { it.exerciseMinutes }
}

/**
 * 🎯 GET WORKOUT CONSISTENCY PERCENTAGE
 * 
 * Calculates what percentage of days had workouts (consistency metric).
 */
fun List<WorkoutCalories>.getWorkoutConsistency(minActiveCalories: Int = 100): Double {
    if (isEmpty()) return 0.0
    val workoutDays = count { it.activeCaloriesBurned >= minActiveCalories }
    return (workoutDays.toDouble() / size) * 100
}