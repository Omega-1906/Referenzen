package me.Laemedir.character;

import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PersonaTabCompleter implements TabCompleter {
    private final MultiCharPlugin plugin;
    private final CoreAPIPlugin coreAPI;

    public PersonaTabCompleter(MultiCharPlugin plugin, CoreAPIPlugin coreAPI) {
        this.plugin = plugin;
        this.coreAPI = coreAPI;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            // Subcommands für Stammgäste und Team
            if (player.hasPermission("laemedir.stammgast") || player.hasPermission("laemedir.team")) {
                List<String> stammgastCommands = Arrays.asList("prefix", "prefixcolour", "chatcolour");
                for (String cmd : stammgastCommands) {
                    if (cmd.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(cmd);
                    }
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            // Für prefixcolour-Befehl im zweiten Argument: Präfix-Farben vorschlagen
            if (subCommand.equals("prefixcolour") &&
                    (player.hasPermission("laemedir.stammgast") || player.hasPermission("laemedir.team"))) {
                List<String> prefixColours = Arrays.asList("DarkRed", "DarkBlue", "DarkAqua", "DarkGreen", "DarkPurple");
                for (String colour : prefixColours) {
                    if (colour.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(colour);
                    }
                }
            }
            // Für chatcolour-Befehl im zweiten Argument: Chat-Farben vorschlagen
            else if (subCommand.equals("chatcolour") &&
                    (player.hasPermission("laemedir.stammgast") || player.hasPermission("laemedir.team"))) {
                List<String> chatColours = Arrays.asList("Red", "Blue", "Green", "Purple");
                for (String colour : chatColours) {
                    if (colour.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(colour);
                    }
                }
            }
        }

        return completions;
    }
}