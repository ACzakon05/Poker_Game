package pl.edu.agh.kis.pz1.model.exceptions;

/**
 * Wyjątek rzucany, gdy gracz próbuje postawić więcej żetonów, niż posiada.
 * Dziedziczy po RuntimeException, aby uniknąć konieczności deklarowania go w sygnaturze metod.

 */
public class NotEnoughChipsException extends RuntimeException {
    public NotEnoughChipsException(String message) {
        super(message);
    }
}