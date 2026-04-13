package com.calorietracker.database

// 🏗️ ROOM DATABASE TOOLS
import androidx.room.Entity           // Database table definition
import androidx.room.PrimaryKey       // Unique identifier
import androidx.room.Index           // Performance optimization

/**
 * 🔥 STREAK TRACKING - TRACK DAILY LOGGING CONSISTENCY
 * 
 * Hey young programmer! This tracks how many days in a row users log their food.
 * 
 * 🎯 What's a streak?
 * A streak is when you do something consistently every day, like:
 * "I've logged my food for 15 days in a row!"
 * 
 * 🏆 Why track streaks?
 * Streaks are motivating! People want to keep their streak going, which encourages
 * consistent food logging and better health habits.
 * 
 * 📊 Types of streaks we track:
 * - Logging streak: Days with at least one food entry
 * - Complete logging: Days with breakfast, lunch, and dinner
 * - Nutrition goal streak: Days meeting nutrition targets
 * - Exercise streak: Days with workout data
 * 
 * @property id            🔢 Unique record ID
 * @property date          📅 The date for this streak record
 * @property hasLogged     ✅ Did user log any food this day?
 * @property hasCompleteLog 🍽️ Did user log breakfast, lunch, AND dinner?
 * @property metNutritionGoals 🎯 Did user meet their nutrition targets?
 * @property hasExercise   💪 Did user have any workout/exercise data?
 * @property calorieGoalMet 🔥 Did user stay within calorie goal (±100 cal tolerance)?
 * @property currentStreak 📈 Current consecutive days of logging
 * @property longestStreak 🏆 Personal best streak ever achieved
 */
@Entity(
    tableName = "streak_tracking",
    indices = [Index(value = ["date"], unique = true)]  // One record per date
)
data class StreakTracking(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                    // 🔢 Unique record ID
    
    val date: String,                    // 📅 Date in YYYY-MM-DD format
    val hasLogged: Boolean = false,      // ✅ Any food logged today?
    val hasCompleteLog: Boolean = false, // 🍽️ Breakfast + lunch + dinner logged?
    val metNutritionGoals: Boolean = false, // 🎯 Met protein/carb/fat targets?
    val hasExercise: Boolean = false,    // 💪 Any workout data today?
    val calorieGoalMet: Boolean = false, // 🔥 Stayed within calorie goal?
    val currentStreak: Int = 0,          // 📈 Current consecutive logging days
    val longestStreak: Int = 0           // 🏆 Personal best streak record
)

/**
 * 🏆 ACHIEVEMENT BADGE - UNLOCKABLE REWARDS FOR HEALTHY HABITS
 * 
 * This tracks achievements users can unlock through consistent app usage.
 * Think of it like earning merit badges in scouts or trophies in video games!
 * 
 * 🎖️ Types of achievements:
 * - Streak milestones: "7 Day Warrior", "30 Day Champion", "100 Day Legend"
 * - Nutrition achievements: "Protein Pro", "Fiber Fighter", "Balanced Eater"
 * - Feature usage: "Barcode Scanner", "Recipe Creator", "Goal Setter"
 * - Special occasions: "New Year New Me", "Summer Body Ready"
 * 
 * @property id           🔢 Unique achievement ID
 * @property badgeType    🏷️ Category like "streak", "nutrition", "feature", "special"
 * @property badgeName    🎖️ Display name like "7 Day Warrior"
 * @property description  📝 What user did to earn this
 * @property iconName     🖼️ Icon resource name for display
 * @property unlockedDate 📅 When user earned this achievement
 * @property difficulty   ⭐ How hard this was to achieve (1-5 stars)
 * @property isVisible    👁️ Should this show in achievements list?
 * @property shareText    📤 Pre-written text for social sharing
 */
@Entity(
    tableName = "achievement_badges",
    indices = [Index(value = ["badgeType"], unique = false)]
)
data class AchievementBadge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                    // 🔢 Unique achievement ID
    
    val badgeType: String,               // 🏷️ "streak", "nutrition", "feature", "special"
    val badgeName: String,               // 🎖️ "7 Day Warrior", "Protein Pro", etc.
    val description: String,             // 📝 "Logged food for 7 consecutive days"
    val iconName: String,                // 🖼️ "badge_7_day", "badge_protein", etc.
    val unlockedDate: String,            // 📅 When earned (YYYY-MM-DD)
    val difficulty: Int = 1,             // ⭐ 1-5 stars difficulty
    val isVisible: Boolean = true,       // 👁️ Show in achievements screen?
    val shareText: String? = null        // 📤 "I just earned my 7 Day Logging Streak! 🔥"
)

/**
 * 🎯 NUTRITION CHALLENGE - WEEKLY/MONTHLY GOALS FOR USER ENGAGEMENT
 * 
 * These are special challenges to keep users engaged with healthy eating.
 * Think of them like fitness challenges but for nutrition!
 * 
 * 🥗 Example challenges:
 * - "Veggie Week": Eat 5 servings of vegetables for 7 days
 * - "Protein Power": Hit protein goal 5 days this week
 * - "Hydration Hero": Drink 8 glasses of water daily for a week
 * - "Fiber Fighter": Get 25g+ fiber for 10 days
 * 
 * @property id             🔢 Unique challenge ID
 * @property challengeName  🏷️ Display name like "Veggie Week Challenge"
 * @property description    📝 What user needs to do
 * @property challengeType  🎯 "weekly", "monthly", "custom"
 * @property targetMetric   📊 What to track ("vegetables", "protein", "water", etc.)
 * @property targetValue    🎯 Goal amount (like "5 servings per day")
 * @property targetDays     📅 How many days to maintain goal
 * @property startDate      📅 When challenge starts
 * @property endDate        📅 When challenge ends
 * @property currentProgress 📈 Current progress (0-100%)
 * @property isCompleted    ✅ Has user completed this challenge?
 * @property isActive       🎮 Is this challenge currently running?
 * @property reward         🏆 What user gets for completing (badge, points, etc.)
 */
@Entity(
    tableName = "nutrition_challenges",
    indices = [Index(value = ["challengeType", "isActive"], unique = false)]
)
data class NutritionChallenge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                    // 🔢 Unique challenge ID
    
    val challengeName: String,           // 🏷️ "Veggie Week Challenge"
    val description: String,             // 📝 "Eat 5+ servings of vegetables daily for 7 days"
    val challengeType: String,           // 🎯 "weekly", "monthly", "custom"
    val targetMetric: String,            // 📊 "vegetables", "protein", "fiber", etc.
    val targetValue: Double,             // 🎯 Target amount (like 5.0 for "5 servings")
    val targetDays: Int,                 // 📅 How many days (like 7 for a week)
    val startDate: String,               // 📅 Challenge start date
    val endDate: String,                 // 📅 Challenge end date
    val currentProgress: Double = 0.0,   // 📈 Progress percentage (0.0 to 100.0)
    val isCompleted: Boolean = false,    // ✅ Challenge completed?
    val isActive: Boolean = true,        // 🎮 Currently running?
    val reward: String? = null           // 🏆 "7 Day Streak Badge" or "50 points"
)