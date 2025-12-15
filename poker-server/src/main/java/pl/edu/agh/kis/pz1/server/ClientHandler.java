package pl.edu.agh.kis.pz1.server;

import pl.edu.agh.kis.pz1.common.protocol.commands.ClientCommand;
import pl.edu.agh.kis.pz1.common.protocol.CommandParser;
import pl.edu.agh.kis.pz1.common.protocol.EventEncoder;
import pl.edu.agh.kis.pz1.common.protocol.ServerEvent;
import pl.edu.agh.kis.pz1.common.protocol.commands.JoinCommand;
import pl.edu.agh.kis.pz1.common.protocol.events.ErrorEvent;
import pl.edu.agh.kis.pz1.model.exceptions.InvalidMoveException;
import pl.edu.agh.kis.pz1.model.exceptions.ProtocolException;


import java.io.*;
import java.net.Socket;
import java.util.UUID;

/**
 * Klasa obsługująca komunikację z pojedynczym klientem w Wątku Wirtualnym.
 * Odpowiedzialna za odczyt (parsuje komendy) i zapis (wysyła eventy).
 */
public class ClientHandler {

    private final Socket socket;
    private final GameLobby lobby;
    private final CommandParser parser = new CommandParser();
    private final EventEncoder encoder = new EventEncoder();
    private final PrintWriter out;
    private final BufferedReader in;

    private String playerId;
    private String playerName; // Docelowo odczytane z JOIN
    private String currentGameId;

    public ClientHandler(Socket socket, GameLobby lobby) throws IOException {
        this.socket = socket;
        this.lobby = lobby;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.playerId = "P" + UUID.randomUUID().toString().substring(0, 7); // Unikalne ID gracza
        this.playerName = "Anonim";
        this.currentGameId = null;
    }

    public void run() throws IOException {
        String rawMessage;

        while ((rawMessage = in.readLine()) != null) {
            if (rawMessage.isBlank()) continue;

            try {
                ClientCommand command = parser.parse(rawMessage);
                GameManager game = null;

                if (command instanceof JoinCommand joinCmd) {
                    // Obsługa dołączania do gry: tylko JOIN jest obsługiwany przez Handlera
                    game = lobby.getGame(joinCmd.getGameId());
                    if (game == null) {
                        sendError(new ProtocolException("GAME_NOT_FOUND"));
                        continue;
                    }
                    this.playerName = joinCmd.getNickname();
                    game.addClient(playerId, this);

                } else {
                    // Wszystkie inne komendy przekazujemy do bieżącej gry
                    game = lobby.getGame(currentGameId);
                    if (game == null) {
                        sendError(new ProtocolException("Musisz najpierw dołączyć do gry (JOIN)."));
                        continue;
                    }
                    game.processCommand(playerId, command);
                }

            } catch (ProtocolException e) {
                sendError(e);
            } catch (InvalidMoveException e) {
                sendError(e);
            } catch (Exception e) {
                System.err.println("Nieoczekiwany błąd obsługi klienta: " + e.getMessage());
                sendError(new ProtocolException("INTERNAL_SERVER_ERROR: " + e.getMessage()));
            }
        }

        // Obsługa rozłączenia (koniec pętli while)
        handleDisconnect();
    }

    private void handleDisconnect() {
        if (currentGameId != null) {
            GameManager game = lobby.getGame(currentGameId);
            if (game != null) {
                game.handlePlayerDisconnect(playerId);
            }
        }
        System.out.println("Gracz " + playerId + " rozłączony.");
    }

    /**
     * Wymagana metoda do wysyłania odpowiedzi. Używana przez GameManager do broadcastu.
     */
    public void sendEvent(ServerEvent event) {
        try {
            // Poprawione wywołanie EventEncoder.encode: (Event, TargetPlayerId)
            String encodedEvent = encoder.encode(event, this.playerId);
            out.println(encodedEvent);
            out.flush();
        } catch (ProtocolException e) {
            System.err.println("[ERROR] Błąd kodowania eventu dla gracza " + this.playerName
                    + " (ID: " + this.playerId + "): " + e.getMessage());
        }
    }

    /**
     * Prywatna metoda pomocnicza do wysyłania ErrorEvent.
     * Używana wewnętrznie w ClientHandler do obsługi wyjątków (np. ProtocolException).
     */
    private void sendError(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Nieznany błąd";
        String code = (ex instanceof InvalidMoveException) ? ((InvalidMoveException) ex).code : "PROTOCOL_ERROR";

        // Tworzymy ErrorEvent na podstawie wyjątku
        ErrorEvent errorEvent = new ErrorEvent(new InvalidMoveException(code, message));

        // Używamy nowej, uproszczonej metody sendEvent(Event)
        sendEvent(errorEvent);
    }

    // --- Gettery i Settery dla GameManager ---
    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public void setCurrentGameId(String currentGameId) { this.currentGameId = currentGameId; }
}