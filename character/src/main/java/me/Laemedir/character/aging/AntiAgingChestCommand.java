package me.Laemedir.character.aging;

import me.Laemedir.character.MultiCharPlugin;
import me.Laemedir.coreApi.debug.DebugManager;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * CommandExecutor für die Verwaltung der Anti-Aging-Kiste (Setzen und Füllen).
 */
public class AntiAgingChestCommand implements CommandExecutor {
    private final MultiCharPlugin plugin;
    private final AntiAgingChestManager chestManager;
    private final DebugManager debugManager;
    private final String prefix;

    public AntiAgingChestCommand(MultiCharPlugin plugin, AntiAgingChestManager chestManager) {
        this.plugin = plugin;
        this.chestManager = chestManager;
        this.debugManager = plugin.getDebugManager();
        this.prefix = "§8[§6Characters§8] ";
    }

    /**
     * Führt den Befehl zur Verwaltung der Anti-Aging-Kiste aus.
     *
     * @param sender  der Absender des Befehls
     * @param command der ausgeführte Befehl
     * @param label   das Label des Befehls
     * @param args    die Argumente (set/fill)
     * @return true, wenn der Befehl erfolgreich war
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + "§cDieser Befehl kann nur von Spielern verwendet werden!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("laemedir.admin") && !player.hasPermission("laemedir.loreteam")) {
            player.sendMessage(prefix + "§cDu hast keine Berechtigung, diesen Befehl zu verwenden!");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(prefix + "§cVerwendung: /antiagingchest <set|fill>");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            // Setze die Kiste an der aktuellen Position
            Block targetBlock = player.getTargetBlock(null, 5);

            if (targetBlock == null || targetBlock.getType() != org.bukkit.Material.CHEST) {
                player.sendMessage(prefix + "§cDu musst auf eine Kiste schauen!");
                if (debugManager != null) {
                    debugManager.warning("character", "Anti-Aging Chest", player.getName() + " versuchte Anti-Aging-Kiste zu setzen, schaute aber nicht auf eine Kiste");
                }
                return true;
            }

            chestManager.setChestLocation(targetBlock.getLocation());
            player.sendMessage(prefix + "§aAnti-Aging-Kiste wurde erfolgreich gesetzt und gefüllt!");
            
            if (debugManager != null) {
                debugManager.info("character", "Anti-Aging Chest", player.getName() + " hat die Anti-Aging-Kiste an " + targetBlock.getLocation() + " gesetzt");
            }

            return true;
        } else if (args[0].equalsIgnoreCase("fill")) {
            // Fülle die Kiste mit Tränken
            chestManager.fillChest();
            player.sendMessage(prefix + "§aAnti-Aging-Kiste wurde erfolgreich mit Tränken gefüllt!");
            
            if (debugManager != null) {
                debugManager.info("character", "Anti-Aging Chest", player.getName() + " hat die Anti-Aging-Kiste neu gefüllt");
            }

            return true;
        }

        player.sendMessage(prefix + "§cUnbekannter Unterbefehl! Verwendung: /antiagingchest <set|fill>");
        return true;
    }
}
