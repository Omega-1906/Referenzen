package me.Laemedir.character;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.Laemedir.character.dice.*;
import me.Laemedir.character.disguise.*;
import me.Laemedir.character.skilltree.*;
import me.Laemedir.character.aging.*;
import me.Laemedir.coreApi.CoreAPIPlugin;
import me.Laemedir.coreApi.debug.DebugManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MultiCharPlugin extends JavaPlugin {

    // Moderner Prefix (Passe diesen nach Wunsch an)
    private final String prefix = "§8[§6Characters§8] ";

    CoreAPIPlugin coreAPI;
    private DebugManager debugManager;
    private DisguiseManager disguiseManager;
    private AgingSystem agingSystem;
    private NameCategoryHandler nameCategoryHandler;
    private AntiAgingChestManager antiAgingChestManager;
    private final Set<UUID> playersInCharacterSelection = new HashSet<>();
    // Speichere pro Spieler den aktiven Charakternamen
    public static final Map<UUID, String> activeCharacters = new HashMap<>();
    // Speichert Spieler, die noch "eingefroren" sind (während der Charakter-Auswahl)
    private final Set<UUID> frozenPlayers = new HashSet<>();
    // Speichert Spieler, die sich im Prozess der Charaktererstellung befinden
    private final Set<UUID> awaitingCreation = new HashSet<>();
    // Set für Spieler, die noch auf das Ressourcenpaket warten
    private final Set<Player> pendingResourcePackPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("core-api") != null) {
            coreAPI = (CoreAPIPlugin) getServer().getPluginManager().getPlugin("core-api");
        } else {
            getLogger().severe("CoreAPIPlugin konnte nicht gefunden werden! Das Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialisiert den Debug-Manager
        this.debugManager = coreAPI.getDebugManager();
        if (debugManager != null) {
            debugManager.info("character", "Startup", "Debug-System erfolgreich initialisiert");
        }
        createCharacterTable();
        createRaceTable();
        createTitlesTable();

        // Registriert den Command-Executor für /character (Hauptbefehl)
        getCommand("characters").setExecutor(new CharactersCommand(this));

        // Initialisiert Manager und Commands
        RaceManager raceManager = new RaceManager(coreAPI, this);
        getCommand("race").setExecutor(new RaceCommands(raceManager));
        getCommand("race").setTabCompleter(new RaceCreateTabCompleter());
        getCommand("deletecharacter").setExecutor(new DeleteCharacterCommand(coreAPI));
        getCommand("persona").setExecutor(new PersonaCommand(this, coreAPI));
        getCommand("persona").setTabCompleter(new PersonaTabCompleter(this, coreAPI));
        
        DiceManager diceManager = new DiceManager(this);

        // Würfel-System registrieren
        getCommand("rpdice").setExecutor(new RPDiceCommand(this, diceManager));
        getCommand("rpdice").setTabCompleter(new RPDiceTabCompleter());

        ArmorClassCommand armorClassCommand = new ArmorClassCommand(this, diceManager);
        AttackRollCommand attackRollCommand = new AttackRollCommand(this, diceManager);

        getCommand("armorclass").setExecutor(armorClassCommand);
        getCommand("attackroll").setExecutor(attackRollCommand);

        getCommand("armorclass").setTabCompleter(new ArmorClassTabCompleter(this));
        getCommand("attackroll").setTabCompleter(new AttackRollTabCompleter(this));

        // Event-Listener registrieren
        WorldChangeListener worldChangeListener = new WorldChangeListener(this, raceManager);
        getServer().getPluginManager().registerEvents(worldChangeListener, this);

        setupSkillTreeSystem();

        // Registriere Listener für interaktive Chat-Eingaben
        getServer().getPluginManager().registerEvents(new RaceChatListener(raceManager, this), this);

        getServer().getPluginManager().registerEvents(new ChatInputRegistry(), this);
        getServer().getPluginManager().registerEvents(new CharacterSaveListener(this), this);
        getServer().getPluginManager().registerEvents(new CharacterJoinListener(this, coreAPI), this);
        getServer().getPluginManager().registerEvents(new CharacterGUIListener(this, coreAPI), this);
        getServer().getPluginManager().registerEvents(new PlayerFreezeListener(this), this);

        // Verkleidungs-System (Disguise) initialisieren
        this.disguiseManager = new DisguiseManager(this, coreAPI);

        getCommand("disguise").setExecutor(new DisguiseCommand(this, disguiseManager));
        getCommand("givedisguise").setExecutor(new GiveDisguiseCommand(this, disguiseManager));
        getCommand("verwandlung").setExecutor(new VerwandlungCommand(this, disguiseManager));

        DisguiseTabCompleter disguiseTabCompleter = new DisguiseTabCompleter();
        getCommand("disguise").setTabCompleter(disguiseTabCompleter);
        getCommand("givedisguise").setTabCompleter(disguiseTabCompleter);
        getCommand("removedisguise").setExecutor(new RemoveDisguiseCommand(disguiseManager));
        getCommand("removedisguise").setTabCompleter(new RemoveDisguiseCommand(disguiseManager));

        // Alterungs-System initialisieren
        this.agingSystem = new AgingSystem(this, coreAPI);
        this.antiAgingChestManager = new AntiAgingChestManager(this, coreAPI);

        getServer().getPluginManager().registerEvents(new AntiAgingChestListener(this, antiAgingChestManager), this);
        getServer().getPluginManager().registerEvents(new AntiAgingPotionListener(this, agingSystem), this);

        getCommand("antiagingchest").setExecutor(new AntiAgingChestCommand(this, antiAgingChestManager));
        getCommand("antiagingchest").setTabCompleter(new AntiAgingChestTabCompleter());
        getCommand("antiagingtrank").setExecutor(new AntiAgingPotionCommand(this));

        getServer().getPluginManager().registerEvents(new DisguiseGUIListener(this, disguiseManager), this);

        // Title-System initialisieren
        TitleManager titleManager = new TitleManager(coreAPI, this);
        
        // Menü-System registrieren
        getCommand("charmenu").setExecutor(new CharMenuCommand(this, coreAPI, titleManager));
        getServer().getPluginManager().registerEvents(new CharMenuGUIListener(this, coreAPI, titleManager), this);
        
        // Editor-System registrieren
        getCommand("charedit").setExecutor(new CharEditCommand(this, coreAPI));
        getCommand("charedit").setTabCompleter(new CharEditTabCompleter(coreAPI));

        if (debugManager != null) {
            debugManager.info("character", "Startup", "MultiCharPlugin erfolgreich aktiviert.");
        }
    }

    @Override
    public void onDisable() {
        // Speichere alle aktiven Charaktere vor dem Herunterfahren
        if (debugManager != null) {
            debugManager.info("character", "Shutdown", "Speichere alle aktiven Charaktere vor dem Herunterfahren");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            String activeCharacter = getActiveCharacter(player.getUniqueId());
            if (activeCharacter != null) {
                try {
                    // Synchrone Speicherung beim Server-Shutdown
                    performSaveCharacter(player, activeCharacter, false);
                    if (debugManager != null) {
                        debugManager.info("character", "Shutdown", "Charakter " + activeCharacter + " von " + player.getName() + " wurde gespeichert");
                    }
                } catch (Exception e) {
                    if (debugManager != null) {
                        debugManager.error("character", "Shutdown", "Fehler beim Speichern des Charakters " + activeCharacter + " von " + player.getName(), e);
                    }
                }
            }
        }

        if (agingSystem != null) {
            agingSystem.shutdown();
        }

        if (debugManager != null) {
            debugManager.info("character", "Shutdown", "MultiCharPlugin deaktiviert");
        }
    }

    /**
     * Synchrone Version der Charakterspeicherung für das Herunterfahren
     */
    /**
     * Führt die Speicherung eines Charakters durch.
     * Bündelt die Logik für synchrone und asynchrone Speicherung.
     *
     * @param player        Der Spieler, dessen Charakter gespeichert wird.
     * @param characterName Der Name des zu speichernden Charakters.
     * @param async         Ob die Datenbankoperation asynchron ausgeführt werden soll.
     */
    private void performSaveCharacter(Player player, String characterName, boolean async) {
        if (characterName == null) return;

        String serializedInventory;
        try {
            serializedInventory = InventoryUtil.itemStackArrayToBase64(player.getInventory().getContents());
        } catch (IllegalStateException e) {
            String errorMsg = "Fehler beim Serialisieren des Inventars von " + player.getName();
            if (debugManager != null) {
                debugManager.error("character", "Character Save", errorMsg, e);
            } else {
                getLogger().severe(errorMsg);
            }
            return;
        }

        Location loc = player.getLocation();
        String world = loc.getWorld().getName();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();
        String activeNameCategory = getActiveNameCategoryForSave(player);

        String sql = "UPDATE characters SET inventory = ?, position_x = ?, position_y = ?, position_z = ?, world = ?, yaw = ?, pitch = ?, active_name_category = ? WHERE player_uuid = ? AND name = ?";
        
        Object[] params = new Object[]{
            serializedInventory, loc.getX(), loc.getY(), loc.getZ(), world, yaw, pitch, activeNameCategory, player.getUniqueId().toString(), characterName
        };

        if (async) {
            coreAPI.executeUpdateAsync(sql, params);
            // Nur bei explizitem asynchronen Speichern (meist manuell oder Auto-Save) eine Nachricht senden
            // Bei Shutdown (sync) ist das oft zu spammy oder kommt nicht mehr an
        } else {
            try {
                coreAPI.executeUpdateSync(sql, params);
            } catch (Exception e) {
                if (debugManager != null) {
                    debugManager.error("character", "Character Save", "Fehler beim synchronen Speichern von " + characterName, e);
                }
            }
        }
    }

    /**
     * Veraltet: Nutze performSaveCharacter.
     * Diese Methode wird beibehalten, falls externe Plugins darauf zugreifen, 
     * leitet aber intern weiter.
     */
    private void saveActiveCharacterSync(Player player) {
        String characterName = getActiveCharacter(player.getUniqueId());
        if (characterName != null) {
            performSaveCharacter(player, characterName, false);
        }
    }

    /**
     * Erstellt oder aktualisiert die 'races'-Tabelle in der Datenbank.
     */
    private void createRaceTable() {
        String sql = "CREATE TABLE IF NOT EXISTS races ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "name VARCHAR(32) NOT NULL UNIQUE, "
                + "description TEXT, "
                + "effects TEXT"
                + ")";
        try {
            coreAPI.executeUpdateSync(sql);
            if (debugManager != null) {
                debugManager.info("character", "Database", "Tabelle 'races' überprüft oder erstellt.");
            }
        } catch (Exception e) {
            if (debugManager != null) {
                debugManager.error("character", "Database", "Fehler beim Erstellen der Tabelle 'races'", e);
            }
        }
    }

    /**
     * Erstellt oder aktualisiert die 'characters'-Tabelle in der Datenbank.
     */
    private void createCharacterTable() {
        String sql = "CREATE TABLE IF NOT EXISTS characters ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "name VARCHAR(16) NOT NULL, "
                + "inventory TEXT, "
                + "position_x DOUBLE, "
                + "position_y DOUBLE, "
                + "position_z DOUBLE, "
                + "world VARCHAR(32), "
                + "yaw DOUBLE, "
                + "pitch DOUBLE, "
                + "active_name_category VARCHAR(20) DEFAULT 'name', "
                + "gamemode VARCHAR(20) DEFAULT 'SURVIVAL', "
                + "UNIQUE KEY(player_uuid, name)"
                + ")";
        try {
            coreAPI.executeUpdateSync(sql);
            if (debugManager != null) {
                debugManager.info("character", "Database", "Tabelle 'characters' überprüft oder erstellt.");
            }
        } catch (Exception e) {
            if (debugManager != null) {
                debugManager.error("character", "Database", "Fehler beim Erstellen der Tabelle 'characters'", e);
            }
        }
    }

    /**
     * Setzt den aktiven Charakter für einen Spieler.
     * Aktualisiert auch den 'last_login' Zeitstempel in der Datenbank.
     *
     * @param playerUUID    Die UUID des Spielers.
     * @param characterName Der Name des aktiven Charakters.
     */
    public void setActiveCharacter(UUID playerUUID, String characterName) {
        activeCharacters.put(playerUUID, characterName);

        // Aktualisiere last_login Zeitstempel
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String updateQuery = "UPDATE characters SET last_login = ? WHERE player_uuid = ? AND name = ?";
        coreAPI.executeUpdateAsync(updateQuery, currentTime, playerUUID.toString(), characterName);
    }

    public void removeActiveCharacter(UUID playerUUID) {
        activeCharacters.remove(playerUUID);
    }

    public static String getActiveCharacter(UUID playerUUID) {
        return activeCharacters.get(playerUUID);
    }

    // Getter/Setter für awaitingCreation
    public void addAwaitingCreation(Player player) {
        awaitingCreation.add(player.getUniqueId());
    }

    public void removeAwaitingCreation(Player player) {
        awaitingCreation.remove(player.getUniqueId());
    }

    public void addPlayerInCharacterSelection(UUID playerUUID) {
        playersInCharacterSelection.add(playerUUID);
    }

    public void removePlayerInCharacterSelection(UUID playerUUID) {
        playersInCharacterSelection.remove(playerUUID);
    }

    public boolean isPlayerInCharacterSelection(UUID playerUUID) {
        return playersInCharacterSelection.contains(playerUUID);
    }

    // Methoden zum "Einfrieren" (freezen) von Spielern
    public void addFrozen(Player player) {
        frozenPlayers.add(player.getUniqueId());
    }

    public void removeFrozen(Player player) {
        frozenPlayers.remove(player.getUniqueId());
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }

    private void setupSkillTreeSystem() {
        // Initialisiere den SkillTreeManager
        SkillTreeManager skillManager = new SkillTreeManager(coreAPI, this);

        // Registriere den Listener für Skill-Tree-Interaktionen
        SkillTreeListener listener = new SkillTreeListener(skillManager, this);
        getServer().getPluginManager().registerEvents(listener, this);

        // Registriere die Befehle
        SkillTreeCommand skillTreeCommand = new SkillTreeCommand(coreAPI, skillManager, this);
        getCommand("skilltree").setExecutor(skillTreeCommand);
        getCommand("skilltree").setTabCompleter(new SkillTreeTabCompleter());

        if (debugManager != null) {
            debugManager.info("character", "Startup", "Skill-Tree-System wurde erfolgreich initialisiert");
        }
    }

    /**
     * Gibt die ID des aktiven Charakters eines Spielers zurück.
     *
     * @param playerUUID Die UUID des Spielers
     * @return Die ID des aktiven Charakters oder -1, wenn kein aktiver Charakter gefunden wurde
     */
    public static int getActiveCharacterId(UUID playerUUID) {
        String characterName = getActiveCharacter(playerUUID);
        if (characterName == null) {
            return -1;
        }

        try {
            // CoreAPI-Instanz holen
            CoreAPIPlugin coreAPI = CoreAPIPlugin.getPlugin(CoreAPIPlugin.class);

            // SQL-Abfrage, um die ID des Charakters zu erhalten
            String sql = "SELECT id FROM characters WHERE player_uuid = ? AND name = ?";

            // Synchrone Abfrage, da wir das Ergebnis sofort benötigen (z.B. für API-Calls)
            return coreAPI.querySync(sql, rs -> {
                try {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return -1;
            }, playerUUID.toString(), characterName);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Fehler beim Abrufen der Charakter-ID: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Führt die Erstellung eines neuen Charakters aus.
     */
    public void createCharacterFromChat(Player player, String name) {
        // Entferne den Wartezustand
        removeAwaitingCreation(player);

        // Prüfe, ob der Spieler MultiChar aktiviert hat (Datenbankabfrage)
        String multicharCheckSql = "SELECT multichar_enabled FROM players WHERE uuid = ?";
        boolean multicharEnabled = false;
        try {
            multicharEnabled = coreAPI.querySync(multicharCheckSql, rs -> {
                try {
                    if (rs.next()) {
                        return rs.getBoolean("multichar_enabled");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }, player.getUniqueId().toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (!multicharEnabled) {
            player.sendMessage(prefix + "§cDu kannst keinen zweiten Charakter erstellen. Bitte frage ein Teammitglied, ob MultiChar für dich aktiviert werden kann.");
            return;
        }

        // Prüfe, ob der Name bereits verwendet wird (durch den Spieler selbst)
        String duplicateSql = "SELECT COUNT(*) AS count FROM characters WHERE player_uuid = ? AND name = ?";
        int duplicateCount;
        try {
            duplicateCount = coreAPI.querySync(duplicateSql, rs -> {
                try {
                    if (rs.next()) {
                        return rs.getInt("count");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 0;
            }, player.getUniqueId().toString(), name);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (duplicateCount > 0) {
            player.sendMessage(prefix + "Ein Charakter mit diesem Namen existiert bereits. Bitte wähle einen anderen Namen.");
            addAwaitingCreation(player); // Spieler bleibt im Wartezustand
            return;
        }

        // Prüfe, ob der Spieler bereits 2 Charaktere hat
        String countSql = "SELECT COUNT(*) AS count FROM characters WHERE player_uuid = ?";
        int count;
        try {
            count = coreAPI.querySync(countSql, rs -> {
                try {
                    if (rs.next()) {
                        return rs.getInt("count");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 0;
            }, player.getUniqueId().toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (count >= 2) {
            player.sendMessage(prefix + "Du kannst maximal 2 Charaktere erstellen!");
            return;
        }

        // Erstelle neuen Charakter mit Default-Daten
        String sql = "INSERT INTO characters (player_uuid, name, inventory, position_x, position_y, position_z, world, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String emptyInventory;
        try {
            emptyInventory = InventoryUtil.itemStackArrayToBase64(new ItemStack[player.getInventory().getSize()]);
        } catch (IllegalStateException e) {
            player.sendMessage(prefix + "Fehler beim Serialisieren des Inventars.");
            return;
        }

        // Standard-Spawn-Position für neue Charaktere
        // TODO: Diese Koordinaten sollten konfigurierbar sein
        String world = "world";
        double x = -685;
        double y = 69;
        double z = -502;
        double yaw = -112.7;
        double pitch = 4.2;

        coreAPI.executeUpdateAsync(sql,
                player.getUniqueId().toString(),
                name,
                emptyInventory,
                x,
                y,
                z,
                world,
                yaw,
                pitch
        );
        player.sendMessage(prefix + "Charakter " + name + " wurde erfolgreich erstellt!");

        // Nach kurzer Verzögerung das Auswahl-GUI erneut öffnen (um Synchronisationsprobleme zu vermeiden)
        getServer().getScheduler().runTaskLater(this, () -> {
            addFrozen(player);
            openCharacterSelectionGUI(player);
        }, 20L);
    }

    /**
     * Speichert den aktiven Charakter (Inventar, Position etc.) in der Datenbank.
     */
    /**
     * Speichert den aktiven Charakter (Inventar, Position etc.) in der Datenbank.
     * Nutzt intern die performSaveCharacter Methode für konsistente Logik.
     */
    public void saveActiveCharacter(Player player) {
        String characterName = getActiveCharacter(player.getUniqueId());
        if (characterName == null) return;
        
        performSaveCharacter(player, characterName, true);
        player.sendMessage(prefix + "§aDein aktiver Charakter §e(" + characterName + ")§a wurde gespeichert.");
    }

    public void openCharacterSelectionGUI(Player player) {
        // Erstelle ein Inventar mit 9 Slots und einem Titel
        Inventory inv = Bukkit.createInventory(null, 9, "§6§lCharakterauswahl");

        // Füge "Server verlassen" Button ganz links hinzu (Slot 0)
        ItemStack leaveItem = new ItemStack(Material.BARRIER);
        ItemMeta leaveMeta = leaveItem.getItemMeta();
        if (leaveMeta != null) {
            leaveMeta.setDisplayName("§cServer verlassen");
            leaveMeta.setLore(Arrays.asList("§7Klicke hier, um den Server zu verlassen"));
            leaveItem.setItemMeta(leaveMeta);
        }
        inv.setItem(0, leaveItem);

        // Hole alle bestehenden Charaktere des Spielers mit Skin-Daten
        String characterSql = "SELECT c.player_uuid, c.id, c.name, c.rufname, c.deckname, c.verwandlung, c.race_id, c.gender, c.age, c.status, c.deactivation_reason, c.active_skin_slot, s.skin_value, s.skin_signature " +
                "FROM characters c " +
                "LEFT JOIN rp_skins s ON c.player_uuid = s.uuid AND c.name = s.name AND c.active_skin_slot = s.slot " +
                "WHERE c.player_uuid = ? ORDER BY c.id ASC";

        coreAPI.queryAsync(characterSql, characterResults -> {
            // Erstelle eine Map, um die Charaktere nach ID zu sortieren
            Map<Integer, Map<String, Object>> charactersByID = new HashMap<>();

            // Fülle die Map mit den Charakterdaten
            for (Map<String, Object> row : characterResults) {
                int characterId = 0;
                if (row.get("id") != null) {
                    if (row.get("id") instanceof Number) {
                        characterId = ((Number) row.get("id")).intValue();
                    } else {
                        try {
                            characterId = Integer.parseInt(row.get("id").toString());
                        } catch (NumberFormatException e) {
                            if (debugManager != null) {
                                debugManager.warning("character", "Data Parsing", "Fehler beim Parsen der Character-ID: " + e.getMessage());
                            }
                        }
                    }
                }
                charactersByID.put(characterId, row);
            }

            // Slots für die Charaktere - feste Positionen
            int[] slots = {2, 6}; // Erster Charakter links (Slot 2), zweiter Charakter rechts (Slot 6)

            // Sortiere die Charaktere nach ID (ältester Charakter zuerst)
            List<Integer> sortedIds = new ArrayList<>(charactersByID.keySet());
            Collections.sort(sortedIds);

            // Platziere die Charaktere in den vordefinierten Slots
            for (int i = 0; i < Math.min(sortedIds.size(), slots.length); i++) {
                int characterId = sortedIds.get(i);
                Map<String, Object> row = charactersByID.get(characterId);

                String playerUuidString = row.get("player_uuid") != null ? row.get("player_uuid").toString() : null;
                if (playerUuidString == null) {
                    continue; // Sollte nicht passieren
                }

                String characterName = row.get("name") != null ? row.get("name").toString() : "Unbekannt";
                String rufname = row.get("rufname") != null ? row.get("rufname").toString() : "Kein Rufname";
                String deckname = row.get("deckname") != null ? row.get("deckname").toString() : "Kein Deckname";
                String verwandlung = row.get("verwandlung") != null ? row.get("verwandlung").toString() : "Keine Verwandlung";
                String gender = row.get("gender") != null ? row.get("gender").toString() : "Unbekannt";
                String age = row.get("age") != null ? row.get("age").toString() : "Unbekannt";

                // Skin-Daten abrufen
                String skinValue = row.get("skin_value") != null ? row.get("skin_value").toString() : null;
                String skinSignature = row.get("skin_signature") != null ? row.get("skin_signature").toString() : null;

                // Status und Deaktivierungsgrund abrufen - sicher parsen
                int status = 1; // Standardwert: 1 (aktiv)
                if (row.get("status") != null) {
                    try {
                        if (row.get("status") instanceof Number) {
                            status = ((Number) row.get("status")).intValue();
                        } else {
                            status = Integer.parseInt(row.get("status").toString());
                        }
                    } catch (Exception e) {
                        if (debugManager != null) {
                            debugManager.warning("character", "Data Parsing", "Fehler beim Parsen des Status für " + characterName + ": " + e.getMessage());
                        }
                    }
                }

                String deactivationReason = row.get("deactivation_reason") != null ? row.get("deactivation_reason").toString() : "";

                // Rasse-ID abrufen
                Object raceIdObj = row.get("race_id");
                int raceId = 0;
                if (raceIdObj != null) {
                    try {
                         raceId = Integer.parseInt(raceIdObj.toString());
                    } catch (NumberFormatException ignored) {}
                }
                
                // Rassenname aus der Tabelle `races` abrufen
                String raceSql = "SELECT race_name FROM races WHERE id = ?";
                final int finalStatus = status;
                final String finalDeactivationReason = deactivationReason;
                final int slotIndex = i;
                final String finalSkinValue = skinValue;
                final String finalSkinSignature = skinSignature;

                coreAPI.queryAsync(raceSql, raceResults -> {
                    String race = "Unbekannte Rasse"; // Standardwert
                    if (!raceResults.isEmpty()) {
                        Map<String, Object> raceRow = raceResults.get(0);
                        race = raceRow.get("race_name") != null ? raceRow.get("race_name").toString() : "Unbekannte Rasse";
                    }

                    try {
                        UUID playerUUID = UUID.fromString(playerUuidString);

                        // Löschanträge werden nicht mehr überprüft
                        boolean hasPendingDeleteRequest = false;

                        inv.setItem(slots[slotIndex], createCharacterItem(characterName, rufname, deckname, verwandlung, race, gender, age, playerUUID, hasPendingDeleteRequest, finalStatus, finalDeactivationReason, finalSkinValue, finalSkinSignature));

                        // Wenn alle Charaktere geladen sind, öffne das Inventar
                        if (slotIndex == sortedIds.size() - 1 || slotIndex == slots.length - 1) {
                            player.openInventory(inv);
                        }
                    } catch (IllegalArgumentException e) {
                        if (debugManager != null) {
                            debugManager.warning("character", "Data Parsing", "Ungültige UUID: " + playerUuidString);
                        }
                    }
                }, raceId);
            }

            // Wenn keine Charaktere vorhanden sind, öffne trotzdem das Inventar
            if (characterResults.isEmpty()) {
                player.openInventory(inv);
            }
        }, player.getUniqueId().toString());
    }

    private ItemStack createCharacterItem(String name, String rufname, String deckname, String verwandlung, String race, String gender, String age, UUID playerUUID, boolean hasPendingDeleteRequest, int status, String deactivationReason, String skinValue, String skinSignature) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD); // Spieler-Kopf als Item
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        if (meta != null) {
            // Setze den Namen des Charakters als Display Name mit Statusindikator
            if (status == 0) {
                // Deaktiviert - gelb
                meta.setDisplayName("§e" + name + " §8[§eDeaktiviert§8]");
            } else if (status == 2) {
                // Gesperrt - rot
                meta.setDisplayName("§c" + name + " §8[§4Gesperrt§8]");
            } else {
                // Aktiv
                meta.setDisplayName("§e" + name + " §8[§aAktiv§8]");
            }

            // Erstelle die Lore mit den Charakterinformationen
            List<String> lore = new ArrayList<>();
            lore.add("§7Rufname: §f" + rufname);
            lore.add("§7Deckname: §f" + deckname);
            lore.add("§7Verwandlung: §f" + verwandlung);
            lore.add("§f------------------------------------------------------------------");

            // Weitere Informationen hinzufügen: Rasse, Geschlecht, Alter
            lore.add("§7Rasse: §f" + race);
            lore.add("§7Geschlecht: §f" + gender);
            lore.add("§7Alter: §f" + age + " §fJahre alt");

            // Status und Grund für Deaktivierung anzeigen, falls vorhanden
            if (status == 0 || status == 2) {
                lore.add("§f------------------------------------------------------------------");

                if (status == 0) {
                    lore.add("§e⚠ Dieser Charakter wurde deaktiviert:");
                } else { // status == 2
                    lore.add("§4⚠ Dieser Charakter wurde gesperrt:");
                }

                // Zeige den Deaktivierungsgrund an, falls vorhanden
                if (deactivationReason != null && !deactivationReason.isEmpty()) {
                    // Teile langen Text in mehrere Zeilen auf
                    String[] reasonLines = splitText(deactivationReason, 40);
                    for (String line : reasonLines) {
                        if (status == 0) {
                            lore.add("§e" + line); // Gelb
                        } else {
                            lore.add("§c" + line); // Rot
                        }
                    }
                } else {
                    lore.add(status == 0 ? "§eKein Grund angegeben" : "§cKein Grund angegeben");
                }
            }

            meta.setLore(lore);

            // Setze den Skin, falls vorhanden
            if (skinValue != null && skinSignature != null && !skinValue.isEmpty() && !skinSignature.isEmpty()) {
                try {
                    PlayerProfile profile = Bukkit.createProfile(playerUUID);
                    profile.setProperty(new ProfileProperty("textures", skinValue, skinSignature));
                    meta.setPlayerProfile(profile);
                } catch (Exception e) {
                    // Falls Skin-Laden fehlschlägt, verwende Standard-Spielerkopf
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUUID));
                    if (debugManager != null) {
                        debugManager.error("character", "Skin Loading", "Fehler beim Setzen des Skins für " + name, e);
                    }
                }
            } else {
                // Verwende Standard-Spielerkopf, wenn kein Skin vorhanden ist
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUUID));
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Teilt einen Text in mehrere Zeilen auf, basierend auf der maximalen Länge pro Zeile.
     */
    private String[] splitText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();

        // Teile den Text in Wörter auf
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            // Wenn das Wort zur aktuellen Zeile passt
            if (currentLine.length() + word.length() + 1 <= maxLength) {
                // Füge ein Leerzeichen hinzu, wenn die Zeile nicht leer ist
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                // Füge die aktuelle Zeile zur Liste hinzu und beginne eine neue Zeile
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        // Füge die letzte Zeile hinzu, wenn sie nicht leer ist
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines.toArray(new String[0]);
    }

    /**
     * Wird aufgerufen, wenn ein Spieler im GUI einen Charakter auswählt.
     * Lädt den Charakter (Inventar, Position etc.) und entfernt den Freeze.
     * Prüft den Status des Charakters und verhindert die Auswahl bei deaktivierten Charakteren.
     */
    public void selectCharacterFromGUI(Player player, String name) {
        String sql = "SELECT c.*, s.skin_value, s.skin_signature FROM characters c " +
                "LEFT JOIN rp_skins s ON c.player_uuid = s.uuid AND c.name = s.name " +
                "AND s.slot = c.active_skin_slot " +
                "WHERE c.player_uuid = ? AND c.name = ?";

        coreAPI.queryAsync(sql, results -> {
            if (results.isEmpty()) {
                player.sendMessage(prefix + "§cKein Charakter mit diesem Namen gefunden.");
                return;
            }
            Map<String, Object> data = results.get(0);

            // Status des Charakters prüfen - sicher parsen
            int status = 1; // Standardwert: 1 (aktiv)
            if (data.get("status") != null) {
                try {
                    if (data.get("status") instanceof Number) {
                        status = ((Number) data.get("status")).intValue();
                    } else {
                        status = Integer.parseInt(data.get("status").toString());
                    }
                } catch (Exception e) {
                    if (debugManager != null) {
                        debugManager.error("character", "Data Parsing", "Fehler beim Parsen des Status für " + name, e);
                    }
                }
            }

            String deactivationReason = data.get("deactivation_reason") != null ? data.get("deactivation_reason").toString() : "";

            // Wenn der Charakter deaktiviert oder gesperrt ist, verweigere die Auswahl
            if (status == 0 || status == 2) {
                if (status == 0) {
                    player.sendMessage(prefix + "§e⚠ Dieser Charakter wurde deaktiviert und kann nicht ausgewählt werden.");
                } else { // status == 2
                    player.sendMessage(prefix + "§c⚠ Dieser Charakter wurde gesperrt und kann nicht ausgewählt werden.");
                }

                // Zeige den Deaktivierungsgrund an, falls vorhanden
                if (deactivationReason != null && !deactivationReason.isEmpty()) {
                    player.sendMessage(prefix + (status == 0 ? "§e" : "§c") + "Grund: §7" + deactivationReason);
                }

                // Öffne das Auswahlmenü erneut
                openCharacterSelectionGUI(player);
                return;
            }

            // Inventar laden
            String serializedInventory = (String) data.get("inventory");
            try {
                ItemStack[] items = InventoryUtil.itemStackArrayFromBase64(serializedInventory);
                player.getInventory().setContents(items);
            } catch (Exception e) {
                player.sendMessage(prefix + "§cFehler beim Laden des Inventars.");
                e.printStackTrace();
            }

            // Position und Blickrichtung laden
            double x = 0, y = 0, z = 0, yaw = 0, pitch = 0;
            try {
                x = parseDoubleValue(data.get("position_x"));
                y = parseDoubleValue(data.get("position_y"));
                z = parseDoubleValue(data.get("position_z"));
                yaw = parseDoubleValue(data.get("yaw"));
                pitch = parseDoubleValue(data.get("pitch"));
            } catch (Exception e) {
                if (debugManager != null) {
                    debugManager.error("character", "Data Parsing", "Fehler beim Parsen der Positionsdaten für " + name, e);
                }
            }

            String worldName = (String) data.get("world");

            if (Bukkit.getWorld(worldName) != null) {
                Location loc = new Location(Bukkit.getWorld(worldName), x, y, z, (float) yaw, (float) pitch);
                player.teleport(loc);
            } else {
                player.sendMessage(prefix + "§cDie gespeicherte Welt wurde nicht gefunden. Du wirst zur Standard-Welt teleportiert.");
                // Fallback zur Standard-Welt (Spawn-Location)
                // TODO: Konfigurierbaren Spawn verwenden
                Location defaultLoc = Bukkit.getWorld("world").getSpawnLocation();
                player.teleport(defaultLoc);

                // Aktualisiere die Datenbank mit den Spawn-Koordinaten
                String updateLocationSql = "UPDATE characters SET position_x = ?, position_y = ?, position_z = ?, world = ?, yaw = ?, pitch = ? WHERE player_uuid = ? AND name = ?";
                coreAPI.executeUpdateAsync(updateLocationSql,
                        defaultLoc.getX(), defaultLoc.getY(), defaultLoc.getZ(),
                        defaultLoc.getWorld().getName(), defaultLoc.getYaw(), defaultLoc.getPitch(),
                        player.getUniqueId().toString(), name
                );
            }

            // Skin laden, falls vorhanden
            String skinValue = (String) data.get("skin_value");
            String skinSignature = (String) data.get("skin_signature");

            if (skinValue != null && skinSignature != null) {
                try {
                    PlayerProfile profile = Bukkit.createProfile(player.getUniqueId());
                    profile.setProperty(new ProfileProperty("textures", skinValue, skinSignature));
                    player.setPlayerProfile(profile);
                } catch (Exception e) {
                    player.sendMessage(prefix + "§cFehler beim Laden des Skins.");
                    e.printStackTrace();
                }
            }

            // Aktive Namenskategorie laden und setzen
            String activeNameCategory = data.get("active_name_category") != null ?
                    data.get("active_name_category").toString() : "name";

            // RPChatListener über die aktive Namenskategorie informieren
            loadActiveNameCategory(player, activeNameCategory);

            // Charakter als aktiv setzen
            setActiveCharacter(player.getUniqueId(), name);
            removePlayerInCharacterSelection(player.getUniqueId());
            removeFrozen(player);
            player.closeInventory();

            String savedGamemode = data.get("gamemode") != null ? data.get("gamemode").toString() : "SURVIVAL";
            try {
                GameMode gameMode = GameMode.valueOf(savedGamemode.toUpperCase());
                player.setGameMode(gameMode);
            } catch (IllegalArgumentException e) {
                if (debugManager != null) {
                    debugManager.warning("character", "Gamemode", "Ungültiger Gamemode für " + name + ": " + savedGamemode + ", verwende SURVIVAL");
                }
                player.setGameMode(GameMode.SURVIVAL);
            }

            // Entferne Blindheit und Unsichtbarkeit
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);

            // Vor dem Laden des neuen Charakters sicherstellen, dass keine alten Effekte bleiben
            clearEffects(player);

            // Rasse und Effekte laden - sicher parsen
            int characterId = parseIntValue(data.get("id"));
            loadCharacterRace(player, characterId);

            // Aktive Verwandlung laden
            if (disguiseManager != null) {
                disguiseManager.loadActiveDisguise(player, characterId);
            }

            // Setze first_login, wenn es noch nicht gesetzt ist
            agingSystem.setFirstLoginIfNeeded(characterId);

            // Aktuelles Datum und Uhrzeit
            // TODO: Zeitzone korrekt handhaben (Server Time vs Local Time)
            LocalDateTime adjustedTime = LocalDateTime.now().plusHours(2);
            String currentTime = adjustedTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Last_login für den ausgewählten Charakter aktualisieren
            String updateQuery = "UPDATE characters SET last_login = ? WHERE player_uuid = ? AND name = ?";
            coreAPI.executeUpdateAsync(updateQuery, currentTime, player.getUniqueId().toString(), name);

            player.sendMessage(prefix + "§aCharakter §e" + name + " §awurde erfolgreich geladen!");

            // Anzeigen: Derzeitiger ausgewählter Name
            String activeCat = data.get("active_name_category") != null
                    ? data.get("active_name_category").toString()
                    : "name";

            String label;       // Anzeige-Label
            String currentName; // Wert aus der passenden Spalte

            switch (activeCat.toLowerCase()) {
                case "deckname":
                    label = "Deckname";
                    currentName = data.get("deckname") != null ? data.get("deckname").toString() : name;
                    break;
                case "rufname":
                    label = "Rufname";
                    currentName = data.get("rufname") != null ? data.get("rufname").toString() : name;
                    break;
                case "verwandlung":
                    label = "Verwandlung";
                    currentName = data.get("verwandlung") != null ? data.get("verwandlung").toString() : name;
                    break;
                default:
                    label = "Name";
                    currentName = name; // Fallback auf den normalen Char-Namen
                    break;
            }

            player.sendMessage(prefix + "§7Derzeitiger ausgewählter Name: §8(§6" + label + "§8) §e" + currentName);
        }, player.getUniqueId().toString(), name);
    }

    /**
     * Hilfsmethode zum sicheren Parsen von Double-Werten
     */
    private double parseDoubleValue(Object value) {
        if (value == null) {
            return 0.0;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            try {
                return Double.parseDouble(value.toString());
            } catch (Exception e) {
                if (debugManager != null) {
                    debugManager.error("character", "Data Parsing", "Fehler beim Konvertieren zu Double", e);
                }
                return 0.0;
            }
        }
    }

    /**
     * Hilfsmethode zum sicheren Parsen von Integer-Werten
     */
    private int parseIntValue(Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else {
            try {
                return Integer.parseInt(value.toString());
            } catch (Exception e) {
                if (debugManager != null) {
                    debugManager.error("character", "Data Parsing", "Fehler beim Konvertieren zu Integer", e);
                }
                return 0;
            }
        }
    }

    private void loadCharacterRace(Player player, int characterId) {
        String sql = "SELECT c.race_id, r.race_name, r.effects, c.affinity " +
                "FROM characters c " +
                "LEFT JOIN races r ON c.race_id = r.id " +
                "WHERE c.id = ?";
        coreAPI.queryAsync(sql, results -> {
            if (results.isEmpty()) {
                assignDefaultRace(player, characterId);
                return;
            }

            Map<String, Object> data = results.get(0);
            String raceName = (String) data.get("race_name");
            String effectsText = (String) data.get("effects");
            String affinity = (String) data.get("affinity");

            if (raceName == null || raceName.isEmpty() || effectsText == null) {
                // Fallback, falls Daten korrupt sind
                player.sendMessage(prefix + "§cFehlerhafte Daten für die Rasse.");
                return;
            }

            if (affinity == null || affinity.equalsIgnoreCase("Unbekannt")) {
                assignAffinity(player, characterId); // Zufällige Affinität zuweisen
            }

            List<String> effects = parseEffects(effectsText);
            applyEffects(player, effects);

            player.sendMessage(prefix + "§aRasse §e" + raceName + " §awurde geladen und Effekte wurden angewendet.");
        }, characterId);
    }

    /**
     * Weist die Standardrasse "Mensch" zu, wenn keine Rasse gefunden wurde.
     */
    private void assignDefaultRace(Player player, int characterId) {
        String defaultRaceQuery = "SELECT id FROM races WHERE race_name = 'Mensch'";
        coreAPI.queryAsync(defaultRaceQuery, results -> {
            if (results.isEmpty()) {
                player.sendMessage(prefix + "§cDie Standardrasse 'Mensch' ist nicht in der Datenbank vorhanden.");
                return;
            }

            // Sicherer Cast
            int defaultRaceId = 0;
            try {
                Object idObj = results.get(0).get("id");
                if (idObj instanceof Number) {
                    defaultRaceId = ((Number) idObj).intValue();
                } else {
                    defaultRaceId = Integer.parseInt(idObj.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String updateQuery = "UPDATE characters SET race_id = ? WHERE id = ?";
            coreAPI.executeUpdateAsync(updateQuery, defaultRaceId, characterId);

            player.sendMessage(prefix + "§e🌟 Dein Charakter hat keine Rasse und wurde der Rasse §aMensch §ezugewiesen.");
        });
    }

    /**
     * Weist einem Charakter eine zufällige Affinität zu, wenn keine vorhanden ist.
     */
    private void assignAffinity(Player player, int characterId) {
        // Liste der möglichen Affinitäten
        String[] affinities = {"Erde", "Wasser", "Luft", "Feuer"};

        // Zufällige Affinität auswählen
        String randomAffinity = affinities[new Random().nextInt(affinities.length)];

        // SQL-Abfrage, um die Affinität in der Datenbank zu aktualisieren
        String updateAffinityQuery = "UPDATE characters SET affinity = ? WHERE id = ?";
        coreAPI.executeUpdateAsync(updateAffinityQuery, randomAffinity, characterId);
    }

    List<String> parseEffects(String effectsText) {
        if (effectsText == null || effectsText.isEmpty()) {
            return new ArrayList<>();
        }
        // Effekte sind durch Semikolon getrennt
        return Arrays.asList(effectsText.split(";"));
    }

    private void clearEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    public void applyEffects(Player player, List<String> effects) {
        if (effects == null || effects.isEmpty()) {
            return;
        }

        // Prüfe, ob der Spieler sich in der "world" befindet
        // TODO: Welt-Name konfigurierbar machen
        if (!"world".equals(player.getWorld().getName())) {
            // Wenn der Spieler nicht in "world" ist, werden keine Effekte angewendet
            return;
        }

        for (String effectEntry : effects) {
            try {
                String[] parts = effectEntry.trim().split(":");
                // Format: EFFEKT:STÄRKE
                String effectName = parts[0].toUpperCase();
                int amplifier = 0;

                if (parts.length > 1) {
                    try {
                        amplifier = Integer.parseInt(parts[1]) - 1; // 1 = Stufe 0 (Potion Effect Logic)
                        amplifier = Math.max(0, Math.min(9, amplifier)); // Begrenzung auf 0-9
                    } catch (NumberFormatException e) {
                        if (debugManager != null) {
                            debugManager.warning("character", "Effect Parsing", "Ungültige Effektstärke für " + effectName + ": " + parts[1]);
                        }
                    }
                }

                PotionEffectType effectType = PotionEffectType.getByName(effectName);
                if (effectType != null) {
                    // Dauer: Unendlich (-1 oder MAX_INT), Ambient: false, Particles: false, Icon: true
                    player.addPotionEffect(new PotionEffect(effectType, PotionEffect.INFINITE_DURATION, amplifier, false, false, true));
                }
            } catch (Exception e) {
                if (debugManager != null) {
                    debugManager.error("character", "Effect Application", "Fehler beim Anwenden des Effekts: " + effectEntry, e);
                }
            }
        }
    }

    /**
     * Fügt einen Spieler zur Warteliste für das Ressourcenpaket hinzu.
     *
     * @param player Der Spieler, der hinzugefügt wird.
     */
    public void addPendingResourcePackPlayer(Player player) {
        pendingResourcePackPlayers.add(player);
    }

    /**
     * Entfernt einen Spieler aus der Warteliste für das Ressourcenpaket.
     *
     * @param player Der Spieler, der entfernt wird.
     */
    public void removePendingResourcePackPlayer(Player player) {
        pendingResourcePackPlayers.remove(player);
    }

    /**
     * Überprüft, ob ein Spieler noch auf das Ressourcenpaket wartet.
     *
     * @param player Der Spieler, der überprüft wird.
     * @return True, wenn der Spieler noch auf das Ressourcenpaket wartet, ansonsten false.
     */
    public boolean isPendingResourcePack(Player player) {
        return pendingResourcePackPlayers.contains(player);
    }

    // Getter für DisguiseManager
    public DisguiseManager getDisguiseManager() {
        return disguiseManager;
    }

    public AgingSystem getAgingSystem() {
        return agingSystem;
    }

    public DebugManager getDebugManager() {
        return debugManager;
    }

    // Neue Methode zum Setzen des Handlers
    public void setNameCategoryHandler(NameCategoryHandler handler) {
        this.nameCategoryHandler = handler;
    }

    // Ändere die loadActiveNameCategory Methode:
    private void loadActiveNameCategory(Player player, String categoryName) {
        if (nameCategoryHandler != null) {
            nameCategoryHandler.setActiveNameCategoryFromString(player, categoryName);
        }
    }

    // Ändere die getActiveNameCategoryForSave Methode:
    private String getActiveNameCategoryForSave(Player player) {
        if (nameCategoryHandler != null) {
            return nameCategoryHandler.getActiveNameCategoryString(player);
        }
        return "name"; // Fallback
    }
    
    /**
     * Erstellt die character_titles Tabelle für das Titel-System
     */
    private void createTitlesTable() {
        String sql = "CREATE TABLE IF NOT EXISTS character_titles (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "character_id INT NOT NULL, " +
                "title_category ENUM('Fadenmagie', 'Drachen', 'Adel', 'Fluch') NOT NULL, " +
                "title_name VARCHAR(50) NOT NULL, " +
                "is_active BOOLEAN DEFAULT FALSE, " +
                "granted_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "granted_by VARCHAR(36), " +
                "FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE, " +
                "UNIQUE KEY unique_char_title (character_id, title_name)" +
                ")";
        
        try {
            coreAPI.executeUpdateSync(sql);
            if (debugManager != null) {
                debugManager.info("character", "Database", "Titel-Tabelle erfolgreich erstellt oder bereits vorhanden");
            }
        } catch (Exception e) {
            if (debugManager != null) {
                debugManager.error("character", "Database", "Fehler beim Erstellen der Titel-Tabelle", e);
            }
        }
    }
}
