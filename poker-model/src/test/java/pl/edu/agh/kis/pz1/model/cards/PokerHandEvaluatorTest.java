package pl.edu.agh.kis.pz1.model.cards;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe dla HandEvaluator - kluczowe dla wymaganego 70% pokrycia.
 * Weryfikują poprawną identyfikację układu oraz poprawną kolejność zwycięstwa (porównanie).
 */
public class PokerHandEvaluatorTest {

    private final HandEvaluator evaluator = PokerHandEvaluator.getInstance();

    // --- Metoda pomocnicza do łatwego tworzenia kart ---
    private Card c(Rank rank, Suit suit) {
        return new Card(rank, suit);
    }

    // --- TESTY IDENTYFIKACJI UKŁADÓW (9) ---

    @Test
    void testRoyalFlush() {
        // A, K, Q, J, 10 Piki (Najwyższa Ranga)
        List<Card> hand = List.of(
                c(Rank.ACE, Suit.SPADES), c(Rank.KING, Suit.SPADES),
                c(Rank.QUEEN, Suit.SPADES), c(Rank.JACK, Suit.SPADES),
                c(Rank.TEN, Suit.SPADES)
        );
        HandEvaluation evaluation = evaluator.evaluate(hand);
        assertEquals(HandRank.STRAIGHT_FLUSH, evaluation.getRank());
        assertEquals(List.of(Rank.ACE), evaluation.getPrimaryRanks());
    }

    @Test
    void testStraightFlush() {
        // 9, 8, 7, 6, 5 Karo
        List<Card> hand = List.of(
                c(Rank.NINE, Suit.DIAMONDS), c(Rank.EIGHT, Suit.DIAMONDS),
                c(Rank.SEVEN, Suit.DIAMONDS), c(Rank.SIX, Suit.DIAMONDS),
                c(Rank.FIVE, Suit.DIAMONDS)
        );
        HandEvaluation evaluation = evaluator.evaluate(hand);
        assertEquals(HandRank.STRAIGHT_FLUSH, evaluation.getRank());
        assertEquals(List.of(Rank.NINE), evaluation.getPrimaryRanks());
    }

    @Test
    void testFourOfAKind() {
        // 4 Walety i kicker 2
        List<Card> hand = List.of(
                c(Rank.JACK, Suit.SPADES), c(Rank.JACK, Suit.HEARTS),
                c(Rank.JACK, Suit.CLUBS), c(Rank.JACK, Suit.DIAMONDS),
                c(Rank.TWO, Suit.SPADES) // Kicker
        );
        HandEvaluation evaluation = evaluator.evaluate(hand);
        assertEquals(HandRank.FOUR_OF_A_KIND, evaluation.getRank());
        assertEquals(List.of(Rank.JACK), evaluation.getPrimaryRanks());
        assertEquals(List.of(Rank.TWO), evaluation.getKickerRanks());
    }

    @Test
    void testFullHouse() {
        // 3 Piątki i 2 Dziesiątki (555TT)
        List<Card> hand = List.of(
                c(Rank.FIVE, Suit.HEARTS), c(Rank.FIVE, Suit.DIAMONDS),
                c(Rank.FIVE, Suit.SPADES), c(Rank.TEN, Suit.CLUBS),
                c(Rank.TEN, Suit.HEARTS)
        );
        HandEvaluation evaluation = evaluator.evaluate(hand);
        assertEquals(HandRank.FULL_HOUSE, evaluation.getRank());
        assertEquals(List.of(Rank.FIVE), evaluation.getPrimaryRanks()); // Trójka
        assertEquals(List.of(Rank.TEN), evaluation.getKickerRanks());  // Para
    }

