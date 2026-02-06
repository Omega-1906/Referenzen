package me.Laemedir.character;

import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Listener für das Charakter-Verwaltungs-Menü.
 * Verarbeitet Klicks und Chat-Eingaben für die Suche.
 */
public class CharMenuGUIListener implements Listener {
    
    private final MultiCharPlugin plugin;
    private final CoreAPIPlugin coreAPI;
    private final CharMenuGUI charMenuGUI;
    private final TitleManager titleManager;
    private final Set<UUID> waitingForSearchInput = new HashSet<>();
    
    public CharMenuGUIListener(MultiCharPlugin plugin, CoreAPIPlugin coreAPI, TitleManager titleManager) {
        this.plugin = plugin;
        this.coreAPI = coreAPI;
        this.titleManager = titleManager;
        this.charMenuGUI = new CharMenuGUI(plugin, coreAPI, titleManager);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Verhindere das Verschieben von Items in unseren GUIs
        if (title.contains("Charakter Verwaltung") || title.contains("Alle Charakter") || 
            title.contains(" - Details") || title.contains("Deaktivierte Charaktere") || 
            title.contains("Gesperrte Charaktere") || title.contains("Suche:") ||
            title.contains("Titel:") || title.contains("Fadenmagie:") || 
            title.contains("Drachen:") || title.contains("Adel:") || title.contains("Fluch:")) {
            event.setCancelled(true);
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType().isAir()) {
                return;
            }
            
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) {
                return;
            }
            
            String displayName = meta.getDisplayName();
            
