package com.calorietracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.Recipe
import com.calorietracker.ui.CommunityRecipesFragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

class RecipeLibraryActivity : AppCompatActivity() {
    
    private lateinit var viewModel: RecipeLibraryViewModel
    private lateinit var recipesAdapter: RecipeLibraryAdapter
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabCreateRecipe: FloatingActionButton
    private lateinit var emptyStateView: View
    private lateinit var tabLayout: TabLayout
    private lateinit var containerMyRecipes: FrameLayout
    private lateinit var containerCommunity: FrameLayout
    private var communityLoaded = false
    
    companion object {
        private const val REQUEST_CREATE_RECIPE = 1001
        private const val REQUEST_EDIT_RECIPE = 1002
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_library)
        
        viewModel = ViewModelProvider(this)[RecipeLibraryViewModel::class.java]
        
        initViews()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        // Load recipes
        viewModel.loadRecipes()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CREATE_RECIPE, REQUEST_EDIT_RECIPE -> {
                    // Reload recipes after create/edit
                    viewModel.loadRecipes()
                }
            }
        }
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewRecipes)
        fabCreateRecipe = findViewById(R.id.fabCreateRecipe)
        emptyStateView = findViewById(R.id.layoutEmptyState)
        tabLayout = findViewById(R.id.tabLayout)
        containerMyRecipes = findViewById(R.id.containerMyRecipes)
        containerCommunity = findViewById(R.id.containerCommunity)

        tabLayout.addTab(tabLayout.newTab().setText("My Recipes"))
        tabLayout.addTab(tabLayout.newTab().setText("Community"))

        // Edge-to-edge (enforced on SDK 35+): shift the FAB above the system nav bar
        ViewCompat.setOnApplyWindowInsetsListener(fabCreateRecipe) { v, windowInsets ->
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val mlp = v.layoutParams as android.view.ViewGroup.MarginLayoutParams
            mlp.bottomMargin = navBars.bottom + (16 * resources.displayMetrics.density).toInt()
            v.layoutParams = mlp
            windowInsets
        }
    }
    
    private fun setupRecyclerView() {
        recipesAdapter = RecipeLibraryAdapter(
            onRecipeClick = { recipe ->
                openRecipeDetails(recipe)
            },
            onEditClick = { recipe ->
                editRecipe(recipe)
            },
            onShareClick = { recipe ->
                shareRecipe(recipe)
            },
            onDeleteClick = { recipe ->
                viewModel.deleteRecipe(recipe)
            },
            onToggleFavoriteClick = { recipe ->
                viewModel.toggleFavorite(recipe)
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = recipesAdapter
    }
    
    private fun setupListeners() {
        fabCreateRecipe.setOnClickListener {
            createNewRecipe()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        containerMyRecipes.visibility = View.VISIBLE
                        containerCommunity.visibility = View.GONE
                        fabCreateRecipe.show()
                    }
                    1 -> {
                        containerMyRecipes.visibility = View.GONE
                        containerCommunity.visibility = View.VISIBLE
                        fabCreateRecipe.hide()
                        if (!communityLoaded) {
                            communityLoaded = true
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.containerCommunity, CommunityRecipesFragment())
                                .commit()
                        }
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    
    private fun observeViewModel() {
        viewModel.recipes.observe(this) { recipes ->
            recipesAdapter.submitList(recipes)
            
            val isEmpty = recipes.isEmpty()
            emptyStateView.visibility = if (isEmpty) View.VISIBLE else View.GONE
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
        
        viewModel.deleteResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Recipe deleted", Toast.LENGTH_SHORT).show()
                viewModel.loadRecipes() // Refresh list
            } else {
                Toast.makeText(this, "Error deleting recipe", Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.favoriteUpdateResult.observe(this) { success ->
            if (success) {
                // Refresh the list to show updated favorite status
                viewModel.loadRecipes()
            }
        }
        
        viewModel.shareData.observe(this) { shareData ->
            shareData?.let { (recipe, ingredients) ->
                val shareManager = RecipeShareManager(this)
                val shareText = shareManager.createShareableText(recipe, ingredients)
                
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, "Recipe: ${recipe.name}")
                }
                
                startActivity(Intent.createChooser(shareIntent, "Share Recipe"))
                
                // Clear the share data after use
                viewModel.clearShareData()
            }
        }
    }
    
    private fun createNewRecipe() {
        val intent = Intent(this, RecipeCreateActivity::class.java)
        startActivityForResult(intent, REQUEST_CREATE_RECIPE)
    }
    
    private fun editRecipe(recipe: Recipe) {
        val intent = Intent(this, RecipeCreateActivity::class.java).apply {
            putExtra("recipe_id", recipe.id)
        }
        startActivityForResult(intent, REQUEST_EDIT_RECIPE)
    }
    
    private fun openRecipeDetails(recipe: Recipe) {
        // For now, open in edit mode since we don't have a detail view yet
        editRecipe(recipe)
    }
    
    private fun shareRecipe(recipe: Recipe) {
        // Show comprehensive sharing dialog with QR code
        val shareDialog = RecipeQRShareDialog(this, recipe)
        shareDialog.show()
    }
}