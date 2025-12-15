package pl.edu.agh.kis.pz1.common.protocol.commands;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.commands.ClientCommand;

import java.util.List;

/**
 * Reprezentacja komendy klienta dla wymiany kart: DRAW CARDS=<i,j,k>.
 */
@Value
public class DrawCommand implements ClientCommand {

    /** Lista indeksów kart (0-4) do wymiany. */
    List<Integer> cardIndices;
}