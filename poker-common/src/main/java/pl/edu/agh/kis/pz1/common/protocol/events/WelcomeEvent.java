package pl.edu.agh.kis.pz1.common.protocol.events;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.ServerEvent;

/**
 * Zdarzenie serwera: Powitanie i przydzielenie ID.
 * Format: WELCOME GAME=<gameld> PLAYER=<playerId>
 */
@Value
public class WelcomeEvent implements ServerEvent {
    String gameId;
    String playerId;
}