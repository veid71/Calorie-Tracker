package com.calorietracker

// 📦 IMPORT ANDROID FRAMEWORK COMPONENTS
// These are the core building blocks that Android provides for us to use
import android.Manifest                        // 🔒 Permission constants (like asking to send notifications)
import android.content.pm.PackageManager       // 📱 Checks what permissions the app has been granted
import android.os.Build                        // 📱 Information about the user's Android device and version
import android.os.Bundle                       // 💾 Saves and restores activity state (like during screen rotation)
import android.util.Log                        // 📝 Logging system for debugging (like console.log in web development)
import android.view.View                       // 📺 Basic building block for everything visible on screen
import androidx.core.app.ActivityCompat        // 🔒 Modern permission request system
import androidx.core.content.ContextCompat     // 🔒 Check permissions in a backwards-compatible way
import android.widget.EditText                 // ✏️ Simple text input box
import android.widget.ProgressBar              // 📊 Visual progress indicator (loading bar)
import android.widget.TextView                 // 📝 Text display component
import androidx.appcompat.app.AlertDialog      // 🗨️ Popup dialog boxes for user interaction
import androidx.appcompat.app.AppCompatActivity // 📱 Base class for app screens with modern features
import androidx.lifecycle.lifecycleScope       // ⚡ Manages background tasks that are lifecycle-aware

// 🗄 OUR APP'S DATABASE COMPONENTS
import com.calorietracker.database.CalorieDatabase     // 🗃️ Our main nutrition database
import com.calorietracker.database.FoodDatabaseStatus  // 📊 Tracks download status of food databases

// 🌍 OUR APP'S NETWORK COMPONENTS
import com.calorietracker.network.FoodDatabaseManager   // 📋 Handles downloading food databases from the internet

// 🎨 OUR APP'S UI COMPONENTS
import com.calorietracker.utils.ThemeManager            // 🌙 Manages dark/light theme switching

// 📱 MATERIAL DESIGN COMPONENTS (Google's pretty UI library)
import com.google.android.material.button.MaterialButton        // 🔘 Modern, pretty buttons
import com.google.android.material.card.MaterialCardView        // 📋 Rounded rectangle containers
import com.google.android.material.snackbar.Snackbar           // 🍭 Bottom popup messages (like Toast but fancier)
import com.google.android.material.textfield.TextInputEditText  // ✏️ Pretty text input with floating labels
import com.calorietracker.sync.DatabaseDownloadForegroundService

// 💫 KOTLIN COROUTINES (for background processing)
import kotlinx.coroutines.flow.collectLatest    // 🔄 Listen for the latest updates in a data stream
import kotlinx.coroutines.launch                // 🚀 Start background tasks without blocking the UI

// 📅 JAVA DATE/TIME UTILITIES
import java.text.SimpleDateFormat               // 📊 Format timestamps for human-readable display
import java.util.*                             // 📅 Date, Calendar, and other utility classes

/**
 * 📋 DATABASE DOWNLOAD SCREEN - THE DATA DOWNLOAD MANAGER
 * 
 * Hey future programmer! Welcome to the "behind the scenes" part of our app!
 * This screen is like a download manager for nutrition databases.
 * 
 * 🍎 What does this screen do?
 * 1. Downloads the USDA food database (113,886+ foods with complete nutrition info)
 * 2. Downloads Open Food Facts database (barcode-scannable products)
 * 3. Shows download progress with pretty progress bars
 * 4. Handles API key setup for unlimited downloads
 * 5. Manages offline food databases for when there's no internet
 * 
 * 📊 Why do we need to download databases?
 * - The app needs nutrition data to auto-fill food information
 * - Downloading once = works offline forever (no internet required after download)
 * - USDA database has official government nutrition data (very accurate)
 * - Open Food Facts has barcode data for packaged foods
 * 
 * 🔑 Technical challenges this screen solves:
 * - API rate limiting (free APIs have usage limits)
 * - Large file downloads (databases are huge!)
 * - Background processing (downloads can't freeze the UI)
 * - Error handling (network failures, API changes, etc.)
 * - Progress tracking (users want to see download progress)
 * 
 * Think of this as the "librarian" that stocks our nutrition library!
 */
