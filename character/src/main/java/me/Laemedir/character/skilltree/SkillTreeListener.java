package me.Laemedir.character.skilltree;

import me.Laemedir.character.MultiCharPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Listener for the skill tree GUI.
 * Processes clicks on skill items and uses PersistentDataContainer to avoid database access.
 */
public class SkillTreeListener implements Listener {

    private final SkillTreeManager skillTreeManager;
    private final MultiCharPlugin plugin;

    public SkillTreeListener(SkillTreeManager skillTreeManager, MultiCharPlugin plugin) {
        this.skillTreeManager = skillTreeManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("Skilltree")) {
            event.setCancelled(true); // Prevent item movement

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || !clickedItem.hasItemMeta()) {
                return;
            }

            // Get character ID from metadata
            List<MetadataValue> metaValues = player.getMetadata("skilltree_character_id");
            if (metaValues.isEmpty()) {
                return;
            }
            int characterId = metaValues.get(0).asInt();

            // Close button
            if (clickedItem.getType() == org.bukkit.Material.BARRIER) {
                player.closeInventory();
                return;
            }

            // Check for PersistentDataContainer data
            ItemMeta meta = clickedItem.getItemMeta();
            PersistentDataContainer container = meta.getPersistentDataContainer();

            if (container.has(skillTreeManager.KEY_SKILL_TYPE, PersistentDataType.STRING)) {
                // It's a skill item
                String skillTypeName = container.get(skillTreeManager.KEY_SKILL_TYPE, PersistentDataType.STRING);
                SkillTreeManager.SkillType type = SkillTreeManager.SkillType.valueOf(skillTypeName);
                
                // Load values directly from the item (no DB call needed!)
                int currentValue = container.getOrDefault(skillTreeManager.KEY_CURRENT_VALUE, PersistentDataType.INTEGER, 0);
                int availablePoints = container.getOrDefault(skillTreeManager.KEY_AVAILABLE_POINTS, PersistentDataType.INTEGER, 0);

                // Left click = add point
                if (event.isLeftClick()) {
                    skillTreeManager.addSkillPoint(player, characterId, type, currentValue, availablePoints);
                }
                // Right click = remove point
                else if (event.isRightClick()) {
                    skillTreeManager.removeSkillPoint(player, characterId, type, currentValue, availablePoints);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().contains("Skilltree") && event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            player.removeMetadata("skilltree_character_id", plugin);
        }
    }
}
