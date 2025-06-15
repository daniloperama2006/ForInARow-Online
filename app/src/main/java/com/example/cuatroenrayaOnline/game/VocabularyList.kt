package com.example.cuatroenrayaOnline.game

data class VocabularyQuestion(
    val word: String = "",
    val correctAnswer: String = "",
    var answeredCorrectly: Boolean = false
)
