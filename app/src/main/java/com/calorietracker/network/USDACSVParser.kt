package com.calorietracker.network

import android.util.Log
import com.calorietracker.database.USDAFoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

/**
 * Comprehensive CSV parser for USDA FoodData Central bulk datasets
 * 
 * **FILE PROCESSING CONCEPTS FOR BEGINNERS:**
 * 
 * **What is CSV (Comma-Separated Values)?**
 * CSV is like a spreadsheet saved as plain text:
 * ```
 * fdc_id,description,calories,protein
 * 123456,"Chicken, cooked",165,31.0
 * 789012,"Rice, white",130,2.7
 * ```
 * - First row contains column headers
 * - Each subsequent row is one record
 * - Commas separate different fields/columns
 * - Quotes protect text that contains commas
 * 
 * **Why Parse CSV Instead of Using APIs?**
 * Government datasets are HUGE (400MB+):
 * - API approach: 100,000+ individual requests (very slow)
 * - CSV approach: 1 download + local processing (much faster)
 * - Offline capability: Works without internet after download
 * - Cost effective: No API rate limits or fees
 * 
 * **ZIP File Processing:**
 * Large datasets come compressed to save bandwidth:
 * - Original: 400MB of CSV files
 * - Compressed: 100MB ZIP file (4x smaller download)
 * - ZipInputStream: Reads compressed data directly without extracting to disk
 * - Memory efficient: Process files as we read them
 * 
 * **Stream Processing vs Loading Everything:**
 * - Loading all 400MB into memory would crash most phones
 * - Stream processing: Read file line by line as needed
 * - BufferedReader: Efficiently reads text files line by line
 * - Batch database inserts: Group records for better performance
 * 
 * **CSV Parsing Challenges:**
 * 1. **Quoted Fields:** "Chicken, grilled" - comma inside quotes
 * 2. **Escaped Quotes:** "Product called ""Super"" Food" - quotes inside quotes
 * 3. **Different Line Endings:** Windows (\r\n) vs Unix (\n)
 * 4. **Encoding:** UTF-8 vs ASCII - international characters
 * 5. **Large Files:** Memory management for 400MB+ files
 * 
 * Handles Foundation Foods, SR Legacy, and Branded Foods datasets
 */
class USDACSVParser {
    
    companion object {
        private const val TAG = "USDACSVParser"
        
        // CSV file names in USDA bulk downloads
        private const val FOOD_CSV = "food.csv"
        private const val NUTRIENT_CSV = "nutrient.csv"
        private const val FOOD_NUTRIENT_CSV = "food_nutrient.csv"
        private const val BRANDED_FOOD_CSV = "branded_food.csv"
        private const val FOUNDATION_FOOD_CSV = "foundation_food.csv"
        
        // Key nutrient IDs from USDA database
        private const val ENERGY_KCAL_ID = 1008
        private const val PROTEIN_ID = 1003
        private const val TOTAL_FAT_ID = 1004
        private const val CARBS_ID = 1005
        private const val FIBER_ID = 1079
        private const val SUGAR_ID = 2000
        private const val SODIUM_ID = 1093
    }
    
