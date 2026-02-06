package me.Laemedir.character;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Listener für Rassen-spezifische Events und Chat-Interaktionen.
 * Verwaltet das Anwenden von Rasseneffekten bei Join/Respawn und das Rassen-Chat-Format.
 * Handelt auch die Eingabe für die Rassenerstellung.
 */
public class RaceChatListener implements Listener {

    private final RaceManager raceManager;
    private final MultiCharPlugin plugin;
    private final String prefix = "§8[§6Rassen§8] ";

    public RaceChatListener(RaceManager raceManager, MultiCharPlugin plugin) {
        this.raceManager = raceManager;
        this.plugin = plugin;
    }

    // NEUE EVENT HANDLER FÜR RASSENEFFEKTE
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Verzögerung für MultiChar-Integration
        new BukkitRunnable() {
            @Override
            public void run() {
                String activeCharacterName = MultiCharPlugin.getActiveCharacter(playerId);

                if (activeCharacterName != null) {
                    raceManager.applyRaceEffectsForCharacter(player, activeCharacterName);
                }
            }
        }.runTaskLater(plugin, 20L); // 1 Sekunde Verzögerung
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Verzögerung, damit der Respawn vollständig abgeschlossen ist
        new BukkitRunnable() {
            @Override
            public void run() {
                String activeCharacterName = MultiCharPlugin.getActiveCharacter(playerId);

                if (activeCharacterName != null) {
                    raceManager.applyRaceEffectsForCharacter(player, activeCharacterName);
                }
            }
        }.runTaskLater(plugin, 10L); // 0.5 Sekunden Verzögerung
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        String activeCharacterName = MultiCharPlugin.getActiveCharacter(playerId);

        if (activeCharacterName != null) {
            raceManager.removeRaceEffectsForCharacter(player, activeCharacterName);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        String activeCharacterName = MultiCharPlugin.getActiveCharacter(playerId);

        if (activeCharacterName != null) {
            // Effekte beim Tod entfernen (werden beim Respawn neu angewendet)
            raceManager.removeRaceEffectsForCharacter(player, activeCharacterName);
        }
    }

    // BESTEHENDER CHAT HANDLER
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Prüfen, ob der Spieler in einer Rassen-Erstellungssession ist
        if (RaceCreationSession.hasSession(player)) {
            event.setCancelled(true);
            RaceCreationSession session = RaceCreationSession.getSession(player);
            String message = event.getMessage();

            // Abbruch der Session
            if (message.equalsIgnoreCase("cancel")) {
                RaceCreationSession.endSession(player);
                player.sendMessage(prefix + "§c❌ Rassenerstellung abgebrochen.");
                return;
            }

            // Je nach aktuellem Status der Session
            switch (session.getStage()) {
                case DESCRIPTION:
                    handleDescriptionInput(player, session, message);
                    break;
                case EFFECTS:
                    handleEffectInput(player, session, message);
                    break;
            }
            return; // Wichtig: Return hier, damit der normale Chat nicht weiterverarbeitet wird
        }

        // RASSEN-CHAT-FORMAT (nur wenn nicht in Session)
        UUID playerId = player.getUniqueId();
        String activeCharacterName = MultiCharPlugin.getActiveCharacter(playerId);

        if (activeCharacterName == null) return;

        Race race = raceManager.getCharacterRace(activeCharacterName);
        if (race == null) return;

        String raceName = race.getName();

        // Chat-Format mit Rasse
        String format = event.getFormat();
        String newFormat = format.replace("%1$s", "§6[" + raceName + "] §r%1$s");
        event.setFormat(newFormat);
    }

    private void handleDescriptionInput(Player player, RaceCreationSession session, String description) {
        session.setDescription(description);
        player.sendMessage(prefix + "§a✅ Beschreibung gespeichert: §f" + description);
        player.sendMessage(prefix + "§e✨ Gebe nun Effekte ein, immer einen pro Nachricht.");
        player.sendMessage(prefix + "§7Format: EFFECT_NAME:STÄRKE (z.B. SPEED:1)");
        player.sendMessage(prefix + "§7Gebe §e'fertig' §7ein, wenn du keine weiteren Effekte hinzufügen möchtest.");
        player.sendMessage(prefix + "§7Oder §e'cancel' §7um die Erstellung abzubrechen.");

        // Liste der verfügbaren Effekte anzeigen
        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage(prefix + "§7Verfügbare Effekte:");
                StringBuilder effectList = new StringBuilder();
                int count = 0;

                for (PotionEffectType type : PotionEffectType.values()) {
                    if (type != null) {
                        effectList.append("§b").append(type.getName()).append("§7, ");
                        count++;

                        if (count % 5 == 0) {
                            player.sendMessage(prefix + "§7" + effectList.toString().trim());
                            effectList = new StringBuilder();
                        }
                    }
                }

                if (effectList.length() > 0) {
                    player.sendMessage(prefix + "§7" + effectList.toString().trim());
                }
            }
        }.runTask(plugin);
    }

    private void handleEffectInput(Player player, RaceCreationSession session, String input) {
        if (input.equalsIgnoreCase("fertig")) {
            // Erstellung abschließen
            String effectsString = session.getEffects().isEmpty() ? "" :
                    session.getEffects().stream().collect(Collectors.joining(";"));

            boolean success = raceManager.createRace(
                    session.getRaceName(),
                    session.getDescription(),
                    effectsString
            );

            if (success) {
                player.sendMessage(prefix + "§a✅ Die Rasse §6'" + session.getRaceName() + "' §awurde erfolgreich erstellt!");
                player.sendMessage(prefix + "§e📜 Beschreibung: §f" + session.getDescription());

                if (!session.getEffects().isEmpty()) {
                    player.sendMessage(prefix + "§e✨ Effekte:");
                    for (String effect : session.getEffects()) {
                        String[] parts = effect.split(":");
                        String effectName = parts[0];
                        String strength = parts.length > 1 ? parts[1] : "1";
                        player.sendMessage(prefix + "§7• §b" + effectName + " (Stärke: " + strength + ") ✨");
                    }
                } else {
                    player.sendMessage(prefix + "§7Keine Effekte hinzugefügt.");
                }
            } else {
                player.sendMessage(prefix + "§c❌ Fehler beim Erstellen der Rasse.");
            }

            RaceCreationSession.endSession(player);
            return;
        }

        // Effekt validieren und hinzufügen
        String[] parts = input.split(":");
        String effectName = parts[0].toUpperCase();
        String strength = parts.length > 1 ? parts[1] : "1";

        PotionEffectType effectType = PotionEffectType.getByName(effectName);
        if (effectType == null) {
            player.sendMessage(prefix + "§c❌ Ungültiger Effekt: §e" + effectName);
            player.sendMessage(prefix + "§7Gebe einen gültigen Effekt ein oder §e'fertig' §7zum Abschließen.");
            return;
        }

        try {
            int amplifier = Integer.parseInt(strength);
            if (amplifier < 1 || amplifier > 10) {
                player.sendMessage(prefix + "§c❌ Die Stärke muss zwischen 1 und 10 liegen.");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + "§c❌ Ungültige Stärke: §e" + strength);
            return;
        }

        // Effekt zur Session hinzufügen
        session.addEffect(effectName + ":" + strength);
        player.sendMessage(prefix + "§a✅ Effekt hinzugefügt: §b" + effectName + " (Stärke: " + strength + ") ✨");
        player.sendMessage(prefix + "§7Gebe einen weiteren Effekt ein oder §e'fertig' §7zum Abschließen.");
    }
}