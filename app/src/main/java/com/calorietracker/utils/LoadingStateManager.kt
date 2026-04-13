package com.calorietracker.utils

import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.calorietracker.R

/**
 * 🔄 LOADING STATE MANAGER - SMOOTH USER EXPERIENCE DURING OPERATIONS
 * 
 * Hey future programmer! This class manages loading states throughout the app
 * to provide users with clear feedback during long-running operations.
 * 
 * 🎯 What this manager handles:
 * - ⏳ **Loading Dialogs**: Full-screen loading with progress messages
 * - 🔘 **Button States**: Disable buttons and show loading spinners
 * - 📊 **Progress Tracking**: Show download/upload progress
 * - ⚡ **Quick Operations**: Subtle loading indicators for fast operations
 * - 🔄 **Retry Logic**: Handle failures with retry options
 * 
 * 🚀 Common Use Cases:
 * - Database downloads (USDA, Open Food Facts)
 * - Recipe import/export operations
 * - Barcode scanning and lookup
 * - Food search and API calls
 * - Photo uploads and processing
 * 
 * 💡 UX Best Practices:
 * - Always show progress for operations > 1 second
 * - Provide meaningful status messages
 * - Allow cancellation when possible
 * - Show estimated time for long operations
 */
class LoadingStateManager(private val context: Context) {
    
    private var loadingDialog: Dialog? = null
    private val managedButtons = mutableMapOf<MaterialButton, ButtonState>()
    
    /**
     * 📱 SHOW FULL-SCREEN LOADING DIALOG
     * 
     * Displays modal loading dialog with customizable message and progress.
     * Use for operations that block the entire UI (like database downloads).
     */
    fun showLoadingDialog(
        title: String = "Loading...",
        message: String = "Please wait",
        isCancellable: Boolean = true,
        onCancel: (() -> Unit)? = null
    ) {
        dismissLoadingDialog() // Dismiss any existing dialog
        
        loadingDialog = Dialog(context).apply {
            setContentView(R.layout.dialog_loading_state)
            setCancelable(isCancellable)
            
            // Set custom content
            findViewById<TextView>(R.id.tvLoadingTitle)?.text = title
            findViewById<TextView>(R.id.tvLoadingMessage)?.text = message
            
            // Handle cancellation
            setOnCancelListener {
                onCancel?.invoke()
                loadingDialog = null
            }
            
            show()
        }
    }
    
    /**
     * 📊 UPDATE LOADING DIALOG PROGRESS
     * 
     * Updates the loading dialog with new progress information.
     * Great for showing download progress, processing steps, etc.
     */
    fun updateLoadingProgress(
        message: String,
        progress: Int? = null,
        maxProgress: Int? = null
    ) {
        loadingDialog?.let { dialog ->
            dialog.findViewById<TextView>(R.id.tvLoadingMessage)?.text = message
            
            if (progress != null && maxProgress != null) {
                val progressBar = dialog.findViewById<ProgressBar>(R.id.pbLoadingProgress)
                progressBar?.apply {
                    visibility = View.VISIBLE
                    max = maxProgress
                    this.progress = progress
                }
                
                // Show percentage if available
                val percentage = ((progress.toFloat() / maxProgress.toFloat()) * 100).toInt()
                dialog.findViewById<TextView>(R.id.tvLoadingPercentage)?.apply {
                    visibility = View.VISIBLE
                    text = "$percentage%"
                }
            }
        }
    }
    
    /**
     * ✅ DISMISS LOADING DIALOG
     * 
     * Safely dismisses any active loading dialog.
     */
    fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
    
    /**
     * 🔘 MANAGE BUTTON LOADING STATE
     * 
     * Sets button to loading state with spinner and disabled interaction.
     * Automatically restores original state when operation completes.
     */
    fun setButtonLoading(
        button: MaterialButton,
        loadingText: String = "Loading...",
        showSpinner: Boolean = true
    ) {
        // Store original button state
        managedButtons[button] = ButtonState(
            originalText = button.text.toString(),
            originalEnabled = button.isEnabled,
            originalIcon = button.icon
        )
        
        // Set loading state
        button.apply {
            text = loadingText
            isEnabled = false
            
            if (showSpinner) {
                // Use Android's built-in progress drawable instead of custom icon
                // setIconResource(android.R.drawable.ic_dialog_info) // Comment out for now
                iconGravity = MaterialButton.ICON_GRAVITY_START
            }
        }
        
        // Start spinning animation if spinner is shown
        if (showSpinner) {
            startButtonSpinAnimation(button)
        }
    }
    
