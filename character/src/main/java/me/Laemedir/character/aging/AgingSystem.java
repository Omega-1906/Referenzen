package me.Laemedir.character.aging;

import me.Laemedir.character.MultiCharPlugin;
import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet das Alterungssystem der Charaktere.
 * Führt tägliche Überprüfungen durch und lässt Charaktere basierend auf ihrer Spielzeit altern.
 */
public class AgingSystem {
    private final MultiCharPlugin plugin;
    private final CoreAPIPlugin coreAPI;
    private final String prefix;
    private BukkitTask dailyCheckTask;

    // DateTimeFormatter für SQL-Datums-/Zeitformate
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // Zeitzone für Berlin
    private final ZoneId berlinZone = ZoneId.of("Europe/Berlin");

    public AgingSystem(MultiCharPlugin plugin, CoreAPIPlugin coreAPI) {
        this.plugin = plugin;
        this.coreAPI = coreAPI;
        this.prefix = "§8[§6Characters§8] ";

        // Starte den täglichen Check um Mitternacht
        startDailyAgeCheck();
    }

    /**
     * Setzt das Erst-Login-Datum für einen Charakter, falls noch nicht geschehen.
     *
     * @param characterId die ID des Charakters
     */
    public void setFirstLoginIfNeeded(int characterId) {
        String checkSql = "SELECT first_login FROM characters WHERE id = ?";

        coreAPI.queryAsync(checkSql, results -> {
            if (results.isEmpty()) {
                return;
            }

            Map<String, Object> data = results.get(0);
            Object firstLogin = data.get("first_login");

            // Wenn first_login noch nicht gesetzt ist, setze es auf das aktuelle Datum
            if (firstLogin == null) {
                String currentTime = LocalDateTime.now(berlinZone).format(dateTimeFormatter);
                String updateSql = "UPDATE characters SET first_login = ? WHERE id = ?";
                coreAPI.executeUpdateAsync(updateSql, currentTime, characterId);

                plugin.getLogger().info("First login date set for character ID: " + characterId);
            }
        }, characterId);
    }

