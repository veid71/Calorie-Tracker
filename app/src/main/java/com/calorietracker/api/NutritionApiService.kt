package com.calorietracker.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NutritionApiService {
    
    @GET("search")
    suspend fun searchFood(
        @Query("query") query: String,
        @Query("app_id") appId: String,
        @Query("app_key") appKey: String
    ): Response<EdamamResponse>
    
    @GET("nutrients")
    suspend fun getFoodByBarcode(
        @Query("upc") barcode: String,
        @Query("app_id") appId: String,
        @Query("app_key") appKey: String
    ): Response<EdamamResponse>
}

data class EdamamResponse(
    val hints: List<FoodHint>
)

data class FoodHint(
    val food: EdamamFood,
    val measures: List<Measure>?
)

data class EdamamFood(
    val foodId: String,
    val label: String,
    val brand: String?,
    val nutrients: EdamamNutrients,
    val servingSize: Double?,
    val servingSizeUnit: String?
)

data class EdamamNutrients(
    val ENERC_KCAL: Double?, // Energy (calories)
    val PROCNT: Double?,     // Protein
    val CHOCDF: Double?,     // Carbohydrates
    val FAT: Double?,        // Fat
    val FIBTG: Double?,      // Fiber
    val SUGAR: Double?,      // Sugar
    val NA: Double?          // Sodium
)

data class Measure(
    val uri: String,
    val label: String,
    val weight: Double
)