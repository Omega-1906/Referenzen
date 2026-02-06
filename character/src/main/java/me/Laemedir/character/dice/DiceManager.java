package me.Laemedir.character.dice;

import me.Laemedir.character.MultiCharPlugin;
import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet das Würfelsystem, einschließlich Rassenattribute, Rüstungsklassen und Item-Modifikatoren.
 */
public class DiceManager {

    private final MultiCharPlugin plugin;
    private final CoreAPIPlugin coreAPI;

    // Cache für Waffen-Modifikatoren
    private final Map<Material, ItemModifier> weaponModifiers = new HashMap<>();

    // Cache für Rüstungswerte
    private final Map<Material, Integer> armorValues = new HashMap<>();

    // Cache für Rassenattribute
    private final Map<Integer, Map<String, Integer>> raceAttributes = new HashMap<>();

    public DiceManager(MultiCharPlugin plugin) {
        this.plugin = plugin;
        this.coreAPI = CoreAPIPlugin.getPlugin(CoreAPIPlugin.class);
        initializeModifiers();
        initializeRaceAttributes();
    }

    private void initializeModifiers() {
        // Waffen-Modifikatoren initialisieren
        weaponModifiers.put(Material.IRON_SWORD, new ItemModifier("Eisenschwert", 2, "Stärke"));
        weaponModifiers.put(Material.WOODEN_SWORD, new ItemModifier("Holzschwert", 1, "Stärke"));
        // Weitere Waffen hier hinzufügen

        // Rüstungswerte initialisieren
        // Lederrüstung
        armorValues.put(Material.LEATHER_HELMET, 1);
        armorValues.put(Material.LEATHER_CHESTPLATE, 2);
        armorValues.put(Material.LEATHER_LEGGINGS, 1);
        armorValues.put(Material.LEATHER_BOOTS, 1);

        // Eisenrüstung
        armorValues.put(Material.IRON_HELMET, 2);
        armorValues.put(Material.IRON_CHESTPLATE, 3);
        armorValues.put(Material.IRON_LEGGINGS, 2);
        armorValues.put(Material.IRON_BOOTS, 1);
    }

    private void initializeRaceAttributes() {
        // Nir (Menschen) - ID: 8
        Map<String, Integer> nirAttributes = new HashMap<>();
        nirAttributes.put("Konstitution", 1);
        raceAttributes.put(8, nirAttributes);

        // Menir (Tiermenschen) - ID: 16
        Map<String, Integer> menirAttributes = new HashMap<>();
        menirAttributes.put("Schnelligkeit", 1);
        menirAttributes.put("Geschicklichkeit", 1);
        menirAttributes.put("Konstitution", -2);
        raceAttributes.put(16, menirAttributes);

        // Mynir (Wassermenschen) - ID: 17
        Map<String, Integer> mynirAttributes = new HashMap<>();
        mynirAttributes.put("Schnelligkeit", 1);
        mynirAttributes.put("Konstitution", -1);
        raceAttributes.put(17, mynirAttributes);

        // Yar (Hochelfen) - ID: 18
        Map<String, Integer> yarAttributes = new HashMap<>();
        yarAttributes.put("Konstitution", 2);
        yarAttributes.put("Stärke", -1);
        raceAttributes.put(18, yarAttributes);

        // Fyryar (Mondelfen) - ID: 19
        Map<String, Integer> fyryarAttributes = new HashMap<>();
        fyryarAttributes.put("Geschicklichkeit", 2);
        fyryarAttributes.put("Stärke", -1);
        raceAttributes.put(19, fyryarAttributes);

        // Retyar (Sonnenelfen) - ID: 20
        Map<String, Integer> retyarAttributes = new HashMap<>();
        retyarAttributes.put("Schnelligkeit", 1);
        retyarAttributes.put("Konstitution", 1);
        retyarAttributes.put("Geschicklichkeit", -2);
        raceAttributes.put(20, retyarAttributes);

        // Orn (Oni) - ID: 21
        Map<String, Integer> ornAttributes = new HashMap<>();
        ornAttributes.put("Schnelligkeit", -2);
        ornAttributes.put("Stärke", 1);
        ornAttributes.put("Konstitution", 1);
        raceAttributes.put(21, ornAttributes);

        // Nalorn (Zwielichtorks) - ID: 22
        Map<String, Integer> nalornAttributes = new HashMap<>();
        nalornAttributes.put("Stärke", 2);
        nalornAttributes.put("Geschicklichkeit", -1);
        raceAttributes.put(22, nalornAttributes);

        // Lytorn (Elementargoliath) - ID: 23
        Map<String, Integer> lytornAttributes = new HashMap<>();
        lytornAttributes.put("Stärke", 1);
        lytornAttributes.put("Konstitution", 1);
        lytornAttributes.put("Schnelligkeit", -2);
        raceAttributes.put(23, lytornAttributes);

        // Eth (Kristallzwerge) - ID: 24
        Map<String, Integer> ethAttributes = new HashMap<>();
        ethAttributes.put("Konstitution", 2);
        ethAttributes.put("Schnelligkeit", -1);
        raceAttributes.put(24, ethAttributes);

        // Maileth (Pflanzengolem) - ID: 25
        Map<String, Integer> mailethAttributes = new HashMap<>();
        mailethAttributes.put("Geschicklichkeit", 1);
        mailethAttributes.put("Konstitution", 1);
        mailethAttributes.put("Schnelligkeit", -2);
        raceAttributes.put(25, mailethAttributes);

        // Valeth (Steingolem) - ID: 26
        Map<String, Integer> valethAttributes = new HashMap<>();
        valethAttributes.put("Konstitution", 2);
        valethAttributes.put("Geschicklichkeit", -1);
        raceAttributes.put(26, valethAttributes);
    }

