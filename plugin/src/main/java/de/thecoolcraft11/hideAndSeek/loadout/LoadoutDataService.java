package de.thecoolcraft11.hideAndSeek.loadout;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class LoadoutDataService {

    private static final Map<UUID, PlayerLoadout> PLAYER_LOADOUTS = new ConcurrentHashMap<>();
    private static final Map<LoadoutRole, LoadoutFilterMode> FILTER_MODES = new EnumMap<>(LoadoutRole.class);
    private static final Map<LoadoutRole, Set<LoadoutItemType>> FILTER_ITEMS = new EnumMap<>(LoadoutRole.class);
    private static final Map<LoadoutRole, Set<String>> DISABLED_PERKS = new EnumMap<>(LoadoutRole.class);
    private static final Map<LoadoutRole, Map<Integer, AdminRolePreset>> ADMIN_ROLE_PRESETS = new EnumMap<>(LoadoutRole.class);
    private static final Map<LoadoutRole, Boolean> RESTRICT_TO_ADMIN_PRESETS = new EnumMap<>(LoadoutRole.class);
    private static final Map<LoadoutRole, Integer> FORCED_ROLE_PRESET_SLOT = new EnumMap<>(LoadoutRole.class);
    private static boolean GLOBAL_LOADOUT_LOCK;
    private static File dataFile;
    private static YamlConfiguration dataConfig;

    private LoadoutDataService() {
    }

    public static void initialize(HideAndSeek plugin) {
        resetAdminDefaults();
        dataFile = new File(plugin.getDataFolder(), "loadout-data.yml");
        if (!dataFile.exists()) {
            try {
                File parent = dataFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    if (!parent.mkdirs()) {
                        plugin.getLogger().warning("Failed to create parent directories for loadout data file");
                    }
                }
                if (!dataFile.createNewFile()) {
                    plugin.getLogger().warning("Failed to create loadout data file");
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create loadout data file: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAll(plugin);
        loadAdminPolicy();
    }

    public static PlayerLoadout getLoadout(UUID playerId) {
        return PLAYER_LOADOUTS.computeIfAbsent(playerId, k -> new PlayerLoadout());
    }

    public static void loadPlayer(HideAndSeek plugin, UUID playerId) {
        if (dataConfig == null) {
            initialize(plugin);
        }

        String basePath = "players." + playerId;
        PlayerLoadout loadout = new PlayerLoadout();


        List<String> hiderItemsStr = dataConfig.getStringList(basePath + ".hider-items");
        for (String itemStr : hiderItemsStr) {
            try {
                LoadoutItemType item = LoadoutItemType.valueOf(itemStr);
                if (item.isForHiders()) {
                    loadout.addHiderItemForced(item, item.getRarity().getDefaultCost());
                }
            } catch (IllegalArgumentException ignored) {
            }
        }


        List<String> seekerItemsStr = dataConfig.getStringList(basePath + ".seeker-items");
        for (String itemStr : seekerItemsStr) {
            try {
                LoadoutItemType item = LoadoutItemType.valueOf(itemStr);
                if (item.isForSeekers()) {
                    loadout.addSeekerItemForced(item, item.getRarity().getDefaultCost());
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        loadout.setHiderLocked(dataConfig.getBoolean(basePath + ".lock-hider", false));
        loadout.setSeekerLocked(dataConfig.getBoolean(basePath + ".lock-seeker", false));
        loadout.setSelectedAdminPresetSlot(LoadoutRole.HIDER, dataConfig.getInt(basePath + ".selected-admin-preset.hider", 0));
        loadout.setSelectedAdminPresetSlot(LoadoutRole.SEEKER, dataConfig.getInt(basePath + ".selected-admin-preset.seeker", 0));

        for (int presetSlot = 1; presetSlot <= PlayerLoadout.MAX_PRESETS; presetSlot++) {
            List<String> hiderItemsRaw = dataConfig.getStringList(basePath + ".presets." + presetSlot + ".hider");
            List<String> seekerItemsRaw = dataConfig.getStringList(basePath + ".presets." + presetSlot + ".seeker");

            LinkedHashSet<LoadoutItemType> hiderItems = new LinkedHashSet<>();
            for (String itemStr : hiderItemsRaw) {
                try {
                    LoadoutItemType item = LoadoutItemType.valueOf(itemStr);
                    if (item.isForHiders()) {
                        hiderItems.add(item);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }

            LinkedHashSet<LoadoutItemType> seekerItems = new LinkedHashSet<>();
            for (String itemStr : seekerItemsRaw) {
                try {
                    LoadoutItemType item = LoadoutItemType.valueOf(itemStr);
                    if (item.isForSeekers()) {
                        seekerItems.add(item);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (!hiderItems.isEmpty() || !seekerItems.isEmpty()) {
                loadout.setPreset(presetSlot, hiderItems, seekerItems);
            }
        }

        PLAYER_LOADOUTS.put(playerId, loadout);
    }

    public static void savePlayer(HideAndSeek plugin, UUID playerId) {
        savePlayer(plugin, playerId, true);
    }

    public static void savePlayer(HideAndSeek plugin, UUID playerId, boolean flush) {
        if (dataConfig == null) {
            initialize(plugin);
        }

        PlayerLoadout loadout = PLAYER_LOADOUTS.get(playerId);
        if (loadout == null) {
            return;
        }

        String basePath = "players." + playerId;


        List<String> hiderItems = loadout.getHiderItems().stream()
                .map(LoadoutItemType::name)
                .sorted()
                .toList();
        dataConfig.set(basePath + ".hider-items", hiderItems);


        List<String> seekerItems = loadout.getSeekerItems().stream()
                .map(LoadoutItemType::name)
                .sorted()
                .toList();
        dataConfig.set(basePath + ".seeker-items", seekerItems);
        dataConfig.set(basePath + ".lock-hider", loadout.isHiderLocked());
        dataConfig.set(basePath + ".lock-seeker", loadout.isSeekerLocked());
        dataConfig.set(basePath + ".selected-admin-preset.hider", loadout.getSelectedAdminPresetSlot(LoadoutRole.HIDER));
        dataConfig.set(basePath + ".selected-admin-preset.seeker", loadout.getSelectedAdminPresetSlot(LoadoutRole.SEEKER));

        String presetsPath = basePath + ".presets";
        dataConfig.set(presetsPath, null);
        for (int presetSlot = 1; presetSlot <= PlayerLoadout.MAX_PRESETS; presetSlot++) {
            if (!loadout.hasPreset(presetSlot)) {
                continue;
            }
            PlayerLoadout.Preset preset = loadout.getPreset(presetSlot);
            List<String> hiderItems2 = preset.hiderItems.stream().map(Enum::name).toList();
            List<String> seekerItems2 = preset.seekerItems.stream().map(Enum::name).toList();
            dataConfig.set(presetsPath + "." + presetSlot + ".hider", hiderItems2);
            dataConfig.set(presetsPath + "." + presetSlot + ".seeker", seekerItems2);
        }

        if (flush) {
            saveData(plugin);
        }
    }

    public static void saveAll(HideAndSeek plugin) {
        if (dataConfig == null) {
            initialize(plugin);
        }
        for (UUID playerId : new HashSet<>(PLAYER_LOADOUTS.keySet())) {
            savePlayer(plugin, playerId, false);
        }
        saveAdminPolicy(plugin, false);
        saveData(plugin);
    }

    public static Set<UUID> getAllKnownPlayerIds() {
        Set<UUID> ids = new HashSet<>(PLAYER_LOADOUTS.keySet());
        if (dataConfig == null) {
            return ids;
        }

        org.bukkit.configuration.ConfigurationSection players = dataConfig.getConfigurationSection("players");
        if (players == null) {
            return ids;
        }

        for (String key : players.getKeys(false)) {
            try {
                ids.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ids;
    }

    public static LoadoutFilterMode getFilterMode(LoadoutRole role) {
        return FILTER_MODES.getOrDefault(role, LoadoutFilterMode.BLACKLIST);
    }

    public static void setFilterMode(LoadoutRole role, LoadoutFilterMode mode) {
        FILTER_MODES.put(role, mode == null ? LoadoutFilterMode.BLACKLIST : mode);
    }

    public static Set<LoadoutItemType> getFilterItems(LoadoutRole role) {
        return EnumSet.copyOf(FILTER_ITEMS.getOrDefault(role, EnumSet.noneOf(LoadoutItemType.class)));
    }

    public static void setFilterItems(LoadoutRole role, Set<LoadoutItemType> items) {
        FILTER_ITEMS.put(role, items == null || items.isEmpty() ? EnumSet.noneOf(LoadoutItemType.class) : EnumSet.copyOf(items));
    }

    public static Set<String> getDisabledPerks(LoadoutRole role) {
        return Set.copyOf(DISABLED_PERKS.getOrDefault(role, Set.of()));
    }

    public static void setDisabledPerks(LoadoutRole role, Set<String> perkIds) {
        DISABLED_PERKS.put(role, perkIds == null ? new HashSet<>() : new HashSet<>(perkIds));
    }

    public static AdminRolePreset getAdminPreset(LoadoutRole role, int slot) {
        Map<Integer, AdminRolePreset> bySlot = ADMIN_ROLE_PRESETS.computeIfAbsent(role, ignored -> new HashMap<>());
        return bySlot.computeIfAbsent(slot, ignored -> new AdminRolePreset());
    }

    public static AdminRolePreset getAdminPresetOrNull(LoadoutRole role, int slot) {
        return ADMIN_ROLE_PRESETS.getOrDefault(role, Map.of()).get(slot);
    }

    public static void clearAdminPreset(LoadoutRole role, int slot) {
        ADMIN_ROLE_PRESETS.computeIfAbsent(role, ignored -> new HashMap<>()).remove(slot);
        if (getForcedRolePresetSlot(role) == slot) {
            setForcedRolePresetSlot(role, 0);
        }
    }

    public static Set<Integer> getAdminPresetSlots(LoadoutRole role) {
        return Set.copyOf(ADMIN_ROLE_PRESETS.getOrDefault(role, Map.of()).keySet());
    }

    public static boolean isRoleRestrictedToAdminPresets(LoadoutRole role) {
        return RESTRICT_TO_ADMIN_PRESETS.getOrDefault(role, false);
    }

    public static void setRoleRestrictedToAdminPresets(LoadoutRole role, boolean restricted) {
        RESTRICT_TO_ADMIN_PRESETS.put(role, restricted);
    }

    public static int getForcedRolePresetSlot(LoadoutRole role) {
        return Math.max(0, FORCED_ROLE_PRESET_SLOT.getOrDefault(role, 0));
    }

    public static void setForcedRolePresetSlot(LoadoutRole role, int slot) {
        FORCED_ROLE_PRESET_SLOT.put(role, Math.max(0, slot));
    }

    public static boolean isGlobalLoadoutLock() {
        return GLOBAL_LOADOUT_LOCK;
    }

    public static void setGlobalLoadoutLock(boolean locked) {
        GLOBAL_LOADOUT_LOCK = locked;
    }

    public static void saveAdminPolicy(HideAndSeek plugin) {
        saveAdminPolicy(plugin, true);
    }

    public static void saveAdminPolicy(HideAndSeek plugin, boolean flush) {
        if (dataConfig == null) {
            initialize(plugin);
        }

        for (LoadoutRole role : LoadoutRole.values()) {
            String roleKey = role.name().toLowerCase();
            dataConfig.set("admin.item-filter." + roleKey + ".mode", getFilterMode(role).name());
            dataConfig.set("admin.item-filter." + roleKey + ".entries", getFilterItems(role).stream().map(Enum::name).sorted().toList());
            dataConfig.set("admin.perks." + roleKey + ".disabled", getDisabledPerks(role).stream().sorted().toList());
            dataConfig.set("admin.restrict-to-presets." + roleKey, isRoleRestrictedToAdminPresets(role));
            dataConfig.set("admin.forced-preset." + roleKey, getForcedRolePresetSlot(role));

            dataConfig.set("admin.role-presets." + roleKey, null);
            for (int slot = 1; slot <= PlayerLoadout.MAX_PRESETS; slot++) {
                AdminRolePreset preset = getAdminPresetOrNull(role, slot);
                if (preset == null) {
                    continue;
                }
                String base = "admin.role-presets." + roleKey + "." + slot;
                dataConfig.set(base + ".enabled", preset.isEnabled());
                dataConfig.set(base + ".items", preset.getItems().stream().map(Enum::name).toList());
                dataConfig.set(base + ".disabled-perks", preset.getDisabledPerks().stream().sorted().toList());
            }
        }
        dataConfig.set("admin.global-loadout-lock", GLOBAL_LOADOUT_LOCK);

        if (flush) {
            saveData(plugin);
        }
    }

    public static void shutdown(HideAndSeek plugin) {
        saveAll(plugin);
    }

    private static void loadAll(HideAndSeek plugin) {
        if (dataConfig == null) {
            return;
        }
        org.bukkit.configuration.ConfigurationSection players = dataConfig.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                loadPlayer(plugin, playerId);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void loadAdminPolicy() {
        resetAdminDefaults();
        if (dataConfig == null) {
            return;
        }

        for (LoadoutRole role : LoadoutRole.values()) {
            String roleKey = role.name().toLowerCase();
            String rawMode = dataConfig.getString("admin.item-filter." + roleKey + ".mode", LoadoutFilterMode.BLACKLIST.name());
            try {
                FILTER_MODES.put(role, LoadoutFilterMode.valueOf(rawMode.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                FILTER_MODES.put(role, LoadoutFilterMode.BLACKLIST);
            }

            Set<LoadoutItemType> items = EnumSet.noneOf(LoadoutItemType.class);
            for (String entry : dataConfig.getStringList("admin.item-filter." + roleKey + ".entries")) {
                try {
                    items.add(LoadoutItemType.valueOf(entry));
                } catch (IllegalArgumentException ignored) {
                }
            }
            FILTER_ITEMS.put(role, items);

            Set<String> perks = new HashSet<>();
            for (String perkId : dataConfig.getStringList("admin.perks." + roleKey + ".disabled")) {
                if (perkId != null && !perkId.isBlank()) {
                    perks.add(perkId);
                }
            }
            DISABLED_PERKS.put(role, perks);

            RESTRICT_TO_ADMIN_PRESETS.put(role, dataConfig.getBoolean("admin.restrict-to-presets." + roleKey, false));
            FORCED_ROLE_PRESET_SLOT.put(role, Math.max(0, dataConfig.getInt("admin.forced-preset." + roleKey, 0)));

            Map<Integer, AdminRolePreset> bySlot = ADMIN_ROLE_PRESETS.computeIfAbsent(role, ignored -> new HashMap<>());
            bySlot.clear();
            for (int slot = 1; slot <= PlayerLoadout.MAX_PRESETS; slot++) {
                String base = "admin.role-presets." + roleKey + "." + slot;
                List<String> itemIds = dataConfig.getStringList(base + ".items");
                List<String> disabledPerkIds = dataConfig.getStringList(base + ".disabled-perks");
                boolean enabled = dataConfig.getBoolean(base + ".enabled", false);
                if (itemIds.isEmpty() && disabledPerkIds.isEmpty() && !enabled) {
                    continue;
                }
                AdminRolePreset preset = new AdminRolePreset();
                for (String raw : itemIds) {
                    try {
                        LoadoutItemType item = LoadoutItemType.valueOf(raw);
                        if ((role == LoadoutRole.HIDER && item.isForHiders()) || (role == LoadoutRole.SEEKER && item.isForSeekers())) {
                            preset.getItems().add(item);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                preset.replaceDisabledPerks(disabledPerkIds);
                preset.setEnabled(enabled);
                bySlot.put(slot, preset);
            }
        }
        GLOBAL_LOADOUT_LOCK = dataConfig.getBoolean("admin.global-loadout-lock", false);
    }

    private static void resetAdminDefaults() {
        for (LoadoutRole role : LoadoutRole.values()) {
            FILTER_MODES.put(role, LoadoutFilterMode.BLACKLIST);
            FILTER_ITEMS.put(role, EnumSet.noneOf(LoadoutItemType.class));
            DISABLED_PERKS.put(role, new HashSet<>());
            ADMIN_ROLE_PRESETS.put(role, new HashMap<>());
            RESTRICT_TO_ADMIN_PRESETS.put(role, false);
            FORCED_ROLE_PRESET_SLOT.put(role, 0);
        }
        GLOBAL_LOADOUT_LOCK = false;
    }

    private static void saveData(HideAndSeek plugin) {
        if (dataConfig == null || dataFile == null) {
            return;
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save loadout data: " + e.getMessage());
        }
    }
}



