package de.thecoolcraft11.hideAndSeek.items.effects.death.impl;

import de.thecoolcraft11.hideAndSeek.items.effects.death.DeathMessageSkin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CreativeDeathMessages implements DeathMessageSkin {

    @Override
    public Component getEnvironmentalDeathMessage(String victimName, String cause) {
        return switch (cause) {
            case "CAMPING" ->
                    base(victimName).append(Component.text(" became a permanent campfire.", NamedTextColor.AQUA));
            case "WORLD_BORDER" ->
                    base(victimName).append(Component.text(" reached the edge of reality.", NamedTextColor.AQUA));
            case "PERK_DEATH_ZONE" ->
                    base(victimName).append(Component.text(" couldn't handle the pressure zone.", NamedTextColor.AQUA));
            case "PERK_RELOCATE" ->
                    base(victimName).append(Component.text(" teleported to oblivion.", NamedTextColor.AQUA));
            default -> base(victimName).append(Component.text(" fell victim to fate.", NamedTextColor.AQUA));
        };
    }

    @Override
    public Component getKillMessage(String killerName, String victimName) {
        return Component.text(killerName, NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(" took ", NamedTextColor.GRAY))
                .append(Component.text(victimName, NamedTextColor.YELLOW))
                .append(Component.text(" down!", NamedTextColor.GRAY));
    }

    private Component base(String victimName) {
        return Component.text(victimName, NamedTextColor.YELLOW);
    }
}

