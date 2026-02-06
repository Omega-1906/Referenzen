package me.Laemedir.character.aging;

import me.Laemedir.character.MultiCharPlugin;
import me.Laemedir.coreApi.debug.DebugManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener-Klasse für die Anti-Aging-Kiste.
 * Verhindert Zerstörung durch Spieler und regelt den Zugriff.
 */
public class AntiAgingChestListener implements Listener {
    private final MultiCharPlugin plugin;
    private final AntiAgingChestManager chestManager;
    private final DebugManager debugManager;
    private final String prefix;

    public AntiAgingChestListener(MultiCharPlugin plugin, AntiAgingChestManager chestManager) {
        this.plugin = plugin;
        this.chestManager = chestManager;
        this.debugManager = plugin.getDebugManager();
        this.prefix = "§8[§6Characters§8] ";
    }

    /**
     * Verhindert das Zerstören der Anti-Aging-Kiste durch Spieler ohne Berechtigung.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.CHEST && chestManager.isAntiAgingChest(block.getLocation())) {
            Player player = event.getPlayer();

            if (!player.hasPermission("laemedir.admin") && !player.hasPermission("laemedir.loreteam")) {
                event.setCancelled(true);
                player.sendMessage(prefix + "§cDu kannst die Anti-Aging-Kiste nicht zerstören!");
                
                if (debugManager != null) {
                    debugManager.warning("character", "Anti-Aging Chest", player.getName() + " versuchte die Anti-Aging-Kiste zu zerstören");
                }
            } else if (debugManager != null) {
                debugManager.info("character", "Anti-Aging Chest", player.getName() + " hat die Anti-Aging-Kiste zerstört");
            }
        }
    }

    /**
     * Behandelt Interaktionen (Rechtsklick) mit der Kiste.
     * Öffnet die Kiste für Admins (beim Schleichen) oder gibt Tränke aus.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) return;

        if (chestManager.isAntiAgingChest(block.getLocation())) {
            event.setCancelled(true);

            Player player = event.getPlayer();

            // Wenn der Spieler ein Admin ist und sneakt, öffne die Kiste normal
            if ((player.hasPermission("laemedir.admin") || player.hasPermission("laemedir.loreteam"))
                    && player.isSneaking()) {
                return;
            }

            // Sonst prüfe, ob der Spieler einen Trank nehmen darf
            chestManager.checkAndGivePotion(player);
        }
    }

    /**
     * Verhindert, dass Spieler Items manuell aus der Kiste nehmen.
     * Nur Admins haben vollen Zugriff auf das Inventar.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof org.bukkit.block.Chest) {
            org.bukkit.block.Chest chest = (org.bukkit.block.Chest) event.getInventory().getHolder();

            if (chestManager.isAntiAgingChest(chest.getLocation())) {
                Player player = (Player) event.getWhoClicked();

                // Erlaube Admins, die Items zu verschieben
                if (player.hasPermission("laemedir.admin") || player.hasPermission("laemedir.loreteam")) {
                    return;
                }

                event.setCancelled(true);
                player.sendMessage(prefix + "§cDu kannst keine Items aus der Anti-Aging-Kiste nehmen!");
                player.closeInventory();
                
                if (debugManager != null) {
                    debugManager.warning("character", "Anti-Aging Chest", player.getName() + " versuchte Items aus der Anti-Aging-Kiste zu nehmen");
                }
            }
        }
    }

    /**
     * Verhindert das automatische Verschieben von Items (z.B. durch Trichter).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        InventoryHolder holder = event.getSource().getHolder();

        if (holder instanceof org.bukkit.block.Chest) {
            org.bukkit.block.Chest chest = (org.bukkit.block.Chest) holder;

            if (chestManager.isAntiAgingChest(chest.getLocation())) {
                event.setCancelled(true);
                
                if (debugManager != null) {
                    debugManager.warning("character", "Anti-Aging Chest", "Ein Hopper/Trichter versuchte Items aus der Anti-Aging-Kiste zu bewegen");
                }
            }
        }
    }
}
