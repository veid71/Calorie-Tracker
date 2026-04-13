package com.calorietracker.utils

import com.calorietracker.database.DailyWeightEntry
import kotlin.math.abs
import kotlin.math.pow

/**
 * Calculates automatic calorie adjustments based on weight changes and goals
 */
class CalorieAdjustmentCalculator {
    
    data class CalorieAdjustment(
        val currentCalories: Int,
        val suggestedCalories: Int,
        val adjustment: Int,
        val reason: String,
        val confidence: Float, // 0.0 to 1.0
        val weightTrend: WeightTrend
    )
    
    data class WeightTrend(
        val direction: TrendDirection,
        val ratePerWeek: Double, // kg or lbs per week
        val duration: Int, // days of data used
        val stability: Float // 0.0 to 1.0, higher = more stable trend
    )
    
    enum class TrendDirection {
        GAINING, LOSING, STABLE, INSUFFICIENT_DATA
    }
    
    /**
     * Calculate calorie adjustment based on recent weight entries
     */
    fun calculateAdjustment(
        recentWeightEntries: List<DailyWeightEntry>,
        currentCalorieGoal: Int,
        weightGoalKg: Double,
        isWeightLoss: Boolean,
        userAge: Int,
        userGender: String,
        userHeightCm: Double,
        activityLevel: String = "moderate"
    ): CalorieAdjustment {
        
        if (recentWeightEntries.size < 7) {
            return CalorieAdjustment(
                currentCalories = currentCalorieGoal,
                suggestedCalories = currentCalorieGoal,
                adjustment = 0,
                reason = "Insufficient weight data (need at least 7 days)",
                confidence = 0.0f,
                weightTrend = WeightTrend(TrendDirection.INSUFFICIENT_DATA, 0.0, recentWeightEntries.size, 0.0f)
            )
        }
        
        val weightTrend = analyzeWeightTrend(recentWeightEntries)
        val currentWeight = recentWeightEntries.first().weight
        
        // Calculate BMR using Mifflin-St Jeor equation
        val bmr = calculateBMR(currentWeight, userHeightCm, userAge, userGender)
        val tdee = calculateTDEE(bmr, activityLevel)
        
        // Determine ideal rate of change (0.5-1 kg per week for weight loss, 0.25-0.5 kg per week for gain)
        val idealWeeklyChange = if (isWeightLoss) -0.75 else 0.375 // kg per week
        
        val adjustment = when {
            weightTrend.direction == TrendDirection.STABLE && abs(currentWeight - weightGoalKg) > 2.0 -> {
                // Weight is stable but far from goal - increase deficit/surplus
                val caloriesPerKgPerWeek = 7700 // approximately 7700 calories per kg
                val weeklyCalorieAdjustment = (idealWeeklyChange * caloriesPerKgPerWeek).toInt()
                val dailyAdjustment = weeklyCalorieAdjustment / 7
                
                CalorieAdjustment(
                    currentCalories = currentCalorieGoal,
                    suggestedCalories = (tdee + dailyAdjustment).toInt(),
                    adjustment = dailyAdjustment,
                    reason = "Weight plateau detected - adjusting to resume ${if (isWeightLoss) "loss" else "gain"}",
                    confidence = weightTrend.stability,
                    weightTrend = weightTrend
                )
            }
            
            abs(weightTrend.ratePerWeek - abs(idealWeeklyChange)) > 0.3 -> {
                // Rate of change is too fast or too slow
                val rateDifference = weightTrend.ratePerWeek - abs(idealWeeklyChange)
                val calorieAdjustment = (rateDifference * 1100).toInt() // ~1100 calories per kg per week
                
                val reason = when {
                    rateDifference > 0.3 -> "Weight changing too quickly - increasing calories"
                    rateDifference < -0.3 -> "Weight changing too slowly - decreasing calories"
                    else -> "Fine-tuning based on weight trend"
                }
                
                CalorieAdjustment(
                    currentCalories = currentCalorieGoal,
                    suggestedCalories = (currentCalorieGoal - calorieAdjustment).coerceIn(
                        (bmr * 0.8).toInt(), // Minimum safe calories
                        (tdee + 500).toInt()  // Maximum reasonable surplus
                    ),
                    adjustment = -calorieAdjustment,
                    reason = reason,
                    confidence = weightTrend.stability,
                    weightTrend = weightTrend
                )
            }
            
            else -> {
                // Current approach is working well
                CalorieAdjustment(
                    currentCalories = currentCalorieGoal,
                    suggestedCalories = currentCalorieGoal,
                    adjustment = 0,
                    reason = "Current calorie goal appears optimal",
                    confidence = weightTrend.stability,
                    weightTrend = weightTrend
                )
            }
        }
        
        return adjustment
    }
    
    private fun analyzeWeightTrend(weightEntries: List<DailyWeightEntry>): WeightTrend {
        if (weightEntries.size < 3) {
            return WeightTrend(TrendDirection.INSUFFICIENT_DATA, 0.0, weightEntries.size, 0.0f)
        }
        
        val weights = weightEntries.map { it.weight }
        val n = weights.size
        
        // Calculate linear regression
        val days = (0 until n).map { it.toDouble() }
        val meanDay = days.average()
        val meanWeight = weights.average()
        
        val slope = days.zip(weights).sumOf { (day, weight) ->
            (day - meanDay) * (weight - meanWeight)
        } / days.sumOf { (it - meanDay).pow(2) }
        
        // Convert slope to weekly rate (slope is per day)
        val weeklyRate = slope * 7
        
        // Calculate R-squared for trend stability
        val predictedWeights = days.map { meanWeight + slope * (it - meanDay) }
        val totalVariance = weights.sumOf { (it - meanWeight).pow(2) }
        val residualVariance = weights.zip(predictedWeights).sumOf { (actual, predicted) ->
            (actual - predicted).pow(2)
        }
        
        val rSquared = if (totalVariance > 0) {
            1 - (residualVariance / totalVariance)
        } else {
            0.0
        }
        
        val stability = rSquared.coerceIn(0.0, 1.0).toFloat()
        
        val direction = when {
            abs(weeklyRate) < 0.1 -> TrendDirection.STABLE
            weeklyRate > 0.1 -> TrendDirection.GAINING
            weeklyRate < -0.1 -> TrendDirection.LOSING
            else -> TrendDirection.STABLE
        }
        
        return WeightTrend(direction, abs(weeklyRate), n, stability)
    }
    
    private fun calculateBMR(weightKg: Double, heightCm: Double, age: Int, gender: String): Double {
        // Mifflin-St Jeor equation
        return when (gender.lowercase()) {
            "male" -> 10 * weightKg + 6.25 * heightCm - 5 * age + 5
            "female" -> 10 * weightKg + 6.25 * heightCm - 5 * age - 161
            else -> 10 * weightKg + 6.25 * heightCm - 5 * age - 78 // Average
        }
    }
    
    private fun calculateTDEE(bmr: Double, activityLevel: String): Double {
        val multiplier = when (activityLevel.lowercase()) {
            "sedentary" -> 1.2
            "light" -> 1.375
            "moderate" -> 1.55
            "active" -> 1.725
            "very_active" -> 1.9
            else -> 1.55
        }
        return bmr * multiplier
    }
}