package me.Laemedir.character.dice;

import me.Laemedir.character.MultiCharPlugin;
import me.Laemedir.coreApi.debug.DebugManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * CommandExecutor für das Rollenspiel-Würfelsystem (/rpdice).
 * Unterstützt d10, d20, d100 und Attributs-Modifikatoren.
 */
public class RPDiceCommand implements CommandExecutor {

    private final MultiCharPlugin plugin;
    private final DiceManager diceManager;
    private final DebugManager debugManager;
    private final Random random = new Random();

    // Liste der erlaubten Attribute
    private final List<String> allowedAttributes = Arrays.asList(
            "Stärke",
            "Schnelligkeit",
            "Geschicklichkeit",
            "Konstitution",
            "Intelligenz"
    );

    public RPDiceCommand(MultiCharPlugin plugin, DiceManager diceManager) {
        this.plugin = plugin;
        this.diceManager = diceManager;
        this.debugManager = plugin.getDebugManager();
    }

    /**
     * Führt den Würfelbefehl aus.
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
            sender.sendMessage(ChatColor.RED + "Dieser Befehl kann nur von Spielern verwendet werden!");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        // Prüfen, ob der Spieler einen aktiven Charakter hat
        if (!MultiCharPlugin.activeCharacters.containsKey(playerUUID)) {
            player.sendMessage(ChatColor.RED + "Du musst einen aktiven Charakter haben, um würfeln zu können!");
            return true;
        }

        // Syntax überprüfen
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Verwendung: /rpdice 1d20|1d10|1d100 [Attribut]");
            player.sendMessage(ChatColor.GRAY + "Erlaubte Attribute: Stärke, Schnelligkeit, Geschicklichkeit, Konstitution, Intelligenz");
            return true;
        }

        // Prüfen, welcher Würfel verwendet wird
        String diceType = args[0].toLowerCase();
        int maxRoll;
        
        if (diceType.equals("1d20")) {
            maxRoll = 20;
        } else if (diceType.equals("1d10")) {
            maxRoll = 10;
        } else if (diceType.equals("1d100")) {
            maxRoll = 100;
        } else {
            player.sendMessage(ChatColor.RED + "Nur 1d20-, 1d10- und 1d100-Würfel werden unterstützt. Verwendung: /rpdice 1d20|1d10|1d100 [Attribut]");
            return true;
        }

        // Würfeln
        int roll = random.nextInt(maxRoll) + 1;

        // Attribut und Modifikator verarbeiten
        String attributeName = args.length > 1 ? args[1] : null;

        if (attributeName != null) {
            // Überprüfen, ob das angegebene Attribut erlaubt ist
            if (!allowedAttributes.contains(attributeName)) {
                player.sendMessage(ChatColor.RED + "Ungültiges Attribut. Erlaubte Attribute: Stärke, Schnelligkeit, Geschicklichkeit, Konstitution, Intelligenz");
                return true;
            }

            // Character ID des aktiven Charakters abrufen
            int characterId = MultiCharPlugin.getActiveCharacterId(player.getUniqueId());

            // Asynchron den Modifikator abrufen
            diceManager.getAttributeModifierAsync(characterId, attributeName, modifier -> {
                // Zurück auf den Main-Thread für Interaktionen mit der API (Items etc.) und Senden von Nachrichten
                Bukkit.getScheduler().runTask(plugin, () -> {
                    processRollWithModifier(player, roll, diceType, attributeName, modifier);
                });
            });
        } else {
            // Kein Attribut, direktes Ergebnis
            broadcastResult(player, roll, diceType, null, 0);
        }

        return true;
    }

    /**
     * Verarbeitet den Wurf mit dem abgerufenen Modifikator und Item-Boni.
     */
    private void processRollWithModifier(Player player, int roll, String diceType, String attributeName, int baseModifier) {
        int finalModifier = baseModifier;

        // Waffen-Modifikator hinzufügen, falls vorhanden
        ItemModifier itemMod = diceManager.getEquippedItemModifier(player, attributeName);
        if (itemMod != null) {
            finalModifier += itemMod.getValue();
        }

        broadcastResult(player, roll, diceType, attributeName, finalModifier);
    }

    /**
     * Sendet das Ergebnis an den Spieler und alle in der Nähe.
     */
    private void broadcastResult(Player player, int roll, String diceType, String attributeName, int modifier) {
        // Gesamtergebnis berechnen
        int total = roll + modifier;

        // Farbe für das Ergebnis basierend auf dem Würfelwurf
        ChatColor resultColor = (roll >= 10 || (roll == 20 && diceType.equals("1d20"))) ? ChatColor.GREEN : ChatColor.RED;

        // Nachricht formatieren - Minecraft-Namen anstatt Charakternamen verwenden
        String diceDisplay = diceType.toUpperCase();
        String resultMessage = ChatColor.YELLOW + "" + ChatColor.BOLD + player.getName() + ChatColor.RESET +
                ChatColor.YELLOW + " hat auf " + (attributeName != null ? attributeName : "Zufall") + " gewürfelt.\n" +
                roll + " (" + diceDisplay + ")";
        
        if (attributeName != null) {
            resultMessage += " + " + modifier + " (Modifikator)";
        }
        
        resultMessage += " = " + resultColor + "" + ChatColor.BOLD + total + ChatColor.RESET +
                ChatColor.YELLOW + " (Ergebnis)";

        // An alle Spieler in der Nähe senden (15 Blöcke Radius)
        for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
            if (nearbyPlayer.getWorld().equals(player.getWorld()) &&
                    nearbyPlayer.getLocation().distance(player.getLocation()) <= 15) {
                nearbyPlayer.sendMessage(resultMessage);
            }
        }
        
        if (debugManager != null) {
            debugManager.info("character", "Dice", player.getName() + " hat gewürfelt: " + diceType + " auf " + (attributeName != null ? attributeName : "Zufall") + " - Ergebnis: " + total);
        }
    }
}
