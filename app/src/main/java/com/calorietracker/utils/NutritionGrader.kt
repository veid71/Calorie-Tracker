package com.calorietracker.utils

// 🧰 CALCULATION TOOLS
import com.calorietracker.database.CalorieEntry    // Food entry data
import com.calorietracker.database.NutritionGoals  // User's nutrition targets
import com.calorietracker.database.getActualCalories   // Extension function for actual calories
import com.calorietracker.database.getActualProtein    // Extension function for actual protein
import com.calorietracker.database.getActualCarbs      // Extension function for actual carbs
import com.calorietracker.database.getActualFat        // Extension function for actual fat
import kotlin.math.abs                            // Absolute value math
import kotlin.math.max                            // Maximum value function
import kotlin.math.min                            // Minimum value function

/**
 * 🎓 NUTRITION GRADER - DAILY NUTRITION REPORT CARD
 * 
 * Hey young programmer! This gives users a daily grade (A, B, C, D, F) for their nutrition.
 * 
 * 📊 How do we grade nutrition?
 * Just like school report cards, we look at different "subjects" and give an overall grade:
 * 
 * 🍎 GRADING CRITERIA:
 * - **Calorie Balance** (25%): Did you stay close to your calorie goal?
 * - **Macro Balance** (25%): Good protein/carb/fat ratios?
 * - **Micronutrients** (20%): Enough fiber? Not too much sodium?
 * - **Food Variety** (15%): Did you eat different types of foods?
 * - **Meal Timing** (15%): Regular meal pattern throughout the day?
 * 
 * 🎯 GRADE SCALE:
 * - 🏆 A (90-100%): Excellent nutrition! Olympic athlete level
 * - 📈 B (80-89%): Great job! Minor improvements possible
 * - 📊 C (70-79%): Good effort, room for improvement
 * - 📉 D (60-69%): Needs work, focus on balance
 * - 💀 F (0-59%): Poor nutrition, significant changes needed
 * 
 * 💡 SMART FEEDBACK:
 * Each grade comes with specific suggestions:
 * - "Great protein intake! Try adding more vegetables."
 * - "You exceeded calories but had excellent macro balance."
 * - "Add more fiber-rich foods like fruits and whole grains."
 */
object NutritionGrader {
    
    /**
     * 🎓 CALCULATE DAILY NUTRITION GRADE
     * 
     * Analyzes a day's worth of food entries and returns comprehensive grade.
     * 
     * @param entries All food entries for the day
     * @param goals User's nutrition goals
     * @param calorieGoal Daily calorie target
     * @return Complete nutrition grade with breakdown
     */
    fun calculateDailyGrade(
        entries: List<CalorieEntry>,
        goals: NutritionGoals?,
        calorieGoal: Int
    ): NutritionGrade {
        
        if (entries.isEmpty()) {
            return NutritionGrade(
                overallGrade = "F",
                overallScore = 0,
                calorieScore = 0,
                macroScore = 0,
                micronutrientScore = 0,
                varietyScore = 0,
                timingScore = 0,
                feedback = listOf("No food logged today. Start tracking your meals!"),
                improvements = listOf("Log at least 3 meals", "Include fruits and vegetables", "Track your calories")
            )
        }
        
        // 🧮 CALCULATE TOTALS
        val totals = calculateDayTotals(entries)
        
        // 📊 CALCULATE COMPONENT SCORES
        val calorieScore = gradeCalorieBalance(totals.calories, calorieGoal)
        val macroScore = gradeMacroBalance(totals, goals)
        val microScore = gradeMicronutrients(totals, goals)
        val varietyScore = gradeVariety(entries)
        val timingScore = gradeMealTiming(entries)
        
        // 🎯 CALCULATE WEIGHTED OVERALL SCORE
        val overallScore = (
            calorieScore * 0.25f +      // 25% weight
            macroScore * 0.25f +        // 25% weight  
            microScore * 0.20f +        // 20% weight
            varietyScore * 0.15f +      // 15% weight
            timingScore * 0.15f         // 15% weight
        ).toInt()
        
        // 🏆 CONVERT SCORE TO LETTER GRADE
        val letterGrade = when (overallScore) {
            in 90..100 -> "A"
            in 80..89 -> "B"
            in 70..79 -> "C"
            in 60..69 -> "D"
            else -> "F"
        }
        
        // 💬 GENERATE FEEDBACK AND SUGGESTIONS
        val feedback = generateFeedback(calorieScore, macroScore, microScore, varietyScore, timingScore)
        val improvements = generateImprovements(calorieScore, macroScore, microScore, varietyScore, timingScore)
        
        return NutritionGrade(
            overallGrade = letterGrade,
            overallScore = overallScore,
            calorieScore = calorieScore,
            macroScore = macroScore,
            micronutrientScore = microScore,
            varietyScore = varietyScore,
            timingScore = timingScore,
            feedback = feedback,
            improvements = improvements
        )
    }
    
