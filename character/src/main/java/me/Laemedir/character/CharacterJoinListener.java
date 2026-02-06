package me.Laemedir.character;

import me.Laemedir.coreApi.CoreAPIPlugin;
import me.Laemedir.coreApi.debug.DebugManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * Listener für Spieler-Beitritts-Events.
 * Handhabt den ersten Join, Ressourcenpaket-Status und die Initialisierung der Charakterauswahl.
 */
public class CharacterJoinListener implements Listener {
    private final MultiCharPlugin plugin;
    private final CoreAPIPlugin coreAPI;
    private final DebugManager debugManager;
    private final String prefix = "§8[§6Characters§8] ";

    public CharacterJoinListener(MultiCharPlugin plugin, CoreAPIPlugin coreAPI) {
        this.plugin = plugin;
        this.coreAPI = coreAPI;
        this.debugManager = plugin.getDebugManager();
    }

    /**
     * Wird ausgeführt, wenn ein Spieler den Server betritt.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Teleportiere den Spieler in den sicheren Bereich ("Spawn")
        World world = Bukkit.getWorld("world");
        if (world != null) {
            Location spawnLoc = new Location(world, -770.002, -20.00, -496.471, -0.0f, 3.9f);
            player.teleport(spawnLoc);
        } else {
            player.sendMessage(prefix + "§cFehler: Welt nicht gefunden!");
            if (debugManager != null) {
                debugManager.warning("character", "Player Join", "Welt 'world' konnte nicht gefunden werden!");
            }
            return;
        }

        player.closeInventory();
        plugin.removePendingResourcePackPlayer(player);
        openCharacterSelectionIfReady(player);

        // Optional: Ressourcenpaket-Logik hier aktivieren, falls benötigt
        // sendResourcePack(player);

        // Spieler-Reset
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));

        // Spieler einfrieren bis Charakter gewählt
        plugin.addFrozen(player);

        player.sendMessage(prefix + "§cDu hast noch keinen Charakter ausgewählt. Bitte wähle einen aus!");

        if (coreAPI == null) {
            if (debugManager != null) {
                debugManager.error("character", "Player Join", "CoreAPI ist nicht initialisiert!");
            }
            return;
        }

        // Überprüfe, ob der Spieler einen bereits aktiven Charakter hat (z.B. nach Server-Neustart)
        if (MultiCharPlugin.getActiveCharacter(playerUUID) == null) {
            checkIfPlayerHasCharacters(player);
        } else {
            // Spieler hat einen aktiven Charakter (Session wiederhergestellt)
            if (plugin.getAgingSystem() != null) {
                int characterId = MultiCharPlugin.getActiveCharacterId(player.getUniqueId());
                if (characterId != -1) {
                    plugin.getAgingSystem().setFirstLoginIfNeeded(characterId);
                }
            }
        }
    }

    /**
     * Prüft async, ob der Spieler überhaupt Charaktere besitzt.
     */
    private void checkIfPlayerHasCharacters(Player player) {
        UUID playerUUID = player.getUniqueId();
        String checkCharacterQuery = "SELECT COUNT(*) AS count FROM characters WHERE player_uuid = ?";
        
        coreAPI.queryAsync(checkCharacterQuery, results -> {
            if (!results.isEmpty()) {
                Long countLong = (Long) results.get(0).get("count");
                int count = countLong.intValue();

                if (count == 0) {
                    handleNoCharacters(player);
                }
            }
        }, playerUUID.toString());
    }

    /**
     * Behandelt den Fall, dass ein Spieler keine Charaktere hat.
     */
    private void handleNoCharacters(Player player) {
        if (!player.isOnline()) return;

        player.closeInventory();
        plugin.removePlayerInCharacterSelection(player.getUniqueId());

        // Kick nach Verzögerung
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.kickPlayer("§cDu hast keinen Charakter. Bitte beantrage einen im Controlpanel.");
            }
        }, 2400L); // 120 Sekunden
    }

    /**
     * Reagiert auf den Status des Ressourcenpakets.
     */
    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        PlayerResourcePackStatusEvent.Status status = event.getStatus();

        switch (status) {
            case ACCEPTED:
                player.sendMessage("§a✅ Ressourcenpaket akzeptiert!");
                plugin.removePendingResourcePackPlayer(player);
                openCharacterSelectionIfReady(player);
                break;
            case DECLINED:
                player.kickPlayer("§c❌ Du hast das Ressourcenpaket abgelehnt. Bitte akzeptiere es, um zu spielen.");
                plugin.removePendingResourcePackPlayer(player);
                break;
            case FAILED_DOWNLOAD:
                player.kickPlayer("§c⚠️ Fehler beim Herunterladen des Ressourcenpakets!");
                plugin.removePendingResourcePackPlayer(player);
                break;
            case SUCCESSFULLY_LOADED:
                player.sendMessage("§a🎉 Ressourcenpaket erfolgreich geladen!");
                plugin.removePendingResourcePackPlayer(player);
                openCharacterSelectionIfReady(player);
                break;
        }
    }

    /**
     * Startet den Prozess zum Senden des Ressourcenpakets.
     */
    private void sendResourcePack(Player player) {
        String resourcePackUrl = "https://www.dropbox.com/scl/fi/6e7adqo2dr598oyq6kqv0/Laemedir-Pack.zip?rlkey=qxnoess6048vnncbi8tz1t2y2&st=z6485ty9&dl=0";
        // player.setResourcePack(resourcePackUrl); // Einkommentieren wenn aktiv
        plugin.addPendingResourcePackPlayer(player);
        player.sendMessage("§6⚙️ §eDas Ressourcenpaket wird geladen...");
    }

    private void openCharacterSelectionIfReady(Player player) {
        // Überprüfen, ob der Spieler bereits einen aktiven Charakter hat
        if (MultiCharPlugin.getActiveCharacter(player.getUniqueId()) == null) {
            // Überprüfen, ob der Spieler überhaupt Charaktere hat
            String checkCharacterQuery = "SELECT COUNT(*) AS count FROM characters WHERE player_uuid = ?";
            coreAPI.queryAsync(checkCharacterQuery, results -> {
                if (!results.isEmpty()) {
                    Long countLong = (Long) results.get(0).get("count");
                    int count = countLong.intValue();

                    if (count == 0) {
                        // Wenn keine Charaktere vorhanden sind, Freeze aufheben und Nachricht senden
                        if (player.isOnline()) {
                            player.sendMessage("§cDu hast keinen Charakter. Bitte beantrage einen im Controlpanel.");
                            player.sendMessage("");
                            player.sendMessage("§cUm Zugriff auf das Controlpanel zu erhalten gebe /registercp ein, du wirst in 120 Sekunden gekickt.");

                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (player.isOnline()) {
                                    player.kickPlayer("§cDu hast keinen Charakter. Bitte beantrage einen im Controlpanel.");
                                }
                            }, 2400L); // 120 Sekunden (2400 Ticks)
                        }
                    } else {
                        // Wenn Charaktere vorhanden sind, GUI öffnen
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                // Spieler einfrieren, falls noch nicht geschehen
                                plugin.addFrozen(player);

                                // GUI öffnen und im Plugin registrieren, dass dieser Spieler im Charakterauswahl-Modus ist
                                plugin.openCharacterSelectionGUI(player);
                                plugin.addPlayerInCharacterSelection(player.getUniqueId());
                            }
                        }, 20L);
                    }
                }
            }, player.getUniqueId().toString());
        } else {
            // Wenn der Spieler bereits einen aktiven Charakter hat, entferne den Freeze-Status
            plugin.removeFrozen(player);
        }
    }
}
