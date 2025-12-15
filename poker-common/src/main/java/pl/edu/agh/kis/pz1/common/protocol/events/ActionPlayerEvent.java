package pl.edu.agh.kis.pz1.common.protocol.events;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.ServerEvent;

/**
 * Zdarzenie serwera: Informuje o ruchu gracza.
 * Format: ACTION PLAYER=<id> TYPE=<BET|CALL|CHECK|FOLD|DRAW> [ARGS=...]
 */
@Value
public class ActionPlayerEvent implements ServerEvent {
    String playerId;
    String actionType; // np. BET, CALL, FOLD
    String arguments; // Dodatkowe argumenty, np. AMOUNT=100
}