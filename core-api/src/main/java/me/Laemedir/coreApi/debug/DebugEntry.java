package me.Laemedir.coreApi.debug;

import java.time.LocalDateTime;
import java.util.UUID;

public class DebugEntry {
    private final int id;
    private final String pluginName;
    private final DebugLevel level;
    private final String category;
    private final String message;
    private final String stackTrace;
    private final LocalDateTime timestamp;
    private final UUID playerId; // Optional: Spieler der die Aktion ausgelöst hat
    private final String playerName;

    public DebugEntry(int id, String pluginName, DebugLevel level, String category, 
                      String message, String stackTrace, LocalDateTime timestamp,
                      UUID playerId, String playerName) {
        this.id = id;
        this.pluginName = pluginName;
        this.level = level;
        this.category = category;
        this.message = message;
        this.stackTrace = stackTrace;
        this.timestamp = timestamp;
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public int getId() {
        return id;
    }

    public String getPluginName() {
        return pluginName;
    }

    public DebugLevel getLevel() {
        return level;
    }

    public String getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean hasStackTrace() {
        return stackTrace != null && !stackTrace.isEmpty();
    }

    public boolean hasPlayer() {
        return playerId != null;
    }
}
