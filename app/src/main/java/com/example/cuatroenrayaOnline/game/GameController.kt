package com.example.cuatroenrayaOnline.game

class GameController {

    fun newBoard(): Array<Array<Char>> = Array(6) { Array(7) { '-' } }

    fun checkGameState(board: Array<Array<Char>>): GameStatus {
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

        var isBoardFull = true

        // Check all cells for winning lines and track if board is full
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val cell = board[i][j]
                if (cell == '-') {
                    isBoardFull = false
                    continue
                }
                if (
                    checkDirection(i, j, 0, 1, cell) ||  // Horizontal
                    checkDirection(i, j, 1, 0, cell) ||  // Vertical
                    checkDirection(i, j, 1, 1, cell) ||  // Diagonal down-right
                    checkDirection(i, j, 1, -1, cell)    // Diagonal down-left
                ) {
                    return if (cell == 'O') GameStatus.PLAYER1_WIN else GameStatus.PLAYER2_WIN
                }
            }
        }

        return if (isBoardFull) GameStatus.DRAW else GameStatus.NOT_FINISHED
    }
}
