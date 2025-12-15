package pl.edu.agh.kis.pz1.model.exceptions;

public class IllegalDrawException extends InvalidMoveException {
    public IllegalDrawException(String message) {
        super("ILLEGAL_DRAW", message);
    }
}