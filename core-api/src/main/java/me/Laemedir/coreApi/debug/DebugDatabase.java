package me.Laemedir.coreApi.debug;

import me.Laemedir.coreApi.CoreAPIPlugin;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

public class DebugDatabase {
    private final CoreAPIPlugin plugin;

    public DebugDatabase(CoreAPIPlugin plugin) {
        this.plugin = plugin;
    }

    public void createTables() {
        // Debug Logs Tabelle
        plugin.executeUpdateAsync(
                "CREATE TABLE IF NOT EXISTS debug_logs (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "plugin_name VARCHAR(50) NOT NULL," +
                        "level VARCHAR(20) NOT NULL," +
                        "category VARCHAR(100)," +
                        "message TEXT NOT NULL," +
                        "stack_trace TEXT," +
                        "player_id CHAR(36)," +
                        "player_name VARCHAR(64)," +
                        "timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "INDEX idx_plugin (plugin_name)," +
                        "INDEX idx_level (level)," +
                        "INDEX idx_category (category)," +
                        "INDEX idx_timestamp (timestamp)," +
                        "INDEX idx_player (player_id)" +
                        ")"
        );

        plugin.getLogger().info("[DebugSystem] Datenbanktabellen erstellt/überprüft");
    }

    /**
     * Speichert einen Debug-Eintrag in der Datenbank
     */
    public void logDebug(String pluginName, DebugLevel level, String category, 
                        String message, String stackTrace, UUID playerId, String playerName) {
        plugin.executeUpdateAsync(
                "INSERT INTO debug_logs (plugin_name, level, category, message, stack_trace, player_id, player_name) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                pluginName,
                level.name(),
                category,
                message,
                stackTrace,
                playerId != null ? playerId.toString() : null,
                playerName
        );
    }

