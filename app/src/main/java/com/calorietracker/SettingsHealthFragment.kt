package com.calorietracker

// Android framework imports
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

// Activity result handling for permission requests
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

// Health Connect API classes for fitness data integration
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.HydrationRecord

// Coroutines for background operations
import androidx.lifecycle.lifecycleScope

// App-specific database and repository classes
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.repository.CalorieRepository

// Material Design UI components
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

// Kotlin coroutines
import kotlinx.coroutines.launch

/**
 * EDUCATIONAL OVERVIEW: Settings Health Fragment
 * 
 * PURPOSE OF THIS FRAGMENT:
 * - Integrates with Android's Health Connect API
 * - Manages permissions for reading fitness data from wearables
 * - Syncs workout calories and exercise data from connected devices
 * - Provides user interface for health data management
 * 
 * HEALTH CONNECT EXPLAINED:
 * - Android's centralized health data platform (introduced in Android 13)
 * - Allows apps to securely share health and fitness data
 * - Users control which apps can read/write specific types of data
 * - Replaces Google Fit for modern Android health data integration
 * 
 * PERMISSION HANDLING COMPLEXITY:
 * - Health data requires explicit user consent for each data type
 * - Uses ActivityResultContracts for modern permission request flow
 * - Must handle permission granted/denied scenarios gracefully
 * - Different from regular Android permissions (uses Health Connect system)
 * 
 * API INTEGRATION PATTERNS:
 * - External API integration with error handling
 * - Background operations for data synchronization
 * - Real-time status updates based on connection state
 */
class SettingsHealthFragment : Fragment() {
    
    /**
     * UI ELEMENTS FOR HEALTH CONNECT STATUS DISPLAY
     * 
     * STATUS INDICATOR PATTERN:
     * - Visual feedback showing current connection state
     * - Status text explains what the current state means
     * - Indicator uses color-coded emojis for quick visual reference
     * - Buttons enable/disable based on current state
     */
    private lateinit var tvHealthConnectStatus: TextView        // Text description of status
    private lateinit var tvHealthConnectIndicator: TextView    // Color-coded emoji indicator
    private lateinit var btnSetupHealthConnect: MaterialButton // Permission request button
    private lateinit var btnSyncWorkoutData: MaterialButton    // Manual data sync button
    private lateinit var btnHealthConnectDebug: MaterialButton // Debug information button
    
    /**
     * DATA ACCESS AND API CLIENT OBJECTS
     * 
     * REPOSITORY PATTERN USAGE:
     * - Same CalorieRepository used across all fragments
     * - Provides consistent interface for database operations
     * - Handles both local database and Health Connect integration
     * 
     * NULLABLE CLIENT REFERENCE:
     * - HealthConnectClient may not be available on all devices
     * - Nullable type forces null-checks before using client
     * - Graceful degradation when Health Connect isn't supported
     */
    private lateinit var repository: CalorieRepository
    private var healthConnectClient: HealthConnectClient? = null
    
