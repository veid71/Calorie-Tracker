package com.calorietracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

/**
 * 📱 QR CODE SCANNER ACTIVITY - VERSATILE QR CODE SCANNING
 * 
 * Hey future programmer! This activity provides QR code scanning functionality
 * that can be used throughout the app for different purposes.
 * 
 * 🎯 What this scanner can do:
 * - 📋 **Recipe Import**: Scan QR codes containing recipe sharing links
 * - 🔗 **General Links**: Handle any QR code with URL or text content
 * - 📷 **Camera Integration**: Uses ZXing library for reliable scanning
 * - 🛡️ **Permission Handling**: Properly requests camera permissions
 * 
 * 🔧 How it works:
 * 1. Receives intent with scanning mode and title
 * 2. Requests camera permission if needed
 * 3. Launches ZXing scanner with custom configuration
 * 4. Returns scanned data to calling activity
 * 
 * 🚀 Usage Examples:
 * - Recipe sharing: scan someone's recipe QR code to import
 * - Food lookup: future feature to scan product QR codes
 * - Settings import: scan QR codes for app configuration
 */
class QRCodeScanActivity : AppCompatActivity() {
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
    
    private var scanMode: String? = null
    private var scanTitle: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 📥 GET CONFIGURATION FROM CALLING ACTIVITY
        scanMode = intent.getStringExtra("scan_mode") ?: "general"
        scanTitle = intent.getStringExtra("title") ?: "Scan QR Code"
        
        // 📷 CHECK CAMERA PERMISSION AND START SCANNING
        if (hasCameraPermission()) {
            startQRScanning()
        } else {
            requestCameraPermission()
        }
    }
    
    /**
     * 🔍 START THE QR CODE SCANNING PROCESS
     * 
     * Configures and launches the ZXing scanner with settings optimized
     * for our specific use case (recipe sharing, food lookup, etc.)
     */
    private fun startQRScanning() {
        val integrator = IntentIntegrator(this)
        
        // 🎨 CUSTOMIZE SCANNER APPEARANCE AND BEHAVIOR
        integrator.apply {
            setPrompt(scanTitle) // Custom title for user guidance
            setBeepEnabled(true) // Audio feedback on successful scan
            setBarcodeImageEnabled(true) // Save image of scanned code
            setOrientationLocked(true) // Lock to portrait for consistency
            setCaptureActivity(CaptureActivityPortrait::class.java) // Custom capture activity
        }
        
        // 🚀 LAUNCH THE SCANNER
        integrator.initiateScan()
    }
    
    /**
     * 📱 HANDLE SCANNER RESULT
     * 
     * Processes the scanned QR code data and returns it to the calling activity.
     * Different scan modes can handle results differently if needed.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        
        if (result.contents != null) {
            // ✅ SUCCESSFUL SCAN - RETURN DATA TO CALLER
            val scannedData = result.contents
            
            // 🎯 HANDLE DIFFERENT SCAN MODES
            when (scanMode) {
                "recipe_import" -> {
                    if (isValidRecipeData(scannedData)) {
                        returnScanResult(scannedData)
                    } else {
                        showErrorAndRetry("This doesn't appear to be a valid recipe QR code. Try scanning a recipe shared from CalorieTracker.")
                    }
                }
                "general" -> {
                    returnScanResult(scannedData)
                }
                else -> {
                    returnScanResult(scannedData)
                }
            }
        } else {
            // ❌ SCAN CANCELLED OR FAILED
            if (result.formatName != null) {
                Toast.makeText(this, "Scan failed: ${result.formatName}", Toast.LENGTH_SHORT).show()
            }
            setResult(RESULT_CANCELED)
            finish()
        }
        
        super.onActivityResult(requestCode, resultCode, data)
    }
    
    /**
     * 🔐 CAMERA PERMISSION HANDLING
     */
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startQRScanning()
                } else {
                    Toast.makeText(this, "Camera permission required for QR scanning", Toast.LENGTH_LONG).show()
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        }
    }
    
    /**
     * ✅ RETURN SUCCESSFUL SCAN RESULT
     */
    private fun returnScanResult(scannedData: String) {
        val resultIntent = Intent().apply {
            putExtra("scanned_data", scannedData)
            putExtra("scan_mode", scanMode)
        }
        
        Toast.makeText(this, "QR Code scanned successfully!", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    /**
     * ⚠️ SHOW ERROR AND OFFER TO RETRY
     */
    private fun showErrorAndRetry(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        // Give option to try again
        val retryIntent = Intent().apply {
            putExtra("error_message", message)
        }
        setResult(RESULT_CANCELED, retryIntent)
        finish()
    }
    
    /**
     * 🔍 VALIDATE RECIPE QR CODE DATA
     * 
     * Checks if scanned data contains valid recipe sharing information.
     * This prevents users from accidentally importing non-recipe QR codes.
     */
    private fun isValidRecipeData(data: String): Boolean {
        return when {
            // Deep link format
            data.contains("calorietracker://recipe/share/") -> true
            // JSON recipe format
            data.contains("\"recipe\":") && data.contains("\"ingredients\":") -> true
            // Plain text recipe format
            data.contains("📋 INGREDIENTS:") || data.contains("Ingredients:") -> true
            else -> false
        }
    }
}

/**
 * 📷 CUSTOM CAPTURE ACTIVITY FOR PORTRAIT ORIENTATION
 * 
 * This extends the ZXing capture activity to lock orientation to portrait
 * for better user experience in our app.
 */
class CaptureActivityPortrait : com.journeyapps.barcodescanner.CaptureActivity()