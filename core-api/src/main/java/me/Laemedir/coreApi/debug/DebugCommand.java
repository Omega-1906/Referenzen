package me.Laemedir.coreApi.debug;

import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DebugCommand implements CommandExecutor, TabCompleter {
    private final CoreAPIPlugin plugin;
    private final DebugManager debugManager;

    public DebugCommand(CoreAPIPlugin plugin, DebugManager debugManager) {
        this.plugin = plugin;
        this.debugManager = debugManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl verwenden.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("laemedir.debug")) {
            player.sendMessage("§cDu hast keine Berechtigung für diesen Befehl.");
            return true;
        }

        if (args.length == 0) {
            // Öffne Haupt-GUI
            new DebugMainGUI(plugin, debugManager, player).open();
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "stats":
                showStats(player);
                break;
            case "cleanup":
                if (args.length < 2) {
                    player.sendMessage("§cVerwendung: /debug cleanup <Tage>");
                    return true;
                }
                try {
                    int days = Integer.parseInt(args[1]);
                    debugManager.cleanupOldEntries(days, count -> {
                        player.sendMessage("§a[Debug] Alte Einträge wurden gelöscht.");
                    });
                } catch (NumberFormatException e) {
                    player.sendMessage("§cUngültige Zahl.");
                }
                break;
            case "test":
                // Test-Eintrag erstellen
                debugManager.log("TestPlugin", DebugLevel.INFO, "Test", "Dies ist ein Test-Eintrag", player);
                player.sendMessage("§a[Debug] Test-Eintrag erstellt.");
                break;
            default:
                player.sendMessage("§cUnbekannter Unterbefehl. Nutze: stats, cleanup, test");
        }

        return true;
    }

    private void showStats(Player player) {
        debugManager.getStats(stats -> {
            player.sendMessage("§8§m                                        ");
            player.sendMessage("§6§lDebug-System Statistiken");
            player.sendMessage("");
            player.sendMessage("§7Gesamt Einträge: §e" + stats.totalEntries);
            player.sendMessage("§7Fehler: §c" + stats.errorCount);
            player.sendMessage("§7Plugins: §b" + stats.pluginCount);
            player.sendMessage("§7Aktive Tage: §a" + stats.activeDays);
            player.sendMessage("§8§m                                        ");
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("laemedir.debug")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("stats", "cleanup", "test");
        }

        return new ArrayList<>();
    }
}
