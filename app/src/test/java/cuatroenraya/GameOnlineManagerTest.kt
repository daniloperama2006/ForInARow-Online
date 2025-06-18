package cuatroenraya

import android.content.Context
import android.os.Looper
import com.example.cuatroenrayaOnline.game.GameOnlineManager
import com.example.cuatroenrayaOnline.game.GameState
import com.example.cuatroenrayaOnline.game.GameStatus
import com.example.cuatroenrayaOnline.game.Player
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Query
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class GameOnlineManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockFirebaseDatabase: FirebaseDatabase

    @Mock
    private lateinit var mockDatabaseReference: DatabaseReference

    @Mock
    private lateinit var mockQuery: Query

    @Mock
    private lateinit var mockDataSnapshot: DataSnapshot

    @Mock
    private lateinit var mockOnBoardUpdate: (Array<Array<Char>>, Boolean) -> Unit

    @Mock
    private lateinit var mockOnGameEnd: (GameStatus) -> Unit

    @Mock
    private lateinit var mockOnMatchFoundListener: () -> Unit

    private lateinit var gameOnlineManager: GameOnlineManager
    private val userId = "testUserId"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        // Mock static method getInstance()
        mockStatic(FirebaseDatabase::class.java).use { mockedStatic ->
            mockedStatic.`when`<FirebaseDatabase> { FirebaseDatabase.getInstance() }
                .thenReturn(mockFirebaseDatabase)
            `when`(mockFirebaseDatabase.reference).thenReturn(mockDatabaseReference)
            `when`(mockDatabaseReference.child(anyString())).thenReturn(mockDatabaseReference)
            `when`(mockDatabaseReference.push()).thenReturn(mockDatabaseReference) // For new session creation
            `when`(mockDatabaseReference.key).thenReturn("mockSessionId")

            gameOnlineManager = GameOnlineManager(userId, mockContext)
            gameOnlineManager.setOnMatchFoundListener(mockOnMatchFoundListener)

            // Advance Looper to clear any pending tasks from initialization
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }
    }


    @Test
    fun `connectToSession - should create new session if no waiting session available`() {
        // Arrange
        // Mock an empty DataSnapshot for no waiting sessions
        `when`(mockDataSnapshot.children).thenReturn(emptyList())

        doAnswer { invocation ->
            val listener = invocation.getArgument<ValueEventListener>(0)
            listener.onDataChange(mockDataSnapshot)
            null
        }.`when`(mockQuery).addListenerForSingleValueEvent(any(ValueEventListener::class.java))

        `when`(mockDatabaseReference.orderByChild(anyString())).thenReturn(mockQuery)
        `when`(mockQuery.equalTo(anyString())).thenReturn(mockQuery)
        `when`(mockQuery.limitToFirst(anyInt())).thenReturn(mockQuery)

        // Act
        gameOnlineManager.connectToSession(mockOnBoardUpdate, mockOnGameEnd)
        Shadows.shadowOf(Looper.getMainLooper()).idle() // Process Handler.post

        // Assert
        // Verify that a new session was created
        verify(mockDatabaseReference, atLeastOnce()).push()
        verify(mockDatabaseReference).setValue(any(GameState::class.java)) // Set initial GameState
        verify(mockDatabaseReference).removeValue() // Remove winner
        verify(mockDatabaseReference, atLeastOnce()).setValue("waiting") // Set gameState to waiting
        verify(mockOnMatchFoundListener).invoke() // Match found callback should be invoked
    }


    @Test
    fun `makeMove - should make a move if it's my turn`() {
        // Arrange
        // Simulate that it's player1's turn
        val initialGameState = GameState(
            board = List(6) { List(7) { 0 } },
            currentPlayer = "player1",
            players = mapOf("player1" to Player(userId), "player2" to Player("opponent")),
            gameState = "playing"
        )

        // Set internal state of GameOnlineManager to be Player1 and it's my turn
        // This is a bit of a hack for testing internal state, ideally, it would be set via methods.
        // For this test, let's assume `connectToSession` has already run and set these.
        // In a real scenario, you'd test `connectToSession` first to ensure correct state setup.
        setInternalState(gameOnlineManager, "isPlayer1", true)
        setInternalState(gameOnlineManager, "isMyTurn", true)
        setInternalState(gameOnlineManager, "sessionRef", mockDatabaseReference)


        // Mock doTransaction
        doAnswer { invocation ->
            val transactionHandler = invocation.getArgument<Transaction.Handler>(0)
            val mutableData = mock(MutableData::class.java)
            `when`(mutableData.getValue(GameState::class.java)).thenReturn(initialGameState)

            val result = transactionHandler.doTransaction(mutableData)
            verify(mutableData).value = any(GameState::class.java) // Verify value was set
            Transaction.success(mutableData) // Return success to trigger onComplete
        }.`when`(mockDatabaseReference).runTransaction(any(Transaction.Handler::class.java))

        // Act
        gameOnlineManager.makeMove(0) // Try to make a move in column 0
        Shadows.shadowOf(Looper.getMainLooper()).idle() // Process Handler.post

        // Assert
        verify(mockDatabaseReference).runTransaction(any(Transaction.Handler::class.java))
        // We can't easily verify the exact board state change without more complex mocking of MutableData,
        // but we can verify the transaction was attempted and completed.
    }

    @Test
    fun `makeMove - should not make a move if it's not my turn`() {
        // Arrange
        // Set internal state of GameOnlineManager to be Player1 but not my turn
        setInternalState(gameOnlineManager, "isPlayer1", true)
        setInternalState(gameOnlineManager, "isMyTurn", false)
        setInternalState(gameOnlineManager, "sessionRef", mockDatabaseReference)

        // Act
        gameOnlineManager.makeMove(0) // Try to make a move
        Shadows.shadowOf(Looper.getMainLooper()).idle() // Process Handler.post

        // Assert
        verify(mockDatabaseReference, never()).runTransaction(any(Transaction.Handler::class.java))
        // Verify that a Toast was shown indicating it's not the turn.
        // This requires more advanced Robolectric Toast testing or capturing Looper messages.
        // For now, we rely on the absence of the transaction call.
    }

    @Test
    fun `sendGameEnd - should set winner and game state to finished`() {
        // Arrange
        setInternalState(gameOnlineManager, "sessionRef", mockDatabaseReference)
        val gameStatus = GameStatus.PLAYER1_WIN

        // Act
        gameOnlineManager.sendGameEnd(gameStatus)

        // Assert
        verify(mockDatabaseReference).child("winner")
        verify(mockDatabaseReference).setValue(gameStatus.name)
        verify(mockDatabaseReference).child("gameState")
        verify(mockDatabaseReference).setValue("finished")
    }

    @Test
    fun `isPlayerTurn - should return current turn status`() {
        // Arrange
        setInternalState(gameOnlineManager, "isMyTurn", true)
        // Act
        val isMyTurn = gameOnlineManager.isPlayerTurn()
        // Assert
        assert(isMyTurn)

        // Arrange
        setInternalState(gameOnlineManager, "isMyTurn", false)
        // Act
        val isNotMyTurn = gameOnlineManager.isPlayerTurn()
        // Assert
        assert(!isNotMyTurn)
    }

    @Test
    fun `skipTurn - player1 should set currentPlayer to player2`() {
        setInternalState(gameOnlineManager, "sessionRef", mockDatabaseReference)
        setInternalState(gameOnlineManager, "isPlayer1", true)

        gameOnlineManager.skipTurn()

        verify(mockDatabaseReference).child("currentPlayer")
        verify(mockDatabaseReference.child("currentPlayer")).setValue("player2")
    }

    @Test
    fun `skipTurn - player2 should set currentPlayer to player1`() {
        setInternalState(gameOnlineManager, "sessionRef", mockDatabaseReference)
        setInternalState(gameOnlineManager, "isPlayer1", false)

        gameOnlineManager.skipTurn()

        verify(mockDatabaseReference).child("currentPlayer")
        verify(mockDatabaseReference.child("currentPlayer")).setValue("player1")
    }


    @Test
    fun `notifyOpponentLeft - should set opponent as winner and game as finished if not already finished`() {
        // Arrange
        setInternalState(gameOnlineManager, "sessionRef", mockDatabaseReference)
        setInternalState(gameOnlineManager, "isPlayer1", true) // Assume current player is Player1
        setInternalState(gameOnlineManager, "gameStateString", "playing")

        // Act
        gameOnlineManager.notifyOpponentLeft()
        Shadows.shadowOf(Looper.getMainLooper()).idle() // Process Handler.post

        // Assert
        verify(mockDatabaseReference).child("winner")
        verify(mockDatabaseReference).setValue(GameStatus.PLAYER2_WIN.name) // Player1 left, so Player2 wins
        verify(mockDatabaseReference).child("gameState")
        verify(mockDatabaseReference).setValue("finished")
    }

    @Test
    fun `notifyOpponentLeft - should not do anything if game is already finished`() {
        // Arrange
        setInternalState(gameOnlineManager, "sessionRef", mockDatabaseReference)
        setInternalState(gameOnlineManager, "isPlayer1", true)
        setInternalState(gameOnlineManager, "gameStateString", "finished")

        // Act
        gameOnlineManager.notifyOpponentLeft()
        Shadows.shadowOf(Looper.getMainLooper()).idle() // Process Handler.post

        // Assert
        verify(mockDatabaseReference, never()).child("winner")
        verify(mockDatabaseReference, never()).setValue(anyString())
    }

    @Test
    fun `getSessionId - should return the current session ID`() {
        // Arrange
        val expectedSessionId = "someMockSessionId"
        setInternalState(gameOnlineManager, "sessionId", expectedSessionId)

        // Act
        val actualSessionId = gameOnlineManager.getSessionId()

        // Assert
        assert(actualSessionId == expectedSessionId)
    }

    // Sets private field for testing
    private fun setInternalState(target: Any, fieldName: String, value: Any?) {
        val field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}