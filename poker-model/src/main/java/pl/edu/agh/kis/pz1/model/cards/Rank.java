package pl.edu.agh.kis.pz1.model.cards;

public enum Rank {
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("T", 10),
    JACK("J", 11),
    QUEEN("Q", 12),
    KING("K", 13),
    ACE("A", 14);

    private final String symbol;
    private final int power;

    Rank(String symbol, int power) {
        this.symbol = symbol;
        this.power = power;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getPower() {
        return power;
    }
}