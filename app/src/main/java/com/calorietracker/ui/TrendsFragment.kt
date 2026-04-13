package com.calorietracker.ui

// 🧰 ANDROID UI TOOLS
import android.os.Bundle                    // Save/restore state
import android.view.LayoutInflater         // Create views from XML
import android.view.View                   // Base view class
import android.view.ViewGroup              // Container for views
import android.widget.*                    // UI components
import androidx.fragment.app.Fragment      // Fragment base class
import androidx.lifecycle.lifecycleScope   // Coroutine management
import androidx.lifecycle.Observer         // Data observation
import com.calorietracker.R               // App resources
import com.calorietracker.repository.CalorieRepository  // Data access
import com.calorietracker.database.CalorieDatabase      // Database access
import com.calorietracker.database.CalorieEntry         // Food entry data
import com.calorietracker.database.getActualProtein     // Extension function for actual protein
import com.calorietracker.database.getActualCarbs       // Extension function for actual carbs
import com.calorietracker.database.getActualFat         // Extension function for actual fat
import com.calorietracker.utils.TrendAnalyzer           // Trend calculation
import com.calorietracker.utils.TrendInsights           // Trend analysis results
import com.calorietracker.widgets.*                     // Custom widgets
import kotlinx.coroutines.launch          // Async operations
import java.text.SimpleDateFormat         // Date formatting
import java.util.*                        // Date utilities

/**
 * 📈 TRENDS FRAGMENT - NUTRITION ANALYTICS DASHBOARD
 * 
 * Hey young programmer! This screen shows beautiful charts and graphs that help users
 * understand their nutrition patterns over time.
 * 
 * 🎯 What does this screen show?
 * - 📊 **Weekly/Monthly Charts**: Line graphs showing calorie trends
 * - 💪 **Macro Progress**: How protein, carbs, and fat intake changes over time
 * - 🎯 **Goal Adherence**: How often user hits their nutrition targets
 * - 🔥 **Streak Analysis**: Consecutive days of good nutrition
 * - 📋 **Smart Insights**: AI-generated advice based on patterns
 * 
 * 🎨 Interactive Features:
 * - Switch between weekly and monthly views
 * - Toggle between different metrics (calories, protein, carbs, fat)
 * - Tap chart points to see exact values
 * - Share progress reports with friends/coaches
 * 
 * 📊 Chart Types Available:
 * - **Line Charts**: Best for showing trends over time
 * - **Bar Charts**: Great for comparing daily values
 * - **Area Charts**: Show cumulative progress visually
 * - **Goal Comparison**: Track adherence to nutrition goals
 * 
 * 💡 Smart Analytics:
 * - "Your protein intake improved 15% this month!"
 * - "You're most consistent on weekdays vs weekends"
 * - "Best nutrition day was Tuesday with an A+ grade"
 * - "Try adding more vegetables on Sunday"
 */
class TrendsFragment : Fragment() {
    
    // 🗄️ DATA MANAGEMENT
    private lateinit var repository: CalorieRepository
    
    // 📊 UI COMPONENTS
    private lateinit var trendGraphWidget: TrendGraphWidget
    private lateinit var macroRingWidget: MacroRingWidget
    private lateinit var timeFrameSpinner: Spinner
    private lateinit var metricTypeSpinner: Spinner
    private lateinit var chartTypeSpinner: Spinner
    private lateinit var insightsTextView: TextView
    private lateinit var consistencyScoreText: TextView
    private lateinit var refreshButton: Button
    
    // 🎯 CURRENT SETTINGS
    private var currentTimeFrame = TrendTimeFrame.WEEKLY
    private var currentMetricType = TrendMetricType.CALORIES
    private var currentChartType = TrendChartType.LINE
    