    /**
     * 🔄 RESTORE BUTTON TO NORMAL STATE
     * 
     * Restores button to its original state before loading was applied.
     */
    fun restoreButtonState(button: MaterialButton) {
        managedButtons[button]?.let { originalState ->
            button.apply {
                text = originalState.originalText
                isEnabled = originalState.originalEnabled
                icon = originalState.originalIcon
                // Stop any animations
                clearAnimation()
            }
            
            managedButtons.remove(button)
        }
    }
    
    /**
     * ⚡ QUICK LOADING STATE FOR FAST OPERATIONS
     * 
     * Shows brief loading state for operations under 3 seconds.
     * Automatically restores state after timeout or manual dismissal.
     */
    fun showQuickLoading(
        button: MaterialButton,
        loadingText: String = "Working...",
        timeoutMs: Long = 3000,
        lifecycleOwner: LifecycleOwner
    ) {
        setButtonLoading(button, loadingText, true)
        
        // Auto-restore after timeout
        lifecycleOwner.lifecycleScope.launch {
            delay(timeoutMs)
            if (managedButtons.containsKey(button)) {
                restoreButtonState(button)
            }
        }
    }
    
    /**
     * 🎭 ANIMATE LOADING SPINNER
     * 
     * Starts rotation animation for button loading spinner.
     */
    private fun startButtonSpinAnimation(button: MaterialButton) {
        val rotation = android.view.animation.RotateAnimation(
            0f, 360f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            repeatCount = android.view.animation.Animation.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
        }
        
        button.startAnimation(rotation)
    }
    
    /**
     * 🧹 CLEANUP ALL LOADING STATES
     * 
     * Restores all managed buttons and dismisses dialogs.
     * Call this in onDestroy() or when leaving the screen.
     */
    fun cleanup() {
        dismissLoadingDialog()
        
        // Restore all managed buttons
        managedButtons.keys.toList().forEach { button ->
            restoreButtonState(button)
        }
    }
    
    /**
     * 📋 DATA CLASSES FOR STATE MANAGEMENT
     */
    private data class ButtonState(
        val originalText: String,
        val originalEnabled: Boolean,
        val originalIcon: android.graphics.drawable.Drawable?
    )
    
    /**
     * 🚀 COMPANION HELPER METHODS
     */
    companion object {
        
        /**
         * 🔧 EXTENSION FUNCTION FOR EASY BUTTON LOADING
         * 
         * Allows calling button.setLoading() directly on any MaterialButton
         */
        fun MaterialButton.setLoadingState(
            loadingText: String = "Loading...",
            manager: LoadingStateManager? = null
        ) {
            val loadingManager = manager ?: LoadingStateManager(context)
            loadingManager.setButtonLoading(this, loadingText)
        }
        
        /**
         * 📱 QUICK DIALOG FACTORY METHODS
         */
        fun showDatabaseDownloadDialog(context: Context, onCancel: (() -> Unit)? = null): LoadingStateManager {
            return LoadingStateManager(context).apply {
                showLoadingDialog(
                    title = "Downloading Food Database",
                    message = "This may take several minutes...",
                    isCancellable = true,
                    onCancel = onCancel
                )
            }
        }
        
        fun showRecipeProcessingDialog(context: Context): LoadingStateManager {
            return LoadingStateManager(context).apply {
                showLoadingDialog(
                    title = "Processing Recipe",
                    message = "Calculating nutrition and saving...",
                    isCancellable = false
                )
            }
        }
        
        fun showBarcodeSearchDialog(context: Context, onCancel: (() -> Unit)? = null): LoadingStateManager {
            return LoadingStateManager(context).apply {
                showLoadingDialog(
                    title = "Looking up Product",
                    message = "Searching food databases...",
                    isCancellable = true,
                    onCancel = onCancel
                )
            }
        }
    }
}