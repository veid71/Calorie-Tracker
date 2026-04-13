package com.calorietracker.widgets

// 🧰 WIDGET BUILDING TOOLS
import android.content.Context       // Access to Android system features
import android.content.Intent        // Navigate between screens
import android.util.AttributeSet     // Handles custom widget properties from XML
import android.view.LayoutInflater   // Creates views from XML layouts
import android.widget.LinearLayout   // Container that stacks views vertically
import android.widget.TextView       // Displays text
import androidx.lifecycle.LifecycleOwner      // Connects to activity lifecycle
import androidx.lifecycle.lifecycleScope     // Background task management
import com.calorietracker.HealthConnectDebugActivity // Debug screen for fitness data
import com.calorietracker.R                         // App resources (layouts, strings, etc.)
import com.calorietracker.database.CalorieDatabase  // Our nutrition database
import com.calorietracker.repository.CalorieRepository // Data manager
import com.google.android.material.button.MaterialButton // Pretty buttons
import com.google.android.material.card.MaterialCardView  // Container with rounded corners
import kotlinx.coroutines.launch     // Start background tasks
import java.text.SimpleDateFormat    // Format dates for display
import java.util.*                   // Date utilities

/**
 * 💪 WORKOUT SUMMARY WIDGET - FITNESS TRACKER DISPLAY
 * 
 * Hey young programmer! This is a custom widget (like a mini-app within our app).
 * 
 * 🏃 What does this widget do?
 * 1. Shows your workout data from today (if you have a fitness tracker)
 * 2. Displays how many bonus calories you earned from exercise
 * 3. Shows when your fitness data was last updated
 * 4. Provides a button to view detailed workout information
 * 
 * 🎯 Why do we need this?
 * When you exercise, you burn calories! This widget helps users understand:
 * "I went to the gym and burned 300 calories, so now I can eat 300 more calories today!"
 * 
 * 🏗️ Widget Architecture:
 * This extends MaterialCardView (a pretty rounded rectangle) and inflates
 * its own layout file. Think of it like building a LEGO component that you
 * can drop into any screen.
 * 
 * 📱 Smart Display Logic:
 * - If you worked out today: Shows workout details and calorie bonus
 * - If no workout data: Shows a friendly message encouraging exercise
 * - If fitness tracker not connected: Shows setup instructions
 * 
 * 🔄 Real-time Updates:
 * This widget automatically updates when new workout data comes in from
 * your smartwatch or fitness tracker via Health Connect.
 */
class WorkoutSummaryWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {
    
    private lateinit var repository: CalorieRepository
    private var lifecycleOwner: LifecycleOwner? = null
    
    private val tvWorkoutTitle: TextView
    private val tvCalorieAdjustment: TextView
    private val tvWorkoutDetails: TextView
    private val tvLastSync: TextView
    private val btnViewDetails: MaterialButton
    private val containerWorkout: LinearLayout
    private val containerNoWorkout: LinearLayout
    private val tvNoWorkoutMessage: TextView
    
    init {
        LayoutInflater.from(context).inflate(R.layout.widget_workout_summary, this, true)
        
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        tvCalorieAdjustment = findViewById(R.id.tvCalorieAdjustment)
        tvWorkoutDetails = findViewById(R.id.tvWorkoutDetails)
        tvLastSync = findViewById(R.id.tvLastSync)
        btnViewDetails = findViewById(R.id.btnViewDetails)
        containerWorkout = findViewById(R.id.containerWorkout)
        containerNoWorkout = findViewById(R.id.containerNoWorkout)
        tvNoWorkoutMessage = findViewById(R.id.tvNoWorkoutMessage)
        
        setupClickListeners()
        initializeRepository()
    }
    
    private fun setupClickListeners() {
        btnViewDetails.setOnClickListener {
            val intent = Intent(context, HealthConnectDebugActivity::class.java)
            context.startActivity(intent)
        }
        
        setOnClickListener {
            val intent = Intent(context, HealthConnectDebugActivity::class.java)
            context.startActivity(intent)
        }
    }
    
    private fun initializeRepository() {
        val database = CalorieDatabase.getDatabase(context)
        repository = CalorieRepository(database, context)
    }
    
    fun setLifecycleOwner(owner: LifecycleOwner) {
        lifecycleOwner = owner
        startObservingWorkoutData()
    }
    
