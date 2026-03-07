package de.thecoolcraft11.hideAndSeek.items.api;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record ItemStateManager(HideAndSeek plugin) {
    public static final Map<UUID, Long> seekerCurseActiveUntil = new HashMap<>();
    public static final Map<UUID, Long> hiderCursedUntil = new HashMap<>();
    public static final Map<UUID, ItemStack> inkHelmetBackup = new HashMap<>();
    public static final Map<Location, BlockDisplay> sensorDisplays = new HashMap<>();
    public static final Map<UUID, SwordChargeState> swordChargeStates = new HashMap<>();
    public static final Map<UUID, BukkitTask> swordChargeTasks = new HashMap<>();
    public static final Map<UUID, MedkitChannelState> medkitChannelStates = new HashMap<>();
    public static final Map<UUID, BukkitTask> medkitChannelTasks = new HashMap<>();

    public record SwordChargeState(long startedAtMs, float previousExp, int previousLevel) {
    }

    public record MedkitChannelState(float previousExp, int previousLevel) {
    }
}
