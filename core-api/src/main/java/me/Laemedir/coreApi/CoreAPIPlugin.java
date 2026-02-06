package me.Laemedir.coreApi;

import me.Laemedir.coreApi.debug.DebugCommand;
import me.Laemedir.coreApi.debug.DebugManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

public class CoreAPIPlugin extends JavaPlugin {

    private String dbUrl;
    private String dbUser;
    private String dbPass;
    private DebugManager debugManager;

    @Override
    public void onEnable() {
        // Verbindungsdaten speichern
        dbUrl =  "jdbc:mysql://92.113.21.87:3306/s1_Laemedir?autoReconnect=true&useSSL=false&serverTimezone=Europe/Berlin";
        dbUser = "u1_RX0D57Q8ba";
        dbPass = "vC+geK.0+K2E84CGn5pfb.zN";
        
        // Debug-System initialisieren
        this.debugManager = new DebugManager(this);
        debugManager.initialize();
        
        // Debug-Command registrieren
        DebugCommand debugCommand = new DebugCommand(this, debugManager);
        getCommand("debug").setExecutor(debugCommand);
        getCommand("debug").setTabCompleter(debugCommand);
    }

    // Neue Verbindung bei jedem Aufruf erstellen
    public Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(dbUrl, dbUser, dbPass);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL-Treiber nicht gefunden", e);
        }
    }

    // Position des Spielers asynchron speichern
    public void savePlayerLocationAsync(Player player, Location location) {
        String sql = "REPLACE INTO player_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)";

        executeUpdateAsync(sql,
                player.getUniqueId().toString(),
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    // Position des Spielers asynchron abrufen
    public void getPlayerLocationAsync(UUID uuid, Consumer<Location> callback) {
        String sql = "SELECT * FROM player_locations WHERE uuid = ?";

        queryAsync(sql, results -> {
            if (results.isEmpty()) {
                callback.accept(null); // Keine Position gefunden
            } else {
                Map<String, Object> row = results.get(0);
                String worldName = (String) row.get("world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    callback.accept(null); // Welt existiert nicht
                    return;
                }

                double x = (double) row.get("x");
                double y = (double) row.get("y");
                double z = (double) row.get("z");
                float yaw = ((Number) row.get("yaw")).floatValue();
                float pitch = ((Number) row.get("pitch")).floatValue();

                callback.accept(new Location(world, x, y, z, yaw, pitch));
            }
        }, uuid.toString());
    }

    /**
     * Führt eine asynchrone Datenbankabfrage aus.
     */
    public void queryAsync(String sql, Consumer<List<Map<String, Object>>> callback, Object... params) {
        new Thread(() -> {
            try (Connection conn = getConnection(); // Neue Verbindung pro Anfrage
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                // Parameter binden
                for(int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }

                // ResultSet verarbeiten
                try (ResultSet rs = ps.executeQuery()) {
                    List<Map<String, Object>> results = new ArrayList<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while(rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for(int i = 1; i <= columnCount; i++) {
                            row.put(metaData.getColumnName(i), rs.getObject(i));
                        }
                        results.add(row);
                    }

                    // Callback im Hauptthread ausführen
                    Bukkit.getScheduler().runTask(this, () -> callback.accept(results));
                }
            } catch (SQLException e) {
                String callingPlugin = getCallingPlugin();
                getLogger().severe("Datenbankfehler [" + callingPlugin + "]: " + e.getMessage());
                getLogger().severe("SQL-Query [" + callingPlugin + "]: " + sql);
            }
        }).start();
    }

    /**
     * Führt ein asynchrones Update (INSERT, UPDATE, DELETE) aus.
     */
    public void executeUpdateAsync(String sql, Object... params) {
        new Thread(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                for(int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                String callingPlugin = getCallingPlugin();
                getLogger().severe("Update fehlgeschlagen [" + callingPlugin + "]: " + e.getMessage());
                getLogger().severe("SQL-Update [" + callingPlugin + "]: " + sql);
            }
        }).start();
    }

    /**
     * Führt ein asynchrones Update mit Erfolgs-Callback aus.
     */
    public void executeUpdateWithCallbackAsync(String sql, Consumer<Boolean> callback, Object... params) {
        new Thread(() -> {
            boolean success = false;
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                // Parameter binden
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }

                // Ausführen des Updates
                ps.executeUpdate();
                success = true; // Wenn kein Fehler auftritt, ist die Operation erfolgreich
            } catch (SQLException e) {
                String callingPlugin = getCallingPlugin();
                getLogger().severe("Update fehlgeschlagen [" + callingPlugin + "]: " + e.getMessage());
                getLogger().severe("SQL-Update [" + callingPlugin + "]: " + sql);
            }

            // Rückruf im Hauptthread ausführen
            boolean finalSuccess = success;
            Bukkit.getScheduler().runTask(this, () -> callback.accept(finalSuccess));
        }).start();
    }

    /**
     * Fügt einen Charakter ein und gibt die generierte ID zurück.
     */
    public void insertCharacterAndGetIdAsync(String sql, Consumer<Integer> callback, Object... params) {
        new Thread(() -> {
            int characterId = -1; // Standardwert für Fehler
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                // Parameter binden
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }

                // SQL ausführen
                ps.executeUpdate();

                // Generierte Schlüssel abrufen
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        characterId = rs.getInt(1); // Erste Spalte enthält die generierte ID
                    }
                }
            } catch (SQLException e) {
                String callingPlugin = getCallingPlugin();
                getLogger().severe("Fehler beim Einfügen des Charakters [" + callingPlugin + "]: " + e.getMessage());
                getLogger().severe("SQL-Insert [" + callingPlugin + "]: " + sql);
            }

            // Rückruf im Hauptthread
            int finalCharacterId = characterId;
            Bukkit.getScheduler().runTask(this, () -> callback.accept(finalCharacterId));
        }).start();
    }

    // Synchrone Abfrage (Vorsicht: blockiert den Thread!)
    public <T> T querySync(String sql, Function<ResultSet, T> processor, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for(int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return processor.apply(rs);
            }
        } catch (SQLException e) {
            String callingPlugin = getCallingPlugin();
            getLogger().severe("Sync-Query fehlgeschlagen [" + callingPlugin + "]: " + e.getMessage());
            getLogger().severe("SQL-Sync-Query [" + callingPlugin + "]: " + sql);
            throw e; // Re-throw für synchrone Methode
        }
    }

    // Synchrones Update (Vorsicht: blockiert den Thread!)
    public int executeUpdateSync(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for(int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            String callingPlugin = getCallingPlugin();
            getLogger().severe("Sync-Update fehlgeschlagen [" + callingPlugin + "]: " + e.getMessage());
            getLogger().severe("SQL-Sync-Update [" + callingPlugin + "]: " + sql);
            throw e; // Re-throw für synchrone Methode
        }
    }

    /**
     * Ermittelt den Namen des aufrufenden Plugins anhand des Stack Trace
     */
    private String getCallingPlugin() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            
            // Überspringe CoreAPI-eigene Klassen
            if (className.startsWith("me.Laemedir.coreApi")) {
                continue;
            }
            
            // Überspringe System-Klassen
            if (className.startsWith("java.") || className.startsWith("sun.") || className.startsWith("org.bukkit.")) {
                continue;
            }
            
            // Versuche Plugin-Namen aus Package-Namen zu extrahieren
            if (className.startsWith("me.Laemedir.")) {
                String[] parts = className.split("\\.");
                if (parts.length >= 3) {
                    return parts[2]; // me.Laemedir.[pluginname]...
                }
            }
            
            // Fallback: Klassenname zurückgeben
            return className;
        }
        
        return "Unbekannt";
    }
    
    /**
     * Gibt den DebugManager zurück
     */
    public DebugManager getDebugManager() {
        return debugManager;
    }
}
