package me.Laemedir.coreApi.debug;

import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class DebugPluginSelectGUI implements Listener {
    private final CoreAPIPlugin plugin;
    private final DebugManager debugManager;
    private final Player player;
    private final Inventory inventory;

    public DebugPluginSelectGUI(CoreAPIPlugin plugin, DebugManager debugManager, Player player) {
        this.plugin = plugin;
        this.debugManager = debugManager;
        this.player = player;
        this.inventory = Bukkit.createInventory(null, 54, "§6§lPlugin auswählen");
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadPlugins();
    }

    private void loadPlugins() {
        debugManager.getAllPluginNames(plugins -> {
            int slot = 0;
            for (String pluginName : plugins) {
                if (slot >= 45) break;
                
                ItemStack item = new ItemStack(Material.COMMAND_BLOCK);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§e§l" + pluginName);
                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    lore.add("§eKlicken um Logs anzuzeigen");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inventory.setItem(slot++, item);
            }
            
            // Zurück-Button
            ItemStack back = new ItemStack(Material.ARROW);
            ItemMeta backMeta = back.getItemMeta();
            if (backMeta != null) {
                backMeta.setDisplayName("§c§lZurück");
                back.setItemMeta(backMeta);
            }
            inventory.setItem(49, back);
        });
    }

    public void open() {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6§lPlugin auswählen")) {
            return;
        }
        
        event.setCancelled(true);
        
        if (event.getRawSlot() == 49) { // Zurück
            event.getWhoClicked().closeInventory();
            new DebugMainGUI(plugin, debugManager, (Player) event.getWhoClicked()).open();
            return;
        }
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String pluginName = clicked.getItemMeta().getDisplayName().replace("§e§l", "");
            event.getWhoClicked().closeInventory();
            new DebugListGUI(plugin, debugManager, (Player) event.getWhoClicked(), 
                    DebugFilter.byPlugin(pluginName)).open();
        }
    }
}