    /**
     * Parse USDA bulk ZIP file and extract food items with nutrition data
     * 
     * **STREAM PROCESSING ARCHITECTURE:**
     * 
     * **Input/Output Streams:**
     * Think of streams like water pipes for data:
     * - InputStream: Data flowing into your app (like a faucet)
     * - OutputStream: Data flowing out of your app (like a drain)
     * - We read data piece by piece, not all at once
     * 
     * **ZIP File Processing:**
     * ZIP files are like Russian nesting dolls:
     * - Outer ZIP file contains multiple CSV files
     * - ZipInputStream reads the outer container
     * - For each "entry" (CSV file inside), we get another stream
     * - We process each CSV without extracting to disk
     * 
     * **Memory Management Strategy:**
     * Large files (400MB) require careful memory management:
     * - Don't load entire file into memory at once
     * - Read line by line using BufferedReader
     * - Process data immediately and discard
     * - Use mutable collections that grow as needed
     * 
     * **Threading with Dispatchers.IO:**
     * File operations are slow and can block the UI:
     * - withContext(Dispatchers.IO): Switch to background thread
     * - Prevents "Application Not Responding" (ANR) errors
     * - UI stays responsive while processing large files
     * 
     * **Data Structure Strategy:**
     * We use Maps to efficiently combine related data:
     * - foods: Map<FdcId, FoodItem> - Basic food information
     * - nutrients: Map<NutrientId, Name> - What each nutrient ID means
     * - foodNutrients: Map<FdcId, Map<NutrientId, Amount>> - Nutrition values
     * - Maps allow O(1) lookup instead of O(n) searching
     */
    suspend fun parseBulkZipFile(zipInputStream: InputStream): List<USDAFoodItem> {
        return withContext(Dispatchers.IO) {
            val foods = mutableMapOf<Int, USDAFoodItem>()
            val nutrients = mutableMapOf<Int, String>()
            val foodNutrients = mutableMapOf<Int, MutableMap<Int, Double>>()
            
            try {
                ZipInputStream(zipInputStream).use { zip ->
                    var entry = zip.nextEntry
                    val zipFiles = mutableListOf<String>()
                    
                    while (entry != null) {
                        val fullPath = entry.name
                        val fileName = fullPath.substringAfterLast("/")
                        zipFiles.add("$fullPath ($fileName)")
                        
                        Log.d(TAG, "Found ZIP entry: $fullPath")
                        Log.d(TAG, "Extracted filename: $fileName")
                        
                        // Process stream directly to avoid memory issues with large ZIP files
                        
                        when {
                            fileName.equals(FOOD_CSV, ignoreCase = true) -> {
                                Log.d(TAG, "Parsing food.csv")
                                parseFoodCSV(zip, foods)
                            }
                            fileName.equals(NUTRIENT_CSV, ignoreCase = true) -> {
                                Log.d(TAG, "Parsing nutrient.csv")
                                parseNutrientCSV(zip, nutrients)
                            }
                            fileName.equals(FOOD_NUTRIENT_CSV, ignoreCase = true) -> {
                                Log.d(TAG, "Parsing food_nutrient.csv")
                                parseFoodNutrientCSV(zip, foodNutrients)
                            }
                            fileName.equals(BRANDED_FOOD_CSV, ignoreCase = true) -> {
                                Log.d(TAG, "Parsing branded_food.csv")
                                parseBrandedFoodCSV(zip, foods)
                            }
                            fileName.equals(FOUNDATION_FOOD_CSV, ignoreCase = true) -> {
                                Log.d(TAG, "Parsing foundation_food.csv")
                                parseFoundationFoodCSV(zip, foods)
                            }
                            // Handle 2025 USDA dataset format - sr_legacy_food.csv
                            fileName.equals("sr_legacy_food.csv", ignoreCase = true) -> {
                                Log.d(TAG, "Parsing sr_legacy_food.csv (2025 format)")
                                parseSRLegacyFoodCSV(zip, foods)
                            }
                            // Check for any CSV file containing food data (more flexible matching)
                            fileName.endsWith(".csv") && 
                            (fileName.contains("food") || fileName.contains("sr_legacy") || 
                             fileName.contains("foundation") || fileName.contains("survey")) &&
                            !fileName.contains("nutrient") && !fileName.contains("branded") && 
                            !fileName.contains("update") && !fileName.contains("input") && 
                            !fileName.contains("log") -> {
                                Log.d(TAG, "Parsing generic food CSV file: $fileName")
                                if (fileName.contains("sr_legacy") || fileName.contains("legacy")) {
                                    parseSRLegacyFoodCSV(zip, foods)
                                } else {
                                    parseFoodCSV(zip, foods)
                                }
                            }
                            else -> {
                                Log.d(TAG, "Skipping unrecognized file: $fileName")
                            }
                        }
                        
                        // Don't call zip.closeEntry() as the BufferedReader has already consumed the stream
                        entry = zip.nextEntry
                    }
                    
                    Log.d(TAG, "ZIP file contained ${zipFiles.size} files:")
                    zipFiles.forEach { Log.d(TAG, "  - $it") }
                    
                    Log.d(TAG, "Final results: ${foods.size} foods, ${nutrients.size} nutrients, ${foodNutrients.size} food-nutrient mappings")
                }
                
                // Merge nutrition data into food items
                mergeNutritionData(foods, foodNutrients)
                
                Log.d(TAG, "Parsed ${foods.size} food items from bulk data")
                foods.values.toList()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing bulk ZIP file: ${e.message}", e)
                Log.e(TAG, "Exception type: ${e.javaClass.name}")
                Log.e(TAG, "Stack trace: ${e.stackTrace.take(5).joinToString("\n")}")
                emptyList()
            }
        }
    }
    
