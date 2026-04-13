package com.calorietracker.notifications

// 🧰 NOTIFICATION TOOLS
import android.app.AlarmManager        // Schedule future notifications
import android.app.NotificationChannel  // Organize notifications by type
import android.app.NotificationManager  // Send notifications to user
import android.app.PendingIntent       // Actions for notification buttons
import android.content.Context         // Android system access
import android.content.Intent          // Navigate to app screens
import androidx.core.app.NotificationCompat // Build beautiful notifications
import androidx.work.*                 // Background task scheduling
import com.calorietracker.MainActivity  // Main app screen
import com.calorietracker.R            // App resources
import com.calorietracker.database.CalorieDatabase  // Access to food data
import com.calorietracker.repository.CalorieRepository // Data manager
import kotlinx.coroutines.Dispatchers   // Background thread management
import kotlinx.coroutines.withContext   // Switch between UI and background threads
import java.text.SimpleDateFormat      // Date formatting
import java.util.*                     // Date utilities
import java.util.concurrent.TimeUnit   // Time calculations

/**
 * 🔔 SMART NOTIFICATION MANAGER - INTELLIGENT MEAL REMINDERS
 * 
 * Hey young programmer! This is like having a smart personal assistant who reminds
 * you to log your meals at the right times.
 * 
 * 🧠 What makes these notifications "smart"?
 * 1. **Time-based**: Remind at typical meal times (8am, 12pm, 6pm)
 * 2. **Pattern learning**: Learn when YOU typically eat meals
 * 3. **Context aware**: "Haven't logged lunch yet, and it's 2 PM"
 * 4. **Streak protection**: Extra reminders when streak is at risk
 * 5. **Personalized**: Adjust based on user's logging habits
 * 
 * 🎯 Types of smart notifications:
 * - Meal time reminders: "Time for lunch! 🍽️"
 * - Missing meal alerts: "Haven't logged breakfast yet"
 * - Streak motivations: "Keep your 7-day streak going!"
 * - Goal achievements: "You hit your protein goal! 💪"
 * - Weekly summaries: "You logged food 6/7 days this week!"
 * 
 * ⏰ Notification Schedule:
 * - Breakfast reminder: 8:00 AM (if nothing logged by 9:30 AM)
 * - Lunch reminder: 12:00 PM (if nothing logged by 2:00 PM)  
 * - Dinner reminder: 6:00 PM (if nothing logged by 8:00 PM)
 * - Evening summary: 9:00 PM (daily progress recap)
 */
class SmartNotificationManager(private val context: Context) {
    
    // 🎯 NOTIFICATION CHANNELS - ORGANIZE BY TYPE
    companion object {
        const val CHANNEL_MEAL_REMINDERS = "meal_reminders"      // 🍽️ Meal time notifications
        const val CHANNEL_STREAK_MOTIVATION = "streak_motivation" // 🔥 Streak and achievement notifications  
        const val CHANNEL_GOAL_UPDATES = "goal_updates"         // 🎯 Goal progress notifications
        const val CHANNEL_WEEKLY_SUMMARY = "weekly_summary"     // 📊 Weekly progress reports
        
        // 🔢 NOTIFICATION IDs
        const val NOTIFICATION_BREAKFAST = 1001
        const val NOTIFICATION_LUNCH = 1002  
        const val NOTIFICATION_DINNER = 1003
        const val NOTIFICATION_STREAK = 1004
        const val NOTIFICATION_ACHIEVEMENT = 1005
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val repository = CalorieRepository(CalorieDatabase.getDatabase(context), context)
    
    /**
     * 🚀 INITIALIZE NOTIFICATION SYSTEM
     * 
     * Set up notification channels and schedule daily reminders.
     * Call this when the app starts.
     */
    fun initializeNotifications() {
        createNotificationChannels()
        scheduleDailyMealReminders()
        scheduleStreakChecks()
    }
    
    /**
     * 📢 CREATE NOTIFICATION CHANNELS
     * 
     * Android requires us to create "channels" to organize different types of notifications.
     * Think of channels like different radio stations - users can control volume for each type.
     */
    private fun createNotificationChannels() {
        // 🍽️ MEAL REMINDER CHANNEL
        val mealChannel = NotificationChannel(
            CHANNEL_MEAL_REMINDERS,
            "Meal Reminders",  
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to log your meals at appropriate times"
            setSound(null, null) // Gentle, no sound
        }
        
        // 🔥 STREAK MOTIVATION CHANNEL  
        val streakChannel = NotificationChannel(
            CHANNEL_STREAK_MOTIVATION,
            "Streak & Achievements",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Celebrate your logging streaks and achievements"
        }
        
        // 🎯 GOAL PROGRESS CHANNEL
        val goalChannel = NotificationChannel(
            CHANNEL_GOAL_UPDATES,
            "Goal Progress",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Updates on your daily nutrition goals"
        }
        
        // 📊 WEEKLY SUMMARY CHANNEL
        val summaryChannel = NotificationChannel(
            CHANNEL_WEEKLY_SUMMARY,
            "Weekly Summary",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Weekly progress reports and insights"
        }
        
        // 📱 REGISTER ALL CHANNELS WITH ANDROID
        notificationManager.createNotificationChannels(listOf(
            mealChannel, streakChannel, goalChannel, summaryChannel
        ))
    }
    
    /**
     * ⏰ SCHEDULE DAILY MEAL REMINDERS
     * 
     * Set up recurring reminders for breakfast, lunch, and dinner.
     * Uses WorkManager for reliable, battery-efficient scheduling.
     */
    private fun scheduleDailyMealReminders() {
        // 🌅 BREAKFAST REMINDER - 8:00 AM daily
        scheduleRepeatingNotification(
            workName = "breakfast_reminder",
            hour = 8,
            minute = 0,
            title = "Good morning! 🌅",
            message = "Ready to log your breakfast?"
        )
        
        // 🌞 LUNCH REMINDER - 12:00 PM daily
        scheduleRepeatingNotification(
            workName = "lunch_reminder", 
            hour = 12,
            minute = 0,
            title = "Lunch time! 🍽️",
            message = "Don't forget to track your midday meal"
        )
        
        // 🌙 DINNER REMINDER - 6:00 PM daily
        scheduleRepeatingNotification(
            workName = "dinner_reminder",
            hour = 18,
            minute = 0, 
            title = "Dinner time! 🍽️",
            message = "Time to log your evening meal"
        )
    }
    
    /**
     * 🔥 SCHEDULE STREAK PROTECTION CHECKS
     * 
     * Check if user is at risk of breaking their logging streak.
     */
    private fun scheduleStreakChecks() {
        val streakCheckWork = PeriodicWorkRequestBuilder<StreakCheckWorker>(6, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Works offline
                    .build()
            )
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "streak_check",
            ExistingPeriodicWorkPolicy.KEEP, // Don't duplicate if already scheduled
            streakCheckWork
        )
    }
    
