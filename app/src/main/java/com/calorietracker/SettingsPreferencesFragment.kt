package com.calorietracker

// Android framework imports for basic functionality
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope

// App-specific utility classes
import com.calorietracker.backup.BackupRestoreManager
import com.calorietracker.utils.ThemeManager

// Material Design components
import com.google.android.material.button.MaterialButton

// Coroutines
import kotlinx.coroutines.launch
import java.io.File

/**
 * EDUCATIONAL OVERVIEW: Settings Preferences Fragment
 * 
 * PURPOSE OF THIS FRAGMENT:
 * - Manages app-wide preference settings that affect user experience
 * - Handles theme switching (dark mode vs light mode)
 * - Controls measurement units (metric vs imperial)
 * - Manages feature toggles (nutrition tips, notifications, etc.)
 * 
 * DATA STORAGE APPROACH:
 * - Uses SharedPreferences for lightweight key-value storage
 * - Perfect for simple settings that need to persist across app sessions
 * - Alternative to database storage for non-complex preference data
 * 
 * EVENT HANDLING PATTERN:
 * - Switch components use OnCheckedChangeListener for toggle events
 * - Immediate feedback to user via Toast messages
 * - Real-time application of settings (no save button needed)
 */
class SettingsPreferencesFragment : Fragment() {
    
    /**
     * UI ELEMENT DECLARATIONS FOR PREFERENCE CONTROLS
     * 
     * SWITCH COMPONENTS EXPLAINED:
     * - Switch is a toggle UI element (on/off, enabled/disabled)
     * - Better user experience than checkboxes for binary preferences
     * - Provides clear visual feedback about current state
     * - Material Design switch follows platform conventions
     * 
     * BUTTON VS AUTOMATIC SAVING:
     * - Some preferences save automatically (switches with listeners)
     * - Reset button provides bulk action to restore all defaults
     * - Gives users easy way to undo all customizations
     */
    private lateinit var switchDarkMode: Switch          // Toggle for dark/light theme
    private lateinit var switchMetricUnits: Switch      // Toggle metric vs imperial units
    private lateinit var switchNutritionTips: Switch    // Toggle helpful nutrition advice
    private lateinit var btnResetDefaults: MaterialButton // Reset all preferences to defaults
    
    // Backup/Restore UI elements
    private lateinit var btnExportData: MaterialButton   // Export all user data to JSON
    private lateinit var btnImportData: MaterialButton   // Import data from backup file
    private lateinit var btnAboutCredits: MaterialButton // About & Credits (OFFs CC BY-SA attribution)
    private lateinit var progressBackup: ProgressBar     // Progress indicator for operations
    private lateinit var textBackupStatus: TextView      // Status text for operations
    
    // Backup/Restore manager
    private lateinit var backupManager: BackupRestoreManager
    
    /**
     * STANDARD FRAGMENT LIFECYCLE METHODS
     * See SettingsGoalsFragment for detailed explanations of these lifecycle methods
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the preferences layout into View objects
        return inflater.inflate(R.layout.fragment_settings_preferences, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize backup manager
        backupManager = BackupRestoreManager(requireContext())
        
        // Initialize UI components and set up their behavior
        initViews(view)        // Connect UI elements to code variables
        setupSwitches()        // Configure switch event handlers
        setupButtons()         // Configure button click handlers  
        loadCurrentSettings()  // Load saved preferences to populate UI
    }
    
    /**
     * UI BINDING: Basic findViewById() Operations
     * (See SettingsGoalsFragment for detailed findViewById() explanation)
     */
    private fun initViews(view: View) {
        switchDarkMode = view.findViewById(R.id.switchDarkMode)
        switchMetricUnits = view.findViewById(R.id.switchMetricUnits)
        switchNutritionTips = view.findViewById(R.id.switchNutritionTips)
        btnResetDefaults = view.findViewById(R.id.btnResetDefaults)
        
        // Backup/Restore UI elements
        btnExportData = view.findViewById(R.id.btnExportData)
        btnImportData = view.findViewById(R.id.btnImportData)
        btnAboutCredits = view.findViewById(R.id.btnAboutCredits)
        progressBackup = view.findViewById(R.id.progressBackup)
        textBackupStatus = view.findViewById(R.id.textBackupStatus)
    }
    
