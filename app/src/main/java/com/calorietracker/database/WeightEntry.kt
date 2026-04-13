package com.calorietracker.database

// 🧰 ROOM DATABASE TOOLS
import androidx.room.Entity      // Tells Android this class represents a database table
import androidx.room.PrimaryKey  // Marks which field is the unique ID

/**
 * ⚖️ WEIGHT ENTRY - WEIGHT TRACKING & PROGRESS MONITORING
 * 
 * Hey future programmer! This class stores your daily weight measurements.
 * Think of this like a digital bathroom scale that remembers your weight over time.
 * 
 * 🎯 Why Track Weight?
 * Weight tracking is essential for health and fitness goals:
 * - Monitor weight loss or weight gain progress over time
 * - See trends and patterns (like weekly fluctuations)
 * - Adjust calorie goals based on progress toward weight targets
 * - Detect if current eating habits are working toward goals
 * - Provide data for calculating BMI and other health metrics
 * 
 * 📊 Smart Scale Integration:
 * This app supports smart scales like Renpho that can automatically sync weight data.
 * The scale connects via Bluetooth and automatically adds entries when you step on it.
 * You can also manually enter weight from any scale.
 * 
 * 📈 Weight Trend Analysis:
 * Daily weight can fluctuate due to many factors (water retention, food timing, etc.)
 * The app looks at longer-term trends (weekly averages) to determine real progress.
 * This prevents users from getting discouraged by daily fluctuations.
 * 
 * 🌍 Unit Support:
 * Weight can be stored in kilograms (kg) or pounds (lbs) based on user preference.
 * The app handles conversion and display automatically based on the user's region settings.
 * 
 * 📝 Progress Notes:
 * Users can add optional notes to weight entries to track context:
 * - "After vacation - expect temporary gain"
 * - "Started new workout routine"
 * - "Feeling more energetic lately"
 * 
 * @property date The date of this weight measurement (like "2024-08-31")
 * @property weight The weight measurement in kg or lbs (like 75.5 or 166.2)
 * @property timestamp Exact time when this measurement was recorded (for sorting)
 * @property notes Optional user notes about this measurement (like "After morning workout")
 */
// 🏷️ DATABASE TABLE SETUP
// @Entity tells Android "this class should become a table in our database"
// tableName = "weight_entries" means the table will be called "weight_entries"
@Entity(tableName = "weight_entries")
data class WeightEntry(
    // 🔑 PRIMARY KEY - The unique ID for this weight entry
    // We use date as primary key because we typically only record one weight per day
    // If someone weighs themselves multiple times per day, the latest measurement replaces the earlier one
    @PrimaryKey 
    val date: String, // 📅 Date in "YYYY-MM-DD" format (like "2024-08-31")
    
    // ⚖️ WEIGHT MEASUREMENT - The main data we're tracking
    val weight: Double, // 📏 Weight in kg or lbs based on user preference (like 75.5 kg or 166.2 lbs)
    
    // ⏰ TIMING INFORMATION - When this measurement was recorded
    val timestamp: Long = System.currentTimeMillis(), // 🕐 Exact time of measurement (computer timestamp)
    
    // 📝 OPTIONAL CONTEXT - User notes about this measurement
    val notes: String? = null // 💭 Optional notes (like "After morning workout" or "Before breakfast")
)

/**
 * 🧮 HELPFUL EXTENSION FUNCTIONS FOR WEIGHT ANALYSIS
 * 
 * These functions add analytical capabilities to our weight entries,
 * helping users understand their progress and trends.
 */

/**
 * 📊 CALCULATE WEIGHT CHANGE FROM ANOTHER ENTRY
 * 
 * Compares this weight entry to another one to show progress.
 * Positive number means weight gain, negative means weight loss.
 * 
 * Example: Current weight 75kg, previous 77kg → returns -2.0 (lost 2kg)
 * 
 * @param previousEntry The earlier weight entry to compare against
 * @return Weight change in the same units (positive = gain, negative = loss)
 */
fun WeightEntry.getWeightChangeFrom(previousEntry: WeightEntry): Double {
    return this.weight - previousEntry.weight
}

/**
 * 📈 GET FORMATTED WEIGHT CHANGE STRING
 * 
 * Creates a user-friendly string showing weight change with appropriate sign and formatting.
 * 
 * @param previousEntry The earlier weight entry to compare against
 * @param unit The weight unit to display ("kg" or "lbs")
 * @return Formatted string like "+2.5 kg" or "-3.2 lbs"
 */
fun WeightEntry.getFormattedWeightChange(previousEntry: WeightEntry, unit: String = "kg"): String {
    val change = getWeightChangeFrom(previousEntry)
    val sign = if (change >= 0) "+" else ""
    return "${sign}${String.format("%.1f", change)} $unit"
}

/**
 * 🏷️ GET WEIGHT TREND INDICATOR
 * 
 * Provides a simple trend indicator based on weight change.
 * Useful for showing progress direction with icons or colors.
 * 
 * @param previousEntry The earlier weight entry to compare against
 * @param threshold Minimum change considered significant (default 0.1)
 * @return Trend indicator: "up", "down", or "stable"
 */
fun WeightEntry.getTrendIndicator(previousEntry: WeightEntry, threshold: Double = 0.1): String {
    val change = getWeightChangeFrom(previousEntry)
    return when {
        change > threshold -> "up"
        change < -threshold -> "down"
        else -> "stable"
    }
}

/**
 * 🎯 CALCULATE BMI (if height is provided)
 * 
 * Calculates Body Mass Index using this weight entry.
 * BMI = weight(kg) / height(m)²
 * 
 * @param heightInCm Height in centimeters
 * @return BMI value (like 23.5) or null if invalid height
 */
fun WeightEntry.calculateBMI(heightInCm: Double): Double? {
    if (heightInCm <= 0) return null
    
    val heightInMeters = heightInCm / 100.0
    // Convert weight to kg if needed (assuming weight > 100 means it's in lbs)
    val weightInKg = if (weight > 100) weight * 0.453592 else weight
    
    return weightInKg / (heightInMeters * heightInMeters)
}

/**
 * 📊 GET BMI CATEGORY
 * 
 * Categorizes BMI value into standard health categories.
 * 
 * @param heightInCm Height in centimeters
 * @return BMI category: "Underweight", "Normal", "Overweight", or "Obese"
 */
fun WeightEntry.getBMICategory(heightInCm: Double): String? {
    val bmi = calculateBMI(heightInCm) ?: return null
    
    return when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal"
        bmi < 30.0 -> "Overweight"
        else -> "Obese"
    }
}

/**
 * 📅 CHECK IF ENTRY IS FROM TODAY
 * 
 * Determines if this weight entry is from today's date.
 * Useful for highlighting current measurements in the UI.
 * 
 * @return True if this entry is from today, false otherwise
 */
fun WeightEntry.isFromToday(): Boolean {
    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        .format(java.util.Date())
    return date == today
}

/**
 * 📝 CHECK IF ENTRY HAS NOTES
 * 
 * Simple check for whether user added notes to this weight entry.
 * 
 * @return True if notes exist and aren't empty, false otherwise
 */
fun WeightEntry.hasNotes(): Boolean = !notes.isNullOrBlank()