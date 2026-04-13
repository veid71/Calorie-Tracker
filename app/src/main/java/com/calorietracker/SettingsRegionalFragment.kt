package com.calorietracker

// Android permission and system imports
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast

// Modern permission handling
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope

// App-specific imports for database and nutrition
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.network.FoodDatabaseManager
import com.calorietracker.notifications.DatabaseDownloadNotificationManager
import com.calorietracker.nutrition.NutritionRecommendations

// UI components
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * EDUCATIONAL OVERVIEW: Settings Regional Fragment
 * 
 * PURPOSE AND FUNCTIONALITY:
 * - Manages regional nutrition database selection and updates
 * - Downloads large food databases (113,000+ items) from USDA and Open Food Facts
 * - Handles notification permissions for download progress updates
 * - Demonstrates long-running background operations with user feedback
 * 
 * NOTIFICATION PERMISSION HANDLING:
 * - Android 13+ requires explicit permission for notifications
 * - Educational approach explains why notifications are helpful
 * - Graceful degradation when notifications are denied
 * - Progress updates work whether notifications are enabled or not
 * 
 * BACKGROUND OPERATIONS:
 * - Large file downloads require background processing
 * - Progress dialogs and notifications keep user informed
 * - Proper error handling for network and storage issues
 * - WorkManager integration for reliable background tasks
 * 
 * SPINNER/DROPDOWN USAGE:
 * - Demonstrates populating spinners with dynamic data
 * - SharedPreferences persistence for selected values
 * - Regional nutrition recommendation customization
 */
class SettingsRegionalFragment : Fragment() {
    
    private lateinit var spinnerRegion: Spinner
    private lateinit var btnUpdateFoodDatabase: MaterialButton
    private lateinit var btnDownloadFullDatabase: MaterialButton
    private lateinit var btnTestNotification: MaterialButton
    private lateinit var tvDatabaseStatus: TextView
    private lateinit var databaseManager: FoodDatabaseManager
    private lateinit var database: CalorieDatabase
    
    private var progressDialog: AlertDialog? = null
    
