package com.calorietracker

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.ShoppingListItem

/**
 * Adapter for displaying shopping list items with check-off functionality
 * Supports category grouping and item management
 */
class ShoppingListAdapter(
    private val onItemClick: (ShoppingListItem) -> Unit,
    private val onItemLongClick: (ShoppingListItem) -> Unit,
    private val onItemEdit: (ShoppingListItem) -> Unit,
    private val onItemDelete: (ShoppingListItem) -> Unit
) : ListAdapter<ShoppingListItem, ShoppingListAdapter.ShoppingItemViewHolder>(ShoppingItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false)
        return ShoppingItemViewHolder(view, onItemClick, onItemLongClick, onItemEdit, onItemDelete)
    }

    override fun onBindViewHolder(holder: ShoppingItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ShoppingItemViewHolder(
        itemView: View,
        private val onItemClick: (ShoppingListItem) -> Unit,
        private val onItemLongClick: (ShoppingListItem) -> Unit,
        private val onItemEdit: (ShoppingListItem) -> Unit,
        private val onItemDelete: (ShoppingListItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cbChecked: CheckBox = itemView.findViewById(R.id.cbChecked)
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvNotes: TextView = itemView.findViewById(R.id.tvNotes)
        private val tvCost: TextView = itemView.findViewById(R.id.tvCost)

        fun bind(item: ShoppingListItem) {
            tvItemName.text = item.itemName
            cbChecked.isChecked = item.isChecked
            
            // Show quantity if available
            if (!item.quantity.isNullOrBlank()) {
                tvQuantity.text = item.quantity
                tvQuantity.visibility = View.VISIBLE
            } else {
                tvQuantity.visibility = View.GONE
            }
            
            // Show category
            tvCategory.text = item.category ?: "Other"
            
            // Show notes if available
            if (!item.notes.isNullOrBlank()) {
                tvNotes.text = item.notes
                tvNotes.visibility = View.VISIBLE
            } else {
                tvNotes.visibility = View.GONE
            }
            
            // Show estimated cost if available
            if (item.estimatedCost != null && item.estimatedCost > 0) {
                tvCost.text = "$${String.format("%.2f", item.estimatedCost)}"
                tvCost.visibility = View.VISIBLE
            } else {
                tvCost.visibility = View.GONE
            }
            
            // Apply strikethrough and alpha for checked items
            if (item.isChecked) {
                tvItemName.paintFlags = tvItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                itemView.alpha = 0.6f
            } else {
                tvItemName.paintFlags = tvItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                itemView.alpha = 1.0f
            }
            
            // Color code by category
            val categoryColor = when (item.category?.lowercase()) {
                "produce" -> itemView.context.getColor(R.color.success_green)
                "dairy" -> itemView.context.getColor(R.color.accent_blue)
                "meat" -> itemView.context.getColor(R.color.error_red)
                "pantry" -> itemView.context.getColor(R.color.accent_orange)
                "frozen" -> itemView.context.getColor(R.color.accent_blue)
                "beverages" -> itemView.context.getColor(R.color.accent_purple)
                else -> itemView.context.getColor(R.color.text_secondary)
            }
            tvCategory.setTextColor(categoryColor)
            
            // Priority indicator
            when (item.priority) {
                1 -> itemView.setBackgroundColor(itemView.context.getColor(R.color.warning_orange))
                -1 -> itemView.alpha = itemView.alpha * 0.8f
                else -> itemView.setBackgroundColor(itemView.context.getColor(R.color.surface_elevated))
            }
            
            // Set up click listeners
            cbChecked.setOnClickListener { onItemClick(item) }
            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener { 
                onItemLongClick(item)
                true 
            }
            
            // Show staple indicator
            if (item.isStaple) {
                tvItemName.text = "📌 ${item.itemName}"
            }
            
            // Show recurring indicator
            if (item.isRecurring) {
                tvItemName.text = "${tvItemName.text} 🔄"
            }
        }
    }

    class ShoppingItemDiffCallback : DiffUtil.ItemCallback<ShoppingListItem>() {
        override fun areItemsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ShoppingListItem, newItem: ShoppingListItem): Boolean {
            return oldItem == newItem
        }
    }
}