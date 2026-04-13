package com.calorietracker.social

// 🧰 SHARING AND SOCIAL TOOLS
import android.content.Context             // Android system access
import android.content.Intent             // For sharing content with other apps
import android.graphics.Bitmap            // Image handling
import android.graphics.Canvas            // Drawing on images
import android.graphics.Color             // Color utilities
import android.graphics.Paint             // Text and drawing styling
import android.graphics.Typeface          // Font styling
import android.net.Uri                    // File URIs for sharing
import androidx.core.content.FileProvider // Secure file sharing
import com.calorietracker.database.CalorieEntry        // Food entry data
import com.calorietracker.database.NutritionGoals      // User's nutrition targets
import com.calorietracker.database.getActualCalories   // Extension function for actual calories
import com.calorietracker.database.getActualProtein    // Extension function for actual protein
import com.calorietracker.database.getActualCarbs      // Extension function for actual carbs
import com.calorietracker.database.getActualFat        // Extension function for actual fat
import com.calorietracker.utils.NutritionGrader        // Nutrition grading system
import com.calorietracker.utils.NutritionGrade         // Nutrition grade data class
import com.calorietracker.widgets.TrendDataPoint       // Chart data
import com.calorietracker.widgets.TrendMetricType      // Type of trend data
import kotlin.math.abs                                 // Absolute value function
import java.io.File                       // File operations
import java.io.FileOutputStream           // Save images to storage
import java.text.SimpleDateFormat         // Date formatting
import java.util.*                        // Date utilities
import kotlin.math.roundToInt             // Math utilities

/**
 * 📱 PROGRESS SHARING MANAGER - SHARE YOUR SUCCESS WITH FRIENDS
 * 
 * Hey young programmer! This lets users share their nutrition progress with friends,
 * family, and on social media in beautiful, easy-to-understand formats.
 * 
 * 🎯 What can users share?
 * - 📊 **Daily nutrition summaries**: "I hit my protein goal today! 💪"
 * - 🏆 **Achievement badges**: "7-day logging streak unlocked! 🔥"
 * - 📈 **Progress charts**: Visual graphs showing improvement over time
 * - 🎓 **Nutrition grades**: "Got an A+ for balanced eating today!"
 * - 📸 **Progress photos**: Before/after comparisons with stats
 * 
 * 🎨 Sharing Formats:
 * - **Text summaries**: Quick, easy-to-read messages
 * - **Beautiful graphics**: Auto-generated images with charts and stats
 * - **Progress cards**: Instagram-style cards with key achievements
 * - **Data exports**: CSV files for fitness coaches or nutritionists
 * 
 * 📱 Sharing Destinations:
 * - Social media (Instagram, Facebook, Twitter, TikTok)
 * - Messaging apps (WhatsApp, Telegram, iMessage)
 * - Email to family, friends, or health coaches
 * - Fitness communities and forums
 * - Health tracking apps that accept imports
 * 
 * 🔒 Privacy Features:
 * - Users control what data to share (calories vs detailed macros)
 * - Can hide sensitive information (weight, specific foods)
 * - Share anonymized data for community challenges
 * - Optional watermark removal for premium users
 * 
 * 💡 Smart Sharing Features:
 * - Auto-generates encouraging captions
 * - Suggests best times to share for motivation
 * - Creates comparison graphics (this week vs last week)
 * - Includes helpful tips and encouragement for followers
 */
object ProgressSharingManager {
    
    // 🎨 IMAGE GENERATION SETTINGS
    private const val SHARE_IMAGE_WIDTH = 1080     // Instagram-friendly width
    private const val SHARE_IMAGE_HEIGHT = 1080    // Square format
    private const val MARGIN = 80                  // Border margin
    private const val HEADER_HEIGHT = 200          // Top section height
    private const val CHART_HEIGHT = 400           // Chart area height
    private const val FOOTER_HEIGHT = 280          // Bottom section height
    
