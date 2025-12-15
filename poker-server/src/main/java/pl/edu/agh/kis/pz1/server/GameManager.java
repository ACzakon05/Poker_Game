package pl.edu.agh.kis.pz1.server;

import pl.edu.agh.kis.pz1.common.protocol.commands.ClientCommand;
import pl.edu.agh.kis.pz1.common.protocol.ServerEvent;
import pl.edu.agh.kis.pz1.common.protocol.commands.*;
import pl.edu.agh.kis.pz1.common.protocol.events.*;
import pl.edu.agh.kis.pz1.model.cards.*;
import pl.edu.agh.kis.pz1.model.exceptions.*;
import pl.edu.agh.kis.pz1.model.game.GameState;
import pl.edu.agh.kis.pz1.model.player.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Główny silnik gry. Zarządza Maszyną Stanów, walidacją ruchu i broadcastem.
 */
public class GameManager {

    private final String gameId;
    private final int maxPlayers;
    // Używamy instancji PokerHandEvaluator do oceniania układów
    private final HandEvaluator handEvaluator = PokerHandEvaluator.getInstance();

    // Utrzymujemy referencje, aby móc wysyłać komunikaty
    private final Map<String, ClientHandler> handlers;
    private final List<Player> playersInGame;

    // Stan Gry
    private GameState currentState;
    private Deck deck;
    private int pot;
    private int ante = 10;
    private int fixedBet = 20;
    private int highestBetInRound;

    // Stan Rundy
    private int dealerIndex = 0;
    private int turnIndex = 0;
    private int bettingRoundStartTurnIndex = 0;
    private Player currentTurnPlayer;

    public GameManager(String gameId, int maxPlayers) {
        this.gameId = gameId;
        this.maxPlayers = maxPlayers;
        this.handlers = new ConcurrentHashMap<>();
        this.playersInGame = Collections.synchronizedList(new ArrayList<>());
        this.currentState = GameState.LOBBY;
        this.deck = new Deck();
        this.pot = 0;
        this.highestBetInRound = 0;
    }

    // --- GRACZE I LOBBY ---

    public synchronized void addClient(String playerId, ClientHandler handler) throws InvalidMoveException {
        if (currentState != GameState.LOBBY) {
            throw new InvalidMoveException("GAME_STARTED", "Nie można dołączyć, gra już się rozpoczęła.");
        }
        if (playersInGame.size() >= maxPlayers) {
            throw new InvalidMoveException("ROOM_FULL", "Pokój jest pełny.");
        }

        Player newPlayer = new Player(playerId, handler.getPlayerName(), 1000);
        playersInGame.add(newPlayer);
        handlers.put(playerId, handler);

        GameLobby.getInstance().registerPlayer(playerId, gameId);
        handler.setCurrentGameId(gameId);


        sendEvent(playerId, new WelcomeEvent(gameId, playerId));
        broadcastLobbyStatus();
    }

    private void broadcastLobbyStatus() {

        String status = playersInGame.stream().map(Player::getName).collect(Collectors.joining(", "));
        // W pełnej implementacji: broadcastEvent(new LobbyEvent(status));
        System.out.println("Lobby Status: " + status);
    }

    // --- OBSŁUGA POŁĄCZEŃ I LOGIKA RUCHU ---

    public void handlePlayerDisconnect(String playerId) {
        synchronized (this) {
            Player disconnectedPlayer = getPlayer(playerId);

            if (disconnectedPlayer != null) {

                // 1. Zabezpieczenie stanu gracza
                disconnectedPlayer.setFolded(true); // Gracz automatycznie spasował

                // 2. Usunięcie powiązań sieciowych i lobby
                handlers.remove(playerId);
                GameLobby.getInstance().unregisterPlayer(playerId); // Wymaga tej metody w GameLobby

                System.out.println("[INFO] Gracz " + disconnectedPlayer.getName() + " (ID: " + playerId + ") rozłączony.");

                // 3. Obsługa trwającej gry
                if (currentState != GameState.LOBBY && currentState != GameState.END) {

                    // Poinformuj innych graczy o spasowaniu/rozłączeniu
                    broadcastEvent(new ActionPlayerEvent(playerId, "FOLD", "DISCONNECT"));

                    // Sprawdzenie, czy to była jego tura
                    if (currentTurnPlayer != null && currentTurnPlayer.getId().equals(playerId)) {
                        // Przesuń turę do następnego aktywnego gracza
                        advanceTurn();
                    } else {
                        // Jeśli rozłączony gracz nie był na ruchu,
                        // musimy sprawdzić, czy jego rozłączenie nie zakończyło licytacji (np. został tylko jeden gracz).
                        long activePlayersCount = playersInGame.stream().filter(p -> !p.isFolded()).count();
                        if (activePlayersCount <= 1) {
                            transitionToNextPhase();
                        }
                    }
                }
                // UWAGA: Gracz nie jest usuwany z listy playersInGame, dopóki runda się nie skończy,
                // aby utrzymać stałą kolejność dealerIndex i turnIndex.
            }
        }
    }

