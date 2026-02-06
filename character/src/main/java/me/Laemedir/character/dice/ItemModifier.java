package me.Laemedir.character.dice;

/**
 * Repräsentiert einen Modifikator für Items (z.B. Waffen).
 * Speichert den Namen, den Wert und das betroffene Attribut.
 */
public class ItemModifier {
    private final String name;
    private final int value;
    private final String attribute;

    public ItemModifier(String name, int value, String attribute) {
        this.name = name;
        this.value = value;
        this.attribute = attribute;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public String getAttribute() {
        return attribute;
    }
}
