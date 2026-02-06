package me.Laemedir.character.disguise;

import me.Laemedir.character.MultiCharPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * CommandExecutor für den /verwandlung Befehl.
 * Öffnet das GUI oder hebt die Verwandlung auf.
 * Alias für /disguise für normale Spieler (in gewisser Weise).
 */
public class VerwandlungCommand implements CommandExecutor {

    private final MultiCharPlugin plugin;
    private final DisguiseManager disguiseManager;

    public VerwandlungCommand(MultiCharPlugin plugin, DisguiseManager disguiseManager) {
        this.plugin = plugin;
        this.disguiseManager = disguiseManager;
    }

    /**
     * Führt den Verwandlung-Befehl aus.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }

        Player player = (Player) sender;

        // Wenn "off" oder "aus" als Argument übergeben wird, Verwandlung entfernen
        if (args.length > 0 && (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("aus") || args[0].equalsIgnoreCase("remove"))) {
            if (disguiseManager.hasActiveDisguise(player)) {
                disguiseManager.removeDisguise(player);
                player.sendMessage("§a§l✓ §aDeine Verwandlung wurde aufgehoben.");
            } else {
                player.sendMessage("§cDu bist aktuell nicht verwandelt.");
            }
            return true;
        }

        // Ansonsten GUI öffnen (ruft asynchron die Liste der Verwandlungen ab)
        disguiseManager.getPlayerDisguises(player, disguises -> {
            if (disguises.length == 0) {
                // Keine Verwandlungen verfügbar
                // Überprüfe ob Spieler schon verwandelt ist, dann nur Info
                if (disguiseManager.hasActiveDisguise(player)) {
                    player.sendMessage("§aDu bist als §e" + disguiseManager.getCurrentDisguise(player) + " §averwandelt.");
                    player.sendMessage("§7Nutze §e/verwandlung aus§7, um dich zurückzuverwandeln.");
                } else {
                    player.sendMessage("§cDu besitzt keine Verwandlungen!");
                }
                return;
            }

            // GUI im Main Thread öffnen
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                new DisguiseGUI(plugin, disguiseManager).openGUI(player, disguises);
            });
        });

        return true;
    }
}
