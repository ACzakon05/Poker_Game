package pl.edu.agh.kis.pz1.common.protocol.commands;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.commands.ClientCommand;

/**
 * [cite_start]Reprezentacja komendy klienta: HELLO VERSION=<semver>[cite: 65].
 */
@Value
public class HelloCommand implements ClientCommand {

    /** Wersja protokołu/klienta. */
    String version;
}