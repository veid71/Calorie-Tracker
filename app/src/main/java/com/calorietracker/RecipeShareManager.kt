package com.calorietracker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.calorietracker.database.Recipe
import com.calorietracker.database.RecipeIngredient
import com.calorietracker.database.getShareableSummary
import java.util.*

/**
 * Manager for handling recipe sharing functionality including QR code generation,
 * deep link creation, and recipe serialization for sharing.
 */
class RecipeShareManager(private val context: Context) {
    
    private val gson = Gson()
    
    companion object {
        private const val DEEP_LINK_BASE = "calorietracker://recipe/share/"
        private const val QR_CODE_SIZE = 512
    }
    
    /**
     * Generate a shareable deep link for a recipe.
     * @param recipe Recipe to share
     * @return Deep link URL string
     */
    fun generateShareLink(recipe: Recipe): String {
        val shareId = recipe.shareId ?: UUID.randomUUID().toString()
        return "$DEEP_LINK_BASE$shareId"
    }
    
    /**
     * Generate QR code bitmap for a recipe.
     * @param recipe Recipe to generate QR code for
     * @return Bitmap of QR code or null if generation failed
     */
    fun generateQRCode(recipe: Recipe): Bitmap? {
        return try {
            val shareLink = generateShareLink(recipe)
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(shareLink, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE)
            
            val bitmap = Bitmap.createBitmap(QR_CODE_SIZE, QR_CODE_SIZE, Bitmap.Config.RGB_565)
            for (x in 0 until QR_CODE_SIZE) {
                for (y in 0 until QR_CODE_SIZE) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create a comprehensive shareable text version of a recipe.
     * @param recipe Recipe to share
     * @param ingredients List of ingredients for the recipe
     * @return Formatted recipe text ready for sharing
     */
    fun createShareableText(recipe: Recipe, ingredients: List<RecipeIngredient>): String {
        return buildString {
            append(recipe.getShareableSummary())
            append("\n")
            
            // Add ingredients list
            if (ingredients.isNotEmpty()) {
                append("📋 INGREDIENTS:\n")
                ingredients.forEach { ingredient ->
                    append("• ${ingredient.ingredientName} - ${ingredient.quantity} ${ingredient.unit}\n")
                }
                append("\n")
            }
            
            // Add instructions if available
            if (!recipe.instructions.isNullOrBlank()) {
                append("📝 INSTRUCTIONS:\n")
                append(recipe.instructions)
                append("\n\n")
            }
            
            // Add sharing info
            append("🔗 Import this recipe into CalorieTracker:\n")
            append(generateShareLink(recipe))
            append("\n\n")
            append("Shared via CalorieTracker")
        }
    }
    
    /**
     * Serialize recipe and ingredients to JSON for sharing.
     * @param recipe Recipe to serialize
     * @param ingredients List of ingredients
     * @return JSON string representation
     */
    fun serializeRecipeToJson(recipe: Recipe, ingredients: List<RecipeIngredient>): String {
        val shareableRecipe = ShareableRecipe(
            recipe = recipe,
            ingredients = ingredients,
            sharedAt = System.currentTimeMillis(),
            version = "1.0"
        )
        
        return gson.toJson(shareableRecipe)
    }
    
    /**
     * Deserialize recipe from JSON sharing format.
     * @param jsonString JSON string to parse
     * @return ShareableRecipe object or null if parsing failed
     */
    fun deserializeRecipeFromJson(jsonString: String): ShareableRecipe? {
        return try {
            gson.fromJson(jsonString, ShareableRecipe::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }
    
    /**
     * Extract share ID from various sharing formats.
     * @param sharedContent Content that was shared (link, ID, etc.)
     * @return Share ID string or null if not found
     */
    fun extractShareId(sharedContent: String): String? {
        return when {
            // Direct UUID
            isValidUUID(sharedContent) -> sharedContent
            
            // Deep link format
            sharedContent.startsWith(DEEP_LINK_BASE) -> {
                sharedContent.removePrefix(DEEP_LINK_BASE)
            }
            
            // Extract from longer text/message
            sharedContent.contains(DEEP_LINK_BASE) -> {
                val startIndex = sharedContent.indexOf(DEEP_LINK_BASE)
                val shareIdStart = startIndex + DEEP_LINK_BASE.length
                val shareIdEnd = shareIdStart + 36 // UUID length
                
                if (shareIdEnd <= sharedContent.length) {
                    val extractedId = sharedContent.substring(shareIdStart, shareIdEnd)
                    if (isValidUUID(extractedId)) extractedId else null
                } else null
            }
            
            else -> null
        }
    }
    
    /**
     * Create a simple sharing message with key recipe info.
     * @param recipe Recipe to create message for
     * @return Short sharing message
     */
    fun createSharingMessage(recipe: Recipe): String {
        return buildString {
            append("Check out this recipe: ${recipe.name}")
            
            if (recipe.totalCalories > 0) {
                val caloriesPerServing = if (recipe.servings > 0) recipe.totalCalories / recipe.servings else 0
                append(" ($caloriesPerServing cal per serving)")
            }
            
            append("\n\n")
            append("Import to CalorieTracker: ${generateShareLink(recipe)}")
        }
    }
    
    /**
     * Validate if a string is a properly formatted UUID.
     */
    private fun isValidUUID(uuid: String): Boolean {
        return try {
            UUID.fromString(uuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}

/**
 * Data class for JSON serialization of shareable recipes.
 */
data class ShareableRecipe(
    val recipe: Recipe,
    val ingredients: List<RecipeIngredient>,
    val sharedAt: Long,
    val version: String
)