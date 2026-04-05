package de.thecoolcraft11.hideAndSeek.items.effects.win;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class WinSkinService {

    private final HideAndSeek plugin;

    public WinSkinService(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void triggerWinSkin(Player player, boolean hidersWon) {
        if (player == null) {
            return;
        }

        String selectedId = ItemSkinSelectionService.getSelectedVariant(player, WinSkinSkins.LOGICAL_ITEM_ID);
        if (selectedId == null || selectedId.isBlank()) {
            return;
        }
        if (!ItemSkinSelectionService.isUnlocked(player.getUniqueId(), WinSkinSkins.LOGICAL_ITEM_ID, selectedId)) {
            return;
        }

        WinSkin winSkin = WinSkinManager.getWinSkin(selectedId);
        if (winSkin == null) {
            return;
        }

        try {
            winSkin.execute(player, hidersWon, plugin);
            playScaledVictoryFlourish(player, selectedId, hidersWon);
        } catch (Exception ignored) {
        }
    }

    private void playScaledVictoryFlourish(Player player, String variantId, boolean hidersWon) {
        if (player == null || !player.isOnline() || variantId == null || variantId.isBlank()) {
            return;
        }

        ItemRarity rarity = ItemSkinSelectionService.getRarity(WinSkinSkins.LOGICAL_ITEM_ID, variantId);
        int cost = ItemSkinSelectionService.getCost(plugin, WinSkinSkins.LOGICAL_ITEM_ID, variantId);
        int intensity = Math.clamp(cost / 300, 1, 6);

        Color primary = hidersWon ? Color.fromRGB(70, 240, 120) : Color.fromRGB(255, 90, 90);
        Color rareAccent = switch (rarity) {
            case COMMON -> Color.fromRGB(230, 230, 230);
            case UNCOMMON -> Color.fromRGB(90, 240, 120);
            case RARE -> Color.fromRGB(90, 150, 255);
            case EPIC -> Color.fromRGB(190, 90, 255);
            case LEGENDARY -> Color.fromRGB(255, 205, 50);
        };

        var base = player.getLocation().clone().add(0, 1.2, 0);
        player.getWorld().spawnParticle(Particle.DUST, base, 12 + intensity * 8, 0.55, 0.7, 0.55, 0,
                new Particle.DustOptions(primary, 1.1f));
        player.getWorld().spawnParticle(Particle.DUST, base, 8 + intensity * 6, 0.45, 0.55, 0.45, 0,
                new Particle.DustOptions(rareAccent, 1.0f + intensity * 0.08f));

        if (rarity == ItemRarity.RARE || rarity == ItemRarity.EPIC || rarity == ItemRarity.LEGENDARY) {
            player.getWorld().spawnParticle(Particle.END_ROD, base, 10 + intensity * 3, 0.65, 0.9, 0.65, 0.03);
            player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.0f + intensity * 0.03f);
        }

        if (rarity == ItemRarity.LEGENDARY) {
            player.getWorld().spawnParticle(Particle.FIREWORK, base.clone().add(0, 0.4, 0), 16, 0.7, 0.9, 0.7, 0.04);
            player.getWorld().spawnParticle(Particle.EXPLOSION, base.clone().add(0, 0.2, 0), 4, 0.3, 0.2, 0.3, 0.01);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.9f, 1.2f);
        }
    }
}
