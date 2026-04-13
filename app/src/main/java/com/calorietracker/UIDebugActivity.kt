package com.calorietracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.calorietracker.BuildConfig
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.repository.CalorieRepository
import kotlinx.coroutines.launch

/**
 * Debug activity to test UI elements and data flow
 */
class UIDebugActivity : AppCompatActivity() {
    
    private lateinit var repository: CalorieRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create simple debug layout
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Repository setup
        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)
        
        // Title
        val title = TextView(this).apply {
            text = "🔧 UI Debug Console"
            textSize = 20f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(title)
        
        // Version info
        val versionInfo = TextView(this).apply {
            text = "CalorieTracker v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})\nSeptember 6, 2025 - Dynamic Version Display"
            textSize = 12f
            setTypeface(android.graphics.Typeface.MONOSPACE)
            setPadding(0, 0, 0, 24)
            alpha = 0.8f
        }
        layout.addView(versionInfo)
        
        // Debug info display
        val debugInfo = TextView(this).apply {
            text = "Loading debug info..."
            textSize = 14f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(debugInfo)
        
        // Test workout data button
        val btnTestWorkout = Button(this).apply {
            text = "🏃 Test Workout Data"
            setOnClickListener {
                lifecycleScope.launch {
                    try {
                        val summary = repository.getTodaysSummary()
                        val workoutData = repository.getTodaysWorkoutCalories()
                        
                        val debugText = """
                            WORKOUT DATA DEBUG:
                            
                            📊 Daily Summary:
                            - Base Goal: ${summary.baseCalorieGoal}
                            - Workout Calories: ${summary.workoutCaloriesBurned}  
                            - Adjusted Goal: ${summary.adjustedCalorieGoal}
                            - Consumed: ${summary.consumedCalories}
                            - Remaining: ${summary.remainingCalories}
                            
                            🏃 Workout Details:
                            ${if (workoutData != null) {
                                "- Active Calories: ${workoutData.activeCaloriesBurned}\n" +
                                "- Exercise Minutes: ${workoutData.exerciseMinutes}\n" +
                                "- Exercise Type: ${workoutData.exerciseType}\n" +
                                "- Date: ${workoutData.date}"
                            } else {
                                "- No workout data found for today"
                            }}
                        """.trimIndent()
                        
                        debugInfo.text = debugText
                        Toast.makeText(this@UIDebugActivity, "Workout test completed", Toast.LENGTH_SHORT).show()
                        
                    } catch (e: Exception) {
                        debugInfo.text = "❌ Error: ${e.message}\n${e.stackTraceToString()}"
                    }
                }
            }
        }
        layout.addView(btnTestWorkout)
        
        // Test dropdown functionality
        val dropdownLabel = TextView(this).apply {
            text = "🔍 Test Dropdown:"
            textSize = 16f
            setPadding(0, 32, 0, 8)
        }
        layout.addView(dropdownLabel)
        
        val testAutoComplete = AutoCompleteTextView(this).apply {
            hint = "Type to test dropdown"
            
            val testItems = arrayOf(
                "Chicken Breast - Grilled", 
                "Chicken Thigh - Baked",
                "Chicken Wings - Buffalo",
                "Chicken Salad - Caesar",
                "Chicken Soup - Noodle"
            )
            
            val adapter = ArrayAdapter(this@UIDebugActivity, android.R.layout.simple_dropdown_item_1line, testItems)
            setAdapter(adapter)
            threshold = 1
            
            setOnItemClickListener { _, _, position, _ ->
                Toast.makeText(this@UIDebugActivity, "Selected: ${testItems[position]}", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(testAutoComplete)
        
        // Test dropdown button
        val btnTestDropdown = Button(this).apply {
            text = "🎯 Force Show Test Dropdown"
            setOnClickListener {
                testAutoComplete.requestFocus()
                testAutoComplete.showDropDown()
                Toast.makeText(this@UIDebugActivity, "Dropdown should appear now", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(btnTestDropdown)
        
        // Manual main screen update test
        val btnUpdateMainScreen = Button(this).apply {
            text = "📱 Test Main Screen Update"
            setOnClickListener {
                // Simulate what MainActivity does
                lifecycleScope.launch {
                    try {
                        repository.syncTodaysWorkoutData()
                        val summary = repository.getTodaysSummary()
                        
                        val resultText = """
                            MAIN SCREEN TEST:
                            
                            What should appear on main screen:
                            - Calories consumed: ${summary.consumedCalories}
                            - Daily goal: ${summary.adjustedCalorieGoal}
                            - Workout bonus visible: ${summary.workoutCaloriesBurned > 0}
                            - Bonus text: Base ${summary.baseCalorieGoal} + ${summary.adjustedCalorieGoal - summary.baseCalorieGoal} workout bonus
                        """.trimIndent()
                        
                        debugInfo.text = resultText
                        
                    } catch (e: Exception) {
                        debugInfo.text = "Main screen test failed: ${e.message}"
                    }
                }
            }
        }
        layout.addView(btnUpdateMainScreen)
        
        setContentView(ScrollView(this).apply { addView(layout) })
    }
}