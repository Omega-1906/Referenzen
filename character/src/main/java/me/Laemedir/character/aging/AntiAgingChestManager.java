package me.Laemedir.character.aging;

import me.Laemedir.character.MultiCharPlugin;
import me.Laemedir.coreApi.CoreAPIPlugin;
import me.Laemedir.coreApi.debug.DebugManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet die Anti-Aging-Kiste, aus der Spieler Tränke erhalten können.
 * Speichert die Position der Kiste und verfolgt die Nutzung durch Spieler.
 */
public class AntiAgingChestManager {
    private final MultiCharPlugin plugin;
    private final CoreAPIPlugin coreAPI;
    private final DebugManager debugManager;
    private final String prefix;
    private final File chestFile;
    private final FileConfiguration chestConfig;
    private Location chestLocation;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AntiAgingChestManager(MultiCharPlugin plugin, CoreAPIPlugin coreAPI) {
        this.plugin = plugin;
        this.coreAPI = coreAPI;
        this.debugManager = plugin.getDebugManager();
        this.prefix = "§8[§6Characters§8] ";

        // Erstelle die Konfigurationsdatei
        this.chestFile = new File(plugin.getDataFolder(), "antiaging_chest.yml");
        if (!chestFile.exists()) {
            try {
                chestFile.createNewFile();
            } catch (IOException e) {
                if (debugManager != null) {
                    debugManager.error("character", "Anti-Aging Chest", "Konnte antiaging_chest.yml nicht erstellen", e);
                }
            }
        }
        this.chestConfig = YamlConfiguration.loadConfiguration(chestFile);

        // Lade die Kistenlocation
        loadChestLocation();

        // Erstelle die Tabelle, falls sie noch nicht existiert
        createTableIfNotExists();
    }

    /**
     * Erstellt die Tabelle für die Nutzungsverfolgung in der Datenbank, falls sie noch nicht existiert.
     */
    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS character_antiaging_chest_usage (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "character_id INT NOT NULL, " +
                "last_used DATETIME NOT NULL, " +
                "FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE" +
                ")";

        coreAPI.executeUpdateAsync(sql);
    }

    /**
     * Lädt die gespeicherte Position der Kiste aus der Konfigurationsdatei.
     */
    private void loadChestLocation() {
        if (chestConfig.contains("chest.world") &&
                chestConfig.contains("chest.x") &&
                chestConfig.contains("chest.y") &&
                chestConfig.contains("chest.z")) {

            String world = chestConfig.getString("chest.world");
            double x = chestConfig.getDouble("chest.x");
            double y = chestConfig.getDouble("chest.y");
            double z = chestConfig.getDouble("chest.z");

            this.chestLocation = new Location(Bukkit.getWorld(world), x, y, z);
        }
    }

    /**
     * Speichert die aktuelle Position der Kiste in der Konfigurationsdatei.
     */
    private void saveChestLocation() {
        if (chestLocation != null) {
            chestConfig.set("chest.world", chestLocation.getWorld().getName());
            chestConfig.set("chest.x", chestLocation.getX());
            chestConfig.set("chest.y", chestLocation.getY());
            chestConfig.set("chest.z", chestLocation.getZ());

            try {
                chestConfig.save(chestFile);
            } catch (IOException e) {
                if (debugManager != null) {
                    debugManager.error("character", "Anti-Aging Chest", "Konnte antiaging_chest.yml nicht speichern", e);
                }
            }
        }
    }

    /**
     * Setzt die Position der Anti-Aging-Kiste und füllt sie initial.
     *
     * @param location die neue Location der Kiste
     */
    public void setChestLocation(Location location) {
        this.chestLocation = location;
        saveChestLocation();

        // Stelle sicher, dass die Kiste mit Anti-Aging-Tränken gefüllt ist
        fillChest();
    }

    /**
     * Füllt die registrierte Kiste vollständig mit Anti-Aging-Tränken auf.
     * Prüft vorher, ob an der Position tatsächlich eine Kiste steht.
     */
    public void fillChest() {
        if (chestLocation == null) return;

        Block block = chestLocation.getBlock();
        if (block.getType() != Material.CHEST) {
            if (debugManager != null) {
                debugManager.warning("character", "Anti-Aging Chest", "An der gespeicherten Location befindet sich keine Kiste!");
            }
            return;
        }

        Chest chest = (Chest) block.getState();
        chest.getInventory().clear();

        // Fülle die Kiste mit Anti-Aging-Tränken
        ItemStack potion = createAntiAgingPotion();
        for (int i = 0; i < chest.getInventory().getSize(); i++) {
            chest.getInventory().setItem(i, potion.clone());
        }
    }

    /**
     * Erstellt einen neuen Anti-Aging-Trank als ItemStack.
     *
     * @return der erstellte Trank
     */
    private ItemStack createAntiAgingPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

        potionMeta.setBasePotionData(new PotionData(PotionType.MUNDANE));
        potionMeta.setDisplayName("§b§lAnti-Alterungs-Trank");
        potionMeta.setLore(Arrays.asList(
                "§7Dieser magische Trank verhindert",
                "§7die Alterung für einen Monat.",
                "",
                "§8» §7Rechtsklick zum Trinken"
        ));

