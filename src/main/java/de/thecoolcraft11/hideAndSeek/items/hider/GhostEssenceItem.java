package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Set;

public class GhostEssenceItem implements GameItem {
    public static final String ID = "has_hider_ghost_essence";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.GHAST_TEAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Ghostly Essence", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Pass through walls for 10 seconds", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("You cannot descend while ghostly!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void register(HideAndSeek plugin) {
        int cooldown = plugin.getSettingRegistry().get("hider-items.ghost-essence.cooldown", 25);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, ctx -> useGhostEssence(ctx.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, ctx -> useGhostEssence(ctx.getPlayer(), plugin))
                .withDescription(getDescription())
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(cooldown * 20)
                .withCustomCooldown(cooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    private void useGhostEssence(Player player, HideAndSeek plugin) {
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) return;

        final Location startLoc = player.getLocation().clone();

        int maxRadius = plugin.getSettingRegistry().get("hider-items.ghost-essence.max-radius", 15);
        int minLightBlock = plugin.getSettingRegistry().get("hider-items.ghost-essence.min-light-block", 1);
        int minLightSky = plugin.getSettingRegistry().get("hider-items.ghost-essence.min-light-sky", 1);
        float flyingSpeed = plugin.getSettingRegistry().get("hider-items.ghost-essence.flying-speed", 0.01f);
        int maxDurationSeconds = plugin.getSettingRegistry().get("hider-items.ghost-essence.max-duration", 3);

        plugin.getNmsAdapter().setServerGameModeSpectator(player);
        plugin.getNmsAdapter().spoofClientGameMode(player, GameMode.SURVIVAL);

        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(flyingSpeed);

        player.sendMessage(Component.text("You are now a Ghost! Phasing enabled for " + maxDurationSeconds + "s.", NamedTextColor.AQUA));
        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_WARN, 1f, 1.2f);
        Location start = player.getLocation().clone();

        org.bukkit.entity.Zombie ghost = start.getWorld().spawn(start, org.bukkit.entity.Zombie.class, s -> {
            s.setAI(true);
            s.setInvisible(true);
            s.setPersistent(true);
            s.setSilent(true);
        });

        Bukkit.getOnlinePlayers().forEach(player1 -> player1.hideEntity(plugin, ghost));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxDurationSeconds * 20) {
                    this.cancel();
                    Location endLoc = player.getLocation();


                    player.setGameMode(GameMode.SURVIVAL);
                    plugin.getNmsAdapter().spoofClientGameMode(player, GameMode.SURVIVAL);
                    player.setAllowFlight(false);
                    player.setFlying(false);


                    boolean isCheating = false;
                    String reason = "";


                    if (!player.getWorld().getWorldBorder().isInside(endLoc)) {
                        isCheating = true;
                        reason = "You cannot materialize outside the world border!";
                    } else if (endLoc.distance(startLoc) > maxRadius) {
                        isCheating = true;
                        reason = "You wandered too far from your physical body!";
                    } else if (endLoc.getBlock().getType().isSolid() || player.getEyeLocation().getBlock().getType().isSolid()) {
                        isCheating = true;
                        reason = "You materialized inside a wall!";
                    } else if (endLoc.getBlock().getLightLevel() < minLightBlock && endLoc.getBlock().getLightFromSky() < minLightSky) {
                        isCheating = true;
                        reason = "It's too dark and cramped to materialize here!";
                    } else if (!canPathfindBack(plugin, ghost, startLoc, endLoc)) {
                        isCheating = true;
                        reason = "There is no way to reach this location!";
                    }

                    if (isCheating) {
                        player.teleport(startLoc);
                        player.sendMessage(Component.text(reason, NamedTextColor.RED));
                        player.playSound(startLoc, Sound.ENTITY_GHAST_HURT, 1f, 1f);
                    } else {
                        player.sendMessage(Component.text("You have successfully materialized!", NamedTextColor.GREEN));
                        player.playSound(endLoc, Sound.ENTITY_GHAST_DEATH, 1f, 1f);
                    }
                    if (ghost.isValid()) ghost.remove();
                    return;
                }

                if (player.getLocation().distanceSquared(startLoc) > (double) (maxRadius * maxRadius)) {

                    org.bukkit.util.Vector direction = player.getLocation().toVector().subtract(startLoc.toVector()).normalize();


                    double boundaryDist = Math.max(0.2, maxRadius - 0.2);
                    Location boundaryLocation = startLoc.clone().add(direction.multiply(boundaryDist));


                    boundaryLocation.setYaw(player.getLocation().getYaw());
                    boundaryLocation.setPitch(player.getLocation().getPitch());


                    player.teleport(boundaryLocation);
                }

                if (ticks % 20 == 0) {
                    Bukkit.getOnlinePlayers().forEach(player1 -> player1.hideEntity(plugin, ghost));
                    ghost.setTarget(player);
                }


                if (ticks % 5 == 0) {
                    player.getWorld().spawnParticle(org.bukkit.Particle.SOUL, player.getLocation().add(0, 1, 0), 3, 0.1, 0.1, 0.1, 0.02);
                }

                if (ticks < maxDurationSeconds * 20 - 20) ghost.teleport(start);

                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private boolean canPathfindBack(HideAndSeek plugin, Mob ghost, Location start, Location end) {

        if (!start.getWorld().equals(end.getWorld())) return false;

        if (!plugin.getNmsAdapter().hasCapability(NmsCapabilities.MOB_PATHFINDING)) {
            return true;
        }

        return plugin.getNmsAdapter().canPathfind(ghost, start, end);
    }

    @Override
    public String getDescription() {
        return "Phase through blocks but stay above your starting height.";
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of(
                "hider-items.ghost-essence.cooldown",
                "hider-items.ghost-essence.max-radius",
                "hider-items.ghost-essence.min-light-block",
                "hider-items.ghost-essence.min-light-sky",
                "hider-items.ghost-essence.flying-speed",
                "hider-items.ghost-essence.max-duration"
        );
    }
}