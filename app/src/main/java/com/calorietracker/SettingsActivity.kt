package com.calorietracker

// 🎨 ANDROID FRAMEWORK IMPORTS - Core Android functionality
import android.content.Intent                           // 🚀 For navigating between different screens and apps
import android.os.Bundle                               // 💾 Saves app state during screen rotations
import android.util.Log                                // 📝 Debug logging system (like console.log)
import android.view.View                               // 📺 Basic building block for everything visible on screen
import android.widget.*                               // 📱 Basic UI components (TextView, Button, etc.)

// 📱 ANDROID SUPPORT LIBRARIES - Modern Android features
import androidx.activity.result.contract.ActivityResultContracts // 📝 Modern way to handle activity results
import androidx.appcompat.app.AlertDialog              // 🗨️ Pretty popup dialog boxes
import androidx.appcompat.app.AppCompatActivity        // 📱 Modern activity base class with backwards compatibility
import androidx.fragment.app.Fragment                  // 🧩 Reusable UI components (like LEGO blocks for screens)
import androidx.fragment.app.FragmentActivity          // 📱 Activity that can host fragments
import androidx.viewpager2.adapter.FragmentStateAdapter // 📋 Manages multiple fragments in tabs
import androidx.viewpager2.widget.ViewPager2           // 📋 Swipeable tabs container
import androidx.lifecycle.lifecycleScope               // ⚡ Manages background tasks safely

// 💪 HEALTH CONNECT IMPORTS - Fitness tracker integration
import androidx.health.connect.client.PermissionController         // 🔒 Manages Health Connect permissions
import androidx.health.connect.client.permission.HealthPermission  // 🔒 Health data permission definitions
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord // 🔥 Active calories from workouts
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord  // 🔥 Total daily calories burned
import androidx.health.connect.client.records.ExerciseSessionRecord      // 🏋️ Workout session data
import androidx.health.connect.client.records.StepsRecord               // 🚶 Step count data
import androidx.health.connect.client.records.HydrationRecord           // 💧 Water intake data

// 🗃️ OUR APP'S DATABASE COMPONENTS
import com.calorietracker.database.CalorieDatabase     // 🗃️ Main nutrition database
import com.calorietracker.database.NutritionGoals      // 🎯 User's nutrition targets (protein, carbs, etc.)
import com.calorietracker.database.WeightGoal          // ⚖️ User's weight loss/gain goals

// 📋 OUR APP'S BUSINESS LOGIC COMPONENTS
import com.calorietracker.fitness.HealthConnectManager // 💪 Connects to fitness trackers
import com.calorietracker.utils.CalorieCalculator      // 🧮 Calculates calorie recommendations
import com.calorietracker.utils.ThemeManager           // 🌙 Dark/light theme management
import com.calorietracker.nutrition.NutritionRecommendations // 🌍 Regional nutrition guidelines
import com.calorietracker.repository.CalorieRepository // 🏛️ Data access layer
import com.calorietracker.SettingsScaleFragment        // ⚖️ Smart scale integration fragment
import com.calorietracker.sync.DataSyncService         // 🔄 Background data synchronization

// 🎨 MATERIAL DESIGN COMPONENTS - Google's pretty UI library
import com.google.android.material.button.MaterialButton        // 🔘 Modern, pretty buttons
import com.google.android.material.tabs.TabLayout               // 📋 Tab navigation header
import com.google.android.material.tabs.TabLayoutMediator       // 🔗 Connects tabs to ViewPager
import com.google.android.material.textfield.TextInputEditText  // ✏️ Pretty text input fields with floating labels

// 💫 KOTLIN COROUTINES - Background processing without UI freezing
import kotlinx.coroutines.launch                       // 🚀 Start background tasks

