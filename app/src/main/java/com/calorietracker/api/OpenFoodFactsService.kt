package com.calorietracker.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * API service for Open Food Facts - a free, open database of food products
 * 
 * **RETROFIT INTERFACE CONCEPTS FOR BEGINNERS:**
 * 
 * **What is a Retrofit Interface?**
 * Think of this interface like a "menu" at a restaurant:
 * - Each function is a "dish" you can order (API endpoint)
 * - The annotations (@GET, @Path) tell the waiter how to place your order
 * - Retrofit is the waiter that takes your order to the kitchen (API server)
 * - The return value is the food (JSON data) delivered back to your table (app)
 * 
 * **HTTP Method Annotations:**
 * - @GET: "Please give me data" (like asking for information)
 * - @POST: "Please save this data" (like submitting a form)
 * - @PUT: "Please update this data" (like editing information)
 * - @DELETE: "Please remove this data" (like canceling an order)
 * 
 * **URL Path Building:**
 * - Base URL: "https://world.openfoodfacts.org/" (the restaurant address)
 * - Endpoint: "api/v0/product/{barcode}.json" (the specific dish)
 * - @Path replaces {barcode} with actual barcode value
 * - Final URL: "https://world.openfoodfacts.org/api/v0/product/123456789.json"
 * 
 * **Response<T> Wrapper:**
 * Instead of just returning data, Response gives us:
 * - response.isSuccessful: Did the request work? (true/false)
 * - response.code(): HTTP status code (200=success, 404=not found, etc.)
 * - response.body(): The actual data (or null if failed)
 * - response.errorBody(): Error message if something went wrong
 * 
 * **suspend Functions:**
 * Network operations are slow, so we use suspend functions:
 * - Can pause the function while waiting for network response
 * - Doesn't freeze the UI thread while waiting
 * - Automatically handles switching between background and UI threads
 * 
 * **Why Open Food Facts?**
 * - Free and open source (no API keys required)
 * - Excellent barcode coverage (millions of products)
 * - Community-driven (people worldwide contribute product data)
 * - More reliable than commercial APIs for consumer products
 * 
 * Website: https://world.openfoodfacts.org/
 */
interface OpenFoodFactsService {
    
    /**
     * Get product information by barcode/UPC code
     * 
     * **RETROFIT ANNOTATION BREAKDOWN:**
     * 
     * **@GET Annotation:**
     * - Tells Retrofit this function makes an HTTP GET request
     * - GET is for retrieving data (like asking for information)
     * - The URL pattern in quotes gets combined with base URL
     * 
     * **@Path Parameter:**
     * - {barcode} in the URL is a placeholder
     * - @Path("barcode") tells Retrofit to replace {barcode} with the actual value
     * - Example: if barcode = "123456789", URL becomes ".../product/123456789.json"
     * 
     * **Response<T> Return Type:**
     * - Wraps the actual data with HTTP response information
     * - Lets us check if the request succeeded before using the data
     * - Contains error information if something goes wrong
     * 
     * **Real-World Example:**
     * When you scan a Coca-Cola barcode (049000028911):
     * 1. This function gets called with barcode = "049000028911"
     * 2. Retrofit builds URL: "https://world.openfoodfacts.org/api/v0/product/049000028911.json"
     * 3. Makes HTTP GET request to that URL
     * 4. Server returns JSON data about Coca-Cola
     * 5. Gson converts JSON to OpenFoodFactsResponse object
     * 6. Function returns Response wrapping the OpenFoodFactsResponse
     * 
     * This is what we use when scanning barcodes to find food products
     */
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<OpenFoodFactsResponse>
}

