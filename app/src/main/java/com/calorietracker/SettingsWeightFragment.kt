package com.calorietracker

// Android framework imports
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast

// Text input change detection
import android.text.Editable
import android.text.TextWatcher

// Spinner selection handling
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.TextView

// Fragment and coroutines
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope

// App-specific imports
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.database.WeightGoal
import com.calorietracker.repository.CalorieRepository
import com.calorietracker.utils.CalorieCalculator

// Material Design components
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * EDUCATIONAL OVERVIEW: Settings Weight Fragment
 * 
 * COMPLEX UI INTERACTIONS:
 * - Real-time calorie calculations as user types
 * - Multiple input fields with interdependent validation
 * - Dynamic UI updates based on form completion
 * - TextWatcher for live input monitoring
 * - Spinner selection listeners for dropdown changes
 * 
 * ADVANCED CALCULATION FEATURES:
 * - BMR (Basal Metabolic Rate) calculations using Mifflin-St Jeor equation
 * - TDEE (Total Daily Energy Expenditure) based on activity level
 * - Weight change rate calculations (safe weight loss/gain recommendations)
 * - Health safety warnings for extreme goals
 * - Real-time metric card updates
 * 
 * UI/UX PATTERNS DEMONSTRATED:
 * - Progressive disclosure (show calculations as form fills)
 * - Interactive cards with educational explanations
 * - Real-time validation with immediate feedback
 * - Complex form state management
 * - Multiple data sources feeding single UI
 * 
 * EVENT HANDLING COMPLEXITY:
 * - TextWatcher for real-time input changes
 * - Spinner OnItemSelectedListener for dropdown changes
 * - Button click handlers for actions
 * - Card click handlers for educational dialogs
 * - Coordinated UI updates from multiple event sources
 */
class SettingsWeightFragment : Fragment() {
    
    // UI elements for weight goals
    private lateinit var etCurrentWeight: TextInputEditText
    private lateinit var etTargetWeight: TextInputEditText
    private lateinit var etTimelineDays: TextInputEditText
    private lateinit var etAge: TextInputEditText
    private lateinit var etHeight: TextInputEditText
    private lateinit var spinnerActivityLevel: Spinner
    private lateinit var spinnerGender: Spinner
    private lateinit var btnCalculateCalories: MaterialButton
    private lateinit var btnSaveWeightGoal: MaterialButton
    
    // Metrics display UI elements
    private lateinit var layoutMetricsCards: LinearLayout
    private lateinit var tvRecommendedCaloriesValue: TextView
    private lateinit var tvBMRValue: TextView
    private lateinit var tvTDEEValue: TextView
    private lateinit var tvWeightChangeRateValue: TextView
    private lateinit var tvHealthStatus: TextView
    private lateinit var cardRecommendedCalories: MaterialCardView
    private lateinit var cardBMR: MaterialCardView
    private lateinit var cardTDEE: MaterialCardView
    private lateinit var cardWeightChangeRate: MaterialCardView
    private lateinit var cardHealthStatus: MaterialCardView
    
    // Repository for data operations
    private lateinit var repository: CalorieRepository
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings_weight, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize repository
        repository = CalorieRepository(CalorieDatabase.getDatabase(requireContext()), requireContext())
        
