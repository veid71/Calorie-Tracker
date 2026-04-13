package com.calorietracker.network

import android.content.Context
import android.util.Log
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.database.USDAFoodItem
import com.calorietracker.utils.FileLogger
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Specialized downloader for USDA Food Database
 * 
 * Handles the complex process of downloading, extracting, and parsing
 * the USDA FoodData Central database (113,886+ food items).
 */
class USDADatabaseDownloader(private val context: Context) {
    
    private val database = CalorieDatabase.getDatabase(context)
    
    companion object {
        private const val TAG = "USDADownloader"
        private const val USDA_BASE_URL = "https://fdc.nal.usda.gov/fdc-datasets/"
        private const val USDA_ZIP_FILE = "FoodData_Central_sr_legacy_food_csv_2025-01-15.zip"
        private const val USDA_CSV_FILE = "sr_legacy_food.csv"
        
        // Progress reporting
        private const val DOWNLOAD_WEIGHT = 0.3f  // 30% of progress
        private const val EXTRACT_WEIGHT = 0.1f   // 10% of progress  
        private const val PARSE_WEIGHT = 0.6f     // 60% of progress
    }
    
    // Progress callback
    var onProgressUpdate: ((progress: Int, message: String) -> Unit)? = null
    
    /**
     * Download and process the complete USDA database
     */
    suspend fun downloadAndProcess(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting USDA database download and processing")
            
            // Step 1: Download ZIP file (30% of progress)
            updateProgress(5, "Downloading USDA database...")
            val zipData = downloadZipFile() ?: return@withContext Result.failure(
                Exception("Failed to download USDA ZIP file")
            )
            updateProgress(30, "Download complete, extracting...")
            
            // Step 2: Extract CSV from ZIP (10% of progress)  
            val csvData = extractCSVFromZip(zipData) ?: return@withContext Result.failure(
                Exception("Failed to extract CSV from ZIP file")
            )
            updateProgress(40, "Extraction complete, parsing data...")
            
            // Step 3: Parse CSV and insert into database (60% of progress)
            val itemCount = parseAndInsertCSVData(csvData)
            updateProgress(100, "Completed! Processed $itemCount food items")
            
            Log.d(TAG, "USDA database processing completed successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing USDA database", e)
            updateProgress(0, "Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Download the USDA ZIP file from remote server with enhanced error handling
     */
    private suspend fun downloadZipFile(): ByteArray? = withContext(Dispatchers.IO) {
        val result = NetworkErrorHandler.executeWithRetry(
            context = context,
            serviceName = "USDA-Download",
            operation = {
                val url = URL("$USDA_BASE_URL$USDA_ZIP_FILE")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 60000
                
                val contentLength = connection.contentLength
                val inputStream = connection.inputStream.buffered()
                val outputStream = ByteArrayOutputStream()
                
                val buffer = ByteArray(8192)
                var totalBytesRead = 0
                var bytesRead: Int
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    // Update download progress (0-30%)
                    if (contentLength > 0) {
                        val progressPercent = (totalBytesRead.toFloat() / contentLength * DOWNLOAD_WEIGHT * 100).toInt()
                        updateProgress(progressPercent, "Downloading... ${totalBytesRead / 1024 / 1024}MB")
                    }
                }
                
                inputStream.close()
                connection.disconnect()
                
                outputStream.toByteArray()
            },
            fallback = null, // No fallback for download - we need the actual data
            userFriendlyAction = "downloading USDA food database"
        )
        
        return@withContext when (result) {
            is NetworkErrorHandler.NetworkResult.Success -> result.data
            is NetworkErrorHandler.NetworkResult.Failed -> {
                Log.e(TAG, "Failed to download USDA ZIP file after retries", result.exception)
                null
            }
            is NetworkErrorHandler.NetworkResult.NoNetwork -> {
                Log.e(TAG, "No network available for USDA download")
                null
            }
            is NetworkErrorHandler.NetworkResult.CircuitBreakerOpen -> {
                Log.e(TAG, "USDA download service temporarily unavailable")
                null
            }
            else -> null
        }
    }
    
    /**
     * Extract CSV file from ZIP data
     */
    private suspend fun extractCSVFromZip(zipData: ByteArray): String? = withContext(Dispatchers.IO) {
        try {
            val zipInputStream = ZipInputStream(ByteArrayInputStream(zipData))
            
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(USDA_CSV_FILE)) {
                    // Found our target CSV file
                    val csvContent = zipInputStream.bufferedReader().readText()
                    zipInputStream.close()
                    return@withContext csvContent
                }
                entry = zipInputStream.nextEntry
            }
            
            zipInputStream.close()
            Log.e(TAG, "Could not find $USDA_CSV_FILE in ZIP archive")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting CSV from ZIP", e)
            null
        }
    }
    
    /**
     * Parse CSV data and insert into database
     */
    private suspend fun parseAndInsertCSVData(csvData: String): Int = withContext(Dispatchers.IO) {
        try {
            val lines = csvData.lines().drop(1) // Skip header row
            val totalLines = lines.size
            var processedItems = 0
            var validItems = 0
            
            val batchSize = 100
            val foodItems = mutableListOf<USDAFoodItem>()
            
            for (line in lines) {
                if (line.isBlank()) continue
                
                val foodItem = parseCSVLine(line)
                if (foodItem != null) {
                    foodItems.add(foodItem)
                    validItems++
                    
                    // Insert in batches for better performance
                    if (foodItems.size >= batchSize) {
                        database.usdaFoodItemDao().insertFoods(foodItems)
                        foodItems.clear()
                    }
                }
                
                processedItems++
                
                // Update progress (40% + 60% of parsing progress)
                val parseProgress = (processedItems.toFloat() / totalLines * PARSE_WEIGHT * 100).toInt()
                val totalProgress = 40 + parseProgress
                updateProgress(totalProgress, "Processed $validItems food items...")
            }
            
            // Insert remaining items
            if (foodItems.isNotEmpty()) {
                database.usdaFoodItemDao().insertFoods(foodItems)
            }
            
            Log.d(TAG, "Parsed $validItems valid food items from $processedItems total lines")
            validItems
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing CSV data", e)
            0
        }
    }
    
    /**
     * Parse a single CSV line into a USDAFoodItem
     */
    private fun parseCSVLine(line: String): USDAFoodItem? {
        try {
            // Split CSV line while handling quoted fields
            val fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                .map { it.trim().removeSurrounding("\"") }
            
            if (fields.size < 15) return null // Not enough fields
            
            val fdcId = fields[0].toLongOrNull() ?: return null
            val description = fields[2].takeIf { it.isNotBlank() } ?: return null
            
            // Parse nutritional data with safe conversion
            val protein = fields[3].toDoubleOrNull() ?: 0.0
            val fat = fields[4].toDoubleOrNull() ?: 0.0
            val carbohydrates = fields[5].toDoubleOrNull() ?: 0.0
            val calories = fields[6].toIntOrNull() ?: 0
            val fiber = fields[7].toDoubleOrNull() ?: 0.0
            val sugar = fields[8].toDoubleOrNull() ?: 0.0
            val sodium = fields[9].toDoubleOrNull() ?: 0.0
            val calcium = fields[10].toDoubleOrNull() ?: 0.0
            val iron = fields[11].toDoubleOrNull() ?: 0.0
            val vitaminC = fields[12].toDoubleOrNull() ?: 0.0
            // Additional nutrients available but not used in current schema
            // val vitaminA = fields[13].toDoubleOrNull() ?: 0.0
            // val potassium = fields[14].toDoubleOrNull() ?: 0.0
            
            return USDAFoodItem(
                fdcId = fdcId.toInt(),
                description = description,
                dataType = "CSV",
                protein = protein,
                fat = fat,
                carbohydrates = carbohydrates,
                calories = calories.toDouble(),
                fiber = fiber,
                sugar = sugar,
                sodium = sodium,
                calcium = calcium,
                iron = iron,
                vitaminC = vitaminC,
                servingSize = 100.0, // USDA data is per 100g
                servingSizeUnit = "g"
            )
            
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing CSV line: $line", e)
            return null
        }
    }
    
    /**
     * Update progress and notify listeners
     */
    private fun updateProgress(progress: Int, message: String) {
        onProgressUpdate?.invoke(progress, message)
        Log.d(TAG, "Progress: $progress% - $message")
    }
}