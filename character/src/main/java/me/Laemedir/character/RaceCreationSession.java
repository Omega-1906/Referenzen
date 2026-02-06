package me.Laemedir.character;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet eine aktive Sitzung zur Erstellung oder Bearbeitung einer Rasse.
 * Speichert den aktuellen Fortschritt (Name, Beschreibung, Effekte) und den Status der Sitzung.
 */
public class RaceCreationSession {
    private static final Map<UUID, RaceCreationSession> activeSessions = new HashMap<>();

    private final Player player;
    private final String raceName;
    private final RaceManager raceManager;
    private String description;
    private List<String> effects = new ArrayList<>();
    private SessionStage stage;

    // Für die Bearbeitung einer bestehenden Rasse
    private Race existingRace;

    /**
     * Enum für die verschiedenen Phasen der Erstellung/Bearbeitung.
     */
    public enum SessionStage {
        DESCRIPTION,
        EFFECTS,
        EDIT_DESCRIPTION
    }

    private RaceCreationSession(Player player, String raceName, RaceManager raceManager) {
        this.player = player;
        this.raceName = raceName;
        this.raceManager = raceManager;
        this.stage = SessionStage.DESCRIPTION;
    }

    private RaceCreationSession(Player player, Race race, RaceManager raceManager) {
        this.player = player;
        this.raceName = race.getName();
        this.raceManager = raceManager;
        this.existingRace = race;
        this.stage = SessionStage.EDIT_DESCRIPTION;
    }

    /**
     * Startet eine neue Erstellungssitzung für einen Spieler.
     *
     * @param player      der Spieler
     * @param raceName    der Name der neuen Rasse
     * @param raceManager der RaceManager
     */
    public static void startSession(Player player, String raceName, RaceManager raceManager) {
        RaceCreationSession session = new RaceCreationSession(player, raceName, raceManager);
        activeSessions.put(player.getUniqueId(), session);
    }

    /**
     * Startet eine Bearbeitungssitzung für eine existierende Rasse (Beschreibung).
     *
     * @param player      der Spieler
     * @param race        die zu bearbeitende Rasse
     * @param raceManager der RaceManager
     */
    public static void startEditDescriptionSession(Player player, Race race, RaceManager raceManager) {
        RaceCreationSession session = new RaceCreationSession(player, race, raceManager);
        activeSessions.put(player.getUniqueId(), session);
    }

    public static RaceCreationSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public static void endSession(Player player) {
        activeSessions.remove(player.getUniqueId());
    }

    public static boolean hasSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public void setDescription(String description) {
        this.description = description;
        this.stage = SessionStage.EFFECTS;
    }

    public void addEffect(String effect) {
        this.effects.add(effect);
    }

    /**
     * Gibt die gesammelten Effekte als formatierten String zurück.
     *
     * @return String im Format "EFFEKT:LEVEL;EFFEKT:LEVEL"
     */
    public String getEffectsAsString() {
        if (effects.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String effect : effects) {
            builder.append(effect).append(";");
        }

        return builder.toString();
    }

    /**
     * Aktualisiert die Beschreibung der existierenden Rasse in der Datenbank.
     */
    public void updateRaceDescription() {
        if (existingRace != null) {
            raceManager.editRace(existingRace.getName(), description, existingRace.getEffectsAsString());
        }
    }

    public SessionStage getStage() {
        return stage;
    }

    public String getRaceName() {
        return raceName;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getEffects() {
        return effects;
    }
}