        initViews(view)
        setupSpinners()
        setupButtons()
        setupRealTimeCalculations()
        loadCurrentSettings()
        updateMetricsDisplay()
    }
    
    private fun initViews(view: View) {
        etCurrentWeight = view.findViewById(R.id.etCurrentWeight)
        etTargetWeight = view.findViewById(R.id.etTargetWeight)
        etTimelineDays = view.findViewById(R.id.etTimelineDays)
        etAge = view.findViewById(R.id.etAge)
        etHeight = view.findViewById(R.id.etHeight)
        spinnerActivityLevel = view.findViewById(R.id.spinnerActivityLevel)
        spinnerGender = view.findViewById(R.id.spinnerGender)
        btnCalculateCalories = view.findViewById(R.id.btnCalculateCalories)
        btnSaveWeightGoal = view.findViewById(R.id.btnSaveWeightGoal)
        
        // Initialize metrics display elements
        layoutMetricsCards = view.findViewById(R.id.layoutMetricsCards)
        tvRecommendedCaloriesValue = view.findViewById(R.id.tvRecommendedCaloriesValue)
        tvBMRValue = view.findViewById(R.id.tvBMRValue)
        tvTDEEValue = view.findViewById(R.id.tvTDEEValue)
        tvWeightChangeRateValue = view.findViewById(R.id.tvWeightChangeRateValue)
        tvHealthStatus = view.findViewById(R.id.tvHealthStatus)
        cardRecommendedCalories = view.findViewById(R.id.cardRecommendedCalories)
        cardBMR = view.findViewById(R.id.cardBMR)
        cardTDEE = view.findViewById(R.id.cardTDEE)
        cardWeightChangeRate = view.findViewById(R.id.cardWeightChangeRate)
        cardHealthStatus = view.findViewById(R.id.cardHealthStatus)
    }
    
    private fun setupSpinners() {
        // Activity level spinner
        val activityLevels = CalorieCalculator.getActivityLevels()
        val activityNames = activityLevels.map { it.second }
        val activityAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, activityNames)
        activityAdapter.setDropDownViewResource(R.layout.dropdown_item)
        spinnerActivityLevel.adapter = activityAdapter
        
        // Gender spinner
        val genders = listOf("Not specified", "Male", "Female")
        val genderAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, genders)
        genderAdapter.setDropDownViewResource(R.layout.dropdown_item)
        spinnerGender.adapter = genderAdapter
    }
    
    private fun setupButtons() {
        btnCalculateCalories.setOnClickListener {
            calculateCalories()
        }
        
        btnSaveWeightGoal.setOnClickListener {
            saveWeightGoal()
        }
    }
    
    /**
     * REAL-TIME UI UPDATES: Setting up Live Calculations
     * 
     * TEXTWATCHER PATTERN:
     * - TextWatcher interface monitors EditText changes in real-time
     * - Three callback methods: beforeTextChanged, onTextChanged, afterTextChanged
     * - Only afterTextChanged is needed here (after user finishes typing)
     * - Object expression creates anonymous implementation
     * 
     * SHARED TEXTWATCHER INSTANCE:
     * - Single TextWatcher object applied to multiple EditText fields
     * - More efficient than creating separate watchers for each field
     * - All fields trigger the same updateMetricsDisplay() method
     * - Demonstrates code reuse and DRY (Don't Repeat Yourself) principle
     * 
     * SPINNER EVENT HANDLING:
     * - OnItemSelectedListener handles dropdown selection changes
     * - onItemSelected() called when user picks different option
     * - onNothingSelected() called when selection is cleared (rarely used)
     * - Object expressions create anonymous listener implementations
     * 
     * EDUCATIONAL INTERACTION DESIGN:
     * - Cards are clickable to show explanatory dialogs
     * - Helps users understand what BMR, TDEE, etc. mean
     * - Interactive learning while using the app
     * - Click listeners use lambda syntax for conciseness
     */
    private fun setupRealTimeCalculations() {
        /**
         * SHARED TEXTWATCHER FOR EFFICIENT MONITORING
         * 
         * TEXTWATCHER INTERFACE METHODS:
         * - beforeTextChanged(): Called before text changes (rarely needed)
         * - onTextChanged(): Called during text changes (can be called frequently)
         * - afterTextChanged(): Called after text changes complete (best for calculations)
         * 
         * OBJECT EXPRESSION SYNTAX:
         * - object : TextWatcher creates anonymous implementation
         * - Only override methods we actually need
         * - afterTextChanged() triggers metric recalculation
         */
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateMetricsDisplay() // Recalculate and update UI when text changes
            }
        }
        
        /**
         * APPLY TEXTWATCHER TO MULTIPLE FIELDS
         * 
         * EFFICIENCY PATTERN:
         * - Same TextWatcher instance shared across multiple EditText fields
         * - Each field change triggers same calculation update
         * - Avoids code duplication and reduces memory usage
         * - All relevant fields participate in real-time updates
         */
        etCurrentWeight.addTextChangedListener(textWatcher)
        etTargetWeight.addTextChangedListener(textWatcher)
        etTimelineDays.addTextChangedListener(textWatcher)
        etAge.addTextChangedListener(textWatcher)
        etHeight.addTextChangedListener(textWatcher)
        
        /**
         * SPINNER SELECTION LISTENERS
         * 
         * ONITEMSELECTEDLISTENER INTERFACE:
         * - onItemSelected(): Called when user selects dropdown item
         * - onNothingSelected(): Called when selection cleared (uncommon)
         * - Parameters provide context about selection (parent, view, position, id)
         * - Anonymous object expressions avoid creating separate listener classes
         */
        spinnerActivityLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateMetricsDisplay() // Recalculate when activity level changes
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        spinnerGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateMetricsDisplay() // Recalculate when gender selection changes
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        /**
         * EDUCATIONAL CARD INTERACTIONS
         * 
         * INTERACTIVE LEARNING DESIGN:
         * - Cards are clickable to provide educational content
         * - Lambda syntax for concise click listener creation
         * - Each card explains a different health/fitness concept
         * - Helps users understand the science behind calculations
         */
        cardRecommendedCalories.setOnClickListener { showCalorieExplanation() }
        cardBMR.setOnClickListener { showBMRExplanation() }
        cardTDEE.setOnClickListener { showTDEEExplanation() }
        cardWeightChangeRate.setOnClickListener { showWeightChangeExplanation() }
    }
    
    private fun loadCurrentSettings() {
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
                }
            } catch (e: Exception) {
                // Weight goal doesn't exist yet, which is fine
            }
        }
    }
    
    private fun updateMetricsDisplay() {
        try {
            val currentWeight = etCurrentWeight.text.toString().toDoubleOrNull()
            val targetWeight = etTargetWeight.text.toString().toDoubleOrNull()
            val timelineDays = etTimelineDays.text.toString().toIntOrNull()
            
            // If basic inputs aren't filled, hide metrics
            if (currentWeight == null || targetWeight == null || timelineDays == null || timelineDays <= 0) {
                resetMetricsDisplay()
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
            val recommendation = CalorieCalculator.calculateCalorieRecommendation(weightGoal, false)
            
            // Update UI with calculated values
            tvRecommendedCaloriesValue.text = "${recommendation.recommendedCalories} cal"
            tvBMRValue.text = "${recommendation.bmr} cal"
            tvTDEEValue.text = "${recommendation.tdee} cal"
            
            val weightChangeText = if (recommendation.weeklyWeightChange >= 0) {
                "+${String.format("%.1f", recommendation.weeklyWeightChange)} lbs/week"
            } else {
                "${String.format("%.1f", recommendation.weeklyWeightChange)} lbs/week"
            }
            tvWeightChangeRateValue.text = weightChangeText
            
            // Update health status
            if (!recommendation.isHealthy && recommendation.healthWarning != null) {
                tvHealthStatus.text = "⚠️ ${recommendation.healthWarning}"
                tvHealthStatus.setTextColor(requireContext().getColor(R.color.accent_orange))
            } else {
                tvHealthStatus.text = "✅ This goal is within healthy guidelines"
                tvHealthStatus.setTextColor(requireContext().getColor(R.color.success_green))
            }
            
        } catch (e: Exception) {
            resetMetricsDisplay()
        }
    }
    
    private fun resetMetricsDisplay() {
        tvRecommendedCaloriesValue.text = "Enter details above"
        tvBMRValue.text = "Enter details above"
        tvTDEEValue.text = "Enter details above"
        tvWeightChangeRateValue.text = "Enter details above"
        tvHealthStatus.text = "💡 Fill in the form above to see your personalized recommendations"
        tvHealthStatus.setTextColor(requireContext().getColor(R.color.black))
    }
    
    private fun showCalorieExplanation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("🎯 Daily Calorie Goal")
            .setMessage("Your recommended daily calorie intake to reach your weight goal.\n\n" +
                    "This is calculated by taking your TDEE (Total Daily Energy Expenditure) " +
                    "and adjusting it based on your weight goal timeline.\n\n" +
                    "• To lose weight: Eat below your TDEE\n" +
                    "• To gain weight: Eat above your TDEE\n" +
                    "• To maintain: Eat at your TDEE")
            .setPositiveButton("Got it!") { _, _ -> }
            .show()
    }
    
    private fun showBMRExplanation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("📊 BMR (Basal Metabolic Rate)")
            .setMessage("The number of calories your body burns at complete rest.\n\n" +
                    "This is the energy needed for basic functions like:\n" +
                    "• Breathing and circulation\n" +
                    "• Cell production and repair\n" +
                    "• Brain and organ function\n\n" +
                    "BMR typically accounts for 60-75% of your daily calorie burn.")
            .setPositiveButton("Got it!") { _, _ -> }
            .show()
    }
    
    private fun showTDEEExplanation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("🔥 TDEE (Total Daily Energy Expenditure)")
            .setMessage("Your BMR multiplied by your activity level.\n\n" +
                    "This represents the total calories you burn in a day including:\n" +
                    "• BMR (resting metabolism)\n" +
                    "• Physical activity\n" +
                    "• Thermic effect of food\n\n" +
                    "TDEE = BMR × Activity Level Factor")
            .setPositiveButton("Got it!") { _, _ -> }
            .show()
    }
    
    private fun showWeightChangeExplanation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("📈 Weekly Weight Change Rate")
            .setMessage("How much weight you'll gain or lose per week with this calorie goal.\n\n" +
                    "Based on the principle that:\n" +
                    "• 1 pound = ~3,500 calories\n" +
                    "• Safe weight loss: 0.5-2 lbs/week\n" +
                    "• Safe weight gain: 0.5-1 lb/week\n\n" +
                    "A 500 calorie daily deficit = 1 lb/week weight loss")
            .setPositiveButton("Got it!") { _, _ -> }
            .show()
    }
    
    private fun calculateCalories() {
        try {
            val currentWeight = etCurrentWeight.text.toString().toDoubleOrNull()
            val targetWeight = etTargetWeight.text.toString().toDoubleOrNull()
            val timelineDays = etTimelineDays.text.toString().toIntOrNull()
            
            if (currentWeight == null || targetWeight == null || timelineDays == null) {
                Toast.makeText(requireContext(), "Please fill in current weight, target weight, and timeline", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (timelineDays <= 0) {
                Toast.makeText(requireContext(), "Timeline must be greater than 0 days", Toast.LENGTH_SHORT).show()
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
            val recommendation = CalorieCalculator.calculateCalorieRecommendation(weightGoal, false)
            
            // Show detailed recommendation with explanations
            val message = buildString {
                append("📊 CALORIE RECOMMENDATION\n\n")
                
                append("🎯 Daily Calorie Goal: ${recommendation.recommendedCalories} calories\n\n")
                
                append("📈 Your Metabolic Profile:\n")
                append("• BMR (Basal Metabolic Rate): ${recommendation.bmr} cal\n")
                append("  (Calories burned at rest)\n")
                append("• TDEE (Total Daily Energy): ${recommendation.tdee} cal\n")
                append("  (BMR × Activity Level)\n\n")
                
                append("⚖️ Weight Change Plan:\n")
                if (recommendation.weeklyWeightChange > 0) {
                    append("• Target: Gain ${String.format("%.1f", recommendation.weeklyWeightChange)} lbs/week\n")
                    append("• Method: Eat ${recommendation.tdee - recommendation.recommendedCalories} calories above TDEE\n")
                } else if (recommendation.weeklyWeightChange < 0) {
                    append("• Target: Lose ${String.format("%.1f", -recommendation.weeklyWeightChange)} lbs/week\n")
                    append("• Method: Eat ${recommendation.tdee - recommendation.recommendedCalories} calories below TDEE\n")
                } else {
                    append("• Target: Maintain current weight\n")
                    append("• Method: Eat at your TDEE level\n")
                }
                
                append("\n🏥 Health Assessment:\n")
                if (!recommendation.isHealthy && recommendation.healthWarning != null) {
                    append("⚠️ ${recommendation.healthWarning}\n")
                } else {
                    append("✅ This goal is within healthy guidelines\n")
                }
                
                append("\n💡 How it works:\n")
                append("• 1 pound = ~3,500 calories\n")
                append("• Safe weight loss: 0.5-2 lbs/week\n")
                append("• Safe weight gain: 0.5-1 lb/week")
            }
            
            // Use AlertDialog for better display of detailed information
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("🎯 Your Calorie Plan")
                .setMessage(message)
                .setPositiveButton("Got it!") { _, _ -> }
                .show()
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error calculating recommendation: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveWeightGoal() {
        lifecycleScope.launch {
            try {
                val currentWeight = etCurrentWeight.text.toString().toDoubleOrNull()
                val targetWeight = etTargetWeight.text.toString().toDoubleOrNull()
                val timelineDays = etTimelineDays.text.toString().toIntOrNull()
                
                if (currentWeight == null || targetWeight == null || timelineDays == null) {
                    Toast.makeText(requireContext(), "Please fill in current weight, target weight, and timeline", Toast.LENGTH_SHORT).show()
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
                
                // Get recommendation and update main calorie goal
                val recommendation = repository.getCalorieRecommendationFromWeightGoal()
                if (recommendation != null) {
                    // Get current nutrition goals and update calorie goal
                    val currentGoals = repository.getNutritionGoalsSync()
                    if (currentGoals != null) {
                        val updatedGoals = currentGoals.copy(
                            calorieGoal = recommendation.recommendedCalories
                        )
                        repository.updateNutritionGoals(updatedGoals)
                        Toast.makeText(requireContext(), "Weight goal saved and calorie goal updated to ${recommendation.recommendedCalories} calories!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "Weight goal saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Weight goal saved successfully!", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error saving weight goal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun getCurrentDateString(): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return formatter.format(java.util.Date())
    }
    
    fun saveSettings() {
        saveWeightGoal()
    }
}