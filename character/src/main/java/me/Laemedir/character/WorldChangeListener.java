package me.Laemedir.character;

import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.stream.Collectors;

public class WorldChangeListener implements Listener {

    private final MultiCharPlugin plugin;
    private final RaceManager raceManager;

    public WorldChangeListener(MultiCharPlugin plugin, RaceManager raceManager) {
        this.plugin = plugin;
        this.raceManager = raceManager;
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World fromWorld = event.getFrom();
        World toWorld = player.getWorld();

        // NEU: Betritt Farmwelt oder Unterwelt -> alle rassenbezogenen Effekte entfernen (inkl. Unsichtbarkeit)
        if (isFarmOrNether(toWorld)) {
            removeRaceEffects(player, true); // true = auch INVISIBILITY entfernen
            return;
        }

        // Verlassen der RP-Welt -> nur rassenbezogene Effekte entfernen (Unsichtbarkeit bleibt)
        if ("world".equals(fromWorld.getName()) && !"world".equals(toWorld.getName())) {
            removeRaceEffects(player, false);
            return;
        }

        // Betreten der RP-Welt -> Rasseneffekte anwenden (sofort + kurze Nachsicherung)
        if (!"world".equals(fromWorld.getName()) && "world".equals(toWorld.getName())) {
            applyRaceEffectsImmediateAndSafe(player);
        }
    }

    /** Farmwelt oder Nether? (Unterwelt) */
    private boolean isFarmOrNether(World world) {
        if (world == null) return false;

        // Nether sicher erkennen
        if (world.getEnvironment() == World.Environment.NETHER) return true;

        // Farmwelt per Name (ggf. anpassen, falls dein Weltname anders ist)
        String name = world.getName();
        return "Farmwelt".equalsIgnoreCase(name);
    }

    /**
     * Entfernt NUR die Effekte, die durch die Rasse vergeben werden.
     * @param includeInvisibility true -> auch INVISIBILITY entfernen; false -> INVISIBILITY behalten
     */
    private void removeRaceEffects(Player player, boolean includeInvisibility) {
        UUID playerUUID = player.getUniqueId();
        String activeCharName = MultiCharPlugin.getActiveCharacter(playerUUID);
        if (activeCharName == null) return;

        plugin.coreAPI.queryAsync(
                "SELECT c.race_id, r.effects FROM characters c " +
                        "LEFT JOIN races r ON c.race_id = r.id " +
                        "WHERE c.player_uuid = ? AND c.name = ?",
                results -> {
                    if (results == null || results.isEmpty()) return;

                    Object effObj = results.get(0).get("effects");
                    if (effObj == null) return;

                    String effectsStr = String.valueOf(effObj);
                    if (effectsStr.isEmpty()) return;

                    Set<PotionEffectType> raceTypes = extractEffectTypes(effectsStr);
                    if (raceTypes.isEmpty()) return;

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline()) return;

                        for (PotionEffectType type : raceTypes) {
                            if (type == null) continue;
                            if (!includeInvisibility && type.equals(PotionEffectType.INVISIBILITY)) continue;
                            if (player.hasPotionEffect(type)) {
                                player.removePotionEffect(type);
                            }
                        }
                    });
                },
                playerUUID.toString(), activeCharName
        );
    }

    /**
     * Wendet die Rasseneffekte sofort an und führt nach wenigen Ticks eine zweite Anwendung durch,
     * falls TPT/Wechsel den Gamemode direkt danach umstellt.
     */
    private void applyRaceEffectsImmediateAndSafe(Player player) {
        UUID playerUUID = player.getUniqueId();
        String activeCharName = MultiCharPlugin.getActiveCharacter(playerUUID);
        if (activeCharName == null) return;

        plugin.coreAPI.queryAsync(
                "SELECT c.race_id, r.effects FROM characters c " +
                        "LEFT JOIN races r ON c.race_id = r.id " +
                        "WHERE c.player_uuid = ? AND c.name = ?",
                results -> {
                    if (results == null || results.isEmpty()) return;

                    Object effObj = results.get(0).get("effects");
                    if (effObj == null) return;

                    String effectsStr = String.valueOf(effObj);
                    if (effectsStr.isEmpty()) return;

                    List<String> effectsList = plugin.parseEffects(effectsStr);

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline()) return;
                        if (!"world".equals(player.getWorld().getName())) return;
                        plugin.applyEffects(player, effectsList);
                    });

                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline()) return;
                        if (!"world".equals(player.getWorld().getName())) return;
                        plugin.applyEffects(player, effectsList);
                    }, 10L);
                },
                playerUUID.toString(), activeCharName
        );
    }

    /**
     * Robust-Parser: Nimmt ein Effects-String wie "SPEED:2:999999, STRENGTH:1:999999"
     * und zieht nur die Typen (SPEED, STRENGTH) heraus.
     */
    private Set<PotionEffectType> extractEffectTypes(String effectsStr) {
        if (effectsStr == null || effectsStr.trim().isEmpty()) return Collections.emptySet();

        Set<PotionEffectType> result = new HashSet<>();

        // NEU: sowohl Komma als auch Semikolon als Trenner
        String[] tokens = effectsStr.split("[,;]");

        for (String token : tokens) {
            if (token == null) continue;
            token = token.trim();
            if (token.isEmpty()) continue;

            String[] parts = token.split(":");
            if (parts.length == 0) continue;

            String rawName = parts[0] == null ? "" : parts[0].trim();
            if (rawName.isEmpty()) continue;

            PotionEffectType type = null;

            // 1) Legacy-/Paper-Mapping ("SPEED", "JUMP", "JUMP_BOOST" je nach Mapping)
            try {
                type = PotionEffectType.getByName(rawName.toUpperCase(Locale.ROOT));
            } catch (Throwable ignored) {}

            // 2) Fallback über NamespacedKey (z. B. "minecraft:jump_boost" oder "jump_boost")
            if (type == null) {
                try {
                    NamespacedKey key = rawName.contains(":")
                            ? NamespacedKey.fromString(rawName.toLowerCase(Locale.ROOT))
                            : NamespacedKey.minecraft(rawName.toLowerCase(Locale.ROOT));
                    if (key != null) type = PotionEffectType.getByKey(key);
                } catch (Throwable ignored) {}
            }

            if (type != null) result.add(type);
        }
        return result;
    }
}