package pl.edu.agh.kis.pz1.common.protocol.events;

import lombok.Value;
import pl.edu.agh.kis.pz1.common.protocol.ServerEvent;

/**
 * Zdarzenie serwera: Ogłoszenie zwycięzcy i wysokości wygranej puli.
 * Format: PAYOUT WINNER=<playerId> AMOUNT=<n> CHIPS_NOW=<n>
 */
@Value
public class PayoutEvent implements ServerEvent {
    /** ID gracza, który wygrał pulę. */
    String winnerId;
    /** Kwota, jaką gracz wygrał w tej rundzie. */
    int amountWon;
    /** Całkowita liczba żetonów gracza po wypłacie. */
    int currentTotalChips;
}