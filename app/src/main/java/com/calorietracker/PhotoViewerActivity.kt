package com.calorietracker

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhotoViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PHOTO_PATH = "extra_photo_path"
        const val EXTRA_PHOTO_DATE = "extra_photo_date"
        const val EXTRA_PHOTO_NOTES = "extra_photo_notes"
        const val EXTRA_PHOTO_WEIGHT = "extra_photo_weight"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_viewer)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val photoPath = intent.getStringExtra(EXTRA_PHOTO_PATH)
        val photoDate = intent.getStringExtra(EXTRA_PHOTO_DATE)
        val notes = intent.getStringExtra(EXTRA_PHOTO_NOTES)
        val weight = intent.getDoubleExtra(EXTRA_PHOTO_WEIGHT, -1.0)

        val imageView = findViewById<ImageView>(R.id.ivPhoto)
        if (!photoPath.isNullOrBlank()) {
            Glide.with(this)
                .load(File(photoPath))
                .into(imageView)
        }

        val tvDate = findViewById<TextView>(R.id.tvDate)
        if (!photoDate.isNullOrBlank()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(photoDate)
                val display = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                tvDate.text = date?.let { display.format(it) } ?: photoDate
            } catch (e: Exception) {
                tvDate.text = photoDate
            }
        }

        val tvWeight = findViewById<TextView>(R.id.tvWeight)
        if (weight > 0) {
            tvWeight.text = "Weight: ${"%.1f".format(weight)} lbs"
            tvWeight.visibility = View.VISIBLE
        } else {
            tvWeight.visibility = View.GONE
        }

        val tvNotes = findViewById<TextView>(R.id.tvNotes)
        if (!notes.isNullOrBlank()) {
            tvNotes.text = notes
            tvNotes.visibility = View.VISIBLE
        } else {
            tvNotes.visibility = View.GONE
        }
    }
}