    /**
     * Lädt Debug-Einträge mit Filteroptionen
     */
    public void getDebugEntries(DebugFilter filter, Consumer<List<DebugEntry>> callback) {
        StringBuilder sql = new StringBuilder("SELECT * FROM debug_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // Filter anwenden
        if (filter.getPluginName() != null) {
            sql.append(" AND plugin_name = ?");
            params.add(filter.getPluginName());
        }

        if (filter.getLevel() != null) {
            sql.append(" AND level = ?");
            params.add(filter.getLevel().name());
        }

        if (filter.getCategory() != null) {
            sql.append(" AND category LIKE ?");
            params.add("%" + filter.getCategory() + "%");
        }

        if (filter.getSearchTerm() != null && !filter.getSearchTerm().isEmpty()) {
            sql.append(" AND (message LIKE ? OR stack_trace LIKE ?)");
            String searchPattern = "%" + filter.getSearchTerm() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if (filter.getPlayerId() != null) {
            sql.append(" AND player_id = ?");
            params.add(filter.getPlayerId().toString());
        }

        if (filter.getStartDate() != null) {
            sql.append(" AND timestamp >= ?");
            params.add(Timestamp.valueOf(filter.getStartDate()));
        }

        if (filter.getEndDate() != null) {
            sql.append(" AND timestamp <= ?");
            params.add(Timestamp.valueOf(filter.getEndDate()));
        }

        // Sortierung
        sql.append(" ORDER BY timestamp DESC");

        // Limit
        if (filter.getLimit() > 0) {
            sql.append(" LIMIT ?");
            params.add(filter.getLimit());
        }

        plugin.queryAsync(sql.toString(), results -> {
            List<DebugEntry> entries = new ArrayList<>();
            for (Map<String, Object> row : results) {
                entries.add(mapRowToEntry(row));
            }
            callback.accept(entries);
        }, params.toArray());
    }

    /**
     * Lädt einen einzelnen Debug-Eintrag
     */
    public void getDebugEntry(int id, Consumer<DebugEntry> callback) {
        plugin.queryAsync(
                "SELECT * FROM debug_logs WHERE id = ?",
                results -> {
                    if (!results.isEmpty()) {
                        callback.accept(mapRowToEntry(results.get(0)));
                    } else {
                        callback.accept(null);
                    }
                },
                id
        );
    }

    /**
     * Lädt Statistiken über Debug-Einträge
     */
    public void getDebugStats(Consumer<DebugStats> callback) {
        plugin.queryAsync(
                "SELECT " +
                        "COUNT(*) as total, " +
                        "SUM(CASE WHEN level = 'ERROR' OR level = 'CRITICAL' THEN 1 ELSE 0 END) as errors, " +
                        "COUNT(DISTINCT plugin_name) as plugins, " +
                        "COUNT(DISTINCT DATE(timestamp)) as days " +
                        "FROM debug_logs",
                results -> {
                    if (!results.isEmpty()) {
                        Map<String, Object> row = results.get(0);
                        callback.accept(new DebugStats(
                                toInt(row.get("total"), 0),
                                toInt(row.get("errors"), 0),
                                toInt(row.get("plugins"), 0),
                                toInt(row.get("days"), 0)
                        ));
                    } else {
                        callback.accept(new DebugStats(0, 0, 0, 0));
                    }
                }
        );
    }

    /**
     * Lädt alle verfügbaren Plugin-Namen
     */
    public void getAllPluginNames(Consumer<List<String>> callback) {
        plugin.queryAsync(
                "SELECT DISTINCT plugin_name FROM debug_logs ORDER BY plugin_name",
                results -> {
                    List<String> names = new ArrayList<>();
                    for (Map<String, Object> row : results) {
                        names.add((String) row.get("plugin_name"));
                    }
                    callback.accept(names);
                }
        );
    }

    /**
     * Lädt alle verfügbaren Kategorien
     */
    public void getAllCategories(Consumer<List<String>> callback) {
        plugin.queryAsync(
                "SELECT DISTINCT category FROM debug_logs WHERE category IS NOT NULL ORDER BY category",
                results -> {
                    List<String> categories = new ArrayList<>();
                    for (Map<String, Object> row : results) {
                        String cat = (String) row.get("category");
                        if (cat != null && !cat.isEmpty()) {
                            categories.add(cat);
                        }
                    }
                    callback.accept(categories);
                }
        );
    }

    /**
     * Löscht alte Debug-Einträge (älter als X Tage)
     */
    public void cleanupOldEntries(int daysToKeep, Consumer<Integer> callback) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        plugin.executeUpdateWithCallbackAsync(
                "DELETE FROM debug_logs WHERE timestamp < ?",
                success -> {
                    if (success) {
                        plugin.getLogger().info("[DebugSystem] Alte Debug-Einträge wurden gelöscht");
                    }
                    // Hier könnten wir die Anzahl gelöschter Zeilen zurückgeben
                    callback.accept(0);
                },
                Timestamp.valueOf(cutoffDate)
        );
    }

    /**
     * Konvertiert eine Datenbank-Zeile zu einem DebugEntry
     */
    private DebugEntry mapRowToEntry(Map<String, Object> row) {
        UUID playerId = null;
        String playerIdStr = (String) row.get("player_id");
        if (playerIdStr != null) {
            try {
                playerId = UUID.fromString(playerIdStr);
            } catch (Exception ignored) {}
        }

        Timestamp ts = (Timestamp) row.get("timestamp");
        LocalDateTime timestamp = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        return new DebugEntry(
                toInt(row.get("id"), 0),
                (String) row.get("plugin_name"),
                DebugLevel.valueOf((String) row.get("level")),
                (String) row.get("category"),
                (String) row.get("message"),
                (String) row.get("stack_trace"),
                timestamp,
                playerId,
                (String) row.get("player_name")
        );
    }

    private int toInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Statistik-Datenklasse
     */
    public static class DebugStats {
        public final int totalEntries;
        public final int errorCount;
        public final int pluginCount;
        public final int activeDays;

        public DebugStats(int totalEntries, int errorCount, int pluginCount, int activeDays) {
            this.totalEntries = totalEntries;
            this.errorCount = errorCount;
            this.pluginCount = pluginCount;
            this.activeDays = activeDays;
        }
    }
}
