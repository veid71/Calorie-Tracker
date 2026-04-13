package com.calorietracker.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.calorietracker.utils.ErrorHandler
import kotlinx.coroutines.delay
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 🌐 ENHANCED NETWORK ERROR HANDLER
 * 
 * Advanced error handling specifically for network operations with:
 * - Smart retry mechanisms with exponential backoff
 * - Network connectivity detection  
 * - HTTP status code analysis
 * - Circuit breaker pattern for failing services
 * - Graceful degradation strategies
 */
object NetworkErrorHandler {
    
    private const val TAG = "NetworkErrorHandler"
    
    // Circuit breaker configuration
    private const val FAILURE_THRESHOLD = 3
    private const val RECOVERY_TIMEOUT_MS = 30000L // 30 seconds
    
    // Retry configuration
    private const val MAX_RETRIES = 3
    private const val BASE_DELAY_MS = 1000L // 1 second
    
    // Track service health for circuit breaker pattern
    private val serviceHealthMap = mutableMapOf<String, ServiceHealth>()
    private const val MAX_TRACKED_SERVICES = 100
    private var lastCleanup = System.currentTimeMillis()
    
    /**
     * 🔄 EXECUTE NETWORK REQUEST WITH SMART RETRY
     * 
     * Wraps network calls with intelligent error handling:
     * - Automatic retries for transient failures
     * - Exponential backoff to avoid overwhelming servers
     * - Circuit breaker to fail fast on persistent issues
     */
    suspend fun <T> executeWithRetry(
        context: Context,
        serviceName: String,
        operation: suspend () -> T,
        fallback: (() -> T)? = null,
        userFriendlyAction: String = "network request"
    ): NetworkResult<T> {
        
        // Check circuit breaker
        if (isCircuitBreakerOpen(serviceName)) {
            Log.w(TAG, "Circuit breaker open for $serviceName, failing fast")
            return NetworkResult.CircuitBreakerOpen(
                "Service temporarily unavailable. Please try again later."
            )
        }
        
        var lastException: Exception? = null
        
        for (attempt in 1..MAX_RETRIES) {
            try {
                // Check network connectivity before attempting
                if (!isNetworkAvailable(context)) {
                    return NetworkResult.NoNetwork(
                        "No internet connection. Please check your network settings."
                    )
                }
                
                Log.d(TAG, "Attempting $serviceName request (attempt $attempt/$MAX_RETRIES)")
                
                val result = operation()
                
                // Success - reset service health
                recordSuccess(serviceName)
                return NetworkResult.Success(result)
                
            } catch (e: Exception) {
                lastException = e
                val errorType = categorizeError(e)
                
                Log.w(TAG, "$serviceName attempt $attempt failed: ${e.message}")
                
                // Record failure for circuit breaker
                recordFailure(serviceName)
                
                // Don't retry for certain errors
                if (!shouldRetry(errorType, attempt)) {
                    break
                }
                
                // Exponential backoff delay
                if (attempt < MAX_RETRIES) {
                    val delayMs = calculateBackoffDelay(attempt)
                    Log.d(TAG, "Waiting ${delayMs}ms before retry...")
                    delay(delayMs)
                }
            }
        }
        
        // All retries failed
        val finalError = lastException ?: Exception("Unknown network error")
        Log.e(TAG, "$serviceName failed after $MAX_RETRIES attempts", finalError)
        
        // Try fallback if available
        fallback?.let { fb ->
            try {
                Log.d(TAG, "Attempting fallback for $serviceName")
                val fallbackResult = fb()
                return NetworkResult.FallbackUsed(fallbackResult, finalError)
            } catch (e: Exception) {
                Log.e(TAG, "Fallback also failed for $serviceName", e)
            }
        }
        
        // Handle user-facing error display
        ErrorHandler.handleError(
            context = context,
            error = finalError,
            action = userFriendlyAction,
            displayMethod = ErrorHandler.DisplayMethod.SNACKBAR
        )
        
        return NetworkResult.Failed(finalError)
    }
    
