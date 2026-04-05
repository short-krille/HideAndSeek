package de.thecoolcraft11.hideAndSeek.items.effects.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PhantomSlashKillEffect implements KillEffect {

    @Override
    public void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin) {
        if (killLocation == null || killLocation.getWorld() == null) {
            return;
        }

        killLocation.getWorld().playSound(killLocation, Sound.ENTITY_PHANTOM_AMBIENT, 1.0f, 0.8f);
        killLocation.getWorld().playSound(killLocation, Sound.ENTITY_PHANTOM_DEATH, 0.7f, 1.2f);


        for (int arc = 0; arc < 2; arc++) {
            final int a = arc;
            new BukkitRunnable() {
                private final double startAngle = (a == 0) ? -45 : 135;
                private int step = 0;

                @Override
                public void run() {
                    if (step >= 16 || killLocation.getWorld() == null) {
                        cancel();
                        return;
                    }
                    double angle = Math.toRadians(startAngle + step * 11.25);
                    double radius = 1.4;
                    Location pt = killLocation.clone().add(
                            Math.cos(angle) * radius,
                            0.8 + step * 0.05,
                            Math.sin(angle) * radius
                    );
                    killLocation.getWorld().spawnParticle(Particle.DUST, pt, 3, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(Color.fromRGB(80, 0, 180), 1.3f));
                    killLocation.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, pt, 1, 0.05, 0.05, 0.05, 0.01);
                    step++;
                }
            }.runTaskTimer(plugin, a * 4L, 1L);
        }


        new BukkitRunnable() {
            @Override
            public void run() {
                if (killLocation.getWorld() == null) return;
                killLocation.getWorld().spawnParticle(Particle.SOUL, killLocation.clone().add(0, 1, 0), 20, 0.5, 0.8, 0.5, 0.05);
                killLocation.getWorld().spawnParticle(Particle.ASH, killLocation.clone().add(0, 0.5, 0), 30, 0.8, 0.5, 0.8, 0.02);
            }
        }.runTaskLater(plugin, 10L);
    }
}
