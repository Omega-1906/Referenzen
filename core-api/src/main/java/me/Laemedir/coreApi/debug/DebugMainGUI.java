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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DebugMainGUI implements Listener {
    private final CoreAPIPlugin plugin;
    private final DebugManager debugManager;
    private final Player player;
    private final Inventory inventory;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public DebugMainGUI(CoreAPIPlugin plugin, DebugManager debugManager, Player player) {
        this.plugin = plugin;
        this.debugManager = debugManager;
        this.player = player;
        this.inventory = Bukkit.createInventory(null, 54, "§6§lDebug-System");
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        buildGUI();
    }

    private void buildGUI() {
        // Statistiken laden
        debugManager.getStats(stats -> {
            // Statistik-Anzeigen
            setItem(10, Material.BOOK, "§6§lStatistiken", 
                    "§7Gesamt Einträge: §e" + stats.totalEntries,
                    "§7Fehler: §c" + stats.errorCount,
                    "§7Plugins: §b" + stats.pluginCount,
                    "§7Aktive Tage: §a" + stats.activeDays);
        });

        // Neueste Einträge
        setItem(12, Material.CLOCK, "§b§lNeueste Einträge",
                "§7Zeigt die 50 neuesten",
                "§7Debug-Einträge an",
                "",
                "§eKlicken zum Öffnen");

        // Fehler anzeigen
        setItem(14, Material.REDSTONE, "§c§lFehler & Warnungen",
                "§7Zeigt alle Fehler und",
                "§7Warnungen der letzten 7 Tage",
                "",
                "§eKlicken zum Öffnen");

        // Nach Plugin filtern
        setItem(16, Material.COMPASS, "§e§lNach Plugin suchen",
                "§7Filtere Debug-Einträge",
                "§7nach einem bestimmten Plugin",
                "",
                "§eKlicken zum Öffnen");

        // Erweiterte Suche
        setItem(28, Material.SPYGLASS, "§d§lErweiterte Suche",
                "§7Filtere nach mehreren",
                "§7Kriterien gleichzeitig",
                "",
                "§7⚠ Bald verfügbar");

        // Cleanup
        setItem(30, Material.TNT, "§c§lAufräumen",
                "§7Lösche alte Debug-Einträge",
                "§7(älter als 30 Tage)",
                "",
                "§eKlicken zum Ausführen");

        // Export (Geplantes Feature)
        setItem(32, Material.WRITABLE_BOOK, "§a§lExportieren",
                "§7Exportiere Debug-Logs",
                "§7als Datei",
                "",
                "§7⚠ Bald verfügbar");

        // Schließen
        setItem(49, Material.BARRIER, "§c§lSchließen",
                "§7Schließt dieses Menü");

        // Fülle leere Slots mit Glas
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        
        for (int i = 0; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, glass);
            }
        }
    }

    private void setItem(int slot, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(line);
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    public void open() {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6§lDebug-System")) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        switch (slot) {
            case 12: // Neueste Einträge
                clicker.closeInventory();
                new DebugListGUI(plugin, debugManager, clicker, DebugFilter.builder().limit(50)).open();
                break;
            case 14: // Fehler
                clicker.closeInventory();
                new DebugListGUI(plugin, debugManager, clicker, DebugFilter.recentErrors()).open();
                break;
            case 16: // Nach Plugin
                clicker.closeInventory();
                new DebugPluginSelectGUI(plugin, debugManager, clicker).open();
                break;
            case 30: // Cleanup
                clicker.closeInventory();
                debugManager.cleanupOldEntries(30, count -> {
                    clicker.sendMessage("§a[Debug] Alte Einträge wurden gelöscht.");
                });
                break;
            case 49: // Schließen
                clicker.closeInventory();
                break;
        }
    }
}
