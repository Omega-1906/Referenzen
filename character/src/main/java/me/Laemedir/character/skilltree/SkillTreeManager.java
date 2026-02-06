package me.Laemedir.character.skilltree;

import me.Laemedir.character.MultiCharPlugin;
import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.util.*;

/**
 * Verwaltet den Skilltree der Charaktere.
 * Speichert Skills in der Datenbank und handhabt das GUI.
 */
public class SkillTreeManager {

    private final CoreAPIPlugin coreAPI;
    private final MultiCharPlugin plugin;
    private static final int MIN_POINTS = 0;
    private static final int MAX_POINTS = 10;
    
    // Keys für PersistentDataContainer
    public final NamespacedKey KEY_SKILL_TYPE;
    public final NamespacedKey KEY_CURRENT_VALUE;
    public final NamespacedKey KEY_AVAILABLE_POINTS;

    // Skill-Typen
    public enum SkillType {
        STRENGTH("Stärke", "💪", Material.NETHERITE_SWORD),
        SPEED("Schnelligkeit", "💨", Material.LEATHER_BOOTS),
        DEXTERITY("Geschicklichkeit", "🎯", Material.BOW),
        CONSTITUTION("Konstitution", "❤", Material.GOLDEN_APPLE);

        private final String name;
        private final String emoji;
        private final Material material;

        SkillType(String name, String emoji, Material material) {
            this.name = name;
            this.emoji = emoji;
            this.material = material;
        }

        public String getName() {
            return name;
        }

        public String getEmoji() {
            return emoji;
        }

        public Material getMaterial() {
            return material;
        }
    }

    public SkillTreeManager(CoreAPIPlugin coreAPI, MultiCharPlugin plugin) {
        this.coreAPI = coreAPI;
        this.plugin = plugin;
        
        // Initialisiere Keys
        this.KEY_SKILL_TYPE = new NamespacedKey(plugin, "skill_type");
        this.KEY_CURRENT_VALUE = new NamespacedKey(plugin, "skill_value");
        this.KEY_AVAILABLE_POINTS = new NamespacedKey(plugin, "available_points");
        
        setupDatabase();
    }

    private void setupDatabase() {
        // Erstelle die Tabelle für Skills, falls sie noch nicht existiert
        String createTableSQL = "CREATE TABLE IF NOT EXISTS character_skills (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "character_id INT NOT NULL, " +
                "strength INT DEFAULT 0, " +
                "speed INT DEFAULT 0, " +
                "dexterity INT DEFAULT 0, " +
                "constitution INT DEFAULT 0, " +
                "available_points INT DEFAULT 0, " +
                "FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE" +
                ")";

        try {
            coreAPI.executeUpdateSync(createTableSQL);
            plugin.getLogger().info("Tabelle 'character_skills' überprüft/erstellt.");
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Erstellen der Tabelle 'character_skills': " + e.getMessage());
        }
    }

    public void openSkillTreeGUI(Player player) {
        String activeChar = MultiCharPlugin.getActiveCharacter(player.getUniqueId());
        if (activeChar == null) {
            player.sendMessage("§8[§6Skills§8] §cDu musst einen aktiven Charakter haben, um den Skilltree zu öffnen.");
            return;
        }

        // Hole Charakter-ID und Skill-Daten
        String getCharIdSQL = "SELECT id FROM characters WHERE player_uuid = ? AND name = ?";
        coreAPI.queryAsync(getCharIdSQL, results -> {
            if (results.isEmpty()) {
                player.sendMessage("§8[§6Skills§8] §cDein Charakter wurde nicht gefunden.");
                return;
            }

            int characterId = ((Number) results.get(0).get("id")).intValue();
            loadSkillData(player, characterId);
        }, player.getUniqueId().toString(), activeChar);
    }