        // Füge ein Custom-Tag hinzu, um den Trank zu identifizieren
        potionMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "anti-aging-potion"),
                PersistentDataType.BYTE,
                (byte) 1
        );

        potion.setItemMeta(potionMeta);
        return potion;
    }

    /**
     * Überprüft, ob ein Spieler berechtigt ist, einen Trank aus der Kiste zu nehmen.
     * Prüft auf aktiven Charakter und monatliche Nutzungsgrenze.
     *
     * @param player der Spieler
     */
    public void checkAndGivePotion(Player player) {
        if (chestLocation == null) {
            player.sendMessage(prefix + "§cDie Anti-Aging-Kiste wurde noch nicht eingerichtet!");
            return;
        }

        // Überprüfe, ob der Spieler einen aktiven Charakter hat
        UUID playerUuid = player.getUniqueId();
        String characterName = MultiCharPlugin.getActiveCharacter(playerUuid);
        if (characterName == null) {
            player.sendMessage(prefix + "§cDu hast keinen aktiven Charakter!");
            return;
        }

        int characterId = MultiCharPlugin.getActiveCharacterId(playerUuid);
        if (characterId == -1) {
            player.sendMessage(prefix + "§cFehler beim Abrufen der Charakter-ID!");
            return;
        }

        // Überprüfe, ob der Charakter in diesem Monat bereits einen Trank genommen hat
        checkLastUsage(player, characterId);
    }

    /**
     * Prüft in der Datenbank, wann der Charakter zuletzt einen Trank genommen hat.
     * Wenn im aktuellen Monat noch nicht geschehen, wird ein Trank ausgegeben.
     *
     * @param player      der Spieler
     * @param characterId die ID des Charakters
     */
    private void checkLastUsage(Player player, int characterId) {
        String sql = "SELECT last_used FROM character_antiaging_chest_usage WHERE character_id = ?";

        coreAPI.queryAsync(sql, results -> {
            if (results.isEmpty()) {
                // Noch nie einen Trank genommen, erlaube es
                givePotion(player, characterId);
                return;
            }

            try {
                Map<String, Object> data = results.get(0);
                Object lastUsedObj = data.get("last_used");
                LocalDateTime lastUsed;

                // Korrekte Behandlung des Datenbankrückgabewerts
                if (lastUsedObj instanceof LocalDateTime) {
                    lastUsed = (LocalDateTime) lastUsedObj;
                } else if (lastUsedObj instanceof String) {
                    lastUsed = LocalDateTime.parse((String) lastUsedObj, dateTimeFormatter);
                } else if (lastUsedObj instanceof java.sql.Timestamp) {
                    lastUsed = ((java.sql.Timestamp) lastUsedObj).toLocalDateTime();
                } else {
                    if (debugManager != null) {
                        debugManager.warning("character", "Anti-Aging Chest", "Unerwarteter Datentyp für last_used: " + (lastUsedObj != null ? lastUsedObj.getClass().getName() : "null"));
                    }
                    player.sendMessage(prefix + "§cEs ist ein Fehler aufgetreten. Bitte kontaktiere einen Administrator.");
                    return;
                }

                LocalDateTime now = LocalDateTime.now();

                // Überprüfe, ob der letzte Gebrauch im aktuellen Monat war
                boolean sameMonth = lastUsed.getMonth() == now.getMonth() &&
                        lastUsed.getYear() == now.getYear();

                if (sameMonth) {
                    player.sendMessage(prefix + "§cDu hast bereits in diesem Monat einen Anti-Alterungs-Trank genommen!");
                } else {
                    givePotion(player, characterId);
                }
            } catch (Exception e) {
                if (debugManager != null) {
                    debugManager.error("character", "Anti-Aging Chest", "Fehler beim Überprüfen der letzten Nutzung", e);
                }
                player.sendMessage(prefix + "§cEs ist ein Fehler aufgetreten. Bitte kontaktiere einen Administrator.");
            }
        }, characterId);
    }

    /**
     * Gibt dem Spieler einen Anti-Aging-Trank ins Inventar und speichert den Zeitpunkt der Nutzung.
     *
     * @param player      der Spieler
     * @param characterId die ID des Charakters
     */
    private void givePotion(Player player, int characterId) {
        // Überprüfe, ob im Inventar Platz ist
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(prefix + "§cDein Inventar ist voll! Mache Platz für den Trank.");
            return;
        }

        // Gib dem Spieler einen Trank
        ItemStack potion = createAntiAgingPotion();
        player.getInventory().addItem(potion);
        player.sendMessage(prefix + "§aDu hast einen §bAnti-Alterungs-Trank §aaus der Kiste genommen!");

        // Aktualisiere die Nutzungsdaten in der Datenbank
        String currentTime = LocalDateTime.now().format(dateTimeFormatter);

        String checkSql = "SELECT COUNT(*) AS count FROM character_antiaging_chest_usage WHERE character_id = ?";
        coreAPI.queryAsync(checkSql, results -> {
            if (results.isEmpty()) return;

            int count = ((Number) results.get(0).get("count")).intValue();

            if (count > 0) {
                // Update bestehenden Eintrag
                String updateSql = "UPDATE character_antiaging_chest_usage SET last_used = ? WHERE character_id = ?";
                coreAPI.executeUpdateAsync(updateSql, currentTime, characterId);
            } else {
                // Erstelle neuen Eintrag
                String insertSql = "INSERT INTO character_antiaging_chest_usage (character_id, last_used) VALUES (?, ?)";
                coreAPI.executeUpdateAsync(insertSql, characterId, currentTime);
            }
        }, characterId);
    }

    /**
     * Überprüft, ob die angegebene Location der registrierten Anti-Aging-Kiste entspricht.
     *
     * @param location die zu prüfende Location
     * @return true, wenn es die Anti-Aging-Kiste ist
     */
    public boolean isAntiAgingChest(Location location) {
        return chestLocation != null &&
                location.getWorld().equals(chestLocation.getWorld()) &&
                location.getBlockX() == chestLocation.getBlockX() &&
                location.getBlockY() == chestLocation.getBlockY() &&
                location.getBlockZ() == chestLocation.getBlockZ();
    }
}
