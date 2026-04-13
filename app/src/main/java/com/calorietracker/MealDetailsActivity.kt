package com.calorietracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.database.MealPlan
import com.calorietracker.repository.CalorieRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MealDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MEAL_ID = "extra_meal_id"
    }

    private lateinit var repository: CalorieRepository
    private var mealId: Long = -1
    private var currentMeal: MealPlan? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_details)

        mealId = intent.getLongExtra(EXTRA_MEAL_ID, -1)
        if (mealId < 0) { finish(); return }

        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        loadMeal()
    }

    private fun loadMeal() {
        lifecycleScope.launch {
            val meal = repository.getMealPlanById(mealId) ?: run { finish(); return@launch }
            currentMeal = meal
            bindMeal(meal)
        }
    }

    private fun bindMeal(meal: MealPlan) {
        findViewById<Chip>(R.id.chipMealType).text = meal.mealType.replaceFirstChar { it.uppercase() }
        findViewById<TextView>(R.id.tvMealName).text = meal.mealName
        findViewById<TextView>(R.id.tvCalories).text = "${meal.estimatedCalories} cal"
        findViewById<TextView>(R.id.tvServings).text = meal.servings.toString()

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(meal.date)
            val displaySdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            findViewById<TextView>(R.id.tvDate).text = date?.let { displaySdf.format(it) } ?: meal.date
        } catch (e: Exception) {
            findViewById<TextView>(R.id.tvDate).text = meal.date
        }

        val cardDetails = findViewById<View>(R.id.cardDetails)
        if (!meal.description.isNullOrBlank()) {
            cardDetails.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvDescription).text = meal.description
        } else {
            cardDetails.visibility = View.GONE
        }

        findViewById<TextView>(R.id.tvStatus).text = if (meal.isCompleted) "Eaten" else "Planned"

        val btnMarkComplete = findViewById<MaterialButton>(R.id.btnMarkComplete)
        btnMarkComplete.text = if (meal.isCompleted) "Mark as Not Eaten" else "Mark as Eaten"
        btnMarkComplete.setOnClickListener {
            lifecycleScope.launch {
                repository.markMealAsCompleted(meal.id, !meal.isCompleted, System.currentTimeMillis())
                loadMeal()
            }
        }

        findViewById<MaterialButton>(R.id.btnEditMeal).setOnClickListener {
            startActivity(Intent(this, AddEditMealActivity::class.java).apply {
                putExtra(AddEditMealActivity.EXTRA_MEAL_ID, meal.id)
            })
        }

        findViewById<MaterialButton>(R.id.btnDeleteMeal).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete Meal")
                .setMessage("Remove \"${meal.mealName}\" from your meal plan?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        repository.deleteMealPlan(meal)
                        finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
