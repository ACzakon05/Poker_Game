package pl.edu.agh.kis.pz1.common.protocol.events;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.ServerEvent;
import pl.edu.agh.kis.pz1.model.cards.Card;
import java.util.List;

/**
 * Zdarzenie serwera: Pokazanie kart w Showdown.
 * Format: SHOWDOWN PLAYER=<id> HAND=<cards> RANK=<rankString>
 */
@Value
public class ShowdownEvent implements ServerEvent {
    String playerId;
    List<Card> hand; // Jawne karty gracza
    String rankString; // Opis układu (np. "Two Pair, Aces and Kings")
}