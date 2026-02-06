package me.Laemedir.character;

import org.bukkit.entity.Player;

/**
 * Interface für das Handling von Namenskategorien.
 * Definiert Methoden zum Setzen und Abrufen der aktiven Namenskategorie eines Spielers.
 */
public interface NameCategoryHandler {

    /**
     * Setzt die aktive Namenskategorie für einen Spieler anhand eines Strings.
     *
     * @param player der Spieler
     * @param categoryName der Name der Kategorie (z.B. "name", "deckname", "rufname")
     */
    void setActiveNameCategoryFromString(Player player, String categoryName);

    /**
     * Ruft die aktive Namenskategorie eines Spielers als String ab.
     *
     * @param player der Spieler
     * @return der Name der aktiven Kategorie
     */
    String getActiveNameCategoryString(Player player);
}
