package pl.edu.agh.kis.pz1.model.player;

import lombok.Data;
import pl.edu.agh.kis.pz1.model.cards.Card;
import pl.edu.agh.kis.pz1.model.exceptions.NotEnoughChipsException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reprezentuje gracza w grze.
 * Adnotacja @Data z Lombok automatycznie generuje gettery, settery,
 * metody equals, hashCode oraz toString.
 */
@Data
public class Player {
    // Pola finalne dla niezmiennych identyfikatorów
    private final String id;
    private final String name;


    private int chips;
    // Ręka jest finalna, ale jej zawartość jest modyfikowana
    private final List<Card> hand = new ArrayList<>();
    private int currentBet;
    private boolean isFolded;
    private boolean hasActed;

    /**
     * Główny konstruktor do inicjalizacji gracza na starcie.
     */
    public Player(String id, String name, int startChips) {
        this.id = id;
        this.name = name;
        this.chips = startChips;
        this.currentBet = 0;
        this.isFolded = false;
        this.hasActed = false;
    }

    /**
     * Drugi konstruktor (używany np. w lobby).
     */
    public Player(String id, String name) {
        this(id, name, 0); // Wywołanie głównego konstruktora z 0 żetonami
    }

    /**
     * Dodaje kartę do ręki gracza.
     */
    public void receiveCard(Card card){
        hand.add(card);
    }

    /**
     * Dodaje listę kart do ręki gracza.
     */
    public void receiveCards(List<Card> cards){
        hand.addAll(cards);
    }

    /**
     * Resetuje stan gracza dla nowej rundy.
     */
    public void resetForNewRound(){
        this.hand.clear();
        this.currentBet = 0;
        this.isFolded = false;
        this.hasActed = false;
    }

    /**
     * Dodaje żetony (np. wygrana) do puli gracza.
     */
    public void addChips(int chips){
        this.chips += chips;
    }

    /**
     * Pobiera żetony od gracza (np. na zakład lub wpisowe ANTE).
     * Zamiast zwracania boolean i wypisywania błędu, rzuca wyjątek.
     * To jest preferowany sposób obsługi błędów w logice biznesowej serwera.
     *
     * @param chipsAmount Liczba żetonów do pobrania.
     * @throws NotEnoughChipsException jeśli gracz ma za mało żetonów.
     */
    public void takeChips(int chipsAmount){
        if (this.chips < chipsAmount){
            // Rzucenie wyjątku, który serwer przekształci na komunikat ERR do klienta
            throw new NotEnoughChipsException(
                    String.format("Gracz %s ma tylko %d żetonów, a próbuje postawić %d.", name, chips, chipsAmount)
            );
        }
        this.chips -= chipsAmount;
    }

    /**
     * Wymienia karty na podstawie indeksów.
     * Używane w GameManager.handleDrawCommand().
     * * @param indicesToReplace Lista indeksów (0-4) kart do usunięcia.
     * @param newCards Nowe karty do dodania.
     */
    public void replaceCards(List<Integer> indicesToReplace, List<Card> newCards) {
        // Sortujemy indeksy malejąco, aby usunięcie nie wpływało na kolejne indeksy
        List<Integer> sortedIndices = indicesToReplace.stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        // 1. Usuwamy stare karty
        for (int index : sortedIndices) {
            if (index >= 0 && index < hand.size()) {
                hand.remove(index);
            }
        }

        // 2. Dodajemy nowe karty
        hand.addAll(newCards);
    }

    /**
     * Pobiera żetony na Ante. Obsługuje All-in, jeśli żetonów brakuje.
     * Używane w GameManager.startAntePhase().
     *
     * @param ante Kwota Ante.
     * @return Rzeczywista zapłacona kwota (może być mniejsza niż Ante, jeśli All-in).
     */
    public int payAnte(int ante) throws NotEnoughChipsException {
        if (chips >= ante) {
            chips -= ante;
            return ante;
        } else {
            // All-in
            int allInAmount = chips;
            chips = 0;
            // Opcjonalnie: ustawienie flagi isAllIn na true
            return allInAmount;
        }
    }


}