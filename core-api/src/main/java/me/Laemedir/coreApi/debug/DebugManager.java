package me.Laemedir.coreApi.debug;

import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * DebugManager - Zentrale API für das Debug-System
 * 
 * Verwendung in anderen Plugins:
 * CoreAPIPlugin coreAPI = (CoreAPIPlugin) Bukkit.getPluginManager().getPlugin("core-api");
 * DebugManager debug = coreAPI.getDebugManager();
 * debug.log("MeinPlugin", DebugLevel.INFO, "System", "Test-Nachricht");
 */
public class DebugManager {
    private final CoreAPIPlugin plugin;
    private final DebugDatabase database;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    
    private boolean consoleOutput = true; // Zusätzliche Console-Ausgabe
    private boolean notifyTeam = true; // Benachrichtige Online-Teammitglieder bei kritischen Fehlern

    public DebugManager(CoreAPIPlugin plugin) {
        this.plugin = plugin;
        this.database = new DebugDatabase(plugin);
    }

    /**
     * Initialisiert das Debug-System
     */
    public void initialize() {
        database.createTables();
        plugin.getLogger().info("[DebugSystem] Erfolgreich initialisiert");
        
        // Automatisches Cleanup alle 24 Stunden
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            database.cleanupOldEntries(30, count -> {
                plugin.getLogger().info("[DebugSystem] Automatisches Cleanup abgeschlossen");
            });
        }, 20L * 60 * 60 * 24, 20L * 60 * 60 * 24); // Alle 24 Stunden
    }

    // ===== HAUPTMETHODEN FÜR LOGGING =====

    /**
     * Loggt einen Debug-Eintrag (volle Kontrolle)
     */
    public void log(String pluginName, DebugLevel level, String category, String message, 
                   String stackTrace, UUID playerId, String playerName) {
        // In Datenbank speichern
        database.logDebug(pluginName, level, category, message, stackTrace, playerId, playerName);
        
        // Console-Ausgabe
        if (consoleOutput) {
            String consoleMessage = String.format("[%s] [%s/%s] %s: %s",
                    LocalDateTime.now().format(timeFormatter),
                    pluginName,
                    level.getDisplayName(),
                    category != null ? category : "General",
                    message);
            plugin.getLogger().info(consoleMessage);
        }
        
        // Benachrichtige Online-Teammitglieder bei kritischen Fehlern
        if (notifyTeam && (level == DebugLevel.ERROR || level == DebugLevel.CRITICAL)) {
            notifyOnlineTeam(pluginName, level, message);
        }
    }

    /**
     * Einfaches Logging ohne zusätzliche Daten
     */
    public void log(String pluginName, DebugLevel level, String category, String message) {
        log(pluginName, level, category, message, null, null, null);
    }

    /**
     * Logging mit Exception
     */
    public void log(String pluginName, DebugLevel level, String category, String message, Exception exception) {
        String stackTrace = exception != null ? getStackTrace(exception) : null;
        log(pluginName, level, category, message, stackTrace, null, null);
    }

    /**
     * Logging mit Spieler-Kontext
     */
    public void log(String pluginName, DebugLevel level, String category, String message, Player player) {
        log(pluginName, level, category, message, null, 
            player != null ? player.getUniqueId() : null,
            player != null ? player.getName() : null);
    }

    /**
     * Logging mit Spieler und Exception
     */
    public void log(String pluginName, DebugLevel level, String category, String message, 
                   Exception exception, Player player) {
        String stackTrace = exception != null ? getStackTrace(exception) : null;
        log(pluginName, level, category, message, stackTrace,
            player != null ? player.getUniqueId() : null,
            player != null ? player.getName() : null);
    }

    // ===== CONVENIENCE-METHODEN =====

    public void info(String pluginName, String category, String message) {
        log(pluginName, DebugLevel.INFO, category, message);
    }

    public void warning(String pluginName, String category, String message) {
        log(pluginName, DebugLevel.WARNING, category, message);
    }

    public void error(String pluginName, String category, String message) {
        log(pluginName, DebugLevel.ERROR, category, message);
    }

    public void error(String pluginName, String category, String message, Exception exception) {
        log(pluginName, DebugLevel.ERROR, category, message, exception);
    }

    public void critical(String pluginName, String category, String message) {
        log(pluginName, DebugLevel.CRITICAL, category, message);
    }

    public void critical(String pluginName, String category, String message, Exception exception) {
        log(pluginName, DebugLevel.CRITICAL, category, message, exception);
    }

    public void success(String pluginName, String category, String message) {
        log(pluginName, DebugLevel.SUCCESS, category, message);
    }

    public void debug(String pluginName, String category, String message) {
        log(pluginName, DebugLevel.DEBUG, category, message);
    }

    // ===== QUERY-METHODEN =====

    /**
     * Lädt Debug-Einträge mit Filter
     */
    public void getEntries(DebugFilter filter, Consumer<List<DebugEntry>> callback) {
        database.getDebugEntries(filter, callback);
    }

    /**
     * Lädt einen einzelnen Eintrag
     */
    public void getEntry(int id, Consumer<DebugEntry> callback) {
        database.getDebugEntry(id, callback);
    }

    /**
     * Lädt Statistiken
     */
    public void getStats(Consumer<DebugDatabase.DebugStats> callback) {
        database.getDebugStats(callback);
    }

    /**
     * Lädt alle Plugin-Namen
     */
    public void getAllPluginNames(Consumer<List<String>> callback) {
        database.getAllPluginNames(callback);
    }

    /**
     * Lädt alle Kategorien
     */
    public void getAllCategories(Consumer<List<String>> callback) {
        database.getAllCategories(callback);
    }

    /**
     * Löscht alte Einträge
     */
    public void cleanupOldEntries(int daysToKeep, Consumer<Integer> callback) {
        database.cleanupOldEntries(daysToKeep, callback);
    }

    // ===== HILFSMETHODEN =====

    /**
     * Konvertiert eine Exception zu einem Stack-Trace String
     */
    private String getStackTrace(Exception exception) {
        if (exception == null) return null;
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Benachrichtigt Online-Teammitglieder über kritische Fehler
     */
    private void notifyOnlineTeam(String pluginName, DebugLevel level, String message) {
        String notification = String.format("§8[§cDebug§8] %s §7[§e%s§7] %s",
                level.getFormattedName(),
                pluginName,
                message);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("laemedir.debug.notify")) {
                player.sendMessage(notification);
            }
        }
    }

    // ===== GETTERS & SETTERS =====

    public void setConsoleOutput(boolean consoleOutput) {
        this.consoleOutput = consoleOutput;
    }

    public void setNotifyTeam(boolean notifyTeam) {
        this.notifyTeam = notifyTeam;
    }

    public DebugDatabase getDatabase() {
        return database;
    }
}
