package pl.edu.agh.kis.pz1.common.protocol.commands;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.commands.ClientCommand;

/**
 * Reprezentacja komendy klienta: START.
 * Używana w fazie LOBBY do rozpoczęcia nowej rundy.
 */
@Value // Automatyczne gettery, equals/hashCode i finalne pola
public class StartCommand implements ClientCommand {

    // Klasa jest pusta, ponieważ komenda START nie ma parametrów.
}