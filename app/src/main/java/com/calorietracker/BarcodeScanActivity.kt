package com.calorietracker

// Android system imports - these give us access to Android features
import android.Manifest                                    // For requesting camera permission from user
import android.content.Intent                              // For navigating between app screens
import android.content.pm.PackageManager                   // For checking if user granted permissions
import android.os.Bundle                                   // For passing data between activities
import android.util.Log                                    // For debugging messages in the console
import android.widget.Toast                                // For showing quick messages to user
import androidx.activity.result.contract.ActivityResultContracts  // Modern way to request permissions
import androidx.appcompat.app.AppCompatActivity            // Base class for app screens with action bar
import androidx.camera.core.*                              // Camera functionality (taking photos, preview)
import androidx.camera.lifecycle.ProcessCameraProvider     // Manages camera lifecycle safely
import androidx.camera.view.PreviewView                    // UI component that shows camera preview
import androidx.core.content.ContextCompat                 // Helper for accessing app resources
import androidx.lifecycle.lifecycleScope                   // For running background tasks safely
import androidx.recyclerview.widget.LinearLayoutManager    // Layout manager for RecyclerView
import androidx.recyclerview.widget.RecyclerView           // List component for showing history
import com.calorietracker.database.CalorieDatabase        // Our app's local database
import com.calorietracker.database.BarcodeHistory          // Barcode history entity
import com.calorietracker.database.FoodItem                // Food item entity
import com.calorietracker.repository.CalorieRepository     // Data access layer for nutrition info
import com.calorietracker.utils.ThemeManager              // Handles dark/light theme switching
import com.google.android.material.button.MaterialButton  // Modern-looking button component
import com.google.mlkit.vision.barcode.BarcodeScanner     // Google's barcode recognition engine
import com.google.mlkit.vision.barcode.BarcodeScannerOptions // Configuration for barcode scanner
import com.google.mlkit.vision.barcode.BarcodeScanning    // Factory to create barcode scanner
import com.google.mlkit.vision.barcode.common.Barcode     // Represents a detected barcode
import com.google.mlkit.vision.common.InputImage          // Image format that ML Kit can process
import kotlinx.coroutines.launch                          // For running tasks in background thread
import java.util.concurrent.ExecutorService               // For managing camera background threads
import java.util.concurrent.Executors                     // Factory for creating thread executors

/**
 * BARCODE SCANNING ACTIVITY
 * 
 * This screen lets users scan barcodes on food packages to automatically add them to their diary.
 * 
 * How it works:
 * 1. Opens the phone's camera and shows live preview
 * 2. Uses Google ML Kit to detect barcodes in camera frames
 * 3. Looks up the barcode in Open Food Facts database
 * 4. If found, takes user to CalorieEntryActivity with pre-filled food info
 * 
 * This is much faster than typing food names manually!
 */
class BarcodeScanActivity : AppCompatActivity() {
    
    // UI component that shows live camera feed to user
    private lateinit var previewView: PreviewView
    
    // Data access layer for looking up nutrition info by barcode
    private lateinit var repository: CalorieRepository
    
    // Recent scans UI components
    private lateinit var recyclerRecentScans: RecyclerView
    private lateinit var btnToggleRecentScans: MaterialButton
    private lateinit var historyAdapter: BarcodeHistoryAdapter
    private var isRecentScansVisible = false
    
    // Background thread executor for camera operations (keeps UI smooth)
    private lateinit var cameraExecutor: ExecutorService
    
    // Camera instance - nullable because we might not have camera permission
    private var camera: Camera? = null
    
    // Google ML Kit barcode scanner - the "brain" that recognizes barcodes
    private var barcodeScanner: BarcodeScanner? = null
    
    // Flag to prevent processing the same barcode multiple times
    // (Without this, we'd try to process the same barcode 30 times per second!)
    private var isProcessingBarcode = false
    
