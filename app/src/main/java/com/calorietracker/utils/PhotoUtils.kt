package com.calorietracker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for photo handling operations
 * Manages photo storage, compression, and thumbnail generation
 */
object PhotoUtils {
    
    private const val PHOTO_QUALITY = 80 // JPEG compression quality
    private const val THUMBNAIL_SIZE = 150 // Thumbnail dimensions in pixels
    
    /**
     * Create a unique image file for storing progress photos
     */
    @Throws(IOException::class)
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "PROGRESS_${timeStamp}_"
        val storageDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "progress_photos")
        
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        return File.createTempFile(fileName, ".jpg", storageDir)
    }
    
    /**
     * Copy an image from URI (like gallery selection) to app directory
     */
    suspend fun copyImageToAppDirectory(context: Context, imageUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val targetFile = createImageFile(context)
            
            inputStream?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Compress the copied image
            compressImage(targetFile.absolutePath)
            targetFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Compress an image file to reduce storage space.
     * Uses inSampleSize to avoid loading the full bitmap into memory.
     */
    fun compressImage(imagePath: String): Boolean {
        return try {
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            BitmapFactory.decodeFile(imagePath, bounds)
            val opts = BitmapFactory.Options()
            opts.inSampleSize = calculateInSampleSize(bounds, 1080, 1920)
            val bitmap: Bitmap = BitmapFactory.decodeFile(imagePath, opts) ?: return false
            FileOutputStream(imagePath).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, out)
            }
            bitmap.recycle()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Create a thumbnail version of an image.
     * Uses inSampleSize so only a fraction of the image is decoded into memory.
     */
    fun createThumbnail(originalPath: String): String? {
        return try {
            val thumbnailPath = originalPath.replace(".jpg", "_thumb.jpg")
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            BitmapFactory.decodeFile(originalPath, bounds)
            val opts = BitmapFactory.Options()
            opts.inSampleSize = calculateInSampleSize(bounds, THUMBNAIL_SIZE, THUMBNAIL_SIZE)
            val sampled: Bitmap = BitmapFactory.decodeFile(originalPath, opts) ?: return null
            val thumbnail = Bitmap.createScaledBitmap(sampled, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true)
            FileOutputStream(thumbnailPath).use { out ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, out)
            }
            if (sampled !== thumbnail) sampled.recycle()
            thumbnail.recycle()
            thumbnailPath
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(opts: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = opts.outHeight
        val width = opts.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    
    /**
     * Get the file size of an image in MB
     */
    fun getImageSizeMB(imagePath: String): Double {
        val file = File(imagePath)
        return file.length() / (1024.0 * 1024.0)
    }
    
    /**
     * Delete photo files (original and thumbnail)
     */
    fun deletePhotoFiles(photoPath: String, thumbnailPath: String?) {
        try {
            File(photoPath).delete()
            thumbnailPath?.let { File(it).delete() }
        } catch (e: Exception) {
            // Handle deletion errors silently
        }
    }
    
    /**
     * Check if photo file exists and is readable
     */
    fun isPhotoValid(photoPath: String): Boolean {
        val file = File(photoPath)
        return file.exists() && file.canRead() && file.length() > 0
    }
    
    /**
     * Clean up old photo files that no longer have database records
     */
    fun cleanupOrphanedPhotos(context: Context, validPhotoPaths: List<String>) {
        try {
            val photosDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "progress_photos")
            if (!photosDir.exists()) return
            
            photosDir.listFiles()?.forEach { file ->
                if (file.isFile && !validPhotoPaths.contains(file.absolutePath)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Handle cleanup errors silently
        }
    }
    
    /**
     * Get total storage used by progress photos in MB
     */
    fun getTotalPhotoStorageUsed(context: Context): Double {
        return try {
            val photosDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "progress_photos")
            if (!photosDir.exists()) return 0.0
            
            var totalSize = 0L
            photosDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    totalSize += file.length()
                }
            }
            
            totalSize / (1024.0 * 1024.0)
        } catch (e: Exception) {
            0.0
        }
    }
}