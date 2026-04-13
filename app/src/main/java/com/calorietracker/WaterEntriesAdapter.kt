package com.calorietracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.WaterIntakeEntry
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying water intake entries
 */
class WaterEntriesAdapter(
    private val onDeleteClick: (WaterIntakeEntry) -> Unit
) : RecyclerView.Adapter<WaterEntriesAdapter.WaterEntryViewHolder>() {

    private var waterEntries = listOf<WaterIntakeEntry>()

    fun updateEntries(entries: List<WaterIntakeEntry>) {
        waterEntries = entries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaterEntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_water_entry, parent, false)
        return WaterEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: WaterEntryViewHolder, position: Int) {
        holder.bind(waterEntries[position])
    }

    override fun getItemCount() = waterEntries.size

    inner class WaterEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvDrinkType: TextView = itemView.findViewById(R.id.tvDrinkType)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)

        fun bind(entry: WaterIntakeEntry) {
            val cups = entry.amount / 237.0
            val cupsText = if (cups == cups.toInt().toDouble()) {
                "${cups.toInt()}"
            } else {
                String.format("%.1f", cups)
            }
            tvAmount.text = "$cupsText cups"
            tvDrinkType.text = entry.drinkType.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
            
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvTime.text = timeFormat.format(Date(entry.timestamp))
            
            btnDelete.setOnClickListener {
                onDeleteClick(entry)
            }
        }
    }
}