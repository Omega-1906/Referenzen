package me.Laemedir.character;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Handhabt Chat-Eingaben von Spielern für interaktive Dialoge.
 * Erlaubt callbacks (Consumer) für die Eingabe und unterstützt einen Abbruch-Befehl.
 */
public class ChatInputHandler {

    private static final String CANCEL_KEYWORD = "abbrechen";

    private final Player player;
    private final Consumer<String> onInput;

    /**
     * Erstellt einen neuen Handler und registriert den Spieler.
     *
     * @param player  der Spieler
     * @param onInput der Callback für die Eingabe
     */
    public ChatInputHandler(Player player, Consumer<String> onInput) {
        this.player = player;
        this.onInput = onInput;

        // Registriere den Spieler für die Chat-Eingabe
        ChatInputRegistry.register(player, this);
    }

    /**
     * Verarbeitet die eingehende Nachricht.
     * Wird vom ChatListener aufgerufen.
     *
     * @param input die Nachricht des Spielers
     */
    public void handleInput(String input) {
        // Entferne den Spieler aus der Registrierung, sobald eine Eingabe erfolgt
        ChatInputRegistry.unregister(player);

        // Prüfe, ob der Spieler "abbrechen" geschrieben hat
        if (input.equalsIgnoreCase(CANCEL_KEYWORD)) {
            onInput.accept(CANCEL_KEYWORD);
            return;
        }

        // Übergib die Eingabe an den Consumer
        onInput.accept(input);
    }
}
