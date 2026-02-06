package me.Laemedir.character.disguise;

import me.Laemedir.character.MultiCharPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Erstellt und verwaltet das Inventar-GUI für die Auswahl von Verwandlungen.
 */
public class DisguiseGUI {

    private final MultiCharPlugin plugin;
    private final DisguiseManager disguiseManager;

    public DisguiseGUI(MultiCharPlugin plugin, DisguiseManager disguiseManager) {
        this.plugin = plugin;
        this.disguiseManager = disguiseManager;
    }

    /**
     * Öffnet das Verwandlungs-GUI für einen Spieler.
     *
     * @param player    der Spieler
     * @param disguises die Liste der verfügbaren Verwandlungen (format "id:size:subtype")
     */
    public void openGUI(Player player, String[] disguises) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8§l▪ §6§lVerwandlungen §8§l▪");

        // Dekorative Rahmen
        fillBorders(inv);

        // Info Item
        inv.setItem(4, createInfoItem(player));

        // Verwandlungen hinzufügen
        int slot = 10;
        for (String disguise : disguises) {
            if (slot >= 44) break;

            // Überspringe Rahmen-Slots
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot += 2;
            }

            String[] parts = disguise.split(":");
            String disguiseId = parts[0];
            String disguiseSize = parts.length > 1 ? parts[1] : "normal";
            String subType = parts.length > 2 ? parts[2] : getDefaultSubType(disguiseId);

