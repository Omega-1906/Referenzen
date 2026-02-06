package me.Laemedir.character;

import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command-Executor für den /charmenu Befehl.
 * Öffnet das Hauptmenü für die Charakterverwaltung.
 */
public class CharMenuCommand implements CommandExecutor {
    
    private final MultiCharPlugin plugin;
    private final CoreAPIPlugin coreAPI;
    private final CharMenuGUI charMenuGUI;
    
    /**
     * Konstruktor für den CharMenuCommand.
     *
     * @param plugin Die Instanz des MultiCharPlugins.
     * @param coreAPI Die Instanz des CoreAPIPlugins.
     * @param titleManager Der Manager für Titel.
     */
    public CharMenuCommand(MultiCharPlugin plugin, CoreAPIPlugin coreAPI, TitleManager titleManager) {
        this.plugin = plugin;
        this.coreAPI = coreAPI;
        this.charMenuGUI = new CharMenuGUI(plugin, coreAPI, titleManager);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Überprüfe, ob der Sender ein Spieler ist
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Command kann nur von Spielern ausgeführt werden!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Überprüfe Berechtigungen
        if (!player.hasPermission("laemedir.team")) {
            player.sendMessage("§cDu hast keine Berechtigung für diesen Command!");
            return true;
        }
        
        // Öffne das Hauptmenü
        charMenuGUI.openMainMenu(player);
        return true;
    }
}