    /**
     * 🏗️ CREATE FRAGMENT VIEW
     * 
     * Build the trends screen layout with charts and controls.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trends, container, false)
    }
    
    /**
     * ⚙️ SET UP FRAGMENT
     * 
     * Initialize all UI components and data connections.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 🗄️ INITIALIZE DATA REPOSITORY
        repository = CalorieRepository(CalorieDatabase.getDatabase(requireContext()), requireContext())
        
        // 🔗 CONNECT UI COMPONENTS
        setupUIComponents(view)
        setupSpinnerListeners()
        setupClickListeners()
        
        // 📊 LOAD INITIAL TREND DATA
        loadTrendData()
    }
    
    /**
     * 🔗 SETUP UI COMPONENTS
     * 
     * Find and initialize all UI elements.
     */
    private fun setupUIComponents(view: View) {
        trendGraphWidget = view.findViewById(R.id.trendGraphWidget)
        macroRingWidget = view.findViewById(R.id.macroRingWidget) 
        timeFrameSpinner = view.findViewById(R.id.timeFrameSpinner)
        metricTypeSpinner = view.findViewById(R.id.metricTypeSpinner)
        chartTypeSpinner = view.findViewById(R.id.chartTypeSpinner)
        insightsTextView = view.findViewById(R.id.insightsTextView)
        consistencyScoreText = view.findViewById(R.id.consistencyScoreText)
        refreshButton = view.findViewById(R.id.refreshButton)
        
        // 🎛️ SETUP SPINNER OPTIONS
        setupSpinnerAdapters()
    }
    
