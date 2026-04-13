package com.calorietracker.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * File logger that writes debug information to external storage
 * Files will be saved to /Android/data/com.calorietracker/files/logs/
 */
object FileLogger {
    private const val TAG = "FileLogger"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    private val timestampFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    
    private var logFile: File? = null
    private var fileWriter: FileWriter? = null
    
    /**
     * Initialize file logging for the current session
     */
    fun initializeLogging(context: Context, sessionName: String = "debug_session") {
        try {
            // Create logs directory in app's external files directory
            val logsDir = File(context.getExternalFilesDir(null), "logs")
            if (!logsDir.exists()) {
                logsDir.mkdirs()
            }
            
            // Create timestamped log file
            val timestamp = dateFormat.format(Date())
            val file = File(logsDir, "${sessionName}_${timestamp}.log")
            logFile = file
            fileWriter = FileWriter(file, true)

            writeToFile("=== CalorieTracker Debug Session Started ===")
            writeToFile("Session: $sessionName")
            writeToFile("Timestamp: ${Date()}")
            writeToFile("Log file: ${file.absolutePath}")
            writeToFile("===============================================")
            writeToFile("")

            Log.d(TAG, "File logging initialized: ${file.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize file logging", e)
        }
    }
    
    /**
     * Log a debug message to both LogCat and file
     */
    fun d(tag: String, message: String) {
        Log.d(tag, message)
        writeToFile("D/$tag: $message")
    }
    
    /**
     * Log an info message to both LogCat and file
     */
    fun i(tag: String, message: String) {
        Log.i(tag, message)
        writeToFile("I/$tag: $message")
    }
    
    /**
     * Log a warning message to both LogCat and file
     */
    fun w(tag: String, message: String) {
        Log.w(tag, message)
        writeToFile("W/$tag: $message")
    }
    
    /**
     * Log an error message to both LogCat and file
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        writeToFile("E/$tag: $message")
        throwable?.let {
            writeToFile("E/$tag: Exception: ${it.message}")
            writeToFile("E/$tag: Stack trace: ${it.stackTraceToString()}")
        }
    }
    
    /**
     * Write raw text to the log file (useful for API responses, etc.)
     */
    fun writeRaw(content: String) {
        writeToFile("RAW: $content")
    }
    
    /**
     * Add a section separator to the log file
     */
    fun addSeparator(sectionName: String) {
        writeToFile("")
        writeToFile("=== $sectionName ===")
        writeToFile("")
    }
    
    /**
     * Internal method to write to file with timestamp
     */
    private fun writeToFile(message: String) {
        try {
            fileWriter?.let { writer ->
                val timestamp = timestampFormat.format(Date())
                writer.write("[$timestamp] $message\n")
                writer.flush() // Ensure immediate write
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
    
    /**
     * Close file logging and finalize the session
     */
    fun closeLogging() {
        try {
            writeToFile("")
            writeToFile("=== Session Ended ===")
            writeToFile("End time: ${Date()}")
            writeToFile("======================")
            
            fileWriter?.close()
            fileWriter = null
            
            logFile?.let { file ->
                Log.d(TAG, "Log file closed: ${file.absolutePath}")
                Log.d(TAG, "File size: ${file.length()} bytes")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error closing log file", e)
        }
    }
    
    /**
     * Get the current log file path (for sharing with user)
     */
    fun getCurrentLogFilePath(): String? {
        return logFile?.absolutePath
    }
    
    /**
     * Get the logs directory path
     */
    fun getLogsDirectoryPath(context: Context): String {
        return File(context.getExternalFilesDir(null), "logs").absolutePath
    }
}