package pl.edu.agh.kis.pz1.server;

import org.junit.jupiter.api.*;
import pl.edu.agh.kis.pz1.common.protocol.commands.*;
import pl.edu.agh.kis.pz1.common.protocol.events.*;
import pl.edu.agh.kis.pz1.model.cards.Card;
import pl.edu.agh.kis.pz1.model.exceptions.InvalidMoveException;
import pl.edu.agh.kis.pz1.model.game.GameState;
import pl.edu.agh.kis.pz1.model.player.Player;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameManagerTests {

    private GameManager gameManager;
    private ClientHandler handler1;
    private ClientHandler handler2;
    private String playerId1;
    private String playerId2;

    @BeforeEach
    void setUp() throws InvalidMoveException {
        // Tworzymy unikalny GameManager dla każdego testu
        gameManager = new GameManager("game-" + UUID.randomUUID(), 4);

        // Unikalne ID graczy dla każdego testu
        playerId1 = "p1-" + UUID.randomUUID();
        playerId2 = "p2-" + UUID.randomUUID();

        // Mock ClientHandler
        handler1 = mock(ClientHandler.class);
        handler2 = mock(ClientHandler.class);

        when(handler1.getPlayerName()).thenReturn("Adam");
        when(handler2.getPlayerName()).thenReturn("Basia");

        // Dodajemy graczy
        gameManager.addClient(playerId1, handler1);
        gameManager.addClient(playerId2, handler2);

        // Host rozpoczyna grę
        gameManager.processCommand(playerId1, new StartCommand());
        // Po tej komendzie gra powinna być w ANTE -> DEAL -> BET1
    }

    @AfterEach
    void tearDown() {
        // Czyszczenie singleton GameLobby między testami
        GameLobby.getInstance().reset(); // metoda reset() musi usunąć wszystkich graczy z mapy
    }

    @Test
    void testFullCycle_Bet1ToDrawToBet2ToShowdown() throws InvalidMoveException {
        assertEquals(GameState.BET1, gameManager.getCurrentState());

        // Gracz1 BET
        gameManager.processCommand(playerId1, new BetCommand("BET", 20));
        assertEquals(GameState.BET1, gameManager.getCurrentState());

        // Gracz2 CALL
        gameManager.processCommand(playerId2, new BetCommand("CALL", 0));

        // Po BET1 -> DRAW
        assertEquals(GameState.DRAW, gameManager.getCurrentState());

        // DRAW: wymiana 2 kart u gracza1
        Player p1 = gameManager.getPlayer(playerId1);
        gameManager.processCommand(playerId1, new DrawCommand(List.of(0,1)));

        // DRAW: gracz2 wymienia 1 kartę
        Player p2 = gameManager.getPlayer(playerId2);
        gameManager.processCommand(playerId2, new DrawCommand(List.of(2)));

        // Po DRAW -> BET2
        assertEquals(GameState.BET2, gameManager.getCurrentState());

        // BET2: minimalne zakłady
        gameManager.processCommand(playerId1, new BetCommand("BET", 20));
        gameManager.processCommand(playerId2, new BetCommand("CALL", 0));

        // SHOWDOWN
        assertEquals(GameState.SHOWDOWN, gameManager.getCurrentState());

        // Wywołanie showDownPhase następuje automatycznie, po niej PAYOUT
        // Ostateczny koniec gry
        assertEquals(GameState.PAYOUT, gameManager.getCurrentState());
    }

    @Test
    void testDrawPhase_InvalidCardIndices() {
        Player p1 = gameManager.getPlayer(playerId1);

        // Próba wymiany 4 kart (max 3) powinna rzucić wyjątek
        assertThrows(Exception.class, () -> {
            gameManager.handleDrawCommand(playerId1, new DrawCommand(List.of(0,1,2,3)));
        });

        // Próba z powtarzającymi się indeksami
        assertThrows(Exception.class, () -> {
            gameManager.handleDrawCommand(playerId1, new DrawCommand(List.of(1,1)));
        });

        // Próba z indeksami poza zakresem
        assertThrows(Exception.class, () -> {
            gameManager.handleDrawCommand(playerId1, new DrawCommand(List.of(-1,5)));
        });
    }

    @Test
    void testDrawPhase_ExchangeZeroCards() {
        Player p1 = gameManager.getPlayer(playerId1);
        gameManager.processCommand(playerId1, new DrawCommand(List.of()));
        assertTrue(p1.isHasActed());
    }

    @Test
    void testBetting_MinimumRaiseValidation() {
        Player p1 = gameManager.getPlayer(playerId1);
        // Zakład za niski
        assertThrows(InvalidMoveException.class, () -> {
            gameManager.handleBettingCommand(playerId1, new BetCommand("BET", 5));
        });
    }

    @Test
    void testBetting_CallSuccess() throws InvalidMoveException {
        Player p1 = gameManager.getPlayer(playerId1);
        Player p2 = gameManager.getPlayer(playerId2);

        // Gracz1 stawia 20
        gameManager.handleBettingCommand(playerId1, new BetCommand("BET", 20));
        // Gracz2 wyrównuje
        gameManager.handleBettingCommand(playerId2, new BetCommand("CALL", 0));

        assertEquals(20, p2.getCurrentBet());
    }

    @Test
    void testFold_GameEndInstantly() throws InvalidMoveException {
        // Gracz1 FOLD
        gameManager.handleBettingCommand(playerId1, new BetCommand("FOLD", 0));
        // Pozostaje jeden aktywny gracz
        assertEquals(GameState.END, gameManager.getCurrentState());
    }

    @Test
    void testPlayerDisconnect_NotOnTurn_ForcesAdvance() throws InvalidMoveException {
        // Wywołanie disconnect innego gracza, który nie jest na ruchu
        gameManager.handlePlayerDisconnect(playerId2);

        // Gracz1 powinien być teraz na ruchu
        assertEquals(gameManager.getCurrentTurnPlayer(), gameManager.getPlayer(playerId1));
    }
}