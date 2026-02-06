package me.Laemedir.character;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Hilfsklasse für Inventar-Operationen.
 * Ermöglicht das Serialisieren und Deserialisieren von ItemStacks zu/aus Base64-Strings.
 * Nützlich für die Speicherung in Datenbanken.
 */
public class InventoryUtil {

    /**
     * Serialisiert ein Array von ItemStacks in einen Base64-String.
     *
     * @param items das Array der zu serialisierenden Items
     * @return der Base64-String
     * @throws IllegalStateException wenn ein Fehler beim Serialisieren auftritt
     */
    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Fehler beim Serialisieren der Items.", e);
        }
    }

    /**
     * Deserialisiert einen Base64-String zurück in ein Array von ItemStacks.
     *
     * @param data der Base64-String
     * @return das Array der Items
     * @throws IllegalStateException wenn ein Fehler beim Deserialisieren auftritt
     */
    public static ItemStack[] itemStackArrayFromBase64(String data) throws IllegalStateException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            throw new IllegalStateException("Fehler beim Deserialisieren der Items.", e);
        }
    }
}
