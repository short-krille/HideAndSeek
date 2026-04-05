package de.thecoolcraft11.hideAndSeek.items.effects.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ThunderStrikeKillEffect implements KillEffect {

    @Override
    public void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin) {
        if (killLocation == null || killLocation.getWorld() == null) {
            return;
        }

        killLocation.getWorld().playSound(killLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
        killLocation.getWorld().playSound(killLocation, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.1f);


        for (int i = 0; i < 20; i++) {
            Location col = killLocation.clone().add(0, i * 0.25, 0);
            killLocation.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, col, 4, 0.08, 0, 0.08, 0.1);
        }


        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            double radius = 2.2;
            Location ring = killLocation.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
            killLocation.getWorld().spawnParticle(Particle.DUST, ring, 2, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.2f));
        }


        new BukkitRunnable() {
            @Override
            public void run() {
                if (killLocation.getWorld() == null) return;
                killLocation.getWorld().spawnParticle(Particle.SMOKE, killLocation.clone().add(0, 0.2, 0), 30, 0.5, 0.4, 0.5, 0.03);
                killLocation.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, killLocation.clone().add(0, 0.5, 0), 20, 0.4, 0.2, 0.4, 0.05);
                killLocation.getWorld().playSound(killLocation, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.5f);
            }
        }.runTaskLater(plugin, 8L);
    }
}
