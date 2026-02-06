package me.Laemedir.coreApi.debug;

public enum DebugLevel {
    INFO("§b", "Info"),
    WARNING("§e", "Warnung"),
    ERROR("§c", "Fehler"),
    CRITICAL("§4§l", "Kritisch"),
    SUCCESS("§a", "Erfolg"),
    DEBUG("§7", "Debug");

    private final String color;
    private final String displayName;

    DebugLevel(String color, String displayName) {
        this.color = color;
        this.displayName = displayName;
    }

    public String getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFormattedName() {
        return color + displayName + "§r";
    }
}
