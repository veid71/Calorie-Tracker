package com.calorietracker.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🚀 DATABASE QUERY OPTIMIZER
 * 
 * Advanced performance optimization utility for Room database queries.
 * 
 * **PERFORMANCE OPTIMIZATION CONCEPTS:**
 * 
 * **Database Indexing:**
 * - Indices are like "bookmarks" in database tables
 * - They allow fast lookups but require storage space
 * - Critical for columns used in WHERE, ORDER BY, GROUP BY clauses
 * 
 * **Query Optimization Strategies:**
 * 1. **Covering Indices**: Include all columns needed in query
 * 2. **Composite Indices**: Multi-column indices for complex queries
 * 3. **Partial Indices**: Index only relevant rows with WHERE conditions
 * 4. **Query Rewriting**: Transform slow queries into faster equivalents
 * 
 * **Common Performance Issues:**
 * - LIKE queries with leading wildcards ('%term' vs 'term%')
 * - Missing indices on frequently queried columns
 * - Inefficient JOIN operations
 * - Unoptimized ORDER BY clauses
 * - Full table scans instead of index seeks
 */
object QueryOptimizer {
    
    private const val TAG = "QueryOptimizer"
    
    /**
     * 🔧 OPTIMIZE EXISTING DATABASE INDICES
     * 
     * Analyzes current database schema and adds missing performance-critical indices.
     */
    suspend fun optimizeDatabase(database: CalorieDatabase) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting database optimization...")
            
            // Get database connection for direct SQL execution
            database.openHelper.writableDatabase.use { db ->
                
                // Core performance indices for frequently used tables
                createOptimizedIndices(db)
                
                // Analyze query patterns and suggest optimizations
                analyzeQueryPerformance(db)
                
                // Update database statistics for better query planning
                updateDatabaseStatistics(db)
                
            }
            
