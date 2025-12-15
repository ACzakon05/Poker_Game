package pl.edu.agh.kis.pz1.common.protocol.events;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.ServerEvent;

/**
 * Zdarzenie serwera: Informuje, czyja jest kolej, w jakiej fazie i jakie są minimalne kwoty.
 * TURN PLAYER=<id> PHASE=<BET1 | DRAW | BET2> CALL=<n> MINRAISE=<n>
 */
@Value
public class TurnEvent implements ServerEvent {
    String playerId;
    String phase; // np. BET1, DRAW, BET2
    int callAmount; // Kwota do wyrównania (CALL)
    int minRaise; // Minimalna kwota podbicia
}