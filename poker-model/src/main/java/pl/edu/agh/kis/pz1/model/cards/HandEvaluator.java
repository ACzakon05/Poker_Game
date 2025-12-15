package pl.edu.agh.kis.pz1.model.cards;

import java.util.List;

/**
 * Interfejs dla logiki oceny układów (wzorzec Strategy).
 * Jest to rdzeń logiki biznesowej, wymagający wysokiego pokrycia testami.
 */
public interface HandEvaluator {

    /**
     * Ocenia 5 kart i zwraca wynik.
     * @param hand Lista pięciu kart.
     * @return Obiekt HandEvaluation zawierający rangę układu i kickery.
     */
    HandEvaluation evaluate(List<Card> hand);
}