package com.calorietracker

import android.util.Log

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.repository.CalorieRepository
import com.calorietracker.utils.ThemeManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * This activity shows detailed analytics and charts for the user's nutrition data
 * Features include calorie trends, macro balance charts, and goal streak tracking
 */
class AnalyticsActivity : AppCompatActivity() {
    
    // Repository to access historical nutrition data from the database
    private lateinit var repository: CalorieRepository
    
    // Chart components for displaying different types of data visualizations
    private lateinit var calorieChart: LineChart
    private lateinit var macroChart: PieChart
    
    // UI elements for navigation and period selection
    private lateinit var btnBack: MaterialButton
    private lateinit var btnWeekly: MaterialButton
    private lateinit var btnMonthly: MaterialButton
    
    // Streak tracking UI elements
    private lateinit var tvCurrentStreak: TextView
    private lateinit var tvBestStreak: TextView
    
    // Variable to track which time period is currently being displayed
    private var isWeeklyView = true
    
    // Variables to store streak information
    private var currentStreak = 0
    private var bestStreak = 0
    
    /**
     * This method runs when the analytics screen opens
     * It sets up all the charts and loads the initial data
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before setting content view
        ThemeManager.applyTheme(this)
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)
        
        // Set up database connection to get nutrition data
        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)
        
        // Connect to all the UI elements and set up their behavior
        initViews()
        setupCharts()
        setupClickListeners()
        
        // Load the initial weekly view of data
        loadWeeklyData()
    }
    
    /**
     * Connect to all the UI elements in the layout
     */
    private fun initViews() {
        calorieChart = findViewById(R.id.calorieChart)
        macroChart = findViewById(R.id.macroChart)
        btnBack = findViewById(R.id.btnBack)
        btnWeekly = findViewById(R.id.btnWeekly)
        btnMonthly = findViewById(R.id.btnMonthly)
        tvCurrentStreak = findViewById(R.id.tvCurrentStreak)
        tvBestStreak = findViewById(R.id.tvBestStreak)
    }
    