    /**
     * 🏥 ANALYZE HTTP RESPONSE FOR DETAILED ERROR HANDLING
     */
    fun <T> analyzeHttpResponse(
        response: Response<T>,
        serviceName: String = "Unknown"
    ): HttpResponseAnalysis {
        Log.d(TAG, "Analyzing HTTP response for $serviceName: ${response.code()}")
        return when (response.code()) {
            in 200..299 -> HttpResponseAnalysis(
                isSuccess = true,
                category = HttpErrorCategory.SUCCESS,
                userMessage = null,
                shouldRetry = false
            )
            
            400 -> HttpResponseAnalysis(
                isSuccess = false,
                category = HttpErrorCategory.CLIENT_ERROR,
                userMessage = "Invalid request. Please check your input.",
                shouldRetry = false
            )
            
            401 -> HttpResponseAnalysis(
                isSuccess = false,
                category = HttpErrorCategory.AUTHENTICATION,
                userMessage = "API key invalid or expired. Please check settings.",
                shouldRetry = false
            )
            
            403 -> HttpResponseAnalysis(
                isSuccess = false,
                category = HttpErrorCategory.AUTHORIZATION,
                userMessage = "Access denied. Please check your permissions.",
                shouldRetry = false
            )
            
            404 -> HttpResponseAnalysis(
                isSuccess = false,
                category = HttpErrorCategory.NOT_FOUND,
                userMessage = "Requested data not found.",
                shouldRetry = false
            )
            
            429 -> HttpResponseAnalysis(
                isSuccess = false,
                category = HttpErrorCategory.RATE_LIMITED,
                userMessage = "Too many requests. Please wait a moment and try again.",
                shouldRetry = true
            )
            
            in 500..599 -> HttpResponseAnalysis(
                isSuccess = false,
                category = HttpErrorCategory.SERVER_ERROR,
                userMessage = "Server error. Please try again later.",
                shouldRetry = true
            )
            
            else -> HttpResponseAnalysis(
                isSuccess = false,
                category = HttpErrorCategory.UNKNOWN,
                userMessage = "Unexpected response from server.",
                shouldRetry = true
            )
        }
    }
    
    /**
     * 🔍 CHECK NETWORK CONNECTIVITY
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * 🏷️ CATEGORIZE EXCEPTION TYPE
     */
    private fun categorizeError(exception: Exception): NetworkErrorType {
        return when (exception) {
            is UnknownHostException -> NetworkErrorType.NO_NETWORK
            is ConnectException -> NetworkErrorType.CONNECTION_FAILED
            is SocketTimeoutException -> NetworkErrorType.TIMEOUT
            is HttpException -> when (exception.code()) {
                401 -> NetworkErrorType.AUTHENTICATION
                403 -> NetworkErrorType.AUTHORIZATION
                429 -> NetworkErrorType.RATE_LIMITED
                in 500..599 -> NetworkErrorType.SERVER_ERROR
                else -> NetworkErrorType.HTTP_ERROR
            }
            is IOException -> NetworkErrorType.IO_ERROR
            else -> NetworkErrorType.UNKNOWN
        }
    }
    
    /**
     * 🔄 DETERMINE IF ERROR SHOULD TRIGGER RETRY
     */
    private fun shouldRetry(errorType: NetworkErrorType, attemptNumber: Int): Boolean {
        if (attemptNumber >= MAX_RETRIES) return false
        
        return when (errorType) {
            NetworkErrorType.NO_NETWORK -> false // Don't retry if no network
            NetworkErrorType.CONNECTION_FAILED -> true
            NetworkErrorType.TIMEOUT -> true
            NetworkErrorType.RATE_LIMITED -> true
            NetworkErrorType.SERVER_ERROR -> true
            NetworkErrorType.IO_ERROR -> true
            NetworkErrorType.AUTHENTICATION -> false // API key issues won't resolve with retry
            NetworkErrorType.AUTHORIZATION -> false
            NetworkErrorType.HTTP_ERROR -> false
            NetworkErrorType.UNKNOWN -> true
        }
    }
    
