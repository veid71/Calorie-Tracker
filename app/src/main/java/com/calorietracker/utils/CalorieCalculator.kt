package com.calorietracker.utils

import com.calorietracker.database.WeightGoal
import kotlin.math.abs

object CalorieCalculator {
    
    // Activity level multipliers for TDEE calculation
    private val ACTIVITY_MULTIPLIERS = mapOf(
        "sedentary" to 1.2,
        "lightly_active" to 1.375,
        "moderately_active" to 1.55,
        "very_active" to 1.725,
        "extra_active" to 1.9
    )
    
    // Calories per pound/kg of body weight
    private const val CALORIES_PER_POUND = 3500.0
    private const val CALORIES_PER_KG = 7700.0
    
    data class CalorieRecommendation(
        val recommendedCalories: Int,
        val bmr: Int,
        val tdee: Int,
        val weeklyWeightChange: Double, // kg or lbs per week
        val dailyCalorieDeficit: Int, // positive for deficit, negative for surplus
        val isHealthy: Boolean,
        val healthWarning: String?
    )
    
    /**
     * Calculate recommended daily calories based on weight goal
     */
    fun calculateCalorieRecommendation(
        weightGoal: WeightGoal,
        useMetricUnits: Boolean = true
    ): CalorieRecommendation {
        
        // Calculate BMR using Mifflin-St Jeor equation if we have all data
        val bmr = if (weightGoal.age != null && weightGoal.height != null && weightGoal.gender != null) {
            calculateBMR(
                weight = weightGoal.currentWeight,
                height = weightGoal.height,
                age = weightGoal.age,
                gender = weightGoal.gender,
                useMetricUnits = useMetricUnits
            )
        } else {
            // Fallback to estimated BMR (roughly 22-24 calories per kg or 10-11 per lb)
            val multiplier = if (useMetricUnits) 23.0 else 10.5
            (weightGoal.currentWeight * multiplier).toInt()
        }
        
        // Calculate TDEE (Total Daily Energy Expenditure)
        val activityMultiplier = ACTIVITY_MULTIPLIERS[weightGoal.activityLevel] ?: 1.375
        val tdee = (bmr * activityMultiplier).toInt()
        
        // Calculate weight change needed
        val weightChange = weightGoal.targetWeight - weightGoal.currentWeight
        val daysToGoal = weightGoal.targetDays
        
        // Calculate weekly weight change rate
        val weeklyWeightChange = (weightChange * 7.0) / daysToGoal
        
        // Calculate daily calorie adjustment needed
        val caloriesPerUnit = if (useMetricUnits) CALORIES_PER_KG else CALORIES_PER_POUND
        val dailyCalorieAdjustment = (weightChange * caloriesPerUnit) / daysToGoal
        
        // Calculate recommended daily calories: TDEE plus the calorie adjustment
        // For weight loss: weightChange is negative, so adjustment is negative (eat less than TDEE)
        // For weight gain: weightChange is positive, so adjustment is positive (eat more than TDEE)
        val recommendedCalories = (tdee + dailyCalorieAdjustment).toInt()
        
        // Safety checks for both weight loss and weight gain
        val minCalories = if (weightGoal.gender == "female") 1200 else 1500  // Minimum calories to prevent metabolic slowdown
        val maxCalorieChange = tdee * 0.25  // Don't exceed 25% calorie change (deficit or surplus) for sustainability
        val maxCalories = (tdee * 1.5).toInt()  // Maximum safe calories for weight gain (50% above TDEE)
        
        val isHealthy = recommendedCalories >= minCalories &&               // Above minimum calorie threshold
                       recommendedCalories <= maxCalories &&               // Below maximum calorie threshold for weight gain
                       abs(dailyCalorieAdjustment) <= maxCalorieChange &&   // Calorie change not too extreme
                       abs(weeklyWeightChange) <= (if (useMetricUnits) 1.0 else 2.0)  // Rate <= 1kg or 2lbs per week
        
        val healthWarning = when {
            recommendedCalories < minCalories -> "Calorie goal too low - may slow metabolism and cause nutrient deficiencies"
            recommendedCalories > maxCalories -> "Calorie goal too high for healthy weight gain - may lead to excessive fat gain"
            abs(weeklyWeightChange) > (if (useMetricUnits) 1.0 else 2.0) -> "Weight change rate too aggressive - medical guidelines recommend max 1kg/2lbs per week"
            dailyCalorieAdjustment < -maxCalorieChange -> "Large calorie deficit may be difficult to sustain and could lead to muscle loss"
            dailyCalorieAdjustment > maxCalorieChange -> "Large calorie surplus may lead to excessive fat gain rather than healthy weight gain"
            else -> null  // Goal is within healthy parameters
        }
        
        // Enforce both minimum and maximum calorie limits for safety
        val safeCalories = maxOf(minCalories, minOf(recommendedCalories, maxCalories))
        
        return CalorieRecommendation(
            recommendedCalories = safeCalories,
            bmr = bmr,
            tdee = tdee,
            weeklyWeightChange = weeklyWeightChange,
            dailyCalorieDeficit = dailyCalorieAdjustment.toInt(),
            isHealthy = isHealthy,
            healthWarning = healthWarning
        )
    }
    
