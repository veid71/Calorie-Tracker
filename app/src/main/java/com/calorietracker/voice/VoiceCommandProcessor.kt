package com.calorietracker.voice

// 🧰 VOICE PROCESSING TOOLS
import android.content.Context          // Android system access
import android.content.Intent          // For launching speech recognition
import android.speech.RecognizerIntent  // Speech-to-text functionality
import android.speech.SpeechRecognizer  // Speech recognition service
import android.speech.RecognitionListener // Listen for speech results
import android.os.Bundle               // Data containers
import kotlinx.coroutines.suspendCancellableCoroutine // Async speech processing
import kotlin.coroutines.resume        // Return results from coroutines
import java.util.regex.Pattern         // Text pattern matching
import kotlin.coroutines.resumeWithException // Handle errors in coroutines

/**
 * 🎤 VOICE COMMAND PROCESSOR - TALK TO YOUR FOOD DIARY
 * 
 * Hey young programmer! This lets users log food just by talking to their phone.
 * 
 * 🗣️ What can users say?
 * - "I just ate a banana" → logs banana with default calories
 * - "I had two slices of pizza" → logs 2 servings of pizza
 * - "I drank a large coffee with cream" → logs large coffee with modifications
 * - "Log my breakfast: oatmeal and berries" → creates breakfast entry
 * 
 * 🧠 How does speech recognition work?
 * 1. User presses microphone button
 * 2. Android listens and converts speech to text
 * 3. Our app analyzes the text for food keywords
 * 4. We extract food name, quantity, and meal type
 * 5. Look up nutrition info and create food entry
 * 
 * 🎯 Smart Features:
 * - Recognizes quantities: "two", "half", "three cups"
 * - Understands meal types: "for breakfast", "lunch", "dinner"
 * - Handles common food descriptions: "large", "small", "with cheese"
 * - Provides suggestions when unclear: "Did you mean 'apple' or 'apple pie'?"
 * 
 * 🔍 Text Analysis Process:
 * 1. Clean up speech text (remove filler words like "um", "uh")
 * 2. Extract quantity words (one, two, half, double, etc.)
 * 3. Find food keywords (apple, pizza, coffee, etc.)
 * 4. Identify meal type context (breakfast, lunch, dinner, snack)
 * 5. Search food databases for matches
 * 6. Create food entry with extracted information
 */
class VoiceCommandProcessor(private val context: Context) {
    
    // 🎤 SPEECH RECOGNITION COMPONENTS
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionListener: RecognitionListener? = null
    
    // 🔢 QUANTITY PATTERNS - Recognize numbers and amounts in speech
    private val quantityPatterns = mapOf(
        "half|one half" to 0.5,
        "one|a|an|single" to 1.0,
        "two|double|twice" to 2.0,
        "three|triple" to 3.0,
        "four" to 4.0,
        "five" to 5.0,
        "small" to 0.75,      // Small serving = 75% of normal
        "medium|regular|normal" to 1.0,  // Medium = normal serving
        "large|big" to 1.5     // Large serving = 150% of normal
    )
    
    // 🍽️ MEAL TYPE PATTERNS - Recognize when user specifies meal type
    private val mealTypePatterns = mapOf(
        "breakfast|morning" to "breakfast",
        "lunch|noon|midday" to "lunch", 
        "dinner|evening|supper" to "dinner",
        "snack|snacking" to "snack"
    )
    
    // 🚫 FILLER WORDS TO REMOVE - Clean up speech recognition text
    private val fillerWords = setOf(
        "um", "uh", "like", "you know", "i mean", "basically", "actually",
        "just", "really", "pretty", "quite", "very", "so", "well"
    )
    
    /**
     * 🎤 START VOICE RECOGNITION
     * 
     * Begin listening for user's voice input.
     * Returns the recognized text when speech is complete.
     */
    suspend fun startVoiceRecognition(): String {
        return suspendCancellableCoroutine { continuation ->
            
            // 🔧 SET UP SPEECH RECOGNIZER
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            
            recognitionListener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    // 🎤 Ready to listen
                }
                
                override fun onBeginningOfSpeech() {
                    // 🗣️ User started speaking
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // 📊 Voice volume level changed (for visual feedback)
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {
                    // 🔊 Audio data received
                }
                
                override fun onEndOfSpeech() {
                    // 🤐 User stopped speaking
                }
                
                override fun onError(error: Int) {
                    // ❌ Speech recognition failed
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech input was matched"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                        SpeechRecognizer.ERROR_SERVER -> "Error from server"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error"
                    }
                    continuation.resumeWithException(Exception("Voice recognition error: $errorMessage"))
                }
                
                override fun onResults(results: Bundle?) {
                    // ✅ SPEECH RECOGNITION SUCCESSFUL
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && matches.isNotEmpty()) {
                        continuation.resume(matches[0]) // Return the best match
                    } else {
                        continuation.resumeWithException(Exception("No speech results"))
                    }
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    // 📝 Partial results (user still speaking)
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {
                    // 🎯 Other speech events
                }
            }
            
            speechRecognizer?.setRecognitionListener(recognitionListener)
            
            // 🚀 START LISTENING
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say what you ate... (e.g., 'I had a banana')")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            
            speechRecognizer?.startListening(intent)
            
