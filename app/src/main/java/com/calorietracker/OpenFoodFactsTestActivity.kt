package com.calorietracker

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.network.FoodDatabaseManager
import com.calorietracker.network.OpenFoodFactsService
import kotlinx.coroutines.launch

class OpenFoodFactsTestActivity : AppCompatActivity() {
    
    private lateinit var tvTestResults: TextView
    private lateinit var btnTestAPI: Button
    private lateinit var btnTestDatabase: Button
    private lateinit var btnDownloadSample: Button
    
    private lateinit var database: CalorieDatabase
    private lateinit var openFoodFactsService: OpenFoodFactsService
    private lateinit var databaseManager: FoodDatabaseManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(android.R.layout.activity_list_item)
        setupSimpleLayout()
        
        database = CalorieDatabase.getDatabase(this)
        openFoodFactsService = OpenFoodFactsService.create()
        databaseManager = FoodDatabaseManager(this, database)
        
        setupClickListeners()
        runInitialTests()
    }
    
    private fun setupSimpleLayout() {
        // Create a simple vertical layout programmatically
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(32, 32, 32, 32)
        
        tvTestResults = TextView(this)
        tvTestResults.text = "Open Food Facts Integration Test\n\nStarting tests..."
        tvTestResults.textSize = 16f
        layout.addView(tvTestResults)
        
        btnTestAPI = Button(this)
        btnTestAPI.text = "Test API Connection"
        layout.addView(btnTestAPI)
        
        btnTestDatabase = Button(this)
        btnTestDatabase.text = "Check Local Database"
        layout.addView(btnTestDatabase)
        
        btnDownloadSample = Button(this)
        btnDownloadSample.text = "Download Sample Data"
        layout.addView(btnDownloadSample)
        
        setContentView(layout)
    }
    
    private fun setupClickListeners() {
        btnTestAPI.setOnClickListener {
            testAPIConnection()
        }
        
        btnTestDatabase.setOnClickListener {
            checkLocalDatabase()
        }
        
        btnDownloadSample.setOnClickListener {
            downloadSampleData()
        }
    }
    
    private fun runInitialTests() {
        lifecycleScope.launch {
            appendToResults("\n=== INITIAL STATUS CHECK ===")
            checkLocalDatabase()
        }
    }
    
    private fun testAPIConnection() {
        lifecycleScope.launch {
            appendToResults("\n=== API CONNECTION TEST ===")
            
            try {
                // Test well-known Coca-Cola barcode
                appendToResults("Testing Coca-Cola barcode: 049000028430")
                val response = openFoodFactsService.getProductByBarcode("049000028430")
                
                val productResponse = if (response.isSuccessful) response.body() else null
                if (productResponse != null) {
                    if (productResponse.status == 1 && productResponse.product != null) {
                        val product = productResponse.product
                        appendToResults("✅ API Working!")
                        appendToResults("Product: ${product.productName}")
                        appendToResults("Brand: ${product.brands}")
                        appendToResults("Calories/100g: ${product.nutriments?.energyKcal100g}")
                        appendToResults("Categories: ${product.categories}")
                        
                        // Test search functionality
                        appendToResults("\nTesting search for 'coca cola'...")
                        val searchResponse = openFoodFactsService.searchProducts("coca cola", pageSize = 5)
                        
                        val searchResult = if (searchResponse.isSuccessful) searchResponse.body() else null
                        if (searchResult != null) {
                            appendToResults("✅ Search Working! Found ${searchResult.count} total products")
                            val products = searchResult.products.orEmpty()
                            appendToResults("Showing first ${products.size} results:")
                            products.take(3).forEach { product ->
                                appendToResults("- ${product.productName} (${product.brands})")
                            }
                        } else {
                            appendToResults("❌ Search API failed")
                        }
                        
                    } else {
                        appendToResults("❌ Product not found or invalid response")
                    }
                } else {
                    appendToResults("❌ API request failed: ${response.code()}")
                }
                
            } catch (e: Exception) {
                appendToResults("❌ API Error: ${e.message}")
                Log.e("OFFTest", "API test error", e)
            }
        }
    }
    
    private fun checkLocalDatabase() {
        lifecycleScope.launch {
            try {
                // Check Open Food Facts table
                val offCount = database.openFoodFactsDao().getCount()
                appendToResults("Local Open Food Facts items: $offCount")
                
                // Check database status
                val statusList = database.foodDatabaseStatusDao().getAllStatuses()
                val offStatus = statusList.find { it.databaseName == "openfoodfacts" }
                
                if (offStatus != null) {
                    appendToResults("Status: ${if (offStatus.isDownloading) "Downloading" else "Ready"}")
                    appendToResults("Total items: ${offStatus.totalItems}")
                    appendToResults("Downloaded: ${offStatus.downloadedItems}")
                    
                    if (offStatus.lastUpdated > 0) {
                        val date = java.util.Date(offStatus.lastUpdated)
                        appendToResults("Last updated: ${java.text.SimpleDateFormat("MM/dd/yyyy HH:mm", java.util.Locale.getDefault()).format(date)}")
                    }
                    
                    if (offStatus.errorMessage != null) {
                        appendToResults("⚠️ Error: ${offStatus.errorMessage}")
                    }
                } else {
                    appendToResults("No download status found - database not initialized")
                }
                
                // Test search in local database
                if (offCount > 0) {
                    appendToResults("\nTesting local search for 'coca'...")
                    val localResults = database.openFoodFactsDao().searchFoods("coca", 5)
                    appendToResults("Found ${localResults.size} local results:")
                    localResults.forEach { item ->
                        appendToResults("- ${item.productName} (${item.brands}) - ${item.energyKcal} cal/100g")
                    }
                }
                
            } catch (e: Exception) {
                appendToResults("❌ Database Error: ${e.message}")
                Log.e("OFFTest", "Database test error", e)
            }
        }
    }
    
    private fun downloadSampleData() {
        lifecycleScope.launch {
            appendToResults("\n=== DOWNLOADING SAMPLE DATA ===")
            appendToResults("Starting Open Food Facts download...")
            
            try {
                val result = databaseManager.downloadOpenFoodFactsDatabase()
                
                if (result.isSuccess) {
                    appendToResults("✅ Download completed successfully!")
                    checkLocalDatabase()
                } else {
                    appendToResults("❌ Download failed: ${result.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                appendToResults("❌ Download Error: ${e.message}")
                Log.e("OFFTest", "Download error", e)
            }
        }
    }
    
    private fun appendToResults(text: String) {
        runOnUiThread {
            tvTestResults.text = "${tvTestResults.text}\n$text"
            Log.d("OFFTest", text)
        }
    }
}