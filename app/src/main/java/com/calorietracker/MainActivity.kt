package com.calorietracker

// These are like importing tools or ingredients we need to build our app
// Each "import" line tells Android what code libraries we want to use
import android.content.Intent                      // Helps us navigate between different screens
import android.util.Log                           // Proper Android logging (visible in logcat)
import android.os.Bundle                          // Saves the app's state when screen rotates
import android.os.Handler
import android.os.Looper
import android.text.Html                          // Formats text with HTML-like styling
import android.view.View                          // Basic building block for everything you see on screen
import android.widget.ImageView                   // Displays images and icons
import android.widget.LinearLayout                // Layout that arranges items in a line
import android.widget.ProgressBar                 // The green progress bar that fills up
import android.widget.TextView                    // Text that shows numbers and words on screen
import android.widget.Toast                       // Small popup messages at bottom of screen
import androidx.appcompat.app.AppCompatActivity   // Basic template for an Android screen/activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder // Pretty popup dialogs
import androidx.lifecycle.lifecycleScope          // Helps us do background tasks safely
import androidx.recyclerview.widget.LinearLayoutManager  // Makes lists show items vertically
import androidx.recyclerview.widget.RecyclerView  // Smart list that can handle thousands of items
import com.google.android.material.card.MaterialCardView // Pretty rounded rectangles
import kotlinx.coroutines.flow.collect            // Helps us listen for data changes
import com.calorietracker.BuildConfig                    // App version and build information
import com.calorietracker.database.CalorieDatabase       // Our app's database (like a digital filing cabinet)
import com.calorietracker.database.DailyGoal            // Stores user's daily calorie target
import com.calorietracker.fitness.HealthConnectManager  // Talks to fitness trackers like smartwatches
import com.calorietracker.repository.CalorieRepository  // Helper that manages all our nutrition data
import com.calorietracker.repository.DailySummary       // Summary of daily calories and workouts
import com.calorietracker.sync.DataSyncService          // Downloads food databases in background
import com.calorietracker.utils.ThemeManager            // Manages dark/light theme switching
import com.calorietracker.widgets.WorkoutSummaryWidget  // Shows workout info and calorie bonus
import com.calorietracker.widgets.HealthMetricsWidget   // Shows BMI and health recommendations
import com.google.android.material.button.MaterialButton // Modern-looking buttons
import com.calorietracker.database.CalorieEntry         // Represents one food entry (like "1 apple, 95 calories")
import kotlinx.coroutines.launch                        // Lets us do tasks in background without freezing UI

