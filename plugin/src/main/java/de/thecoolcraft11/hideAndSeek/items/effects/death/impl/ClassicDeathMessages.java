package de.thecoolcraft11.hideAndSeek.items.effects.death.impl;

import de.thecoolcraft11.hideAndSeek.items.effects.death.DeathMessageSkin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ClassicDeathMessages implements DeathMessageSkin {

    @Override
    public Component getEnvironmentalDeathMessage(String victimName, String cause) {
        return switch (cause) {
            case "CAMPING" ->
                    base(victimName).append(Component.text(" was struck down for camping too long.", NamedTextColor.YELLOW));
            case "WORLD_BORDER" ->
                    base(victimName).append(Component.text(" was consumed by the world border.", NamedTextColor.YELLOW));
            case "PERK_DEATH_ZONE" ->
                    base(victimName).append(Component.text(" failed to escape the Death Zone.", NamedTextColor.YELLOW));
            case "PERK_RELOCATE" ->
                    base(victimName).append(Component.text(" did not relocate in time.", NamedTextColor.YELLOW));
            default ->
                    base(victimName).append(Component.text(" was eliminated by the environment.", NamedTextColor.YELLOW));
        };
    }

    @Override
    public Component getKillMessage(String killerName, String victimName) {
        return Component.text(killerName, NamedTextColor.RED)
                .append(Component.text(" eliminated ", NamedTextColor.GRAY))
                .append(Component.text(victimName, NamedTextColor.GREEN))
                .append(Component.text(".", NamedTextColor.GRAY));
    }

    private Component base(String victimName) {
        return Component.text(victimName, NamedTextColor.GREEN);
    }
}

