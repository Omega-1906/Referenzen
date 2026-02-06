package me.Laemedir.character.dice;

import me.Laemedir.character.MultiCharPlugin;
import me.Laemedir.coreApi.CoreAPIPlugin;
import me.Laemedir.coreApi.debug.DebugManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Map;

/**
 * CommandExecutor für die Berechnung der Rüstungsklasse (/armorclass).
 * Zeigt die aktuelle Rüstungsklasse eines Charakters an.
 */
public class ArmorClassCommand implements CommandExecutor {

    private final MultiCharPlugin plugin;
    private final DiceManager diceManager;
    private final DebugManager debugManager;
    private final CoreAPIPlugin coreAPI;

    public ArmorClassCommand(MultiCharPlugin plugin, DiceManager diceManager) {
        this.plugin = plugin;
        this.diceManager = diceManager;
        this.debugManager = plugin.getDebugManager();
        this.coreAPI = CoreAPIPlugin.getPlugin(CoreAPIPlugin.class);
    }

    /**
     * Führt den Befehl zum Anzeigen der Rüstungsklasse aus.
     *
     * @param sender  der Absender des Befehls
     * @param command der ausgeführte Befehl
     * @param label   das Label des Befehls
     * @param args    die Argumente (Charakter-ID)
     * @return true, wenn der Befehl erfolgreich war
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern ausgeführt werden.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage("§cBitte gib eine Charakter-ID an: /armorclass <charakter-id>");
            return true;
        }

        try {
            int characterId = Integer.parseInt(args[0]);

            // Prüfe, ob der Charakter existiert und hole gleichzeitig die player_uuid und den Namen (Asynchron)
            String query = "SELECT player_uuid, name FROM characters WHERE id = ?";
            coreAPI.queryAsync(query, rs -> {
                if (rs.isEmpty()) {
                    player.sendMessage("§cEs wurde kein Charakter mit der ID " + characterId + " gefunden.");
                    return;
                }

                try {
                    Map<String, Object> data = rs.get(0);
                    String playerUUID = (String) data.get("player_uuid");
                    String characterName = (String) data.get("name");

                    if (playerUUID == null) {
                        player.sendMessage("§cDer Charakter ist keinem Spieler zugeordnet.");
                        return;
                    }

                    // Zurück auf den Main-Thread für Bukkit-API-Aufrufe
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        calculateAndShowAC(player, characterId, characterName, playerUUID);
                    });

                } catch (Exception e) {
                    player.sendMessage("§cEs ist ein Fehler aufgetreten: " + e.getMessage());
                    if (debugManager != null) {
                        debugManager.error("character", "Dice", "Fehler bei der Rüstungsklassen-Abfrage", e);
                    }
                }
            }, characterId);


        } catch (NumberFormatException e) {
            player.sendMessage("§cBitte gib eine gültige Charakter-ID an.");
        }

        return true;
    }

    /**
     * Berechnet und zeigt die Rüstungsklasse an (Main-Thread).
     */
    private void calculateAndShowAC(Player player, int characterId, String characterName, String playerUUID) {
        Player targetPlayer = Bukkit.getPlayer(java.util.UUID.fromString(playerUUID));
        if (targetPlayer == null) {
            player.sendMessage("§cDer Spieler dieses Charakters ist nicht online.");
            return;
        }

        // Berechne die Rüstungsklasse
        int armorClass = diceManager.calculateArmorClass(targetPlayer);

        player.sendMessage("§6Rüstungsklasse von §e" + characterName + " §6(ID: §e" + characterId + "§6): §e" + armorClass);
        
        if (debugManager != null) {
            debugManager.info("character", "Dice", player.getName() + " hat die Rüstungsklasse von " + characterName + " (ID: " + characterId + ") berechnet: " + armorClass);
        }
    }
}
