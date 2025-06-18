package com.example.cuatroenrayaOnline

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.cuatroenraya.R
import com.example.cuatroenrayaOnline.game.GameOnlineManager
import com.example.cuatroenrayaOnline.game.GameStatus
import com.example.cuatroenrayaOnline.game.GameController
import com.google.firebase.auth.FirebaseAuth
import java.text.Normalizer
import java.util.UUID
import com.example.cuatroenrayaOnline.game.VocabularyQuestion

class MainActivity : ComponentActivity() {
    // Game variables
    private lateinit var board: Array<Array<Char>>
    private lateinit var boardViews: Array<Array<ImageView>>
    private lateinit var vocabularyList: List<VocabularyQuestion>
    private var gameOver = false
    private val gameController = GameController()
    private var isVsCPU = true
    private var isOnlineGame = false
    private var turnPlayer1 = true
    private var isMyTurn = false
    private var onlineManager: GameOnlineManager? = null
    private var waitingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initBoardViews()
        findViewById<Button>(R.id.restartGame).setOnClickListener {
            // Leave session if in an active online game
            if (isOnlineGame && !gameOver) {
                onlineManager?.notifyOpponentLeft()
                onlineManager?.leaveSession()
            }
            chooseModeAndStart()
        }
        loadVocabularyFromAssets()
        chooseModeAndStart()
    }

    // Show dialog to choose the game mode
    private fun chooseModeAndStart() {
        AlertDialog.Builder(this)
            .setTitle("Select game mode")
            .setPositiveButton("Player vs CPU") { _, _ ->
                leaveOnlineSessionIfNeeded()
                isVsCPU = true
                isOnlineGame = false
                startGame()
            }
            .setNegativeButton("Player vs Player") { _, _ ->
                leaveOnlineSessionIfNeeded()
                isVsCPU = false
                isOnlineGame = false
                startGame()
            }
            .setNeutralButton("Player vs Online") { _, _ ->
                leaveOnlineSessionIfNeeded()
                isOnlineGame = true
                isVsCPU = false
                setupOnlineGame()
            }
            .setCancelable(false)
            .show()
    }

    // Starts or joins an online game session
    private fun setupOnlineGame() {
        board = Array(6) { Array(7) { '-' } }
        gameOver = false

        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: UUID.randomUUID().toString()
        onlineManager = GameOnlineManager(userId, this)

        // Show waiting dialog
        waitingDialog = AlertDialog.Builder(this)
            .setTitle("Waiting for a player...")
            .setMessage("Please wait while someone joins the game.")
            .setCancelable(false)
            .create()
        waitingDialog?.show()

        // Connect to online session
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
                    val state = gameController.checkGameState(board)
                    if (state != GameStatus.NOT_FINISHED && !gameOver) {
                        gameOver = true
                        onlineManager?.sendGameEnd(state)
                        showResult(state)
                    }
                }
            },
            onGameEnd = { status ->
                if (!gameOver) {
                    gameOver = true
                    runOnUiThread {
                        updateUIFromBoard()
                        Toast.makeText(this, "Game ended: $status", Toast.LENGTH_SHORT).show()
                        showResult(status)
                    }
                }
            }
        )
    }

    // Starts local game
    private fun startGame() {
        board = gameController.newBoard()
        gameOver = false
        turnPlayer1 = true
        updateUIFromBoard()
    }

    // Initialize board UI with click listeners
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

    // Called when a cell is tapped
    private fun onCellClicked(col: Int) {
        if (gameOver) return

        if (isOnlineGame) {
            if (!isMyTurn) {
                Toast.makeText(this, "Wait for your turn...", Toast.LENGTH_SHORT).show()
                return
            }

            val question = getRandomUnansweredQuestion()
            if (question != null) {
                showVocabularyDialog(question, col)
            } else {
                performOnlineMove(col)
            }
            return
        }

        // Local game move
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

    // Make a move on the board
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

    // CPU move logic
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

    // Show the current board state on the screen
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

    // Show end game result
    private fun showResult(state: GameStatus) {
        val message = when (state) {
            GameStatus.PLAYER1_WIN -> "Player 1 wins!"
            GameStatus.PLAYER2_WIN -> if (isVsCPU) "Machine wins!" else "Player 2 wins!"
            GameStatus.DRAW -> "Draw!"
            else -> ""
        }
        AlertDialog.Builder(this)
            .setTitle(message)
            .setPositiveButton("Menu") { _, _ ->
                leaveOnlineSessionIfNeeded()
                chooseModeAndStart()
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Load questions from vocabulary.json
    private fun loadVocabularyFromAssets() {
        val jsonString = assets.open("vocabulary.json").bufferedReader().use { it.readText() }
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<VocabularyQuestion>>() {}.type
        vocabularyList = gson.fromJson(jsonString, type)
    }

    // Get a random unanswered question
    private fun getRandomUnansweredQuestion(): VocabularyQuestion? {
        return vocabularyList.shuffled().firstOrNull { !it.answeredCorrectly }
    }

    // Send move to Firebase
    private fun performOnlineMove(col: Int) {
        onlineManager?.makeMove(col)
    }

    // Normalize string (remove accents, lowercase, etc.)
    private fun normalizeText(s: String): String {
        val tmp = Normalizer.normalize(s, Normalizer.Form.NFD)
        val noDiacritics = Regex("\\p{InCombiningDiacriticalMarks}+").replace(tmp, "")
        return noDiacritics.trim().lowercase()
    }

    // Show vocabulary question dialog
    private fun showVocabularyDialog(question: VocabularyQuestion, col: Int) {
        val input = EditText(this).apply {
            hint = "Translate: ${question.word}"
            setPadding(30, 20, 30, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(30, 10, 30, 10)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Vocabulary Question")
            .setMessage("What is the correct translation of \"${question.word}\"?")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Submit") { dialog, _ ->
                val answerNorm = normalizeText(input.text.toString())
                val correct = question.answers.map { normalizeText(it) }.any { it == answerNorm }

                if (correct) {
                    question.answeredCorrectly = true
                    Toast.makeText(this, "Correct! You make the move.", Toast.LENGTH_SHORT).show()
                    performOnlineMove(col)
                } else {
                    Toast.makeText(this, "Incorrect. You lose your turn.", Toast.LENGTH_SHORT).show()
                    onlineManager?.skipTurn()
                    isMyTurn = false
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // If in online game, leave session
    private fun leaveOnlineSessionIfNeeded() {
        if (isOnlineGame && !gameOver) {
            onlineManager?.notifyOpponentLeft()
            onlineManager?.leaveSession()
        }
    }

    override fun onDestroy() {
        leaveOnlineSessionIfNeeded()
        super.onDestroy()
    }
}