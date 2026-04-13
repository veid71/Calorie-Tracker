package com.calorietracker.database

// 🏗️ ROOM DATABASE TOOLS
import androidx.room.Entity           // Tells Android this class represents a database table
import androidx.room.PrimaryKey       // Marks which field is the unique ID
import androidx.room.Index           // Creates database indexes for faster searching

/**
 * 🍽️ MEAL TEMPLATE - SAVE YOUR FAVORITE MEAL COMBINATIONS
 * 
 * Hey young programmer! This is like having recipe cards for common meals.
 * 
 * 🎯 What's a meal template?
 * Instead of logging "coffee + toast + eggs" every morning, you can save it as
 * "My Typical Breakfast" and add the whole meal with one button press!
 * 
 * 💡 Real-world examples:
 * - "Quick Lunch": Turkey sandwich + apple + chips
 * - "Post-Workout": Protein shake + banana + granola bar  
 * - "Movie Night": Popcorn + soda + candy
 * 
 * 📊 How it works:
 * 1. User creates a template from existing food entries
 * 2. We save the template name and which foods belong to it
 * 3. Later, user can quickly add all those foods to any day
 * 
 * @property id           🔢 Unique template ID number
 * @property templateName 🏷️ User's name for this meal (like "Typical Breakfast")
 * @property description  📝 Optional description (like "My go-to morning meal")
 * @property mealType     🍽️ What kind of meal (breakfast, lunch, dinner, snack)
 * @property totalCalories 🔥 Pre-calculated total calories for quick display
 * @property isActive     ✅ Whether this template is still being used
 * @property createdDate  📅 When this template was first created
 * @property lastUsed     ⏰ When this template was last used (for sorting)
 * @property useCount     📊 How many times this template has been used
 */
@Entity(
    tableName = "meal_templates",
    indices = [Index(value = ["templateName"], unique = false)]
)
data class MealTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                    // 🔢 Unique ID for this meal template
    
    val templateName: String,            // 🏷️ Name like "Typical Breakfast" or "Post-Workout Meal"
    val description: String? = null,     // 📝 Optional description from user
    val mealType: String,               // 🍽️ "breakfast", "lunch", "dinner", "snack"
    val totalCalories: Int,             // 🔥 Sum of calories from all foods in this template
    val isActive: Boolean = true,       // ✅ Is this template still being used?
    val createdDate: String,            // 📅 When template was created (YYYY-MM-DD format)
    val lastUsed: Long = 0,             // ⏰ Last usage timestamp (for recent templates)
    val useCount: Int = 0               // 📊 How many times user has used this template
)

/**
 * 🍎 MEAL TEMPLATE ITEM - ONE FOOD IN A MEAL TEMPLATE
 * 
 * This represents one food item within a meal template.
 * For example, if "Typical Breakfast" template contains coffee, toast, and eggs,
 * there would be 3 MealTemplateItem entries (one for each food).
 * 
 * 🔗 Database Relationship:
 * This links to MealTemplate via templateId - it's like saying
 * "This piece of toast belongs to the 'Typical Breakfast' template"
 * 
 * @property id          🔢 Unique ID for this template item
 * @property templateId  🔗 Which meal template this food belongs to
 * @property foodName    🍎 Name of the food (like "Whole Wheat Toast")
 * @property calories    🔥 Calories for this food in the template
 * @property protein     💪 Protein content (can be null if unknown)
 * @property carbs       🍞 Carbohydrate content  
 * @property fat         🥑 Fat content
 * @property fiber       🌾 Fiber content
 * @property sugar       🍯 Sugar content
 * @property sodium      🧂 Sodium content
 * @property servings    🥄 How many servings of this food
 * @property barcode     📷 Barcode if this food was originally scanned
 * @property sortOrder   📋 Order within the template (1st, 2nd, 3rd food)
 */
@Entity(
    tableName = "meal_template_items",
    indices = [Index(value = ["templateId"], unique = false)]
)
data class MealTemplateItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                   // 🔢 Unique ID for this template item
    
    val templateId: Long,               // 🔗 Which meal template this belongs to
    val foodName: String,               // 🍎 Food name like "Greek Yogurt"
    val calories: Int,                  // 🔥 Calories for this food item
    val protein: Double? = null,        // 💪 Protein in grams
    val carbs: Double? = null,          // 🍞 Carbs in grams  
    val fat: Double? = null,            // 🥑 Fat in grams
    val fiber: Double? = null,          // 🌾 Fiber in grams
    val sugar: Double? = null,          // 🍯 Sugar in grams
    val sodium: Double? = null,         // 🧂 Sodium in milligrams
    val servings: Double = 1.0,         // 🥄 Serving multiplier
    val barcode: String? = null,        // 📷 Original barcode if scanned
    val sortOrder: Int = 0              // 📋 Order within template (for consistent display)
)