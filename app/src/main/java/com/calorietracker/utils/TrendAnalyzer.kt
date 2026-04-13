package com.calorietracker.utils

// 🧰 DATA ANALYSIS TOOLS
import android.content.Context                     // Android system access
import androidx.core.content.ContextCompat         // Resource access helpers
import com.calorietracker.R                       // App resources
import com.calorietracker.database.CalorieEntry    // Food entry data
import com.calorietracker.database.WeightEntry     // Weight measurement data
import com.calorietracker.database.getActualCalories   // Extension function for actual calories
import com.calorietracker.database.getActualProtein    // Extension function for actual protein
import com.calorietracker.database.getActualCarbs      // Extension function for actual carbs
import com.calorietracker.database.getActualFat        // Extension function for actual fat
import com.calorietracker.widgets.TrendDataPoint   // Chart data structure
import com.calorietracker.widgets.TrendMetricType  // Type of metric to analyze
import com.calorietracker.widgets.TrendTimeFrame   // Weekly vs monthly analysis
import java.text.SimpleDateFormat                  // Date formatting
import java.util.*                                 // Date utilities
import kotlin.math.abs                            // Math functions

/**
 * 📊 TREND ANALYZER - SMART NUTRITION PATTERN DETECTION
 * 
 * Hey young programmer! This is like a nutrition detective that finds patterns
 * in your eating habits over time.
 * 
 * 🔍 What kinds of trends can we detect?
 * - 📈 **Upward trends**: "Your protein intake is improving!"
 * - 📉 **Downward trends**: "Calories have been decreasing"
 * - 🎯 **Goal adherence**: "You hit your calorie goal 5/7 days this week"
 * - 🌊 **Consistency**: "Your eating schedule is getting more regular"
 * - 🏆 **Personal bests**: "Best protein day was Tuesday with 145g!"
 * 
 * 📊 Analysis Types:
 * 1. **Raw Data Trends**: Simple line graphs of daily values
 * 2. **Moving Averages**: Smoothed trends removing daily fluctuations
 * 3. **Goal Comparison**: How often you hit your nutrition targets
 * 4. **Weekly Summaries**: Average values for each week
 * 5. **Streak Analysis**: Consecutive days meeting goals
 * 
 * 🎯 Smart Insights Generated:
 * - "Your average calories this week: 1,847 (target: 1,800)"
 * - "Protein intake up 12% compared to last week"
 * - "Most consistent macro balance on weekdays vs weekends"
 * - "Suggestion: Your Tuesday meals are perfectly balanced!"
 * 
 * 💡 How it works:
 * 1. Take your food entries from the database
 * 2. Group them by day, week, or month
 * 3. Calculate totals for each time period
 * 4. Identify patterns and trends
 * 5. Generate actionable insights and suggestions
 */
object TrendAnalyzer {
    
    /**
     * 📈 GENERATE TREND DATA FOR CHART
     * 
     * Convert raw food entries into chart-ready data points.
     * 
     * @param entries List of food entries from database
     * @param metricType What nutrition value to analyze
     * @param timeFrame How many days to include
     * @param goalValue Daily goal for this metric (optional)
     * @return List of data points ready for chart display
     */
    fun generateTrendData(
        context: Context,
        entries: List<CalorieEntry>,
        metricType: TrendMetricType,
        timeFrame: TrendTimeFrame,
        goalValue: Float? = null
    ): List<TrendDataPoint> {
        
        // 📅 DETERMINE DATE RANGE
        val daysToAnalyze = when (timeFrame) {
            TrendTimeFrame.WEEKLY -> 7
            TrendTimeFrame.MONTHLY -> 30
        }
        
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, -daysToAnalyze + 1)
        val startDate = calendar.timeInMillis
        
        // 🗓️ CREATE DATA POINT FOR EACH DAY
        val dataPoints = mutableListOf<TrendDataPoint>()
        
        for (dayOffset in 0 until daysToAnalyze) {
            calendar.timeInMillis = startDate
            calendar.add(Calendar.DAY_OF_MONTH, dayOffset)
            
            // 📅 GET START AND END OF THIS DAY
            val dayStart = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val dayEnd = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            
            // 🍎 FILTER ENTRIES FOR THIS DAY
            val dayEntries = entries.filter { entry ->
                entry.timestamp in dayStart..dayEnd
            }
            
            // 🧮 CALCULATE METRIC VALUE FOR THIS DAY
            val dayValue = calculateMetricValue(dayEntries, metricType)
            
            // 🎨 DETERMINE POINT COLOR (OPTIONAL)
            val pointColor = goalValue?.let { goal ->
                when {
                    dayValue >= goal * 0.9f -> ContextCompat.getColor(context, R.color.success_green) // Close to or above goal
                    dayValue >= goal * 0.7f -> ContextCompat.getColor(context, R.color.warning_yellow) // Decent progress
                    else -> ContextCompat.getColor(context, R.color.error_red) // Below goal
                }
            }
            
            // 📊 CREATE DATA POINT
            dataPoints.add(
                TrendDataPoint(
                    date = dayStart,
                    value = dayValue,
                    label = generateDayLabel(dayValue, goalValue, metricType),
                    color = pointColor
                )
            )
        }
        