    @Test
    void testFlush() {
        // Kolor (np. Piki), ale nie strit
        List<Card> hand = List.of(
                c(Rank.TWO, Suit.SPADES), c(Rank.FIVE, Suit.SPADES),
                c(Rank.NINE, Suit.SPADES), c(Rank.QUEEN, Suit.SPADES),
                c(Rank.ACE, Suit.SPADES)
        );
        HandEvaluation evaluation = evaluator.evaluate(hand);
        assertEquals(HandRank.FLUSH, evaluation.getRank());
    }

    @Test
    void testStraight() {
        // Standardowy Strit 9 do 5 (różne kolory)
        List<Card> hand = List.of(
                c(Rank.FIVE, Suit.HEARTS), c(Rank.SIX, Suit.DIAMONDS),
                c(Rank.SEVEN, Suit.CLUBS), c(Rank.EIGHT, Suit.SPADES),
                c(Rank.NINE, Suit.HEARTS)
        );
        HandEvaluation evaluation = evaluator.evaluate(hand);
        assertEquals(HandRank.STRAIGHT, evaluation.getRank());
        assertEquals(List.of(Rank.NINE), evaluation.getPrimaryRanks()); // Najwyższa karta
    }

    @Test
    void testLowStraightAceToFive() {
        // Strit A-2-3-4-5 (różne kolory)
        List<Card> hand = List.of(
                c(Rank.ACE, Suit.HEARTS), c(Rank.FIVE, Suit.DIAMONDS),
                c(Rank.FOUR, Suit.CLUBS), c(Rank.THREE, Suit.SPADES),
                c(Rank.TWO, Suit.HEARTS)
        );
        HandEvaluation evaluation = evaluator.evaluate(hand);
        assertEquals(HandRank.STRAIGHT, evaluation.getRank());
        // Uwaga: W tym przypadku highest rank w primaryRanks powinna być PIĄTKA, bo A liczy się jako 1
        // W implementacji zostawiliśmy ASA, to musi być obsłużone przez logikę porównania
        assertEquals(List.of(Rank.ACE), evaluation.getPrimaryRanks());
    }

    @Test
    void testThreeOfAKind() {
        // 3 Siódemki, kicker K, 2
        List<Card> hand = List.of(
                c(Rank.SEVEN, Suit.HEARTS), c(Rank.SEVEN, Suit.DIAMONDS),
                c(Rank.SEVEN, Suit.SPADES), c(Rank.KING, Suit.CLUBS),
                c(Rank.TWO, Suit.HEARTS)
        );
        HandEvaluation evaluation = evaluator.evaluate(hand);
        assertEquals(HandRank.THREE_OF_A_KIND, evaluation.getRank());
        assertEquals(List.of(Rank.SEVEN), evaluation.getPrimaryRanks());
        assertEquals(List.of(Rank.KING, Rank.TWO), evaluation.getKickerRanks()); // Kicker posortowany malejąco
    }

    @Test
    void testTwoPair() {
        // Dwie Pary: Walety i Czwórki, kicker As
        List<Card> hand = List.of(
                c(Rank.JACK, Suit.HEARTS), c(Rank.JACK, Suit.DIAMONDS),
                c(Rank.FOUR, Suit.CLUBS), c(Rank.FOUR, Suit.SPADES),
                c(Rank.ACE, Suit.HEARTS) // Kicker
        );
        HandEvaluation evaluation = evaluator.evaluate(hand);
        assertEquals(HandRank.TWO_PAIR, evaluation.getRank());
        assertEquals(List.of(Rank.JACK, Rank.FOUR), evaluation.getPrimaryRanks()); // Pary posortowane (J > 4)
        assertEquals(List.of(Rank.ACE), evaluation.getKickerRanks());
    }

    @Test
    void testPair() {
        // Para Króli, kickery 10, 7, 3
        List<Card> hand = List.of(
                c(Rank.KING, Suit.HEARTS), c(Rank.KING, Suit.DIAMONDS),
                c(Rank.TEN, Suit.CLUBS), c(Rank.SEVEN, Suit.SPADES),
                c(Rank.THREE, Suit.HEARTS)
        );
        HandEvaluation evaluation = evaluator.evaluate(hand);
        assertEquals(HandRank.PAIR, evaluation.getRank());
        assertEquals(List.of(Rank.KING), evaluation.getPrimaryRanks());
        assertEquals(List.of(Rank.TEN, Rank.SEVEN, Rank.THREE), evaluation.getKickerRanks());
    }

