package me.Laemedir.character;

import me.Laemedir.coreApi.CoreAPIPlugin;
import me.Laemedir.coreApi.debug.DebugManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verwaltet alle Rassen, deren Laden, Speichern und Effekte.
 */
public class RaceManager {

    private final CoreAPIPlugin coreAPI;
    private final MultiCharPlugin plugin;
    private final DebugManager debugManager;

    // Cache für Rassen
    private final Map<String, Race> races = new HashMap<>();
    
    // Cache für Charakter-Rassen (CharakterName -> Rasse)
    private final Map<String, Race> characterRaceCache = new java.util.concurrent.ConcurrentHashMap<>();

    public RaceManager(CoreAPIPlugin coreAPI, MultiCharPlugin plugin) {
        this.coreAPI = coreAPI;
        this.plugin = plugin;
        this.debugManager = plugin.getDebugManager();

        // Stelle sicher, dass die Tabelle existiert
        ensureTableExists();

        loadRacesFromDatabase();
    }

    // NEUE METHODEN FÜR RASSENEFFEKTE

    /**
     * Wendet die Rasseneffekte für einen bestimmten Charakter an (Asynchron).
     *
     * @param player der Spieler
     * @param characterName der Name des Charakters
     */
    public void applyRaceEffectsForCharacter(Player player, String characterName) {
        getCharacterRaceAsync(characterName).thenAccept(race -> {
            // Zurück auf Main-Thread für das Anwenden der Effekte
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (race == null) {
                    if (debugManager != null) {
                        debugManager.info("character", "Race", "Keine Rasse für Charakter " + characterName + " gefunden");
                    }
                    return;
                }

                // Verwende die Race-Klassen-Methode zum Anwenden der Effekte
                race.applyEffects(player);
                if (debugManager != null) {
                    debugManager.info("character", "Race", "Rasseneffekte für " + characterName + " (" + race.getName() + ") angewendet");
                }
            });
        });
    }

    /**
     * Entfernt die Rasseneffekte für einen bestimmten Charakter (Asynchron).
     *
     * @param player der Spieler
     * @param characterName der Name des Charakters
     */
    public void removeRaceEffectsForCharacter(Player player, String characterName) {
        // Entferne aus Cache
        characterRaceCache.remove(characterName);

        getCharacterRaceAsync(characterName).thenAccept(race -> {
             // Zurück auf Main-Thread für das Entfernen der Effekte
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (race == null) {
                    return;
                }

                // Verwende die Race-Klassen-Methode zum Entfernen der Effekte
                race.removeEffects(player);
                if (debugManager != null) {
                    debugManager.info("character", "Race", "Rasseneffekte für " + characterName + " (" + race.getName() + ") entfernt");
                }
            });
        });
    }

    /**
     * Holt die Rasse eines bestimmten Charakters synchron (Vorsicht: blocking).
     * Nutzt einen Cache für bessere Performance.
     *
     * @param characterName der Character-Name
     * @return die Rasse oder null
     */
    public Race getCharacterRace(String characterName) {
        if (characterName == null) return null;
        
        // Prüfe Cache
        if (characterRaceCache.containsKey(characterName)) {
            return characterRaceCache.get(characterName);
        }
        
        try {
            Race race = coreAPI.querySync(
                    "SELECT r.* FROM races r " +
                            "JOIN characters c ON r.id = c.race_id " +
                            "WHERE c.name = ?",
                    rs -> {
                        try {
                            if (rs.next()) {
                                int id = rs.getInt("id");
                                String raceName = rs.getString("race_name");
                                String description = rs.getString("description");
                                String effects = rs.getString("effects");
                                return new Race(id, raceName, description, effects);
                            }
                        } catch (SQLException e) {
                            if (debugManager != null) {
                                debugManager.error("character", "Race", "Fehler beim Laden der Charakterrasse", e);
                            }
                        }
                        return null;
                    },
                    characterName
            );
            
            if (race != null) {
                characterRaceCache.put(characterName, race);
            }
            
            return race;
        } catch (SQLException e) {
            if (debugManager != null) {
                debugManager.error("character", "Race", "Datenbankfehler beim Abrufen der Charakterrasse: " + characterName, e);
            }
            return null;
        }
    }

    /**
     * Holt die Rasse eines bestimmten Charakters asynchron.
     *
     * @param characterName der Character-Name
     * @return CompletableFuture mit der Rasse oder null
     */
    public java.util.concurrent.CompletableFuture<Race> getCharacterRaceAsync(String characterName) {
        java.util.concurrent.CompletableFuture<Race> future = new java.util.concurrent.CompletableFuture<>();
        String sql = "SELECT r.* FROM races r JOIN characters c ON r.id = c.race_id WHERE c.name = ?";
        
        coreAPI.queryAsync(sql, rs -> {
            if (!rs.isEmpty()) {
                Map<String, Object> row = rs.get(0);
                int id = (Integer) row.get("id");
                String raceName = (String) row.get("race_name");
                String description = (String) row.get("description");
                String effects = (String) row.get("effects");
                Race race = new Race(id, raceName, description, effects);
                
                // Update Cache
                characterRaceCache.put(characterName, race);
                
                future.complete(race);
            } else {
                future.complete(null);
            }
        }, characterName);
        
        return future;
    }

    // BESTEHENDE METHODEN (unverändert)

    // Stelle sicher, dass die Tabelle existiert und das richtige Schema hat
    private void ensureTableExists() {
        try {
            if (debugManager != null) {
                debugManager.info("character", "Database", "Prüfe Rassen-Tabelle");
            }

            // Prüfe, ob die Tabelle existiert
            boolean tableExists = coreAPI.querySync(
                    "SHOW TABLES LIKE 'races'",
                    rs -> {
                        try {
                            return rs.next(); // Tabelle existiert, wenn Ergebnis vorhanden
                        } catch (SQLException e) {
                            if (debugManager != null) {
                                debugManager.error("character", "Database", "Fehler beim Prüfen der Tabelle", e);
                            }
                            return false;
                        }
                    }
            );

            if (!tableExists) {
                // Erstelle die Tabelle, wenn sie nicht existiert
                if (debugManager != null) {
                    debugManager.info("character", "Database", "Erstelle Rassen-Tabelle");
                }
                coreAPI.executeUpdateAsync(
                        "CREATE TABLE races (" +
                                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                "race_name VARCHAR(50) NOT NULL UNIQUE, " +
                                "description TEXT, " +
                                "effects TEXT" +
                                ")"
                );
                if (debugManager != null) {
                    debugManager.info("character", "Database", "Rassen-Tabelle wurde erstellt");
                }
            } else {
                // Prüfe, ob das effects-Feld existiert
                boolean effectsColumnExists = coreAPI.querySync(
                        "SHOW COLUMNS FROM races LIKE 'effects'",
                        rs -> {
                            try {
                                return rs.next(); // Spalte existiert, wenn Ergebnis vorhanden
                        } catch (SQLException e) {
                            if (debugManager != null) {
                                debugManager.error("character", "Database", "Fehler beim Prüfen der Spalte", e);
                            }
                            return false;
                        }
                        }
                );

                if (!effectsColumnExists) {
                    // Füge die Spalte hinzu, wenn sie nicht existiert
                    if (debugManager != null) {
                        debugManager.info("character", "Database", "Füge effects-Spalte zur Rassen-Tabelle hinzu");
                    }
                    coreAPI.executeUpdateAsync(
                            "ALTER TABLE races ADD COLUMN effects TEXT"
                    );
                    if (debugManager != null) {
                        debugManager.info("character", "Database", "Effects-Spalte wurde hinzugefügt");
                    }
                }
            }
        } catch (SQLException e) {
            if (debugManager != null) {
                debugManager.error("character", "Database", "Fehler beim Einrichten der Datenbanktabelle", e);
            }
        }
    }

    // Lade alle Rassen aus der Datenbank
    private void loadRacesFromDatabase() {
        if (debugManager != null) {
            debugManager.info("character", "Race Loading", "Versuche Rassen aus der Datenbank zu laden");
        }
        races.clear();

        try {
            coreAPI.querySync(
                    "SELECT * FROM races",
                    resultSet -> {
                        try {
                            while (resultSet.next()) {
                                int id = resultSet.getInt("id");
                                String raceName = resultSet.getString("race_name");
                                String description = resultSet.getString("description");
                                String effectsStr = resultSet.getString("effects");

                                Race race = new Race(id, raceName, description, effectsStr);
                                races.put(raceName.toLowerCase(), race);
                            }
                            if (debugManager != null) {
                                debugManager.info("character", "Race Loading", races.size() + " Rassen erfolgreich geladen");
                            }
                            return null;
                        } catch (SQLException e) {
                            if (debugManager != null) {
                                debugManager.error("character", "Race Loading", "Fehler beim Laden der Rassen", e);
                            }
                            return null;
                        }
                    }
            );
        } catch (Exception e) {
            if (debugManager != null) {
                debugManager.error("character", "Race Loading", "Fehler beim Laden der Rassen", e);
            }
        }
    }

    // Erstelle eine neue Rasse
    public boolean createRace(String name, String description, String effects) {
        if (raceExists(name)) {
            if (debugManager != null) {
                debugManager.info("character", "Race Management", "Rasse existiert bereits: " + name);
            }
            return false;
        }

        // Erstelle eine neue Rasse und füge sie zum Cache hinzu
        Race race = new Race(name, description, effects);
        races.put(name.toLowerCase(), race);

        // Speichere die Rasse in der Datenbank
        try {
            if (debugManager != null) {
                debugManager.info("character", "Race Management", "Speichere neue Rasse in Datenbank: " + name);
            }
            coreAPI.executeUpdateAsync(
                    "INSERT INTO races (race_name, description, effects) VALUES (?, ?, ?)",
                    name, description, effects
            );
            return true;
        } catch (Exception e) {
            if (debugManager != null) {
                debugManager.error("character", "Race Management", "Fehler beim Speichern der Rasse: " + name, e);
            }
            races.remove(name.toLowerCase()); // Aus Cache entfernen bei Fehler
            return false;
        }
    }

    // Bearbeite eine bestehende Rasse
    public boolean editRace(String name, String newDescription, String newEffects) {
        // Direkt in der Datenbank prüfen, ob die Rasse existiert
        try {
            boolean exists = coreAPI.querySync(
                    "SELECT COUNT(*) as count FROM races WHERE LOWER(race_name) = LOWER(?)",
                    rs -> {
                        try {
                            if (rs.next()) {
                                return rs.getInt("count") > 0;
                            }
                        } catch (SQLException e) {
                            if (debugManager != null) {
                                debugManager.error("character", "Race Management", "Fehler bei der Rassen-Existenzprüfung", e);
                            }
                        }
                        return false;
                    },
                    name.toLowerCase()
            );

            if (!exists) {
                if (debugManager != null) {
                    debugManager.info("character", "Race Management", "Rasse nicht gefunden in Datenbank: " + name);
                }
                return false;
            }

            // Aktualisiere die Rasse im Cache
            Race race = getRace(name);
            if (race != null) {
                race.setDescription(newDescription);
                race.setEffects(newEffects);
            } else {
                // Falls nicht im Cache, neu erstellen
                races.put(name.toLowerCase(), new Race(name, newDescription, newEffects));
            }

            // Aktualisiere die Rasse in der Datenbank
            if (debugManager != null) {
                debugManager.info("character", "Race Management", "Aktualisiere Rasse in Datenbank: " + name);
            }
            coreAPI.executeUpdateAsync(
                    "UPDATE races SET description = ?, effects = ? WHERE LOWER(race_name) = LOWER(?)",
                    newDescription, newEffects, name
            );
            return true;
        } catch (SQLException e) {
            if (debugManager != null) {
                debugManager.error("character", "Race Management", "Datenbankfehler beim Bearbeiten der Rasse: " + name, e);
            }
            return false;
        }
    }

    // Lösche eine Rasse
    public boolean deleteRace(String name) {
        if (name == null || name.isEmpty()) {
            if (debugManager != null) {
                debugManager.warning("character", "Race Management", "Versuch, eine Rasse mit leerem Namen zu löschen");
            }
            return false;
        }

        // Direkt in der Datenbank prüfen, ob die Rasse existiert
        try {
            boolean exists = coreAPI.querySync(
                    "SELECT COUNT(*) as count FROM races WHERE LOWER(race_name) = LOWER(?)",
                    rs -> {
                        try {
                            if (rs.next()) {
                                return rs.getInt("count") > 0;
                            }
                        } catch (SQLException e) {
                            if (debugManager != null) {
                                debugManager.error("character", "Race Management", "Fehler bei der Rassen-Existenzprüfung", e);
                            }
                        }
                        return false;
                    },
                    name.toLowerCase()
            );

            if (!exists) {
                if (debugManager != null) {
                    debugManager.warning("character", "Race Management", "Rasse nicht gefunden in Datenbank: " + name);
                }
                return false;
            }

            // Entferne die Rasse aus dem Cache
            races.remove(name.toLowerCase());

            // Entferne die Rasse aus der Datenbank
            if (debugManager != null) {
                debugManager.info("character", "Race Management", "Lösche Rasse aus Datenbank: " + name);
            }
            coreAPI.executeUpdateAsync(
                    "DELETE FROM races WHERE LOWER(race_name) = LOWER(?)",
                    name.toLowerCase()
            );
            return true;
        } catch (SQLException e) {
            if (debugManager != null) {
                debugManager.error("character", "Race Management", "Datenbankfehler beim Löschen der Rasse: " + name, e);
            }
            return false;
        }
    }

    // Hole eine Rasse nach Namen
    public Race getRace(String name) {
        if (name == null) return null;

        Race race = races.get(name.toLowerCase());

        // Wenn nicht im Cache, versuche direkt aus der Datenbank zu laden
        if (race == null) {
            try {
                if (debugManager != null) {
                    debugManager.info("character", "Race Management", "Rasse nicht im Cache, versuche aus Datenbank zu laden: " + name);
                }
                race = coreAPI.querySync(
                        "SELECT * FROM races WHERE LOWER(race_name) = LOWER(?)",
                        rs -> {
                            try {
                                if (rs.next()) {
                                    int id = rs.getInt("id");
                                    String raceName = rs.getString("race_name");
                                    String description = rs.getString("description");
                                    String effects = rs.getString("effects");
                                    Race loadedRace = new Race(id, raceName, description, effects);
                                    races.put(raceName.toLowerCase(), loadedRace); // In Cache speichern
                                    return loadedRace;
                                }
                            } catch (SQLException e) {
                                if (debugManager != null) {
                                    debugManager.error("character", "Race Management", "Fehler beim Laden einer einzelnen Rasse", e);
                                }
                            }
                            return null;
                        },
                        name.toLowerCase()
                );
            } catch (SQLException e) {
                if (debugManager != null) {
                    debugManager.error("character", "Race Management", "Datenbankfehler beim Abrufen der Rasse: " + name, e);
                }
            }
        }

        return race;
    }

    // Hole alle Rassen
    public List<Race> getAllRaces() {
        // Sicherstellen, dass der Cache aktuell ist
        loadRacesFromDatabase();
        return new ArrayList<>(races.values());
    }

    // Prüfe, ob eine Rasse existiert
    public boolean raceExists(String name) {
        if (name == null) return false;

        // Zuerst im Cache prüfen
        if (races.containsKey(name.toLowerCase())) {
            return true;
        }

        // Falls nicht im Cache, direkt in der Datenbank prüfen
        try {
            return coreAPI.querySync(
                    "SELECT COUNT(*) as count FROM races WHERE LOWER(race_name) = LOWER(?)",
                    rs -> {
                        try {
                            if (rs.next()) {
                                boolean exists = rs.getInt("count") > 0;
                                if (exists) {
                                    // Wenn existiert aber nicht im Cache, nachladen
                                    getRace(name);
                                }
                                return exists;
                            }
                        } catch (SQLException e) {
                            if (debugManager != null) {
                                debugManager.error("character", "Race Management", "Fehler bei der Rassen-Existenzprüfung", e);
                            }
                        }
                        return false;
                    },
                    name.toLowerCase()
            );
        } catch (SQLException e) {
            if (debugManager != null) {
                debugManager.error("character", "Race Management", "Datenbankfehler bei der Rassen-Existenzprüfung: " + name, e);
            }
            return false;
        }
    }
}