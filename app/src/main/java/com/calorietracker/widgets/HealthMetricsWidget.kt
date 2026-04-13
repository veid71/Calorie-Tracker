package com.calorietracker.widgets

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.calorietracker.HealthConnectDebugActivity
import com.calorietracker.R
import com.calorietracker.fitness.HealthConnectManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Widget that displays daily health metrics (steps, water, activity) with sync verification
 */
class HealthMetricsWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {
    
    private lateinit var healthConnectManager: HealthConnectManager
    private var lifecycleOwner: LifecycleOwner? = null
    
    private val tvStepsCount: TextView
    private val tvWaterGlasses: TextView
    private val tvActivitySummary: TextView
    private val tvHealthStatus: TextView
    private val tvLastHealthSync: TextView
    private val btnRefreshHealth: MaterialButton
    private val btnHealthDetails: MaterialButton
    private val containerActivitySummary: LinearLayout
    
    init {
        LayoutInflater.from(context).inflate(R.layout.widget_health_metrics, this, true)
        
        tvStepsCount = findViewById(R.id.tvStepsCount)
        tvWaterGlasses = findViewById(R.id.tvWaterGlasses)
        tvActivitySummary = findViewById(R.id.tvActivitySummary)
        tvHealthStatus = findViewById(R.id.tvHealthStatus)
        tvLastHealthSync = findViewById(R.id.tvLastHealthSync)
        btnRefreshHealth = findViewById(R.id.btnRefreshHealth)
        btnHealthDetails = findViewById(R.id.btnHealthDetails)
        containerActivitySummary = findViewById(R.id.containerActivitySummary)
        
        initializeHealthConnect()
        setupClickListeners()
        updateLastSyncTime()
    }
    
    private fun initializeHealthConnect() {
        healthConnectManager = HealthConnectManager(context)
    }
    
    private fun setupClickListeners() {
        btnRefreshHealth.setOnClickListener {
            refreshHealthData()
        }
        
        btnHealthDetails.setOnClickListener {
            val intent = Intent(context, HealthConnectDebugActivity::class.java)
            context.startActivity(intent)
        }
        
        setOnClickListener {
            refreshHealthData()
        }
    }
    
    fun setLifecycleOwner(owner: LifecycleOwner) {
        lifecycleOwner = owner
        loadInitialHealthData()
    }
    
    private fun loadInitialHealthData() {
        lifecycleOwner?.let { owner ->
            owner.lifecycleScope.launch {
                refreshHealthData()
            }
        }
    }
    
    private fun refreshHealthData() {
        lifecycleOwner?.let { owner ->
            owner.lifecycleScope.launch {
                try {
                    tvHealthStatus.text = "Syncing health data..."
                    btnRefreshHealth.isEnabled = false
                    
                    // Check if Health Connect is available and has permissions
                    val isAvailable = healthConnectManager.isHealthConnectAvailable()
                    val hasPermissions = healthConnectManager.hasRequiredPermissions()
                    
                    if (!isAvailable) {
                        showUnavailableState("Health Connect not available")
                        return@launch
                    }
                    
                    if (!hasPermissions) {
                        showPermissionState()
                        return@launch
                    }
                    
                    // Get today's health data
                    val healthData = healthConnectManager.getTodaysHealthData()
                    updateHealthDisplay(healthData)
                    updateLastSyncTime()
                    
                } catch (e: Exception) {
                    showErrorState("Error syncing: ${e.message}")
                } finally {
                    btnRefreshHealth.isEnabled = true
                }
            }
        }
    }
    
    private fun updateHealthDisplay(healthData: com.calorietracker.fitness.HealthData) {
        // Update steps
        if (healthData.steps > 0) {
            tvStepsCount.text = formatNumber(healthData.steps)
        } else {
            tvStepsCount.text = "0"
        }
        
        // Update water intake
        if (healthData.hydrationMl > 0) {
            tvWaterGlasses.text = String.format("%.1f", healthData.hydrationCups)
        } else {
            tvWaterGlasses.text = "0"
        }
        
        // Update activity summary
        if (healthData.activeCaloriesBurned > 0 || healthData.exerciseMinutes > 0) {
            containerActivitySummary.visibility = View.VISIBLE
            
            val summaryText = buildString {
                if (healthData.exerciseMinutes > 0) {
                    append("Exercise: ${healthData.exerciseMinutes} minutes")
                    if (!healthData.primaryExerciseType.isNullOrBlank()) {
                        append(" (${healthData.primaryExerciseType})")
                    }
                    append("\n")
                }
                if (healthData.activeCaloriesBurned > 0) {
                    append("Active calories: ${healthData.activeCaloriesBurned}")
                    append("\n")
                }
                if (healthData.totalCaloriesBurned > 0) {
                    append("Total calories: ${healthData.totalCaloriesBurned}")
                }
            }
            tvActivitySummary.text = summaryText.trim()
        } else {
            containerActivitySummary.visibility = View.GONE
        }
        
        // Update status
        val statusText = buildString {
            val dataPoints = mutableListOf<String>()
            if (healthData.steps > 0) dataPoints.add("steps")
            if (healthData.hydrationMl > 0) dataPoints.add("water")
            if (healthData.activeCaloriesBurned > 0) dataPoints.add("workouts")
            
            if (dataPoints.isNotEmpty()) {
                append("✅ Synced ${dataPoints.joinToString(", ")}")
            } else {
                append("No health data found for today")
            }
        }
        tvHealthStatus.text = statusText
    }
    
    private fun showUnavailableState(message: String) {
        tvStepsCount.text = "--"
        tvWaterGlasses.text = "--"
        containerActivitySummary.visibility = View.GONE
        tvHealthStatus.text = message
    }
    
    private fun showPermissionState() {
        tvStepsCount.text = "--"
        tvWaterGlasses.text = "--"
        containerActivitySummary.visibility = View.GONE
        tvHealthStatus.text = "⚠️ Health Connect permissions required - tap Details to setup"
    }
    
    private fun showErrorState(message: String) {
        tvStepsCount.text = "--"
        tvWaterGlasses.text = "--"
        containerActivitySummary.visibility = View.GONE
        tvHealthStatus.text = "❌ $message"
        
        Snackbar.make(this, message, Snackbar.LENGTH_LONG).show()
    }
    
    private fun updateLastSyncTime() {
        val sharedPrefs = context.getSharedPreferences("health_metrics_prefs", Context.MODE_PRIVATE)
        val currentTime = System.currentTimeMillis()
        
        // Update sync time
        sharedPrefs.edit().putLong("last_health_sync", currentTime).apply()
        
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        tvLastHealthSync.text = "Last sync: ${formatter.format(Date(currentTime))}"
    }
    
    private fun formatNumber(number: Long): String {
        return when {
            number >= 1000000 -> String.format("%.1fM", number / 1000000.0)
            number >= 1000 -> String.format("%.1fK", number / 1000.0)
            else -> number.toString()
        }
    }
    
    /**
     * Manually trigger a refresh of health data
     */
    fun refreshData() {
        refreshHealthData()
    }
}