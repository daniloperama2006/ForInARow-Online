// GameState.kt
package com.example.cuatroenraya

/**
 * Enumeración de los posibles estados de la partida.
 */
enum class GameState {
    PLAYER1_WIN,    // Jugador 1 (O) conecta cuatro
    PLAYER2_WIN,    // Jugador 2 o CPU (X) conecta cuatro
    DRAW,           // Empate: tablero lleno sin ganador
    NOT_FINISHED    // Partida en curso
}