class DatabaseDownloadActivity : AppCompatActivity() {
    
    // 📝 CONSTANTS - Fixed values that don't change
    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1001

        /**
         * URL of the database_version.json file hosted in the GitHub repo.
         * Update this after pushing to GitHub:
         *   https://raw.githubusercontent.com/YOUR_USERNAME/CalorieTracker/main/database_version.json
         */
        private const val PREBUILT_VERSION_URL =
            "https://raw.githubusercontent.com/veid71/Calorie-Tracker/main/database_version.json"
    }

    // 📋 DATA MANAGERS - The "workers" that handle the heavy lifting
    private lateinit var databaseManager: FoodDatabaseManager  // 📋 Downloads and manages food databases
    private lateinit var database: CalorieDatabase            // 🗃️ Our local database for storing everything
    
    // 📊 DATABASE STATUS DISPLAY ELEMENTS - Show info about each database
    // These text views show users the current state of each food database
    private lateinit var tvUSDAStatus: TextView        // 🍎 "Downloaded", "Downloading...", "Error", etc.
    private lateinit var tvUSDACount: TextView         // 🔢 "113,886 foods" - how many items we have
    private lateinit var tvUSDALastUpdated: TextView  // 📅 "Updated Aug 31, 2024" - when we last downloaded
    private lateinit var tvOFFStatus: TextView         // 📷 Open Food Facts status (for barcode scanning)
    private lateinit var tvOFFCount: TextView          // 🔢 How many barcoded products we have
    private lateinit var tvOFFLastUpdated: TextView   // 📅 When Open Food Facts was last updated
    
    // 📊 PROGRESS DISPLAY ELEMENTS - Show download progress in real-time
    // These elements appear during downloads to keep users informed
    private lateinit var cardProgress: MaterialCardView    // 📋 Container that holds all progress info
    private lateinit var tvCurrentOperation: TextView      // 📝 "Downloading USDA database..." - what's happening now
    private lateinit var progressBarDownload: ProgressBar  // 📊 Visual progress bar (0% to 100%)
    private lateinit var tvProgressText: TextView          // 🔢 "15,234 / 113,886 items" - numerical progress
    
    // ⚡ Fast Download (pre-built database)
    private lateinit var tvFastDownloadStatus: TextView
    private lateinit var btnCheckForUpdate: MaterialButton
    private lateinit var btnStartFastDownload: MaterialButton
    private var pendingPrebuiltInfo: FoodDatabaseManager.PrebuiltDatabaseInfo? = null

    // 🔘 ACTION BUTTONS - What users can click to do things
    private lateinit var btnDownloadDatabases: MaterialButton
    private lateinit var btnDownloadUSDA: MaterialButton
    private lateinit var btnDownloadOFF: MaterialButton
    private lateinit var btnCancelDownload: MaterialButton
    private lateinit var btnResetUSDAStatus: MaterialButton
    private lateinit var btnSetupAPIKey: MaterialButton
    private lateinit var btnBack: MaterialButton

    /**
     * 🎆 THE GRAND OPENING: onCreate()
     * 
     * This method runs when the user opens the database download screen.
     * Think of it like setting up a construction site before the workers arrive!
     * 
     * Step-by-step setup process:
     * 1. Apply the user's preferred theme (dark/light mode)
     * 2. Load the visual layout from the XML file
     * 3. Connect to all the UI elements (buttons, text views, etc.)
     * 4. Set up database connections
     * 5. Prepare button click handlers
     * 6. Start monitoring download progress
     * 7. Clean up any old error messages from previous attempts
     * 8. Check if we can show download notifications
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // 🎨 STEP 1: Apply theme before anything visual happens
        ThemeManager.applyTheme(this)
        
        // 🏗️ STEP 2: Call parent class and load visual layout
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database_download)  // Load the XML layout file

        // 🔍 STEP 3: Find and connect to all UI elements
        initViews()              // Connect to buttons, text views, progress bars, etc.
        
        // 🗃️ STEP 4: Set up database and download manager
        setupDatabase()          // Create connections to our local database and download manager
        
        // 🔘 STEP 5: Set up what happens when buttons are pressed
        setupClickListeners()    // Tell each button what function to call when tapped
        
        // 📊 STEP 6: Start watching for download progress updates
        observeDownloadProgress() // Listen for progress changes and update the UI in real-time
        
        // 🧝 STEP 7: Clean up old error messages (background task)
        // Sometimes downloads fail and leave error messages. Let's clear those on startup.
        lifecycleScope.launch {
            clearOldErrorStatusesSync()                    // Remove old "temporarily unavailable" errors
            kotlinx.coroutines.delay(100)                  // Wait a tiny bit for database update
            loadDatabaseStatuses()                         // Refresh the status display
        }
        
        // 🔔 STEP 8: Ask for notification permission (so we can show download progress)
        checkNotificationPermission() // Modern Android requires permission to show notifications
    }

    /**
     * 🔍 FIND ALL THE UI ELEMENTS: initViews()
     * 
     * This function is like taking inventory of all the furniture in a room.
     * We need to find each button, text view, and progress bar in our layout
     * and connect them to variables in our code so we can control them.
     * 
     * Think of findViewById() like a game of "Where's Waldo?" - we're searching
     * for specific UI elements by their ID and storing references to them.
     */
    private fun initViews() {
        // 🍎 USDA DATABASE STATUS ELEMENTS
        // These show information about the government nutrition database
        tvUSDAStatus = findViewById(R.id.tvUSDAStatus)           // 🏆 "Downloaded" or "Error" or "Downloading..."
        tvUSDACount = findViewById(R.id.tvUSDACount)             // 🔢 "113,886 foods" - item count
        tvUSDALastUpdated = findViewById(R.id.tvUSDALastUpdated) // 📅 "Updated Aug 31, 2024" - last download date
        
        // 📷 OPEN FOOD FACTS STATUS ELEMENTS  
        // These show information about the barcode database
        tvOFFStatus = findViewById(R.id.tvOFFStatus)             // 🏆 Status of barcode database
        tvOFFCount = findViewById(R.id.tvOFFCount)               // 🔢 How many barcoded products we have
        tvOFFLastUpdated = findViewById(R.id.tvOFFLastUpdated)   // 📅 When barcode DB was last updated
        
        // 📊 PROGRESS TRACKING ELEMENTS
        // These appear during downloads to show real-time progress
        cardProgress = findViewById(R.id.cardProgress)               // 📋 Container that holds progress info (hidden when not downloading)
        tvCurrentOperation = findViewById(R.id.tvCurrentOperation)   // 📝 "Downloading USDA database..." - what's happening right now
        progressBarDownload = findViewById(R.id.progressBarDownload) // 📊 Visual progress bar (fills from 0% to 100%)
        tvProgressText = findViewById(R.id.tvProgressText)           // 🔢 "15,234 / 113,886 items" - numerical progress
        
        // ⚡ Fast download views
        tvFastDownloadStatus = findViewById(R.id.tvFastDownloadStatus)
        btnCheckForUpdate    = findViewById(R.id.btnCheckForUpdate)
        btnStartFastDownload = findViewById(R.id.btnStartFastDownload)

        // 🔘 ACTION BUTTONS
        btnDownloadDatabases = findViewById(R.id.btnDownloadDatabases)
        btnDownloadUSDA      = findViewById(R.id.btnDownloadUSDA)
        btnDownloadOFF       = findViewById(R.id.btnDownloadOFF)
        btnCancelDownload    = findViewById(R.id.btnCancelDownload)
        btnResetUSDAStatus   = findViewById(R.id.btnResetUSDAStatus)
        btnSetupAPIKey       = findViewById(R.id.btnSetupAPIKey)
        btnBack              = findViewById(R.id.btnBack)
    }

    /**
     * 🗃️ SET UP DATABASE CONNECTIONS: setupDatabase()
     * 
     * This creates our two main "workers":
     * 1. The local database (our digital filing cabinet)
     * 2. The download manager (our internet data fetcher)
     * 
     * Think of this like hiring a librarian (database) and a book buyer (download manager)
     * to work together in managing our nutrition data library!
     */
    private fun setupDatabase() {
        // 🗃️ Create connection to our local SQLite database
        // This is where we store all the nutrition data on the user's device
        database = CalorieDatabase.getDatabase(this)
        
        // 📋 Create our download manager
        // This handles fetching data from USDA and Open Food Facts APIs
        databaseManager = FoodDatabaseManager(this, database)
    }

    private fun setupClickListeners() {

        btnCheckForUpdate.setOnClickListener {
            checkForPrebuiltDatabase()
        }

        btnStartFastDownload.setOnClickListener {
            pendingPrebuiltInfo?.let { startPrebuiltDownload(it) }
        }

        btnDownloadDatabases.setOnClickListener {
            startDatabaseDownload()
        }
        
        btnDownloadUSDA.setOnClickListener {
            startUSDADownload()
        }
        
        btnDownloadOFF.setOnClickListener {
            startOpenFoodFactsDownload()
        }
        
        btnCancelDownload.setOnClickListener {
            DatabaseDownloadForegroundService.stop(this)
            databaseManager.cancelDownload()
            databaseManager.resetDownloadProgress()
            hideProgressCard()
            
            // Re-enable all buttons
            btnDownloadDatabases.isEnabled = true
            btnDownloadUSDA.isEnabled = true
            btnDownloadOFF.isEnabled = true
            
            Snackbar.make(
                findViewById(android.R.id.content),
                "Download cancelled and progress cleared",
                Snackbar.LENGTH_SHORT
            ).show()
        }
        
        btnResetUSDAStatus.setOnClickListener {
            resetUSDAStatus()
        }
        
        btnSetupAPIKey.setOnClickListener {
            showAPIKeyInputDialog()
        }
        
        btnBack.setOnClickListener {
            finish()
        }
    }

    /**
     * 📹 START MONITORING DOWNLOADS: observeDownloadProgress()
     * 
     * This sets up two "security cameras" that watch for changes and update the UI:
     * 1. Progress Monitor - watches download progress (15,234 / 113,886 items)
     * 2. Status Monitor - watches database status ("Downloading", "Complete", "Error")
     * 
     * Think of it like having two assistants who constantly watch the download process
     * and report back to us so we can update what the user sees on screen.
     * 
     * The "collectLatest" means "give me the most recent update and ignore old ones"
     * - like having a news feed that always shows the latest information.
     */
    private fun observeDownloadProgress() {
        // 📊 PROGRESS MONITOR - Watch for download progress changes
        // This updates the progress bar and "15,234 / 113,886 items" text
        lifecycleScope.launch {
            databaseManager.downloadProgress.collectLatest { progress ->
                updateProgressUI(progress) // Update progress bar and text when progress changes
            }
        }
        
        // 🏆 STATUS MONITOR - Watch for database status changes  
        // This updates the "Downloaded", "Error", "Downloading..." status indicators
        lifecycleScope.launch {
            databaseManager.getDownloadStatuses().collectLatest { statuses ->
                updateStatusUI(statuses) // Update status badges when database status changes
            }
        }
    }

    private fun startDatabaseDownload() {
        // Route through the foreground service so Android cannot freeze the process
        // when the screen turns off mid-download (same pattern as startOpenFoodFactsDownload).
        clearUSDAErrorStatus()
        DatabaseDownloadForegroundService.startAll(this)
        showProgressCard()
        btnDownloadDatabases.isEnabled = false
        btnDownloadUSDA.isEnabled = false
        btnDownloadOFF.isEnabled = false
    }

    private fun startUSDADownload() {
        lifecycleScope.launch {
            // Bulk CSV downloads don't require API keys
            Log.d("DatabaseDownloadActivity", "Starting USDA bulk download (no API key required)")
            
            Log.d("DatabaseDownloadActivity", "Valid API key found, proceeding with download")
            
            // Clear any previous error status to allow retry
            clearUSDAErrorStatus()
            
            showProgressCard()
            btnDownloadDatabases.isEnabled = false
            btnDownloadUSDA.isEnabled = false
            btnDownloadOFF.isEnabled = false
            
            val result = databaseManager.downloadUSDADatabaseOnly()
            
            if (result.isSuccess) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "USDA database downloaded successfully!",
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "USDA download failed: $errorMessage",
                    Snackbar.LENGTH_LONG
                ).show()
                
                // If it's a rate limit error, show API key setup option
                if (errorMessage.contains("rate limit", ignoreCase = true)) {
                    showAPIKeySetupOption()
                }
            }
            
            btnDownloadDatabases.isEnabled = true
            btnDownloadUSDA.isEnabled = true
            btnDownloadOFF.isEnabled = true
            hideProgressCard()
        }
    }

    private fun startOpenFoodFactsDownload() {
        // Launch as a foreground service so Android cannot freeze the process
        // when the screen turns off. Progress is reported via downloadProgress StateFlow.
        DatabaseDownloadForegroundService.start(this)
        showProgressCard()
        btnDownloadDatabases.isEnabled = false
        btnDownloadUSDA.isEnabled = false
        btnDownloadOFF.isEnabled = false
    }

    private fun loadDatabaseStatuses() {
        lifecycleScope.launch {
            val statuses = database.foodDatabaseStatusDao().getAllStatuses()
            updateStatusUI(statuses)
        }
    }

    private fun updateProgressUI(progress: FoodDatabaseManager.DownloadProgress) {
        if (progress.isDownloading) {
            showProgressCard()
            tvCurrentOperation.text = progress.currentOperation
            
            val progressPercentage = if (progress.totalItems > 0) {
                (progress.downloadedItems * 100 / progress.totalItems)
            } else {
                0
            }
            
            progressBarDownload.progress = progressPercentage
            tvProgressText.text = "${progress.downloadedItems} / ${progress.totalItems} items"
            
        } else {
            if (progress.isComplete) {
                tvCurrentOperation.text = "Download completed!"
                progressBarDownload.progress = 100
                btnDownloadDatabases.isEnabled = true
                btnDownloadUSDA.isEnabled = true
                btnDownloadOFF.isEnabled = true
                // Auto-hide progress card after 2 seconds
                lifecycleScope.launch {
                    kotlinx.coroutines.delay(2000)
                    hideProgressCard()
                }
            } else if (progress.error != null) {
                tvCurrentOperation.text = "Error: ${progress.error}"
                btnDownloadDatabases.isEnabled = true
                btnDownloadUSDA.isEnabled = true
                btnDownloadOFF.isEnabled = true
                // Auto-hide progress card after 3 seconds on error
                lifecycleScope.launch {
                    kotlinx.coroutines.delay(3000)
                    hideProgressCard()
                }
            } else {
                // Default case - no download active
                hideProgressCard()
            }
        }
    }

    private fun updateStatusUI(statuses: List<FoodDatabaseStatus>) {
        val usdaStatus = statuses.find { it.databaseName == "usda" }
        val offStatus = statuses.find { it.databaseName == "openfoodfacts" }
        
        // Update USDA status
        if (usdaStatus != null) {
            tvUSDAStatus.text = when {
                usdaStatus.isDownloading -> "Downloading..."
                usdaStatus.isComplete -> {
                    if (usdaStatus.errorMessage?.contains("temporarily unavailable", ignoreCase = true) == true) {
                        "Alternative Data"
                    } else {
                        "Downloaded"
                    }
                }
                usdaStatus.errorMessage != null -> {
                    when {
                        usdaStatus.errorMessage.contains("temporarily unavailable", ignoreCase = true) -> "API Unavailable"
                        usdaStatus.errorMessage.contains("bulk download", ignoreCase = true) -> "Using Bulk Data"
                        else -> "Error"
                    }
                }
                else -> "Not Downloaded"
            }
            
            tvUSDAStatus.setBackgroundColor(
                getColor(when {
                    usdaStatus.isDownloading -> R.color.accent_orange
                    usdaStatus.isComplete -> {
                        if (usdaStatus.errorMessage?.contains("temporarily unavailable", ignoreCase = true) == true) {
                            R.color.accent_orange // Alternative data - use orange to show it's different
                        } else {
                            R.color.success_green // Normal success
                        }
                    }
                    usdaStatus.errorMessage != null -> {
                        when {
                            usdaStatus.errorMessage.contains("temporarily unavailable", ignoreCase = true) -> R.color.accent_orange
                            usdaStatus.errorMessage.contains("bulk download", ignoreCase = true) -> R.color.success_green
                            else -> R.color.error_red
                        }
                    }
                    else -> R.color.light_gray
                })
            )
            
            tvUSDACount.text = "${usdaStatus.downloadedItems} items"
            
            tvUSDALastUpdated.text = if (usdaStatus.lastDownloadDate != null) {
                val updateText = "Updated ${formatDate(usdaStatus.lastDownloadDate)}"
                if (usdaStatus.errorMessage?.contains("temporarily unavailable", ignoreCase = true) == true) {
                    "$updateText (Alternative)"
                } else {
                    updateText
                }
            } else {
                "Never updated"
            }
        }
        
        // Update Open Food Facts status
        if (offStatus != null) {
            tvOFFStatus.text = when {
                offStatus.isDownloading -> "Downloading..."
                offStatus.isComplete -> "Downloaded"
                offStatus.errorMessage != null -> "Error"
                else -> "Not Downloaded"
            }
            
            tvOFFStatus.setBackgroundColor(
                getColor(when {
                    offStatus.isDownloading -> R.color.accent_orange
                    offStatus.isComplete -> R.color.success_green
                    offStatus.errorMessage != null -> R.color.error_red
                    else -> R.color.light_gray
                })
            )
            
            tvOFFCount.text = "${offStatus.downloadedItems} items"
            
            tvOFFLastUpdated.text = if (offStatus.lastDownloadDate != null) {
                "Updated ${formatDate(offStatus.lastDownloadDate)}"
            } else {
                "Never updated"
            }
        }
    }

    private fun showProgressCard() {
        cardProgress.visibility = View.VISIBLE
    }

    private fun hideProgressCard() {
        cardProgress.visibility = View.GONE
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    private fun showAPIKeyWarning() {
        AlertDialog.Builder(this)
            .setTitle("USDA API Key Required")
            .setMessage("""
                The app is currently using DEMO_KEY which has severe limitations:
                • Only 5 requests per hour
                • Cannot download full database
                
                To download the USDA food database, you need a free API key:
                
                1. Visit https://fdc.nal.usda.gov/api-key-signup.html
                2. Sign up for free
                3. Get your API key
                4. Enter it below
                
                Would you like to set up your API key now?
            """.trimIndent())
            .setPositiveButton("Set Up API Key") { _, _ ->
                showAPIKeyInputDialog()
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Download Limited Data") { _, _ ->
                // Allow download with DEMO_KEY but limited data
                proceedWithLimitedDownload()
            }
            .show()
    }
    
    private fun showAPIKeyInputDialog() {
        val input = EditText(this)
        input.hint = "Enter your USDA API key"
        
        AlertDialog.Builder(this)
            .setTitle("Enter USDA API Key")
            .setMessage("Get your free API key from:\nhttps://fdc.nal.usda.gov/api-key-signup.html")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val apiKey = input.text.toString().trim()
                if (apiKey.isNotBlank() && apiKey != "DEMO_KEY") {
                    databaseManager.setUSDAApiKey(apiKey)
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "API key saved! You can now download the full database.",
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Please enter a valid API key",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAPIKeySetupOption() {
        AlertDialog.Builder(this)
            .setTitle("Rate Limit Exceeded")
            .setMessage("The download failed due to API rate limits. Would you like to set up a free USDA API key for unlimited downloads?")
            .setPositiveButton("Set Up API Key") { _, _ ->
                showAPIKeyInputDialog()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun proceedWithLimitedDownload() {
        lifecycleScope.launch {
            showProgressCard()
            btnDownloadDatabases.isEnabled = false
            
            Snackbar.make(
                findViewById(android.R.id.content),
                "Downloading limited dataset with DEMO_KEY...",
                Snackbar.LENGTH_SHORT
            ).show()
            
            val result = databaseManager.downloadAllDatabases()
            
            if (result.isSuccess) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Limited database downloaded successfully!",
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Download failed: ${result.exceptionOrNull()?.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
            
            btnDownloadDatabases.isEnabled = true
            hideProgressCard()
        }
    }
    
    private fun debugDatabaseCounts() {
        lifecycleScope.launch {
            try {
                val usdaCount = database.usdaFoodItemDao().getCount()
                val offCount = database.openFoodFactsDao().getCount()
                android.util.Log.d("DatabaseDownloadActivity", "DEBUG: USDA count = $usdaCount, OFF count = $offCount")
            } catch (e: Exception) {
                android.util.Log.e("DatabaseDownloadActivity", "DEBUG: Error getting counts", e)
            }
        }
    }
    
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS)) {
                    
                    // Show explanation
                    AlertDialog.Builder(this)
                        .setTitle("Notification Permission")
                        .setMessage("This app needs notification permission to show download progress. Would you like to enable it?")
                        .setPositiveButton("Enable") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                REQUEST_NOTIFICATION_PERMISSION
                            )
                        }
                        .setNegativeButton("Skip", null)
                        .show()
                } else {
                    // Request permission directly
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_NOTIFICATION_PERMISSION
                    )
                }
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Notifications enabled! You'll see download progress.",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Notification permission denied. Download progress won't be shown in notifications.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        debugDatabaseCounts()
        loadDatabaseStatuses()
        
        // Show debug info in UI
        showDebugInfo()
    }
    
    private fun showDebugInfo() {
        lifecycleScope.launch {
            try {
                val usdaCount = database.usdaFoodItemDao().getCount()
                val offCount = database.openFoodFactsDao().getCount()
                
                // Show counts as a subtle toast for debugging
                android.widget.Toast.makeText(
                    this@DatabaseDownloadActivity,
                    "Debug: USDA=$usdaCount, OFF=$offCount items in DB",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    this@DatabaseDownloadActivity,
                    "Debug: Error checking counts - ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private suspend fun clearOldErrorStatusesSync() {
        try {
            // Check current USDA status
            val currentStatus = database.foodDatabaseStatusDao().getStatus("usda")
            
            // If there's an error status (like "temporarily unavailable"), clear it
            if (currentStatus?.errorMessage?.contains("temporarily unavailable", ignoreCase = true) == true ||
                currentStatus?.errorMessage?.contains("API", ignoreCase = true) == true) {
                
                Log.d("DatabaseDownloadActivity", "Clearing old USDA error status: ${currentStatus.errorMessage}")
                
                // Reset to a clean state
                database.foodDatabaseStatusDao().insertStatus(
                    FoodDatabaseStatus(
                        databaseName = "usda",
                        isDownloading = false,
                        isComplete = false,
                        errorMessage = null,
                        totalItems = 0,
                        downloadedItems = 0,
                        lastDownloadDate = null
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("DatabaseDownloadActivity", "Error clearing old error statuses", e)
        }
    }

    private fun clearOldErrorStatuses() {
        lifecycleScope.launch {
            clearOldErrorStatusesSync()
        }
    }
    
    private fun resetUSDAStatus() {
        lifecycleScope.launch {
            try {
                Log.d("DatabaseDownloadActivity", "Manual USDA status reset requested")
                
                // Cancel any ongoing download first
                databaseManager.cancelDownload()
                
                // Force delete all USDA entries first
                database.foodDatabaseStatusDao().deleteStatus("usda")
                
                // Insert fresh status
                database.foodDatabaseStatusDao().insertStatus(
                    FoodDatabaseStatus(
                        databaseName = "usda",
                        isDownloading = false,
                        isComplete = false,
                        errorMessage = null,
                        totalItems = 0,
                        downloadedItems = 0,
                        lastDownloadDate = null
                    )
                )
                
                // Also reset the download progress state
                databaseManager.resetDownloadProgress()
                
                // Hide any progress cards that might be stuck
                hideProgressCard()
                
                // Re-enable all download buttons
                btnDownloadDatabases.isEnabled = true
                btnDownloadUSDA.isEnabled = true
                btnDownloadOFF.isEnabled = true
                
                // Refresh the UI
                loadDatabaseStatuses()
                
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "USDA status reset successfully! You can now retry the download.",
                    Snackbar.LENGTH_SHORT
                ).show()
                
                Log.d("DatabaseDownloadActivity", "USDA status reset completed")
                
            } catch (e: Exception) {
                Log.e("DatabaseDownloadActivity", "Error resetting USDA status", e)
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Error resetting status: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun clearUSDAErrorStatus() {
        lifecycleScope.launch {
            try {
                // Reset USDA status to clear any previous errors
                database.foodDatabaseStatusDao().insertStatus(
                    FoodDatabaseStatus(
                        databaseName = "usda",
                        isDownloading = false,
                        isComplete = false,
                        errorMessage = null,
                        totalItems = 0,
                        downloadedItems = 0,
                        lastDownloadDate = null
                    )
                )
            } catch (e: Exception) {
                Log.e("DatabaseDownloadActivity", "Error clearing USDA status", e)
            }
        }
    }
    
    // ── Fast / Pre-built Database Download ────────────────────────────────────

    private fun checkForPrebuiltDatabase() {
        if (PREBUILT_VERSION_URL.contains("YOUR_USERNAME")) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "GitHub not configured yet — see setup instructions",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }
        btnCheckForUpdate.isEnabled = false
        tvFastDownloadStatus.text = "Checking for updates..."
        lifecycleScope.launch {
            val info = databaseManager.checkForPrebuiltDatabase(PREBUILT_VERSION_URL)
            if (info != null) {
                pendingPrebuiltInfo = info
                val sizeMb = info.fileSizeBytes / 1_048_576
                tvFastDownloadStatus.text =
                    "Update available: ${info.version} — %,d products, ${sizeMb} MB".format(info.productCount)
                btnStartFastDownload.visibility = View.VISIBLE
            } else {
                val prefs = getSharedPreferences("prebuilt_db_prefs", MODE_PRIVATE)
                val installed = prefs.getString("installed_version", null)
                tvFastDownloadStatus.text = if (installed != null)
                    "Already up to date (version $installed)"
                else
                    "No pre-built database published yet — check back later"
                btnStartFastDownload.visibility = View.GONE
            }
            btnCheckForUpdate.isEnabled = true
        }
    }

    private fun startPrebuiltDownload(info: FoodDatabaseManager.PrebuiltDatabaseInfo) {
        pendingPrebuiltInfo = null
        btnStartFastDownload.visibility = View.GONE
        btnCheckForUpdate.isEnabled = false
        btnDownloadDatabases.isEnabled = false
        btnDownloadUSDA.isEnabled = false
        btnDownloadOFF.isEnabled = false
        showProgressCard()
        tvFastDownloadStatus.text = "Downloading ${info.version}..."

        lifecycleScope.launch {
            val result = databaseManager.downloadPrebuiltDatabase(info)
            if (result.isSuccess) {
                tvFastDownloadStatus.text = "Installed — version ${info.version}"
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Food database installed successfully!",
                    Snackbar.LENGTH_LONG
                ).show()
                loadDatabaseStatuses()
            } else {
                tvFastDownloadStatus.text = "Download failed — tap 'Check for Update' to retry"
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Download failed: ${result.exceptionOrNull()?.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
            btnCheckForUpdate.isEnabled = true
            btnDownloadDatabases.isEnabled = true
            btnDownloadUSDA.isEnabled = true
            btnDownloadOFF.isEnabled = true
            hideProgressCard()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseManager.cleanup()
    }
}