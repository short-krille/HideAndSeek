package de.thecoolcraft11.hideAndSeek.items.effects.win.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.win.WinSkin;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class FireworkCelebrationWinSkin implements WinSkin {

    @Override
    public void execute(Player player, boolean hidersWon, HideAndSeek plugin) {
        Color primary = hidersWon ? Color.GREEN : Color.RED;
        Color secondary = hidersWon ? Color.LIME : Color.ORANGE;

        new BukkitRunnable() {
            private int shot = 0;

            @Override
            public void run() {
                if (shot >= 3 || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation().clone().add(
                        (Math.random() - 0.5) * 1.5,
                        0.5,
                        (Math.random() - 0.5) * 1.5
                );

                Firework fw = loc.getWorld().spawn(loc, Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                FireworkEffect.Builder builder = FireworkEffect.builder()
                        .with(shot % 2 == 0 ? FireworkEffect.Type.STAR : FireworkEffect.Type.BURST)
                        .withColor(primary)
                        .withFade(secondary)
                        .withTrail();
                if (shot == 2) {
                    builder.withFlicker();
                }
                meta.addEffect(builder.build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);

                shot++;
            }
        }.runTaskTimer(plugin, 0L, 8L);
    }
}
