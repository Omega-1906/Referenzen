package me.Laemedir.character;

import me.Laemedir.coreApi.CoreAPIPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command-Executor für den /deletecharacter Befehl.
 * Erlaubt Administratoren das unwiderrufliche Löschen von Charakteren.
 */
public class DeleteCharacterCommand implements CommandExecutor {
    private final CoreAPIPlugin coreAPI;

    // Zentraler Prefix für Nachrichten
    private static final String PREFIX = "§8[§6⚔§eCharakter§6⚔§8] §7";

    /**
     * Konstruktor für den DeleteCharacterCommand.
     *
     * @param coreAPI Die Instanz der CoreAPI.
     */
    public DeleteCharacterCommand(CoreAPIPlugin coreAPI) {
        this.coreAPI = coreAPI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("character.delete")) {
            sender.sendMessage(PREFIX + "§cDu hast keine Berechtigung, diesen Befehl auszuführen. §8(§4✖§8)");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(PREFIX + "§eVerwendung: §7/deletecharacter <Charaktername>");
            sender.sendMessage(PREFIX + "§cBitte gib den Namen des Charakters an, den du löschen möchtest. §8(§4✖§8)");
            return true;
        }

        String characterName = args[0];

        // Prüft, ob der Charakter existiert
        String checkCharacterSql = "SELECT id FROM characters WHERE name = ?";
        coreAPI.queryAsync(checkCharacterSql, result -> {
            if (result.isEmpty()) {
                sender.sendMessage(PREFIX + "§cEin Charakter mit dem Namen §e'" + characterName + "'§c existiert nicht. §8(§4✖§8)");
                return;
            }

            // Ruft die Charakter-ID ab
            Number characterIdNumber = (Number) result.get(0).get("id");
            int characterId = characterIdNumber.intValue();

            // Löscht den Charakter aus der Datenbank
            String deleteCharacterSql = "DELETE FROM characters WHERE id = ?";
            coreAPI.executeUpdateWithCallbackAsync(deleteCharacterSql, deleteCharacterResult -> {
                if (!deleteCharacterResult) {
                    sender.sendMessage(PREFIX + "§cFehler beim Löschen des Charakters §e'" + characterName + "'§c. §8(§4✖§8)");
                    return;
                }

                // Sendet eine Erfolgsmeldung
                sender.sendMessage(PREFIX + "§aDer Charakter §e'" + characterName + "'§a wurde erfolgreich gelöscht! §8(§2✔§8)");
            }, characterId);
        }, characterName);

        return true;
    }
}