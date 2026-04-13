package com.calorietracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.*

class RecipeLibraryAdapter(
    private val onRecipeClick: (Recipe) -> Unit,
    private val onEditClick: (Recipe) -> Unit,
    private val onShareClick: (Recipe) -> Unit,
    private val onDeleteClick: (Recipe) -> Unit,
    private val onToggleFavoriteClick: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeLibraryAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_library, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRecipeName: TextView = itemView.findViewById(R.id.tvRecipeName)
        private val tvRecipeDescription: TextView = itemView.findViewById(R.id.tvRecipeDescription)
        private val tvServings: TextView = itemView.findViewById(R.id.tvServings)
        private val tvCaloriesPerServing: TextView = itemView.findViewById(R.id.tvCaloriesPerServing)
        private val tvNutrition: TextView = itemView.findViewById(R.id.tvNutrition)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvTimes: TextView = itemView.findViewById(R.id.tvTimes)
        private val tvTimesUsed: TextView = itemView.findViewById(R.id.tvTimesUsed)
        
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnShare: ImageButton = itemView.findViewById(R.id.btnShare)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)

        fun bind(recipe: Recipe) {
            tvRecipeName.text = recipe.name
            
            // Description (show/hide based on availability)
            if (!recipe.description.isNullOrBlank()) {
                tvRecipeDescription.text = recipe.description
                tvRecipeDescription.visibility = View.VISIBLE
            } else {
                tvRecipeDescription.visibility = View.GONE
            }
            
            // Basic info
            tvServings.text = "${recipe.servings} servings"
            tvCaloriesPerServing.text = "${recipe.getCaloriesPerServing()} cal"
            
            // Nutrition summary
            val nutrition = buildString {
                if (recipe.hasNutritionData()) {
                    append("${String.format("%.1f", recipe.getProteinPerServing())}g protein • ")
                    append("${String.format("%.1f", recipe.getCarbsPerServing())}g carbs • ")
                    append("${String.format("%.1f", recipe.getFatPerServing())}g fat")
                } else {
                    append("Nutrition data not available")
                }
            }
            tvNutrition.text = nutrition
            
            // Category
            if (!recipe.category.isNullOrBlank()) {
                tvCategory.text = recipe.category
                tvCategory.visibility = View.VISIBLE
                setCategoryIcon(recipe.category)
            } else {
                tvCategory.visibility = View.GONE
                ivCategoryIcon.visibility = View.GONE
            }
            
            // Times (prep + cook)
            val totalTime = recipe.getTotalTime()
            if (totalTime != null) {
                tvTimes.text = "${totalTime} min total"
                tvTimes.visibility = View.VISIBLE
            } else {
                if (recipe.prepTime != null || recipe.cookTime != null) {
                    val timeText = buildString {
                        recipe.prepTime?.let { append("${it}m prep") }
                        if (recipe.prepTime != null && recipe.cookTime != null) append(" • ")
                        recipe.cookTime?.let { append("${it}m cook") }
                    }
                    tvTimes.text = timeText
                    tvTimes.visibility = View.VISIBLE
                } else {
                    tvTimes.visibility = View.GONE
                }
            }
            
            // Usage count
            if (recipe.timesUsed > 0) {
                tvTimesUsed.text = "Used ${recipe.timesUsed} times"
                tvTimesUsed.visibility = View.VISIBLE
            } else {
                tvTimesUsed.visibility = View.GONE
            }
            
            // Favorite button
            btnFavorite.setImageResource(
                if (recipe.isFavorite) android.R.drawable.btn_star_big_on 
                else android.R.drawable.btn_star_big_off
            )
            
            // Click listeners
            itemView.setOnClickListener { onRecipeClick(recipe) }
            btnFavorite.setOnClickListener { onToggleFavoriteClick(recipe) }
            btnEdit.setOnClickListener { onEditClick(recipe) }
            btnShare.setOnClickListener { onShareClick(recipe) }
            btnDelete.setOnClickListener { onDeleteClick(recipe) }
        }
        
        private fun setCategoryIcon(category: String) {
            ivCategoryIcon.visibility = View.VISIBLE
            // Use generic system icons for now
            val iconRes = when (category.lowercase()) {
                "breakfast" -> android.R.drawable.ic_menu_today
                "lunch" -> android.R.drawable.ic_menu_today
                "dinner" -> android.R.drawable.ic_menu_today
                "dessert", "desserts" -> android.R.drawable.ic_menu_today
                "snack", "snacks" -> android.R.drawable.ic_menu_today
                "appetizer", "appetizers" -> android.R.drawable.ic_menu_today
                "soup", "soups" -> android.R.drawable.ic_menu_today
                "salad", "salads" -> android.R.drawable.ic_menu_today
                "beverage", "beverages", "drink", "drinks" -> android.R.drawable.ic_menu_today
                else -> android.R.drawable.ic_menu_info_details
            }
            ivCategoryIcon.setImageResource(iconRes)
        }
    }

    class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem == newItem
        }
    }
}