    /**
     * EVENT HANDLING: Setting up Switch Listeners
     * 
     * WHAT IS SharedPreferences?
     * - Android's built-in key-value storage system
     * - Perfect for simple settings that need to persist across app sessions
     * - Stored as XML files in app's private directory
     * - Much lighter weight than database for simple preferences
     * 
     * MODE_PRIVATE EXPLAINED:
     * - Only this app can access these preferences (security)
     * - Alternative modes like MODE_WORLD_READABLE are deprecated for security
     * - Each app gets its own private preferences storage area
     * 
     * EVENT LISTENER PATTERN:
     * - setOnCheckedChangeListener() registers callback function
     * - Lambda syntax { _, isChecked -> } creates anonymous function
     * - First parameter (_) is the view that triggered the event (ignored)
     * - Second parameter (isChecked) is the new state (true/false)
     */
    private fun setupSwitches() {
        // Get SharedPreferences instance for persistent storage
        val prefs = requireContext().getSharedPreferences("calorie_tracker_prefs", Context.MODE_PRIVATE)
        
        /**
         * DARK MODE TOGGLE IMPLEMENTATION
         * 
         * COMPLEX THEME HANDLING:
         * - ThemeManager utility class handles system vs manual theme detection
         * - Some users may have "Follow System" setting enabled
         * - When they manually toggle, we switch to manual mode
         * - Real-time theme application without requiring app restart
         */
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // Check if user was previously following system theme
            if (ThemeManager.isFollowingSystem(requireContext())) {
                Toast.makeText(requireContext(), 
                    "Switching to manual theme control", 
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            // Apply theme change immediately
            ThemeManager.setDarkMode(requireContext(), isChecked)
            
            // Provide user feedback about the change
            Toast.makeText(requireContext(), 
                if (isChecked) "Dark mode enabled" else "Light mode enabled", 
                Toast.LENGTH_SHORT
            ).show()
        }
        
        /**
         * METRIC UNITS TOGGLE
         * 
         * SIMPLE PREFERENCE STORAGE PATTERN:
         * - Store boolean value directly in SharedPreferences
         * - edit() returns Editor object for making changes
         * - putBoolean() stores the key-value pair
         * - apply() commits changes asynchronously (non-blocking)
         * - Alternative: commit() is synchronous but can block UI thread
         */
        switchMetricUnits.setOnCheckedChangeListener { _, isChecked ->
            // Save preference immediately when changed
            prefs.edit().putBoolean("metric_units", isChecked).apply()
            
            // Inform user what units will be used throughout the app
            Toast.makeText(requireContext(),
                if (isChecked) "Using metric units (kg, cm)" else "Using imperial units (lbs, inches)",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        /**
         * NUTRITION TIPS TOGGLE
         * 
         * FEATURE FLAG PATTERN:
         * - This boolean controls whether helpful nutrition advice appears
         * - Other parts of the app check this preference before showing tips
         * - Allows users to customize their experience based on expertise level
         * - Immediate feedback confirms the setting change
         */
        switchNutritionTips.setOnCheckedChangeListener { _, isChecked ->
            // Store the user's preference for nutrition tips
            prefs.edit().putBoolean("nutrition_tips", isChecked).apply()
            
            // Confirm the change to the user
            Toast.makeText(requireContext(),
                if (isChecked) "Nutrition tips enabled" else "Nutrition tips disabled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * BUTTON EVENT HANDLING
     * 
     * SIMPLE CLICK LISTENER PATTERN:
     * - setOnClickListener() registers a callback for button press
     * - Lambda syntax creates inline function to handle the click
     * - In this case, delegates to resetToDefaults() method
     */
    private fun setupButtons() {
        btnResetDefaults.setOnClickListener {
            resetToDefaults() // Call method to restore all default settings
        }
        
        // Export data button
        btnExportData.setOnClickListener {
            exportUserData()
        }
        
        // Import data button
        btnImportData.setOnClickListener {
            showImportDialog()
        }

        // About & Credits — required CC BY-SA 4.0 attribution for Open Food Facts data
        btnAboutCredits.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("About CalorieTracker")
            .setMessage(
                "Data Sources & Credits:\n\n" +
                "• Open Food Facts\n" +
                "  openfoodfacts.org\n" +
                "  Data licensed under CC BY-SA 4.0\n\n" +
                "• USDA FoodData Central\n" +
                "  fdc.nal.usda.gov\n" +
                "  Public domain (U.S. government)\n\n" +
                "• Edamam Food Database\n" +
                "  developer.edamam.com\n\n" +
                "• Nutritionix\n" +
                "  nutritionix.com\n\n" +
                "Health data synced via Android Health Connect."
            )
            .setPositiveButton("OK", null)
            .show()
    }
    
    /**
     * DATA LOADING: Restoring Saved Preferences
     * 
     * WHY LOAD SETTINGS ON STARTUP?
     * - UI should reflect current preference states when fragment opens
     * - User expects switches to show correct on/off positions
     * - Prevents confusion about what settings are currently active
     * 
     * DEFAULT VALUE PATTERN:
     * - getBoolean(key, defaultValue) returns saved value or fallback
     * - Handles case where preference was never set before
     * - Default values should match app's expected initial behavior
     */
    private fun loadCurrentSettings() {
        // Access the same SharedPreferences used for saving
        val prefs = requireContext().getSharedPreferences("calorie_tracker_prefs", Context.MODE_PRIVATE)
        
        // Load each preference and set corresponding UI element
        switchDarkMode.isChecked = ThemeManager.isDarkModeEnabled(requireContext())      // Complex theme logic
        switchMetricUnits.isChecked = prefs.getBoolean("metric_units", false)           // Default: imperial
        switchNutritionTips.isChecked = prefs.getBoolean("nutrition_tips", true)        // Default: tips enabled
    }
    
    /**
     * BULK RESET FUNCTIONALITY
     * 
     * CLEARING ALL PREFERENCES:
     * - clear() removes all key-value pairs from SharedPreferences
     * - apply() commits the changes (empty preferences file)
     * - UI must be manually updated to reflect the reset state
     * 
     * WHY MANUAL UI UPDATE?
     * - Switch listeners only fire when user toggles them
     * - Programmatic changes (isChecked = value) don't trigger listeners
     * - Must manually set switch positions and apply theme changes
     * 
     * USER FEEDBACK IMPORTANCE:
     * - Destructive actions should always confirm completion
     * - Toast message assures user that reset actually happened
     * - Clear feedback prevents user uncertainty
     */
    private fun resetToDefaults() {
        // Get SharedPreferences instance
        val prefs = requireContext().getSharedPreferences("calorie_tracker_prefs", Context.MODE_PRIVATE)
        
        // Remove all stored preferences (complete reset)
        prefs.edit().clear().apply()
        
        // Manually update UI to show default states
        // (These don't trigger the switch listeners, so no unwanted side effects)
        switchDarkMode.isChecked = false          // Default: light mode
        switchMetricUnits.isChecked = false       // Default: imperial units  
        switchNutritionTips.isChecked = true      // Default: tips enabled
        
        // Apply theme change immediately (required for dark mode reset)
        ThemeManager.setDarkMode(requireContext(), false)
        
        // Confirm successful reset to user
        Toast.makeText(requireContext(), "All preferences reset to defaults", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * SAVE SETTINGS METHOD (FOR EXTERNAL CALLING)
     * 
     * AUTOMATIC VS MANUAL SAVING:
     * - This fragment uses automatic saving (preferences saved immediately when changed)
     * - Switch listeners call SharedPreferences.edit().apply() immediately  
     * - No explicit save button or save operation needed
     * - This method exists for consistency with other fragments that do require saving
     * 
     * DESIGN PATTERN CONSISTENCY:
     * - All settings fragments implement saveSettings() method
     * - Parent activity can call saveSettings() on all fragments uniformly
     * - Some fragments do complex saving (database), others do nothing (like this one)
     */
    fun saveSettings() {
        // Preferences are saved automatically via switch listeners
        // No additional action needed
        // This method exists for interface consistency with other settings fragments
    }
    
    /**
     * BACKUP/RESTORE FUNCTIONALITY
     */
    
    /**
     * Export all user data to a JSON file in Downloads folder
     */
    private fun exportUserData() {
        // Check storage permission
        if (!hasStoragePermission()) {
            requestStoragePermission()
            return
        }
        
        // Show progress
        progressBackup.visibility = View.VISIBLE
        textBackupStatus.visibility = View.VISIBLE
        textBackupStatus.text = "Exporting data..."
        
        lifecycleScope.launch {
            try {
                val result = backupManager.exportUserData()
                
                // Hide progress
                progressBackup.visibility = View.GONE
                
                if (result.success) {
                    textBackupStatus.text = "✅ Export successful! ${result.entriesCount} entries saved"
                    Toast.makeText(requireContext(), 
                        "Backup saved to Downloads folder", 
                        Toast.LENGTH_LONG).show()
                    
                    // Show file location dialog
                    showExportSuccessDialog(result.filePath ?: "")
                } else {
                    textBackupStatus.text = "❌ Export failed: ${result.message}"
                    Toast.makeText(requireContext(), 
                        "Export failed: ${result.message}", 
                        Toast.LENGTH_LONG).show()
                }
                
                // Hide status after delay
                view?.postDelayed({
                    textBackupStatus.visibility = View.GONE
                }, 5000)
                
            } catch (e: Exception) {
                progressBackup.visibility = View.GONE
                textBackupStatus.text = "❌ Export failed: ${e.message}"
                textBackupStatus.visibility = View.VISIBLE
                Toast.makeText(requireContext(), 
                    "Export failed: ${e.message}", 
                    Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * Show dialog to import data from backup file
     */
    private fun showImportDialog() {
        val availableBackups = backupManager.getAvailableBackups()
        
        if (availableBackups.isEmpty()) {
            Toast.makeText(requireContext(), 
                "No backup files found in Downloads folder", 
                Toast.LENGTH_LONG).show()
            return
        }
        
        val backupNames = availableBackups.map { file ->
            "${file.name} (${android.text.format.DateUtils.formatDateTime(requireContext(), 
                file.lastModified(), 
                android.text.format.DateUtils.FORMAT_SHOW_DATE or android.text.format.DateUtils.FORMAT_SHOW_TIME)})"
        }.toTypedArray()
        
        AlertDialog.Builder(requireContext())
            .setTitle("Select Backup File")
            .setItems(backupNames) { _, which ->
                val selectedFile = availableBackups[which]
                confirmImport(selectedFile)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Confirm import operation with warning
     */
    private fun confirmImport(backupFile: File) {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Import Data")
            .setMessage("This will replace ALL your current data with the backup from ${backupFile.name}.\n\nThis action cannot be undone. Continue?")
            .setPositiveButton("Import") { _, _ ->
                importUserData(backupFile.absolutePath)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Import user data from backup file
     */
    private fun importUserData(filePath: String) {
        // Show progress
        progressBackup.visibility = View.VISIBLE
        textBackupStatus.visibility = View.VISIBLE
        textBackupStatus.text = "Importing data..."
        
        lifecycleScope.launch {
            try {
                val result = backupManager.importUserData(filePath)
                
                // Hide progress
                progressBackup.visibility = View.GONE
                
                if (result.success) {
                    textBackupStatus.text = "✅ Import successful! ${result.entriesRestored} entries restored"
                    Toast.makeText(requireContext(), 
                        "Data restored successfully. Restart the app to see changes.", 
                        Toast.LENGTH_LONG).show()
                    
                    // Show restart suggestion
                    AlertDialog.Builder(requireContext())
                        .setTitle("Import Complete")
                        .setMessage("${result.message}\n\nRestart the app to see all imported data.")
                        .setPositiveButton("OK", null)
                        .show()
                        
                } else {
                    textBackupStatus.text = "❌ Import failed: ${result.message}"
                    Toast.makeText(requireContext(), 
                        "Import failed: ${result.message}", 
                        Toast.LENGTH_LONG).show()
                }
                
                // Hide status after delay
                view?.postDelayed({
                    textBackupStatus.visibility = View.GONE
                }, 5000)
                
            } catch (e: Exception) {
                progressBackup.visibility = View.GONE
                textBackupStatus.text = "❌ Import failed: ${e.message}"
                textBackupStatus.visibility = View.VISIBLE
                Toast.makeText(requireContext(), 
                    "Import failed: ${e.message}", 
                    Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * Show success dialog after export with file location
     */
    private fun showExportSuccessDialog(filePath: String) {
        val fileName = File(filePath).name
        
        AlertDialog.Builder(requireContext())
            .setTitle("✅ Backup Created")
            .setMessage("Your data has been exported to:\n\n$fileName\n\nThis file has been saved to your Downloads folder and contains all your food entries, recipes, workout data, and settings.")
            .setPositiveButton("OK", null)
            .setNeutralButton("Open Downloads") { _, _ ->
                openDownloadsFolder()
            }
            .show()
    }
    
    /**
     * Open Downloads folder in file manager
     */
    private fun openDownloadsFolder() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath), "resource/folder")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not open Downloads folder", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Check if app has storage permission
     */
    private fun hasStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Request storage permission
     */
    private fun requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // For Android 11 and above
            AlertDialog.Builder(requireContext())
                .setTitle("Storage Permission Required")
                .setMessage("To export your data, please grant 'All files access' permission in the next screen.\n\nAfter granting permission, return to the app and tap 'Export All Data' again.")
                .setPositiveButton("Grant Permission") { _, _ ->
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:${requireContext().packageName}")
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Could not open settings", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // For Android 10 and below
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1001
    }
}