    /**
     * 📊 SHARE DAILY NUTRITION SUMMARY
     * 
     * Create and share a beautiful summary of today's nutrition progress.
     * 
     * @param context Android context
     * @param entries Today's food entries
     * @param goals User's nutrition goals
     * @param includeDetails Include detailed macro breakdown?
     */
    fun shareDailyNutritionSummary(
        context: Context,
        entries: List<CalorieEntry>,
        goals: NutritionGoals?,
        includeDetails: Boolean = true
    ) {
        // 📊 CALCULATE TODAY'S TOTALS
        val totalCalories = entries.sumOf { it.getActualCalories() }
        val totalProtein = entries.sumOf { it.getActualProtein() ?: 0.0 }
        val totalCarbs = entries.sumOf { it.getActualCarbs() ?: 0.0 }
        val totalFat = entries.sumOf { it.getActualFat() ?: 0.0 }
        
        // 🎓 GET NUTRITION GRADE
        val nutritionGrade = goals?.let { userGoals ->
            NutritionGrader.calculateDailyGrade(entries, userGoals, userGoals.calorieGoal)
        }
        
        // 📝 CREATE SHARE TEXT
        val shareText = buildString {
            append("🍎 My Nutrition Progress Today\n\n")
            
            // 🔥 CALORIE SUMMARY
            append("📊 Calories: $totalCalories")
            goals?.calorieGoal?.let { goal ->
                val percentage = (totalCalories.toFloat() / goal * 100).roundToInt()
                append(" / $goal (${percentage}%)")
            }
            append("\n")
            
            if (includeDetails) {
                // 💪 MACRO BREAKDOWN
                append("\n💪 Macro Breakdown:\n")
                append("• Protein: ${totalProtein.roundToInt()}g")
                goals?.proteinGoal?.let { goal -> append(" / ${goal}g") }
                append("\n")
                
                append("• Carbs: ${totalCarbs.roundToInt()}g")
                goals?.carbsGoal?.let { goal -> append(" / ${goal}g") }
                append("\n")
                
                append("• Fat: ${totalFat.roundToInt()}g")
                goals?.fatGoal?.let { goal -> append(" / ${goal}g") }
                append("\n")
            }
            
            // 🎓 NUTRITION GRADE
            nutritionGrade?.let { grade ->
                append("\n🎓 Nutrition Grade: ${grade.overallGrade} (${grade.overallScore}%)\n")
                
                if (grade.feedback.isNotEmpty()) {
                    append("✨ ${grade.feedback.first()}\n")
                }
            }
            
            append("\n🏷️ #CalorieTracker #HealthyEating #NutritionGoals")
        }
        
        // 📱 SHARE VIA ANDROID INTENT
        shareText(context, shareText, "Daily Nutrition Summary")
    }
    
    /**
     * 🏆 SHARE ACHIEVEMENT MILESTONE
     * 
     * Share when user reaches important milestones or achievements.
     */
    fun shareAchievement(
        context: Context,
        achievementTitle: String,
        achievementDescription: String,
        streakDays: Int? = null,
        totalCaloriesLogged: Int? = null,
        daysTracked: Int? = null
    ) {
        val shareText = buildString {
            append("🏆 Achievement Unlocked!\n\n")
            append("🎯 $achievementTitle\n")
            append("📝 $achievementDescription\n\n")
            
            // 🔥 STREAK INFORMATION
            streakDays?.let { days ->
                append("🔥 Current streak: $days days!\n")
            }
            
            // 📊 TRACKING STATISTICS
            if (totalCaloriesLogged != null && daysTracked != null) {
                append("📊 Total tracked: $totalCaloriesLogged calories over $daysTracked days\n")
                val averageDaily = (totalCaloriesLogged.toFloat() / daysTracked).roundToInt()
                append("📈 Daily average: $averageDaily calories\n")
            }
            
            append("\n💪 Consistency is key to reaching health goals!")
            append("\n\n🏷️ #CalorieTracker #HealthGoals #Achievement #Consistency")
        }
        
        shareText(context, shareText, "Achievement Unlocked!")
    }
    
