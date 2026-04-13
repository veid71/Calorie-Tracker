package com.calorietracker

// Android permission system imports
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings

// Standard Android UI imports
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

// Modern permission request handling
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope

// App-specific imports
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.integrations.RenphoScaleManager

// Material Design components
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar

// Coroutines and date formatting
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * EDUCATIONAL OVERVIEW: Settings Scale Fragment
 * 
 * PURPOSE AND COMPLEXITY:
 * - Most complex settings fragment due to Bluetooth LE integration
 * - Manages both Health Connect sync and direct Bluetooth device communication
 * - Handles multiple Android permission types (Bluetooth, Location)
 * - Demonstrates advanced permission flows with educational dialogs
 * 
 * BLUETOOTH LE INTEGRATION:
 * - Connects directly to Renpho smart scales via Bluetooth Low Energy
 * - Scans for compatible devices and establishes connections
 * - Real-time weight data capture from scale measurements
 * - Alternative to Health Connect for users who prefer direct device communication
 * 
 * COMPLEX PERMISSION HANDLING:
 * - Different Bluetooth permissions for Android 12+ vs older versions
 * - Location permission required for Bluetooth scanning (Android security requirement)
 * - Educational dialogs explain why each permission is needed
 * - Deep links to device settings for manual permission management
 * 
 * USER EXPERIENCE FOCUS:
 * - Progressive disclosure of permission requirements
 * - Clear explanation of why location permission is needed for Bluetooth
 * - Multiple sync options (Health Connect vs direct Bluetooth)
 * - Comprehensive error handling and user guidance
 */
class SettingsScaleFragment : Fragment() {
    
    private lateinit var tvScaleStatus: TextView
    private lateinit var tvScaleStatusIndicator: TextView
    private lateinit var tvLastWeightSync: TextView
    private lateinit var tvScanningStatus: TextView
    private lateinit var tvConnectedDevice: TextView
    private lateinit var tvRecentWeightData: TextView
    
    private lateinit var btnSyncHealthConnect: MaterialButton
    private lateinit var btnScanBluetoothScale: MaterialButton
    private lateinit var btnScaleDebug: MaterialButton
    private lateinit var btnStopScanning: MaterialButton
    private lateinit var btnDisconnectScale: MaterialButton
    
    private lateinit var cardScanningStatus: MaterialCardView
    private lateinit var cardConnectedDevice: MaterialCardView
    
    private lateinit var scaleManager: RenphoScaleManager
    private lateinit var database: CalorieDatabase
    
