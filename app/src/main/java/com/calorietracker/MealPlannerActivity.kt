package com.calorietracker

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.card.MaterialCardView
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.repository.CalorieRepository
import com.calorietracker.utils.WeekUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MealPlannerActivity : AppCompatActivity() {
    
    private lateinit var repository: CalorieRepository
    private lateinit var weekUtils: WeekUtils
    private lateinit var mealPlanAdapter: MealPlanAdapter
    private lateinit var tvWeekTitle: TextView
    private lateinit var btnPreviousWeek: MaterialButton
    private lateinit var btnNextWeek: MaterialButton
    private lateinit var fabAddMeal: FloatingActionButton
    
    // Day cards
    private lateinit var cardMonday: MaterialCardView
    private lateinit var cardTuesday: MaterialCardView
    private lateinit var cardWednesday: MaterialCardView
    private lateinit var cardThursday: MaterialCardView
    private lateinit var cardFriday: MaterialCardView
    private lateinit var cardSaturday: MaterialCardView
    private lateinit var cardSunday: MaterialCardView
    
    private var currentWeekStart: Calendar = Calendar.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_planner)
        
        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)
        weekUtils = WeekUtils()
        
        initViews()
        setupRecyclerViews()
        setupClickListeners()
        
        // Initialize with current week
        updateWeekDisplay()
        loadMealPlansForCurrentWeek()
    }
    
    private fun initViews() {
        tvWeekTitle = findViewById(R.id.tvWeekTitle)
        btnPreviousWeek = findViewById(R.id.btnPreviousWeek)
        btnNextWeek = findViewById(R.id.btnNextWeek)
        fabAddMeal = findViewById(R.id.fabAddMeal)
        ViewCompat.setOnApplyWindowInsetsListener(fabAddMeal) { v, windowInsets ->
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val mlp = v.layoutParams as android.view.ViewGroup.MarginLayoutParams
            mlp.bottomMargin = navBars.bottom + (16 * resources.displayMetrics.density).toInt()
            v.layoutParams = mlp
            windowInsets
        }

        cardMonday = findViewById(R.id.cardMonday)
        cardTuesday = findViewById(R.id.cardTuesday)
        cardWednesday = findViewById(R.id.cardWednesday)
        cardThursday = findViewById(R.id.cardThursday)
        cardFriday = findViewById(R.id.cardFriday)
        cardSaturday = findViewById(R.id.cardSaturday)
        cardSunday = findViewById(R.id.cardSunday)
    }
    
    private fun setupRecyclerViews() {
        // Find recycler view in Monday card for now (simplified)
        val recyclerMeals = findViewById<RecyclerView>(R.id.recyclerMeals)
        recyclerMeals?.let { recycler ->
            mealPlanAdapter = MealPlanAdapter(
                onMealClick = { meal ->
                    startActivity(Intent(this, MealDetailsActivity::class.java).apply {
                        putExtra(MealDetailsActivity.EXTRA_MEAL_ID, meal.id)
                    })
                },
                onMealLongClick = { meal ->
                    startActivity(Intent(this, AddEditMealActivity::class.java).apply {
                        putExtra(AddEditMealActivity.EXTRA_MEAL_ID, meal.id)
                    })
                },
                onMealEdit = { meal ->
                    startActivity(Intent(this, AddEditMealActivity::class.java).apply {
                        putExtra(AddEditMealActivity.EXTRA_MEAL_ID, meal.id)
                    })
                },
                onMealDelete = { meal ->
                    // Delete meal
                    lifecycleScope.launch {
                        try {
                            repository.deleteMealPlan(meal)
                            Toast.makeText(this@MealPlannerActivity, "Meal deleted", Toast.LENGTH_SHORT).show()
                            loadMealPlansForCurrentWeek()
                        } catch (e: Exception) {
                            Toast.makeText(this@MealPlannerActivity, "Failed to delete meal", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onMealComplete = { meal ->
                    // Mark meal as completed
                    lifecycleScope.launch {
                        try {
                            repository.markMealAsCompleted(meal.id, !meal.isCompleted, System.currentTimeMillis())
                            Toast.makeText(this@MealPlannerActivity, "Meal updated", Toast.LENGTH_SHORT).show()
                            loadMealPlansForCurrentWeek()
                        } catch (e: Exception) {
                            Toast.makeText(this@MealPlannerActivity, "Failed to update meal", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
            
            recycler.apply {
                adapter = mealPlanAdapter
                layoutManager = LinearLayoutManager(this@MealPlannerActivity)
            }
        }
    }
    
    private fun setupClickListeners() {
        btnPreviousWeek.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
            updateWeekDisplay()
            loadMealPlansForCurrentWeek()
        }
        
        btnNextWeek.setOnClickListener {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1)
            updateWeekDisplay()
            loadMealPlansForCurrentWeek()
        }
        
        fabAddMeal.setOnClickListener {
            startActivity(Intent(this, AddEditMealActivity::class.java))
        }
        
        // Setup day card click listeners for simplified implementation
        val dayCards = listOf(cardMonday, cardTuesday, cardWednesday, cardThursday, cardFriday, cardSaturday, cardSunday)
        dayCards.forEachIndexed { index, card ->
            card.setOnClickListener {
                val calendar = Calendar.getInstance()
                calendar.time = currentWeekStart.time
                calendar.add(Calendar.DAY_OF_YEAR, index)
                
                val dayName = when (index) {
                    0 -> "Monday"
                    1 -> "Tuesday"
                    2 -> "Wednesday"
                    3 -> "Thursday"
                    4 -> "Friday"
                    5 -> "Saturday"
                    6 -> "Sunday"
                    else -> "Day"
                }
                
                Toast.makeText(this, "$dayName meal planning", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Update day date labels
        updateDayLabels()
    }
    
    private fun updateWeekDisplay() {
        val weekDates = weekUtils.getWeekDates(currentWeekStart)
        val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
        val startDate = formatter.format(weekDates.first())
        val endDate = formatter.format(weekDates.last())
        
        tvWeekTitle.text = "Week of $startDate - $endDate"
        updateDayLabels()
    }
    
    private fun updateDayLabels() {
        val weekDates = weekUtils.getWeekDates(currentWeekStart)
        val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
        
        val dayCards = listOf(cardMonday, cardTuesday, cardWednesday, cardThursday, cardFriday, cardSaturday, cardSunday)
        val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        
        dayCards.forEachIndexed { index, card ->
            val dateLabel = card.findViewById<TextView>(R.id.tvDayDate)
            dateLabel?.text = "${dayNames[index]}, ${formatter.format(weekDates[index])}"
        }
    }
    
    private fun loadMealPlansForCurrentWeek() {
        lifecycleScope.launch {
            try {
                val weekDates = weekUtils.getWeekDates(currentWeekStart)
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startDate = formatter.format(weekDates.first())
                val endDate = formatter.format(weekDates.last())
                
                val mealPlans = repository.getMealPlansForDateRange(startDate, endDate)
                mealPlanAdapter.submitList(mealPlans)
                
            } catch (e: Exception) {
                Toast.makeText(this@MealPlannerActivity, "Failed to load meal plans", Toast.LENGTH_SHORT).show()
            }
        }
    }
}