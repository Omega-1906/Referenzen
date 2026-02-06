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
import java.util.Random;

/**
 * CommandExecutor für Angriffswürfe (/attackroll).
 * Führt einen Angriffswurf gegen einen anderen Charakter aus.
 */
public class AttackRollCommand implements CommandExecutor {

    private final MultiCharPlugin plugin;
    private final DiceManager diceManager;
    private final DebugManager debugManager;
    private final CoreAPIPlugin coreAPI;
    private final Random random = new Random();
    private final int NEARBY_RADIUS = 15; // Radius in Blöcken, in dem Spieler als "in der Nähe" gelten

    public AttackRollCommand(MultiCharPlugin plugin, DiceManager diceManager) {
        this.plugin = plugin;
        this.diceManager = diceManager;
        this.debugManager = plugin.getDebugManager();
        this.coreAPI = CoreAPIPlugin.getPlugin(CoreAPIPlugin.class);
    }

    /**
     * Führt den Befehl für einen Angriffswurf aus.
     *
     * @param sender  der Absender des Befehls
     * @param command der ausgeführte Befehl
     * @param label   das Label des Befehls
     * @param args    die Argumente (Angreifer-Name, Ziel-Name)
     * @return true, wenn der Befehl erfolgreich war
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern ausgeführt werden.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage("§cVerwendung: /attackroll <angreifer-charakter-name> <ziel-charakter-name>");
            return true;
        }

        String attackerNameInput = args[0];
        String targetNameInput = args[1];

        // Finde den Angreifer-Charakter asynchron anhand des Namens
        String attackerQuery = "SELECT id, player_uuid, name FROM characters WHERE name = ?";
        coreAPI.queryAsync(attackerQuery, rsAttacker -> {
            if (rsAttacker.isEmpty()) {
                player.sendMessage("§cEs wurde kein Angreifer-Charakter mit dem Namen " + attackerNameInput + " gefunden.");
                return;
            }

            // Finde den Ziel-Charakter asynchron anhand des Namens
            String targetQuery = "SELECT id, player_uuid, name FROM characters WHERE name = ?";
            coreAPI.queryAsync(targetQuery, rsTarget -> {
                if (rsTarget.isEmpty()) {
                    player.sendMessage("§cEs wurde kein Ziel-Charakter mit dem Namen " + targetNameInput + " gefunden.");
                    return;
                }

                try {
                    // Daten extrahieren
                    Map<String, Object> attackerData = rsAttacker.get(0);
                    int attackerCharId = (int) attackerData.get("id");
                    String attackerUUID = (String) attackerData.get("player_uuid");
                    String attackerName = (String) attackerData.get("name");

                    Map<String, Object> targetData = rsTarget.get(0);
                    String targetUUID = (String) targetData.get("player_uuid");
                    String targetName = (String) targetData.get("name");

                    if (attackerUUID == null) {
                        player.sendMessage("§cDer Angreifer-Charakter ist keinem Spieler zugeordnet.");
                        return;
                    }

                    if (targetUUID == null) {
                        player.sendMessage("§cDer Ziel-Charakter ist keinem Spieler zugeordnet.");
                        return;
                    }

                    // Zurück auf den Main-Thread für Bukkit-API-Aufrufe
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        executeAttack(player, attackerCharId, attackerName, attackerUUID, targetName, targetUUID);
                    });

                } catch (Exception e) {
                    player.sendMessage("§cEs ist ein Fehler aufgetreten: " + e.getMessage());
                    if (debugManager != null) {
                        debugManager.error("character", "Dice", "Fehler beim Angriffswurf", e);
                    }
                }
            }, targetNameInput);

        }, attackerNameInput);

        return true;
    }

    /**
     * Führt die Logik des Angriffs auf dem Main-Thread aus.
     */
    private void executeAttack(Player player, int attackerCharId, String attackerName, String attackerUUID, String targetName, String targetUUID) {
        Player attackerPlayer = Bukkit.getPlayer(java.util.UUID.fromString(attackerUUID));
        Player targetPlayer = Bukkit.getPlayer(java.util.UUID.fromString(targetUUID));

        if (attackerPlayer == null) {
            player.sendMessage("§cDer Spieler des Angreifer-Charakters ist nicht online.");
            return;
        }

        if (targetPlayer == null) {
            player.sendMessage("§cDer Spieler des Ziel-Charakters ist nicht online.");
            return;
        }

        // Überprüfe, ob der Ziel-Spieler in der Nähe ist
        if (!targetPlayer.getWorld().equals(attackerPlayer.getWorld()) ||
                targetPlayer.getLocation().distance(attackerPlayer.getLocation()) > NEARBY_RADIUS) {
            player.sendMessage("§cDer Ziel-Charakter ist nicht in deiner Nähe.");
            return;
        }

        // Berechne die Rüstungsklasse des Ziels
        int armorClass = diceManager.calculateArmorClass(targetPlayer);

        // Würfle den Angriff (1d20)
        int diceRoll = random.nextInt(20) + 1;

        // Hole den Stärke-Modifikator des Angreifers (Async-Call mit Callback auf Main-Thread verschachtelt)
        diceManager.getAttributeModifierAsync(attackerCharId, "Stärke", strengthMod -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                finalizeAttack(player, attackerPlayer, targetPlayer, attackerName, targetName, diceRoll, armorClass, strengthMod);
            });
        });
    }

    /**
     * Finalisiert den Angriff nach dem Abrufen aller Modifikatoren.
     */
    private void finalizeAttack(Player admin, Player attacker, Player target, String attackerName, String targetName, int diceRoll, int armorClass, int strengthMod) {
        // Hole den Waffen-Modifikator des Angreifers
        int weaponMod = 0;
        ItemModifier itemMod = diceManager.getEquippedItemModifier(attacker, "Stärke");
        if (itemMod != null) {
            weaponMod = itemMod.getValue();
        }

        // Berechne den Gesamtwurf
        int totalRoll = diceRoll + strengthMod + weaponMod;

        // Prüfe, ob der Angriff trifft
        boolean hits = diceManager.checkHit(totalRoll, armorClass);

        // Formatiere die Nachricht im Dice-Schema-Stil
        String resultSymbol = hits ? "§a✓" : "§c✗";
        String resultText = hits ? "§aTreffer!" : "§cVerfehlt!";

        // Erstelle die Nachricht im Dice-Schema-Format
        String[] messages = {
                "§8§l§m-----§r §e§lAngriffswurf §8§l§m-----",
                "§7" + attackerName + " §8» §7greift §7" + targetName + " §7an",
                "",
                "§8[§e1d20§8] §7Wurf: §e" + diceRoll,
                "§8[§b+§8] §7Stärke-Mod: §b" + strengthMod,
                "§8[§b+§8] §7Waffen-Mod: §b" + weaponMod,
                "§8[§7=§8] §7Gesamt: §e" + totalRoll + " §8vs. §7RK: §e" + armorClass,
                "",
                "§8[" + resultSymbol + "§8] §7Ergebnis: " + resultText,
                "§8§l§m-------------------------"
        };

        // Sende die Nachricht an alle Spieler im Radius
        for (Player nearbyPlayer : attacker.getWorld().getPlayers()) {
            if (nearbyPlayer.getLocation().distance(attacker.getLocation()) <= NEARBY_RADIUS) {
                for (String message : messages) {
                    nearbyPlayer.sendMessage(message);
                }
            }
        }
        
        if (debugManager != null) {
            debugManager.info("character", "Dice", "Angriffswurf: " + attackerName + " greift " + targetName + " an. Wurf: " + totalRoll + " vs RK: " + armorClass + " - " + (hits ? "Treffer" : "Verfehlt"));
        }
    }
}
