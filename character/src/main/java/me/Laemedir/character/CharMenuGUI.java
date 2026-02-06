package me.Laemedir.character;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Verwaltet die GUI für das Charakter-Menü.
 * Erstellt Inventare für Übersicht, Details und Suche.
 */
public class CharMenuGUI {
    
    private final MultiCharPlugin plugin;
    private final CoreAPIPlugin coreAPI;
    private final TitleManager titleManager;
    
    public CharMenuGUI(MultiCharPlugin plugin, CoreAPIPlugin coreAPI, TitleManager titleManager) {
        this.plugin = plugin;
        this.coreAPI = coreAPI;
        this.titleManager = titleManager;
    }
    
    /**
     * Öffnet das Hauptmenü für das Character-Management.
     */
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§1§lCharakter Verwaltung");
        
        // Alle Charakter anzeigen
        ItemStack allChars = createMenuItem(Material.PLAYER_HEAD, "§a§lAlle Charakter anzeigen", 
            "§7Zeigt alle erstellten Charaktere", "§7mit deren Informationen an.");
        inv.setItem(10, allChars);
        
        // Charakter suchen
        ItemStack searchChars = createMenuItem(Material.COMPASS, "§e§lCharakter suchen", 
            "§7Suche nach einem bestimmten", "§7Charakter oder Spieler.");
        inv.setItem(12, searchChars);
        
        // Deaktivierte Charakter
        ItemStack disabledChars = createMenuItem(Material.BARRIER, "§c§lDeaktivierte Charakter", 
            "§7Zeigt alle deaktivierten", "§7Charaktere an.");
        inv.setItem(14, disabledChars);
        
        // Gesperrte Charakter
        ItemStack blockedChars = createMenuItem(Material.IRON_BARS, "§4§lGesperrte Charakter", 
            "§7Zeigt alle gesperrten", "§7Charaktere an.");
        inv.setItem(16, blockedChars);
        
        // Fülle leere Slots mit Glasscheiben
        fillEmptySlots(inv);
        
