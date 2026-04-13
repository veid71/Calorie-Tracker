package com.calorietracker

// Standard Android imports for fragment lifecycle and UI components
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope

// App-specific imports for database operations
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.repository.CalorieRepository

// Material Design text input component
import com.google.android.material.textfield.TextInputEditText

// Kotlin coroutines for background operations
import kotlinx.coroutines.launch

/**
 * EDUCATIONAL OVERVIEW: Settings Goals Fragment
 * 
 * WHAT IS A FRAGMENT?
 * - A Fragment is a reusable UI component that represents a portion of a screen
 * - Unlike Activities (which represent entire screens), Fragments can be combined within tabs or sections
 * - This fragment handles nutrition goal settings within the larger Settings screen
 * - Fragments have their own lifecycle but depend on their parent Activity
 * 
 * WHY USE FRAGMENTS IN SETTINGS?
 * - Allows tabbed interface without creating separate Activities
 * - Better memory management - only loads when tab is active
 * - Easier navigation between different settings categories
 * - Can be reused in different contexts (tablet vs phone layouts)
 */
class SettingsGoalsFragment : Fragment() {
    
    /**
     * UI ELEMENT DECLARATIONS
     * 
     * WHAT IS lateinit?
     * - Tells Kotlin "I promise this variable will be initialized before it's used"
     * - Necessary because we can't initialize UI elements until onViewCreated() runs
     * - Alternative would be nullable types (var etCalorieGoal: TextInputEditText?)
     * 
     * WHAT IS TextInputEditText?
     * - Material Design component that provides a text input field with floating labels
     * - Better than plain EditText because it follows Material Design guidelines
     * - Automatically handles input validation styling and animations
     */
    private lateinit var etCalorieGoal: TextInputEditText      // Daily calorie target
    private lateinit var etProteinGoal: TextInputEditText     // Daily protein target (grams)
    private lateinit var etCarbsGoal: TextInputEditText       // Daily carbohydrate target (grams)
    private lateinit var etFatGoal: TextInputEditText         // Daily fat target (grams)
    private lateinit var etFiberGoal: TextInputEditText       // Daily fiber target (grams)
    private lateinit var etSugarLimit: TextInputEditText      // Daily sugar limit (grams)
    private lateinit var etSodiumLimit: TextInputEditText     // Daily sodium limit (milligrams)
    
    /**
     * DATA ACCESS LAYER
     * 
     * WHAT IS A REPOSITORY?
     * - A design pattern that abstracts data access logic
     * - Provides a clean API for UI components to interact with data
     * - Can handle multiple data sources (database, network, cache) transparently
     * - Makes testing easier by allowing mock implementations
     */
    private lateinit var repository: CalorieRepository
    
    /**
     * FRAGMENT LIFECYCLE: onCreateView()
     * 
     * WHEN IS THIS CALLED?
     * - Called when the fragment needs to create its UI for the first time
     * - Happens when the fragment becomes visible (user switches to this tab)
     * 
     * WHAT DOES IT DO?
     * - Creates the View object that represents this fragment's UI
     * - Inflates an XML layout file into actual View objects in memory
     * 
     * LAYOUT INFLATION EXPLAINED:
     * - inflater.inflate() reads XML layout files and creates View objects
     * - R.layout.fragment_settings_goals refers to res/layout/fragment_settings_goals.xml
     * - container is the parent view that will hold this fragment
     * - attachToRoot=false means "create the view but don't add it to parent yet"
     */
    override fun onCreateView(
        inflater: LayoutInflater,      // System service that converts XML to View objects
        container: ViewGroup?,         // Parent view that will contain this fragment
        savedInstanceState: Bundle?    // Previous state if fragment was recreated
    ): View? {
        // Convert XML layout file to actual View objects in memory
        return inflater.inflate(R.layout.fragment_settings_goals, container, false)
    }
    
