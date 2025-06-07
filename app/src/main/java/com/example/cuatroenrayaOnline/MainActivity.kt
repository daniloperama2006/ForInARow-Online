package com.example.cuatroenrayaOnline

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import com.example.cuatroenrayaOnline.game.*

class MainActivity : ComponentActivity() {
    private lateinit var board: Array<Array<Char>>
    private lateinit var boardViews: Array<Array<ImageView>>
    private var gameOver = false
    private val gameController = GameController()
    private var isVsCPU = true
    private var isOnlineGame = false
    private var turnPlayer1 = true

    private var onlineManager: GameOnlineManager? = null
    private val sessionId = "test-session-001"

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
        onlineManager = GameOnlineManager(sessionId)
        onlineManager?.joinGame()
        onlineManager?.connectToSession(
            onBoardUpdate = { newBoard, isMyTurn ->
                runOnUiThread {
                    board = newBoard
                    updateUIFromBoard()
                    gameOver = gameController.checkGameState(board) != GameStatus.NOT_FINISHED
                    turnPlayer1 = isMyTurn
                }
            },
            onGameEnd = { status ->
                runOnUiThread {
                    gameOver = true
                    showResult(status)
                }
            }
        )
        board = gameController.newBoard()
        updateUIFromBoard()
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
            if (!turnPlayer1) return
            onlineManager?.makeMove(col)
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
            .setPositiveButton("Restart") { _, _ -> chooseModeAndStart() }
            .setNegativeButton("Exit") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
