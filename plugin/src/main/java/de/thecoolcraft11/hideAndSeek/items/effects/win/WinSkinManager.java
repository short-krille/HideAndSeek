package de.thecoolcraft11.hideAndSeek.items.effects.win;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WinSkinManager {

    private static final Map<String, WinSkin> WIN_SKINS = new ConcurrentHashMap<>();

    private WinSkinManager() {
    }

    public static void registerWinSkin(String id, WinSkin winSkin) {
        if (id == null || id.isBlank() || winSkin == null) {
            return;
        }
        WIN_SKINS.put(id, winSkin);
    }

    public static WinSkin getWinSkin(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return WIN_SKINS.get(id);
    }

    public static void clear() {
        WIN_SKINS.clear();
    }
}
