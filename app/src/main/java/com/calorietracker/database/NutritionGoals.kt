package com.calorietracker.database

// 🧰 ROOM DATABASE TOOLS
import androidx.room.Entity      // Tells Android this class represents a database table
import androidx.room.PrimaryKey  // Marks which field is the unique ID

/**
 * 🎯 NUTRITION GOALS - USER'S DAILY TARGETS AND PREFERENCES
 * 
 * Hey future programmer! This class stores the user's daily nutrition goals and app preferences.
 * Think of this like a digital nutrition coach that remembers exactly what the user wants to achieve each day.
 * 
 * 🏃‍♂️ What Are Nutrition Goals?
 * These are the daily targets a user sets for themselves based on their health objectives:
 * - Weight loss: Lower calorie goal, moderate protein, controlled carbs
 * - Weight gain: Higher calorie goal, high protein for muscle building
 * - Maintenance: Balanced macros to maintain current weight
 * - Athletic training: Higher protein and carbs for performance and recovery
 * 
 * 📊 The "Big Three" Macronutrients:
 * - Calories: Total energy intake (like 2000 per day)
 * - Protein: For muscle building/maintenance (like 150g per day)
 * - Carbohydrates: For quick energy (like 200g per day) 
 * - Fat: For long-term energy and vitamins (like 80g per day)
 * 
 * 🌟 Additional Nutrition Targets:
 * - Fiber: For digestive health (like 25g per day)
 * - Sugar: Daily limit to avoid too much (like 50g max per day)
 * - Sodium: Daily limit for heart health (like 2300mg max per day)
 * 
 * 🌍 Regional and Personal Preferences:
 * - Region selection affects which food databases we use (US, UK, Canada, etc.)
 * - Unit preferences (metric vs imperial) for user comfort
 * - UI preferences like showing nutrition tips and recommendations
 * 
 * 🎯 How Goals Are Used Throughout The App:
 * - Main screen shows progress toward daily goals (like "1200 / 2000 calories")
 * - Food entry screen warns if you're going over limits (like too much sodium)
 * - Analytics show trends over time (like "averaging 90% of protein goal")
 * - Recommendations adjust based on progress (like "add more fiber to your diet")
 * 
 * 💡 Smart Goal Adjustments:
 * - Workout calories can temporarily increase calorie goals
 * - Goals can be personalized based on user's weight, age, activity level
 * - Goals can be adjusted over time as users progress toward their targets
 * 
 * @property id Always 1 - we only store one set of goals per user
 * @property calorieGoal Daily calorie target (like 2000 for maintenance, 1800 for weight loss)
 * @property proteinGoal Daily protein target in grams (like 150g for active person)
 * @property carbsGoal Daily carbohydrate target in grams (like 200g for balanced diet)
 * @property fatGoal Daily fat target in grams (like 80g for balanced diet)
 * @property fiberGoal Daily fiber target in grams (like 25g for digestive health)
 * @property sugarGoal Daily sugar LIMIT in grams (like 50g max - this is a ceiling, not a target)
 * @property sodiumGoal Daily sodium LIMIT in mg (like 2300mg max for heart health)
 * @property selectedRegion User's country/region for food database selection ("US", "UK", "CA", "AU")
 * @property useMetricUnits Whether to show kg/grams (true) or lbs/ounces (false)
 * @property showNutritionTips Whether to display helpful nutrition advice and tips
 * @property lastUpdated When these goals were last modified (for sync and versioning)
 */
