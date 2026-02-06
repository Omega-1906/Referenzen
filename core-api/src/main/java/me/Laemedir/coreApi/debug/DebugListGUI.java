package me.Laemedir.coreApi.debug;

import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DebugListGUI implements Listener {
    private final CoreAPIPlugin plugin;
    private final DebugManager debugManager;
    private final Player player;
    private final Inventory inventory;
    private final DebugFilter filter;
    private final NamespacedKey entryIdKey;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yy");

    public DebugListGUI(CoreAPIPlugin plugin, DebugManager debugManager, Player player, DebugFilter filter) {
        this.plugin = plugin;
        this.debugManager = debugManager;
        this.player = player;
        this.filter = filter;
        this.inventory = Bukkit.createInventory(null, 54, "§6§lDebug-Einträge");
        this.entryIdKey = new NamespacedKey(plugin, "debug_entry_id");
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadEntries();
    }

    private void loadEntries() {
        debugManager.getEntries(filter, entries -> {
            int slot = 0;
            for (DebugEntry entry : entries) {
                if (slot >= 45) break; // Max 45 Einträge pro Seite
                
                Material material = getMaterialForLevel(entry.getLevel());
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(entry.getLevel().getFormattedName() + " §7[§e" + entry.getPluginName() + "§7]");
                    
                    List<String> lore = new ArrayList<>();
                    lore.add("§7Zeit: §f" + entry.getTimestamp().format(TIME_FORMAT));
                    lore.add("§7Kategorie: §f" + (entry.getCategory() != null ? entry.getCategory() : "Keine"));
                    lore.add("");
                    lore.add("§7Nachricht:");
                    lore.add("§f" + truncate(entry.getMessage(), 40));
                    if (entry.hasPlayer()) {
                        lore.add("");
                        lore.add("§7Spieler: §e" + entry.getPlayerName());
                    }
                    if (entry.hasStackTrace()) {
                        lore.add("");
                        lore.add("§c⚠ Hat Stack-Trace");
                    }
                    lore.add("");
                    lore.add("§eKlicken für Details");
                    
                    meta.setLore(lore);
                    // Speichere die Entry-ID im Item
                    meta.getPersistentDataContainer().set(entryIdKey, PersistentDataType.INTEGER, entry.getId());
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

    private Material getMaterialForLevel(DebugLevel level) {
        return switch (level) {
            case ERROR, CRITICAL -> Material.REDSTONE;
            case WARNING -> Material.GOLD_INGOT;
            case INFO -> Material.PAPER;
            case SUCCESS -> Material.EMERALD;
            case DEBUG -> Material.FEATHER;
        };
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    public void open() {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6§lDebug-Einträge")) {
            return;
        }
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        if (event.getRawSlot() == 49) { // Zurück
            HandlerList.unregisterAll(this);
            event.getWhoClicked().closeInventory();
            new DebugMainGUI(plugin, debugManager, (Player) event.getWhoClicked()).open();
            return;
        }
        
        // Prüfe, ob das Item eine Entry-ID hat
        ItemMeta meta = clicked.getItemMeta();
        if (meta.getPersistentDataContainer().has(entryIdKey, PersistentDataType.INTEGER)) {
            int entryId = meta.getPersistentDataContainer().get(entryIdKey, PersistentDataType.INTEGER);
            
            // Unregister diesen Listener
            HandlerList.unregisterAll(this);
            
            // Öffne Details-GUI
            event.getWhoClicked().closeInventory();
            new DebugDetailGUI(plugin, debugManager, (Player) event.getWhoClicked(), entryId, this).open();
        }
    }
    
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals("§6§lDebug-Einträge")) {
            return;
        }
        if (event.getPlayer().equals(player)) {
            HandlerList.unregisterAll(this);
        }
    }
}
