package com.calorietracker.database

// 🏗️ ROOM DATABASE TOOLS
import androidx.lifecycle.LiveData    // Auto-updating data
import androidx.room.*               // Database operations
import kotlinx.coroutines.flow.Flow  // Reactive streams

/**
 * 🗃️ STREAK TRACKING DAO - THE ACHIEVEMENT MANAGER
 * 
 * Hey young programmer! This manages all the streak tracking and achievement data.
 * Think of it like a coach who keeps track of your daily progress and achievements!
 * 
 * 🏆 What does this do?
 * - Records daily logging activity
 * - Calculates current and longest streaks
 * - Manages achievement badges
 * - Tracks nutrition challenges
 * - Provides motivation through progress visualization
 */
@Dao
interface StreakTrackingDao {
    
    // 🔥 STREAK TRACKING OPERATIONS
    
    /**
     * 📅 RECORD TODAY'S ACTIVITY
     * 
     * This saves whether user logged food today, met goals, etc.
     * Called at the end of each day to update streak records.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreakRecord(streakRecord: StreakTracking)
    
    /**
     * 📈 GET CURRENT LOGGING STREAK
     * 
     * Returns how many consecutive days user has logged food.
     * For example: "You're on a 12 day logging streak! 🔥"
     */
    @Query("SELECT MAX(currentStreak) FROM streak_tracking WHERE hasLogged = 1")
    suspend fun getCurrentLoggingStreak(): Int?
    
    /**
     * 🏆 GET LONGEST STREAK EVER
     * 
     * User's personal best - their longest streak achievement.
     */
    @Query("SELECT MAX(longestStreak) FROM streak_tracking")
    suspend fun getLongestStreak(): Int?
    
    /**
     * 📊 GET RECENT STREAK DATA
     * 
     * Gets last 30 days of streak data for visualization.
     * Useful for showing streak calendars or progress charts.
     */
    @Query("SELECT * FROM streak_tracking ORDER BY date DESC LIMIT 30")
    suspend fun getRecentStreakData(): List<StreakTracking>
    
    // 🏆 ACHIEVEMENT BADGE OPERATIONS
    
    /**
     * 🎖️ AWARD NEW ACHIEVEMENT BADGE
     * 
     * When user accomplishes something noteworthy, award them a badge!
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievementBadge(badge: AchievementBadge): Long
    
    /**
     * 🏆 GET ALL EARNED BADGES
     * 
     * Shows user's complete achievement collection.
     */
    @Query("SELECT * FROM achievement_badges WHERE isVisible = 1 ORDER BY unlockedDate DESC")
    fun getAllAchievements(): LiveData<List<AchievementBadge>>
    
    /**
     * 🔍 CHECK IF BADGE ALREADY EARNED
     * 
     * Prevents awarding the same badge multiple times.
     */
    @Query("SELECT COUNT(*) FROM achievement_badges WHERE badgeName = :badgeName")
    suspend fun hasBadge(badgeName: String): Int
    
    // 🎯 NUTRITION CHALLENGE OPERATIONS
    
    /**
     * 🎮 CREATE NEW NUTRITION CHALLENGE
     * 
     * Start a new challenge like "Eat 5 vegetables daily for a week".
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: NutritionChallenge): Long
    
    /**
     * 🎯 GET ACTIVE CHALLENGES
     * 
     * Shows all currently running challenges for the user.
     */
    @Query("SELECT * FROM nutrition_challenges WHERE isActive = 1 ORDER BY endDate ASC")
    fun getActiveChallenges(): LiveData<List<NutritionChallenge>>
    
    /**
     * 📈 UPDATE CHALLENGE PROGRESS
     * 
     * When user makes progress on a challenge, update the completion percentage.
     */
    @Query("UPDATE nutrition_challenges SET currentProgress = :progress WHERE id = :challengeId")
    suspend fun updateChallengeProgress(challengeId: Long, progress: Double)
    
    /**
     * ✅ COMPLETE A CHALLENGE
     * 
     * Mark challenge as completed and award any associated rewards.
     */
    @Query("UPDATE nutrition_challenges SET isCompleted = 1, isActive = 0, currentProgress = 100.0 WHERE id = :challengeId")
    suspend fun completeChallenge(challengeId: Long)
    
    /**
     * 📊 GET CHALLENGE COMPLETION STATS
     * 
     * Returns overall statistics about user's challenge participation.
     */
    @Query("SELECT COUNT(*) as total, SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as completed FROM nutrition_challenges")
    suspend fun getChallengeStats(): ChallengeStats
    
    // 📈 ANALYTICS AND INSIGHTS
    
    /**
     * 📅 GET WEEKLY LOGGING PERCENTAGE
     * 
     * What percentage of days this week did user log food?
     */
    @Query("""
        SELECT 
            (SUM(CASE WHEN hasLogged = 1 THEN 1 ELSE 0 END) * 100.0) / COUNT(*) as percentage
        FROM streak_tracking 
        WHERE date >= date('now', '-7 days')
    """)
    suspend fun getWeeklyLoggingPercentage(): Double?
    
    /**
     * 🎯 GET MONTHLY GOAL ACHIEVEMENT RATE
     * 
     * What percentage of days this month did user meet their nutrition goals?
     */
    @Query("""
        SELECT 
            (SUM(CASE WHEN metNutritionGoals = 1 THEN 1 ELSE 0 END) * 100.0) / COUNT(*) as percentage
        FROM streak_tracking 
        WHERE date >= date('now', 'start of month')
    """)
    suspend fun getMonthlyGoalAchievementRate(): Double?
}

/**
 * 📊 CHALLENGE STATISTICS DATA CLASS
 * 
 * Simple container for challenge completion statistics.
 */
data class ChallengeStats(
    val total: Int,      // 🔢 Total challenges attempted
    val completed: Int   // ✅ Challenges successfully completed
)