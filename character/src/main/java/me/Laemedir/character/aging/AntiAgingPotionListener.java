package me.Laemedir.character.aging;

import me.Laemedir.character.MultiCharPlugin;
import me.Laemedir.coreApi.debug.DebugManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener für das Trinken von Anti-Aging-Tränken.
 * Erfasst das Rechtsklick-Event, prüft den Trank und leitet den Effekt ein.
 */
public class AntiAgingPotionListener implements Listener {
    private final MultiCharPlugin plugin;
    private final AgingSystem agingSystem;
    private final DebugManager debugManager;
    private final String prefix;
    private final NamespacedKey potionKey;

    public AntiAgingPotionListener(MultiCharPlugin plugin, AgingSystem agingSystem) {
        this.plugin = plugin;
        this.agingSystem = agingSystem;
        this.debugManager = plugin.getDebugManager();
        this.prefix = "§8[§6Characters§8] ";
        this.potionKey = new NamespacedKey(plugin, "anti-aging-potion");
    }

    /**
     * Handhabt die Interaktion mit dem Anti-Aging-Trank.
     *
     * @param event das PlayerInteractEvent
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Überprüfe, ob der Spieler mit Rechtsklick interagiert
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Überprüfe, ob das Item ein Trank ist und das richtige Tag hat
        if (item == null || item.getType() != Material.POTION) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof PotionMeta)) {
            return;
        }

        // Überprüfe, ob es sich um einen Anti-Aging-Trank handelt
        if (!meta.getPersistentDataContainer().has(potionKey, PersistentDataType.BYTE)) {
            return;
        }

        // Verhindere die normale Interaktion
        event.setCancelled(true);

        // Überprüfe, ob der Spieler einen aktiven Charakter hat
        String characterName = MultiCharPlugin.getActiveCharacter(player.getUniqueId());
        if (characterName == null) {
            player.sendMessage(prefix + "§cDu hast keinen aktiven Charakter!");
            return;
        }

        // Hole die Charakter-ID
        int characterId = MultiCharPlugin.getActiveCharacterId(player.getUniqueId());
        if (characterId == -1) {
            player.sendMessage(prefix + "§cFehler beim Abrufen der Charakter-ID!");
            return;
        }

        // Entferne einen Trank aus dem Inventar
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Spiele Trink-Animation und Sound ab
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);

        if (debugManager != null) {
            debugManager.info("character", "Anti-Aging Potion", player.getName() + " (Charakter: " + characterName + ") hat einen Anti-Alterungs-Trank getrunken");
        }

        // Lasse den Charakter den Trank trinken
        agingSystem.drinkAntiAgingPotion(player, characterId);
    }
}
