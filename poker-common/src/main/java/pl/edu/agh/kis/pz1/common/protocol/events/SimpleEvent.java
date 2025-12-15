package pl.edu.agh.kis.pz1.common.protocol.events;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.ServerEvent;

/**
 * Zdarzenie serwera: Proste powiadomienie ogólne.
 * Format: <TYPE> MESSAGE=<text>
 */
@Value
public class SimpleEvent implements ServerEvent {
    /** Typ wiadomości (np. END, STATUS, INFO). */
    String type;
    /** Treść wiadomości. */
    String message;
}