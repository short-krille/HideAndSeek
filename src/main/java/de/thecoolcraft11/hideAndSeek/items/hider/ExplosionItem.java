package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.util.points.PointAction;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Candle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Set;

public class ExplosionItem implements GameItem {
    public static final String ID = "has_hider_explosion";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.RED_CANDLE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Firecracker", NamedTextColor.RED, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to place a firecracker that will explode", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }

        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        int points = plugin.getPointService().getInt("points.hider.taunt.small", 25);
        return String.format("Place a firecracker that pops, granting %d points.", points);
    }

    @Override
    public void register(HideAndSeek plugin) {
        int cooldown = plugin.getSettingRegistry().get("hider-items.explosion.cooldown", 8);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> spawnExplosionForAll(context, plugin))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK, context -> spawnExplosionForAll(context, plugin))
                .withVanillaCooldown(cooldown * 20)
                .withCustomCooldown(cooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.explosion.cooldown");
    }

    private static void spawnExplosionForAll(ItemInteractionContext context, HideAndSeek plugin) {
        Location location = context.getLocation();
        Player hider = context.getPlayer();
        boolean confetti = ItemSkinSelectionService.isSelected(hider, ID, "skin_confetti_popper");
        boolean bubble = ItemSkinSelectionService.isSelected(hider, ID, "skin_bubble_popper");

        Block block = location.clone().add(0, 1, 0).getBlock();
        if (!block.getType().isAir()) {
            context.skipCooldown();
            return;
        }

        block.setType(confetti ? Material.YELLOW_CANDLE : bubble ? Material.LIGHT_BLUE_CANDLE : Material.RED_CANDLE);

        Candle candle = (Candle) block.getBlockData();
        candle.setLit(true);
        candle.setCandles(1);
        block.setBlockData(candle);

        var tauntPoints = plugin.getPointService().award(hider.getUniqueId(), PointAction.HIDER_TAUNT_SMALL);
        double volume = plugin.getSettingRegistry().get("hider-items.explosion.volume", 0.65);
        double pitch = plugin.getSettingRegistry().get("hider-items.explosion.pitch", 1.5);
        int smokeParticles = plugin.getSettingRegistry().get("hider-items.explosion.smoke-particles", 3);
        int fuseTime = plugin.getSettingRegistry().get("hider-items.explosion.fuse-time", 40);

        hider.sendMessage(
                Component.text("You used a taunt!", NamedTextColor.GREEN)
        );
        hider.sendMessage(
                Component.text("+" + tauntPoints + " points", NamedTextColor.GOLD)
        );

        int smokeTaskId = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> {
                    Location smokeLoc = location.clone().add(0.5, 1.6, 0.5);
                    location.getWorld().spawnParticle(
                            bubble ? Particle.BUBBLE : confetti ? Particle.FIREWORK : Particle.SMOKE,
                            smokeLoc,
                            smokeParticles,
                            0.05, 0.1, 0.05,
                            0.01
                    );

                    location.getWorld().spawnParticle(
                            bubble ? Particle.BUBBLE_POP : confetti ? Particle.HAPPY_VILLAGER : Particle.FLAME,
                            smokeLoc.clone().add(0, -0.2, 0),
                            bubble ? 3 : 1,
                            0.05, 0.05, 0.05,
                            0.05
                    );
                },
                0L,
                4L
        ).getTaskId();

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {
                    Bukkit.getScheduler().cancelTask(smokeTaskId);

                    block.setType(Material.AIR);

                    Location explosionLoc = location.clone().add(0.5, 1.5, 0.5);
                    for (Player target : Bukkit.getOnlinePlayers()) {
                        target.getWorld().spawnParticle(
                                bubble ? Particle.SPLASH : confetti ? Particle.FIREWORK : Particle.EXPLOSION,
                                explosionLoc,
                                bubble ? 14 : 1,
                                0, 0, 0, 0
                        );

                        if (bubble) {
                            target.getWorld().spawnParticle(
                                    Particle.BUBBLE_POP,
                                    explosionLoc,
                                    20,
                                    0.3, 0.3, 0.3,
                                    0.03
                            );
                        } else {
                            target.getWorld().spawnParticle(
                                    Particle.DUST,
                                    explosionLoc,
                                    15,
                                    0.3, 0.3, 0.3,
                                    new Particle.DustOptions(
                                            Color.fromARGB(255, 255, confetti ? 220 : 100, confetti ? 120 : 0),
                                            1.0f)
                            );
                        }
                        target.playSound(
                                location,
                                bubble ? Sound.ITEM_BOTTLE_FILL : confetti ? Sound.ENTITY_FIREWORK_ROCKET_BLAST : Sound.ENTITY_GENERIC_EXPLODE,
                                (float) volume,
                                bubble ? 1.6f : (float) pitch
                        );
                    }
                },
                fuseTime
        );
    }
}
