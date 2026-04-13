package com.calorietracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.database.MealPlan
import com.calorietracker.repository.CalorieRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddEditMealActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MEAL_ID = "extra_meal_id"
        const val EXTRA_PREFILL_DATE = "extra_prefill_date"
    }

    private lateinit var repository: CalorieRepository
    private var mealId: Long = -1
    private var existingMeal: MealPlan? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private lateinit var etMealName: TextInputEditText
    private lateinit var spinnerMealType: AutoCompleteTextView
    private lateinit var etDate: TextInputEditText
    private lateinit var etCalories: TextInputEditText
    private lateinit var etServings: TextInputEditText
    private lateinit var etNotes: TextInputEditText

    private val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_meal)

        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)
        mealId = intent.getLongExtra(EXTRA_MEAL_ID, -1)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = if (mealId > 0) "Edit Meal" else "Add Meal"
        toolbar.setNavigationOnClickListener { finish() }

        etMealName = findViewById(R.id.etMealName)
        spinnerMealType = findViewById(R.id.spinnerMealType)
        etDate = findViewById(R.id.etDate)
        etCalories = findViewById(R.id.etCalories)
        etServings = findViewById(R.id.etServings)
        etNotes = findViewById(R.id.etNotes)

        // Set up meal type dropdown
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mealTypes)
        spinnerMealType.setAdapter(adapter)
        spinnerMealType.setText("Breakfast", false)

        // Set default date
        val prefillDate = intent.getStringExtra(EXTRA_PREFILL_DATE) ?: dateFormat.format(Date())
        etDate.setText(prefillDate)

        // Date picker on tap
        etDate.setOnClickListener { showDatePicker() }

        if (mealId > 0) {
            loadExistingMeal()
        }

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { saveMeal() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        try {
            dateFormat.parse(etDate.text.toString())?.let { cal.time = it }
        } catch (e: Exception) { /* use today */ }

        DatePickerDialog(this, { _, year, month, day ->
            cal.set(year, month, day)
            etDate.setText(dateFormat.format(cal.time))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadExistingMeal() {
        lifecycleScope.launch {
            val meal = repository.getMealPlanById(mealId) ?: return@launch
            existingMeal = meal
            etMealName.setText(meal.mealName)
            spinnerMealType.setText(meal.mealType.replaceFirstChar { it.uppercase() }, false)
            etDate.setText(meal.date)
            etCalories.setText(meal.estimatedCalories.toString())
            etServings.setText(meal.servings.toString())
            etNotes.setText(meal.description ?: "")
        }
    }

    private fun saveMeal() {
        val name = etMealName.text?.toString()?.trim() ?: ""
        if (name.isBlank()) {
            etMealName.error = "Meal name is required"
            return
        }
        val mealType = spinnerMealType.text.toString().lowercase()
        val date = etDate.text?.toString() ?: dateFormat.format(Date())
        val calories = etCalories.text?.toString()?.toIntOrNull() ?: 0
        val servings = etServings.text?.toString()?.toIntOrNull() ?: 1
        val notes = etNotes.text?.toString()?.trim()?.ifBlank { null }

        val meal = (existingMeal ?: MealPlan(date = date, mealType = mealType, mealName = name)).copy(
            mealName = name,
            mealType = mealType,
            date = date,
            estimatedCalories = calories,
            servings = servings,
            description = notes,
            updatedAt = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            if (meal.id > 0) {
                repository.updateMealPlan(meal)
                Toast.makeText(this@AddEditMealActivity, "Meal updated", Toast.LENGTH_SHORT).show()
            } else {
                repository.addMealPlan(meal)
                Toast.makeText(this@AddEditMealActivity, "Meal added", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
