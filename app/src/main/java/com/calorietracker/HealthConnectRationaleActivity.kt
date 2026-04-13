package com.calorietracker

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.calorietracker.utils.ThemeManager

/**
 * Activity to show Health Connect permissions rationale and privacy policy
 * This is required by Health Connect for app discovery
 */
class HealthConnectRationaleActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before setting content view
        ThemeManager.applyTheme(this)
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_connect_rationale)
        
        // Set up the privacy policy content
        val tvContent = findViewById<TextView>(R.id.tvRationaleContent)
        
        // Set up close button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClose).setOnClickListener {
            finish()
        }
        
        tvContent.text = """
            CalorieTracker Health Connect Integration
            
            Privacy and Data Usage Policy
            
            CalorieTracker requests access to your Health Connect data to provide the following features:
            
            • Active Calories Burned: To automatically adjust your daily calorie goals based on your workouts and exercise activities
            
            • Total Calories Burned: To provide comprehensive calorie tracking that includes both exercise and daily activities
            
            • Exercise Sessions: To track workout types and duration for better calorie goal recommendations
            
            How We Use Your Data:
            
            1. Automatic Calorie Goal Adjustment: When you exercise, CalorieTracker reads your burned calories from Health Connect and adds bonus calories to your daily eating goal
            
            2. Workout Integration: Exercise data helps provide personalized calorie recommendations based on your activity level
            
            3. Local Storage Only: All health data is processed locally on your device and is not transmitted to external servers
            
            Data Security:
            
            • Your health data never leaves your device
            • No data is shared with third parties
            • All processing happens locally within the CalorieTracker app
            • You can revoke permissions at any time through Health Connect settings
            
            This integration is designed to help you maintain a healthy balance between calories consumed and calories burned through exercise.
        """.trimIndent()
    }
}