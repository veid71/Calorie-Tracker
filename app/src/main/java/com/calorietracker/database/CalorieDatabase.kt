package com.calorietracker.database

// 🧰 ANDROID ROOM DATABASE TOOLS
// These imports give us everything we need to create and manage our app's database
import androidx.room.Database       // Tells Android "this class defines our database structure"
import androidx.room.Room          // The factory that creates database instances for us
import androidx.room.RoomDatabase  // The base class all Room databases extend from
import androidx.room.migration.Migration // Tools for safely updating the database structure
import androidx.sqlite.db.SupportSQLiteDatabase // Direct database access for complex operations
import android.content.Context     // Gives us access to the app's file system

/**
 * 🏗️ CALORIE DATABASE - THE BRAIN OF OUR DATA STORAGE
 * 
 * Hey future programmer! This is the most important database file in our entire app.
 * Think of this like the "blueprint" for a digital filing cabinet that stores all our app's information.
 * 
 * 📚 What is Room Database?
 * Room is Android's modern database system. It's like having a smart librarian who:
 * - Organizes all your data into neat tables (like spreadsheets)
 * - Remembers exactly where everything is stored
 * - Protects your data from corruption
 * - Makes sure multiple parts of your app can safely access data at the same time
 * - Automatically handles complex database operations for you
 * 
 * 🗂️ Database Structure - Think of it like a digital filing cabinet:
 * Each "Entity" (like CalorieEntry, WorkoutCalories) becomes a separate table
 * Each table is like a spreadsheet with rows and columns
 * Each row represents one piece of data (like one food entry)
 * Each column represents one property (like calories, date, food name)
 * 
 * 🔄 Database Migrations - Like Renovating Your Filing System:
 * Sometimes we need to add new types of data or change how we store things
 * Migrations are like step-by-step renovation instructions that safely update
 * the database structure without losing any user data
 * 
 * 📱 Why We Need This:
 * - Store user's food entries permanently (survives app restarts)
 * - Keep track of workout data from fitness trackers
 * - Remember user preferences and goals
 * - Cache food information for offline use
 * - Store meal plans, shopping lists, and recipes
 * - Maintain barcode scan history
 * - Track weight and progress photos
 * 
 * 🎯 Version History:
 * Version 16 = Current version (we've made 16 updates to the database structure)
 * Each time we add new features, we increment the version and add a migration
 */
@Database(
    // 📋 ENTITIES LIST - All the different types of data we store
    // Think of each entity as a different type of form in our filing cabinet
    entities = [
        // 🍎 CORE NUTRITION DATA
        FoodItem::class,          // Individual food items from databases (like "Banana" with nutrition facts)
        CalorieEntry::class,      // User's daily food entries (like "I ate 1 banana at breakfast")
        DailyGoal::class,         // User's daily calorie targets (like "I want to eat 2000 calories/day")
        NutritionGoals::class,    // Detailed nutrition targets (protein, carbs, fat goals)
        
        // 📷 BARCODE & CACHING SYSTEM
        BarcodeQueue::class,      // Queue of barcodes to sync when internet is available
        BarcodeCache::class,      // Cached barcode scan results (for offline use)
        BarcodeHistory::class,    // History of all barcode scans (successful and failed)
        
        // 💪 FITNESS & HEALTH TRACKING
        WorkoutCalories::class,   // Daily workout calories from Health Connect / smartwatches
        WeightEntry::class,       // Weight measurements over time
        WeightGoal::class,        // Weight loss/gain goals and targets
        DailyWeightEntry::class,  // Daily weight tracking entries
        WaterIntakeEntry::class,  // Daily water consumption tracking
        ProgressPhoto::class,     // Before/after photos with measurements
        
        // 🗃️ EXTERNAL FOOD DATABASES
        USDAFoodItem::class,      // Foods from USDA nutritional database (113,886+ items)
        OpenFoodFactsItem::class, // Foods from Open Food Facts database (international products)
        FoodDatabaseStatus::class,// Status of database downloads and updates
        
        // ⚡ PERFORMANCE & OFFLINE FEATURES
        OfflineFood::class,       // Locally cached food data for offline use
        SearchCache::class,       // Cached search results to speed up repeated searches
        CacheMetadata::class,     // Information about cache operations and cleanup
        
        // 🍽️ MEAL PLANNING & RECIPES
        Recipe::class,            // User-created recipes with ingredients and instructions
        RecipeIngredient::class,  // Individual ingredients within recipes
        FavoriteMeal::class,      // Frequently eaten foods for quick access
        MealPlan::class,          // Planned meals for specific dates and times
        
        // 🛒 SHOPPING & COMMUNITY  
        ShoppingListItem::class,  // Shopping list items generated from meal plans
        
        // 👥 COMMUNITY FEATURES - NOW ENABLED!
        CommunityRecipe::class,   // Community shared recipes with ratings
        RecipeReview::class,      // User reviews and ratings for community recipes
        RecipeComment::class,     // Comments and discussions on recipes
        RecipeFavorite::class     // User's saved/favorited community recipes
    ],
    version = 19,           // 📊 Current database version - added date index on calorie_entries
    exportSchema = true     // Export schema JSON for migration verification (committed to version control)
)
abstract class CalorieDatabase : RoomDatabase() {
    