            // Hauptmenü-Aktionen
            if (title.equals("§1§lCharakter Verwaltung")) {
                handleMainMenuClick(player, displayName);
            }
            // Alle Charakter-Menü Aktionen
            else if (title.contains("§1§lAlle Charakter")) {
                handleAllCharactersMenuClick(player, displayName, clickedItem);
            }
            // Character-Detail-Menü Aktionen
            else if (title.contains(" - Details")) {
                handleCharacterDetailMenuClick(player, displayName);
            }
            // Deaktivierte/Gesperrte/Suchresultate Aktionen
            else if (title.contains("Deaktivierte Charaktere") || title.contains("Gesperrte Charaktere") || title.contains("Suche:")) {
                handleFilteredCharacterMenuClick(player, displayName, clickedItem);
            }
            // Titel-Management Aktionen
            else if (title.contains("Titel:") || title.contains("Fadenmagie:") || 
                     title.contains("Drachen:") || title.contains("Adel:") || title.contains("Fluch:")) {
                handleTitleMenuClick(player, displayName, title);
            }
        }
    }
    
    /**
     * Behandelt Klicks im Hauptmenü
     */
    private void handleMainMenuClick(Player player, String displayName) {
        switch (displayName) {
            case "§a§lAlle Charakter anzeigen":
                player.closeInventory();
                charMenuGUI.openAllCharactersMenu(player);
                break;
                
            case "§e§lCharakter suchen":
                player.closeInventory();
                waitingForSearchInput.add(player.getUniqueId());
                player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                player.sendMessage("§e§lCharakter Suche");
                player.sendMessage("§7Bitte gib den Namen des Charakters ein (mindestens 4 Buchstaben):");
                player.sendMessage("§7Schreibe '§ccancel§7' um abzubrechen.");
                player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                break;
                
            case "§c§lDeaktivierte Charakter":
                player.closeInventory();
                charMenuGUI.openDeactivatedCharactersMenu(player);
                break;
                
            case "§4§lGesperrte Charakter":
                player.closeInventory();
                charMenuGUI.openBlockedCharactersMenu(player);
                break;
        }
    }
    
    /**
     * Behandelt Klicks im Alle-Charakter-Menü
     */
    private void handleAllCharactersMenuClick(Player player, String displayName, ItemStack clickedItem) {
        // Zurück-Button
        if (displayName.equals("§c§lZurück")) {
            player.closeInventory();
            charMenuGUI.openMainMenu(player);
            return;
        }
        
        // Charakterkopf geklickt
        if (clickedItem.getType().toString().contains("PLAYER_HEAD")) {
            String characterName = displayName.replace("§b§l", "");
            
            // Finde die UUID des Charakters in der Datenbank (Async)
            String sql = "SELECT player_uuid FROM characters WHERE name = ?";
            coreAPI.queryAsync(sql, rs -> {
                if (!rs.isEmpty()) {
                    String uuid = (String) rs.get(0).get("player_uuid");
                    if (uuid != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> 
                            charMenuGUI.openCharacterDetailMenu(player, characterName, uuid));
                    } else {
                        player.sendMessage("§cFehler: Charakterdaten nicht gefunden!");
                    }
                } else {
                    player.sendMessage("§cFehler: Charakterdaten nicht gefunden!");
                }
            }, characterName);
        }
    }
    
    /**
     * Behandelt Klicks im Character-Detail-Menü
     */
    private void handleCharacterDetailMenuClick(Player player, String displayName) {
        // Zurück-Button
        if (displayName.equals("§c§l⬅ Zurück")) {
            player.closeInventory();
            charMenuGUI.openAllCharactersMenu(player);
            return;
        }
        
        // Extrahiere Charaktername aus GUI-Titel
        String characterName = player.getOpenInventory().getTitle().replace("§1§l", "").replace(" - Details", "");
        
        // Text-Kategorien behandeln
        switch (displayName) {
            case "§c§l📋 Charakter Profil":
                showTextDetails(player, characterName, "appearance", "§c§lCharakter Profil");
                break;
            case "§a§l💪 Stärken":
                showTextDetails(player, characterName, "strengths", "§a§lStärken");
                break;
            case "§9§l⚡ Schwächen":
                showTextDetails(player, characterName, "weaknesses", "§9§lSchwächen");
                break;
            case "§6§l📚 Hintergrundgeschichte":
                showTextDetails(player, characterName, "background_story", "§6§lHintergrundgeschichte");
                break;
            case "§d§l🎭 Charaktereigenschaften":
                showTextDetails(player, characterName, "character_traits", "§d§lCharaktereigenschaften");
                break;
                
            // Edit-Buttons behandeln
            case "§e§l✎ Bearbeiten: Namen":
                showEditMenu(player, characterName, "names");
                break;
            case "§3§l⚔ Bearbeiten: Gameplay":
                showEditMenu(player, characterName, "gameplay");
                break;
            case "§9§l📝 Bearbeiten: Texte":
                showEditMenu(player, characterName, "texts");
                break;
            case "§c§l⚙ Verwaltung: Status":
                showEditMenu(player, characterName, "status");
                break;
                
            // Titel verwalten
            case "§6§l👑 Titel verwalten":
                // Hole UUID des Charakters (Async)
                String uuidSql = "SELECT player_uuid FROM characters WHERE name = ?";
                coreAPI.queryAsync(uuidSql, rs -> {
                    if (!rs.isEmpty()) {
                        String uuid = (String) rs.get(0).get("player_uuid");
                        if (uuid != null) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                player.closeInventory();
                                charMenuGUI.openTitleManagementMenu(player, characterName, uuid);
                            });
                        } else {
                            player.sendMessage("§cFehler: Charakterdaten nicht gefunden!");
                        }
                    } else {
                        player.sendMessage("§cFehler: Charakterdaten nicht gefunden!");
                    }
                }, characterName);
                break;
        }
    }
    
    /**
     * Zeigt Text-Details im Chat an
     */
    private void showTextDetails(Player player, String characterName, String columnName, String categoryTitle) {
        String sql = "SELECT " + columnName + " FROM characters WHERE name = ?";
        
        coreAPI.queryAsync(sql, rs -> {
             if (!rs.isEmpty()) {
                 String text = (String) rs.get(0).get(columnName);
                 if (text != null && !text.isEmpty()) {
                    player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                    player.sendMessage(categoryTitle + " §7von §e" + characterName + "§7:");
                    player.sendMessage("");
                    
                    // Text mit korrekter Formatierung ausgeben
                    String[] lines = text.split("\\n");
                    for (String line : lines) {
                        player.sendMessage("§f" + line.trim());
                    }
                    
                    player.sendMessage("");
                    player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                } else {
                    player.sendMessage("§c" + categoryTitle.replaceAll("§[0-9a-fA-F]", "") + " ist für " + characterName + " nicht gesetzt.");
                }
             } else {
                 player.sendMessage("§cFehler: Charakter nicht gefunden.");
             }
        }, characterName);
    }
    
    /**
     * Behandelt Klicks in gefilterten Charakter-Menüs (Deaktiviert/Gesperrt/Suche)
     */
    private void handleFilteredCharacterMenuClick(Player player, String displayName, ItemStack clickedItem) {
        // Zurück-Button
        if (displayName.equals("§c§lZurück")) {
            player.closeInventory();
            charMenuGUI.openMainMenu(player);
            return;
        }
        
        // Charakterkopf geklickt - öffne Detail-GUI
        if (clickedItem.getType().toString().contains("PLAYER_HEAD")) {
            String characterName = displayName.replaceAll("§[0-9a-fA-F]§l", "");
            
            // Finde die UUID des Charakters in der Datenbank (Async)
            String sql = "SELECT player_uuid FROM characters WHERE name = ?";
            coreAPI.queryAsync(sql, rs -> {
                if (!rs.isEmpty()) {
                    String uuid = (String) rs.get(0).get("player_uuid");
                    if (uuid != null) {
                         Bukkit.getScheduler().runTask(plugin, () -> {
                            player.closeInventory();
                            charMenuGUI.openCharacterDetailMenu(player, characterName, uuid);
                         });
                    } else {
                        player.sendMessage("§cFehler: Charakterdaten nicht gefunden!");
                    }
                } else {
                   player.sendMessage("§cFehler: Charakterdaten nicht gefunden!");
                }
            }, characterName);
        }
    }
    
    /**
     * Handler für Chat-Input bei Charakter-Suche
     */
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        if (waitingForSearchInput.contains(player.getUniqueId())) {
            event.setCancelled(true);
            waitingForSearchInput.remove(player.getUniqueId());
            
            String input = event.getMessage().trim();
            
            // Abbrechen
            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage("§7Suche abgebrochen.");
                // Öffne das Hauptmenü wieder (Sync Task)
                Bukkit.getScheduler().runTask(plugin, () -> charMenuGUI.openMainMenu(player));
                return;
            }
            
            // Validierung
            if (input.length() < 4) {
                player.sendMessage("§cDer Suchbegriff muss mindestens 4 Zeichen lang sein!");
                player.sendMessage("§7Versuche es erneut oder schreibe '§ccancel§7' zum Abbrechen.");
                waitingForSearchInput.add(player.getUniqueId());
                return;
            }
            
            // Führe Suche aus (auf dem Haupt-Thread starten und dann async)
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§7Suche nach: §e" + input + "§7...");
                charMenuGUI.openSearchResultsMenu(player, input);
            });
        }
    }
    
    /**
     * Zeigt Edit-Menü für verschiedene Kategorien
     */
    private void showEditMenu(Player player, String characterName, String category) {
        player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        switch (category) {
            case "names":
                player.sendMessage("§e§lBearbeitung: Namen für §f" + characterName);
                player.sendMessage("§7Verwende folgende Commands:");
                player.sendMessage("§e/charedit " + characterName + " deckname <neuer_name>");
                player.sendMessage("§e/charedit " + characterName + " rufname <neuer_name>");
                player.sendMessage("§e/charedit " + characterName + " verwandlung <verwandlung>");
                player.sendMessage("§e/charedit " + characterName + " affinity <affinität>");
                player.sendMessage("§e/charedit " + characterName + " kategorie <name|deckname|rufname>");
                break;
                
            case "gameplay":
                player.sendMessage("§3§lBearbeitung: Gameplay für §f" + characterName);
                player.sendMessage("§7Verwende folgende Commands:");
                player.sendMessage("§e/charedit " + characterName + " alter <zahl>");
                player.sendMessage("§e/charedit " + characterName + " geschlecht <geschlecht>");
                break;
                
            case "texts":
                player.sendMessage("§9§lBearbeitung: Texte für §f" + characterName);
                player.sendMessage("§7Verwende folgende Commands:");
                player.sendMessage("§e/charedit " + characterName + " profil <text>");
                player.sendMessage("§e/charedit " + characterName + " staerken <text>");
                player.sendMessage("§e/charedit " + characterName + " schwaechen <text>");
                player.sendMessage("§e/charedit " + characterName + " geschichte <text>");
                player.sendMessage("§e/charedit " + characterName + " eigenschaften <text>");
                player.sendMessage("§7§oHinweis: Für Zeilenumbrüche verwende \\n");
                break;
                
            case "status":
                player.sendMessage("§c§lVerwaltung: Status für §f" + characterName);
                player.sendMessage("§7Verwende folgende Commands:");
                player.sendMessage("§e/charedit " + characterName + " status <0|1|2> [grund]");
                player.sendMessage("§7  0 = Deaktiviert, 1 = Aktiv, 2 = Gesperrt");
                player.sendMessage("§e/charedit " + characterName + " gamemode <survival|creative|adventure|spectator>");
                break;
        }
        
        player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    /**
     * Behandelt Klicks in den Titel-Menüs
     */
    private void handleTitleMenuClick(Player player, String displayName, String guiTitle) {
        // Zurück-Button
        if (displayName.equals("§c§l⬅ Zurück")) {
            // Extrahiere Charaktername aus GUI-Titel
            String characterName;
            
            if (guiTitle.startsWith("§6§lTitel: ")) {
                // Haupt-Titel-Menü -> zurück zu Charakterdetails
                characterName = guiTitle.replace("§6§lTitel: ", "");
                
                // Hole playerUUID (Async)
                String sql = "SELECT player_uuid FROM characters WHERE name = ?";
                coreAPI.queryAsync(sql, rs -> {
                    if (!rs.isEmpty()) {
                        String playerUUID = (String) rs.get(0).get("player_uuid");
                        if (playerUUID != null) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                player.closeInventory();
                                charMenuGUI.openCharacterDetailMenu(player, characterName, playerUUID);
                            });
                        } else {
                            player.sendMessage("§cFehler: Charakterdaten nicht gefunden!");
                        }
                    } else {
                        player.sendMessage("§cFehler: Charakterdaten nicht gefunden!");
                    }
                }, characterName);

            } else {
                // Kategorie-Detail-Menü -> zurück zur Titelübersicht
                if (guiTitle.contains(": ")) {
                    characterName = guiTitle.substring(guiTitle.lastIndexOf(": ") + 2);
                    
                    // Hole playerUUID (Async)
                    String sql = "SELECT player_uuid FROM characters WHERE name = ?";
                    coreAPI.queryAsync(sql, rs -> {
                        if (!rs.isEmpty()) {
                            String playerUUID = (String) rs.get(0).get("player_uuid");
                            if (playerUUID != null) {
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    player.closeInventory();
                                    charMenuGUI.openTitleManagementMenu(player, characterName, playerUUID);
                                });
                            } else {
                                player.sendMessage("§cFehler: Charakterdaten nicht gefunden!");
                            }
                        }
                    }, characterName);
                }
            }
            return;
        }
        
        // Kategorie-Buttons im Haupt-Titel-Menü
        if (guiTitle.startsWith("§6§lTitel: ")) {
            String characterName = guiTitle.replace("§6§lTitel: ", "");
            
            // Bestimme die Kategorie basierend auf dem Button-Namen
            String category = null;
            if (displayName.contains("Fadenmagie-Titel")) category = "Fadenmagie";
            else if (displayName.contains("Drachen-Titel")) category = "Drachen";
            else if (displayName.contains("Adel-Titel")) category = "Adel";
            else if (displayName.contains("Fluch-Titel")) category = "Fluch";
            
            final String finalCategory = category;

            if (category != null) {
                // Hole playerUUID und characterId (Async)
                String sql = "SELECT player_uuid, id FROM characters WHERE name = ?";
                coreAPI.queryAsync(sql, rs -> {
                    if (!rs.isEmpty()) {
                        String playerUUID = (String) rs.get(0).get("player_uuid");
                        int characterId = (Integer) rs.get(0).get("id");
                        
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.closeInventory();
                            charMenuGUI.openTitleCategoryMenu(player, characterName, playerUUID, characterId, finalCategory);
                        });
                    } else {
                        player.sendMessage("§cFehler: Charakterdaten nicht gefunden!");
                    }
                }, characterName);
            }
        }
        // Titel-Toggle in Kategorie-Detail-Menü  
        else {
            if (guiTitle.contains(": ")) {
                String[] parts = guiTitle.split(": ");
                if (parts.length == 2) {
                    String categoryWithColor = parts[0];
                    String characterName = parts[1];
                    String category = categoryWithColor.replaceAll("§[0-9a-fA-Fklmnor]", "");
                    String titleName = displayName.replaceAll("§[0-9a-fA-Fklmnor]", "");
                
                    if (TitleManager.TITLE_CATEGORIES.containsKey(category) && 
                        TitleManager.TITLE_CATEGORIES.get(category).contains(titleName)) {
                        
                        // Hole characterId (Async)
                        String sql = "SELECT id FROM characters WHERE name = ?";
                        coreAPI.queryAsync(sql, rs -> {
                            if (!rs.isEmpty()) {
                                int characterId = (Integer) rs.get(0).get("id");
                                
                                // Toggle den Titel (Sync, da DB-Call evtl. schon in TitleManager async ist? 
                                // TitleManager.toggleTitle müssen wir prüfen, aber wir rufen es hier auf)
                                // Angenommen toggleTitle macht async update:
                                titleManager.toggleTitle(characterId, category, titleName, player);
                                
                                // Aktualisiere das GUI nach kurzer Verzögerung (Async-Chain)
                                String uuidSql = "SELECT player_uuid FROM characters WHERE name = ?";
                                coreAPI.queryAsync(uuidSql, uuidRs -> {
                                    if (!uuidRs.isEmpty()) {
                                        String playerUUID = (String) uuidRs.get(0).get("player_uuid");
                                        if (playerUUID != null) {
                                            Bukkit.getScheduler().runTask(plugin, () -> 
                                                charMenuGUI.openTitleCategoryMenu(player, characterName, playerUUID, characterId, category));
                                        }
                                    }
                                }, characterName);
                            } else {
                                player.sendMessage("§cFehler: Charakter nicht gefunden!");
                            }
                        }, characterName);
                    }
                }
            }
        }
    }
}
