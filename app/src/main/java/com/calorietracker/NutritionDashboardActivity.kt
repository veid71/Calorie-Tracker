package com.calorietracker

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.nutrition.NutritionRecommendations
import com.calorietracker.repository.CalorieRepository
import com.calorietracker.utils.ThemeManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

/**
 * This activity shows the user a detailed nutrition dashboard
 * It displays how much of each nutrient they've consumed today vs. their daily goals
 * Think of it like a report card for nutrition
 */
class NutritionDashboardActivity : AppCompatActivity() {
    
    // Repository to get nutrition data from the database
    private lateinit var repository: CalorieRepository
    
    // UI elements for calories
    private lateinit var tvCaloriesConsumed: TextView
    private lateinit var tvCaloriesGoal: TextView
    private lateinit var tvCaloriesPercentage: TextView
    private lateinit var progressBarCalories: ProgressBar
    
    // UI elements for protein
    private lateinit var tvProteinConsumed: TextView
    private lateinit var tvProteinGoal: TextView
    private lateinit var tvProteinPercentage: TextView
    private lateinit var progressBarProtein: ProgressBar
    
    // UI elements for carbs
    private lateinit var tvCarbsConsumed: TextView
    private lateinit var tvCarbsGoal: TextView
    private lateinit var tvCarbsPercentage: TextView
    private lateinit var progressBarCarbs: ProgressBar
    
    // UI elements for fat
    private lateinit var tvFatConsumed: TextView
    private lateinit var tvFatGoal: TextView
    private lateinit var tvFatPercentage: TextView
    private lateinit var progressBarFat: ProgressBar
    
    // UI elements for fiber
    private lateinit var tvFiberConsumed: TextView
    private lateinit var tvFiberGoal: TextView
    private lateinit var tvFiberPercentage: TextView
    private lateinit var progressBarFiber: ProgressBar
    
    // UI elements for sugar (this is a limit, not a goal)
    private lateinit var tvSugarConsumed: TextView
    private lateinit var tvSugarLimit: TextView
    private lateinit var tvSugarPercentage: TextView
    private lateinit var progressBarSugar: ProgressBar
    private lateinit var cardSugar: MaterialCardView
    