            ItemStack item = createDisguiseItem(disguiseId, disguiseSize, subType);
            inv.setItem(slot, item);
            slot++;
        }

        // Verwandlung entfernen Button
        if (disguiseManager.hasActiveDisguise(player)) {
            inv.setItem(49, createRemoveDisguiseItem());
        }

        player.openInventory(inv);
    }

    /**
     * Füllt die Ränder des Inventars mit Glas.
     */
    private void fillBorders(Inventory inv) {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName("§7");
            border.setItemMeta(borderMeta);
        }

        // Obere und untere Reihe
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(i + 45, border);
        }

        // Seitliche Ränder
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }
    }

    /**
     * Erstellt das Info-Item (Spielerkopf).
     */
    private ItemStack createInfoItem(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6§l✦ Verwandlungs-Info §6§l✦");

            String currentDisguise = disguiseManager.hasActiveDisguise(player) ?
                    "§a" + disguiseManager.getCurrentDisguise(player) : "§cKeine";

            List<String> lore = Arrays.asList(
                    "§8§m                    ",
                    "§7Aktuelle Verwandlung:",
                    "§f" + currentDisguise,
                    "",
                    "§7Wähle eine Verwandlung aus,",
                    "§7um dich zu verwandeln!",
                    "§8§m                    "
            );
            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Erstellt das Item zum Entfernen der Verwandlung.
     */
    private ItemStack createRemoveDisguiseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§c§l✖ Verwandlung entfernen");

            List<String> lore = Arrays.asList(
                    "§8§m                    ",
                    "§7Klicke hier, um deine",
                    "§7aktuelle Verwandlung zu",
                    "§7entfernen und wieder",
                    "§7normal auszusehen.",
                    "§8§m                    "
            );
            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Erstellt ein Item für eine bestimmte Verwandlung.
     */
    private ItemStack createDisguiseItem(String disguiseId, String size, String subType) {
        Material material = getDisguiseMaterial(disguiseId);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String displayName = getDisguiseDisplayName(disguiseId);
            meta.setDisplayName("§e§l" + displayName);

            String sizeDisplay = getSizeDisplay(size);
            String sizeColor = getSizeColor(size);

            // Dynamische Lore-Erstellung
            List<String> lore = new ArrayList<>();
            lore.add("§8§m                    ");
            lore.add("§7Größe: " + sizeColor + "§l" + sizeDisplay);

            // SubType/Variante hinzufügen falls vorhanden
            if (subType != null && !subType.isEmpty()) {
                String subTypeDisplay = getSubTypeDisplay(disguiseId, subType);
                String subTypeLabel = getSubTypeLabel(disguiseId);
                lore.add("§7" + subTypeLabel + ": §f" + subTypeDisplay);
            }

            lore.add("");
            lore.add("§7Verwandle dich in " + getArticle(disguiseId) + " §f" + displayName + "§7!");
            lore.add("");
            lore.add("§a§l▶ Klicke zum Verwandeln!");
            lore.add("§8§m                    ");

            meta.setLore(lore);

            // Glowing Effekt für besondere Größen oder SubTypes
            if (!size.equals("normal") || (subType != null && !subType.isEmpty())) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Gibt den Standard-SubType für bestimmte Disguises zurück.
     */
    private String getDefaultSubType(String disguiseId) {
        switch (disguiseId.toLowerCase()) {
            case "wolf":
                return "pale";
            case "cat":
                return "tabby";
            case "horse":
                return "white";
            case "parrot":
                return "red";
            case "llama":
                return "creamy";
            case "rabbit":
                return "brown";
            case "fox":
                return "red";
            default:
                return null;
        }
    }

    /**
     * Gibt das passende Label für den SubType zurück (z.B. "Variante" oder "Farbe").
     */
    private String getSubTypeLabel(String disguiseId) {
        switch (disguiseId.toLowerCase()) {
            case "wolf":
                return "Variante";
            case "cat":
                return "Typ";
            case "horse":
            case "parrot":
            case "llama":
                return "Farbe";
            case "rabbit":
            case "fox":
                return "Typ";
            default:
                return "Variante";
        }
    }

    /**
     * Konvertiert SubType-IDs in deutsche Anzeigenamen.
     */
    private String getSubTypeDisplay(String disguiseId, String subType) {
        if (subType == null) return "Standard";

        switch (disguiseId.toLowerCase()) {
            case "wolf":
                return getWolfVariantDisplay(subType);
            case "cat":
                return getCatTypeDisplay(subType);
            case "horse":
                return getHorseColorDisplay(subType);
            case "parrot":
                return getParrotColorDisplay(subType);
            case "llama":
                return getLlamaColorDisplay(subType);
            case "rabbit":
                return getRabbitTypeDisplay(subType);
            case "fox":
                return getFoxTypeDisplay(subType);
            default:
                return capitalizeFirst(subType);
        }
    }

    private String getWolfVariantDisplay(String variant) {
        return capitalizeFirst(variant);
    }

    private String getCatTypeDisplay(String type) {
        return capitalizeFirst(type);
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private String getHorseColorDisplay(String color) {
        switch (color.toLowerCase()) {
            case "white": return "Weiß";
            case "creamy": return "Cremig";
            case "chestnut": return "Kastanie";
            case "brown": return "Braun";
            case "black": return "Schwarz";
            case "gray": return "Grau";
            case "dark_brown": return "Dunkelbraun";
            default: return capitalizeFirst(color);
        }
    }

    private String getParrotColorDisplay(String color) {
        switch (color.toLowerCase()) {
            case "red": return "Rot";
            case "blue": return "Blau";
            case "green": return "Grün";
            case "cyan": return "Cyan";
            case "gray": return "Grau";
            default: return capitalizeFirst(color);
        }
    }

    private String getLlamaColorDisplay(String color) {
        switch (color.toLowerCase()) {
            case "creamy": return "Cremig";
            case "white": return "Weiß";
            case "brown": return "Braun";
            case "gray": return "Grau";
            default: return capitalizeFirst(color);
        }
    }

    private String getRabbitTypeDisplay(String type) {
        switch (type.toLowerCase()) {
            case "brown": return "Braun";
            case "white": return "Weiß";
            case "black": return "Schwarz";
            case "black_and_white": return "Schwarz-Weiß";
            case "gold": return "Gold";
            case "salt_and_pepper": return "Salz & Pfeffer";
            case "the_killer_bunny": return "Killer-Hase";
            default: return capitalizeFirst(type);
        }
    }

    private String getFoxTypeDisplay(String type) {
        switch (type.toLowerCase()) {
            case "red": return "Rot";
            case "snow": return "Schnee";
            default: return capitalizeFirst(type);
        }
    }

    private Material getDisguiseMaterial(String disguiseId) {
        switch (disguiseId.toLowerCase()) {
            case "cow": return Material.BEEF;
            case "pig": return Material.PORKCHOP;
            case "chicken": return Material.CHICKEN;
            case "sheep": return Material.WHITE_WOOL;
            case "wolf": return Material.BONE;
            case "cat": return Material.COD;
            case "horse": return Material.SADDLE;
            case "villager": return Material.EMERALD;
            case "zombie": return Material.ROTTEN_FLESH;
            case "skeleton": return Material.BONE;
            case "creeper": return Material.GUNPOWDER;
            case "spider": return Material.SPIDER_EYE;
            case "enderman": return Material.ENDER_PEARL;
            case "blaze": return Material.BLAZE_ROD;
            case "ghast": return Material.GHAST_TEAR;
            case "slime": return Material.SLIME_BALL;
            case "magma_cube": return Material.MAGMA_CREAM;
            case "bat": return Material.FEATHER;
            case "squid": return Material.INK_SAC;
            case "rabbit": return Material.RABBIT;
            case "armor_stand": return Material.ARMOR_STAND;
            case "item_frame": return Material.ITEM_FRAME;
            case "painting": return Material.PAINTING;
            case "fox": return Material.SWEET_BERRIES;
            case "bee": return Material.HONEYCOMB;
            case "turtle": return Material.TURTLE_EGG;
            case "panda": return Material.BAMBOO;
            case "llama": return Material.WHITE_CARPET;
            case "dolphin": return Material.TROPICAL_FISH;
            case "parrot": return Material.FEATHER;
            case "ocelot": return Material.TROPICAL_FISH;
            default: return Material.BARRIER;
        }
    }

    private String getDisguiseDisplayName(String disguiseId) {
        switch (disguiseId.toLowerCase()) {
            case "cow": return "Kuh";
            case "pig": return "Schwein";
            case "chicken": return "Huhn";
            case "sheep": return "Schaf";
            case "wolf": return "Wolf";
            case "cat": return "Katze";
            case "horse": return "Pferd";
            case "villager": return "Dorfbewohner";
            case "zombie": return "Zombie";
            case "skeleton": return "Skelett";
            case "creeper": return "Creeper";
            case "spider": return "Spinne";
            case "enderman": return "Enderman";
            case "blaze": return "Blaze";
            case "ghast": return "Ghast";
            case "slime": return "Schleim";
            case "magma_cube": return "Magmawürfel";
            case "bat": return "Fledermaus";
            case "squid": return "Tintenfisch";
            case "rabbit": return "Hase";
            case "armor_stand": return "Rüstungsständer";
            case "item_frame": return "Rahmen";
            case "painting": return "Gemälde";
            case "fox": return "Fuchs";
            case "bee": return "Biene";
            case "turtle": return "Schildkröte";
            case "panda": return "Panda";
            case "llama": return "Lama";
            case "dolphin": return "Delfin";
            case "parrot": return "Papagei";
            case "ozelot": return "Ozelot";
            default: return disguiseId;
        }
    }

    private String getArticle(String disguiseId) {
        switch (disguiseId.toLowerCase()) {
            case "cow": return "eine";
            case "pig": return "ein";
            case "chicken": return "ein";
            case "sheep": return "ein";
            case "wolf": return "einen";
            case "cat": return "eine";
            case "horse": return "ein";
            case "villager": return "einen";
            case "zombie": return "einen";
            case "skeleton": return "ein";
            case "creeper": return "einen";
            case "spider": return "eine";
            case "enderman": return "einen";
            case "blaze": return "einen";
            case "ghast": return "einen";
            case "slime": return "einen";
            case "magma_cube": return "einen";
            case "bat": return "eine";
            case "squid": return "einen";
            case "rabbit": return "einen";
            case "armor_stand": return "einen";
            case "item_frame": return "einen";
            case "painting": return "ein";
            case "fox": return "einen";
            case "bee": return "eine";
            case "turtle": return "eine";
            case "panda": return "einen";
            case "llama": return "ein";
            case "dolphin": return "einen";
            case "parrot": return "einen";
            case "ozelot": return "einen";
            default: return "ein";
        }
    }

    private String getSizeDisplay(String size) {
        switch (size.toLowerCase()) {
            case "small": return "Klein";
            case "big": return "Groß";
            case "normal":
            default: return "Normal";
        }
    }

    private String getSizeColor(String size) {
        switch (size.toLowerCase()) {
            case "small": return "§9";      // Dunkelblau
            case "big": return "§4";        // Dunkelrot
            case "normal":
            default: return "§2";           // Dunkelgrün
        }
    }
}
