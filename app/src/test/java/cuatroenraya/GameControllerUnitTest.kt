package cuatroenraya // Specifies the package this code belongs to.

import com.example.cuatroenrayaOnline.game.GameController // Imports the GameController class from another package.
import com.example.cuatroenrayaOnline.game.GameStatus // Imports the GameState enum from another package.
import org.junit.Assert.assertEquals // Imports the assertEquals function from JUnit for making assertions in tests.
import org.junit.Test // Imports the Test annotation from JUnit to mark methods as test cases.

/**
 * This class contains unit tests for the GameController class.
 * It uses JUnit to verify the functionality of different methods in GameController.
 */
class GameControllerUnitTest {

    private val gameController = GameController() // Creates an instance of the GameController class to be tested.

    /**
     * Tests the 'newBoard' method of GameController.
     * It asserts that the method returns a 6x7 2D array (representing the game board)
     * where every cell is initialized with the '-' character, indicating an empty cell.
     */
    @Test
    fun `newBoard should return a 6x7 board filled with empty cells`() {
        val board = gameController.newBoard() // Calls the newBoard method to get the game board.
        assertEquals(6, board.size) // Asserts that the board has 6 rows.
        assertEquals(7, board[0].size) // Asserts that the first row (and by extension, all rows) has 7 columns.
        board.forEach { row -> // Iterates through each row in the board.
            row.forEach { cell -> // Iterates through each cell in the current row.
                assertEquals('-', cell) // Asserts that the value of the current cell is '-', the representation of an empty cell.
            }
        }
    }

    /**
     * Tests the 'checkGameState' method for an empty game board.
     * It asserts that when the board is empty (newly created), the game state should be 'NOT_FINISHED'.
     */
    @Test
    fun `checkGameState should return NOT_FINISHED for an empty board`() {
        val board = gameController.newBoard() // Creates a new empty game board.
        assertEquals(GameStatus.NOT_FINISHED, gameController.checkGameState(board)) // Asserts that checking the game state of the empty board returns NOT_FINISHED.
    }

    /**
     * Tests the 'checkGameState' method when Player 1 ('O') has four pieces in a horizontal line.
     * It sets up a board with four 'O's in the last row and asserts that the game state is 'PLAYER1_WIN'.
     */
    @Test
    fun `checkGameState should return PLAYER1_WIN for four 'O' in a horizontal line`() {
        val board = arrayOf( // Creates a specific game board configuration.
            arrayOf('-', '-', '-', '-', '-', '-', '-'),
            arrayOf('-', '-', '-', '-', '-', '-', '-'),
            arrayOf('-', '-', '-', '-', '-', '-', '-'),
            arrayOf('-', '-', '-', '-', '-', '-', '-'),
            arrayOf('-', '-', '-', '-', '-', '-', '-'),
            arrayOf('O', 'O', 'O', 'O', '-', '-', '-') // Four 'O's in a horizontal line.
        )
        assertEquals(GameStatus.PLAYER1_WIN, gameController.checkGameState(board)) // Asserts that checking the game state returns PLAYER1_WIN.
    }

    /**
     * Tests the 'checkGameState' method when Player 2 ('X') has four pieces in a vertical line.
     * It sets up a board with four 'X's in the third column and asserts that the game state is 'PLAYER2_WIN'.
     */
    @Test
    fun `checkGameState should return PLAYER2_WIN for four 'X' in a vertical line`() {
        val board = arrayOf( // Creates a specific game board configuration.
            arrayOf('-', '-', 'X', '-', '-', '-', '-'),
            arrayOf('-', '-', 'X', '-', '-', '-', '-'),
            arrayOf('-', '-', 'X', '-', '-', '-', '-'),
            arrayOf('-', '-', 'X', '-', '-', '-', '-'), // Four 'X's in a vertical line.
            arrayOf('-', '-', '-', '-', '-', '-', '-'),
            arrayOf('-', '-', '-', '-', '-', '-', '-')
        )
        assertEquals(GameStatus.PLAYER2_WIN, gameController.checkGameState(board)) // Asserts that checking the game state returns PLAYER2_WIN.
    }