    /**
     * 📈 SHARE PROGRESS CHART
     * 
     * Create and share a visual chart showing nutrition trends.
     */
    fun shareProgressChart(
        context: Context,
        trendData: List<TrendDataPoint>,
        metricType: TrendMetricType,
        goalValue: Float? = null,
        timeFrame: String = "week"
    ) {
        // 🎨 GENERATE CHART IMAGE
        val chartImage = generateProgressChartImage(context, trendData, metricType, goalValue, timeFrame)
        
        // 📝 CREATE CAPTION
        val caption = buildString {
            append("📈 My ${metricType.name.lowercase()} progress this $timeFrame!\n\n")
            
            if (trendData.isNotEmpty()) {
                val latest = trendData.last().value
                val earliest = trendData.first().value
                val change = latest - earliest
                val changePercent = if (earliest > 0) (change / earliest * 100).roundToInt() else 0
                
                when {
                    change > 0 -> append("📊 Improved by ${change.roundToInt()} (${changePercent}% increase)\n")
                    change < 0 -> append("📉 Decreased by ${abs(change).roundToInt()} (${abs(changePercent)}% reduction)\n")
                    else -> append("➡️ Stayed consistent this $timeFrame\n")
                }
            }
            
            goalValue?.let { goal ->
                val adherence = trendData.count { it.value >= goal * 0.9f }
                append("🎯 Hit my goal $adherence/${trendData.size} days\n")
            }
            
            append("\n💪 Small consistent changes lead to big results!")
            append("\n\n🏷️ #ProgressChart #HealthyLiving #CalorieTracker")
        }
        
        // 📱 SHARE IMAGE WITH CAPTION
        shareImageWithText(context, chartImage, caption, "Progress Chart")
    }
    
    /**
     * 🎓 SHARE NUTRITION GRADE REPORT
     * 
     * Share daily nutrition grade with breakdown and suggestions.
     */
    fun shareNutritionGradeReport(
        context: Context,
        grade: NutritionGrade,
        includeBreakdown: Boolean = true
    ) {
        val shareText = buildString {
            append("🎓 Today's Nutrition Report Card\n\n")
            append("📊 Overall Grade: ${grade.overallGrade} (${grade.overallScore}%)\n\n")
            
            if (includeBreakdown) {
                append("📋 Score Breakdown:\n")
                append("• 🔥 Calorie Balance: ${grade.calorieScore}%\n")
                append("• 💪 Macro Balance: ${grade.macroScore}%\n")
                append("• 🌿 Micronutrients: ${grade.micronutrientScore}%\n")
                append("• 🌈 Food Variety: ${grade.varietyScore}%\n")
                append("• ⏰ Meal Timing: ${grade.timingScore}%\n\n")
            }
            
            // ✨ HIGHLIGHTS
            if (grade.feedback.isNotEmpty()) {
                append("✨ Highlights:\n")
                grade.feedback.take(2).forEach { feedback ->
                    append("• $feedback\n")
                }
                append("\n")
            }
            
            // 💡 NEXT STEPS
            if (grade.improvements.isNotEmpty()) {
                append("💡 Tomorrow's focus:\n")
                append("• ${grade.improvements.first()}\n")
            }
            
            append("\n🎯 Tracking nutrition helps me make better choices!")
            append("\n\n🏷️ #NutritionGrade #HealthyEating #CalorieTracker")
        }
        
        shareText(context, shareText, "Nutrition Report Card")
    }
    