    /**
     * MODERN MULTIPLE PERMISSION REQUEST HANDLING
     * 
     * BLUETOOTH PERMISSION COMPLEXITY:
     * - Multiple permissions required for Bluetooth LE functionality
     * - RequestMultiplePermissions() handles batch permission requests
     * - Result is Map<String, Boolean> showing permission -> granted status
     * 
     * PERMISSION RESULT PROCESSING:
     * - all { it } checks if every permission was granted
     * - filterValues { !it } finds permissions that were denied
     * - Different handling for complete success vs partial failure
     * 
     * USER EXPERIENCE CONSIDERATIONS:
     * - Success case enables functionality immediately
     * - Failure case provides educational guidance
     * - No harsh failures - always guide user toward solution
     */
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions() // Batch permission request contract
    ) { permissions ->
        // Check if user granted all required permissions
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Success: Enable Bluetooth scanning functionality
            updateScaleStatus()
            Snackbar.make(requireView(), "Bluetooth permissions granted! You can now scan for scales.", Snackbar.LENGTH_LONG).show()
        } else {
            // Partial/complete failure: Educate user about next steps
            val deniedPermissions = permissions.filterValues { !it }.keys // Find what was denied
            handlePermissionDenied(deniedPermissions.toList()) // Provide helpful guidance
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings_scale, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeComponents()
        initViews(view)
        setupButtons()
        setupScaleManager()
        updateScaleStatus()
        loadRecentWeightData()
    }
    
    private fun initializeComponents() {
        database = CalorieDatabase.getDatabase(requireContext())
        scaleManager = RenphoScaleManager(requireContext())
    }
    
    private fun initViews(view: View) {
        tvScaleStatus = view.findViewById(R.id.tvScaleStatus)
        tvScaleStatusIndicator = view.findViewById(R.id.tvScaleStatusIndicator)
        tvLastWeightSync = view.findViewById(R.id.tvLastWeightSync)
        tvScanningStatus = view.findViewById(R.id.tvScanningStatus)
        tvConnectedDevice = view.findViewById(R.id.tvConnectedDevice)
        tvRecentWeightData = view.findViewById(R.id.tvRecentWeightData)
        
        btnSyncHealthConnect = view.findViewById(R.id.btnSyncHealthConnect)
        btnScanBluetoothScale = view.findViewById(R.id.btnScanBluetoothScale)
        btnScaleDebug = view.findViewById(R.id.btnScaleDebug)
        btnStopScanning = view.findViewById(R.id.btnStopScanning)
        btnDisconnectScale = view.findViewById(R.id.btnDisconnectScale)
        
        cardScanningStatus = view.findViewById(R.id.cardScanningStatus)
        cardConnectedDevice = view.findViewById(R.id.cardConnectedDevice)
    }
    
    private fun setupButtons() {
        btnSyncHealthConnect.setOnClickListener {
            syncFromHealthConnect()
        }
        
        btnScanBluetoothScale.setOnClickListener {
            startBluetoothScanning()
        }
        
        btnScaleDebug.setOnClickListener {
            showScaleDebugInfo()
        }
        
        btnStopScanning.setOnClickListener {
            stopScanning()
        }
        
        btnDisconnectScale.setOnClickListener {
            disconnectScale()
        }
    }
    
    private fun setupScaleManager() {
        scaleManager.setOnWeightDetectedListener { weight, bodyFat, timestamp ->
            lifecycleScope.launch {
                val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                Snackbar.make(requireView(), "Weight recorded: ${weight}kg on $dateString", Snackbar.LENGTH_LONG).show()
                loadRecentWeightData()
                updateLastSyncTime()
            }
        }
        
        scaleManager.setOnScanStatusChangedListener { isScanning, deviceFound ->
            lifecycleScope.launch {
                if (isScanning) {
                    cardScanningStatus.visibility = View.VISIBLE
                    cardConnectedDevice.visibility = View.GONE
                    tvScanningStatus.text = "Scanning for Renpho scales..."
                    btnScanBluetoothScale.isEnabled = false
                } else if (deviceFound) {
                    cardScanningStatus.visibility = View.GONE
                    cardConnectedDevice.visibility = View.VISIBLE
                    btnScanBluetoothScale.isEnabled = true
                    updateScaleStatus()
                } else {
                    cardScanningStatus.visibility = View.GONE
                    cardConnectedDevice.visibility = View.GONE
                    btnScanBluetoothScale.isEnabled = true
                    updateScaleStatus()
                }
            }
        }
        
        scaleManager.setOnErrorListener { message ->
            lifecycleScope.launch {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
                cardScanningStatus.visibility = View.GONE
                btnScanBluetoothScale.isEnabled = true
            }
        }
        
        // Add device discovery callback for debugging
        scaleManager.setDeviceFoundCallback { deviceName, address, rssi, services ->
            lifecycleScope.launch {
                val serviceList = services.joinToString(", ") { it.substring(0, 8) + "..." }
                val deviceInfo = "$deviceName\n${address.substring(address.length - 8)}\nRSSI: $rssi\nServices: $serviceList"
                
                // Update scanning status to show discovered devices
                if (tvScanningStatus.text.toString().startsWith("Scanning")) {
                    tvScanningStatus.text = "Found: $deviceName (${address.substring(address.length - 8)})\nRSSI: $rssi dBm"
                }
            }
        }
    }
    
    private fun updateScaleStatus() {
        if (scaleManager.isIntegrationAvailable()) {
            tvScaleStatus.text = "Bluetooth LE available - ready for scale connection"
            tvScaleStatusIndicator.text = "🟢"
            btnScanBluetoothScale.isEnabled = true
        } else {
            val hasBluetoothPermissions = hasRequiredPermissions()
            if (!hasBluetoothPermissions) {
                tvScaleStatus.text = "Bluetooth permissions required - tap to grant"
                tvScaleStatusIndicator.text = "🟡"
                btnScanBluetoothScale.isEnabled = true // Enable so user can tap to request permissions
            } else {
                tvScaleStatus.text = "Bluetooth LE not available"
                tvScaleStatusIndicator.text = "🔴"
                btnScanBluetoothScale.isEnabled = false
            }
        }
    }
    
    /**
     * ANDROID VERSION-SPECIFIC PERMISSION HANDLING
     * 
     * BLUETOOTH PERMISSION EVOLUTION:
     * - Android 12+ (API 31) introduced new granular Bluetooth permissions
     * - Replaced generic BLUETOOTH/BLUETOOTH_ADMIN with specific use cases
     * - BLUETOOTH_CONNECT: Connect to paired devices
     * - BLUETOOTH_SCAN: Discover nearby devices
     * - ACCESS_FINE_LOCATION: Required for Bluetooth scanning (privacy requirement)
     * 
     * WHY LOCATION FOR BLUETOOTH?
     * - Bluetooth scanning can reveal user's location (nearby device fingerprinting)
     * - Android requires location permission as privacy protection
     * - Not actually used for GPS - only for Bluetooth scanning permissions
     * 
     * BACKWARD COMPATIBILITY:
     * - Must handle both old and new permission systems
     * - Build.VERSION.SDK_INT determines which permissions to request
     * - all { } ensures every required permission is granted
     */
    private fun hasRequiredPermissions(): Boolean {
        // Determine which permissions are needed based on Android version
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+) uses new granular Bluetooth permissions
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,      // Connect to paired devices
                Manifest.permission.BLUETOOTH_SCAN,         // Discover nearby devices
                Manifest.permission.ACCESS_FINE_LOCATION    // Required for BLE scanning
            )
        } else {
            // Android 11 and below use legacy permissions
            listOf(
                Manifest.permission.BLUETOOTH,              // Legacy Bluetooth access
                Manifest.permission.BLUETOOTH_ADMIN,        // Legacy Bluetooth management
                Manifest.permission.ACCESS_FINE_LOCATION    // Still required for scanning
            )
        }
        
        // Check if all required permissions are granted
        return permissions.all { permission ->
            ActivityCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * VERSION-AWARE PERMISSION REQUEST
     * 
     * SAME LOGIC AS CHECK METHOD:
     * - Must request the correct permissions for user's Android version
     * - Array vs List: launch() expects Array, hasRequiredPermissions() uses List
     * - Both methods must stay in sync with permission requirements
     */
    private fun requestBluetoothPermissions() {
        // Create permission array based on Android version
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+) uses new Bluetooth permissions
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            // Android 11 and below use legacy permissions
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        
        // Launch permission request using ActivityResultContract
        requestPermissions.launch(permissions)
    }
    
    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("🔵 Bluetooth Permissions Required")
            .setMessage("To connect to your Renpho smart scale, this app needs:\n\n" +
                    "📱 **Bluetooth Access** - To discover and connect to your scale\n" +
                    "📍 **Location Access** - Required by Android for Bluetooth scanning\n\n" +
                    "Your location is never stored or shared - it's only used for Bluetooth functionality.")
            .setPositiveButton("Grant Permissions") { _, _ ->
                requestBluetoothPermissions()
            }
            .setNegativeButton("Manual Setup") { _, _ ->
                showManualPermissionInstructions()
            }
            .setCancelable(true)
            .show()
    }
    
    private fun handlePermissionDenied(deniedPermissions: List<String>) {
        val permanentlyDenied = deniedPermissions.any { permission ->
            !shouldShowRequestPermissionRationale(permission)
        }
        
        if (permanentlyDenied) {
            showManualPermissionInstructions()
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Permissions Needed")
                .setMessage("Bluetooth and location permissions are required to scan for your Renpho scale.\n\n" +
                        "Without these permissions, you can only sync weight data from Health Connect.")
                .setPositiveButton("Try Again") { _, _ ->
                    requestBluetoothPermissions()
                }
                .setNegativeButton("Open Settings") { _, _ ->
                    openAppSettings()
                }
                .show()
        }
        
        updateScaleStatus()
    }
    
    private fun showManualPermissionInstructions() {
        AlertDialog.Builder(requireContext())
            .setTitle("📋 Manual Permission Setup")
            .setMessage("To enable Bluetooth scale scanning:\n\n" +
                    "1️⃣ Tap 'Open Settings' below\n" +
                    "2️⃣ Find 'Permissions' section\n" +
                    "3️⃣ Enable 'Location' permission\n" +
                    "4️⃣ Enable 'Nearby devices' (Bluetooth)\n" +
                    "5️⃣ Return to this screen\n\n" +
                    "Alternative: Use 'Sync from Health Connect' if your scale syncs there.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNeutralButton("Health Connect Instead") { _, _ ->
                syncFromHealthConnect()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * DEEP LINKING TO ANDROID SYSTEM SETTINGS
     * 
     * SETTINGS DEEP LINKS EXPLAINED:
     * - Intent with specific action opens targeted settings screen
     * - ACTION_APPLICATION_DETAILS_SETTINGS opens app-specific permission screen
     * - URI with package scheme identifies which app's settings to show
     * - FLAG_ACTIVITY_NEW_TASK starts settings in separate task stack
     * 
     * GRACEFUL FALLBACK PATTERN:
     * - Try specific settings first (better user experience)
     * - Fall back to general settings if specific intent fails
     * - Never let the app crash from settings intent failures
     * 
     * USER GUIDANCE:
     * - Snackbar provides instructions for what to do in settings
     * - Clear expectation that user should return to the app
     * - Educational approach rather than just opening settings
     */
    private fun openAppSettings() {
        try {
            /**
             * CREATE APP-SPECIFIC SETTINGS INTENT
             * 
             * INTENT CONSTRUCTION DETAILS:
             * - ACTION_APPLICATION_DETAILS_SETTINGS opens this app's permission screen
             * - Uri.fromParts() creates URI in format "package:com.calorietracker"
             * - packageName identifies which app's settings to display
             * - FLAG_ACTIVITY_NEW_TASK prevents settings from being part of app's task stack
             */
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent) // Open Android settings for this app
            
            // Provide clear instructions for what user should do
            Snackbar.make(requireView(), "💡 Grant Location & Bluetooth permissions, then return here", Snackbar.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            /**
             * FALLBACK TO GENERAL SETTINGS
             * 
             * WHY MIGHT APP-SPECIFIC SETTINGS FAIL?
             * - Some custom Android ROMs don't support ACTION_APPLICATION_DETAILS_SETTINGS
             * - Older Android versions might have different settings structure
             * - Device manufacturers sometimes modify settings behavior
             * - Always provide fallback for reliability
             */
            // Fallback to general settings if app-specific settings fails
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }
    
    private fun syncFromHealthConnect() {
        lifecycleScope.launch {
            try {
                btnSyncHealthConnect.isEnabled = false
                btnSyncHealthConnect.text = "Syncing..."
                
                val weightEntries = scaleManager.syncFromHealthConnect()
                
                if (weightEntries.isNotEmpty()) {
                    Snackbar.make(requireView(), "Synced ${weightEntries.size} weight entries from Health Connect", Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(requireView(), "No new weight data found in Health Connect", Snackbar.LENGTH_SHORT).show()
                }
                
                loadRecentWeightData()
                updateLastSyncTime()
                
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Health Connect sync error: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                btnSyncHealthConnect.isEnabled = true
                btnSyncHealthConnect.text = "Sync from Health Connect"
            }
        }
    }
    
    private fun startBluetoothScanning() {
        if (!hasRequiredPermissions()) {
            showPermissionExplanationDialog()
            return
        }
        
        if (!scaleManager.isIntegrationAvailable()) {
            Snackbar.make(requireView(), "Bluetooth LE not available on this device", Snackbar.LENGTH_SHORT).show()
            return
        }
        
        scaleManager.startScanning()
    }
    
    private fun stopScanning() {
        scaleManager.stopScanning()
    }
    
    private fun disconnectScale() {
        scaleManager.disconnect()
        cardConnectedDevice.visibility = View.GONE
        updateScaleStatus()
    }
    
    private fun showScaleDebugInfo() {
        val debugInfo = scaleManager.getStatusInfo()
        
        val message = buildString {
            append("Scale Integration Debug Info:\n\n")
            append(debugInfo)
        }
        
        // Show debug info in a snackbar or dialog
        Snackbar.make(requireView(), "Debug info logged - check system logs", Snackbar.LENGTH_SHORT).show()
        android.util.Log.d("ScaleDebug", message)
    }
    
    private fun loadRecentWeightData() {
        lifecycleScope.launch {
            try {
                val recentWeights = database.weightEntryDao().getRecentWeightEntries(7) // Last 7 entries
                
                if (recentWeights.isNotEmpty()) {
                    val weightText = buildString {
                        recentWeights.forEach { weightEntry ->
                            append("${weightEntry.date}: ${weightEntry.weight}kg")
                            if (!weightEntry.notes.isNullOrBlank()) {
                                append(" (${weightEntry.notes})")
                            }
                            append("\n")
                        }
                    }
                    tvRecentWeightData.text = weightText.trim()
                } else {
                    tvRecentWeightData.text = "No weight data available\n\nSync from Health Connect or scan for a Renpho scale to get started"
                }
            } catch (e: Exception) {
                tvRecentWeightData.text = "Error loading weight data: ${e.message}"
            }
        }
    }
    
    private fun updateLastSyncTime() {
        val sharedPrefs = requireContext().getSharedPreferences("scale_sync_prefs", android.content.Context.MODE_PRIVATE)
        val lastSync = sharedPrefs.getLong("last_scale_sync", 0)
        
        if (lastSync == 0L) {
            tvLastWeightSync.text = "Last weight sync: Never"
        } else {
            val now = System.currentTimeMillis()
            val timeDiff = now - lastSync
            val minutes = timeDiff / (1000 * 60)
            
            tvLastWeightSync.text = when {
                minutes < 1 -> "Last weight sync: Just now"
                minutes < 60 -> "Last weight sync: ${minutes}m ago"
                minutes < 1440 -> "Last weight sync: ${minutes / 60}h ago"
                else -> {
                    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
                    "Last weight sync: ${formatter.format(Date(lastSync))}"
                }
            }
        }
        
        // Update sync time
        sharedPrefs.edit().putLong("last_scale_sync", System.currentTimeMillis()).apply()
    }
    
    override fun onResume() {
        super.onResume()
        // Check permissions again when user returns from settings
        updateScaleStatus()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scaleManager.disconnect()
    }
    
    fun saveSettings() {
        // Scale settings are managed automatically
    }
}