    /**
     * Gibt die Rasse-ID eines Charakters zurück (Synchron).
     * @deprecated Nutzen Sie {@link #getCharacterRaceIdAsync(int, java.util.function.Consumer)}
     */
    @Deprecated
    public int getCharacterRaceId(int characterId) throws SQLException {
        String query = "SELECT race_id FROM s1_Laemedir.characters WHERE id = ?";
        return coreAPI.querySync(query, rs -> {
            try {
                if (rs.next()) {
                    return rs.getInt("race_id");
                }
                return -1;
            } catch (SQLException e) {
                plugin.getLogger().severe("Fehler beim Abrufen der Rasse-ID: " + e.getMessage());
                return -1;
            }
        }, characterId);
    }

    /**
     * Gibt die Rasse-ID eines Charakters asynchron zurück.
     *
     * @param characterId die ID des Charakters
     * @param callback    der Callback mit der Rassen-ID (oder -1 bei Fehler)
     */
    public void getCharacterRaceIdAsync(int characterId, java.util.function.Consumer<Integer> callback) {
        String query = "SELECT race_id FROM s1_Laemedir.characters WHERE id = ?";
        coreAPI.queryAsync(query, rs -> {
            if (rs.isEmpty()) {
                callback.accept(-1);
                return;
            }
            try {
                Map<String, Object> row = rs.get(0);
                callback.accept((Integer) row.get("race_id"));
            } catch (Exception e) {
                plugin.getLogger().severe("Fehler beim asynchronen Abrufen der Rasse-ID: " + e.getMessage());
                callback.accept(-1);
            }
        }, characterId);
    }

    /**
     * Gibt den Attributsmodifikator für eine bestimmte Rasse zurück
     */
    public int getRaceAttributeModifier(int raceId, String attribute) {
        Map<String, Integer> attributes = raceAttributes.get(raceId);
        if (attributes == null) {
            return 0;
        }

        return attributes.getOrDefault(attribute, 0);
    }

    /**
     * Gibt den Attributsmodifikator für einen Charakter zurück (Synchron).
     * @deprecated Nutzen Sie {@link #getAttributeModifierAsync(int, String, java.util.function.Consumer)}
     */
    @Deprecated
    public int getAttributeModifier(int characterId, String attribute) throws SQLException {
        int raceId = getCharacterRaceId(characterId);
        if (raceId == -1) {
            return 0;
        }

        return getRaceAttributeModifier(raceId, attribute);
    }

    /**
     * Gibt den Attributsmodifikator für einen Charakter asynchron zurück.
     *
     * @param characterId die ID des Charakters
     * @param attribute   das gewünschte Attribut
     * @param callback    der Callback mit dem Modifikator-Wert
     */
    public void getAttributeModifierAsync(int characterId, String attribute, java.util.function.Consumer<Integer> callback) {
        getCharacterRaceIdAsync(characterId, raceId -> {
            if (raceId == -1) {
                callback.accept(0);
                return;
            }
            callback.accept(getRaceAttributeModifier(raceId, attribute));
        });
    }

    /**
     * Gibt den Modifikator für ein ausgerüstetes Item zurück
     */
    public ItemModifier getEquippedItemModifier(Player player, String attribute) {
        PlayerInventory inventory = player.getInventory();
        ItemStack mainHand = inventory.getItemInMainHand();

        if (mainHand != null && !mainHand.getType().isAir()) {
            ItemModifier modifier = weaponModifiers.get(mainHand.getType());
            if (modifier != null && modifier.getAttribute().equals(attribute)) {
                return modifier;
            }
        }

        return null;
    }

    /**
     * Berechnet die Rüstungsklasse eines Spielers
     */
    public int calculateArmorClass(Player player) {
        int baseAC = 10;
        PlayerInventory inventory = player.getInventory();

        ItemStack helmet = inventory.getHelmet();
        ItemStack chestplate = inventory.getChestplate();
        ItemStack leggings = inventory.getLeggings();
        ItemStack boots = inventory.getBoots();

        int totalArmor = 0;

        if (helmet != null && !helmet.getType().isAir()) {
            totalArmor += armorValues.getOrDefault(helmet.getType(), 0);
        }

        if (chestplate != null && !chestplate.getType().isAir()) {
            totalArmor += armorValues.getOrDefault(chestplate.getType(), 0);
        }

        if (leggings != null && !leggings.getType().isAir()) {
            totalArmor += armorValues.getOrDefault(leggings.getType(), 0);
        }

        if (boots != null && !boots.getType().isAir()) {
            totalArmor += armorValues.getOrDefault(boots.getType(), 0);
        }

        return baseAC + totalArmor;
    }

    /**
     * Prüft, ob ein Angriff trifft
     */
    public boolean checkHit(int attackRoll, int armorClass) {
        return attackRoll >= armorClass;
    }

    /**
     * Gibt den Namen einer Rasse zurück
     */
    public String getRaceName(int raceId) throws SQLException {
        String query = "SELECT name FROM s1_Laemedir.races WHERE id = ?";  // ✅ Vollständiger Tabellenname und richtige Spalte

        return coreAPI.querySync(query, rs -> {
            try {
                if (rs.next()) {
                    return rs.getString("name");  // ✅ Spaltenname je nach Datenbankschema
                }
                return "Unbekannt";
            } catch (SQLException e) {
                plugin.getLogger().severe("Fehler beim Abrufen des Rassennamens: " + e.getMessage());
                return "Unbekannt";
            }
        }, raceId);
    }
}
