package com.calorietracker.database

import androidx.room.*
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for progress photos
 * Provides database operations for user progress tracking
 */
@Dao
interface ProgressPhotoDao {
    
    @Query("SELECT * FROM progress_photos WHERE isVisible = 1 ORDER BY timestamp DESC")
    fun getAllVisiblePhotos(): LiveData<List<ProgressPhoto>>
    
    @Query("SELECT * FROM progress_photos WHERE isVisible = 1 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentPhotos(limit: Int = 20): List<ProgressPhoto>
    
    @Query("SELECT * FROM progress_photos WHERE isVisible = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPhotosFlow(limit: Int = 20): Flow<List<ProgressPhoto>>
    
    @Query("SELECT * FROM progress_photos WHERE date BETWEEN :startDate AND :endDate AND isVisible = 1 ORDER BY timestamp DESC")
    suspend fun getPhotosInDateRange(startDate: String, endDate: String): List<ProgressPhoto>
    
    @Query("SELECT * FROM progress_photos WHERE photoType = :type AND isVisible = 1 ORDER BY timestamp DESC")
    suspend fun getPhotosByType(type: String): List<ProgressPhoto>
    
    @Query("SELECT * FROM progress_photos WHERE tags LIKE '%' || :tag || '%' AND isVisible = 1 ORDER BY timestamp DESC")
    suspend fun getPhotosByTag(tag: String): List<ProgressPhoto>
    
    @Query("SELECT * FROM progress_photos WHERE notes LIKE '%' || :query || '%' AND isVisible = 1 ORDER BY timestamp DESC")
    suspend fun searchPhotosByNotes(query: String): List<ProgressPhoto>
    
    @Query("SELECT * FROM progress_photos WHERE id = :id")
    suspend fun getPhotoById(id: Long): ProgressPhoto?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: ProgressPhoto): Long
    
    @Update
    suspend fun updatePhoto(photo: ProgressPhoto)
    
    @Delete
    suspend fun deletePhoto(photo: ProgressPhoto)
    
    @Query("DELETE FROM progress_photos WHERE id = :id")
    suspend fun deletePhotoById(id: Long)
    
    @Query("UPDATE progress_photos SET isVisible = 0 WHERE id = :id")
    suspend fun hidePhoto(id: Long)
    
    @Query("UPDATE progress_photos SET isVisible = 1 WHERE id = :id")
    suspend fun showPhoto(id: Long)
    
    @Query("SELECT COUNT(*) FROM progress_photos WHERE isVisible = 1")
    suspend fun getVisiblePhotoCount(): Int
    
    @Query("SELECT COUNT(*) FROM progress_photos WHERE isVisible = 1 AND date = :date")
    suspend fun getPhotoCountForDate(date: String): Int
    
    // Statistics queries
    @Query("SELECT * FROM progress_photos WHERE weight IS NOT NULL AND isVisible = 1 ORDER BY timestamp ASC")
    suspend fun getPhotosWithWeight(): List<ProgressPhoto>
    
    @Query("SELECT AVG(moodRating) FROM progress_photos WHERE moodRating IS NOT NULL AND timestamp >= :since")
    suspend fun getAverageMoodSince(since: Long): Double?
    
    @Query("SELECT COUNT(*) FROM progress_photos WHERE timestamp >= :since AND isVisible = 1")
    suspend fun getPhotoCountSince(since: Long): Int
    
    // Timeline queries
    @Query("SELECT date, COUNT(*) as count FROM progress_photos WHERE isVisible = 1 GROUP BY date ORDER BY date DESC")
    suspend fun getPhotoCountsByDate(): List<PhotoDateCount>
    
    @Query("SELECT DISTINCT photoType FROM progress_photos WHERE isVisible = 1")
    suspend fun getAllPhotoTypes(): List<String>
    
    @Query("SELECT DISTINCT tags FROM progress_photos WHERE tags IS NOT NULL AND isVisible = 1")
    suspend fun getAllTags(): List<String>
    
    // Cleanup old photos (optional maintenance)
    @Query("DELETE FROM progress_photos WHERE timestamp < :cutoffTime AND isVisible = 0")
    suspend fun cleanupHiddenPhotos(cutoffTime: Long)
}

/**
 * Data class for photo count by date aggregations
 */
data class PhotoDateCount(
    val date: String,
    val count: Int
)