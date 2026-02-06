package me.Laemedir.character;

import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command-Executor für den /charedit Befehl.
 * Erlaubt die Bearbeitung von Charakterdaten (für Admins/Teammitglieder).
 */
public class CharEditCommand implements CommandExecutor {
    
    private final MultiCharPlugin plugin;
    private final CoreAPIPlugin coreAPI;
    
    /**
     * Konstruktor für den CharEditCommand.
     *
     * @param plugin Die Instanz des MultiCharPlugins.
     * @param coreAPI Die Instanz der CoreAPI.
     */
    public CharEditCommand(MultiCharPlugin plugin, CoreAPIPlugin coreAPI) {
        this.plugin = plugin;
        this.coreAPI = coreAPI;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Überprüfe, ob der Sender ein Spieler ist
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Command kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Überprüfe Berechtigungen
        if (!player.hasPermission("laemedir.team")) {
            player.sendMessage("§cDu hast keine Berechtigung für diesen Command!");
            return true;
        }
        
        // Parse Argumente (unterstützt Anführungszeichen für Namen)
        ParsedArgs parsedArgs = parseArguments(args);
        
        if (parsedArgs == null || parsedArgs.characterName == null || parsedArgs.field == null || parsedArgs.value == null) {
            sendUsage(player);
            return true;
        }
        
        String characterName = parsedArgs.characterName;
        String field = parsedArgs.field.toLowerCase();
        String value = parsedArgs.value;
        
        // Überprüfe, ob Charakter existiert
        if (!characterExists(characterName)) {
            player.sendMessage("§cCharakter '" + characterName + "' wurde nicht gefunden!");
            return true;
        }
        
        // Führe die entsprechende Bearbeitung aus
        executeEdit(player, characterName, field, value, parsedArgs.originalArgs);
        
        return true;
    }
    
    /**
     * Führt die Charakterbearbeitung aus.
     */
    private void executeEdit(Player player, String characterName, String field, String value, String[] args) {
        try {
            switch (field) {
                // Namen-Kategorie
                case "deckname":
                    updateField(player, characterName, "deckname", value, "Deckname");
                    break;
                case "rufname":
                    updateField(player, characterName, "rufname", value, "Rufname");
                    break;
                case "verwandlung":
                    updateField(player, characterName, "verwandlung", value, "Verwandlung");
                    break;
                case "affinity":
                case "affinität":
                    updateAffinity(player, characterName, value);
                    break;
                case "kategorie":
                    updateNameCategory(player, characterName, value);
                    break;
                    
                // Gameplay-Kategorie
                case "alter":
                    updateAge(player, characterName, value);
                    break;
                case "geschlecht":
                    updateField(player, characterName, "gender", value, "Geschlecht");
                    break;
                    
                // Text-Kategorie
                case "profil":
                    updateTextField(player, characterName, "appearance", value, "Charakter Profil");
                    break;
                case "staerken":
                case "stärken":
                    updateTextField(player, characterName, "strengths", value, "Stärken");
                    break;
                case "schwaechen":
                case "schwächen":
                    updateTextField(player, characterName, "weaknesses", value, "Schwächen");
                    break;
                case "geschichte":
                    updateTextField(player, characterName, "background_story", value, "Hintergrundgeschichte");
                    break;
                case "eigenschaften":
                    updateTextField(player, characterName, "character_traits", value, "Charaktereigenschaften");
                    break;
                    
                // Status-Kategorie
                case "status":
                    updateStatus(player, characterName, args);
                    break;
                case "gamemode":
                    updateGamemode(player, characterName, value);
                    break;
                    
                default:
                    player.sendMessage("§cUnbekanntes Feld: " + field);
                    sendUsage(player);
                    break;
            }
        } catch (Exception e) {
            player.sendMessage("§cFehler beim Bearbeiten: " + e.getMessage());
            if (plugin.getDebugManager() != null) {
                plugin.getDebugManager().error("character", "Char Edit", "Fehler beim Bearbeiten von Charakter " + characterName, e);
            }
        }
    }
    
