package com.calorietracker

// 🧰 IMPORT OUR TOOLS - These are like different tools in a toolbox that we need to build our food entry screen
// Think of imports like ingredients in a recipe - we gather everything we need before we start cooking!

// ANDROID FRAMEWORK IMPORTS - Core Android functionality
import android.app.DatePickerDialog           // 📅 Creates a calendar popup for choosing dates (like "I ate this yesterday")
import android.os.Bundle                      // 💾 Saves app state during phone rotations (so data doesn't disappear)
import android.util.Log                       // 📋 Android logging system for debugging
import android.text.Editable                  // ✏️ Represents text that can be changed in text fields
import android.text.TextWatcher               // 👁️ Listens for when user types in text fields (like a spy watching keystrokes)
import android.widget.Toast                   // 📢 Small popup messages at bottom of screen ("Food saved!")
import android.widget.TextView                 // 📝 Text display widget for showing search results header
import androidx.appcompat.app.AppCompatActivity // 📱 Basic template for app screens (gives us onCreate, buttons, etc.)
import androidx.lifecycle.lifecycleScope      // ⚡ For background tasks that don't freeze the UI

// OUR APP'S CUSTOM IMPORTS - Code we wrote specifically for this calorie tracker
import com.calorietracker.database.CalorieDatabase   // 🗃️ Our nutrition database (digital filing cabinet)
import com.calorietracker.database.CalorieEntry     // 📝 One food entry (like "1 apple, 95 calories")
import com.calorietracker.database.FoodItem         // 🍎 Food data from online search databases
import com.calorietracker.repository.CalorieRepository // 🏛️ Data manager that handles saving/loading nutrition info
import com.calorietracker.utils.ThemeManager        // 🌙 Dark/light theme switcher for user preference
import com.calorietracker.utils.PerformanceOptimizer // 🚀 Performance improvements and optimizations
import com.calorietracker.utils.ErrorHandler         // 🛡️ User-friendly error handling

// MATERIAL DESIGN IMPORTS - Google's pretty UI components
import com.google.android.material.textfield.TextInputEditText // 💎 Pretty text input fields with floating labels
import com.google.android.material.card.MaterialCardView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.calorietracker.adapters.FoodSearchResultsAdapter

// KOTLIN COROUTINES - For doing multiple things at once without blocking the UI
import kotlinx.coroutines.Job                       // 🎯 Represents a background task (like "search food databases")
import kotlinx.coroutines.delay                     // ⏱️ Wait function for background tasks ("wait 500ms before searching")
import kotlinx.coroutines.launch                    // 🚀 Start background tasks without freezing the screen
import kotlinx.coroutines.withTimeoutOrNull         // ⏰ Timeout wrapper to prevent hanging API calls

// JAVA UTILITIES - Built-in Java tools for dates and formatting
import java.text.SimpleDateFormat                   // 📊 Format dates for display ("Aug 31, 2024" vs "2024-08-31")
import java.util.*                                  // 📅 Date and time utilities (Calendar, Date, etc.)

/**
 * 📝 FOOD ENTRY SCREEN - WHERE USERS ADD OR EDIT THEIR MEALS
 * 
 * Hi young programmer! This is like a digital nutrition label that users can fill out.
 * 
 * 🍎 What can users do here?
 * 1. Type in a food name (like "Apple" or "Chocolate Chip Cookie")
 * 2. Search for foods with smart suggestions (like Google search but for food!)
 * 3. Enter all the nutrition facts (calories, protein, carbs, etc.)
 * 4. Choose what date this food was eaten (for catching up on missed entries)
 * 5. Calculate portions (like "I ate 2 servings" or "half a cup")
 * 6. Edit existing food entries (fix that 14,000 calorie Coke!)
 * 
 * 🎯 This screen can work in two modes:
 * - ADD MODE: Creating a brand new food entry
 * - EDIT MODE: Modifying an existing food entry that was already saved
 * 
 * Think of it like a smart form that helps users track exactly what they eat!
 */
class CalorieEntryActivity : AppCompatActivity() {
    
    // 🗃️ OUR DATA MANAGER
    // Think of this like a librarian who helps us save and load nutrition information
    private lateinit var repository: CalorieRepository
    
    // 📷 BARCODE INFO
    // If this food entry came from scanning a barcode, we save the barcode number here
    // The "?" means this might be empty (null) if user typed the food manually
    private var scannedBarcode: String? = null
    
    // ✏️ EDIT MODE VARIABLES
    // When users want to fix an existing food entry (like that 14,000 calorie Coke!),
    // we store the entry's ID number and remember we're in "edit mode"
    private var editingEntryId: Long? = null  // The ID number of the entry we're editing
    private var isEditMode: Boolean = false   // Are we editing (true) or creating new (false)?
    