/**
 * Response structure from Open Food Facts API
 * 
 * **DATA CLASS CONCEPTS:**
 * 
 * **What is a Data Class?**
 * A data class is like a "container" that holds related information:
 * - Automatically generates equals(), hashCode(), toString(), and copy() functions
 * - Primary purpose is to hold data, not behavior
 * - Perfect for representing API responses
 * 
 * **JSON Mapping:**
 * When Open Food Facts API returns JSON like this:
 * ```json
 * {
 *   "status": 1,
 *   "code": "049000028911",
 *   "status_verbose": "product found",
 *   "product": { "product_name": "Coca-Cola", ... }
 * }
 * ```
 * 
 * Gson automatically converts it to this Kotlin object:
 * - JSON property names match Kotlin property names
 * - JSON numbers become Int/Double
 * - JSON strings become String
 * - JSON objects become nested data classes
 * - JSON null becomes Kotlin null (with ? type)
 * 
 * **Nullable Types (?):**
 * - product: OpenFoodFactsProduct? means product can be null
 * - This happens when barcode is not found in database
 * - Prevents crashes by forcing us to check for null before using
 * 
 * **API Response Pattern:**
 * Most APIs follow this pattern:
 * - Status indicator (was request successful?)
 * - Error message (what went wrong?)
 * - Data payload (the actual information requested)
 * 
 * Contains information about whether the product was found and its details
 */
data class OpenFoodFactsResponse(
    val status: Int,           // 1 = found, 0 = not found
    val code: String,          // The barcode that was searched
    val status_verbose: String, // Human-readable status
    val product: OpenFoodFactsProduct? // Product details (null if not found)
)

/**
 * Response structure for Open Food Facts search API
 * Used when searching by product name instead of barcode
 */
data class OpenFoodFactsSearchResponse(
    val count: Int,            // Number of results found
    val page: Int,             // Current page number
    val page_count: Int,       // Total pages available
    val page_size: Int,        // Number of results per page
    val products: List<OpenFoodFactsProduct>? // List of matching products
)

/**
 * Product information from Open Food Facts
 * Contains all the nutrition and product details we need
 */
data class OpenFoodFactsProduct(
    val code: String,                    // Barcode
    val product_name: String?,           // Name of the product
    val product_name_en: String?,        // English name (fallback)
    val brands: String?,                 // Brand names (comma-separated)
    val quantity: String?,               // Package size (e.g., "355 ml")
    val serving_size: String?,           // Serving size (e.g., "1 bottle")
    val nutriments: OpenFoodFactsNutriments?, // Nutrition information
    val nutrient_levels: Map<String, String>?, // Nutrient quality levels
    val nova_group: Int?,                // NOVA food processing classification
    val ecoscore_grade: String?,         // Environmental impact score
    val nutriscore_grade: String?,       // Nutrition quality score
    val image_url: String?,              // Product image
    val image_front_url: String?,        // Front packaging image
    val ingredients_text: String?,        // List of ingredients
    val allergens: String?,              // Allergen information
    val traces: String?                  // May contain traces of...
)

/**
 * Nutrition information from Open Food Facts
 * Values are typically per 100g, but we'll convert to per serving
 */
data class OpenFoodFactsNutriments(
    // Energy values
    val energy_100g: Double?,            // Energy in kJ per 100g
    val `energy-kcal_100g`: Double?,     // Energy in kcal per 100g
    val `energy-kcal_serving`: Double?,  // Energy in kcal per serving
    
    // Macronutrients (per 100g)
    val proteins_100g: Double?,          // Protein in grams
    val carbohydrates_100g: Double?,     // Total carbs in grams
    val fat_100g: Double?,               // Total fat in grams
    val fiber_100g: Double?,             // Dietary fiber in grams
    val sugars_100g: Double?,            // Sugar in grams
    val sodium_100g: Double?,            // Sodium in grams
    val salt_100g: Double?,              // Salt in grams
    
    // Per serving values (if available)
    val proteins_serving: Double?,       // Protein per serving
    val carbohydrates_serving: Double?,  // Carbs per serving
    val fat_serving: Double?,            // Fat per serving
    val fiber_serving: Double?,          // Fiber per serving
    val sugars_serving: Double?,         // Sugar per serving
    val sodium_serving: Double?,         // Sodium per serving
    
    // Vitamins and minerals (per 100g)
    val `vitamin-c_100g`: Double?,       // Vitamin C in mg
    val calcium_100g: Double?,           // Calcium in mg
    val iron_100g: Double?,              // Iron in mg
    val potassium_100g: Double?,         // Potassium in mg
    
    // Quality indicators
    val `nutrition-score-fr_100g`: Double?, // French nutrition score
    val `nutrition-score-uk_100g`: Double?  // UK nutrition score
)