    /**
     * 🧮 CALCULATE DAY'S NUTRITION TOTALS
     * 
     * Add up all nutrition values from every food entry for the day.
     */
    private fun calculateDayTotals(entries: List<CalorieEntry>): DayNutritionTotals {
        var totalCalories = 0
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0
        var totalFiber = 0.0
        var totalSugar = 0.0
        var totalSodium = 0.0
        
        entries.forEach { entry ->
            totalCalories += entry.getActualCalories()
            totalProtein += entry.getActualProtein() ?: 0.0
            totalCarbs += entry.getActualCarbs() ?: 0.0
            totalFat += entry.getActualFat() ?: 0.0
            totalFiber += (entry.fiber ?: 0.0) * entry.servings
            totalSugar += (entry.sugar ?: 0.0) * entry.servings
            totalSodium += (entry.sodium ?: 0.0) * entry.servings
        }
        
        return DayNutritionTotals(
            calories = totalCalories,
            protein = totalProtein,
            carbs = totalCarbs,
            fat = totalFat,
            fiber = totalFiber,
            sugar = totalSugar,
            sodium = totalSodium
        )
    }
    
    /**
     * 🔥 GRADE CALORIE BALANCE
     * 
     * How well did user stay within their calorie goal?
     */
    private fun gradeCalorieBalance(actualCalories: Int, goalCalories: Int): Int {
        if (goalCalories <= 0) return 50 // Can't grade without a goal
        
        val difference = abs(actualCalories - goalCalories)
        val percentageOff = (difference.toFloat() / goalCalories.toFloat()) * 100
        
        return when {
            percentageOff <= 5 -> 100    // 🎯 Within 5% of goal = perfect!
            percentageOff <= 10 -> 90    // 📊 Within 10% = excellent
            percentageOff <= 15 -> 80    // 📈 Within 15% = good
            percentageOff <= 25 -> 70    // 📉 Within 25% = okay
            percentageOff <= 40 -> 60    // 🚨 Within 40% = needs work
            else -> 40                   // 💀 Way off target = poor
        }
    }
    
    /**
     * 💪 GRADE MACRO BALANCE
     * 
     * How well balanced were protein, carbs, and fat?
     */
    private fun gradeMacroBalance(totals: DayNutritionTotals, goals: NutritionGoals?): Int {
        if (goals == null) return 70 // Default score without goals
        
        // 🎯 CALCULATE MACRO GOAL ADHERENCE
        val proteinScore = gradeMacroComponent(totals.protein, goals.proteinGoal.toDouble())
        val carbsScore = gradeMacroComponent(totals.carbs, goals.carbsGoal.toDouble())
        val fatScore = gradeMacroComponent(totals.fat, goals.fatGoal.toDouble())
        
        // 📊 AVERAGE THE THREE MACRO SCORES
        return ((proteinScore + carbsScore + fatScore) / 3).toInt()
    }
    
    /**
     * 🔬 GRADE INDIVIDUAL MACRO COMPONENT
     * 
     * Score one macro (protein, carbs, or fat) against its goal.
     */
    private fun gradeMacroComponent(actual: Double, goal: Double): Int {
        if (goal <= 0) return 70 // Default score without goal
        
        val percentageOfGoal = (actual / goal) * 100
        
        return when {
            percentageOfGoal in 90.0..110.0 -> 100  // 🎯 90-110% of goal = perfect
            percentageOfGoal in 80.0..120.0 -> 90   // 📊 80-120% = excellent
            percentageOfGoal in 70.0..130.0 -> 80   // 📈 70-130% = good
            percentageOfGoal in 60.0..140.0 -> 70   // 📉 60-140% = okay
            percentageOfGoal in 50.0..150.0 -> 60   // 🚨 50-150% = needs work
            else -> 40                               // 💀 Way off = poor
        }
    }
    