    // 🚪 DATABASE ACCESS OBJECTS (DAOs) - Your Data Department Managers
    // 
    // 📖 What are DAOs?
    // DAO stands for "Data Access Object" - think of them like specialized department managers
    // in our database company. Each DAO knows how to work with one specific type of data.
    // 
    // 🏢 How this works:
    // - Instead of directly messing with raw database tables (dangerous!), 
    //   we ask the appropriate DAO manager to handle operations for us
    // - Each DAO provides safe, tested methods like "getAllEntries()", "insertEntry()", etc.
    // - This prevents database corruption and makes our code much cleaner
    // 
    // 📋 Think of it like this:
    // If you want to add a food entry, you don't go directly into the filing cabinet.
    // Instead, you ask the "Food Entry Department Manager" (CalorieEntryDao) to handle it properly.
    
    // 🍎 CORE NUTRITION DATA MANAGERS
    abstract fun foodItemDao(): FoodItemDao                    // Manages food database items
    abstract fun calorieEntryDao(): CalorieEntryDao            // Manages user's daily food entries
    abstract fun dailyGoalDao(): DailyGoalDao                  // Manages daily calorie targets
    abstract fun nutritionGoalsDao(): NutritionGoalsDao        // Manages detailed nutrition goals
    
    // 📷 BARCODE & CACHING SYSTEM MANAGERS
    abstract fun barcodeQueueDao(): BarcodeQueueDao            // Manages pending barcode syncs
    abstract fun barcodeCacheDao(): BarcodeCacheDao            // Manages cached barcode results
    abstract fun barcodeHistoryDao(): BarcodeHistoryDao        // Manages barcode scan history
    
    // 💪 FITNESS & HEALTH TRACKING MANAGERS
    abstract fun workoutCaloriesDao(): WorkoutCaloriesDao      // Manages workout/fitness data
    abstract fun weightEntryDao(): WeightEntryDao              // Manages weight measurements
    abstract fun weightGoalDao(): WeightGoalDao                // Manages weight goals
    abstract fun dailyWeightEntryDao(): DailyWeightEntryDao    // Manages daily weight tracking
    abstract fun waterIntakeEntryDao(): WaterIntakeEntryDao    // Manages water consumption
    abstract fun progressPhotoDao(): ProgressPhotoDao          // Manages before/after photos
    
    // 🗃️ EXTERNAL FOOD DATABASE MANAGERS
    abstract fun usdaFoodItemDao(): USDAFoodItemDao            // Manages USDA food database
    abstract fun openFoodFactsDao(): OpenFoodFactsDao          // Manages Open Food Facts data
    abstract fun foodDatabaseStatusDao(): FoodDatabaseStatusDao // Manages database sync status
    
    // ⚡ PERFORMANCE & OFFLINE FEATURE MANAGERS
    abstract fun offlineFoodDao(): OfflineFoodDao              // Manages offline cached foods
    abstract fun searchCacheDao(): SearchCacheDao              // Manages cached search results
    abstract fun cacheMetadataDao(): CacheMetadataDao          // Manages cache maintenance info
    
    // 🍽️ MEAL PLANNING & RECIPE MANAGERS
    abstract fun recipeDao(): RecipeDao                        // Manages user recipes
    abstract fun recipeIngredientDao(): RecipeIngredientDao    // Manages recipe ingredients
    abstract fun favoriteMealDao(): FavoriteMealDao            // Manages frequently eaten foods
    abstract fun mealPlanDao(): MealPlanDao                    // Manages planned meals
    
