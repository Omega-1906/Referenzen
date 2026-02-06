package me.Laemedir.coreApi.debug;

import java.time.LocalDateTime;
import java.util.UUID;

public class DebugFilter {
    private String pluginName;
    private DebugLevel level;
    private String category;
    private String searchTerm;
    private UUID playerId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int limit = 50; // Standard-Limit

    public DebugFilter() {
    }

    // Builder-Pattern für einfache Filter-Erstellung
    public static DebugFilter builder() {
        return new DebugFilter();
    }

    public DebugFilter pluginName(String pluginName) {
        this.pluginName = pluginName;
        return this;
    }

    public DebugFilter level(DebugLevel level) {
        this.level = level;
        return this;
    }

    public DebugFilter category(String category) {
        this.category = category;
        return this;
    }

    public DebugFilter searchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
        return this;
    }

    public DebugFilter playerId(UUID playerId) {
        this.playerId = playerId;
        return this;
    }

    public DebugFilter startDate(LocalDateTime startDate) {
        this.startDate = startDate;
        return this;
    }

    public DebugFilter endDate(LocalDateTime endDate) {
        this.endDate = endDate;
        return this;
    }

    public DebugFilter limit(int limit) {
        this.limit = limit;
        return this;
    }

    // Vordefinierte Filter
    public static DebugFilter recentErrors() {
        return builder()
                .level(DebugLevel.ERROR)
                .startDate(LocalDateTime.now().minusDays(7))
                .limit(100);
    }

    public static DebugFilter todayAll() {
        return builder()
                .startDate(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0))
                .limit(500);
    }

    public static DebugFilter byPlugin(String pluginName) {
        return builder()
                .pluginName(pluginName)
                .limit(100);
    }

    // Getters
    public String getPluginName() {
        return pluginName;
    }

    public DebugLevel getLevel() {
        return level;
    }

    public String getCategory() {
        return category;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public int getLimit() {
        return limit;
    }
}