    // 🔍 SEARCH FUNCTIONALITY
    // These help us provide smart food suggestions as the user types
    private var searchJob: Job? = null                    // Background task for searching food databases
    private var searchResults: List<FoodItem> = emptyList() // List of food suggestions found
    private lateinit var searchResultsAdapter: FoodSearchResultsAdapter // Manages the search results list
    private var searchSequence: Int = 0                  // Track search sequence to prevent race conditions
    private var isSettingTextFromSelection: Boolean = false // Prevent search during result selection
    
    // UI COMPONENTS FOR CUSTOM DROPDOWN
    private lateinit var cardSearchResults: MaterialCardView
    private lateinit var recyclerSearchResults: RecyclerView
    
    // 📝 INPUT FIELDS - These are the text boxes where users type information
    // Think of each one like a different line on a nutrition label
    private lateinit var etFoodName: TextInputEditText     // 🍎 Food name text input
    private lateinit var etEntryDate: TextInputEditText    // 📅 Date picker (for retroactive entries)
    private lateinit var etServingSize: TextInputEditText  // 🥄 Serving size ("1 cup", "2 slices")
    private lateinit var etCalories: TextInputEditText     // 🔥 Calories (like "150")
    private lateinit var etProtein: TextInputEditText      // 💪 Protein in grams (like "5.2")
    private lateinit var etCarbs: TextInputEditText        // 🍞 Carbohydrates in grams (like "30.1")
    private lateinit var etFat: TextInputEditText          // 🥑 Fat in grams (like "8.5")
    private lateinit var etFiber: TextInputEditText        // 🌾 Fiber in grams (like "2.1")
    private lateinit var etSugar: TextInputEditText        // 🍯 Sugar in grams (like "12.3")
    private lateinit var etSodium: TextInputEditText       // 🧂 Sodium in milligrams (like "450")
    
