package pl.edu.agh.kis.pz1.common.protocol.events;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.ServerEvent;

/**
 * Zdarzenie serwera: Potwierdzenie przyjęcia komendy.
 * Format: OK [MESSAGE=...]
 */
@Value
public class OkEvent implements ServerEvent {
    String message;
}