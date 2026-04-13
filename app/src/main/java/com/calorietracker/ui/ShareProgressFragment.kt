package com.calorietracker.ui

// 🧰 ANDROID UI TOOLS
import android.os.Bundle                    // Save/restore state
import android.view.LayoutInflater         // Create views from XML
import android.view.View                   // Base view class
import android.view.ViewGroup              // Container for views
import android.widget.*                    // UI components
import androidx.fragment.app.Fragment      // Fragment base class
import androidx.lifecycle.lifecycleScope   // Coroutine management
import com.calorietracker.R               // App resources
import com.calorietracker.repository.CalorieRepository  // Data access
import com.calorietracker.database.CalorieDatabase      // Database access
import com.calorietracker.database.CalorieEntry         // Food entry data
import com.calorietracker.database.NutritionGoals       // Nutrition goals
import com.calorietracker.database.getActualCalories    // Extension function for actual calories
import com.calorietracker.database.getActualProtein     // Extension function for actual protein
import com.calorietracker.database.getActualCarbs       // Extension function for actual carbs
import com.calorietracker.database.getActualFat         // Extension function for actual fat
import kotlin.math.roundToInt                           // Math utilities
import com.calorietracker.social.ProgressSharingManager  // Sharing functionality
import com.calorietracker.utils.TrendAnalyzer           // Trend analysis
import com.calorietracker.utils.NutritionGrader         // Nutrition grading
import com.calorietracker.widgets.TrendMetricType       // Metric types
import com.calorietracker.widgets.TrendTimeFrame        // Time frames
import kotlinx.coroutines.launch          // Async operations
import java.text.SimpleDateFormat         // Date formatting
import java.util.*                        // Date utilities
import java.util.Calendar                 // Calendar utilities

/**
 * 📱 SHARE PROGRESS FRAGMENT - SOCIAL SHARING HUB
 * 
 * Hey young programmer! This screen lets users share their nutrition progress
 * in beautiful, motivational formats with friends and family.
 * 
 * 🎯 What can users share from here?
 * - 📊 **Today's nutrition summary**: Quick daily progress with grade
 * - 📈 **Weekly progress charts**: Visual trends showing improvement
 * - 🏆 **Achievement milestones**: Celebrate streaks and goals
 * - 📋 **Detailed data exports**: CSV files for coaches
 * - 🎓 **Nutrition report cards**: A/B/C/D/F grades with feedback
 * 
 * 🎨 Sharing Options:
 * - **Quick share**: One-tap sharing with smart captions
 * - **Custom messages**: Edit text before sharing
 * - **Visual charts**: Auto-generated progress graphics
 * - **Privacy controls**: Choose what details to include/hide
 * - **Platform optimization**: Formats optimized for Instagram, Facebook, etc.
 * 
 * 💡 Smart Features:
 * - Auto-generates encouraging captions based on progress
 * - Suggests best sharing content based on achievements
 * - Provides privacy toggles for sensitive information
 * - Creates motivational graphics with charts and stats
 * 
 * 📱 Social Integration:
 * - Direct sharing to Instagram Stories, Facebook, Twitter
 * - WhatsApp/iMessage for family and close friends
 * - Email export for healthcare providers
 * - Community forum integration for group challenges
 */
class ShareProgressFragment : Fragment() {
    
    // 🗄️ DATA MANAGEMENT
    private lateinit var repository: CalorieRepository
    
    // 📱 UI COMPONENTS
    private lateinit var shareDailySummaryButton: Button
    private lateinit var shareWeeklyChartButton: Button
    private lateinit var shareAchievementButton: Button
    private lateinit var shareNutritionGradeButton: Button
    private lateinit var exportDataButton: Button
    private lateinit var customMessageButton: Button
    
    private lateinit var privacySettingsGroup: LinearLayout
    private lateinit var includeCaloriesCheckbox: CheckBox
    private lateinit var includeMacrosCheckbox: CheckBox
    private lateinit var includeWeightCheckbox: CheckBox
    private lateinit var includeGoalsCheckbox: CheckBox
    
    private lateinit var previewTextView: TextView
    private lateinit var motivationalQuoteText: TextView
    