    /**
     * ⏰ SCHEDULE A REPEATING NOTIFICATION
     * 
     * Helper function to schedule notifications at specific times daily.
     */
    private fun scheduleRepeatingNotification(
        workName: String,
        hour: Int,
        minute: Int,
        title: String,
        message: String
    ) {
        // 📅 CALCULATE INITIAL DELAY
        val now = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // If time has passed today, schedule for tomorrow
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        val initialDelayMs = targetTime.timeInMillis - now.timeInMillis
        
        // 🔔 CREATE NOTIFICATION WORK REQUEST
        val workRequest = PeriodicWorkRequestBuilder<MealReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString("title", title)
                    .putString("message", message)
                    .putInt("notificationId", workName.hashCode())
                    .build()
            )
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.REPLACE, // Update if schedule changes
            workRequest
        )
    }
    
    /**
     * 📱 SEND IMMEDIATE NOTIFICATION
     * 
     * Send a notification right now (for testing or immediate alerts).
     */
    fun sendImmediateNotification(
        title: String,
        message: String,
        channelId: String = CHANNEL_MEAL_REMINDERS,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        // 🎨 BUILD NOTIFICATION
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_restaurant) // App icon for notification
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Disappear when tapped
            .setContentIntent(createMainActivityIntent()) // Open app when tapped
            .build()
            
        // 📤 SEND TO USER
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * 🎯 CREATE INTENT TO OPEN MAIN APP
     * 
     * When user taps notification, open the main screen of our app.
     */
    private fun createMainActivityIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

/**
 * 🔔 MEAL REMINDER WORKER - BACKGROUND NOTIFICATION SENDER
 * 
 * This runs in the background to send meal reminders at scheduled times.
 */
class MealReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    /**
     * 🎯 CHECK IF REMINDER IS NEEDED
     * 
     * Before sending reminder, check if user already logged food for this meal.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repository = CalorieRepository(CalorieDatabase.getDatabase(applicationContext), applicationContext)
            val notificationManager = SmartNotificationManager(applicationContext)
            
            // 📊 GET NOTIFICATION PARAMETERS
            val title = inputData.getString("title") ?: "Meal Reminder"
            val message = inputData.getString("message") ?: "Time to log your meal!"
            val notificationId = inputData.getInt("notificationId", 0)
            
            // 🔍 CHECK IF USER ALREADY LOGGED FOOD TODAY
            val todayEntries = repository.getTodaysTotalCalories()
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            
            // 🎯 SMART REMINDER LOGIC
            val shouldSendReminder = when (currentHour) {
                in 8..10 -> todayEntries == 0  // Morning: remind if no breakfast
                in 12..14 -> todayEntries < 300 // Afternoon: remind if very low calories
                in 18..20 -> todayEntries < 800 // Evening: remind if missed lunch/dinner
                else -> false // Don't disturb during sleep hours
            }
            
            if (shouldSendReminder) {
                notificationManager.sendImmediateNotification(title, message, notificationId = notificationId)
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

/**
 * 🔥 STREAK CHECK WORKER - PROTECT USER'S LOGGING STREAK
 * 
 * Runs periodically to check if user's logging streak is at risk.
 */
class StreakCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repository = CalorieRepository(CalorieDatabase.getDatabase(applicationContext), applicationContext)
            val notificationManager = SmartNotificationManager(applicationContext)
            
            // 📊 CHECK TODAY'S LOGGING STATUS
            val todayEntries = repository.getTodaysTotalCalories()
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            
            // 🔥 STREAK AT RISK?
            if (todayEntries == 0 && currentHour >= 20) { // 8 PM and no food logged
                notificationManager.sendImmediateNotification(
                    title = "Streak Alert! 🔥",
                    message = "Don't break your logging streak! Log today's meals now.",
                    channelId = SmartNotificationManager.CHANNEL_STREAK_MOTIVATION
                )
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}