    /**
     * HEALTH CONNECT PERMISSIONS SETUP
     * 
     * GRANULAR PERMISSION MODEL:
     * - Each data type requires separate permission (security by design)
     * - Read permissions allow app to retrieve data from Health Connect
     * - Write permissions would allow app to store data (not needed here)
     * 
     * DATA TYPES EXPLAINED:
     * - ActiveCaloriesBurnedRecord: Calories from physical activity
     * - TotalCaloriesBurnedRecord: All calories burned (active + resting)
     * - ExerciseSessionRecord: Workout sessions with duration/type
     * - StepsRecord: Step count data from devices
     * - HydrationRecord: Water intake tracking (future feature)
     */
    private val permissions = setOf(
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),  // Workout calories
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),   // Total daily burn
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),       // Exercise details
        HealthPermission.getReadPermission(StepsRecord::class),                 // Step count
        HealthPermission.getReadPermission(HydrationRecord::class)              // Water intake
    )
    
    /**
     * MODERN PERMISSION REQUEST HANDLING
     * 
     * ACTIVITY RESULT CONTRACTS EXPLAINED:
     * - Modern replacement for deprecated startActivityForResult()
     * - Type-safe permission request handling
     * - Automatically handles Activity lifecycle issues
     * - More reliable than old permission request methods
     * 
     * PERMISSION CONTRACT CREATION:
     * - PermissionController.createRequestPermissionResultContract() creates Health Connect specific contract
     * - Different from regular Android permission contracts
     * - Handles Health Connect's custom permission flow
     */
    private val requestPermissionActivityContract = 
        PermissionController.createRequestPermissionResultContract()
    
    /**
     * PERMISSION RESULT CALLBACK
     * 
     * LAMBDA FUNCTION AS CALLBACK:
     * - registerForActivityResult() sets up callback for permission results
     * - Lambda { granted -> } receives Set<HealthPermission> of granted permissions
     * - Must check if all required permissions were granted
     * 
     * PERMISSION RESULT HANDLING:
     * - granted.containsAll(permissions) checks if user granted everything
     * - Set operations (permissions - granted) find missing permissions
     * - UI feedback via Snackbar informs user of results
     * - Always call checkHealthConnectStatus() to update UI state
     */
    private val requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
        if (granted.containsAll(permissions)) {
            // Success: All permissions granted
            Snackbar.make(requireView(), "All Health Connect permissions granted!", Snackbar.LENGTH_LONG).show()
            checkHealthConnectStatus() // Update UI to reflect new permissions
        } else {
            // Partial failure: Some permissions denied
            val missingPermissions = permissions - granted  // Calculate what's missing
            val missingCount = missingPermissions.size
            Snackbar.make(requireView(), "Missing $missingCount permissions. Please try again.", Snackbar.LENGTH_LONG).show()
            checkHealthConnectStatus() // Update UI to show current state
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings_health, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        /**
         * API CLIENT INITIALIZATION
         * 
         * HEALTH CONNECT CLIENT CREATION:
         * - getOrCreate() attempts to create Health Connect client
         * - Returns null if Health Connect not available on device
         * - Null check required before using client methods
         * - Graceful degradation pattern for unsupported devices
         */
        // Initialize repository for data operations
        val database = CalorieDatabase.getDatabase(requireContext())
        repository = CalorieRepository(database, requireContext())
        
        // Initialize Health Connect client (may be null if not supported)
        healthConnectClient = HealthConnectClient.getOrCreate(requireContext())
        
        // Standard fragment initialization
        initViews(view)
        setupButtons()
        checkHealthConnectStatus() // Check initial state and update UI
    }
    
    private fun initViews(view: View) {
        tvHealthConnectStatus = view.findViewById(R.id.tvHealthConnectStatus)
        tvHealthConnectIndicator = view.findViewById(R.id.tvHealthConnectIndicator)
        btnSetupHealthConnect = view.findViewById(R.id.btnSetupHealthConnect)
        btnSyncWorkoutData = view.findViewById(R.id.btnSyncWorkoutData)
        btnHealthConnectDebug = view.findViewById(R.id.btnHealthConnectDebug)
    }
    
    private fun setupButtons() {
        btnSetupHealthConnect.setOnClickListener {
            setupHealthConnect()
        }
        
        btnSyncWorkoutData.setOnClickListener {
            syncWorkoutData()
        }
        
        btnHealthConnectDebug.setOnClickListener {
            openHealthConnectDebug()
        }
    }
    
    private fun checkHealthConnectStatus() {
        lifecycleScope.launch {
            try {
                val isAvailable = repository.isHealthConnectAvailable()
                val hasPermissions = repository.hasHealthConnectPermissions()
                
                when {
                    !isAvailable -> {
                        tvHealthConnectStatus.text = "Health Connect not available"
                        tvHealthConnectIndicator.text = "🔴"
                        btnSyncWorkoutData.isEnabled = false
                        btnSetupHealthConnect.isEnabled = false
                    }
                    !hasPermissions -> {
                        tvHealthConnectStatus.text = "Permissions required"
                        tvHealthConnectIndicator.text = "🟡"
                        btnSyncWorkoutData.isEnabled = false
                        btnSetupHealthConnect.isEnabled = true
                    }
                    else -> {
                        tvHealthConnectStatus.text = "Connected"
                        tvHealthConnectIndicator.text = "🟢"
                        btnSyncWorkoutData.isEnabled = true
                        btnSetupHealthConnect.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                tvHealthConnectStatus.text = "Error checking status"
                tvHealthConnectIndicator.text = "🔴"
                btnSyncWorkoutData.isEnabled = false
            }
        }
    }
    
    /**
     * PERMISSION SETUP AND REQUEST FLOW
     * 
     * BACKGROUND OPERATIONS FOR API CALLS:
     * - Health Connect permission checks must run on background thread
     * - lifecycleScope.launch handles coroutine lifecycle automatically
     * - Multiple early returns handle different error conditions gracefully
     * 
     * PERMISSION OPTIMIZATION:
     * - Check existing permissions before requesting new ones
     * - Only request missing permissions (better UX than requesting all)
     * - Set operations efficiently calculate what's missing
     * 
     * ERROR HANDLING PATTERNS:
     * - Try-catch blocks prevent app crashes from API failures
     * - User-friendly error messages via Snackbar
     * - Graceful degradation when Health Connect unavailable
     */
    private fun setupHealthConnect() {
        // Permission setup must run in background (API calls involved)
        lifecycleScope.launch {
            try {
                // First check: Is Health Connect available on this device?
                val isAvailable = repository.isHealthConnectAvailable()
                if (!isAvailable) {
                    Snackbar.make(requireView(), "Health Connect is not available on this device", Snackbar.LENGTH_LONG).show()
                    return@launch // Early exit for unsupported devices
                }
                
                // Second check: Do we have a valid client instance?
                val client = healthConnectClient
                if (client == null) {
                    Snackbar.make(requireView(), "Health Connect client not available", Snackbar.LENGTH_LONG).show()
                    return@launch // Early exit if client creation failed
                }
                
                /**
                 * SMART PERMISSION CHECKING
                 * 
                 * WHY CHECK EXISTING PERMISSIONS?
                 * - Avoids redundant permission requests (better user experience)
                 * - getGrantedPermissions() queries current permission state
                 * - Set subtraction (permissions - grantedPermissions) finds gaps
                 * - Only request what we actually need
                 */
                // Check which permissions we already have
                val grantedPermissions = client.permissionController.getGrantedPermissions()
                val missingPermissions = permissions - grantedPermissions
                
                if (missingPermissions.isEmpty()) {
                    // All permissions already granted, no need to request
                    Snackbar.make(requireView(), "All permissions already granted!", Snackbar.LENGTH_SHORT).show()
                    checkHealthConnectStatus() // Update UI to reflect current state
                    return@launch
                }
                
                /**
                 * PERMISSION REQUEST LAUNCH
                 * 
                 * MODERN PERMISSION REQUEST:
                 * - requestPermissions.launch() triggers the permission request flow
                 * - Uses ActivityResultContract registered in class initialization
                 * - System handles showing permission dialogs to user
                 * - Result handled by callback registered earlier
                 */
                // Request the missing permissions
                try {
                    requestPermissions.launch(permissions) // Launch permission request UI
                } catch (e: Exception) {
                    Snackbar.make(requireView(), "Error requesting permissions: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                // Catch-all for any unexpected API errors
                Snackbar.make(requireView(), "Error setting up Health Connect: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * BACKGROUND DATA SYNCHRONIZATION
     * 
     * UI STATE MANAGEMENT DURING ASYNC OPERATIONS:
     * - Disable button to prevent multiple simultaneous syncs
     * - Change button text to show operation in progress
     * - Use finally block to restore UI state regardless of success/failure
     * 
     * REPOSITORY ABSTRACTION BENEFITS:
     * - syncTodaysWorkoutData() hides complex Health Connect API calls
     * - Repository handles data transformation and local storage
     * - Fragment only needs to handle UI and user feedback
     * 
     * USER FEEDBACK PATTERNS:
     * - Different messages for success, failure, and no data scenarios
     * - Detailed success messages show actual synced data
     * - Error messages guide user toward solution (check permissions)
     */
    private fun syncWorkoutData() {
        // Background operation for API calls and database updates
        lifecycleScope.launch {
            try {
                /**
                 * UI STATE MANAGEMENT DURING OPERATION
                 * 
                 * PREVENT MULTIPLE SIMULTANEOUS OPERATIONS:
                 * - Disable button to prevent user clicking multiple times
                 * - Visual feedback via changed button text
                 * - finally block ensures UI state gets restored
                 */
                btnSyncWorkoutData.isEnabled = false
                btnSyncWorkoutData.text = "Syncing..."
                
                // Delegate actual sync work to repository layer
                val success = repository.syncTodaysWorkoutData()
                
                if (success) {
                    /**
                     * SUCCESS SCENARIO WITH DATA VERIFICATION
                     * 
                     * WHY VERIFY DATA AFTER SYNC?
                     * - Confirm that sync actually retrieved meaningful data
                     * - Provide specific feedback about what was synced
                     * - Handle case where sync succeeds but no data exists
                     */
                    val workoutData = repository.getTodaysWorkoutCalories()
                    val message = if (workoutData != null && workoutData.activeCaloriesBurned > 0) {
                        "Sync successful! Active calories: ${workoutData.activeCaloriesBurned}, Exercise: ${workoutData.exerciseMinutes} min"
                    } else {
                        "Sync completed - no workout data found for today"
                    }
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
                } else {
                    // Sync failed - guide user toward likely solution
                    Snackbar.make(requireView(), "Sync failed. Check Health Connect permissions.", Snackbar.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                // Handle unexpected errors with detailed error message
                Snackbar.make(requireView(), "Sync error: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                /**
                 * GUARANTEED UI STATE RESTORATION
                 * 
                 * FINALLY BLOCK IMPORTANCE:
                 * - Executes regardless of success, failure, or exception
                 * - Prevents button from staying disabled if error occurs
                 * - Ensures consistent UI state after operation completes
                 */
                btnSyncWorkoutData.isEnabled = true
                btnSyncWorkoutData.text = "Sync Workout Data"
                
                // Refresh status to show updated connection state
                checkHealthConnectStatus()
            }
        }
    }
    
    private fun openHealthConnectDebug() {
        val intent = Intent(requireContext(), HealthConnectDebugActivity::class.java)
        startActivity(intent)
    }
    
    fun saveSettings() {
        // Health Connect settings are managed automatically
    }
}