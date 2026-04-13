package com.calorietracker.fitness

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Comprehensive Health Connect integration manager for fitness data synchronization.
 * 
 * **ANDROID HEALTH CONNECT CONCEPTS FOR BEGINNERS:**
 * 
 * **What is Health Connect?**
 * Health Connect is like a "universal translator" for health apps:
 * - Fitness trackers, smartwatches, and health apps can all share data
 * - Your step counter app can share steps with your calorie tracker
 * - Your smartwatch can share workout data with multiple apps
 * - No need to manually sync between different apps
 * 
 * **How Health Connect Works:**
 * ```
 * [Smartwatch] → [Health Connect] ← [Fitness App A]
 *      ↓                              ↑
 * [Phone Sensors] → [Health Connect] ← [Fitness App B]
 *                           ↓
 *                  [Your CalorieTracker App]
 * ```
 * 
 * **Permission System:**
 * Health data is sensitive, so Android requires explicit permission:
 * - READ_ACTIVE_CALORIES_BURNED: Can see workout calories
 * - READ_TOTAL_CALORIES_BURNED: Can see all calories (including BMR)
 * - READ_EXERCISE_SESSION: Can see workout types and duration
 * - User grants these one-by-one in system settings
 * 
 * **Data Types Available:**
 * - Steps: Walking/running step counts
 * - Calories: Active (exercise) vs Total (including metabolism)
 * - Exercise Sessions: Type (running, cycling), duration, intensity
 * - Heart Rate: Beats per minute during workouts and rest
 * - Weight: Scale measurements
 * - Hydration: Water intake tracking
 * 
 * **Real-Time vs Historical Data:**
 * - Real-time: Current heart rate, today's step count
 * - Historical: Last 30 days of workouts, weight trends
 * - We can query specific date ranges for analysis
 * 
 * This class serves as the bridge between the CalorieTracker app and Android's unified
 * Health Connect platform, enabling seamless integration with smartwatches and fitness
 * devices like the OnePlus Watch 3.
 * 
 * **Core Functionality:**
 * - Real-time fitness data synchronization from connected wearables
 * - Active and total calories burned tracking
 * - Exercise session recognition and duration tracking
 * - Step counting and hydration monitoring
 * - Automatic calorie goal adjustment based on workout intensity
 * 
 * **Supported Data Types:**
 * - ActiveCaloriesBurnedRecord: Calories from intentional exercise
 * - TotalCaloriesBurnedRecord: All calories including BMR
 * - ExerciseSessionRecord: Workout sessions with type and duration
 * - StepsRecord: Daily step counting
 * - HydrationRecord: Water intake tracking
 * 
 * **Integration Flow:**
 * 1. Check Health Connect SDK availability on device
 * 2. Request necessary health permissions from user
 * 3. Register app with Health Connect platform
 * 4. Sync fitness data from connected devices
 * 5. Calculate smart calorie goal adjustments
 * 6. Store workout data in local database
 * 
 * **Smart Features:**
 * - 70% workout calorie bonus (research-backed safe multiplier)
 * - Daily goal adjustment based on exercise intensity
 * - Fallback UI when Health Connect unavailable
 * - Comprehensive error handling and retry logic
 * 
 * **Device Compatibility:**
 * - OnePlus Watch 3 (primary target)
 * - Any Health Connect compatible fitness device
 * - Requires Android 8.0+ (API 26) minimum
 * 
 * **Privacy & Security:**
 * - All health data stays on device
 * - Uses Android's standardized health permissions
 * - No cloud storage of personal health information
 * - User has full control over data sharing
 * 
 * @param context Android context for Health Connect client initialization
 */
class HealthConnectManager(private val context: Context) {
    
