package de.thecoolcraft11.hideAndSeek.listener.item;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.seeker.PhantomViewerItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class PhantomViewerMapListener implements Listener {
    private final HideAndSeek plugin;

    public PhantomViewerMapListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDropSnapshotMap(PlayerDropItemEvent event) {
        Item droppedEntity = event.getItemDrop();
        ItemStack stack = droppedEntity.getItemStack();
        if (PhantomViewerItem.isSnapshotNotTagged(plugin, stack)) {
            return;
        }

        long expiryAt = PhantomViewerItem.getSnapshotExpiryMs(plugin, stack);
        long now = System.currentTimeMillis();
        if (expiryAt <= now) {
            if (PhantomViewerItem.expireSnapshotStack(plugin, stack)) {
                droppedEntity.remove();
            } else {
                droppedEntity.setItemStack(stack);
            }
            return;
        }

        long delayTicks = Math.max(1L, (expiryAt - now + 49L) / 50L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!droppedEntity.isValid()) {
                return;
            }
            ItemStack current = droppedEntity.getItemStack();
            if (PhantomViewerItem.isSnapshotNotTagged(plugin, current)) {
                return;
            }
            long currentExpiry = PhantomViewerItem.getSnapshotExpiryMs(plugin, current);
            if (currentExpiry <= 0L || currentExpiry > System.currentTimeMillis()) {
                return;
            }
            if (PhantomViewerItem.expireSnapshotStack(plugin, current)) {
                droppedEntity.remove();
            } else {
                droppedEntity.setItemStack(current);
            }
        }, delayTicks);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickupSnapshotMap(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Item droppedEntity = event.getItem();
        ItemStack stack = droppedEntity.getItemStack();
        if (PhantomViewerItem.isSnapshotNotTagged(plugin, stack)) {
            return;
        }

        long expiryAt = PhantomViewerItem.getSnapshotExpiryMs(plugin, stack);
        if (expiryAt > 0L && expiryAt <= System.currentTimeMillis()) {
            event.setCancelled(true);
            if (PhantomViewerItem.expireSnapshotStack(plugin, stack)) {
                droppedEntity.remove();
            } else {
                droppedEntity.setItemStack(stack);
            }
        }
    }
}

