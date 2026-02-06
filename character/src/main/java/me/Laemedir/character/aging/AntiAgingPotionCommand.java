package me.Laemedir.character.aging;

import me.Laemedir.character.MultiCharPlugin;
import me.Laemedir.coreApi.debug.DebugManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.Arrays;

/**
 * CommandExecutor für das Erstellen von Anti-Aging-Tränken.
 * Ermöglicht Admins, Tränke direkt zu erhalten.
 */
public class AntiAgingPotionCommand implements CommandExecutor {
    private final MultiCharPlugin plugin;
    private final DebugManager debugManager;
    private final String prefix;

    public AntiAgingPotionCommand(MultiCharPlugin plugin) {
        this.plugin = plugin;
        this.debugManager = plugin.getDebugManager();
        this.prefix = "§8[§6Characters§8] ";
    }

    /**
     * Führt den Befehl zum Erhalten von Anti-Aging-Tränken aus.
     *
     * @param sender  der Absender des Befehls
     * @param command der ausgeführte Befehl
     * @param label   das Label des Befehls
     * @param args    die Argumente (optional: Anzahl)
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

        int amount = 1;

        if (args.length > 0) {
            try {
                amount = Integer.parseInt(args[0]);
                if (amount < 1) amount = 1;
                if (amount > 64) amount = 64;
            } catch (NumberFormatException e) {
                player.sendMessage(prefix + "§cUngültige Anzahl! Verwende eine Zahl zwischen 1 und 64.");
                return true;
            }
        }

        // Erstelle den Anti-Alterungs-Trank
        ItemStack potion = new ItemStack(Material.POTION, amount);
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

        potionMeta.setBasePotionData(new PotionData(PotionType.MUNDANE));
        potionMeta.setDisplayName("§b§lAnti-Alterungs-Trank");
        potionMeta.setLore(Arrays.asList(
                "§7Dieser magische Trank verhindert",
                "§7die Alterung für einen Monat.",
                "",
                "§8» §7Verwende §f/aging trinken§7, um",
                "§7den Trank zu konsumieren."
        ));

        potion.setItemMeta(potionMeta);

        // Gib dem Spieler den Trank
        player.getInventory().addItem(potion);
        player.sendMessage(prefix + "§aDu hast §b" + amount + " Anti-Alterungs-Trank(e) §aerhalten!");
        
        if (debugManager != null) {
            debugManager.info("character", "Anti-Aging Potion", player.getName() + " hat " + amount + " Anti-Alterungs-Trank(e) erstellt");
        }

        return true;
    }
}