    private void loadSkillData(Player player, int characterId) {
        String getSkillsSQL = "SELECT * FROM character_skills WHERE character_id = ?";
        coreAPI.queryAsync(getSkillsSQL, results -> {
            Map<SkillType, Integer> skillValues = new EnumMap<>(SkillType.class);
            int availablePoints = 0;

            if (results.isEmpty()) {
                // Erstelle neue Skill-Einträge für diesen Charakter
                createNewSkillEntry(characterId);

                // Standardwerte
                for (SkillType type : SkillType.values()) {
                    skillValues.put(type, 0);
                }
            } else {
                Map<String, Object> data = results.get(0);
                skillValues.put(SkillType.STRENGTH, ((Number) data.get("strength")).intValue());
                skillValues.put(SkillType.SPEED, ((Number) data.get("speed")).intValue());
                skillValues.put(SkillType.DEXTERITY, ((Number) data.get("dexterity")).intValue());
                skillValues.put(SkillType.CONSTITUTION, ((Number) data.get("constitution")).intValue());
                availablePoints = ((Number) data.get("available_points")).intValue();
            }

            showSkillGUI(player, characterId, skillValues, availablePoints);
        }, characterId);
    }

    private void createNewSkillEntry(int characterId) {
        String insertSQL = "INSERT INTO character_skills (character_id, strength, speed, dexterity, constitution, available_points) " +
                "VALUES (?, 0, 0, 0, 0, 0)";
        coreAPI.executeUpdateAsync(insertSQL, characterId);
    }