    /**
     * Aktualisiert ein einfaches Textfeld
     */
    private void updateField(Player player, String characterName, String dbField, String value, String displayName) {
        // Whitelist für erlaubte DB-Felder (SQL-Injection Schutz)
        if (!isValidDbField(dbField)) {
            player.sendMessage("§cFehler: Ungültiges Datenbankfeld!");
            if (plugin.getDebugManager() != null) {
                plugin.getDebugManager().warning("character", "Char Edit", "Versuch eines ungültigen DB-Feldzugriffs: " + dbField + " von " + player.getName());
            }
            return;
        }
        
        String sql = "UPDATE characters SET " + dbField + " = ? WHERE name = ?";
        
        coreAPI.executeUpdateWithCallbackAsync(sql, result -> {
            if (result) {
                player.sendMessage("§a" + displayName + " für §e" + characterName + "§a wurde erfolgreich auf §f'" + value + "'§a gesetzt!");
            } else {
                player.sendMessage("§cFehler beim Aktualisieren von " + displayName + " für " + characterName);
            }
        }, value, characterName);
    }
    
    /**
     * Validiert DB-Feldnamen gegen Whitelist (SQL-Injection Schutz)
     */
    private boolean isValidDbField(String dbField) {
        return dbField.equals("deckname") || dbField.equals("rufname") || dbField.equals("verwandlung") ||
               dbField.equals("affinity") || dbField.equals("active_name_category") || dbField.equals("gender") ||
               dbField.equals("gamemode");
    }
    /**
     * Aktualisiert ein Textfeld und konvertiert \\n zu echten Zeilenumbrüchen
     */
    private void updateTextField(Player player, String characterName, String dbField, String value, String displayName) {
        // Whitelist für erlaubte Text-DB-Felder (SQL-Injection Schutz)
        if (!isValidTextDbField(dbField)) {
            player.sendMessage("§cFehler: Ungültiges Datenbankfeld!");
            if (plugin.getDebugManager() != null) {
                plugin.getDebugManager().warning("character", "Char Edit", "Versuch eines ungültigen Text-DB-Feldzugriffs: " + dbField + " von " + player.getName());
            }
            return;
        }
        
        // Konvertiere \\\\n zu echten Zeilenumbrüchen
        String formattedValue = value.replace("\\\\n", "\n");
        
        String sql = "UPDATE characters SET " + dbField + " = ? WHERE name = ?";
        
        coreAPI.executeUpdateWithCallbackAsync(sql, result -> {
            if (result) {
                player.sendMessage("§a" + displayName + " für §e" + characterName + "§a wurde erfolgreich aktualisiert!");
                player.sendMessage("§7Neuer Inhalt: §f" + formattedValue.replace("\n", " "));
            } else {
                player.sendMessage("§cFehler beim Aktualisieren von " + displayName + " für " + characterName);
            }
        }, formattedValue, characterName);
    }
    
    /**
     * Validiert Text-DB-Feldnamen gegen Whitelist (SQL-Injection Schutz)
     */
    private boolean isValidTextDbField(String dbField) {
        return dbField.equals("appearance") || dbField.equals("strengths") || dbField.equals("weaknesses") ||
               dbField.equals("background_story") || dbField.equals("character_traits");
    }
    
    /**
     * Aktualisiert die Affinität mit Validierung
     */
    private void updateAffinity(Player player, String characterName, String affinity) {
        if (affinity == null || affinity.trim().isEmpty()) {
            player.sendMessage("§cAffinität darf nicht leer sein! Erlaubt: Erde, Wasser, Luft, Feuer");
            return;
        }
        
        // Normalisiere Eingabe (erster Buchstabe groß, Rest klein)
        String trimmedAffinity = affinity.trim();
        String normalizedAffinity = trimmedAffinity.substring(0, 1).toUpperCase() + trimmedAffinity.substring(1).toLowerCase();
        
        if (!normalizedAffinity.equals("Erde") && !normalizedAffinity.equals("Wasser") && 
            !normalizedAffinity.equals("Luft") && !normalizedAffinity.equals("Feuer")) {
            player.sendMessage("§cUngültige Affinität! Erlaubt: Erde, Wasser, Luft, Feuer");
            player.sendMessage("§7Eingegeben wurde: '" + affinity + "'");
            return;
        }
        
        updateField(player, characterName, "affinity", normalizedAffinity, "Affinität");
    }
    
    /**
     * Aktualisiert die aktive Namenskategorie
     */
    private void updateNameCategory(Player player, String characterName, String category) {
        if (!category.equals("name") && !category.equals("deckname") && !category.equals("rufname")) {
            player.sendMessage("§cUngültige Kategorie! Erlaubt: name, deckname, rufname");
            return;
        }
        
        updateField(player, characterName, "active_name_category", category, "Aktive Namenskategorie");
    }
    
