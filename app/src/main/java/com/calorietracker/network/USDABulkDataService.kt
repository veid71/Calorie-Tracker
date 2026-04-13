package com.calorietracker.network

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import okhttp3.ResponseBody

/**
 * USDA Food Data Central Bulk Data Download Service
 * Documentation: https://fdc.nal.usda.gov/download-datasets.html
 * 
 * Alternative to the API that downloads complete database files
 * These are updated regularly and bypass API outages
 */
interface USDABulkDataService {
    
    /**
     * Download Foundation Foods CSV file (~2MB)
     * Contains nutrient data for ~1,000 foods that represent the foundation of the US food supply
     */
    @Streaming
    @GET
    suspend fun downloadFoundationFoods(@Url url: String = FOUNDATION_FOODS_URL): Response<ResponseBody>
    
    /**
     * Download SR Legacy Foods CSV file (~15MB)
     * Contains data from the legacy Standard Reference database (~8,000 foods)
     */
    @Streaming
    @GET
    suspend fun downloadSRLegacyFoods(@Url url: String = SR_LEGACY_FOODS_URL): Response<ResponseBody>
    
    /**
     * Download Branded Foods CSV file (~200MB+)
     * Contains data for branded/packaged foods (~300,000+ items)
     */
    @Streaming
    @GET
    suspend fun downloadBrandedFoods(@Url url: String = BRANDED_FOODS_URL): Response<ResponseBody>
    
    /**
     * Download Full USDA Dataset CSV file (~3GB)
     * Contains ALL food data types: Foundation, SR Legacy, Survey, and Branded foods (~300,000+ foods)
     * Includes standard CSV files: food.csv, nutrient.csv, food_nutrient.csv, etc.
     */
    @Streaming
    @GET
    suspend fun downloadSurveyFoods(@Url url: String = SURVEY_FOODS_URL): Response<ResponseBody>
    
    companion object {
        private const val BASE_URL = "https://fdc.nal.usda.gov/"
        
        // Bulk download URLs (updated regularly by USDA - April 2025 release)
        const val FOUNDATION_FOODS_URL = "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_foundation_food_csv_2025-04-24.zip"
        const val SR_LEGACY_FOODS_URL = "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_csv_2018-04.zip"
        const val BRANDED_FOODS_URL = "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_branded_food_csv_2025-04-24.zip"
        const val SURVEY_FOODS_URL = "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_csv_2025-04-24.zip"
        
        // Alternative: Smaller subset files for testing
        const val FOUNDATION_FOODS_SMALL_URL = "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_foundation_food_csv_2025-04-24.zip"
        
        fun create(): USDABulkDataService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .build()
                .create(USDABulkDataService::class.java)
        }
    }
}

/**
 * Data classes for parsing CSV files from USDA bulk downloads
 */
data class USDAFoundationFood(
    val fdcId: Int,
    val dataType: String,
    val description: String,
    val foodCategory: String?,
    val publicationDate: String?
)

data class USDANutrientData(
    val fdcId: Int,
    val nutrientId: Int,
    val amount: Double,
    val dataPoints: Int?,
    val derivationId: Int?,
    val min: Double?,
    val max: Double?,
    val median: Double?,
    val footnote: String?,
    val minYearAcquired: Int?
)