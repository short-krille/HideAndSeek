package de.thecoolcraft11.hideAndSeek.items;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.hider.KnockbackStickItem;
import de.thecoolcraft11.hideAndSeek.items.hider.SpeedBoostItem;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemSkinSelectionService {

    private static final Map<UUID, Map<String, String>> PLAYER_VARIANTS = new ConcurrentHashMap<>();

    private ItemSkinSelectionService() {
    }

    public static void setSelectedVariant(UUID playerId, String logicalItemId, String variantId) {
        if (variantId == null || variantId.isBlank()) {
            clearSelectedVariant(playerId, logicalItemId);
            return;
        }
        PLAYER_VARIANTS
                .computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(logicalItemId, variantId);
    }

    public static void clearSelectedVariant(UUID playerId, String logicalItemId) {
        Map<String, String> variants = PLAYER_VARIANTS.get(playerId);
        if (variants == null) {
            return;
        }
        variants.remove(logicalItemId);
        if (variants.isEmpty()) {
            PLAYER_VARIANTS.remove(playerId);
        }
    }

    public static String getSelectedVariant(UUID playerId, String logicalItemId) {
        Map<String, String> variants = PLAYER_VARIANTS.get(playerId);
        return variants == null ? null : variants.get(logicalItemId);
    }

    public static String getSelectedVariant(Player player, String logicalItemId) {
        return player == null ? null : getSelectedVariant(player.getUniqueId(), logicalItemId);
    }

    public static boolean isSelected(Player player, String logicalItemId, String variantId) {
        String selected = getSelectedVariant(player, logicalItemId);
        return selected != null && selected.equals(variantId);
    }

    public static void applySelectedVariants(Player player, HideAndSeek plugin) {
        Map<String, String> variants = PLAYER_VARIANTS.get(player.getUniqueId());
        if (variants == null || variants.isEmpty()) {
            return;
        }

        var variantManager = plugin.getCustomItemManager().getVariantManager();
        for (Map.Entry<String, String> entry : variants.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            String runtimeItemId = resolveRuntimeItemId(player, entry.getKey());
            if (!variantManager.hasVariants(runtimeItemId)) {
                continue;
            }
            variantManager.switchVariant(player, runtimeItemId, entry.getValue());
        }
    }

    public static String resolveRuntimeItemId(Player player, String logicalItemId) {
        if (logicalItemId.equals(SpeedBoostItem.ID)) {
            return SpeedBoostItem.ID + "_" + SpeedBoostItem.getSpeedLevel(player.getUniqueId());
        }
        if (logicalItemId.equals(KnockbackStickItem.ID)) {
            return KnockbackStickItem.ID + "_" + KnockbackStickItem.getKnockbackLevel(player.getUniqueId());
        }
        return logicalItemId;
    }

    public static String normalizeLogicalItemId(String itemId) {
        if (itemId.startsWith(SpeedBoostItem.ID + "_")) {
            return SpeedBoostItem.ID;
        }
        if (itemId.startsWith(KnockbackStickItem.ID + "_")) {
            return KnockbackStickItem.ID;
        }
        return itemId;
    }
}



