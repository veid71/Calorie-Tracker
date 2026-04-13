package com.calorietracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.calorietracker.database.CalorieDatabase
import com.calorietracker.database.CalorieEntry
import com.calorietracker.repository.CalorieRepository
import com.calorietracker.utils.VoiceParser
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for voice-based food logging
 * Allows users to add food entries using speech recognition
 */
class VoiceInputActivity : AppCompatActivity(), RecognitionListener {

    private lateinit var repository: CalorieRepository
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    // UI elements
    private lateinit var tvStatus: TextView
    private lateinit var tvRecognizedText: TextView
    private lateinit var tvParsedInfo: TextView
    private lateinit var btnStartListening: MaterialButton
    private lateinit var btnStopListening: MaterialButton
    private lateinit var btnSaveEntry: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var cardResults: MaterialCardView

    private var isListening = false
    private var parsedEntry: CalorieEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_input)

        repository = CalorieRepository(CalorieDatabase.getDatabase(this), this)

        initViews()
        setupSpeechRecognizer()
        setupClickListeners()
        setupPermissionLauncher()

        // Set up action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Voice Food Logging"
        }
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvRecognizedText = findViewById(R.id.tvRecognizedText)
        tvParsedInfo = findViewById(R.id.tvParsedInfo)
        btnStartListening = findViewById(R.id.btnStartListening)
        btnStopListening = findViewById(R.id.btnStopListening)
        btnSaveEntry = findViewById(R.id.btnSaveEntry)
        progressIndicator = findViewById(R.id.progressIndicator)
        cardResults = findViewById(R.id.cardResults)

        // Initial UI state
        updateUI(VoiceState.READY)
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(this)

            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000)
            }
        } else {
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupClickListeners() {
        btnStartListening.setOnClickListener {
            requestMicrophonePermissionAndStart()
        }

        btnStopListening.setOnClickListener {
            stopListening()
        }

        btnSaveEntry.setOnClickListener {
            saveEntry()
        }
    }

    private fun setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startListening()
            } else {
                Toast.makeText(this, "Microphone permission is required for voice input", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestMicrophonePermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startListening()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startListening() {
        if (isListening) return

        isListening = true
        updateUI(VoiceState.LISTENING)
        speechRecognizer.startListening(recognizerIntent)
    }

    private fun stopListening() {
        if (!isListening) return

        isListening = false
        speechRecognizer.stopListening()
        updateUI(VoiceState.PROCESSING)
    }

    private fun saveEntry() {
        parsedEntry?.let { entry ->
            lifecycleScope.launch {
                try {
                    repository.addCalorieEntry(entry)
                    Toast.makeText(this@VoiceInputActivity, "Food entry saved!", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@VoiceInputActivity, "Failed to save entry: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI(state: VoiceState) {
        when (state) {
            VoiceState.READY -> {
                tvStatus.text = "Ready to listen"
                btnStartListening.isEnabled = true
                btnStopListening.isEnabled = false
                btnSaveEntry.isEnabled = false
                progressIndicator.hide()
                cardResults.visibility = android.view.View.GONE
            }
            VoiceState.LISTENING -> {
                tvStatus.text = "🎤 Listening... Say something like:\n\"I ate 2 slices of pizza\" or \"I had a medium apple\""
                btnStartListening.isEnabled = false
                btnStopListening.isEnabled = true
                btnSaveEntry.isEnabled = false
                progressIndicator.show()
                cardResults.visibility = android.view.View.GONE
            }
            VoiceState.PROCESSING -> {
                tvStatus.text = "Processing your input..."
                btnStartListening.isEnabled = false
                btnStopListening.isEnabled = false
                btnSaveEntry.isEnabled = false
                progressIndicator.show()
            }
            VoiceState.RESULTS -> {
                tvStatus.text = "Review your food entry:"
                btnStartListening.isEnabled = true
                btnStopListening.isEnabled = false
                btnSaveEntry.isEnabled = true
                progressIndicator.hide()
                cardResults.visibility = android.view.View.VISIBLE
            }
            VoiceState.ERROR -> {
                tvStatus.text = "Speech recognition failed. Try again."
                btnStartListening.isEnabled = true
                btnStopListening.isEnabled = false
                btnSaveEntry.isEnabled = false
                progressIndicator.hide()
                cardResults.visibility = android.view.View.GONE
            }
        }
    }

    // Speech Recognition Listener Methods
    override fun onReadyForSpeech(params: Bundle?) {
        updateUI(VoiceState.LISTENING)
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        updateUI(VoiceState.PROCESSING)
    }

    override fun onError(error: Int) {
        isListening = false
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech input matched"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Recognition error"
        }
        
        tvStatus.text = errorMessage
        updateUI(VoiceState.ERROR)
    }

    override fun onResults(results: Bundle?) {
        isListening = false
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
            if (matches.isNotEmpty()) {
                val recognizedText = matches[0]
                tvRecognizedText.text = "You said: \"$recognizedText\""
                
                // Parse the recognized text into food entry
                parseVoiceInput(recognizedText)
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
            if (matches.isNotEmpty()) {
                tvRecognizedText.text = "Hearing: \"${matches[0]}...\""
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun parseVoiceInput(recognizedText: String) {
        lifecycleScope.launch {
            try {
                val voiceParser = VoiceParser()
                val parsedData = voiceParser.parseVoiceInput(recognizedText)
                
                if (parsedData != null && parsedData.isValid()) {
                    // Create calorie entry from parsed data
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    parsedEntry = CalorieEntry(
                        foodName = parsedData.foodName,
                        calories = parsedData.estimatedCalories,
                        date = today,
                        protein = parsedData.estimatedProtein,
                        carbs = parsedData.estimatedCarbs,
                        fat = parsedData.estimatedFat
                    )
                    
                    // Display parsed information
                    tvParsedInfo.text = buildString {
                        append("Food: ${parsedData.foodName}\n")
                        append("Quantity: ${parsedData.quantity} ${parsedData.unit}\n")
                        append("Estimated Calories: ${parsedData.estimatedCalories}\n")
                        if (parsedData.estimatedProtein > 0) append("Protein: ${parsedData.estimatedProtein}g\n")
                        if (parsedData.estimatedCarbs > 0) append("Carbs: ${parsedData.estimatedCarbs}g\n")
                        if (parsedData.estimatedFat > 0) append("Fat: ${parsedData.estimatedFat}g\n")
                        append("\nConfidence: ${(parsedData.confidence * 100).toInt()}%")
                    }
                    
                    updateUI(VoiceState.RESULTS)
                } else {
                    tvParsedInfo.text = "Could not understand food information. Please try again with phrases like:\n" +
                            "• \"I ate 2 slices of pizza\"\n" +
                            "• \"I had a medium apple\"\n" +
                            "• \"I drank a cup of coffee with milk\""
                    updateUI(VoiceState.ERROR)
                }
            } catch (e: Exception) {
                tvParsedInfo.text = "Error parsing voice input: ${e.message}"
                updateUI(VoiceState.ERROR)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    enum class VoiceState {
        READY, LISTENING, PROCESSING, RESULTS, ERROR
    }
}