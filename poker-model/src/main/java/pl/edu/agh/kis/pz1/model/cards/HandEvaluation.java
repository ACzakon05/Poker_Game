package pl.edu.agh.kis.pz1.model.cards;

import lombok.Builder;
import lombok.Value;

import java.util.Comparator;
import java.util.List;

/**
 * Reprezentuje wynik oceny ręki gracza.
 * Zawiera rangę układu oraz rangi kart użytych do rozstrzygnięcia remisów (kickery).
 */
@Value
@Builder
public class HandEvaluation implements Comparable<HandEvaluation> {

    /** Ranga układu (np. TWO_PAIR, FULL_HOUSE). */
    HandRank rank;

    /** Lista rang kart tworzących układ, posortowana malejąco (np. 8, 5 dla Full House 88855). */
    List<Rank> primaryRanks;

    /** Kickery (karty rozstrzygające remisy), posortowane malejąco. */
    List<Rank> kickerRanks;

    // Komparator używany do porównywania list rang (primaryRanks i kickerRanks)
    private static final CardRanksListComparator RANKS_COMPARATOR = new CardRanksListComparator();

    /**
     * Komparator statyczny do oceny dwóch układów pokerowych.
     * Kolejność porównania:
     * 1. Ranga Układu (np. Full House > Flush)
     * 2. Rangi Główne (np. 888 > 777 w Full House)
     * 3. Rangi Kickerów (np. Kicker A > Kicker K)
     */
    private static final Comparator<HandEvaluation> COMPARATOR = Comparator
            // Krok 1: Porównaj rangę układu (np. 8 vs 7)
            .comparing(HandEvaluation::getRank)

            // Krok 2: Porównaj Rangi Główne (np. 88855 vs 88844)
            .thenComparing(HandEvaluation::getPrimaryRanks, RANKS_COMPARATOR)

            // Krok 3: Porównaj Kickery (jeśli remisy w Kroku 1 i 2)
            .thenComparing(HandEvaluation::getKickerRanks, RANKS_COMPARATOR);

    @Override
    public int compareTo(HandEvaluation other) {
        return COMPARATOR.compare(this, other);
    }
}