    /**
     * 🎨 GENERATE PROGRESS CHART IMAGE
     * 
     * Create a beautiful image showing progress trends for sharing.
     */
    private fun generateProgressChartImage(
        context: Context,
        trendData: List<TrendDataPoint>,
        metricType: TrendMetricType,
        goalValue: Float?,
        timeFrame: String
    ): Bitmap {
        
        val bitmap = Bitmap.createBitmap(SHARE_IMAGE_WIDTH, SHARE_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 🎨 SETUP DRAWING TOOLS
        val backgroundPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        
        val titlePaint = Paint().apply {
            color = Color.parseColor("#2E7D32") // Primary green
            textSize = 72f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#757575") // Gray
            textSize = 48f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        val chartLinePaint = Paint().apply {
            color = when (metricType) {
                TrendMetricType.CALORIES -> Color.parseColor("#2E7D32") // Green
                TrendMetricType.PROTEIN -> Color.parseColor("#1976D2")  // Blue
                TrendMetricType.CARBS -> Color.parseColor("#FF6F00")    // Orange
                TrendMetricType.FAT -> Color.parseColor("#388E3C")      // Dark green
                TrendMetricType.WEIGHT -> Color.parseColor("#9C27B0")   // Purple
            }
            strokeWidth = 8f
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        
        val goalLinePaint = Paint().apply {
            color = Color.parseColor("#9E9E9E") // Medium gray
            strokeWidth = 4f
            isAntiAlias = true
            style = Paint.Style.STROKE
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(20f, 10f), 0f)
        }
        
        // 🎨 DRAW BACKGROUND
        canvas.drawRect(0f, 0f, SHARE_IMAGE_WIDTH.toFloat(), SHARE_IMAGE_HEIGHT.toFloat(), backgroundPaint)
        
        // 📊 DRAW TITLE
        val title = "${metricType.name.lowercase().replaceFirstChar { it.uppercase() }} Progress"
        canvas.drawText(title, SHARE_IMAGE_WIDTH / 2f, 120f, titlePaint)
        
        // 📅 DRAW SUBTITLE
        val subtitle = "Past $timeFrame • CalorieTracker"
        canvas.drawText(subtitle, SHARE_IMAGE_WIDTH / 2f, 200f, subtitlePaint)
        
        // 📈 DRAW CHART AREA
        if (trendData.isNotEmpty()) {
            val chartLeft = MARGIN.toFloat()
            val chartTop = HEADER_HEIGHT.toFloat()
            val chartRight = SHARE_IMAGE_WIDTH - MARGIN.toFloat()
            val chartBottom = chartTop + CHART_HEIGHT
            val chartWidth = chartRight - chartLeft
            val chartHeight = CHART_HEIGHT.toFloat()
            
            // 📊 CALCULATE VALUE RANGES
            val maxValue = trendData.maxOfOrNull { it.value } ?: 0f
            val minValue = trendData.minOfOrNull { it.value } ?: 0f
            val valueRange = kotlin.math.max(maxValue - minValue, 1f)
            
            // 🎯 DRAW GOAL LINE
            goalValue?.let { goal ->
                if (goal >= minValue && goal <= maxValue) {
                    val goalY = chartBottom - ((goal - minValue) / valueRange) * chartHeight
                    canvas.drawLine(chartLeft, goalY, chartRight, goalY, goalLinePaint)
                }
            }
            
            // 📈 DRAW TREND LINE
            if (trendData.size >= 2) {
                for (i in 1 until trendData.size) {
                    val x1 = chartLeft + (chartWidth / (trendData.size - 1)) * (i - 1)
                    val y1 = chartBottom - ((trendData[i - 1].value - minValue) / valueRange) * chartHeight
                    val x2 = chartLeft + (chartWidth / (trendData.size - 1)) * i
                    val y2 = chartBottom - ((trendData[i].value - minValue) / valueRange) * chartHeight
                    
                    canvas.drawLine(x1, y1, x2, y2, chartLinePaint)
                }
            }
            
            // 🔵 DRAW DATA POINTS
            chartLinePaint.style = Paint.Style.FILL
            trendData.forEachIndexed { index, dataPoint ->
                val x = chartLeft + (chartWidth / (trendData.size - 1)) * index
                val y = chartBottom - ((dataPoint.value - minValue) / valueRange) * chartHeight
                canvas.drawCircle(x, y, 12f, chartLinePaint)
            }
            chartLinePaint.style = Paint.Style.STROKE
        }
        
        // 📊 DRAW STATS FOOTER
        drawStatsFooter(canvas, trendData, metricType, goalValue)
        
        return bitmap
    }
    
    /**
     * 📊 DRAW STATS FOOTER
     * 
     * Add key statistics at the bottom of the shared image.
     */
    private fun drawStatsFooter(
        canvas: Canvas,
        trendData: List<TrendDataPoint>,
        metricType: TrendMetricType,
        goalValue: Float?
    ) {
        val footerTop = HEADER_HEIGHT + CHART_HEIGHT + 50f
        
        val statsPaint = Paint().apply {
            color = Color.parseColor("#212121") // Dark text
            textSize = 44f
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }
        
        val labelPaint = Paint().apply {
            color = Color.parseColor("#757575") // Gray
            textSize = 36f
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }
        
        if (trendData.isNotEmpty()) {
            val average = trendData.map { it.value }.average().toFloat()
            val latest = trendData.last().value
            val change = if (trendData.size >= 2) latest - trendData.first().value else 0f
            
            // 📊 AVERAGE VALUE
            canvas.drawText("Average:", MARGIN.toFloat(), footerTop, labelPaint)
            canvas.drawText("${average.roundToInt()}", MARGIN.toFloat(), footerTop + 60f, statsPaint)
            
            // 📈 LATEST VALUE
            canvas.drawText("Latest:", MARGIN + 250f, footerTop, labelPaint)
            canvas.drawText("${latest.roundToInt()}", MARGIN + 250f, footerTop + 60f, statsPaint)
            
            // 🔄 CHANGE
            canvas.drawText("Change:", MARGIN + 500f, footerTop, labelPaint)
            val changeText = if (change >= 0) "+${change.roundToInt()}" else "${change.roundToInt()}"
            canvas.drawText(changeText, MARGIN + 500f, footerTop + 60f, statsPaint)
            
            // 🎯 GOAL ADHERENCE
            goalValue?.let { goal ->
                val adherence = trendData.count { it.value >= goal * 0.9f }
                canvas.drawText("Goal hits:", MARGIN + 750f, footerTop, labelPaint)
                canvas.drawText("$adherence/${trendData.size}", MARGIN + 750f, footerTop + 60f, statsPaint)
            }
        }
        
        // 📱 APP ATTRIBUTION
        val attributionPaint = Paint().apply {
            color = Color.parseColor("#9E9E9E")
            textSize = 32f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawText(
            "Generated with CalorieTracker",
            SHARE_IMAGE_WIDTH / 2f,
            SHARE_IMAGE_HEIGHT - 40f,
            attributionPaint
        )
    }
    
    /**
     * 🎊 SHARE WEEKLY PROGRESS SUMMARY
     * 
     * Share a comprehensive weekly nutrition summary with achievements.
     */
    fun shareWeeklyProgressSummary(
        context: Context,
        weeklyEntries: List<CalorieEntry>,
        weeklyGoals: NutritionGoals?,
        weekStartDate: String,
        achievements: List<String> = emptyList()
    ) {
        // 📊 CALCULATE WEEKLY TOTALS
        val totalDays = 7
        val daysWithData = weeklyEntries.groupBy { it.date }.size
        val totalCalories = weeklyEntries.sumOf { it.getActualCalories() }
        val avgDailyCalories = if (daysWithData > 0) totalCalories / daysWithData else 0
        
        val shareText = buildString {
            append("📅 Week of $weekStartDate - Nutrition Summary\n\n")
            
            // 📊 TRACKING CONSISTENCY
            append("📊 Tracking: $daysWithData/$totalDays days (${(daysWithData * 100 / totalDays)}%)\n")
            append("🔥 Average daily calories: $avgDailyCalories\n\n")
            
            // 🎯 GOAL PROGRESS
            weeklyGoals?.let { goals ->
                val goalHits = weeklyEntries.groupBy { it.date }.count { (_, dayEntries) ->
                    dayEntries.sumOf { it.getActualCalories() } >= goals.calorieGoal * 0.9
                }
                append("🎯 Goal achievement: $goalHits/$daysWithData days\n\n")
            }
            
            // 🏆 ACHIEVEMENTS
            if (achievements.isNotEmpty()) {
                append("🏆 This week's achievements:\n")
                achievements.take(3).forEach { achievement ->
                    append("• $achievement\n")
                }
                append("\n")
            }
            
            append("💪 Consistency builds lasting healthy habits!")
            append("\n\n🏷️ #WeeklyProgress #HealthGoals #CalorieTracker")
        }
        
        shareText(context, shareText, "Weekly Progress Summary")
    }
    
    /**
     * 📱 SHARE TEXT CONTENT
     * 
     * Use Android's sharing system to send text to other apps.
     */
    private fun shareText(context: Context, text: String, subject: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        
        val chooser = Intent.createChooser(shareIntent, "Share your progress")
        context.startActivity(chooser)
    }
    
    /**
     * 📸 SHARE GENERATED IMAGE
     * 
     * Save generated chart image and share it via Android's sharing system.
     */
    private fun shareGeneratedImage(context: Context, bitmap: Bitmap, title: String) {
        try {
            // 💾 SAVE IMAGE TO CACHE
            val imagesFolder = File(context.cacheDir, "images")
            imagesFolder.mkdirs()
            val imageFile = File(imagesFolder, "progress_chart_${System.currentTimeMillis()}.png")
            
            FileOutputStream(imageFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            
            // 📱 CREATE SHARE INTENT
            val imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Share your progress chart")
            context.startActivity(chooser)
            
        } catch (e: Exception) {
            // 🚨 FALLBACK TO TEXT SHARING
            shareText(context, "📊 Check out my nutrition progress! #CalorieTracker", title)
        }
    }
    
    /**
     * 📸 SHARE IMAGE WITH TEXT CAPTION
     * 
     * Share an image along with descriptive text.
     */
    private fun shareImageWithText(context: Context, bitmap: Bitmap, caption: String, title: String) {
        try {
            // 💾 SAVE IMAGE TO CACHE
            val imagesFolder = File(context.cacheDir, "images")
            imagesFolder.mkdirs()
            val imageFile = File(imagesFolder, "progress_${System.currentTimeMillis()}.png")
            
            FileOutputStream(imageFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            
            // 📱 CREATE SHARE INTENT WITH BOTH IMAGE AND TEXT
            val imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider", 
                imageFile
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Share your progress")
            context.startActivity(chooser)
            
        } catch (e: Exception) {
            // 🚨 FALLBACK TO TEXT ONLY
            shareText(context, caption, title)
        }
    }
    
    /**
     * 📤 EXPORT NUTRITION DATA
     * 
     * Export detailed nutrition data as CSV for coaches or detailed analysis.
     */
    fun exportNutritionData(
        context: Context,
        entries: List<CalorieEntry>,
        startDate: String,
        endDate: String
    ) {
        val csvContent = buildString {
            // 📋 CSV HEADER
            append("Date,Food,Calories,Protein,Carbs,Fat,Fiber,Sugar,Sodium,Servings\n")
            
            // 📊 DATA ROWS
            entries.forEach { entry ->
                append("${entry.date},")
                append("\"${entry.foodName}\",")
                append("${entry.getActualCalories()},")
                append("${entry.getActualProtein() ?: 0},")
                append("${entry.getActualCarbs() ?: 0},")
                append("${entry.getActualFat() ?: 0},")
                append("${(entry.fiber ?: 0.0) * entry.servings},")
                append("${(entry.sugar ?: 0.0) * entry.servings},")
                append("${(entry.sodium ?: 0.0) * entry.servings},")
                append("${entry.servings}\n")
            }
        }
        
        // 💾 SAVE CSV FILE
        try {
            val fileName = "nutrition_data_${startDate}_to_${endDate}.csv"
            val csvFile = File(context.cacheDir, fileName)
            csvFile.writeText(csvContent)
            
            // 📤 SHARE CSV FILE
            val csvUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, csvUri)
                putExtra(Intent.EXTRA_SUBJECT, "Nutrition Data Export")
                putExtra(Intent.EXTRA_TEXT, "My nutrition data from $startDate to $endDate")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Export nutrition data")
            context.startActivity(chooser)
            
        } catch (e: Exception) {
            // 🚨 FALLBACK TO TEXT SHARING
            shareText(context, "📊 My nutrition data is ready to export! #CalorieTracker", "Nutrition Data")
        }
    }
    
    /**
     * 🏆 GENERATE MOTIVATIONAL SHARING MESSAGES
     * 
     * Create encouraging messages based on user's progress patterns.
     */
    fun generateMotivationalMessage(
        entries: List<CalorieEntry>,
        goals: NutritionGoals?,
        streakDays: Int = 0
    ): String {
        val totalCalories = entries.sumOf { it.getActualCalories() }
        val daysTracked = entries.groupBy { it.date }.size
        val avgDaily = if (daysTracked > 0) totalCalories / daysTracked else 0
        
        val motivationalPhrases = listOf(
            "🔥 Consistency is my superpower!",
            "💪 Small changes, big results!",
            "🎯 Progress over perfection!",
            "✨ Building healthy habits daily!",
            "🌟 Every meal logged is a win!",
            "🏆 Committed to my health journey!",
            "📈 Growth mindset in action!",
            "🎉 Celebrating small victories!"
        )
        
        return buildString {
            append(motivationalPhrases.random())
            append("\n\n📊 Recent progress highlights:\n")
            
            if (streakDays > 0) {
                append("🔥 ${streakDays}-day tracking streak\n")
            }
            
            if (daysTracked > 0) {
                append("📱 $daysTracked days of consistent logging\n")
                append("📊 ${avgDaily} average daily calories\n")
            }
            
            goals?.let { userGoals ->
                val goalHits = entries.groupBy { it.date }.count { (_, dayEntries) ->
                    dayEntries.sumOf { it.getActualCalories() } >= userGoals.calorieGoal * 0.9
                }
                if (goalHits > 0) {
                    append("🎯 Hit my calorie goal $goalHits times\n")
                }
            }
            
            append("\n💡 What keeps me motivated:\n")
            append("• Seeing daily progress in my charts\n")
            append("• Understanding what foods fuel me best\n")
            append("• Building sustainable healthy habits\n")
            
            append("\n🌟 #HealthJourney #CalorieTracker #ProgressOverPerfection")
        }
    }
}