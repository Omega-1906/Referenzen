package me.Laemedir.character;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * CommandExecutor für das Rassen-System.
 * Behandelt alle /race Befehle wie create, edit, delete, list und info.
 */
public class RaceCommands implements CommandExecutor {

    private final RaceManager raceManager;
    private final String prefix = "§8[§6Rassen§8] ";

    public RaceCommands(RaceManager raceManager) {
        this.raceManager = raceManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c❌ Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage("§c❌ Verwendung: §e/race <create|edit|delete|list|info>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                handleCreateCommand(player, args);
                break;
            case "edit":
                handleEditCommand(player, args);
                break;
            case "delete":
                handleDeleteCommand(player, args);
                break;
            case "list":
                handleListCommand(player);
                break;
            case "info":
                handleInfoCommand(player, args);
                break;
            default:
                player.sendMessage(prefix + "§c❌ Unbekannter Unterbefehl. Verwendung: §e/race <create|edit|delete|list|info>");
                break;
        }

        return true;
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (!player.hasPermission("character.race.create")) {
            player.sendMessage(prefix + "§c❌ Du hast keine Berechtigung, Rassen zu erstellen.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(prefix + "§c❌ Verwendung: §e/race create <Name>");
            return;
        }

        String raceName = args[1];

        // Prüfe, ob die Rasse bereits existiert
        if (raceManager.raceExists(raceName)) {
            player.sendMessage(prefix + "§c❌ Diese Rasse existiert bereits.");
            return;
        }

        // Starte den Erstellungsprozess
        RaceCreationSession.startSession(player, raceName, raceManager);
        player.sendMessage(prefix + "§a✓ Gib die Beschreibung der Rasse im Chat ein oder schreibe §e'cancel'§a zum Abbrechen.");
    }

    private void handleEditCommand(Player player, String[] args) {
        if (!player.hasPermission("character.race.edit")) {
            player.sendMessage(prefix + "§c❌ Du hast keine Berechtigung, Rassen zu bearbeiten.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(prefix + "§c❌ Verwendung: §e/race edit <Name> <description|effects>");
            return;
        }

        String raceName = args[1];
        Race race = raceManager.getRace(raceName);

        if (race == null) {
            player.sendMessage(prefix + "§c❌ Diese Rasse existiert nicht.");
            return;
        }

        String editOption = args[2].toLowerCase();

        switch (editOption) {
            case "description":
                // Starte den Bearbeitungsprozess für die Beschreibung
                RaceCreationSession.startEditDescriptionSession(player, race, raceManager);
                player.sendMessage(prefix + "§a✓ Gib die neue Beschreibung der Rasse im Chat ein oder schreibe §e'cancel'§a zum Abbrechen.");
                break;
            case "effects":
                if (args.length < 4) {
                    player.sendMessage(prefix + "§c❌ Verwendung: §e/race edit <Name> effects <add|remove|list|clear>");
                    return;
                }

                String effectAction = args[3].toLowerCase();

                switch (effectAction) {
                    case "add":
                        if (args.length < 6) {
                            player.sendMessage(prefix + "§c❌ Verwendung: §e/race edit <Name> effects add <Effekt> <Level>");
                            return;
                        }

                        String effectName = args[4].toUpperCase();
                        PotionEffectType effectType = PotionEffectType.getByName(effectName);

                        if (effectType == null) {
                            player.sendMessage(prefix + "§c❌ Ungültiger Effektname: " + effectName);
                            return;
                        }

                        int level;
                        try {
                            level = Integer.parseInt(args[5]);
                            if (level < 1 || level > 10) {
                                player.sendMessage(prefix + "§c❌ Das Level muss zwischen 1 und 10 liegen.");
                                return;
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage(prefix + "§c❌ Ungültiges Level: " + args[5]);
                            return;
                        }

                        // Aktuelle Effekte parsen
                        Map<String, Integer> effects = parseEffects(race.getEffectsAsString());

                        // Effekt hinzufügen oder aktualisieren
                        effects.put(effectName, level);

                        // Effekte zurück in String konvertieren
                        String newEffectsString = buildEffectsString(effects);

                        // Rasse aktualisieren
                        raceManager.editRace(race.getName(), race.getDescription(), newEffectsString);
                        player.sendMessage(prefix + "§a✓ Effekt " + effectName + " (Level " + level + ") zur Rasse " + race.getName() + " hinzugefügt.");
                        break;

                    case "remove":
                        if (args.length < 5) {
                            player.sendMessage(prefix + "§c❌ Verwendung: §e/race edit <Name> effects remove <Effekt>");
                            return;
                        }

                        String removeEffectName = args[4].toUpperCase();

                        // Aktuelle Effekte parsen
                        Map<String, Integer> currentEffects = parseEffects(race.getEffectsAsString());

                        if (!currentEffects.containsKey(removeEffectName)) {
                            player.sendMessage(prefix + "§c❌ Die Rasse " + race.getName() + " hat keinen Effekt " + removeEffectName + ".");
                            return;
                        }

                        // Effekt entfernen
                        currentEffects.remove(removeEffectName);

                        // Effekte zurück in String konvertieren
                        String updatedEffectsString = buildEffectsString(currentEffects);

                        // Rasse aktualisieren
                        raceManager.editRace(race.getName(), race.getDescription(), updatedEffectsString);
                        player.sendMessage(prefix + "§a✓ Effekt " + removeEffectName + " von der Rasse " + race.getName() + " entfernt.");
                        break;

                    case "list":
                        Map<String, Integer> effectMap = parseEffects(race.getEffectsAsString());

                        if (effectMap.isEmpty()) {
                            player.sendMessage(prefix + "§e Die Rasse " + race.getName() + " hat keine Effekte.");
                        } else {
                            player.sendMessage(prefix + "§6Effekte der Rasse " + race.getName() + ":");
                            for (Map.Entry<String, Integer> entry : effectMap.entrySet()) {
                                player.sendMessage("§8- §e" + entry.getKey() + " §7(Level " + entry.getValue() + ")");
                            }
                        }
                        break;

                    case "clear":
                        raceManager.editRace(race.getName(), race.getDescription(), "");
                        player.sendMessage(prefix + "§a✓ Alle Effekte von der Rasse " + race.getName() + " entfernt.");
                        break;

                    default:
                        player.sendMessage(prefix + "§c❌ Verwendung: §e/race edit <Name> effects <add|remove|list|clear>");
                        break;
                }
                break;

            default:
                player.sendMessage(prefix + "§c❌ Verwendung: §e/race edit <Name> <description|effects>");
                break;
        }
    }

    private void handleDeleteCommand(Player player, String[] args) {
        if (!player.hasPermission("character.race.delete")) {
            player.sendMessage(prefix + "§c❌ Du hast keine Berechtigung, Rassen zu löschen.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(prefix + "§c❌ Verwendung: §e/race delete <Name>");
            return;
        }

        String raceName = args[1];

        if (!raceManager.raceExists(raceName)) {
            player.sendMessage(prefix + "§c❌ Diese Rasse existiert nicht.");
            return;
        }

        if (raceManager.deleteRace(raceName)) {
            player.sendMessage(prefix + "§a✓ Die Rasse " + raceName + " wurde erfolgreich gelöscht.");
        } else {
            player.sendMessage(prefix + "§c❌ Fehler beim Löschen der Rasse " + raceName + ".");
        }
    }

    private void handleListCommand(Player player) {
        if (!player.hasPermission("character.race.list")) {
            player.sendMessage(prefix + "§c❌ Du hast keine Berechtigung, Rassen aufzulisten.");
            return;
        }

        List<Race> races = raceManager.getAllRaces();

        if (races.isEmpty()) {
            player.sendMessage(prefix + "§e Es sind keine Rassen vorhanden.");
            return;
        }

        player.sendMessage(prefix + "§6Verfügbare Rassen:");
        for (Race race : races) {
            player.sendMessage("§8- §e" + race.getName());
        }
    }

    private void handleInfoCommand(Player player, String[] args) {
        if (!player.hasPermission("character.race.info")) {
            player.sendMessage(prefix + "§c❌ Du hast keine Berechtigung, Rassen-Informationen anzuzeigen.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(prefix + "§c❌ Verwendung: §e/race info <Name>");
            return;
        }

        String raceName = args[1];
        Race race = raceManager.getRace(raceName);

        if (race == null) {
            player.sendMessage(prefix + "§c❌ Diese Rasse existiert nicht.");
            return;
        }

        player.sendMessage(prefix + "§6Informationen zur Rasse §e" + race.getName() + "§6:");
        player.sendMessage("§7Beschreibung: §f" + race.getDescription());

        // Zeige Effekte an
        Map<String, Integer> effectMap = parseEffects(race.getEffectsAsString());
        if (effectMap.isEmpty()) {
            player.sendMessage("§7Effekte: §fKeine");
        } else {
            player.sendMessage("§7Effekte:");
            for (Map.Entry<String, Integer> entry : effectMap.entrySet()) {
                player.sendMessage("§8- §e" + entry.getKey() + " §7(Level " + entry.getValue() + ")");
            }
        }
    }

    // Hilfsmethode zum Parsen der Effekte im Format "SPEED:1;STRENGTH:2;"
    private Map<String, Integer> parseEffects(String effectsString) {
        Map<String, Integer> effects = new HashMap<>();

        if (effectsString == null || effectsString.isEmpty()) {
            return effects;
        }

        String[] effectParts = effectsString.split(";");
        for (String effect : effectParts) {
            if (!effect.isEmpty()) {
                String[] parts = effect.split(":");
                if (parts.length == 2) {
                    try {
                        String effectName = parts[0].toUpperCase();
                        int level = Integer.parseInt(parts[1]);
                        effects.put(effectName, level);
                    } catch (NumberFormatException ignored) {
                        // Ignoriere ungültige Einträge
                    }
                }
            }
        }

        return effects;
    }

    // Hilfsmethode zum Erstellen des Effekte-Strings im Format "SPEED:1;STRENGTH:2;"
    private String buildEffectsString(Map<String, Integer> effects) {
        if (effects.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : effects.entrySet()) {
            builder.append(entry.getKey()).append(":").append(entry.getValue()).append(";");
        }

        return builder.toString();
    }
}
