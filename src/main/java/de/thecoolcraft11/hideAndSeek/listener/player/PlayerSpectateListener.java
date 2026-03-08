package de.thecoolcraft11.hideAndSeek.listener.player;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import de.thecoolcraft11.hideAndSeek.util.DataController;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerSpectateListener implements Listener {

    @EventHandler
    public void onStartSpectate(PlayerStartSpectatingEntityEvent event) {
        if (DataController.getInstance().getHiders().contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            ((CraftPlayer) event.getPlayer()).getHandle().setCamera(((CraftPlayer) event.getPlayer()).getHandle());
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (DataController.getInstance().getHiders().contains(event.getPlayer().getUniqueId())) {
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onHotbarScroll(PlayerItemHeldEvent event) {
        if (DataController.getInstance().getHiders().contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(false);
            event.getPlayer().getInventory().setHeldItemSlot(event.getNewSlot());
        }
    }
}
