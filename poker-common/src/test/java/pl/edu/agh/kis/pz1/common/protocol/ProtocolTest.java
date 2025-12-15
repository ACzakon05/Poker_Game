package pl.edu.agh.kis.pz1.common.protocol;

import org.junit.jupiter.api.Test;
import pl.edu.agh.kis.pz1.common.protocol.commands.*;
import pl.edu.agh.kis.pz1.common.protocol.events.*;
import pl.edu.agh.kis.pz1.model.cards.*;
import pl.edu.agh.kis.pz1.model.exceptions.*;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe dla Parsowania (Client->Server) i Kodowania (Server->Client).
 * KLUCZOWE dla spełnienia wymogu 70% pokrycia protokołu.
 */
public class ProtocolTest {

    private final CommandParser parser = new CommandParser();
    private final EventEncoder encoder = new EventEncoder();
    private static final String DUMMY_GAME_ID = "GAME_0";
    private static final String DUMMY_PLAYER_ID = "P1";

    // --- 1. TESTY PARSOWANIA KOMEND (Klient -> Serwer) ---

    @Test
    void testParseJoinCommand() {
        String raw = "JOIN GAME=GAME_0 NAME=Janusz";
        ClientCommand command = parser.parse(raw);

        assertTrue(command instanceof JoinCommand);
        JoinCommand joinCmd = (JoinCommand) command;
        assertEquals("GAME_0", joinCmd.getGameId());
        assertEquals("Janusz", joinCmd.getNickname());
    }

    @Test
    void testParseBetCommand() {
        String raw = "BET AMOUNT=100";
        ClientCommand command = parser.parse(raw);

        assertTrue(command instanceof BetCommand);
        BetCommand betCmd = (BetCommand) command;
        assertEquals("BET", betCmd.getActionType());
        assertEquals(100, betCmd.getAmount());
    }

    @Test
    void testParseSimpleCommand() {
        String raw = "FOLD";
        ClientCommand command = parser.parse(raw);

        // FOLD jest również parsowany jako BetCommand w uproszczonej logice
        assertTrue(command instanceof BetCommand);
        BetCommand foldCmd = (BetCommand) command;
        assertEquals("FOLD", foldCmd.getActionType());
    }

    @Test
    void testParseDrawCommand() {
        String raw = "DRAW CARDS=0,2,4";
        ClientCommand command = parser.parse(raw);

        assertTrue(command instanceof DrawCommand);
        DrawCommand drawCmd = (DrawCommand) command;
        assertEquals(List.of(0, 2, 4), drawCmd.getCardIndices());
    }

    @Test
    void testParseInvalidFormatThrowsProtocolException() {
        String raw = "JOIN GAME=GAME_0 NAME"; // Brakuje wartości dla NAME
        assertThrows(ProtocolException.class, () -> parser.parse(raw));
    }

    // --- 2. TESTY KODOWANIA ZDARZEŃ (Serwer -> Klient) ---

    @Test
    void testEncodeWelcomeEvent() {
        ServerEvent event = new WelcomeEvent(DUMMY_GAME_ID, DUMMY_PLAYER_ID);
        String expected = "WELCOME GAME=GAME_0 PLAYER=P1\n";

        String actual = encoder.encode(event, DUMMY_PLAYER_ID);
        assertEquals(expected, actual);
    }

    @Test
    void testEncodeTurnEvent() {
        ServerEvent event = new TurnEvent(DUMMY_PLAYER_ID, "BET1", 50, 10);
        String expected = "TURN PLAYER=P1 PHASE=BET1 CALL=50 MINRAISE=10\n";

        String actual = encoder.encode(event, DUMMY_PLAYER_ID);
        assertEquals(expected, actual);
    }

    @Test
    void testEncodeErrorEvent() {
        InvalidMoveException ex = new OutOfTurnException(); // CODE="OUT_OF_TURN"
        ServerEvent event = new ErrorEvent(ex);
        String expected = "ERR CODE=OUT_OF_TURN REASON=\"Ruch wykonany poza kolejnością.\"\n";

        String actual = encoder.encode(event, DUMMY_PLAYER_ID);
        assertEquals(expected, actual);
    }

    @Test
    void testEncodeDealEvent_ToOwner() {
        // Karty dla P1
        List<Card> hand = List.of(new Card(Rank.ACE, Suit.SPADES), new Card(Rank.KING, Suit.HEARTS));
        ServerEvent event = new DealEvent(DUMMY_PLAYER_ID, hand, "*****");

        // Kodowanie dla P1 (właściciela) -> wysyłamy jawne karty
        String actual = encoder.encode(event, DUMMY_PLAYER_ID);
        String expected = "DEAL PLAYER=P1 CARDS=AS,KH\n";
        assertEquals(expected, actual);
    }

    @Test
    void testEncodeDealEvent_ToOtherPlayer() {
        // Karty dla P1
        List<Card> hand = List.of(new Card(Rank.ACE, Suit.SPADES), new Card(Rank.KING, Suit.HEARTS));
        ServerEvent event = new DealEvent(DUMMY_PLAYER_ID, hand, "*****");

        // Kodowanie dla P2 (innego gracza) -> wysyłamy maskę
        String actual = encoder.encode(event, "P2");
        String expected = "DEAL PLAYER=P1 CARDS=*****\n";
        assertEquals(expected, actual);
    }
}