package pl.edu.agh.kis.pz1.model.cards;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reprezentuje talię kart. Pozwala tworzyć, tasować oraz pobierać karty.
 * Talia zawiera standardowo 52 karty.
 */
public class Deck {
    private final List<Card> cards;
    // Użycie SecureRandom
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Tworzy nową talię 52 kart i natychmiast ją tasuje.
     */
    public Deck() {
        this.cards = shuffle(factoryDeck());
    }

    /**
     * Tworzy talię na podstawie istniejącej listy kart (np. dla testów).
     * @param cards Lista kart.
     */
    public Deck(List<Card> cards) {
        this.cards = new ArrayList<>(cards);
    }

    /**
     * Generuje standardową, niepotasowaną talię 52 kart.
     * @return Lista 52 kart.
     */
    public static List<Card> factoryDeck() {
        List<Card> deck = new ArrayList<>();
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) {
                // Używamy zaktualizowanego konstruktora (rank, suit)
                deck.add(new Card(r, s));
            }
        }
        return deck;
    }

    /**
     * Tasuje podaną listę kart, używając SecureRandom.
     *
     * @param deck Lista kart do tasowania.
     * @return Nowa, potasowana lista kart.
     */
    public static List<Card> shuffle(List<Card> deck) {
        List<Card> shuffled = new ArrayList<>(deck);
        Collections.shuffle(shuffled, RANDOM); // Tasowanie SecureRandom [cite: 150]
        return shuffled;
    }

    /**
     * Dobiera i usuwa ostatnią kartę z talii.
     *
     * @return Dobrana karta.
     * @throws IllegalStateException jeśli talia jest pusta.
     */
    public Card draw() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Deck is empty");
        }
        // Użycie removeLast() jest wydajne, jeśli karty są w kolejności do dobierania
        return cards.removeLast();
    }

    /**
     * Dobiera określoną liczbę kart (np. dla ręki gracza lub wymiany).
     *
     * @param n Liczba kart do dobrania.
     * @return Lista dobranych kart.
     * @throws IllegalArgumentException jeśli w talii jest za mało kart.
     */
    public List<Card> draw(int n) {
        if (n > cards.size()) {
            throw new IllegalArgumentException("Not enough cards in deck");
        }
        List<Card> hand = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            hand.add(draw());
        }
        return hand;
    }

    /**
     * Wyczyść talię i napełnij ją nową, potasowaną talią 52 kart.
     */
    public void reset() {
        cards.clear();
        cards.addAll(shuffle(factoryDeck()));
    }

    public int size() {
        return cards.size();
    }

    public List<Card> getCards() {
        // Zwracamy kopię, aby uniemożliwić modyfikację wewnętrznej listy z zewnątrz
        return new ArrayList<>(cards);
    }
}