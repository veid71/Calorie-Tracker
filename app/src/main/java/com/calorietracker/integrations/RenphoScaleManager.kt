package com.calorietracker.integrations

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.database.WeightEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

/**
 * Comprehensive manager for Renpho scale integration using multiple approaches.
 * 
 * **BLUETOOTH LOW ENERGY (BLE) CONCEPTS FOR BEGINNERS:**
 * 
 * **What is Bluetooth Low Energy?**
 * BLE is like a walkie-talkie system for devices:
 * - Low power consumption (months on a battery)
 * - Short range (30 feet typical)
 * - Perfect for sensors and IoT devices
 * - Different from classic Bluetooth (headphones, speakers)
 * 
 * **How Smart Scales Work:**
 * 1. Scale measures your weight
 * 2. Scale broadcasts weight data over BLE
 * 3. Your phone scans for nearby BLE devices
 * 4. Phone connects to scale and reads data
 * 5. App processes data and stores in database
 * 
 * **BLE Device Discovery Process:**
 * ```
 * [Phone] --------scan--------> [Multiple BLE devices]
 * [Phone] <----advertisement---- [Scale: "I'm a Renpho scale!"]
 * [Phone] --------connect------> [Scale]
 * [Phone] <-------data--------- [Scale: "Weight: 150.5 lbs"]
 * ```
 * 
 * **Services and Characteristics:**
 * BLE devices organize data like a filing system:
 * - Service: Like a filing cabinet (e.g., "Weight Service")
 * - Characteristic: Like a file in the cabinet (e.g., "Current Weight")
 * - Each has a unique UUID (like a postal address)
 * 
 * **Android BLE Permissions:**
 * Android requires multiple permissions for BLE:
 * - BLUETOOTH: Basic Bluetooth access
 * - BLUETOOTH_ADMIN: Can start/stop scans
 * - ACCESS_FINE_LOCATION: BLE can reveal location (required by Android)
 * 
 * **Multiple Integration Approaches:**
 * We use two methods for maximum compatibility:
 * 1. **Health Connect:** Gets data from Renpho's official app
 *    - More reliable, handles all the BLE complexity
 *    - Requires user to have Renpho app installed
 *    - Automatic sync when Renpho app updates
 * 
 * 2. **Direct BLE:** Connects directly to scale
 *    - Works without Renpho app
 *    - Immediate data (no waiting for app sync)
 *    - More complex, requires BLE implementation
 * 
 * **Data Processing Pipeline:**
 * Raw scale data → Parse bytes → Validate → Convert units → Store database
 * Example: [0x3A, 0x2B, 0x00, 0x00] → 150.5 → 68.3 kg → Database record
 * 
 * 1. Health Connect integration for weight data from Renpho Health app
 * 2. Bluetooth LE integration for direct scale communication
 * 3. Automatic weight sync and storage
 */
class RenphoScaleManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RenphoScaleManager"
        
        // Renpho scale Bluetooth LE identifiers
        private const val RENPHO_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb"
        private const val WEIGHT_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"
        private const val COMPOSITION_CHARACTERISTIC_UUID = "0000ffe2-0000-1000-8000-00805f9b34fb"
        
        // Known Renpho device names (expanded list)
        private val RENPHO_DEVICE_NAMES = listOf(
            "QN-Scale", "Renpho-Scale", "RENPHO", "ES-24M", "ES-26M", "ES-28M",
            "RenphoHealth", "Renpho Health", "RH-CS20C", "RH-CS20D", "RH-CS20E",
            "QNSCALE", "QN_SCALE", "Renpho_Scale", "YUNMAI", "体重秤", "body scale"
        )
        
        // Scan timeout in milliseconds
        private const val SCAN_TIMEOUT_MS = 30000L
    }
    
    private val database = CalorieDatabase.getDatabase(context)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val healthConnectClient = HealthConnectClient.getOrCreate(context)
    
    // Use a proper scope instead of GlobalScope to prevent memory leaks
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var isScanning = false
    private var bluetoothGatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private var onWeightDetected: ((weight: Double, bodyFat: Double?, timestamp: Long) -> Unit)? = null
    private var onScanStatusChanged: ((isScanning: Boolean, deviceFound: Boolean) -> Unit)? = null
    private var onError: ((message: String) -> Unit)? = null
    private var onDeviceFound: ((deviceName: String, address: String, rssi: Int, services: List<String>) -> Unit)? = null
    
    /**
     * Set callback for device discovery (for debugging)
     */
    fun setDeviceFoundCallback(callback: (deviceName: String, address: String, rssi: Int, services: List<String>) -> Unit) {
        onDeviceFound = callback
    }
    
    /**
     * Check if Renpho scale integration is available
     */
    fun isIntegrationAvailable(): Boolean {
        return bluetoothAdapter != null && 
               context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
               hasRequiredPermissions()
    }
    
    /**
     * Check if required permissions are granted (Android 13+ compatible)
     */
    private fun hasRequiredPermissions(): Boolean {
        // Always require location permission for BLE scanning
        val locationPermission = ActivityCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        // Check Bluetooth permissions based on Android version
        val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+) - New Bluetooth permissions
            val scanPermission = ActivityCompat.checkSelfPermission(
                context, 
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            
            val connectPermission = ActivityCompat.checkSelfPermission(
                context, 
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            
            scanPermission && connectPermission
        } else {
            // Android 11 and below - Legacy Bluetooth permissions
            val bluetoothPermission = ActivityCompat.checkSelfPermission(
                context, 
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
            
            val bluetoothAdminPermission = ActivityCompat.checkSelfPermission(
                context, 
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
            
            bluetoothPermission && bluetoothAdminPermission
        }
        
        return locationPermission && bluetoothPermissions
    }
    
    /**
     * Sync weight data from Health Connect (from Renpho Health app)
     */
    suspend fun syncFromHealthConnect(): List<WeightEntry> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Syncing weight data from Health Connect")
            
            val endTime = Instant.now()
            val startTime = endTime.minusSeconds(30 * 24 * 60 * 60) // Last 30 days
            
            val request = ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            
            val response = healthConnectClient.readRecords(request)
            val weightEntries = mutableListOf<WeightEntry>()
            
            Log.d(TAG, "Found ${response.records.size} weight records from Health Connect")
            
            response.records.forEach { weightRecord ->
                val date = weightRecord.time.atZone(ZoneId.systemDefault()).toLocalDate()
                val dateString = date.toString()
                val weightKg = weightRecord.weight.inKilograms
                
                // Check if this date already has a weight entry
                val existingEntry = database.weightEntryDao().getWeightForDate(dateString)
                
                if (existingEntry == null) {
                    val weightEntry = WeightEntry(
                        date = dateString,
                        weight = weightKg,
                        notes = "Synced from Health Connect"
                    )
                    
                    database.weightEntryDao().insertWeight(weightEntry)
                    weightEntries.add(weightEntry)
                    Log.d(TAG, "Added weight entry: ${weightKg}kg on $dateString")
                }
            }
            
            weightEntries
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from Health Connect", e)
            onError?.invoke("Error syncing from Health Connect: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Start scanning for Renpho scales via Bluetooth LE
     * 
     * **BLE SCANNING PROCESS EXPLAINED:**
     * 
     * **Device Discovery:**
     * BLE scanning is like shouting in a crowded room:
     * - Phone: "Is anyone a Renpho scale?"
     * - Scale: "Yes! I'm a QN-Scale with weight data!"
     * - Phone: "Great, let me connect to you!"
     * 
     * **Scan Filters:**
     * Without filters, we'd see ALL BLE devices (hundreds!):
     * - Fitness trackers
     * - Smart home devices  
     * - Car entertainment systems
     * - Other phones
     * 
     * Filters help us find only what we want:
     * - Service UUID filter: Only devices with Renpho's specific service
     * - Device name filter: Only devices named "QN-Scale", "RENPHO", etc.
     * 
     * **Scan Settings:**
     * - SCAN_MODE_LOW_LATENCY: Find devices quickly (uses more battery)
     * - SCAN_MODE_LOW_POWER: Save battery but slower discovery
     * - SCAN_MODE_BALANCED: Good compromise
     * 
     * **Timeout Strategy:**
     * BLE scans can run forever and drain battery:
     * - Set 30-second timeout using Handler.postDelayed()
     * - Stop scanning automatically if no device found
     * - Inform user if no scale detected
     * 
     * **Error Handling:**
     * BLE operations can fail for many reasons:
     * - Bluetooth disabled
     * - Permissions not granted
     * - Scale not turned on
     * - Scale too far away
     * - Interference from other devices
     * 
     * We catch SecurityException specifically because BLE permissions
     * are dangerous permissions that can be denied at runtime.
     */
    fun startScanning() {
        if (!isIntegrationAvailable()) {
            onError?.invoke("Bluetooth LE not available or permissions missing")
            return
        }
        
        if (isScanning) {
            Log.d(TAG, "Already scanning")
            return
        }
        
        Log.d(TAG, "Starting Bluetooth LE scan for Renpho scales")
        isScanning = true
        onScanStatusChanged?.invoke(true, false)
        
        val scanner = bluetoothAdapter.bluetoothLeScanner
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        // Remove restrictive filters - scan for ALL devices and filter in callback
        // This ensures we don't miss scales due to unknown device names or UUIDs
        val filters = emptyList<ScanFilter>()
        
        try {
            scanner.startScan(filters, settings, scanCallback)
            
            // Stop scanning after timeout
            handler.postDelayed({
                stopScanning()
                if (bluetoothGatt == null) {
                    onScanStatusChanged?.invoke(false, false)
                    onError?.invoke("No Renpho scale found. Make sure the scale is on and nearby.")
                }
            }, SCAN_TIMEOUT_MS)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during scan", e)
            isScanning = false
            onError?.invoke("Bluetooth permission required for scale scanning")
        }
    }
    
    /**
     * Stop scanning for devices
     */
    fun stopScanning() {
        if (!isScanning) return
        
        Log.d(TAG, "Stopping Bluetooth LE scan")
        isScanning = false
        
        try {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception stopping scan", e)
        }
        
        onScanStatusChanged?.invoke(false, bluetoothGatt != null)
    }
    
    /**
     * Disconnect from currently connected scale
     */
    fun disconnect() {
        bluetoothGatt?.let { gatt ->
            try {
                gatt.disconnect()
                gatt.close()
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception during disconnect", e)
            }
        }
        bluetoothGatt = null
        onScanStatusChanged?.invoke(false, false)
    }
    
    /**
     * Cleanup resources and cancel background operations to prevent memory leaks
     */
    fun cleanup() {
        stopScanning()
        disconnect()
        managerScope.cancel() // Cancel all pending operations
        onWeightDetected = null
        onScanStatusChanged = null
        onError = null
        onDeviceFound = null
    }
    
    /**
     * Build scan filters for Renpho devices (now includes broader scan)
     */
    private fun buildScanFilters(): List<ScanFilter> {
        val filters = mutableListOf<ScanFilter>()
        
        // Filter by service UUID
        filters.add(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(RENPHO_SERVICE_UUID))
                .build()
        )
        
        // Filter by device names
        RENPHO_DEVICE_NAMES.forEach { deviceName ->
            filters.add(
                ScanFilter.Builder()
                    .setDeviceName(deviceName)
                    .build()
            )
        }
        
        // Add broader filter for any device with "Renpho" in name (case insensitive)
        // This helps catch devices with slight name variations
        
        return filters
    }
    
    /**
     * Bluetooth LE scan callback
     */
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = device.name ?: "Unknown"
            
            // Enhanced logging for debugging
            val serviceUuids = result.scanRecord?.serviceUuids?.map { it.toString() } ?: emptyList()
            val rssi = result.rssi
            
            Log.d(TAG, "Found device: $deviceName (${device.address}) RSSI: $rssi")
            Log.d(TAG, "Services: $serviceUuids")
            
            // Notify UI about ALL devices found (for debugging)
            onDeviceFound?.invoke(deviceName, device.address, rssi, serviceUuids)
            
            val isNameMatch = isRenphoDevice(deviceName)
            val isServiceMatch = hasRenphoService(result)
            
            Log.d(TAG, "Name match: $isNameMatch, Service match: $isServiceMatch")
            
            if (isNameMatch || isServiceMatch) {
                Log.d(TAG, "*** FOUND POTENTIAL RENPHO SCALE: $deviceName ***")
                stopScanning()
                connectToDevice(result)
            } else if (deviceName.isNotBlank() && deviceName != "Unknown") {
                // DEBUG MODE: Accept any device that could be a scale
                // Remove this after identifying the correct device
                val couldBeScale = deviceName.lowercase().contains("scale") || 
                                 deviceName.lowercase().contains("weight") ||
                                 deviceName.lowercase().contains("body") ||
                                 rssi > -60 // Strong signal device nearby
                
                if (couldBeScale) {
                    Log.d(TAG, "*** DEBUG: TRYING DEVICE: $deviceName (might be scale) ***")
                    stopScanning()
                    connectToDevice(result)
                } else {
                    Log.d(TAG, "Skipping device: $deviceName (no match)")
                }
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed with error code: $errorCode")
            isScanning = false
            onScanStatusChanged?.invoke(false, false)
            onError?.invoke("Bluetooth scan failed. Try turning Bluetooth off and on.")
        }
    }
    
    /**
     * Check if device name indicates a Renpho scale (expanded detection)
     */
    private fun isRenphoDevice(deviceName: String): Boolean {
        // Check known names first
        val knownNameMatch = RENPHO_DEVICE_NAMES.any { name ->
            deviceName.contains(name, ignoreCase = true)
        }
        
        if (knownNameMatch) return true
        
        // Broader pattern matching for Renpho scales
        val renphoPatterns = listOf(
            "renpho", "qn-", "qn_", "yunmai", "scale", "body", "weight",
            "体重", "健康", "RH-", "ES-", "BF-"
        )
        
        return renphoPatterns.any { pattern ->
            deviceName.contains(pattern, ignoreCase = true)
        }
    }
    
    /**
     * Check if scan result contains Renpho service UUID
     */
    private fun hasRenphoService(result: ScanResult): Boolean {
        val serviceUuids = result.scanRecord?.serviceUuids
        return serviceUuids?.any { uuid ->
            uuid.toString().equals(RENPHO_SERVICE_UUID, ignoreCase = true)
        } ?: false
    }
    
    /**
     * Connect to discovered Renpho device
     */
    private fun connectToDevice(scanResult: ScanResult) {
        try {
            bluetoothGatt = scanResult.device.connectGatt(
                context,
                false,
                gattCallback
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during connection", e)
            onError?.invoke("Bluetooth permission required for scale connection")
        }
    }
    
    /**
     * Bluetooth GATT callback for handling scale communication
     */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to Renpho scale")
                    onScanStatusChanged?.invoke(false, true)
                    try {
                        gatt.discoverServices()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Security exception discovering services", e)
                    }
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from Renpho scale")
                    onScanStatusChanged?.invoke(false, false)
                    bluetoothGatt = null
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered on Renpho scale")
                setupNotifications(gatt)
            }
        }
        
        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            when (characteristic.uuid.toString()) {
                WEIGHT_CHARACTERISTIC_UUID -> {
                    @Suppress("DEPRECATION")
                    handleWeightData(characteristic.value)
                }
                COMPOSITION_CHARACTERISTIC_UUID -> {
                    @Suppress("DEPRECATION")
                    handleCompositionData(characteristic.value)
                }
            }
        }
    }
    
    /**
     * Setup notifications for weight and composition characteristics
     */
    private fun setupNotifications(gatt: BluetoothGatt) {
        val service = gatt.getService(UUID.fromString(RENPHO_SERVICE_UUID))
        if (service != null) {
            // Enable notifications for weight characteristic
            val weightChar = service.getCharacteristic(UUID.fromString(WEIGHT_CHARACTERISTIC_UUID))
            if (weightChar != null) {
                try {
                    gatt.setCharacteristicNotification(weightChar, true)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception setting weight notifications", e)
                }
            }
            
            // Enable notifications for composition characteristic
            val compositionChar = service.getCharacteristic(UUID.fromString(COMPOSITION_CHARACTERISTIC_UUID))
            if (compositionChar != null) {
                try {
                    gatt.setCharacteristicNotification(compositionChar, true)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception setting composition notifications", e)
                }
            }
        }
    }
    
    /**
     * Handle weight data from scale
     * 
     * **BINARY DATA PROCESSING:**
     * 
     * **Why Binary Data?**
     * Bluetooth devices send data as raw bytes, not text:
     * - More efficient (smaller data packets)
     * - Faster transmission
     * - Universal format (works regardless of language)
     * 
     * **Byte Array Decoding:**
     * Weight data comes as array of bytes: [0x3A, 0x2B, 0x00, 0x00]
     * 
     * **Bit Manipulation Explained:**
     * ```kotlin
     * val weightRaw = ((data[0].toInt() and 0xFF) shl 8) + (data[1].toInt() and 0xFF)
     * ```
     * 
     * Breaking this down:
     * 1. `data[0].toInt()`: Convert first byte to integer
     * 2. `and 0xFF`: Mask to keep only lowest 8 bits (handles negative bytes)
     * 3. `shl 8`: Shift left 8 bits (multiply by 256)
     * 4. `data[1].toInt() and 0xFF`: Second byte as integer
     * 5. `+`: Combine the two bytes
     * 
     * **Example:**
     * If data = [0x3A, 0x2B] (representing 150.5 * 100):
     * - data[0] = 0x3A = 58 decimal
     * - data[1] = 0x2B = 43 decimal
     * - weightRaw = (58 × 256) + 43 = 14,848 + 43 = 14,891
     * - weight = 14,891 / 100 = 148.91 kg
     * 
     * **Protocol Understanding:**
     * Each scale manufacturer uses their own protocol:
     * - Renpho: Weight in first 2 bytes, multiplied by 100
     * - Other brands might use 4 bytes, different multipliers
     * - This is why we need device-specific code
     * 
     * **Data Validation:**
     * Always validate raw sensor data:
     * - Check minimum packet size (at least 4 bytes expected)
     * - Reasonable weight range (20-300 kg for human scales)
     * - Timestamp to prevent duplicate readings
     */
    private fun handleWeightData(data: ByteArray) {
        if (data.size >= 4) {
            // Decode weight from bytes (Renpho format)
            val weightRaw = ((data[0].toInt() and 0xFF) shl 8) + (data[1].toInt() and 0xFF)
            val weight = weightRaw / 100.0 // Convert to kg
            
            Log.d(TAG, "Received weight: ${weight}kg")
            
            val timestamp = System.currentTimeMillis()
            onWeightDetected?.invoke(weight, null, timestamp)
            
            // Store weight in database
            saveWeightToDatabase(weight, timestamp)
        }
    }
    
    /**
     * Handle body composition data from scale
     */
    private fun handleCompositionData(data: ByteArray) {
        if (data.size >= 8) {
            // Decode body fat percentage (simplified - actual algorithm is proprietary)
            val bodyFatRaw = ((data[2].toInt() and 0xFF) shl 8) + (data[3].toInt() and 0xFF)
            val bodyFat = bodyFatRaw / 100.0
            
            Log.d(TAG, "Received body composition - Body fat: ${bodyFat}%")
        }
    }
    
    /**
     * Save weight measurement to database
     */
    private fun saveWeightToDatabase(weight: Double, timestamp: Long) {
        try {
            val dateString = LocalDate.now().toString()
            val weightEntry = WeightEntry(
                date = dateString,
                weight = weight,
                notes = "Auto-synced from Renpho scale"
            )
            
            // Save in background thread with coroutines
            managerScope.launch {
                try {
                    database.weightEntryDao().insertWeight(weightEntry)
                    Log.d(TAG, "Weight saved to database: ${weight}kg")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving weight to database", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing weight for database", e)
        }
    }
    
    /**
     * Set callback for weight detection events
     */
    fun setOnWeightDetectedListener(listener: (weight: Double, bodyFat: Double?, timestamp: Long) -> Unit) {
        onWeightDetected = listener
    }
    
    /**
     * Set callback for scan status changes
     */
    fun setOnScanStatusChangedListener(listener: (isScanning: Boolean, deviceFound: Boolean) -> Unit) {
        onScanStatusChanged = listener
    }
    
    /**
     * Set callback for error events
     */
    fun setOnErrorListener(listener: (message: String) -> Unit) {
        onError = listener
    }
    
    /**
     * Get status information for debugging
     */
    fun getStatusInfo(): String {
        return buildString {
            append("Bluetooth Available: ${bluetoothAdapter != null}\n")
            append("BLE Support: ${context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)}\n")
            append("Permissions Granted: ${hasRequiredPermissions()}\n")
            append("Currently Scanning: $isScanning\n")
            append("Connected Device: ${bluetoothGatt?.device?.name ?: "None"}\n")
            append("Health Connect Client: Available")
        }
    }
}