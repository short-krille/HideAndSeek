package de.thecoolcraft11.hideAndSeek.util.setting;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;

public class SettingChangeListener {
    private final HideAndSeek plugin;

    public SettingChangeListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void onSettingChange(String key, Object oldValue, Object newValue) {
        SeekerItems.reregisterSpecificItem(key, plugin);
        HiderItems.reregisterSpecificItem(key, plugin);
    }

    public static void register(HideAndSeek plugin) {
        SettingChangeListener listener = new SettingChangeListener(plugin);

        SeekerItems.getAllConfigKeys().forEach(key ->
                plugin.getSettingRegistry().onSettingChange(key, listener::onSettingChange));

        HiderItems.getAllConfigKeys().forEach(key ->
                plugin.getSettingRegistry().onSettingChange(key, listener::onSettingChange));
    }
}