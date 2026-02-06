package me.Laemedir.character.skilltree;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab-Completer für den /skilltree Befehl.
 */
public class SkillTreeTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("open");

            // Admin-Befehle hinzufügen, wenn der Spieler die Berechtigung hat
            if (player.hasPermission("character.skilltree.admin")) {
                completions.add("addpoints");
                completions.add("info");
            }

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if ((args[0].equalsIgnoreCase("addpoints") || args[0].equalsIgnoreCase("info"))
                    && player.hasPermission("character.skilltree.admin")) {
                // Spielernamen vorschlagen
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("addpoints") && player.hasPermission("character.skilltree.admin")) {
                // Punktzahlen vorschlagen
                return Arrays.asList("1", "5", "10");
            }
        }

        return new ArrayList<>();
    }
}