            // 🧹 CLEANUP WHEN CANCELLED
            continuation.invokeOnCancellation {
                speechRecognizer?.destroy()
                speechRecognizer = null
                recognitionListener = null
            }
        }
    }
    
    /**
     * 🧠 PARSE VOICE COMMAND INTO FOOD ENTRY DATA
     * 
     * Analyze the speech text and extract food information.
     * 
     * @param speechText What the user said (like "I ate two slices of pizza")
     * @return Parsed food information ready for database entry
     */
    fun parseVoiceCommand(speechText: String): VoiceParsedFood {
        // 🧹 CLEAN UP THE TEXT
        val cleanedText = cleanSpeechText(speechText)
        
        // 🔢 EXTRACT QUANTITY
        val quantity = extractQuantity(cleanedText)
        
        // 🍽️ EXTRACT MEAL TYPE
        val mealType = extractMealType(cleanedText)
        
        // 🍎 EXTRACT FOOD NAME
        val foodName = extractFoodName(cleanedText, quantity)
        
        return VoiceParsedFood(
            foodName = foodName,
            quantity = quantity,
            mealType = mealType,
            originalText = speechText,
            confidence = calculateConfidence(foodName, quantity, mealType)
        )
    }
    
    /**
     * 🧹 CLEAN SPEECH TEXT
     * 
     * Remove filler words and normalize the text for better parsing.
     */
    private fun cleanSpeechText(text: String): String {
        return text.lowercase()
            .split(" ")
            .filter { word -> word !in fillerWords && word.isNotBlank() }
            .joinToString(" ")
    }
    
    /**
     * 🔢 EXTRACT QUANTITY FROM SPEECH
     * 
     * Find quantity words in the speech and convert to numbers.
     */
    private fun extractQuantity(text: String): Double {
        for ((pattern, value) in quantityPatterns) {
            if (Pattern.compile("\\b($pattern)\\b").matcher(text).find()) {
                return value
            }
        }
        return 1.0 // Default to single serving
    }
    
    /**
     * 🍽️ EXTRACT MEAL TYPE FROM SPEECH
     * 
     * Determine if user specified breakfast, lunch, dinner, or snack.
     */
    private fun extractMealType(text: String): String? {
        for ((pattern, mealType) in mealTypePatterns) {
            if (Pattern.compile("\\b($pattern)\\b").matcher(text).find()) {
                return mealType
            }
        }
        return null // Let app auto-determine based on time of day
    }
    
    /**
     * 🍎 EXTRACT FOOD NAME FROM SPEECH
     * 
     * Find the actual food name after removing quantity and meal type words.
     */
    private fun extractFoodName(text: String, quantity: Double): String {
        // 🧹 REMOVE QUANTITY AND MEAL TYPE WORDS
        var foodText = text
        
        // Remove quantity patterns
        quantityPatterns.keys.forEach { pattern ->
            foodText = foodText.replace(Regex("\\b($pattern)\\b"), "")
        }
        
        // Remove meal type patterns  
        mealTypePatterns.keys.forEach { pattern ->
            foodText = foodText.replace(Regex("\\b($pattern)\\b"), "")
        }
        
        // Remove common speech prefixes
        val speechPrefixes = listOf("i ate", "i had", "i drank", "i consumed", "for", "of", "a", "an", "the")
        speechPrefixes.forEach { prefix ->
            foodText = foodText.replace(Regex("\\b$prefix\\b"), "")
        }
        
        // 🍎 CLEAN UP AND RETURN FOOD NAME
        return foodText.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
    
    /**
     * 🎯 CALCULATE PARSING CONFIDENCE
     * 
     * How confident are we that we parsed the speech correctly?
     */
    private fun calculateConfidence(foodName: String, quantity: Double, mealType: String?): Float {
        var confidence = 0f
        
        // 🍎 FOOD NAME CONFIDENCE
        if (foodName.isNotBlank() && foodName.length >= 3) {
            confidence += 0.6f // 60% base confidence for reasonable food name
        }
        
        // 🔢 QUANTITY CONFIDENCE  
        if (quantity != 1.0) {
            confidence += 0.2f // 20% bonus if we detected specific quantity
        }
        
        // 🍽️ MEAL TYPE CONFIDENCE
        if (mealType != null) {
            confidence += 0.2f // 20% bonus if we detected meal type
        }
        
        return confidence.coerceIn(0f, 1f) // Keep between 0% and 100%
    }
    
    /**
     * 🧹 CLEANUP RESOURCES
     * 
     * Clean up speech recognizer when done to prevent memory leaks.
     */
    fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        recognitionListener = null
    }
}

/**
 * 📊 VOICE PARSED FOOD - RESULT OF VOICE ANALYSIS
 * 
 * Container for information extracted from user's speech.
 * 
 * @property foodName     🍎 Extracted food name
 * @property quantity     🔢 How much user ate (servings)
 * @property mealType     🍽️ Breakfast/lunch/dinner/snack (or null if not specified)
 * @property originalText 🗣️ Original speech text for reference
 * @property confidence   🎯 How confident we are in our parsing (0.0 to 1.0)
 */
data class VoiceParsedFood(
    val foodName: String,        // 🍎 "Banana", "Pizza", "Coffee"
    val quantity: Double,        // 🔢 1.0, 2.0, 0.5, etc.
    val mealType: String?,       // 🍽️ "breakfast", "lunch", "dinner", "snack", or null
    val originalText: String,    // 🗣️ "I just ate two bananas for breakfast"
    val confidence: Float        // 🎯 0.8 = 80% confident in parsing accuracy
)