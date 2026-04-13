package com.calorietracker.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderNotificationDao {
    
    @Query("SELECT * FROM reminder_notifications ORDER BY type, hour, minute")
    fun getAllReminders(): Flow<List<ReminderNotification>>
    
    @Query("SELECT * FROM reminder_notifications WHERE isEnabled = 1")
    suspend fun getActiveReminders(): List<ReminderNotification>
    
    @Query("SELECT * FROM reminder_notifications WHERE type = :type")
    suspend fun getRemindersByType(type: String): List<ReminderNotification>
    
    @Query("SELECT * FROM reminder_notifications WHERE id = :id")
    suspend fun getReminderById(id: Int): ReminderNotification?
    
    @Insert
    suspend fun insertReminder(reminder: ReminderNotification): Long
    
    @Insert
    suspend fun insertReminders(reminders: List<ReminderNotification>)
    
    @Update
    suspend fun updateReminder(reminder: ReminderNotification)
    
    @Delete
    suspend fun deleteReminder(reminder: ReminderNotification)
    
    @Query("DELETE FROM reminder_notifications WHERE id = :id")
    suspend fun deleteReminderById(id: Int)
    
    @Query("UPDATE reminder_notifications SET isEnabled = :enabled WHERE id = :id")
    suspend fun toggleReminder(id: Int, enabled: Boolean)
    
    @Query("UPDATE reminder_notifications SET lastTriggered = :timestamp WHERE id = :id")
    suspend fun updateLastTriggered(id: Int, timestamp: Long)
    
    @Query("DELETE FROM reminder_notifications WHERE type = :type")
    suspend fun deleteRemindersByType(type: String)
}