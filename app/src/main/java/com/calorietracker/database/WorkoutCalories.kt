package com.calorietracker.database

// 🧰 ROOM DATABASE TOOLS
import androidx.room.Entity      // Tells Android this class represents a database table
import androidx.room.PrimaryKey  // Marks which field is the unique ID

/**
 * 💪 WORKOUT CALORIES - FITNESS TRACKER INTEGRATION
 * 
 * Hey future programmer! This class stores daily workout/fitness data from your smartwatch or fitness tracker.
 * Think of this like a digital fitness diary that remembers how active you were each day.
 * 
 * 🏃‍♂️ What is this used for?
 * When you wear a smartwatch (like OnePlus Watch 3) or use a fitness tracker, it monitors your activity all day.
 * This data gets synced to Android Health Connect, and our app reads it to help calculate how many
 * extra calories you can eat based on your workouts.
 * 
 * 💡 Smart Calorie Adjustment:
 * If you normally eat 2000 calories per day, but you burned 400 calories working out,
 * our app might suggest you can eat 2280 calories today (70% of workout calories as bonus).
 * This helps maintain your weight loss/gain goals while accounting for exercise.
 * 
 * 📊 Two Types of Calories:
 * - Active Calories = Extra calories burned from intentional exercise (running, gym, sports)
 * - Total Calories = All calories burned including basic metabolism (breathing, thinking, etc.)
 * We mainly use Active Calories for adjustments since Total includes your baseline metabolism.
 * 
 * 🗓️ Why Store by Date?
 * Each day gets one row in the database. If you work out multiple times per day,
 * the fitness tracker combines it all into daily totals. This makes it easy to see
 * patterns over time and calculate weekly/monthly averages.
 * 
 * 🔄 Data Sync Process:
 * 1. You exercise wearing your smartwatch/fitness tracker
 * 2. Watch syncs data to Android Health Connect (system-wide fitness database)
 * 3. Our app reads from Health Connect and stores it in this table
 * 4. We use this data to adjust your daily calorie goals
 * 
 * @property date The date this workout data is for (like "2024-08-31")
 * @property activeCaloriesBurned Calories burned from intentional exercise (like 350 for a 45-minute run)
 * @property totalCaloriesBurned All calories burned including basic metabolism (like 2100 total for the day)
 * @property exerciseMinutes Total minutes spent exercising (like 45 minutes)
 * @property exerciseType Main type of exercise if known (like "Running", "Cycling", "Weight Training")
 * @property lastSyncTime When we last updated this data from Health Connect
 * @property source Where this data came from (like "Health Connect", "OnePlus Watch 3", or "Test Data")
 */
// 🏷️ DATABASE TABLE SETUP
// @Entity tells Android "this class should become a table in our database"
// tableName = "workout_calories" means the table will be called "workout_calories"
@Entity(tableName = "workout_calories")
data class WorkoutCalories(
    // 🔑 PRIMARY KEY - The unique ID for this workout day
    // We use date as the primary key because we only store one workout summary per day
    // This means if we sync data for the same date twice, it replaces the old data (which is what we want)
    @PrimaryKey
    val date: String, // 📅 Date in "yyyy-MM-dd" format (like "2024-08-31")
    
    // 🔥 CALORIE DATA - The main information we track
    val activeCaloriesBurned: Int, // 🏃‍♂️ Extra calories burned from intentional exercise (like 350 for a good workout)
    val totalCaloriesBurned: Int,  // 🔄 Total calories including basic metabolism (like 2100 for the whole day)
    
    // 📊 WORKOUT DETAILS - Additional information about the exercise
    val exerciseMinutes: Int = 0,  // ⏱️ Total minutes of exercise (like 45 for a run + 15 for stretching)
    val exerciseType: String? = null, // 🏃‍♂️ Primary exercise type if available (like "Running", "Cycling", "Mixed")
    
    // 🔄 SYNC METADATA - Information about when and where this data came from
    val lastSyncTime: Long = System.currentTimeMillis(), // ⏰ When we last updated this data (computer timestamp)
    val source: String = "Health Connect" // 🌐 Where we got this data (like "Health Connect", "OnePlus Watch 3", "Manual Entry")
)

/**
 * 🧮 HELPFUL EXTENSION FUNCTIONS FOR WORKOUT CALCULATIONS
 * 
 * These are special functions that add new abilities to our WorkoutCalories class!
 * Think of them like giving your workout data some superpowers.
 */

/**
 * 🎯 CALCULATE CALORIE BONUS FOR FOOD GOALS
 * 
 * This calculates how many extra calories you can eat based on your workout.
 * We use 70% of active calories as a conservative estimate (avoids overeating).
 * 
 * Example: You burned 400 active calories → You can eat an extra 280 calories
 * 
 * @return Extra calories you can add to your daily food goal
 */
fun WorkoutCalories.getCalorieBonus(): Int = (activeCaloriesBurned * 0.7).toInt()

/**
 * 🏃‍♂️ CHECK IF THIS WAS A SIGNIFICANT WORKOUT
 * 
 * Determines if this was a "real" workout worth adjusting calories for.
 * We consider 150+ active calories and 15+ minutes as a meaningful workout.
 * 
 * @return True if this was a significant workout, false for light activity
 */
fun WorkoutCalories.isSignificantWorkout(): Boolean = 
    activeCaloriesBurned >= 150 && exerciseMinutes >= 15

/**
 * 📊 GET WORKOUT INTENSITY LEVEL
 * 
 * Categorizes the workout intensity based on active calories burned per minute.
 * This helps users understand how intense their workout was.
 * 
 * @return Intensity level: "Light", "Moderate", "High", or "Very High"
 */
fun WorkoutCalories.getIntensityLevel(): String {
    if (exerciseMinutes == 0) return "No Exercise"
    
    val caloriesPerMinute = activeCaloriesBurned.toDouble() / exerciseMinutes
    return when {
        caloriesPerMinute < 3 -> "Light"        // Like casual walking
        caloriesPerMinute < 6 -> "Moderate"     // Like brisk walking or light jogging
        caloriesPerMinute < 10 -> "High"        // Like running or intense cycling
        else -> "Very High"                     // Like sprinting or HIIT training
    }
}

/**
 * ⏱️ FORMAT EXERCISE TIME FOR DISPLAY
 * 
 * Converts exercise minutes into a user-friendly format.
 * 
 * @return Formatted time string (like "45 min" or "1h 15m")
 */
fun WorkoutCalories.getFormattedExerciseTime(): String {
    return if (exerciseMinutes < 60) {
        "${exerciseMinutes}m"
    } else {
        val hours = exerciseMinutes / 60
        val minutes = exerciseMinutes % 60
        if (minutes == 0) "${hours}h" else "${hours}h ${minutes}m"
    }
}