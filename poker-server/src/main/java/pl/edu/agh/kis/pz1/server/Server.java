package pl.edu.agh.kis.pz1.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Główna klasa serwera TCP. Obsługuje nowe połączenia i przekazuje je do wątków wirtualnych.
 * Wykorzystuje JDK 21 Virtual Threads.
 * Uruchomienie: java -jar poker-server.jar [maxPlayers]
 */
public class Server {

    private static final int PORT = 7777;

    // Użycie fabryki wątków wirtualnych (Virtual Threads) zgodnie z wytycznymi
    private final ExecutorService clientPool = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
    private final GameLobby lobby = GameLobby.getInstance();
    private final int maxPlayersPerGame;

    public Server(int maxPlayers) {
        this.maxPlayersPerGame = maxPlayers;
        // Na start tworzymy domyślną grę, do której można dołączyć
        lobby.createNewGame(maxPlayers);
    }

    public void start() {
        System.out.println("--- Poker Server ---");
        System.out.println("Uruchomiony na porcie " + PORT + " (Max graczy/gra: " + maxPlayersPerGame + ")...");
        System.out.println("Oczekiwanie na połączenia. Pierwsza gra to GAME_0.");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // Blokowanie do momentu połączenia nowego klienta
                Socket clientSocket = serverSocket.accept();
                System.out.println("[INFO] Nowy klient połączony: " + clientSocket.getRemoteSocketAddress());

                // Przypisanie nowego wątku wirtualnego do obsługi gniazda
                // Wątki Wirtualne doskonale nadają się do obsługi blokującego I/O
                clientPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("[BŁĄD KRYTYCZNY] Błąd serwera: " + e.getMessage());
            clientPool.shutdown();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (clientSocket) {
            // Przekazanie lobby do handlera, aby mógł dołączyć do gry
            ClientHandler handler = new ClientHandler(clientSocket, lobby);
            handler.run();
        } catch (IOException e) {
            System.err.println("[BŁĄD I/O] Klient rozłączony lub błąd transmisji: " + e.getMessage());
            // Zamykanie zasobów jest obsługiwane przez try-with-resources
        }
    }

    public static void main(String[] args) {
        int maxPlayers = 4;
        if (args.length > 0) {
            try {
                maxPlayers = Integer.parseInt(args[0]);
                if (maxPlayers < 2 || maxPlayers > 4) {
                    System.err.println("Liczba graczy musi być między 2 a 4. Użyto domyślnej: 4.");
                    maxPlayers = 4;
                }
            } catch (NumberFormatException e) {
                System.err.println("Nieprawidłowy argument. Użyto domyślnej liczby graczy (4).");
            }
        }
        new Server(maxPlayers).start();
    }
}