    /**
     * 🎛️ SETUP SPINNER ADAPTERS
     * 
     * Configure dropdown options for chart customization.
     */
    private fun setupSpinnerAdapters() {
        // ⏰ TIME FRAME OPTIONS
        val timeFrameOptions = listOf("📅 Weekly (7 days)", "📅 Monthly (30 days)")
        timeFrameSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            timeFrameOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        // 📊 METRIC TYPE OPTIONS
        val metricOptions = listOf(
            "🔥 Calories",
            "💪 Protein", 
            "🍞 Carbohydrates",
            "🥑 Fat",
            "⚖️ Weight"
        )
        metricTypeSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            metricOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        // 📈 CHART TYPE OPTIONS
        val chartOptions = listOf("📈 Line Chart", "📊 Bar Chart", "🏔️ Area Chart")
        chartTypeSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            chartOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
    
    /**
     * 👂 SETUP SPINNER LISTENERS
     * 
     * React when user changes chart settings.
     */
    private fun setupSpinnerListeners() {
        timeFrameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentTimeFrame = when (position) {
                    0 -> TrendTimeFrame.WEEKLY
                    1 -> TrendTimeFrame.MONTHLY
                    else -> TrendTimeFrame.WEEKLY
                }
                loadTrendData()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        metricTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentMetricType = when (position) {
                    0 -> TrendMetricType.CALORIES
                    1 -> TrendMetricType.PROTEIN
                    2 -> TrendMetricType.CARBS
                    3 -> TrendMetricType.FAT
                    4 -> TrendMetricType.WEIGHT
                    else -> TrendMetricType.CALORIES
                }
                loadTrendData()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        chartTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentChartType = when (position) {
                    0 -> TrendChartType.LINE
                    1 -> TrendChartType.BAR
                    2 -> TrendChartType.AREA
                    else -> TrendChartType.LINE
                }
                loadTrendData()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    /**
     * 👆 SETUP CLICK LISTENERS
     * 
     * Handle button taps and interactions.
     */
    private fun setupClickListeners() {
        refreshButton.setOnClickListener {
            loadTrendData()
            Toast.makeText(context, "📊 Trends updated!", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 📊 LOAD TREND DATA
     * 
     * Fetch data from database and update charts.
     */
    private fun loadTrendData() {
        lifecycleScope.launch {
            try {
                // 📅 GET DATE RANGE
                val daysBack = when (currentTimeFrame) {
                    TrendTimeFrame.WEEKLY -> 7
                    TrendTimeFrame.MONTHLY -> 30
                }
                
                val calendar = Calendar.getInstance()
                val endDate = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, -daysBack)
                val startDate = calendar.timeInMillis
                
                // 🍎 FETCH FOOD ENTRIES
                val entries = repository.getEntriesInDateRange(startDate, endDate)
                
                // 🎯 GET USER'S GOAL FOR THIS METRIC
                val goalValue = when (currentMetricType) {
                    TrendMetricType.CALORIES -> repository.getCalorieGoal()?.toFloat()
                    TrendMetricType.PROTEIN -> repository.getNutritionGoalsAsync()?.proteinGoal?.toFloat()
                    TrendMetricType.CARBS -> repository.getNutritionGoalsAsync()?.carbsGoal?.toFloat()
                    TrendMetricType.FAT -> repository.getNutritionGoalsAsync()?.fatGoal?.toFloat()
                    TrendMetricType.WEIGHT -> null // Weight doesn't have a daily "goal"
                }
                
                // 📊 GENERATE TREND DATA
                val trendData = if (currentMetricType == TrendMetricType.WEIGHT) {
                    // ⚖️ SPECIAL HANDLING FOR WEIGHT DATA
                    val weightEntries = repository.getWeightEntriesInRange(startDate, endDate)
                    TrendAnalyzer.generateWeightTrendData(weightEntries, currentTimeFrame)
                } else {
                    // 🍎 REGULAR NUTRITION DATA
                    TrendAnalyzer.generateTrendData(requireContext(), entries, currentMetricType, currentTimeFrame, goalValue)
                }
                
                // 🎨 UPDATE CHART
                trendGraphWidget.updateTrendData(
                    newDataPoints = trendData,
                    goalValue = goalValue,
                    metricType = currentMetricType,
                    timeFrame = currentTimeFrame,
                    chartType = currentChartType
                )
                
                // 💡 GENERATE AND DISPLAY INSIGHTS
                val insights = TrendAnalyzer.generateTrendInsights(trendData, currentMetricType, goalValue)
                displayInsights(insights)
                
                // 📊 UPDATE MACRO RINGS (if showing nutrition data)
                if (currentMetricType != TrendMetricType.WEIGHT) {
                    updateMacroRings(entries)
                }
                
                // 🎯 CALCULATE CONSISTENCY SCORE
                val consistencyScore = TrendAnalyzer.calculateConsistencyScore(trendData, goalValue)
                displayConsistencyScore(consistencyScore)
                
            } catch (e: Exception) {
                Toast.makeText(context, "📊 Error loading trend data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * 💡 DISPLAY INSIGHTS AND RECOMMENDATIONS
     * 
     * Show AI-generated insights about user's nutrition trends.
     */
    private fun displayInsights(insights: TrendInsights) {
        val insightText = buildString {
            append("📋 ${insights.summary}\n\n")
            
            append("🔍 Key Insights:\n")
            insights.insights.forEach { insight ->
                append("• $insight\n")
            }
            append("\n")
            
            append("💡 Recommendations:\n")
            insights.recommendations.forEach { recommendation ->
                append("• $recommendation\n")
            }
        }
        
        insightsTextView.text = insightText
    }
    
    /**
     * 🎯 DISPLAY CONSISTENCY SCORE
     * 
     * Show how consistent the user's nutrition tracking is.
     */
    private fun displayConsistencyScore(score: Float) {
        val scoreText = "🎯 Consistency Score: ${score.toInt()}%"
        val gradeEmoji = when {
            score >= 90f -> "🏆"
            score >= 80f -> "⭐"
            score >= 70f -> "👍"
            score >= 60f -> "📊"
            else -> "📈"
        }
        
        consistencyScoreText.text = "$gradeEmoji $scoreText"
    }
    
    /**
     * 💪 UPDATE MACRO RINGS
     * 
     * Show current macro balance in the ring widget.
     */
    private fun updateMacroRings(entries: List<CalorieEntry>) {
        lifecycleScope.launch {
            try {
                // 🧮 CALCULATE TODAY'S MACROS
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val todayEntries = entries.filter { it.timestamp >= today }
                
                val totalProtein = todayEntries.sumOf { it.getActualProtein() ?: 0.0 }.toFloat()
                val totalCarbs = todayEntries.sumOf { it.getActualCarbs() ?: 0.0 }.toFloat()
                val totalFat = todayEntries.sumOf { it.getActualFat() ?: 0.0 }.toFloat()
                
                // 🎯 GET NUTRITION GOALS
                val goals = repository.getNutritionGoalsAsync()
                val proteinGoal = goals?.proteinGoal?.toFloat() ?: 100f
                val carbsGoal = goals?.carbsGoal?.toFloat() ?: 250f
                val fatGoal = goals?.fatGoal?.toFloat() ?: 80f
                
                // 🎨 UPDATE RING WIDGET
                macroRingWidget.updateMacros(
                    proteinCurrent = totalProtein,
                    proteinGoal = proteinGoal,
                    carbsCurrent = totalCarbs,
                    carbsGoal = carbsGoal,
                    fatCurrent = totalFat,
                    fatGoal = fatGoal
                )
                
            } catch (e: Exception) {
                Toast.makeText(context, "📊 Error updating macro rings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}