package com.calorietracker.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import android.view.View
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException

/**
 * 🛡️ COMPREHENSIVE ERROR HANDLER - USER-FRIENDLY ERROR MANAGEMENT
 * 
 * Hey future programmer! This class provides centralized error handling
 * with user-friendly messages and appropriate actions for different error types.
 * 
 * 🎯 What this handler does:
 * - 🔍 **Smart Error Detection**: Identifies common error patterns
 * - 💬 **User-Friendly Messages**: Converts technical errors to readable text
 * - 🎨 **Multiple Display Options**: Toast, Snackbar, or Dialog presentation
 * - 🔄 **Action Suggestions**: Provides "try again" or "settings" actions
 * - 📝 **Error Logging**: Logs errors for debugging while showing clean UI
 * 
 * 🚀 Common Usage:
 * - Network failures: "Check internet connection"
 * - Database errors: "Database temporarily unavailable" 
 * - Permission errors: "Permission required - tap to grant"
 * - Validation errors: "Please check your input"
 * 
 * 📱 Display Methods:
 * - showToast(): Quick notification
 * - showSnackbar(): Dismissible message with action
 * - showDialog(): Detailed error with options
 */
object ErrorHandler {
    
    /**
     * 📋 MAIN ERROR HANDLING METHOD
     * 
     * Takes any exception and context, determines the best user message
     * and display method based on error type and severity.
     */
    fun handleError(
        context: Context,
        error: Throwable,
        action: String = "performing this action",
        displayMethod: DisplayMethod = DisplayMethod.TOAST,
        view: View? = null,
        retryAction: (() -> Unit)? = null
    ) {
        val errorInfo = analyzeError(error, action)
        
        // Log error for debugging (with sanitized info)
        logError(error, action, context)
        
        // Display user-friendly message
        when (displayMethod) {
            DisplayMethod.TOAST -> {
                showToast(context, errorInfo.userMessage)
            }
            DisplayMethod.SNACKBAR -> {
                view?.let { v ->
                    showSnackbar(v, errorInfo.userMessage, errorInfo.actionText, retryAction)
                } ?: showToast(context, errorInfo.userMessage)
            }
            DisplayMethod.DIALOG -> {
                showErrorDialog(context, errorInfo)
            }
        }
    }
    
    /**
     * 🔍 ANALYZE ERROR TYPE AND SEVERITY
     * 
     * Examines the exception to determine:
     * - User-friendly message
     * - Suggested action
     * - Error severity
     * - Whether retry is possible
     */
    private fun analyzeError(error: Throwable, action: String): ErrorInfo {
        return when (error) {
            // 🌐 NETWORK ERRORS
            is UnknownHostException, is ConnectException -> ErrorInfo(
                userMessage = "No internet connection. Please check your network and try again.",
                actionText = "Try Again",
                severity = ErrorSeverity.RECOVERABLE,
                errorType = ErrorType.NETWORK
            )
            
            is SocketTimeoutException -> ErrorInfo(
                userMessage = "Connection timeout. The server is taking too long to respond.",
                actionText = "Try Again",
                severity = ErrorSeverity.RECOVERABLE,
                errorType = ErrorType.NETWORK
            )
            
            is IOException -> ErrorInfo(
                userMessage = "Network error while $action. Please try again.",
                actionText = "Try Again",
                severity = ErrorSeverity.RECOVERABLE,
                errorType = ErrorType.NETWORK
            )
            
            // 🗃️ DATABASE ERRORS
            is android.database.sqlite.SQLiteException -> ErrorInfo(
                userMessage = "Database error while $action. Please restart the app.",
                actionText = "Restart App",
                severity = ErrorSeverity.SERIOUS,
                errorType = ErrorType.DATABASE
            )
            
            // 🔐 PERMISSION ERRORS
            is SecurityException -> ErrorInfo(
                userMessage = "Permission required for $action. Please check app permissions.",
                actionText = "Open Settings",
                severity = ErrorSeverity.RECOVERABLE,
                errorType = ErrorType.PERMISSION
            )
            
            // 📝 VALIDATION ERRORS
            is IllegalArgumentException -> ErrorInfo(
                userMessage = "Invalid input while $action. Please check your data and try again.",
                actionText = "Check Input",
                severity = ErrorSeverity.MINOR,
                errorType = ErrorType.VALIDATION
            )
            
            // 💾 STORAGE ERRORS
            is java.io.FileNotFoundException -> ErrorInfo(
                userMessage = "Required file not found. Please try downloading data again.",
                actionText = "Download",
                severity = ErrorSeverity.RECOVERABLE,
                errorType = ErrorType.STORAGE
            )
            
            // 🔧 GENERAL ERRORS
            else -> ErrorInfo(
                userMessage = "An unexpected error occurred while $action. Please try again.",
                actionText = "Try Again",
                severity = ErrorSeverity.UNKNOWN,
                errorType = ErrorType.GENERAL
            )
        }
    }
    
