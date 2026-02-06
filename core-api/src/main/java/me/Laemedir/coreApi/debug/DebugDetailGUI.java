package me.Laemedir.coreApi.debug;

import me.Laemedir.coreApi.CoreAPIPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;

public class DebugDetailGUI {
    private final CoreAPIPlugin plugin;
    private final DebugManager debugManager;
    private final Player player;
    private final int entryId;
    private final DebugListGUI previousGUI;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public DebugDetailGUI(CoreAPIPlugin plugin, DebugManager debugManager, Player player, int entryId, DebugListGUI previousGUI) {
        this.plugin = plugin;
        this.debugManager = debugManager;
        this.player = player;
        this.entryId = entryId;
        this.previousGUI = previousGUI;
    }

    public void open() {
        debugManager.getEntry(entryId, entry -> {
            if (entry == null) {
                player.sendMessage("§c[Debug] Eintrag nicht gefunden!");
                return;
            }

            // Schließe das Inventar und zeige Details im Chat
            player.closeInventory();
            
            // Kopfzeile
            player.sendMessage("");
            player.sendMessage("§8§m                                        ");
            player.sendMessage(entry.getLevel().getFormattedName() + " §7[§e" + entry.getPluginName() + "§7]");
            player.sendMessage("§8§m                                        ");
            player.sendMessage("");
            
            // Basis-Informationen
            player.sendMessage("§7Zeit: §f" + entry.getTimestamp().format(TIME_FORMAT));
            player.sendMessage("§7Kategorie: §f" + (entry.getCategory() != null ? entry.getCategory() : "Keine"));
            
            if (entry.hasPlayer()) {
                player.sendMessage("§7Spieler: §e" + entry.getPlayerName() + " §8(§7" + entry.getPlayerId() + "§8)");
            }
            
            player.sendMessage("");
            player.sendMessage("§7Nachricht:");
            player.sendMessage("§f" + entry.getMessage());
            
            // Stack-Trace
            if (entry.hasStackTrace()) {
                player.sendMessage("");
                player.sendMessage("§c§lStack-Trace:");
                player.sendMessage("§8(Klicke unten für Kopie)");
                
                // Adventure Component mit Hover und Click
                Component stackTraceComponent = Component.text()
                    .content("§e[Klicke hier zum Kopieren]")
                    .color(NamedTextColor.YELLOW)
                    .decorate(TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(
                        Component.text("Klicken um Stack-Trace zu kopieren")
                            .color(NamedTextColor.GRAY)
                    ))
                    .clickEvent(ClickEvent.copyToClipboard(entry.getStackTrace()))
                    .build();
                
                player.sendMessage(stackTraceComponent);
                
                // Zeige die ersten paar Zeilen des Stack-Traces
                String[] lines = entry.getStackTrace().split("\n");
                int maxLines = Math.min(5, lines.length);
                for (int i = 0; i < maxLines; i++) {
                    player.sendMessage("§7" + lines[i]);
                }
                if (lines.length > maxLines) {
                    player.sendMessage("§8... (" + (lines.length - maxLines) + " weitere Zeilen)");
                }
            }
            
            player.sendMessage("");
            player.sendMessage("§8§m                                        ");
            
            // Zurück-Button als clickable Component
            Component backButton = Component.text()
                .content("[Zurück zur Liste]")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(
                    Component.text("Zurück zur Debug-Liste")
                        .color(NamedTextColor.GRAY)
                ))
                .clickEvent(ClickEvent.runCommand("/debug"))
                .build();
            
            player.sendMessage(backButton);
            player.sendMessage("");
        });
    }
}
