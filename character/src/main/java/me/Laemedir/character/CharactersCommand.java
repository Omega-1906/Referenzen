package me.Laemedir.character;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CharactersCommand implements CommandExecutor {

    private final MultiCharPlugin plugin;

    /**
     * Konstruktor für den CharactersCommand.
     *
     * @param plugin Die Instanz des MultiCharPlugins.
     */
    public CharactersCommand(MultiCharPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur Spieler können diesen Befehl ausführen.");
            return true;
        }
        Player player = (Player) sender;
        // Öffne das GUI, ohne den Spieler zu freezen.
        plugin.openCharacterSelectionGUI(player);
        return true;
    }
}