        player.openInventory(inv);
    }
    
    /**
     * Öffnet das GUI mit allen Charakteren.
     * Lädt Daten asynchron.
     */
    public void openAllCharactersMenu(Player player) {
        // Hole alle Charaktere aus der Datenbank mit Rassen-Information async
        String sql = "SELECT c.name, c.player_uuid, r.race_name " +
                    "FROM characters c " +
                    "LEFT JOIN races r ON c.race_id = r.id " +
                    "ORDER BY c.name";
        
        coreAPI.queryAsync(sql, rs -> {
            List<CharacterInfo> characters = new ArrayList<>();
            // Map results from List<Map<String, Object>>
            for (Map<String, Object> row : rs) {
                String charName = (String) row.get("name");
                String playerUUID = (String) row.get("player_uuid");
                String raceName = (String) row.get("race_name");
                
                // Konvertiere UUID direkt zu Spielername
                String playerName = getPlayerNameFromUUID(playerUUID);
                
                characters.add(new CharacterInfo(charName, playerName, raceName != null ? raceName : "Keine Rasse"));
            }
            
            // Erstelle GUI basierend auf Anzahl der Charaktere
            int slots = Math.max(54, ((characters.size() + 8) / 9) * 9); // Mindestens 54 Slots, dann in 9er-Reihen
            slots = Math.min(slots, 54); // Maximum 54 Slots
            
            final int finalSlots = slots;
            
            // Zurück auf Main-Thread für Inventory-Erstellung
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(null, finalSlots, "§1§lAlle Charakter (" + characters.size() + ")");
                
                // Füge Charakterköpfe hinzu
                for (int i = 0; i < Math.min(characters.size(), finalSlots - 9); i++) {
                    CharacterInfo character = characters.get(i);
                    ItemStack skull = createCharacterSkull(character);
                    inv.setItem(i, skull);
                }
                
                // Zurück-Button
                ItemStack backButton = createMenuItem(Material.ARROW, "§c§lZurück", 
                    "§7Zurück zum Hauptmenü");
                inv.setItem(finalSlots - 5, backButton);
                
                player.openInventory(inv);
            });
        });
    }
    
    /**
     * Erstellt einen Charakterkopf mit Hover-Informationen.
     */
    private ItemStack createCharacterSkull(CharacterInfo character) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§b§l" + character.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Rasse: §a" + character.getRace());
            lore.add("§7Spieler: §e" + character.getPlayerName());
            lore.add("");
            lore.add("§7Rechtsklick für weitere Optionen");
            
            meta.setLore(lore);
            
            // Versuche den Spielerkopf zu setzen (nur wenn es kein Fallback-Name ist)
            try {
                if (character.getPlayerName() != null && !character.getPlayerName().startsWith("Spieler-") && !character.getPlayerName().equals("Unbekannter Spieler")) {
                    // Verwende nur Online-Spieler oder bereits bekannte Namen
                    Player onlinePlayer = Bukkit.getPlayerExact(character.getPlayerName());
                    if (onlinePlayer != null) {
                        meta.setOwningPlayer(onlinePlayer);
                    }
                    // Für Offline-Spieler verwenden wir keinen Kopf, um API-Aufrufe zu vermeiden
                }
            } catch (Exception e) {
                // Ignoriere Fehler beim Setzen des Spielerkopfs
            }
            
            skull.setItemMeta(meta);
        }
        
        return skull;
    }
    
    /**
     * Öffnet das detaillierte Character-Info GUI.
     */
    public void openCharacterDetailMenu(Player player, String characterName, String playerUUID) {
        // Hole alle Charakterdaten aus der Datenbank
        String sql = "SELECT c.*, r.race_name " +
                    "FROM characters c " +
                    "LEFT JOIN races r ON c.race_id = r.id " +
                    "WHERE c.name = ? AND c.player_uuid = ?";
        
        coreAPI.queryAsync(sql, rs -> {
            if (!rs.isEmpty()) {
                Map<String, Object> row = rs.get(0);
                // Zurück auf Main-Thread für das GUI
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        // Erstelle das GUI mit den Charakterdaten (row Map)
                        createCharacterDetailGUI(player, row, characterName);
                    } catch (Exception e) {
                        player.sendMessage("§cFehler beim Erstellen des GUIs: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                player.sendMessage("§cCharakter nicht gefunden!");
            }
        }, characterName, playerUUID);
    }
    
    /**
     * Erstellt das detaillierte Character-Info GUI.
     */
    private void createCharacterDetailGUI(Player player, Map<String, Object> rs, String characterName) {
        Inventory inv = Bukkit.createInventory(null, 54, "§1§l" + characterName + " - Details");
        
        // Name & Co (Slot 10)
        String deckname = (String) rs.get("deckname");
        String rufname = (String) rs.get("rufname");
        String verwandlung = (String) rs.get("verwandlung");
        String affinity = (String) rs.get("affinity");
        
        List<String> nameLore = new ArrayList<>();
        nameLore.add("§7Minecraft Name: §b" + getPlayerNameFromUUID((String) rs.get("player_uuid")));
        nameLore.add("§7Charakter Name: §a" + rs.get("name"));
        if (deckname != null && !deckname.isEmpty()) {
            nameLore.add("§7Deckname: §e" + deckname);
        }
        if (rufname != null && !rufname.isEmpty()) {
            nameLore.add("§7Rufname: §f" + rufname);
        }
        if (verwandlung != null && !verwandlung.isEmpty()) {
            nameLore.add("§7Verwandlung: §6" + verwandlung);
        }
        if (affinity != null && !affinity.isEmpty()) {
            nameLore.add("§7Affinität: §5" + affinity);
        }
        nameLore.add("§7Aktive Kategorie: §6" + (rs.get("active_name_category") != null ? rs.get("active_name_category") : "name"));
        nameLore.add("§7Rasse: §d" + (rs.get("race_name") != null ? rs.get("race_name") : "Keine Rasse"));
        
        ItemStack nameInfo = createMenuItem(Material.NAME_TAG, "§e§l📞 Name & Identität", nameLore.toArray(new String[0]));
        inv.setItem(11, nameInfo);
        
        // Position & Welt (Slot 15) - Mit besserer Formatierung
        String world = (String) rs.get("world");
        double x = rs.get("position_x") != null ? ((Number) rs.get("position_x")).doubleValue() : 0.0;
        double y = rs.get("position_y") != null ? ((Number) rs.get("position_y")).doubleValue() : 0.0;
        double z = rs.get("position_z") != null ? ((Number) rs.get("position_z")).doubleValue() : 0.0;
        double yaw = rs.get("yaw") != null ? ((Number) rs.get("yaw")).doubleValue() : 0.0;
        double pitch = rs.get("pitch") != null ? ((Number) rs.get("pitch")).doubleValue() : 0.0;

        ItemStack locationInfo = createMenuItem(Material.COMPASS, "§2§l🌍 Position & Welt",
            "§r",
            "§7🌏 Welt: §b" + (world != null ? world : "Unbekannt"),
            "§7🗺 Koordinaten:",
            "§f  ● X: §a" + String.format("%.2f", x),
            "§f  ● Y: §a" + String.format("%.2f", y),
            "§f  ● Z: §a" + String.format("%.2f", z),
            "§7🧭 Rotation:",
            "§f  ● Yaw: §a" + String.format("%.2f", yaw),
            "§f  ● Pitch: §a" + String.format("%.2f", pitch)
        );
        inv.setItem(15, locationInfo);
        
        // === ZWEITE REIHE (Gameplay & Texte) ===
        
        // Gameplay Info (Slot 20)
        int age = rs.get("age") != null ? ((Number) rs.get("age")).intValue() : 0;
        ItemStack gameplayInfo = createMenuItem(Material.DIAMOND_SWORD, "§3§l⚔ Gameplay Info",
            "§r",
            "§7🎂 Alter: §f" + (age > 0 ? age + " Jahre" : "Nicht gesetzt"),
            "§7⚧ Geschlecht: §f" + (rs.get("gender") != null ? rs.get("gender") : "Nicht gesetzt")
        );
        inv.setItem(20, gameplayInfo);
        
        // Charakter Profil (Slot 22)
        String charProfile = (String) rs.get("appearance");
        boolean hasProfile = charProfile != null && !charProfile.isEmpty();
        ItemStack profileInfo = createMenuItem(Material.BOOK, "§c§l📋 Charakter Profil",
            "§r",
            hasProfile ? "§a✓ Profil vorhanden" : "§7✗ Kein Profil vorhanden",
            "§r",
            hasProfile ? "§e» Linksklick für Details" : "§7Noch nicht ausgefüllt"
        );
        inv.setItem(22, profileInfo);
        
        // Stärken (Slot 24)
        String strengthsText = (String) rs.get("strengths");
        boolean hasStrengths = strengthsText != null && !strengthsText.isEmpty();
        ItemStack strengthsInfo = createMenuItem(Material.ENCHANTED_BOOK, "§a§l💪 Stärken",
            "§r",
            hasStrengths ? "§a✓ Stärken definiert" : "§7✗ Keine Stärken gesetzt",
            "§r",
            hasStrengths ? "§e» Linksklick für Details" : "§7Noch nicht ausgefüllt"
        );
        inv.setItem(24, strengthsInfo);
        
        // === DRITTE REIHE (Weitere Texte) ===
        
        // Schwächen (Slot 29)
        String weaknesses = (String) rs.get("weaknesses");
        boolean hasWeaknesses = weaknesses != null && !weaknesses.isEmpty();
        ItemStack weaknessInfo = createMenuItem(Material.WRITTEN_BOOK, "§9§l⚡ Schwächen",
            "§r",
            hasWeaknesses ? "§a✓ Schwächen definiert" : "§7✗ Keine Schwächen vorhanden",
            "§r",
            hasWeaknesses ? "§e» Linksklick für Details" : "§7Noch nicht ausgefüllt"
        );
        inv.setItem(29, weaknessInfo);
        
        // Hintergrundgeschichte (Slot 31)
        String backgroundStory = (String) rs.get("background_story");
        boolean hasBackgroundStory = backgroundStory != null && !backgroundStory.isEmpty();
        ItemStack backgroundInfo = createMenuItem(Material.WRITABLE_BOOK, "§6§l📚 Hintergrundgeschichte",
            "§r",
            hasBackgroundStory ? "§a✓ Geschichte vorhanden" : "§7✗ Keine Hintergrundgeschichte vorhanden",
            "§r",
            hasBackgroundStory ? "§e» Linksklick für Details" : "§7Noch nicht ausgefüllt"
        );
        inv.setItem(31, backgroundInfo);
        
        // Charaktereigenschaften (Slot 33)
        String characterTraitsText = (String) rs.get("character_traits");
        boolean hasCharacterTraits = characterTraitsText != null && !characterTraitsText.isEmpty();
        ItemStack characterTraitsInfo = createMenuItem(Material.ENCHANTED_BOOK, "§d§l🎭 Charaktereigenschaften",
            "§r",
            hasCharacterTraits ? "§a✓ Eigenschaften definiert" : "§7✗ Keine Eigenschaften gesetzt",
            "§r",
            hasCharacterTraits ? "§e» Linksklick für Details" : "§7Noch nicht ausgefüllt"
        );
        inv.setItem(33, characterTraitsInfo);
        
        // Technische Daten (Slot 40 - untere Reihe links)
        String createdAt = rs.get("created_at") != null ? rs.get("created_at").toString() : null;
        String lastLogin = rs.get("last_login") != null ? rs.get("last_login").toString() : null;
        String firstLogin = rs.get("first_login") != null ? rs.get("first_login").toString() : null;
        String gamemode = (String) rs.get("gamemode");
        int statusCode = rs.get("status") != null ? ((Number) rs.get("status")).intValue() : 0;
        String deactivationReason = (String) rs.get("deactivation_reason");
        
        String statusText;
        String statusColor;
        switch (statusCode) {
            case 1: statusText = "Aktiv"; statusColor = "§a"; break;
            case 2: statusText = "Gesperrt"; statusColor = "§c"; break;
            case 0: statusText = "Deaktiviert"; statusColor = "§7"; break;
            default: statusText = "Unbekannt (" + statusCode + ")"; statusColor = "§f"; break;
        }
        
        List<String> techLore = new ArrayList<>();
        techLore.add("§r");
        techLore.add("§7🎮 Gamemode: §6" + (gamemode != null ? gamemode : "SURVIVAL"));
        techLore.add("§7🟢 Status: " + statusColor + statusText);
        
        if ((statusCode == 0 || statusCode == 2) && deactivationReason != null && !deactivationReason.isEmpty()) {
            techLore.add("§7⚠ Grund: §f" + deactivationReason);
        }
        
        techLore.add("§r");
        techLore.add("§7📅 Zeiten:");
        techLore.add("§f  ● Erstellt: §a" + (createdAt != null ? createdAt : "Unbekannt"));
        techLore.add("§f  ● Erstes Login: §a" + (firstLogin != null ? firstLogin : "Unbekannt"));
        techLore.add("§f  ● Letztes Login: §a" + (lastLogin != null ? lastLogin : "Nie"));
        
        ItemStack techInfo = createMenuItem(Material.REDSTONE, "§4§l⚙ Technische Daten", techLore.toArray(new String[0]));
        inv.setItem(40, techInfo);
        
        // === EDIT BUTTONS (Rechte Seite) ===
        
        ItemStack editName = createMenuItem(Material.WRITABLE_BOOK, "§e§l✎ Bearbeiten: Namen",
            "§r", "§f● §7Deckname ändern", "§f● §7Rufname ändern", "§f● §7Verwandlung ändern", "§f● §7Affinität ändern", "§f● §7Aktive Kategorie ändern",
            "§r", "§e» Linksklick zum Bearbeiten");
        inv.setItem(13, editName);
        
        ItemStack editGameplay = createMenuItem(Material.DIAMOND_SWORD, "§3§l⚔ Bearbeiten: Gameplay",
            "§r", "§f● §7Alter ändern", "§f● §7Geschlecht ändern",
            "§r", "§3» Linksklick zum Bearbeiten");
        inv.setItem(16, editGameplay);
        
        ItemStack editTexts = createMenuItem(Material.ENCHANTED_BOOK, "§9§l📝 Bearbeiten: Texte",
            "§r", "§f● §7Charakter Profil bearbeiten", "§f● §7Stärken bearbeiten", "§f● §7Schwächen bearbeiten", "§f● §7Hintergrundgeschichte bearbeiten", "§f● §7Charaktereigenschaften bearbeiten",
            "§r", "§9» Linksklick zum Bearbeiten");
        inv.setItem(25, editTexts);
        
        ItemStack editStatus = createMenuItem(Material.COMMAND_BLOCK, "§c§l⚙ Verwaltung: Status",
            "§r", "§f● §7Status ändern (Aktiv/Deaktiviert/Gesperrt)", "§f● §7Deaktivierungsgrund setzen", "§f● §7Gamemode ändern",
            "§r", "§c⚠ Nur für Admins!", "§c» Linksklick zum Verwalten");
        inv.setItem(43, editStatus);
        
        ItemStack titlesButton = createMenuItem(Material.GOLDEN_HELMET, "§6§l👑 Titel verwalten",
            "§r", "§7Aktiviere oder deaktiviere Titel", "§7für diesen Charakter.",
            "§r", "§6» Linksklick zum Verwalten");
        inv.setItem(48, titlesButton);
        
        ItemStack backButton = createMenuItem(Material.ARROW, "§c§l⬅ Zurück", 
            "§r", "§7Zurück zur Charakterübersicht",
            "§r", "§c» Linksklick zum Zurückgehen");
        inv.setItem(49, backButton);
        
        // === GLASSCHEIBEN ===
        ItemStack borderGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderGlass.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName("§r");
            borderGlass.setItemMeta(borderMeta);
        }
        
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, borderGlass);
            if (inv.getItem(i + 45) == null) inv.setItem(i + 45, borderGlass);
        }
        for (int i = 9; i < 45; i += 9) {
            if (inv.getItem(i) == null) inv.setItem(i, borderGlass);
            if (inv.getItem(i + 8) == null) inv.setItem(i + 8, borderGlass);
        }
        
        ItemStack accentGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta accentMeta = accentGlass.getItemMeta();
        if (accentMeta != null) {
            accentMeta.setDisplayName("§r");
            accentGlass.setItemMeta(accentMeta);
        }
        
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null && i != 49) {
                inv.setItem(i, accentGlass);
            }
        }
        
        player.openInventory(inv);
    }

    /**
     * Konvertiert eine UUID direkt zu einem Spielernamen (ohne blockierende API-Aufrufe falls möglich).
     */
    private String getPlayerNameFromUUID(String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null) {
                return onlinePlayer.getName();
            }
            
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
                return offlinePlayer.getName();
            }
            
            return "Spieler-" + uuidString.substring(0, 8);
            
        } catch (Exception e) {
            if (plugin.getDebugManager() != null) {
                plugin.getDebugManager().error("character", "Char Menu", "Fehler beim Konvertieren der UUID " + uuidString, e);
            }
            return "Unbekannter Spieler";
        }
    }
    
    /**
     * Teilt langen Text in mehrere Zeilen auf für bessere Lesbarkeit
     */
    private String[] splitTextIntoLines(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return new String[]{"Leer"};
        }
        
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder("§7");
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxLength) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder("§7" + word);
            } else {
                if (currentLine.length() > 2) { 
                    currentLine.append(" ");
                }
                currentLine.append(word);
            }
        }
        
        if (currentLine.length() > 2) {
            lines.add(currentLine.toString());
        }
        
        return lines.toArray(new String[0]);
    }
    
    /**
     * Öffnet das GUI mit deaktivierten Charakteren (Status = 0)
     */
    public void openDeactivatedCharactersMenu(Player player) {
        openFilteredCharactersMenu(player, 0, "Deaktivierte Charaktere", "§7§l");
    }
    
    /**
     * Öffnet das GUI mit gesperrten Charakteren (Status = 2)
     */
    public void openBlockedCharactersMenu(Player player) {
        openFilteredCharactersMenu(player, 2, "Gesperrte Charaktere", "§6§l");
    }
    
    /**
     * Öffnet ein gefiltertes Charakter-GUI basierend auf Status
     */
    private void openFilteredCharactersMenu(Player player, int statusFilter, String menuTitle, String titleColor) {
        String sql = "SELECT c.name, c.player_uuid, r.race_name, c.deactivation_reason " +
                    "FROM characters c " +
                    "LEFT JOIN races r ON c.race_id = r.id " +
                    "WHERE c.status = ? " +
                    "ORDER BY c.name";
        
        coreAPI.queryAsync(sql, rs -> {
            List<FilteredCharacterInfo> characters = new ArrayList<>();
            for (Map<String, Object> row : rs) {
                String charName = (String) row.get("name");
                String playerUUID = (String) row.get("player_uuid");
                String raceName = (String) row.get("race_name");
                String reason = (String) row.get("deactivation_reason");
                
                String playerName = getPlayerNameFromUUID(playerUUID);
                
                characters.add(new FilteredCharacterInfo(charName, playerName, 
                    raceName != null ? raceName : "Keine Rasse", reason));
            }
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                int slots = Math.max(54, ((characters.size() + 8) / 9) * 9);
                slots = Math.min(slots, 54);
                
                Inventory inv = Bukkit.createInventory(null, slots, "§1" + titleColor + menuTitle + " (" + characters.size() + ")");
                
                for (int i = 0; i < Math.min(characters.size(), slots - 9); i++) {
                    FilteredCharacterInfo character = characters.get(i);
                    ItemStack skull = createFilteredCharacterSkull(character, statusFilter);
                    inv.setItem(i, skull);
                }
                
                ItemStack backButton = createMenuItem(Material.ARROW, "§c§lZurück", 
                    "Zurück zum Hauptmenü");
                inv.setItem(slots - 5, backButton);
                
                player.openInventory(inv);
            });
        }, statusFilter);
    }
    
    /**
     * Öffnet das Suchergebnis-GUI
     */
    public void openSearchResultsMenu(Player player, String searchTerm) {
        String sql = "SELECT c.name, c.player_uuid, r.race_name, c.status " +
                    "FROM characters c " +
                    "LEFT JOIN races r ON c.race_id = r.id " +
                    "WHERE c.name LIKE ? " +
                    "ORDER BY c.name";
        
        String searchPattern = "%" + searchTerm + "%";
        
        coreAPI.queryAsync(sql, rs -> {
            List<SearchResultCharacterInfo> characters = new ArrayList<>();
            for (Map<String, Object> row : rs) {
                String charName = (String) row.get("name");
                String playerUUID = (String) row.get("player_uuid");
                String raceName = (String) row.get("race_name");
                int status = row.get("status") != null ? ((Number) row.get("status")).intValue() : 0;
                
                String playerName = getPlayerNameFromUUID(playerUUID);
                
                characters.add(new SearchResultCharacterInfo(charName, playerName, 
                    raceName != null ? raceName : "Keine Rasse", status));
            }
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                int slots = Math.max(54, ((characters.size() + 8) / 9) * 9);
                slots = Math.min(slots, 54);
                
                String title = characters.isEmpty() ? 
                    "§1§lSuche: Keine Ergebnisse" : 
                    "§1§lSuche: " + searchTerm + " (" + characters.size() + ")";
                
                Inventory inv = Bukkit.createInventory(null, slots, title);
                
                if (characters.isEmpty()) {
                    ItemStack noResults = createMenuItem(Material.BARRIER, "§c§lKeine Ergebnisse",
                        "§7Suchbegriff: §e" + searchTerm,
                        "§7Keine Charaktere gefunden."
                    );
                    inv.setItem(22, noResults);
                } else {
                    for (int i = 0; i < Math.min(characters.size(), slots - 9); i++) {
                        SearchResultCharacterInfo character = characters.get(i);
                        ItemStack skull = createSearchResultSkull(character);
                        inv.setItem(i, skull);
                    }
                }
                
                ItemStack backButton = createMenuItem(Material.ARROW, "§c§lZurück", 
                    "Zurück zum Hauptmenü");
                inv.setItem(slots - 5, backButton);
                
                player.openInventory(inv);
            });
        }, searchPattern);
    }
    
    /**
     * Erstellt einen gefilterten Charakterkopf mit Status-spezifischen Informationen
     */
    private ItemStack createFilteredCharacterSkull(FilteredCharacterInfo character, int statusFilter) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        
        if (meta != null) {
            String nameColor = statusFilter == 2 ? "§6§l" : "§7§l";
            meta.setDisplayName(nameColor + character.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Rasse: §a" + character.getRace());
            lore.add("§7Spieler: §e" + character.getPlayerName());
            lore.add("§7Status: " + (statusFilter == 2 ? "§6Gesperrt" : "§7Deaktiviert"));
            
            if (character.getReason() != null && !character.getReason().isEmpty()) {
                lore.add("§7Grund: §f" + character.getReason());
            }
            
            lore.add("");
            lore.add("§7Rechtsklick für weitere Optionen");
            
            meta.setLore(lore);
            
            try {
                if (character.getPlayerName() != null && !character.getPlayerName().startsWith("Spieler-") && !character.getPlayerName().equals("Unbekannter Spieler")) {
                    Player onlinePlayer = Bukkit.getPlayerExact(character.getPlayerName());
                    if (onlinePlayer != null) {
                        meta.setOwningPlayer(onlinePlayer);
                    }
                }
            } catch (Exception e) {}
            
            skull.setItemMeta(meta);
        }
        
        return skull;
    }
    
    /**
     * Erstellt einen Suchresultat-Charakterkopf
     */
    private ItemStack createSearchResultSkull(SearchResultCharacterInfo character) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        
        if (meta != null) {
            String nameColor;
            String statusText;
            switch (character.getStatus()) {
                case 1: nameColor = "§a§l"; statusText = "§aAktiv"; break;
                case 2: nameColor = "§6§l"; statusText = "§6Gesperrt"; break;
                case 0: nameColor = "§7§l"; statusText = "§7Deaktiviert"; break;
                default: nameColor = "§f§l"; statusText = "§fUnbekannt"; break;
            }
            
            meta.setDisplayName(nameColor + character.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Rasse: §a" + character.getRace());
            lore.add("§7Spieler: §e" + character.getPlayerName());
            lore.add("§7Status: " + statusText);
            lore.add("");
            lore.add("§7Rechtsklick für weitere Optionen");
            
            meta.setLore(lore);
            
            try {
                if (character.getPlayerName() != null && !character.getPlayerName().startsWith("Spieler-") && !character.getPlayerName().equals("Unbekannter Spieler")) {
                    Player onlinePlayer = Bukkit.getPlayerExact(character.getPlayerName());
                    if (onlinePlayer != null) {
                        meta.setOwningPlayer(onlinePlayer);
                    }
                }
            } catch (Exception e) {}
            
            skull.setItemMeta(meta);
        }
        
        return skull;
    }
    
    /**
     * Erstellt ein Menü-Item
     */
    private ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Füllt leere Slots mit Glasscheiben
     */
    private void fillEmptySlots(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName("§r");
            glass.setItemMeta(glassMeta);
        }
        
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
    }

    /**
     * Hilfsklasse für Charakterinformationen
     */
    public static class CharacterInfo {
        private final String name;
        private final String playerName;
        private final String race;
        
        public CharacterInfo(String name, String playerName, String race) {
            this.name = name;
            this.playerName = playerName;
            this.race = race;
        }
        
        public String getName() { return name; }
        public String getPlayerName() { return playerName; }
        public String getRace() { return race; }
    }
    
    /**
     * Hilfsklasse für gefilterte Charakterinformationen (Deaktiviert/Gesperrt)
     */
    public static class FilteredCharacterInfo {
        private final String name;
        private final String playerName;
        private final String race;
        private final String reason;
        
        public FilteredCharacterInfo(String name, String playerName, String race, String reason) {
            this.name = name;
            this.playerName = playerName;
            this.race = race;
            this.reason = reason;
        }
        
        public String getName() { return name; }
        public String getPlayerName() { return playerName; }
        public String getRace() { return race; }
        public String getReason() { return reason; }
    }
    
    /**
     * Hilfsklasse für Suchresultat-Charakterinformationen
     */
    public static class SearchResultCharacterInfo {
        private final String name;
        private final String playerName;
        private final String race;
        private final int status;
        
        public SearchResultCharacterInfo(String name, String playerName, String race, int status) {
            this.name = name;
            this.playerName = playerName;
            this.race = race;
            this.status = status;
        }
        
        public String getName() { return name; }
        public String getPlayerName() { return playerName; }
        public String getRace() { return race; }
        public int getStatus() { return status; }
    }
    
    /**
     * Öffnet das Titel-Management-Menü für einen Charakter
     */
    public void openTitleManagementMenu(Player player, String characterName, String playerUUID) {
        // Hole Charakter-ID aus der Datenbank
        String sql = "SELECT id FROM characters WHERE name = ? AND player_uuid = ?";
        
        coreAPI.queryAsync(sql, rs -> {
            if (rs.isEmpty()) {
                player.sendMessage("§cCharakter nicht gefunden!");
                return;
            }
            
            int characterId = (Integer) rs.get(0).get("id");
            
            // Lade Titel asynchron und öffne dann das GUI
            titleManager.loadCharacterTitles(characterId).thenAccept(titles -> {
                // Wechsel zurück zum Haupt-Thread für GUI-Operationen
                Bukkit.getScheduler().runTask(plugin, () -> {
                    createTitleManagementGUI(player, characterName, playerUUID, characterId, titles);
                });
            }).exceptionally(ex -> {
                if (plugin.getDebugManager() != null) {
                    plugin.getDebugManager().error("character", "Title Management", "Fehler beim Laden der Titel", (Exception) ex);
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§cFehler beim Laden der Titel!");
                });
                return null;
            });
        }, characterName, playerUUID);
    }
    
    /**
     * Erstellt das Titel-Management-GUI
     */
    private void createTitleManagementGUI(Player player, String characterName, String playerUUID, 
                                        int characterId, Map<String, Map<String, Boolean>> titles) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lTitel: " + characterName);
        
        // Kategorie-Buttons erstellen
        int slot = 10;
        for (Map.Entry<String, List<String>> categoryEntry : TitleManager.TITLE_CATEGORIES.entrySet()) {
            String category = categoryEntry.getKey();
            List<String> categoryTitles = categoryEntry.getValue();
            
            // Zähle aktive Titel in dieser Kategorie
            int activeTitles = 0;
            if (titles.containsKey(category)) {
                for (Boolean isActive : titles.get(category).values()) {
                    if (isActive) activeTitles++;
                }
            }
            
            // Erstelle Kategorie-Button
            Material categoryMaterial = getCategoryMaterial(category);
            String categoryColor = getCategoryColor(category);
            
            List<String> lore = new ArrayList<>();
            lore.add("§r");
            lore.add("§7Aktive Titel: §e" + activeTitles + "§7/§e" + categoryTitles.size());
            lore.add("§r");
            lore.add("§7Verfügbare Titel:");
            
            // Zeige erste 3 Titel als Vorschau
            for (int i = 0; i < Math.min(3, categoryTitles.size()); i++) {
                String title = categoryTitles.get(i);
                boolean isActive = titles.containsKey(category) && 
                                 titles.get(category).getOrDefault(title, false);
                String status = isActive ? "§a✓" : "§7✗";
                lore.add("§f  " + status + " §7" + title);
            }
            
            if (categoryTitles.size() > 3) {
                lore.add("§7  ... und " + (categoryTitles.size() - 3) + " weitere");
            }
            
            lore.add("§r");
            lore.add(categoryColor + "» Linksklick zum Verwalten");
            
            ItemStack categoryItem = createMenuItem(categoryMaterial, 
                categoryColor + "§l" + category + "-Titel", 
                lore.toArray(new String[0]));
            inv.setItem(slot, categoryItem);
            
            slot += 2; // 2 Slots Abstand zwischen Kategorien
        }
        
        // Zurück-Button
        ItemStack backButton = createMenuItem(Material.ARROW, "§c§l⬅ Zurück",
            "§r", "§7Zurück zur Charakterdetails",
            "§r", "§c» Linksklick zum Zurückgehen");
        inv.setItem(49, backButton);
        
        fillEmptySlots(inv);
        player.openInventory(inv);
    }
    
    /**
     * Öffnet das Detail-Menü für eine bestimmte Titel-Kategorie
     */
    public void openTitleCategoryMenu(Player player, String characterName, String playerUUID, 
                                    int characterId, String category) {
        titleManager.loadCharacterTitles(characterId).thenAccept(titles -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                createTitleCategoryGUI(player, characterName, playerUUID, characterId, category, titles);
            });
        }).exceptionally(ex -> {
            if (plugin.getDebugManager() != null) {
                plugin.getDebugManager().error("character", "Title Management", "Fehler beim Laden der Titel", (Exception) ex);
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cFehler beim Laden der Titel!");
            });
            return null;
        });
    }
    
    /**
     * Erstellt das Kategorie-Detail-GUI für Titel
     */
    private void createTitleCategoryGUI(Player player, String characterName, String playerUUID, 
                                      int characterId, String category, Map<String, Map<String, Boolean>> titles) {
        List<String> categoryTitles = TitleManager.TITLE_CATEGORIES.get(category);
        if (categoryTitles == null) {
            player.sendMessage("§cUngültige Titel-Kategorie!");
            return;
        }
        
        int slots = Math.max(54, ((categoryTitles.size() + 8) / 9) * 9);
        slots = Math.min(slots, 54);
        
        String categoryColor = getCategoryColor(category);
        Inventory inv = Bukkit.createInventory(null, slots, categoryColor + "§l" + category + ": " + characterName);
        
        Map<String, Boolean> categoryTitleStates = titles.getOrDefault(category, new HashMap<>());
        
        for (int i = 0; i < categoryTitles.size() && i < slots - 9; i++) {
            String titleName = categoryTitles.get(i);
            boolean isActive = categoryTitleStates.getOrDefault(titleName, false);
            
            Material titleMaterial = isActive ? Material.LIME_DYE : Material.GRAY_DYE;
            String titleColor = isActive ? "§a" : "§7";
            String statusText = isActive ? "§aAktiviert" : "§7Deaktiviert";
            
            ItemStack titleItem = createMenuItem(titleMaterial, titleColor + "§l" + titleName,
                "§r", "§7Status: " + statusText,
                "§r", isActive ? "§c» Linksklick zum Deaktivieren" : "§a» Linksklick zum Aktivieren"
            );
            
            inv.setItem(i, titleItem);
        }
        
        ItemStack backButton = createMenuItem(Material.ARROW, "§c§l⬅ Zurück",
            "§r", "§7Zurück zur Titelübersicht",
            "§r", "§c» Linksklick zum Zurückgehen");
        inv.setItem(slots - 5, backButton);
        
        fillEmptySlots(inv);
        player.openInventory(inv);
    }
    
    /**
     * Bestimmt das Material für eine Titel-Kategorie
     */
    private Material getCategoryMaterial(String category) {
        switch (category) {
            case "Fadenmagie": return Material.ENCHANTED_BOOK;
            case "Drachen": return Material.DRAGON_HEAD;
            case "Adel": return Material.GOLDEN_HELMET;
            case "Fluch": return Material.WITHER_SKELETON_SKULL;
            default: return Material.BOOK;
        }
    }
    
    /**
     * Bestimmt die Farbe für eine Titel-Kategorie
     */
    private String getCategoryColor(String category) {
        switch (category) {
            case "Fadenmagie": return "§5";
            case "Drachen": return "§c";
            case "Adel": return "§6";
            case "Fluch": return "§8";
            default: return "§f";
        }
    }
}