    public void processCommand(String playerId, ClientCommand command) {

        synchronized (this) {
            try {
                if (getPlayer(playerId) == null) {
                    throw new ProtocolException("PLAYER_NOT_FOUND");
                }

                if (currentState != GameState.LOBBY) {
                    validateTurn(playerId);
                }

                switch (currentState) {
                    case LOBBY -> handleLobbyCommand(playerId, command);
                    case ANTE -> throw new InvalidMoveException("AUTO_ACTION", "Faza ANTE jest automatyczna, nie oczekuje się komend.");
                    case BET1, BET2 -> handleBettingCommand(playerId, command);
                    case DRAW -> handleDrawCommand(playerId, command);
                    default -> throw new InvalidMoveException("WRONG_STATE", "Nieprawidłowa akcja w stanie " + currentState.name());
                }

            } catch (InvalidMoveException e) {
                sendEvent(playerId, new ErrorEvent(e));
            }
        }
    }

    // --- WALIDACJE I KOLEJKA RUCHU ---

    // ... (getPlayer i validateTurn bez zmian) ...
    private Player getPlayer(String playerId) {
        return playersInGame.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    private void validateTurn(String playerId) throws OutOfTurnException {
        if (currentTurnPlayer == null || !playerId.equals(currentTurnPlayer.getId())) {
            throw new OutOfTurnException();
        }
    }

    // W GameManager.java

    private void advanceTurn() {
        int activePlayersCount = (int) playersInGame.stream().filter(p -> !p.isFolded()).count();

        // 1. Natychmiastowe zakończenie fazy (został tylko jeden gracz)
        if (activePlayersCount <= 1) {
            transitionToNextPhase();
            return;
        }

        // 2. Warunek zakończenia fazy DRAW (jeśli wszyscy już wymienili karty)
        if (currentState == GameState.DRAW) {
            boolean allActed = playersInGame.stream()
                    .filter(p -> !p.isFolded())
                    .allMatch(Player::isHasActed);

            if (allActed) {
                transitionToNextPhase();
                return;
            }
        }

        // 3. Przesunięcie tury do następnego aktywnego gracza
        int startIndex = turnIndex;
        do {
            turnIndex = (turnIndex + 1) % playersInGame.size();
            Player nextPlayer = playersInGame.get(turnIndex);

            if (!nextPlayer.isFolded()) {

                // A) W fazie DRAW musimy pominąć graczy, którzy już wymienili karty (hasActed=true)
                if (currentState == GameState.DRAW && nextPlayer.isHasActed()) {
                    continue;
                }

                // B) Sprawdzenie zakończenia licytacji (tylko dla BET1/BET2)
                if (currentState == GameState.BET1 || currentState == GameState.BET2) {

                    // Warunek ZAMKNIĘCIA: Tura wróciła do gracza, który ostatni podbił / otworzył
                    if (turnIndex == bettingRoundStartTurnIndex) {

                        // Wszyscy, którzy nie spasowali, musieli wyrównać zakład
                        boolean allCalled = playersInGame.stream()
                                .filter(p -> !p.isFolded())
                                .allMatch(p -> p.getCurrentBet() == highestBetInRound);

                        if (allCalled) {
                            transitionToNextPhase();
                            return; // KONIEC FAZY LICYTACJI
                        }
                    }
                }

                // C) Ustawienie nowego gracza na ruchu i wysłanie eventu
                this.currentTurnPlayer = nextPlayer;
                startBettingRound();
                return;
            }
        } while (turnIndex != startIndex);

        // 4. Jeśli pętla się zamknęła (np. wszyscy spasowali poza 1 lub błąd logiki), zakończ fazę
        transitionToNextPhase();
    }

    // --- MASZYNA STANÓW: Obsługa Przejść ---

    private void transitionTo(GameState newState) {
        this.currentState = newState;
        System.out.println("Gra " + gameId + ": Przejście do stanu: " + newState);

        switch (newState) {
            case ANTE -> startAntePhase();
            case DEAL -> dealCardsPhase();
            case BET1 -> startBettingRound();
            case DRAW -> transitionToDrawPhase();
            case BET2 -> startBettingRound();
            case SHOWDOWN -> showDownPhase();
            case PAYOUT -> payoutPhase();
            case END -> endRoundPhase();
            default -> {}
        }
    }

    private void transitionToNextPhase() {
        switch (currentState) {
            case ANTE -> transitionTo(GameState.DEAL);
            case BET1 -> transitionTo(GameState.DRAW);
            case DRAW -> transitionTo(GameState.BET2);
            case BET2 -> transitionTo(GameState.SHOWDOWN);
            default -> throw new IllegalStateException("Nieznane przejście ze stanu: " + currentState);
        }
    }

    // --- IMPLEMENTACJA FAZ ---

    private void startAntePhase() {
        highestBetInRound = ante;
        playersInGame.forEach(player -> {
            try {
                int amountPaid = player.payAnte(ante);
                pot += amountPaid;
            } catch (NotEnoughChipsException e) {
                // Jeśli gracz nie ma, jest All-in (logika w Player.payAnte)
                pot += player.getChips();
                player.setChips(0);
                System.out.println(player.getName() + " poszedł All-in za ANTE.");
            }
        });
        transitionTo(GameState.DEAL);
    }
    private void dealCardsPhase() {
        // 1. Reset i Tasowanie
        deck.reset();

        // 2. Rozdanie 5 kart każdemu graczowi
        playersInGame.forEach(player -> {
            player.resetForNewRound();
            if (!player.isFolded()) {
                List<Card> cards = deck.draw(5);
                player.receiveCards(cards);
            }
        });

        // 3. Ustawienie pierwszego gracza na ruchu (po dealerze)
        // Zapewniamy rotację dealera i osoby rozpoczynającej licytację.
        dealerIndex = (dealerIndex + 1) % playersInGame.size();
        turnIndex = dealerIndex;
        bettingRoundStartTurnIndex = turnIndex;
        Player firstActivePlayer = null;
        int startIndex = turnIndex;
        do {
            firstActivePlayer = playersInGame.get(turnIndex);
            if (!firstActivePlayer.isFolded()) {
                break; // Znaleziono aktywnego gracza
            }
            turnIndex = (turnIndex + 1) % playersInGame.size();
        } while (turnIndex != startIndex); // Pętla musi się zatrzymać, jeśli wrócimy do punktu startowego


        this.currentTurnPlayer = firstActivePlayer;

        // 4. Wysłanie DEAL eventu do wszystkich
        for (Player player : playersInGame) {
            DealEvent dealEvent = new DealEvent(
                    player.getId(),
                    player.getHand(),
                    "*****" // Maska dla wiadomości publicznej
            );
            // Handler musi wiedzieć, czy wysłać jawne karty (do siebie) czy maskę.
            sendEvent(player.getId(), dealEvent);
        }

        // 5. Przejście do pierwszej rundy zakładów
        transitionTo(GameState.BET1);
    }

    private void transitionToDrawPhase() {
        // Faza DRAW następuje po pierwszej rundzie zakładów (BET1).

        // 1. Zresetowanie stanu zakładów przed fazą wymiany
        highestBetInRound = 0;
        playersInGame.forEach(p -> {
            p.setHasActed(false); // Resetujemy flagę, aby wymiana kart była uznana za akcję.
            p.setCurrentBet(0);
        });

        // 2. Ustawienie pierwszego gracza, który ma prawo do wymiany
        turnIndex = (dealerIndex + 1) % playersInGame.size();
        Player firstDrawPlayer = playersInGame.get(turnIndex);
        int startIndex = turnIndex;

        // Znajdujemy pierwszego aktywnego gracza
        while (firstDrawPlayer.isFolded() && turnIndex != startIndex) {
            turnIndex = (turnIndex + 1) % playersInGame.size();
            firstDrawPlayer = playersInGame.get(turnIndex);
        }
        this.currentTurnPlayer = firstDrawPlayer;

        // 3. Rozpoczęcie tury wymiany i OGRANICZENIE pętli
        if (this.currentTurnPlayer != null && !this.currentTurnPlayer.isFolded()) {
            // Poprawne wywołanie metody rozpoczynającej turę (która wysyła TURN event)
            startBettingRound();
        } else {
            // Jeśli nie ma aktywnych graczy (np. wszyscy spasowali), natychmiast przejdź do BET2
            transitionToNextPhase(); // -> Przejdzie do BET2
        }


    }

    private void showDownPhase() {


        List<Player> contenders = playersInGame.stream()
                .filter(p -> !p.isFolded() && p.getHand() != null && !p.getHand().isEmpty())
                .toList();

        if (contenders.isEmpty()) {
            System.out.println("SHOWDOWN: Wszyscy spasowali, pula powinna być już rozstrzygnięta w advanceTurn.");
            transitionTo(GameState.PAYOUT);
            return;
        }

        // 1. Ocena układów dla wszystkich walczących
        Map<Player, HandEvaluation> evaluations = contenders.stream()
                .collect(Collectors.toMap(
                        p -> p,
                        p -> handEvaluator.evaluate(p.getHand())
                ));

        // 2. Znalezienie zwycięzcy
        Player winner = evaluations.keySet().stream()
                .max(Comparator.comparing(evaluations::get))
                .orElse(null);

        if (winner == null) {
            // W przypadku, gdy jest tylko jeden gracz lub błąd, który powinien być złapany wcześniej
            transitionTo(GameState.PAYOUT);
            return;
        }

        // 3. Broadcast ShowdownEvent
        for (Player p : contenders) {
            HandEvaluation evaluation = evaluations.get(p);
            // Uproszczony rankString do protokołu
            String rankString = evaluation.getRank().name() + " (Highest: " + evaluation.getPrimaryRanks().stream().map(Rank::name).collect(Collectors.joining(",")) + ")";

            broadcastEvent(new ShowdownEvent(
                    p.getId(),
                    p.getHand(),
                    rankString
            ));
        }

        // Ustawienie zwycięzcy dla fazy PAYOUT
        // Zakładamy, że winner jest jedynym zwycięzcą w uproszczonej wersji
        this.currentTurnPlayer = winner;

        transitionTo(GameState.PAYOUT);
    }

    private void payoutPhase() {

        if (currentTurnPlayer != null) {
            currentTurnPlayer.addChips(pot);
            broadcastEvent(new PayoutEvent(currentTurnPlayer.getId(), pot, currentTurnPlayer.getChips()));
            System.out.println("PAYOUT: " + currentTurnPlayer.getName() + " wygrał " + pot + " żetonów.");
        }
        pot = 0; // Wyzerowanie puli
        transitionTo(GameState.END);
    }

    private void endRoundPhase() {

        broadcastEvent(new SimpleEvent("END", "Runda zakończona. Przygotowanie do nowej."));
        // Resetowanie stanu graczy do następnej rundy
        playersInGame.forEach(Player::resetForNewRound);
        transitionTo(GameState.LOBBY); // Wracamy do LOBBY (i czekamy na START)
    }

    // --- OBSŁUGA KOMEND W DANYM STANIE ---

    private void handleLobbyCommand(String playerId, ClientCommand command) throws InvalidMoveException {

        // Używamy instanceof, aby obsłużyć wszystkie komendy, które powinny być dozwolone w LOBBY
        if (command instanceof StartCommand ||
                (command instanceof SimpleCommand simple && simple.getAction().equals("START"))) {

            // --- LOGIKA START ---
            if (playersInGame.size() < 2) {
                throw new InvalidMoveException("MIN_PLAYERS", "Potrzeba min. 2 graczy, aby zacząć.");
            }

            // Walidacja Host
            // (zakładamy, że pierwszy gracz jest hostem)
            if (!playersInGame.getFirst().getId().equals(playerId)) {
                throw new InvalidMoveException("NOT_HOST", "Tylko Host (pierwszy gracz) może rozpocząć grę.");
            }

            transitionTo(GameState.ANTE);

        } else if (command instanceof SimpleCommand simple && simple.getAction().equals("STATUS")) {
            // Opcjonalna obsługa STATUS w lobby
            broadcastLobbyStatus();

        } else {
            throw new InvalidMoveException("WRONG_STATE", "Niewłaściwa komenda w stanie LOBBY. Oczekiwano START.");
        }
    }



    private void handleBettingCommand(String playerId, ClientCommand command) {
        if (command instanceof BetCommand bet) {
            Player player = getPlayer(playerId);
            int amountToCall = highestBetInRound - player.getCurrentBet();

            switch (bet.getActionType()) {
                case "FOLD" -> {
                    player.setFolded(true);
                    broadcastEvent(new ActionPlayerEvent(playerId, "FOLD", ""));
                }
                case "CHECK" -> {
                    if (amountToCall > 0) throw new InvalidMoveException("CANNOT_CHECK", "Musisz wyrównać (CALL) lub spasować (FOLD).");
                }
                case "CALL" -> {
                    if (amountToCall == 0) throw new InvalidMoveException("CANNOT_CALL", "Musisz BET/CHECK.");

                    try {
                        player.takeChips(amountToCall);
                    } catch (NotEnoughChipsException e) {
                        throw new InvalidMoveException("NOT_ENOUGH_CHIPS", "Brak żetonów na CALL.");
                    }

                    player.setCurrentBet(highestBetInRound);
                    pot += amountToCall;
                    broadcastEvent(new ActionPlayerEvent(playerId, "CALL", ""));
                }
                case "BET" -> { // Może to być BET lub RAISE
                    int totalBet = bet.getAmount();
                    int minimumBet = (highestBetInRound == 0) ? fixedBet : highestBetInRound + fixedBet;

                    if (totalBet < minimumBet) {
                        throw new InvalidMoveException("RAISE_TOO_SMALL", "Minimalny zakład to " + minimumBet);
                    }

                    int amountToPay = totalBet - player.getCurrentBet();

                    try {
                        player.takeChips(amountToPay);
                    } catch (NotEnoughChipsException e) {
                        throw new InvalidMoveException("NOT_ENOUGH_CHIPS", "Brak żetonów na zakład.");
                    }

                    player.setCurrentBet(totalBet);
                    pot += amountToPay;
                    highestBetInRound = totalBet;
                    bettingRoundStartTurnIndex = turnIndex; // Resetowanie rundy licytacji

                    broadcastEvent(new ActionPlayerEvent(playerId, "BET", "AMOUNT=" + totalBet));
                }
            }
            player.setHasActed(true);
            advanceTurn();
        } else {
            throw new InvalidMoveException("WRONG_COMMAND", "W fazie BET oczekiwano komendy BET/CALL/CHECK/FOLD.");
        }
    }

    private void handleDrawCommand(String playerId, ClientCommand command) {
        if (command instanceof DrawCommand draw) {
            Player player = getPlayer(playerId);
            List<Integer> indices = draw.getCardIndices();

            if (indices.size() > 3) {
                throw new IllegalDrawException("Nie można wymienić więcej niż 3 kart.");
            }


            if (indices.stream().anyMatch(i -> i < 0 || i > 4) || indices.size() != new HashSet<>(indices).size()) {
                throw new IllegalDrawException("Indeksy kart muszą być unikalne i w zakresie 0-4.");
            }

            // Wymiana kart
            List<Card> newCards = deck.draw(indices.size());


            // Wymaga, aby w klasie Player.java istniała metoda:
            // public void replaceCards(List<Integer> indicesToReplace, List<Card> newCards)
            player.replaceCards(indices, newCards);

            broadcastEvent(new ActionPlayerEvent(playerId, "DRAW", "COUNT=" + newCards.size()));

            // Wysłanie nowej ręki tylko do tego gracza (w pełnej implementacji)
            // sendEvent(playerId, new DealEvent(playerId, player.getHand(), ""));

            advanceTurn();
        } else {
            throw new InvalidMoveException("WRONG_COMMAND", "W fazie DRAW oczekiwano komendy DRAW.");
        }
    }

    private void startBettingRound() {
        // 1. Upewniamy się, że currentTurnPlayer jest ustawiony przez advanceTurn
        if (currentTurnPlayer == null) {
            System.err.println("Błąd: currentTurnPlayer jest null w startBettingRound.");
            // W przypadku błędu, przejdź do następnej fazy
            transitionToNextPhase();
            return;
        }

        // 2. Pobieramy gracza na ruchu
        Player player = currentTurnPlayer;

        // 3. Obliczamy kwoty do wyrównania/podbicia
        int callAmount = highestBetInRound - player.getCurrentBet();
        int minRaise = fixedBet;

        // 4. Wysyłamy TURN event
        TurnEvent turnEvent = new TurnEvent(player.getId(), currentState.name(), callAmount, minRaise);
        sendEvent(player.getId(), turnEvent);

        System.out.println("[RUCH] Tura gracza " + player.getName() +
                " (CALL: " + callAmount + ", MIN_RAISE: " + minRaise + ")");
    }

    // --- METODY KOMUNIKACYJNE (WYSYŁANIE) ---

    public void sendEvent(String playerId, ServerEvent event) {
        ClientHandler handler = handlers.get(playerId);
        if (handler != null) {
            // Zmieniona sygnatura ClientHandler.sendEvent (już bez mapy!)
            handler.sendEvent(event);
        }
    }

    public void broadcastEvent(ServerEvent event) {
        // Ta metoda jest niekompletna w Twoim kodzie, musimy ją dodać,
        // ale potrzebujemy jej tylko do eventów bez wykluczeń
        handlers.values().forEach(handler -> handler.sendEvent(event));
    }

    public void broadcastEvent(ServerEvent event, String excludePlayerId) {
        handlers.values().stream()
                .filter(handler -> !handler.getPlayerId().equals(excludePlayerId))
                .forEach(handler -> handler.sendEvent(event));
    }

}