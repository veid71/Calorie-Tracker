package com.calorietracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.database.FavoriteMeal
import com.calorietracker.repository.CalorieRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FavoritesActivity : AppCompatActivity() {

    private lateinit var repository: CalorieRepository
    private lateinit var adapter: FavoritesAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        tvEmpty = findViewById(R.id.tvEmpty)
        etSearch = findViewById(R.id.etSearch)

        adapter = FavoritesAdapter(
            onQuickAdd = { favorite -> quickAdd(favorite) },
            onLongClick = { favorite -> confirmRemove(favorite) }
        )

        val recycler = findViewById<RecyclerView>(R.id.recyclerFavorites)
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { loadFavorites(s?.toString()?.trim()) }
        })

        loadFavorites()
    }

    private fun loadFavorites(query: String? = null) {
        lifecycleScope.launch {
            val favorites = if (query.isNullOrBlank()) {
                repository.getTopFavorites(50)
            } else {
                repository.searchFavorites(query)
            }
            adapter.submitList(favorites)
            tvEmpty.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun quickAdd(favorite: FavoriteMeal) {
        lifecycleScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                repository.quickAddFavorite(favorite, today)
                Toast.makeText(
                    this@FavoritesActivity,
                    "${favorite.foodName} added (${favorite.calories} cal)",
                    Toast.LENGTH_SHORT
                ).show()
                loadFavorites(etSearch.text?.toString()?.trim())
            } catch (e: Exception) {
                Toast.makeText(this@FavoritesActivity, "Failed to add entry", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmRemove(favorite: FavoriteMeal) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Remove Favorite")
            .setMessage("Remove \"${favorite.foodName}\" from favorites?")
            .setPositiveButton("Remove") { _, _ ->
                lifecycleScope.launch {
                    repository.removeFromFavorites(favorite)
                    loadFavorites(etSearch.text?.toString()?.trim())
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

class FavoritesAdapter(
    private val onQuickAdd: (FavoriteMeal) -> Unit,
    private val onLongClick: (FavoriteMeal) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

    private var items = listOf<FavoriteMeal>()

    fun submitList(list: List<FavoriteMeal>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_meal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFoodName = itemView.findViewById<TextView>(R.id.tvFoodName)
        private val tvCalories = itemView.findViewById<TextView>(R.id.tvCalories)
        private val tvServing = itemView.findViewById<TextView>(R.id.tvServing)
        private val tvTimesUsed = itemView.findViewById<TextView>(R.id.tvTimesUsed)
        private val btnQuickAdd = itemView.findViewById<MaterialButton>(R.id.btnQuickAdd)

        fun bind(favorite: FavoriteMeal) {
            tvFoodName.text = favorite.foodName
            tvCalories.text = "${favorite.calories} cal"
            tvServing.text = favorite.servingSize?.let { "• $it" } ?: ""
            tvTimesUsed.text = "Used ${favorite.timesUsed}×"

            btnQuickAdd.setOnClickListener { onQuickAdd(favorite) }
            itemView.setOnLongClickListener { onLongClick(favorite); true }
        }
    }
}
