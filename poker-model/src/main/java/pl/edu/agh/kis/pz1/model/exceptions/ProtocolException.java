package pl.edu.agh.kis.pz1.model.exceptions;

public class ProtocolException extends InvalidMoveException {
    public ProtocolException(String message) {
        super("PROTOCOL_ERR", "Nieprawidłowy format komunikatu: " + message);
    }
}