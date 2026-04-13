package com.calorietracker.database

// 🏗️ ROOM DATABASE TOOLS
import androidx.room.Entity      // Tells Android this class represents a database table
import androidx.room.Index       // Marks columns that need fast lookup index
import androidx.room.PrimaryKey  // Marks which field is the unique ID

/**
 * 🍎 CALORIE ENTRY - ONE FOOD ITEM IN OUR DIGITAL FOOD DIARY
 * 
 * Hey future programmer! This is like one line in a food diary, but stored in our database.
 * 
 * 📖 Think of it like this:
 * If you write in a paper food diary: "Breakfast: 1 banana, 105 calories, 1.3g protein"
 * This class stores that same information digitally so our app can remember it forever!
 * 
 * 🎯 What information do we store for each food?
 * - Food name (like "Banana" or "Chocolate Chip Cookie")
 * - Calories (the energy that food gives you)
 * - Nutrition facts (protein, carbs, fat, fiber, sugar, sodium)
 * - Date when you ate it (so we can group by days)
 * - Time when you logged it (so we can sort chronologically)
 * - Barcode (if you scanned it instead of typing)
 * - Serving size (in case you ate 2 servings instead of 1)
 * 
 * 💾 Database Storage:
 * This gets saved in a database table called "calorie_entries"
 * Each food entry becomes one row in that table (like one line in a spreadsheet)
 * 
 * @property id         🔢 Unique number that identifies this specific food entry (auto-generated)
 * @property foodName   🍎 What food this is (like "Granny Smith Apple")
 * @property calories   🔥 How much energy this food gives you (like 95 calories)
 * @property protein    💪 Protein content in grams (helps build muscles) - can be unknown
 * @property carbs      🍞 Carbohydrate content in grams (quick energy) - can be unknown
 * @property fat        🥑 Fat content in grams (long-term energy + vitamins) - can be unknown
 * @property fiber      🌾 Fiber content in grams (helps digestion) - can be unknown
 * @property sugar      🍯 Sugar content in grams (sweet carbs) - can be unknown
 * @property sodium     🧂 Sodium content in milligrams (salt - affects blood pressure) - can be unknown
 * @property date       📅 What day you ate this (like "2024-08-31")
 * @property timestamp  ⏰ Exact time you logged this (for sorting entries by time)
 * @property barcode    📷 Barcode number if you scanned this food (optional)
 * @property servings   🥄 How many servings you ate (like 1.5 if you ate one and a half portions)
 */
// 🏷️ DATABASE TABLE SETUP
// @Entity tells Android "this class should become a table in our database"
// tableName = "calorie_entries" means the table will be called "calorie_entries"
@Entity(
    tableName = "calorie_entries",
    indices = [Index(value = ["date"])]
)
data class CalorieEntry(
    // 🔑 PRIMARY KEY - The unique ID number for this food entry
    // @PrimaryKey(autoGenerate = true) means Android will automatically give each
    // food entry a unique number, starting from 1, then 2, then 3, etc.
    // Think of it like giving each entry a unique student ID number in school
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // 🔢 Unique ID number (0 means "create new number automatically")
    
    // 🍎 FOOD IDENTIFICATION
    val foodName: String, // 📛 What food this is (like "Banana" or "Pepperoni Pizza")
    
    // 🔥 MAIN NUTRITION VALUE
    val calories: Int, // ⚡ Energy this food gives you (like 95 calories for an apple)
    
    // 💪 MACRONUTRIENTS - The "big three" nutrients your body needs in large amounts
    // The "?" after Double means these can be unknown/missing (null)
    // Sometimes food databases don't have complete information
    val protein: Double? = null, // 💪 Protein in grams (builds and repairs muscles)
    val carbs: Double? = null,   // 🍞 Carbohydrates in grams (main source of quick energy)
    val fat: Double? = null,     // 🥑 Fat in grams (long-term energy + helps absorb vitamins)
    
    // 🌿 MICRONUTRIENTS - Smaller amounts but still important for health
    val fiber: Double? = null,   // 🌾 Dietary fiber in grams (helps with digestion)
    val sugar: Double? = null,   // 🍯 Sugar in grams (type of carb, but we track separately)
    val sodium: Double? = null,  // 🧂 Sodium in milligrams (salt - too much raises blood pressure)
    
    // 📅 WHEN DID YOU EAT THIS?
    val date: String,            // 📅 What day (like "2024-08-31" for August 31st, 2024)
    val timestamp: Long = System.currentTimeMillis(), // ⏰ Exact time in computer format (milliseconds since 1970)
    
    // 📷 WHERE DID THIS DATA COME FROM?
    val barcode: String? = null, // 📷 Barcode number if you scanned this food (like "123456789012")
    
    // 🥄 HOW MUCH DID YOU EAT?
    val servings: Double = 1.0   // 🥄 Serving multiplier (1.0 = normal serving, 2.0 = double portion, 0.5 = half portion)
)

/**
 * 🧮 SERVING SIZE CALCULATORS - HELPER FUNCTIONS FOR MATH
 * 
 * These are special functions that help us do math with serving sizes!
 * 
 * 📚 What are "extension functions"?
 * These are like giving a class new superpowers! We're adding new abilities
 * to our CalorieEntry class without changing the original class.
 * 
 * 🥄 Why do we need these?
 * If someone eats 2 servings of something, we need to double all the nutrition values.
 * For example: 1 slice of pizza = 300 calories, so 2 slices = 600 calories
 */

/**
 * 💪 CALCULATE ACTUAL PROTEIN EATEN
 * 
 * If the food has 10g protein per serving, and you ate 1.5 servings,
 * this calculates: 10 × 1.5 = 15g of actual protein consumed
 * 
 * @return How many grams of protein you actually ate, or null if we don't know the protein content
 */
fun CalorieEntry.getActualProtein(): Double? = protein?.times(servings)

/**
 * 🍞 CALCULATE ACTUAL CARBS EATEN
 * 
 * If the food has 20g carbs per serving, and you ate 0.5 servings,
 * this calculates: 20 × 0.5 = 10g of actual carbs consumed
 * 
 * @return How many grams of carbs you actually ate, or null if we don't know the carb content
 */
fun CalorieEntry.getActualCarbs(): Double? = carbs?.times(servings)

/**
 * 🥑 CALCULATE ACTUAL FAT EATEN
 * 
 * If the food has 5g fat per serving, and you ate 2 servings,
 * this calculates: 5 × 2 = 10g of actual fat consumed
 * 
 * @return How many grams of fat you actually ate, or null if we don't know the fat content
 */
fun CalorieEntry.getActualFat(): Double? = fat?.times(servings)

/**
 * 🔥 CALCULATE ACTUAL CALORIES EATEN
 * 
 * This is the most important calculation! If a food has 100 calories per serving,
 * and you ate 1.5 servings, this calculates: 100 × 1.5 = 150 total calories
 * 
 * @return Total calories you actually consumed from this food entry
 */
fun CalorieEntry.getActualCalories(): Int = (calories * servings).toInt()

/**
 * ✅ CHECK IF WE HAVE COMPLETE NUTRITION DATA
 * 
 * Sometimes food databases are missing information. This function checks if we have
 * all the "big three" macronutrients (protein, carbs, fat) for this food.
 * 
 * Think of it like checking if a homework assignment is complete - do we have all the required parts?
 * 
 * @return True if we know protein AND carbs AND fat, False if any are missing
 */
fun CalorieEntry.hasCompleteMacros(): Boolean = 
    protein != null && carbs != null && fat != null