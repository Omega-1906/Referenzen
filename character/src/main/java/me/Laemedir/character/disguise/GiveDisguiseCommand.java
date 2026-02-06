package me.Laemedir.character.disguise;

import me.Laemedir.character.MultiCharPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * CommandExecutor für den /givedisguise Befehl.
 * Erlaubt Admins, Spielern dauerhaft eine Verwandlung zu geben.
 */
public class GiveDisguiseCommand implements CommandExecutor {

    private final MultiCharPlugin plugin;
    private final DisguiseManager disguiseManager;

    public GiveDisguiseCommand(MultiCharPlugin plugin, DisguiseManager disguiseManager) {
        this.plugin = plugin;
        this.disguiseManager = disguiseManager;
    }

    /**
     * Führt den GiveDisguise-Befehl aus.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Überprüfe Berechtigung
        // Die ursprüngliche Prüfung auf Player-Instanz ist nicht mehr nötig, da sender.hasPermission auch für Console funktioniert.
        // Die Berechtigung wurde auf "laemedir.admin" vereinfacht.
        if (!sender.hasPermission("laemedir.admin")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        // Überprüfe Argumentanzahl
        // Die Mindestanzahl der Argumente wurde auf 2 reduziert (Spieler, ID).
        if (args.length < 2) {
            sender.sendMessage("§cVerwendung: /givedisguise <Spieler> <ID> [Größe] [SubType]");
            return true;
        }

        String targetName = args[0];
        String disguiseId = args[1].toLowerCase(); // ID wird jetzt immer kleingeschrieben
        String size = args.length > 2 ? args[2].toLowerCase() : "normal"; // Größe ist optional, Standard "normal"
        String subType = args.length > 3 ? args[3].toLowerCase() : null; // SubType ist optional

        // Validiere Größe
        if (!size.equals("small") && !size.equals("normal") && !size.equals("big")) {
            player.sendMessage("§cUngültige Größe! Verwende: small, normal, big");
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§cSpieler nicht gefunden!");
            return true;
        }

        // Gib dem Spieler die Verwandlung
        disguiseManager.giveDisguise(target, disguiseId, size, subType, player);

        return true;
    }
}