    // 📅 DATE MANAGEMENT
    // These help us handle dates when users want to add food from previous days
    private var selectedCalendar: Calendar = Calendar.getInstance() // Currently selected date
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())           // For database storage "2024-08-31"
    private val displayDateFormatter = SimpleDateFormat("MMM dd, yyyy (EEEE)", Locale.getDefault()) // For user display "Aug 31, 2024 (Saturday)"
    
    // 🧮 SERVING SIZE CALCULATOR
    // When users change serving size (like "2 servings" instead of "1 serving"),
    // we need to multiply all nutrition values accordingly
    private var originalNutritionData: NutritionData? = null // Original values before serving size changes
    
    /**
     * 📊 NUTRITION DATA CONTAINER
     * 
     * This is like a recipe card that holds all the nutrition information for one food.
     * We use this to store the original nutrition values so we can recalculate them
     * when users change the serving size.
     * 
     * For example:
     * - 1 slice of bread = 80 calories, 3g protein, 15g carbs
     * - 2 slices of bread = 160 calories, 6g protein, 30g carbs (everything doubled!)
     */
    data class NutritionData(
        val calories: Int,      // 🔥 Energy from food
        val protein: Double?,   // 💪 Protein content (question mark means it might be unknown)
        val carbs: Double?,     // 🍞 Carbohydrate content
        val fat: Double?,       // 🥑 Fat content  
        val fiber: Double?,     // 🌾 Fiber content
        val sugar: Double?,     // 🍯 Sugar content
        val sodium: Double?     // 🧂 Sodium content
    )
    
    /**
     * This method runs when the screen first opens
     * It sets up the form and loads any data passed from barcode scanning
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before setting content view
        ThemeManager.applyTheme(this)
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calorie_entry) // Load the visual layout
        
        // Set up database connection
        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)
        
        // Set up all the form elements
        initViews()         // Connect to all the input fields
        setupClickListeners() // Set up button actions
        loadIntentData()    // Load any pre-filled data from barcode scanning or editing
    }
    
    /**
     * 🔍 CONNECT TO ALL THE INPUT FIELDS: initViews()
     * 
     * This function is like making a list of all the furniture in your room.
     * We need to tell our code where each text box, button, and input field is located
     * in the visual layout so we can control them later.
     * 
     * Think of findViewById() like playing "Where's Waldo?" - we're searching for specific
     * UI elements by their ID name and storing references to them in variables.
     * 
     * Once we find them, we can:
     * - Read what the user typed (like getting text from food name field)
     * - Change their content (like auto-filling calories when food is selected)
     * - Listen for when they change (like recalculating nutrition when serving size changes)
     */
    private fun initViews() {
        // 🍎 FOOD IDENTIFICATION FIELDS
        // These fields help identify WHAT food the user is adding
        etFoodName = findViewById(R.id.etFoodName)       // 🔍 Food name text input
        etEntryDate = findViewById(R.id.etEntryDate)     // 📅 Date picker for retroactive entries ("I forgot to log yesterday's lunch")
        etServingSize = findViewById(R.id.etServingSize) // 🥄 Text box for portion size ("1 cup", "2 slices", "half a banana")
        
        // 🔍 SEARCH RESULTS UI COMPONENTS
        cardSearchResults = findViewById(R.id.cardSearchResults)
        recyclerSearchResults = findViewById(R.id.recyclerSearchResults)
        
        // 🔥 MAIN NUTRITION FIELD
        // This is the most important field - every food must have calories
        etCalories = findViewById(R.id.etCalories)       // 🔥 Text box for energy content (the main number everyone cares about!)
        
        // 💪 MACRONUTRIENT FIELDS (the "big three" nutrients)
        // These are optional but help users track balanced nutrition
        etProtein = findViewById(R.id.etProtein)         // 💪 Protein content in grams (builds muscles, keeps you full)
        etCarbs = findViewById(R.id.etCarbs)             // 🍞 Carbohydrate content in grams (energy for brain and muscles) 
        etFat = findViewById(R.id.etFat)                 // 🥑 Fat content in grams (healthy fats are essential!)
        
        // 🌾 DETAILED NUTRITION FIELDS
        // These provide more specific nutritional information for health-conscious users
        etFiber = findViewById(R.id.etFiber)             // 🌾 Fiber content in grams (helps digestion, keeps you full)
        etSugar = findViewById(R.id.etSugar)             // 🍯 Sugar content in grams (naturally occurring + added sugars)
        etSodium = findViewById(R.id.etSodium)           // 🧂 Sodium content in milligrams (important for blood pressure)
        
        // 🛠️ SET UP INTERACTIVE FEATURES
        // Now that we've found all our form fields, make them smart and interactive!
        setupDatePicker()            // Make the date field open a calendar when tapped
        setupFoodSearch()            // Add search functionality to the food name field  
        setupServingSizeCalculation() // Auto-recalculate nutrition when serving size changes
    }
    
    /**
     * 📅 SET UP THE DATE PICKER: setupDatePicker()
     * 
     * This makes the date field interactive! When users tap on it, a calendar popup appears.
     * This is useful for retroactive entries - like when someone forgot to log yesterday's lunch.
     * 
     * How it works:
     * 1. Initialize with today's date (most entries are for today)
     * 2. When user taps the field, show a calendar dialog
     * 3. User picks a date, we update the field and remember their choice
     * 4. Prevent picking dates too far in the past (max 30 days) or future dates
     * 
     * Think of it like a time machine for food entries, but with safety limits!
     */
    private fun setupDatePicker() {
        // Initialize with today's date
        updateDateDisplay()
        
        // Set up click listener for date field
        etEntryDate.setOnClickListener {
            showDatePickerDialog()
        }
    }
    
    /**
     * Show date picker dialog to let user select a date
     */
    private fun showDatePickerDialog() {
        val year = selectedCalendar.get(Calendar.YEAR)
        val month = selectedCalendar.get(Calendar.MONTH)
        val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Update the selected date
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                updateDateDisplay()
            },
            year, month, day
        )
        
        // Set minimum date to 30 days ago (prevent too far back entries)
        val minCalendar = Calendar.getInstance()
        minCalendar.add(Calendar.DAY_OF_MONTH, -30)
        datePickerDialog.datePicker.minDate = minCalendar.timeInMillis
        
        // Set maximum date to today (prevent future entries)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        
        datePickerDialog.show()
    }
    
    /**
     * Update the date display in the text field
     */
    private fun updateDateDisplay() {
        val displayText = if (isToday(selectedCalendar)) {
            "Today (${displayDateFormatter.format(selectedCalendar.time)})"
        } else {
            displayDateFormatter.format(selectedCalendar.time)
        }
        etEntryDate.setText(displayText)
    }
    
    /**
     * Check if the selected date is today
     */
    private fun isToday(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
               calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * 🔍 SET UP SMART FOOD SEARCH: setupFoodSearch()
     * 
     * This creates an intelligent food search system using a custom dropdown interface.
     * When users type food names, we show search results in a clean card below the input.
     * 
     * Features:
     * - Real-time suggestions as you type (like Google search)
     * - Custom RecyclerView-based results display (bypasses AutoCompleteTextView issues)
     * - Searches multiple databases (USDA, Open Food Facts, saved foods)
     * - Auto-fills calories, protein, carbs, fat, fiber, sugar, sodium
     * - Clean, modern UI with nutrition details
     */
    private fun setupFoodSearch() {
        // Set up the RecyclerView for search results
        searchResultsAdapter = FoodSearchResultsAdapter { selectedFood ->
            onFoodItemSelected(selectedFood)
        }
        
        recyclerSearchResults.apply {
            layoutManager = LinearLayoutManager(this@CalorieEntryActivity)
            adapter = searchResultsAdapter
        }
        
        // Set up hide results button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHideResults)?.setOnClickListener {
            hideSearchResults()
        }
        
        // Add text change listener for search
        etFoodName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()
                Log.d("CalorieEntry", "🔍 DEBUG afterTextChanged: query='$query', isSettingTextFromSelection=$isSettingTextFromSelection")
                
                // Don't search when we're programmatically setting text from selection
                if (isSettingTextFromSelection) {
                    return
                }
                
                if (!query.isNullOrEmpty() && query.length >= 2) {
                    performFoodSearch(query)
                } else {
                    // Clear search results if query is too short
                    hideSearchResults()
                }
            }
        })
    }
    
    /**
     * Handle when a user selects a food item from search results
     */
    private fun onFoodItemSelected(selectedFood: FoodItem) {
        Log.d("CalorieEntry", "🔍 Selected food: ${selectedFood.name}")
        
        // Set flag to prevent search during text update
        isSettingTextFromSelection = true
        
        // Fill form with selected food data
        fillFormWithFoodData(selectedFood)
        
        // Clean the food name to remove (saved) indicator for display
        val cleanName = selectedFood.name.replace(" (saved)", "")
        etFoodName.setText(cleanName)
        
        // Hide the search results
        hideSearchResults()
        
        // Reset flag after a short delay
        etFoodName.post {
            isSettingTextFromSelection = false
        }
    }
    
    /**
     * Show search results in the custom dropdown card
     */
    private fun showSearchResults(results: List<FoodItem>) {
        if (results.isNotEmpty()) {
            searchResults = results
            searchResultsAdapter.updateItems(results)
            cardSearchResults.visibility = android.view.View.VISIBLE
            
            // Update header text
            findViewById<TextView>(R.id.tvSearchResultsHeader)?.text = "Found ${results.size} foods:"
        } else {
            hideSearchResults()
        }
    }
    
    /**
     * Hide the search results card
     */
    private fun hideSearchResults() {
        cardSearchResults.visibility = android.view.View.GONE
        searchResults = emptyList()
    }
    
    /**
     * 🌐 SEARCH FOR FOOD ITEMS ONLINE: performFoodSearch()
     * 
     * This function is like having a personal nutrition researcher who searches
     * multiple food databases to find the exact nutrition information you need.
     * 
     * What is "debouncing"?
     * Imagine if every single keystroke immediately triggered a database search:
     * - User types "a" → search for "a" 
     * - User types "p" → search for "ap"
     * - User types "p" → search for "app"
     * - User types "l" → search for "appl"
     * - User types "e" → search for "apple"
     * 
     * That would be 5 API calls just to type "apple"! Very wasteful and slow.
     * 
     * Debouncing solution:
     * - Wait 300ms after user stops typing (reduced from 500ms for better responsiveness)
     * - THEN search for the complete word "apple"
     * - Cancel previous search if user is still typing
     * - Result: 1 efficient API call instead of 5 wasteful ones
     * 
     * The search process:
     * 1. Check if we have internet connection
     * 2. Search our repository (which checks USDA, Open Food Facts, local foods)
     * 3. Get up to 8 suggestions
     * 4. Update the dropdown with food names + brands
     * 5. Show the dropdown if we found anything
     * 6. Handle errors gracefully (no internet? no problem!)
     */
    private fun performFoodSearch(query: String) {
        // Use our PerformanceOptimizer for intelligent search debouncing
        PerformanceOptimizer.debounceSearch("food_search", 500) {
            Log.d("CalorieEntry", "🔍 Starting optimized search for '$query'")
            executeSearch(query)
        }
    }
    
    private fun executeSearch(query: String) {
        // Cancel previous search if still running
        searchJob?.cancel()
        
        // Increment sequence number for this search
        val currentSequence = ++searchSequence
        
        searchJob = lifecycleScope.launch {
            try {
                // Check if we have cached results first
                val cacheKey = "search_$query"
                val cachedResults = PerformanceOptimizer.getCached<List<FoodItem>>(cacheKey)
                if (cachedResults != null) {
                    Log.d("CalorieEntry", "📦 Using cached results for '$query'")
                    searchResults = cachedResults
                    runOnUiThread {
                        if (currentSequence == searchSequence) {
                            showSearchResults(cachedResults)
                        }
                    }
                    return@launch
                }
                
                // Check if this search is still the latest one
                if (currentSequence != searchSequence) {
                    Log.d("CalorieEntry", "🔍 DEBUG: Search #$currentSequence cancelled - newer search started")
                    return@launch
                }
                
                // Quick network check with timeout to prevent hanging
                val networkAvailable = try {
                    withTimeoutOrNull(2000) { // 2 second timeout
                        repository.isNetworkAvailable()
                    } ?: false
                } catch (e: Exception) {
                    false
                }
                
                if (networkAvailable) {
                    Log.d("CalorieEntry", "🔍 DEBUG: Search #$currentSequence calling repository...")
                    
                    // Call repository with timeout to prevent ANR
                    val results = try {
                        withTimeoutOrNull(8000) { // 8 second timeout for API call
                            repository.searchFoodByName(query, 12)
                        } ?: emptyList()
                    } catch (e: Exception) {
                        Log.e("CalorieEntry", "🚨 API call failed: ${e.message}")
                        emptyList()
                    }
                    
                    // Check again if this search is still the latest
                    if (currentSequence != searchSequence) {
                        Log.d("CalorieEntry", "🔍 DEBUG: Search #$currentSequence cancelled after network call")
                        return@launch
                    }
                    
                    Log.d("CalorieEntry", "🔍 DEBUG: Search #$currentSequence got ${results.size} results")
                    searchResults = results
                    
                    // Cache the results for future searches
                    val cacheKey = "search_$query"
                    PerformanceOptimizer.setCached(cacheKey, results)
                    
                    // Clean up search results - remove (saved) indicators and format properly
                    val suggestions = results.map { foodItem ->
                        val cleanName = foodItem.name.replace(" (saved)", "")
                        if (foodItem.brand != null) "$cleanName - ${foodItem.brand}" else cleanName
                    }
                    
                    // Update UI only if this is still the latest search
                    runOnUiThread {
                        if (currentSequence == searchSequence) {
                            Log.d("CalorieEntry", "🔍 DEBUG: Search #$currentSequence - updating UI with ${results.size} results")
                            if (results.isNotEmpty()) {
                                showSearchResults(results)
                                Toast.makeText(this@CalorieEntryActivity, "Found ${results.size} foods", Toast.LENGTH_SHORT).show()
                            } else {
                                hideSearchResults()
                                Toast.makeText(this@CalorieEntryActivity, "No results for '$query'", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    // No internet - show offline message
                    runOnUiThread {
                        if (currentSequence == searchSequence) {
                            hideSearchResults()
                            Toast.makeText(this@CalorieEntryActivity, "No internet - try manual entry", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CalorieEntry", "🚨 ERROR in performFoodSearch #$currentSequence: ${e.message}")
                runOnUiThread {
                    if (currentSequence == searchSequence) {
                        hideSearchResults()
                        ErrorHandler.handleError(this@CalorieEntryActivity, e, "searching for foods")
                    }
                }
            }
        }
    }
    
    /**
     * 🔍 IMMEDIATE SEARCH WITHOUT DEBOUNCE: performFoodSearchImmediate()
     * 
     * This function performs an immediate search when the user clicks the search button.
     * Unlike the automatic search (which waits 300ms), this runs instantly when requested.
     * 
     * This gives users a reliable way to trigger search when:
     * - Automatic search didn't work
     * - They want to refresh results
     * - Network was slow the first time
     */
    private fun performFoodSearchImmediate(query: String) {
        // Cancel any ongoing search
        searchJob?.cancel()
        
        searchJob = lifecycleScope.launch {
            try {
                // No delay - search immediately!
                
                val networkAvailable = repository.isNetworkAvailable()
                
                if (networkAvailable) {
                    val results = repository.searchFoodByName(query, 15) // More results for manual search
                    
                    // Log each result for debugging
                    results.forEachIndexed { index, foodItem ->
                    }
                    
                    searchResults = results
                    
                    // Clean up search results and update dropdown
                    val suggestions = if (results.isNotEmpty()) {
                        results.map { foodItem ->
                            val cleanName = foodItem.name.replace(" (saved)", "")
                            if (foodItem.brand != null) "$cleanName - ${foodItem.brand}" else cleanName
                        }
                    } else {
                        listOf("No results found for '$query' - try different keywords")
                    }
                    
                    runOnUiThread {
                        showSearchResults(results)
                    }
                    
                    // Show success message with instruction
                    if (results.isNotEmpty()) {
                        Toast.makeText(this@CalorieEntryActivity, "Found ${results.size} foods - dropdown should appear below", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // No internet connection
                    hideSearchResults()
                    Toast.makeText(this@CalorieEntryActivity, "No internet connection", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Search failed, show detailed error
                hideSearchResults()
                Toast.makeText(this@CalorieEntryActivity, "Search failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * ✨ AUTO-FILL NUTRITION MAGIC: fillFormWithFoodData()
     * 
     * This is the "wow factor" function that users love! When they select a food
     * from the dropdown suggestions, we automatically populate ALL the nutrition
     * fields for them. It's like having a nutrition expert fill out the form instantly.
     * 
     * What happens step by step:
     * 1. User selects "Banana, medium" from dropdown
     * 2. We grab ALL the nutrition data for that food item
     * 3. Store the "original" values for serving size calculations later
     * 4. Fill in the serving size ("1 medium banana")
     * 5. Fill in calories (105)
     * 6. Fill in protein (1.3g), carbs (27g), fat (0.4g), etc.
     * 7. Format numbers nicely ("1.3" not "1.2999999")
     * 8. Show success message with tip about serving sizes
     * 
     * Smart number formatting:
     * - Protein/carbs/fat: 1 decimal place ("5.2g")
     * - Sodium: whole numbers ("150mg" not "150.0mg")
     * - Handle missing data gracefully (some foods don't have fiber data)
     * 
     * This saves users 30+ seconds of manual data entry per food item!
     * Instead of typing in 8 different nutrition values, they just pick from a list.
     */
    private fun fillFormWithFoodData(foodItem: FoodItem) {
        // Store original nutrition data for serving size calculations
        originalNutritionData = NutritionData(
            calories = foodItem.caloriesPerServing,
            protein = foodItem.proteinPerServing,
            carbs = foodItem.carbsPerServing,
            fat = foodItem.fatPerServing,
            fiber = foodItem.fiberPerServing,
            sugar = foodItem.sugarPerServing,
            sodium = foodItem.sodiumPerServing
        )
        
        // Set serving size if available
        foodItem.servingSize?.let { servingSize ->
            etServingSize.setText(servingSize)
        }
        
        // Fill nutrition fields
        etCalories.setText(foodItem.caloriesPerServing.toString())
        
        foodItem.proteinPerServing?.let { 
            etProtein.setText(String.format("%.1f", it))
        }
        
        foodItem.carbsPerServing?.let { 
            etCarbs.setText(String.format("%.1f", it))
        }
        
        foodItem.fatPerServing?.let { 
            etFat.setText(String.format("%.1f", it))
        }
        
        foodItem.fiberPerServing?.let { 
            etFiber.setText(String.format("%.1f", it))
        }
        
        foodItem.sugarPerServing?.let { 
            etSugar.setText(String.format("%.1f", it))
        }
        
        foodItem.sodiumPerServing?.let { 
            etSodium.setText(String.format("%.0f", it))
        }
        
        Toast.makeText(this, "Auto-filled nutrition data! Adjust serving size to automatically recalculate.", Toast.LENGTH_LONG).show()
    }
    
    /**
     * 🧮 SET UP SERVING SIZE CALCULATOR: setupServingSizeCalculation()
     * 
     * This creates an intelligent serving size system that automatically adjusts ALL
     * nutrition values when users change the portion size. It's like having a built-in
     * math tutor that handles the multiplication for you!
     * 
     * How it works:
     * 1. When food is auto-filled, we save the "original" nutrition data
     * 2. User changes serving size from "1 serving" to "2 servings"
     * 3. We automatically DOUBLE all nutrition values (calories, protein, etc.)
     * 4. Works with various formats: "2 cups", "1.5 servings", "half", "double"
     * 
     * Smart parsing examples:
     * - "2 cups" → multiplies by 2
     * - "1.5 servings" → multiplies by 1.5  
     * - "half" or "0.5" → multiplies by 0.5
     * - "double" or "twice" → multiplies by 2
     * - "quarter" → multiplies by 0.25
     * 
     * This prevents math errors and makes portion tracking effortless!
     * No more manual calculation: "If 1 slice of pizza is 300 calories, 
     * then 2.5 slices is... um... *pulls out calculator*"
     */
    private fun setupServingSizeCalculation() {
        etServingSize.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val newServingText = s?.toString()?.trim()
                if (!newServingText.isNullOrEmpty() && originalNutritionData != null) {
                    recalculateNutrition(newServingText)
                }
            }
        })
    }
    
    /**
     * Recalculate nutrition based on serving size changes
     */
    private fun recalculateNutrition(newServingText: String) {
        val originalData = originalNutritionData ?: return
        
        try {
            // Extract multiplier from serving size text
            val multiplier = extractServingMultiplier(newServingText)
            
            if (multiplier > 0) {
                // Update all nutrition fields
                etCalories.setText((originalData.calories * multiplier).toInt().toString())
                
                originalData.protein?.let { 
                    etProtein.setText(String.format("%.1f", it * multiplier))
                }
                
                originalData.carbs?.let { 
                    etCarbs.setText(String.format("%.1f", it * multiplier))
                }
                
                originalData.fat?.let { 
                    etFat.setText(String.format("%.1f", it * multiplier))
                }
                
                originalData.fiber?.let { 
                    etFiber.setText(String.format("%.1f", it * multiplier))
                }
                
                originalData.sugar?.let { 
                    etSugar.setText(String.format("%.1f", it * multiplier))
                }
                
                originalData.sodium?.let { 
                    etSodium.setText(String.format("%.0f", it * multiplier))
                }
            }
        } catch (e: Exception) {
            // If parsing fails, just continue without recalculation
        }
    }
    
    /**
     * Extract serving multiplier from serving size text
     * Examples: "2 cups" -> 2.0, "1.5 servings" -> 1.5, "half" -> 0.5
     */
    private fun extractServingMultiplier(servingText: String): Double {
        val text = servingText.lowercase().trim()
        
        // Handle common text representations
        when {
            text.contains("half") || text.contains("0.5") -> return 0.5
            text.contains("quarter") || text.contains("0.25") -> return 0.25
            text.contains("double") || text.contains("twice") -> return 2.0
            text.contains("triple") -> return 3.0
        }
        
        // Try to extract number from the beginning of the text
        val numberRegex = Regex("""^(\d+(?:\.\d+)?)""")
        val matchResult = numberRegex.find(text)
        
        return if (matchResult != null) {
            matchResult.groupValues[1].toDoubleOrNull() ?: 1.0
        } else {
            1.0 // Default to 1 serving if no number found
        }
    }
    
    private fun setupClickListeners() {
        findViewById<android.widget.Button>(R.id.btnCancel).setOnClickListener {
            finish()
        }
        
        findViewById<android.widget.Button>(R.id.btnSave).setOnClickListener {
            saveCalorieEntry()
        }
        
        // Manual search button - force search without waiting for debounce
        try {
            val searchButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnManualSearch)
            if (searchButton != null) {
                searchButton.setOnClickListener {
                    val query = etFoodName.text?.toString()?.trim()
                    
                    if (!query.isNullOrEmpty() && query.length >= 2) {
                        // Cancel current search and start immediate search
                        searchJob?.cancel()
                        performFoodSearchImmediate(query)
                        Toast.makeText(this, "Searching for '$query'...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Enter at least 2 characters to search", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Search button not found in layout", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
        }
        
        // Quick serving size buttons
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHalfServing)?.setOnClickListener {
            etServingSize.setText("0.5 serving")
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSingleServing)?.setOnClickListener {
            etServingSize.setText("1 serving")
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDoubleServing)?.setOnClickListener {
            etServingSize.setText("2 servings")
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSmallServing)?.setOnClickListener {
            etServingSize.setText("0.75 serving")
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMediumServing)?.setOnClickListener {
            etServingSize.setText("1 serving")
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLargeServing)?.setOnClickListener {
            etServingSize.setText("1.5 servings")
        }
        
        // Quick calorie buttons
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCalories100).setOnClickListener {
            etCalories.setText("100")
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCalories200).setOnClickListener {
            etCalories.setText("200")
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCalories300).setOnClickListener {
            etCalories.setText("300")
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCalories500).setOnClickListener {
            etCalories.setText("500")
        }
    }
    
    private fun loadIntentData() {
        intent?.let { intent ->
            // Check if we're in edit mode
            editingEntryId = intent.getLongExtra("EDIT_ENTRY_ID", -1L).takeIf { it != -1L }
            isEditMode = editingEntryId != null
            
            // Update title and save button text based on mode
            if (isEditMode) {
                title = "Edit Food Entry"
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)?.text = "Update Entry"
            }
            
            scannedBarcode = intent.getStringExtra("barcode") ?: intent.getStringExtra("EDIT_BARCODE")
            
            val foodName = intent.getStringExtra("food_name") ?: intent.getStringExtra("EDIT_FOOD_NAME")
            foodName?.let { 
                etFoodName.setText(it)
            }
            
            val calories = intent.getIntExtra("calories", -1).takeIf { it != -1 }
                ?: intent.getIntExtra("EDIT_CALORIES", -1).takeIf { it != -1 }
            calories?.let {
                etCalories.setText(it.toString())
            }
            
            val protein = intent.getDoubleExtra("protein", -1.0).takeIf { it != -1.0 }
                ?: intent.getDoubleExtra("EDIT_PROTEIN", -1.0).takeIf { it != -1.0 }
            protein?.let {
                etProtein.setText(it.toString())
            }
            
            val carbs = intent.getDoubleExtra("carbs", -1.0).takeIf { it != -1.0 }
                ?: intent.getDoubleExtra("EDIT_CARBS", -1.0).takeIf { it != -1.0 }
            carbs?.let {
                etCarbs.setText(it.toString())
            }
            
            val fat = intent.getDoubleExtra("fat", -1.0).takeIf { it != -1.0 }
                ?: intent.getDoubleExtra("EDIT_FAT", -1.0).takeIf { it != -1.0 }
            fat?.let {
                etFat.setText(it.toString())
            }
            
            val fiber = intent.getDoubleExtra("fiber", -1.0).takeIf { it != -1.0 }
                ?: intent.getDoubleExtra("EDIT_FIBER", -1.0).takeIf { it != -1.0 }
            fiber?.let {
                etFiber.setText(it.toString())
            }
            
            val sugar = intent.getDoubleExtra("sugar", -1.0).takeIf { it != -1.0 }
                ?: intent.getDoubleExtra("EDIT_SUGAR", -1.0).takeIf { it != -1.0 }
            sugar?.let {
                etSugar.setText(it.toString())
            }
            
            val sodium = intent.getDoubleExtra("sodium", -1.0).takeIf { it != -1.0 }
                ?: intent.getDoubleExtra("EDIT_SODIUM", -1.0).takeIf { it != -1.0 }
            sodium?.let {
                etSodium.setText(it.toString())
            }
            
            // Store original nutrition data for serving size calculations
            // This enables the serving size multiplier to work with barcode scanned items
            calories?.let {
                originalNutritionData = NutritionData(
                    calories = it,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    fiber = fiber,
                    sugar = sugar,
                    sodium = sodium
                )
            }
            
            // Handle date for edit mode
            val editDate = intent.getStringExtra("EDIT_DATE")
            if (editDate != null) {
                try {
                    val date = dateFormatter.parse(editDate)
                    selectedCalendar.time = date
                    updateDateDisplay()
                } catch (e: Exception) {
                    // Use today's date if parsing fails
                }
            }
            
            // Set default serving size if not provided
            val servingSize = intent.getStringExtra("serving_size")
            if (servingSize != null) {
                etServingSize.setText(servingSize)
            } else {
                etServingSize.setText("1 serving")
            }
        }
    }
    
    private fun saveCalorieEntry() {
        val foodName = etFoodName.text?.toString()?.trim()
        val caloriesText = etCalories.text?.toString()?.trim()
        
        if (foodName.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter food name", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (caloriesText.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter calories", Toast.LENGTH_SHORT).show()
            return
        }
        
        val calories = try {
            caloriesText.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter a valid number for calories", Toast.LENGTH_SHORT).show()
            return
        }
        
        val protein = etProtein.text?.toString()?.trim()?.toDoubleOrNull()
        val carbs = etCarbs.text?.toString()?.trim()?.toDoubleOrNull()
        val fat = etFat.text?.toString()?.trim()?.toDoubleOrNull()
        val fiber = etFiber.text?.toString()?.trim()?.toDoubleOrNull()
        val sugar = etSugar.text?.toString()?.trim()?.toDoubleOrNull()
        val sodium = etSodium.text?.toString()?.trim()?.toDoubleOrNull()
        
        // Use the selected date instead of hardcoded today
        val selectedDate = dateFormatter.format(selectedCalendar.time)
        
        val entry = if (isEditMode) {
            // In edit mode, we must have a valid entry ID - if not, something went wrong
            val entryId = editingEntryId ?: run {
                Toast.makeText(this, "Error: Cannot edit entry - invalid ID", Toast.LENGTH_SHORT).show()
                return
            }
            CalorieEntry(
                id = entryId,
                foodName = foodName,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                fiber = fiber,
                sugar = sugar,
                sodium = sodium,
                date = selectedDate,
                barcode = scannedBarcode
            )
        } else {
            CalorieEntry(
                foodName = foodName,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                fiber = fiber,
                sugar = sugar,
                sodium = sodium,
                date = selectedDate,
                barcode = scannedBarcode
            )
        }
        
        lifecycleScope.launch {
            try {
                // Save or update the calorie entry
                if (isEditMode) {
                    repository.updateCalorieEntry(entry)
                    Toast.makeText(this@CalorieEntryActivity, "Food entry updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    repository.addCalorieEntry(entry)
                    Toast.makeText(this@CalorieEntryActivity, "Food entry saved successfully!", Toast.LENGTH_SHORT).show()
                }
                
                // Also save this food item locally for future offline use
                val foodItem = FoodItem(
                    barcode = scannedBarcode ?: "MANUAL_${foodName.hashCode()}_${System.currentTimeMillis()}",
                    name = foodName,
                    brand = null, // Could extract brand if needed
                    servingSize = etServingSize.text?.toString()?.trim(),
                    caloriesPerServing = calories,
                    proteinPerServing = protein,
                    carbsPerServing = carbs,
                    fatPerServing = fat,
                    fiberPerServing = fiber,
                    sugarPerServing = sugar,
                    sodiumPerServing = sodium
                )
                
                val foodSaved = repository.saveFoodItemToLocal(foodItem)
                
                val dateText = if (isToday(selectedCalendar)) "today" else displayDateFormatter.format(selectedCalendar.time)
                val message = if (foodSaved) {
                    "Entry saved for $dateText! Food saved for offline use."
                } else {
                    "Entry saved for $dateText! (Food save failed, but entry recorded)"
                }
                
                Toast.makeText(this@CalorieEntryActivity, message, Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@CalorieEntryActivity, "Error saving entry: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 🧹 CLEANUP PERFORMANCE RESOURCES
     * 
     * Called when the activity is destroyed to prevent memory leaks
     */
    override fun onDestroy() {
        super.onDestroy()
        
        // Cancel any pending searches
        searchJob?.cancel()
        
        // Clear search-specific cache to prevent memory buildup
        PerformanceOptimizer.clearCache("search_")
        
        Log.d("CalorieEntry", "🧹 Performance resources cleaned up")
    }
}