/**
 * 🏠 MAIN ACTIVITY - THE HOME SCREEN OF OUR APP
 * 
 * Hi there, future programmer! This is the main screen (called an "Activity") 
 * of our Calorie Tracker app. Think of it like the home page of a website,
 * but for a mobile app.
 * 
 * What does this screen do?
 * 1. Shows how many calories you've eaten today (like a digital food diary)
 * 2. Shows how many calories you have left to eat (your daily budget)
 * 3. Has buttons to add new food (either by typing or scanning barcodes)
 * 4. Shows a list of everything you've eaten today
 * 5. Connects to your fitness tracker to add bonus calories from workouts
 * 
 * Think of this class like the "control room" for our entire app!
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // 📚 WHAT ARE THESE VARIABLES?
    // Think of these like ingredients we need to cook a meal, but for building our app screen!
    // "lateinit" means "we'll set this up later" - like saying "I'll get the flour when I need it"
    // "private" means only this class can use these variables (like keeping your diary private)

    // 🗃️ REPOSITORY: Our data manager (like a librarian who knows where everything is stored)
    private lateinit var repository: CalorieRepository
    
    // 📋 ADAPTER: Helps display lists of food entries (like a waiter showing you the menu)
    private lateinit var calorieEntryAdapter: CalorieEntryAdapter
    
    // 📱 UI ELEMENTS: These are all the things you see on your phone screen
    // Think of each one like a different part of a dashboard in a car
    private lateinit var tvCaloriesConsumed: TextView   // 🍎 Shows how many calories eaten today (like "500 calories")
    private lateinit var tvDailyGoal: TextView          // 🎯 Shows the daily calorie target (like "2000 calories")
    private lateinit var tvCaloriesRemaining: TextView  // ⏰ Shows how many calories left to eat (like "1500 remaining")
    private lateinit var tvWorkoutBonus: TextView       // 🏃 Shows bonus calories from workouts (like "+300 from gym")
    private lateinit var progressBarCalories: ProgressBar // 📊 Visual progress bar (like a health bar in video games)
    private lateinit var recyclerViewTodayEntries: RecyclerView // 📜 List of today's food entries (like a grocery receipt)
    private lateinit var tvNoEntries: TextView          // 💬 Message shown when no food entries exist ("Nothing here yet!")
    private lateinit var workoutSummaryWidget: WorkoutSummaryWidget // 💪 Widget showing workout verification
    private lateinit var healthMetricsWidget: HealthMetricsWidget // 📈 Widget showing health metrics like BMI
    
    // ⭐ FAVORITES UI: Quick access to favorite foods (like bookmarks in a web browser)
    private lateinit var cardQuickFavorites: MaterialCardView // The container that holds favorite foods
    private lateinit var recyclerFavorites: RecyclerView     // Horizontal scrolling list of favorites
    private lateinit var tvNoFavorites: TextView             // Message when user has no favorites yet
    private lateinit var favoritesAdapter: FavoritesQuickAdapter // Helper that displays favorite foods

    // 🔥 STREAK UI
    private lateinit var tvStreakCount: TextView
    private lateinit var tvStreakSubtitle: TextView
    
    // 🎯 USER'S GOAL: Stores the user's daily calorie goal (like a target to aim for)
    // The "?" means this can be empty if the user hasn't set a goal yet
    private var dailyGoal: DailyGoal? = null

    // Shared HealthConnectManager instance — created once, reused on every UI refresh
    private lateinit var healthConnectManager: HealthConnectManager

    // Debounce: coalesce rapid LiveData emissions into a single Health Connect call
    private val uiUpdateHandler = Handler(Looper.getMainLooper())
    private val uiUpdateRunnable = Runnable { executeUIUpdate() }
    
    /**
     * 🎬 THE OPENING SCENE: onCreate() 
     * 
     * This is like the app's "morning routine" - it runs every time the user opens this screen.
     * Think of it like setting up a lemonade stand: you need to arrange the cups, pour the lemonade,
     * put up your sign, and get ready for customers!
     * 
     * What happens here step by step:
     * 1. Set up the theme (dark or light mode)
     * 2. Load the visual layout (all the buttons and text you see)
     * 3. Connect to the database (our digital filing cabinet)
     * 4. Set up fitness tracking (connect to smartwatches)
     * 5. Prepare all the interactive elements
     * 6. Start listening for data changes
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // 📱 APPLICATION START
        // Initialize the main activity - this is where users see their calorie tracking dashboard
        
        // 🎨 STEP 1: Choose our theme (like picking light or dark wallpaper)
        ThemeManager.applyTheme(this)
        
        // 🏗️ STEP 2: Call the parent class (like asking for permission from your parents)
        super.onCreate(savedInstanceState)
        
        // 📋 STEP 3: Load our visual layout from the XML file (like assembling furniture from IKEA instructions)
        setContentView(R.layout.activity_main)

        // Keep screen on while app is open
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // 🗃️ STEP 4: Create our data manager (like hiring a librarian for our nutrition data)
        // This connects us to the database and gives us tools to save/load food entries
        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)
        
        // 💪 STEP 5: Set up fitness tracking (like connecting to your fitness watch)
        healthConnectManager = HealthConnectManager(this)
        initializeHealthConnect()
        
        // 🔐 STEP 5.5: Migrate API keys to secure storage (security enhancement)
        migrateLegacyApiKeys()
        
        // 🔧 STEP 6: Set up all the visual components (like arranging furniture in a room)
        initViews()           // Find all the text views, buttons, etc. in our layout
        setupRecyclerView()   // Set up the list that shows food entries
        setupClickListeners() // Tell each button what to do when pressed
        observeData()         // Start watching for changes in nutrition data (like a security camera)
        
        // 🎯 STEP 7: Set up default goals if this is the user's first time
        // This happens in the background so it doesn't slow down the app
        lifecycleScope.launch {
            // If user has never set a daily calorie goal, give them a reasonable default
            if (repository.getDailyGoalSync() == null) {
                repository.setDailyGoal(2000) // 2000 calories is a common daily target
            }
            
            // If user has never set nutrition goals (protein, carbs, etc.), create defaults
            if (repository.getNutritionGoalsSync() == null) {
                val defaultGoals = com.calorietracker.database.NutritionGoals()
                repository.updateNutritionGoals(defaultGoals)
            }
        }
        
        // 📥 STEP 8: Background data downloading (currently disabled to prevent crashes)
        // These would automatically download food databases when the app starts
        // We've temporarily disabled them because they can cause crashes on newer Android versions
        
        // 🏃 STEP 9: Sync today's workout data from fitness trackers
        // This happens in background so the app stays responsive
        lifecycleScope.launch {
            try {
                repository.syncTodaysWorkoutData()
                updateUI()
            } catch (e: Exception) {
                Log.e(TAG, "Workout sync failed", e)
            }
        }
    }
    
    /**
     * 🔍 FINDING OUR INGREDIENTS: initViews()
     * 
     * Imagine you're baking a cake and need to find all your ingredients from different cabinets.
     * That's what this function does - it finds all the visual elements (text, buttons, lists)
     * from our layout file and connects them to our code so we can control them.
     * 
     * Think of findViewById() like playing "Where's Waldo?" - we're searching for specific
     * elements by their ID and storing them in variables so we can use them later.
     */
    private fun initViews() {
        // 📊 FIND OUR CALORIE DISPLAY ELEMENTS
        // These show the numbers that matter most to users
        tvCaloriesConsumed = findViewById(R.id.tvCaloriesConsumed)     // 🍎 "750 calories eaten"
        tvDailyGoal = findViewById(R.id.tvDailyGoal)                  // 🎯 "Goal: 2000 calories"
        tvCaloriesRemaining = findViewById(R.id.tvCaloriesRemaining)  // ⏰ "1250 calories remaining"
        tvWorkoutBonus = findViewById(R.id.tvWorkoutBonus)            // 🏃 "+300 bonus from gym"
        progressBarCalories = findViewById(R.id.progressBarCalories)  // 📊 Green progress bar
        
        // 🏃 WORKOUT INFO ELEMENTS
        val layoutWorkoutBonus = findViewById<LinearLayout>(R.id.layoutWorkoutBonus)
        val ivWorkoutInfo = findViewById<ImageView>(R.id.ivWorkoutInfo)
        
        // Set up click handler for workout info icon
        ivWorkoutInfo.setOnClickListener {
            showWorkoutExplanationDialog()
        }
        
        // 📜 FIND OUR FOOD ENTRY LIST ELEMENTS
        recyclerViewTodayEntries = findViewById(R.id.recyclerViewTodayEntries) // The actual list
        tvNoEntries = findViewById(R.id.tvNoEntries)                  // "No food entries yet" message
        
        // 🖼️ FIND OUR CUSTOM WIDGETS (special UI components we built)
        workoutSummaryWidget = findViewById(R.id.workoutSummaryWidget) // Shows workout details
        healthMetricsWidget = findViewById(R.id.healthMetricsWidget)   // Shows BMI and health tips
        
        // ⭐ FIND OUR FAVORITES SECTION ELEMENTS
        cardQuickFavorites = findViewById(R.id.cardQuickFavorites)  // Container for favorites
        recyclerFavorites = findViewById(R.id.recyclerFavorites)    // Horizontal list of favorite foods
        tvNoFavorites = findViewById(R.id.tvNoFavorites)           // "No favorites yet" message

        // 🔥 STREAK
        tvStreakCount = findViewById(R.id.tvStreakCount)
        tvStreakSubtitle = findViewById(R.id.tvStreakSubtitle)
        
        // 🔗 CONNECT WIDGETS TO OUR ACTIVITY
        // Widgets need to know about our activity's lifecycle (when it starts/stops/pauses)
        // This is like giving them a phone number to call us when they need something
        workoutSummaryWidget.setLifecycleOwner(this)
        healthMetricsWidget.setLifecycleOwner(this)
        
        // 📱 SET DYNAMIC VERSION INFO
        // Update the version display from build.gradle instead of hardcoded XML
        val tvVersionInfo: TextView = findViewById(R.id.tvVersionInfo)
        tvVersionInfo.text = "CalorieTracker v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
        
        // 🎨 SET UP THE FAVORITES LIST
        // This creates the horizontal scrolling list of favorite foods
        setupFavoritesRecyclerView()
    }
    
    /**
     * 📜 SETTING UP THE FOOD DIARY: setupRecyclerView()
     * 
     * A RecyclerView is like a magical list that can show thousands of items without slowing down.
     * Think of it like a conveyor belt at a sushi restaurant - it only shows what you need to see,
     * and efficiently manages everything else behind the scenes.
     * 
     * We're setting up the list that shows today's food entries (breakfast, lunch, dinner, snacks).
     */
    private fun setupRecyclerView() {
        // 🎭 CREATE OUR LIST MANAGER (the "adapter")
        // An adapter is like a translator between our data and the visual list
        // It knows how to take food data and turn it into pretty list items
        calorieEntryAdapter = CalorieEntryAdapter(
            // 📝 WHAT HAPPENS WHEN USER CLICKS ON A FOOD ENTRY
            onEditClick = { entry ->
                // When someone taps a food entry, open the editing screen
                // This is like clicking "edit" on a document
                
                // 📦 PACK UP ALL THE FOOD INFO TO SEND TO THE EDITING SCREEN
                val intent = Intent(this, CalorieEntryActivity::class.java)
                intent.putExtra("EDIT_ENTRY_ID", entry.id)        // Unique ID number
                intent.putExtra("EDIT_FOOD_NAME", entry.foodName) // "Granny Smith Apple"
                intent.putExtra("EDIT_CALORIES", entry.calories)  // 95
                intent.putExtra("EDIT_PROTEIN", entry.protein)    // 0.5g
                intent.putExtra("EDIT_CARBS", entry.carbs)        // 25g
                intent.putExtra("EDIT_FAT", entry.fat)            // 0.3g
                intent.putExtra("EDIT_FIBER", entry.fiber)        // 4.4g
                intent.putExtra("EDIT_SUGAR", entry.sugar)        // 19g
                intent.putExtra("EDIT_SODIUM", entry.sodium)      // 2mg
                intent.putExtra("EDIT_DATE", entry.date)          // "2024-08-31"
                intent.putExtra("EDIT_BARCODE", entry.barcode)    // "1234567890" (if scanned)
                
                // 🚀 LAUNCH THE EDITING SCREEN
                startActivity(intent)
            },
            // 🗑️ WHAT HAPPENS WHEN USER LONG-PRESSES A FOOD ENTRY (WANTS TO DELETE)
            onDeleteClick = { entry ->
                // When someone holds down on a food entry, ask if they want to delete it
                // This prevents accidental deletions (like "Are you sure?" dialogs)
                showDeleteConfirmationDialog(entry)
            }
        )
        
        // 🔧 CONFIGURE OUR LIST
        recyclerViewTodayEntries.apply {
            adapter = calorieEntryAdapter                           // Tell the list who manages its content
            layoutManager = LinearLayoutManager(this@MainActivity)  // Make items stack vertically (like a column)
        }
    }
    
    /**
     * ⭐ SETTING UP THE FAVORITES BAR: setupFavoritesRecyclerView()
     * 
     * This creates a horizontal scrolling list of the user's favorite foods.
     * Think of it like having your most-used apps on your phone's home screen - 
     * it's for quick access to foods you eat often (like coffee, eggs, bananas).
     * 
     * The list scrolls left and right (horizontal) instead of up and down (vertical).
     */
    private fun setupFavoritesRecyclerView() {
        // 🎭 CREATE THE FAVORITES LIST MANAGER
        favoritesAdapter = FavoritesQuickAdapter(
            // 👆 WHAT HAPPENS WHEN USER TAPS A FAVORITE FOOD
            onFavoriteClick = { favorite ->
                // Add this favorite food to today's entries with one tap!
                // This is like having a "quick add" button for foods you eat regularly
                
                // Do this in the background so the app doesn't freeze
                lifecycleScope.launch {
                    try {
                        // Ask the repository to add this favorite to today's food diary
                        repository.quickAddFavorite(favorite)
                        // Show a success message at the bottom of the screen
                        Toast.makeText(this@MainActivity, "${favorite.foodName} added!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        // If something goes wrong, tell the user
                        Toast.makeText(this@MainActivity, "Failed to add ${favorite.foodName}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            // 👆📱 WHAT HAPPENS WHEN USER LONG-PRESSES A FAVORITE FOOD
            onFavoriteLongClick = { favorite ->
                // For now, just show a message. Later we could add options like "Remove from favorites"
                Toast.makeText(this@MainActivity, "Long pressed ${favorite.foodName}", Toast.LENGTH_SHORT).show()
            }
        )
        
        // 🔧 CONFIGURE THE FAVORITES LIST
        recyclerFavorites.apply {
            adapter = favoritesAdapter                                                              // Who manages the content
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false) // Scroll left/right
            // LinearLayoutManager.HORIZONTAL = items go left to right instead of top to bottom
            // false = don't reverse the order (start from left, not right)
        }
    }
    
    /**
     * Set up what happens when the user presses buttons
     * Each button opens a different screen or performs an action
     */
    private fun setupClickListeners() {
        // Manual Entry button - opens screen to manually type in food info
        findViewById<MaterialButton>(R.id.btnManualEntry).setOnClickListener {
            startActivity(Intent(this, CalorieEntryActivity::class.java))
        }
        
        // Scan Barcode button - opens camera to scan food barcodes
        findViewById<MaterialButton>(R.id.btnScanBarcode).setOnClickListener {
            startActivity(Intent(this, BarcodeScanActivity::class.java))
        }
        
        // View History button - shows past food entries
        findViewById<MaterialButton>(R.id.btnViewHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        
        // Nutrition Dashboard button - shows detailed nutrition breakdown
        findViewById<MaterialButton>(R.id.btnNutritionDashboard).setOnClickListener {
            startActivity(Intent(this, NutritionDashboardActivity::class.java))
        }
        
        // Analytics button - shows calorie trends, macro balance, and streak tracking
        findViewById<MaterialButton>(R.id.btnAnalytics).setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }
        
        // Settings button - opens app settings and configuration
        findViewById<MaterialButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // Water Tracking button - opens water intake tracking
        findViewById<MaterialButton>(R.id.btnWaterTracking).setOnClickListener {
            startActivity(Intent(this, WaterTrackingActivity::class.java))
        }
        
        // Weight Tracking button - opens Settings directly on the Weight tab
        findViewById<MaterialButton>(R.id.btnWeightTracking).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java).apply {
                putExtra(SettingsActivity.EXTRA_TAB, SettingsActivity.TAB_WEIGHT)
            })
        }
        
        // Create Recipe button - opens recipe creation screen
        findViewById<MaterialButton>(R.id.btnCreateRecipe).setOnClickListener {
            startActivity(Intent(this, RecipeCreateActivity::class.java))
        }
        
        // Recipe Library button - opens recipe library to view and manage recipes
        findViewById<MaterialButton>(R.id.btnRecipeLibrary).setOnClickListener {
            startActivity(Intent(this, RecipeLibraryActivity::class.java))
        }
        
        // Test Workout button - opens Health Connect Debug for testing workout functionality
        findViewById<MaterialButton>(R.id.btnTestWorkout).setOnClickListener {
            startActivity(Intent(this, HealthConnectDebugActivity::class.java))
        }
        
        // Test Open Food Facts button - opens test activity for database verification
        findViewById<MaterialButton>(R.id.btnTestOpenFoodFacts).setOnClickListener {
            startActivity(Intent(this, OpenFoodFactsTestActivity::class.java))
        }
        
        // Developer debug row — visible only in debug builds
        val debugRow = findViewById<LinearLayout>(R.id.layoutDebugRow)
        if (BuildConfig.DEBUG) {
            debugRow?.visibility = View.VISIBLE
            findViewById<MaterialButton>(R.id.btnTestWorkout).setOnClickListener {
                startActivity(Intent(this, HealthConnectDebugActivity::class.java))
            }
            findViewById<MaterialButton>(R.id.btnTestOpenFoodFacts).setOnClickListener {
                startActivity(Intent(this, OpenFoodFactsTestActivity::class.java))
            }
            findViewById<MaterialButton>(R.id.btnUIDebug).setOnClickListener {
                startActivity(Intent(this, UIDebugActivity::class.java))
            }
        }

        // Meal Planner
        findViewById<MaterialButton>(R.id.btnMealPlanner).setOnClickListener {
            startActivity(Intent(this, MealPlannerActivity::class.java))
        }

        // Shopping List
        findViewById<MaterialButton>(R.id.btnShoppingList).setOnClickListener {
            startActivity(Intent(this, ShoppingListActivity::class.java))
        }

        // Voice Input
        findViewById<MaterialButton>(R.id.btnVoiceInput).setOnClickListener {
            startActivity(Intent(this, VoiceInputActivity::class.java))
        }

        // Progress Photos
        findViewById<MaterialButton>(R.id.btnProgressPhotos).setOnClickListener {
            startActivity(Intent(this, ProgressPhotoActivity::class.java))
        }

        // See All Favorites
        findViewById<MaterialButton>(R.id.btnSeeAllFavorites)?.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
    }

    /**
     * Start watching for changes in nutrition data
     * When data changes (like when user adds food), the screen automatically updates
     */
    private fun observeData() {
        // Watch for changes to the user's daily calorie goal
        repository.getDailyGoal().observe(this) { goal ->
            dailyGoal = goal     // Save the goal for later use
            updateUI()           // Refresh the display with new goal
        }
        
        // Watch for changes to today's food entries
        repository.getTodaysEntries().observe(this) { entries ->
            calorieEntryAdapter.submitList(entries) // Update the list of food entries
            
            // Show appropriate message based on whether there are entries
            if (entries.isEmpty()) {
                tvNoEntries.visibility = View.VISIBLE               // Show "no entries" message
                recyclerViewTodayEntries.visibility = View.GONE     // Hide the empty list
            } else {
                tvNoEntries.visibility = View.GONE                  // Hide "no entries" message
                recyclerViewTodayEntries.visibility = View.VISIBLE  // Show the list with food
            }
            
            updateUI() // Refresh the calorie totals and progress bar
        }
        
        // Watch for changes to today's workout data from Health Connect
        repository.getTodaysWorkoutCaloriesLive().observe(this) { _ ->
            updateUI() // Refresh UI when workout data changes
        }
        
        // Refresh streak on load and whenever entries change
        lifecycleScope.launch { refreshStreak() }

        // Watch for changes to favorite meals
        lifecycleScope.launch {
            repository.getTopFavoritesLive(6).collect { favorites ->
                favoritesAdapter.submitList(favorites)
                
                // Show/hide favorites section based on whether user has favorites
                if (favorites.isEmpty()) {
                    cardQuickFavorites.visibility = View.GONE
                } else {
                    cardQuickFavorites.visibility = View.VISIBLE
                    tvNoFavorites.visibility = View.GONE
                    recyclerFavorites.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private suspend fun refreshStreak() {
        val streak = repository.calculateCurrentStreak()
        tvStreakCount.text = if (streak == 1) "1 day streak" else "$streak day streak"
        tvStreakSubtitle.text = when {
            streak == 0 -> "Log today to start your streak!"
            streak < 7  -> "Keep it up! $streak days and counting."
            streak < 30 -> "$streak days — you're on fire!"
            else        -> "$streak days — incredible consistency!"
        }
    }

    // Prevent concurrent UI updates
    private var isUpdatingUI = false

    /**
     * Schedule a UI refresh, coalescing rapid calls (from multiple LiveData emissions)
     * into a single update 150ms later.
     */
    private fun updateUI() {
        uiUpdateHandler.removeCallbacks(uiUpdateRunnable)
        uiUpdateHandler.postDelayed(uiUpdateRunnable, 150L)
    }

    private fun executeUIUpdate() {
        if (isUpdatingUI) return
        isUpdatingUI = true

        lifecycleScope.launch {
            try {
                
                // GET WORKOUT DATA: Try Health Connect first, then fall back to local test data
                var directWorkoutCalories = 0
                var workoutSource = "None"
                
                // First try Health Connect (real fitness data)
                try {
                    val healthConnect = healthConnectManager
                    val isAvailable = healthConnect.isHealthConnectAvailable()
                    val hasPermissions = healthConnect.hasRequiredPermissions()
                    
                    if (isAvailable && hasPermissions) {
                        val healthData = healthConnect.getTodaysHealthData()
                        
                        // Try total calories first, then active calories as fallback
                        directWorkoutCalories = if (healthData.totalCaloriesBurned > 0) {
                            healthData.totalCaloriesBurned
                        } else {
                            healthData.activeCaloriesBurned
                        }
                        
                        workoutSource = "Health Connect"
                    } else {
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Unexpected error", e)
                }
                
                // If no Health Connect data, check local database for test workout data
                if (directWorkoutCalories == 0) {
                    try {
                        val localWorkoutData = repository.getTodaysWorkoutCalories()
                        if (localWorkoutData != null && localWorkoutData.activeCaloriesBurned > 0) {
                            directWorkoutCalories = localWorkoutData.activeCaloriesBurned
                            workoutSource = "Test Data (${localWorkoutData.source})"
                        } else {
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Unexpected error", e)
                    }
                }
                
                // Get comprehensive daily summary including workout data
                val summary = repository.getTodaysSummary()
                
                // MANUAL CALCULATION using direct Health Connect data
                val baseGoal = summary.baseCalorieGoal
                val consumedCalories = summary.consumedCalories
                val bonusCalories = (directWorkoutCalories * 0.7).toInt()
                val adjustedGoal = baseGoal + bonusCalories
                val remainingCalories = maxOf(0, adjustedGoal - consumedCalories)
                
                // DEBUG: Compare repository vs direct data
                
                // Force UI updates on main thread using DIRECT Health Connect data
                runOnUiThread {
                    // Update the main "Today" section with consumed/goal format
                    tvCaloriesConsumed.text = consumedCalories.toString()
                    
                    // Show daily goal with workout bonus calculated from direct Health Connect data
                    tvDailyGoal.text = adjustedGoal.toString()
                    
                    
                    // Show workout bonus breakdown using direct data
                    val layoutWorkoutBonus = findViewById<LinearLayout>(R.id.layoutWorkoutBonus)
                    if (directWorkoutCalories > 0) {
                        tvWorkoutBonus.text = "🏃 Base: $baseGoal + $bonusCalories workout bonus (from $workoutSource)"
                        layoutWorkoutBonus.visibility = View.VISIBLE
                    } else {
                        layoutWorkoutBonus.visibility = View.GONE
                    }
                    
                    // Calculate and show calories remaining using direct Health Connect data
                    if (directWorkoutCalories > 0) {
                        // Show format: "500 out of 2776 calories consumed +997 bonus calories for the workout"
                        // Use bonusCalories (70%) to match the workout bonus shown above
                        val remainingText = if (remainingCalories > 0) {
                            "$consumedCalories out of $adjustedGoal calories consumed +$bonusCalories bonus calories for the workout"
                        } else {
                            val overAmount = consumedCalories - adjustedGoal
                            "$consumedCalories out of $adjustedGoal calories consumed (+$bonusCalories workout bonus) - $overAmount over goal"
                        }
                        tvCaloriesRemaining.text = remainingText
                        tvCaloriesRemaining.setTextColor(if (remainingCalories > 0) getColor(R.color.dark_gray) else getColor(R.color.red))
                    } else {
                        // No workout, use simple format
                        if (remainingCalories > 0) {
                            tvCaloriesRemaining.text = "$remainingCalories calories remaining"
                            tvCaloriesRemaining.setTextColor(getColor(R.color.dark_gray))
                        } else {
                            val overAmount = consumedCalories - adjustedGoal
                            tvCaloriesRemaining.text = "$overAmount calories over goal"
                            tvCaloriesRemaining.setTextColor(getColor(R.color.red))
                        }
                    }
                    
                    // Update the progress bar using direct Health Connect data
                    val progress = if (adjustedGoal > 0) {
                        ((consumedCalories.toFloat() / adjustedGoal.toFloat()) * 100).toInt()
                    } else {
                        0
                    }
                    progressBarCalories.progress = minOf(progress, 100) // Cap at 100% for visual purposes
                }
                
            } catch (e: Exception) {
                Log.e("MainActivity", "Unexpected error", e)
                // Fallback to basic UI update if workout integration fails
                updateBasicUI()
            } finally {
                isUpdatingUI = false
            }
        }
    }
    
    /**
     * Fallback UI update method without workout integration
     */
    private fun updateBasicUI() {
        lifecycleScope.launch {
            // Get today's total calories consumed
            val totalCalories = repository.getTodaysTotalCalories()
            
            // Get the daily goal (use 2000 as default if not set)
            val goal = dailyGoal?.calorieGoal ?: 2000
            
            // Update the displayed numbers
            tvCaloriesConsumed.text = totalCalories.toString()
            tvDailyGoal.text = goal.toString()
            tvWorkoutBonus.visibility = View.GONE
            
            // Calculate and show calories remaining
            val remaining = goal - totalCalories
            if (remaining > 0) {
                tvCaloriesRemaining.text = "$remaining calories remaining"
                tvCaloriesRemaining.setTextColor(getColor(R.color.dark_gray))
            } else {
                tvCaloriesRemaining.text = "${Math.abs(remaining)} calories over goal"
                tvCaloriesRemaining.setTextColor(getColor(R.color.red))
            }
            
            // Update the progress bar
            val progress = ((totalCalories.toFloat() / goal.toFloat()) * 100).toInt()
            progressBarCalories.progress = minOf(progress, 100)
        }
    }
    
    /**
     * This method runs every time the user returns to this screen
     * It refreshes the data to make sure everything is up to date
     */
    /**
     * Initialize Health Connect to register the app for discovery
     */
    private fun initializeHealthConnect() {
        lifecycleScope.launch {
            try {
                // healthConnectManager field is already initialized in onCreate()
                healthConnectManager.testHealthConnectIntegration()
            } catch (e: Exception) {
                // Health Connect initialization failed, continue without it
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Force UI refresh with multiple attempts to ensure data loads
        lifecycleScope.launch {
            repository.syncTodaysWorkoutData() // Refresh workout data
            updateUI() // Refresh the display
            
            // Additional refresh after short delay to catch async updates
            kotlinx.coroutines.delay(1000)
            updateUI()
        }
    }
    
    /**
     * Show explanation dialog for workout calorie calculations
     */
    private fun showWorkoutExplanationDialog() {
        val message = Html.fromHtml(getString(R.string.workout_explanation_message), Html.FROM_HTML_MODE_LEGACY)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.workout_explanation_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.got_it)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    /**
     * Show confirmation dialog before deleting a food entry
     */
    private fun showDeleteConfirmationDialog(entry: CalorieEntry) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Food Entry")
            .setMessage("Are you sure you want to delete \"${entry.foodName}\"?")
            .setPositiveButton("Delete") { _, _ ->
                deleteEntry(entry)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Delete a food entry from the database
     */
    private fun deleteEntry(entry: CalorieEntry) {
        lifecycleScope.launch {
            try {
                repository.deleteCalorieEntry(entry)
                Toast.makeText(this@MainActivity, "\"${entry.foodName}\" deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to delete entry", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 🔐 MIGRATE LEGACY API KEYS TO SECURE STORAGE
     * 
     * One-time migration that moves plain text API keys from SharedPreferences
     * to encrypted Android Keystore storage for enhanced security.
     */
    private fun migrateLegacyApiKeys() {
        lifecycleScope.launch {
            try {
                val migration = com.calorietracker.security.ApiKeyMigration
                
                // Check if migration is needed
                if (migration.isMigrationNeeded(this@MainActivity)) {
                    
                    val result = migration.migrateApiKeys(this@MainActivity)
                    
                    if (result.success) {
                        
                        // Show user notification if keys were migrated
                        if (result.keysMigrated > 0) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "🔐 Enhanced security: API keys now encrypted",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                    }
                } else {
                }
                
            } catch (e: Exception) {
            }
        }
    }
}