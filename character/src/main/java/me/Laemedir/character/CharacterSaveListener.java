package me.Laemedir.character;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener für das Speichern von Charakterdaten beim Verlassen des Servers.
 */
public class CharacterSaveListener implements Listener {

    private final MultiCharPlugin plugin;

    public CharacterSaveListener(MultiCharPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Speichert den aktiven Charakter, wenn ein Spieler den Server verlässt.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Beim Verlassen des Servers den aktiven Charakter speichern
        plugin.saveActiveCharacter(event.getPlayer());
        
        // Entferne den Spieler aus der aktiven Map, um Speicherlecks zu vermeiden
        plugin.removeActiveCharacter(event.getPlayer().getUniqueId());
    }
}

