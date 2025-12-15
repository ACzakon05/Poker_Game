package pl.edu.agh.kis.pz1.model.player;

/**
 * Definiuje stałe identyfikatory dla graczy (P1-P4) i Serwera.
 * Używane głównie w protokole komunikacyjnym i mapowaniu sesji.
 */
public enum PlayerID {

    P1("P1"),
    P2("P2"),
    P3("P3"),
    P4("P4"),

    SERVER("SERVER");

    private final String id;

    PlayerID(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * Metoda pomocnicza do mapowania Stringa z tokenu na obiekt enum.
     */
    public static PlayerID fromString(String text) {
        for (PlayerID b : PlayerID.values()) {
            if (b.id.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Nieznane ID Gracza: " + text);
    }
}