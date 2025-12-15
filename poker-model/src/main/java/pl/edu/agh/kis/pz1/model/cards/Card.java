package pl.edu.agh.kis.pz1.model.cards;

/**
 * Reprezentuje pojedynczą kartę. Użycie rekordu (record) jest ZALECANE,
 * ponieważ karta jest niezmiennym (immutable) obiektem wartości.
 */
public record Card(Rank rank, Suit suit) {

    /**
     * Zwraca skróconą, czytelną dla protokołu reprezentację karty (np. "AS" dla Asa Pik, "TD" dla 10 Karo).
     *
     * @return Skrót karty jako String.
     */
    public String toShortString() {
        return rank.getSymbol() + suit.getSymbol();
    }
}