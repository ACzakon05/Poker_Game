package pl.edu.agh.kis.pz1.common.protocol.events;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.ServerEvent;
import pl.edu.agh.kis.pz1.model.exceptions.InvalidMoveException;

/**
 * Zdarzenie serwera: Błąd, wysyłane po nieprawidłowym ruchu/protokole.
 * Format: ERR CODE=<code> REASON=<text>
 */
@Value
public class ErrorEvent implements ServerEvent {
    InvalidMoveException exception;
}