package com.calorietracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.RecipeIngredient
import com.calorietracker.database.getDisplayText
import com.calorietracker.database.getNutritionSummary

class RecipeIngredientsAdapter(
    private val onRemoveClick: (RecipeIngredient) -> Unit,
    private val onEditClick: (RecipeIngredient) -> Unit
) : ListAdapter<RecipeIngredient, RecipeIngredientsAdapter.ViewHolder>(IngredientDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_ingredient, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIngredientName: TextView = itemView.findViewById(R.id.tvIngredientName)
        private val tvIngredientDetails: TextView = itemView.findViewById(R.id.tvIngredientDetails)
        private val tvNutritionSummary: TextView = itemView.findViewById(R.id.tvNutritionSummary)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditIngredient)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemoveIngredient)
        
        fun bind(ingredient: RecipeIngredient) {
            tvIngredientName.text = ingredient.ingredientName
            tvIngredientDetails.text = ingredient.getDisplayText()
            tvNutritionSummary.text = ingredient.getNutritionSummary()
            
            btnEdit.setOnClickListener { onEditClick(ingredient) }
            btnRemove.setOnClickListener { onRemoveClick(ingredient) }
        }
    }
    
    class IngredientDiffCallback : DiffUtil.ItemCallback<RecipeIngredient>() {
        override fun areItemsTheSame(oldItem: RecipeIngredient, newItem: RecipeIngredient): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: RecipeIngredient, newItem: RecipeIngredient): Boolean {
            return oldItem == newItem
        }
    }
}