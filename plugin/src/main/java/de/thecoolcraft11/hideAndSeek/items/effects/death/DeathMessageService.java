package de.thecoolcraft11.hideAndSeek.items.effects.death;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.listener.player.PlayerHitListener;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class DeathMessageService {

    private final HideAndSeek plugin;

    public DeathMessageService(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public Component getEnvironmentalDeathMessage(Player victim, PlayerHitListener.EnvironmentalDeathCause cause, Player messageOwner) {
        if (victim == null || cause == null || messageOwner == null) {
            return null;
        }

        String selectedId = ItemSkinSelectionService.getSelectedVariant(messageOwner, DeathMessageSkins.LOGICAL_ITEM_ID);
        if (selectedId == null || selectedId.isBlank()) {
            return null;
        }

        if (!ItemSkinSelectionService.isUnlocked(messageOwner.getUniqueId(), DeathMessageSkins.LOGICAL_ITEM_ID, selectedId)) {
            return null;
        }

        DeathMessageSkin skin = DeathMessageManager.getDeathMessageSkin(selectedId);
        if (skin == null) {
            return null;
        }

        try {
            return skin.getEnvironmentalDeathMessage(victim.getName(), cause.name());
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting environmental death message: " + e.getMessage());
            return null;
        }
    }

    public Component getKillMessage(Player killer, Player victim) {
        if (killer == null || victim == null) {
            return null;
        }

        String selectedId = ItemSkinSelectionService.getSelectedVariant(killer, DeathMessageSkins.LOGICAL_ITEM_ID);
        if (selectedId == null || selectedId.isBlank()) {
            return null;
        }

        if (!ItemSkinSelectionService.isUnlocked(killer.getUniqueId(), DeathMessageSkins.LOGICAL_ITEM_ID, selectedId)) {
            return null;
        }

        DeathMessageSkin skin = DeathMessageManager.getDeathMessageSkin(selectedId);
        if (skin == null) {
            return null;
        }

        try {
            return skin.getKillMessage(killer.getName(), victim.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting kill message: " + e.getMessage());
            return null;
        }
    }

    public Component getSeekerPerkDeathMessage(Player seeker, Player victim, PlayerHitListener.EnvironmentalDeathCause cause) {
        if (seeker == null || victim == null) {
            return null;
        }
        return getEnvironmentalDeathMessage(victim, cause, seeker);
    }
}

