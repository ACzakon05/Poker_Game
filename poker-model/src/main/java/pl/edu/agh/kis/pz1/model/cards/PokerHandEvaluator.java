package pl.edu.agh.kis.pz1.model.cards;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Główna implementacja logiki oceny układów pokerowych (5-Card Draw).
 * Realizuje wzorzec Strategy z interfejsu HandEvaluator.
 */
public class PokerHandEvaluator implements HandEvaluator {

    private static final PokerHandEvaluator INSTANCE = new PokerHandEvaluator();

    public static PokerHandEvaluator getInstance() {
        return INSTANCE;
    }

    private PokerHandEvaluator() {
    }

    @Override
    public HandEvaluation evaluate(List<Card> hand) {
        if (hand == null || hand.size() != 5) {
            // W logice biznesowej rzucamy wyjątek, jeśli dane są niepoprawne
            throw new IllegalArgumentException("Ręka musi zawierać dokładnie 5 kart.");
        }

        // Kopiujemy i sortujemy malejąco: A, K, Q, J, 10, 9... 2
        List<Card> sortedHand = new ArrayList<>(hand);
        sortedHand.sort(Comparator.comparing(Card::rank).reversed());

        Map<Rank, Long> rankCounts = getRankCounts(sortedHand);
        Map<Suit, Long> suitCounts = getSuitCounts(sortedHand);

        boolean isFlush = isFlush(suitCounts);
        boolean isStraight = isStraight(sortedHand, rankCounts);

        // 1. Sprawdzenie najsilniejszych układów: Straight Flush
        if (isStraight && isFlush) {
            // Najwyższa karta decyduje o wartości strita.
            // W Poker Evaluation Builderze musisz uwzględnić, że najsilniejszy strit to ten z najwyższą kartą
            return HandEvaluation.builder()
                    .rank(HandRank.STRAIGHT_FLUSH)
                    .primaryRanks(List.of(sortedHand.getFirst().rank())) // Najwyższa karta strita
                    .kickerRanks(Collections.emptyList())
                    .build();
        }

        // 2. Sprawdzenie Cztery takie same, Full House (bazuje na countach)
        HandEvaluation countEvaluation = evaluateByCounts(sortedHand, rankCounts);
        if (countEvaluation != null) {
            return countEvaluation;
        }

        // 3. Sprawdzenie Flusha
        if (isFlush) {
            // Wszystkie 5 kart jako primary Ranks
            return HandEvaluation.builder()
                    .rank(HandRank.FLUSH)
                    .primaryRanks(extractRanks(sortedHand))
                    .kickerRanks(Collections.emptyList())
                    .build();
        }

        // 4. Sprawdzenie Straight
        if (isStraight) {
            // Najwyższa karta strita jako primary Rank
            return HandEvaluation.builder()
                    .rank(HandRank.STRAIGHT)
                    .primaryRanks(List.of(sortedHand.getFirst().rank()))
                    .kickerRanks(Collections.emptyList())
                    .build();
        }

        // 5. Ostatnie: Trójki, Pary i Wysoka Karta
        return evaluatePairsAndHighCard(sortedHand, rankCounts);
    }

    // --- METODY POMOCNICZE ---

    private List<Rank> extractRanks(List<Card> hand) {
        return hand.stream().map(Card::rank).collect(Collectors.toList());
    }

    private Map<Rank, Long> getRankCounts(List<Card> hand) {
        return hand.stream()
                .collect(Collectors.groupingBy(Card::rank, Collectors.counting()));
    }

    private Map<Suit, Long> getSuitCounts(List<Card> hand) {
        return hand.stream()
                .collect(Collectors.groupingBy(Card::suit, Collectors.counting()));
    }

    private boolean isFlush(Map<Suit, Long> suitCounts) {
        // Wszystkie karty mają ten sam kolor (tylko 1 unikalny kolor)
        return suitCounts.size() == 1;
    }

    private boolean isStraight(List<Card> sortedHand, Map<Rank, Long> rankCounts) {
        // Muszą być unikalne rangi i 5 kart musi tworzyć sekwencję.
        if (rankCounts.size() != 5) {
            return false;
        }

        // Przypadek Low Straight (A, 5, 4, 3, 2). As ma wartość 14.
        boolean isLowStraight = sortedHand.get(0).rank() == Rank.ACE &&
                sortedHand.get(1).rank() == Rank.FIVE &&
                sortedHand.get(2).rank() == Rank.FOUR &&
                sortedHand.get(3).rank() == Rank.THREE &&
                sortedHand.get(4).rank() == Rank.TWO;

        if (isLowStraight) {
            return true;
        }

        // Standardowy Strit (kolejne wartości)
        for (int i = 0; i < 4; i++) {
            // Sprawdzamy, czy następna karta ma rangę o 1 niższą
            if (sortedHand.get(i).rank().getPower() != sortedHand.get(i + 1).rank().getPower() + 1) {
                return false;
            }
        }
        return true;
    }

