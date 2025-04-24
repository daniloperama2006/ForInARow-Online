package com.example.cuatroenraya

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity

/**
 * MainActivity is the main activity that manages the user interface of the Connect Four game.
 * It allows selecting the game mode (vs CPU or vs Player), placing pieces on the board,
 * controlling turns, invoking the AI, and displaying results (win/draw).
 */
class MainActivity : ComponentActivity() {
    // Logical matrix of the board (6 rows x 7 columns). '-' indicates an empty cell.
    private lateinit var board: Array<Array<Char>>
    // Matrix of ImageView views that represent each cell in the user interface.
    private lateinit var boardViews: Array<Array<ImageView>>
    // Indicates whether the game has ended (win or draw).
    private var gameOver = false
    // Logic controller (creates the board and verifies the game state).
    private val gameController = GameController()
    // Game mode: true if it's vs CPU, false if it's vs Player 2.
    private var isVsCPU = true
    // Indicates whose turn it is: true for Player 1 ('O'), false for Player 2 or CPU ('X').
    private var turnPlayer1 = true

    /**
     * onCreate lifecycle method: inflate layout, initialize views,
     * configure the restart button, and show the mode selection dialog.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Create the matrix of ImageViews and assign click listeners
        initBoardViews()
        // Configure the Restart button to ask for the mode again and restart the game
        findViewById<Button>(R.id.restartGame)
            .setOnClickListener { chooseModeAndStart() }
        // Show the game mode selection dialog
        chooseModeAndStart()
    }

    /**
     * onStart: the activity is about to become visible.
     */
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart called")
    }

    /**
     * onResume: the activity is visible and the user can interact with it.
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
    }

    /**
     * onPause: the activity is partially hidden or in transition to another activity.
     */
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
    }

    /**
     * onStop: the activity is no longer visible.
     */
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop called")
    }

    /**
     * onRestart: the activity is becoming visible again after being stopped.
     */
    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart called")
    }

    /**
     * onDestroy: the activity is about to be destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
    }

    /**
     * Shows a modal dialog to choose the game mode:
     * - "Player vs CPU": sets isVsCPU=true
     * - "Player vs Player": sets isVsCPU=false
     * Then calls startGame().
     */
    private fun chooseModeAndStart() {
        AlertDialog.Builder(this)
            .setTitle("Select game mode")
            .setPositiveButton("Player vs CPU") { _, _ ->
                isVsCPU = true
                startGame()
            }
            .setNegativeButton("Player vs Player") { _, _ ->
                isVsCPU = false
                startGame()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Initializes the boardViews matrix with references to each ImageView
     * in the layout: iterates through the 6 rows and 7 columns.
     * Assigns a click listener that receives the clicked column.
     */
    private fun initBoardViews() {
        val layout = findViewById<LinearLayout>(R.id.boardLayout)
        boardViews = Array(6) { row ->
            val rowLayout = layout.getChildAt(row) as LinearLayout
            Array(7) { col ->
                (rowLayout.getChildAt(col) as ImageView).apply {
                    // When clicked, only the column index is relevant
                    setOnClickListener { onCellClicked(col) }
                }
            }
        }
    }

    /**
     * Prepares a new game: creates an empty logical board,
     * resets flags, and clears all cells in the UI.
     */
    private fun startGame() {
        board = gameController.newBoard()
        gameOver = false
        turnPlayer1 = true
        // Clear all graphical cells to the 'blue' background
        for (i in 0 until 6) for (j in 0 until 7) {
            boardViews[i][j].setImageResource(R.drawable.blue)
        }
    }

    /**
     * Handles a click on a column.
     * 1. Tries to place the current player's piece.
     * 2. Checks the game state: win or draw.
     * 3. Alternates the turn.
     * 4. If it's vs CPU and it's the CPU's turn, schedules an AI move.
     * @param col The index of the clicked column (0..6).
     */
    private fun onCellClicked(col: Int) {
        if (gameOver) return       // Ignore clicks if the game is over
        // Try to make a move; if the column is full, do nothing
        if (!makeMove(col, turnPlayer1)) return
        // Check the game state after the move
        val state = gameController.checkGameState(board)
        if (state != GameState.NOT_FINISHED) {
            // End of the game
            gameOver = true
            showResult(state)
            return
        }
        // Switch the turn to the next player/CPU
        turnPlayer1 = !turnPlayer1
        // If in vs CPU mode and it's the machine's turn,
        // wait 500ms and make a move
        if (isVsCPU && !turnPlayer1) {
            Handler(Looper.getMainLooper()).postDelayed({
                makeCpuMove()
                val cpuState = gameController.checkGameState(board)
                if (cpuState != GameState.NOT_FINISHED) {
                    gameOver = true
                    showResult(cpuState)
                } else {
                    // Return the turn to the human player
                    turnPlayer1 = true
                }
            }, 500)
        }
    }

    /**
     * Makes a move in the given column for the specified player.
     * Iterates from the bottom row to the top,
     * places the symbol, and updates the UI.
     * @param col The column index (0..6).
     * @param isPlayer1 true for Player 1 ('O'), false for Player 2/CPU ('X').
     * @return true if the move was placed, false if the column was full.
     */
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

    /**
     * Basic AI logic:
     * 1. Looks for a winning move in any column.
     * 2. If not found, looks to block the player's winning move.
     * 3. If neither, chooses a random available column.
     */
    private fun makeCpuMove() {
        // 1) Try to win
        for (col in 0 until 7) {
            if (board.any { it[col] == '-' }) {
                val temp = board.map { it.copyOf() }.toTypedArray()
                for (r in 5 downTo 0) if (temp[r][col] == '-') { temp[r][col] = 'X'; break }
                if (gameController.checkGameState(temp) == GameState.PLAYER2_WIN) {
                    makeMove(col, false)
                    return
                }
            }
        }
        // 2) Block player's win
        for (col in 0 until 7) {
            if (board.any { it[col] == '-' }) {
                val temp = board.map { it.copyOf() }.toTypedArray()
                for (r in 5 downTo 0) if (temp[r][col] == '-') { temp[r][col] = 'O'; break }
                if (gameController.checkGameState(temp) == GameState.PLAYER1_WIN) {
                    makeMove(col, false)
                    return
                }
            }
        }
        // 3) Random move if nothing prioritized
        val avail = (0 until 7).filter { c -> board.any { it[c] == '-' } }
        if (avail.isNotEmpty()) makeMove(avail.random(), false)
    }

    /**
     * Shows a dialog with the game result and options:
     * - "Restart": go back to mode selection and restart
     * - "Exit": close the dialog
     * @param state The final [GameState] of the game.
     */
    private fun showResult(state: GameState) {
        val message = when (state) {
            GameState.PLAYER1_WIN -> "Player wins!"
            GameState.PLAYER2_WIN -> if (isVsCPU) "Machine wins!" else "Player 2 wins!"
            GameState.DRAW -> "Draw!"
            else -> ""
        }
        AlertDialog.Builder(this)
            .setTitle(message)
            .setPositiveButton("Restart") { _, _ -> chooseModeAndStart() }
            .setNegativeButton("Exit") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}