    /**
     * Set up the basic appearance and behavior of both charts
     */
    private fun setupCharts() {
        // Configure the calorie trend line chart
        calorieChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setBackgroundColor(Color.WHITE)
            
            // Configure the chart axes
            xAxis.apply {
                granularity = 1f
                setDrawGridLines(false)
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = true
        }
        
        // Configure the macro balance pie chart
        macroChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            
            dragDecelerationFrictionCoef = 0.95f
            setDrawHoleEnabled(true)
            setHoleColor(Color.WHITE)
            holeRadius = 40f
            transparentCircleRadius = 45f
            
            setDrawCenterText(true)
            centerText = "Macro\\nBalance"
            
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
        }
    }
    
    /**
     * Set up what happens when buttons are pressed
     */
    private fun setupClickListeners() {
        // Back button - closes this screen and returns to main screen
        btnBack.setOnClickListener {
            finish()
        }
        
        // Weekly view button - shows data for the past 7 days
        btnWeekly.setOnClickListener {
            if (!isWeeklyView) {
                isWeeklyView = true
                updateButtonStates()
                loadWeeklyData()
            }
        }
        
        // Monthly view button - shows data for the past 30 days
        btnMonthly.setOnClickListener {
            if (isWeeklyView) {
                isWeeklyView = false
                updateButtonStates()
                loadMonthlyData()
            }
        }
    }
    
    /**
     * Update the visual appearance of period selection buttons
     */
    private fun updateButtonStates() {
        if (isWeeklyView) {
            btnWeekly.setBackgroundColor(getColor(R.color.primary_green))
            btnMonthly.setBackgroundColor(getColor(R.color.light_gray))
        } else {
            btnWeekly.setBackgroundColor(getColor(R.color.light_gray))
            btnMonthly.setBackgroundColor(getColor(R.color.primary_green))
        }
    }
    
    /**
     * Load and display nutrition data for the past 7 days
     */
    private fun loadWeeklyData() {
        val endDate = getCurrentDateString()
        val startDate = getDateDaysAgo(7)
        
        // Get entries for the past week and display them in charts
        repository.getEntriesForDateRange(startDate, endDate).observe(this) { entries ->
            try {
                if (entries != null) {
                    updateCalorieChart(entries, isWeekly = true)
                    updateMacroChart(entries)
                    updateStreakInfo(entries)
                }
            } catch (e: Exception) {
                // Handle any errors that occur while loading data
                Log.e("AnalyticsActivity", "Unexpected error", e)
            }
        }
    }
    
    /**
     * Load and display nutrition data for the past 30 days
     */
    private fun loadMonthlyData() {
        val endDate = getCurrentDateString()
        val startDate = getDateDaysAgo(30)
        
        // Get entries for the past month and display them in charts
        repository.getEntriesForDateRange(startDate, endDate).observe(this) { entries ->
            try {
                if (entries != null) {
                    updateCalorieChart(entries, isWeekly = false)
                    updateMacroChart(entries)
                    updateStreakInfo(entries)
                }
            } catch (e: Exception) {
                // Handle any errors that occur while loading data
                Log.e("AnalyticsActivity", "Unexpected error", e)
            }
        }
    }
    
    /**
     * Update the line chart showing calorie intake trends over time
     */
    private fun updateCalorieChart(entries: List<com.calorietracker.database.CalorieEntry>, isWeekly: Boolean) {
        try {
            if (entries.isEmpty()) {
                calorieChart.data = null
                calorieChart.invalidate()
                return
            }
            
            // Group entries by date to get daily totals
            val dailyCalories = entries.groupBy { 
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp))
            }.mapValues { (_, dayEntries) ->
                dayEntries.sumOf { it.calories }
            }.toSortedMap()
            
            // Create chart data points
            val chartEntries = ArrayList<Entry>()
            val labels = ArrayList<String>()
            
            dailyCalories.entries.forEachIndexed { index, (date, calories) ->
                chartEntries.add(Entry(index.toFloat(), calories.toFloat()))
                // Format date for display (show month/day only)
                try {
                    val displayDate = SimpleDateFormat("M/d", Locale.getDefault()).format(
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) ?: Date()
                    )
                    labels.add(displayDate)
                } catch (e: Exception) {
                    labels.add(date.substring(5)) // Fallback: just show MM-dd
                }
            }
            
            if (chartEntries.isEmpty()) {
                calorieChart.data = null
                calorieChart.invalidate()
                return
            }
            
            // Create the line dataset
            val dataSet = LineDataSet(chartEntries, "Daily Calories").apply {
                color = getColor(R.color.primary_green)
                setCircleColor(getColor(R.color.primary_green))
                lineWidth = 3f
                circleRadius = 4f
                setDrawCircleHole(false)
                valueTextSize = 10f
                setDrawFilled(true)
                fillColor = getColor(R.color.primary_green)
                fillAlpha = 50
            }
            
            // Apply the data to the chart
            val lineData = LineData(dataSet)
            calorieChart.data = lineData
            
            // Configure x-axis labels
            calorieChart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value.toInt() >= 0 && value.toInt() < labels.size) {
                        labels[value.toInt()]
                    } else ""
                }
            }
            
            calorieChart.invalidate() // Refresh the chart display
            
        } catch (e: Exception) {
            Log.e("AnalyticsActivity", "Unexpected error", e)
            // If chart update fails, clear the chart
            calorieChart.data = null
            calorieChart.invalidate()
        }
    }
    
    /**
     * Update the pie chart showing the balance of macronutrients
     */
    private fun updateMacroChart(entries: List<com.calorietracker.database.CalorieEntry>) {
        try {
            // Calculate total macronutrients from all entries
            var totalProtein = 0.0
            var totalCarbs = 0.0
            var totalFat = 0.0
            
            entries.forEach { entry ->
                totalProtein += (entry.protein ?: 0.0)
                totalCarbs += (entry.carbs ?: 0.0)
                totalFat += (entry.fat ?: 0.0)
            }
            
            // Only show chart if there's macro data available
            if (totalProtein + totalCarbs + totalFat > 0) {
                val pieEntries = ArrayList<PieEntry>()
                
                // Add each macronutrient as a slice if it has data
                if (totalProtein > 0) pieEntries.add(PieEntry(totalProtein.toFloat(), "Protein"))
                if (totalCarbs > 0) pieEntries.add(PieEntry(totalCarbs.toFloat(), "Carbs"))
                if (totalFat > 0) pieEntries.add(PieEntry(totalFat.toFloat(), "Fat"))
                
                if (pieEntries.isNotEmpty()) {
                    // Create the pie chart dataset with colors
                    val dataSet = PieDataSet(pieEntries, "Macronutrients").apply {
                        colors = listOf(
                            getColor(R.color.primary_green),  // Protein
                            getColor(R.color.teal_200),       // Carbs
                            Color.parseColor("#FF9800")       // Fat (orange)
                        )
                        valueTextSize = 12f
                        valueTextColor = Color.WHITE
                    }
                    
                    // Apply the data to the pie chart
                    val pieData = PieData(dataSet).apply {
                        setValueFormatter(object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return "${value.toInt()}g"
                            }
                        })
                    }
                    
                    macroChart.data = pieData
                } else {
                    macroChart.data = null
                    macroChart.centerText = "No Macro\\nData Available"
                }
            } else {
                // Show empty state if no macro data is available
                macroChart.data = null
                macroChart.centerText = "No Macro\\nData Available"
            }
            
            macroChart.invalidate() // Refresh the chart display
            
        } catch (e: Exception) {
            Log.e("AnalyticsActivity", "Unexpected error", e)
            // If chart update fails, show empty state
            macroChart.data = null
            macroChart.centerText = "Error Loading\\nMacro Data"
            macroChart.invalidate()
        }
    }
    
    /**
     * Calculate and display streak information for consecutive goal achievements
     */
    private fun updateStreakInfo(entries: List<com.calorietracker.database.CalorieEntry>) {
        try {
            // Group entries by date and calculate daily totals
            val dailyTotals = entries.groupBy { 
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp))
            }.mapValues { (_, dayEntries) ->
                dayEntries.sumOf { it.calories }
            }.toSortedMap()
            
            // Calculate streak of consecutive days meeting calorie goals
            // (This is a simplified version - assumes 2000 calorie goal)
            val goalCalories = 2000
            currentStreak = 0
            var tempStreak = 0
            bestStreak = 0
            
            if (dailyTotals.isNotEmpty()) {
                // Check from most recent day backwards for current streak
                val sortedDates = dailyTotals.keys.sortedDescending()
                for (date in sortedDates) {
                    val dailyCalories = dailyTotals[date] ?: 0
                    if (dailyCalories >= goalCalories * 0.8 && dailyCalories <= goalCalories * 1.2) {
                        // Within 80-120% of goal is considered "meeting goal"
                        if (date == sortedDates.first()) {
                            currentStreak++
                        } else if (currentStreak > 0) {
                            currentStreak++
                        }
                        tempStreak++
                        bestStreak = maxOf(bestStreak, tempStreak)
                    } else {
                        if (date == sortedDates.first()) {
                            currentStreak = 0
                        }
                        tempStreak = 0
                    }
                }
            }
            
            // Update streak display in the UI
            tvCurrentStreak.text = currentStreak.toString()
            tvBestStreak.text = bestStreak.toString()
            
            // Update macro chart center text with current streak info
            macroChart.centerText = "Current Streak\\n$currentStreak days"
            
        } catch (e: Exception) {
            Log.e("AnalyticsActivity", "Unexpected error", e)
            // If streak calculation fails, show defaults
            currentStreak = 0
            bestStreak = 0
            tvCurrentStreak.text = "0"
            tvBestStreak.text = "0"
            macroChart.centerText = "Streak\\nUnavailable"
        }
    }
    
    /**
     * Get the current date as a string in YYYY-MM-DD format
     */
    private fun getCurrentDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }
    
    /**
     * Get the date from X days ago as a string in YYYY-MM-DD format
     */
    private fun getDateDaysAgo(days: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(calendar.time)
    }
}