    // Remember the last barcode we processed to avoid duplicates
    private var lastProcessedBarcode: String? = null
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before setting content view
        ThemeManager.applyTheme(this)
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scan)
        
        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)
        previewView = findViewById(R.id.previewView)
        recyclerRecentScans = findViewById(R.id.recyclerRecentScans)
        btnToggleRecentScans = findViewById(R.id.btnToggleRecentScans)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        setupBarcodeScanner()
        setupRecentScans()
        setupClickListeners()
        
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun setupBarcodeScanner() {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39
            )
            .build()
        
        barcodeScanner = BarcodeScanning.getClient(options)
    }
    
    private fun setupRecentScans() {
        // Initialize the adapter with click handler
        historyAdapter = BarcodeHistoryAdapter { history ->
            onHistoryItemClicked(history)
        }
        
        // Set up RecyclerView with horizontal layout
        recyclerRecentScans.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(this@BarcodeScanActivity, LinearLayoutManager.HORIZONTAL, false)
        }
        
        // Load recent history
        loadRecentScans()
    }
    
    private fun loadRecentScans() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading recent scans...")
                val recentHistory = repository.getRecentBarcodeHistory(20)
                Log.d(TAG, "Found ${recentHistory.size} recent scans")
                historyAdapter.submitList(recentHistory)
                
                // Update button text with count
                val count = recentHistory.size
                if (count > 0) {
                    btnToggleRecentScans.text = "📜 Recent Scans ($count)"
                    Log.d(TAG, "Updated button text with count: $count")
                } else {
                    btnToggleRecentScans.text = "📜 Recent Scans"
                    Log.d(TAG, "No recent scans found, using default text")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading recent scans", e)
                Toast.makeText(this@BarcodeScanActivity, "Error loading recent scans: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun onHistoryItemClicked(history: BarcodeHistory) {
        // When user clicks a history item, simulate finding that barcode
        val intent = Intent(this, CalorieEntryActivity::class.java).apply {
            putExtra("food_name", history.foodName)
            putExtra("calories", history.calories)
            putExtra("protein", history.protein ?: 0.0)
            putExtra("carbs", history.carbs ?: 0.0)
            putExtra("fat", history.fat ?: 0.0)
            putExtra("fiber", history.fiber ?: 0.0)
            putExtra("sugar", history.sugar ?: 0.0)
            putExtra("sodium", history.sodium ?: 0.0)
            putExtra("barcode", history.barcode)
        }
        
        // Record that this item was accessed again
        lifecycleScope.launch {
            repository.recordBarcodeHistory(
                FoodItem(
                    barcode = history.barcode,
                    name = history.foodName,
                    brand = history.brand,
                    caloriesPerServing = history.calories,
                    proteinPerServing = history.protein ?: 0.0,
                    carbsPerServing = history.carbs ?: 0.0,
                    fatPerServing = history.fat ?: 0.0,
                    fiberPerServing = history.fiber ?: 0.0,
                    sugarPerServing = history.sugar ?: 0.0,
                    sodiumPerServing = history.sodium ?: 0.0
                ),
                "history_reuse"
            )
        }
        
        startActivity(intent)
        finish()
    }
    
    private fun toggleRecentScans() {
        Log.d(TAG, "toggleRecentScans called, current visibility: $isRecentScansVisible")
        isRecentScansVisible = !isRecentScansVisible
        
        if (isRecentScansVisible) {
            Log.d(TAG, "Showing recent scans")
            recyclerRecentScans.visibility = RecyclerView.VISIBLE
            btnToggleRecentScans.text = "📜 Hide Recent"
            loadRecentScans() // Refresh the list
            Toast.makeText(this, "Showing recent scans", Toast.LENGTH_SHORT).show()
        } else {
            Log.d(TAG, "Hiding recent scans")
            recyclerRecentScans.visibility = RecyclerView.GONE
            btnToggleRecentScans.text = "📜 Recent Scans"
            Toast.makeText(this, "Hiding recent scans", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupClickListeners() {
        findViewById<MaterialButton>(R.id.btnClose).setOnClickListener {
            finish()
        }
        
        findViewById<MaterialButton>(R.id.btnManualEntry).setOnClickListener {
            startActivity(Intent(this, CalorieEntryActivity::class.java))
            finish()
        }
        
        btnToggleRecentScans.setOnClickListener {
            Log.d(TAG, "Recent Scans button clicked!")
            Toast.makeText(this, "Recent Scans button clicked!", Toast.LENGTH_SHORT).show()
            toggleRecentScans()
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                        runOnUiThread {
                            handleBarcodeResult(barcode)
                        }
                    })
                }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun handleBarcodeResult(barcode: String) {
        // Prevent processing the same barcode multiple times
        if (isProcessingBarcode || barcode == lastProcessedBarcode) {
            return
        }
        
        isProcessingBarcode = true
        lastProcessedBarcode = barcode
        
        lifecycleScope.launch {
            try {
                val foodItem = repository.getFoodItemByBarcode(barcode)
                if (foodItem != null) {
                    // Food found in database, start entry activity with pre-filled data
                    val intent = Intent(this@BarcodeScanActivity, CalorieEntryActivity::class.java).apply {
                        putExtra("food_name", foodItem.name)
                        putExtra("calories", foodItem.caloriesPerServing)
                        putExtra("protein", foodItem.proteinPerServing)
                        putExtra("carbs", foodItem.carbsPerServing)
                        putExtra("fat", foodItem.fatPerServing)
                        putExtra("fiber", foodItem.fiberPerServing)
                        putExtra("sugar", foodItem.sugarPerServing)
                        putExtra("sodium", foodItem.sodiumPerServing)
                        putExtra("barcode", barcode)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // Food not found in local database
                    if (repository.isNetworkAvailable()) {
                        // Try to fetch from API if network is available
                        Toast.makeText(this@BarcodeScanActivity, 
                            "Food not found locally. Searching online...", 
                            Toast.LENGTH_SHORT).show()
                        
                        try {
                            val onlineFoodItem = repository.searchFoodOnline(barcode)
                            if (onlineFoodItem != null) {
                                // Found online, save to database and proceed
                                repository.addFoodItem(onlineFoodItem)
                                val intent = Intent(this@BarcodeScanActivity, CalorieEntryActivity::class.java).apply {
                                    putExtra("food_name", onlineFoodItem.name)
                                    putExtra("calories", onlineFoodItem.caloriesPerServing)
                                    putExtra("protein", onlineFoodItem.proteinPerServing)
                                    putExtra("carbs", onlineFoodItem.carbsPerServing)
                                    putExtra("fat", onlineFoodItem.fatPerServing)
                                    putExtra("fiber", onlineFoodItem.fiberPerServing)
                                    putExtra("sugar", onlineFoodItem.sugarPerServing)
                                    putExtra("sodium", onlineFoodItem.sodiumPerServing)
                                    putExtra("barcode", barcode)
                                }
                                startActivity(intent)
                                finish()
                            } else {
                                // Not found online either
                                showNotFoundDialog(barcode)
                            }
                        } catch (e: Exception) {
                            showNetworkErrorDialog(barcode)
                        }
                    } else {
                        // No network available
                        showOfflineDialog(barcode)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@BarcodeScanActivity, 
                    "Error processing barcode: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
                // Reset processing flag on error
                isProcessingBarcode = false
            }
        }
    }
    
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner?.close()
    }
    
    private fun showNotFoundDialog(barcode: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Food Not Found")
            .setMessage("This barcode ($barcode) was not found in our database or online. Would you like to add it manually?")
            .setPositiveButton("Add Manually") { _, _ ->
                val intent = Intent(this, CalorieEntryActivity::class.java).apply {
                    putExtra("barcode", barcode)
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Scan Again") { _, _ ->
                // Reset processing flag to allow scanning again
                isProcessingBarcode = false
                lastProcessedBarcode = null
            }
            .setNeutralButton("Cancel") { _, _ ->
                finish()
            }
            .show()
    }
    
    private fun showNetworkErrorDialog(barcode: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Network Error")
            .setMessage("Could not search online for this barcode due to network issues. The barcode will be saved for later sync when internet is available.")
            .setPositiveButton("Add Manually") { _, _ ->
                lifecycleScope.launch {
                    repository.queueBarcodeForSync(barcode)
                }
                val intent = Intent(this, CalorieEntryActivity::class.java).apply {
                    putExtra("barcode", barcode)
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Try Again") { _, _ ->
                // Reset processing flag and try again
                isProcessingBarcode = false
                lastProcessedBarcode = null
                handleBarcodeResult(barcode)
            }
            .setNeutralButton("Cancel") { _, _ ->
                finish()
            }
            .show()
    }
    
    private fun showOfflineDialog(barcode: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Offline Mode")
            .setMessage("This food is not in our offline database. The barcode will be saved and automatically looked up when you're back online.")
            .setPositiveButton("Add Manually") { _, _ ->
                lifecycleScope.launch {
                    repository.queueBarcodeForSync(barcode)
                }
                val intent = Intent(this, CalorieEntryActivity::class.java).apply {
                    putExtra("barcode", barcode)
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Scan Again") { _, _ ->
                // Reset processing flag to allow scanning again
                isProcessingBarcode = false
                lastProcessedBarcode = null
            }
            .setNeutralButton("Cancel") { _, _ ->
                finish()
            }
            .show()
    }
    
    private inner class BarcodeAnalyzer(private val onBarcodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
        
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                barcodeScanner?.process(image)
                    ?.addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { value ->
                                onBarcodeDetected(value)
                                return@addOnSuccessListener
                            }
                        }
                    }
                    ?.addOnFailureListener {
                        Log.e(TAG, "Barcode scanning failed", it)
                    }
                    ?.addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
    
    companion object {
        private const val TAG = "BarcodeScanActivity"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}