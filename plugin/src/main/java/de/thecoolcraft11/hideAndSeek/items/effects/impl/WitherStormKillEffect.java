package de.thecoolcraft11.hideAndSeek.items.effects.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class WitherStormKillEffect implements KillEffect {

    @Override
    public void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin) {
        if (killLocation == null || killLocation.getWorld() == null) {
            return;
        }

        killLocation.getWorld().playSound(killLocation, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.6f);
        killLocation.getWorld().playSound(killLocation, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.7f);


        new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (tick >= 20 || killLocation.getWorld() == null) {
                    cancel();
                    return;
                }

                double angle = tick * 1.1;
                double radius = 1.5 - tick * 0.05;
                double height = tick * 0.12;

                for (int arm = 0; arm < 3; arm++) {
                    double armAngle = angle + (Math.PI * 2 / 3) * arm;
                    Location pt = killLocation.clone().add(
                            Math.cos(armAngle) * radius,
                            height,
                            Math.sin(armAngle) * radius
                    );
                    killLocation.getWorld().spawnParticle(Particle.DUST, pt, 2, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(Color.fromRGB(20, 0, 30), 1.5f));
                    killLocation.getWorld().spawnParticle(Particle.SMOKE, pt, 1, 0.03, 0.03, 0.03, 0.01);
                }


                if (tick == 5 || tick == 12) {
                    killLocation.getWorld().spawnParticle(Particle.WITCH, killLocation.clone().add(0, 1.2, 0),
                            16, 0.5, 0.5, 0.5, 0.05);
                    killLocation.getWorld().playSound(killLocation, Sound.ENTITY_WITHER_HURT, 0.5f, 1.3f);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);


        killLocation.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, killLocation.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
    }
}