    // UI elements for sodium (this is a limit, not a goal)
    private lateinit var tvSodiumConsumed: TextView
    private lateinit var tvSodiumLimit: TextView
    private lateinit var tvSodiumPercentage: TextView
    private lateinit var progressBarSodium: ProgressBar
    private lateinit var cardSodium: MaterialCardView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before setting content view
        ThemeManager.applyTheme(this)
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nutrition_dashboard)
        
        // Set up the repository to access nutrition data
        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)
        
        // Connect to all the UI elements
        initViews()
        
        // Set up click listeners for buttons
        setupClickListeners()
        
        // Load and display nutrition data
        loadNutritionData()
    }
    
    /**
     * Connect to all the UI elements in the layout
     * This is like introducing the code to all the buttons and text views
     */
    private fun initViews() {
        // Calorie elements
        tvCaloriesConsumed = findViewById(R.id.tvCaloriesConsumed)
        tvCaloriesGoal = findViewById(R.id.tvCaloriesGoal)
        tvCaloriesPercentage = findViewById(R.id.tvCaloriesPercentage)
        progressBarCalories = findViewById(R.id.progressBarCalories)
        
        // Protein elements
        tvProteinConsumed = findViewById(R.id.tvProteinConsumed)
        tvProteinGoal = findViewById(R.id.tvProteinGoal)
        tvProteinPercentage = findViewById(R.id.tvProteinPercentage)
        progressBarProtein = findViewById(R.id.progressBarProtein)
        
        // Carbs elements
        tvCarbsConsumed = findViewById(R.id.tvCarbsConsumed)
        tvCarbsGoal = findViewById(R.id.tvCarbsGoal)
        tvCarbsPercentage = findViewById(R.id.tvCarbsPercentage)
        progressBarCarbs = findViewById(R.id.progressBarCarbs)
        
        // Fat elements
        tvFatConsumed = findViewById(R.id.tvFatConsumed)
        tvFatGoal = findViewById(R.id.tvFatGoal)
        tvFatPercentage = findViewById(R.id.tvFatPercentage)
        progressBarFat = findViewById(R.id.progressBarFat)
        
        // Fiber elements
        tvFiberConsumed = findViewById(R.id.tvFiberConsumed)
        tvFiberGoal = findViewById(R.id.tvFiberGoal)
        tvFiberPercentage = findViewById(R.id.tvFiberPercentage)
        progressBarFiber = findViewById(R.id.progressBarFiber)
        
        // Sugar elements (limit, not goal)
        tvSugarConsumed = findViewById(R.id.tvSugarConsumed)
        tvSugarLimit = findViewById(R.id.tvSugarLimit)
        tvSugarPercentage = findViewById(R.id.tvSugarPercentage)
        progressBarSugar = findViewById(R.id.progressBarSugar)
        cardSugar = findViewById(R.id.cardSugar)
        
        // Sodium elements (limit, not goal)
        tvSodiumConsumed = findViewById(R.id.tvSodiumConsumed)
        tvSodiumLimit = findViewById(R.id.tvSodiumLimit)
        tvSodiumPercentage = findViewById(R.id.tvSodiumPercentage)
        progressBarSodium = findViewById(R.id.progressBarSodium)
        cardSodium = findViewById(R.id.cardSodium)
    }
    
    /**
     * Set up what happens when buttons are clicked
     */
    private fun setupClickListeners() {
        // Settings button - opens the settings screen
        findViewById<MaterialButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // Back button - closes this screen
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        // Add food button - quick way to add more food
        findViewById<MaterialButton>(R.id.btnAddFood).setOnClickListener {
            startActivity(Intent(this, CalorieEntryActivity::class.java))
        }
    }
    
    /**
     * Load nutrition data and update the display
     * This runs in the background so it doesn't freeze the screen
     */
    private fun loadNutritionData() {
        lifecycleScope.launch {
            try {
                // Get today's total nutrition intake
                val nutritionTotals = repository.getTodaysNutritionTotals()
                
                // Get the user's goals and region preferences
                val userGoals = repository.getNutritionGoalsSync()
                val userRegion = userGoals?.selectedRegion ?: "US"
                
                // Get recommended daily values for the user's region
                val recommendations = NutritionRecommendations.getRecommendationsForRegion(userRegion)
                
                // Update the UI with all the nutrition data
                updateNutritionDisplay(nutritionTotals, userGoals, recommendations, userRegion)
                
            } catch (e: Exception) {
                // If something goes wrong, show default values
                showDefaultValues()
            }
        }
    }
    
    /**
     * Update all the nutrition displays with current data
     * This shows the user how they're doing with their nutrition goals
     */
    private fun updateNutritionDisplay(
        totals: Map<String, Double>,
        userGoals: com.calorietracker.database.NutritionGoals?,
        recommendations: Map<String, Double>,
        region: String
    ) {
        // Get actual consumed amounts (default to 0 if not found)
        val caloriesConsumed = totals["calories"] ?: 0.0
        val proteinConsumed = totals["protein"] ?: 0.0
        val carbsConsumed = totals["carbs"] ?: 0.0
        val fatConsumed = totals["fat"] ?: 0.0
        val fiberConsumed = totals["fiber"] ?: 0.0
        val sugarConsumed = totals["sugar"] ?: 0.0
        val sodiumConsumed = totals["sodium"] ?: 0.0
        
        // Get goals (use user's custom goals if available, otherwise use recommendations)
        val calorieGoal = userGoals?.calorieGoal?.toDouble() ?: recommendations["calories"] ?: 2000.0
        val proteinGoal = userGoals?.proteinGoal ?: recommendations["protein"] ?: 50.0
        val carbsGoal = userGoals?.carbsGoal ?: recommendations["carbs"] ?: 300.0
        val fatGoal = userGoals?.fatGoal ?: recommendations["fat"] ?: 65.0
        val fiberGoal = userGoals?.fiberGoal ?: recommendations["fiber"] ?: 25.0
        val sugarLimit = userGoals?.sugarGoal ?: recommendations["sugar"] ?: 50.0
        val sodiumLimit = userGoals?.sodiumGoal ?: recommendations["sodium"] ?: 2300.0
        
        // Update calories display
        updateNutrientDisplay(
            tvCaloriesConsumed, tvCaloriesGoal, tvCaloriesPercentage, progressBarCalories,
            caloriesConsumed, calorieGoal, "cal", false
        )
        
        // Update protein display
        updateNutrientDisplay(
            tvProteinConsumed, tvProteinGoal, tvProteinPercentage, progressBarProtein,
            proteinConsumed, proteinGoal, "g", false
        )
        
        // Update carbs display
        updateNutrientDisplay(
            tvCarbsConsumed, tvCarbsGoal, tvCarbsPercentage, progressBarCarbs,
            carbsConsumed, carbsGoal, "g", false
        )
        
        // Update fat display
        updateNutrientDisplay(
            tvFatConsumed, tvFatGoal, tvFatPercentage, progressBarFat,
            fatConsumed, fatGoal, "g", false
        )
        
        // Update fiber display
        updateNutrientDisplay(
            tvFiberConsumed, tvFiberGoal, tvFiberPercentage, progressBarFiber,
            fiberConsumed, fiberGoal, "g", false
        )
        
        // Update sugar display (this is a limit - red if over)
        updateNutrientDisplay(
            tvSugarConsumed, tvSugarLimit, tvSugarPercentage, progressBarSugar,
            sugarConsumed, sugarLimit, "g", true
        )
        
        // Change sugar card color if over limit
        if (sugarConsumed > sugarLimit) {
            cardSugar.setCardBackgroundColor(getColor(R.color.light_red))
        } else {
            cardSugar.setCardBackgroundColor(getColor(R.color.white))
        }
        
        // Update sodium display (this is a limit - red if over)
        updateNutrientDisplay(
            tvSodiumConsumed, tvSodiumLimit, tvSodiumPercentage, progressBarSodium,
            sodiumConsumed, sodiumLimit, "mg", true
        )
        
        // Change sodium card color if over limit
        if (sodiumConsumed > sodiumLimit) {
            cardSodium.setCardBackgroundColor(getColor(R.color.light_red))
        } else {
            cardSodium.setCardBackgroundColor(getColor(R.color.white))
        }
    }
    
    /**
     * Update a single nutrient's display (consumed, goal, percentage, progress bar)
     * This is a helper function to avoid repeating the same code 7 times
     */
    private fun updateNutrientDisplay(
        consumedTextView: TextView,
        goalTextView: TextView,
        percentageTextView: TextView,
        progressBar: ProgressBar,
        consumed: Double,
        goal: Double,
        unit: String,
        isLimit: Boolean
    ) {
        // Show consumed amount (like "150g")
        consumedTextView.text = "${consumed.toInt()}$unit"
        
        // Show goal/limit (like "/ 200g")
        goalTextView.text = "/ ${goal.toInt()}$unit"
        
        // Calculate percentage
        val percentage = if (goal > 0) (consumed / goal * 100).toInt() else 0
        
        // Show percentage (like "75%")
        percentageTextView.text = "$percentage%"
        
        // Update progress bar (max 100%)
        progressBar.progress = minOf(percentage, 100)
        
        // Change color based on whether this is good or bad
        if (isLimit) {
            // For limits (sugar, sodium), red means bad (over limit)
            if (consumed > goal) {
                percentageTextView.setTextColor(getColor(R.color.red))
                progressBar.progressTintList = getColorStateList(R.color.red)
            } else {
                percentageTextView.setTextColor(getColor(R.color.green))
                progressBar.progressTintList = getColorStateList(R.color.green)
            }
        } else {
            // For goals (protein, fiber, etc.), green means good (reaching goal)
            if (percentage >= 100) {
                percentageTextView.setTextColor(getColor(R.color.green))
                progressBar.progressTintList = getColorStateList(R.color.green)
            } else if (percentage >= 75) {
                percentageTextView.setTextColor(getColor(R.color.orange))
                progressBar.progressTintList = getColorStateList(R.color.orange)
            } else {
                percentageTextView.setTextColor(getColor(R.color.gray))
                progressBar.progressTintList = getColorStateList(R.color.gray)
            }
        }
    }
    
    /**
     * Show default values if there's an error loading data
     */
    private fun showDefaultValues() {
        // This provides a fallback display if something goes wrong
        val defaultTotals = mapOf(
            "calories" to 0.0,
            "protein" to 0.0,
            "carbs" to 0.0,
            "fat" to 0.0,
            "fiber" to 0.0,
            "sugar" to 0.0,
            "sodium" to 0.0
        )
        
        val defaultRecommendations = NutritionRecommendations.US_RECOMMENDATIONS
        
        updateNutritionDisplay(defaultTotals, null, defaultRecommendations, "US")
    }
    
    /**
     * Refresh the display when the user returns to this screen
     * This ensures the data is always current
     */
    override fun onResume() {
        super.onResume()
        loadNutritionData()
    }
}