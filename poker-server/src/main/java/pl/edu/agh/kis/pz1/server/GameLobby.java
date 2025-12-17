package pl.edu.agh.kis.pz1.server;

import pl.edu.agh.kis.pz1.model.exceptions.InvalidMoveException;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Globalny menedżer gier i lobby. Utrzymuje listę aktywnych gier.
 */
public class GameLobby {

    // Mapa aktywnych gier: GameId -> GameManager
    private final ConcurrentHashMap<String, GameManager> activeGames = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, String> playerToGameMap = new ConcurrentHashMap<>();

    private static final GameLobby INSTANCE = new GameLobby();

    private GameLobby() {}

    public static GameLobby getInstance() {
        return INSTANCE;
    }

    /**
     * Tworzy nową grę i dodaje ją do lobby.
     * @param maxPlayers Maksymalna liczba graczy.
     * @return Nowa instancja GameManager.
     */
    public GameManager createNewGame(int maxPlayers) {
        // Uproszczone generowanie ID gry
        String newGameId = "GAME_" + activeGames.size();
        GameManager game = new GameManager(newGameId, maxPlayers);
        activeGames.put(newGameId, game);
        System.out.println("Utworzono nową grę: " + newGameId + " (max: " + maxPlayers + ")");
        return game;
    }

    /**
     * Pobiera GameManagera po ID.
     */
    public GameManager getGame(String gameId) {
        return activeGames.get(gameId);
    }

    /**
     * Usuwa grę z lobby (np. po zakończeniu).
     */
    public void removeGame(String gameId) {
        activeGames.remove(gameId);
        playerToGameMap.values().removeIf(id -> id.equals(gameId));
    }

    /**
     * Rejestruje gracza do gry (używane przez GameManager po udanym JOIN).
     * @param playerId ID gracza.
     * @param gameId ID gry.
     */
    public void registerPlayer(String playerId, String gameId) throws InvalidMoveException {
        if (playerToGameMap.containsKey(playerId)) {
            throw new InvalidMoveException("ALREADY_IN_GAME", "Gracz już jest przypisany do innej gry.");
        }
        playerToGameMap.put(playerId, gameId);
    }

    /**
     * Pobiera ID gry, w której jest dany gracz.
     */
    public String getGameIdForPlayer(String playerId) {
        return playerToGameMap.get(playerId);
    }

    public void unregisterPlayer(String playerId) {
        playerToGameMap.remove(playerId);
    }

    public void reset() {
        activeGames.clear();
        playerToGameMap.clear();
    }
}