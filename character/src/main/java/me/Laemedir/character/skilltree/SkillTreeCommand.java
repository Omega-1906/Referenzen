package me.Laemedir.character.skilltree;

import me.Laemedir.character.MultiCharPlugin;
import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * CommandExecutor für den /skilltree Befehl.
 * Erlaubt Spielern das Öffnen des Skilltrees und Admins das Verwalten von Punkten.
 */
public class SkillTreeCommand implements CommandExecutor {

    private final CoreAPIPlugin coreAPI;
    private final SkillTreeManager skillTreeManager;
    private final MultiCharPlugin plugin;
    private final String prefix = "§8[§6Skills§8] ";

    public SkillTreeCommand(CoreAPIPlugin coreAPI, SkillTreeManager skillTreeManager, MultiCharPlugin plugin) {
        this.coreAPI = coreAPI;
        this.skillTreeManager = skillTreeManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + "§cDieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Öffne den Skilltree für den Spieler
            skillTreeManager.openSkillTreeGUI(player);
            return true;
        }

        // Admin-Befehle
        if (args.length >= 1) {
            // Punkte hinzufügen
            if (args[0].equalsIgnoreCase("addpoints") && player.hasPermission("character.skilltree.admin")) {
                if (args.length < 3) {
                    player.sendMessage(prefix + "§cVerwendung: /skilltree addpoints <Charakter> <Anzahl>");
                    return true;
                }

                String targetName = args[1];
                int points;

                try {
                    points = Integer.parseInt(args[2]);
                    if (points <= 0) {
                        player.sendMessage(prefix + "§cDie Punktzahl muss positiv sein.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(prefix + "§cBitte gib eine gültige Zahl ein.");
                    return true;
                }

                // Suche direkt nach dem Charakter in der characters-Tabelle (Async)
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    String getCharacterSQL = "SELECT id, player_uuid, name FROM characters WHERE name = ?";
                    coreAPI.queryAsync(getCharacterSQL, results -> {
                        if (results.isEmpty()) {
                            player.sendMessage(prefix + "§cCharakter nicht gefunden.");
                            return;
                        }

                        int characterId = ((Number) results.get(0).get("id")).intValue();
                        String playerUuid = results.get(0).get("player_uuid").toString();

                        // Prüfe, ob dieser Charakter aktiv ist
                        String activeChar = MultiCharPlugin.getActiveCharacter(java.util.UUID.fromString(playerUuid));
                        if (activeChar == null || !activeChar.equals(targetName)) {
                            player.sendMessage(prefix + "§cDer Charakter ist nicht aktiv.");
                            return;
                        }

                        // Füge Skillpunkte hinzu
                        skillTreeManager.addSkillPoints(characterId, points);
                        player.sendMessage(prefix + "§aDu hast §e" + points + " Skillpunkte §afür §e" + targetName + " §ahinzugefügt.");

                        // Benachrichtige den Spieler, falls online
                        Player targetPlayer = plugin.getServer().getPlayer(java.util.UUID.fromString(playerUuid));
                        if (targetPlayer != null) {
                            targetPlayer.sendMessage(prefix + "§aDu hast §e" + points + " §aneue Skillpunkte erhalten!");
                        }
                    }, targetName);
                });

                return true;
            }

            // Info anzeigen
            if (args[0].equalsIgnoreCase("info") && player.hasPermission("character.skilltree.admin")) {
                if (args.length < 2) {
                    player.sendMessage(prefix + "§cVerwendung: /skilltree info <Charakter>");
                    return true;
                }

                String targetName = args[1];

                // Suche direkt nach dem Charakter in der characters-Tabelle (Async)
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    String getCharacterSQL = "SELECT id, player_uuid FROM characters WHERE name = ?";
                    coreAPI.queryAsync(getCharacterSQL, results -> {
                        if (results.isEmpty()) {
                            player.sendMessage(prefix + "§cCharakter nicht gefunden.");
                            return;
                        }

                        int characterId = ((Number) results.get(0).get("id")).intValue();
                        String playerUuid = results.get(0).get("player_uuid").toString();

                        // Prüfe, ob dieser Charakter aktiv ist
                        String activeChar = MultiCharPlugin.getActiveCharacter(java.util.UUID.fromString(playerUuid));
                        if (activeChar == null || !activeChar.equals(targetName)) {
                            player.sendMessage(prefix + "§cDer Charakter ist nicht aktiv.");
                            return;
                        }

                        // Hole Skill-Daten
                        String getSkillsSQL = "SELECT * FROM character_skills WHERE character_id = ?";
                        coreAPI.queryAsync(getSkillsSQL, skillResults -> {
                            if (skillResults.isEmpty()) {
                                player.sendMessage(prefix + "§cKeine Skill-Daten gefunden.");
                                return;
                            }

                            Map<String, Object> data = skillResults.get(0);
                            int strength = ((Number) data.get("strength")).intValue();
                            int speed = ((Number) data.get("speed")).intValue();
                            int dexterity = ((Number) data.get("dexterity")).intValue();
                            int constitution = ((Number) data.get("constitution")).intValue();
                            int availablePoints = ((Number) data.get("available_points")).intValue();

                            player.sendMessage("§8§m-----------------------------------------------------");
                            player.sendMessage(prefix + "§6Skill-Informationen für §e" + targetName);
                            player.sendMessage("§8» §7Stärke: §e" + strength);
                            player.sendMessage("§8» §7Schnelligkeit: §e" + speed);
                            player.sendMessage("§8» §7Geschicklichkeit: §e" + dexterity);
                            player.sendMessage("§8» §7Konstitution: §e" + constitution);
                            player.sendMessage("§8» §7Verfügbare Punkte: §e" + availablePoints);
                            player.sendMessage("§8§m-----------------------------------------------------");
                        }, characterId);
                    }, targetName);
                });

                return true;
            }
        }

        // Wenn kein bekannter Befehl erkannt wurde
        player.sendMessage(prefix + "§cUnbekannter Befehl. Verwende /skilltree ohne Parameter, um den Skilltree zu öffnen.");
        return true;
    }
}
