package pl.edu.agh.kis.pz1.common.protocol;

import pl.edu.agh.kis.pz1.common.protocol.commands.*;
import pl.edu.agh.kis.pz1.model.exceptions.ProtocolException;

import java.util.Arrays; // Dodany import
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors; // Dodany import

/**
 * Klasa odpowiedzialna za parsowanie surowych komunikatów tekstowych od klienta
 * do obiektów ClientCommand.
 */
public class CommandParser {

    // Regex do wyodrębnienia par KEY=VALUE
    private static final Pattern PARAM_PATTERN = Pattern.compile("(\\w+)=([^\\s]+)");

    // Wymagane, ale nie są używane w switchu (można je usunąć)
    // private static final List<String> PARAMETERIZED_ACTIONS = List.of("HELLO", "CREATE", "JOIN", "BET", "DRAW");

    /**
     * Parsuje surową wiadomość tekstową (np. JOIN GAME=X NAME=Y) na obiekt ClientCommand.
     * Format: ACTION PARAM=VALUE [PARAM2=VALUE2...]
     * @param rawMessage Surowy komunikat od klienta.
     * @return Obiekt ClientCommand.
     * @throws ProtocolException jeśli format jest nieprawidłowy.
     */
    public ClientCommand parse(String rawMessage) throws ProtocolException {
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new ProtocolException("Pusta wiadomość.");
        }

        // Dzielenie na akcję i resztę (parametry)
        String[] parts = rawMessage.trim().split("\\s+", 2);
        String action = parts[0].toUpperCase();
        String paramsString = (parts.length > 1) ? parts[1] : "";

        // 1. Sprawdzenie, czy akcja jest wymagana
        if (action.isBlank()) {
            throw new ProtocolException("Brak akcji w wiadomości.");
        }

        Map<String, String> params = parseParams(paramsString);

        // 2. Mapowanie na konkretny typ komendy
        try {
            return switch (action) {
                // Komendy z parametrami
                case "HELLO" -> new HelloCommand(params.getOrDefault("VERSION", "UNKNOWN"));

                case "JOIN" -> new JoinCommand(
                        getRequiredParam(params, "GAME", action),
                        getRequiredParam(params, "NAME", action)
                );

                case "CREATE" -> new CreateCommand(
                        getRequiredIntParam(params, "ANTE", action),
                        getRequiredIntParam(params, "BET", action)
                        // TODO: Dodać obsługę LIMIT
                );

                // Akcje Licytacji (Parsowane bezpośrednio w switchu, aby uniknąć błędów rzutowania)
                case "FOLD" -> new BetCommand("FOLD");
                case "CALL" -> new BetCommand("CALL");
                case "CHECK" -> new BetCommand("CHECK");

                // BET (wymaga AMOUNT)
                case "BET" -> new BetCommand("BET", getRequiredIntParam(params, "AMOUNT", action));


                // Komendy Simple Command (bez parametrów)
                case "LEAVE", "START", "STATUS", "QUIT" -> {
                    if (!params.isEmpty()) {
                        throw new ProtocolException(String.format("Komenda %s nie przyjmuje parametrów.", action));
                    }
                    yield new SimpleCommand(action);
                }

                case "DRAW" -> parseDrawCommand(params, action);

                default -> throw new ProtocolException("Nieznana akcja: " + action);
            };
        } catch (NumberFormatException e) {
            throw new ProtocolException("Nieprawidłowy format numeryczny w parametrze.");
        }
    }

    // --- Metody Pomocnicze do Parsowania ---

    private Map<String, String> parseParams(String paramsString) {
        Map<String, String> params = new HashMap<>();
        if (paramsString.isBlank()) {
            return params;
        }

        Matcher matcher = PARAM_PATTERN.matcher(paramsString);
        while (matcher.find()) {
            params.put(matcher.group(1).toUpperCase(), matcher.group(2));
        }
        return params;
    }

    private String getRequiredParam(Map<String, String> params, String key, String action) throws ProtocolException {
        String value = params.get(key.toUpperCase());
        if (value == null) {
            throw new ProtocolException(String.format("Komenda %s wymaga parametru %s.", action, key));
        }
        return value;
    }

    private int getRequiredIntParam(Map<String, String> params, String key, String action) throws ProtocolException {
        String value = getRequiredParam(params, key, action);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ProtocolException(String.format("Parametr %s musi być liczbą całkowitą.", key));
        }
    }

    /**
     * Parsuje akcję DRAW CARDS=<i,j,k>.
     */
    private ClientCommand parseDrawCommand(Map<String, String> params, String action) throws ProtocolException {
        String cardsString = getRequiredParam(params, "CARDS", action);
        List<Integer> cardIndices;

        try {
            // Parsowanie listy indeksów (oddzielonych przecinkami)
            cardIndices = Arrays.stream(cardsString.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new ProtocolException("Parametr CARDS musi zawierać listę indeksów oddzielonych przecinkami.");
        }

        return new DrawCommand(cardIndices);
    }
}