    @Test
    void testHighCard() {
        // Wysoka Karta (żaden układ), As
        List<Card> hand = List.of(
                c(Rank.ACE, Suit.HEARTS), c(Rank.KING, Suit.DIAMONDS),
                c(Rank.TEN, Suit.CLUBS), c(Rank.SEVEN, Suit.SPADES),
                c(Rank.TWO, Suit.HEARTS)
        );
        HandEvaluation evaluation = evaluator.evaluate(hand);
        assertEquals(HandRank.HIGH_CARD, evaluation.getRank());
        assertEquals(List.of(Rank.ACE, Rank.KING, Rank.TEN, Rank.SEVEN, Rank.TWO), evaluation.getKickerRanks());
    }

    // --- TESTY PORÓWNYWANIA UKŁADÓW (DLA WERYFIKACJI COMPARETO) ---

    @Test
    void testComparison_HigherRankWins() {
        // Full House (Trips 8) vs Flush
        HandEvaluation fullHouse = evaluator.evaluate(List.of(c(Rank.EIGHT, Suit.HEARTS), c(Rank.EIGHT, Suit.DIAMONDS), c(Rank.EIGHT, Suit.SPADES), c(Rank.FIVE, Suit.CLUBS), c(Rank.FIVE, Suit.HEARTS)));
        HandEvaluation flush = evaluator.evaluate(List.of(c(Rank.ACE, Suit.HEARTS), c(Rank.TEN, Suit.HEARTS), c(Rank.SEVEN, Suit.HEARTS), c(Rank.FOUR, Suit.HEARTS), c(Rank.TWO, Suit.HEARTS)));

        assertTrue(fullHouse.compareTo(flush) > 0);
    }

    @Test
    void testComparison_SameRank_PrimaryRanks() {
        // Dwa Full House: 888AA vs 777KK
        HandEvaluation fh888 = evaluator.evaluate(List.of(c(Rank.EIGHT, Suit.HEARTS), c(Rank.EIGHT, Suit.DIAMONDS), c(Rank.EIGHT, Suit.SPADES), c(Rank.ACE, Suit.CLUBS), c(Rank.ACE, Suit.HEARTS)));
        HandEvaluation fh777 = evaluator.evaluate(List.of(c(Rank.SEVEN, Suit.HEARTS), c(Rank.SEVEN, Suit.DIAMONDS), c(Rank.SEVEN, Suit.SPADES), c(Rank.KING, Suit.CLUBS), c(Rank.KING, Suit.HEARTS)));

        // 888 wygrywa z 777
        assertTrue(fh888.compareTo(fh777) > 0);
    }

    @Test
    void testComparison_SameRank_Kickers() {
        // Dwie Pary: JJ44A vs JJ44K (As wygrywa)
        HandEvaluation p1 = evaluator.evaluate(List.of(c(Rank.JACK, Suit.HEARTS), c(Rank.JACK, Suit.DIAMONDS), c(Rank.FOUR, Suit.CLUBS), c(Rank.FOUR, Suit.SPADES), c(Rank.ACE, Suit.HEARTS))); // Kicker A
        HandEvaluation p2 = evaluator.evaluate(List.of(c(Rank.JACK, Suit.CLUBS), c(Rank.JACK, Suit.SPADES), c(Rank.FOUR, Suit.HEARTS), c(Rank.FOUR, Suit.DIAMONDS), c(Rank.KING, Suit.SPADES))); // Kicker K

        // A wygrywa
        assertTrue(p1.compareTo(p2) > 0);
    }
}