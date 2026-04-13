package com.calorietracker.database

// 🏗️ ROOM DATABASE TOOLS
import androidx.lifecycle.LiveData    // Data that automatically updates UI
import androidx.room.*               // Database annotations and tools
import kotlinx.coroutines.flow.Flow  // Reactive data streams

/**
 * 🗃️ MEAL TEMPLATE DAO - THE TEMPLATE FILING SYSTEM
 * 
 * Hey young programmer! This is like a filing clerk who specializes in meal templates.
 * 
 * 🎯 What does this DAO do?
 * DAO stands for "Data Access Object" - it's like having a specialized assistant
 * who knows exactly how to save, find, and organize meal templates in our database.
 * 
 * 📋 Template Operations:
 * - Save new meal templates (like "My Typical Breakfast")
 * - Find templates by meal type (all breakfast templates)
 * - Get recently used templates (show favorites first)
 * - Update template usage statistics
 * - Delete templates the user no longer wants
 * 
 * 🔍 Smart Features:
 * - Search templates by name ("find my breakfast templates")
 * - Sort by popularity (most-used templates first)
 * - Track usage statistics for better recommendations
 */
@Dao
interface MealTemplateDao {
    
    /**
     * 💾 SAVE A NEW MEAL TEMPLATE
     * 
     * When user creates a new template like "My Typical Lunch", this saves it.
     * 
     * @param template The meal template to save
     * @return The unique ID assigned to this template
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MealTemplate): Long
    
    /**
     * 📝 SAVE MULTIPLE TEMPLATE ITEMS AT ONCE
     * 
     * When creating a template with multiple foods (like coffee + toast + eggs),
     * this efficiently saves all the foods in one operation.
     * 
     * @param items List of foods that belong to this template
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateItems(items: List<MealTemplateItem>)
    
    /**
     * 📋 GET ALL ACTIVE MEAL TEMPLATES
     * 
     * Returns all meal templates that are still active (not deleted),
     * sorted by most recently used first.
     * 
     * @return LiveData list that automatically updates when templates change
     */
    @Query("SELECT * FROM meal_templates WHERE isActive = 1 ORDER BY lastUsed DESC, useCount DESC")
    fun getAllActiveTemplates(): LiveData<List<MealTemplate>>
    
    /**
     * 🍽️ GET TEMPLATES BY MEAL TYPE
     * 
     * Find all templates for a specific meal (like all breakfast templates).
     * Useful for showing relevant templates at appropriate times.
     * 
     * @param mealType The meal type ("breakfast", "lunch", "dinner", "snack")
     * @return LiveData list of matching templates
     */
    @Query("SELECT * FROM meal_templates WHERE mealType = :mealType AND isActive = 1 ORDER BY useCount DESC, lastUsed DESC")
    fun getTemplatesByMealType(mealType: String): LiveData<List<MealTemplate>>
    
    /**
     * 🔍 SEARCH TEMPLATES BY NAME
     * 
     * Find templates that contain specific words in their name.
     * Like searching for "breakfast" to find all morning meal templates.
     * 
     * @param query Search terms
     * @return List of matching templates
     */
    @Query("SELECT * FROM meal_templates WHERE templateName LIKE '%' || :query || '%' AND isActive = 1 ORDER BY useCount DESC")
    suspend fun searchTemplates(query: String): List<MealTemplate>
    
    /**
     * 🍎 GET ALL FOODS IN A MEAL TEMPLATE
     * 
     * When user selects a template, this gets all the individual food items
     * that make up that template (like coffee + toast + eggs for breakfast).
     * 
     * @param templateId Which template to get foods for
     * @return List of foods in display order
     */
    @Query("SELECT * FROM meal_template_items WHERE templateId = :templateId ORDER BY sortOrder ASC")
    suspend fun getTemplateItems(templateId: Long): List<MealTemplateItem>
    
    /**
     * 📊 UPDATE TEMPLATE USAGE STATISTICS
     * 
     * When user uses a template, this updates the usage count and last used time.
     * This helps us show popular templates first.
     * 
     * @param templateId Which template was used
     * @param newUseCount Updated usage count
     * @param lastUsedTime When it was used
     */
    @Query("UPDATE meal_templates SET useCount = :newUseCount, lastUsed = :lastUsedTime WHERE id = :templateId")
    suspend fun updateTemplateUsage(templateId: Long, newUseCount: Int, lastUsedTime: Long)
    
    /**
     * ✏️ UPDATE MEAL TEMPLATE INFO
     * 
     * When user wants to rename a template or change its description.
     * 
     * @param template Updated template information
     */
    @Update
    suspend fun updateTemplate(template: MealTemplate)
    
    /**
     * 🗑️ SOFT DELETE A MEAL TEMPLATE
     * 
     * Instead of permanently deleting, we mark templates as inactive.
     * This way we can recover them if needed, and preserve usage statistics.
     * 
     * @param templateId Which template to deactivate
     */
    @Query("UPDATE meal_templates SET isActive = 0 WHERE id = :templateId")
    suspend fun deactivateTemplate(templateId: Long)
    
    /**
     * 🔢 COUNT ACTIVE TEMPLATES
     * 
     * Get total number of active templates for statistics.
     * 
     * @return Number of active meal templates
     */
    @Query("SELECT COUNT(*) FROM meal_templates WHERE isActive = 1")
    suspend fun getActiveTemplateCount(): Int
    
    /**
     * ⭐ GET TOP USED TEMPLATES
     * 
     * Get the most popular templates for quick access display.
     * 
     * @param limit How many templates to return
     * @return Flow of most popular templates
     */
    @Query("SELECT * FROM meal_templates WHERE isActive = 1 ORDER BY useCount DESC, lastUsed DESC LIMIT :limit")
    fun getTopUsedTemplates(limit: Int = 6): Flow<List<MealTemplate>>
}