    /**
     * FRAGMENT LIFECYCLE: onViewCreated()
     * 
     * WHEN IS THIS CALLED?
     * - Called immediately after onCreateView() returns
     * - Guaranteed that the fragment's view hierarchy has been created
     * - Safe to access UI elements with findViewById() at this point
     * 
     * WHAT SHOULD YOU DO HERE?
     * - Initialize UI components (findViewById, set up listeners)
     * - Set up data connections (database, repository)
     * - Load initial data to populate the UI
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)  // Always call parent implementation first
        
        /**
         * REPOSITORY INITIALIZATION
         * 
         * DATABASE SINGLETON PATTERN:
         * - CalorieDatabase.getDatabase() uses singleton pattern
         * - Ensures only one database instance exists across the entire app
         * - Prevents memory leaks and database corruption from multiple instances
         * 
         * CONTEXT EXPLAINED:
         * - Context provides access to system resources (database files, preferences, etc.)
         * - requireContext() gets the Context from the parent Activity
         * - Throws exception if fragment is not attached to Activity (safer than nullable)
         */
        repository = CalorieRepository(
            CalorieDatabase.getDatabase(requireContext()), // Database instance (singleton)
            requireContext()                              // System context for file access
        )
        
        // Set up UI components by connecting them to XML layout elements
        initViews(view)
        
