package de.thecoolcraft11.hideAndSeek.items.effects.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RainbowBlastKillEffect implements KillEffect {

    private static final Color[] RAINBOW = {
            Color.fromRGB(255, 0, 0),
            Color.fromRGB(255, 127, 0),
            Color.fromRGB(255, 255, 0),
            Color.fromRGB(0, 255, 0),
            Color.fromRGB(0, 100, 255),
            Color.fromRGB(148, 0, 211)
    };

    @Override
    public void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin) {
        if (killLocation == null || killLocation.getWorld() == null) {
            return;
        }

        killLocation.getWorld().playSound(killLocation, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.3f);
        killLocation.getWorld().playSound(killLocation, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, 1.0f);


        new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (tick >= RAINBOW.length) {
                    cancel();
                    return;
                }
                if (killLocation.getWorld() == null) {
                    cancel();
                    return;
                }

                Color color = RAINBOW[tick];
                double radius = 0.5 + tick * 0.45;
                double height = tick * 0.3;

                for (int i = 0; i < 24; i++) {
                    double angle = Math.toRadians(i * 15);
                    Location pt = killLocation.clone().add(
                            Math.cos(angle) * radius,
                            height,
                            Math.sin(angle) * radius
                    );
                    killLocation.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(color, 1.4f));
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
