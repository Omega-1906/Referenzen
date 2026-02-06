package me.Laemedir.character.disguise;

import me.Laemedir.character.MultiCharPlugin;
import me.Laemedir.coreApi.CoreAPIPlugin;
import me.Laemedir.coreApi.debug.DebugManager;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.MiscDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.AgeableWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.CatWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.WolfWatcher;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet das Verwandlungssystem (Disguises) für Charaktere.
 * Zuständig für das Speichern, Laden und Anwenden von Verwandlungen.
 */
public class DisguiseManager {

    private final MultiCharPlugin plugin;
    private final CoreAPIPlugin coreAPI;
    private final DebugManager debugManager;

    // Speichert aktive Verwandlungen: UUID -> DisguiseData
    private final Map<UUID, DisguiseData> activeDisguises = new HashMap<>();
    private final java.util.Set<java.util.UUID> tempFlightGranted = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public DisguiseManager(MultiCharPlugin plugin, CoreAPIPlugin coreAPI) {
        this.plugin = plugin;
        this.coreAPI = coreAPI;
        this.debugManager = plugin.getDebugManager();
        createDisguiseTable();
        createActiveDisguiseTable();
    }

    private void createDisguiseTable() {
        String sql = "CREATE TABLE IF NOT EXISTS character_disguises ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "character_id INT NOT NULL, "
                + "disguise_id VARCHAR(50) NOT NULL, "
                + "disguise_size VARCHAR(10) DEFAULT 'normal', "
                + "granted_by VARCHAR(36), "
                + "granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE"
                + ")";
        try {
            coreAPI.executeUpdateSync(sql);
            if (debugManager != null) {
                debugManager.info("character", "Disguise", "Tabelle 'character_disguises' überprüft/erstellt");
            }
        } catch (Exception e) {
            if (debugManager != null) {
                debugManager.error("character", "Disguise", "Fehler beim Erstellen der Tabelle 'character_disguises'", e);
            }
        }
    }

    private void createActiveDisguiseTable() {
        String sql = "CREATE TABLE IF NOT EXISTS active_disguises ("
                + "character_id INT PRIMARY KEY, "
                + "disguise_id VARCHAR(50) NOT NULL, "
                + "disguise_size VARCHAR(10) DEFAULT 'normal', "
                + "activated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE"
                + ")";
        try {
            coreAPI.executeUpdateSync(sql);
            if (debugManager != null) {
                debugManager.info("character", "Disguise", "Tabelle 'active_disguises' überprüft/erstellt");
            }
        } catch (Exception e) {
            if (debugManager != null) {
                debugManager.error("character", "Disguise", "Fehler beim Erstellen der Tabelle 'active_disguises'", e);
            }
        }
    }

