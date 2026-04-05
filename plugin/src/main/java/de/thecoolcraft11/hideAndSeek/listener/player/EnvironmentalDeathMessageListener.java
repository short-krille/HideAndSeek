package de.thecoolcraft11.hideAndSeek.listener.player;

import de.thecoolcraft11.hideAndSeek.items.effects.death.DeathMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class EnvironmentalDeathMessageListener implements Listener {
    private final PlayerHitListener playerHitListener;
    private final DeathMessageService deathMessageService;

    public EnvironmentalDeathMessageListener(PlayerHitListener playerHitListener, DeathMessageService deathMessageService) {
        this.playerHitListener = playerHitListener;
        this.deathMessageService = deathMessageService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        var victim = event.getEntity();
        PlayerHitListener.EnvironmentalDeathCause cause = playerHitListener.peekEnvironmentalDeathCause(victim.getUniqueId());
        if (cause == null) {
            return;
        }

        java.util.UUID attributorId = playerHitListener.peekEnvironmentalDeathAttributor(victim.getUniqueId());
        org.bukkit.entity.Player attributor = attributorId == null ? null : org.bukkit.Bukkit.getPlayer(attributorId);

        Component customMessage = (attributor != null)
                ? deathMessageService.getSeekerPerkDeathMessage(attributor, victim, cause)
                : deathMessageService.getEnvironmentalDeathMessage(victim, cause, victim);

        if (customMessage != null) {
            event.deathMessage(customMessage);
            return;
        }

        Component fallback = switch (cause) {
            case CAMPING -> Component.text(victim.getName(), NamedTextColor.GREEN)
                    .append(Component.text(" was struck down for camping too long.", NamedTextColor.YELLOW));
            case WORLD_BORDER -> Component.text(victim.getName(), NamedTextColor.GREEN)
                    .append(Component.text(" was consumed by the world border.", NamedTextColor.YELLOW));
            case PERK_DEATH_ZONE -> Component.text(victim.getName(), NamedTextColor.GREEN)
                    .append(Component.text(" failed to escape the Death Zone.", NamedTextColor.YELLOW));
            case PERK_RELOCATE -> Component.text(victim.getName(), NamedTextColor.GREEN)
                    .append(Component.text(" did not relocate in time.", NamedTextColor.YELLOW));
            default -> Component.text(victim.getName(), NamedTextColor.GREEN)
                    .append(Component.text(" was eliminated by the environment.", NamedTextColor.YELLOW));
        };
        event.deathMessage(fallback);
    }
}
