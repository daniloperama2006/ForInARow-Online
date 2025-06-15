package com.example.cuatroenrayaOnline.game

data class VocabularyQuestion(
    val word: String,
    val answers: List<String>,             // Varias respuestas posibles
    var answeredCorrectly: Boolean = false 
)