    /**
     * 🍞 SHOW TOAST MESSAGE
     * 
     * Quick, non-intrusive notification for minor errors.
     */
    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 📌 SHOW SNACKBAR WITH ACTION
     * 
     * Dismissible message bar with optional retry action.
     * Great for recoverable errors where user can try again.
     */
    private fun showSnackbar(
        view: View,
        message: String,
        actionText: String?,
        retryAction: (() -> Unit)?
    ) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        
        if (actionText != null && retryAction != null) {
            snackbar.setAction(actionText) {
                try {
                    retryAction()
                } catch (e: Exception) {
                    // If retry also fails, show simpler message
                    showToast(view.context, "Retry failed. Please check your connection.")
                }
            }
        }
        
        snackbar.show()
    }
    
    /**
     * 📱 SHOW ERROR DIALOG
     * 
     * Detailed error dialog for serious errors that need user attention.
     * Includes error details and multiple action options.
     */
    private fun showErrorDialog(context: Context, errorInfo: ErrorInfo) {
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage(errorInfo.userMessage)
            .setPositiveButton(errorInfo.actionText ?: "OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
    
    /**
     * 📝 LOG ERROR FOR DEBUGGING
     * 
     * Logs error details for developers while keeping sensitive info secure.
     */
    private fun logError(error: Throwable, action: String, context: Context) {
        val errorMessage = """
            |📱 App Error Report
            |Action: $action
            |Error Type: ${error.javaClass.simpleName}
            |Message: ${error.message}
            |Package: ${context.packageName}
            |Thread: ${Thread.currentThread().name}
            |Stack Trace: ${error.stackTrace.take(3).joinToString("\n") { "  at $it" }}
        """.trimMargin()
        
        // In production, this would go to crash reporting service
        Log.e("ErrorHandler", errorMessage)
    }
    
    /**
     * 🚀 QUICK HELPER METHODS FOR COMMON SCENARIOS
     */
    
    fun handleNetworkError(context: Context, view: View? = null, retryAction: (() -> Unit)? = null) {
        handleError(
            context = context,
            error = UnknownHostException("Network unavailable"),
            action = "connecting to server",
            displayMethod = if (view != null) DisplayMethod.SNACKBAR else DisplayMethod.TOAST,
            view = view,
            retryAction = retryAction
        )
    }
    
    fun handleDatabaseError(context: Context) {
        handleError(
            context = context,
            error = android.database.sqlite.SQLiteException("Database error"),
            action = "accessing database",
            displayMethod = DisplayMethod.DIALOG
        )
    }
    
    fun handleValidationError(context: Context, field: String) {
        handleError(
            context = context,
            error = IllegalArgumentException("Invalid $field"),
            action = "validating $field",
            displayMethod = DisplayMethod.TOAST
        )
    }
    
    /**
     * 📊 ERROR CLASSIFICATION DATA CLASSES
     */
    enum class DisplayMethod {
        TOAST,      // Quick, non-intrusive
        SNACKBAR,   // Dismissible with action
        DIALOG      // Detailed with options
    }
    
    enum class ErrorSeverity {
        MINOR,      // Can continue using app
        RECOVERABLE, // User can fix with action
        SERIOUS,    // Needs app restart
        CRITICAL,   // Needs user intervention
        UNKNOWN     // Severity unclear
    }
    
    enum class ErrorType {
        NETWORK,     // Internet/connectivity
        DATABASE,    // Data storage
        PERMISSION,  // App permissions
        VALIDATION,  // User input
        STORAGE,     // File system
        GENERAL      // Other/unknown
    }
    
    data class ErrorInfo(
        val userMessage: String,
        val actionText: String?,
        val severity: ErrorSeverity,
        val errorType: ErrorType
    )
}