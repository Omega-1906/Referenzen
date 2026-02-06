package me.Laemedir.character;

import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Command-Executor für den /persona Befehl.
 * Zeigt Steckbriefe (Personas) von Charakteren an und erlaubt das Anpassen von Farben/Prefixen.
 */
public class PersonaCommand implements CommandExecutor, TabCompleter {

    private final MultiCharPlugin plugin;
    private final CoreAPIPlugin coreAPI;
    private final String prefix;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ZoneId serverZone = ZoneId.of("Europe/Berlin"); // Zeitzone für Deutschland

    /**
     * Konstruktor für den PersonaCommand.
     *
     * @param plugin Die Instanz des MultiCharPlugins.
     * @param coreAPI Die Instanz der CoreAPI.
     */
    public PersonaCommand(MultiCharPlugin plugin, CoreAPIPlugin coreAPI) {
        this.plugin = plugin;
        this.coreAPI = coreAPI;
        this.prefix = "§8[§6Characters§8] ";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + "§c⛔ Nur Spieler können diesen Befehl ausführen.");
            return true;
        }

        Player player = (Player) sender;

        // Wenn keine Argumente angegeben wurden oder nur ein Argument, das kein Unterbefehl ist
        if (args.length == 0) {
            // Zeige den Steckbrief des aktiven Charakters
            String activeCharName = MultiCharPlugin.getActiveCharacter(player.getUniqueId());
            if (activeCharName == null) {
                player.sendMessage(prefix + "§c⚠️ Du hast keinen aktiven Charakter.");
                return true;
            }
            displayCharacterProfile(player, activeCharName, player.getUniqueId().toString());
            return true;
        } else if (args.length == 1) {
            // Prüfe, ob das Argument ein bekannter Unterbefehl ist
            String potentialSubCommand = args[0].toLowerCase();
            if (!isKnownSubCommand(potentialSubCommand)) {
                // Wenn nicht, behandle es als Minecraft-Namen
                Player targetPlayer = plugin.getServer().getPlayer(args[0]);
                if (targetPlayer == null) {
                    player.sendMessage(prefix + "§c❌ Spieler '§e" + args[0] + "§c' wurde nicht gefunden oder ist offline.");
                    return true;
                }

                String activeCharName = MultiCharPlugin.getActiveCharacter(targetPlayer.getUniqueId());
                if (activeCharName == null) {
                    player.sendMessage(prefix + "§c⚠️ Der Spieler '§e" + args[0] + "§c' hat keinen aktiven Charakter.");
                    return true;
                }

                displayCharacterProfile(player, activeCharName, targetPlayer.getUniqueId().toString());
                return true;
            } else {
                // Es ist ein bekannter Unterbefehl, aber es fehlt das zweite Argument
                sendUsageMessage(player);
                return true;
            }
        }

        String subCommand = args[0].toLowerCase();
        String content = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // Hole den aktiven Charakter des Spielers
        String activeCharName = MultiCharPlugin.getActiveCharacter(player.getUniqueId());
        if (activeCharName == null) {
            player.sendMessage(prefix + "§c⚠️ Du hast keinen aktiven Charakter.");
            return true;
        }

        switch (subCommand) {
            case "prefix":
                handleCharPrefix(player, activeCharName, content);
                break;
            case "prefixcolour":
                if (args.length < 2) {
                    player.sendMessage(prefix + "§c⚠️ Verwendung: /persona prefixcolour <DarkRed/DarkBlue/DarkAqua/DarkGreen/DarkPurple>");
                    return true;
                }
                handlePrefixColour(player, activeCharName, args[1]);
                break;
            case "chatcolour":
                if (args.length < 2) {
                    player.sendMessage(prefix + "§c⚠️ Verwendung: /persona chatcolour <Red/Blue/Green/Purple>");
                    return true;
                }
                handleChatColour(player, activeCharName, args[1]);
                break;
            default:
                player.sendMessage(prefix + "§c❓ Unbekannter Befehl. Verwende einen der folgenden Befehle:");
                sendUsageMessage(player);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Unterbefehle
            List<String> subCommands = Arrays.asList("prefix", "prefixcolour", "chatcolour");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);

            // Auch Online-Spieler für Steckbrief-Anzeige vorschlagen
            List<String> onlinePlayers = new ArrayList<>();
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                onlinePlayers.add(onlinePlayer.getName());
            }
            StringUtil.copyPartialMatches(args[0], onlinePlayers, completions);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("prefixcolour")) {
                List<String> colors = Arrays.asList("DarkRed", "DarkBlue", "DarkAqua", "DarkGreen", "DarkPurple");
                StringUtil.copyPartialMatches(args[1], colors, completions);
            } else if (subCommand.equals("chatcolour")) {
                List<String> colors = Arrays.asList("Red", "Blue", "Green", "Purple");
                StringUtil.copyPartialMatches(args[1], colors, completions);
            }
        }

        return completions;
    }

    /**
     * Prüft, ob ein String ein bekannter Unterbefehl ist
     */
    private boolean isKnownSubCommand(String cmd) {
        List<String> knownCommands = Arrays.asList(
                "prefix", "prefixcolour", "chatcolour"
        );
        return knownCommands.contains(cmd.toLowerCase());
    }

    private void displayCharacterProfile(Player viewer, String characterName, String characterOwnerUuid) {
        // Korrigierte SQL-Abfrage - Beachte die Spaltenaliase
        String query = "SELECT c.*, r.id as race_id, r.race_name, r.description, " +
                "r.effects, c.first_login " +
                "FROM characters c " +
                "LEFT JOIN races r ON c.race_id = r.id " +
                "WHERE c.name = ?";

        coreAPI.queryAsync(query, results -> {
            if (results.isEmpty()) {
                viewer.sendMessage(prefix + "§c❌ Charakter '§e" + characterName + "§c' wurde nicht gefunden.");
                return;
            }

            Map<String, Object> characterData = results.get(0);

            // Charakter-Basisdaten extrahieren
            CharacterProfileData profile = extractCharacterData(characterData);

            // Rassen-Informationen
            RaceData raceData = new RaceData();
            raceData.id = characterData.get("race_id") != null ? ((Number) characterData.get("race_id")).intValue() : 0;
            raceData.name = characterData.get("race_name") != null ? characterData.get("race_name").toString() : null;
            raceData.description = characterData.get("description") != null ? characterData.get("description").toString() : null;
            raceData.effects = characterData.get("effects") != null ? characterData.get("effects").toString() : null;

            // Berechne den nächsten Geburtstag, falls first_login gesetzt ist
            Object firstLoginObj = characterData.get("first_login");
            if (firstLoginObj != null) {
                try {
                    LocalDateTime firstLogin;

                    // Verarbeite verschiedene mögliche Typen für first_login
                    if (firstLoginObj instanceof LocalDateTime) {
                        firstLogin = (LocalDateTime) firstLoginObj;
                    } else if (firstLoginObj instanceof String) {
                        firstLogin = LocalDateTime.parse((String) firstLoginObj, dateTimeFormatter);
                    } else {
                        if (plugin.getDebugManager() != null) {
                            plugin.getDebugManager().warning("character", "Persona", "Unbekannter Typ für first_login: " + firstLoginObj.getClass().getName());
                        }
                        firstLogin = null;
                    }

                    if (firstLogin != null) {
                        // Füge 2 Stunden hinzu, um die Zeitzonenverschiebung zu korrigieren
                        firstLogin = firstLogin.plusHours(2);
                        profile.nextBirthday = calculateNextMonthlyBirthday(firstLogin);
                    }
                } catch (Exception e) {
                    if (plugin.getDebugManager() != null) {
                        plugin.getDebugManager().error("character", "Persona", "Fehler beim Berechnen des nächsten Geburtstags", e);
                    }
                }
            }

            // Zeige das formatierte Profil an
            displayFormattedProfile(viewer, profile, raceData, characterOwnerUuid);
        }, characterName);
    }

    /**
     * Berechnet das Datum des nächsten monatlichen Geburtstags basierend auf dem first_login
     */
    private String calculateNextMonthlyBirthday(LocalDateTime firstLogin) {
        // Aktuelle Zeit
        LocalDateTime now = LocalDateTime.now();

        // Extrahiere den Tag des Monats vom first_login
        int birthDay = firstLogin.getDayOfMonth();

        // Erstelle ein Datum für den aktuellen Monat mit dem Geburtstag
        LocalDate birthdayThisMonth = LocalDate.of(
                now.getYear(),
                now.getMonth(),
                Math.min(birthDay, now.getMonth().length(now.toLocalDate().isLeapYear()))
        );

        // Wenn der Geburtstag diesen Monat bereits vorbei ist, nehme nächsten Monat
        LocalDate currentDate = now.toLocalDate();
        if (birthdayThisMonth.isBefore(currentDate) || birthdayThisMonth.isEqual(currentDate)) {
            // Gehe zum nächsten Monat
            LocalDate nextMonth = currentDate.plusMonths(1);
            birthdayThisMonth = LocalDate.of(
                    nextMonth.getYear(),
                    nextMonth.getMonth(),
                    Math.min(birthDay, nextMonth.getMonth().length(nextMonth.isLeapYear()))
            );
        }

        // Berechne die Tage bis zum nächsten Geburtstag
        long daysUntilBirthday = ChronoUnit.DAYS.between(currentDate, birthdayThisMonth);

        // Formatiere das Datum für die Anzeige
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return birthdayThisMonth.format(displayFormatter) + " (in " + daysUntilBirthday + " Tagen)";
    }

    private CharacterProfileData extractCharacterData(Map<String, Object> data) {
        CharacterProfileData profile = new CharacterProfileData();
        profile.name = (String) data.get("name");
        profile.rufname = (String) data.get("rufname");
        profile.deckname = (String) data.get("deckname");
        profile.verwandlung = (String) data.get("verwandlung");
        profile.age = (Integer) data.get("age");
        profile.gender = (String) data.get("gender");
        profile.charPrefix = (String) data.get("char_prefix");
        profile.prefixColour = (String) data.get("prefix_colour");
        profile.chatColour = (String) data.get("chat_colour");
        // first_login wird separat verarbeitet, da es verschiedene Typen haben kann
        return profile;
    }

    private void displayFormattedProfile(Player viewer, CharacterProfileData profile, RaceData race, String characterOwnerUuid) {
        // Prüfe, ob der Betrachter der Besitzer des Charakters ist
        boolean isOwner = viewer.getUniqueId().toString().equals(characterOwnerUuid);

        viewer.sendMessage("");
        viewer.sendMessage("§8§m⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯");
        viewer.sendMessage("§6📋 §eSteckbrief von:");
        viewer.sendMessage("");
        viewer.sendMessage("§6✨ §l" + profile.name + "§r§6 ✨");
        viewer.sendMessage("");

        // Hauptinformationen
        viewer.sendMessage("§6👤 §eIdentität:");
        viewer.sendMessage("  §7• Name: §f" + profile.name);

        if (profile.rufname != null && !profile.rufname.isEmpty()) {
            viewer.sendMessage("  §7• Rufname: §f" + profile.rufname);
        }

        if (profile.deckname != null && !profile.deckname.isEmpty()) {
            viewer.sendMessage("  §7• Deckname: §f" + profile.deckname);
        }

        // Verwandlung nur für Besitzer anzeigen
        if (profile.verwandlung != null && !profile.verwandlung.isEmpty()) {
            if (isOwner) {
                viewer.sendMessage("  §7• Verwandlung: §f" + profile.verwandlung);
            } else {
                viewer.sendMessage("  §7• Verwandlung: §f???");
            }
        }

        viewer.sendMessage("");
        viewer.sendMessage("§6📋 §eDetails:");

        if (profile.age != null) {
            viewer.sendMessage("  §7• Alter: §f" + profile.age + " Jahre");

            // Zeige den nächsten Geburtstag an, wenn verfügbar
            if (profile.nextBirthday != null) {
                viewer.sendMessage("  §7• Nächster Geburtstag: §f" + profile.nextBirthday);
            }
        }

        if (profile.gender != null && !profile.gender.isEmpty()) {
            viewer.sendMessage("  §7• Geschlecht: §f" + profile.gender);
        }

        if (race != null && race.name != null && !race.name.isEmpty()) {
            viewer.sendMessage("  §7• Rasse: §f" + race.name);

            // Rassen-Beschreibung und Effekte
            if (race.description != null && !race.description.isEmpty()) {
                viewer.sendMessage("");
                viewer.sendMessage("§6📜 §eRassenbeschreibung:");
                viewer.sendMessage("  §f" + race.description);
            }

            if (race.effects != null && !race.effects.isEmpty()) {
                viewer.sendMessage("");
                viewer.sendMessage("§6✨ §eRasseneffekte:");
                viewer.sendMessage("  §f" + race.effects);
            }
        } else {
            viewer.sendMessage("  §7• Rasse: §fKeine Rasse zugewiesen");
        }

        if (profile.charPrefix != null && !profile.charPrefix.isEmpty()) {
            viewer.sendMessage("");
            viewer.sendMessage("  §7• Prefix: §f" + profile.charPrefix);
        }

        // Zeige Farben an
        if (profile.prefixColour != null && !profile.prefixColour.isEmpty()) {
            viewer.sendMessage("  §7• Prefix-Farbe: §f" + profile.prefixColour);
        }

        if (profile.chatColour != null && !profile.chatColour.isEmpty()) {
            viewer.sendMessage("  §7• Chat-Farbe: §f" + profile.chatColour);
        }

        viewer.sendMessage("§8§m⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯");
        viewer.sendMessage("");
    }

    // Datenklassen für bessere Struktur
    private static class CharacterProfileData {
        String name;
        String rufname;
        String deckname;
        String verwandlung;
        Integer age;
        String gender;
        String charPrefix;
        String prefixColour;
        String chatColour;
        String nextBirthday;
    }

    private static class RaceData {
        Integer id;
        String name;
        String description;
        String effects;
    }

    private void sendUsageMessage(Player player) {
        player.sendMessage("§8§m                                                §r");
        player.sendMessage("§6✨ §lPersona Befehle §r§6✨");
        player.sendMessage("§f/persona §8» §7Zeigt deinen Charaktersteckbrief");
        player.sendMessage("§f/persona <Minecraft-Name> §8» §7Zeigt den Steckbrief des aktiven Charakters eines Spielers");
        player.sendMessage("");
        player.sendMessage("§6👤 §lSpieler Befehle:");
        player.sendMessage("§f/persona prefix <Prefix> §8» §7Ändert den Prefix deines Charakters");
        player.sendMessage("§f/persona prefixcolour <Farbe> §8» §7Ändert die Farbe deines Prefix");
        player.sendMessage("§f/persona chatcolour <Farbe> §8» §7Ändert die Farbe deiner Taten (*)");
        player.sendMessage("§8§m                                                §r");
    }

    // Handler-Methoden für die verbleibenden Spieler-Commands

    private void handleCharPrefix(Player player, String characterName, String prefix) {
        if (!player.hasPermission("laemedir.stammgast") && !player.hasPermission("laemedir.team")) {
            player.sendMessage(this.prefix + "§c⛔ Du hast keine Berechtigung, den Prefix zu ändern.");
            return;
        }

        String updateQuery = "UPDATE characters SET char_prefix = ? WHERE player_uuid = ? AND name = ?";
        coreAPI.executeUpdateAsync(updateQuery, prefix, player.getUniqueId().toString(), characterName);
        player.sendMessage(this.prefix + "§a✅ Der Prefix deines Charakters wurde zu §e" + prefix + " §ageändert.");
    }

    private void handlePrefixColour(Player player, String characterName, String colour) {
        // Überprüfe Berechtigungen
        if (!player.hasPermission("laemedir.stammgast") && !player.hasPermission("laemedir.team")) {
            player.sendMessage(this.prefix + "§c⛔ Du hast keine Berechtigung, die Prefix-Farbe zu ändern.");
            return;
        }

        // Validiere die Farbe
        String[] validPrefixColours = {"DarkRed", "DarkBlue", "DarkAqua", "DarkGreen", "DarkPurple"};
        boolean isValid = false;
        for (String validColour : validPrefixColours) {
            if (validColour.equalsIgnoreCase(colour)) {
                isValid = true;
                colour = validColour; // Normalisiere die Schreibweise
                break;
            }
        }

        if (!isValid) {
            player.sendMessage(prefix + "§c⚠️ Ungültige Farbe! Verfügbare Farben: DarkRed, DarkBlue, DarkAqua, DarkGreen, DarkPurple");
            return;
        }

        String updateQuery = "UPDATE characters SET prefix_colour = ? WHERE player_uuid = ? AND name = ?";
        coreAPI.executeUpdateAsync(updateQuery, colour, player.getUniqueId().toString(), characterName);
        player.sendMessage(prefix + "§a✅ Die Prefix-Farbe deines Charakters wurde zu §e" + colour + " §ageändert.");
    }

    private void handleChatColour(Player player, String characterName, String colour) {
        // Überprüfe Berechtigungen
        if (!player.hasPermission("laemedir.stammgast") && !player.hasPermission("laemedir.team")) {
            player.sendMessage(this.prefix + "§c⛔ Du hast keine Berechtigung, die Chat-Farbe zu ändern.");
            return;
        }

        // Validiere die Farbe
        String[] validChatColours = {"Red", "Blue", "Green", "Purple"};
        boolean isValid = false;
        for (String validColour : validChatColours) {
            if (validColour.equalsIgnoreCase(colour)) {
                isValid = true;
                colour = validColour; // Normalisiere die Schreibweise
                break;
            }
        }

        if (!isValid) {
            player.sendMessage(prefix + "§c⚠️ Ungültige Farbe! Verfügbare Farben: Red, Blue, Green, Purple");
            return;
        }

        String updateQuery = "UPDATE characters SET chat_colour = ? WHERE player_uuid = ? AND name = ?";
        coreAPI.executeUpdateAsync(updateQuery, colour, player.getUniqueId().toString(), characterName);
        player.sendMessage(prefix + "§a✅ Die Chat-Farbe (Taten) deines Charakters wurde zu §e" + colour + " §ageändert.");
    }

}
