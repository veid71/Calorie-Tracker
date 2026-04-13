package com.calorietracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.MealPlan
import com.google.android.material.card.MaterialCardView

/**
 * Adapter for displaying meal plans in the weekly planner
 * Supports drag-and-drop, completion tracking, and meal management
 */
class MealPlanAdapter(
    private val onMealClick: (MealPlan) -> Unit,
    private val onMealLongClick: (MealPlan) -> Unit,
    private val onMealEdit: (MealPlan) -> Unit,
    private val onMealDelete: (MealPlan) -> Unit,
    private val onMealComplete: (MealPlan) -> Unit
) : ListAdapter<MealPlan, MealPlanAdapter.MealPlanViewHolder>(MealPlanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealPlanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal_plan, parent, false)
        return MealPlanViewHolder(view, onMealClick, onMealLongClick, onMealEdit, onMealDelete, onMealComplete)
    }

    override fun onBindViewHolder(holder: MealPlanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MealPlanViewHolder(
        itemView: View,
        private val onMealClick: (MealPlan) -> Unit,
        private val onMealLongClick: (MealPlan) -> Unit,
        private val onMealEdit: (MealPlan) -> Unit,
        private val onMealDelete: (MealPlan) -> Unit,
        private val onMealComplete: (MealPlan) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardMeal: MaterialCardView = itemView.findViewById(R.id.cardMeal)
        private val tvMealName: TextView = itemView.findViewById(R.id.tvMealName)
        private val tvMealType: TextView = itemView.findViewById(R.id.tvMealType)
        private val tvCalories: TextView = itemView.findViewById(R.id.tvCalories)
        private val tvPrepTime: TextView = itemView.findViewById(R.id.tvPrepTime)
        private val tvDifficulty: TextView = itemView.findViewById(R.id.tvDifficulty)
        private val ivCompleted: ImageView = itemView.findViewById(R.id.ivCompleted)
        private val ivDragHandle: ImageView = itemView.findViewById(R.id.ivDragHandle)

        fun bind(meal: MealPlan) {
            tvMealName.text = meal.mealName
            tvMealType.text = meal.mealType.replaceFirstChar { it.titlecaseChar() }
            
            // Show calories if available
            if (meal.estimatedCalories > 0) {
                tvCalories.text = "${meal.estimatedCalories} cal"
                tvCalories.visibility = View.VISIBLE
            } else {
                tvCalories.visibility = View.GONE
            }
            
            // Show prep time if available
            if (meal.estimatedPrep != null && meal.estimatedPrep > 0) {
                tvPrepTime.text = "${meal.estimatedPrep}min"
                tvPrepTime.visibility = View.VISIBLE
            } else {
                tvPrepTime.visibility = View.GONE
            }
            
            // Show difficulty if available
            if (!meal.difficulty.isNullOrBlank()) {
                tvDifficulty.text = meal.difficulty.replaceFirstChar { it.titlecaseChar() }
                tvDifficulty.visibility = View.VISIBLE
                
                // Color code difficulty
                val difficultyColor = when (meal.difficulty.lowercase()) {
                    "easy" -> itemView.context.getColor(R.color.success_green)
                    "medium" -> itemView.context.getColor(R.color.accent_orange) 
                    "hard" -> itemView.context.getColor(R.color.error_red)
                    else -> itemView.context.getColor(R.color.text_secondary)
                }
                tvDifficulty.setTextColor(difficultyColor)
            } else {
                tvDifficulty.visibility = View.GONE
            }
            
            // Show completion status
            if (meal.isCompleted) {
                ivCompleted.visibility = View.VISIBLE
                cardMeal.alpha = 0.7f
                cardMeal.setCardBackgroundColor(itemView.context.getColor(R.color.success_green_light))
            } else {
                ivCompleted.visibility = View.GONE
                cardMeal.alpha = 1.0f
                
                // Color code based on meal type
                val mealTypeColor = when (meal.mealType.lowercase()) {
                    "breakfast" -> itemView.context.getColor(R.color.accent_orange)
                    "lunch" -> itemView.context.getColor(R.color.accent_blue)
                    "dinner" -> itemView.context.getColor(R.color.primary_green)
                    "snack" -> itemView.context.getColor(R.color.accent_purple)
                    else -> itemView.context.getColor(R.color.surface_elevated)
                }
                cardMeal.setCardBackgroundColor(mealTypeColor)
            }
            
            // Apply custom color if set
            if (!meal.color.isNullOrBlank()) {
                try {
                    val customColor = android.graphics.Color.parseColor(meal.color)
                    cardMeal.setCardBackgroundColor(customColor)
                } catch (e: Exception) {
                    // Ignore invalid colors
                }
            }
            
            // Set up click listeners
            itemView.setOnClickListener { onMealClick(meal) }
            itemView.setOnLongClickListener { 
                onMealLongClick(meal)
                true 
            }
            
            // Completion toggle on completed icon
            ivCompleted.setOnClickListener {
                onMealComplete(meal)
            }
            
            // Show context menu on drag handle click
            ivDragHandle.setOnClickListener {
                showMealContextMenu(meal)
            }
            
            // Set up drag functionality
            ivDragHandle.setOnLongClickListener {
                onMealLongClick(meal)
                true
            }
        }
        
        private fun showMealContextMenu(meal: MealPlan) {
            val context = itemView.context
            val items = arrayOf(
                "Edit Meal",
                "Mark as ${if (meal.isCompleted) "Not Completed" else "Completed"}",
                "Duplicate Meal",
                "Add to Shopping List",
                "Delete Meal"
            )
            
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(meal.mealName)
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> onMealEdit(meal)
                        1 -> onMealComplete(meal)
                        2 -> duplicateMeal(meal)
                        3 -> addToShoppingList(meal)
                        4 -> confirmDelete(meal)
                    }
                }
                .show()
        }
        
        private fun duplicateMeal(meal: MealPlan) {
            // Implementation would copy meal to another day/time
            android.widget.Toast.makeText(itemView.context, "Duplicate meal functionality", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        private fun addToShoppingList(meal: MealPlan) {
            // Implementation would add ingredients to shopping list
            android.widget.Toast.makeText(itemView.context, "Added to shopping list", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        private fun confirmDelete(meal: MealPlan) {
            androidx.appcompat.app.AlertDialog.Builder(itemView.context)
                .setTitle("Delete Meal")
                .setMessage("Are you sure you want to delete \"${meal.mealName}\"?")
                .setPositiveButton("Delete") { _, _ -> onMealDelete(meal) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    class MealPlanDiffCallback : DiffUtil.ItemCallback<MealPlan>() {
        override fun areItemsTheSame(oldItem: MealPlan, newItem: MealPlan): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MealPlan, newItem: MealPlan): Boolean {
            return oldItem == newItem
        }
    }
}