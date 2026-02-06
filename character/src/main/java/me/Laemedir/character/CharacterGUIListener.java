package me.Laemedir.character;

import me.Laemedir.coreApi.CoreAPIPlugin;
import me.Laemedir.coreApi.debug.DebugManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener für das Charakter-Auswahl-GUI beim Joinen.
 * Verhindert das Schließen des GUIs ohne Auswahl und verarbeitet Klicks.
 */
public class CharacterGUIListener implements Listener {

    private final MultiCharPlugin plugin;
    private final CoreAPIPlugin coreAPI;
    private final DebugManager debugManager;
    private final String prefix = "§8[§6Characters§8] ";
    private static final String CHARACTER_SELECTION_TITLE = "§6§lCharakterauswahl";

    public CharacterGUIListener(MultiCharPlugin plugin, CoreAPIPlugin coreAPI) {
        this.plugin = plugin;
        this.coreAPI = coreAPI;
        this.debugManager = plugin.getDebugManager();
    }

    /**
     * Behandelt Klicks im Auswahl-GUI
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(CHARACTER_SELECTION_TITLE)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        String displayName = clickedItem.getItemMeta().getDisplayName();
        Player player = (Player) event.getWhoClicked();

        // Prüfe, ob der "Server verlassen" Button geklickt wurde
        if (displayName.equals("§cServer verlassen")) {
            player.kickPlayer("§cDu hast den Server verlassen.");
            return;
        }

        // Entferne Farbcodes
        String selectedName = ChatColor.stripColor(displayName);

        // Entferne sowohl "[Aktiv]" als auch "[Deaktiviert]" aus dem Namen
        selectedName = selectedName.replaceAll("\\s*\\[(Aktiv|Deaktiviert|Gesperrt)\\]\\s*", "").trim();

        // Debug-Ausgabe hinzufügen
        if (debugManager != null) {
            debugManager.info("character", "Character Selection", "Charakter ausgewählt (bereinigt): '" + selectedName + "' von Spieler " + player.getName());
        }

        String activeName = plugin.getActiveCharacter(player.getUniqueId());
        if (activeName != null && activeName.equals(selectedName)) {
            player.sendMessage(prefix + "§cDieser Charakter ist bereits aktiv!");
            return;
        }

        plugin.saveActiveCharacter(player);
        String finalSelectedName = selectedName;
        // Kurze Verzögerung für GUI-Update/Feedback
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.selectCharacterFromGUI(player, finalSelectedName), 1L);
    }

    /**
     * Verhindert das Schließen des GUIs, wenn noch kein Charakter gewählt wurde.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        // Wenn es sich um das Charakterauswahl-GUI handelt und der Spieler in der Charakterauswahl ist
        if (title.equals(CHARACTER_SELECTION_TITLE) && plugin.isPlayerInCharacterSelection(player.getUniqueId())) {
            // GUI nach einem Tick wieder öffnen, es sei denn, der Spieler hat inzwischen einen Charakter ausgewählt
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && plugin.isPlayerInCharacterSelection(player.getUniqueId()) &&
                        MultiCharPlugin.getActiveCharacter(player.getUniqueId()) == null) {
                    plugin.openCharacterSelectionGUI(player);
                }
            }, 1L);
        }
    }
}

