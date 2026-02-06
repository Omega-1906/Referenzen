package me.Laemedir.character.dice;

import me.Laemedir.character.MultiCharPlugin;
import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArmorClassTabCompleter implements TabCompleter {

    private final MultiCharPlugin plugin;
    private final CoreAPIPlugin coreAPI;

    public ArmorClassTabCompleter(MultiCharPlugin plugin) {
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

        if (args.length == 1) {
            // Vervollständige mit den Charakter-IDs des Spielers
            String query = "SELECT c.id, c.character_name FROM characters c " +
                    "JOIN character_player cp ON c.id = cp.character_id " +
                    "WHERE cp.uuid = ?";

            try {
                List<String> characterOptions = coreAPI.querySync(query, rs -> {
                    List<String> options = new ArrayList<>();
                    try {
                        while (rs.next()) {
                            int id = rs.getInt("id");
                            String name = rs.getString("character_name");
                            options.add(id + " - " + name);
                        }
                        return options;
                    } catch (SQLException e) {
                        plugin.getLogger().severe("Fehler beim Abrufen der Charaktere: " + e.getMessage());
                        return options;
                    }
                }, playerUUID.toString());

                String partialArg = args[0].toLowerCase();
                for (String option : characterOptions) {
                    if (option.toLowerCase().startsWith(partialArg)) {
                        // Nur die ID als Vervollständigung hinzufügen
                        completions.add(option.split(" - ")[0]);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Fehler bei der Tab-Vervollständigung: " + e.getMessage());
            }
        }

        return completions;
    }
}