    /**
     * Calculate BMR using Mifflin-St Jeor equation
     */
    private fun calculateBMR(
        weight: Double,
        height: Double,
        age: Int,
        gender: String,
        useMetricUnits: Boolean
    ): Int {
        return if (useMetricUnits) {
            // Metric: BMR = 10 * weight(kg) + 6.25 * height(cm) - 5 * age + gender_offset
            val genderOffset = if (gender == "male") 5 else -161
            (10 * weight + 6.25 * height - 5 * age + genderOffset).toInt()
        } else {
            // Imperial: BMR = 4.536 * weight(lbs) + 15.88 * height(inches) - 5 * age + gender_offset
            val genderOffset = if (gender == "male") 5 else -161
            (4.536 * weight + 15.88 * height - 5 * age + genderOffset).toInt()
        }
    }
    
    /**
     * Get activity level options with descriptions for user selection
     * These correspond to the multipliers used in TDEE calculation
     * 
     * @return List of (key, description) pairs for activity levels
     */
    fun getActivityLevels(): List<Pair<String, String>> = listOf(
        "sedentary" to "Sedentary (little/no exercise)",                                   // 1.2x multiplier
        "lightly_active" to "Lightly Active (light exercise 1-3 days/week)",              // 1.375x multiplier
        "moderately_active" to "Moderately Active (moderate exercise 3-5 days/week)",      // 1.55x multiplier
        "very_active" to "Very Active (hard exercise 6-7 days/week)",                     // 1.725x multiplier
        "extra_active" to "Extra Active (very hard exercise, physical job)"                // 1.9x multiplier
    )
}

/*
 * Scientific References:
 * 
 * 1. Mifflin-St Jeor Equation:
 *    Mifflin, M. D., et al. (1990). "A new predictive equation for resting energy expenditure 
 *    in healthy individuals." American Journal of Clinical Nutrition, 51(2), 241-247.
 * 
 * 2. Activity Level Multipliers:
 *    Harris, J. A., & Benedict, F. G. (1918). "A biometric study of human basal metabolism."
 *    Proceedings of the National Academy of Sciences, 4(12), 370-373.
 * 
 * 3. Safe Weight Loss Guidelines:
 *    National Institute of Health (NIH) Guidelines for healthy weight loss: 1-2 pounds per week
 *    World Health Organization (WHO) recommendations for gradual weight changes
 * 
 * 4. Minimum Calorie Requirements:
 *    Academy of Nutrition and Dietetics recommendations for minimum daily calories
 *    to prevent metabolic adaptation and nutrient deficiencies
 */