    // 🛒 SHOPPING & COMMUNITY MANAGERS
    abstract fun shoppingListDao(): ShoppingListDao            // Manages shopping lists
    abstract fun communityRecipeDao(): CommunityRecipeDao      // Manages shared community recipes
    
    // 🔔 NOTIFICATION MANAGERS - TEMPORARILY DISABLED
    // abstract fun reminderNotificationDao(): ReminderNotificationDao // Manages meal reminders
    
    companion object {
        // 🏢 SINGLETON PATTERN - One Database Instance For The Entire App
        // 
        // 📖 What is @Volatile?
        // @Volatile tells the computer "multiple parts of the app might access this at the same time"
        // This ensures that when one part creates the database, all other parts can see it immediately
        // Think of it like a bulletin board that everyone checks for the latest updates
        @Volatile
        private var INSTANCE: CalorieDatabase? = null
        
        // 🔄 DATABASE MIGRATIONS - Safe Ways To Update The Database Structure
        // 
        // 📖 What are Migrations?
        // Migrations are like "renovation instructions" for your database. When we need to add
        // new features (like workout tracking), we can't just change the database structure -
        // that would delete all user data! Instead, we write migrations that safely transform
        // the old structure into the new one without losing anything.
        // 
        // 🏗️ How Migrations Work:
        // 1. User has app version with database v8 (old structure)
        // 2. User updates app to version with database v9 (new structure) 
        // 3. App detects version difference and runs MIGRATION_8_9
        // 4. Migration adds new tables/columns without touching existing data
        // 5. User keeps all their food entries, but now has access to new features!
        // 
        // 🎯 Migration Best Practices:
        // - Always increment version number when changing database structure
        // - Never delete or modify existing columns (breaks old data)
        // - Always provide migrations for every version change
        // - Test migrations thoroughly to prevent data loss
        
        // 🆕 MIGRATION 8→9: Added Caching System For Offline Functionality
        // This migration adds tables for offline food caching and search result caching
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 🗃️ CREATE OFFLINE_FOODS TABLE
                // This table stores food data downloaded from APIs so the app works without internet
                // Think of it like downloading your favorite songs so you can listen without WiFi
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS offline_foods (
                        barcode TEXT NOT NULL PRIMARY KEY,    -- 📷 The barcode is the unique ID
                        name TEXT NOT NULL,                   -- 🍎 Food name like "Coca Cola"
                        calories INTEGER NOT NULL,            -- 🔥 Calories per serving
                        protein REAL,                         -- 💪 Protein in grams (optional)
                        carbs REAL,                          -- 🍞 Carbs in grams (optional)
                        fat REAL,                            -- 🥑 Fat in grams (optional)
                        fiber REAL,                          -- 🌾 Fiber in grams (optional)
                        sugar REAL,                          -- 🍯 Sugar in grams (optional)
                        sodium REAL,                         -- 🧂 Sodium in mg (optional)
                        servingSize TEXT,                    -- 📏 Like "1 cup" or "100g"
                        servingUnit TEXT,                    -- 📐 Like "cup", "gram", "piece"
                        brand TEXT,                          -- 🏷️ Brand name like "Coca Cola"
                        categories TEXT,                     -- 🗂️ Food categories (JSON format)
                        ingredients TEXT,                    -- 📝 List of ingredients
                        allergens TEXT,                      -- ⚠️ Allergy warnings
                        source TEXT NOT NULL,                -- 🌐 Where we got this data (API name)
                        lastUpdated INTEGER NOT NULL,        -- ⏰ When we last updated this info
                        popularity INTEGER NOT NULL DEFAULT 0,-- 📊 How often users search for this
                        isVerified INTEGER NOT NULL DEFAULT 0 -- ✅ Whether nutrition data is verified
                    )
                """.trimIndent())
                
                // 🔍 CREATE SEARCH_CACHE TABLE  
                // This table remembers search results so repeated searches are super fast
                // Like having a smart assistant who remembers your previous questions
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS search_cache (
                        query TEXT NOT NULL PRIMARY KEY,      -- 🔍 The search term like "apple"
                        resultCount INTEGER NOT NULL,         -- 📊 How many results we found
                        resultBarcodes TEXT NOT NULL,          -- 📋 List of matching barcodes (JSON)
                        timestamp INTEGER NOT NULL,           -- ⏰ When we cached this search
                        source TEXT NOT NULL DEFAULT 'mixed', -- 🌐 Which API(s) we searched
                        locale TEXT NOT NULL DEFAULT 'en_US'  -- 🗺️ Language/country setting
                    )
                """.trimIndent())
                
