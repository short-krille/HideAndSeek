package de.thecoolcraft11.hideAndSeek.items.effects.death;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeathMessageManager {

    private static final Map<String, DeathMessageSkin> DEATH_MESSAGE_SKINS = new ConcurrentHashMap<>();

    private DeathMessageManager() {
    }

    public static void registerDeathMessageSkin(String id, DeathMessageSkin skin) {
        if (id == null || id.isBlank() || skin == null) {
            return;
        }
        DEATH_MESSAGE_SKINS.put(id, skin);
    }

    public static DeathMessageSkin getDeathMessageSkin(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return DEATH_MESSAGE_SKINS.get(id);
    }

    public static void clear() {
        DEATH_MESSAGE_SKINS.clear();
    }
}

