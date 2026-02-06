package me.Laemedir.character.dice;

import me.Laemedir.character.MultiCharPlugin;
import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AttackRollTabCompleter implements TabCompleter {

    private final MultiCharPlugin plugin;
    private final CoreAPIPlugin coreAPI;
    private final int NEARBY_RADIUS = 15; // Radius in Blöcken, in dem Spieler als "in der Nähe" gelten

    public AttackRollTabCompleter(MultiCharPlugin plugin) {
        this.plugin = plugin;
        this.coreAPI = CoreAPIPlugin.getPlugin(CoreAPIPlugin.class);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        try {
            if (args.length == 1) {
                // Nur den aktiven Charakter des Spielers anzeigen
                String activeCharacter = MultiCharPlugin.getActiveCharacter(playerUUID);

                if (activeCharacter != null && activeCharacter.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(activeCharacter);
                }
            } else if (args.length == 2) {
                // Vervollständige mit aktiven Charakternamen von Spielern in der Nähe (Ziel)
                List<String> nearbyCharacterNames = new ArrayList<>();

                // Sammle alle Spieler in der Nähe
                for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
                    // Überprüfe, ob der Spieler in Reichweite ist
                    if (nearbyPlayer != player &&
                            nearbyPlayer.getWorld().equals(player.getWorld()) &&
                            nearbyPlayer.getLocation().distance(player.getLocation()) <= NEARBY_RADIUS) {

                        // Hole nur den aktiven Charakter dieses Spielers
                        String activeCharacter = MultiCharPlugin.getActiveCharacter(nearbyPlayer.getUniqueId());

                        if (activeCharacter != null) {
                            nearbyCharacterNames.add(activeCharacter);
                        }
                    }
                }

                String partialArg = args[1].toLowerCase();
                for (String option : nearbyCharacterNames) {
                    if (option.toLowerCase().startsWith(partialArg)) {
                        completions.add(option);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler bei der Tab-Vervollständigung: " + e.getMessage());
        }

        return completions;
    }
}
