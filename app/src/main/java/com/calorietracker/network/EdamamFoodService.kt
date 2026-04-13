package com.calorietracker.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Edamam Food Database API service
 * Documentation: https://developer.edamam.com/food-database-api-docs
 * 
 * Provides access to 900,000+ foods including USDA data
 */
interface EdamamFoodService {
    
    /**
     * Search for foods in Edamam database
     */
    @GET("api/food-database/v2/parser")
    suspend fun searchFoods(
        @Query("app_id") appId: String,
        @Query("app_key") appKey: String,
        @Query("ingr") ingredient: String,
        @Query("nutrition-type") nutritionType: String = "cooking", // "cooking" or "logging"
        @Query("category") category: String? = null,
        @Query("health") health: String? = null,
        @Query("diet") diet: String? = null,
        @Query("calories") calories: String? = null,
        @Query("time") time: String? = null,
        @Query("imageSize") imageSize: String? = null,
        @Query("glycemicIndex") glycemicIndex: String? = null,
        @Query("co2EmissionsClass") co2EmissionsClass: String? = null
    ): Response<EdamamSearchResponse>
    
    companion object {
        private const val BASE_URL = "https://api.edamam.com/"
        
        fun create(): EdamamFoodService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(EdamamFoodService::class.java)
        }
    }
}

// Response data classes for Edamam API
data class EdamamSearchResponse(
    @SerializedName("text") val text: String,
    @SerializedName("parsed") val parsed: List<EdamamParsedFood>,
    @SerializedName("hints") val hints: List<EdamamHintFood>,
    @SerializedName("_links") val links: EdamamLinks?
)

data class EdamamParsedFood(
    @SerializedName("food") val food: EdamamFood
)

data class EdamamHintFood(
    @SerializedName("food") val food: EdamamFood,
    @SerializedName("measures") val measures: List<EdamamMeasure>?
)

data class EdamamFood(
    @SerializedName("foodId") val foodId: String,
    @SerializedName("label") val label: String,
    @SerializedName("knownAs") val knownAs: String? = null,
    @SerializedName("nutrients") val nutrients: EdamamNutrients,
    @SerializedName("category") val category: String? = null,
    @SerializedName("categoryLabel") val categoryLabel: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("foodContentsLabel") val foodContentsLabel: String? = null,
    @SerializedName("servingsPerContainer") val servingsPerContainer: Double? = null
)

data class EdamamNutrients(
    @SerializedName("ENERC_KCAL") val calories: Double? = null, // Energy (kcal)
    @SerializedName("PROCNT") val protein: Double? = null, // Protein (g)
    @SerializedName("FAT") val fat: Double? = null, // Total fat (g)
    @SerializedName("CHOCDF") val carbohydrates: Double? = null, // Carbohydrates (g)
    @SerializedName("FIBTG") val fiber: Double? = null, // Fiber (g)
    @SerializedName("SUGAR") val sugar: Double? = null, // Sugar (g)
    @SerializedName("NA") val sodium: Double? = null, // Sodium (mg)
    @SerializedName("CA") val calcium: Double? = null, // Calcium (mg)
    @SerializedName("MG") val magnesium: Double? = null, // Magnesium (mg)
    @SerializedName("K") val potassium: Double? = null, // Potassium (mg)
    @SerializedName("FE") val iron: Double? = null, // Iron (mg)
    @SerializedName("ZN") val zinc: Double? = null, // Zinc (mg)
    @SerializedName("P") val phosphorus: Double? = null, // Phosphorus (mg)
    @SerializedName("VITA_RAE") val vitaminA: Double? = null, // Vitamin A (µg)
    @SerializedName("VITC") val vitaminC: Double? = null, // Vitamin C (mg)
    @SerializedName("THIA") val thiamin: Double? = null, // Thiamin (mg)
    @SerializedName("RIBF") val riboflavin: Double? = null, // Riboflavin (mg)
    @SerializedName("NIA") val niacin: Double? = null, // Niacin (mg)
    @SerializedName("VITB6A") val vitaminB6: Double? = null, // Vitamin B6 (mg)
    @SerializedName("FOLDFE") val folate: Double? = null, // Folate (µg)
    @SerializedName("VITB12") val vitaminB12: Double? = null, // Vitamin B12 (µg)
    @SerializedName("VITD") val vitaminD: Double? = null, // Vitamin D (µg)
    @SerializedName("TOCPHA") val vitaminE: Double? = null, // Vitamin E (mg)
    @SerializedName("VITK1") val vitaminK: Double? = null, // Vitamin K (µg)
    @SerializedName("FASAT") val saturatedFat: Double? = null, // Saturated fat (g)
    @SerializedName("FAMS") val monounsaturatedFat: Double? = null, // Monounsaturated fat (g)
    @SerializedName("FAPU") val polyunsaturatedFat: Double? = null, // Polyunsaturated fat (g)
    @SerializedName("FATRN") val transFat: Double? = null, // Trans fat (g)
    @SerializedName("CHOLE") val cholesterol: Double? = null, // Cholesterol (mg)
    @SerializedName("WATER") val water: Double? = null // Water (g)
)

data class EdamamMeasure(
    @SerializedName("uri") val uri: String,
    @SerializedName("label") val label: String,
    @SerializedName("weight") val weight: Double,
    @SerializedName("qualified") val qualified: List<EdamamQualified>?
)

data class EdamamQualified(
    @SerializedName("qualifiers") val qualifiers: List<EdamamQualifier>,
    @SerializedName("weight") val weight: Double
)

data class EdamamQualifier(
    @SerializedName("uri") val uri: String,
    @SerializedName("label") val label: String
)

data class EdamamLinks(
    @SerializedName("next") val next: EdamamLink?
)

data class EdamamLink(
    @SerializedName("href") val href: String,
    @SerializedName("title") val title: String
)