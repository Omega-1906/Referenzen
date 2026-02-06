package me.Laemedir.character;

import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.Map;

/**
 * Repräsentiert eine Rasse im Charaktersystem.
 * Speichert Informationen wie Name, Beschreibung und aktive Effekte.
 */
public class Race {
    private int id;
    private final String name;
    private String description;
    private String effects;

    // Konstruktor für neue Rassen (ohne ID)
    public Race(String name, String description, String effects) {
        this.id = 0; // 0 bedeutet, dass die Rasse noch nicht in der Datenbank gespeichert ist
        this.name = name;
        this.description = description;
        this.effects = effects;
    }

    // Konstruktor für bestehende Rassen (mit ID)
    public Race(int id, String name, String description, String effects) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.effects = effects;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEffects() {
        return effects;
    }

    public void setEffects(String effects) {
        this.effects = effects;
    }

    // Hilfsmethode, um die Effekte als String zu erhalten
    public String getEffectsAsString() {
        return effects;
    }

    // Neue Methode zum Parsen der Effekte
    public Map<PotionEffectType, Integer> parseEffects() {
        Map<PotionEffectType, Integer> effectMap = new HashMap<>();

        if (effects == null || effects.isEmpty()) {
            return effectMap;
        }

        String[] effectsArray = effects.split(";");
        for (String effect : effectsArray) {
            String[] parts = effect.split(":");
            if (parts.length != 2) continue;

            try {
                PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                int level = Integer.parseInt(parts[1]);

                if (type != null && level > 0) {
                    effectMap.put(type, level);
                }
            } catch (NumberFormatException e) {
                // Ignoriere ungültige Einträge
            }
        }

        return effectMap;
    }

    // Methode zum Anwenden der Effekte auf einen Spieler
    public void applyEffects(Player player) {
        Map<PotionEffectType, Integer> effectMap = parseEffects();

        for (Map.Entry<PotionEffectType, Integer> entry : effectMap.entrySet()) {
            PotionEffectType type = entry.getKey();
            int level = entry.getValue();

            // Entferne zuerst den Effekt, falls er bereits existiert
            player.removePotionEffect(type);

            // Wende den neuen Effekt an (unendliche Dauer mit 999999 Ticks)
            player.addPotionEffect(new PotionEffect(type, -1, level - 1, false, false));
        }
    }

    // Methode zum Entfernen aller Effekte von einem Spieler
    public void removeEffects(Player player) {
        Map<PotionEffectType, Integer> effectMap = parseEffects();

        for (PotionEffectType type : effectMap.keySet()) {
            player.removePotionEffect(type);
        }
    }

    // Methode zum Konvertieren einer Map von Effekten in einen String
    public static String effectsMapToString(Map<PotionEffectType, Integer> effectMap) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<PotionEffectType, Integer> entry : effectMap.entrySet()) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(entry.getKey().getName()).append(":").append(entry.getValue());
        }

        return sb.toString();
    }

    // Hilfsmethode zum Hinzufügen eines einzelnen Effekts
    public void addEffect(PotionEffectType type, int level) {
        Map<PotionEffectType, Integer> effectMap = parseEffects();
        effectMap.put(type, level);
        this.effects = effectsMapToString(effectMap);
    }

    // Hilfsmethode zum Entfernen eines einzelnen Effekts
    public void removeEffect(PotionEffectType type) {
        Map<PotionEffectType, Integer> effectMap = parseEffects();
        effectMap.remove(type);
        this.effects = effectsMapToString(effectMap);
    }

    // Hilfsmethode zum Prüfen, ob ein bestimmter Effekt vorhanden ist
    public boolean hasEffect(PotionEffectType type) {
        return parseEffects().containsKey(type);
    }

    // Hilfsmethode zum Abrufen des Levels eines bestimmten Effekts
    public int getEffectLevel(PotionEffectType type) {
        Map<PotionEffectType, Integer> effectMap = parseEffects();
        return effectMap.getOrDefault(type, 0);
    }
}
