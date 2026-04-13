package com.calorietracker.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * USDA FoodData Central API service
 * Documentation: https://fdc.nal.usda.gov/api-guide.html
 */
interface USDAFoodService {
    
    /**
     * Get food list with pagination
     */
    @GET("v1/foods/list")
    suspend fun getFoodList(
        @Query("api_key") apiKey: String,
        @Query("dataType") dataType: String? = null, // "Foundation", "SR Legacy", "Survey (FNDDS)", "Branded"
        @Query("pageSize") pageSize: Int = 200,
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("sortBy") sortBy: String? = "description",
        @Query("sortOrder") sortOrder: String? = "asc"
    ): Response<List<USDAFoodBasic>>
    
    /**
     * Search for foods
     */
    @GET("v1/foods/search")
    suspend fun searchFoods(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("dataType") dataType: String? = null,
        @Query("pageSize") pageSize: Int = 50,
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("sortBy") sortBy: String? = "description",
        @Query("sortOrder") sortOrder: String? = "asc",
        @Query("brandOwner") brandOwner: String? = null
    ): Response<USDAFoodSearchResponse>
    
    /**
     * Get detailed food information by FDC ID
     */
    @GET("v1/food/{fdcId}")
    suspend fun getFoodById(
        @Path("fdcId") fdcId: Int,
        @Query("api_key") apiKey: String,
        @Query("nutrients") nutrients: String? = null
    ): Response<USDAFoodDetail>
    
    /**
     * Get multiple foods by FDC IDs (alternative endpoint)
     */
    @POST("v1/foods")
    suspend fun getFoodsByIds(
        @Body request: USDAFoodsRequest
    ): Response<List<USDAFoodDetail>>
    
    /**
     * Get foods list using alternative parameters (may use different infrastructure)
     */
    @GET("v1/foods")
    suspend fun getFoodsAlternative(
        @Query("api_key") apiKey: String,
        @Query("query") query: String? = null,
        @Query("dataType") dataType: String? = null,
        @Query("pageSize") pageSize: Int = 50,
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("requireAllWords") requireAllWords: Boolean? = null
    ): Response<List<USDAFoodBasic>>
    
    companion object {
        private const val BASE_URL = "https://api.nal.usda.gov/fdc/"
        
        fun create(): USDAFoodService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(USDAFoodService::class.java)
        }
    }
}

// Response data classes - USDA foods/list endpoint returns an array directly
// So we'll use List<USDAFoodBasic> directly as the response type

data class USDAFoodSearchResponse(
    @SerializedName("totalHits") val totalHits: Int,
    @SerializedName("currentPage") val currentPage: Int,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("foods") val foods: List<USDAFoodBasic>
)

data class USDAFoodBasic(
    @SerializedName("fdcId") val fdcId: Int,
    @SerializedName("description") val description: String,
    @SerializedName("dataType") val dataType: String,
    @SerializedName("publicationDate") val publicationDate: String? = null,
    @SerializedName("ndbNumber") val ndbNumber: String? = null,
    @SerializedName("brandOwner") val brandOwner: String? = null,
    @SerializedName("brandName") val brandName: String? = null,
    @SerializedName("ingredients") val ingredients: String? = null,
    @SerializedName("servingSize") val servingSize: Double? = null,
    @SerializedName("servingSizeUnit") val servingSizeUnit: String? = null,
    @SerializedName("householdServingFullText") val householdServingFullText: String? = null,
    @SerializedName("foodCategory") val foodCategory: String? = null,
    @SerializedName("foodNutrients") val foodNutrients: List<USDANutrient>? = null
)

data class USDAFoodDetail(
    @SerializedName("fdcId") val fdcId: Int,
    @SerializedName("description") val description: String,
    @SerializedName("dataType") val dataType: String,
    @SerializedName("publicationDate") val publicationDate: String?,
    @SerializedName("brandOwner") val brandOwner: String?,
    @SerializedName("brandName") val brandName: String?,
    @SerializedName("ingredients") val ingredients: String?,
    @SerializedName("servingSize") val servingSize: Double?,
    @SerializedName("servingSizeUnit") val servingSizeUnit: String?,
    @SerializedName("householdServingFullText") val householdServingFullText: String?,
    @SerializedName("foodCategory") val foodCategory: String?,
    @SerializedName("foodNutrients") val foodNutrients: List<USDANutrient>
)

data class USDANutrient(
    @SerializedName("nutrientId") val nutrientId: Int? = null,
    @SerializedName("number") val number: String,
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("unitName") val unitName: String,
    @SerializedName("derivationCode") val derivationCode: String? = null,
    @SerializedName("derivationDescription") val derivationDescription: String? = null
)

data class USDAFoodsRequest(
    @SerializedName("fdcIds") val fdcIds: List<Int>,
    @SerializedName("format") val format: String = "abridged",
    @SerializedName("nutrients") val nutrients: List<Int>? = null
)