    /**
     * 🌿 GRADE MICRONUTRIENTS
     * 
     * Score fiber intake and sodium limits.
     */
    private fun gradeMicronutrients(totals: DayNutritionTotals, goals: NutritionGoals?): Int {
        var score = 0
        var components = 0
        
        // 🌾 FIBER SCORE (higher is better)
        if (goals?.fiberGoal != null && goals.fiberGoal > 0) {
            val fiberPercentage = (totals.fiber / goals.fiberGoal) * 100
            score += when {
                fiberPercentage >= 100 -> 100  // 🎯 Met fiber goal
                fiberPercentage >= 75 -> 85    // 📊 Close to fiber goal  
                fiberPercentage >= 50 -> 70    // 📈 Decent fiber
                fiberPercentage >= 25 -> 55    // 📉 Low fiber
                else -> 30                     // 💀 Very low fiber
            }
            components++
        }
        
        // 🧂 SODIUM SCORE (lower is better)
        if (goals?.sodiumGoal != null && goals.sodiumGoal > 0) {
            val sodiumPercentage = (totals.sodium / goals.sodiumGoal) * 100
            score += when {
                sodiumPercentage <= 80 -> 100   // 🎯 Well under sodium limit
                sodiumPercentage <= 100 -> 85   // 📊 At sodium limit
                sodiumPercentage <= 120 -> 70   // 📈 Slightly over limit
                sodiumPercentage <= 150 -> 55   // 📉 Too much sodium
                else -> 30                      // 💀 Way too much sodium
            }
            components++
        }
        
        return if (components > 0) score / components else 75 // Default score
    }
    
    /**
     * 🌈 GRADE FOOD VARIETY
     * 
     * Did user eat different types of foods, or just pizza all day?
     */
    private fun gradeVariety(entries: List<CalorieEntry>): Int {
        val uniqueFoods = entries.map { it.foodName.lowercase() }.toSet()
        val totalEntries = entries.size
        
        if (totalEntries == 0) return 0
        
        val varietyRatio = uniqueFoods.size.toFloat() / totalEntries.toFloat()
        
        return when {
            varietyRatio >= 0.8 -> 100    // 🌈 80%+ unique foods = excellent variety
            varietyRatio >= 0.6 -> 85     // 🎨 60%+ unique = good variety
            varietyRatio >= 0.4 -> 70     // 📊 40%+ unique = okay variety
            varietyRatio >= 0.2 -> 55     // 📉 20%+ unique = low variety
            else -> 30                    // 💀 Very repetitive eating
        }
    }
    
    /**
     * ⏰ GRADE MEAL TIMING
     * 
     * Did user eat at regular intervals throughout the day?
     */
    private fun gradeMealTiming(entries: List<CalorieEntry>): Int {
        if (entries.size < 2) return 50 // Need multiple entries to grade timing
        
        // 📅 ANALYZE MEAL DISTRIBUTION
        val timestamps = entries.map { it.timestamp }.sorted()
        val dayStart = timestamps.first()
        val dayEnd = timestamps.last()
        val daySpan = dayEnd - dayStart
        
        // 🕒 CHECK FOR REGULAR MEAL INTERVALS
        val intervals = mutableListOf<Long>()
        for (i in 1 until timestamps.size) {
            intervals.add(timestamps[i] - timestamps[i-1])
        }
        
        // ⏰ IDEAL MEAL TIMING ANALYSIS
        val averageInterval = intervals.average()
        val intervalVariation = intervals.map { abs(it - averageInterval) }.average()
        
        // 🎯 SCORE BASED ON REGULARITY AND DISTRIBUTION
        return when {
            daySpan >= 8 * 60 * 60 * 1000 && intervalVariation < 2 * 60 * 60 * 1000 -> 100 // 8+ hours span, regular intervals
            daySpan >= 6 * 60 * 60 * 1000 && intervalVariation < 3 * 60 * 60 * 1000 -> 85  // 6+ hours, fairly regular
            daySpan >= 4 * 60 * 60 * 1000 -> 70  // 4+ hours span
            daySpan >= 2 * 60 * 60 * 1000 -> 60  // 2+ hours span
            else -> 40                           // All meals logged in short time span
        }
    }
    
