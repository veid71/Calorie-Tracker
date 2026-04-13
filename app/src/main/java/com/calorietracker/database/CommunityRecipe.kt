package com.calorietracker.database

// 🧰 DATABASE TOOLS
import androidx.room.*      // Database annotations and tools
import androidx.lifecycle.LiveData  // Auto-updating data for UI
import kotlinx.coroutines.flow.Flow // Modern async data streams

/**
 * 🍽️ COMMUNITY RECIPE - SHARED HEALTHY MEALS
 * 
 * Hey young programmer! This represents a recipe that someone in the CalorieTracker
 * community has shared for others to try.
 * 
 * 🎯 What makes a great community recipe?
 * - 📊 **Detailed nutrition info**: Accurate calories, protein, carbs, fat
 * - 👨‍🍳 **Clear instructions**: Step-by-step cooking directions
 * - 📸 **Appetizing photos**: Make people want to try it!
 * - ⭐ **Community ratings**: Other users rate taste and healthiness
 * - 🏷️ **Smart tags**: "High Protein", "Low Carb", "Kid Friendly", "Quick & Easy"
 * 
 * 🌟 Community Features:
 * - Users can "favorite" recipes they want to try
 * - Rate recipes on taste (1-5 stars) and healthiness (1-5 stars)
 * - Leave helpful comments and cooking tips
 * - Share modifications: "I added spinach for extra fiber!"
 * - Report inappropriate content to keep community friendly
 * 
 * 🔒 Privacy & Safety:
 * - All recipes are moderated before going live
 * - Users can report inappropriate content
 * - No personal information shared (just usernames)
 * - Family-friendly content only
 */
@Entity(
    tableName = "community_recipes",
    indices = [
        Index(value = ["authorId"], unique = false),
        Index(value = ["categoryTag"], unique = false),
        Index(value = ["totalRating"], unique = false),
        Index(value = ["createdAt"], unique = false)
    ]
)
data class CommunityRecipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                           // 🆔 Unique recipe ID
    
    // 👨‍🍳 RECIPE BASICS
    val recipeName: String,                     // 🍽️ "Mediterranean Chickpea Bowl"
    val description: String,                    // 📝 "Protein-packed bowl perfect for lunch"
    val authorId: String,                       // 👤 Anonymous user ID (like "user_12345")
    val authorDisplayName: String,              // 📛 "HealthyEater23" (chosen username)
    
    // 🥘 RECIPE DETAILS
    val instructions: String,                   // 👨‍🍳 Step-by-step cooking directions
    val ingredients: String,                    // 🛒 JSON list of ingredients with amounts
    val servingSize: String,                    // 🍽️ "1 bowl" or "4 servings"
    val prepTimeMinutes: Int,                   // ⏰ How long to prepare
    val cookTimeMinutes: Int,                   // 🔥 How long to cook
    val difficulty: String,                     // 🎯 "Easy", "Medium", "Hard"
    
    // 📊 NUTRITION INFORMATION
    val caloriesPerServing: Int,                // 🔥 Calories in one serving
    val proteinPerServing: Double?,             // 💪 Protein grams per serving
    val carbsPerServing: Double?,               // 🍞 Carb grams per serving
    val fatPerServing: Double?,                 // 🥑 Fat grams per serving
    val fiberPerServing: Double?,               // 🌾 Fiber grams per serving
    val sugarPerServing: Double?,               // 🍯 Sugar grams per serving
    val sodiumPerServing: Double?,              // 🧂 Sodium milligrams per serving
    
    // 🏷️ CATEGORIZATION AND TAGS
    val categoryTag: String,                    // 🏷️ "Breakfast", "Lunch", "Dinner", "Snack", "Dessert"
    val dietaryTags: String,                    // 🌱 JSON array: ["Vegetarian", "High Protein", "Low Carb"]
    val cuisineType: String?,                   // 🌍 "Italian", "Mexican", "Asian", "American"
    val mealType: String?,                      // 🍽️ "Main Dish", "Side Dish", "Appetizer", "Dessert"
    
    // ⭐ COMMUNITY RATINGS
    val totalRating: Float = 0f,                // ⭐ Average rating (1.0 to 5.0 stars)
    val tasteRating: Float = 0f,                // 😋 How good does it taste? (1-5 stars)
    val healthRating: Float = 0f,               // 🥗 How healthy is it? (1-5 stars)
    val difficultyRating: Float = 0f,           // 🎯 How hard to make? (1-5 stars)
    val totalReviews: Int = 0,                  // 📊 How many people rated this recipe
    val totalFavorites: Int = 0,                // ⭐ How many people saved this recipe
    val totalMade: Int = 0,                     // 👨‍🍳 How many people actually cooked this
    
    // 📸 VISUAL CONTENT
    val photoUrl: String?,                      // 📸 Main recipe photo URL
    val thumbnailUrl: String?,                  // 🖼️ Small preview image URL
    val photoCount: Int = 0,                    // 📸 Total photos uploaded for this recipe
    
    // 📅 METADATA
    val createdAt: Long = System.currentTimeMillis(),  // 📅 When recipe was first shared
    val updatedAt: Long = System.currentTimeMillis(),  // 🔄 When recipe was last modified
    val isPublished: Boolean = false,           // 📢 Is recipe live in community feed?
    val isFeatured: Boolean = false,            // 🌟 Featured recipe of the week?
    val isReported: Boolean = false,            // 🚨 Has been reported for review
    val reportCount: Int = 0,                   // 📊 Number of reports (for moderation)
    
    // 📊 ENGAGEMENT METRICS
    val viewCount: Int = 0,                     // 👀 How many times recipe was viewed
    val shareCount: Int = 0,                    // 📱 How many times recipe was shared
    val commentCount: Int = 0,                  // 💬 Number of comments on recipe
    val saveCount: Int = 0,                     // 💾 Times saved to personal recipe box
    
    // 🎯 NUTRITION SCORE
    val nutritionScore: Float = 0f,             // 📊 AI-calculated nutrition quality (0-100)
    val macroBalance: String?,                  // 📈 "Balanced", "High Protein", "Low Carb"
    val healthBenefits: String?,                // 💚 JSON list of health benefits
    val allergenInfo: String?                   // ⚠️ "Contains nuts, dairy" for safety
)

