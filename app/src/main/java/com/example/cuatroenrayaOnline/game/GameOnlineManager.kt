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
                        // Si hay player1 distinto a nosotros y está en 'waiting', únase como player2
                        if (player1Uid != null && player1Uid != userId && gameStateValue == "waiting") {
                            sessionId = sessionKey
                            sessionRef = database.child("sessions").child(sessionId)

                            // Añadir player2 y cambiar estado a playing
                            sessionRef!!.child("players").child("player2").setValue(Player(userId))
                            sessionRef!!.child("gameState").setValue("playing")

                            isPlayer1 = false
                            isMyTurn = false

                            // Inicial prevPlayersCount = 2 cuando detectemos en listenForUpdates
                            listenForUpdates()
                            joined = true
                            break
                        }
                    }

                    if (!joined) {
                        // No se unió a ninguna: crear nueva sesión como player1
                        sessionId = database.child("sessions").push().key ?: "session-${System.currentTimeMillis()}"
                        sessionRef = database.child("sessions").child(sessionId)

                        val initialState = GameState(
                            board = List(6) { List(7) { 0 } },
                            currentPlayer = "player1",
                            players = mapOf("player1" to Player(userId)),
                            gameState = "waiting"
                        )

                        sessionRef!!.setValue(initialState)
                        // Aseguramos que no haya campo 'winner' residual
                        sessionRef!!.child("winner").removeValue()

                        isPlayer1 = true
                        isMyTurn = true

                        // prevPlayersCount = 1 inicialmente, se actualizará en listenForUpdates
                        listenForUpdates()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Opcional: notificar error al UI
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Error al emparejar: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    /**
     * Escucha cambios en la sesión: tablero, turno, jugadores, fin de juego.
     */
    private fun listenForUpdates() {
        sessionRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.getValue(GameState::class.java) ?: return

                // Detectar cambios en número de jugadores para UX (solo si eres player1)
                val playersMap = state.players
                val currentPlayersCount = playersMap.size
                if (isPlayer1) {
                    // Si pasa de 1 a 2, el oponente se unió
                    if (prevPlayersCount < 2 && currentPlayersCount == 2) {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Oponente conectado. ¡Comienza el juego!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    // Si pasa de 2 a <2, oponente abandonó
                    if (prevPlayersCount == 2 && currentPlayersCount < 2) {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Oponente abandonó la partida.", Toast.LENGTH_SHORT).show()
                        }
                        // Opcional: podrías decidir finalizar la sesión localmente o volver al menú
                    }
                }
                prevPlayersCount = currentPlayersCount

                // Actualizar turno según currentPlayer en Firebase
                val wasMyTurn = isMyTurn
                isMyTurn = (isPlayer1 && state.currentPlayer == "player1") ||
                        (!isPlayer1 && state.currentPlayer == "player2")

                // Convertir tablero de List<List<Int>> a Array<Array<Char>>
                val board = Array(6) { row ->
                    Array(7) { col ->
                        when (state.board[row][col]) {
                            1 -> 'O'
                            2 -> 'X'
                            else -> '-'
                        }
                    }
                }

                // Llamar siempre al callback para que UI pinte el tablero
                onBoardUpdate?.invoke(board, isMyTurn)

                // Notificar cuando cambia a nuestro turno
                if (!wasMyTurn && isMyTurn) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "¡Es tu turno!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Opcional: notificar error
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Error en escucha: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // Escuchar fin de juego (campo "winner")
        sessionRef?.child("winner")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val winnerStr = snapshot.getValue(String::class.java) ?: return
                if (winnerStr.isBlank()) return

                sessionRef?.child("gameState")?.get()?.addOnSuccessListener { stateSnapshot ->
                    val gameState = stateSnapshot.getValue(String::class.java)
                    if (gameState != "playing") {
                        // Solo actuamos si estaba en "playing"
                        try {
                            val result = GameStatus.valueOf(winnerStr)
                            onGameEnd?.invoke(result)
                        } catch (_: Exception) {
                            // Valor inesperado
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Error al detectar fin: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }


    fun makeMove(col: Int) {
        if (!isMyTurn) {
            // Opcional: notificar intento inválido
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "No es tu turno, no se envía movimiento.", Toast.LENGTH_SHORT).show()
            }
            return
        }
        sessionRef?.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val gameState = currentData.getValue(GameState::class.java)
                    ?: return Transaction.success(currentData)

                // Si ya terminado, abortar
                if (gameState.gameState == "finished") {
                    return Transaction.abort()
                }

                // Clonar tablero y buscar fila disponible
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
                    // Columna llena: abortar
                    return Transaction.abort()
                }

                // Alternar turno
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
                    // Opcional: log o notificar error de transacción
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Error al enviar movimiento: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
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


    fun leaveSession() {
        sessionRef?.child("players")?.child(if (isPlayer1) "player1" else "player2")?.removeValue()
        sessionRef?.removeValue()
    }
}
