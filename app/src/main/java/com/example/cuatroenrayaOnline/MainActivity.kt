package com.example.cuatroenrayaOnline

import android.R.id.message
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import com.example.cuatroenraya.R
import com.example.cuatroenrayaOnline.game.*
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var board: Array<Array<Char>>
    private lateinit var boardViews: Array<Array<ImageView>>
    private var gameOver = false
    private val gameController = GameController()
    private var isVsCPU = true
    private var isOnlineGame = false
    private var turnPlayer1 = true
    private var isMyTurn = false

    private var onlineManager: GameOnlineManager? = null
    private var waitingDialog: AlertDialog? = null // NUEVO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initBoardViews()
        findViewById<Button>(R.id.restartGame).setOnClickListener {
            chooseModeAndStart()
        }
        chooseModeAndStart()
    }

    private fun chooseModeAndStart() {
        AlertDialog.Builder(this)
            .setTitle("Select game mode")
            .setPositiveButton("Player vs CPU") { _, _ ->
                isVsCPU = true
                isOnlineGame = false
                startGame()
            }
            .setNegativeButton("Player vs Player") { _, _ ->
                isVsCPU = false
                isOnlineGame = false
                startGame()
            }
            .setNeutralButton("Player vs Online") { _, _ ->
                isOnlineGame = true
                isVsCPU = false
                setupOnlineGame()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupOnlineGame() {
        board = Array(6) { Array(7) { '-' } }
        gameOver = false

        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: UUID.randomUUID().toString()
        onlineManager = GameOnlineManager(userId)

        // Mostrar diálogo de espera
        waitingDialog = AlertDialog.Builder(this)
            .setTitle("Waiting for a player...")
            .setMessage("Please wait while anybody joins the game.")
            .setCancelable(false)
            .create()
        waitingDialog?.show()

        onlineManager?.connectToSession(
            onBoardUpdate = { updatedBoard, isMyTurnNow ->
                board = updatedBoard
                isMyTurn = isMyTurnNow

                if (waitingDialog?.isShowing == true) {
                    waitingDialog?.dismiss()
                    waitingDialog = null
                }

                runOnUiThread {
                    updateUIFromBoard()
                }
            }
            ,
            onGameEnd = { status ->
                gameOver = true
                runOnUiThread {
                    updateUIFromBoard()
                    showResult(status)
                }
            }

        )
    }

    private fun startGame() {
        board = gameController.newBoard()
        gameOver = false
        turnPlayer1 = true
        updateUIFromBoard()
    }

    private fun initBoardViews() {
        val layout = findViewById<LinearLayout>(R.id.boardLayout)
        boardViews = Array(6) { row ->
            val rowLayout = layout.getChildAt(row) as LinearLayout
            Array(7) { col ->
                (rowLayout.getChildAt(col) as ImageView).apply {
                    setOnClickListener { onCellClicked(col) }
                }
            }
        }
    }

    private fun onCellClicked(col: Int) {
        if (gameOver) return

        if (isOnlineGame) {
            if (!isMyTurn || gameOver) return

            onlineManager?.makeMove(col)

            // Esperar un momento para que el board se actualice (porque es asincrónico)
            Handler(Looper.getMainLooper()).postDelayed({
                if (!gameOver) {
                    val status = gameController.checkGameState(board)
                    if (status != GameStatus.NOT_FINISHED) {
                        gameOver = true
                        onlineManager?.sendGameEnd(status)
                        showResult(status)
                    }
                }
            }, 300)

            return
        }


        if (!makeMove(col, turnPlayer1)) return

        val state = gameController.checkGameState(board)
        if (state != GameStatus.NOT_FINISHED) {
            gameOver = true
            showResult(state)
            return
        }

        turnPlayer1 = !turnPlayer1

        if (isVsCPU && !turnPlayer1) {
            Handler(Looper.getMainLooper()).postDelayed({
                makeCpuMove()
                val cpuState = gameController.checkGameState(board)
                if (cpuState != GameStatus.NOT_FINISHED) {
                    gameOver = true
                    showResult(cpuState)
                } else {
                    turnPlayer1 = true
                }
            }, 500)
        }
    }

    private fun makeMove(col: Int, isPlayer1: Boolean): Boolean {
        for (row in 5 downTo 0) {
            if (board[row][col] == '-') {
                board[row][col] = if (isPlayer1) 'O' else 'X'
                boardViews[row][col].setImageResource(
                    if (isPlayer1) R.drawable.yellow else R.drawable.red
                )
                return true
            }
        }
        return false
    }

    private fun makeCpuMove() {
        for (col in 0 until 7) {
            if (board.any { it[col] == '-' }) {
                val temp = board.map { it.copyOf() }.toTypedArray()
                for (r in 5 downTo 0) if (temp[r][col] == '-') { temp[r][col] = 'X'; break }
                if (gameController.checkGameState(temp) == GameStatus.PLAYER2_WIN) {
                    makeMove(col, false)
                    return
                }
            }
        }
        for (col in 0 until 7) {
            if (board.any { it[col] == '-' }) {
                val temp = board.map { it.copyOf() }.toTypedArray()
                for (r in 5 downTo 0) if (temp[r][col] == '-') { temp[r][col] = 'O'; break }
                if (gameController.checkGameState(temp) == GameStatus.PLAYER1_WIN) {
                    makeMove(col, false)
                    return
                }
            }
        }
        val avail = (0 until 7).filter { c -> board.any { it[c] == '-' } }
        if (avail.isNotEmpty()) makeMove(avail.random(), false)
    }

    private fun updateUIFromBoard() {
        for (i in 0 until 6) {
            for (j in 0 until 7) {
                val cell = board[i][j]
                val resId = when (cell) {
                    'O' -> R.drawable.yellow
                    'X' -> R.drawable.red
                    else -> R.drawable.blue
                }
                boardViews[i][j].setImageResource(resId)
            }
        }
    }

    private fun showResult(state: GameStatus) {
        val message = when (state) {
            GameStatus.PLAYER1_WIN -> "Player 1 wins!"
            GameStatus.PLAYER2_WIN -> if (isVsCPU) "Machine wins!" else "Player 2 wins!"
            GameStatus.DRAW -> "Draw!"
            else -> ""
        }
        AlertDialog.Builder(this)
            .setTitle(message)
            .setPositiveButton("Menu") { _, _ -> chooseModeAndStart() }
            .setNegativeButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