    /**
     * Tests the 'checkGameState' method when Player 1 ('O') has four pieces in a diagonal line going up and to the right.
     * It sets up a board with a diagonal of four 'O's and asserts that the game state is 'PLAYER1_WIN'.
     */
    @Test
    fun `checkGameState should return PLAYER1_WIN for four 'O' in a diagonal up-right line`() {
        val board = arrayOf( // Creates a specific game board configuration.
            arrayOf('-', '-', '-', 'O', '-', '-', '-'),
            arrayOf('-', '-', 'O', '-', '-', '-', '-'),
            arrayOf('-', 'O', '-', '-', '-', '-', '-'),
            arrayOf('O', '-', '-', '-', '-', '-', '-'), // Four 'O's in a diagonal up-right line.
            arrayOf('-', '-', '-', '-', '-', '-', '-'),
            arrayOf('-', '-', '-', '-', '-', '-', '-')
        )
        assertEquals(GameStatus.PLAYER1_WIN, gameController.checkGameState(board)) // Asserts that checking the game state returns PLAYER1_WIN.
    }

    /**
     * Tests the 'checkGameState' method when Player 2 ('X') has four pieces in a diagonal line going up and to the left.
     * It sets up a board with a diagonal of four 'X's and asserts that the game state is 'PLAYER2_WIN'.
     */
    @Test
    fun `checkGameState should return PLAYER2_WIN for four 'X' in a diagonal up-left line`() {
        val board = arrayOf( // Creates a specific game board configuration.
            arrayOf('-', '-', '-', '-', 'X', '-', '-'),
            arrayOf('-', '-', '-', 'X', '-', '-', '-'),
            arrayOf('-', '-', 'X', '-', '-', '-', '-'),
            arrayOf('-', 'X', '-', '-', '-', '-', '-'), // Four 'X's in a diagonal up-left line.
            arrayOf('-', '-', '-', '-', '-', '-', '-'),
            arrayOf('-', '-', '-', '-', '-', '-', '-')
        )
        assertEquals(GameStatus.PLAYER2_WIN, gameController.checkGameState(board)) // Asserts that checking the game state returns PLAYER2_WIN.
    }

    /**
     * Tests the 'checkGameState' method when the game board is completely full and there is no winner.
     * It sets up a full board with alternating 'O's and 'X's and asserts that the game state is 'DRAW'.
     */
    @Test
    fun `checkGameState should return DRAW for a full board with no winner`() {
        val board = arrayOf( // Creates a full game board configuration with no winning lines.
            arrayOf('O', 'X', 'O', 'X', 'O', 'X', 'O'),
            arrayOf('X', 'O', 'X', 'O', 'X', 'O', 'X'),
            arrayOf('O', 'X', 'O', 'X', 'O', 'X', 'O'),
            arrayOf('X', 'O', 'X', 'O', 'X', 'O', 'X'),
            arrayOf('O', 'X', 'O', 'X', 'O', 'X', 'O'),
            arrayOf('X', 'O', 'X', 'O', 'X', 'O', 'X')
        )
        assertEquals(GameStatus.DRAW, gameController.checkGameState(board)) // Asserts that checking the game state of the full board returns DRAW.
    }

    /**
     * Tests the 'checkGameState' method for a board where some moves have been made,
     * but there is no winner and the board is not full.
     * It sets up a partially filled board and asserts that the game state is 'NOT_FINISHED'.
     */
    @Test
    fun `checkGameState should return NOT_FINISHED for a board with some moves but no winner or draw`() {
        val board = arrayOf( // Creates a partially filled game board with no winning lines.
            arrayOf('-', '-', '-', '-', '-', '-', '-'),
            arrayOf('-', '-', '-', '-', '-', '-', '-'),
            arrayOf('-', '-', '-', '-', '-', '-', '-'),
            arrayOf('-', '-', '-', 'O', '-', '-', '-'),
            arrayOf('-', '-', 'X', '-', '-', '-', '-'),
            arrayOf('O', '-', '-', '-', '-', '-', '-')
        )
        assertEquals(GameStatus.NOT_FINISHED, gameController.checkGameState(board)) // Asserts that checking the game state of the partially filled board returns NOT_FINISHED.
    }
}