/**
 * 💬 RECIPE REVIEW - USER FEEDBACK ON COMMUNITY RECIPES
 * 
 * When someone tries a community recipe, they can leave a review to help others.
 */
@Entity(
    tableName = "recipe_reviews",
    foreignKeys = [
        ForeignKey(
            entity = CommunityRecipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recipeId"], unique = false),
        Index(value = ["reviewerId"], unique = false),
        Index(value = ["createdAt"], unique = false)
    ]
)
data class RecipeReview(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                           // 🆔 Unique review ID
    
    val recipeId: Long,                         // 🍽️ Which recipe this review is for
    val reviewerId: String,                     // 👤 Anonymous reviewer ID
    val reviewerDisplayName: String,            // 📛 Reviewer's chosen username
    
    // ⭐ RATINGS
    val overallRating: Float,                   // ⭐ Overall rating (1-5 stars)
    val tasteRating: Float,                     // 😋 How tasty? (1-5 stars)
    val healthRating: Float,                    // 🥗 How healthy? (1-5 stars)
    val difficultyRating: Float,                // 🎯 How difficult to make? (1-5 stars)
    val valueRating: Float,                     // 💰 Good value for ingredients cost? (1-5 stars)
    
    // 💬 FEEDBACK
    val reviewText: String?,                    // 💬 "Delicious! I added extra garlic."
    val cookingTips: String?,                   // 💡 "Cook quinoa separately for better texture"
    val modifications: String?,                 // 🔄 "Swapped chicken for tofu, still great!"
    val difficultyNotes: String?,               // 📝 "Easy to make but prep takes 30 mins"
    
    // 👨‍🍳 COOKING EXPERIENCE
    val actualPrepTime: Int?,                   // ⏰ How long prep actually took
    val actualCookTime: Int?,                   // 🔥 How long cooking actually took
    val wouldMakeAgain: Boolean = false,        // 🔄 Would you cook this again?
    val wouldRecommend: Boolean = false,        // 👍 Would you recommend to friends?
    
    // 📊 HELPFULNESS
    val helpfulVotes: Int = 0,                  // 👍 How many found this review helpful
    val notHelpfulVotes: Int = 0,               // 👎 How many found this unhelpful
    val isVerifiedMaker: Boolean = false,       // ✅ Did they actually cook it? (tracked by app)
    
    // 📅 METADATA
    val createdAt: Long = System.currentTimeMillis(),  // 📅 When review was posted
    val updatedAt: Long = System.currentTimeMillis(),  // 🔄 When review was last edited
    val isReported: Boolean = false,            // 🚨 Has been reported for inappropriate content
    val isHidden: Boolean = false               // 👻 Hidden by moderators
)

/**
 * ⭐ RECIPE FAVORITE - USER'S SAVED RECIPES
 * 
 * When users find recipes they want to try, they save them to their personal collection.
 */
@Entity(
    tableName = "recipe_favorites",
    foreignKeys = [
        ForeignKey(
            entity = CommunityRecipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"], unique = false),
        Index(value = ["recipeId"], unique = false),
        Index(value = ["savedAt"], unique = false)
    ]
)
data class RecipeFavorite(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                           // 🆔 Unique favorite ID
    
    val userId: String,                         // 👤 User who saved this recipe
    val recipeId: Long,                         // 🍽️ Recipe they saved
    
    // 📅 SAVING INFO
    val savedAt: Long = System.currentTimeMillis(),    // 📅 When they saved it
    val personalNotes: String?,                 // 📝 Personal notes: "Want to try with salmon"
    val plannedCookDate: String?,               // 📅 "2024-09-15" - when they plan to cook it
    val personalTags: String?,                  // 🏷️ JSON: ["Sunday Dinner", "Date Night"]
    
    // 👨‍🍳 COOKING STATUS
    val hasTriedCooking: Boolean = false,       // 🍳 Have they actually made this recipe?
    val dateCookedFirst: Long? = null,          // 📅 When they first cooked it
    val timesCookedTotal: Int = 0,              // 🔄 How many times they've made it
    val lastCookedDate: Long? = null,           // 📅 Most recent time they cooked it
    
    // 📊 PERSONAL RATING
    val personalRating: Float? = null,          // ⭐ Their personal rating after cooking
    val personalReview: String?,                // 💬 Their personal notes after cooking
    
    // 🛒 MEAL PLANNING
    val addedToMealPlan: Boolean = false,       // 📅 Added to weekly meal plan?
    val addedToShoppingList: Boolean = false,   // 🛒 Ingredients added to shopping list?
    val mealPlanDate: String?,                  // 📅 Which day it's planned for
    
    // 📱 ENGAGEMENT
    val sharedWithFriends: Boolean = false,     // 📱 Did they share this recipe?
    val recommendedToOthers: Int = 0            // 👍 How many people they recommended it to
)

/**
 * 💬 RECIPE COMMENT - COMMUNITY DISCUSSION
 * 
 * Users can comment on recipes to share tips, ask questions, or give feedback.
 */
@Entity(
    tableName = "recipe_comments",
    foreignKeys = [
        ForeignKey(
            entity = CommunityRecipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recipeId"], unique = false),
        Index(value = ["commenterId"], unique = false),
        Index(value = ["createdAt"], unique = false)
    ]
)
data class RecipeComment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                           // 🆔 Unique comment ID
    
    val recipeId: Long,                         // 🍽️ Which recipe this comment is on
    val commenterId: String,                    // 👤 Who wrote this comment
    val commenterDisplayName: String,           // 📛 Commenter's username
    
    // 💬 COMMENT CONTENT
    val commentText: String,                    // 💬 "Great recipe! I added extra herbs."
    val commentType: String,                    // 🏷️ "tip", "question", "review", "modification"
    
    // 🔗 THREADED CONVERSATIONS
    val parentCommentId: Long? = null,          // 💬 Replying to another comment?
    val isReply: Boolean = false,               // 💬 Is this a reply to another comment?
    val replyCount: Int = 0,                    // 📊 How many replies to this comment
    
    // 👍 COMMUNITY FEEDBACK
    val helpfulVotes: Int = 0,                  // 👍 People found this helpful
    val notHelpfulVotes: Int = 0,               // 👎 People found this not helpful
    val heartReactions: Int = 0,                // ❤️ Love reactions
    val laughReactions: Int = 0,                // 😂 Funny reactions
    
    // 📅 METADATA
    val createdAt: Long = System.currentTimeMillis(),  // 📅 When comment was posted
    val updatedAt: Long = System.currentTimeMillis(),  // 🔄 When comment was last edited
    val isEdited: Boolean = false,              // ✏️ Has this comment been edited?
    val isReported: Boolean = false,            // 🚨 Reported for inappropriate content
    val isHidden: Boolean = false,              // 👻 Hidden by moderators
    val isPinned: Boolean = false               // 📌 Pinned by recipe author as helpful
)

/**
 * 🗄️ COMMUNITY RECIPE DAO - DATABASE ACCESS FOR RECIPES
 * 
 * This handles all database operations for community recipes.
 */
@Dao
interface CommunityRecipeDao {
    
    // ➕ CREATE OPERATIONS
    @Insert
    suspend fun insertRecipe(recipe: CommunityRecipe): Long
    
    @Insert
    suspend fun insertRecipes(recipes: List<CommunityRecipe>)
    
    // NOTE: Review, Comment, and Favorite functionality temporarily removed
    // due to missing entity implementations. Can be re-added when entities are created.
    
    // @Insert
    // suspend fun insertReview(review: RecipeReview): Long
    
    // @Insert  
    // suspend fun insertComment(comment: RecipeComment): Long
    
    // @Insert
    // suspend fun insertFavorite(favorite: RecipeFavorite): Long
    
    // 📖 READ OPERATIONS
    @Query("SELECT * FROM community_recipes WHERE isPublished = 1 ORDER BY totalRating DESC, createdAt DESC")
    fun getAllPublishedRecipes(): LiveData<List<CommunityRecipe>>
    
    @Query("SELECT * FROM community_recipes WHERE isPublished = 1 ORDER BY totalRating DESC, createdAt DESC LIMIT :limit")
    suspend fun getTopRatedRecipes(limit: Int = 20): List<CommunityRecipe>
    
    @Query("SELECT * FROM community_recipes WHERE isPublished = 1 ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getNewestRecipes(limit: Int = 20): List<CommunityRecipe>
    
    @Query("SELECT * FROM community_recipes WHERE isFeatured = 1 AND isPublished = 1")
    suspend fun getFeaturedRecipes(): List<CommunityRecipe>
    
    @Query("SELECT * FROM community_recipes WHERE categoryTag = :category AND isPublished = 1 ORDER BY totalRating DESC")
    suspend fun getRecipesByCategory(category: String): List<CommunityRecipe>
    
    @Query("SELECT * FROM community_recipes WHERE recipeName LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' AND isPublished = 1")
    suspend fun searchRecipes(query: String): List<CommunityRecipe>
    
    @Query("SELECT * FROM community_recipes WHERE id = :recipeId")
    suspend fun getRecipeById(recipeId: Long): CommunityRecipe?
    
    @Query("SELECT * FROM community_recipes WHERE authorId = :authorId ORDER BY createdAt DESC")
    suspend fun getRecipesByAuthor(authorId: String): List<CommunityRecipe>
    
    // 🔍 ADVANCED SEARCH
    @Query("""
        SELECT * FROM community_recipes 
        WHERE isPublished = 1 
        AND caloriesPerServing BETWEEN :minCalories AND :maxCalories
        AND (:proteinMin IS NULL OR proteinPerServing >= :proteinMin)
        AND (:cookTimeMax IS NULL OR cookTimeMinutes <= :cookTimeMax)
        AND (:dietaryTag IS NULL OR dietaryTags LIKE '%' || :dietaryTag || '%')
        ORDER BY totalRating DESC
    """)
    suspend fun searchRecipesAdvanced(
        minCalories: Int = 0,
        maxCalories: Int = 5000,
        proteinMin: Double? = null,
        cookTimeMax: Int? = null,
        dietaryTag: String? = null
    ): List<CommunityRecipe>
    
    // ⭐ RATING AND REVIEW OPERATIONS - TEMPORARILY DISABLED
    // NOTE: These operations disabled due to missing RecipeReview entity
    /*
    @Query("SELECT * FROM recipe_reviews WHERE recipeId = :recipeId ORDER BY createdAt DESC")
    suspend fun getReviewsForRecipe(recipeId: Long): List<RecipeReview>
    
    @Query("SELECT * FROM recipe_reviews WHERE recipeId = :recipeId AND reviewerId = :reviewerId")
    suspend fun getUserReviewForRecipe(recipeId: Long, reviewerId: String): RecipeReview?
    
    @Query("SELECT AVG(overallRating) FROM recipe_reviews WHERE recipeId = :recipeId")
    suspend fun getAverageRatingForRecipe(recipeId: Long): Float?
    
    @Query("SELECT COUNT(*) FROM recipe_reviews WHERE recipeId = :recipeId")
    suspend fun getReviewCountForRecipe(recipeId: Long): Int
    */
    
    // 💬 COMMENT OPERATIONS - TEMPORARILY DISABLED
    // NOTE: These operations disabled due to missing RecipeComment entity
    /*
    @Query("SELECT * FROM recipe_comments WHERE recipeId = :recipeId AND parentCommentId IS NULL ORDER BY createdAt DESC")
    suspend fun getTopLevelCommentsForRecipe(recipeId: Long): List<RecipeComment>
    
    @Query("SELECT * FROM recipe_comments WHERE parentCommentId = :parentId ORDER BY createdAt ASC")
    suspend fun getRepliesForComment(parentId: Long): List<RecipeComment>
    
    @Query("SELECT COUNT(*) FROM recipe_comments WHERE recipeId = :recipeId")
    suspend fun getCommentCountForRecipe(recipeId: Long): Int
    */
    
    // ⭐ FAVORITE OPERATIONS - TEMPORARILY DISABLED
    // NOTE: These operations disabled due to missing RecipeFavorite entity
    /*
    @Query("SELECT * FROM recipe_favorites WHERE userId = :userId ORDER BY savedAt DESC")
    suspend fun getUserFavorites(userId: String): List<RecipeFavorite>
    
    @Query("SELECT * FROM recipe_favorites WHERE userId = :userId AND recipeId = :recipeId")
    suspend fun getUserFavoriteForRecipe(userId: String, recipeId: Long): RecipeFavorite?
    
    @Query("SELECT COUNT(*) FROM recipe_favorites WHERE recipeId = :recipeId")
    suspend fun getFavoriteCountForRecipe(recipeId: Long): Int
    */
    
    // 🔄 UPDATE OPERATIONS
    @Update
    suspend fun updateRecipe(recipe: CommunityRecipe)
    
    @Update
    suspend fun updateReview(review: RecipeReview)
    
    @Update
    suspend fun updateComment(comment: RecipeComment)
    
    @Update
    suspend fun updateFavorite(favorite: RecipeFavorite)
    
    // 📊 UPDATE RECIPE STATS
    @Query("UPDATE community_recipes SET totalRating = :rating, totalReviews = :reviewCount WHERE id = :recipeId")
    suspend fun updateRecipeRating(recipeId: Long, rating: Float, reviewCount: Int)
    
    @Query("UPDATE community_recipes SET totalFavorites = :favoriteCount WHERE id = :recipeId")
    suspend fun updateRecipeFavoriteCount(recipeId: Long, favoriteCount: Int)
    
    @Query("UPDATE community_recipes SET viewCount = viewCount + 1 WHERE id = :recipeId")
    suspend fun incrementRecipeViewCount(recipeId: Long)
    
    @Query("UPDATE community_recipes SET shareCount = shareCount + 1 WHERE id = :recipeId")
    suspend fun incrementRecipeShareCount(recipeId: Long)
    
    // Temporarily disabled due to missing RecipeComment entity
    /*
    @Query("UPDATE recipe_comments SET helpfulVotes = helpfulVotes + 1 WHERE id = :commentId")
    suspend fun incrementCommentHelpfulVotes(commentId: Long)
    
    @Query("UPDATE recipe_comments SET notHelpfulVotes = notHelpfulVotes + 1 WHERE id = :commentId")
    suspend fun incrementCommentNotHelpfulVotes(commentId: Long)
    */
    
    // 🗑️ DELETE OPERATIONS
    @Delete
    suspend fun deleteRecipe(recipe: CommunityRecipe)
    
    // Temporarily disabled due to missing entities
    /*
    @Delete
    suspend fun deleteReview(review: RecipeReview)
    
    @Delete
    suspend fun deleteComment(comment: RecipeComment)
    
    @Delete
    suspend fun deleteFavorite(favorite: RecipeFavorite)
    */
    
    // 🧹 CLEANUP OPERATIONS
    @Query("DELETE FROM community_recipes WHERE isReported = 1 AND reportCount > 10")
    suspend fun cleanupReportedRecipes()
    
    // Temporarily disabled due to missing RecipeComment entity
    /*
    @Query("DELETE FROM recipe_comments WHERE isReported = 1 AND createdAt < :cutoffTime")
    suspend fun cleanupOldReportedComments(cutoffTime: Long)
    */
    
    // 📊 ANALYTICS QUERIES
    @Query("SELECT categoryTag as category, COUNT(*) as count FROM community_recipes WHERE isPublished = 1 GROUP BY categoryTag ORDER BY count DESC")
    suspend fun getRecipeCountsByCategory(): List<CategoryCount>
    
    @Query("SELECT authorDisplayName, COUNT(*) as recipeCount FROM community_recipes WHERE isPublished = 1 GROUP BY authorId ORDER BY recipeCount DESC LIMIT :limit")
    suspend fun getTopContributors(limit: Int = 10): List<AuthorStats>
    
    @Query("SELECT * FROM community_recipes WHERE isPublished = 1 AND createdAt >= :since ORDER BY totalRating DESC LIMIT :limit")
    suspend fun getTrendingRecipes(since: Long, limit: Int = 10): List<CommunityRecipe>
}

/**
 * 📊 HELPER DATA CLASSES FOR ANALYTICS
 */
data class AuthorStats(
    val authorDisplayName: String,  // 👤 Author's display name
    val recipeCount: Int           // 📊 Number of recipes they've shared
)

data class CategoryCount(
    val category: String,          // 🏷️ Recipe category name
    val count: Int                // 📊 Number of recipes in this category
)