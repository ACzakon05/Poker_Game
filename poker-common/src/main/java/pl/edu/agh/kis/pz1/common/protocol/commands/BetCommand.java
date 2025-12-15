package pl.edu.agh.kis.pz1.common.protocol.commands;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.commands.ClientCommand;

/**
 * Reprezentacja komendy klienta dla akcji licytacji:
 * BET AMOUNT=<n> | CALL | CHECK | FOLD .
 */
@Value
public class BetCommand implements ClientCommand {

    /** Typ akcji: BET, CALL, CHECK, FOLD. */
    String actionType;

    /** Kwota zakładu (wymagana tylko dla akcji BET). */
    int amount;

    /**
     * Konstruktor dla akcji BET/RAISE.
     */
    public BetCommand(String actionType, int amount) {
        this.actionType = actionType;
        this.amount = amount;
    }

    /**
     * Konstruktor dla akcji CALL/CHECK/FOLD (gdzie kwota jest nieważna).
     */
    public BetCommand(String actionType) {
        this(actionType, 0);
    }
}