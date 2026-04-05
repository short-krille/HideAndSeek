package de.thecoolcraft11.hideAndSeek.items.effects.win.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.win.WinSkin;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class GoldenRainWinSkin implements WinSkin {

    @Override
    public void execute(Player player, boolean hidersWon, HideAndSeek plugin) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.2f);

        new BukkitRunnable() {
            private static final int DURATION = 50;
            private int tick = 0;

            @Override
            public void run() {
                if (tick >= DURATION || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location base = player.getLocation().clone();


                for (int i = 0; i < 8; i++) {
                    double ox = (Math.random() - 0.5) * 3.0;
                    double oz = (Math.random() - 0.5) * 3.0;
                    Location rain = base.clone().add(ox, 4.0, oz);
                    base.getWorld().spawnParticle(Particle.DUST, rain, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(255, 200, 0), 1.1f));
                }

                if (tick % 2 == 0) {
                    spawnGoldIngotDrops(base, plugin);
                }


                if (tick % 4 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double ox = (Math.random() - 0.5) * 2.5;
                        double oz = (Math.random() - 0.5) * 2.5;
                        base.getWorld().spawnParticle(Particle.DUST,
                                base.clone().add(ox, 0.1, oz), 1, 0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(255, 215, 0), 0.8f));
                    }
                }


                if (tick % 15 == 0) {
                    base.getWorld().playSound(base, Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.0f + (tick / 50.0f));
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnGoldIngotDrops(Location base, HideAndSeek plugin) {
        if (base == null || base.getWorld() == null) {
            return;
        }

        for (int i = 0; i < 4; i++) {
            double ox = (Math.random() - 0.5) * 2.6;
            double oz = (Math.random() - 0.5) * 2.6;
            Location spawnLoc = base.clone().add(ox, 3.8 + Math.random() * 1.8, oz);

            Item item = base.getWorld().dropItem(spawnLoc, new ItemStack(Material.GOLD_INGOT));
            item.setPickupDelay(Integer.MAX_VALUE);
            item.setCanMobPickup(false);
            item.setUnlimitedLifetime(false);
            item.setVelocity(new Vector(
                    (Math.random() - 0.5) * 0.18,
                    0.05 + Math.random() * 0.2,
                    (Math.random() - 0.5) * 0.18
            ));

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (item.isValid()) {
                    item.remove();
                }
            }, 50L);
        }
    }
}