    /**
     * 💬 GENERATE PERSONALIZED FEEDBACK
     * 
     * Create encouraging messages based on grades.
     */
    private fun generateFeedback(
        calorieScore: Int,
        macroScore: Int, 
        microScore: Int,
        varietyScore: Int,
        timingScore: Int
    ): List<String> {
        val feedback = mutableListOf<String>()
        
        // 🔥 CALORIE FEEDBACK
        when (calorieScore) {
            in 90..100 -> feedback.add("🎯 Perfect calorie balance!")
            in 80..89 -> feedback.add("📊 Great calorie control!")
            in 70..79 -> feedback.add("📈 Good calorie awareness!")
            else -> feedback.add("🚨 Focus on staying closer to your calorie goal")
        }
        
        // 💪 MACRO FEEDBACK
        when (macroScore) {
            in 90..100 -> feedback.add("💪 Excellent macro balance!")
            in 80..89 -> feedback.add("📊 Great protein/carb/fat ratios!")
            else -> feedback.add("🎯 Work on balancing proteins, carbs, and fats")
        }
        
        // 🌈 VARIETY FEEDBACK
        when (varietyScore) {
            in 85..100 -> feedback.add("🌈 Amazing food variety!")
            in 70..84 -> feedback.add("🎨 Good food diversity!")
            else -> feedback.add("🍎 Try adding more variety to your meals")
        }
        
        return feedback
    }
    
    /**
     * 💡 GENERATE IMPROVEMENT SUGGESTIONS
     * 
     * Specific actionable advice based on weak areas.
     */
    private fun generateImprovements(
        calorieScore: Int,
        macroScore: Int,
        microScore: Int, 
        varietyScore: Int,
        timingScore: Int
    ): List<String> {
        val improvements = mutableListOf<String>()
        
        if (calorieScore < 70) {
            improvements.add("📏 Use measuring cups for portion control")
            improvements.add("🥗 Add more vegetables to feel full with fewer calories")
        }
        
        if (macroScore < 70) {
            improvements.add("🥩 Include lean protein at each meal")
            improvements.add("🍞 Choose complex carbs over simple sugars")
            improvements.add("🥑 Add healthy fats like nuts, avocado, olive oil")
        }
        
        if (microScore < 70) {
            improvements.add("🌾 Add more fiber-rich foods like beans and whole grains")
            improvements.add("🧂 Reduce processed foods to lower sodium")
        }
        
        if (varietyScore < 70) {
            improvements.add("🌈 Try a new fruit or vegetable this week")
            improvements.add("🍽️ Mix up your protein sources")
        }
        
        if (timingScore < 70) {
            improvements.add("⏰ Aim for 3 regular meals spread throughout the day")
            improvements.add("🍎 Add healthy snacks between meals if needed")
        }
        
        return improvements
    }
}

/**
 * 📊 DAY NUTRITION TOTALS
 * 
 * Container for daily nutrition totals used in grading.
 */
data class DayNutritionTotals(
    val calories: Int,      // 🔥 Total calories for the day
    val protein: Double,    // 💪 Total protein in grams
    val carbs: Double,      // 🍞 Total carbs in grams
    val fat: Double,        // 🥑 Total fat in grams
    val fiber: Double,      // 🌾 Total fiber in grams
    val sugar: Double,      // 🍯 Total sugar in grams
    val sodium: Double      // 🧂 Total sodium in milligrams
)

/**
 * 🎓 NUTRITION GRADE
 * 
 * Complete daily nutrition report card with grade and feedback.
 */
data class NutritionGrade(
    val overallGrade: String,        // 🏆 "A", "B", "C", "D", or "F"
    val overallScore: Int,           // 📊 Overall percentage score (0-100)
    val calorieScore: Int,           // 🔥 Calorie balance score (0-100)
    val macroScore: Int,             // 💪 Macro balance score (0-100)
    val micronutrientScore: Int,     // 🌿 Micronutrient score (0-100)
    val varietyScore: Int,           // 🌈 Food variety score (0-100)
    val timingScore: Int,            // ⏰ Meal timing score (0-100)
    val feedback: List<String>,      // 💬 Positive feedback messages
    val improvements: List<String>   // 💡 Specific improvement suggestions
)