package com.calorietracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.database.CalorieEntry
import com.calorietracker.repository.CalorieRepository
import com.calorietracker.utils.ThemeManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * This activity shows the user's food entry history
 * Users can see all their past food entries organized by date
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var repository: CalorieRepository
    private lateinit var historyAdapter: HistoryEntryAdapter
    private lateinit var recyclerViewHistory: RecyclerView
    private lateinit var tvPeriodTitle: android.widget.TextView
    private lateinit var tvTotalCalories: android.widget.TextView
    private lateinit var btnToday: MaterialButton
    private lateinit var btnThisWeek: MaterialButton

    // Keep a reference to the current observer so we can remove it before reattaching
    private var currentLiveData: LiveData<List<CalorieEntry>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)

        initViews()
        setupRecyclerView()
        setupClickListeners()
        // Default view: This Week
        loadWeekData()
    }

    private fun initViews() {
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory)
        tvPeriodTitle     = findViewById(R.id.tvPeriodTitle)
        tvTotalCalories   = findViewById(R.id.tvTotalCalories)
        btnToday          = findViewById(R.id.btnToday)
        btnThisWeek       = findViewById(R.id.btnThisWeek)
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryEntryAdapter()
        recyclerViewHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(this@HistoryActivity)
        }
    }

    private fun setupClickListeners() {
        btnToday.setOnClickListener {
            selectTab(today = true)
            loadTodayData()
        }

        btnThisWeek.setOnClickListener {
            selectTab(today = false)
            loadWeekData()
        }

        findViewById<MaterialButton>(R.id.btnBarcodeHistory)
            ?.setOnClickListener {
                startActivity(Intent(this, BarcodeHistoryActivity::class.java))
            }
    }

    /** Toggle the filled/outlined styles on the two tab buttons. */
    private fun selectTab(today: Boolean) {
        if (today) {
            btnToday.setBackgroundColor(getColor(R.color.primary_green))
            btnToday.setTextColor(getColor(android.R.color.white))
            btnThisWeek.backgroundTintList = null
            btnThisWeek.setTextColor(getColor(R.color.primary_green))
        } else {
            btnThisWeek.setBackgroundColor(getColor(R.color.primary_green))
            btnThisWeek.setTextColor(getColor(android.R.color.white))
            btnToday.backgroundTintList = null
            btnToday.setTextColor(getColor(R.color.primary_green))
        }
    }

    private fun loadTodayData() {
        tvPeriodTitle.text = "Today's Entries"
        val today = getCurrentDateString()
        observeEntries(repository.getEntriesForDateRange(today, today))
    }

    private fun loadWeekData() {
        tvPeriodTitle.text = "This Week's Entries"
        val today = getCurrentDateString()
        val weekAgo = getDateDaysAgo(7)
        observeEntries(repository.getEntriesForDateRange(weekAgo, today))
    }

    private fun observeEntries(liveData: LiveData<List<CalorieEntry>>) {
        // Remove the previous observer before attaching a new one to avoid duplicate updates
        currentLiveData?.removeObservers(this)
        currentLiveData = liveData
        liveData.observe(this) { entries ->
            historyAdapter.submitList(entries)
            val total = entries.sumOf { it.calories }
            tvTotalCalories.text = "$total cal"
        }
    }

    private fun getCurrentDateString(): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return formatter.format(java.util.Date())
    }

    private fun getDateDaysAgo(days: Int): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return formatter.format(calendar.time)
    }
}
