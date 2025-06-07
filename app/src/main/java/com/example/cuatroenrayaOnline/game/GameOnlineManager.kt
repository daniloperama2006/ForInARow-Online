package com.example.cuatroenrayaOnline.game

import com.google.firebase.database.*
import com.example.cuatroenrayaOnline.auth.AnonymousAuth
import kotlin.random.Random

class GameOnlineManager(private val gameId: String) {
    private val databaseRef = FirebaseDatabase.getInstance().getReference("gameSessions").child(gameId)
    private val gameController = GameController()
    private var myPlayerKey: String = ""

    fun createNewGame() {
        val uid = AnonymousAuth.getCurrentUserUid() ?: return
        myPlayerKey = "player1"
        val emptyBoard = List(6) { MutableList(7) { 0 } }

        val game = GameState(
            board = emptyBoard,
            currentPlayer = "player1",
            players = mapOf("player1" to Player(uid = uid)),
            vocabularyQuestion = generateRandomQuestion(),
            gameState = "waiting",
            winner = ""
        )

        databaseRef.setValue(game)
    }

    fun joinGame() {
        val uid = AnonymousAuth.getCurrentUserUid() ?: return
        databaseRef.child("players").get().addOnSuccessListener {
            val currentPlayers = it.value as? Map<*, *> ?: emptyMap<Any, Any>()
            myPlayerKey = if ("player1" in currentPlayers) "player2" else "player1"
            databaseRef.child("players").child(myPlayerKey).setValue(Player(uid = uid))
            databaseRef.child("gameState").setValue("playing")
        }
    }

    fun connectToSession(
        onBoardUpdate: (Array<Array<Char>>, Boolean) -> Unit,
        onGameEnd: (GameStatus) -> Unit
    ) {
        val uid = AnonymousAuth.getCurrentUserUid() ?: return

        listenToGameState { state ->
            val playerEntry = state.players.entries.find { it.value.uid == uid }
            playerEntry?.let {
                myPlayerKey = it.key
                val isMyTurn = state.currentPlayer == myPlayerKey
                val boardAsChar = convertBoardToChar(state.board)
                val gameStatus = gameController.checkGameState(boardAsChar)

                onBoardUpdate(boardAsChar, isMyTurn)

                if (gameStatus != GameStatus.NOT_FINISHED) {
                    onGameEnd(gameStatus)
                }
            }
        }
    }

    fun makeMove(column: Int) {
        val uid = AnonymousAuth.getCurrentUserUid() ?: return
        databaseRef.child("players").get().addOnSuccessListener {
            val playersMap = it.value as? Map<*, *> ?: return@addOnSuccessListener
            val currentKey = playersMap.entries.find { entry ->
                (entry.value as? Map<*, *>)?.get("uid") == uid
            }?.key as? String ?: return@addOnSuccessListener

            databaseRef.child("board").get().addOnSuccessListener { snapshot ->
                val board = snapshot.getValue(object : GenericTypeIndicator<List<MutableList<Int>>>() {}) ?: return@addOnSuccessListener

                for (row in 5 downTo 0) {
                    if (board[row][column] == 0) {
                        board[row][column] = if (currentKey == "player1") 1 else 2
                        databaseRef.child("board").setValue(board)
                        databaseRef.child("currentPlayer").setValue(if (currentKey == "player1") "player2" else "player1")
                        databaseRef.child("vocabularyQuestion").setValue(generateRandomQuestion())
                        break
                    }
                }
            }
        }
    }

    private fun convertBoardToChar(board: List<List<Int>>): Array<Array<Char>> {
        return Array(6) { row ->
            Array(7) { col ->
                when (board[row][col]) {
                    1 -> 'O'
                    2 -> 'X'
                    else -> '-'
                }
            }
        }
    }

    fun listenToGameState(onUpdate: (GameState) -> Unit) {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(GameState::class.java)?.let { onUpdate(it) }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun submitAnswer(answer: String, callback: (Boolean) -> Unit) {
        databaseRef.child("vocabularyQuestion").get().addOnSuccessListener {
            val correct = it.child("correctAnswer").value as String
            if (answer.trim().lowercase() == correct.lowercase()) {
                databaseRef.child("vocabularyQuestion/answeredCorrectly").setValue(true)
                callback(true)
            } else {
                callback(false)
            }
        }
    }

    fun restartGame() {
        val emptyBoard = List(6) { MutableList(7) { 0 } }
        databaseRef.child("board").setValue(emptyBoard)
        databaseRef.child("currentPlayer").setValue("player1")
        databaseRef.child("vocabularyQuestion").setValue(generateRandomQuestion())
        databaseRef.child("gameState").setValue("playing")
        databaseRef.child("winner").setValue("")
    }

    private fun generateRandomQuestion(): VocabularyQuestion {
        val words = listOf("cat" to "gato", "dog" to "perro", "apple" to "manzana")
        val (word, answer) = words[Random.nextInt(words.size)]
        return VocabularyQuestion(word = word, correctAnswer = answer)
    }
}