    public void showSkillGUI(Player player, int characterId, Map<SkillType, Integer> skillValues, int availablePoints) {
        // Erstelle ein Inventar mit 3 Reihen (27 Slots)
        Inventory inv = Bukkit.createInventory(null, 27, "§6§l" + player.getName() + " - Skilltree");

        // Fülle das Inventar mit Glasscheiben als Hintergrund
        ItemStack background = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta backgroundMeta = background.getItemMeta();
        backgroundMeta.setDisplayName(" ");
        background.setItemMeta(backgroundMeta);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, background);
        }

        // Verfügbare Punkte anzeigen
        ItemStack pointsItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta pointsMeta = pointsItem.getItemMeta();
        pointsMeta.setDisplayName("§e§lVerfügbare Punkte: §a" + availablePoints);
        List<String> pointsLore = new ArrayList<>();
        pointsLore.add("§7Verteile diese Punkte auf deine Fähigkeiten");
        pointsMeta.setLore(pointsLore);
        
        // Speichere verfügbare Punkte im Item für einfachen Zugriff
        pointsMeta.getPersistentDataContainer().set(KEY_AVAILABLE_POINTS, PersistentDataType.INTEGER, availablePoints);
        
        pointsItem.setItemMeta(pointsMeta);
        inv.setItem(4, pointsItem);

        // Skill-Items erstellen und platzieren
        createSkillItem(inv, SkillType.STRENGTH, skillValues.get(SkillType.STRENGTH), availablePoints, 10);
        createSkillItem(inv, SkillType.SPEED, skillValues.get(SkillType.SPEED), availablePoints, 12);
        createSkillItem(inv, SkillType.DEXTERITY, skillValues.get(SkillType.DEXTERITY), availablePoints, 14);
        createSkillItem(inv, SkillType.CONSTITUTION, skillValues.get(SkillType.CONSTITUTION), availablePoints, 16);

        // Schließen-Button
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName("§c§lSchließen");
        closeItem.setItemMeta(closeMeta);
        inv.setItem(22, closeItem);

        // GUI öffnen
        player.openInventory(inv);

        // Speichere Charakter-ID für den Listener
        player.setMetadata("skilltree_character_id", new org.bukkit.metadata.FixedMetadataValue(plugin, characterId));
    }

    private void createSkillItem(Inventory inv, SkillType type, int currentValue, int availablePoints, int slot) {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§l" + type.getEmoji() + " " + type.getName() + " §7(§e" + currentValue + "§7/§e" + MAX_POINTS + "§7)");

        List<String> lore = new ArrayList<>();
        lore.add("§7Aktueller Wert: §e" + currentValue);

        // Fortschrittsbalken
        StringBuilder progressBar = new StringBuilder("§a");
        for (int i = 0; i < MAX_POINTS; i++) {
            if (i < currentValue) {
                progressBar.append("■");
            } else {
                progressBar.append("§7■");
            }
        }
        lore.add(progressBar.toString());
        lore.add("");

        // Beschreibung basierend auf dem Skill-Typ
        switch (type) {
            case STRENGTH:
                lore.add("§7W.I.P (Work in Progress)");
                break;
            case SPEED:
                lore.add("§7W.I.P (Work in Progress)");
                break;
            case DEXTERITY:
                lore.add("§7W.I.P (Work in Progress)");
                break;
            case CONSTITUTION:
                lore.add("§7W.I.P (Work in Progress)");
                break;
        }

        lore.add("");

        // Aktionshinweise
        if (currentValue < MAX_POINTS && availablePoints > 0) {
            lore.add("§a➡ Linksklick: Punkt hinzufügen");
        } else if (currentValue == MAX_POINTS) {
            lore.add("§c✖ Maximaler Wert erreicht");
        } else if (availablePoints == 0) {
            lore.add("§c✖ Keine Punkte verfügbar");
        }

        if (currentValue > MIN_POINTS) {
            lore.add("§c⬅ Rechtsklick: Punkt entfernen");
        }

        // Speichere Daten im Item
        meta.getPersistentDataContainer().set(KEY_SKILL_TYPE, PersistentDataType.STRING, type.name());
        meta.getPersistentDataContainer().set(KEY_CURRENT_VALUE, PersistentDataType.INTEGER, currentValue);
        meta.getPersistentDataContainer().set(KEY_AVAILABLE_POINTS, PersistentDataType.INTEGER, availablePoints);

        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    /**
     * Fügt einen Skillpunkt hinzu und aktualisiert das GUI.
     */
    public void addSkillPoint(Player player, int characterId, SkillType type, int currentPoints, int availablePoints) {
        if (currentPoints >= MAX_POINTS || availablePoints <= 0) {
            return;
        }

        // Optimistisches Update: Datenbank async, GUI sofort neu laden
        String columnName = type.name().toLowerCase();
        String updateSQL = "UPDATE character_skills SET " + columnName + " = ?, available_points = ? WHERE character_id = ?";

        coreAPI.executeUpdateAsync(updateSQL, currentPoints + 1, availablePoints - 1, characterId);

        // Sound und Effekt für Feedback
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);

        // GUI neu laden (da wir die anderen Werte nicht sicher wissen, laden wir neu - aber async)
        // Um Flackern zu vermeiden, könnten wir auch direkt updaten, aber loadSkillData ist sauberer
        loadSkillData(player, characterId);
    }

    /**
     * Entfernt einen Skillpunkt und aktualisiert das GUI.
     */
    public void removeSkillPoint(Player player, int characterId, SkillType type, int currentPoints, int availablePoints) {
        if (currentPoints <= MIN_POINTS) {
            return;
        }

        String columnName = type.name().toLowerCase();
        String updateSQL = "UPDATE character_skills SET " + columnName + " = ?, available_points = ? WHERE character_id = ?";

        coreAPI.executeUpdateAsync(updateSQL, currentPoints - 1, availablePoints + 1, characterId);

        // Sound und Effekt für Feedback
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);

        loadSkillData(player, characterId);
    }

    /**
     * @deprecated Sync DB call vermeiden! Verwende loadSkillData.
     */
    @Deprecated
    public int getSkillValue(int characterId, SkillType type) {
        // ... (legacy implementation)
        return 0; // Placeholder, da wir das nicht mehr nutzen wollen
    }

    public int getAvailablePoints(int characterId) {
        String getPointsSQL = "SELECT available_points FROM character_skills WHERE character_id = ?";
        try {
            return coreAPI.querySync(getPointsSQL, rs -> {
                try {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return 0;
            }, characterId);
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Laden der verfügbaren Punkte: " + e.getMessage());
            return 0;
        }
    }
}
