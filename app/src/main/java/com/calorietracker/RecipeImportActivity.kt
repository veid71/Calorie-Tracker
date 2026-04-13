package com.calorietracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.calorietracker.database.*
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RecipeImportActivity : AppCompatActivity() {
    
    companion object {
        private const val REQUEST_QR_SCAN = 100
    }
    
    private lateinit var viewModel: RecipeImportViewModel
    
    private lateinit var etImportData: TextInputEditText
    private lateinit var btnImport: Button
    private lateinit var btnScanQR: Button
    private lateinit var btnCancel: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_import)
        
        viewModel = ViewModelProvider(this)[RecipeImportViewModel::class.java]
        
        initViews()
        setupListeners()
        observeViewModel()
        
        // Check if launched from a deep link or shared text
        handleIncomingIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_QR_SCAN && resultCode == RESULT_OK) {
            data?.getStringExtra("scanned_data")?.let { scannedData ->
                etImportData.setText(scannedData)
                
                // Auto-import if it's a valid recipe share link
                if (scannedData.contains("calorietracker://recipe/share/")) {
                    viewModel.importRecipe(scannedData)
                } else {
                    Toast.makeText(this, "QR code scanned! Review and import.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun initViews() {
        etImportData = findViewById(R.id.etImportData)
        btnImport = findViewById(R.id.btnImport)
        btnScanQR = findViewById(R.id.btnScanQR)
        btnCancel = findViewById(R.id.btnCancel)
    }
    
    private fun setupListeners() {
        btnImport.setOnClickListener {
            val importData = etImportData.text.toString().trim()
            if (importData.isNotEmpty()) {
                viewModel.importRecipe(importData)
            } else {
                Toast.makeText(this, "Please enter recipe data or scan QR code", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnScanQR.setOnClickListener {
            // Launch QR code scanning for recipe import
            val intent = Intent(this, QRCodeScanActivity::class.java).apply {
                putExtra("scan_mode", "recipe_import")
                putExtra("title", "Scan Recipe QR Code")
            }
            startActivityForResult(intent, REQUEST_QR_SCAN)
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun observeViewModel() {
        viewModel.importResult.observe(this) { result ->
            result.fold(
                onSuccess = { recipe ->
                    Toast.makeText(this, "Recipe '${recipe.name}' imported successfully!", Toast.LENGTH_LONG).show()
                    
                    // Open the imported recipe for viewing/editing
                    val intent = Intent(this, RecipeCreateActivity::class.java).apply {
                        putExtra("recipe_id", recipe.id)
                    }
                    startActivity(intent)
                    finish()
                },
                onFailure = { error ->
                    Toast.makeText(this, "Import failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
    
    private fun handleIncomingIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                        etImportData.setText(sharedText)
                        
                        // Auto-import if it looks like recipe data
                        if (sharedText.contains("calorietracker://recipe/share/")) {
                            viewModel.importRecipe(sharedText)
                        }
                    }
                }
            }
            
            Intent.ACTION_VIEW -> {
                // Handle deep link
                intent.data?.let { uri ->
                    if (uri.scheme == "calorietracker" && uri.host == "recipe" && uri.pathSegments.contains("share")) {
                        val shareId = uri.pathSegments.lastOrNull()
                        shareId?.let {
                            viewModel.importRecipeByShareId(it)
                        }
                    }
                }
            }
        }
    }
}

class RecipeImportViewModel(application: android.app.Application) : AndroidViewModel(application) {
    
    private val database = CalorieDatabase.getDatabase(application)
    private val recipeDao = database.recipeDao()
    private val recipeIngredientDao = database.recipeIngredientDao()
    
    private val _importResult = MutableLiveData<Result<Recipe>>()
    val importResult: LiveData<Result<Recipe>> = _importResult
    
    fun importRecipe(importData: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val shareManager = RecipeShareManager(getApplication())
                
                // Try to extract share ID from the data
                val shareId = shareManager.extractShareId(importData)
                if (shareId != null) {
                    importRecipeByShareId(shareId)
                    return@launch
                }
                
                // Try to parse as JSON
                val shareableRecipe = shareManager.deserializeRecipeFromJson(importData)
                if (shareableRecipe != null) {
                    importShareableRecipe(shareableRecipe)
                    return@launch
                }
                
                // Try to parse as plain text recipe
                parseTextRecipe(importData)?.let { (recipe, ingredients) ->
                    saveImportedRecipe(recipe, ingredients)
                } ?: run {
                    _importResult.postValue(Result.failure(Exception("Unable to parse recipe data")))
                }
                
            } catch (e: Exception) {
                _importResult.postValue(Result.failure(e))
            }
        }
    }
    
    fun importRecipeByShareId(shareId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // In a real app, this would fetch from a sharing service
                // For now, we'll just show that the functionality is there
                _importResult.postValue(Result.failure(Exception("Recipe sharing service not implemented yet")))
            } catch (e: Exception) {
                _importResult.postValue(Result.failure(e))
            }
        }
    }
    
    private suspend fun importShareableRecipe(shareableRecipe: ShareableRecipe) {
        val importedRecipe = shareableRecipe.recipe.copy(
            id = 0, // New recipe
            createdDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            lastModified = System.currentTimeMillis(),
            isShared = false,
            shareId = null,
            timesUsed = 0
        )
        
        val importedIngredients = shareableRecipe.ingredients.map { ingredient ->
            ingredient.copy(id = 0, recipeId = 0) // Will be updated when recipe is saved
        }
        
        saveImportedRecipe(importedRecipe, importedIngredients)
    }
    
    private fun parseTextRecipe(text: String): Pair<Recipe, List<RecipeIngredient>>? {
        return try {
            // Simple text parsing - look for common patterns
            val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            
            val recipeName = lines.firstOrNull { line ->
                line.contains("Recipe:", ignoreCase = true) || 
                (!line.contains("•") && !line.contains("-") && line.length < 100)
            }?.replace("Recipe:", "")?.trim() ?: "Imported Recipe"
            
            // Look for ingredients section
            val ingredientsStartIndex = lines.indexOfFirst { 
                it.contains("ingredients", ignoreCase = true) || it.contains("📋")
            }
            
            val ingredients = mutableListOf<RecipeIngredient>()
            
            if (ingredientsStartIndex >= 0) {
                for (i in (ingredientsStartIndex + 1) until lines.size) {
                    val line = lines[i]
                    if (line.contains("instructions", ignoreCase = true) || line.contains("📝")) break
                    
                    if (line.startsWith("•") || line.startsWith("-")) {
                        val ingredientText = line.removePrefix("•").removePrefix("-").trim()
                        val parts = ingredientText.split(" - ", " ", limit = 3)
                        
                        if (parts.size >= 2) {
                            val ingredient = RecipeIngredient(
                                recipeId = 0,
                                ingredientName = parts.getOrNull(2) ?: parts[0],
                                quantity = parts.getOrNull(0)?.toDoubleOrNull() ?: 1.0,
                                unit = parts.getOrNull(1) ?: "serving",
                                calories = 0,
                                protein = 0.0,
                                carbs = 0.0,
                                fat = 0.0
                            )
                            ingredients.add(ingredient)
                        }
                    }
                }
            }
            
            val recipe = Recipe(
                name = recipeName,
                description = "Imported from shared text",
                servings = 4, // Default
                createdDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                lastModified = System.currentTimeMillis()
            )
            
            recipe to ingredients
            
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun saveImportedRecipe(recipe: Recipe, ingredients: List<RecipeIngredient>) {
        val recipeId = recipeDao.insertRecipe(recipe)
        
        val ingredientsWithRecipeId = ingredients.map { ingredient ->
            ingredient.copy(recipeId = recipeId)
        }
        
        recipeIngredientDao.insertIngredients(ingredientsWithRecipeId)
        
        val savedRecipe = recipeDao.getRecipeById(recipeId)
            ?: run { _importResult.postValue(Result.failure(Exception("Recipe not found after save"))); return }
        _importResult.postValue(Result.success(savedRecipe))
    }
}