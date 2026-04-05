package de.thecoolcraft11.hideAndSeek.items.effects.death;

import net.kyori.adventure.text.Component;

public interface DeathMessageSkin {

    Component getEnvironmentalDeathMessage(String victimName, String cause);

    Component getKillMessage(String killerName, String victimName);
}