// 🏷️ DATABASE TABLE SETUP
// @Entity tells Android "this class should become a table in our database"
// tableName = "nutrition_goals" means the table will be called "nutrition_goals"
@Entity(tableName = "nutrition_goals")
data class NutritionGoals(
    // 🔑 PRIMARY KEY - Always 1 because we only need one set of goals per user
    // Unlike other entities that have many entries, this is a "settings" table with just one row
    @PrimaryKey 
    val id: Int = 1, // 🎯 Always 1 - we only store one set of goals per user
    
    // 🔥 MAIN DAILY NUTRITION TARGETS
    // These are the core goals that users try to reach each day
    val calorieGoal: Int = 2000,     // ⚡ Daily calorie target (like 2000 for average adult)
    val proteinGoal: Double = 50.0,  // 💪 Daily protein target in grams (like 50g minimum)
    val carbsGoal: Double = 250.0,   // 🍞 Daily carbs target in grams (like 250g for energy)
    val fatGoal: Double = 65.0,      // 🥑 Daily fat target in grams (like 65g for balanced diet)
    
    // 🌿 ADDITIONAL HEALTH TARGETS
    // These help users track specific aspects of their nutrition
    val fiberGoal: Double = 25.0,    // 🌾 Daily fiber target in grams (like 25g for digestion)
    val sugarGoal: Double = 50.0,    // 🍯 Daily sugar LIMIT in grams (like 50g max - this is a ceiling!)
    val sodiumGoal: Double = 2300.0, // 🧂 Daily sodium LIMIT in mg (like 2300mg max for heart health)
    
    // 🌍 REGIONAL AND PERSONAL PREFERENCES
    // These customize the app experience for the user's location and preferences
    val selectedRegion: String = "US",      // 🗺️ Country/region ("US", "UK", "CA", "AU" - affects food databases)
    val useMetricUnits: Boolean = false,    // 📏 True = kg/grams, False = lbs/ounces (based on region)
    val showNutritionTips: Boolean = true,  // 💡 Whether to show helpful nutrition advice in the app
    
    // 📅 METADATA - Information about when these settings were last changed
    val lastUpdated: Long = System.currentTimeMillis() // ⏰ When goals were last modified (computer timestamp)
)

/**
 * 🧮 HELPFUL EXTENSION FUNCTIONS FOR NUTRITION GOAL ANALYSIS
 * 
 * These functions add useful capabilities to work with nutrition goals,
 * helping calculate progress, validate targets, and provide user guidance.
 */

/**
 * 🎯 CALCULATE CALORIE DISTRIBUTION FROM MACROS
 * 
 * Determines what percentage of calories come from protein, carbs, and fat.
 * This helps verify that macro goals make sense together (should add up to total calories).
 * 
 * @return Map with "protein", "carbs", "fat" percentages, or null if goals are invalid
 */
fun NutritionGoals.getMacroDistribution(): Map<String, Double>? {
    val proteinCalories = proteinGoal * 4  // Protein has 4 calories per gram
    val carbCalories = carbsGoal * 4       // Carbs have 4 calories per gram  
    val fatCalories = fatGoal * 9          // Fat has 9 calories per gram
    
    val totalMacroCalories = proteinCalories + carbCalories + fatCalories
    
    // Return null if the macros don't make sense with the calorie goal
    if (totalMacroCalories <= 0 || calorieGoal <= 0) return null
    
    return mapOf(
        "protein" to (proteinCalories / calorieGoal * 100),
        "carbs" to (carbCalories / calorieGoal * 100),
        "fat" to (fatCalories / calorieGoal * 100)
    )
}

/**
 * ✅ VALIDATE THAT NUTRITION GOALS ARE REALISTIC
 * 
 * Checks if the user's goals are within reasonable ranges for human health.
 * This prevents obviously incorrect goals like 100 calories per day or 5000g protein.
 * 
 * @return True if goals seem realistic, false if they're probably mistakes
 */
fun NutritionGoals.areGoalsRealistic(): Boolean {
    return calorieGoal in 800..5000 &&           // Reasonable calorie range
           proteinGoal in 20.0..300.0 &&         // Reasonable protein range
           carbsGoal in 50.0..800.0 &&           // Reasonable carb range
           fatGoal in 20.0..200.0 &&             // Reasonable fat range
           fiberGoal in 10.0..80.0 &&            // Reasonable fiber range
           sugarGoal in 20.0..200.0 &&           // Reasonable sugar limit
           sodiumGoal in 1000.0..5000.0          // Reasonable sodium limit
}

