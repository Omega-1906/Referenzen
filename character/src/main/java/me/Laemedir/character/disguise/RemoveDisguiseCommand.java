package me.Laemedir.character.disguise;

import me.Laemedir.character.MultiCharPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CommandExecutor für den /removedisguise Befehl.
 * Erlaubt Admins, einem Spieler eine Verwandlung wegzunehmen.
 */
public class RemoveDisguiseCommand implements CommandExecutor, TabCompleter {

    private final MultiCharPlugin plugin;
    private final DisguiseManager disguiseManager;

    public RemoveDisguiseCommand(MultiCharPlugin plugin, DisguiseManager disguiseManager) {
        this.plugin = plugin;
        this.disguiseManager = disguiseManager;
    }

    /**
     * Führt den RemoveDisguise-Befehl aus.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("laemedir.admin")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cVerwendung: /removedisguise <Spieler> <ID> [RecordID]");
            sender.sendMessage("§7Wenn RecordID angegeben ist, wird genau dieser Eintrag gelöscht.");
            sender.sendMessage("§7Sonst werden alle Einträge mit der Disguise-ID gelöscht.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cSpieler nicht gefunden.");
            return true;
        }

        // Wenn 3. Argument Zahl ist -> RecordID löschen
        if (args.length >= 3 && args[2].matches("\\d+")) {
            int recordId = Integer.parseInt(args[2]);
            disguiseManager.removeOwnedDisguiseByRecordId(sender, target, recordId);
        } else {
            String disguiseKey = args[1].toLowerCase();
            disguiseManager.removeOwnedDisguiseByKey(sender, target, disguiseKey);
        }

        return true;
    }

    /**
     * Tab-Vervollständigung für Spielernamen.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
        // ID (arg 2) bleibt frei – kann numerisch ODER string sein
        return Collections.emptyList();
    }
}
