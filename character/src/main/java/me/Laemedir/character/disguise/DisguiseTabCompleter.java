package me.Laemedir.character.disguise;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DisguiseTabCompleter implements TabCompleter {

    private final List<String> sizes = Arrays.asList("small", "normal", "big");
    private final List<String> commonDisguises = Arrays.asList(
            "cow", "pig", "chicken", "sheep", "wolf", "cat", "horse", "villager",
            "zombie", "skeleton", "creeper", "spider", "enderman", "blaze",
            "ghast", "slime", "magma_cube", "bat", "squid", "rabbit",
            "armor_stand", "item_frame", "painting"
    );

    private final List<String> catTypes = Arrays.asList(
            "all_black", "ashen", "black", "british_shorthair", "calico",
            "chestnut", "jellie", "pale", "persian", "ragdoll", "red",
            "siamese", "snowy", "spotted", "striped", "tabby", "white", "woods"
    );

    private final List<String> wolfVariants = Arrays.asList(
            "ashen", "black", "chestnut", "pale", "rusty", "snowy",
            "spotted", "striped", "woods"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("disguise")) {
            if (args.length == 1) {
                // Spielernamen vervollständigen
                String partialName = args[0].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partialName)) {
                        completions.add(player.getName());
                    }
                }
            } else if (args.length == 2) {
                // Disguise-IDs vervollständigen
                String partialId = args[1].toLowerCase();
                for (String disguise : commonDisguises) {
                    if (disguise.startsWith(partialId)) {
                        completions.add(disguise);
                    }
                }
            } else if (args.length == 3) {
                // Größen vervollständigen
                String partialSize = args[2].toLowerCase();
                for (String size : sizes) {
                    if (size.startsWith(partialSize)) {
                        completions.add(size);
                    }
                }
            } else if (args.length == 4) {
                // Typen/Varianten für Katzen/Wölfe
                String disguiseType = args[1].toLowerCase();
                String partialType = args[3].toLowerCase();

                if (disguiseType.equals("cat")) {
                    for (String type : catTypes) {
                        if (type.startsWith(partialType)) {
                            completions.add(type);
                        }
                    }
                } else if (disguiseType.equals("wolf")) {
                    for (String variant : wolfVariants) {
                        if (variant.startsWith(partialType)) {
                            completions.add(variant);
                        }
                    }
                }
            }
        } else if (command.getName().equalsIgnoreCase("givedisguise")) {
            if (args.length == 1) {
                // Spielernamen vervollständigen
                String partialName = args[0].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partialName)) {
                        completions.add(player.getName());
                    }
                }
            } else if (args.length == 2) {
                // Disguise-IDs vervollständigen
                String partialId = args[1].toLowerCase();
                for (String disguise : commonDisguises) {
                    if (disguise.startsWith(partialId)) {
                        completions.add(disguise);
                    }
                }
            } else if (args.length == 3) {
                // Größen vervollständigen
                String partialSize = args[2].toLowerCase();
                for (String size : sizes) {
                    if (size.startsWith(partialSize)) {
                        completions.add(size);
                    }
                }
            } else if (args.length == 4) {
                // Typen/Varianten für Katzen/Wölfe
                String disguiseType = args[1].toLowerCase();
                String partialType = args[3].toLowerCase();

                if (disguiseType.equals("cat")) {
                    for (String type : catTypes) {
                        if (type.startsWith(partialType)) {
                            completions.add(type);
                        }
                    }
                } else if (disguiseType.equals("wolf")) {
                    for (String variant : wolfVariants) {
                        if (variant.startsWith(partialType)) {
                            completions.add(variant);
                        }
                    }
                }
            }
        }

        return completions;
    }
}
