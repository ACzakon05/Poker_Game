package pl.edu.agh.kis.pz1.common.protocol.commands;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.commands.ClientCommand;

/**
 * Reprezentacja komendy klienta: CREATE ANTE=<n> BET=<n> LIMIT=FIXED.
 */
@Value
public class CreateCommand implements ClientCommand {

    int anteAmount;
    int fixedBetAmount;
    // String limitType; // Można dodać, jeśli implementujemy inne limity niż FIXED
}