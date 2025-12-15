package pl.edu.agh.kis.pz1.model.game;

/**
 * Reprezentuje stany maszyny stanów serwera w cyklu pojedynczej rundy.
 * Zgodnie z wytycznymi: LOBBY -> ANTE -> DEAL -> BET1 -> DRAW -> BET2 -> SHOWDOWN -> PAYOUT -> END
 */
public enum GameState {
    LOBBY,
    ANTE, // Wpłata wpisowego (ANTE REQUEST) [cite: 67]
    DEAL, // Rozdanie kart [cite: 69]
    BET1, // Pierwsza runda zakładów [cite: 71]
    DRAW, // Runda wymiany kart (do 3) [cite: 74]
    BET2, // Druga runda zakładów [cite: 75]
    SHOWDOWN, // Porównanie układów [cite: 77]
    PAYOUT, // Wypłata puli [cite: 79]
    END // Zakończenie rundy [cite: 81]
}