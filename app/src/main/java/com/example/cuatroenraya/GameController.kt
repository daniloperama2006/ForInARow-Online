// GameController.kt
package com.example.cuatroenraya

/**
 * GameController contains the pure logic of the game:
 * - creation of an empty board
 * - state verification: victory in a line of 4 or a draw
 */
class GameController {
    /**
     * Generates a new 6 rows x 7 columns board filled with '-' (empty).
     * @return A 2D array of characters representing the empty game board.
     */
    fun newBoard(): Array<Array<Char>> = Array(6) { Array(7) { '-' } }

    /**
     * Checks the state of the board:
     * - If there are 4 identical symbols ('O' or 'X') in a horizontal, vertical, or diagonal line, returns victory.
     * - If there are no empty spaces left, returns a draw.
     * - Otherwise, returns not finished.
     * @param board The current state of the game board as a 2D array of characters.
     * @return [GameState] indicating the current state of the game.
     */
    fun checkGameState(board: Array<Array<Char>>): GameState {
        val rows = board.size
        val cols = board[0].size

        // Checks for 4 in a line starting from (r, c) in the direction (dr, dc)
        fun checkDirection(r: Int, c: Int, dr: Int, dc: Int, symbol: Char): Boolean {
            for (k in 0..3) {
                val nr = r + dr * k
                val nc = c + dc * k
                if (nr !in 0 until rows || nc !in 0 until cols || board[nr][nc] != symbol) return false
            }
            return true
        }

        // Draw: no empty cells left on the board
        if (board.all { row -> row.none { it == '-' } }) return GameState.DRAW

        // Iterate through each non-empty cell and check for possible 4 in a line
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val cell = board[i][j]
                if (cell == '-') continue // Skip empty cells
                // Check in all four directions: horizontal, vertical, and both diagonals
                if (
                    checkDirection(i, j, 0, 1, cell) ||  // Horizontal
                    checkDirection(i, j, 1, 0, cell) ||  // Vertical
                    checkDirection(i, j, 1, 1, cell) ||  // Diagonal up-right
                    checkDirection(i, j, 1, -1, cell)    // Diagonal up-left
                ) {
                    return if (cell == 'O') GameState.PLAYER1_WIN else GameState.PLAYER2_WIN
                }
            }
        }

        // If no winner and not a draw, the game is not finished
        return GameState.NOT_FINISHED
    }
}