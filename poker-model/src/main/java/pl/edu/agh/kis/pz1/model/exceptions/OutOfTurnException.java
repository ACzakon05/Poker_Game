package pl.edu.agh.kis.pz1.model.exceptions;

public class OutOfTurnException extends InvalidMoveException {
    public OutOfTurnException() {
        super("OUT_OF_TURN", "Ruch wykonany poza kolejnością.");
    }
}