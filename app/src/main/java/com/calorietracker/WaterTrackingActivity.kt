package com.calorietracker

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.database.WaterIntakeEntry
import com.calorietracker.utils.ThemeManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for tracking daily water intake
 */
class WaterTrackingActivity : AppCompatActivity() {

    private lateinit var database: CalorieDatabase
    private lateinit var waterEntriesAdapter: WaterEntriesAdapter
    
    // UI elements
    private lateinit var tvCurrentDate: TextView
    private lateinit var tvWaterAmount: TextView
    private lateinit var tvWaterGoal: TextView
    private lateinit var progressWater: ProgressBar
    private lateinit var recyclerViewWaterEntries: RecyclerView
    private lateinit var tvNoWaterEntries: TextView
    private lateinit var waterChart: BarChart
    private lateinit var etCustomAmount: TextInputEditText
    
    private lateinit var btnAdd1Cup: MaterialButton
    private lateinit var btnAdd2Cups: MaterialButton
    private lateinit var btnAdd4Cups: MaterialButton
    private lateinit var btnAddCustom: MaterialButton
    private lateinit var btnBack: MaterialButton
    
    private val waterGoalCups = 10.5 // Daily water goal in cups
    private val waterGoalMl = (waterGoalCups * 237).toInt() // Convert to ml for database storage
    private val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_tracking)

        initDatabase()
        initViews()
        setupRecyclerView()
        setupClickListeners()
        observeWaterData()
        loadWeeklyChart()
        
        // Set current date
        val displayDate = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())
        tvCurrentDate.text = displayDate
    }

    private fun initDatabase() {
        database = CalorieDatabase.getDatabase(this)
    }

    private fun initViews() {
        tvCurrentDate = findViewById(R.id.tvCurrentDate)
        tvWaterAmount = findViewById(R.id.tvWaterAmount)
        tvWaterGoal = findViewById(R.id.tvWaterGoal)
        progressWater = findViewById(R.id.progressWater)
        recyclerViewWaterEntries = findViewById(R.id.recyclerViewWaterEntries)
        tvNoWaterEntries = findViewById(R.id.tvNoWaterEntries)
        waterChart = findViewById(R.id.waterChart)
        etCustomAmount = findViewById(R.id.etCustomAmount)
        
        btnAdd1Cup = findViewById(R.id.btnAdd250ml)
        btnAdd2Cups = findViewById(R.id.btnAdd500ml)
        btnAdd4Cups = findViewById(R.id.btnAdd1000ml)
        btnAddCustom = findViewById(R.id.btnAddCustom)
        btnBack = findViewById(R.id.btnBack)
        
        tvWaterGoal.text = "Goal: ${waterGoalCups} cups"
    }

    private fun setupRecyclerView() {
        waterEntriesAdapter = WaterEntriesAdapter { entry ->
            deleteWaterEntry(entry)
        }
        recyclerViewWaterEntries.apply {
            layoutManager = LinearLayoutManager(this@WaterTrackingActivity)
            adapter = waterEntriesAdapter
        }
    }

    private fun setupClickListeners() {
        btnAdd1Cup.setOnClickListener { addWaterFromCups(1.0) }
        btnAdd2Cups.setOnClickListener { addWaterFromCups(2.0) }
        btnAdd4Cups.setOnClickListener { addWaterFromCups(4.0) }
        
        btnAddCustom.setOnClickListener {
            val customAmountCups = etCustomAmount.text.toString().toDoubleOrNull()
            if (customAmountCups != null && customAmountCups > 0 && customAmountCups <= 20) {
                addWaterFromCups(customAmountCups)
                etCustomAmount.text?.clear()
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Please enter a valid amount (0.1-20 cups)", Snackbar.LENGTH_SHORT).show()
            }
        }
        
        btnBack.setOnClickListener { finish() }
    }

    private fun addWaterFromCups(cups: Double) {
        val amountMl = (cups * 237).toInt() // Convert cups to ml for database storage
        lifecycleScope.launch {
            val entry = WaterIntakeEntry(
                date = currentDate,
                amount = amountMl,
                drinkType = "water"
            )
            
            database.waterIntakeEntryDao().insertWaterEntry(entry)
            
            val cupsText = if (cups == cups.toInt().toDouble()) {
                "${cups.toInt()}"
            } else {
                String.format("%.1f", cups)
            }
            
            Snackbar.make(
                findViewById(android.R.id.content),
                "+$cupsText cups water logged!",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun addWater(amountMl: Int) {
        val cups = amountMl / 237.0
        addWaterFromCups(cups)
    }

    private fun deleteWaterEntry(entry: WaterIntakeEntry) {
        lifecycleScope.launch {
            database.waterIntakeEntryDao().deleteWaterEntry(entry)
            val cups = entry.amount / 237.0
            val cupsText = if (cups == cups.toInt().toDouble()) {
                "${cups.toInt()}"
            } else {
                String.format("%.1f", cups)
            }
            Snackbar.make(
                findViewById(android.R.id.content),
                "$cupsText cups water entry removed",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeWaterData() {
        lifecycleScope.launch {
            database.waterIntakeEntryDao().getWaterEntriesForDate(currentDate).collectLatest { entries ->
                updateWaterEntries(entries)
            }
        }
        
        lifecycleScope.launch {
            database.waterIntakeEntryDao().getTotalWaterForDateFlow(currentDate).collectLatest { total ->
                updateWaterProgress(total ?: 0)
            }
        }
    }

    private fun updateWaterEntries(entries: List<WaterIntakeEntry>) {
        if (entries.isEmpty()) {
            recyclerViewWaterEntries.visibility = View.GONE
            tvNoWaterEntries.visibility = View.VISIBLE
        } else {
            recyclerViewWaterEntries.visibility = View.VISIBLE
            tvNoWaterEntries.visibility = View.GONE
            waterEntriesAdapter.updateEntries(entries)
        }
    }

    private fun updateWaterProgress(totalMl: Int) {
        val totalCups = totalMl / 237.0
        val cupsText = String.format("%.1f", totalCups)
        tvWaterAmount.text = "$cupsText cups"
        
        val progressPercent = ((totalMl.toFloat() / waterGoalMl) * 100).toInt().coerceAtMost(100)
        progressWater.progress = progressPercent
        
        // Update text color based on progress
        val color = when {
            progressPercent >= 100 -> getColor(R.color.success_green)
            progressPercent >= 70 -> getColor(R.color.accent_blue)
            else -> getColor(R.color.text_primary)
        }
        tvWaterAmount.setTextColor(color)
    }

    private fun loadWeeklyChart() {
        lifecycleScope.launch {
            val calendar = Calendar.getInstance()
            val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            
            calendar.add(Calendar.DAY_OF_YEAR, -6)
            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            
            val waterTotals = database.waterIntakeEntryDao().getWaterTotalsInRange(startDate, endDate)
            
            // Create entries for the last 7 days
            val entries = mutableListOf<BarEntry>()
            val labels = mutableListOf<String>()
            
            for (i in 0..6) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                val dayLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
                
                val total = waterTotals.find { it.date == date }?.total ?: 0
                entries.add(BarEntry(i.toFloat(), total.toFloat()))
                labels.add(dayLabel)
            }
            
            setupBarChart(entries, labels)
        }
    }

    private fun setupBarChart(entries: List<BarEntry>, labels: List<String>) {
        val dataSet = BarDataSet(entries, "Daily Water Intake (ml)").apply {
            color = getColor(R.color.accent_blue)
            valueTextColor = getColor(R.color.text_primary)
            valueTextSize = 10f
        }
        
        val barData = BarData(dataSet)
        barData.barWidth = 0.8f
        
        waterChart.apply {
            data = barData
            description.isEnabled = false
            setFitBars(true)
            animateY(1000)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                textColor = getColor(R.color.text_secondary)
                gridColor = Color.TRANSPARENT
                axisLineColor = getColor(R.color.text_secondary)
            }
            
            axisLeft.apply {
                axisMinimum = 0f
                textColor = getColor(R.color.text_secondary)
                gridColor = getColor(R.color.light_gray)
                axisLineColor = getColor(R.color.text_secondary)
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = false
            
            invalidate()
        }
    }
}