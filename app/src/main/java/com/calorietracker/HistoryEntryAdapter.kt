package com.calorietracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.CalorieEntry
import java.text.SimpleDateFormat
import java.util.*

class HistoryEntryAdapter : ListAdapter<CalorieEntry, HistoryEntryAdapter.HistoryEntryViewHolder>(DiffCallback) {
    
    class HistoryEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHistoryFoodName: TextView = itemView.findViewById(R.id.tvHistoryFoodName)
        private val tvHistoryDate: TextView = itemView.findViewById(R.id.tvHistoryDate)
        private val tvHistoryCalories: TextView = itemView.findViewById(R.id.tvHistoryCalories)
        private val tvHistoryProtein: TextView = itemView.findViewById(R.id.tvHistoryProtein)
        private val tvHistoryCarbs: TextView = itemView.findViewById(R.id.tvHistoryCarbs)
        private val tvHistoryFat: TextView = itemView.findViewById(R.id.tvHistoryFat)
        private val tvHistoryFiber: TextView = itemView.findViewById(R.id.tvHistoryFiber)
        private val nutritionLayout: View = itemView.findViewById(R.id.layoutHistoryNutrition)
        
        fun bind(entry: CalorieEntry) {
            tvHistoryFoodName.text = entry.foodName
            tvHistoryCalories.text = "${entry.calories} cal"
            
            // Format the date and time
            val dateTime = formatDateTime(entry.timestamp)
            tvHistoryDate.text = dateTime
            
            // Show nutrition info if available
            val hasNutritionInfo = entry.protein != null || entry.carbs != null || 
                                  entry.fat != null || entry.fiber != null
            
            if (hasNutritionInfo) {
                nutritionLayout.visibility = View.VISIBLE
                
                tvHistoryProtein.text = if (entry.protein != null) {
                    "P: ${String.format("%.1f", entry.protein)}g"
                } else "P: --"
                
                tvHistoryCarbs.text = if (entry.carbs != null) {
                    "C: ${String.format("%.1f", entry.carbs)}g"
                } else "C: --"
                
                tvHistoryFat.text = if (entry.fat != null) {
                    "F: ${String.format("%.1f", entry.fat)}g"
                } else "F: --"
                
                tvHistoryFiber.text = if (entry.fiber != null) {
                    "Fiber: ${String.format("%.1f", entry.fiber)}g"
                } else "Fiber: --"
            } else {
                nutritionLayout.visibility = View.GONE
            }
        }
        
        private fun formatDateTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val timeString = timeFormat.format(Date(timestamp))
            
            return when {
                diff < 24 * 60 * 60 * 1000 -> "Today, $timeString"
                diff < 48 * 60 * 60 * 1000 -> "Yesterday, $timeString"
                else -> {
                    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                    "${dateFormat.format(Date(timestamp))}, $timeString"
                }
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryEntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_entry, parent, false)
        return HistoryEntryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: HistoryEntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    companion object DiffCallback : DiffUtil.ItemCallback<CalorieEntry>() {
        override fun areItemsTheSame(oldItem: CalorieEntry, newItem: CalorieEntry): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: CalorieEntry, newItem: CalorieEntry): Boolean {
            return oldItem == newItem
        }
    }
}