package de.thecoolcraft11.hideAndSeek.items.effects;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.listener.player.PlayerHitListener.EnvironmentalDeathCause;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class KillEffectService {

    private final HideAndSeek plugin;

    public KillEffectService(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void triggerKillEffect(Player killer, Player victim, Location killLocation) {
        if (killer == null || victim == null || killLocation == null) {
            return;
        }

        String selectedKillEffectId = ItemSkinSelectionService.getSelectedVariant(killer, KillEffectSkins.LOGICAL_ITEM_ID);
        if (selectedKillEffectId == null || selectedKillEffectId.isBlank()) {
            return;
        }
        if (!ItemSkinSelectionService.isUnlocked(killer.getUniqueId(), KillEffectSkins.LOGICAL_ITEM_ID, selectedKillEffectId)) {
            return;
        }

        KillEffect killEffect = KillEffectManager.getKillEffect(selectedKillEffectId);

        if (killEffect == null) {
            return;
        }

        try {
            killEffect.execute(killer, victim, killLocation, plugin);
            playScaledFinisher(killLocation, selectedKillEffectId);
        } catch (Exception e) {
            plugin.getLogger().warning("Error executing kill effect: " + e.getMessage());
        }
    }

    private void playScaledFinisher(Location loc, String variantId) {
        if (loc == null || loc.getWorld() == null || variantId == null || variantId.isBlank()) {
            return;
        }

        ItemRarity rarity = ItemSkinSelectionService.getRarity(KillEffectSkins.LOGICAL_ITEM_ID, variantId);
        int cost = ItemSkinSelectionService.getCost(plugin, KillEffectSkins.LOGICAL_ITEM_ID, variantId);
        int intensity = Math.clamp(cost / 300, 1, 6);

        Color accent = switch (rarity) {
            case COMMON -> Color.fromRGB(220, 220, 220);
            case UNCOMMON -> Color.fromRGB(120, 255, 120);
            case RARE -> Color.fromRGB(80, 160, 255);
            case EPIC -> Color.fromRGB(190, 90, 255);
            case LEGENDARY -> Color.fromRGB(255, 190, 40);
        };

        loc.getWorld().spawnParticle(
                Particle.DUST,
                loc.clone().add(0, 1.0, 0),
                10 + (intensity * 8),
                0.55,
                0.65,
                0.55,
                0,
                new Particle.DustOptions(accent, 1.0f + (intensity * 0.08f))
        );
        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1.0, 0), 4 + intensity * 2, 0.45, 0.5, 0.45, 0.02);

        if (rarity == ItemRarity.EPIC || rarity == ItemRarity.LEGENDARY) {
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 1.2, 0), 2 + intensity, 0.5, 0.35, 0.5, 0.01);
            loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.45f + (intensity * 0.05f), 1.05f);
        }

        if (rarity == ItemRarity.LEGENDARY) {
            loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 0.7, 0), 1, 0.2, 0.15, 0.2, 0.01);
            loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.7f, 1.2f);
        }
    }

    public void triggerEnvironmentalKillEffect(Player victim, Location killLocation, EnvironmentalDeathCause cause) {
        if (victim == null || killLocation == null || killLocation.getWorld() == null || cause == null) {
            return;
        }

        switch (cause) {
            case WORLD_BORDER -> playWorldBorderEffect(killLocation);
            case CAMPING -> playCampingEffect(killLocation);
            case PERK_DEATH_ZONE -> playDeathZoneEffect(killLocation);
            case PERK_RELOCATE -> playRelocateEffect(killLocation);
            default -> playGenericEnvironmentalEffect(killLocation);
        }
    }

    private void playWorldBorderEffect(Location loc) {
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.6f);
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.9f, 0.5f);


        for (int i = 0; i < 48; i++) {
            double angle = Math.toRadians(i * 7.5);
            double radius = 1.8;
            Location pt = loc.clone().add(Math.cos(angle) * radius, 0.8, Math.sin(angle) * radius);
            loc.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 50, 50), 1.3f));
        }
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 0.5, 0), 5, 0.4, 0.4, 0.4, 0.1);
        loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 0.3, 0), 25, 0.5, 0.4, 0.5, 0.04);
    }

    private void playCampingEffect(Location loc) {
        loc.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_DEATH, 0.7f, 0.9f);

        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 0.3, 0), 40, 0.5, 0.7, 0.5, 0.08);
        loc.getWorld().spawnParticle(Particle.LAVA, loc.clone().add(0, 0.5, 0), 12, 0.4, 0.3, 0.4, 0.05);
        loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 1.2, 0), 30, 0.4, 0.3, 0.4, 0.03);


        for (int i = 0; i < 16; i++) {
            double angle = Math.toRadians(i * 22.5);
            Location pt = loc.clone().add(Math.cos(angle) * 1.2, 0.1, Math.sin(angle) * 1.2);
            loc.getWorld().spawnParticle(Particle.SMALL_FLAME, pt, 2, 0.05, 0.1, 0.05, 0.01);
        }
    }

    private void playDeathZoneEffect(Location loc) {
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, 0.7f, 1.1f);

        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 0.5, 0), 2, 0.2, 0.2, 0.2, 0.05);
        loc.getWorld().spawnParticle(Particle.WITCH, loc.clone().add(0, 1, 0), 30, 0.6, 0.6, 0.6, 0.08);
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.5, 0), 20, 0.6, 0.6, 0.6, 0,
                new Particle.DustOptions(Color.fromRGB(120, 0, 200), 1.5f));
    }

    private void playRelocateEffect(Location loc) {
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.9f);
        loc.getWorld().playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.3f);

        loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 60, 0.4, 0.8, 0.4, 0.3);
        loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 0.5, 0), 20, 0.3, 0.5, 0.3, 0.1);
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1.2, 0), 15, 0.3, 0.5, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(60, 0, 160), 1.2f));
    }

    private void playGenericEnvironmentalEffect(Location loc) {
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 0.5, 0), 6, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 0.5, 0), 20, 0.4, 0.4, 0.4, 0.04);
    }
}
