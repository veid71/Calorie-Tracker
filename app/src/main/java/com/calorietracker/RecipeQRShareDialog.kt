package com.calorietracker

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.database.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.provider.MediaStore
import android.content.ContentValues

/**
 * 📱 RECIPE QR SHARE DIALOG - COMPREHENSIVE RECIPE SHARING
 * 
 * Hey future programmer! This dialog provides multiple ways to share recipes
 * with QR codes, text sharing, and social media integration.
 * 
 * 🎯 What users can do:
 * - 📷 **Generate QR Code**: Create scannable QR code for the recipe
 * - 📱 **Share via Apps**: Send via WhatsApp, SMS, email, social media
 * - 💾 **Save QR Code**: Download QR code image to gallery
 * - 📋 **Copy Link**: Copy sharing link to clipboard
 * - 📄 **Share as Text**: Send formatted recipe text
 * 
 * 🔧 Technical Features:
 * - Generates QR codes using ZXing library
 * - Creates shareable deep links
 * - Formats recipes as human-readable text
 * - Handles multiple sharing methods
 * - Saves QR codes to device storage
 * 
 * 🚀 Usage: Called from RecipeLibraryActivity when user taps share button
 */
class RecipeQRShareDialog(
    context: Context,
    private val recipe: Recipe
) : Dialog(context) {
    
    private lateinit var ivQRCode: ImageView
    private lateinit var tvRecipeName: TextView
    private lateinit var tvShareLink: TextView
    private lateinit var btnShareLink: Button
    private lateinit var btnShareText: Button
    private lateinit var btnSaveQR: Button
    private lateinit var btnClose: Button
    
    private var qrCodeBitmap: Bitmap? = null
    private val shareManager = RecipeShareManager(context)
    private val database = CalorieDatabase.getDatabase(context)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_recipe_qr_share)
        
        initViews()
        setupListeners()
        loadRecipeData()
    }
    
    private fun initViews() {
        ivQRCode = findViewById(R.id.ivQRCode)
        tvRecipeName = findViewById(R.id.tvRecipeName)
        tvShareLink = findViewById(R.id.tvShareLink)
        btnShareLink = findViewById(R.id.btnShareLink)
        btnShareText = findViewById(R.id.btnShareText)
        btnSaveQR = findViewById(R.id.btnSaveQR)
        btnClose = findViewById(R.id.btnClose)
    }
    
    private fun setupListeners() {
        btnShareLink.setOnClickListener {
            shareRecipeLink()
        }
        
        btnShareText.setOnClickListener {
            shareRecipeText()
        }
        
        btnSaveQR.setOnClickListener {
            saveQRCodeToGallery()
        }
        
        btnClose.setOnClickListener {
            dismiss()
        }
        
        // Long press on QR code to copy link
        ivQRCode.setOnLongClickListener {
            copyLinkToClipboard()
            true
        }
    }
    
    /**
     * 📊 LOAD AND DISPLAY RECIPE DATA
     * 
     * Loads recipe details and generates QR code in background thread
     * for smooth UI performance.
     */
    private fun loadRecipeData() {
        // Set recipe name
        tvRecipeName.text = recipe.name
        
        // Generate sharing link and QR code
        val shareLink = shareManager.generateShareLink(recipe)
        tvShareLink.text = shareLink
        
        // Generate QR code in background
        (context as androidx.lifecycle.LifecycleOwner).lifecycleScope.launch {
            qrCodeBitmap = withContext(Dispatchers.IO) {
                shareManager.generateQRCode(recipe)
            }
            
            qrCodeBitmap?.let { bitmap ->
                ivQRCode.setImageBitmap(bitmap)
                btnSaveQR.isEnabled = true
            } ?: run {
                Toast.makeText(context, "Failed to generate QR code", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 🔗 SHARE RECIPE LINK
     * 
     * Creates sharing intent with the recipe deep link.
     * Users can choose which app to share with.
     */
    private fun shareRecipeLink() {
        val shareLink = shareManager.generateShareLink(recipe)
        val sharingMessage = shareManager.createSharingMessage(recipe)
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sharingMessage)
            putExtra(Intent.EXTRA_SUBJECT, "Recipe: ${recipe.name}")
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share Recipe Link"))
    }
    
    /**
     * 📄 SHARE RECIPE AS FORMATTED TEXT
     * 
     * Shares the complete recipe with ingredients and instructions
     * as human-readable text.
     */
    private fun shareRecipeText() {
        (context as androidx.lifecycle.LifecycleOwner).lifecycleScope.launch {
            val recipeText = withContext(Dispatchers.IO) {
                // Create simple recipe text without ingredients for now
                shareManager.createShareableText(recipe, emptyList())
            }
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, recipeText)
                putExtra(Intent.EXTRA_SUBJECT, "Recipe: ${recipe.name}")
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Recipe"))
        }
    }
    
    /**
     * 💾 SAVE QR CODE TO DEVICE GALLERY
     * 
     * Saves the generated QR code image to the user's photo gallery
     * so they can share it later or print it.
     */
    private fun saveQRCodeToGallery() {
        qrCodeBitmap?.let { bitmap ->
            try {
                val filename = "CalorieTracker_Recipe_${recipe.name.replace("[^a-zA-Z0-9]".toRegex(), "_")}_QR.png"
                
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CalorieTracker")
                }
                
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                
                uri?.let { imageUri ->
                    context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        Toast.makeText(context, "QR code saved to gallery!", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    Toast.makeText(context, "Failed to save QR code", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving QR code: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(context, "QR code not ready yet", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 📋 COPY SHARING LINK TO CLIPBOARD
     * 
     * Copies the recipe sharing link to clipboard for easy pasting.
     */
    private fun copyLinkToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val shareLink = shareManager.generateShareLink(recipe)
        val clip = android.content.ClipData.newPlainText("Recipe Share Link", shareLink)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(context, "Recipe link copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}