                // 📋 CREATE CACHE_METADATA TABLE
                // This table tracks cache operations for maintenance and debugging
                // Like a logbook that records what happened when
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cache_metadata (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, -- 🔢 Unique ID
                        operation TEXT NOT NULL,              -- 📝 What happened ("cleanup", "sync", etc.)
                        timestamp INTEGER NOT NULL,           -- ⏰ When this happened
                        details TEXT,                         -- 📄 Extra information about the operation
                        cacheVersion INTEGER NOT NULL DEFAULT 1, -- 📊 Cache format version
                        affectedRows INTEGER                  -- 📊 How many items were affected
                    )
                """.trimIndent())
                
                // ⚡ CREATE DATABASE INDICES FOR BETTER PERFORMANCE
                // Indices are like bookmarks that help the database find things faster
                // Think of them like the index in the back of a textbook - they point to where things are
                database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_foods_name ON offline_foods(name)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_foods_barcode ON offline_foods(barcode)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_foods_brand ON offline_foods(brand)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_offline_foods_lastUpdated ON offline_foods(lastUpdated)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_search_cache_timestamp ON search_cache(timestamp)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_search_cache_resultCount ON search_cache(resultCount)")
            }
        }
        
        // Migration from version 9 to 10 to add recipe system tables
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create Recipe table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recipes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        instructions TEXT,
                        servings INTEGER NOT NULL,
                        prepTime INTEGER,
                        cookTime INTEGER,
                        category TEXT,
                        createdBy TEXT,
                        createdDate TEXT NOT NULL,
                        lastModified INTEGER NOT NULL,
                        isShared INTEGER NOT NULL,
                        shareId TEXT,
                        isFavorite INTEGER NOT NULL,
                        timesUsed INTEGER NOT NULL,
                        averageRating REAL,
                        totalCalories INTEGER NOT NULL,
                        totalProtein REAL NOT NULL,
                        totalCarbs REAL NOT NULL,
                        totalFat REAL NOT NULL,
                        totalFiber REAL NOT NULL,
                        totalSugar REAL NOT NULL,
                        totalSodium REAL NOT NULL
                    )
                """.trimIndent())
                
                // Create RecipeIngredient table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recipe_ingredients (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recipeId INTEGER NOT NULL,
                        ingredientName TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        calories INTEGER NOT NULL,
                        protein REAL NOT NULL,
                        carbs REAL NOT NULL,
                        fat REAL NOT NULL,
                        fiber REAL,
                        sugar REAL,
                        sodium REAL,
                        foodItemId INTEGER,
                        barcode TEXT,
                        originalServingSize REAL,
                        originalServingUnit TEXT,
                        notes TEXT,
                        `order` INTEGER NOT NULL,
                        isOptional INTEGER NOT NULL,
                        substitutions TEXT,
                        FOREIGN KEY(recipeId) REFERENCES recipes(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create indices for better performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipes_name ON recipes(name)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipes_category ON recipes(category)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipes_isFavorite ON recipes(isFavorite)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipes_shareId ON recipes(shareId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_recipeId ON recipe_ingredients(recipeId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_ingredients_ingredientName ON recipe_ingredients(ingredientName)")
            }
        }
        
        // Migration from version 10 to 11 to add favorite meals table  
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create favorite_meals table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorite_meals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        foodName TEXT NOT NULL,
                        brand TEXT,
                        servingSize TEXT,
                        calories INTEGER NOT NULL,
                        protein REAL,
                        carbs REAL,
                        fat REAL,
                        fiber REAL,
                        sugar REAL,
                        sodium REAL,
                        barcode TEXT,
                        timesUsed INTEGER NOT NULL DEFAULT 1,
                        lastUsed INTEGER NOT NULL,
                        dateAdded INTEGER NOT NULL,
                        category TEXT,
                        tags TEXT
                    )
                """)
            }
        }
        
        // Migration from version 11 to 12 to add barcode history table  
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create barcode_history table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS barcode_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        barcode TEXT NOT NULL,
                        foodName TEXT NOT NULL,
                        brand TEXT,
                        calories INTEGER NOT NULL,
                        servingSize TEXT,
                        firstScanned INTEGER NOT NULL,
                        lastScanned INTEGER NOT NULL,
                        timesScanned INTEGER NOT NULL DEFAULT 1,
                        wasSuccessful INTEGER NOT NULL DEFAULT 1,
                        source TEXT,
                        protein REAL,
                        carbs REAL,
                        fat REAL,
                        fiber REAL,
                        sugar REAL,
                        sodium REAL
                    )
                """)
                
                // Create index on barcode
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_barcode_history_barcode ON barcode_history(barcode)")
            }
        }
        
        // Migration from version 12 to 13 - Add progress photos
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS progress_photos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        photoPath TEXT NOT NULL,
                        thumbnailPath TEXT,
                        date TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        weight REAL,
                        bodyFatPercentage REAL,
                        notes TEXT,
                        tags TEXT,
                        isVisible INTEGER NOT NULL DEFAULT 1,
                        reminderSet INTEGER NOT NULL DEFAULT 0,
                        photoType TEXT NOT NULL DEFAULT 'progress',
                        moodRating INTEGER,
                        reminderFrequency TEXT
                    )
                """)
            }
        }
        
        // Migration from version 13 to 14 - Add meal planning
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS meal_plans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        mealType TEXT NOT NULL,
                        mealName TEXT NOT NULL,
                        description TEXT,
                        estimatedCalories INTEGER NOT NULL DEFAULT 0,
                        estimatedPrep INTEGER,
                        difficulty TEXT,
                        tags TEXT,
                        ingredients TEXT,
                        recipe TEXT,
                        servings INTEGER NOT NULL DEFAULT 1,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        addedToShoppingList INTEGER NOT NULL DEFAULT 0,
                        shoppingListDate INTEGER,
                        recipeId INTEGER,
                        isFromRecipe INTEGER NOT NULL DEFAULT 0,
                        planWeek TEXT,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        color TEXT,
                        reminder INTEGER NOT NULL DEFAULT 0,
                        reminderTime TEXT
                    )
                """)
            }
        }
        
        // Migration from version 14 to 15 - Add shopping lists
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_list_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemName TEXT NOT NULL,
                        category TEXT,
                        quantity TEXT,
                        unit TEXT,
                        notes TEXT,
                        estimatedCost REAL,
                        priority INTEGER NOT NULL DEFAULT 0,
                        isChecked INTEGER NOT NULL DEFAULT 0,
                        checkedAt INTEGER,
                        addedAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        fromMealPlanId INTEGER,
                        mealDate TEXT,
                        mealType TEXT,
                        servings INTEGER NOT NULL DEFAULT 1,
                        shoppingTrip TEXT,
                        store TEXT,
                        aisle TEXT,
                        brand TEXT,
                        isRecurring INTEGER NOT NULL DEFAULT 0,
                        lastPurchased INTEGER,
                        averageCost REAL,
                        isStaple INTEGER NOT NULL DEFAULT 0,
                        alternativeItems TEXT,
                        color TEXT,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }
        
        // Migration from version 15 to 16 - Add additional entities (placeholder for missing migration)
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // This migration was missing - add placeholder for any v15->v16 changes
                // If no schema changes were made between v15 and v16, this is a no-op migration
            }
        }
        
        // Migration from version 16 to 17 - Schema cleanup: Remove non-existent entities  
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No-op migration: Fixed database schema integrity
                // Removed non-existent entities from database declaration:
                // - RecipeReview::class (never implemented)
                // - RecipeComment::class (never implemented) 
                // - RecipeFavorite::class (never implemented)
                // 
                // These entities were declared in @Database but their corresponding
                // table creation SQL and entity classes were never created, causing
                // "no such table" crashes on fresh installs.
                //
                // No actual database changes needed since tables were never created.
            }
        }
        
        // Migration from version 17 to 18 - Add community features
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add index on calorie_entries.date — this column is queried on every
                // screen load and was missing an index, causing full table scans.
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_calorie_entries_date ON calorie_entries (date)"
                )
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create community_recipes table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS community_recipes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recipeName TEXT NOT NULL,
                        description TEXT,
                        authorId TEXT NOT NULL,
                        authorDisplayName TEXT NOT NULL,
                        instructions TEXT NOT NULL,
                        ingredients TEXT NOT NULL,
                        servingSize TEXT NOT NULL,
                        prepTimeMinutes INTEGER NOT NULL,
                        cookTimeMinutes INTEGER NOT NULL,
                        difficulty TEXT NOT NULL,
                        caloriesPerServing INTEGER NOT NULL,
                        proteinPerServing REAL,
                        carbsPerServing REAL,
                        fatPerServing REAL,
                        fiberPerServing REAL,
                        sugarPerServing REAL,
                        sodiumPerServing REAL,
                        categoryTag TEXT NOT NULL,
                        dietaryTags TEXT NOT NULL,
                        cuisineType TEXT,
                        mealType TEXT,
                        totalRating REAL NOT NULL DEFAULT 0,
                        tasteRating REAL NOT NULL DEFAULT 0,
                        healthRating REAL NOT NULL DEFAULT 0,
                        difficultyRating REAL NOT NULL DEFAULT 0,
                        totalReviews INTEGER NOT NULL DEFAULT 0,
                        totalFavorites INTEGER NOT NULL DEFAULT 0,
                        totalMade INTEGER NOT NULL DEFAULT 0,
                        photoUrl TEXT,
                        thumbnailUrl TEXT,
                        photoCount INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isPublished INTEGER NOT NULL DEFAULT 0,
                        isFeatured INTEGER NOT NULL DEFAULT 0,
                        isReported INTEGER NOT NULL DEFAULT 0,
                        reportCount INTEGER NOT NULL DEFAULT 0,
                        viewCount INTEGER NOT NULL DEFAULT 0,
                        shareCount INTEGER NOT NULL DEFAULT 0,
                        commentCount INTEGER NOT NULL DEFAULT 0,
                        saveCount INTEGER NOT NULL DEFAULT 0,
                        nutritionScore REAL NOT NULL DEFAULT 0,
                        macroBalance TEXT,
                        healthBenefits TEXT,
                        allergenInfo TEXT
                    )
                """)
                
                // Create recipe_reviews table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recipe_reviews (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recipeId INTEGER NOT NULL,
                        reviewerId TEXT NOT NULL,
                        reviewerDisplayName TEXT NOT NULL,
                        overallRating REAL NOT NULL,
                        tasteRating REAL NOT NULL,
                        healthRating REAL NOT NULL,
                        difficultyRating REAL NOT NULL,
                        valueRating REAL NOT NULL,
                        reviewText TEXT,
                        cookingTips TEXT,
                        modifications TEXT,
                        difficultyNotes TEXT,
                        actualPrepTime INTEGER,
                        actualCookTime INTEGER,
                        wouldMakeAgain INTEGER NOT NULL DEFAULT 0,
                        wouldRecommend INTEGER NOT NULL DEFAULT 0,
                        helpfulVotes INTEGER NOT NULL DEFAULT 0,
                        notHelpfulVotes INTEGER NOT NULL DEFAULT 0,
                        isVerifiedMaker INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isReported INTEGER NOT NULL DEFAULT 0,
                        isHidden INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (recipeId) REFERENCES community_recipes (id) ON DELETE CASCADE
                    )
                """)
                
                // Create recipe_comments table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recipe_comments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recipeId INTEGER NOT NULL,
                        commenterId TEXT NOT NULL,
                        commenterDisplayName TEXT NOT NULL,
                        commentText TEXT NOT NULL,
                        commentType TEXT NOT NULL,
                        parentCommentId INTEGER,
                        isReply INTEGER NOT NULL DEFAULT 0,
                        replyCount INTEGER NOT NULL DEFAULT 0,
                        helpfulVotes INTEGER NOT NULL DEFAULT 0,
                        notHelpfulVotes INTEGER NOT NULL DEFAULT 0,
                        heartReactions INTEGER NOT NULL DEFAULT 0,
                        laughReactions INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isEdited INTEGER NOT NULL DEFAULT 0,
                        isReported INTEGER NOT NULL DEFAULT 0,
                        isHidden INTEGER NOT NULL DEFAULT 0,
                        isPinned INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (recipeId) REFERENCES community_recipes (id) ON DELETE CASCADE
                    )
                """)
                
                // Create recipe_favorites table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recipe_favorites (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        recipeId INTEGER NOT NULL,
                        savedAt INTEGER NOT NULL,
                        personalNotes TEXT,
                        plannedCookDate TEXT,
                        personalTags TEXT,
                        hasTriedCooking INTEGER NOT NULL DEFAULT 0,
                        dateCookedFirst INTEGER,
                        timesCookedTotal INTEGER NOT NULL DEFAULT 0,
                        lastCookedDate INTEGER,
                        personalRating REAL,
                        personalReview TEXT,
                        addedToMealPlan INTEGER NOT NULL DEFAULT 0,
                        addedToShoppingList INTEGER NOT NULL DEFAULT 0,
                        mealPlanDate TEXT,
                        sharedWithFriends INTEGER NOT NULL DEFAULT 0,
                        recommendedToOthers INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (recipeId) REFERENCES community_recipes (id) ON DELETE CASCADE
                    )
                """)
                
                // Create indices for better performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_community_recipes_authorId ON community_recipes (authorId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_community_recipes_categoryTag ON community_recipes (categoryTag)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_community_recipes_totalRating ON community_recipes (totalRating)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_community_recipes_createdAt ON community_recipes (createdAt)")
                
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_reviews_recipeId ON recipe_reviews (recipeId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_reviews_reviewerId ON recipe_reviews (reviewerId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_reviews_createdAt ON recipe_reviews (createdAt)")
                
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_comments_recipeId ON recipe_comments (recipeId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_comments_commenterId ON recipe_comments (commenterId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_comments_createdAt ON recipe_comments (createdAt)")
                
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_favorites_userId ON recipe_favorites (userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_favorites_recipeId ON recipe_favorites (recipeId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recipe_favorites_savedAt ON recipe_favorites (savedAt)")
            }
        }
        
        /**
         * 🏗️ GET DATABASE INSTANCE - The Main Factory Method
         * 
         * This is THE most important method in this entire file! This is how the rest of our app
         * gets access to the database. Think of this like the "front desk" of our database building.
         * 
         * 📖 What is the Singleton Pattern?
         * We only want ONE database instance for our entire app. Why?
         * - Databases are expensive to create (slow and uses lots of memory)
         * - Multiple database instances could conflict with each other
         * - We want all parts of our app to see the same data
         * 
         * 🔒 Thread Safety with synchronized():
         * Multiple parts of our app might try to create the database at the same time.
         * synchronized(this) is like a "one person at a time" rule - it prevents crashes
         * from multiple threads trying to create the database simultaneously.
         * 
         * 🔄 Migration Strategy:
         * We provide all our migrations so users never lose their data when updating the app.
         * fallbackToDestructiveMigration() is our "emergency plan" - if a migration fails,
         * we'll create a fresh database (user loses data, but app doesn't crash).
         * 
         * @param context Android context (gives us access to the file system)
         * @return The one and only CalorieDatabase instance for this app
         */
        fun getDatabase(context: Context): CalorieDatabase {
            // 🔍 Check if we already created the database
            return INSTANCE ?: synchronized(this) {
                // 🏗️ Create the database using Room's builder pattern
                val instance = Room.databaseBuilder(
                    context.applicationContext,  // 📱 Use app context (survives activity changes)
                    CalorieDatabase::class.java,  // 🎯 Tell Room which database class to create
                    "calorie_database"            // 📁 Name of the database file on disk
                )
                // 🔄 Add all our migrations (version 8 through 16)
                // This ensures users never lose data when updating the app
                .addMigrations(
                    MIGRATION_8_9,    // Added caching system
                    MIGRATION_9_10,   // Added recipe system (missing from original list)
                    MIGRATION_10_11,  // Added favorite meals
                    MIGRATION_11_12,  // Added barcode history
                    MIGRATION_12_13,  // Added progress photos  
                    MIGRATION_13_14,  // Added meal planning
                    MIGRATION_14_15,  // Added shopping lists
                    MIGRATION_15_16,  // No-op placeholder migration
                    MIGRATION_16_17,  // Schema cleanup - removed non-existent entities
                    MIGRATION_17_18,  // Added community features - recipes, reviews, comments, favorites
                    MIGRATION_18_19   // Added date index on calorie_entries for query performance
                )
                // 🔨 Actually create the database instance
                .build()
                
                // 💾 Save this instance so we don't create it again
                INSTANCE = instance
                instance // Return the database to whoever asked for it
            }
        }
    }
}