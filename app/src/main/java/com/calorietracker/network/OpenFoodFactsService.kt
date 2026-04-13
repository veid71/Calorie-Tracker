package com.calorietracker.network

// 🧰 NETWORK TOOLS - These help us talk to the internet
import com.google.gson.annotations.SerializedName  // Helps convert JSON data to Kotlin objects
import okhttp3.OkHttpClient
import retrofit2.Response                          // Wraps API responses with success/error info
import retrofit2.Retrofit                          // Library that makes API calls easy
import retrofit2.converter.gson.GsonConverterFactory // Converts JSON to Kotlin automatically
import retrofit2.http.GET                          // Tells Retrofit this is a GET request
import retrofit2.http.Path                         // Puts variables into URL paths
import retrofit2.http.Query                        // Adds query parameters to URLs
import java.util.concurrent.TimeUnit

/**
 * 🌐 OPEN FOOD FACTS API SERVICE - OUR FOOD DETECTIVE
 * 
 * Hey future programmer! This is like having a super-smart food detective on the internet.
 * 
 * 🕵️ What is Open Food Facts?
 * It's a free database with millions of food products from around the world!
 * When you scan a barcode or search for "Coca Cola", this service finds
 * the nutrition information for you automatically.
 * 
 * 🔍 What can our food detective do?
 * 1. Look up foods by barcode (scan a Coke can → get nutrition facts)
 * 2. Search for foods by name ("pizza" → show all pizza types)
 * 3. Find foods by category ("breakfast-cereals" → show all cereals)
 * 4. Find foods by brand ("Kellogg's" → show all Kellogg's products)
 * 
 * 🌍 Where does the data come from?
 * Website: https://world.openfoodfacts.org/
 * API Documentation: https://wiki.openfoodfacts.org/API
 * 
 * 🤖 How does this work?
 * We use a library called "Retrofit" that makes talking to internet APIs easy.
 * Instead of writing complicated internet code, we just write function signatures
 * and Retrofit handles all the messy network stuff for us!
 */
interface OpenFoodFactsService {
    
    /**
     * 📷 BARCODE LOOKUP - Find food by scanning its barcode
     * 
     * When you scan a barcode (like on a Coke can), this function asks the internet:
     * "Hey Open Food Facts, what food has barcode 049000028430?"
     * And it returns all the nutrition information!
     * 
     * 🔍 How it works:
     * 1. We send the barcode number to Open Food Facts
     * 2. They look it up in their huge database
     * 3. They send back the food name, calories, protein, etc.
     * 
     * @param barcode The barcode number (like "049000028430" for Coca-Cola)
     * @return Response containing the food's nutrition information
     */
    @GET("api/v0/product/{barcode}.json")  // This tells Retrofit what URL to call
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String   // Put the barcode number into the URL
    ): Response<OpenFoodFactsProductResponse>
    
    /**
     * 🔍 FOOD SEARCH - Find foods by typing their name
     * 
     * When you type "chocolate" in the search box, this function asks:
     * "Hey Open Food Facts, show me all foods that contain the word chocolate!"
     * 
     * 🎯 Search features:
     * - Finds foods by name (like "pizza", "apple", "chicken")
     * - Returns results sorted by popularity (most common foods first)
     * - Can specify how many results you want (page size)
     * - Can get different "pages" of results (like page 1, page 2, etc.)
     * 
     * @param searchTerms What to search for (like "chocolate chip cookies")
     * @param searchSimple Use simple search (1) or advanced (0)
     * @param action Always "process" for search requests
     * @param json Always 1 to get JSON response format
     * @param pageSize How many results to return (like 50 foods)
     * @param page Which page of results (1 = first page, 2 = second page, etc.)
     * @param sortBy How to sort results ("popularity" = most common first)
     * @return Response containing list of matching foods
     */
    @GET("cgi/search.pl")  // This is the URL endpoint for searching
    suspend fun searchProducts(
        @Query("search_terms") searchTerms: String,           // What to search for
        @Query("search_simple") searchSimple: Int = 1,        // Use simple search
        @Query("action") action: String = "process",          // Tell API we want to search
        @Query("json") json: Int = 1,                         // We want JSON response format
        @Query("page_size") pageSize: Int = 50,               // How many results per page
        @Query("page") page: Int = 1,                         // Which page (start with first page)
        @Query("sort_by") sortBy: String = "popularity"       // Most popular foods first
    ): Response<OpenFoodFactsSearchResponse>
    
    /**
     * Get products by category (v1 legacy endpoint — page_size capped at 24 by server).
     * Prefer searchProductsV2 for bulk downloads.
     */
    @GET("category/{category}.json")
    suspend fun getProductsByCategory(
        @Path("category") category: String,
        @Query("page_size") pageSize: Int = 24,
        @Query("page") page: Int = 1
    ): Response<OpenFoodFactsSearchResponse>

    /**
     * V2 search API — supports page_size up to 200 and proper tag-based filtering.
     * Use this for all bulk downloads.
     *
     * @param categoriesTags OFFs taxonomy tag, e.g. "en:dairy-products"
     * @param countriesTags  OFFs country tag, e.g. "en:united-states"
     * @param fields         Comma-separated list of fields to return (reduces response size)
     */
    @GET("api/v2/search")
    suspend fun searchProductsV2(
        @Query("categories_tags") categoriesTags: String? = null,
        @Query("countries_tags") countriesTags: String? = null,
        @Query("page_size") pageSize: Int = 200,
        @Query("page") page: Int = 1,
        @Query("fields") fields: String = "code,product_name,brands,categories,labels,countries," +
                "ingredients_text,allergens,serving_size,serving_quantity,nutriments," +
                "image_url,url,nutriscore_grade,nova_group,completeness,last_modified_t",
        @Query("sort_by") sortBy: String = "popularity"
    ): Response<OpenFoodFactsSearchResponse>

    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org/"

        fun create(): OpenFoodFactsService {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            // OFFs asks third-party apps to identify themselves
                            .header("User-Agent", "CalorieTrackerApp/3.1 (Android; open-source)")
                            .build()
                    )
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenFoodFactsService::class.java)
        }
    }
}

