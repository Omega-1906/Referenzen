package me.Laemedir.character;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerFreezeListener implements Listener {
    private final MultiCharPlugin plugin;
    public PlayerFreezeListener(MultiCharPlugin plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.isFrozen(player)) {
            // Bewegung verhindern
            event.setCancelled(true);
        }
    }
}
