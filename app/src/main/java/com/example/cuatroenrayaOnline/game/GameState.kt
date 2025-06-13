package com.example.cuatroenrayaOnline.game

data class GameState(
    val board: List<List<Int>> = List(6) { List(7) { 0 } },
    val currentPlayer: String = "player1",
    val players: Map<String, Player> = mapOf(),
    val vocabularyQuestion: VocabularyQuestion = VocabularyQuestion(),
    val gameState: String = "waiting", // waiting, playing, finished
    val winner: String = ""
)

data class Player(val uid: String = "")

data class VocabularyQuestion(
    val word: String = "",
    val correctAnswer: String = "",
    val answeredCorrectly: Boolean = false
)


