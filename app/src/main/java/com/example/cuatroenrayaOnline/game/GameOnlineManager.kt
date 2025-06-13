package com.example.cuatroenrayaOnline.game

import android.app.AlertDialog
import com.google.firebase.database.*

class GameOnlineManager(
    private val userId: String
) {
    private val database = FirebaseDatabase.getInstance().reference
    private var sessionRef: DatabaseReference? = null
    private var sessionId: String = ""
    private var isPlayer1 = false
    private var isMyTurn = false

    private var onBoardUpdate: ((Array<Array<Char>>, Boolean) -> Unit)? = null
    private var onGameEnd: ((GameStatus) -> Unit)? = null

    fun connectToSession(
        onBoardUpdate: (Array<Array<Char>>, Boolean) -> Unit,
        onGameEnd: (GameStatus) -> Unit
    ) {
        this.onBoardUpdate = onBoardUpdate
        this.onGameEnd = onGameEnd
        joinGame()
    }

    private fun joinGame() {
        database.child("sessions").orderByChild("gameState").equalTo("waiting")
            .limitToFirst(10)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var joined = false
                    for (session in snapshot.children) {
                        val sessionKey = session.key ?: continue
                        val player1Uid = session.child("players/player1/uid").getValue(String::class.java)
                        val gameStateValue = session.child("gameState").getValue(String::class.java)

                        if (player1Uid != null && player1Uid != userId && gameStateValue == "waiting") {
                            sessionId = sessionKey
                            sessionRef = database.child("sessions").child(sessionId)

                            sessionRef!!.child("players/player2").setValue(Player(userId))
                            sessionRef!!.child("gameState").setValue("playing")

                            isPlayer1 = false
                            isMyTurn = false

                            listenForUpdates()
                            joined = true
                            break
                        }
                    }

                    if (!joined) {
                        // Crear nueva sesión
                        sessionId = database.child("sessions").push().key ?: "session-${System.currentTimeMillis()}"
                        sessionRef = database.child("sessions").child(sessionId)

                        val initialState = GameState(
                            board = List(6) { List(7) { 0 } },
                            currentPlayer = "player1",
                            players = mapOf("player1" to Player(userId)),
                            gameState = "waiting"
                        )

                        sessionRef!!.setValue(initialState)
                        sessionRef!!.child("winner").removeValue()

                        isPlayer1 = true
                        isMyTurn = true

                        listenForUpdates()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Manejo opcional de errores
                }
            })
    }

    private fun listenForUpdates() {
        sessionRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.getValue(GameState::class.java) ?: return

                isMyTurn = (isPlayer1 && state.currentPlayer == "player1") ||
                        (!isPlayer1 && state.currentPlayer == "player2")

                val board = Array(6) { row ->
                    Array(7) { col ->
                        when (state.board[row][col]) {
                            1 -> 'O'
                            2 -> 'X'
                            else -> '-'
                        }
                    }
                }

                onBoardUpdate?.invoke(board, isMyTurn)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        sessionRef?.child("winner")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val winnerStr = snapshot.getValue(String::class.java) ?: return
                if (winnerStr.isBlank()) return

                sessionRef?.child("gameState")?.get()?.addOnSuccessListener { stateSnapshot ->
                    val gameState = stateSnapshot.getValue(String::class.java)
                    if (gameState != "playing") return@addOnSuccessListener

                    try {
                        val result = GameStatus.valueOf(winnerStr)
                        onGameEnd?.invoke(result)
                    } catch (_: Exception) {}
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun makeMove(col: Int) {
        sessionRef?.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val gameState = currentData.getValue(GameState::class.java)
                    ?: return Transaction.success(currentData)

                if (gameState.gameState == "finished") {
                    return Transaction.abort()
                }

                val board = gameState.board.map { it.toMutableList() }.toMutableList()
                var moveMade = false

                for (row in 5 downTo 0) {
                    if (board[row][col] == 0) {
                        board[row][col] = if (isPlayer1) 1 else 2
                        moveMade = true
                        break
                    }
                }

                if (!moveMade) {
                    return Transaction.abort()
                }

                val updatedState = gameState.copy(
                    board = board,
                    currentPlayer = if (gameState.currentPlayer == "player1") "player2" else "player1"
                )

                currentData.value = updatedState
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                error?.toException()?.printStackTrace()
            }
        })
    }

    fun sendGameEnd(status: GameStatus) {
        sessionRef?.child("winner")?.setValue(status.name)
        sessionRef?.child("gameState")?.setValue("finished")
    }

    fun isPlayerTurn(): Boolean = isMyTurn

    fun skipTurn() {
        sessionRef?.child("currentPlayer")?.setValue(
            if (isPlayer1) "player2" else "player1"
        )
    }

    

}