    /**
     * Lädt und wendet die aktive Verwandlung eines Charakters an (wird beim Charakterwechsel aufgerufen).
     *
     * @param player      der Spieler
     * @param characterId die ID des Charakters
     */
    public void loadActiveDisguise(Player player, int characterId) {
        String sql = "SELECT disguise_id, disguise_size, sub_type FROM active_disguises WHERE character_id = ?";
        coreAPI.queryAsync(sql, results -> {
            if (!results.isEmpty()) {
                Map<String, Object> row = results.get(0);
                String disguiseId = (String) row.get("disguise_id");
                String size = (String) row.get("disguise_size");
                String subType = (String) row.get("sub_type");

                // Verwandlung anwenden (ohne sie erneut in der DB zu speichern)
                applyDisguiseInternal(player, disguiseId, size, subType, false);

                // Automatisches Fly und Nightvision für Ender_Dragon
                if ("Ender_Dragon".equalsIgnoreCase(disguiseId)) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
                }

                if (debugManager != null) {
                    debugManager.info("character", "Disguise", "Aktive Verwandlung für " + player.getName() + " wiederhergestellt: " + disguiseId + (subType != null ? " (" + subType + ")" : ""));
                }
            }
        }, characterId);
    }

    /**
     * Gibt den Anzeigenamen der aktuellen Verwandlung zurück.
     *
     * @param player der Spieler
     * @return der Anzeigename oder null, wenn nicht verwandelt
     */
    public String getCurrentDisguise(Player player) {
        UUID playerId = player.getUniqueId();
        if (!activeDisguises.containsKey(playerId)) {
            return null;
        }

        DisguiseData data = activeDisguises.get(playerId);
        return getDisguiseDisplayName(data.getDisguiseId()) + " (" + getSizeDisplay(data.getSize()) + ")";
    }

    private String getDisguiseDisplayName(String disguiseId) {
        switch (disguiseId.toLowerCase()) {
            case "cow": return "Kuh";
            case "pig": return "Schwein";
            case "chicken": return "Huhn";
            case "sheep": return "Schaf";
            case "wolf": return "Wolf";
            case "cat": return "Katze";
            case "horse": return "Pferd";
            case "villager": return "Dorfbewohner";
            case "zombie": return "Zombie";
            case "skeleton": return "Skelett";
            case "creeper": return "Creeper";
            case "spider": return "Spinne";
            case "enderman": return "Enderman";
            case "blaze": return "Blaze";
            case "ghast": return "Ghast";
            case "slime": return "Schleim";
            case "magma_cube": return "Magmawürfel";
            case "bat": return "Fledermaus";
            case "squid": return "Tintenfisch";
            case "rabbit": return "Hase";
            case "armor_stand": return "Rüstungsständer";
            case "item_frame": return "Rahmen";
            case "painting": return "Gemälde";
            case "fox": return "Fuchs";
            case "bee": return "Biene";
            case "turtle": return "Schildkröte";
            case "panda": return "Panda";
            case "llama": return "Lama";
            case "dolphin": return "Delfin";
            case "parrot": return "Papagei";
            case "ocelot": return "Ozelot";
            default: return disguiseId;
        }
    }

    private String getSizeDisplay(String size) {
        switch (size.toLowerCase()) {
            case "small": return "Klein";
            case "big": return "Groß";
            case "normal":
            default: return "Normal";
        }
    }

    /**
     * Prüft, ob ein Spieler eine aktive Verwandlung hat.
     *
     * @param player der Spieler
     * @return true, wenn verwandelt
     */
    public boolean hasActiveDisguise(Player player) {
        return isDisguised(player.getUniqueId());
    }

    /**
     * Entfernt die aktive Verwandlung aus der Datenbank (wird beim Charakterwechsel aufgerufen).
     * Speichert den aktuellen Status, um ihn später wiederherzustellen.
     *
     * @param player der Spieler
     */
    public void saveActiveDisguiseState(Player player) {
        UUID playerUUID = player.getUniqueId();
        int characterId = MultiCharPlugin.getActiveCharacterId(playerUUID);

        if (characterId == -1) return;

        if (isDisguised(playerUUID)) {
            // Spieler ist verwandelt - speichere den aktuellen Zustand
            DisguiseData data = activeDisguises.get(playerUUID);
            String sql = "INSERT INTO active_disguises (character_id, disguise_id, disguise_size) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE disguise_id = VALUES(disguise_id), disguise_size = VALUES(disguise_size)";
            coreAPI.executeUpdateAsync(sql, characterId, data.getDisguiseId(), data.getSize());
        } else {
            // Spieler ist nicht verwandelt - entferne aus der DB
            String sql = "DELETE FROM active_disguises WHERE character_id = ?";
            coreAPI.executeUpdateAsync(sql, characterId);
        }
    }

    /**
     * Verwandelt einen Spieler oder hebt die Verwandlung auf.
     *
     * @param player     der Spieler
     * @param disguiseId die ID der Verwandlung
     * @param size       die Größe (small, normal, big)
     * @param subType    der Untertyp (z.B. Wolf-Variante, Katzen-Typ)
     */
    public void toggleDisguise(Player player, String disguiseId, String size, String subType) {
        UUID playerUUID = player.getUniqueId();

        if (isDisguised(playerUUID)) {
            // Verwandlung aufheben
            removeDisguise(player);
        } else {
            // Verwandeln
            applyDisguise(player, disguiseId, size, subType);
        }
    }

    /**
     * Wendet eine Verwandlung auf einen Spieler an.
     *
     * @param player     der Spieler
     * @param disguiseId die ID der Verwandlung
     * @param size       die Größe
     * @param subType    der Untertyp
     */
    public void applyDisguise(Player player, String disguiseId, String size, String subType) {
        applyDisguiseInternal(player, disguiseId, size, subType, true);
    }

    /**
     * Interne Methode zum Anwenden einer Verwandlung.
     */
    private void applyDisguiseInternal(Player player, String disguiseId, String size, String subType, boolean saveToDatabase) {
        UUID playerUUID = player.getUniqueId();

        if (isDisguised(playerUUID)) {
            if (saveToDatabase) player.sendMessage("§cDu bist bereits verwandelt!");
            return;
        }

        try {
            DisguiseType disguiseType = parseDisguiseId(disguiseId);
            if (disguiseType == null) {
                if (saveToDatabase) player.sendMessage("§cUngültige Disguise-ID: " + disguiseId);
                return;
            }

            Disguise disguise = createDisguise(disguiseType, size, subType);
            if (disguise != null) {
                if ("Ender_Dragon".equalsIgnoreCase(disguiseId)) {
                    // Spieler-Sounds statt Drachen-Sounds
                    disguise.setReplaceSounds(false);
                    // oder: ALLES stumm schalten (auch Schritte/Hurt/etc.)
                    // disguise.getWatcher().setSilent(true);
                }

                // VOR dem Disguise: Flugzustand snappen
                snapshotFlight(player);

                // Disguise anwenden
                DisguiseAPI.disguiseToAll(player, disguise);

                if (!"Ender_Dragon".equalsIgnoreCase(disguiseId)) {
                    // Nur bei normalen Disguises: alten Zustand sofort + verzögert wiederherstellen
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) restoreFlight(player);
                    });
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) restoreFlight(player);
                    }, 5L);
                } else {
                    // Ender-Drache -> Fly geben; nur als "temporär" markieren, wenn es vorher AUS war
                    boolean wasAllowedBefore = false;
                    FlightState before = savedFlight.get(playerUUID);
                    if (before != null) wasAllowedBefore = before.allowFlight;

                    player.setAllowFlight(true);
                    player.setFlying(true);
                    if (!wasAllowedBefore && player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                        tempFlightGranted.add(playerUUID);
                    }
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
                }

                // lokale Map + DB + Message wie gehabt …
                DisguiseData data = new DisguiseData(disguiseId, size, subType, disguiseType);
                activeDisguises.put(playerUUID, data);

                if (saveToDatabase) {
                    int characterId = MultiCharPlugin.getActiveCharacterId(playerUUID);
                    if (characterId != -1) {
                        String sql = "INSERT INTO active_disguises (character_id, disguise_id, disguise_size, sub_type) VALUES (?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE disguise_id = VALUES(disguise_id), disguise_size = VALUES(disguise_size), sub_type = VALUES(sub_type)";
                        coreAPI.executeUpdateAsync(sql, characterId, disguiseId, size, subType);
                    }

                    String article = getArticle(disguiseId);
                    String disguiseName = getDisguiseDisplayName(disguiseId);
                    String sizeColor = getSizeColor(size);
                    String sizeDisplay = getSizeDisplay(size);
                    String subTypeText = subType != null ? " (" + getSubTypeDisplay(disguiseId, subType) + ")" : "";

                    player.sendMessage("§a§l✓ §aDu wurdest in " + article + " " + sizeColor + "§l" + sizeDisplay + " §6§l" + disguiseName + subTypeText + " §averwandelt!");
                }
            } else {
                if (saveToDatabase) player.sendMessage("§cFehler beim Erstellen der Verwandlung!");
            }
        } catch (Exception e) {
            if (saveToDatabase) {
                player.sendMessage("§cFehler bei der Verwandlung: " + e.getMessage());
            }
            // Stacktrace sauber loggen:
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Disguise-Fehler", e);
        }
    }

    /**
     * Entfernt die Verwandlung eines Spielers.
     *
     * @param player der Spieler
     */
    public void removeDisguise(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (!isDisguised(playerUUID)) {
            player.sendMessage("§cDu bist nicht verwandelt!");
            return;
        }

        try {
            DisguiseAPI.undisguiseToAll(player);

            // Drachen: nur Night Vision entfernen
            DisguiseData data = activeDisguises.get(playerUUID);
            if (data != null && "Ender_Dragon".equalsIgnoreCase(data.getDisguiseId())) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }

            // Nach dem Undisguise: Fly gemäß VORHERIGEM Zustand setzen
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                org.bukkit.GameMode gm = player.getGameMode();
                boolean creativeOrSpec = (gm == org.bukkit.GameMode.CREATIVE || gm == org.bukkit.GameMode.SPECTATOR);
                FlightState before = savedFlight.get(playerUUID);

                // Wenn Fly NUR durch die Verwandlung kam (tempFlightGranted=true) und vorher aus war -> jetzt aus
                if (!creativeOrSpec && tempFlightGranted.remove(playerUUID) && (before == null || !before.allowFlight)) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                } else {
                    // sonst: ursprünglichen Zustand wiederherstellen (/fly-User behalten es)
                    restoreFlight(player);
                }

                savedFlight.remove(playerUUID);
            });

            // Map/DB/messages wie gehabt
            activeDisguises.remove(playerUUID);
            int characterId = MultiCharPlugin.getActiveCharacterId(playerUUID);
            if (characterId != -1) {
                String sql = "DELETE FROM active_disguises WHERE character_id = ?";
                coreAPI.executeUpdateAsync(sql, characterId);
            }

            player.sendMessage("§aDeine Verwandlung wurde aufgehoben!");

        } catch (Exception e) {
            player.sendMessage("§cFehler beim Aufheben der Verwandlung: " + e.getMessage());
            plugin.getLogger().severe("Undisguise-Fehler: " + e.getMessage());
        }
    }

    /**
     * Gibt einem Spieler eine Verwandlung (fügt sie zum Besitz hinzu).
     *
     * @param target      der Zielspieler
     * @param disguiseId  die ID der Verwandlung
     * @param size        die Größe
     * @param subType     der Untertyp
     * @param grantedBy   der Spieler, der die Verwandlung gibt
     */
    public void giveDisguise(Player target, String disguiseId, String size, String subType, Player grantedBy) {
        int characterId = MultiCharPlugin.getActiveCharacterId(target.getUniqueId());

        if (characterId == -1) {
            grantedBy.sendMessage("§cDer Spieler hat keinen aktiven Charakter!");
            return;
        }

        // Prüfe ob der Spieler die Disguise bereits hat
        String checkSql = "SELECT COUNT(*) as count FROM character_disguises WHERE character_id = ? AND disguise_id = ? AND disguise_size = ? AND (sub_type = ? OR (sub_type IS NULL AND ? IS NULL))";
        coreAPI.queryAsync(checkSql, results -> {
            if (!results.isEmpty()) {
                int count = ((Number) results.get(0).get("count")).intValue();
                if (count > 0) {
                    grantedBy.sendMessage("§cDer Spieler hat diese Verwandlung bereits!");
                    return;
                }
            }

            // Füge Disguise zur Datenbank hinzu
            String insertSql = "INSERT INTO character_disguises (character_id, disguise_id, disguise_size, sub_type, granted_by) VALUES (?, ?, ?, ?, ?)";
            coreAPI.executeUpdateAsync(insertSql, characterId, disguiseId, size, subType, grantedBy.getUniqueId().toString());

            String article = getArticle(disguiseId);
            String disguiseName = getDisguiseDisplayName(disguiseId);
            String subTypeText = subType != null ? " (" + getSubTypeDisplay(disguiseId, subType) + ")" : "";

            grantedBy.sendMessage("§a§l✓ §aDu hast §e" + target.getName() + " §adie Verwandlung " + article + " §6§l" + disguiseName + subTypeText + " §agegeben!");
            target.sendMessage("§a§l✓ §aDu hast die Verwandlung " + article + " §6§l" + disguiseName + subTypeText + " §aerhalten! Nutze §6/Verwandlung §azum Verwandeln!");
        }, characterId, disguiseId, size, subType, subType);
    }

    /**
     * Entfernt eine Verwandlung aus dem Besitz eines Spielers anhand des Disguise-Keys.
     *
     * @param actor       der ausführende Spieler/Sender
     * @param target      der betroffene Spieler
     * @param disguiseKey der Schlüssel der Verwandlung
     */
    public void removeOwnedDisguiseByKey(CommandSender actor, Player target, String disguiseKey) {
        int characterId = me.Laemedir.character.MultiCharPlugin.getActiveCharacterId(target.getUniqueId());
        if (characterId == -1) {
            actor.sendMessage("§cKein aktiver Charakter gefunden.");
            return;
        }

        String selectSql = "SELECT id FROM character_disguises WHERE character_id = ? AND disguise_id = ? LIMIT 1";
        coreAPI.queryAsync(selectSql, rows -> {
            if (rows.isEmpty() || rows.get(0).get("id") == null) {
                actor.sendMessage("§cDiese Verwandlung besitzt der Charakter nicht §7(disguise_id='§e" + disguiseKey + "§7').");
                return;
            }

            // Wenn diese Verwandlung gerade aktiv ist -> zuerst deaktivieren
            isActiveDisguise(characterId, disguiseKey, isActive -> {
                if (isActive) {
                    // auf den Main-Thread – entfernt aktiven Zustand & Eintrag aus active_disguises
                    plugin.getServer().getScheduler().runTask(plugin, () -> removeDisguise(target));
                }

                int recordId = ((Number) rows.get(0).get("id")).intValue();
                String deleteSql = "DELETE FROM character_disguises WHERE id = ?";
                coreAPI.executeUpdateWithCallbackAsync(deleteSql, ok -> {
                    if (ok) {
                        actor.sendMessage("§a§l✓ §aVerwandlung '§e" + disguiseKey + "§a' wurde aus dem Besitz entfernt.");
                        if (!actor.equals(target)) {
                            target.sendMessage("§eEine Verwandlung (§6" + disguiseKey + "§e) wurde dir von einem Teammitglied entfernt.");
                        }
                    } else {
                        actor.sendMessage("§cEntfernen fehlgeschlagen §7(disguise_id='§e" + disguiseKey + "§7').");
                    }
                }, recordId);
            });
        }, characterId, disguiseKey);
    }

    /**
     * Entfernt eine Verwandlung aus dem Besitz eines Spielers anhand der Datenbank-ID.
     *
     * @param actor    der ausführende Spieler/Sender
     * @param target   der betroffene Spieler
     * @param recordId die ID des Datenbank-Eintrags
     */
    public void removeOwnedDisguiseByRecordId(CommandSender actor, Player target, int recordId) {
        int characterId = me.Laemedir.character.MultiCharPlugin.getActiveCharacterId(target.getUniqueId());
        if (characterId == -1) {
            actor.sendMessage("§cKein aktiver Charakter gefunden.");
            return;
        }

        // zuerst ermitteln, welche disguise_id das ist (für Feedback + Aktiv-Check)
        String infoSql = "SELECT disguise_id FROM character_disguises WHERE id = ? AND character_id = ? LIMIT 1";
        coreAPI.queryAsync(infoSql, rows -> {
            if (rows.isEmpty() || rows.get(0).get("disguise_id") == null) {
                actor.sendMessage("§cKein Besitz-Eintrag mit ID §e#" + recordId + " §cgefunden.");
                return;
            }

            String disguiseKey = String.valueOf(rows.get(0).get("disguise_id"));

            isActiveDisguise(characterId, disguiseKey, isActive -> {
                if (isActive) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> removeDisguise(target));
                }

                String deleteSql = "DELETE FROM character_disguises WHERE id = ?";
                coreAPI.executeUpdateWithCallbackAsync(deleteSql, ok -> {
                    if (ok) {
                        actor.sendMessage("§a§l✓ §aBesitz-Eintrag §e#" + recordId + " §a(§6" + disguiseKey + "§a) wurde entfernt.");
                        if (!actor.equals(target)) {
                            target.sendMessage("§eEine Verwandlung (§6" + disguiseKey + "§e) wurde dir von einem Teammitglied entfernt.");
                        }
                    } else {
                        actor.sendMessage("§cEntfernen von Besitz-Eintrag §e#" + recordId + " §cfehlgeschlagen.");
                    }
                }, recordId);
            });
        }, recordId, characterId);
    }

    /**
     * Prüft, ob für den Charakter aktuell genau diese disguise_id aktiv ist.
     */
    private void isActiveDisguise(int characterId, String disguiseKey, java.util.function.Consumer<Boolean> cb) {
        String sql = "SELECT 1 FROM active_disguises WHERE character_id = ? AND disguise_id = ? LIMIT 1";
        coreAPI.queryAsync(sql, rows -> cb.accept(!rows.isEmpty()), characterId, disguiseKey);
    }

    /**
     * Prüft, ob ein Spieler verwandelt ist (interner Cache).
     *
     * @param playerUUID die UUID des Spielers
     * @return true, wenn verwandelt
     */
    public boolean isDisguised(UUID playerUUID) {
        return activeDisguises.containsKey(playerUUID);
    }

    /**
     * Prüft asynchron, ob ein Spieler eine bestimmte Verwandlung besitzt.
     *
     * @param player     der Spieler
     * @param disguiseId die ID der Verwandlung
     * @param callback   Callback mit dem Ergebnis
     */
    public void hasDisguise(Player player, String disguiseId, DisguiseCheckCallback callback) {
        int characterId = MultiCharPlugin.getActiveCharacterId(player.getUniqueId());

        if (characterId == -1) {
            callback.onResult(false);
            return;
        }

        String sql = "SELECT COUNT(*) as count FROM character_disguises WHERE character_id = ? AND disguise_id = ?";
        coreAPI.queryAsync(sql, results -> {
            if (!results.isEmpty()) {
                int count = ((Number) results.get(0).get("count")).intValue();
                callback.onResult(count > 0);
            } else {
                callback.onResult(false);
            }
        }, characterId, disguiseId);
    }

    /**
     * Holt asynchron alle Verwandlungen eines Spielers.
     *
     * @param player   der Spieler
     * @param callback Callback mit Array der Verwandlungen (Format: "id:size")
     */
    public void getPlayerDisguises(Player player, DisguiseListCallback callback) {
        int characterId = MultiCharPlugin.getActiveCharacterId(player.getUniqueId());

        if (characterId == -1) {
            callback.onResult(new String[0]);
            return;
        }

        String sql = "SELECT disguise_id, disguise_size FROM character_disguises WHERE character_id = ?";
        coreAPI.queryAsync(sql, results -> {
            String[] disguises = new String[results.size()];
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> row = results.get(i);
                String id = (String) row.get("disguise_id");
                String size = (String) row.get("disguise_size");
                disguises[i] = id + ":" + size;
            }
            callback.onResult(disguises);
        }, characterId);
    }

    private DisguiseType parseDisguiseId(String disguiseId) {
        try {
            return DisguiseType.valueOf(disguiseId.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unbekannte Disguise-ID: " + disguiseId);
            return null;
        }
    }

    private Disguise createDisguise(DisguiseType disguiseType, String size, String subType) {
        try {
            Disguise disguise;

            if (disguiseType.isMob()) {
                disguise = new MobDisguise(disguiseType);
            } else {
                disguise = new MiscDisguise(disguiseType);
            }

            // Größe anwenden für Mob-Disguises
            if (disguise instanceof MobDisguise) {
                MobDisguise mobDisguise = (MobDisguise) disguise;

                switch (size.toLowerCase()) {
                    case "small":
                        // Prüfe ob das Entity ein Baby sein kann
                        if (mobDisguise.getWatcher() instanceof AgeableWatcher) {
                            AgeableWatcher ageableWatcher = (AgeableWatcher) mobDisguise.getWatcher();
                            ageableWatcher.setBaby(true);
                        }
                        break;
                    case "big":
                        // Für größere Disguises - implementiere falls nötig
                        break;
                    case "normal":
                    default:
                        // Standard-Größe
                        break;
                }

                // SubType anwenden
                if (subType != null) {
                    applySubType(mobDisguise, disguiseType, subType);
                }
            }

            return disguise;

        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Erstellen der Disguise: " + e.getMessage());
            return null;
        }
    }

    private void applySubType(MobDisguise disguise, DisguiseType disguiseType, String subType) {
        if (subType == null) return;

        try {
            if (disguiseType == DisguiseType.CAT) {
                CatWatcher catWatcher = (CatWatcher) disguise.getWatcher();
                try {
                    // Verwende moderne API ohne deprecated valueOf
                    for (Cat.Type catType : Cat.Type.values()) {
                        if (catType.name().equalsIgnoreCase(subType)) {
                            catWatcher.setType(catType);
                            return;
                        }
                    }
                    plugin.getLogger().warning("Unbekannter Katzen-Typ: " + subType);
                } catch (Exception e) {
                    plugin.getLogger().warning("Fehler beim Setzen des Katzen-Typs: " + e.getMessage());
                }
            } else if (disguiseType == DisguiseType.WOLF) {
                WolfWatcher wolfWatcher = (WolfWatcher) disguise.getWatcher();
                try {
                    // Korrekte Wolf.Variant Implementierung mit Registry
                    Wolf.Variant wolfVariant = getWolfVariantByName(subType);

                    if (wolfVariant != null) {
                        wolfWatcher.setVariant(wolfVariant);
                    } else {
                        plugin.getLogger().warning("Unbekannte Wolf-Variante: " + subType);
                        // Fallback zu Collar-Farbe
                        setWolfColorFromVariant(wolfWatcher, subType);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Fehler beim Setzen der Wolf-Variante: " + e.getMessage());
                    // Fallback zu Collar-Farbe
                    setWolfColorFromVariant(wolfWatcher, subType);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Anwenden des SubTypes: " + e.getMessage());
        }
    }

    /**
     * Hilfsmethode, um Wolf.Variant anhand des Namens zu finden.
     */
    private Wolf.Variant getWolfVariantByName(String name) {
        try {
            // Versuche direkte Konstanten-Zugriffe
            switch (name.toUpperCase()) {
                case "PALE": return Wolf.Variant.PALE;
                case "SPOTTED": return Wolf.Variant.SPOTTED;
                case "SNOWY": return Wolf.Variant.SNOWY;
                case "BLACK": return Wolf.Variant.BLACK;
                case "ASHEN": return Wolf.Variant.ASHEN;
                case "RUSTY": return Wolf.Variant.RUSTY;
                case "WOODS": return Wolf.Variant.WOODS;
                case "CHESTNUT": return Wolf.Variant.CHESTNUT;
                case "STRIPED": return Wolf.Variant.STRIPED;
                default:
                    // Versuche Registry-Zugriff für custom Varianten
                    NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
                    return Registry.WOLF_VARIANT.get(key);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Abrufen der Wolf-Variante: " + e.getMessage());
            return null;
        }
    }

    private void setWolfColorFromVariant(WolfWatcher wolfWatcher, String variant) {
        try {
            DyeColor color;
            switch (variant.toLowerCase()) {
                case "black":
                    color = DyeColor.BLACK;
                    break;
                case "ashen":
                    color = DyeColor.LIGHT_GRAY;
                    break;
                case "chestnut":
                    color = DyeColor.BROWN;
                    break;
                case "pale":
                    color = DyeColor.WHITE;
                    break;
                case "rusty":
                    color = DyeColor.ORANGE;
                    break;
                case "snowy":
                    color = DyeColor.WHITE;
                    break;
                case "spotted":
                    color = DyeColor.GRAY;
                    break;
                case "striped":
                    color = DyeColor.YELLOW;
                    break;
                case "woods":
                    color = DyeColor.GREEN;
                    break;
                default:
                    color = DyeColor.RED;
                    break;
            }
            wolfWatcher.setCollarColor(color);
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Setzen der Wolf-Farbe: " + e.getMessage());
        }
    }

    private String getSubTypeDisplay(String disguiseId, String subType) {
        if (disguiseId.equalsIgnoreCase("cat")) {
            return getCatTypeDisplay(subType);
        } else if (disguiseId.equalsIgnoreCase("wolf")) {
            return getWolfVariantDisplay(subType);
        }
        return capitalizeFirst(subType);
    }

    private String getWolfVariantDisplay(String variant) {
        return capitalizeFirst(variant);
    }

    private String getCatTypeDisplay(String type) {
        return capitalizeFirst(type);
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    // Callback-Interfaces
    public interface DisguiseCheckCallback {
        void onResult(boolean hasDisguise);
    }

    public interface DisguiseListCallback {
        void onResult(String[] disguises);
    }

    // Flight-Status sichern/wiederherstellen
    private static final class FlightState {
        final boolean allowFlight; final boolean flying; final float flySpeed;
        FlightState(boolean a, boolean f, float s) { this.allowFlight=a; this.flying=f; this.flySpeed=s; }
    }
    private final java.util.Map<java.util.UUID, FlightState> savedFlight = new java.util.concurrent.ConcurrentHashMap<>();

    private void snapshotFlight(org.bukkit.entity.Player p) {
        savedFlight.put(p.getUniqueId(), new FlightState(p.getAllowFlight(), p.isFlying(), p.getFlySpeed()));
    }
    private void restoreFlight(org.bukkit.entity.Player p) {
        org.bukkit.GameMode gm = p.getGameMode();
        if (gm == org.bukkit.GameMode.CREATIVE || gm == org.bukkit.GameMode.SPECTATOR) {
            p.setAllowFlight(true);
            return;
        }
        FlightState st = savedFlight.get(p.getUniqueId());
        if (st != null && st.allowFlight) p.setAllowFlight(true); else p.setAllowFlight(false);
        if (p.getAllowFlight() && st != null && st.flying) p.setFlying(true); else p.setFlying(false);
        if (st != null) { try { p.setFlySpeed(st.flySpeed); } catch (IllegalArgumentException ignored) {} }
    }

    private void ensureFlightConsistent(org.bukkit.entity.Player p) {
        org.bukkit.GameMode gm = p.getGameMode();
        if (gm == org.bukkit.GameMode.CREATIVE || gm == org.bukkit.GameMode.SPECTATOR) p.setAllowFlight(true);
    }

    // Erweitere die DisguiseData-Klasse:
    private static class DisguiseData {
        private final String disguiseId;
        private final String size;
        private final String subType;
        private final DisguiseType disguiseType;

        public DisguiseData(String disguiseId, String size, String subType, DisguiseType disguiseType) {
            this.disguiseId = disguiseId;
            this.size = size;
            this.subType = subType;
            this.disguiseType = disguiseType;
        }

        public String getDisguiseId() { return disguiseId; }
        public String getSize() { return size; }
        public String getSubType() { return subType; }
        public DisguiseType getDisguiseType() { return disguiseType; }
    }

    private String getArticle(String disguiseId) {
        switch (disguiseId.toLowerCase()) {
            case "cow": return "eine";
            case "pig": return "ein";
            case "chicken": return "ein";
            case "sheep": return "ein";
            case "wolf": return "einen";
            case "cat": return "eine";
            case "horse": return "ein";
            case "villager": return "einen";
            case "zombie": return "einen";
            case "skeleton": return "ein";
            case "creeper": return "einen";
            case "spider": return "eine";
            case "enderman": return "einen";
            case "blaze": return "einen";
            case "ghast": return "einen";
            case "slime": return "einen";
            case "magma_cube": return "einen";
            case "bat": return "eine";
            case "squid": return "einen";
            case "rabbit": return "einen";
            case "armor_stand": return "einen";
            case "item_frame": return "einen";
            case "painting": return "ein";
            case "fox": return "einen";
            case "bee": return "eine";
            case "turtle": return "eine";
            case "panda": return "einen";
            case "llama": return "ein";
            case "dolphin": return "einen";
            case "parrot": return "einen";
            case "ocelot": return "einen";
            default: return "ein";
        }
    }

    private String getSizeColor(String size) {
        switch (size.toLowerCase()) {
            case "small": return "§9";      // Dunkelblau
            case "big": return "§4";        // Dunkelrot
            case "normal":
            default: return "§2";           // Dunkelgrün
        }
    }
}
