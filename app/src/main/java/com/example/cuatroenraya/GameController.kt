package com.example.cuatroenraya

import android.app.GameState
import androidx.compose.ui.res.booleanResource

class GameController {
    private lateinit var board: Array<Array<Char>>

    // here it's created the new board size 6 rows 7 colums
    // the character - means that the board it's empty
    fun newBoard(): Array<Array<Char>> {
        board = Array(6, { Array(7, { '-' }) })
        return board
    }

    enum class GameState {
        PLAYER1_WIN,
        PLAYER2_WIN,
        DRAW,
        NOT_FINISHED
    }


    fun checkGameState(board: Array<Array<Char>>): GameState {
        val rows = board.size
        val cols = board[0].size

        fun checkDirection(r: Int, c: Int, dr: Int, dc: Int, symbol: Char): Boolean {
            for (k in 0..3) {
                val nr = r + k * dr
                val nc = c + k * dc
                if (nr !in 0 until rows || nc !in 0 until cols || board[nr][nc] != symbol) return false
            }
            return true
        }

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val cell = board[i][j]
                if (cell == '-') continue

                if (
                    checkDirection(i, j, 0, 1, cell) ||  // Horizontal →
                    checkDirection(i, j, 1, 0, cell) ||  // Vertical ↓
                    checkDirection(i, j, 1, 1, cell) ||  // Diagonal ↘
                    checkDirection(i, j, 1, -1, cell)    // Diagonal ↙
                ) {
                    return if (cell == 'O') GameState.PLAYER1_WIN else GameState.PLAYER2_WIN
                }
            }
        }

        // ¿Hay algún espacio vacío?
        for (row in board) {
            if (row.contains('-')) return GameState.NOT_FINISHED
        }

        if (checkFullBoard()){
            return GameState.DRAW
        }

        return GameState.NOT_FINISHED
    }

    fun checkFullBoard(): Boolean{
        for (i in board.indices){
            for(j in board.indices){

                if(board[i][j]=='-'){
                    return false
                }

            }
        }
        return true
    }


}