package com.calorietracker

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.EditText
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.database.ShoppingListItem
import com.calorietracker.repository.CalorieRepository
import com.calorietracker.utils.ShoppingListGenerator
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class ShoppingListActivity : AppCompatActivity() {
    
    private lateinit var repository: CalorieRepository
    private lateinit var shoppingListGenerator: ShoppingListGenerator
    private lateinit var shoppingListAdapter: ShoppingListAdapter
    
    private lateinit var tvShoppingTrip: TextView
    private lateinit var tvItemCount: TextView
    private lateinit var tvEstimatedCost: TextView
    private lateinit var btnGenerateFromMeals: MaterialButton
    private lateinit var btnClearCompleted: MaterialButton
    private lateinit var recyclerShoppingList: RecyclerView
    private lateinit var tvNoItems: TextView
    private lateinit var fabAddItem: FloatingActionButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_list)
        
        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)
        shoppingListGenerator = ShoppingListGenerator(repository)
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        loadShoppingList()
    }
    
    private fun initViews() {
        tvShoppingTrip = findViewById(R.id.tvShoppingTrip)
        tvItemCount = findViewById(R.id.tvItemCount)
        tvEstimatedCost = findViewById(R.id.tvEstimatedCost)
        btnGenerateFromMeals = findViewById(R.id.btnGenerateFromMeals)
        btnClearCompleted = findViewById(R.id.btnClearCompleted)
        recyclerShoppingList = findViewById(R.id.recyclerShoppingList)
        tvNoItems = findViewById(R.id.tvNoItems)
        fabAddItem = findViewById(R.id.fabAddItem)
        ViewCompat.setOnApplyWindowInsetsListener(fabAddItem) { v, windowInsets ->
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val mlp = v.layoutParams as android.view.ViewGroup.MarginLayoutParams
            mlp.bottomMargin = navBars.bottom + (16 * resources.displayMetrics.density).toInt()
            v.layoutParams = mlp
            windowInsets
        }
    }

    private fun setupRecyclerView() {
        shoppingListAdapter = ShoppingListAdapter(
            onItemClick = { item ->
                // Toggle checked status
                lifecycleScope.launch {
                    try {
                        val updatedItem = item.copy(
                            isChecked = !item.isChecked,
                            checkedAt = if (!item.isChecked) System.currentTimeMillis() else null,
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.updateShoppingItem(updatedItem)
                        loadShoppingList()
                    } catch (e: Exception) {
                        Toast.makeText(this@ShoppingListActivity, "Failed to update item", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onItemLongClick = { item ->
                Toast.makeText(this, "Edit ${item.itemName}", Toast.LENGTH_SHORT).show()
            },
            onItemEdit = { item ->
                Toast.makeText(this, "Edit ${item.itemName}", Toast.LENGTH_SHORT).show()
            },
            onItemDelete = { item ->
                lifecycleScope.launch {
                    try {
                        repository.deleteShoppingItem(item)
                        Toast.makeText(this@ShoppingListActivity, "Item deleted", Toast.LENGTH_SHORT).show()
                        loadShoppingList()
                    } catch (e: Exception) {
                        Toast.makeText(this@ShoppingListActivity, "Failed to delete item", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        
        recyclerShoppingList.apply {
            adapter = shoppingListAdapter
            layoutManager = LinearLayoutManager(this@ShoppingListActivity)
        }
    }
    
    private fun setupClickListeners() {
        btnGenerateFromMeals.setOnClickListener {
            generateShoppingListFromMeals()
        }
        
        btnClearCompleted.setOnClickListener {
            clearCompletedItems()
        }
        
        fabAddItem.setOnClickListener {
            showAddItemDialog()
        }
    }
    
    private fun loadShoppingList() {
        lifecycleScope.launch {
            try {
                val activeItems = repository.getActiveShoppingListSync()
                shoppingListAdapter.submitList(activeItems)
                
                // Update header info
                val itemCount = activeItems.size
                val estimatedCost = activeItems.sumOf { it.estimatedCost ?: 0.0 }
                
                tvItemCount.text = "$itemCount items"
                val formatter = NumberFormat.getCurrencyInstance(Locale.US)
                tvEstimatedCost.text = "Est. ${formatter.format(estimatedCost)}"
                
                // Show/hide empty state
                if (activeItems.isEmpty()) {
                    tvNoItems.visibility = android.view.View.VISIBLE
                    recyclerShoppingList.visibility = android.view.View.GONE
                } else {
                    tvNoItems.visibility = android.view.View.GONE
                    recyclerShoppingList.visibility = android.view.View.VISIBLE
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@ShoppingListActivity, "Failed to load shopping list", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun generateShoppingListFromMeals() {
        lifecycleScope.launch {
            try {
                val generatedItems = shoppingListGenerator.generateFromCurrentWeek()
                if (generatedItems.isNotEmpty()) {
                    repository.insertShoppingItems(generatedItems)
                    Toast.makeText(this@ShoppingListActivity, "Generated ${generatedItems.size} items from meal plans", Toast.LENGTH_SHORT).show()
                    loadShoppingList()
                } else {
                    Toast.makeText(this@ShoppingListActivity, "No meal plans found for this week", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ShoppingListActivity, "Failed to generate shopping list", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showAddItemDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
        }
        val etName = EditText(this).apply {
            hint = "Item name (e.g. Chicken breast)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        val etQty = EditText(this).apply {
            hint = "Quantity (e.g. 2 lbs, optional)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        layout.addView(etName)
        layout.addView(etQty)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Item")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotBlank()) {
                    val qty = etQty.text.toString().trim().ifBlank { null }
                    val item = ShoppingListItem(itemName = name, quantity = qty)
                    lifecycleScope.launch {
                        repository.insertShoppingItem(item)
                        loadShoppingList()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearCompletedItems() {
        lifecycleScope.launch {
            try {
                val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // 24 hours ago
                repository.cleanupOldCheckedItems(cutoffTime)
                Toast.makeText(this@ShoppingListActivity, "Cleared completed items", Toast.LENGTH_SHORT).show()
                loadShoppingList()
            } catch (e: Exception) {
                Toast.makeText(this@ShoppingListActivity, "Failed to clear completed items", Toast.LENGTH_SHORT).show()
            }
        }
    }
}