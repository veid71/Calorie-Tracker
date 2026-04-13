package com.calorietracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.utils.ThemeManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Debug activity for Health Connect sync verification
 */
class HealthConnectDebugActivity : AppCompatActivity() {

    private lateinit var database: CalorieDatabase
    private var healthConnectClient: HealthConnectClient? = null
    
    // UI elements
    private lateinit var tvConnectionStatus: TextView
    private lateinit var tvConnectionDetails: TextView
    private lateinit var tvPermissionsStatus: TextView
    private lateinit var tvLastSyncTime: TextView
    private lateinit var tvRecentWorkouts: TextView
    private lateinit var tvDebugLogs: TextView
    
    private lateinit var btnRefreshStatus: MaterialButton
    private lateinit var btnRequestPermissions: MaterialButton
    private lateinit var btnSyncNow: MaterialButton
    private lateinit var btnClearLogs: MaterialButton
    private lateinit var btnOpenHealthConnect: MaterialButton
    private lateinit var btnBack: MaterialButton
    private lateinit var btnAddTestWorkout: MaterialButton
    private lateinit var btnClearWorkouts: MaterialButton
    
    private val debugLogs = mutableListOf<String>()
    
    // Required permissions for Health Connect
    private val permissions = setOf(
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class)
    )
    
    // Permission request launcher
    private val requestPermissionActivityContract = 
        PermissionController.createRequestPermissionResultContract()
    
    private val requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
        addDebugLog("Permission request result: $granted")
        if (granted.containsAll(permissions)) {
            addDebugLog("✅ All permissions granted")
            lifecycleScope.launch { updatePermissionsStatus() }
            syncWorkoutData()
        } else {
            addDebugLog("❌ Some permissions denied: ${granted}")
            lifecycleScope.launch { updatePermissionsStatus() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_connect_debug)

        initDatabase()
        initViews()
        setupClickListeners()
        checkHealthConnectAvailability()
        
        addDebugLog("Health Connect Debug Activity started")
    }

    private fun initDatabase() {
        database = CalorieDatabase.getDatabase(this)
    }

    private fun initViews() {
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)
        tvConnectionDetails = findViewById(R.id.tvConnectionDetails)
        tvPermissionsStatus = findViewById(R.id.tvPermissionsStatus)
        tvLastSyncTime = findViewById(R.id.tvLastSyncTime)
        tvRecentWorkouts = findViewById(R.id.tvRecentWorkouts)
        tvDebugLogs = findViewById(R.id.tvDebugLogs)
        
        btnRefreshStatus = findViewById(R.id.btnRefreshStatus)
        btnRequestPermissions = findViewById(R.id.btnRequestPermissions)
        btnSyncNow = findViewById(R.id.btnSyncNow)
        btnClearLogs = findViewById(R.id.btnClearLogs)
        btnOpenHealthConnect = findViewById(R.id.btnOpenHealthConnect)
        btnBack = findViewById(R.id.btnBack)
        btnAddTestWorkout = findViewById(R.id.btnAddTestWorkout)
        btnClearWorkouts = findViewById(R.id.btnClearWorkouts)
    }

    private fun setupClickListeners() {
        btnRefreshStatus.setOnClickListener {
            addDebugLog("🔄 Refreshing status...")
            checkHealthConnectAvailability()
        }
        
        btnRequestPermissions.setOnClickListener {
            addDebugLog("📋 Requesting Health Connect permissions...")
            requestHealthConnectPermissions()
        }
        
        btnSyncNow.setOnClickListener {
            addDebugLog("🔄 Manual sync requested...")
            syncWorkoutData()
        }
        
        btnClearLogs.setOnClickListener {
            debugLogs.clear()
            updateDebugLogs()
        }
        
        btnOpenHealthConnect.setOnClickListener {
            openHealthConnectApp()
        }
        
        btnBack.setOnClickListener {
            finish()
        }
        
        btnAddTestWorkout.setOnClickListener {
            addTestWorkoutData()
        }
        
        btnClearWorkouts.setOnClickListener {
            clearWorkoutData()
        }
    }

    private fun checkHealthConnectAvailability() {
        lifecycleScope.launch {
            try {
                when (HealthConnectClient.getSdkStatus(this@HealthConnectDebugActivity)) {
                    HealthConnectClient.SDK_UNAVAILABLE -> {
                        tvConnectionStatus.text = "Unavailable"
                        tvConnectionDetails.text = "Health Connect is not available on this device"
                        addDebugLog("❌ Health Connect SDK unavailable")
                    }
                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                        tvConnectionStatus.text = "Update Required"
                        tvConnectionDetails.text = "Health Connect app needs to be updated"
                        addDebugLog("⚠️ Health Connect app update required")
                    }
                    HealthConnectClient.SDK_AVAILABLE -> {
                        tvConnectionStatus.text = "Available"
                        tvConnectionDetails.text = "Health Connect is available and ready"
                        addDebugLog("✅ Health Connect available")
                        
                        healthConnectClient = HealthConnectClient.getOrCreate(this@HealthConnectDebugActivity)
                        lifecycleScope.launch { updatePermissionsStatus() }
                        loadLastSyncTime()
                    }
                }
            } catch (e: Exception) {
                tvConnectionStatus.text = "Error"
                tvConnectionDetails.text = "Error checking Health Connect: ${e.message}"
                addDebugLog("❌ Error checking Health Connect: ${e.message}")
            }
        }
    }

    private suspend fun updatePermissionsStatus() {
        try {
            val client = healthConnectClient ?: return
            
            val grantedPermissions = client.permissionController.getGrantedPermissions()
            val hasAllPermissions = grantedPermissions.containsAll(permissions)
            
            if (hasAllPermissions) {
                tvPermissionsStatus.text = "✅ All permissions granted:\n" +
                    "• Active calories burned\n" +
                    "• Total calories burned\n" +
                    "• Exercise sessions\n" +
                    "• Steps\n" +
                    "• Hydration (water intake)"
                btnRequestPermissions.text = "Permissions Granted"
                btnRequestPermissions.isEnabled = false
                addDebugLog("✅ All Health Connect permissions granted")
            } else {
                val missingPermissions = permissions - grantedPermissions
                val missingPermissionNames = missingPermissions.map { permission ->
                    when {
                        permission.toString().contains("ActiveCaloriesBurnedRecord") -> "Active calories burned"
                        permission.toString().contains("TotalCaloriesBurnedRecord") -> "Total calories burned"
                        permission.toString().contains("ExerciseSessionRecord") -> "Exercise sessions"
                        permission.toString().contains("StepsRecord") -> "Steps"
                        permission.toString().contains("HydrationRecord") -> "Hydration (water intake)"
                        else -> permission.toString()
                    }
                }
                tvPermissionsStatus.text = "❌ Missing permissions:\n${missingPermissionNames.joinToString("\n") { "• $it" }}"
                btnRequestPermissions.text = "Request Permissions"
                btnRequestPermissions.isEnabled = true
                addDebugLog("⚠️ Missing permissions: $missingPermissionNames")
            }
        } catch (e: Exception) {
            tvPermissionsStatus.text = "Error checking permissions: ${e.message}"
            addDebugLog("❌ Error checking permissions: ${e.message}")
        }
    }

    private fun requestHealthConnectPermissions() {
        val client = healthConnectClient
        if (client == null) {
            addDebugLog("❌ Health Connect client not available")
            return
        }
        
        lifecycleScope.launch {
            try {
                requestPermissions.launch(permissions)
            } catch (e: Exception) {
                addDebugLog("❌ Error requesting permissions: ${e.message}")
                Snackbar.make(findViewById(android.R.id.content), "Error requesting permissions: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun syncWorkoutData() {
        val client = healthConnectClient
        if (client == null) {
            addDebugLog("❌ Health Connect client not available")
            return
        }
        
        lifecycleScope.launch {
            try {
                addDebugLog("🔄 Starting workout data sync...")
                
                val endTime = Instant.now()
                val startTime = endTime.minus(7, ChronoUnit.DAYS)
                
                addDebugLog("📅 Fetching data from ${startTime} to ${endTime}")
                
                // Read active calories burned
                val activeCaloriesRequest = ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
                val activeCaloriesResponse = client.readRecords(activeCaloriesRequest)
                addDebugLog("🔥 Found ${activeCaloriesResponse.records.size} active calorie records")
                
                // Read total calories burned
                val totalCaloriesRequest = ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
                val totalCaloriesResponse = client.readRecords(totalCaloriesRequest)
                addDebugLog("🔥 Found ${totalCaloriesResponse.records.size} total calorie records")
                
                // Read exercise sessions
                val exerciseRequest = ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
                val exerciseResponse = client.readRecords(exerciseRequest)
                addDebugLog("🏃 Found ${exerciseResponse.records.size} exercise sessions")
                
                // Read steps data
                val stepsRequest = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
                val stepsResponse = client.readRecords(stepsRequest)
                addDebugLog("👟 Found ${stepsResponse.records.size} step records")
                
                // Read hydration data
                val hydrationRequest = ReadRecordsRequest(
                    recordType = HydrationRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
                val hydrationResponse = client.readRecords(hydrationRequest)
                addDebugLog("💧 Found ${hydrationResponse.records.size} hydration records")
                
                // Display recent workout data
                displayRecentWorkouts(activeCaloriesResponse.records, totalCaloriesResponse.records, exerciseResponse.records, stepsResponse.records, hydrationResponse.records)
                
                // Update last sync time
                val currentTime = System.currentTimeMillis()
                val sharedPrefs = getSharedPreferences("health_connect_prefs", MODE_PRIVATE)
                sharedPrefs.edit().putLong("last_sync_time", currentTime).apply()
                updateLastSyncTime(currentTime)
                
                addDebugLog("✅ Sync completed successfully")
                
            } catch (e: Exception) {
                addDebugLog("❌ Error syncing workout data: ${e.message}")
                tvRecentWorkouts.text = "Error loading workout data: ${e.message}"
            }
        }
    }

    private fun displayRecentWorkouts(
        activeCalories: List<ActiveCaloriesBurnedRecord>,
        totalCalories: List<TotalCaloriesBurnedRecord>,
        exercises: List<ExerciseSessionRecord>,
        steps: List<StepsRecord>,
        hydration: List<HydrationRecord>
    ) {
        val workoutText = StringBuilder()
        
        if (exercises.isNotEmpty()) {
            workoutText.append("Recent Exercise Sessions:\n")
            exercises.take(5).forEach { exercise ->
                val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date.from(exercise.startTime))
                workoutText.append("• ${exercise.exerciseType} at $date\n")
            }
            workoutText.append("\n")
        }
        
        if (activeCalories.isNotEmpty()) {
            workoutText.append("Recent Active Calories:\n")
            activeCalories.take(5).forEach { calorie ->
                val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date.from(calorie.startTime))
                // Fix unit conversion - convert from kilojoules to calories properly
                val calories = (calorie.energy.inKilojoules * 0.239).toInt()
                workoutText.append("• ${calories} cal at $date\n")
            }
            workoutText.append("\n")
        }
        
        if (totalCalories.isNotEmpty()) {
            workoutText.append("Recent Total Calories:\n")
            totalCalories.take(5).forEach { calorie ->
                val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date.from(calorie.startTime))
                // Fix unit conversion - convert from kilojoules to calories properly
                val calories = (calorie.energy.inKilojoules * 0.239).toInt()
                workoutText.append("• ${calories} cal at $date\n")
            }
            workoutText.append("\n")
        }
        
        if (steps.isNotEmpty()) {
            workoutText.append("Recent Steps:\n")
            steps.take(5).forEach { step ->
                val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date.from(step.startTime))
                workoutText.append("• ${step.count} steps at $date\n")
            }
            workoutText.append("\n")
        }
        
        if (hydration.isNotEmpty()) {
            workoutText.append("Recent Hydration:\n")
            hydration.take(5).forEach { water ->
                val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date.from(water.startTime))
                val cups = water.volume.inMilliliters / 237.0
                val glasses = water.volume.inMilliliters / 250.0
                workoutText.append("• ${String.format("%.1f", cups)} cups (${String.format("%.1f", glasses)} glasses, ${water.volume.inMilliliters.toInt()}ml) at $date\n")
            }
            workoutText.append("\n")
        }
        
        if (workoutText.isEmpty()) {
            tvRecentWorkouts.text = "No health data found in the last 7 days.\n\nThis could mean:\n• No workouts, steps, or water intake recorded\n• Watch/fitness tracker not syncing\n• Health apps not sharing data with Health Connect\n• Health Connect permissions not properly granted"
        } else {
            tvRecentWorkouts.text = workoutText.toString()
        }
    }

    private fun loadLastSyncTime() {
        val sharedPrefs = getSharedPreferences("health_connect_prefs", MODE_PRIVATE)
        val lastSync = sharedPrefs.getLong("last_sync_time", 0)
        updateLastSyncTime(lastSync)
    }

    private fun updateLastSyncTime(timestamp: Long) {
        if (timestamp == 0L) {
            tvLastSyncTime.text = "Last sync: Never"
        } else {
            val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            tvLastSyncTime.text = "Last sync: ${formatter.format(Date(timestamp))}"
        }
    }

    private fun openHealthConnectApp() {
        try {
            val intent = Intent().apply {
                action = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            addDebugLog("📱 Opened Health Connect settings")
        } catch (e: Exception) {
            addDebugLog("❌ Could not open Health Connect: ${e.message}")
            // Fallback: try to open Health Connect app directly
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata")
                if (intent != null) {
                    startActivity(intent)
                    addDebugLog("📱 Opened Health Connect app")
                } else {
                    addDebugLog("❌ Health Connect app not found")
                    Snackbar.make(findViewById(android.R.id.content), "Health Connect app not found", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e2: Exception) {
                addDebugLog("❌ Error opening Health Connect: ${e2.message}")
                Snackbar.make(findViewById(android.R.id.content), "Cannot open Health Connect", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun addTestWorkoutData() {
        addDebugLog("🏃‍♂️ Adding test workout data...")
        lifecycleScope.launch {
            try {
                val repository = com.calorietracker.repository.CalorieRepository(database, this@HealthConnectDebugActivity)
                
                // Add test workout with 350 calories burned
                repository.addTestWorkoutData(activeCalories = 350, exerciseMinutes = 45, exerciseType = "Test Workout")
                
                addDebugLog("✅ Test workout added: 350 active calories, 45 minutes")
                addDebugLog("💡 Base calorie goal should now increase by ${(350 * 0.7).toInt()} calories")
                addDebugLog("🔄 Go back to main screen to see the calorie adjustment!")
                
            } catch (e: Exception) {
                addDebugLog("❌ Error adding test workout: ${e.message}")
            }
        }
    }
    
    private fun clearWorkoutData() {
        addDebugLog("🧹 Clearing workout data...")
        lifecycleScope.launch {
            try {
                val repository = com.calorietracker.repository.CalorieRepository(database, this@HealthConnectDebugActivity)
                
                // Clear today's workout data
                repository.clearTodaysWorkoutData()
                
                addDebugLog("✅ Workout data cleared")
                addDebugLog("💡 Calorie goal should now return to base value")
                addDebugLog("🔄 Go back to main screen to see the change!")
                
            } catch (e: Exception) {
                addDebugLog("❌ Error clearing workout data: ${e.message}")
            }
        }
    }

    private fun addDebugLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        debugLogs.add("[$timestamp] $message")
        if (debugLogs.size > 50) {
            debugLogs.removeAt(0) // Keep only last 50 logs
        }
        updateDebugLogs()
    }

    private fun updateDebugLogs() {
        tvDebugLogs.text = debugLogs.joinToString("\n")
    }
}