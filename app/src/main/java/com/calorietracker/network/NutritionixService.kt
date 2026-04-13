package com.calorietracker.network

import retrofit2.Response
import retrofit2.http.*

/**
 * Nutritionix API service for restaurant and branded food data
 * Provides comprehensive coverage of restaurant chains and packaged foods
 */
interface NutritionixService {
    
    companion object {
        const val BASE_URL = "https://trackapi.nutritionix.com/v2/"
        
        // API endpoints
        const val SEARCH_INSTANT = "search/instant"
        const val SEARCH_NATURAL = "natural/nutrients"
        const val SEARCH_ITEM = "search/item"
        const val SEARCH_BRAND = "search/brand"
    }
    
    /**
     * Instant search for branded and common foods
     * Returns quick results for autocomplete/dropdown suggestions
     */
    @GET(SEARCH_INSTANT)
    suspend fun searchInstant(
        @Header("x-app-id") appId: String,
        @Header("x-app-key") appKey: String,
        @Query("query") query: String,
        @Query("detailed") detailed: Boolean = false
    ): Response<NutritionixInstantResponse>
    
    /**
     * Natural language nutrition search
     * Parse natural language like "2 slices of pizza" or "large Big Mac"
     */
    @POST(SEARCH_NATURAL)
    suspend fun searchNatural(
        @Header("x-app-id") appId: String,
        @Header("x-app-key") appKey: String,
        @Body request: NutritionixNaturalRequest
    ): Response<NutritionixNaturalResponse>
    
    /**
     * Get detailed nutrition info for a specific branded food item
     */
    @GET(SEARCH_ITEM)
    suspend fun getItemDetails(
        @Header("x-app-id") appId: String,
        @Header("x-app-key") appKey: String,
        @Query("nix_item_id") itemId: String
    ): Response<NutritionixItemResponse>
    
    /**
     * Search for foods from a specific brand/restaurant
     */
    @GET(SEARCH_BRAND)
    suspend fun searchBrand(
        @Header("x-app-id") appId: String,
        @Header("x-app-key") appKey: String,
        @Query("brand_id") brandId: String,
        @Query("query") query: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<NutritionixBrandResponse>
}

/**
 * Request body for natural language search
 */
data class NutritionixNaturalRequest(
    val query: String,
    val num_servings: Int? = null,
    val aggregate: String? = null,
    val line_delimited: Boolean? = null,
    val use_raw_foods: Boolean? = null,
    val include_subrecipe: Boolean? = null,
    val timezone: String = "US/Eastern",
    val consumed_at: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val meal_type: Int? = null,
    val use_branded_foods: Boolean? = null,
    val locale: String = "en_US"
)

/**
 * Response from instant search API
 */
data class NutritionixInstantResponse(
    val branded: List<NutritionixBrandedFood>?,
    val common: List<NutritionixCommonFood>?,
    val self: List<NutritionixCommonFood>?
)

/**
 * Branded food item from instant search
 */
data class NutritionixBrandedFood(
    val food_name: String,
    val serving_unit: String?,
    val nix_brand_id: String?,
    val brand_name_item_name: String?,
    val serving_qty: Double?,
    val nf_calories: Double?,
    val nf_total_fat: Double?,
    val nf_saturated_fat: Double?,
    val nf_cholesterol: Double?,
    val nf_sodium: Double?,
    val nf_total_carbohydrate: Double?,
    val nf_dietary_fiber: Double?,
    val nf_sugars: Double?,
    val nf_protein: Double?,
    val nf_potassium: Double?,
    val nf_p: Double?,
    val full_nutrients: List<NutritionixNutrient>?,
    val nix_item_name: String?,
    val nix_item_id: String?,
    val metadata: NutritionixMetadata?,
    val source: Int?,
    val ndb_no: Int?,
    val tags: NutritionixTags?,
    val alt_measures: List<NutritionixAltMeasure>?,
    val lat: Double?,
    val lng: Double?,
    val photo: NutritionixPhoto?,
    val note: String?,
    val class_code: Int?,
    val brick_code: Int?,
    val tag_id: Int?
)

/**
 * Common food item from instant search
 */
data class NutritionixCommonFood(
    val food_name: String,
    val serving_unit: String?,
    val tag_name: String?,
    val serving_qty: Double?,
    val common_type: Int?,
    val tag_id: Int?,
    val photo: NutritionixPhoto?,
    val locale: String?
)

/**
 * Response from natural language search
 */
data class NutritionixNaturalResponse(
    val foods: List<NutritionixFood>?
)

/**
 * Detailed food item with complete nutrition information
 */
data class NutritionixFood(
    val food_name: String,
    val brand_name: String?,
    val serving_qty: Double?,
    val serving_unit: String?,
    val serving_weight_grams: Double?,
    val nf_metric_qty: Double?,
    val nf_metric_uom: String?,
    val nf_calories: Double?,
    val nf_total_fat: Double?,
    val nf_saturated_fat: Double?,
    val nf_cholesterol: Double?,
    val nf_sodium: Double?,
    val nf_total_carbohydrate: Double?,
    val nf_dietary_fiber: Double?,
    val nf_sugars: Double?,
    val nf_protein: Double?,
    val nf_potassium: Double?,
    val nf_p: Double?,
    val full_nutrients: List<NutritionixNutrient>?,
    val nix_brand_name: String?,
    val nix_brand_id: String?,
    val nix_item_name: String?,
    val nix_item_id: String?,
    val upc: String?,
    val consumed_at: String?,
    val metadata: NutritionixMetadata?,
    val source: Int?,
    val ndb_no: Int?,
    val tags: NutritionixTags?,
    val alt_measures: List<NutritionixAltMeasure>?,
    val lat: Double?,
    val lng: Double?,
    val meal_type: Int?,
    val photo: NutritionixPhoto?,
    val sub_recipe: String?,
    val class_code: Int?,
    val brick_code: Int?,
    val tag_id: Int?
)

/**
 * Individual nutrient information
 */
data class NutritionixNutrient(
    val attr_id: Int,
    val value: Double
)

/**
 * Metadata about the food item
 */
data class NutritionixMetadata(
    val is_raw_food: Boolean?
)

/**
 * Tags associated with the food
 */
data class NutritionixTags(
    val item: String?,
    val measure: String?,
    val quantity: String?,
    val food_group: Int?,
    val tag_id: Int?
)

/**
 * Alternative serving measurements
 */
data class NutritionixAltMeasure(
    val serving_weight: Double?,
    val measure: String?,
    val seq: Int?,
    val qty: Double?
)

/**
 * Photo information for the food item
 */
data class NutritionixPhoto(
    val thumb: String?,
    val highres: String?,
    val is_user_uploaded: Boolean?
)

/**
 * Response from item details API
 */
data class NutritionixItemResponse(
    val foods: List<NutritionixFood>?
)

/**
 * Response from brand search API
 */
data class NutritionixBrandResponse(
    val foods: List<NutritionixFood>?
)