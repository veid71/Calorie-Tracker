package com.calorietracker.adapters

// 🧰 IMPORTING THE TOOLS WE NEED TO BUILD OUR FOOD SEARCH LIST
import android.view.LayoutInflater    // Creates new list items from XML layouts
import android.view.View              // Basic building block for things you see on screen
import android.view.ViewGroup         // Container that holds other views
import android.widget.TextView        // Displays text on screen
import androidx.recyclerview.widget.RecyclerView  // Smart list that can handle thousands of items efficiently
import com.calorietracker.R          // Links to our app's resources (layouts, colors, etc.)
import com.calorietracker.database.FoodItem       // Data structure representing one food item

/**
 * 📋 FOOD SEARCH RESULTS ADAPTER - THE LIST MANAGER FOR SEARCH RESULTS
 * 
 * Hey future programmer! This class is like the "list manager" for our food search results.
 * Think of it like a waiter at a restaurant who:
 * 
 * 🍽️ What does this list manager do?
 * 1. Takes a list of food items (like "Apple", "Banana", "Chicken Breast")
 * 2. Creates a visual list item for each food (shows name, calories, protein)
 * 3. Handles when users tap on a food item
 * 4. Updates the list when new search results come in
 * 
 * 🤔 Why do we need an "Adapter"?
 * Android's RecyclerView (the smart list) doesn't know what our food data looks like.
 * The adapter acts like a translator: "Hey RecyclerView, here's how to display this food data!"
 * 
 * 🎯 Key Features:
 * - Memory efficient: Only creates list items that are visible on screen
 * - Shows food name, brand, calories, and protein content
 * - Handles user taps to select foods
 * - Can update the list without recreating everything
 */
class FoodSearchResultsAdapter(
    // 📋 The list of food items we want to display
    private var foodItems: List<FoodItem> = emptyList(),
    // 🖱️ Function to call when user taps on a food item
    private val onItemClick: (FoodItem) -> Unit
) : RecyclerView.Adapter<FoodSearchResultsAdapter.ViewHolder>() {

    /**
     * 📦 VIEW HOLDER - LIKE A CONTAINER FOR ONE LIST ITEM
     * 
     * This class represents one single food item in our list.
     * Think of it like a template that says "every food item should have a name and details"
     * 
     * It's like having a mold for making cookies - once we define the shape,
     * we can use it to make many identical cookie (list item) containers!
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val foodName: TextView = view.findViewById(R.id.tvFoodName)       // Shows "Apple - Red Delicious"
        val foodDetails: TextView = view.findViewById(R.id.tvFoodDetails) // Shows "80 cal per medium apple • 0.3g protein"
    }

    /**
     * 🏭 CREATE VIEW HOLDER - LIKE A FACTORY FOR LIST ITEMS
     * 
     * This method creates a new empty container for a list item.
     * Android calls this when it needs a new list item container.
     * 
     * Think of it like a factory assembly line:
     * 1. Take the XML layout template (item_food_search_result.xml)
     * 2. Inflate it into a real Android view
     * 3. Wrap it in a ViewHolder container
     * 4. Return the container ready to be filled with data
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_search_result, parent, false)
        return ViewHolder(view)
    }

    /**
     * 🎨 BIND VIEW HOLDER - FILL THE CONTAINER WITH ACTUAL DATA
     * 
     * This method takes an empty list item container and fills it with real food data.
     * Android calls this for each food item that needs to be displayed.
     * 
     * It's like taking an empty picture frame (ViewHolder) and putting a photo in it:
     * 1. Get the food item at the specified position
     * 2. Clean up the food name (remove "(saved)" tags)
     * 3. Show food name and brand if available
     * 4. Build nutrition details string
     * 5. Set up tap listener so user can select this food
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 🎯 Step 1: Get the food item we want to display at this position
        val foodItem = foodItems[position]
        
        // 🧹 Step 2: Clean up the food name (remove "(saved)" indicators for display)
        val cleanName = foodItem.name.replace(" (saved)", "")
        
        // 🏷️ Step 3: Create the main food name text, adding brand if available
        holder.foodName.text = if (foodItem.brand != null) {
            "$cleanName - ${foodItem.brand}"  // Example: "Apple - Red Delicious"
        } else {
            cleanName                         // Example: "Apple"
        }
        
        // 📊 Step 4: Build the nutrition details string
        val details = buildString {
            append("${foodItem.caloriesPerServing} cal")  // Always show calories
            foodItem.servingSize?.let { append(" per $it") }  // Add serving size if available
            foodItem.proteinPerServing?.let { 
                append(" • ${String.format("%.1f", it)}g protein")  // Add protein if available
            }
        }
        holder.foodDetails.text = details  // Example: "80 cal per medium apple • 0.3g protein"
        
        // 🖱️ Step 5: Make this list item clickable - when user taps it, select this food
        holder.itemView.setOnClickListener {
            onItemClick(foodItem)  // Tell the parent screen "user selected this food!"
        }
    }

    /**
     * 📏 GET ITEM COUNT - TELL RECYCLERVIEW HOW MANY ITEMS WE HAVE
     * 
     * Android needs to know how many list items to expect so it can:
     * - Scroll properly 
     * - Know when to stop creating list items
     * - Handle memory efficiently
     */
    override fun getItemCount() = foodItems.size

    /**
     * 🔄 UPDATE ITEMS - REFRESH THE LIST WITH NEW SEARCH RESULTS
     * 
     * When new food search results come in, we call this method to update the list.
     * This is like replacing all the photos in a photo album with new ones.
     * 
     * Steps:
     * 1. Replace the old food list with the new one
     * 2. Tell Android "hey, the data changed, please redraw the list"
     */
    fun updateItems(newItems: List<FoodItem>) {
        foodItems = newItems
        notifyDataSetChanged()  // This tells RecyclerView to refresh all visible items
    }
}