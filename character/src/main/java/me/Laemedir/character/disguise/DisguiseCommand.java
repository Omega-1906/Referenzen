package me.Laemedir.character.disguise;

import me.Laemedir.character.MultiCharPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * CommandExecutor für den /disguise Befehl (Admin-Befehl).
 * Erlaubt Teammitgliedern, sich oder andere zu verwandeln.
 */
public class DisguiseCommand implements CommandExecutor {

    private final MultiCharPlugin plugin;
    private final DisguiseManager disguiseManager;

    public DisguiseCommand(MultiCharPlugin plugin, DisguiseManager disguiseManager) {
        this.plugin = plugin;
        this.disguiseManager = disguiseManager;
    }

    /**
     * Führt den Disguise-Befehl aus.
     *
     * @param sender  der Absender des Befehls
     * @param command der ausgeführte Befehl
     * @param label   das Label des Befehls
     * @param args    die Argumente
     * @return true, wenn der Befehl erfolgreich war
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern ausgeführt werden!");
            return true;
        }

        Player player = (Player) sender;

        // Überprüfe Berechtigung
        if (!player.hasPermission("laemedir.team")) {
            player.sendMessage("§cDu hast keine Berechtigung für diesen Befehl!");
            return true;
        }

        // Wenn nur ein Argument (Spielername) gegeben wurde - dient zum Entfernen
        if (args.length == 1) {
            String targetName = args[0];
            Player target = Bukkit.getPlayer(targetName);

            if (target == null) {
                player.sendMessage("§cSpieler nicht gefunden!");
                return true;
            }

            // Prüfe, ob der Spieler eine aktive Disguise hat und entferne sie
            if (disguiseManager.hasActiveDisguise(target)) {
                disguiseManager.removeDisguise(target);
                player.sendMessage("§aDu hast die Verwandlung von §e" + target.getName() + " §aentfernt!");
                if (!target.equals(player)) {
                    target.sendMessage("§eDeine Verwandlung wurde entfernt!");
                }
            } else {
                player.sendMessage("§e" + target.getName() + " §chat keine aktive Verwandlung!");
            }
            return true;
        }

        if (args.length < 3) {
            player.sendMessage("§cVerwendung: /disguise <Spieler> <ID> <Größe> [Typ/Variant]");
            player.sendMessage("§cOder: /disguise <Spieler> (um Verwandlung zu entfernen)");
            player.sendMessage("§cGrößen: small, normal, big");
            player.sendMessage("§cFür Katzen: Typ angeben (z.B. siamese, tabby)");
            player.sendMessage("§cFür Wölfe: Variant angeben (z.B. ashen, snowy)");
            return true;
        }

        String targetName = args[0];
        String disguiseId = args[1];
        String size = args[2].toLowerCase();
        String subType = args.length > 3 ? args[3] : null;

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

        // Verwandle den Spieler oder hebe Verwandlung auf
        disguiseManager.toggleDisguise(target, disguiseId, size, subType);

        if (!target.equals(player)) {
            player.sendMessage("§aDu hast §e" + target.getName() + " §averwandelt/enttarnt!");
        }

        return true;
    }
}
