package com.calorietracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.database.ProgressPhoto
import com.calorietracker.repository.CalorieRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProgressPhotoActivity : AppCompatActivity() {

    private lateinit var repository: CalorieRepository
    private lateinit var adapter: ProgressPhotoAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var tvEmpty: TextView

    private var pendingPhotoFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) {
            pendingPhotoFile?.let { savePhoto(it) }
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress_photo)

        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        recycler = findViewById(R.id.recyclerPhotos)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = ProgressPhotoAdapter(
            onPhotoClick = { photo ->
                startActivity(Intent(this, PhotoViewerActivity::class.java).apply {
                    putExtra(PhotoViewerActivity.EXTRA_PHOTO_PATH, photo.photoPath)
                    putExtra(PhotoViewerActivity.EXTRA_PHOTO_DATE, photo.date)
                    putExtra(PhotoViewerActivity.EXTRA_PHOTO_NOTES, photo.notes)
                    photo.weight?.let { putExtra(PhotoViewerActivity.EXTRA_PHOTO_WEIGHT, it) }
                })
            },
            onPhotoLongClick = { photo ->
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Photo")
                    .setMessage("Delete this progress photo?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            repository.deleteProgressPhoto(photo)
                            loadPhotos()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        recycler.adapter = adapter
        recycler.layoutManager = GridLayoutManager(this, 2)

        findViewById<FloatingActionButton>(R.id.fabAddPhoto).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        loadPhotos()
    }

    private fun launchCamera() {
        val photoFile = createImageFile()
        pendingPhotoFile = photoFile
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
        cameraLauncher.launch(uri)
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: filesDir.also { it.mkdirs() }
        return File.createTempFile("PHOTO_${timestamp}_", ".jpg", storageDir)
    }

    private fun savePhoto(file: File) {
        val today = dateFormat.format(Date())
        val photo = ProgressPhoto(
            photoPath = file.absolutePath,
            thumbnailPath = null,
            date = today
        )
        lifecycleScope.launch {
            repository.addProgressPhoto(photo)
            loadPhotos()
            Toast.makeText(this@ProgressPhotoActivity, "Photo saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPhotos() {
        lifecycleScope.launch {
            val photos = repository.getProgressPhotosByType("progress")
            adapter.submitList(photos)
            tvEmpty.visibility = if (photos.isEmpty()) View.VISIBLE else View.GONE
            recycler.visibility = if (photos.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
