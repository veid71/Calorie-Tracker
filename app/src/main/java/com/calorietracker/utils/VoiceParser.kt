package com.calorietracker.utils

import kotlin.math.roundToInt

/**
 * Simple voice input parser for food entries
 */
class VoiceParser {
    
    data class ParsedFood(
        val foodName: String,
        val quantity: String = "1",
        val unit: String = "",
        val calories: Int = 100,
        val estimatedCalories: Int = 100,
        val protein: Double = 0.0,
        val estimatedProtein: Double = 0.0,
        val carbs: Double = 0.0,
        val estimatedCarbs: Double = 0.0,
        val fat: Double = 0.0,
        val estimatedFat: Double = 0.0,
        val confidence: Double = 0.8
    ) {
        fun isValid(): Boolean = foodName.isNotBlank() && calories > 0
    }
    
    fun parseVoiceInput(input: String): ParsedFood? {
        val lowerInput = input.lowercase().trim()
        
        // Simple patterns to match
        val patterns = listOf(
            "i ate (.+)" to { match: MatchResult -> extractFood(match.groupValues[1]) },
            "i had (.+)" to { match: MatchResult -> extractFood(match.groupValues[1]) },
            "(.+)" to { match: MatchResult -> extractFood(match.groupValues[1]) }
        )
        
        for ((pattern, extractor) in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(lowerInput)
            if (match != null) {
                return extractor(match)
            }
        }
        
        return null
    }
    
    private fun extractFood(foodText: String): ParsedFood {
        val text = foodText.trim()
        
        // Extract quantity and food name
        val quantityRegex = Regex("(\\d+(?:\\.\\d+)?|a|an|one|two|three|half)\\s*(\\w+)?\\s+(.+)")
        val match = quantityRegex.find(text)
        
        return if (match != null) {
            val quantityStr = match.groupValues[1]
            val unit = match.groupValues[2]
            val foodName = match.groupValues[3]
            
            val quantity = parseQuantity(quantityStr)
            val baseCalories = estimateCalories(foodName)
            
            ParsedFood(
                foodName = foodName.trim(),
                quantity = if (unit.isNotBlank()) "$quantityStr $unit" else quantityStr,
                unit = unit,
                calories = (baseCalories * quantity).roundToInt(),
                estimatedCalories = (baseCalories * quantity).roundToInt(),
                protein = estimateProtein(foodName) * quantity,
                estimatedProtein = estimateProtein(foodName) * quantity,
                carbs = estimateCarbs(foodName) * quantity,
                estimatedCarbs = estimateCarbs(foodName) * quantity,
                fat = estimateFat(foodName) * quantity,
                estimatedFat = estimateFat(foodName) * quantity,
                confidence = 0.7
            )
        } else {
            ParsedFood(
                foodName = text,
                quantity = "1",
                unit = "",
                calories = estimateCalories(text),
                estimatedCalories = estimateCalories(text),
                protein = estimateProtein(text),
                estimatedProtein = estimateProtein(text),
                carbs = estimateCarbs(text),
                estimatedCarbs = estimateCarbs(text),
                fat = estimateFat(text),
                estimatedFat = estimateFat(text),
                confidence = 0.5
            )
        }
    }
    
    private fun parseQuantity(quantityStr: String): Double {
        return when (quantityStr.lowercase()) {
            "a", "an", "one" -> 1.0
            "two" -> 2.0
            "three" -> 3.0
            "half" -> 0.5
            else -> quantityStr.toDoubleOrNull() ?: 1.0
        }
    }
    
    private fun estimateCalories(foodName: String): Int {
        val lower = foodName.lowercase()
        return when {
            "pizza" in lower -> 280
            "apple" in lower -> 80
            "banana" in lower -> 105
            "sandwich" in lower -> 350
            "burger" in lower -> 540
            "salad" in lower -> 150
            "chicken" in lower -> 200
            "rice" in lower -> 150
            "pasta" in lower -> 200
            else -> 100
        }
    }
    
    private fun estimateProtein(foodName: String): Double {
        val lower = foodName.lowercase()
        return when {
            "chicken" in lower -> 25.0
            "burger" in lower -> 20.0
            "sandwich" in lower -> 15.0
            else -> 5.0
        }
    }
    
    private fun estimateCarbs(foodName: String): Double {
        val lower = foodName.lowercase()
        return when {
            "rice" in lower -> 30.0
            "pasta" in lower -> 40.0
            "pizza" in lower -> 35.0
            "apple" in lower -> 20.0
            else -> 10.0
        }
    }
    
    private fun estimateFat(foodName: String): Double {
        val lower = foodName.lowercase()
        return when {
            "burger" in lower -> 25.0
            "pizza" in lower -> 12.0
            "chicken" in lower -> 8.0
            else -> 3.0
        }
    }
}