        return dataPoints
    }
    
    /**
     * 🧮 CALCULATE METRIC VALUE FOR DAY
     * 
     * Sum up the specified nutrition metric for all entries in a day.
     */
    private fun calculateMetricValue(entries: List<CalorieEntry>, metricType: TrendMetricType): Float {
        return when (metricType) {
            TrendMetricType.CALORIES -> entries.sumOf { it.getActualCalories() }.toFloat()
            TrendMetricType.PROTEIN -> entries.sumOf { it.getActualProtein() ?: 0.0 }.toFloat()
            TrendMetricType.CARBS -> entries.sumOf { it.getActualCarbs() ?: 0.0 }.toFloat()
            TrendMetricType.FAT -> entries.sumOf { it.getActualFat() ?: 0.0 }.toFloat()
            TrendMetricType.WEIGHT -> 0f // Weight handled separately from food entries
        }
    }
    
    /**
     * 🏷️ GENERATE DAY LABEL
     * 
     * Create helpful label for each data point.
     */
    private fun generateDayLabel(value: Float, goal: Float?, metricType: TrendMetricType): String? {
        return goal?.let { targetGoal ->
            val percentage = (value / targetGoal * 100).toInt()
            when {
                percentage >= 100 -> "🎯 Goal achieved!"
                percentage >= 90 -> "📊 Close to goal"
                percentage >= 70 -> "📈 Good progress"
                percentage >= 50 -> "📉 Needs improvement"
                else -> "🚨 Far from goal"
            }
        }
    }
    
    /**
     * ⚖️ GENERATE WEIGHT TREND DATA
     * 
     * Create trend data specifically for weight measurements.
     */
    fun generateWeightTrendData(
        weightEntries: List<WeightEntry>,
        timeFrame: TrendTimeFrame
    ): List<TrendDataPoint> {
        
        val daysToAnalyze = when (timeFrame) {
            TrendTimeFrame.WEEKLY -> 7
            TrendTimeFrame.MONTHLY -> 30
        }
        
        // 🗓️ FILTER RECENT WEIGHT ENTRIES
        val cutoffDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -daysToAnalyze)
        }.timeInMillis
        
        val recentWeights = weightEntries
            .filter { it.timestamp >= cutoffDate }
            .sortedBy { it.timestamp }
        
        // 📊 CONVERT TO TREND DATA POINTS
        return recentWeights.map { weightEntry ->
            TrendDataPoint(
                date = weightEntry.timestamp,
                value = weightEntry.weight.toFloat(),
                label = "⚖️ ${weightEntry.weight} lbs"
            )
        }
    }
    
    /**
     * 📊 GENERATE TREND INSIGHTS
     * 
     * Analyze trends and generate helpful insights for the user.
     */
    fun generateTrendInsights(
        dataPoints: List<TrendDataPoint>,
        metricType: TrendMetricType,
        goalValue: Float? = null
    ): TrendInsights {
        
        if (dataPoints.size < 3) {
            return TrendInsights(
                summary = "Need more data for trend analysis",
                insights = listOf("Log food for a few more days to see patterns!"),
                recommendations = listOf("Keep tracking consistently", "Set up nutrition goals")
            )
        }
        
        // 📈 CALCULATE TREND DIRECTION
        val firstThird = dataPoints.take(dataPoints.size / 3)
        val lastThird = dataPoints.takeLast(dataPoints.size / 3)
        val firstAverage = firstThird.map { it.value }.average().toFloat()
        val lastAverage = lastThird.map { it.value }.average().toFloat()
        val trendDirection = lastAverage - firstAverage
        val trendPercentage = if (firstAverage > 0) (trendDirection / firstAverage * 100) else 0f
        
        // 🎯 GOAL ADHERENCE ANALYSIS
        val goalInsights = goalValue?.let { goal ->
            val daysMetGoal = dataPoints.count { it.value >= goal * 0.9f } // Within 90% counts as "met"
            val adherencePercentage = (daysMetGoal.toFloat() / dataPoints.size * 100).toInt()
            
            listOf(
                "🎯 Goal adherence: $adherencePercentage% ($daysMetGoal/${dataPoints.size} days)",
                when {
                    adherencePercentage >= 80 -> "🏆 Excellent consistency!"
                    adherencePercentage >= 60 -> "📊 Good progress, keep it up!"
                    else -> "📈 Room for improvement on consistency"
                }
            )
        } ?: emptyList()
        
        // 📊 TREND INSIGHTS
        val trendInsights = mutableListOf<String>()
        
        when {
            abs(trendPercentage) < 5 -> trendInsights.add("➡️ Stable ${metricType.name.lowercase()} intake")
            trendPercentage > 10 -> trendInsights.add("📈 ${metricType.name.lowercase().replaceFirstChar { it.uppercase() }} increasing by ${trendPercentage.toInt()}%")
            trendPercentage < -10 -> trendInsights.add("📉 ${metricType.name.lowercase().replaceFirstChar { it.uppercase() }} decreasing by ${abs(trendPercentage).toInt()}%")
            trendPercentage > 0 -> trendInsights.add("📊 Slight upward trend in ${metricType.name.lowercase()}")
            else -> trendInsights.add("📊 Slight downward trend in ${metricType.name.lowercase()}")
        }
        
        // 🏆 BEST AND WORST DAYS
        val bestDay = dataPoints.maxByOrNull { it.value }
        val worstDay = dataPoints.minByOrNull { it.value }
        
        bestDay?.let { best ->
            val dayName = Calendar.getInstance().apply { timeInMillis = best.date }.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
            trendInsights.add("🏆 Best day: $dayName with ${best.value.toInt()}")
        }
        
        // 💡 GENERATE RECOMMENDATIONS
        val recommendations = mutableListOf<String>()
        
        goalValue?.let { goal ->
            val averageValue = dataPoints.map { it.value }.average().toFloat()
            when {
                averageValue < goal * 0.8f -> {
                    recommendations.add("🎯 Increase daily ${metricType.name.lowercase()} by ${(goal - averageValue).toInt()}")
                    recommendations.add("📱 Enable meal reminders to stay on track")
                }
                averageValue > goal * 1.2f -> {
                    recommendations.add("⚖️ Consider reducing ${metricType.name.lowercase()} portion sizes")
                    recommendations.add("🥗 Focus on nutrient-dense, lower-calorie foods")
                }
                else -> {
                    recommendations.add("✅ Great job staying near your goal!")
                    recommendations.add("🔄 Keep up the consistent tracking")
                }
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("📊 Continue tracking to build more insights")
            recommendations.add("🎯 Set nutrition goals for personalized advice")
        }
        
        // 📋 CREATE SUMMARY
        val summary = when (metricType) {
            TrendMetricType.CALORIES -> "📊 ${dataPoints.size}-day calorie analysis"
            TrendMetricType.PROTEIN -> "💪 ${dataPoints.size}-day protein analysis"
            TrendMetricType.CARBS -> "🍞 ${dataPoints.size}-day carbohydrate analysis"
            TrendMetricType.FAT -> "🥑 ${dataPoints.size}-day fat analysis"
            TrendMetricType.WEIGHT -> "⚖️ ${dataPoints.size}-day weight analysis"
        }
        
        return TrendInsights(
            summary = summary,
            insights = goalInsights + trendInsights,
            recommendations = recommendations
        )
    }
    
    /**
     * 📅 GENERATE WEEKLY SUMMARY DATA
     * 
     * Group daily data into weekly averages for longer-term trends.
     */
    fun generateWeeklySummary(entries: List<CalorieEntry>, metricType: TrendMetricType): List<TrendDataPoint> {
        // 🗓️ GROUP ENTRIES BY WEEK
        val weeklyGroups = entries.groupBy { entry ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = entry.timestamp
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY) // Start week on Sunday
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            calendar.timeInMillis
        }
        
        // 📊 CALCULATE WEEKLY AVERAGES
        return weeklyGroups.map { (weekStart, weekEntries) ->
            val weeklyTotal = calculateMetricValue(weekEntries, metricType)
            val dailyAverage = weeklyTotal / 7f // Convert to daily average
            
            TrendDataPoint(
                date = weekStart,
                value = dailyAverage,
                label = "Week of ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(weekStart))}"
            )
        }.sortedBy { it.date }
    }
    
    /**
     * 🎯 CALCULATE GOAL ADHERENCE TRENDS
     * 
     * Track how consistently user meets their nutrition goals over time.
     */
    fun calculateGoalAdherenceTrend(
        context: Context,
        entries: List<CalorieEntry>,
        metricType: TrendMetricType,
        goalValue: Float,
        timeFrame: TrendTimeFrame
    ): List<TrendDataPoint> {
        
        val trendData = generateTrendData(context, entries, metricType, timeFrame, goalValue)
        
        // 📊 CONVERT TO ADHERENCE PERCENTAGES
        return trendData.map { dataPoint ->
            val adherencePercentage = (dataPoint.value / goalValue * 100).coerceIn(0f, 150f)
            
            TrendDataPoint(
                date = dataPoint.date,
                value = adherencePercentage,
                label = "${adherencePercentage.toInt()}% of goal",
                color = when {
                    adherencePercentage >= 90f -> ContextCompat.getColor(context, R.color.success_green)
                    adherencePercentage >= 70f -> ContextCompat.getColor(context, R.color.warning_yellow)
                    else -> ContextCompat.getColor(context, R.color.error_red)
                }
            )
        }
    }
    
    /**
     * 🔥 CALCULATE CONSISTENCY SCORE
     * 
     * How consistent is the user's nutrition tracking and goal adherence?
     */
    fun calculateConsistencyScore(dataPoints: List<TrendDataPoint>, goalValue: Float?): Float {
        if (dataPoints.size < 3) return 0f
        
        // 📊 CALCULATE VARIATION FROM AVERAGE
        val average = dataPoints.map { it.value }.average().toFloat()
        val variations = dataPoints.map { abs(it.value - average) }
        val averageVariation = variations.average().toFloat()
        
        // 🎯 GOAL ADHERENCE CONSISTENCY
        val goalConsistency = goalValue?.let { goal ->
            val daysNearGoal = dataPoints.count { abs(it.value - goal) <= goal * 0.1f } // Within 10% of goal
            daysNearGoal.toFloat() / dataPoints.size
        } ?: 0.5f
        
        // 📈 COMBINE METRICS (lower variation = higher consistency)
        val variationScore = 1f - (averageVariation / (average + 1f)).coerceIn(0f, 1f)
        
        return ((variationScore * 0.6f) + (goalConsistency * 0.4f)) * 100f // 0-100 scale
    }
    
    /**
     * 📈 DETECT TREND PATTERNS
     * 
     * Find meaningful patterns in the data trends.
     */
    fun detectTrendPatterns(dataPoints: List<TrendDataPoint>): List<String> {
        if (dataPoints.size < 5) return listOf("Need more data for pattern detection")
        
        val patterns = mutableListOf<String>()
        
        // 📊 WEEKEND VS WEEKDAY ANALYSIS
        val weekdayValues = mutableListOf<Float>()
        val weekendValues = mutableListOf<Float>()
        
        dataPoints.forEach { point ->
            val calendar = Calendar.getInstance().apply { timeInMillis = point.date }
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                weekendValues.add(point.value)
            } else {
                weekdayValues.add(point.value)
            }
        }
        
        if (weekdayValues.isNotEmpty() && weekendValues.isNotEmpty()) {
            val weekdayAvg = weekdayValues.average().toFloat()
            val weekendAvg = weekendValues.average().toFloat()
            val difference = abs(weekendAvg - weekdayAvg)
            
            if (difference > weekdayAvg * 0.15f) { // 15% or more difference
                patterns.add(
                    if (weekendAvg > weekdayAvg) {
                        "🎉 Weekend values are ${((weekendAvg / weekdayAvg - 1) * 100).toInt()}% higher than weekdays"
                    } else {
                        "📉 Weekend values are ${((1 - weekendAvg / weekdayAvg) * 100).toInt()}% lower than weekdays"
                    }
                )
            }
        }
        
        // 📈 TREND MOMENTUM ANALYSIS
        val recentValues = dataPoints.takeLast(3).map { it.value }
        val olderValues = dataPoints.take(3).map { it.value }
        
        if (recentValues.isNotEmpty() && olderValues.isNotEmpty()) {
            val recentAvg = recentValues.average().toFloat()
            val olderAvg = olderValues.average().toFloat()
            val momentum = recentAvg - olderAvg
            
            if (abs(momentum) > olderAvg * 0.1f) { // 10% or more change
                patterns.add(
                    if (momentum > 0) {
                        "🚀 Recent upward momentum detected"
                    } else {
                        "📉 Recent downward trend observed"
                    }
                )
            }
        }
        
        return patterns.ifEmpty { listOf("📊 Steady, consistent tracking pattern") }
    }
}

/**
 * 💡 TREND INSIGHTS
 * 
 * Container for analysis results and recommendations.
 */
data class TrendInsights(
    val summary: String,              // 📋 "7-day calorie analysis"
    val insights: List<String>,       // 📊 Key findings from the data
    val recommendations: List<String> // 💡 Actionable suggestions for improvement
)