    /**
     * ⏰ CALCULATE EXPONENTIAL BACKOFF DELAY
     */
    private fun calculateBackoffDelay(attemptNumber: Int): Long {
        return BASE_DELAY_MS * (1L shl (attemptNumber - 1)) // 2^(attempt-1) * base delay
    }
    
    /**
     * 🔴 CIRCUIT BREAKER IMPLEMENTATION
     */
    private fun isCircuitBreakerOpen(serviceName: String): Boolean {
        val health = serviceHealthMap[serviceName] ?: return false
        
        if (health.failures >= FAILURE_THRESHOLD) {
            val timeSinceLastFailure = System.currentTimeMillis() - health.lastFailureTime
            if (timeSinceLastFailure < RECOVERY_TIMEOUT_MS) {
                return true // Circuit breaker is open
            } else {
                // Recovery timeout elapsed, reset and allow one attempt
                serviceHealthMap[serviceName] = ServiceHealth(0, 0, 0L)
            }
        }
        
        return false
    }
    
    private fun recordSuccess(serviceName: String) {
        serviceHealthMap[serviceName] = ServiceHealth(0, 
            serviceHealthMap[serviceName]?.successes?.plus(1) ?: 1, 
            System.currentTimeMillis())
    }
    
    private fun recordFailure(serviceName: String) {
        cleanupOldServices()
        val current = serviceHealthMap[serviceName] ?: ServiceHealth(0, 0, 0L)
        serviceHealthMap[serviceName] = ServiceHealth(
            current.failures + 1,
            current.successes,
            System.currentTimeMillis()
        )
    }
    
    /**
     * 🧹 CLEANUP OLD SERVICE HEALTH RECORDS
     */
    private fun cleanupOldServices() {
        val now = System.currentTimeMillis()
        
        // Cleanup every hour and if map gets too large
        if (now - lastCleanup > 3600000 || serviceHealthMap.size > MAX_TRACKED_SERVICES) {
            val cutoffTime = now - RECOVERY_TIMEOUT_MS * 3 // Keep only recent services
            serviceHealthMap.entries.removeAll { (_, health) ->
                health.lastFailureTime < cutoffTime
            }
            lastCleanup = now
            Log.d(TAG, "Cleaned up service health map, size: ${serviceHealthMap.size}")
        }
    }
    
    /**
     * 📊 DATA CLASSES FOR ERROR HANDLING
     */
    
    sealed class NetworkResult<T> {
        data class Success<T>(val data: T) : NetworkResult<T>()
        data class Failed<T>(val exception: Exception) : NetworkResult<T>()
        data class FallbackUsed<T>(val data: T, val originalError: Exception) : NetworkResult<T>()
        data class NoNetwork<T>(val message: String) : NetworkResult<T>()
        data class CircuitBreakerOpen<T>(val message: String) : NetworkResult<T>()
    }
    
    data class HttpResponseAnalysis(
        val isSuccess: Boolean,
        val category: HttpErrorCategory,
        val userMessage: String?,
        val shouldRetry: Boolean
    )
    
    enum class NetworkErrorType {
        NO_NETWORK,
        CONNECTION_FAILED,
        TIMEOUT,
        AUTHENTICATION,
        AUTHORIZATION,
        RATE_LIMITED,
        SERVER_ERROR,
        HTTP_ERROR,
        IO_ERROR,
        UNKNOWN
    }
    
    enum class HttpErrorCategory {
        SUCCESS,
        CLIENT_ERROR,
        AUTHENTICATION,
        AUTHORIZATION,
        NOT_FOUND,
        RATE_LIMITED,
        SERVER_ERROR,
        UNKNOWN
    }
    
    private data class ServiceHealth(
        val failures: Int,
        val successes: Int,
        val lastFailureTime: Long
    )
}