package com.calorietracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.Recipe
import com.calorietracker.database.RecipeIngredient
import com.google.android.material.button.MaterialButton
import android.widget.EditText
import java.text.SimpleDateFormat
import java.util.*

class RecipeCreateActivity : AppCompatActivity() {
    
    private lateinit var viewModel: RecipeCreateViewModel
    private lateinit var ingredientsAdapter: RecipeIngredientsAdapter
    
    // UI Elements
    private lateinit var etRecipeName: EditText
    private lateinit var etRecipeDescription: EditText
    private lateinit var etServings: EditText
    private lateinit var etPrepTime: EditText
    private lateinit var etCookTime: EditText
    private lateinit var etCategory: EditText
    private lateinit var etInstructions: EditText
    
    // Ingredient input
    private lateinit var layoutIngredientSearch: LinearLayout
    private lateinit var etIngredientSearch: AutoCompleteTextView
    private lateinit var etQuantity: EditText
    private lateinit var etUnit: EditText
    
    // Buttons
    private lateinit var btnAddIngredient: MaterialButton
    private lateinit var btnConfirmIngredient: MaterialButton
    private lateinit var btnSave: ImageButton
    private lateinit var btnBack: ImageButton
    
    // RecyclerView
    private lateinit var recyclerViewIngredients: RecyclerView
    private lateinit var tvNoIngredients: TextView
    
