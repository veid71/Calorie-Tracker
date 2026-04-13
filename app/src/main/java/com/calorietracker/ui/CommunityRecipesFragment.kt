package com.calorietracker.ui

// 🧰 ANDROID UI TOOLS
import android.os.Bundle                    // Save/restore state
import android.view.LayoutInflater         // Create views from XML
import android.view.View                   // Base view class
import android.view.ViewGroup              // Container for views
import android.widget.*                    // UI components
import androidx.fragment.app.Fragment      // Fragment base class
import androidx.lifecycle.lifecycleScope   // Coroutine management
import androidx.recyclerview.widget.LinearLayoutManager  // List layout
import androidx.recyclerview.widget.RecyclerView         // Scrollable lists
import com.calorietracker.R               // App resources
import com.calorietracker.database.*      // Database entities
import com.calorietracker.social.CommunityRecipeManager  // Recipe management
import kotlinx.coroutines.launch          // Async operations
import java.text.SimpleDateFormat         // Date formatting
import java.util.*                        // Date utilities
import kotlin.math.roundToInt             // Math utilities

/**
 * 👨‍🍳 COMMUNITY RECIPES FRAGMENT - DISCOVER & SHARE HEALTHY MEALS
 * 
 * Hey young programmer! This screen is like a healthy recipe social network where
 * users can discover amazing meals from other health-conscious people.
 * 
 * 🌟 What can users do here?
 * - 🔍 **Browse Recipes**: Scroll through community-shared healthy meals
 * - ⭐ **Rate & Review**: Give feedback on recipes they've tried
 * - 💾 **Save Favorites**: Build personal recipe collection
 * - 📝 **Share Recipes**: Submit their own healthy creations
 * - 💬 **Comment & Tips**: Share cooking advice and modifications
 * - 🎯 **Filter by Goals**: Find recipes matching nutrition goals
 * 
 * 🎨 Screen Sections:
 * - **Featured Recipes**: Hand-picked best recipes of the week
 * - **Trending Now**: Popular recipes getting lots of love
 * - **Browse by Category**: Breakfast, lunch, dinner, snacks
 * - **Search & Filters**: Find exactly what you're craving
 * - **Your Recipe Box**: Personal saved recipes
 * 
 * 📱 Interactive Features:
 * - Tap recipe cards to see full details
 * - Star rating system (tap stars to rate)
 * - Heart button to save to favorites
 * - Share button to send recipe to friends
 * - Comment section for community discussion
 * 
 * 🎯 Smart Recommendations:
 * - "Based on your protein goals..."
 * - "Other users who saved this also liked..."
 * - "Quick 20-minute meals for busy days"
 * - "High-fiber recipes for digestive health"
 */
class CommunityRecipesFragment : Fragment() {
    
    // 🗄️ DATA MANAGEMENT
    internal lateinit var recipeManager: CommunityRecipeManager
    private lateinit var database: CalorieDatabase
    
    // 📱 UI COMPONENTS
    private lateinit var recipesRecyclerView: RecyclerView
    private lateinit var recipesAdapter: CommunityRecipesAdapter
    private lateinit var searchEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var filterChipGroup: LinearLayout
    private lateinit var submitRecipeButton: Button
    private lateinit var myRecipeBoxButton: Button
    
    // 🎛️ FILTER CONTROLS
    private lateinit var featuredOnlyCheckbox: CheckBox
    private lateinit var highProteinCheckbox: CheckBox
    private lateinit var lowCarbCheckbox: CheckBox
    private lateinit var quickMealsCheckbox: CheckBox
    private lateinit var vegetarianOnlyCheckbox: CheckBox
    
    // 📊 CURRENT FILTERS
    private var currentCategory: String = "All"
    private var activeFilters = mutableSetOf<String>()
    
