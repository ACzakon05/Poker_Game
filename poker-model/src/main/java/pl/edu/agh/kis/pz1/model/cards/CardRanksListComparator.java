package pl.edu.agh.kis.pz1.model.cards;

import java.util.Comparator;
import java.util.List;

/**
 * Komparator używany do porównywania list rang (Rank), np. list kickerów,
 * lub rang tworzących układ (np. Trójka w Full House).
 * Porównuje elementy listę po liście, zaczynając od najwyższej wartości.
 */
public class CardRanksListComparator implements Comparator<List<Rank>> {

    @Override
    public int compare(List<Rank> ranks1, List<Rank> ranks2) {
        if (ranks1 == null && ranks2 == null) {
            return 0;
        }
        if (ranks1 == null) {
            return -1;
        }
        if (ranks2 == null) {
            return 1;
        }

        // Zakładamy, że listy rang są już posortowane malejąco
        int size = Math.min(ranks1.size(), ranks2.size());

        for (int i = 0; i < size; i++) {
            // Używamy siły rangi (Power) do porównania, a nie kolejności w enum
            int result = Integer.compare(ranks1.get(i).getPower(), ranks2.get(i).getPower());

            if (result != 0) {
                return result; // Znaleziono decydującą różnicę
            }
        }

        // Jeśli wszystkie porównane elementy są równe, wygrywa dłuższa lista
        return Integer.compare(ranks1.size(), ranks2.size());
    }
}