    // Nutrition summary
    private lateinit var tvCaloriesPerServing: TextView
    private lateinit var tvProteinPerServing: TextView
    private lateinit var tvCarbsPerServing: TextView
    private lateinit var tvFatPerServing: TextView
    private lateinit var cardNutritionSummary: View
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_create)
        
        viewModel = ViewModelProvider(this)[RecipeCreateViewModel::class.java]
        
        initViews()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        // Check if editing existing recipe
        val recipeId = intent.getLongExtra("recipe_id", -1)
        if (recipeId != -1L) {
            viewModel.loadRecipe(recipeId)
        }
    }
    
    private fun initViews() {
        etRecipeName = findViewById(R.id.etRecipeName)
        etRecipeDescription = findViewById(R.id.etRecipeDescription)
        etServings = findViewById(R.id.etServings)
        etPrepTime = findViewById(R.id.etPrepTime)
        etCookTime = findViewById(R.id.etCookTime)
        etCategory = findViewById(R.id.etCategory)
        etInstructions = findViewById(R.id.etInstructions)
        
        layoutIngredientSearch = findViewById(R.id.layoutIngredientSearch)
        etIngredientSearch = findViewById(R.id.etIngredientSearch)
        etQuantity = findViewById(R.id.etQuantity)
        etUnit = findViewById(R.id.etUnit)
        
        btnAddIngredient = findViewById(R.id.btnAddIngredient)
        btnConfirmIngredient = findViewById(R.id.btnConfirmIngredient)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
        
        recyclerViewIngredients = findViewById(R.id.recyclerViewIngredients)
        tvNoIngredients = findViewById(R.id.tvNoIngredients)
        
        tvCaloriesPerServing = findViewById(R.id.tvCaloriesPerServing)
        tvProteinPerServing = findViewById(R.id.tvProteinPerServing)
        tvCarbsPerServing = findViewById(R.id.tvCarbsPerServing)
        tvFatPerServing = findViewById(R.id.tvFatPerServing)
        cardNutritionSummary = findViewById(R.id.cardNutritionSummary)
    }
    
    private fun setupRecyclerView() {
        ingredientsAdapter = RecipeIngredientsAdapter(
            onRemoveClick = { ingredient ->
                viewModel.removeIngredient(ingredient)
            },
            onEditClick = { ingredient ->
                // TODO: Edit ingredient functionality
                Toast.makeText(this, "Edit ingredient: ${ingredient.ingredientName}", Toast.LENGTH_SHORT).show()
            }
        )
        
        recyclerViewIngredients.layoutManager = LinearLayoutManager(this)
        recyclerViewIngredients.adapter = ingredientsAdapter
    }
    
    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        btnSave.setOnClickListener {
            saveRecipe()
        }
        
        btnAddIngredient.setOnClickListener {
            showIngredientInput(true)
        }
        
        btnConfirmIngredient.setOnClickListener {
            addIngredient()
        }
        
        // Setup food search functionality
        etIngredientSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.let { query ->
                    if (query.length >= 2) {
                        viewModel.searchFood(query)
                    }
                }
            }
        })
        
        // Auto-calculate nutrition on servings change
        etServings.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateNutritionDisplay()
            }
        })
    }
    
    private fun observeViewModel() {
        viewModel.ingredients.observe(this) { ingredients ->
            ingredientsAdapter.submitList(ingredients)
            
            val hasIngredients = ingredients.isNotEmpty()
            tvNoIngredients.visibility = if (hasIngredients) View.GONE else View.VISIBLE
            recyclerViewIngredients.visibility = if (hasIngredients) View.VISIBLE else View.GONE
            cardNutritionSummary.visibility = if (hasIngredients) View.VISIBLE else View.GONE
            
            updateNutritionDisplay()
        }
        
        viewModel.foodSearchResults.observe(this) { results ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, 
                results.map { "${it.name} (${it.calories} cal)" })
            etIngredientSearch.setAdapter(adapter)
        }
        
        viewModel.selectedFood.observe(this) { food ->
            food?.let {
                // Auto-fill quantity defaults
                if (etQuantity.text.isNullOrBlank()) {
                    etQuantity.setText("1")
                }
                if (etUnit.text.isNullOrBlank()) {
                    etUnit.setText(food.unit ?: "serving")
                }
            }
        }
        
        viewModel.currentRecipe.observe(this) { recipe ->
            recipe?.let {
                fillRecipeFields(it)
            }
        }
        
        viewModel.saveResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Recipe saved successfully!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Error saving recipe", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showIngredientInput(show: Boolean) {
        layoutIngredientSearch.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            etIngredientSearch.requestFocus()
        } else {
            // Clear input fields
            etIngredientSearch.text.clear()
            etQuantity.text?.clear()
            etUnit.text?.clear()
        }
    }
    
    private fun addIngredient() {
        val ingredientName = etIngredientSearch.text.toString().trim()
        val quantityStr = etQuantity.text.toString().trim()
        val unit = etUnit.text.toString().trim()
        
        if (ingredientName.isEmpty() || quantityStr.isEmpty() || unit.isEmpty()) {
            Toast.makeText(this, "Please fill all ingredient fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        val quantity = quantityStr.toDoubleOrNull()
        if (quantity == null || quantity <= 0) {
            Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModel.addIngredient(ingredientName, quantity, unit)
        showIngredientInput(false)
        
        Toast.makeText(this, "Ingredient added: $ingredientName", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateNutritionDisplay() {
        val ingredients = viewModel.ingredients.value ?: emptyList()
        val servings = etServings.text.toString().toIntOrNull() ?: 1
        
        if (ingredients.isEmpty()) {
            tvCaloriesPerServing.text = "0"
            tvProteinPerServing.text = "0g"
            tvCarbsPerServing.text = "0g"
            tvFatPerServing.text = "0g"
            return
        }
        
        val totalCalories = ingredients.sumOf { it.calories }
        val totalProtein = ingredients.sumOf { it.protein }
        val totalCarbs = ingredients.sumOf { it.carbs }
        val totalFat = ingredients.sumOf { it.fat }
        
        tvCaloriesPerServing.text = (totalCalories / servings).toString()
        tvProteinPerServing.text = String.format("%.1fg", totalProtein / servings)
        tvCarbsPerServing.text = String.format("%.1fg", totalCarbs / servings)
        tvFatPerServing.text = String.format("%.1fg", totalFat / servings)
    }
    
    private fun fillRecipeFields(recipe: Recipe) {
        etRecipeName.setText(recipe.name)
        etRecipeDescription.setText(recipe.description)
        etServings.setText(recipe.servings.toString())
        etPrepTime.setText(recipe.prepTime?.toString() ?: "")
        etCookTime.setText(recipe.cookTime?.toString() ?: "")
        etCategory.setText(recipe.category)
        etInstructions.setText(recipe.instructions)
    }
    
    private fun saveRecipe() {
        val name = etRecipeName.text.toString().trim()
        val servingsStr = etServings.text.toString().trim()
        
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a recipe name", Toast.LENGTH_SHORT).show()
            return
        }
        
        val servings = servingsStr.toIntOrNull()
        if (servings == null || servings <= 0) {
            Toast.makeText(this, "Please enter a valid number of servings", Toast.LENGTH_SHORT).show()
            return
        }
        
        val ingredients = viewModel.ingredients.value
        if (ingredients.isNullOrEmpty()) {
            Toast.makeText(this, "Please add at least one ingredient", Toast.LENGTH_SHORT).show()
            return
        }
        
        val recipe = Recipe(
            id = viewModel.currentRecipe.value?.id ?: 0,
            name = name,
            description = etRecipeDescription.text.toString().trim().ifEmpty { null },
            instructions = etInstructions.text.toString().trim().ifEmpty { null },
            servings = servings,
            prepTime = etPrepTime.text.toString().toIntOrNull(),
            cookTime = etCookTime.text.toString().toIntOrNull(),
            category = etCategory.text.toString().trim().ifEmpty { null },
            createdDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            lastModified = System.currentTimeMillis(),
            totalCalories = ingredients.sumOf { it.calories },
            totalProtein = ingredients.sumOf { it.protein },
            totalCarbs = ingredients.sumOf { it.carbs },
            totalFat = ingredients.sumOf { it.fat },
            totalFiber = ingredients.sumOf { it.fiber ?: 0.0 },
            totalSugar = ingredients.sumOf { it.sugar ?: 0.0 },
            totalSodium = ingredients.sumOf { it.sodium ?: 0.0 }
        )
        
        viewModel.saveRecipe(recipe)
    }
}