/**
 * 🏷️ GET GOALS CATEGORY (Weight Loss/Maintenance/Gain)
 * 
 * Attempts to determine the user's likely goal based on their calorie target.
 * This is a rough estimate since we don't know their BMR, but helpful for UI.
 * 
 * @return Goal category: "Weight Loss", "Maintenance", or "Weight Gain"
 */
fun NutritionGoals.getGoalCategory(): String {
    return when {
        calorieGoal < 1600 -> "Weight Loss"      // Lower calories suggest weight loss
        calorieGoal > 2400 -> "Weight Gain"      // Higher calories suggest weight gain
        else -> "Maintenance"                     // Moderate calories suggest maintenance
    }
}

/**
 * 🎨 GET RECOMMENDED MACRO SPLIT FOR GOAL TYPE
 * 
 * Provides standard macro recommendations based on the goal category.
 * These are general guidelines, not personalized advice.
 * 
 * @return Map with recommended percentages for "protein", "carbs", "fat"
 */
fun NutritionGoals.getRecommendedMacroSplit(): Map<String, Double> {
    return when (getGoalCategory()) {
        "Weight Loss" -> mapOf("protein" to 30.0, "carbs" to 40.0, "fat" to 30.0)    // Higher protein for muscle retention
        "Weight Gain" -> mapOf("protein" to 25.0, "carbs" to 45.0, "fat" to 30.0)    // Higher carbs for energy
        else -> mapOf("protein" to 20.0, "carbs" to 50.0, "fat" to 30.0)             // Balanced maintenance split
    }
}

/**
 * 📊 CALCULATE DAILY PROGRESS PERCENTAGE
 * 
 * Determines what percentage of each goal has been achieved today.
 * 
 * @param consumed Map of consumed nutrients (like what CalorieRepository.getTodaysNutritionTotals() returns)
 * @return Map with progress percentages for each nutrient
 */
fun NutritionGoals.calculateProgressPercentages(consumed: Map<String, Double>): Map<String, Double> {
    return mapOf(
        "calories" to ((consumed["calories"] ?: 0.0) / calorieGoal * 100),
        "protein" to ((consumed["protein"] ?: 0.0) / proteinGoal * 100),
        "carbs" to ((consumed["carbs"] ?: 0.0) / carbsGoal * 100),
        "fat" to ((consumed["fat"] ?: 0.0) / fatGoal * 100),
        "fiber" to ((consumed["fiber"] ?: 0.0) / fiberGoal * 100),
        "sugar" to ((consumed["sugar"] ?: 0.0) / sugarGoal * 100),
        "sodium" to ((consumed["sodium"] ?: 0.0) / sodiumGoal * 100)
    )
}

/**
 * ⚠️ GET NUTRIENTS OVER LIMIT
 * 
 * Identifies which nutrients (sugar, sodium) have exceeded their daily limits.
 * 
 * @param consumed Map of consumed nutrients
 * @return List of nutrient names that are over their limits
 */
fun NutritionGoals.getNutrientsOverLimit(consumed: Map<String, Double>): List<String> {
    val overLimit = mutableListOf<String>()
    
    if ((consumed["sugar"] ?: 0.0) > sugarGoal) {
        overLimit.add("sugar")
    }
    if ((consumed["sodium"] ?: 0.0) > sodiumGoal) {
        overLimit.add("sodium")
    }
    
    return overLimit
}

/**
 * 🏆 GET NUTRIENTS ON TRACK
 * 
 * Identifies which nutrients are within 10% of their daily targets (the "green zone").
 * 
 * @param consumed Map of consumed nutrients
 * @return List of nutrient names that are on track for their goals
 */
fun NutritionGoals.getNutrientsOnTrack(consumed: Map<String, Double>): List<String> {
    val onTrack = mutableListOf<String>()
    val progressPercentages = calculateProgressPercentages(consumed)
    
    progressPercentages.forEach { (nutrient, percentage) ->
        if (percentage in 90.0..110.0) {  // Within 10% of goal
            onTrack.add(nutrient)
        }
    }
    
    return onTrack
}