    /**
     * Startet den täglichen Task zur Überprüfung der Charakter-Alterung (jeden Tag um Mitternacht).
     */
    private void startDailyAgeCheck() {
        // Berechne die Zeit bis zur nächsten Mitternacht in Berlin
        LocalDateTime now = LocalDateTime.now(berlinZone);
        LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
        long initialDelay = now.until(nextMidnight, ChronoUnit.SECONDS);

        // Konvertiere in Ticks (20 Ticks = 1 Sekunde)
        long initialDelayTicks = initialDelay * 20;

        // Starte den täglichen Task
        dailyCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    plugin.getLogger().info("Starting daily aging check at " +
                            LocalDateTime.now(berlinZone).format(dateTimeFormatter));
                    checkAllCharactersForAging();
                } catch (Exception e) {
                    plugin.getLogger().severe("Error in daily aging check: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskTimerAsynchronously(plugin, initialDelayTicks, 24 * 60 * 60 * 20); // Einmal täglich

        plugin.getLogger().info("Daily aging check scheduled. Next check in " + initialDelay +
                " seconds at " + nextMidnight.format(dateTimeFormatter) + " Berlin time.");
    }

    /**
     * Überprüft alle Charaktere auf ihre Alterung und aktualisiert diese gegebenenfalls.
     * Berücksichtigt die Zeit seit dem ersten Login.
     */
    public void checkAllCharactersForAging() {
        String sql = "SELECT id, name, player_uuid, first_login, age FROM characters WHERE first_login IS NOT NULL";

        coreAPI.queryAsync(sql, results -> {
            if (results.isEmpty()) {
                plugin.getLogger().info("No characters found for aging check");
                return;
            }

            LocalDateTime now = LocalDateTime.now(berlinZone);
            plugin.getLogger().info("Checking " + results.size() + " characters for aging");

            for (Map<String, Object> data : results) {
                int characterId = ((Number) data.get("id")).intValue();
                String name = (String) data.get("name");
                String playerUuidStr = (String) data.get("player_uuid");
                Object firstLoginObj = data.get("first_login");
                int currentAge = ((Number) data.get("age")).intValue();

                if (firstLoginObj == null) {
                    continue;
                }

                try {
                    // Parse das first_login-Datum je nach Typ
                    LocalDateTime firstLogin;
                    if (firstLoginObj instanceof LocalDateTime) {
                        firstLogin = (LocalDateTime) firstLoginObj;
                    } else if (firstLoginObj instanceof String) {
                        firstLogin = LocalDateTime.parse((String) firstLoginObj, dateTimeFormatter);
                    } else if (firstLoginObj instanceof java.sql.Timestamp) {
                        firstLogin = ((java.sql.Timestamp) firstLoginObj).toLocalDateTime();
                    } else {
                        plugin.getLogger().warning("Unexpected data type for first_login: " +
                                firstLoginObj.getClass().getName() + " for character " + name);
                        continue;
                    }

                    // Berechne die Anzahl der Monate seit dem ersten Login
                    long monthsSinceFirstLogin = ChronoUnit.MONTHS.between(firstLogin, now);
                    plugin.getLogger().info("Character " + name + " has been active for " +
                            monthsSinceFirstLogin + " months since " + firstLogin.format(dateTimeFormatter));

                    // Berechne, wie viele Jahre der Charakter altern sollte
                    int yearsToAge = (int) (monthsSinceFirstLogin);

                    if (yearsToAge > 0) {
                        // Überprüfe, ob ein Anti-Alterungs-Trank aktiv ist
                        checkAndProcessAging(characterId, name, playerUuidStr, yearsToAge, currentAge);
                    } else {
                        plugin.getLogger().info("Character " + name + " doesn't need to age yet");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error processing aging for character " + name + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Prüft auf aktive Anti-Aging-Tränke und führt den Alterungsprozess durch, falls kein Schutz besteht.
     *
     * @param characterId   die ID des Charakters
     * @param name          der Name des Charakters
     * @param playerUuidStr die UUID des Spielers als String
     * @param yearsToAge    die Anzahl der Jahre, die der Charakter altern soll
     * @param currentAge    das aktuelle Alter
     */
    private void checkAndProcessAging(int characterId, String name, String playerUuidStr, int yearsToAge, int currentAge) {
        String potionSql = "SELECT COUNT(*) AS count FROM character_aging_potions " +
                "WHERE character_id = ? AND expires_at > NOW()";

        coreAPI.queryAsync(potionSql, results -> {
            if (results.isEmpty()) {
                return;
            }

            int activePotion = ((Number) results.get(0).get("count")).intValue();

            if (activePotion > 0) {
                // Anti-Alterungs-Trank ist aktiv, keine Alterung
                plugin.getLogger().info("Character " + name + " has an active anti-aging potion. No aging applied.");
            } else {
                // Kein aktiver Trank, Charakter altert
                int newAge = currentAge + yearsToAge;

                // Aktualisiere das Alter in der Datenbank
                String updateSql = "UPDATE characters SET age = ? WHERE id = ?";
                coreAPI.executeUpdateAsync(updateSql, newAge, characterId);

                plugin.getLogger().info("Character " + name + " aged from " + currentAge + " to " + newAge);

                // Aktualisiere das first_login-Datum auf das aktuelle Datum
                String currentTime = LocalDateTime.now(berlinZone).format(dateTimeFormatter);
                String updateFirstLoginSql = "UPDATE characters SET first_login = ? WHERE id = ?";
                coreAPI.executeUpdateAsync(updateFirstLoginSql, currentTime, characterId);

                // Benachrichtige den Spieler, wenn er online ist
                try {
                    UUID playerUuid = UUID.fromString(playerUuidStr);
                    Player player = Bukkit.getPlayer(playerUuid);

                    if (player != null && player.isOnline() &&
                            name.equals(MultiCharPlugin.getActiveCharacter(playerUuid))) {

                        player.sendMessage(prefix + "§eDein Charakter §6" + name + " §eist um §6" +
                                yearsToAge + " §eJahr(e) gealtert und ist jetzt §6" + newAge + " §eJahre alt.");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error notifying player about aging: " + e.getMessage());
                }
            }
        }, characterId);
    }

    /**
     * Verarbeitet das Trinken eines Anti-Aging-Tranks durch einen Spieler.
     * Setzt den Schutz für einen Monat.
     *
     * @param player      der Spieler
     * @param characterId die ID des Charakters
     */
    public void drinkAntiAgingPotion(Player player, int characterId) {
        // Überprüfe, ob der Charakter bereits einen aktiven Trank hat
        String checkSql = "SELECT COUNT(*) AS count FROM character_aging_potions " +
                "WHERE character_id = ? AND expires_at > NOW()";

        coreAPI.queryAsync(checkSql, results -> {
            if (results.isEmpty()) {
                return;
            }

            int activePotion = ((Number) results.get(0).get("count")).intValue();

            if (activePotion > 0) {
                player.sendMessage(prefix + "§cDu hast bereits einen aktiven Anti-Alterungs-Trank!");
                return;
            }

            // Berechne das Ablaufdatum (1 Monat ab jetzt)
            LocalDateTime now = LocalDateTime.now(berlinZone);
            LocalDateTime expiresAt = now.plusMonths(1);

            String potionDate = now.format(dateTimeFormatter);
            String expiryDate = expiresAt.format(dateTimeFormatter);

            // Füge den Trank zur Datenbank hinzu
            String insertSql = "INSERT INTO character_aging_potions (character_id, potion_date, expires_at) " +
                    "VALUES (?, ?, ?)";

            coreAPI.executeUpdateAsync(insertSql, characterId, potionDate, expiryDate);

            player.sendMessage(prefix + "§aDu hast einen §bAnti-Alterungs-Trank §agetrunken! " +
                    "Dein Charakter wird für einen Monat (ein Jahr) nicht altern.");

        }, characterId);
    }

    /**
     * Stoppt alle laufenden Tasks des Alterungssystems.
     */
    public void shutdown() {
        if (dailyCheckTask != null) {
            dailyCheckTask.cancel();
        }
    }
}