        // Load existing nutrition goals from database to populate form fields
        loadCurrentSettings()
    }
    
    /**
     * UI BINDING: Connecting Kotlin Code to XML Layout
     * 
     * WHAT IS findViewById()?
     * - Searches through the view hierarchy to find a view with the specified ID
     * - IDs are defined in XML using android:id="@+id/viewName"
     * - Returns the actual View object so you can interact with it programmatically
     * 
     * WHY NOT USE VIEW BINDING?
     * - View Binding is a newer, type-safe alternative to findViewById()
     * - Generates binding classes automatically from XML layouts
     * - This code uses traditional findViewById() for educational clarity
     * 
     * R CLASS EXPLAINED:
     * - R is an auto-generated class that contains resource IDs
     * - R.id.etCalorieGoal corresponds to android:id="@+id/etCalorieGoal" in XML
     * - Compiler ensures typos cause build errors rather than runtime crashes
     */
    private fun initViews(view: View) {
        // Find each text input field by its ID and store reference for later use
        etCalorieGoal = view.findViewById(R.id.etCalorieGoal)    // Daily calorie target input
        etProteinGoal = view.findViewById(R.id.etProteinGoal)    // Protein goal input field
        etCarbsGoal = view.findViewById(R.id.etCarbsGoal)        // Carbohydrate goal input
        etFatGoal = view.findViewById(R.id.etFatGoal)            // Fat goal input field
        etFiberGoal = view.findViewById(R.id.etFiberGoal)        // Fiber goal input field
        etSugarLimit = view.findViewById(R.id.etSugarLimit)      // Sugar limit input field
        etSodiumLimit = view.findViewById(R.id.etSodiumLimit)    // Sodium limit input field
    }
    
    /**
     * DATA LOADING: Retrieving Existing Goals from Database
     * 
     * BACKGROUND OPERATIONS WITH COROUTINES:
     * - Database operations can be slow and would freeze the UI if run on main thread
     * - lifecycleScope.launch creates a coroutine tied to this fragment's lifecycle
     * - Coroutine automatically cancels if fragment is destroyed (prevents memory leaks)
     * 
     * WHAT IS A COROUTINE?
     * - Lightweight thread that can be paused and resumed
     * - Allows writing asynchronous code that looks synchronous
     * - Much more efficient than creating actual threads
     * 
     * ERROR HANDLING BEST PRACTICES:
     * - Always wrap database operations in try-catch blocks
     * - Show user-friendly error messages via Toast
     * - Never let the app crash due to database errors
     */
    private fun loadCurrentSettings() {
        // Launch background operation tied to fragment lifecycle
        lifecycleScope.launch {
            try {
                // Retrieve nutrition goals from database (runs on background thread)
                val nutritionGoals = repository.getNutritionGoalsSync()
                
                // Check if goals exist before trying to populate UI
                if (nutritionGoals != null) {
                    // Populate form fields with existing values
                    // toString() converts numbers to text for display in EditText
                    etCalorieGoal.setText(nutritionGoals.calorieGoal.toString())   // Int to String
                    etProteinGoal.setText(nutritionGoals.proteinGoal.toString())   // Double to String
                    etCarbsGoal.setText(nutritionGoals.carbsGoal.toString())       // Double to String
                    etFatGoal.setText(nutritionGoals.fatGoal.toString())           // Double to String
                    etFiberGoal.setText(nutritionGoals.fiberGoal.toString())       // Double to String
                    etSugarLimit.setText(nutritionGoals.sugarGoal.toString())      // Double to String
                    etSodiumLimit.setText(nutritionGoals.sodiumGoal.toString())    // Double to String
                }
                // If nutritionGoals is null, form fields remain empty (default state)
            } catch (e: Exception) {
                // Show user-friendly error message if database operation fails
                // Toast.LENGTH_SHORT displays message for ~2 seconds
                Toast.makeText(requireContext(), "Error loading nutrition goals: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * DATA PERSISTENCE: Saving User Input to Database
     * 
     * PUBLIC FUNCTION CALLED BY PARENT:
     * - This function is called by the parent SettingsActivity when user saves
     * - Public visibility allows external components to trigger save operation
     * 
     * INPUT VALIDATION AND SANITIZATION:
     * - User input from EditText fields comes as String, needs conversion to numbers
     * - toIntOrNull() safely converts text to integer (returns null if invalid)
     * - Elvis operator (?:) provides default values if conversion fails
     * - This prevents crashes from invalid input like "abc" or empty fields
     * 
     * DATA CLASS COPY PATTERN:
     * - Kotlin data classes provide copy() method for creating modified copies
     * - Immutable pattern: don't modify existing object, create new one with changes
     * - Safer than modifying existing objects (prevents accidental side effects)
     */
    fun saveSettings() {
        // Save operation runs in background to avoid blocking UI
        lifecycleScope.launch {
            try {
                /**
                 * INPUT SANITIZATION WITH SAFE DEFAULTS
                 * 
                 * PARSING STRATEGY:
                 * - getText().toString() gets current text from EditText
                 * - toIntOrNull()/toDoubleOrNull() safely convert to numbers
                 * - ?: provides fallback values if parsing fails (empty field, invalid text)
                 * 
                 * DEFAULT VALUES RATIONALE:
                 * - Based on standard dietary guidelines (FDA recommended values)
                 * - Prevents app crashes from empty or invalid input
                 * - Gives users reasonable starting points
                 */
                val calorieGoal = etCalorieGoal.text.toString().toIntOrNull() ?: 2000      // Daily calories
                val proteinGoal = etProteinGoal.text.toString().toDoubleOrNull() ?: 50.0   // Grams protein
                val carbsGoal = etCarbsGoal.text.toString().toDoubleOrNull() ?: 250.0      // Grams carbs
                val fatGoal = etFatGoal.text.toString().toDoubleOrNull() ?: 65.0           // Grams fat
                val fiberGoal = etFiberGoal.text.toString().toDoubleOrNull() ?: 25.0       // Grams fiber
                val sugarGoal = etSugarLimit.text.toString().toDoubleOrNull() ?: 50.0      // Grams sugar limit
                val sodiumGoal = etSodiumLimit.text.toString().toDoubleOrNull() ?: 2300.0  // Mg sodium limit
                
                /**
                 * DATABASE UPDATE PATTERN
                 * 
                 * WHY GET CURRENT GOALS FIRST?
                 * - Need existing database record to update (can't create from scratch)
                 * - Preserves other fields that aren't shown in this form
                 * - Ensures we're updating existing record, not creating duplicate
                 */
                val currentGoals = repository.getNutritionGoalsSync()
                if (currentGoals != null) {
                    // Create updated copy with new values (immutable update pattern)
                    val updatedGoals = currentGoals.copy(
                        calorieGoal = calorieGoal,
                        proteinGoal = proteinGoal,
                        carbsGoal = carbsGoal,
                        fatGoal = fatGoal,
                        fiberGoal = fiberGoal,
                        sugarGoal = sugarGoal,
                        sodiumGoal = sodiumGoal
                    )
                    
                    // Persist updated goals to database
                    repository.updateNutritionGoals(updatedGoals)
                    
                    // Confirm success to user
                    Toast.makeText(requireContext(), "Nutrition goals saved successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    // Handle case where no existing goals record exists
                    Toast.makeText(requireContext(), "Error: No existing nutrition goals found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Catch any unexpected errors during save process
                Toast.makeText(requireContext(), "Error saving nutrition goals: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}