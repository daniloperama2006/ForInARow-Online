package com.example.cuatroenrayaOnline.game

// Represents a vocabulary question for the player
data class VocabularyQuestion(
    val word: String, // The word to translate
    val answers: List<String>, // List of accepted answers
    var answeredCorrectly: Boolean = false // True if answered correctly
)