    /**
     * 🏗️ CREATE FRAGMENT VIEW
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community_recipes, container, false)
    }
    
    /**
     * ⚙️ SET UP FRAGMENT
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 🗄️ INITIALIZE DATA MANAGERS
        database = CalorieDatabase.getDatabase(requireContext())
        recipeManager = CommunityRecipeManager(database, requireContext())
        
        // 🔗 CONNECT UI COMPONENTS
        setupUIComponents(view)
        setupRecyclerView()
        setupFilterListeners()
        setupClickListeners()
        
        // 📊 LOAD INITIAL RECIPE DATA
        loadFeaturedRecipes()
    }
    
    /**
     * 🔗 SETUP UI COMPONENTS
     */
    private fun setupUIComponents(view: View) {
        recipesRecyclerView = view.findViewById(R.id.recipesRecyclerView)
        searchEditText = view.findViewById(R.id.searchEditText)
        categorySpinner = view.findViewById(R.id.categorySpinner)
        filterChipGroup = view.findViewById(R.id.filterChipGroup)
        submitRecipeButton = view.findViewById(R.id.submitRecipeButton)
        myRecipeBoxButton = view.findViewById(R.id.myRecipeBoxButton)
        
        // 🎛️ FILTER CONTROLS
        featuredOnlyCheckbox = view.findViewById(R.id.featuredOnlyCheckbox)
        highProteinCheckbox = view.findViewById(R.id.highProteinCheckbox)
        lowCarbCheckbox = view.findViewById(R.id.lowCarbCheckbox)
        quickMealsCheckbox = view.findViewById(R.id.quickMealsCheckbox)
        vegetarianOnlyCheckbox = view.findViewById(R.id.vegetarianOnlyCheckbox)
        
        setupCategorySpinner()
    }
    
    /**
     * 🎛️ SETUP CATEGORY SPINNER
     */
    private fun setupCategorySpinner() {
        val categories = listOf(
            "🍽️ All Recipes",
            "🌅 Breakfast", 
            "🌞 Lunch",
            "🌙 Dinner",
            "🍎 Snacks",
            "🍰 Desserts"
        )
        
        categorySpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
    
    /**
     * 📋 SETUP RECYCLER VIEW
     */
    private fun setupRecyclerView() {
        recipesAdapter = CommunityRecipesAdapter(this) { recipe ->
            // 👆 HANDLE RECIPE CARD TAPS
            openRecipeDetails(recipe)
        }
        
        recipesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recipesAdapter
        }
    }
    
    /**
     * 👂 SETUP FILTER LISTENERS
     */
    private fun setupFilterListeners() {
        
        // 🎛️ CATEGORY SPINNER
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentCategory = when (position) {
                    0 -> "All"
                    1 -> "Breakfast"
                    2 -> "Lunch"
                    3 -> "Dinner"
                    4 -> "Snack"
                    5 -> "Dessert"
                    else -> "All"
                }
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // 🔍 SEARCH TEXT CHANGES
        searchEditText.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }
        
        // ✅ FILTER CHECKBOXES
        val filterChangeListener = { _: Boolean ->
            updateActiveFilters()
            applyFilters()
        }
        