            Log.d(TAG, "Database optimization completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during database optimization", e)
        }
    }
    
    /**
     * 📊 CREATE PERFORMANCE-CRITICAL INDICES
     */
    private fun createOptimizedIndices(db: SupportSQLiteDatabase) {
        Log.d(TAG, "Creating optimized database indices...")
        
        // CalorieEntry optimizations - most frequently accessed table
        createIndexIfNotExists(db, "idx_calorie_entries_date_timestamp", "calorie_entries", "date, timestamp DESC")
        createIndexIfNotExists(db, "idx_calorie_entries_date_calories", "calorie_entries", "date, calories")
        createIndexIfNotExists(db, "idx_calorie_entries_foodName", "calorie_entries", "foodName")
        
        // USDA food search optimizations - large dataset queries
        createIndexIfNotExists(db, "idx_usda_description_prefix", "usda_food_items", "description COLLATE NOCASE")
        createIndexIfNotExists(db, "idx_usda_category_desc", "usda_food_items", "foodCategory, description")
        createIndexIfNotExists(db, "idx_usda_calories_protein", "usda_food_items", "calories, protein")
        
        // OpenFoodFacts optimizations - barcode lookups
        createIndexIfNotExists(db, "idx_openfoodfacts_barcode", "openfoodfacts_items", "barcode")
        createIndexIfNotExists(db, "idx_openfoodfacts_name", "openfoodfacts_items", "productName COLLATE NOCASE")
        
        // BarcodeCache optimizations - offline performance 
        createIndexIfNotExists(db, "idx_barcode_cache_barcode", "barcode_cache", "barcode")
        createIndexIfNotExists(db, "idx_barcode_cache_timestamp", "barcode_cache", "cacheTimestamp DESC")
        
        // SearchCache optimizations - search performance
        createIndexIfNotExists(db, "idx_search_cache_query", "search_cache", "query COLLATE NOCASE")
        createIndexIfNotExists(db, "idx_search_cache_timestamp", "search_cache", "timestamp DESC")
        
        // WorkoutCalories optimizations - fitness data
        createIndexIfNotExists(db, "idx_workout_date", "workout_calories", "date DESC")
        
        // WeightEntry optimizations - tracking data
        createIndexIfNotExists(db, "idx_weight_entries_date", "weight_entries", "date DESC")
        
        // Recipe optimizations - meal planning
        createIndexIfNotExists(db, "idx_recipes_name_category", "recipes", "name COLLATE NOCASE, category")
        createIndexIfNotExists(db, "idx_recipes_favorite", "recipes", "isFavorite DESC, name")
        
        // Community recipe optimizations - social features
        createIndexIfNotExists(db, "idx_community_recipes_rating", "community_recipes", "totalRating DESC, createdAt DESC")
        createIndexIfNotExists(db, "idx_community_recipes_category", "community_recipes", "categoryTag, totalRating DESC")
        createIndexIfNotExists(db, "idx_community_recipes_author", "community_recipes", "authorId, createdAt DESC")
        
        Log.d(TAG, "Optimized indices creation completed")
    }
    
    /**
     * 🆕 CREATE INDEX IF NOT EXISTS (Safe Index Creation)
     */
    private fun createIndexIfNotExists(
        db: SupportSQLiteDatabase,
        indexName: String,
        tableName: String,
        columns: String
    ) {
        try {
            val sql = "CREATE INDEX IF NOT EXISTS $indexName ON $tableName ($columns)"
            db.execSQL(sql)
            Log.d(TAG, "Created index: $indexName")
        } catch (e: Exception) {
            Log.w(TAG, "Could not create index $indexName: ${e.message}")
        }
    }
    
    /**
     * 📈 ANALYZE QUERY PERFORMANCE
     */
    private fun analyzeQueryPerformance(db: SupportSQLiteDatabase) {
        Log.d(TAG, "Analyzing query performance patterns...")
        
        try {
            // Enable query planning analysis
            db.execSQL("PRAGMA optimize")
            
            // Check for tables that might benefit from VACUUM
            val cursor = db.query("PRAGMA integrity_check")
            cursor.use {
                while (it.moveToNext()) {
                    val result = it.getString(0)
                    if (result != "ok") {
                        Log.w(TAG, "Database integrity issue: $result")
                    }
                }
            }
            
            // Update table statistics for better query optimization
            db.execSQL("ANALYZE")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during performance analysis", e)
        }
    }
    
    /**
     * 📊 UPDATE DATABASE STATISTICS
     */
    private fun updateDatabaseStatistics(db: SupportSQLiteDatabase) {
        try {
            // Update SQLite's internal statistics for better query planning
            db.execSQL("PRAGMA optimize")
            
            Log.d(TAG, "Database statistics updated")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating database statistics", e)
        }
    }
    
    /**
     * 🔍 OPTIMIZED SEARCH QUERY BUILDERS
     * 
     * Pre-built optimized queries for common search patterns
     */
    object OptimizedQueries {
        
        /**
         * Fast text search with proper index usage
         */
        fun buildFastTextSearch(tableName: String, searchColumn: String, query: String): String {
            // Avoid leading wildcards for better index usage
            return if (query.length >= 3) {
                """
                SELECT * FROM $tableName 
                WHERE $searchColumn LIKE '$query%' 
                   OR $searchColumn LIKE '% $query%'
                ORDER BY 
                    CASE 
                        WHEN $searchColumn LIKE '$query%' THEN 1 
                        ELSE 2 
                    END,
                    $searchColumn 
                LIMIT 50
                """.trimIndent()
            } else {
                // For short queries, use exact match or prefix match only
                """
                SELECT * FROM $tableName 
                WHERE $searchColumn LIKE '$query%'
                ORDER BY $searchColumn 
                LIMIT 50
                """.trimIndent()
            }
        }
        
        /**
         * Optimized date range queries with proper index usage
         */
        fun buildDateRangeQuery(
            tableName: String,
            dateColumn: String,
            startDate: String,
            endDate: String,
            additionalColumns: String = "*"
        ): String {
            return """
                SELECT $additionalColumns 
                FROM $tableName 
                WHERE $dateColumn BETWEEN '$startDate' AND '$endDate'
                ORDER BY $dateColumn DESC
                """.trimIndent()
        }
        
        /**
         * Optimized aggregation queries
         */
        fun buildAggregationQuery(
            tableName: String,
            groupByColumn: String,
            aggregateColumn: String,
            aggregateFunction: String = "SUM"
        ): String {
            return """
                SELECT $groupByColumn, $aggregateFunction($aggregateColumn) as total
                FROM $tableName 
                GROUP BY $groupByColumn
                ORDER BY $groupByColumn DESC
                """.trimIndent()
        }
    }
    
    /**
     * 🎯 QUERY PERFORMANCE MONITORING
     */
    data class QueryPerformance(
        val query: String,
        val executionTimeMs: Long,
        val rowsReturned: Int,
        val indexesUsed: List<String>
    )
    
    /**
     * ⚡ PERFORMANCE RECOMMENDATIONS
     */
    object PerformanceRecommendations {
        
        fun analyzeSlowQueries(): List<String> {
            return listOf(
                "Consider adding index on frequently searched text columns",
                "Use LIMIT clauses on large result sets to improve UI responsiveness",
                "Avoid SELECT * when only specific columns are needed",
                "Use prepared statements for repeated queries",
                "Consider denormalizing heavily joined data for read performance",
                "Use EXPLAIN QUERY PLAN to analyze query execution paths",
                "Regular VACUUM operations to reclaim space and optimize storage",
                "Consider pagination for large data sets in UI"
            )
        }
        
        fun getOptimizationChecklist(): List<String> {
            return listOf(
                "✅ Add indices on all foreign key columns",
                "✅ Index columns used in WHERE clauses",
                "✅ Index columns used in ORDER BY clauses", 
                "✅ Create composite indices for multi-column searches",
                "✅ Use covering indices when possible",
                "✅ Avoid leading wildcards in LIKE queries ('%term')",
                "✅ Use LIMIT to prevent large result sets",
                "✅ Regular ANALYZE to update query statistics",
                "✅ Monitor query execution plans with EXPLAIN",
                "✅ Consider full-text search for complex text queries"
            )
        }
    }
}