    /**
     * 🏗️ CREATE FRAGMENT VIEW
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_share_progress, container, false)
    }
    
    /**
     * ⚙️ SET UP FRAGMENT
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 🗄️ INITIALIZE DATA REPOSITORY
        repository = CalorieRepository(CalorieDatabase.getDatabase(requireContext()), requireContext())
        
        // 🔗 CONNECT UI COMPONENTS
        setupUIComponents(view)
        setupClickListeners()
        
        // 💡 GENERATE MOTIVATIONAL CONTENT
        generateMotivationalContent()
        
        // 📊 LOAD PREVIEW
        updatePreview()
    }
    
    /**
     * 🔗 SETUP UI COMPONENTS
     */
    private fun setupUIComponents(view: View) {
        // 📱 SHARING BUTTONS
        shareDailySummaryButton = view.findViewById(R.id.shareDailySummaryButton)
        shareWeeklyChartButton = view.findViewById(R.id.shareWeeklyChartButton)
        shareAchievementButton = view.findViewById(R.id.shareAchievementButton)
        shareNutritionGradeButton = view.findViewById(R.id.shareNutritionGradeButton)
        exportDataButton = view.findViewById(R.id.exportDataButton)
        customMessageButton = view.findViewById(R.id.customMessageButton)
        
        // 🔒 PRIVACY CONTROLS
        privacySettingsGroup = view.findViewById(R.id.privacySettingsGroup)
        includeCaloriesCheckbox = view.findViewById(R.id.includeCaloriesCheckbox)
        includeMacrosCheckbox = view.findViewById(R.id.includeMacrosCheckbox)
        includeWeightCheckbox = view.findViewById(R.id.includeWeightCheckbox)
        includeGoalsCheckbox = view.findViewById(R.id.includeGoalsCheckbox)
        
        // 📊 PREVIEW AND MOTIVATION
        previewTextView = view.findViewById(R.id.previewTextView)
        motivationalQuoteText = view.findViewById(R.id.motivationalQuoteText)
        
        // 🔒 SET DEFAULT PRIVACY SETTINGS
        includeCaloriesCheckbox.isChecked = true
        includeMacrosCheckbox.isChecked = false
        includeWeightCheckbox.isChecked = false
        includeGoalsCheckbox.isChecked = true
    }
    
    /**
     * 👆 SETUP CLICK LISTENERS
     */
    private fun setupClickListeners() {
        
        // 📊 DAILY SUMMARY SHARING
        shareDailySummaryButton.setOnClickListener {
            shareDailySummary()
        }
        
        // 📈 WEEKLY CHART SHARING
        shareWeeklyChartButton.setOnClickListener {
            shareWeeklyChart()
        }
        
        // 🏆 ACHIEVEMENT SHARING
        shareAchievementButton.setOnClickListener {
            shareRecentAchievement()
        }
        
        // 🎓 NUTRITION GRADE SHARING
        shareNutritionGradeButton.setOnClickListener {
            shareNutritionGrade()
        }
        
        // 📤 DATA EXPORT
        exportDataButton.setOnClickListener {
            exportDetailedData()
        }
        
        // ✏️ CUSTOM MESSAGE
        customMessageButton.setOnClickListener {
            createCustomMessage()
        }
        
        // 🔒 PRIVACY SETTING CHANGES
        val privacyChangeListener = { _: Boolean ->
            updatePreview()
        }
        
        includeCaloriesCheckbox.setOnCheckedChangeListener { _, isChecked -> privacyChangeListener(isChecked) }
        includeMacrosCheckbox.setOnCheckedChangeListener { _, isChecked -> privacyChangeListener(isChecked) }
        includeWeightCheckbox.setOnCheckedChangeListener { _, isChecked -> privacyChangeListener(isChecked) }
        includeGoalsCheckbox.setOnCheckedChangeListener { _, isChecked -> privacyChangeListener(isChecked) }
    }
    
