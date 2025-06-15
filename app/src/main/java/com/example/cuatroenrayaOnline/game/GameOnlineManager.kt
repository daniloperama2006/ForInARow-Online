package com.example.cuatroenrayaOnline.game

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.firebase.database.*

class GameOnlineManager(
    private val userId: String,
    private val context: Context
) {
    private val database = FirebaseDatabase.getInstance().reference
    private var sessionRef: DatabaseReference? = null
    private var sessionId: String = ""
    private var isPlayer1 = false
    private var isMyTurn = false

    private var onBoardUpdate: ((Array<Array<Char>>, Boolean) -> Unit)? = null
    private var onGameEnd: ((GameStatus) -> Unit)? = null

    private var prevPlayersCount = 0
    private var gameStateString: String = ""  // "waiting", "playing", "finished", etc.

    /**
     * Conecta a la sesión online, establece callbacks.
     */
    fun connectToSession(
        onBoardUpdate: (Array<Array<Char>>, Boolean) -> Unit,
        onGameEnd: (GameStatus) -> Unit
    ) {
        this.onBoardUpdate = onBoardUpdate
        this.onGameEnd = onGameEnd
        joinGame()
    }

    private fun joinGame() {
        // Busca sesiones en estado "waiting"
        database.child("sessions")
            .orderByChild("gameState").equalTo("waiting")
            .limitToFirst(10)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var joined = false
                    for (session in snapshot.children) {
                        val sessionKey = session.key ?: continue
                        val player1Uid = session.child("players/player1/uid").getValue(String::class.java)
                        val gameStateValue = session.child("gameState").getValue(String::class.java)
                        // Si hay player1 distinto y estado "waiting", únase como player2
                        if (player1Uid != null && player1Uid != userId && gameStateValue == "waiting") {
                            sessionId = sessionKey
                            sessionRef = database.child("sessions").child(sessionId)

                            // Añadir player2 y cambiar estado a "playing"
                            sessionRef!!.child("players").child("player2").setValue(Player(userId))
                            sessionRef!!.child("gameState").setValue("playing")

                            isPlayer1 = false
                            isMyTurn = false
                            gameStateString = "playing"

                            listenForUpdates()
                            joined = true
                            break
                        }
                    }

                    if (!joined) {
                        // Crear nueva sesión como player1
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
                        sessionRef!!.child("gameState").setValue("waiting")

                        isPlayer1 = true
                        isMyTurn = true
                        gameStateString = "waiting"

                        listenForUpdates()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Error pairing: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    /**
     * Escucha cambios en la sesión: tablero, turno, jugadores y fin de juego.
     */
    private fun listenForUpdates() {
        // Listener principal de GameState completo
        sessionRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.getValue(GameState::class.java) ?: return

                val prevGameStateString = gameStateString
                val newGameStateString = state.gameState ?: ""
                gameStateString = newGameStateString

                // Detectar cambios en número de jugadores
                val playersMap = state.players
                val currentPlayersCount = playersMap.size
                if (isPlayer1) {
                    if (prevPlayersCount < 2 && currentPlayersCount == 2) {
                        // Oponente se unió
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Opponent connected. Game starts!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    if (prevPlayersCount == 2 && currentPlayersCount < 2) {
                        // Oponente se desconectó
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Opponent disconnected. You win!", Toast.LENGTH_SHORT).show()
                        }
                        // Notificar fin si estábamos en playing y no terminado
                        if (prevGameStateString == "playing" && gameStateString != "finished") {
                            onGameEnd?.invoke(GameStatus.PLAYER1_WIN)
                        }
                    }
                } else {
                    if (prevPlayersCount == 2 && currentPlayersCount < 2) {
                        // Oponente (player1) se desconectó
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Opponent disconnected. You win!", Toast.LENGTH_SHORT).show()
                        }
                        if (prevGameStateString == "playing" && gameStateString != "finished") {
                            onGameEnd?.invoke(GameStatus.PLAYER2_WIN)
                        }
                    }
                }
                prevPlayersCount = currentPlayersCount

                // Actualizar turno
                val wasMyTurn = isMyTurn
                isMyTurn = (isPlayer1 && state.currentPlayer == "player1")
                        || (!isPlayer1 && state.currentPlayer == "player2")

                // Convertir tablero y llamar callback
                val boardArr = Array(6) { row ->
                    Array(7) { col ->
                        when (state.board[row][col]) {
                            1 -> 'O'
                            2 -> 'X'
                            else -> '-'
                        }
                    }
                }
                onBoardUpdate?.invoke(boardArr, isMyTurn)

                if (!wasMyTurn && isMyTurn) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Your turn!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Error listening updates: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // Listener para "winner"
        sessionRef?.child("winner")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val winnerStr = snapshot.getValue(String::class.java) ?: return
                if (winnerStr.isBlank()) return

                sessionRef?.child("gameState")?.get()?.addOnSuccessListener { stateSnapshot ->
                    val gs = stateSnapshot.getValue(String::class.java)
                    if (gs != "playing") {
                        try {
                            val result = GameStatus.valueOf(winnerStr)
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context, "Game ended: $winnerStr", Toast.LENGTH_SHORT).show()
                            }
                            onGameEnd?.invoke(result)
                        } catch (_: Exception) {
                            // Valor inesperado
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Error detecting game end: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    /**
     * Realiza movimiento en Firebase.
     */
    fun makeMove(col: Int) {
        if (!isMyTurn) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Not your turn, move not sent.", Toast.LENGTH_SHORT).show()
            }
            return
        }
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
                val nextPlayer = if (gameState.currentPlayer == "player1") "player2" else "player1"
                val updatedState = gameState.copy(
                    board = board,
                    currentPlayer = nextPlayer
                )
                currentData.value = updatedState
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Error sending move: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    /**
     * Envía fin de juego.
     */
    fun sendGameEnd(status: GameStatus) {
        sessionRef?.child("winner")?.setValue(status.name)
        sessionRef?.child("gameState")?.setValue("finished")
    }

    /**
     * Devuelve si es turno del jugador.
     */
    fun isPlayerTurn(): Boolean = isMyTurn

    /**
     * Cede turno al oponente.
     */
    fun skipTurn() {
        sessionRef?.child("currentPlayer")?.setValue(
            if (isPlayer1) "player2" else "player1"
        )
    }

    /**
     * Notifica al oponente que tú te desconectaste/abandonaste: marca gameState=finished y winner=opponent.
     * Luego debería llamarse leaveSession() para limpiar.
     */
    fun notifyOpponentLeft() {
        if (gameStateString == "finished") return
        val opponentWinStatus = if (isPlayer1) GameStatus.PLAYER2_WIN else GameStatus.PLAYER1_WIN
        sessionRef?.child("winner")?.setValue(opponentWinStatus.name)
        sessionRef?.child("gameState")?.setValue("finished")
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "You left the game. Opponent wins.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Sale de la sesión, elimina tu nodo de players y opcionalmente borra toda la sesión.
     */
    fun leaveSession() {
        sessionRef?.child("players")?.child(if (isPlayer1) "player1" else "player2")?.removeValue()
        sessionRef?.removeValue()
    }
}
