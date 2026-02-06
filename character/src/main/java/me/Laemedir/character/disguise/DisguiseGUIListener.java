package me.Laemedir.character.disguise;

import me.Laemedir.character.MultiCharPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Listener für Interaktionen im Verwandlungs-GUI.
 * Behandelt Klicks auf Verwandlungs-Items und das Entfernen von Verwandlungen.
 */
public class DisguiseGUIListener implements Listener {

    private final MultiCharPlugin plugin;
    private final DisguiseManager disguiseManager;

    public DisguiseGUIListener(MultiCharPlugin plugin, DisguiseManager disguiseManager) {
        this.plugin = plugin;
        this.disguiseManager = disguiseManager;
    }

    /**
     * Behandelt Klicks im Inventar.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        if (!event.getView().getTitle().equals("§8§l▪ §6§lVerwandlungen §8§l▪")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String displayName = meta.getDisplayName();

        // Verwandlung entfernen
        if (displayName.equals("§c§l✖ Verwandlung entfernen")) {
            player.closeInventory();
            disguiseManager.removeDisguise(player);
            player.sendMessage("§a§l✓ §aDeine Verwandlung wurde entfernt!");
            return;
        }

        // Info Item - ignorieren
        if (displayName.contains("Verwandlungs-Info")) {
            return;
        }

        // Verwandlungs-Items
        if (displayName.startsWith("§e§l")) {
            String disguiseId = getDisguiseIdFromDisplayName(displayName);

            // Extrahiere Größe aus der Lore
            String size = "normal";
            if (meta.hasLore() && meta.getLore().size() > 1) {
                String sizeLine = meta.getLore().get(1);
                if (sizeLine.contains("§b§lKlein")) size = "small";
                else if (sizeLine.contains("§c§lGroß")) size = "big";
            }

            // Extrahiere SubType aus der Lore (falls vorhanden)
            String subType = extractSubTypeFromLore(meta, disguiseId);

            player.closeInventory();

            // Verwandle den Spieler mit SubType
            disguiseManager.applyDisguise(player, disguiseId, size, subType);

            String article = getArticle(disguiseId);
            String disguiseName = getDisguiseDisplayName(disguiseId);
            String sizeColor = getSizeColor(size);
            String sizeDisplay = getSizeDisplay(size);

            // player.sendMessage("§a§l✓ §aDu hast dich in " + article + " " + sizeColor + "§l" + sizeDisplay + " §6§l" + disguiseName + " §averwandelt!");
        }
    }

    /**
     * Extrahiert SubType aus der Item-Lore (für zukünftige Erweiterungen).
     */
    private String extractSubTypeFromLore(ItemMeta meta, String disguiseId) {
        if (!meta.hasLore()) return null;

        // Beispiel für Wolf-Varianten oder Cat-Typen in der Lore
        for (String line : meta.getLore()) {
            if (line.contains("Variante:") || line.contains("Typ:")) {
                // Extrahiere den SubType nach dem ":"
                String[] parts = line.split(":");
                if (parts.length > 1) {
                    return parts[1].trim().replaceAll("§[0-9a-fk-or]", ""); // Entferne Farbcodes
                }
            }
        }

        // Standard SubTypes für bestimmte Disguises
        switch (disguiseId.toLowerCase()) {
            case "wolf":
                return "pale"; // Standard Wolf-Variante
            case "cat":
                return "tabby"; // Standard Cat-Typ
            default:
                return null;
        }
    }

    private String getDisguiseIdFromDisplayName(String displayName) {
        String cleanName = displayName.replace("§e§l", "");

        switch (cleanName) {
            case "Kuh": return "cow";
            case "Schwein": return "pig";
            case "Huhn": return "chicken";
            case "Schaf": return "sheep";
            case "Wolf": return "wolf";
            case "Katze": return "cat";
            case "Pferd": return "horse";
            case "Dorfbewohner": return "villager";
            case "Zombie": return "zombie";
            case "Skelett": return "skeleton";
            case "Creeper": return "creeper";
            case "Spinne": return "spider";
            case "Enderman": return "enderman";
            case "Blaze": return "blaze";
            case "Ghast": return "ghast";
            case "Schleim": return "slime";
            case "Magmawürfel": return "magma_cube";
            case "Fledermaus": return "bat";
            case "Tintenfisch": return "squid";
            case "Hase": return "rabbit";
            case "Rüstungsständer": return "armor_stand";
            case "Rahmen": return "item_frame";
            case "Gemälde": return "painting";
            case "Fuchs": return "fox";
            case "Biene": return "bee";
            case "Schildkröte": return "turtle";
            case "Panda": return "panda";
            case "Lama": return "llama";
            case "Delfin": return "dolphin";
            case "Papagei": return "parrot";
            case "Ozelot": return "ocelot";
            default: return cleanName.toLowerCase();
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