    /**
     * Parse the main food.csv file containing basic food information
     */
    private fun parseFoodCSV(inputStream: InputStream, foods: MutableMap<Int, USDAFoodItem>) {
        try {
            // Don't use .use {} to avoid closing the underlying ZipInputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val header = reader.readLine() // Skip header
            Log.d(TAG, "Food CSV header: $header")
            
            var line = reader.readLine()
            var count = 0
            
            while (line != null && count < 50000) { // Limit to prevent memory issues
                val parts = parseCSVLine(line)
                
                if (parts.size >= 4) {
                    try {
                        val fdcId = parts[0].toIntOrNull()
                        val dataType = parts[1]
                        val description = parts[2]
                        val publicationDate = parts.getOrNull(3) ?: ""
                        
                        if (fdcId != null && description.isNotBlank()) {
                            foods[fdcId] = USDAFoodItem(
                                fdcId = fdcId,
                                description = cleanFoodName(description),
                                dataType = dataType,
                                publicationDate = publicationDate,
                                brandOwner = null,
                                brandName = null,
                                ingredients = null,
                                servingSize = null,
                                servingSizeUnit = null,
                                householdServingFullText = null,
                                calories = 0.0,
                                protein = 0.0,
                                fat = 0.0,
                                carbohydrates = 0.0,
                                fiber = 0.0,
                                sugar = 0.0,
                                sodium = 0.0
                            )
                            count++
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing food line: $line", e)
                    }
                }
                line = reader.readLine()
            }
            
            Log.d(TAG, "Parsed $count food items")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing food CSV", e)
        }
    }
    
    /**
     * Parse nutrient.csv to get nutrient names and units
     */
    private fun parseNutrientCSV(inputStream: InputStream, nutrients: MutableMap<Int, String>) {
        try {
            // Don't use .use {} to avoid closing the underlying ZipInputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val header = reader.readLine() // Skip header
            
            var line = reader.readLine()
            while (line != null) {
                val parts = parseCSVLine(line)
                
                if (parts.size >= 3) {
                    try {
                        val nutrientId = parts[0].toIntOrNull()
                        val nutrientName = parts[1]
                        val unitName = parts[2]
                        
                        if (nutrientId != null) {
                            nutrients[nutrientId] = "$nutrientName ($unitName)"
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing nutrient line: $line", e)
                    }
                }
                line = reader.readLine()
            }
            
            Log.d(TAG, "Parsed ${nutrients.size} nutrients")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing nutrient CSV", e)
        }
    }
    
    /**
     * Parse food_nutrient.csv to get nutrition values for each food
     */
    private fun parseFoodNutrientCSV(inputStream: InputStream, foodNutrients: MutableMap<Int, MutableMap<Int, Double>>) {
        try {
            // Don't use .use {} to avoid closing the underlying ZipInputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val header = reader.readLine() // Skip header
            
            var line = reader.readLine()
            var count = 0
            
            while (line != null && count < 500000) { // Limit for performance
                val parts = parseCSVLine(line)
                
                if (parts.size >= 4) {
                    try {
                        val fdcId = parts[1].toIntOrNull()
                        val nutrientId = parts[2].toIntOrNull()
                        val amount = parts[3].toDoubleOrNull()
                        
                        if (fdcId != null && nutrientId != null && amount != null) {
                            val foodNutrientMap = foodNutrients.getOrPut(fdcId) { mutableMapOf() }
                            foodNutrientMap[nutrientId] = amount
                            count++
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing food nutrient line: $line", e)
                    }
                }
                line = reader.readLine()
            }
            
            Log.d(TAG, "Parsed $count food-nutrient relationships")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing food nutrient CSV", e)
        }
    }
    
    /**
     * Parse branded_food.csv for branded/packaged food information
     */
    private fun parseBrandedFoodCSV(inputStream: InputStream, foods: MutableMap<Int, USDAFoodItem>) {
        try {
            // Don't use .use {} to avoid closing the underlying ZipInputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val header = reader.readLine() // Skip header
            
            var line = reader.readLine()
            while (line != null) {
                val parts = parseCSVLine(line)
                
                if (parts.size >= 8) {
                    try {
                        val fdcId = parts[0].toIntOrNull()
                        
                        if (fdcId != null && foods.containsKey(fdcId)) {
                            val food = foods[fdcId] ?: continue
                            foods[fdcId] = food.copy(
                                brandOwner = parts.getOrNull(1)?.takeIf { it.isNotBlank() },
                                brandName = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
                                ingredients = parts.getOrNull(6)?.takeIf { it.isNotBlank() },
                                servingSize = parts.getOrNull(7)?.toDoubleOrNull(),
                                servingSizeUnit = parts.getOrNull(8)?.takeIf { it.isNotBlank() },
                                householdServingFullText = parts.getOrNull(9)?.takeIf { it.isNotBlank() }
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing branded food line: $line", e)
                    }
                }
                line = reader.readLine()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing branded food CSV", e)
        }
    }
    
    /**
     * Parse foundation_food.csv for foundation food information
     */
    private fun parseFoundationFoodCSV(inputStream: InputStream, foods: MutableMap<Int, USDAFoodItem>) {
        // Foundation foods don't have additional fields beyond basic food info
        // This method exists for completeness and future enhancements
        Log.d(TAG, "Processing foundation food data")
    }
    
    /**
     * Parse sr_legacy_food.csv for SR Legacy food information (2025 format)
     * Header format: "fdc_id","NDB_number","description","scientific_name","category","publication_date"
     */
    private fun parseSRLegacyFoodCSV(inputStream: InputStream, foods: MutableMap<Int, USDAFoodItem>) {
        try {
            // Don't use .use {} to avoid closing the underlying ZipInputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val header = reader.readLine() // Skip header
            Log.d(TAG, "SR Legacy CSV header (full): $header")
            Log.d(TAG, "SR Legacy CSV header length: ${header?.length ?: 0}")
            
            var line = reader.readLine()
            var count = 0
            var lineNumber = 2
            
            while (line != null && count < 50000) { // Limit to prevent memory issues
                val parts = parseCSVLine(line)
                Log.d(TAG, "Line $lineNumber: ${parts.size} parts - ${parts.take(3)}")
                
                if (parts.size >= 3) {
                    try {
                        val fdcId = parts[0].toIntOrNull()
                        val ndbNumber = parts.getOrNull(1) ?: ""
                        val description = parts[2]
                        val scientificName = parts.getOrNull(3) ?: ""
                        val category = parts.getOrNull(4) ?: ""
                        val publicationDate = parts.getOrNull(5) ?: ""
                        
                        if (fdcId != null && description.isNotBlank()) {
                            foods[fdcId] = USDAFoodItem(
                                fdcId = fdcId,
                                description = cleanFoodName(description),
                                dataType = "sr_legacy_food",
                                publicationDate = publicationDate,
                                brandOwner = null,
                                brandName = null,
                                ingredients = scientificName.takeIf { it.isNotBlank() },
                                servingSize = null,
                                servingSizeUnit = null,
                                householdServingFullText = null,
                                calories = 0.0,
                                protein = 0.0,
                                fat = 0.0,
                                carbohydrates = 0.0,
                                fiber = 0.0,
                                sugar = 0.0,
                                sodium = 0.0
                            )
                            count++
                            if (count <= 5) {
                                Log.d(TAG, "Successfully parsed food #$count: FDC_ID=$fdcId, Description='$description'")
                            }
                        } else {
                            if (lineNumber <= 10) {
                                Log.w(TAG, "Skipping line $lineNumber: fdcId=$fdcId, description='$description'")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing SR Legacy line $lineNumber: $line", e)
                    }
                } else {
                    if (lineNumber <= 10) {
                        Log.w(TAG, "Line $lineNumber has only ${parts.size} parts (need at least 3): $parts")
                    }
                }
                line = reader.readLine()
                lineNumber++
            }
            
            Log.d(TAG, "Parsed $count SR Legacy food items from $lineNumber lines")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SR Legacy CSV", e)
        }
    }
    
    /**
     * Merge nutrition data from food_nutrient.csv into food items
     */
    private fun mergeNutritionData(foods: MutableMap<Int, USDAFoodItem>, foodNutrients: Map<Int, Map<Int, Double>>) {
        for ((fdcId, food) in foods) {
            val nutrients = foodNutrients[fdcId] ?: continue
            
            foods[fdcId] = food.copy(
                calories = nutrients[ENERGY_KCAL_ID] ?: 0.0,
                protein = nutrients[PROTEIN_ID] ?: 0.0,
                fat = nutrients[TOTAL_FAT_ID] ?: 0.0,
                carbohydrates = nutrients[CARBS_ID] ?: 0.0,
                fiber = nutrients[FIBER_ID] ?: 0.0,
                sugar = nutrients[SUGAR_ID] ?: 0.0,
                sodium = nutrients[SODIUM_ID] ?: 0.0
            )
        }
        
        Log.d(TAG, "Merged nutrition data for ${foods.size} foods")
    }
    
    /**
     * Parse a CSV line handling quoted fields and commas within quotes
     * 
     * **CSV PARSING ALGORITHM EXPLAINED:**
     * 
     * **The Problem:**
     * Simple split(",") doesn't work for CSV because:
     * ```
     * 123,"Chicken, grilled",25.0
     * ```
     * Would incorrectly split into: ["123", "\"Chicken", " grilled\"", "25.0"]
     * But should be: ["123", "Chicken, grilled", "25.0"]
     * 
     * **Our Algorithm:**
     * 1. Go through each character one by one
     * 2. Track whether we're inside quotes or outside
     * 3. Only split on commas when outside of quotes
     * 4. Remove surrounding quotes from final values
     * 
     * **State Machine Approach:**
     * - inQuotes = false: Normal mode, split on commas
     * - inQuotes = true: Inside quoted field, ignore commas
     * - Quote character toggles the state
     * 
     * **Example Walkthrough:**
     * Input: `123,"Chicken, grilled",25.0`
     * 
     * Position | Char | inQuotes | Action
     * ---------|------|----------|--------
     * 0        | '1'  | false    | Add to current field
     * 1        | '2'  | false    | Add to current field
     * 2        | '3'  | false    | Add to current field
     * 3        | ','  | false    | End field: "123", start new field
     * 4        | '"'  | false    | Set inQuotes=true
     * 5        | 'C'  | true     | Add to current field
     * ...      | ...  | true     | Add to current field
     * 12       | ','  | true     | Add to current field (ignore comma)
     * 13       | ' '  | true     | Add to current field
     * ...      | ...  | true     | Add to current field
     * 20       | '"'  | true     | Set inQuotes=false
     * 21       | ','  | false    | End field: "Chicken, grilled", start new
     * 22       | '2'  | false    | Add to current field
     * ...
     * 
     * Result: ["123", "Chicken, grilled", "25.0"]
     */
    private fun parseCSVLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        
        while (i < line.length) {
            val char = line[i]
            
            when {
                char == '"' && (i == 0 || line[i-1] != '\\') -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.clear()
                }
                else -> {
                    current.append(char)
                }
            }
            i++
        }
        
        result.add(current.toString().trim())
        return result.map { it.removeSurrounding("\"") }
    }
    
    /**
     * Clean and normalize food names
     */
    private fun cleanFoodName(name: String): String {
        return name
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(255) // Limit length for database storage
    }
}