    /**
     * Aktualisiert das Alter
     */
    private void updateAge(Player player, String characterName, String ageStr) {
        try {
            int age = Integer.parseInt(ageStr);
            if (age < 0 || age > 1000) {
                player.sendMessage("§cUngültiges Alter! Erlaubt: 0-1000");
                return;
            }
            
            String sql = "UPDATE characters SET age = ? WHERE name = ?";
            
            coreAPI.executeUpdateWithCallbackAsync(sql, result -> {
                if (result) {
                    player.sendMessage("§aAlter für §e" + characterName + "§a wurde erfolgreich auf §f" + age + "§a Jahre gesetzt!");
                } else {
                    player.sendMessage("§cFehler beim Aktualisieren des Alters für " + characterName);
                }
            }, age, characterName);
            
        } catch (NumberFormatException e) {
            player.sendMessage("§cUngültiges Alter! Bitte gib eine Zahl ein.");
        }
    }
    
    /**
     * Aktualisiert den Status
     */
    private void updateStatus(Player player, String characterName, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUsage: /charedit " + characterName + " status <0|1|2> [grund]");
            return;
        }
        
        try {
            int status = Integer.parseInt(args[2]);
            if (status < 0 || status > 2) {
                player.sendMessage("§cUngültiger Status! Erlaubt: 0 (Deaktiviert), 1 (Aktiv), 2 (Gesperrt)");
                return;
            }
            
            String reason = "";
            if (args.length > 3) {
                StringBuilder reasonBuilder = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (i > 3) reasonBuilder.append(" ");
                    reasonBuilder.append(args[i]);
                }
                reason = reasonBuilder.toString();
            }
            
