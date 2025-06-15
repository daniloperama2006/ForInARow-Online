package com.example.cuatroenrayaOnline.game

data class GameState(
    val board: List<List<Int>> = List(6) { List(7) { 0 } },
    val currentPlayer: String = "player1",
    val players: Map<String, Player> = mapOf(),
    val gameState: String = "waiting",
    val winner: String = ""
)

data class Player(val uid: String = "")




