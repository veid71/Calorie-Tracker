package com.calorietracker.database

// 🏗️ ROOM DATABASE TOOLS
import androidx.room.Entity           // Database table definition
import androidx.room.PrimaryKey       // Unique identifier  
import androidx.room.Index           // Database indexing for performance

/**
 * 🍔 RESTAURANT CHAIN - FAST FOOD AND CHAIN RESTAURANT DATA
 * 
 * Hey young programmer! This stores nutrition information from popular restaurant chains.
 * 
 * 🎯 Why do we need this?
 * Restaurant food often has very different nutrition than home-cooked meals.
 * A McDonald's Big Mac has specific, standardized nutrition facts that are different
 * from a homemade burger. We need accurate data for chain restaurants!
 * 
 * 🍟 Supported Chains:
 * - McDonald's, Burger King, Wendy's
 * - Subway, Jimmy John's, Quiznos  
 * - Starbucks, Dunkin' Donuts
 * - Pizza Hut, Domino's, Papa John's
 * - Taco Bell, Chipotle, Qdoba
 * 
 * 📊 Data Sources:
 * - Official restaurant nutrition PDFs
 * - Public nutrition APIs where available
 * - Verified nutritional databases
 * 
 * @property id            🔢 Unique item ID
 * @property chainName     🏪 Restaurant name like "McDonald's"
 * @property itemName      🍔 Menu item like "Big Mac"
 * @property category      🍽️ Food category like "burgers", "sides", "drinks"
 * @property calories      🔥 Calories per standard serving
 * @property protein       💪 Protein in grams
 * @property carbs         🍞 Carbohydrates in grams
 * @property fat           🥑 Total fat in grams
 * @property saturatedFat  🧈 Saturated fat in grams
 * @property fiber         🌾 Dietary fiber in grams
 * @property sugar         🍯 Total sugar in grams
 * @property sodium        🧂 Sodium in milligrams
 * @property servingSize   🥄 Standard serving description
 * @property imageUrl      🖼️ Official product image URL
 * @property isAvailable   ✅ Currently on menu?
 * @property lastUpdated   📅 When nutrition data was last verified
 */
@Entity(
    tableName = "restaurant_chains",
    indices = [
        Index(value = ["chainName"], unique = false),
        Index(value = ["itemName"], unique = false),
        Index(value = ["category"], unique = false)
    ]
)
data class RestaurantChain(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                    // 🔢 Unique database ID
    
    val chainName: String,               // 🏪 "McDonald's", "Subway", "Starbucks"
    val itemName: String,                // 🍔 "Big Mac", "Italian BMT", "Caramel Macchiato"
    val category: String,                // 🍽️ "burgers", "sandwiches", "drinks", "sides"
    
    // 📊 NUTRITION INFORMATION
    val calories: Int,                   // 🔥 Calories per standard serving
    val protein: Double? = null,         // 💪 Protein content in grams
    val carbs: Double? = null,           // 🍞 Carbohydrate content in grams
    val fat: Double? = null,             // 🥑 Total fat content in grams
    val saturatedFat: Double? = null,    // 🧈 Saturated fat in grams
    val fiber: Double? = null,           // 🌾 Dietary fiber in grams
    val sugar: Double? = null,           // 🍯 Sugar content in grams
    val sodium: Double? = null,          // 🧂 Sodium in milligrams
    
    // 📏 SERVING INFORMATION
    val servingSize: String? = null,     // 🥄 "1 sandwich", "16 fl oz", "1 large"
    val servingWeight: Double? = null,   // ⚖️ Weight in grams if available
    
    // 🖼️ VISUAL AND METADATA
    val imageUrl: String? = null,        // 🖼️ Official product image
    val isAvailable: Boolean = true,     // ✅ Currently on menu?
    val price: Double? = null,           // 💰 Typical price (for meal planning)
    val lastUpdated: String,             // 📅 When data was last verified
    
    // 🔍 SEARCH OPTIMIZATION
    val searchKeywords: String? = null   // 🔍 Additional search terms for better findability
)

/**
 * 📸 MEAL PHOTO - VISUAL FOOD DIARY ENTRIES
 * 
 * This stores photos that users take of their meals for visual tracking.
 * Think of it like Instagram but for nutrition tracking!
 * 
 * 🎯 Why take meal photos?
 * - Visual memory aid ("What did I eat for lunch Tuesday?")
 * - Portion size reference for future meals
 * - Progress motivation (see healthy meal choices over time)
 * - Share meals with nutritionist or trainer
 * 
 * @property id           🔢 Unique photo ID
 * @property photoPath    📸 Local file path where photo is stored
 * @property thumbnailPath 🖼️ Smaller version for list display
 * @property mealType     🍽️ "breakfast", "lunch", "dinner", "snack"
 * @property date         📅 When photo was taken
 * @property timestamp    ⏰ Precise time photo was taken
 * @property calories     🔥 Estimated calories from this meal
 * @property description  📝 User's notes about the meal
 * @property location     📍 Where photo was taken (optional)
 * @property isShared     📤 Has user shared this photo?
 * @property rating       ⭐ User's rating of meal (1-5 stars)
 */
@Entity(
    tableName = "meal_photos",
    indices = [
        Index(value = ["date"], unique = false),
        Index(value = ["mealType"], unique = false)
    ]
)
data class MealPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                    // 🔢 Unique photo ID
    
    val photoPath: String,               // 📸 Where photo file is stored on device
    val thumbnailPath: String? = null,   // 🖼️ Smaller version for faster loading
    val mealType: String,                // 🍽️ "breakfast", "lunch", "dinner", "snack"
    val date: String,                    // 📅 Date photo was taken (YYYY-MM-DD)
    val timestamp: Long,                 // ⏰ Exact time photo was taken
    val calories: Int? = null,           // 🔥 Estimated calories (user can add manually)
    val description: String? = null,     // 📝 "Homemade pasta with vegetables"
    val location: String? = null,        // 📍 "Home", "Olive Garden", etc.
    val isShared: Boolean = false,       // 📤 Shared to social media?
    val rating: Int? = null              // ⭐ 1-5 star rating of how good meal was
)

/**
 * 🎨 TABLET LAYOUT PREFERENCE - OPTIMIZE FOR LARGER SCREENS
 * 
 * This tracks user preferences for tablet-optimized layouts.
 * 
 * 📱 Device Detection:
 * - Phone: Single column layout with stacked elements
 * - Tablet: Multi-column layout with side-by-side panels
 * - Auto-detect: Automatically choose based on screen size
 * 
 * @property id              🔢 Unique preference ID
 * @property isTabletLayout  📱 Use tablet-optimized layout?
 * @property isAutoDetect    🤖 Automatically detect device type?
 * @property screenSize      📏 Detected screen size category
 * @property layoutDensity   🔍 Screen density (hdpi, xhdpi, xxhdpi)
 * @property userOverride    👤 User manually chose layout preference?
 */
@Entity(tableName = "tablet_preferences")
data class TabletPreference(
    @PrimaryKey val id: Int = 1,         // 🔢 Only one preference record needed
    val isTabletLayout: Boolean = false, // 📱 Use tablet layout?
    val isAutoDetect: Boolean = true,    // 🤖 Auto-detect screen size?
    val screenSize: String = "normal",   // 📏 "small", "normal", "large", "xlarge"
    val layoutDensity: String = "mdpi",  // 🔍 Screen density category
    val userOverride: Boolean = false    // 👤 User manually set preference?
)