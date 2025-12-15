package pl.edu.agh.kis.pz1.model.cards;

import java.util.Comparator;

/**
 * Enum reprezentujący rangę (siłę) układu pokerowego.
 * Wartości są posortowane od najniższej (HIGH_CARD) do najwyższej (ROYAL_FLUSH).
 * Wymagane przez wytyczne (Lab 7, pkt 6).
 */
public enum HandRank {
    HIGH_CARD(0),
    PAIR(1),
    TWO_PAIR(2),
    THREE_OF_A_KIND(3),
    STRAIGHT(4),
    FLUSH(5),
    FULL_HOUSE(6),
    FOUR_OF_A_KIND(7),
    STRAIGHT_FLUSH(8);

    private final int value;

    HandRank(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}