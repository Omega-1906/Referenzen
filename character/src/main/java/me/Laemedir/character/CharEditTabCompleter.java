package me.Laemedir.character;

import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CharEditTabCompleter implements TabCompleter {
    
    private final CoreAPIPlugin coreAPI;
    
    public CharEditTabCompleter(CoreAPIPlugin coreAPI) {
        this.coreAPI = coreAPI;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        
        Player player = (Player) sender;
        
        // Überprüfe Permission
        if (!player.hasPermission("laemedir.team")) {
            return new ArrayList<>();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Erstes Argument: Charaktername
            String searchTerm = args[0];
            
            // Entferne Anführungszeichen für die Suche
            if (searchTerm.startsWith("\"")) {
                searchTerm = searchTerm.substring(1);
            }
            
            List<String> characterNames = getCharacterNames(searchTerm);
            
            // Füge Anführungszeichen zu Namen mit Leerzeichen hinzu
            for (String name : characterNames) {
                if (name.contains(" ")) {
                    completions.add("\"" + name + "\"");
                } else {
                    completions.add(name);
                }
            }
        } else if (args.length == 2) {
            // Zweites Argument: Feld
            List<String> fields = Arrays.asList(
                "deckname", "rufname", "verwandlung", "affinity", "affinität", "kategorie",
                "alter", "geschlecht",
                "profil", "staerken", "stärken", "schwaechen", "schwächen", "geschichte", "eigenschaften",
                "status", "gamemode"
            );
            
            completions.addAll(fields.stream()
                .filter(field -> field.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList()));
                
        } else if (args.length == 3) {
            // Drittes Argument: Wert-Vorschläge
            String field = args[1].toLowerCase();
            
            switch (field) {
                case "kategorie":
                    completions.addAll(Arrays.asList("name", "deckname", "rufname"));
                    break;
                case "geschlecht":
                    completions.addAll(Arrays.asList("männlich", "weiblich", "divers", "unbekannt", "Mann", "Frau"));
                    break;
                case "status":
                    completions.addAll(Arrays.asList("0", "1", "2"));
                    break;
            case "affinity":
            case "affinität":
                completions.addAll(Arrays.asList("Erde", "Wasser", "Luft", "Feuer"));
                break;
            case "gamemode":
                completions.addAll(Arrays.asList("SURVIVAL", "CREATIVE", "ADVENTURE", "SPECTATOR"));
                break;
            case "alter":
                // Beispiele für häufige Alter
                completions.addAll(Arrays.asList("18", "20", "25", "30", "35", "40", "50", "100", "200", "500"));
                break;
            default:
                // Für Textfelder keine Vorschläge
                break;
            }
            
            // Filtere basierend auf bereits eingegebenen Text
            completions = completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
                
        } else if (args.length >= 4) {
            // Erweiterte Argumente für verschiedene Felder
            handleExtendedArguments(args, completions);
        }
        
        return completions;
    }
    
    /**
     * Behandelt erweiterte Argumente für verschiedene Felder
     */
    private void handleExtendedArguments(String[] args, List<String> completions) {
        // Parse Argumente um mit Anführungszeichen umzugehen
        ParseResult parseResult = parseArgsForCompletion(args);
        
        if (parseResult == null) {
            return;
        }
        
        String field = parseResult.field.toLowerCase();
        int currentArgIndex = parseResult.currentArgIndex;
        String currentInput = parseResult.currentInput;
        
        switch (field) {
            case "status":
                handleStatusCompletion(args, currentArgIndex, currentInput, completions, parseResult.fieldArgIndex);
                break;
            case "kategorie":
                if (currentArgIndex == parseResult.fieldArgIndex + 1) {
                    addFilteredCompletions(Arrays.asList("name", "deckname", "rufname"), currentInput, completions);
                }
                break;
            case "gamemode":
                if (currentArgIndex == parseResult.fieldArgIndex + 1) {
                    addFilteredCompletions(Arrays.asList("SURVIVAL", "CREATIVE", "ADVENTURE", "SPECTATOR"), currentInput, completions);
                }
                break;
            case "affinity":
            case "affinität":
                if (currentArgIndex == parseResult.fieldArgIndex + 1) {
                    addFilteredCompletions(Arrays.asList("Erde", "Wasser", "Luft", "Feuer"), currentInput, completions);
                }
                break;
            case "geschlecht":
                if (currentArgIndex == parseResult.fieldArgIndex + 1) {
                    addFilteredCompletions(Arrays.asList("männlich", "weiblich", "divers", "unbekannt"), currentInput, completions);
                }
                break;
            case "alter":
                if (currentArgIndex == parseResult.fieldArgIndex + 1) {
                    addFilteredCompletions(Arrays.asList("18", "20", "25", "30", "35", "40", "50", "100", "200", "500"), currentInput, completions);
                }
                break;
            case "deckname":
            case "verwandlung":
            case "profil":
            case "staerken":
            case "stärken":
            case "schwaechen":
            case "schwächen":
            case "geschichte":
            case "eigenschaften":
                // Keine Tab-Completion für individuelle/kreative Antworten
                // Nur das Feld selbst wird vervollständigt, nicht die Werte
                break;
        }
    }
    
    /**
     * Behandelt Status-spezifische Tab-Completion
     */
    private void handleStatusCompletion(String[] args, int currentArgIndex, String currentInput, List<String> completions, int fieldArgIndex) {
        if (currentArgIndex == fieldArgIndex + 1) {
            // Status-Codes
            addFilteredCompletions(Arrays.asList("0", "1", "2"), currentInput, completions);
        } else if (currentArgIndex == fieldArgIndex + 2) {
            // Grund basierend auf Status
            if (args.length > fieldArgIndex + 1) {
                String statusCode = args[fieldArgIndex + 1];
                switch (statusCode) {
                    case "0":
                        addFilteredCompletions(Arrays.asList(
                            "Inaktiv", "Pausiert", "Temporär_deaktiviert", 
                            "Urlaub", "Charakterpause", "Story-Pause"
                        ), currentInput, completions);
                        break;
                    case "2":
                        addFilteredCompletions(Arrays.asList(
                            "Regelverstoß", "Unter_Überprüfung", "Administrative_Sperre",
                            "Griefing", "Trolling", "Unpassendes_Verhalten", "Meta-Gaming",
                            "PowerRP", "FailRP", "RDM", "VDM"
                        ), currentInput, completions);
                        break;
                }
            }
        } else if (currentArgIndex > fieldArgIndex + 2) {
            // Weitere Grund-Argumente für mehrteilige Gründe
            addFilteredCompletions(Arrays.asList(
                "und", "oder", "sowie", "aufgrund", "wegen", "durch", "mittels"
            ), currentInput, completions);
        }
    }
    
    // Text-Completion entfernt - individuelle Eingaben sollen nicht vorgegeben werden
    
    /**
     * Parst Argumente für Tab-Completion unter Berücksichtigung von Anführungszeichen
     */
    private ParseResult parseArgsForCompletion(String[] args) {
        String characterName = null;
        String field = null;
        int fieldArgIndex = -1;
        int currentArgIndex = args.length - 1;
        String currentInput = args[currentArgIndex];
        
        // Überprüfe, ob der erste Parameter mit Anführungszeichen beginnt
        if (args[0].startsWith("\"")) {
            // Suche das Ende der Anführungszeichen
            int endIndex = -1;
            
            if (args[0].endsWith("\"") && args[0].length() > 1) {
                // Name ist nur ein Wort in Anführungszeichen
                characterName = args[0].substring(1, args[0].length() - 1);
                endIndex = 1;
            } else {
                // Name erstreckt sich über mehrere Argumente
                StringBuilder nameBuilder = new StringBuilder(args[0].substring(1));
                
                for (int i = 1; i < args.length; i++) {
                    nameBuilder.append(" ").append(args[i]);
                    if (args[i].endsWith("\"")) {
                        characterName = nameBuilder.toString().substring(0, nameBuilder.length() - 1);
                        endIndex = i + 1;
                        break;
                    }
                }
                
                if (endIndex == -1) {
                    // Anführungszeichen noch nicht geschlossen
                    return null;
                }
            }
            
            if (endIndex < args.length) {
                field = args[endIndex];
                fieldArgIndex = endIndex;
            }
        } else {
            // Kein Anführungszeichen, normales Parsing
            characterName = args[0];
            if (args.length > 1) {
                field = args[1];
                fieldArgIndex = 1;
            }
        }
        
        return new ParseResult(characterName, field, fieldArgIndex, currentArgIndex, currentInput);
    }
    
    /**
     * Fügt gefilterte Completions hinzu
     */
    private void addFilteredCompletions(List<String> options, String currentInput, List<String> completions) {
        completions.addAll(options.stream()
            .filter(option -> option.toLowerCase().startsWith(currentInput.toLowerCase()))
            .collect(Collectors.toList()));
    }
    
    // Decknamen und Verwandlungs-Vorschläge entfernt - sollen individuell sein
    
    /**
     * Hilfsklasse für Parse-Ergebnisse
     */
    private static class ParseResult {
        final String characterName;
        final String field;
        final int fieldArgIndex;
        final int currentArgIndex;
        final String currentInput;
        
        ParseResult(String characterName, String field, int fieldArgIndex, int currentArgIndex, String currentInput) {
            this.characterName = characterName;
            this.field = field;
            this.fieldArgIndex = fieldArgIndex;
            this.currentArgIndex = currentArgIndex;
            this.currentInput = currentInput;
        }
    }
    
    /**
     * Holt Charakternamen aus der Datenbank für Auto-Completion
     */
    private List<String> getCharacterNames(String partial) {
        try {
            String sql = "SELECT name FROM characters WHERE name LIKE ? LIMIT 10";
            String searchPattern = partial + "%";
            
            return coreAPI.querySync(sql, rs -> {
                List<String> names = new ArrayList<>();
                try {
                    while (rs.next()) {
                        names.add(rs.getString("name"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return names;
            }, searchPattern);
            
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