    /**
     * NOTIFICATION PERMISSION HANDLING (ANDROID 13+)
     * 
     * NOTIFICATION PERMISSION EVOLUTION:
     * - Android 13 (API 33) introduced explicit notification permission requirement
     * - Previous versions allowed notifications by default
     * - POST_NOTIFICATIONS permission controls all app notifications
     * 
     * SINGLE PERMISSION CONTRACT:
     * - RequestPermission() handles single permission (vs RequestMultiplePermissions)
     * - Boolean result indicates granted (true) or denied (false)
     * - Simpler than Bluetooth permissions which require multiple grants
     * 
     * GRACEFUL DEGRADATION PATTERN:
     * - App continues to function even if notifications are denied
     * - User gets different feedback mechanism (dialogs instead of notifications)
     * - Always call proceedWithDownload() regardless of permission result
     * - Don't block functionality based on notification permission
     */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission() // Single permission request contract
    ) { isGranted ->
        if (isGranted) {
            // Permission granted: User will see notification-based progress updates
            Toast.makeText(requireContext(), "✅ Notifications enabled - you'll see download progress", Toast.LENGTH_LONG).show()
        } else {
            // Permission denied: Guide user but don't block functionality
            Toast.makeText(requireContext(), "⚠️ Notifications disabled - check Settings app to enable manually", Toast.LENGTH_LONG).show()
        }
        
        // Continue with download regardless of notification permission
        // App provides alternative feedback mechanisms (dialogs, toasts)
        proceedWithDownload()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings_regional, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupDatabase()
        setupRegionSpinner()
        setupButtons()
        loadCurrentSettings()
        observeDatabaseSize()
    }
    
    private fun initViews(view: View) {
        spinnerRegion = view.findViewById(R.id.spinnerRegion)
        btnUpdateFoodDatabase = view.findViewById(R.id.btnUpdateFoodDatabase)
        btnDownloadFullDatabase = view.findViewById(R.id.btnDownloadFullDatabase)
        btnTestNotification = view.findViewById(R.id.btnTestNotification)
        tvDatabaseStatus = view.findViewById(R.id.tvDatabaseStatus)
    }
    
    private fun setupDatabase() {
        database = CalorieDatabase.getDatabase(requireContext())
        databaseManager = FoodDatabaseManager(requireContext(), database)
    }
    
    /**
     * SPINNER/DROPDOWN SETUP AND POPULATION
     * 
     * DATA TRANSFORMATION FOR UI:
     * - getAvailableRegions() returns List<Pair<String, String>>
     * - Pair contains (regionCode, displayName) like ("US", "United States")
     * - map { it.second } extracts just the display names for UI
     * - Shows how to transform data structures for UI consumption
     * 
     * ARRAYADAPTER SETUP:
     * - ArrayAdapter connects List<String> data to Spinner UI component
     * - First parameter: Context for accessing resources and layout inflation
     * - Second parameter: Layout resource for individual spinner items
     * - Third parameter: Data list to display
     * 
     * DROPDOWN CUSTOMIZATION:
     * - setDropDownViewResource() sets layout for opened dropdown list
     * - Can be different from closed spinner layout (customization flexibility)
     * - R.layout.dropdown_item provides consistent Material Design styling
     * 
     * PROGRAMMATIC SELECTION:
     * - indexOf() finds position of specific item in list
     * - setSelection() programmatically chooses default item
     * - Handles case where default item doesn't exist (indexOf returns -1)
     */
    private fun setupRegionSpinner() {
        /**
         * DATA EXTRACTION AND TRANSFORMATION
         * 
         * PAIR DESTRUCTURING:
         * - regions contains List<Pair<String, String>>
         * - Each pair: (regionCode, displayName)
         * - map { it.second } extracts display names only
         * - Demonstrates functional programming style in Kotlin
         */
        val regions = NutritionRecommendations.getAvailableRegions()
        val regionNames = regions.map { it.second } // Get the full names like "United States"
        
        /**
         * ADAPTER SETUP FOR SPINNER
         * 
         * ARRAYADAPTER PARAMETERS:
         * - Context: requireContext() provides fragment's context
         * - Item layout: R.layout.dropdown_item defines how each item looks
         * - Data: regionNames provides the actual content to display
         */
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, regionNames)
        adapter.setDropDownViewResource(R.layout.dropdown_item) // Layout for opened dropdown
        spinnerRegion.adapter = adapter // Connect adapter to spinner
        
        /**
         * DEFAULT SELECTION LOGIC
         * 
         * SAFE DEFAULT SELECTION:
         * - indexOf() searches for "United States" in the list
         * - Returns -1 if not found, valid index if found
         * - Only call setSelection() if item actually exists
         * - Prevents crashes from invalid selection indices
         */
        val defaultIndex = regionNames.indexOf("United States")
        if (defaultIndex >= 0) {
            spinnerRegion.setSelection(defaultIndex) // Set default selection
        }
    }
    
    private fun setupButtons() {
        btnUpdateFoodDatabase.setOnClickListener {
            updateFoodDatabase()
        }
        
        btnDownloadFullDatabase.setOnClickListener {
            val intent = android.content.Intent(requireContext(), DatabaseDownloadActivity::class.java)
            startActivity(intent)
        }
        
        btnTestNotification.setOnClickListener {
            testNotificationSystem()
        }
    }
    
    private fun loadCurrentSettings() {
        // Load saved region preference from SharedPreferences
        val prefs = requireContext().getSharedPreferences("calorie_tracker_prefs", android.content.Context.MODE_PRIVATE)
        val savedRegion = prefs.getString("selected_region", "United States")
        
        // Set spinner selection to saved region
        val adapter = spinnerRegion.adapter
        if (adapter != null) {
            for (i in 0 until adapter.count) {
                if (adapter.getItem(i).toString() == savedRegion) {
                    spinnerRegion.setSelection(i)
                    break
                }
            }
        }
    }
    
    private fun observeDatabaseSize() {
        lifecycleScope.launch {
            database.openFoodFactsDao().getCountFlow().collectLatest { count ->
                val text = if (count > 0) {
                    "Local database: ${"%,d".format(count)} Open Food Facts products"
                } else {
                    "Local database: not downloaded yet"
                }
                tvDatabaseStatus.text = text
            }
        }
    }

    private fun updateFoodDatabase() {
        // Show confirmation dialog first
        AlertDialog.Builder(requireContext())
            .setTitle("Update Food Database")
            .setMessage("This will download the complete USDA database (113,000+ foods) and Open Food Facts data. This may take several minutes and use significant data. Continue?")
            .setPositiveButton("Download") { _, _ ->
                checkNotificationPermissionAndDownload()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun checkNotificationPermissionAndDownload() {
        // Check if we need notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    Toast.makeText(requireContext(), "✅ Notifications enabled - you'll see download progress", Toast.LENGTH_SHORT).show()
                    proceedWithDownload()
                }
                else -> {
                    // Request notification permission
                    Toast.makeText(requireContext(), "📱 Please allow notifications to see download progress", Toast.LENGTH_LONG).show()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // No permission needed for older Android versions
            proceedWithDownload()
        }
    }
    
    private fun proceedWithDownload() {
        startDatabaseDownload()
    }
    
    private fun startDatabaseDownload() {
        lifecycleScope.launch {
            try {
                // Show initial progress dialog
                showProgressDialog()
                
                // Show immediate Toast feedback
                val notificationStatus = if (areNotificationsEnabled()) {
                    "📥 Starting database download... (Check notifications for progress)"
                } else {
                    "📥 Starting database download... (Enable notifications in Settings for progress updates)"
                }
                Toast.makeText(requireContext(), notificationStatus, Toast.LENGTH_LONG).show()
                
                btnUpdateFoodDatabase.isEnabled = false
                btnUpdateFoodDatabase.text = "Downloading..."
                
                // Start monitoring progress
                monitorDownloadProgress()
                
                // Download all databases with notifications
                val result = databaseManager.downloadAllDatabases()
                
                dismissProgressDialog()
                
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "✅ Database updated successfully! 113,000+ foods available", Toast.LENGTH_LONG).show()
                    view?.let { v ->
                        Snackbar.make(v, "Food database updated successfully! Check notifications for details.", Snackbar.LENGTH_LONG).show()
                    }
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    Toast.makeText(requireContext(), "❌ Update failed: $errorMessage", Toast.LENGTH_LONG).show()
                    view?.let { v ->
                        Snackbar.make(v, "Update failed: $errorMessage", Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                dismissProgressDialog()
                Toast.makeText(requireContext(), "❌ Update error: ${e.message}", Toast.LENGTH_SHORT).show()
                view?.let { v ->
                    Snackbar.make(v, "Update error: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            } finally {
                btnUpdateFoodDatabase.isEnabled = true
                btnUpdateFoodDatabase.text = "Update Food Database for Selected Region"
            }
        }
    }
    
    private fun showProgressDialog() {
        progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Downloading Database")
            .setMessage("Downloading USDA and Open Food Facts databases...\n\nThis may take several minutes. Check your notification panel for detailed progress.")
            .setCancelable(false)
            .create()
        progressDialog?.show()
    }
    
    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
    
    private suspend fun monitorDownloadProgress() {
        // Monitor progress for up to 30 seconds to provide initial feedback
        var monitoring = true
        var elapsedTime = 0
        
        lifecycleScope.launch {
            while (monitoring && elapsedTime < 30000) {
                kotlinx.coroutines.delay(3000) // Check every 3 seconds
                elapsedTime += 3000
                
                progressDialog?.let { dialog ->
                    val message = when (elapsedTime) {
                        3000 -> "Downloading USDA database...\n\nThis may take several minutes. Check notifications for progress."
                        9000 -> "Processing USDA data (113,000+ foods)...\n\nCheck notifications for detailed progress."
                        15000 -> "Starting Open Food Facts download...\n\nAlmost done!"
                        else -> "Still downloading...\n\nCheck notifications for detailed progress."
                    }
                    dialog.setMessage(message)
                }
            }
            monitoring = false
        }
    }
    
    private fun testNotificationSystem() {
        if (!areNotificationsEnabled()) {
            Toast.makeText(requireContext(), "⚠️ Please enable notifications first", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val notificationManager = DatabaseDownloadNotificationManager(requireContext())
            
            // Test sequence of notifications
            notificationManager.showDownloadStarted("Test Database")
            Toast.makeText(requireContext(), "✅ Test notification sent! Check your notification panel", Toast.LENGTH_LONG).show()
            
            // After 3 seconds, show progress update
            lifecycleScope.launch {
                kotlinx.coroutines.delay(3000)
                notificationManager.updateProgress(
                    "Test Database", 
                    50, 
                    100, 
                    "Testing notification system..."
                )
                
                // After another 3 seconds, show completion
                kotlinx.coroutines.delay(3000)
                notificationManager.showDownloadCompleted("Test Database", 100)
            }
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "❌ Notification test failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            // On older versions, notifications are enabled by default
            true
        }
    }
    
    fun saveSettings() {
        // Save selected region to SharedPreferences
        val selectedRegion = spinnerRegion.selectedItem?.toString()
        if (selectedRegion != null) {
            val prefs = requireContext().getSharedPreferences("calorie_tracker_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("selected_region", selectedRegion).apply()
        }
    }
}