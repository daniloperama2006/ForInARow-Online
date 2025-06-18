package com.example.cuatroenrayaOnline.game

// Represents the full game state stored in Firebase
data class GameState(
    // 6x7 board, 0 = empty, 1 = player1, 2 = player2
    val board: List<List<Int>> = List(6) { List(7) { 0 } },

    // Current player's turn: "player1" or "player2"
    val currentPlayer: String = "player1",

    // Map of players in the game (player1 and player2)
    val players: Map<String, Player> = mapOf(),

    // Game status: "waiting", "playing", or "finished"
    val gameState: String = "waiting",

    // Winner status: "PLAYER1_WIN", "PLAYER2_WIN", "DRAW", etc.
    val winner: String = ""
)

// Represents a player with their Firebase UID
data class Player(val uid: String = "")
