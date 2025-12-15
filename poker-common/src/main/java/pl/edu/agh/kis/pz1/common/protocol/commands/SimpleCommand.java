package pl.edu.agh.kis.pz1.common.protocol.commands;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.commands.ClientCommand;

/**
 * Reprezentuje proste komendy, które nie mają parametrów (np. START, QUIT, LEAVE).
 */
@Value
public class SimpleCommand implements ClientCommand {
    String action;
}