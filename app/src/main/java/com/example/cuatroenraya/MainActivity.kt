package com.example.cuatroenraya

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.cuatroenraya.ui.theme.CuatroEnRayaTheme
import com.example.cuatroenraya.GameController

class MainActivity : ComponentActivity() {

    private lateinit var board: Array<Array<Char>>
    private var gameOver = false
    private var turnPlayer1 = true
    private val GameController = GameController()


    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startGame()
        startListeners()
    }


    private fun startListeners() {

        val layoutBoard = findViewById<LinearLayout>(R.id.boardLayout)

        for(i in 0 until layoutBoard.childCount){
            val row = layoutBoard.getChildAt(i) as LinearLayout

            for(j in 0 until row.childCount){
                val cell = row.getChildAt(j) as ImageView

                cell.setOnClickListener {
                    // Buscar la fila más baja disponible en la columna j
                    for (rowIndex in board.lastIndex downTo 0) {
                        if (board[rowIndex][j] == '-' && !gameOver) {
                            val rowLayout = layoutBoard.getChildAt(rowIndex) as LinearLayout
                            val targetCell = rowLayout.getChildAt(j) as ImageView

                            val gameState1 = GameController.checkGameState(board)

                            setCard(targetCell, rowIndex, j)

                            // Verificar estado del juego después de cada movimiento
                            val gameState = GameController.checkGameState(board)

                            if (gameState == GameState.PLAYER1_WIN) {
                                gameOver = true
                                showDialog("Player 1 wins")
                            } else if (gameState == GameState.PLAYER2_WIN) {
                                gameOver = true
                                showDialog("Player 2 wins")
                            } else if (gameState == GameState.DRAW) {
                                gameOver = true
                                showDialog("Draw")
                            } else {
                                turnPlayer1 = !turnPlayer1
                            }


                            break
                        }
                    }
                }

            }
        }

    }

    private fun startGame() {
        board = GameController.newBoard()
        gameOver = false
        turnPlayer1 = true


    }

    private fun setCard(view: ImageView, positionRow: Int, positionColumn: Int){

        if (turnPlayer1){
            board[positionRow][positionColumn] = 'O'
            view.setImageDrawable(getDrawable(R.drawable.yellow))
        }else{
            board[positionRow][positionColumn] = 'X'
            view.setImageDrawable(getDrawable(R.drawable.red))
        }

    }

    private fun showDialog(message: String){
        AlertDialog.Builder(this)
            .setTitle(message)
            .setPositiveButton("Play Again", {dialog, which -> startGame()})
            .setNegativeButton("Cancel", {dialog, which -> dialog.dismiss()})
            .show()
    }


}

