package me.Laemedir.character;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RaceCreateTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("race")) {
            if (args.length == 1) {
                // Hauptbefehle
                List<String> subCommands = Arrays.asList("create", "edit", "delete", "list", "info");
                return filterCompletions(subCommands, args[0]);
            } else if (args.length >= 2) {
                String subCommand = args[0].toLowerCase();

                switch (subCommand) {
                    case "create":
                        // Bei /race create <name> werden keine Vorschläge gemacht
                        return completions;

                    case "edit":
                        if (args.length == 2) {
                            // Bei /race edit <name> werden alle vorhandenen Rassen vorgeschlagen
                            return null; // Wird vom RaceManager bereitgestellt
                        } else if (args.length == 3) {
                            // Bei /race edit <name> <option> werden die bearbeitbaren Optionen vorgeschlagen
                            List<String> editOptions = Arrays.asList("description", "effects");
                            return filterCompletions(editOptions, args[2]);
                        } else if (args.length == 4 && args[2].equalsIgnoreCase("effects")) {
                            // Bei /race edit <name> effects <action> werden die Aktionen vorgeschlagen
                            List<String> effectActions = Arrays.asList("add", "remove", "list", "clear");
                            return filterCompletions(effectActions, args[3]);
                        } else if (args.length == 5 && args[2].equalsIgnoreCase("effects") && args[3].equalsIgnoreCase("add")) {
                            // Bei /race edit <name> effects add <effect> werden alle Effekte vorgeschlagen
                            return filterCompletions(getAllPotionEffectNames(), args[4]);
                        } else if (args.length == 6 && args[2].equalsIgnoreCase("effects") && args[3].equalsIgnoreCase("add")) {
                            // Bei /race edit <name> effects add <effect> <level> werden Level 1-10 vorgeschlagen
                            List<String> levels = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
                            return filterCompletions(levels, args[5]);
                        }
                        break;

                    case "delete":
                    case "info":
                        if (args.length == 2) {
                            // Bei /race delete <name> oder /race info <name> werden alle vorhandenen Rassen vorgeschlagen
                            return null; // Wird vom RaceManager bereitgestellt
                        }
                        break;
                }
            }
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> completions, String currentArg) {
        if (currentArg.isEmpty()) {
            return completions;
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentArg.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getAllPotionEffectNames() {
        List<String> effectNames = new ArrayList<>();
        for (PotionEffectType type : PotionEffectType.values()) {
            if (type != null) {
                effectNames.add(type.getName().toUpperCase());
            }
        }
        return effectNames;
    }
}
