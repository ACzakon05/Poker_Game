package pl.edu.agh.kis.pz1.common.protocol.events;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.ServerEvent;
import pl.edu.agh.kis.pz1.model.cards.Card;
import java.util.List;

/**
 * Zdarzenie serwera: Informuje o rozdaniu kart.
 * Format: DEAL PLAYER=<id> CARDS=<mask | cards>
 */
@Value
public class DealEvent implements ServerEvent {
    String playerId;

    // Lista kart dla właściciela wiadomości (jawne karty)
    List<Card> cards;

    // Maska dla wiadomości broadcastowanej do innych (np. "*****")
    String mask;
}