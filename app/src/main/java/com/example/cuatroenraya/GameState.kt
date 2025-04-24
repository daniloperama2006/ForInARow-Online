// GameState.kt
package com.example.cuatroenraya

/**
 * Enumeration of the possible states of the game.
 */
enum class GameState {
    PLAYER1_WIN,    // Player 1 (O) connects four
    PLAYER2_WIN,    // Player 2 or CPU (X) connects four
    DRAW,           // Draw: board full with no winner
    NOT_FINISHED    // Game in progress
}