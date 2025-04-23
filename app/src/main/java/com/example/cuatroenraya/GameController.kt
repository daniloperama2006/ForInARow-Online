
// GameController.kt
package com.example.cuatroenraya

/**
 * GameController contiene la lógica pura del juego:
 * - creación de un tablero vacío
 * - verificación de estado: victoria en línea de 4 o empate
 */
class GameController {
    /**
     * Genera un tablero nuevo de 6 filas x 7 columnas lleno de '-' (vacío).
     */
    fun newBoard(): Array<Array<Char>> = Array(6) { Array(7) { '-' } }

    /**
     * Revisa el estado del tablero:
     * - Si hay 4 iguales ('O' o 'X') en línea horiz., vert. o diag., devuelve victoria.
     * - Si no hay espacios vacíos, devuelve empate.
     * - En otro caso, no finalizado.
     * @return GameState con el resultado.
     */
    fun checkGameState(board: Array<Array<Char>>): GameState {
        val rows = board.size
        val cols = board[0].size

        // Chequea 4 en línea desde (r,c) en dirección (dr,dc)
        fun checkDirection(r: Int, c: Int, dr: Int, dc: Int, symbol: Char): Boolean {
            for (k in 0..3) {
                val nr = r + dr * k
                val nc = c + dc * k
                if (nr !in 0 until rows || nc !in 0 until cols || board[nr][nc] != symbol) return false
            }
            return true
        }

        // Recorrer cada celda no vacía y comprobar posibles 4 en línea
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val cell = board[i][j]
                if (cell == '-') continue
                if (
                    checkDirection(i, j, 0, 1, cell) ||
                    checkDirection(i, j, 1, 0, cell) ||
                    checkDirection(i, j, 1, 1, cell) ||
                    checkDirection(i, j, 1, -1, cell)
                ) {
                    return if (cell == 'O') GameState.PLAYER1_WIN else GameState.PLAYER2_WIN
                }
            }
        }
        // Empate: sin celdas vacías
        if (board.all { row -> row.none { it == '-' } }) return GameState.DRAW
        // Si no ganó ni empató, continúa
        return GameState.NOT_FINISHED
    }
}