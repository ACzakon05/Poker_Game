package pl.edu.agh.kis.pz1.common.protocol;

import pl.edu.agh.kis.pz1.common.protocol.events.*;
import pl.edu.agh.kis.pz1.model.cards.Card;
import pl.edu.agh.kis.pz1.model.exceptions.InvalidMoveException;
import pl.edu.agh.kis.pz1.model.exceptions.ProtocolException;
import pl.edu.agh.kis.pz1.model.player.PlayerID;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Klasa odpowiedzialna za kodowanie obiektów ServerEvent na komunikat tekstowy
 * zgodny z protokołem (np. WELCOME GAME=... \n). Używa identyfikatorów graczy (ID)
 * zamiast nazw.
 */
public class EventEncoder {

    private static final String SERVER_ID = PlayerID.SERVER.getId();
    private static final String SEPARATOR = " ";
    private static final String LINE_END = "\n";

    /**
     * Koduje obiekt ServerEvent na format protokołu tekstowego.
     * * @param event Zdarzenie serwera.
     * @param targetPlayerId ID gracza, do którego wiadomość jest kierowana (ważne dla DealEvent).
     * @return Zakodowany String z zakończeniem linii.
     * @throws ProtocolException jeśli nie można zakodować typu eventu.
     */
    // USUNIĘTO ARGUMENT MAP<STRING, STRING> PLAYERSIDTONAME
    public String encode(ServerEvent event, String targetPlayerId) throws ProtocolException {

        String actionName = event.getClass().getSimpleName().replace("Event", "").toUpperCase();

        String payload = switch (event) {
            case OkEvent ok -> String.format("MESSAGE=%s", ok.getMessage());
            case ErrorEvent err -> encodeError(err.getException());
            case WelcomeEvent welcome -> encodeWelcome(welcome);

            // Zaktualizowane wywołania: Używamy tylko event i, jeśli potrzebne, targetPlayerId
            case TurnEvent turn -> encodeTurn(turn);
            case DealEvent deal -> encodeDeal(deal, targetPlayerId);
            case ActionPlayerEvent actionPlayer -> encodeActionPlayer(actionPlayer);
            case ShowdownEvent showdown -> encodeShowdown(showdown);
            case PayoutEvent payout -> encodePayout(payout);

            case SimpleEvent simple -> encodeSimple(simple);

            default -> throw new ProtocolException("Nieznany typ zdarzenia do kodowania: " + event.getClass().getSimpleName());
        };

        // Format: ACTION [PAYLOAD] + \n
        return actionName + SEPARATOR + payload + LINE_END;
    }

    // --- Metody Kodowania Specyficznych Eventów (UŻYWAJĄ TYLKO ID) ---

    private String encodeError(InvalidMoveException ex) {
        // Protokół błędu: ERR CODE=<code> REASON=<text>
        return String.format("CODE=%s REASON=\"%s\"", ex.code, ex.getMessage().replace("\"", "'"));
    }

    private String encodeWelcome(WelcomeEvent event) {
        // WELCOME GAME=<gameld> PLAYER=<playerId>
        return String.format("GAME=%s PLAYER=%s",
                event.getGameId(), event.getPlayerId());
    }

    // ZMIENIONA SYGNATURA: Usuwamy Mapę, używamy event.getPlayerId()
    private String encodeTurn(TurnEvent event) {
        String playerId = event.getPlayerId();

        // TURN PLAYER=<id> PHASE=%s CALL=%d MINRAISE=%d
        return String.format("PLAYER=%s PHASE=%s CALL=%d MINRAISE=%d",
                playerId, event.getPhase(), event.getCallAmount(), event.getMinRaise());
    }

    // ZMIENIONA SYGNATURA: Usuwamy Mapę, używamy event.getPlayerId()
    private String encodeDeal(DealEvent event, String targetPlayerId) {
        // 1. Używamy ID gracza
        String playerId = event.getPlayerId();

        // 2. Logika wyświetlania kart / maski
        String cardRepresentation;

        // Jeśli wiadomość jest kierowana do gracza, którego karty dotyczą, wysyłamy jawne karty.
        if (event.getPlayerId().equals(targetPlayerId)) {
            // Jawne karty: "AS,TD,7H,7C,2S"
            cardRepresentation = event.getCards().stream()
                    .map(Card::toShortString)
                    .collect(Collectors.joining(","));
        } else {
            // Do pozostałych graczy wysyłamy maskę: "*****"
            cardRepresentation = event.getMask();
        }

        // DEAL PLAYER=<id> CARDS=<mask | cards>
        return String.format("PLAYER=%s CARDS=%s",
                playerId, cardRepresentation);
    }

    // ZMIENIONA SYGNATURA: Usuwamy Mapę, używamy event.getPlayerId()
    private String encodeActionPlayer(ActionPlayerEvent event) {
        String playerId = event.getPlayerId();

        // ACTION PLAYER=<id> TYPE=<BET|CALL | CHECK | FOLD | DRAW> [ARGS=...]
        String args = event.getArguments() != null && !event.getArguments().isBlank()
                ? SEPARATOR + event.getArguments()
                : "";

        return String.format("PLAYER=%s TYPE=%s%s",
                playerId, event.getActionType(), args);
    }

    // ZMIENIONA SYGNATURA: Usuwamy Mapę, używamy event.getPlayerId()
    private String encodeShowdown(ShowdownEvent event) {
        // 1. Używamy ID gracza
        String playerId = event.getPlayerId();

        // 2. Kodowanie kart
        String cards = event.getHand().stream()
                .map(Card::toShortString)
                .collect(Collectors.joining(","));

        // SHOWDOWN PLAYER=<id> HAND=<cards> RANK=<rankString>
        return String.format("PLAYER=%s HAND=%s RANK=\"%s\"",
                playerId, cards, event.getRankString().replace("\"", "'"));
    }

    // ZMIENIONA SYGNATURA: Usuwamy Mapę, używamy event.getWinnerId()
    private String encodePayout(PayoutEvent event) {
        String winnerId = event.getWinnerId();

        // PAYOUT WINNER=<id> AMOUNT=%d CHIPS_NOW=%d
        return String.format("WINNER=%s AMOUNT=%d CHIPS_NOW=%d",
                winnerId, event.getAmountWon(), event.getCurrentTotalChips());
    }

    private String encodeSimple(SimpleEvent event) {
        // SIMPLE MESSAGE=<text>
        return String.format("MESSAGE=\"%s\"", event.getMessage().replace("\"", "'"));
    }

    // USUNIĘTO METODĘ getPlayerName
}