    // Obsługa 4-of-a-kind, Full House, 3-of-a-kind, Two Pair, Pair, High Card
    private HandEvaluation evaluateByCounts(List<Card> sortedHand, Map<Rank, Long> rankCounts) {
        List<Rank> ranks = extractRanks(sortedHand);
        // Mapowanie {Ranga -> Liczba Wystąpień}
        Map<Long, List<Rank>> countMap = rankCounts.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        // Najpierw sortujemy Rangi w obrębie każdej grupy (od najwyższej do najniższej),
        // aby ułatwić wybór "głównej" karty w remisie.
        countMap.values().forEach(list -> list.sort(Comparator.comparing(Rank::getPower).reversed()));

        List<Rank> quads = countMap.getOrDefault(4L, Collections.emptyList());
        List<Rank> trips = countMap.getOrDefault(3L, Collections.emptyList());
        List<Rank> pairs = countMap.getOrDefault(2L, Collections.emptyList());
        List<Rank> singles = countMap.getOrDefault(1L, Collections.emptyList());

        // 4. Four of a Kind
        if (!quads.isEmpty()) {
            // Kicker to jedyna karta pojedyncza (singles)
            return HandEvaluation.builder()
                    .rank(HandRank.FOUR_OF_A_KIND)
                    .primaryRanks(quads)
                    .kickerRanks(singles)
                    .build();
        }

        // 5. Full House (Trójka + Para)
        if (!trips.isEmpty() && !pairs.isEmpty()) {
            // Primary: Trójka, Sekundary: Para
            return HandEvaluation.builder()
                    .rank(HandRank.FULL_HOUSE)
                    .primaryRanks(trips) // Najważniejsza (wyższa trójka wygrywa)
                    .kickerRanks(pairs)  // Sekundarna (wyższa para wygrywa, jeśli trójki są równe)
                    .build();
        }

        // Wracamy, by resztę (Trips, Two Pair, Pair, High Card) obsłużyć w oddzielnej metodzie.
        return null;
    }

    private HandEvaluation evaluatePairsAndHighCard(List<Card> sortedHand, Map<Rank, Long> rankCounts) {
        List<Rank> ranks = extractRanks(sortedHand);
        Map<Long, List<Rank>> countMap = rankCounts.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
        countMap.values().forEach(list -> list.sort(Comparator.comparing(Rank::getPower).reversed()));

        List<Rank> trips = countMap.getOrDefault(3L, Collections.emptyList());
        List<Rank> pairs = countMap.getOrDefault(2L, Collections.emptyList());
        List<Rank> singles = countMap.getOrDefault(1L, Collections.emptyList());

        // 6. Three of a Kind
        if (!trips.isEmpty()) {
            // Kicker to pozostałe dwie karty (singles)
            return HandEvaluation.builder()
                    .rank(HandRank.THREE_OF_A_KIND)
                    .primaryRanks(trips) // Ranga trójki
                    .kickerRanks(singles) // Kickery (posortowane, najwyższy pierwszy)
                    .build();
        }

        // 7. Two Pair
        if (pairs.size() == 2) {
            // Główna ranga to obydwie pary (posortowane: wyższa para pierwsza)
            return HandEvaluation.builder()
                    .rank(HandRank.TWO_PAIR)
                    .primaryRanks(pairs)
                    .kickerRanks(singles) // Kicker to jedyna karta pojedyncza
                    .build();
        }

        // 8. One Pair
        if (pairs.size() == 1) {
            // Główna ranga to ranga pary
            return HandEvaluation.builder()
                    .rank(HandRank.PAIR)
                    .primaryRanks(pairs)
                    .kickerRanks(singles) // Kickery to pozostałe trzy karty
                    .build();
        }

        // 9. High Card (Wysoka Karta) - Domyślnie
        // Wszystkie 5 kart to kickery (posortowane)
        return HandEvaluation.builder()
                .rank(HandRank.HIGH_CARD)
                .primaryRanks(Collections.emptyList())
                .kickerRanks(ranks)
                .build();
    }
}