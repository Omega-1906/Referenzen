package me.Laemedir.character.dice;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RPDiceTabCompleter implements TabCompleter {

    private final List<String> diceOptions = Arrays.asList("1d20", "1d10", "1d100");
    private final List<String> attributeOptions = Arrays.asList(
            "Stärke",
            "Schnelligkeit",
            "Geschicklichkeit",
            "Konstitution",
            "Intelligenz"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Erste Argument-Vervollständigung: 1d20
            String partialArg = args[0].toLowerCase();
            completions.addAll(diceOptions.stream()
                    .filter(option -> option.toLowerCase().startsWith(partialArg))
                    .collect(Collectors.toList()));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("1d20") || args[0].equalsIgnoreCase("1d10") || args[0].equalsIgnoreCase("1d100"))) {
            // Zweite Argument-Vervollständigung: Attribute
            String partialArg = args[1].toLowerCase();
            completions.addAll(attributeOptions.stream()
                    .filter(option -> option.toLowerCase().startsWith(partialArg))
                    .collect(Collectors.toList()));
        }

        return completions;
    }
}
