package com.calorietracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.calorietracker.database.ProgressPhoto
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying progress photos in a grid layout
 * Shows photo thumbnails with date and optional weight information
 */
class ProgressPhotoAdapter(
    private val onPhotoClick: (ProgressPhoto) -> Unit,
    private val onPhotoLongClick: (ProgressPhoto) -> Unit
) : ListAdapter<ProgressPhoto, ProgressPhotoAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_progress_photo, parent, false)
        return PhotoViewHolder(view, onPhotoClick, onPhotoLongClick)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PhotoViewHolder(
        itemView: View,
        private val onPhotoClick: (ProgressPhoto) -> Unit,
        private val onPhotoLongClick: (ProgressPhoto) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvWeight: TextView = itemView.findViewById(R.id.tvWeight)
        private val tvPhotoType: TextView = itemView.findViewById(R.id.tvPhotoType)
        private val tvNotes: TextView = itemView.findViewById(R.id.tvNotes)

        fun bind(photo: ProgressPhoto) {
            // Load photo using Glide (handles thumbnails and caching)
            val imagePath = photo.thumbnailPath ?: photo.photoPath
            Glide.with(itemView.context)
                .load(imagePath)
                .apply(RequestOptions()
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error))
                .into(ivPhoto)
            
            // Format and show date
            val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(photo.date)
            tvDate.text = date?.let { dateFormatter.format(it) } ?: photo.date
            
            // Show weight if available
            if (photo.weight != null) {
                tvWeight.text = "${photo.weight} lbs"
                tvWeight.visibility = View.VISIBLE
            } else {
                tvWeight.visibility = View.GONE
            }
            
            // Show photo type badge
            if (photo.photoType != "progress") {
                tvPhotoType.text = photo.photoType.replaceFirstChar { it.titlecaseChar() }
                tvPhotoType.visibility = View.VISIBLE
            } else {
                tvPhotoType.visibility = View.GONE
            }
            
            // Show brief notes if available
            if (!photo.notes.isNullOrBlank() && photo.notes.length > 3) {
                tvNotes.text = if (photo.notes.length > 30) {
                    "${photo.notes.take(27)}..."
                } else {
                    photo.notes
                }
                tvNotes.visibility = View.VISIBLE
            } else {
                tvNotes.visibility = View.GONE
            }
            
            // Set click listeners
            itemView.setOnClickListener { onPhotoClick(photo) }
            itemView.setOnLongClickListener { 
                onPhotoLongClick(photo)
                true 
            }
            
            // Visual styling based on photo type
            val backgroundResource = when (photo.photoType) {
                "before" -> R.color.accent_blue
                "after" -> R.color.success_green
                "milestone" -> R.color.accent_orange
                else -> R.color.surface_elevated
            }
            
            // Apply subtle tint for photo type differentiation
            if (photo.photoType != "progress") {
                itemView.background = itemView.context.getDrawable(backgroundResource)
                itemView.alpha = 0.9f
            } else {
                itemView.alpha = 1.0f
            }
        }
    }

    class PhotoDiffCallback : DiffUtil.ItemCallback<ProgressPhoto>() {
        override fun areItemsTheSame(oldItem: ProgressPhoto, newItem: ProgressPhoto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProgressPhoto, newItem: ProgressPhoto): Boolean {
            return oldItem == newItem
        }
    }
}