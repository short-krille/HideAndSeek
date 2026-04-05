package de.thecoolcraft11.hideAndSeek.items.effects.win.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.win.WinSkin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class StarburstVictoryWinSkin implements WinSkin {

    @Override
    public void execute(Player player, boolean hidersWon, HideAndSeek plugin) {
        Color burstColor = hidersWon ? Color.fromRGB(50, 255, 120) : Color.fromRGB(255, 80, 80);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.1f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 0.8f, 0.9f);

        Location center = player.getLocation().clone().add(0, 2.5, 0);


        int rays = 20;
        for (int i = 0; i < rays; i++) {
            double pitch = Math.toRadians((i * 180.0 / rays) - 90);
            double yaw = Math.toRadians(i * (360.0 / rays));

            final double finalDx = Math.cos(pitch) * Math.cos(yaw), finalDy = Math.sin(pitch), finalDz = Math.cos(pitch) * Math.sin(yaw);
            new BukkitRunnable() {
                private int step = 0;

                @Override
                public void run() {
                    if (step >= 8 || !player.isOnline()) {
                        cancel();
                        return;
                    }
                    double scale = step * 0.35;
                    Location pt = center.clone().add(finalDx * scale, finalDy * scale, finalDz * scale);
                    if (pt.getWorld() != null) {
                        pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                                new Particle.DustOptions(burstColor, 1.3f));
                        pt.getWorld().spawnParticle(Particle.END_ROD, pt, 1, 0, 0, 0, 0.02);
                    }
                    step++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }


        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                Location base = player.getLocation().clone().add(0, 1, 0);
                for (int i = 0; i < 30; i++) {
                    double ox = (Math.random() - 0.5) * 3.0;
                    double oy = Math.random() * 3.0;
                    double oz = (Math.random() - 0.5) * 3.0;
                    base.getWorld().spawnParticle(Particle.END_ROD, base.clone().add(ox, oy, oz), 1, 0, 0, 0, 0.03);
                }
                base.getWorld().playSound(base, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.6f, 1.4f);
            }
        }.runTaskLater(plugin, 12L);
    }
}
