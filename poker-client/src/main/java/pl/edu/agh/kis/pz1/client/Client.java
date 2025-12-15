package pl.edu.agh.kis.pz1.client;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Główna klasa klienta tekstowego (TUI).
 * Obsługuje połączenie z serwerem i oddzielną pętlę dla odczytu i zapisu.
 */
public class Client {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 7777;
    private static final String AVAILABLE_COMMANDS =
            "DOSTĘPNE KOMENDY: " +
                    "JOIN GAME=<id> NAME=<nick>, " +
                    "START, " +
                    "STATUS, " +
                    "FOLD, " +
                    "CHECK, " +
                    "CALL, " +
                    "BET AMOUNT=<n>, " +
                    "DRAW CARDS=<i,j,k>, " +
                    "QUIT.";
    private PrintWriter out;
    private BufferedReader serverIn;
    private BufferedReader userIn;

    // Executor dla wątku odczytu z serwera (używamy SingleThreadExecutor do zachowania kolejności)
    private final ExecutorService readerExecutor = Executors.newSingleThreadExecutor();

    public void start() {
        System.out.println("--- Poker Client - Text User Interface (TUI) ---");
        System.out.println("\nPodaj pierwszą komendę: JOIN GAME=<id> NAME=<Name>");

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {

            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.userIn = new BufferedReader(new InputStreamReader(System.in));

            // Startujemy wątek wirtualny do słuchania wiadomości od serwera
            // Użycie submit() zamiast execute() jest bezpieczniejsze
            readerExecutor.submit(this::readServerMessages);

            // Główny wątek obsługuje input użytkownika
            handleUserInput();

        } catch (IOException e) {
            System.err.println("\n[SYSTEM] Błąd połączenia z serwerem: " + e.getMessage());
        } finally {
            readerExecutor.shutdownNow();
        }
    }

    // --- 1. Pętla Odczytu z Serwera ---

    private void readServerMessages() {
        String rawEvent;
        try {
            while ((rawEvent = serverIn.readLine()) != null) {
                // Renderowanie wiadomości (wyświetlanie stanu)
                renderServerMessage(rawEvent);
            }
        } catch (IOException e) {
            // Ten błąd oznacza, że serwer zakończył połączenie
            System.err.println("\n[SYSTEM] Serwer rozłączył połączenie.");
        }
    }

    /**
     * Uproszczone renderowanie wiadomości od serwera.
     * Używa \r do powrotu na początek linii, aby nie mieszać inputu z wiadomościami serwera.
     * @param rawEvent Surowy komunikat serwera.
     */
    private void renderServerMessage(String rawEvent) {
        // Powrót na początek linii, aby zignorować częściowo wpisaną komendę użytkownika
        System.out.print("\r");


        if (rawEvent.startsWith("ERR")) {
            System.out.println("\n[!!! BLAD !!!] " + rawEvent);

        } else if (rawEvent.startsWith("WELCOME")) {
            System.out.println("\n[### OK ###] " + rawEvent);
            System.out.println("\nGotowe. Wpisz następną komendę");

        } else if (rawEvent.startsWith("TURN")) {
            System.out.println("\n[>> TWOJA KOLEJ >>] " + rawEvent);
        } else {
            System.out.println("\n[SERWER] " + rawEvent);
        }

        // Ponowne wyświetlenie promptu
        System.out.print("Komenda > ");
    }

    // --- 2. Pętla Inputu Użytkownika ---

    private void handleUserInput() throws IOException {
        String userInput;
        System.out.println("\n--- Pomoc ---");
        System.out.println(AVAILABLE_COMMANDS);
        System.out.println("-------------");
        System.out.print("Komenda > ");

        while ((userInput = userIn.readLine()) != null) {

            if (userInput.isBlank()) {
                System.out.print("Komenda > ");
                continue;
            }

            if (userInput.equalsIgnoreCase("QUIT")) {
                // Wyślij QUIT do serwera i przerwij pętlę inputu
                out.println("QUIT");
                break;
            }

            // Wysyłanie wiadomości do serwera (zakończenie linią \n jest kluczowe)
            out.println(userInput);

            // Renderowanie promptu jest obsługiwane przez readServerMessages po otrzymaniu odpowiedzi
            // Ale dajemy prompta po wysłaniu komendy, by UX był lepszy (przed odpowiedzią serwera)
            System.out.print("Komenda > ");
        }
    }

    public static void main(String[] args) {
        new Client().start();
    }
}