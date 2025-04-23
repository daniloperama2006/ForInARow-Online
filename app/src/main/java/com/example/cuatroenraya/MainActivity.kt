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
 * MainActivity es la actividad principal que gestiona la interfaz del juego Conecta 4.
 * Permite seleccionar modo de juego (vs CPU o vs Jugador), colocar fichas en el tablero,
 * controlar turnos, invocar la IA, y mostrar resultados (victoria/empate).
 */
class MainActivity : ComponentActivity() {
    // Matriz lógica del tablero (6 filas x 7 columnas). '-' indica celda vacía.
    private lateinit var board: Array<Array<Char>>
    // Matriz de vistas ImageView que representan cada celda en la interfaz.
    private lateinit var boardViews: Array<Array<ImageView>>
    // Indica si la partida ha terminado (victoria o empate).
    private var gameOver = false
    // Controlador de lógica (crea tablero y verifica estado del juego).
    private val gameController = GameController()
    // Modo de juego: true si es vs CPU, false si es vs Jugador 2.
    private var isVsCPU = true
    // Indica de quién es el turno: true para Jugador 1 (O), false para Jugador 2 o CPU (X).
    private var turnPlayer1 = true

    /**
     * Ciclo de vida onCreate: inflar layout, inicializar vistas,
     * configurar botón de reinicio, y mostrar diálogo de selección de modo.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Crear matriz de ImageViews y asignar listeners
        initBoardViews()
        // Configurar botón Reiniciar para volver a preguntar modo y reiniciar
        findViewById<Button>(R.id.restartGame)
            .setOnClickListener { chooseModeAndStart() }
        // Mostrar diálogo de selección de modo de juego
        chooseModeAndStart()
    }

    /**
     * onStart: la actividad está a punto de ser visible.
     */
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart llamado")
    }

    /**
     * onResume: la actividad está visible y el usuario puede interactuar.
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume llamado")
    }

    /**
     * onPause: la actividad está parcialmente oculta o en transición a otra.
     */
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause llamado")
    }

    /**
     * onStop: la actividad ya no es visible.
     */
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop llamado")
    }

    /**
     * onRestart: la actividad vuelve a ser visible después de estar parada.
     */
    override fun onRestart() {
        super.onRestart()
        Log.d(TAG, "onRestart llamado")
    }

    /**
     * onDestroy: la actividad va a ser destruida.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy llamado")
    }



    /**
     * Muestra un diálogo modal para elegir modo de juego:
     * - "Jugador vs CPU": asigna isVsCPU=true
     * - "Jugador vs Jugador": asigna isVsCPU=false
     * Luego llama a startGame().
     */
    private fun chooseModeAndStart() {
        AlertDialog.Builder(this)
            .setTitle("Selecciona modo de juego")
            .setPositiveButton("Jugador vs CPU") { _, _ ->
                isVsCPU = true
                startGame()
            }
            .setNegativeButton("Jugador vs Jugador") { _, _ ->
                isVsCPU = false
                startGame()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Inicializa la matriz boardViews con referencias a cada ImageView
     * del layout: recorre las 6 filas y 7 columnas.
     * Asigna listener que recibe la columna clickeada.
     */
    private fun initBoardViews() {
        val layout = findViewById<LinearLayout>(R.id.boardLayout)
        boardViews = Array(6) { row ->
            val rowLayout = layout.getChildAt(row) as LinearLayout
            Array(7) { col ->
                (rowLayout.getChildAt(col) as ImageView).apply {
                    // Al hacer clic, solo importa la columna
                    setOnClickListener { onCellClicked(col) }
                }
            }
        }
    }

    /**
     * Prepara una nueva partida: crea tablero lógico vacío,
     * resetea flags, y limpia todas las celdas en la UI.
     */
    private fun startGame() {
        board = gameController.newBoard()
        gameOver = false
        turnPlayer1 = true
        // Limpiar todas las celdas gráficas al color 'blue'
        for (i in 0 until 6) for (j in 0 until 7) {
            boardViews[i][j].setImageResource(R.drawable.blue)
        }
    }

    /**
     * Maneja el clic en una columna.
     * 1. Intenta colocar ficha del turno actual.
     * 2. Verifica estado: victoria o empate.
     * 3. Alterna turno.
     * 4. Si es vs CPU y le toca, programa jugada de la IA.
     */
    private fun onCellClicked(col: Int) {
        if (gameOver) return       // Ignora clics si terminó la partida
        // Intentar jugada; si columna llena, no hace nada
        if (!makeMove(col, turnPlayer1)) return
        // Revisar estado tras la jugada
        val state = gameController.checkGameState(board)
        if (state != GameState.NOT_FINISHED) {
            // Fin de partida
            gameOver = true
            showResult(state)
            return
        }
        // Cambiar turno a siguiente jugador/CPU
        turnPlayer1 = !turnPlayer1
        // Si modo vs CPU y es turno de la máquina,
        // esperar 500ms y realizar jugada
        if (isVsCPU && !turnPlayer1) {
            Handler(Looper.getMainLooper()).postDelayed({
                makeCpuMove()
                val cpuState = gameController.checkGameState(board)
                if (cpuState != GameState.NOT_FINISHED) {
                    gameOver = true
                    showResult(cpuState)
                } else {
                    // Devolver turno al jugador humano
                    turnPlayer1 = true
                }
            }, 500)
        }
    }

    /**
     * Realiza un movimiento en la columna dada para el jugador indicado.
     * Recorre desde la fila inferior hasta la superior,
     * coloca símbolo y actualiza UI.
     * @param col Índice de columna (0..6)
     * @param isPlayer1 true para Jugador 1 ('O'), false para Jugador 2/CPU ('X')
     * @return true si la jugada se colocó, false si la columna estaba llena
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
     * Lógica básica de IA:
     * 1. Busca jugada ganadora en una columna.
     * 2. Si no, busca bloquear jugada del jugador.
     * 3. Si no, elige columna aleatoria.
     */
    private fun makeCpuMove() {
        // 1) Intentar ganar
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
        // 2) Bloquear victoria del jugador
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
        // 3) Movimiento aleatorio si nada prioritario
        val avail = (0 until 7).filter { c -> board.any { it[c] == '-' } }
        if (avail.isNotEmpty()) makeMove(avail.random(), false)
    }

    /**
     * Muestra un diálogo con el resultado de la partida y opciones:
     * - "Reiniciar": volver a elegir modo y reiniciar
     * - "Salir": cerrar diálogo
     */
    private fun showResult(state: GameState) {
        val message = when (state) {
            GameState.PLAYER1_WIN -> "¡Jugador gana!"
            GameState.PLAYER2_WIN -> if (isVsCPU) "¡Máquina gana!" else "¡Jugador 2 gana!"
            GameState.DRAW -> "¡Empate!"
            else -> ""
        }
        AlertDialog.Builder(this)
            .setTitle(message)
            .setPositiveButton("Reiniciar") { _, _ -> chooseModeAndStart() }
            .setNegativeButton("Salir") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