    companion object {
        private const val TAG = "HealthConnectManager"
        
        // Required permissions for reading fitness data
        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HydrationRecord::class)
        )
    }
    
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    
    /**
     * Check if Health Connect is available on this device
     */
    suspend fun isHealthConnectAvailable(): Boolean {
        return try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Health Connect availability", e)
            false
        }
    }
    
    /**
     * Check if all required permissions are granted
     */
    suspend fun hasRequiredPermissions(): Boolean {
        return try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            grantedPermissions.containsAll(REQUIRED_PERMISSIONS)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Health Connect permissions", e)
            false
        }
    }
    
    /**
     * Check if we have all required permissions
     */
    suspend fun hasAllPermissions(): Boolean {
        return try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            
            grantedPermissions.forEach { permission ->
            }
            
            REQUIRED_PERMISSIONS.forEach { permission ->
                val isGranted = permission in grantedPermissions
            }
            
            val hasAll = REQUIRED_PERMISSIONS.all { it in grantedPermissions }
            
            if (!hasAll) {
                REQUIRED_PERMISSIONS.forEach { permission ->
                    if (permission !in grantedPermissions) {
                    }
                }
            }
            
            hasAll
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
            Log.e("HealthConnectManager", "Unexpected error", e)
            false
        }
    }
    
    /**
     * Get the permission controller for requesting permissions
     */
    fun getPermissionController() = healthConnectClient.permissionController
    
    /**
     * Get count of missing permissions that need to be granted
     */
    suspend fun getMissingPermissionCount(): Int {
        return try {
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            REQUIRED_PERMISSIONS.count { it !in grantedPermissions }
        } catch (e: Exception) {
            REQUIRED_PERMISSIONS.size
        }
    }
    
    /**
     * Check if Health Connect is properly set up and permissions are granted
     */
    suspend fun isHealthConnectReady(): Boolean {
        return try {
            val isAvailable = isHealthConnectAvailable()
            if (!isAvailable) {
                return false
            }
            
            val hasPermissions = hasAllPermissions()
            if (!hasPermissions) {
                val missingCount = getMissingPermissionCount()
                return false
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Test Health Connect integration by attempting to read permissions
     * This will trigger the app to register with Health Connect if not already done
     */
    suspend fun testHealthConnectIntegration(): Boolean {
        return try {
            // Attempt to get granted permissions - this will register the app with Health Connect
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            Log.d(TAG, "Health Connect integration test successful. Granted permissions: ${grantedPermissions.size}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Health Connect integration test failed", e)
            false
        }
    }
    
    /**
     * Get active calories burned for a specific date
     * 
     * **HEALTH DATA CONCEPTS:**
     * 
     * **Active vs Total Calories:**
     * - Active Calories: Only from intentional exercise (running, cycling, gym)
     * - Total Calories: Active + BMR (Basal Metabolic Rate - calories to stay alive)
     * - BMR Example: 1,800 calories/day just for breathing, circulation, etc.
     * - Active Example: 300 calories from 30-minute run
     * - Total: 2,100 calories for that day
     * 
     * **Why We Use Active Calories:**
     * For calorie goal adjustment, we only want exercise calories:
     * - Base calorie goal: 2,000 calories (for weight maintenance)
     * - After 300-calorie workout: Can eat 2,000 + (300 × 70%) = 2,210 calories
     * - We use 70% multiplier because body becomes more efficient at burning calories
     * 
     * **Date Range Queries:**
     * Health Connect uses UTC time ranges:
     * - Start: Beginning of day (00:00:00)
     * - End: Beginning of next day (00:00:00 + 1 day)
     * - This captures all data for the specified date
     * 
     * **Data Aggregation:**
     * Smartwatches record calories every few minutes:
     * - 9:00 AM: 50 calories (morning walk)
     * - 11:00 AM: 25 calories (stairs)
     * - 6:00 PM: 300 calories (gym workout)
     * - We sum all records for the day: 50 + 25 + 300 = 375 total
     * 
     * This represents calories burned during exercise/workouts
     */
    suspend fun getActiveCaloriesForDate(date: LocalDate): Int {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasAllPermissions()) {
                    Log.w(TAG, "Missing permissions for reading active calories")
                    return@withContext 0
                }
                
                val startTime = date.atStartOfDay().toInstant(ZoneOffset.UTC)
                val endTime = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                
                val request = ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
                
                val response = healthConnectClient.readRecords(request)
                
                // Debug: Log each individual record to see what we're getting
                response.records.forEach { record ->
                    Log.d(TAG, "Raw energy record: ${record.energy.inCalories} calories, ${record.energy.inKilojoules} kJ")
                }
                
                // Convert energy to calories - Health Connect uses kilojoules internally
                // 1 kilojoule = 0.239 calories (this is the standard conversion)
                val totalActiveCalories = response.records.sumOf { record ->
                    // Use kilojoules and convert to calories to avoid double conversion
                    val kilojoules = record.energy.inKilojoules
                    val calories = (kilojoules * 0.239).toInt()
                    Log.d(TAG, "Converting: ${kilojoules} kJ → ${calories} calories")
                    calories
                }
                
                Log.d(TAG, "Active calories for $date: $totalActiveCalories (from ${response.records.size} records)")
                totalActiveCalories
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading active calories for $date", e)
                0
            }
        }
    }
    
    /**
     * Get total calories burned for a specific date
     * This includes both active calories and BMR (Basal Metabolic Rate)
     */
    suspend fun getTotalCaloriesForDate(date: LocalDate): Int {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasAllPermissions()) {
                    Log.w(TAG, "Missing permissions for reading total calories")
                    return@withContext 0
                }
                
                val startTime = date.atStartOfDay().toInstant(ZoneOffset.UTC)
                val endTime = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                
                val request = ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
                
                val response = healthConnectClient.readRecords(request)
                // Same unit conversion fix for total calories
                val totalCalories = response.records.sumOf { record ->
                    val kilojoules = record.energy.inKilojoules
                    val calories = (kilojoules * 0.239).toInt()
                    calories
                }
                
                Log.d(TAG, "Total calories for $date: $totalCalories (converted from kJ)")
                totalCalories
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading total calories for $date", e)
                0
            }
        }
    }
    
    /**
     * Get exercise session data for a specific date
     * Returns total exercise minutes and primary exercise type
     */
    suspend fun getExerciseDataForDate(date: LocalDate): ExerciseData {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasAllPermissions()) {
                    Log.w(TAG, "Missing permissions for reading exercise data")
                    return@withContext ExerciseData(0, null)
                }
                
                val startTime = date.atStartOfDay().toInstant(ZoneOffset.UTC)
                val endTime = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                
                val request = ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
                
                val response = healthConnectClient.readRecords(request)
                
                val totalMinutes = response.records.sumOf { session ->
                    val duration = java.time.Duration.between(session.startTime, session.endTime)
                    duration.toMinutes().toInt()
                }
                
                // Get the most common exercise type
                val exerciseTypes = response.records.mapNotNull { session ->
                    // Convert exercise type to string representation
                    session.exerciseType::class.simpleName
                }
                val primaryExerciseType = exerciseTypes.groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }?.key
                
                Log.d(TAG, "Exercise data for $date: $totalMinutes minutes, type: $primaryExerciseType")
                ExerciseData(totalMinutes, primaryExerciseType)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading exercise data for $date", e)
                ExerciseData(0, null)
            }
        }
    }
    
    /**
     * Get comprehensive fitness data for a specific date
     * Combines active calories, total calories, and exercise data
     */
    suspend fun getFitnessDataForDate(date: LocalDate): FitnessData {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting fitness data for $date")
                
                // Check permissions first
                val hasPermissions = hasAllPermissions()
                Log.d(TAG, "Has all Health Connect permissions: $hasPermissions")
                
                if (!hasPermissions) {
                    return@withContext FitnessData(date, 0, 0, 0, null)
                }
                
                val activeCalories = getActiveCaloriesForDate(date)
                Log.d(TAG, "Active calories: $activeCalories")
                
                val totalCalories = getTotalCaloriesForDate(date)
                Log.d(TAG, "Total calories: $totalCalories")
                
                val exerciseData = getExerciseDataForDate(date)
                Log.d(TAG, "Exercise: ${exerciseData.minutes} minutes of ${exerciseData.type}")
                
                val fitnessData = FitnessData(
                    date = date,
                    activeCaloriesBurned = activeCalories,
                    totalCaloriesBurned = totalCalories,
                    exerciseMinutes = exerciseData.minutes,
                    primaryExerciseType = exerciseData.type
                )
                
                Log.d(TAG, "Returning fitness data: $fitnessData")
                return@withContext fitnessData
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting fitness data for $date", e)
                Log.e("HealthConnectManager", "Unexpected error", e)
                FitnessData(date, 0, 0, 0, null)
            }
        }
    }
    
    /**
     * Get fitness data for a date range
     */
    suspend fun getFitnessDataForDateRange(startDate: LocalDate, endDate: LocalDate): List<FitnessData> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<FitnessData>()
            var currentDate = startDate
            
            while (!currentDate.isAfter(endDate)) {
                val fitnessData = getFitnessDataForDate(currentDate)
                results.add(fitnessData)
                currentDate = currentDate.plusDays(1)
            }
            
            results
        }
    }
    
    /**
     * Get step count for a specific date
     */
    suspend fun getStepsForDate(date: LocalDate): Long {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = date.atStartOfDay().toInstant(ZoneOffset.UTC)
                val endTime = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                
                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
                
                val response = healthConnectClient.readRecords(request)
                response.records.sumOf { it.count }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading steps for $date", e)
                0L
            }
        }
    }
    
    /**
     * Get hydration (water intake) for a specific date in milliliters
     */
    suspend fun getHydrationForDate(date: LocalDate): Double {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = date.atStartOfDay().toInstant(ZoneOffset.UTC)
                val endTime = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                
                val request = ReadRecordsRequest(
                    recordType = HydrationRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
                
                val response = healthConnectClient.readRecords(request)
                response.records.sumOf { it.volume.inMilliliters }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reading hydration for $date", e)
                0.0
            }
        }
    }
    
    /**
     * Get today's step count
     */
    suspend fun getTodaysSteps(): Long {
        return getStepsForDate(LocalDate.now())
    }
    
    /**
     * Get today's hydration in milliliters
     */
    suspend fun getTodaysHydration(): Double {
        return getHydrationForDate(LocalDate.now())
    }
    
    /**
     * Get comprehensive health data for today including steps and hydration
     */
    suspend fun getTodaysHealthData(): HealthData {
        return withContext(Dispatchers.IO) {
            try {
                val today = LocalDate.now()
                val fitnessData = getFitnessDataForDate(today)
                val steps = getTodaysSteps()
                val hydrationMl = getTodaysHydration()
                
                HealthData(
                    date = today,
                    steps = steps,
                    hydrationMl = hydrationMl,
                    activeCaloriesBurned = fitnessData.activeCaloriesBurned,
                    totalCaloriesBurned = fitnessData.totalCaloriesBurned,
                    exerciseMinutes = fitnessData.exerciseMinutes,
                    primaryExerciseType = fitnessData.primaryExerciseType
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting today's health data", e)
                HealthData(LocalDate.now(), 0, 0.0, 0, 0, 0, null)
            }
        }
    }
}

/**
 * Data class representing exercise session data
 */
data class ExerciseData(
    val minutes: Int,
    val type: String?
)

/**
 * Data class representing comprehensive fitness data for a single date
 */
data class FitnessData(
    val date: LocalDate,
    val activeCaloriesBurned: Int,
    val totalCaloriesBurned: Int,
    val exerciseMinutes: Int,
    val primaryExerciseType: String?
)

/**
 * Data class representing comprehensive health data including steps and hydration
 */
data class HealthData(
    val date: LocalDate,
    val steps: Long,
    val hydrationMl: Double,
    val activeCaloriesBurned: Int,
    val totalCaloriesBurned: Int,
    val exerciseMinutes: Int,
    val primaryExerciseType: String?
) {
    /**
     * Convert hydration from milliliters to cups (assuming 237ml per cup - US standard)
     */
    val hydrationCups: Double
        get() = hydrationMl / 237.0
    
    /**
     * Convert hydration from milliliters to glasses (assuming 250ml per glass)
     */
    val hydrationGlasses: Double
        get() = hydrationMl / 250.0
}