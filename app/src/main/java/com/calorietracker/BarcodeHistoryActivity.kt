package com.calorietracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.repository.CalorieRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class BarcodeHistoryActivity : AppCompatActivity() {

    private lateinit var repository: CalorieRepository
    private lateinit var adapter: BarcodeHistoryAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvCount: TextView
    private lateinit var etSearch: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_history)

        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        recycler = findViewById(R.id.recyclerHistory)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvCount = findViewById(R.id.tvCount)
        etSearch = findViewById(R.id.etSearch)

        adapter = BarcodeHistoryAdapter { history ->
            // Open CalorieEntryActivity pre-filled with this product's data
            val intent = Intent(this, CalorieEntryActivity::class.java).apply {
                putExtra("food_name", history.foodName)
                putExtra("calories", history.calories)
                putExtra("barcode", history.barcode)
                history.protein?.let { putExtra("protein", it) }
                history.carbs?.let { putExtra("carbs", it) }
                history.fat?.let { putExtra("fat", it) }
            }
            startActivity(intent)
        }

        recycler.adapter = adapter
        recycler.layoutManager = GridLayoutManager(this, 2)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loadHistory(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadHistory("")
    }

    private fun loadHistory(query: String) {
        lifecycleScope.launch {
            val items = if (query.isBlank()) {
                repository.getRecentBarcodeHistory(100)
            } else {
                repository.searchBarcodeHistory(query)
            }
            adapter.submitList(items)
            val label = if (query.isBlank()) "${items.size} items scanned" else "${items.size} results"
            tvCount.text = label
            tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            recycler.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
