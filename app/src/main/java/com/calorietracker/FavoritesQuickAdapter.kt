package com.calorietracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.FavoriteMeal

/**
 * Compact adapter for showing favorite meals in a horizontal scrolling list on the main screen
 * Optimized for quick one-tap adding of favorite foods
 */
class FavoritesQuickAdapter(
    private val onFavoriteClick: (FavoriteMeal) -> Unit,
    private val onFavoriteLongClick: ((FavoriteMeal) -> Unit)? = null
) : ListAdapter<FavoriteMeal, FavoritesQuickAdapter.FavoriteViewHolder>(FavoriteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_quick, parent, false)
        return FavoriteViewHolder(view, onFavoriteClick, onFavoriteLongClick)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FavoriteViewHolder(
        itemView: View,
        private val onFavoriteClick: (FavoriteMeal) -> Unit,
        private val onFavoriteLongClick: ((FavoriteMeal) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvFoodName: TextView = itemView.findViewById(R.id.tvFoodName)
        private val tvBrand: TextView = itemView.findViewById(R.id.tvBrand)
        private val tvCalories: TextView = itemView.findViewById(R.id.tvCalories)
        private val tvUsageCount: TextView = itemView.findViewById(R.id.tvUsageCount)

        fun bind(favorite: FavoriteMeal) {
            tvFoodName.text = favorite.foodName
            tvBrand.text = favorite.brand ?: ""
            tvCalories.text = "${favorite.calories} cal"
            
            // Show usage count if used more than once
            if (favorite.timesUsed > 1) {
                tvUsageCount.text = "${favorite.timesUsed}x"
                tvUsageCount.visibility = View.VISIBLE
            } else {
                tvUsageCount.visibility = View.GONE
            }
            
            // Set click listeners
            itemView.setOnClickListener { onFavoriteClick(favorite) }
            onFavoriteLongClick?.let { longClickHandler ->
                itemView.setOnLongClickListener { 
                    longClickHandler(favorite)
                    true
                }
            }
            
            // Show brand only if it exists and fits
            if (favorite.brand.isNullOrBlank()) {
                tvBrand.visibility = View.GONE
            } else {
                tvBrand.visibility = View.VISIBLE
            }
        }
    }

    class FavoriteDiffCallback : DiffUtil.ItemCallback<FavoriteMeal>() {
        override fun areItemsTheSame(oldItem: FavoriteMeal, newItem: FavoriteMeal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FavoriteMeal, newItem: FavoriteMeal): Boolean {
            return oldItem == newItem
        }
    }
}