    /**
     * 📊 SHARE DAILY SUMMARY
     */
    private fun shareDailySummary() {
        lifecycleScope.launch {
            try {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.timeInMillis
                val todayEntries = repository.getEntriesInDateRange(startOfDay, endOfDay)
                val goals = repository.getNutritionGoalsSync()
                
                ProgressSharingManager.shareDailyNutritionSummary(
                    context = requireContext(),
                    entries = todayEntries,
                    goals = goals,
                    includeDetails = includeMacrosCheckbox.isChecked
                )
                
            } catch (e: Exception) {
                Toast.makeText(context, "📊 Error sharing daily summary: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 📈 SHARE WEEKLY CHART
     */
    private fun shareWeeklyChart() {
        lifecycleScope.launch {
            try {
                // 📅 GET PAST WEEK'S DATA
                val calendar = Calendar.getInstance()
                val endDate = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, -7)
                val startDate = calendar.timeInMillis
                
                val weeklyEntries = repository.getEntriesInDateRange(startDate, endDate)
                val goals = repository.getNutritionGoalsSync()
                val goalValue = goals?.calorieGoal?.toFloat()
                
                // 📊 GENERATE TREND DATA
                val trendData = TrendAnalyzer.generateTrendData(
                    context = requireContext(),
                    entries = weeklyEntries,
                    metricType = TrendMetricType.CALORIES,
                    timeFrame = TrendTimeFrame.WEEKLY,
                    goalValue = goalValue
                )
                
                ProgressSharingManager.shareProgressChart(
                    context = requireContext(),
                    trendData = trendData,
                    metricType = TrendMetricType.CALORIES,
                    goalValue = goalValue,
                    timeFrame = "week"
                )
                
            } catch (e: Exception) {
                Toast.makeText(context, "📈 Error sharing weekly chart: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 🏆 SHARE RECENT ACHIEVEMENT
     */
    private fun shareRecentAchievement() {
        lifecycleScope.launch {
            try {
                // 🔥 CALCULATE CURRENT STREAK (simplified)
                val recentEntries = repository.getEntriesInDateRange(
                    System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L), // 30 days ago
                    System.currentTimeMillis()
                )
                
                val daysWithEntries = recentEntries.groupBy { it.date }.size
                val totalCalories = recentEntries.sumOf { it.getActualCalories() }
                
                ProgressSharingManager.shareAchievement(
                    context = requireContext(),
                    achievementTitle = "🔥 Nutrition Tracking Champion",
                    achievementDescription = "Consistently tracking my nutrition for better health!",
                    streakDays = daysWithEntries,
                    totalCaloriesLogged = totalCalories,
                    daysTracked = daysWithEntries
                )
                
            } catch (e: Exception) {
                Toast.makeText(context, "🏆 Error sharing achievement: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 🎓 SHARE NUTRITION GRADE
     */
    private fun shareNutritionGrade() {
        lifecycleScope.launch {
            try {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.timeInMillis
                val todayEntries = repository.getEntriesInDateRange(startOfDay, endOfDay)
                val goals = repository.getNutritionGoalsSync()
                
                if (goals != null) {
                    val grade = NutritionGrader.calculateDailyGrade(todayEntries, goals, goals.calorieGoal)
                    
                    ProgressSharingManager.shareNutritionGradeReport(
                        context = requireContext(),
                        grade = grade,
                        includeBreakdown = includeMacrosCheckbox.isChecked
                    )
                } else {
                    Toast.makeText(context, "🎯 Set up nutrition goals first to share grades", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(context, "🎓 Error sharing nutrition grade: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 📤 EXPORT DETAILED DATA
     */
    private fun exportDetailedData() {
        lifecycleScope.launch {
            try {
                // 📅 GET PAST MONTH'S DATA
                val calendar = Calendar.getInstance()
                val endDate = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, -30)
                val startDate = calendar.timeInMillis
                
                val entries = repository.getEntriesInDateRange(startDate, endDate)
                
                val startDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startDate))
                val endDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(endDate))
                
                ProgressSharingManager.exportNutritionData(
                    context = requireContext(),
                    entries = entries,
                    startDate = startDateString,
                    endDate = endDateString
                )
                
            } catch (e: Exception) {
                Toast.makeText(context, "📤 Error exporting data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * ✏️ CREATE CUSTOM MESSAGE
     */
    private fun createCustomMessage() {
        lifecycleScope.launch {
            try {
                // 📅 GET RECENT DATA FOR CONTEXT
                val recentEntries = repository.getEntriesInDateRange(
                    System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L), // 7 days ago
                    System.currentTimeMillis()
                )
                val goals = repository.getNutritionGoalsSync()
                
                // 💬 GENERATE MOTIVATIONAL MESSAGE
                val motivationalMessage = ProgressSharingManager.generateMotivationalMessage(
                    entries = recentEntries,
                    goals = goals,
                    streakDays = recentEntries.groupBy { it.date }.size
                )
                
                // 📱 SHOW IN SHARING DIALOG
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, motivationalMessage)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "My Health Journey Progress")
                }
                
                val chooser = android.content.Intent.createChooser(shareIntent, "Share your journey")
                startActivity(chooser)
                
            } catch (e: Exception) {
                Toast.makeText(context, "✏️ Error creating custom message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 💡 GENERATE MOTIVATIONAL CONTENT
     */
    private fun generateMotivationalContent() {
        val motivationalQuotes = listOf(
            "🌟 Progress, not perfection, is the goal!",
            "💪 Every healthy choice is a victory!",
            "🎯 Small consistent actions create big results!",
            "✨ Your health journey inspires others!",
            "🔥 Consistency beats perfection every time!",
            "🏆 You're building lifelong healthy habits!",
            "📈 Growth happens one meal at a time!",
            "🌈 Celebrating every step of your journey!"
        )
        
        motivationalQuoteText.text = motivationalQuotes.random()
    }
    
    /**
     * 👀 UPDATE PREVIEW
     * 
     * Show preview of what will be shared based on privacy settings.
     */
    private fun updatePreview() {
        lifecycleScope.launch {
            try {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.timeInMillis
                val todayEntries = repository.getEntriesInDateRange(startOfDay, endOfDay)
                val goals = repository.getNutritionGoalsSync()
                
                val previewText = buildString {
                    append("📱 Sharing Preview:\n\n")
                    append("🍎 My Nutrition Progress Today\n\n")
                    
                    if (includeCaloriesCheckbox.isChecked) {
                        val totalCalories = todayEntries.sumOf { it.getActualCalories() }
                        append("📊 Calories: $totalCalories")
                        
                        if (includeGoalsCheckbox.isChecked && goals != null) {
                            val percentage = (totalCalories.toFloat() / goals.calorieGoal * 100).roundToInt()
                            append(" / ${goals.calorieGoal} (${percentage}%)")
                        }
                        append("\n")
                    }
                    
                    if (includeMacrosCheckbox.isChecked) {
                        val totalProtein = todayEntries.sumOf { it.getActualProtein() ?: 0.0 }
                        val totalCarbs = todayEntries.sumOf { it.getActualCarbs() ?: 0.0 }
                        val totalFat = todayEntries.sumOf { it.getActualFat() ?: 0.0 }
                        
                        append("\n💪 Macro Breakdown:\n")
                        append("• Protein: ${totalProtein.roundToInt()}g\n")
                        append("• Carbs: ${totalCarbs.roundToInt()}g\n")
                        append("• Fat: ${totalFat.roundToInt()}g\n")
                    }
                    
                    append("\n🏷️ #CalorieTracker #HealthyEating")
                    
                    if (!includeCaloriesCheckbox.isChecked && !includeMacrosCheckbox.isChecked) {
                        append("\n\n⚠️ Select what to include in your share")
                    }
                }
                
                previewTextView.text = previewText
                
            } catch (e: Exception) {
                previewTextView.text = "📊 Preview will appear here..."
            }
        }
    }
    
    /**
     * 🎯 GET PRIVACY-FILTERED SUMMARY
     * 
     * Create summary respecting user's privacy preferences.
     */
    private fun getPrivacyFilteredSummary(
        entries: List<CalorieEntry>,
        goals: NutritionGoals?
    ): String {
        return buildString {
            append("🍎 My nutrition progress")
            
            if (includeCaloriesCheckbox.isChecked) {
                val calories = entries.sumOf { it.getActualCalories() }
                append(" - $calories calories")
                
                if (includeGoalsCheckbox.isChecked && goals != null) {
                    val percentage = (calories.toFloat() / goals.calorieGoal * 100).roundToInt()
                    append(" (${percentage}% of goal)")
                }
            }
            
            if (includeMacrosCheckbox.isChecked) {
                val protein = entries.sumOf { it.getActualProtein() ?: 0.0 }.roundToInt()
                val carbs = entries.sumOf { it.getActualCarbs() ?: 0.0 }.roundToInt()
                val fat = entries.sumOf { it.getActualFat() ?: 0.0 }.roundToInt()
                append(" | Macros: ${protein}p/${carbs}c/${fat}f")
            }
            
            append("\n\n💪 Building healthy habits one meal at a time!")
            append("\n\n🏷️ #CalorieTracker #HealthGoals")
        }
    }
}