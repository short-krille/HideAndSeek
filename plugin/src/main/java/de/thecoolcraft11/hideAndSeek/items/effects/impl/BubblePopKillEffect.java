package de.thecoolcraft11.hideAndSeek.items.effects.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BubblePopKillEffect implements KillEffect {

    @Override
    public void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin) {
        if (killLocation == null || killLocation.getWorld() == null) {
            return;
        }

        killLocation.getWorld().playSound(killLocation, Sound.ENTITY_GENERIC_SPLASH, 0.9f, 1.4f);
        killLocation.getWorld().playSound(killLocation, Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.7f, 1.6f);


        new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (tick >= 14 || killLocation.getWorld() == null) {
                    cancel();
                    return;
                }

                int count = (tick < 7) ? (tick + 1) : (14 - tick);
                for (int i = 0; i < count; i++) {
                    double offsetX = (Math.random() - 0.5) * 2.0;
                    double offsetY = Math.random() * 1.8;
                    double offsetZ = (Math.random() - 0.5) * 2.0;
                    killLocation.getWorld().spawnParticle(Particle.BUBBLE_POP,
                            killLocation.clone().add(offsetX, offsetY, offsetZ),
                            1, 0.05, 0.05, 0.05, 0);
                }


                if (tick % 3 == 0) {
                    killLocation.getWorld().spawnParticle(Particle.SPLASH,
                            killLocation.clone().add(0, 0.2, 0), 12, 0.6, 0.2, 0.6, 0.1);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
