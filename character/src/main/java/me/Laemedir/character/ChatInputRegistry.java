package me.Laemedir.character;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;

public class ChatInputRegistry implements Listener {

    private static final Map<Player, ChatInputHandler> handlers = new HashMap<>();

    public static void register(Player player, ChatInputHandler handler) {
        handlers.put(player, handler);
    }

    public static void unregister(Player player) {
        handlers.remove(player);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Prüfe, ob der Spieler registriert ist
        if (handlers.containsKey(player)) {
            event.setCancelled(true); // Verhindere, dass die Nachricht im Chat erscheint
            String message = event.getMessage();

            // Übergib die Nachricht an den entsprechenden Handler
            handlers.get(player).handleInput(message);
        }
    }
}