    private fun startObservingWorkoutData() {
        lifecycleOwner?.let { owner ->
            // Observe workout data changes
            repository.getTodaysWorkoutCaloriesLive().observe(owner) { workoutCalories ->
                owner.lifecycleScope.launch {
                    updateWorkoutDisplay(workoutCalories)
                }
            }
            
            // Initial load
            owner.lifecycleScope.launch {
                refreshWorkoutData()
            }
        }
    }
    
    suspend fun refreshWorkoutData() {
        try {
            // Try to sync latest data
            val isAvailable = repository.isHealthConnectAvailable()
            val hasPermissions = repository.hasHealthConnectPermissions()
            
            if (isAvailable && hasPermissions) {
                repository.syncTodaysWorkoutData()
            }
            
            val workoutData = repository.getTodaysWorkoutCalories()
            updateWorkoutDisplay(workoutData)
            
        } catch (e: Exception) {
            showError("Error loading workout data: ${e.message}")
        }
    }
    
    private suspend fun updateWorkoutDisplay(workoutData: com.calorietracker.database.WorkoutCalories?) {
        if (workoutData != null && workoutData.activeCaloriesBurned > 0) {
            showWorkoutData(workoutData)
        } else {
            showNoWorkoutData()
        }
        updateLastSyncTime()
    }
    
    private suspend fun showWorkoutData(workoutData: com.calorietracker.database.WorkoutCalories) {
        containerWorkout.visibility = VISIBLE
        containerNoWorkout.visibility = GONE
        
        // Calculate calorie bonus (70% of active calories)
        val bonusCalories = (workoutData.activeCaloriesBurned * 0.7).toInt()
        val baseGoal = repository.getNutritionGoalsSync()?.calorieGoal ?: 2000
        val adjustedGoal = baseGoal + bonusCalories
        
        // Update UI
        tvWorkoutTitle.text = "🏃 Today's Workout Bonus"
        tvCalorieAdjustment.text = "+$bonusCalories calories"
        
        // Build workout details
        val details = buildString {
            append("Active calories burned: ${workoutData.activeCaloriesBurned}\n")
            if (workoutData.exerciseMinutes > 0) {
                append("Exercise time: ${workoutData.exerciseMinutes} minutes\n")
            }
            if (!workoutData.exerciseType.isNullOrBlank()) {
                append("Activity: ${workoutData.exerciseType}\n")
            }
            append("Daily goal adjusted: $baseGoal → $adjustedGoal calories")
        }
        tvWorkoutDetails.text = details
        
        // Set colors based on workout intensity
        val intensityColor = when {
            workoutData.activeCaloriesBurned >= 400 -> R.color.success_green
            workoutData.activeCaloriesBurned >= 200 -> R.color.warning_orange
            else -> R.color.primary_green
        }
        tvCalorieAdjustment.setTextColor(context.getColor(intensityColor))
    }
    
    private fun showNoWorkoutData() {
        containerWorkout.visibility = GONE
        containerNoWorkout.visibility = VISIBLE
        
        tvNoWorkoutMessage.text = buildString {
            append("No workout data found for today.\n\n")
            append("To see workout bonuses:\n")
            append("• Sync your fitness tracker with Health Connect\n")
            append("• Tap here to verify Health Connect setup\n")
            append("• Make sure to grant all required permissions")
        }
    }
    
    private fun updateLastSyncTime() {
        val sharedPrefs = context.getSharedPreferences("health_connect_prefs", Context.MODE_PRIVATE)
        val lastSync = sharedPrefs.getLong("last_sync_time", 0)
        
        if (lastSync == 0L) {
            tvLastSync.text = "Tap to verify Health Connect setup"
        } else {
            val now = System.currentTimeMillis()
            val timeDiff = now - lastSync
            val minutes = timeDiff / (1000 * 60)
            
            tvLastSync.text = when {
                minutes < 1 -> "Last sync: Just now"
                minutes < 60 -> "Last sync: ${minutes}m ago"
                minutes < 1440 -> "Last sync: ${minutes / 60}h ago"
                else -> {
                    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
                    "Last sync: ${formatter.format(Date(lastSync))}"
                }
            }
        }
    }
    
    private fun showError(message: String) {
        containerWorkout.visibility = GONE
        containerNoWorkout.visibility = VISIBLE
        tvNoWorkoutMessage.text = message
    }
    
    /**
     * Manually trigger a refresh of workout data
     */
    fun refreshData() {
        lifecycleOwner?.lifecycleScope?.launch {
            refreshWorkoutData()
        }
    }
}