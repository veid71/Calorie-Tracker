package com.calorietracker.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

object PerformanceOptimizer {

    private const val TAG = "PerformanceOptimizer"

    // Search debouncing
    private val searchHandlers = ConcurrentHashMap<String, Handler>()
    private val searchRunnables = ConcurrentHashMap<String, Runnable>()

    // Memory cache
    private val memoryCache = ConcurrentHashMap<String, CacheItem>()
    private const val CACHE_MAX_SIZE = 100
    private const val CACHE_TTL_MS = 5 * 60 * 1000L

    // Performance monitoring
    private val performanceMetrics = ConcurrentHashMap<String, PerformanceMetric>()

    fun debounceSearch(searchKey: String, delayMs: Long = 500, searchAction: () -> Unit) {
        searchRunnables[searchKey]?.let { runnable ->
            searchHandlers[searchKey]?.removeCallbacks(runnable)
        }
        val searchRunnable = Runnable {
            try {
                searchAction()
            } catch (e: Exception) {
                Log.w(TAG, "Debounced search error for key '$searchKey'", e)
            } finally {
                searchHandlers.remove(searchKey)
                searchRunnables.remove(searchKey)
            }
        }
        val handler = Handler(Looper.getMainLooper())
        searchHandlers[searchKey] = handler
        searchRunnables[searchKey] = searchRunnable
        handler.postDelayed(searchRunnable, delayMs)
    }

    fun <T> getCached(key: String): T? {
        val item = memoryCache[key] ?: return null
        if (System.currentTimeMillis() - item.timestamp > CACHE_TTL_MS) {
            memoryCache.remove(key)
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return item.data as? T
    }

    fun <T> setCached(key: String, data: T) {
        if (memoryCache.size >= CACHE_MAX_SIZE) cleanupOldCache()
        memoryCache[key] = CacheItem(data = data as Any, timestamp = System.currentTimeMillis())
    }

    fun clearCache(keyPrefix: String? = null) {
        if (keyPrefix == null) {
            memoryCache.clear()
        } else {
            memoryCache.keys.filter { it.startsWith(keyPrefix) }.forEach { memoryCache.remove(it) }
        }
    }

    /**
     * Runs a batched suspend operation using the provided scope.
     * Callers own the scope lifecycle — cancel it when done.
     */
    fun <T> batchOperation(
        items: List<T>,
        batchSize: Int = 50,
        scope: CoroutineScope,
        operation: suspend (List<T>) -> Unit
    ): Job {
        return scope.launch(Dispatchers.IO) {
            try {
                items.chunked(batchSize).forEach { batch ->
                    operation(batch)
                    delay(10)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Batch operation failed", e)
                throw e
            }
        }
    }

    fun scheduleBackgroundTask(
        priority: TaskPriority = TaskPriority.MEDIUM,
        lifecycleOwner: LifecycleOwner,
        task: suspend () -> Unit
    ) {
        val context = when (priority) {
            TaskPriority.HIGH -> Dispatchers.Main.immediate
            TaskPriority.MEDIUM -> Dispatchers.IO
            TaskPriority.LOW -> Dispatchers.IO.limitedParallelism(1)
        }
        lifecycleOwner.lifecycleScope.launch(context) {
            try {
                if (priority == TaskPriority.LOW) delay(100)
                task()
            } catch (e: Exception) {
                Log.e(TAG, "Background task failed (priority: $priority)", e)
            }
        }
    }

    fun <T> measurePerformance(operationName: String, operation: () -> T): T {
        var result: T
        val executionTime = measureTimeMillis { result = operation() }
        recordMetric(operationName, executionTime)
        if (executionTime > 1000) Log.w(TAG, "Slow operation: $operationName took ${executionTime}ms")
        return result
    }

    /** Suspend version — avoids runBlocking entirely. */
    suspend fun <T> measurePerformanceSuspend(operationName: String, operation: suspend () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = operation()
        val executionTime = System.currentTimeMillis() - startTime
        recordMetric(operationName, executionTime)
        if (executionTime > 1000) Log.w(TAG, "Slow async operation: $operationName took ${executionTime}ms")
        return result
    }

    private fun recordMetric(operationName: String, executionTime: Long) {
        val current = performanceMetrics[operationName]
        performanceMetrics[operationName] = if (current != null) {
            PerformanceMetric(
                totalExecutions = current.totalExecutions + 1,
                totalTime = current.totalTime + executionTime,
                averageTime = (current.totalTime + executionTime) / (current.totalExecutions + 1),
                lastExecutionTime = executionTime
            )
        } else {
            PerformanceMetric(1, executionTime, executionTime, executionTime)
        }
    }

    private fun cleanupOldCache() {
        val now = System.currentTimeMillis()
        memoryCache.entries.filter { now - it.value.timestamp > CACHE_TTL_MS }.forEach { memoryCache.remove(it.key) }
        if (memoryCache.size >= CACHE_MAX_SIZE) {
            memoryCache.entries.sortedBy { it.value.timestamp }
                .take(memoryCache.size - CACHE_MAX_SIZE + 10)
                .forEach { memoryCache.remove(it.key) }
        }
    }

    fun getPerformanceReport(): Map<String, PerformanceMetric> = performanceMetrics.toMap()

    fun clearPerformanceMetrics() = performanceMetrics.clear()

    fun cleanup() {
        searchHandlers.values.forEach { handler ->
            searchRunnables.values.forEach { runnable -> handler.removeCallbacks(runnable) }
        }
        searchHandlers.clear()
        searchRunnables.clear()
        memoryCache.clear()
        Log.d(TAG, "PerformanceOptimizer cleaned up")
    }

    enum class TaskPriority { HIGH, MEDIUM, LOW }

    private data class CacheItem(val data: Any, val timestamp: Long)

    data class PerformanceMetric(
        val totalExecutions: Long,
        val totalTime: Long,
        val averageTime: Long,
        val lastExecutionTime: Long
    )

    fun (() -> Unit).debounce(key: String, delayMs: Long = 500) = debounceSearch(key, delayMs, this)

    /** Safe suspend extension — no runBlocking needed. */
    suspend fun <T> (suspend () -> T).measureAsync(operationName: String): T =
        measurePerformanceSuspend(operationName, this)
}
