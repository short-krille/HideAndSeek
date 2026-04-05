package de.thecoolcraft11.hideAndSeek.items.effects.win.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.win.WinSkin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TriumphantAuraWinSkin implements WinSkin {

    @Override
    public void execute(Player player, boolean hidersWon, HideAndSeek plugin) {
        Color auraColor = hidersWon ? Color.fromRGB(0, 220, 80) : Color.fromRGB(220, 50, 50);
        Color glowColor = Color.fromRGB(255, 215, 0);

        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        new BukkitRunnable() {
            private static final int DURATION = 60;
            private int tick = 0;

            @Override
            public void run() {
                if (tick >= DURATION || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location center = player.getLocation().clone().add(0, 1, 0);
                double angle = tick * 0.25;


                for (int ring = 0; ring < 2; ring++) {
                    double ringAngle = angle + ring * Math.PI;
                    double radius = 1.0;
                    Location pt = center.clone().add(
                            Math.cos(ringAngle) * radius,
                            Math.sin(tick * 0.18) * 0.4,
                            Math.sin(ringAngle) * radius
                    );
                    player.getWorld().spawnParticle(Particle.DUST, pt, 2, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(auraColor, 1.2f));
                }


                if (tick % 3 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double ox = (Math.random() - 0.5) * 1.2;
                        double oz = (Math.random() - 0.5) * 1.2;
                        player.getWorld().spawnParticle(Particle.DUST,
                                center.clone().add(ox, Math.random() * 1.5, oz),
                                1, 0, 0, 0, 0,
                                new Particle.DustOptions(glowColor, 0.9f));
                    }
                }


                if (tick == 20) {
                    player.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 1.5, 0), 24, 0.4, 0.1, 0.4, 0.08);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