        featuredOnlyCheckbox.setOnCheckedChangeListener { _, isChecked -> filterChangeListener(isChecked) }
        highProteinCheckbox.setOnCheckedChangeListener { _, isChecked -> filterChangeListener(isChecked) }
        lowCarbCheckbox.setOnCheckedChangeListener { _, isChecked -> filterChangeListener(isChecked) }
        quickMealsCheckbox.setOnCheckedChangeListener { _, isChecked -> filterChangeListener(isChecked) }
        vegetarianOnlyCheckbox.setOnCheckedChangeListener { _, isChecked -> filterChangeListener(isChecked) }
    }
    
    /**
     * 👆 SETUP CLICK LISTENERS
     */
    private fun setupClickListeners() {
        
        // 📝 SUBMIT NEW RECIPE
        submitRecipeButton.setOnClickListener {
            openRecipeSubmissionDialog()
        }
        
        // 📦 MY RECIPE BOX
        myRecipeBoxButton.setOnClickListener {
            openPersonalRecipeBox()
        }
    }
    
    /**
     * 🔍 UPDATE ACTIVE FILTERS
     */
    private fun updateActiveFilters() {
        activeFilters.clear()
        
        if (featuredOnlyCheckbox.isChecked) activeFilters.add("featured")
        if (highProteinCheckbox.isChecked) activeFilters.add("high_protein")
        if (lowCarbCheckbox.isChecked) activeFilters.add("low_carb")
        if (quickMealsCheckbox.isChecked) activeFilters.add("quick_meals")
        if (vegetarianOnlyCheckbox.isChecked) activeFilters.add("vegetarian")
    }
    
    /**
     * 🎯 APPLY FILTERS TO RECIPE LIST
     */
    private fun applyFilters() {
        lifecycleScope.launch {
            try {
                val filteredRecipes = when {
                    // 🌟 FEATURED RECIPES ONLY
                    activeFilters.contains("featured") -> {
                        recipeManager.getFeaturedRecipes()
                    }
                    
                    // 🏷️ CATEGORY FILTERING
                    currentCategory != "All" -> {
                        recipeManager.searchRecipesByNutritionGoals().filter { recipe ->
                            recipe.categoryTag.equals(currentCategory, ignoreCase = true) &&
                            matchesActiveFilters(recipe)
                        }
                    }
                    
                    // 🔍 GENERAL FILTERING
                    else -> {
                        recipeManager.searchRecipesByNutritionGoals().filter { recipe ->
                            matchesActiveFilters(recipe)
                        }
                    }
                }
                
                recipesAdapter.updateRecipes(filteredRecipes)
                
            } catch (e: Exception) {
                Toast.makeText(context, "🔍 Error applying filters: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * ✅ CHECK IF RECIPE MATCHES ACTIVE FILTERS
     */
    private fun matchesActiveFilters(recipe: CommunityRecipe): Boolean {
        return activeFilters.all { filter ->
            when (filter) {
                "high_protein" -> (recipe.proteinPerServing ?: 0.0) >= 20.0
                "low_carb" -> (recipe.carbsPerServing ?: 0.0) <= 30.0
                "quick_meals" -> recipe.prepTimeMinutes + recipe.cookTimeMinutes <= 30
                "vegetarian" -> recipe.dietaryTags.contains("Vegetarian", ignoreCase = true)
                else -> true
            }
        }
    }
    
    /**
     * 🔍 PERFORM SEARCH
     */
    private fun performSearch() {
        val query = searchEditText.text.toString().trim()
        
        if (query.isEmpty()) {
            loadFeaturedRecipes()
            return
        }
        
        lifecycleScope.launch {
            try {
                val searchResults = recipeManager.searchRecipes(query)
                recipesAdapter.updateRecipes(searchResults)
                if (searchResults.isEmpty()) {
                    Toast.makeText(context, "No recipes found for '$query'", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Search error", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 📊 LOAD FEATURED RECIPES
     */
    private fun loadFeaturedRecipes() {
        lifecycleScope.launch {
            try {
                val featuredRecipes = recipeManager.getFeaturedRecipes()
                
                if (featuredRecipes.isNotEmpty()) {
                    recipesAdapter.updateRecipes(featuredRecipes)
                } else {
                    // 📈 FALLBACK TO TRENDING RECIPES
                    val trendingRecipes = recipeManager.getTrendingRecipes()
                    recipesAdapter.updateRecipes(trendingRecipes)
                }
                
            } catch (e: Exception) {
                Toast.makeText(context, "📊 Error loading recipes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 📖 OPEN RECIPE DETAILS
     */
    private fun openRecipeDetails(recipe: CommunityRecipe) {
        lifecycleScope.launch {
            // 👀 RECORD VIEW
            recipeManager.recordRecipeView(recipe.id)
            
            // 🚀 OPEN DETAILED VIEW
            // This would typically open a detailed fragment or activity
            Toast.makeText(context, "📖 Opening recipe: ${recipe.recipeName}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 📝 OPEN RECIPE SUBMISSION DIALOG
     */
    private fun openRecipeSubmissionDialog() {
        startActivity(android.content.Intent(requireContext(), com.calorietracker.RecipeCreateActivity::class.java))
    }

    private fun openPersonalRecipeBox() {
        // Show recommended recipes (full user favorites require account system)
        lifecycleScope.launch {
            try {
                val recommended = recipeManager.getRecipeRecommendations(10)
                recipesAdapter.updateRecipes(recommended)
                Toast.makeText(context, "Showing recommended recipes for you", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading recommendations", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * 📋 COMMUNITY RECIPES ADAPTER - RECIPE CARD LIST MANAGER
 * 
 * Manages the scrollable list of recipe cards in the community feed.
 */
class CommunityRecipesAdapter(
    private val fragment: CommunityRecipesFragment,
    private val onRecipeClick: (CommunityRecipe) -> Unit
) : RecyclerView.Adapter<CommunityRecipesAdapter.RecipeViewHolder>() {
    
    private var recipes = listOf<CommunityRecipe>()
    
    /**
     * 🔄 UPDATE RECIPE LIST
     */
    fun updateRecipes(newRecipes: List<CommunityRecipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community_recipe, parent, false)
        return RecipeViewHolder(view, fragment)
    }
    
    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(recipes[position])
    }
    
    override fun getItemCount(): Int = recipes.size
    
    /**
     * 🍽️ RECIPE VIEW HOLDER - INDIVIDUAL RECIPE CARD
     */
    inner class RecipeViewHolder(itemView: View, private val fragment: CommunityRecipesFragment) : RecyclerView.ViewHolder(itemView) {
        
        private val recipeNameText: TextView = itemView.findViewById(R.id.recipeNameText)
        private val authorNameText: TextView = itemView.findViewById(R.id.authorNameText)
        private val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        private val nutritionSummaryText: TextView = itemView.findViewById(R.id.nutritionSummaryText)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        private val reviewCountText: TextView = itemView.findViewById(R.id.reviewCountText)
        private val categoryTagText: TextView = itemView.findViewById(R.id.categoryTagText)
        private val cookTimeText: TextView = itemView.findViewById(R.id.cookTimeText)
        private val difficultyText: TextView = itemView.findViewById(R.id.difficultyText)
        private val favoriteButton: ImageButton = itemView.findViewById(R.id.favoriteButton)
        private val shareButton: ImageButton = itemView.findViewById(R.id.shareButton)
        
        /**
         * 🎨 BIND RECIPE DATA TO UI
         */
        fun bind(recipe: CommunityRecipe) {
            // 📝 BASIC INFO
            recipeNameText.text = recipe.recipeName
            authorNameText.text = "by ${recipe.authorDisplayName}"
            descriptionText.text = recipe.description
            
            // 📊 NUTRITION SUMMARY
            nutritionSummaryText.text = buildString {
                append("🔥 ${recipe.caloriesPerServing} cal")
                recipe.proteinPerServing?.let { protein ->
                    append(" • 💪 ${protein.roundToInt()}g protein")
                }
                append(" • ⏰ ${recipe.prepTimeMinutes + recipe.cookTimeMinutes} min")
            }
            
            // ⭐ RATING DISPLAY
            ratingBar.rating = recipe.totalRating
            reviewCountText.text = "(${recipe.totalReviews} reviews)"
            
            // 🏷️ CATEGORY AND METADATA
            categoryTagText.text = "🏷️ ${recipe.categoryTag}"
            cookTimeText.text = "⏰ ${recipe.prepTimeMinutes + recipe.cookTimeMinutes} min"
            difficultyText.text = "🎯 ${recipe.difficulty}"
            
            // 👆 CLICK LISTENERS
            itemView.setOnClickListener {
                onRecipeClick(recipe)
            }
            
            favoriteButton.setOnClickListener {
                handleFavoriteClick(recipe)
            }
            
            shareButton.setOnClickListener {
                handleShareClick(recipe)
            }
        }
        
        /**
         * ⭐ HANDLE FAVORITE BUTTON CLICK
         */
        private fun handleFavoriteClick(recipe: CommunityRecipe) {
            // 💾 SAVE TO FAVORITES
            fragment.lifecycleScope.launch {
                try {
                    val userId = "user_${System.currentTimeMillis()}" // Simplified user ID
                    fragment.recipeManager.addToFavorites(userId, recipe.id, "Saved from community")
                    
                    Toast.makeText(itemView.context, "⭐ Saved to your recipe box!", Toast.LENGTH_SHORT).show()
                    
                } catch (e: Exception) {
                    Toast.makeText(itemView.context, "❌ Error saving recipe: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        /**
         * 📱 HANDLE SHARE BUTTON CLICK
         */
        private fun handleShareClick(recipe: CommunityRecipe) {
            fragment.lifecycleScope.launch {
                // 📊 RECORD SHARE
                fragment.recipeManager.recordRecipeShare(recipe.id)
                
                // 📱 CREATE SHARE CONTENT
                val shareText = buildString {
                    append("🍽️ Check out this healthy recipe!\n\n")
                    append("📝 ${recipe.recipeName}\n")
                    append("👨‍🍳 by ${recipe.authorDisplayName}\n\n")
                    append("📊 ${recipe.caloriesPerServing} calories")
                    recipe.proteinPerServing?.let { protein ->
                        append(" • ${protein.roundToInt()}g protein")
                    }
                    append("\n")
                    append("⏰ ${recipe.prepTimeMinutes + recipe.cookTimeMinutes} minutes to make\n")
                    append("⭐ ${recipe.totalRating}/5 stars (${recipe.totalReviews} reviews)\n\n")
                    append("📝 ${recipe.description}\n\n")
                    append("🏷️ #HealthyRecipes #CalorieTracker #Community")
                }
                
                // 📱 SHARE VIA ANDROID INTENT
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Healthy Recipe: ${recipe.recipeName}")
                }
                
                val chooser = android.content.Intent.createChooser(shareIntent, "Share this recipe")
                itemView.context.startActivity(chooser)
            }
        }
    }
}