package pl.edu.agh.kis.pz1.common.protocol.commands;
import pl.edu.agh.kis.pz1.common.protocol.commands.ClientCommand;

import lombok.Value;

/**
 * Reprezentacja komendy klienta: JOIN GAME=<gameld> NAME=<nick>[cite: 67].
 */
@Value // Automatyczne gettery, equals/hashCode i finalne pola
public class JoinCommand implements ClientCommand {
    String gameId;
    String nickname;
}