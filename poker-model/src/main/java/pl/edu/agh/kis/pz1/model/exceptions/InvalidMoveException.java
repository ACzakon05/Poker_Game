package pl.edu.agh.kis.pz1.model.exceptions;

/**
 * Bazowy wyjątek dla błędów ruchu gracza, zawiera kod błędu do wysłania klientowi.
 */
public class InvalidMoveException extends RuntimeException {

    public final String code;

    public InvalidMoveException(String code, String message) {
        super(message);
        this.code = code;
    }
}