/**
 * ⚙️ SETTINGS SCREEN - THE CONTROL CENTER OF OUR APP
 * 
 * Welcome to mission control! This is where users customize everything about their
 * calorie tracking experience. Think of it like the settings menu in your phone,
 * but specifically for nutrition and health tracking.
 * 
 * 🎯 What can users customize here?
 * 1. 🏆 NUTRITION GOALS: Daily targets for calories, protein, carbs, fat, fiber
 * 2. ⚖️ WEIGHT GOALS: Current weight, target weight, timeline, and calorie calculations
 * 3. 🌍 REGIONAL SETTINGS: Nutrition guidelines for different countries (US, UK, Canada, etc.)
 * 4. 🎨 APP PREFERENCES: Dark mode, metric units, notification settings
 * 5. 💪 HEALTH INTEGRATION: Connect fitness trackers (OnePlus Watch, etc.)
 * 6. ⚖️ SMART SCALE: Connect Bluetooth scales for automatic weight tracking
 * 
 * 📋 How is this screen organized?
 * We use a TABBED INTERFACE with 6 different sections:
 * - Goals Tab: Set daily nutrition targets
 * - Weight Tab: Weight management and calorie calculation
 * - Regional Tab: Choose your country's nutrition guidelines
 * - Preferences Tab: App appearance and behavior
 * - Health Tab: Fitness tracker integration
 * - Scale Tab: Smart scale connection and management
 * 
 * 🧮 Technical architecture:
 * - Uses ViewPager2 with Fragment tabs (modern Android design pattern)
 * - Each tab is a separate Fragment for better performance
 * - Real-time calorie calculations based on user's weight goals
 * - Integration with Health Connect for fitness data
 * - Bluetooth LE support for smart scale integration
 * 
 * Think of this as the "command center" where users set up their entire nutrition journey!
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TAB = "extra_tab"
        const val TAB_GOALS = 0
        const val TAB_WEIGHT = 1
        const val TAB_REGIONAL = 2
        const val TAB_PREFERENCES = 3
        const val TAB_HEALTH = 4
        const val TAB_SCALE = 5
    }

    // 🏛️ DATA REPOSITORY - Our connection to the nutrition database
    // This is like having a personal assistant who knows where all your data is stored
    // and can save/load your preferences, goals, and settings
    private lateinit var repository: CalorieRepository
    
    // 🍎 LEGACY UI ELEMENTS (now moved to individual fragments)
    // These variables used to hold references to UI elements when this was a single screen.
    // Now each tab has its own Fragment that manages its own UI elements.
    // We keep these here for backwards compatibility, but they're not actively used.
    
    // 🔥 CALORIE GOAL INPUT - Daily calorie target
    private lateinit var etCalorieGoal: TextInputEditText    // "2000" calories per day
    
    // 💪 MACRONUTRIENT GOAL INPUTS - The "big three" nutrients  
    private lateinit var etProteinGoal: TextInputEditText    // "150g" protein per day (muscle building)
    private lateinit var etCarbsGoal: TextInputEditText      // "250g" carbs per day (energy)
    private lateinit var etFatGoal: TextInputEditText        // "70g" fat per day (essential nutrients)
    private lateinit var etFiberGoal: TextInputEditText      // "25g" fiber per day (digestive health)
    
    // 🚫 NUTRIENT LIMIT INPUTS - Things to limit for health
    private lateinit var etSugarLimit: TextInputEditText     // "50g" max sugar per day
    private lateinit var etSodiumLimit: TextInputEditText    // "2300mg" max sodium per day (blood pressure)
    
    // 🌍 REGION SELECTION - Choose your country's guidelines
    private lateinit var spinnerRegion: Spinner             // Dropdown: "United States", "United Kingdom", etc.
    
    // ⚙️ APP PREFERENCE SWITCHES - Customize app behavior
    private lateinit var switchDarkMode: Switch             // Toggle dark/light theme
    private lateinit var switchMetricUnits: Switch          // Toggle kg/lbs, cm/inches
    private lateinit var switchNutritionTips: Switch        // Toggle helpful nutrition tips
    
    // 💪 HEALTH CONNECT UI ELEMENTS - Fitness tracker integration
    // These show the status of fitness tracker connections and allow manual sync
    private lateinit var tvHealthConnectStatus: TextView        // 🏆 "Connected - OnePlus Watch 3 data syncing"
    private lateinit var tvHealthConnectIndicator: TextView     // ✅ Status emoji (✅ connected, ❌ error, ⚠️ setup needed)
    private lateinit var btnSetupHealthConnect: MaterialButton  // 🔗 "Setup OnePlus Watch Integration" button
    private lateinit var btnSyncWorkoutData: MaterialButton     // 🔄 "Sync Workout Data Now" manual sync button
    
    // Weight Goal UI elements
    private lateinit var etCurrentWeight: TextInputEditText
    private lateinit var etTargetWeight: TextInputEditText
    private lateinit var etTimelineDays: TextInputEditText
    private lateinit var etAge: TextInputEditText
    private lateinit var etHeight: TextInputEditText
    private lateinit var spinnerActivityLevel: Spinner
    private lateinit var spinnerGender: Spinner
    private lateinit var tvCalorieRecommendation: TextView
    private lateinit var btnCalculateCalories: MaterialButton
    private lateinit var btnSaveWeightGoal: MaterialButton
    
    // Interactive metrics cards
    private lateinit var layoutMetricsCards: LinearLayout
    private lateinit var tvRecommendedCaloriesValue: TextView
    private lateinit var tvBMRValue: TextView
    private lateinit var tvTDEEValue: TextView
    private lateinit var tvWeightChangeRateValue: TextView
    private lateinit var tvHealthStatus: TextView
    
    // Store current nutrition goals
    private var currentGoals: NutritionGoals? = null
    
    // Health Connect manager
    private lateinit var healthConnectManager: HealthConnectManager
    
    // Health Connect permission launcher
    private val healthConnectPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        // Check permissions after user grants/denies them
        if (granted.containsAll(getRequiredPermissions())) {
            Toast.makeText(this, "Health Connect permissions granted!", Toast.LENGTH_SHORT).show()
            checkHealthConnectStatus()
        } else {
            Toast.makeText(this, "Health Connect permissions denied", Toast.LENGTH_SHORT).show()
            // Show manual setup if automatic permission request was denied
            showManualSetupInstructions()
        }
    }
    
    private fun getRequiredPermissions() = setOf(
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class)
    )
    
    /**
     * 🏗️ THE CONSTRUCTION PHASE: onCreate()
     * 
     * This method runs when the user opens the Settings screen. Think of it like
     * setting up a control room with multiple monitoring stations (tabs).
     * 
     * Setup process:
     * 1. 🎨 Apply the user's theme preference (dark/light mode)
     * 2. 📺 Load the tabbed layout from XML
     * 3. 🗃️ Create connections to our data repository
     * 4. 💪 Set up Health Connect integration for fitness tracking
     * 5. 📋 Create the 6 tab system (Goals, Weight, Regional, Preferences, Health, Scale)
     * 6. 🔘 Set up the Save/Cancel buttons
     * 7. 🔄 Check if fitness trackers are connected
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // 🎨 STEP 1: Apply theme before any visual elements are created
        // This ensures the entire screen uses the user's preferred dark/light mode
        ThemeManager.applyTheme(this)
        
        // 🏗️ STEP 2: Standard Android activity setup
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_tabbed)  // Load the tabbed layout XML
        
        // Note: We don't show the action bar for a cleaner, more modern look
        
        // 🗃️ STEP 3: Set up our data access layer
        // Create repository connection to save/load user preferences and goals
        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)
        
        // 💪 STEP 4: Set up Health Connect manager for fitness integration
        // This connects us to OnePlus Watch, Samsung Health, etc.
        healthConnectManager = HealthConnectManager(this)
        
        // 📋 STEP 5: Create the tabbed interface with 6 different sections
        setupTabs()          // Create Goals, Weight, Regional, Preferences, Health, Scale tabs
        setupActionButtons() // Set up the Save Settings and Cancel buttons

        // Navigate to a specific tab if requested via intent extra
        val tabIndex = intent.getIntExtra(EXTRA_TAB, -1)
        if (tabIndex >= 0) {
            findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)?.currentItem = tabIndex
        }
        
        // 🔄 STEP 6: Check if fitness trackers are properly connected
        checkHealthConnectStatus() // Test Health Connect integration and update UI
    }
    
    /**
     * Connect to all the UI elements in the layout
     */
    private fun initViews() {
        // Legacy UI elements moved to individual fragments - no longer accessible from main activity
        // All UI setup now handled by fragments:
        // - Goals: SettingsGoalsFragment
        // - Weight: SettingsWeightFragment  
        // - Regional: SettingsRegionalFragment
        // - Preferences: SettingsPreferencesFragment
        // - Health: SettingsHealthFragment
        // - Scale: SettingsScaleFragment
    }
    
    /**
     * Set up click listeners for interactive metrics cards with detailed explanations
     */
    private fun setupMetricsClickListeners() {
        // Recommended Calories Card
        findViewById<View>(R.id.cardRecommendedCalories)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("🎯 Daily Calorie Goal")
                .setMessage("This is your personalized daily calorie target to reach your weight goal.\n\n" +
                    "How it's calculated:\n" +
                    "• Start with your TDEE (maintenance calories)\n" +
                    "• Adjust based on your weight goal timeline\n" +
                    "• Apply safety limits for healthy weight change\n\n" +
                    "Example: If your TDEE is 2000 calories and you want to lose 1 lb/week, " +
                    "you need a 500 calorie deficit daily (3500 calories = 1 lb), " +
                    "so your goal would be 1500 calories.")
                .setPositiveButton("Got it", null)
                .show()
        }
        
        // BMR Card
        findViewById<View>(R.id.cardBMR)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("📊 BMR (Basal Metabolic Rate)")
                .setMessage("BMR is the minimum calories your body needs to function at rest.\n\n" +
                    "What it covers:\n" +
                    "• Breathing and circulation\n" +
                    "• Cell production and repair\n" +
                    "• Nutrient processing\n" +
                    "• Protein synthesis\n\n" +
                    "Calculation (Mifflin-St Jeor equation):\n" +
                    "Men: (10 × weight kg) + (6.25 × height cm) - (5 × age) + 5\n" +
                    "Women: (10 × weight kg) + (6.25 × height cm) - (5 × age) - 161\n\n" +
                    "Think of BMR as your body's 'idle speed' - the energy needed just to stay alive.")
                .setPositiveButton("Got it", null)
                .show()
        }
        
        // TDEE Card
        findViewById<View>(R.id.cardTDEE)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("🔥 TDEE (Total Daily Energy Expenditure)")
                .setMessage("TDEE is your total daily calorie burn including all activities.\n\n" +
                    "Components:\n" +
                    "• BMR (60-75%): Basic body functions\n" +
                    "• TEF (8-15%): Digesting food\n" +
                    "• NEAT (15-30%): Non-exercise activities\n" +
                    "• Exercise (0-30%): Planned workouts\n\n" +
                    "Calculation:\n" +
                    "TDEE = BMR × Activity Multiplier\n" +
                    "• Sedentary: BMR × 1.2\n" +
                    "• Light activity: BMR × 1.375\n" +
                    "• Moderate activity: BMR × 1.55\n" +
                    "• Very active: BMR × 1.725\n" +
                    "• Extremely active: BMR × 1.9\n\n" +
                    "TDEE represents your maintenance calories - eat this amount to maintain weight.")
                .setPositiveButton("Got it", null)
                .show()
        }
        
        // Weight Change Rate Card
        findViewById<View>(R.id.cardWeightChangeRate)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("📈 Weekly Weight Change Rate")
                .setMessage("This shows how fast you'll reach your weight goal.\n\n" +
                    "Safe weight change rates:\n" +
                    "• Weight loss: 0.5-2 lbs per week\n" +
                    "• Weight gain: 0.5-1 lb per week\n\n" +
                    "The math:\n" +
                    "• 1 pound ≈ 3500 calories\n" +
                    "• Daily deficit/surplus ÷ 500 = lbs per week\n" +
                    "• Example: 750 calorie deficit = 1.5 lbs lost per week\n\n" +
                    "Faster isn't always better!\n" +
                    "• Too fast weight loss: muscle loss, nutrient deficiencies\n" +
                    "• Too fast weight gain: excess fat storage\n\n" +
                    "Sustainable changes lead to lasting results.")
                .setPositiveButton("Got it", null)
                .show()
        }
    }
    
    
    
    /**
     * 📋 SET UP THE TABBED INTERFACE: setupTabs()
     * 
     * This creates a modern tabbed interface where users can swipe between different
     * settings categories. Think of it like having 6 different control panels that
     * you can switch between by tapping tabs or swiping left/right.
     * 
     * How ViewPager2 + TabLayout works:
     * 1. ViewPager2 is like a horizontal scrolling container that holds 6 different screens
     * 2. TabLayout provides the tab headers ("Goals", "Weight", "Regional", etc.)
     * 3. TabLayoutMediator connects them so tapping a tab switches the screen
     * 4. Each tab contains a separate Fragment with its own UI and logic
     * 
     * Modern Android design pattern:
     * - Better performance (only loads visible tab + adjacent tabs)
     * - Smooth swipe animations between tabs
     * - Each Fragment manages its own lifecycle
     * - Easier to maintain (each tab is independent)
     */
    private fun setupTabs() {
        // 📋 FIND THE TAB COMPONENTS in our layout
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)   // 📋 Tab headers ("Goals", "Weight", etc.)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)  // 📱 Swipeable content container
        
        // 🎯 CREATE THE ADAPTER that manages our 6 different tab fragments
        val adapter = SettingsPagerAdapter(this)  // This knows how to create each Fragment
        viewPager.adapter = adapter               // Tell ViewPager to use our adapter
        
        // 🔗 CONNECT TABS TO CONTENT using TabLayoutMediator
        // This is like assigning a label to each tab and telling it which screen to show
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // Set the text label for each tab based on its position
            tab.text = when (position) {
                0 -> "Goals"        // 🎯 Nutrition targets (calories, protein, etc.)
                1 -> "Weight"       // ⚖️ Weight management and calorie calculation
                2 -> "Regional"     // 🌍 Choose country-specific nutrition guidelines
                3 -> "Preferences"  // ⚙️ App settings (dark mode, units, etc.)
                4 -> "Health"       // 💪 Fitness tracker integration
                5 -> "Scale"        // ⚖️ Smart scale Bluetooth connection
                else -> "Tab $position" // Fallback (should never happen)
            }
        }.attach() // 🔗 Actually connect the tabs to the ViewPager
    }
    
    /**
     * Set up the save/cancel action buttons
     */
    private fun setupActionButtons() {
        findViewById<MaterialButton>(R.id.btnSaveSettings).setOnClickListener {
            saveAllSettings()
        }
        
        findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            finish()
        }
    }
    
    /**
     * Save settings from all tabs
     */
    private fun saveAllSettings() {
        try {
            // Get the ViewPager2 and adapter
            val viewPager = findViewById<ViewPager2>(R.id.viewPager)
            val adapter = viewPager.adapter as? SettingsPagerAdapter
            
            if (adapter != null) {
                // Save settings from all fragments that have a saveSettings method
                val fragmentManager = supportFragmentManager
                
                // Save from each fragment
                for (i in 0 until adapter.itemCount) {
                    val fragmentTag = "f$i" // ViewPager2 uses this tag format
                    val fragment = fragmentManager.findFragmentByTag(fragmentTag)
                    
                    when (fragment) {
                        is SettingsScaleFragment -> fragment.saveSettings()
                        // Other fragments can implement saveSettings() method if needed
                        // is SettingsGoalsFragment -> fragment.saveSettings()
                        // is SettingsWeightFragment -> fragment.saveSettings()
                        // etc.
                    }
                }
                
                Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error accessing settings fragments", Toast.LENGTH_SHORT).show()
            }
            
            finish()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving settings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Set up theme preference and dark mode toggle
     */
    private fun setupThemePreference() {
        // Set current state based on actual theme
        switchDarkMode.isChecked = ThemeManager.isDarkModeEnabled(this)
        
        // Handle toggle changes
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (ThemeManager.isFollowingSystem(this)) {
                // If currently following system, inform user we're switching to manual
                Toast.makeText(this, 
                    "Switching to manual theme control", 
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            ThemeManager.setDarkMode(this, isChecked)
            
            // Show theme change message
            Toast.makeText(this, 
                if (isChecked) "Dark mode enabled" else "Light mode enabled", 
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Set up click listeners for buttons and other interactive elements
     */
    private fun setupClickListeners() {
        // Save settings button
        findViewById<MaterialButton>(R.id.btnSaveSettings)?.setOnClickListener {
            saveSettings()
        }
        
        // Cancel button
        findViewById<MaterialButton>(R.id.btnCancel)?.setOnClickListener {
            finish()
        }
        
        // Calculate calories button
        findViewById<MaterialButton>(R.id.btnCalculateCalories)?.setOnClickListener {
            calculateCalorieRecommendation()
        }
        
        // Save weight goal button
        findViewById<MaterialButton>(R.id.btnSaveWeightGoal)?.setOnClickListener {
            saveWeightGoal()
        }
        
        // Reset to defaults button
        findViewById<MaterialButton>(R.id.btnResetDefaults)?.setOnClickListener {
            resetToDefaults()
        }
        
        // Database buttons are now handled by SettingsRegionalFragment
        
        // Health Connect functionality moved to SettingsHealthFragment
    }
    
    /**
     * Set up the region selection dropdown with available countries
     */
    private fun setupRegionSpinner() {
        val regions = NutritionRecommendations.getAvailableRegions()
        val regionNames = regions.map { it.second } // Get the full names like "United States"
        
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, regionNames)
        adapter.setDropDownViewResource(R.layout.dropdown_item)
        spinnerRegion.adapter = adapter
    }
    
    /**
     * Set up the weight goal spinners with available options
     */
    private fun setupWeightGoalSpinners() {
        // Activity level spinner
        val activityLevels = CalorieCalculator.getActivityLevels()
        val activityNames = activityLevels.map { it.second }
        val activityAdapter = ArrayAdapter(this, R.layout.dropdown_item, activityNames)
        activityAdapter.setDropDownViewResource(R.layout.dropdown_item)
        spinnerActivityLevel.adapter = activityAdapter
        
        // Gender spinner
        val genders = listOf("Not specified", "Male", "Female")
        val genderAdapter = ArrayAdapter(this, R.layout.dropdown_item, genders)
        genderAdapter.setDropDownViewResource(R.layout.dropdown_item)
        spinnerGender.adapter = genderAdapter
    }
    
    
    /**
     * Load the user's current settings and fill in the form
     */
    private fun loadCurrentSettings() {
        lifecycleScope.launch {
            try {
                // Get user's current goals from database
                currentGoals = repository.getNutritionGoalsSync()
                
                val goals = currentGoals
                if (goals != null) {
                    // Fill in the form with current values
                    populateFormWithCurrentGoals(goals)
                } else {
                    // User doesn't have goals yet, use defaults
                    setDefaultValues()
                }
                
                // Load weight goal if available
                loadWeightGoal()
            } catch (e: Exception) {
                // If something goes wrong, use default values
                setDefaultValues()
                Toast.makeText(this@SettingsActivity, "Error loading settings", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Fill in the form with the user's current nutrition goals
     */
    private fun populateFormWithCurrentGoals(goals: NutritionGoals) {
        // Fill in calorie goal
        etCalorieGoal.setText(goals.calorieGoal.toString())
        
        // Fill in macronutrient goals
        etProteinGoal.setText(goals.proteinGoal.toString())
        etCarbsGoal.setText(goals.carbsGoal.toString())
        etFatGoal.setText(goals.fatGoal.toString())
        etFiberGoal.setText(goals.fiberGoal.toString())
        
        // Fill in limits
        etSugarLimit.setText(goals.sugarGoal.toString())
        etSodiumLimit.setText(goals.sodiumGoal.toString())
        
        // Set region selection
        val regions = NutritionRecommendations.getAvailableRegions()
        val regionIndex = regions.indexOfFirst { it.first == goals.selectedRegion }
        if (regionIndex >= 0) {
            spinnerRegion.setSelection(regionIndex)
        }
        
        // Set preference switches
        switchMetricUnits.isChecked = goals.useMetricUnits
        switchNutritionTips.isChecked = goals.showNutritionTips
    }
    
    /**
     * Set default values based on US recommendations
     */
    private fun setDefaultValues() {
        val usRecommendations = NutritionRecommendations.US_RECOMMENDATIONS
        
        etCalorieGoal.setText(usRecommendations["calories"]?.toInt().toString())
        etProteinGoal.setText(usRecommendations["protein"].toString())
        etCarbsGoal.setText(usRecommendations["carbs"].toString())
        etFatGoal.setText(usRecommendations["fat"].toString())
        etFiberGoal.setText(usRecommendations["fiber"].toString())
        etSugarLimit.setText(usRecommendations["sugar"].toString())
        etSodiumLimit.setText(usRecommendations["sodium"].toString())
        
        // Default to US region
        spinnerRegion.setSelection(0)
        
        // Default preferences
        switchMetricUnits.isChecked = false
        switchNutritionTips.isChecked = true
    }
    
    /**
     * Save all the user's settings to the database
     */
    private fun saveSettings() {
        lifecycleScope.launch {
            try {
                // Get values from form inputs
                val calorieGoal = etCalorieGoal.text.toString().toIntOrNull() ?: 2000
                val proteinGoal = etProteinGoal.text.toString().toDoubleOrNull() ?: 50.0
                val carbsGoal = etCarbsGoal.text.toString().toDoubleOrNull() ?: 300.0
                val fatGoal = etFatGoal.text.toString().toDoubleOrNull() ?: 65.0
                val fiberGoal = etFiberGoal.text.toString().toDoubleOrNull() ?: 25.0
                val sugarLimit = etSugarLimit.text.toString().toDoubleOrNull() ?: 50.0
                val sodiumLimit = etSodiumLimit.text.toString().toDoubleOrNull() ?: 2300.0
                
                // Get selected region
                val regions = NutritionRecommendations.getAvailableRegions()
                val selectedRegionIndex = spinnerRegion.selectedItemPosition
                val selectedRegion = if (selectedRegionIndex >= 0 && selectedRegionIndex < regions.size) {
                    regions[selectedRegionIndex].first
                } else {
                    "US"
                }
                
                // Get preference switches
                val useMetricUnits = switchMetricUnits.isChecked
                val showNutritionTips = switchNutritionTips.isChecked
                
                // Create new nutrition goals object
                val newGoals = NutritionGoals(
                    calorieGoal = calorieGoal,
                    proteinGoal = proteinGoal,
                    carbsGoal = carbsGoal,
                    fatGoal = fatGoal,
                    fiberGoal = fiberGoal,
                    sugarGoal = sugarLimit,
                    sodiumGoal = sodiumLimit,
                    selectedRegion = selectedRegion,
                    useMetricUnits = useMetricUnits,
                    showNutritionTips = showNutritionTips
                )
                
                // Save to database
                repository.updateNutritionGoals(newGoals)
                
                // Show success message
                Toast.makeText(this@SettingsActivity, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
                
                // Go back to previous screen
                finish()
                
            } catch (e: Exception) {
                // Show error message if something goes wrong
                Toast.makeText(this@SettingsActivity, "Error saving settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Show dialog asking if user wants to reset to default values
     */
    private fun showResetDefaultsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset to Defaults")
            .setMessage("This will reset all your nutrition goals to the recommended values for your selected region. Are you sure?")
            .setPositiveButton("Reset") { _, _ ->
                resetToDefaults()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Reset all values to the defaults for the selected region
     */
    private fun resetToDefaults() {
        // Get selected region
        val regions = NutritionRecommendations.getAvailableRegions()
        val selectedRegionIndex = spinnerRegion.selectedItemPosition
        val selectedRegion = if (selectedRegionIndex >= 0 && selectedRegionIndex < regions.size) {
            regions[selectedRegionIndex].first
        } else {
            "US"
        }
        
        // Get recommendations for that region
        val recommendations = NutritionRecommendations.getRecommendationsForRegion(selectedRegion)
        
        // Fill in the form with recommended values
        etCalorieGoal.setText(recommendations["calories"]?.toInt().toString())
        etProteinGoal.setText(recommendations["protein"].toString())
        etCarbsGoal.setText(recommendations["carbs"].toString())
        etFatGoal.setText(recommendations["fat"].toString())
        etFiberGoal.setText(recommendations["fiber"].toString())
        etSugarLimit.setText(recommendations["sugar"].toString())
        etSodiumLimit.setText(recommendations["sodium"].toString())
        
        Toast.makeText(this, "Reset to recommended values for ${regions[selectedRegionIndex].second}", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Update the food database with foods from the selected region
     */
    private fun updateFoodDatabase() {
        // Get selected region
        val regions = NutritionRecommendations.getAvailableRegions()
        val selectedRegionIndex = spinnerRegion.selectedItemPosition
        val selectedRegion = if (selectedRegionIndex >= 0 && selectedRegionIndex < regions.size) {
            regions[selectedRegionIndex].first
        } else {
            "US"
        }
        
        // Show confirmation dialog
        AlertDialog.Builder(this)
            .setTitle("Update Food Database")
            .setMessage("This will download common foods from ${regions[selectedRegionIndex].second} to your device for offline use. This may take a few moments.")
            .setPositiveButton("Update") { _, _ ->
                // Save the region preference first
                lifecycleScope.launch {
                    repository.updateUserRegion(selectedRegion)
                    
                    // Start the food download service
                    DataSyncService.startPreloadService(this@SettingsActivity)
                    
                    Toast.makeText(this@SettingsActivity, "Updating food database in background...", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    // ===== HEALTH CONNECT METHODS =====
    
    /**
     * Check Health Connect availability and permission status
     */
    private fun checkHealthConnectStatus() {
        lifecycleScope.launch {
            try {
                val isAvailable = healthConnectManager.isHealthConnectAvailable()
                
                // Test Health Connect integration to register the app
                if (isAvailable) {
                    healthConnectManager.testHealthConnectIntegration()
                }
                
                val hasPermissions = if (isAvailable) {
                    healthConnectManager.hasAllPermissions()
                } else {
                    false
                }
                
                updateHealthConnectUI(isAvailable, hasPermissions)
            } catch (e: Exception) {
                updateHealthConnectUI(false, false)
            }
        }
    }
    
    /**
     * Update Health Connect UI based on status
     */
    private fun updateHealthConnectUI(isAvailable: Boolean, hasPermissions: Boolean) {
        // Check if Health Connect UI elements exist (they might not in the current layout)
        if (::tvHealthConnectStatus.isInitialized && ::tvHealthConnectIndicator.isInitialized && 
            ::btnSetupHealthConnect.isInitialized && ::btnSyncWorkoutData.isInitialized) {
            when {
                !isAvailable -> {
                    tvHealthConnectStatus.text = "Health Connect not available"
                    tvHealthConnectIndicator.text = "❌"
                    btnSetupHealthConnect.text = "Health Connect Required"
                    btnSetupHealthConnect.isEnabled = false
                    btnSyncWorkoutData.isEnabled = false
                }
                hasPermissions -> {
                    tvHealthConnectStatus.text = "Connected - OnePlus Watch 3 data syncing"
                    tvHealthConnectIndicator.text = "✅"
                    btnSetupHealthConnect.text = "Manage Permissions"
                    btnSetupHealthConnect.isEnabled = true
                    btnSyncWorkoutData.isEnabled = true
                }
                else -> {
                    tvHealthConnectStatus.text = "Setup required for OnePlus Watch 3 integration"
                    tvHealthConnectIndicator.text = "⚠️"
                    btnSetupHealthConnect.text = "Setup OnePlus Watch Integration"
                    btnSetupHealthConnect.isEnabled = true
                    btnSyncWorkoutData.isEnabled = false
                }
            }
        }
    }
    
    /**
     * Setup Health Connect permissions with fallback manual instructions
     */
    private fun setupHealthConnect() {
        lifecycleScope.launch {
            try {
                // First check if Health Connect is available
                val isAvailable = healthConnectManager.isHealthConnectAvailable()
                if (!isAvailable) {
                    showManualSetupInstructions()
                    return@launch
                }
                
                // Try automatic permission request
                try {
                    val requiredPermissions = getRequiredPermissions()
                    healthConnectPermissionLauncher.launch(requiredPermissions)
                } catch (e: Exception) {
                    Log.e("SettingsActivity", "Error launching permission request", e)
                    // Fall back to manual setup if automatic fails
                    showManualSetupInstructions()
                }
                
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Error in setupHealthConnect", e)
                showManualSetupInstructions()
            }
        }
    }
    
    /**
     * Show manual setup instructions when automatic permission request fails
     */
    private fun showManualSetupInstructions() {
        AlertDialog.Builder(this)
            .setTitle("Health Connect Setup")
            .setMessage("""
                KNOWN ISSUE: Apps installed before Health Connect don't appear in the apps list.
                
                SOLUTION - Force Restart Health Connect:
                
                1. Go to Settings → Apps → Health Connect
                2. Tap "Force Stop" 
                3. Open Health Connect again
                4. CalorieTracker should now appear in Apps list!
                
                Alternative Method:
                1. Settings → Apps → Health Connect → Storage → Clear Cache
                2. Restart Health Connect
                3. Check Apps list again
                
                OnePlus Users:
                1. Check OHealth → Personal Center → Data Sharing → Health Connect
                2. Ensure OHealth has Health Connect permissions
                3. Then try the force restart method above
                
                Once CalorieTracker appears:
                • Grant permissions for Active Calories, Total Calories, and Exercise Sessions
                • Return to CalorieTracker and tap "Sync Workout Data Now"
                
                This is a documented Health Connect bug affecting apps installed before Health Connect.
            """.trimIndent())
            .setPositiveButton("Open Health Connect") { _, _ ->
                openHealthConnectManually()
            }
            .setNeutralButton("Force Restart HC") { _, _ ->
                forceRestartHealthConnect()
            }
            .setNegativeButton("OK", null)
            .show()
    }
    
    /**
     * Try to open Health Connect app manually
     */
    private fun openHealthConnectManually() {
        val packageManager = packageManager
        val healthConnectPackages = listOf(
            "com.google.android.healthconnect.controller",  // User's actual package
            "com.google.android.apps.healthdata",
            "com.android.healthconnect.controller"
        )
        
        var opened = false
        for (packageName in healthConnectPackages) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    startActivity(intent)
                    opened = true
                    break
                }
            } catch (e: Exception) {
                // Continue to next package
            }
        }
        
        if (!opened) {
            Toast.makeText(this, "Please search for 'Health Connect' in your app drawer", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Manually sync workout data from Health Connect
     */
    private fun syncWorkoutData() {
        lifecycleScope.launch {
            try {
                btnSyncWorkoutData.isEnabled = false
                btnSyncWorkoutData.text = "Syncing..."
                
                val success = repository.syncTodaysWorkoutData()
                
                if (success) {
                    Toast.makeText(this@SettingsActivity, "Workout data synced successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "Failed to sync workout data. Check Health Connect permissions.", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Error syncing workout data: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                btnSyncWorkoutData.isEnabled = true
                btnSyncWorkoutData.text = "Sync Workout Data Now"
            }
        }
    }
    
    /**
     * Show debug information about Health Connect packages (long press button)
     */
    private fun showHealthConnectDebugInfo() {
        val packageManager = packageManager
        val installedPackages = mutableListOf<String>()
        
        val healthConnectPackages = listOf(
            "com.google.android.healthconnect.controller",  // User's actual Health Connect package
            "com.google.android.apps.healthdata",
            "com.android.healthconnect.controller", 
            "androidx.health.connect.client",
            "com.google.android.apps.fitness",
            "com.samsung.android.health.connect",
            "android.health.connect",
            "com.oplus.healthservice"                       // OnePlus Health Service
        )
        
        for (packageName in healthConnectPackages) {
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                installedPackages.add("✅ $packageName (v${packageInfo.versionName})")
            } catch (e: Exception) {
                installedPackages.add("❌ $packageName (not installed)")
            }
        }
        
        // Also check for any package containing "health" or "connect"
        try {
            val allPackages = packageManager.getInstalledPackages(0)
            val healthRelated = allPackages.filter { pkg ->
                pkg.packageName.contains("health", ignoreCase = true) || 
                pkg.packageName.contains("connect", ignoreCase = true)
            }.map { "📱 ${it.packageName}" }
            
            if (healthRelated.isNotEmpty()) {
                installedPackages.add("\nOther health-related apps:")
                installedPackages.addAll(healthRelated)
            }
        } catch (e: Exception) {
            installedPackages.add("Error scanning packages: ${e.message}")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Health Connect Debug Info")
            .setMessage(installedPackages.joinToString("\n"))
            .setPositiveButton("OK", null)
            .show()
    }
    
    // ===== WEIGHT GOAL METHODS =====
    
    /**
     * Calculate calorie recommendation based on current form inputs
     */
    private fun calculateCalorieRecommendation() {
        try {
            val currentWeight = etCurrentWeight.text.toString().toDoubleOrNull()
            val targetWeight = etTargetWeight.text.toString().toDoubleOrNull()
            val timelineDays = etTimelineDays.text.toString().toIntOrNull()
            
            if (currentWeight == null || targetWeight == null || timelineDays == null) {
                Toast.makeText(this, "Please fill in current weight, target weight, and timeline", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (timelineDays <= 0) {
                Toast.makeText(this, "Timeline must be greater than 0 days", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Get activity level
            val activityLevels = CalorieCalculator.getActivityLevels()
            val selectedActivityIndex = spinnerActivityLevel.selectedItemPosition
            val activityLevel = if (selectedActivityIndex >= 0) {
                activityLevels[selectedActivityIndex].first
            } else {
                "lightly_active"
            }
            
            // Get optional demographics
            val age = etAge.text.toString().toIntOrNull()
            val height = etHeight.text.toString().toDoubleOrNull()
            val genderIndex = spinnerGender.selectedItemPosition
            val gender = when (genderIndex) {
                1 -> "male"
                2 -> "female"
                else -> null
            }
            
            // Create weight goal for calculation
            val weightGoal = WeightGoal(
                currentWeight = currentWeight,
                targetWeight = targetWeight,
                targetDays = timelineDays,
                activityLevel = activityLevel,
                age = age,
                height = height,
                gender = gender,
                createdDate = getCurrentDateString()
            )
            
            // Calculate recommendation
            val useMetricUnits = currentGoals?.useMetricUnits ?: false
            val recommendation = CalorieCalculator.calculateCalorieRecommendation(weightGoal, useMetricUnits)
            
            // Display recommendation
            displayCalorieRecommendation(recommendation, useMetricUnits)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error calculating recommendation: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Display the calorie recommendation in the UI using interactive cards
     */
    private fun displayCalorieRecommendation(recommendation: CalorieCalculator.CalorieRecommendation, useMetricUnits: Boolean) {
        val weightUnit = if (useMetricUnits) "kg" else "lbs"
        val weightChangeText = if (recommendation.weeklyWeightChange >= 0) {
            "gain ${String.format("%.1f", recommendation.weeklyWeightChange)} $weightUnit/week"
        } else {
            "lose ${String.format("%.1f", -recommendation.weeklyWeightChange)} $weightUnit/week"
        }
        
        // Update individual metric cards
        tvRecommendedCaloriesValue.text = "${recommendation.recommendedCalories} cal"
        tvBMRValue.text = "${recommendation.bmr} cal"
        tvTDEEValue.text = "${recommendation.tdee} cal"
        tvWeightChangeRateValue.text = weightChangeText
        
        // Update health status
        if (!recommendation.isHealthy && recommendation.healthWarning != null) {
            tvHealthStatus.text = "⚠️ ${recommendation.healthWarning}"
            tvHealthStatus.setTextColor(getColor(R.color.warning_orange))
        } else {
            tvHealthStatus.text = "✅ This goal is within healthy guidelines"
            tvHealthStatus.setTextColor(getColor(R.color.success_green))
        }
        
        // Show the interactive cards layout and hide old text view
        layoutMetricsCards.visibility = View.VISIBLE
        tvCalorieRecommendation.visibility = View.GONE
    }
    
    /**
     * Save the weight goal to the database
     */
    private fun saveWeightGoal() {
        lifecycleScope.launch {
            try {
                val currentWeight = etCurrentWeight.text.toString().toDoubleOrNull()
                val targetWeight = etTargetWeight.text.toString().toDoubleOrNull()
                val timelineDays = etTimelineDays.text.toString().toIntOrNull()
                
                if (currentWeight == null || targetWeight == null || timelineDays == null) {
                    Toast.makeText(this@SettingsActivity, "Please fill in current weight, target weight, and timeline", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Get activity level
                val activityLevels = CalorieCalculator.getActivityLevels()
                val selectedActivityIndex = spinnerActivityLevel.selectedItemPosition
                val activityLevel = if (selectedActivityIndex >= 0) {
                    activityLevels[selectedActivityIndex].first
                } else {
                    "lightly_active"
                }
                
                // Get optional demographics
                val age = etAge.text.toString().toIntOrNull()
                val height = etHeight.text.toString().toDoubleOrNull()
                val genderIndex = spinnerGender.selectedItemPosition
                val gender = when (genderIndex) {
                    1 -> "male"
                    2 -> "female"
                    else -> null
                }
                
                // Create weight goal
                val weightGoal = WeightGoal(
                    currentWeight = currentWeight,
                    targetWeight = targetWeight,
                    targetDays = timelineDays,
                    activityLevel = activityLevel,
                    age = age,
                    height = height,
                    gender = gender,
                    createdDate = getCurrentDateString()
                )
                
                // Save to database
                repository.setWeightGoal(weightGoal)
                
                // Get recommendation and update calorie goal
                val recommendation = repository.getCalorieRecommendationFromWeightGoal()
                if (recommendation != null) {
                    // Update the calorie goal field
                    etCalorieGoal.setText(recommendation.recommendedCalories.toString())
                    
                    Toast.makeText(this@SettingsActivity, "Weight goal saved and calorie goal updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SettingsActivity, "Weight goal saved successfully!", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Error saving weight goal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Load existing weight goal if available
     */
    private fun loadWeightGoal() {
        lifecycleScope.launch {
            try {
                val weightGoal = repository.getCurrentWeightGoal()
                if (weightGoal != null) {
                    // Fill in the form with existing weight goal
                    etCurrentWeight.setText(weightGoal.currentWeight.toString())
                    etTargetWeight.setText(weightGoal.targetWeight.toString())
                    etTimelineDays.setText(weightGoal.targetDays.toString())
                    
                    // Set activity level
                    val activityLevels = CalorieCalculator.getActivityLevels()
                    val activityIndex = activityLevels.indexOfFirst { it.first == weightGoal.activityLevel }
                    if (activityIndex >= 0) {
                        spinnerActivityLevel.setSelection(activityIndex)
                    }
                    
                    // Set optional fields
                    weightGoal.age?.let { etAge.setText(it.toString()) }
                    weightGoal.height?.let { etHeight.setText(it.toString()) }
                    
                    // Set gender
                    val genderIndex = when (weightGoal.gender) {
                        "male" -> 1
                        "female" -> 2
                        else -> 0
                    }
                    spinnerGender.setSelection(genderIndex)
                    
                    // Auto-calculate recommendation
                    calculateCalorieRecommendation()
                }
            } catch (e: Exception) {
                // Weight goal doesn't exist yet, which is fine
            }
        }
    }
    
    private fun getCurrentDateString(): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return formatter.format(java.util.Date())
    }
    
    /**
     * Attempt to force restart Health Connect to refresh app list
     */
    private fun forceRestartHealthConnect() {
        try {
            val packageManager = packageManager
            val healthConnectPackages = listOf(
                "com.google.android.apps.healthdata",
                "com.google.android.healthconnect.controller",
                "com.android.healthconnect.controller"
            )
            
            // Try to open Health Connect settings page to force restart
            for (packageName in healthConnectPackages) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    Toast.makeText(this, "Tap 'Force Stop' then restart Health Connect", Toast.LENGTH_LONG).show()
                    return
                } catch (e: Exception) {
                    // Continue to next package
                }
            }
            
            // Fallback to general apps settings
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Find Health Connect → Force Stop → Restart", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Go to Settings → Apps → Health Connect → Force Stop", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check Health Connect status when returning to settings
        checkHealthConnectStatus()
    }
}

/**
 * ViewPager2 adapter for settings tabs
 */
class SettingsPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 6
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SettingsGoalsFragment()
            1 -> SettingsWeightFragment()
            2 -> SettingsRegionalFragment()
            3 -> SettingsPreferencesFragment()
            4 -> SettingsHealthFragment()
            5 -> SettingsScaleFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}