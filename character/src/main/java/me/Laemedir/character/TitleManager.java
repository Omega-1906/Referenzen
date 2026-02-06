package me.Laemedir.character;

import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Verwaltet die Charakter-Titel (Prefixe/Suffixe) und deren Kategorien.
 * Ermöglicht das Laden, Zuweisen und Entfernen von Titeln sowie das Umschalten ihres Status.
 */
public class TitleManager {
    
    private final CoreAPIPlugin coreAPI;
    private final MultiCharPlugin plugin;
    
    // Titel-Definitionen nach Kategorien
    public static final Map<String, List<String>> TITLE_CATEGORIES = new HashMap<>();
    
    static {
        // Fadenmagie-Titel
        TITLE_CATEGORIES.put("Fadenmagie", Arrays.asList(
            "Fadenmeister", "Lehrmeister", "Fadenklinger", "Elementenschneider", 
            "Seelenwirker", "Alchemist", "Verbotener Wirker", "Fadenweber", 
            "Fadenseher", "Novize I", "Novize II", "Novize III"
        ));
        
        // Drachentitel
        TITLE_CATEGORIES.put("Drachen", Arrays.asList(
            "Feuerdrache", "Wasserdrache", "Erddrache", "Luftdrache", "Drachenreiter"
        ));
        
        // Adelstitel
        TITLE_CATEGORIES.put("Adel", Arrays.asList(
            "Hoher Adel", "Niederer Adel", "Volk"
        ));
        
        // Fluchtitel
        TITLE_CATEGORIES.put("Fluch", Arrays.asList(
            "Verstrickter", "Verfluchter", "Verseuchter"
        ));
    }
    
    public TitleManager(CoreAPIPlugin coreAPI, MultiCharPlugin plugin) {
        this.coreAPI = coreAPI;
        this.plugin = plugin;
    }
    
    /**
     * Lädt alle Titel eines Charakters
     */
    public CompletableFuture<Map<String, Map<String, Boolean>>> loadCharacterTitles(int characterId) {
        CompletableFuture<Map<String, Map<String, Boolean>>> future = new CompletableFuture<>();
        
        String sql = "SELECT title_category, title_name, is_active FROM character_titles WHERE character_id = ?";
        
        coreAPI.queryAsync(sql, results -> {
            Map<String, Map<String, Boolean>> titles = new HashMap<>();
            
            // Initialisiere alle Kategorien mit allen verfügbaren Titeln (alle false)
            for (Map.Entry<String, List<String>> entry : TITLE_CATEGORIES.entrySet()) {
                String category = entry.getKey();
                Map<String, Boolean> categoryTitles = new HashMap<>();
                
                for (String titleName : entry.getValue()) {
                    categoryTitles.put(titleName, false);
                }
                titles.put(category, categoryTitles);
            }
            
            // Überschreibe mit tatsächlichen Daten aus der Datenbank
            for (Map<String, Object> row : results) {
                String category = (String) row.get("title_category");
                String titleName = (String) row.get("title_name");
                boolean isActive = (Boolean) row.get("is_active");
                
                if (titles.containsKey(category) && titles.get(category).containsKey(titleName)) {
                    titles.get(category).put(titleName, isActive);
                }
            }
            
            future.complete(titles);
        }, characterId);
        
        return future;
    }
    
    /**
     * Togglet einen Titel für einen Charakter (aktiviert/deaktiviert)
     */
    public void toggleTitle(int characterId, String category, String titleName, Player player) {
        // Überprüfe, ob der Titel existiert
        if (!TITLE_CATEGORIES.containsKey(category) || 
            !TITLE_CATEGORIES.get(category).contains(titleName)) {
            player.sendMessage("§cUngültiger Titel oder Kategorie!");
            return;
        }
        
        String checkSql = "SELECT is_active FROM character_titles WHERE character_id = ? AND title_name = ?";
        
        coreAPI.queryAsync(checkSql, results -> {
            if (results.isEmpty()) {
                // Titel existiert noch nicht -> erstellen und aktivieren
                grantTitle(characterId, category, titleName, player, true);
            } else {
                // Titel existiert -> Status umkehren
                boolean currentStatus = (Boolean) results.get(0).get("is_active");
                boolean newStatus = !currentStatus;
                
                String updateSql = "UPDATE character_titles SET is_active = ? WHERE character_id = ? AND title_name = ?";
                coreAPI.executeUpdateWithCallbackAsync(updateSql, success -> {
                    if (success) {
                        String statusText = newStatus ? "§aaktiviert" : "§7deaktiviert";
                        player.sendMessage("§eTitel §6" + titleName + " §e" + statusText + "!");
                    } else {
                        player.sendMessage("§cFehler beim Aktualisieren des Titel-Status!");
                    }
                }, newStatus, characterId, titleName);
            }
        }, characterId, titleName);
    }
    
    /**
     * Gewährt einem Charakter einen neuen Titel
     */
    private void grantTitle(int characterId, String category, String titleName, Player grantedBy, boolean isActive) {
        String sql = "INSERT INTO character_titles (character_id, title_category, title_name, is_active, granted_by) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE is_active = VALUES(is_active)";
        
        coreAPI.executeUpdateWithCallbackAsync(sql, success -> {
            if (success) {
                String statusText = isActive ? "§aaktiviert" : "§7deaktiviert";
                grantedBy.sendMessage("§eTitel §6" + titleName + " §ewurde gewährt und " + statusText + "!");
            } else {
                grantedBy.sendMessage("§cFehler beim Gewähren des Titels!");
            }
        }, characterId, category, titleName, isActive, grantedBy.getUniqueId().toString());
    }
    
    /**
     * Gewährt einem Charakter einen Titel (öffentliche Methode)
     */
    public void grantTitleToCharacter(int characterId, String category, String titleName, Player grantedBy) {
        grantTitle(characterId, category, titleName, grantedBy, false); // Standardmäßig inaktiv
    }
    
    /**
     * Entfernt einen Titel von einem Charakter
     */
    public void removeTitle(int characterId, String titleName, Player removedBy) {
        String sql = "DELETE FROM character_titles WHERE character_id = ? AND title_name = ?";
        
        coreAPI.executeUpdateWithCallbackAsync(sql, success -> {
            if (success) {
                removedBy.sendMessage("§eTitel §6" + titleName + " §ewurde entfernt!");
            } else {
                removedBy.sendMessage("§cFehler beim Entfernen des Titels!");
            }
        }, characterId, titleName);
    }
    
    /**
     * Gibt alle aktiven Titel eines Charakters zurück
     */
    public CompletableFuture<List<String>> getActiveTitles(int characterId) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        
        String sql = "SELECT title_name FROM character_titles WHERE character_id = ? AND is_active = TRUE";
        
        coreAPI.queryAsync(sql, results -> {
            List<String> activeTitles = new ArrayList<>();
            for (Map<String, Object> row : results) {
                activeTitles.add((String) row.get("title_name"));
            }
            future.complete(activeTitles);
        }, characterId);
        
        return future;
    }
    
    /**
     * Deaktiviert alle Titel einer bestimmten Kategorie für einen Charakter
     */
    public void deactivateCategoryTitles(int characterId, String category, Player player) {
        String sql = "UPDATE character_titles SET is_active = FALSE WHERE character_id = ? AND title_category = ?";
        
        coreAPI.executeUpdateWithCallbackAsync(sql, success -> {
            if (success) {
                player.sendMessage("§eAlle §6" + category + "§e-Titel wurden deaktiviert!");
            } else {
                player.sendMessage("§cFehler beim Deaktivieren der Titel!");
            }
        }, characterId, category);
    }
}