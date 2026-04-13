package com.calorietracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.BarcodeHistory
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying barcode scanning history in a compact horizontal list
 * Shows recently scanned items for quick re-selection
 */
class BarcodeHistoryAdapter(
    private val onItemClick: (BarcodeHistory) -> Unit
) : ListAdapter<BarcodeHistory, BarcodeHistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_barcode_history, parent, false)
        return HistoryViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(
        itemView: View,
        private val onItemClick: (BarcodeHistory) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvFoodName: TextView = itemView.findViewById(R.id.tvFoodName)
        private val tvBrand: TextView = itemView.findViewById(R.id.tvBrand)
        private val tvCalories: TextView = itemView.findViewById(R.id.tvCalories)
        private val tvTimesScanned: TextView = itemView.findViewById(R.id.tvTimesScanned)
        private val tvLastScanned: TextView = itemView.findViewById(R.id.tvLastScanned)

        fun bind(history: BarcodeHistory) {
            tvFoodName.text = history.foodName
            tvBrand.text = history.brand ?: ""
            tvCalories.text = "${history.calories} cal"
            
            // Show scan count if > 1
            if (history.timesScanned > 1) {
                tvTimesScanned.text = "${history.timesScanned}x"
                tvTimesScanned.visibility = View.VISIBLE
            } else {
                tvTimesScanned.visibility = View.GONE
            }
            
            // Show relative time
            val timeFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
            tvLastScanned.text = formatRelativeTime(history.lastScanned)
            
            // Set click listener
            itemView.setOnClickListener { onItemClick(history) }
            
            // Visual indication of success/failure
            val bgColor = if (history.wasSuccessful) {
                itemView.context.getColor(R.color.surface_elevated)
            } else {
                itemView.context.getColor(R.color.error_background)
            }
            itemView.setBackgroundColor(bgColor)
        }
        
        private fun formatRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60 * 1000 -> "Just now"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
                diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
                else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<BarcodeHistory>() {
        override fun areItemsTheSame(oldItem: BarcodeHistory, newItem: BarcodeHistory): Boolean {
            return oldItem.barcode == newItem.barcode
        }

        override fun areContentsTheSame(oldItem: BarcodeHistory, newItem: BarcodeHistory): Boolean {
            return oldItem == newItem
        }
    }
}