            // Update Status und ggf. Grund
            String sql;
            if (status == 0 || status == 2) {
                // Deaktiviert oder Gesperrt - setze auch Grund
                sql = "UPDATE characters SET status = ?, deactivation_reason = ? WHERE name = ?";
                String finalReason = reason.isEmpty() ? "Kein Grund angegeben" : reason;
                
                coreAPI.executeUpdateWithCallbackAsync(sql, result -> {
                    if (result) {
                        String statusText = status == 0 ? "Deaktiviert" : "Gesperrt";
                        player.sendMessage("§aStatus für §e" + characterName + "§a wurde auf §f" + statusText + "§a gesetzt!");
                        player.sendMessage("§7Grund: §f" + finalReason);
                    } else {
                        player.sendMessage("§cFehler beim Aktualisieren des Status für " + characterName);
                    }
                }, status, finalReason, characterName);
            } else {
                // Aktiv - lösche Grund
                sql = "UPDATE characters SET status = ?, deactivation_reason = NULL WHERE name = ?";
                
                coreAPI.executeUpdateWithCallbackAsync(sql, result -> {
                    if (result) {
                        player.sendMessage("§aStatus für §e" + characterName + "§a wurde auf §fAktiv§a gesetzt!");
                    } else {
                        player.sendMessage("§cFehler beim Aktualisieren des Status für " + characterName);
                    }
                }, status, characterName);
            }
            
        } catch (NumberFormatException e) {
            player.sendMessage("§cUngültiger Status! Bitte gib eine Zahl (0, 1 oder 2) ein.");
        }
    }
    
    /**
     * Aktualisiert den Gamemode
     */
    private void updateGamemode(Player player, String characterName, String gamemode) {
        String normalizedGamemode = gamemode.toUpperCase();
        
        if (!normalizedGamemode.equals("SURVIVAL") && !normalizedGamemode.equals("CREATIVE") && 
            !normalizedGamemode.equals("ADVENTURE") && !normalizedGamemode.equals("SPECTATOR")) {
            player.sendMessage("§cUngültiger Gamemode! Erlaubt: SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR");
            return;
        }
        
        updateField(player, characterName, "gamemode", normalizedGamemode, "Gamemode");
    }
    
    /**
     * Überprüft, ob ein Charakter existiert
     */
    private boolean characterExists(String characterName) {
        try {
            String sql = "SELECT COUNT(*) as count FROM characters WHERE name = ?";
            return coreAPI.querySync(sql, rs -> {
                try {
                    if (rs.next()) {
                        return rs.getInt("count") > 0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }, characterName);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Parst die Argumente und unterstützt Anführungszeichen für Namen
     */
    private ParsedArgs parseArguments(String[] args) {
        if (args.length < 3) {
            return null;
        }
        
        String characterName = null; // Initialisierung hinzugefügt
        String field;
        String value;
        String[] remainingArgs;
        
        // Überprüfe, ob der erste Parameter mit Anführungszeichen beginnt
        if (args[0].startsWith("\"")) {
            // Suche das Ende der Anführungszeichen
            StringBuilder nameBuilder = new StringBuilder();
            int endIndex = -1;
            
            // Entferne das erste Anführungszeichen
            nameBuilder.append(args[0].substring(1));
            
            if (args[0].endsWith("\"") && args[0].length() > 1) {
                // Name ist nur ein Wort in Anführungszeichen
                characterName = nameBuilder.toString().substring(0, nameBuilder.length() - 1);
                endIndex = 1;
            } else {
                // Name erstreckt sich über mehrere Argumente
                for (int i = 1; i < args.length; i++) {
                    nameBuilder.append(" ").append(args[i]);
                    if (args[i].endsWith("\"")) {
                        // Ende der Anführungszeichen gefunden
                        characterName = nameBuilder.toString().substring(0, nameBuilder.length() - 1);
                        endIndex = i + 1;
                        break;
                    }
                }
                
                if (endIndex == -1) {
                    return null; // Anführungszeichen nicht geschlossen
                }
            }
            
            // Überprüfe, ob genug Argumente für Feld und Wert übrig sind
            if (endIndex >= args.length - 1) {
                return null; // Nicht genug Parameter
            }
            
            // Spezialfall: Status Command benötigt mindestens 2 weitere Parameter
            if (endIndex < args.length && args[endIndex].equalsIgnoreCase("status") && endIndex >= args.length - 2) {
                return null; // Status braucht mindestens einen Wert
            }
            
            field = args[endIndex];
            
            // Wert aus den restlichen Argumenten zusammensetzen
            StringBuilder valueBuilder = new StringBuilder();
            for (int i = endIndex + 1; i < args.length; i++) {
                if (i > endIndex + 1) valueBuilder.append(" ");
                valueBuilder.append(args[i]);
            }
            value = valueBuilder.toString();
            
            // Erstelle neues Array für Status-Commands (die das vollständige args-Array benötigen)
            // Format: [characterName, field, param1, param2, ...]
            int totalParams = args.length - endIndex + 1; // +1 für characterName
            remainingArgs = new String[totalParams];
            remainingArgs[0] = characterName;
            remainingArgs[1] = field;
            
            // Kopiere alle Parameter nach dem Feld
            for (int i = endIndex + 1; i < args.length; i++) {
                int targetIndex = i - endIndex + 1;
                remainingArgs[targetIndex] = args[i];
            }
            
        } else {
            // Kein Anführungszeichen, normales Parsing
            characterName = args[0];
            field = args[1];
            
            // Wert aus den restlichen Argumenten zusammensetzen
            StringBuilder valueBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) valueBuilder.append(" ");
                valueBuilder.append(args[i]);
            }
            value = valueBuilder.toString();
            
            remainingArgs = args;
        }
        
        return new ParsedArgs(characterName, field, value, remainingArgs);
    }
    
    /**
     * Hilfsklasse für geparste Argumente
     */
    private static class ParsedArgs {
        final String characterName;
        final String field;
        final String value;
        final String[] originalArgs;
        
        ParsedArgs(String characterName, String field, String value, String[] originalArgs) {
            this.characterName = characterName;
            this.field = field;
            this.value = value;
            this.originalArgs = originalArgs;
        }
    }
    
    /**
     * Zeigt die Verwendung des Commands
     */
    private void sendUsage(Player player) {
        player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§e§lCharEdit Command - Usage:");
        player.sendMessage("§7Für Namen mit Leerzeichen: §e/charedit \"Max Mustermann\" <feld> <wert>");
        player.sendMessage("§7Für Namen ohne Leerzeichen: §e/charedit Hans <feld> <wert>");
        player.sendMessage("");
        player.sendMessage("§7Namen: §e/charedit <char> deckname|rufname|verwandlung|affinity|kategorie <wert>");
        player.sendMessage("§7Gameplay: §e/charedit <char> alter|geschlecht <wert>");
        player.sendMessage("§7Texte: §e/charedit <char> profil|staerken|schwaechen|geschichte|eigenschaften <text>");
        player.sendMessage("§7Status: §e/charedit <char> status <0|1|2> [grund]");
        player.sendMessage("§7Gamemode: §e/charedit <char> gamemode <survival|creative|adventure|spectator>");
        player.sendMessage("§7§oHinweis: Für Zeilenumbrüche in Texten verwende \\n");
        player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
}
