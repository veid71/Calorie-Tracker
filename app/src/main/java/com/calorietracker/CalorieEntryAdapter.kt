package com.calorietracker

// 🧰 UI BUILDING TOOLS
import android.view.LayoutInflater      // Creates new views from XML layouts
import android.view.View               // Basic building block for everything on screen
import android.view.ViewGroup          // Container that holds other views
import android.widget.TextView         // Text that displays words and numbers
import androidx.recyclerview.widget.DiffUtil      // Smart tool that efficiently updates lists
import androidx.recyclerview.widget.ListAdapter  // Special adapter optimized for lists
import androidx.recyclerview.widget.RecyclerView  // Efficient scrolling list
import com.calorietracker.database.CalorieEntry   // Our food entry data

/**
 * 📋 CALORIE ENTRY ADAPTER - THE LIST MANAGER
 * 
 * Hey young programmer! This is like the person who arranges items in a display case.
 * 
 * 🎭 What does an "Adapter" do?
 * An adapter is like a translator between our data and what you see on screen.
 * Think of it like this:
 * - Our data: "CalorieEntry(foodName='Apple', calories=95, protein=0.5)"
 * - What you see: A pretty card showing "Apple - 95 cal - P: 0.5g"
 * 
 * 🔄 How does it work?
 * 1. Our app gives the adapter a list of food entries
 * 2. The adapter creates a visual card for each food entry
 * 3. When you scroll, it efficiently shows/hides cards as needed
 * 4. When data changes, it smoothly updates just the changed items
 * 
 * 👆 Interactive Features:
 * - Single tap: Edit the food entry (fix wrong calories, etc.)
 * - Long press: Delete the food entry (with confirmation)
 * 
 * 🧠 Why use ListAdapter?
 * It's super smart! When the food list changes, it figures out exactly what
 * changed and animates only those items. Much more efficient than rebuilding
 * the entire list every time.
 */
class CalorieEntryAdapter(
    // 📝 CALLBACK FUNCTIONS - What happens when user interacts with food entries
    private val onEditClick: ((CalorieEntry) -> Unit)? = null,    // Function to call when user taps to edit
    private val onDeleteClick: ((CalorieEntry) -> Unit)? = null   // Function to call when user long-presses to delete
) : ListAdapter<CalorieEntry, CalorieEntryAdapter.CalorieEntryViewHolder>(DiffCallback) {
    
    class CalorieEntryViewHolder(
        itemView: View,
        private val onEditClick: ((CalorieEntry) -> Unit)?,
        private val onDeleteClick: ((CalorieEntry) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvFoodName: TextView = itemView.findViewById(R.id.tvFoodName)
        private val tvCalories: TextView = itemView.findViewById(R.id.tvCalories)
        private val tvProtein: TextView = itemView.findViewById(R.id.tvProtein)
        private val tvCarbs: TextView = itemView.findViewById(R.id.tvCarbs)
        private val tvFat: TextView = itemView.findViewById(R.id.tvFat)
        private val nutritionLayout: View = itemView.findViewById(R.id.layoutNutritionInfo)
        
        fun bind(entry: CalorieEntry) {
            tvFoodName.text = entry.foodName
            tvCalories.text = "${entry.calories} cal"
            
            // Show nutrition info if available
            val hasNutritionInfo = entry.protein != null || entry.carbs != null || entry.fat != null
            if (hasNutritionInfo) {
                nutritionLayout.visibility = View.VISIBLE
                
                if (entry.protein != null) {
                    tvProtein.text = "P: ${String.format("%.1f", entry.protein)}g"
                    tvProtein.visibility = View.VISIBLE
                } else {
                    tvProtein.visibility = View.GONE
                }
                
                if (entry.carbs != null) {
                    tvCarbs.text = "C: ${String.format("%.1f", entry.carbs)}g"
                    tvCarbs.visibility = View.VISIBLE
                } else {
                    tvCarbs.visibility = View.GONE
                }
                
                if (entry.fat != null) {
                    tvFat.text = "F: ${String.format("%.1f", entry.fat)}g"
                    tvFat.visibility = View.VISIBLE
                } else {
                    tvFat.visibility = View.GONE
                }
            } else {
                nutritionLayout.visibility = View.GONE
            }
            
            // Set up click listeners for edit/delete
            itemView.setOnClickListener {
                onEditClick?.invoke(entry)
            }
            
            itemView.setOnLongClickListener {
                onDeleteClick?.invoke(entry)
                true
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalorieEntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calorie_entry, parent, false)
        return CalorieEntryViewHolder(view, onEditClick, onDeleteClick)
    }
    
    override fun onBindViewHolder(holder: CalorieEntryViewHolder, position: Int) {
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