// Response data classes
data class OpenFoodFactsProductResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("status_verbose") val statusVerbose: String?,
    @SerializedName("product") val product: OpenFoodFactsProduct?
)

data class OpenFoodFactsSearchResponse(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("page_count") val pageCount: Int = 0,
    @SerializedName("page_size") val pageSize: Int = 0,
    @SerializedName("products") val products: List<OpenFoodFactsProduct>? = null
)

data class OpenFoodFactsProduct(
    @SerializedName("code") val code: String?,
    @SerializedName("product_name") val productName: String?,
    @SerializedName("generic_name") val genericName: String?,
    @SerializedName("brands") val brands: String?,
    @SerializedName("categories") val categories: String?,
    @SerializedName("labels") val labels: String?,
    @SerializedName("countries") val countries: String?,
    @SerializedName("ingredients_text") val ingredientsText: String?,
    @SerializedName("allergens") val allergens: String?,
    
    // Serving info
    @SerializedName("serving_size") val servingSize: String?,
    @SerializedName("serving_quantity") val servingQuantity: Double?,
    
    // Nutrition per 100g
    @SerializedName("nutriments") val nutriments: OpenFoodFactsNutriments?,
    
    // Product info
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("nutriscore_grade") val nutriscoreGrade: String?,
    @SerializedName("nova_group") val novaGroup: Int?,
    
    // Quality indicators
    @SerializedName("completeness") val completeness: Double?,
    @SerializedName("last_modified_t") val lastModified: Long?
)

data class OpenFoodFactsNutriments(
    @SerializedName("energy-kj") val energyKj: Double?,
    @SerializedName("energy-kj_100g") val energyKj100g: Double?,
    @SerializedName("energy-kcal") val energyKcal: Double?,
    @SerializedName("energy-kcal_100g") val energyKcal100g: Double?,
    @SerializedName("fat") val fat: Double?,
    @SerializedName("fat_100g") val fat100g: Double?,
    @SerializedName("saturated-fat") val saturatedFat: Double?,
    @SerializedName("saturated-fat_100g") val saturatedFat100g: Double?,
    @SerializedName("carbohydrates") val carbohydrates: Double?,
    @SerializedName("carbohydrates_100g") val carbohydrates100g: Double?,
    @SerializedName("sugars") val sugars: Double?,
    @SerializedName("sugars_100g") val sugars100g: Double?,
    @SerializedName("fiber") val fiber: Double?,
    @SerializedName("fiber_100g") val fiber100g: Double?,
    @SerializedName("proteins") val proteins: Double?,
    @SerializedName("proteins_100g") val proteins100g: Double?,
    @SerializedName("salt") val salt: Double?,
    @SerializedName("salt_100g") val salt100g: Double?,
    @SerializedName("sodium") val sodium: Double?,
    @SerializedName("sodium_100g") val sodium100g: Double?
)