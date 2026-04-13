package com.calorietracker.database

import androidx.room.*
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for shopping list operations
 * Handles shopping list generation from meal plans and manual management
 */
@Dao
interface ShoppingListDao {
    
    @Query("SELECT * FROM shopping_list_items WHERE isChecked = 0 ORDER BY category, sortOrder, itemName")
    fun getActiveShoppingList(): LiveData<List<ShoppingListItem>>
    
    @Query("SELECT * FROM shopping_list_items WHERE isChecked = 0 ORDER BY category, sortOrder, itemName")
    fun getActiveShoppingListFlow(): Flow<List<ShoppingListItem>>
    
    @Query("SELECT * FROM shopping_list_items WHERE isChecked = 0 ORDER BY category, sortOrder, itemName")
    suspend fun getActiveShoppingListSync(): List<ShoppingListItem>
    
    @Query("SELECT * FROM shopping_list_items ORDER BY addedAt DESC")
    fun getAllShoppingItems(): LiveData<List<ShoppingListItem>>
    
    @Query("SELECT * FROM shopping_list_items WHERE category = :category ORDER BY sortOrder, itemName")
    suspend fun getItemsByCategory(category: String): List<ShoppingListItem>
    
    @Query("SELECT * FROM shopping_list_items WHERE shoppingTrip = :tripName ORDER BY category, sortOrder, itemName")
    suspend fun getItemsByTrip(tripName: String): List<ShoppingListItem>
    
    @Query("SELECT * FROM shopping_list_items WHERE itemName LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%'")
    suspend fun searchShoppingItems(query: String): List<ShoppingListItem>
    
    @Query("SELECT * FROM shopping_list_items WHERE fromMealPlanId = :mealPlanId")
    suspend fun getItemsFromMealPlan(mealPlanId: Long): List<ShoppingListItem>
    
    @Query("SELECT * FROM shopping_list_items WHERE mealDate = :date")
    suspend fun getItemsForMealDate(date: String): List<ShoppingListItem>
    
    @Query("SELECT * FROM shopping_list_items WHERE id = :id")
    suspend fun getShoppingItemById(id: Long): ShoppingListItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingListItem): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItems(items: List<ShoppingListItem>)
    
    @Update
    suspend fun updateShoppingItem(item: ShoppingListItem)
    
    @Delete
    suspend fun deleteShoppingItem(item: ShoppingListItem)
    
    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteShoppingItemById(id: Long)
    
    @Query("DELETE FROM shopping_list_items WHERE fromMealPlanId = :mealPlanId")
    suspend fun deleteItemsFromMealPlan(mealPlanId: Long)
    
    // Checking off items
    @Query("UPDATE shopping_list_items SET isChecked = :checked, checkedAt = :checkedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateItemCheckedStatus(id: Long, checked: Boolean, checkedAt: Long?, updatedAt: Long)
    
    @Query("UPDATE shopping_list_items SET isChecked = 1, checkedAt = :checkedAt, updatedAt = :updatedAt WHERE category = :category AND isChecked = 0")
    suspend fun checkOffCategory(category: String, checkedAt: Long, updatedAt: Long)
    
    @Query("UPDATE shopping_list_items SET isChecked = 0, checkedAt = NULL, updatedAt = :updatedAt")
    suspend fun uncheckAllItems(updatedAt: Long)
    
    // Shopping trip management
    @Query("UPDATE shopping_list_items SET shoppingTrip = :tripName, updatedAt = :updatedAt WHERE id IN (:itemIds)")
    suspend fun assignItemsToTrip(itemIds: List<Long>, tripName: String, updatedAt: Long)
    
    @Query("SELECT DISTINCT shoppingTrip FROM shopping_list_items WHERE shoppingTrip IS NOT NULL ORDER BY shoppingTrip")
    suspend fun getAllShoppingTrips(): List<String>
    
    // Category management
    @Query("SELECT DISTINCT category FROM shopping_list_items WHERE category IS NOT NULL ORDER BY category")
    suspend fun getAllCategories(): List<String>
    
    @Query("UPDATE shopping_list_items SET category = :newCategory, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateItemCategory(id: Long, newCategory: String, updatedAt: Long)
    
    // Statistics and analytics
    @Query("SELECT COUNT(*) FROM shopping_list_items WHERE isChecked = 0")
    suspend fun getActiveItemCount(): Int
    
    @Query("SELECT COUNT(*) FROM shopping_list_items WHERE isChecked = 1 AND checkedAt >= :since")
    suspend fun getCompletedItemCountSince(since: Long): Int
    
    @Query("SELECT SUM(estimatedCost) FROM shopping_list_items WHERE isChecked = 0 AND estimatedCost IS NOT NULL")
    suspend fun getEstimatedTotalCost(): Double?
    
    @Query("SELECT SUM(estimatedCost) FROM shopping_list_items WHERE isChecked = 1 AND checkedAt >= :since AND estimatedCost IS NOT NULL")
    suspend fun getActualSpentSince(since: Long): Double?
    
    @Query("SELECT itemName, COUNT(*) as frequency FROM shopping_list_items WHERE lastPurchased IS NOT NULL GROUP BY itemName ORDER BY frequency DESC LIMIT :limit")
    suspend fun getMostFrequentItems(limit: Int = 20): List<ItemFrequency>
    
    @Query("SELECT AVG(estimatedCost) FROM shopping_list_items WHERE itemName = :itemName AND estimatedCost IS NOT NULL")
    suspend fun getAverageCostForItem(itemName: String): Double?
    
    // Recurring items and staples
    @Query("SELECT * FROM shopping_list_items WHERE isStaple = 1 ORDER BY itemName")
    suspend fun getStapleItems(): List<ShoppingListItem>
    
    @Query("SELECT * FROM shopping_list_items WHERE isRecurring = 1 ORDER BY lastPurchased ASC")
    suspend fun getRecurringItems(): List<ShoppingListItem>
    
    @Query("UPDATE shopping_list_items SET isStaple = :isStaple, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStapleStatus(id: Long, isStaple: Boolean, updatedAt: Long)
    
    @Query("UPDATE shopping_list_items SET isRecurring = :isRecurring, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRecurringStatus(id: Long, isRecurring: Boolean, updatedAt: Long)
    
    // Bulk operations
    @Query("DELETE FROM shopping_list_items WHERE isChecked = 1 AND checkedAt < :cutoffTime")
    suspend fun cleanupOldCheckedItems(cutoffTime: Long)
    
    @Query("UPDATE shopping_list_items SET sortOrder = :newOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, newOrder: Int)
    
    // Smart suggestions
    @Query("SELECT * FROM shopping_list_items WHERE itemName LIKE :partialName AND lastPurchased IS NOT NULL ORDER BY lastPurchased DESC LIMIT 5")
    suspend fun getSimilarItems(partialName: String): List<ShoppingListItem>
    
    @Query("SELECT DISTINCT store FROM shopping_list_items WHERE store IS NOT NULL ORDER BY store")
    suspend fun getAllStores(): List<String>
    
    @Query("SELECT DISTINCT brand FROM shopping_list_items WHERE brand IS NOT NULL ORDER BY brand")
    suspend fun getAllBrands(): List<String>
}

/**
 * Data class for item frequency analytics
 */
data class ItemFrequency(
    val itemName: String,
    val frequency: Int
)