package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.loadout.*;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.util.CustomModelDataUtil;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryBuilder;
import de.thecoolcraft11.minigameframework.inventory.InventoryClickHandler;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class AdminLoadoutManagementGUI {

    private static final String ADMIN_PERMISSION = "admin.loadout";

    private final HideAndSeek plugin;
    private final LoadoutManager loadoutManager;
    private final Map<UUID, Tab> tabByAdmin = new HashMap<>();
    private final Map<UUID, UUID> selectedTargetByAdmin = new HashMap<>();
    private final Map<UUID, LoadoutRole> presetRoleByAdmin = new HashMap<>();

    public AdminLoadoutManagementGUI(HideAndSeek plugin) {
        this.plugin = plugin;
        this.loadoutManager = plugin.getLoadoutManager();
    }

    public void open(Player admin) {
        if (!admin.hasPermission(ADMIN_PERMISSION)) {
            admin.sendMessage(Component.text("You do not have permission to use this GUI.", NamedTextColor.RED));
            return;
        }
        Tab tab = tabByAdmin.getOrDefault(admin.getUniqueId(), Tab.HIDER);
        openTab(admin, tab);
    }

    private void openTab(Player admin, Tab tab) {
        tabByAdmin.put(admin.getUniqueId(), tab);

        FrameworkInventory inv = new InventoryBuilder(plugin.getInventoryFramework())
                .id("admin_loadout_" + admin.getUniqueId() + "_" + tab.name().toLowerCase())
                .title("Admin Loadout Manager")
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        switch (tab) {
            case HIDER -> fillRoleTab(inv, LoadoutRole.HIDER);
            case SEEKER -> fillRoleTab(inv, LoadoutRole.SEEKER);
            case PERKS -> fillPerkTab(inv);
            case PLAYERS -> fillPlayersTab(admin, inv);
            case PRESETS -> fillPresetTab(admin, inv);
        }

        setTabButtons(admin, inv, tab);
        plugin.getInventoryFramework().openInventory(admin, inv);
    }

    private void fillRoleTab(FrameworkInventory inv, LoadoutRole role) {
        LoadoutFilterMode mode = loadoutManager.getFilterMode(role);
        Set<LoadoutItemType> filterItems = loadoutManager.getFilterItems(role);

        inv.setItem(0, clickable(createInfoItem(role, mode, filterItems.size()), (p, i, e, s) -> e.setCancelled(true)));

        inv.setItem(1, clickable(createUtility(Material.LECTERN, "Mode: " + mode.name(), NamedTextColor.YELLOW,
                List.of(Component.text("Click to toggle blacklist/whitelist", NamedTextColor.GRAY))), (p, i, e, s) -> {
            LoadoutFilterMode next = mode == LoadoutFilterMode.BLACKLIST ? LoadoutFilterMode.WHITELIST : LoadoutFilterMode.BLACKLIST;
            loadoutManager.setFilterMode(role, next);
            int affected = loadoutManager.enforcePoliciesAndNotify();
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            p.sendMessage(Component.text("Set " + role.name().toLowerCase() + " filter mode to " + next.name() + ". Updated " + affected + " online player(s).", NamedTextColor.GREEN));
            openTab(p, tabByRole(role));
            e.setCancelled(true);
        }));

        inv.setItem(2, clickable(createUtility(Material.BARRIER, "Clear Filter Entries", NamedTextColor.RED,
                List.of(Component.text("Keeps mode, clears item list", NamedTextColor.GRAY))), (p, i, e, s) -> {
            loadoutManager.clearRoleFilter(role);
            int affected = loadoutManager.enforcePoliciesAndNotify();
            p.sendMessage(Component.text("Cleared " + role.name().toLowerCase() + " filter entries. Updated " + affected + " online player(s).", NamedTextColor.YELLOW));
            openTab(p, tabByRole(role));
            e.setCancelled(true);
        }));

        inv.setItem(3, clickable(createUtility(Material.TNT, "Reset All " + role.name() + " Loadouts", NamedTextColor.RED,
                List.of(Component.text("Bulk reset for all known players", NamedTextColor.GRAY))), (p, i, e, s) -> {
            int changed = loadoutManager.resetAllLoadouts(role);
            loadoutManager.enforcePoliciesAndNotify();
            p.sendMessage(Component.text("Reset " + role.name().toLowerCase() + " loadouts for " + changed + " player(s).", NamedTextColor.YELLOW));
            openTab(p, tabByRole(role));
            e.setCancelled(true);
        }));

        int slot = 9;
        for (LoadoutItemType item : LoadoutItemType.values()) {
            if (role == LoadoutRole.HIDER && !item.isForHiders()) {
                continue;
            }
            if (role == LoadoutRole.SEEKER && !item.isForSeekers()) {
                continue;
            }
            if (slot >= 45) {
                break;
            }

            boolean inFilter = filterItems.contains(item);
            boolean allowed = loadoutManager.isItemAvailableForRole(role, item);
            ItemStack stack = createLoadoutPolicyItem(item, role, inFilter, allowed,
                    List.of(
                            Component.text("Filter entry: " + (inFilter ? "YES" : "NO"), NamedTextColor.GRAY),
                            Component.text("Mode: " + mode.name(), NamedTextColor.GRAY),
                            Component.text("Status: " + (allowed ? "ALLOWED" : "BLOCKED"), allowed ? NamedTextColor.GREEN : NamedTextColor.RED),
                            Component.text("Click to toggle filter entry", NamedTextColor.YELLOW)
                    ));

            inv.setItem(slot++, clickable(stack, (p, i, e, s) -> {
                loadoutManager.toggleRoleFilterItem(role, item);
                int affected = loadoutManager.enforcePoliciesAndNotify();
                p.sendMessage(Component.text("Updated " + humanize(item.name()) + ". Online updates: " + affected + ".", NamedTextColor.YELLOW));
                openTab(p, tabByRole(role));
                e.setCancelled(true);
            }));
        }
    }

    private void fillPerkTab(FrameworkInventory inv) {
        inv.setItem(0, clickable(createUtility(Material.BEACON, "Perk Availability", NamedTextColor.AQUA,
                List.of(Component.text("Click a perk to enable/disable by role", NamedTextColor.GRAY))), (p, i, e, s) -> e.setCancelled(true)));

        inv.setItem(1, clickable(createUtility(Material.CLOCK, "Refresh Round Perks", NamedTextColor.YELLOW,
                List.of(Component.text("Re-selects perks and refreshes shop items", NamedTextColor.GRAY))), (p, i, e, s) -> {
            loadoutManager.enforcePoliciesAndNotify();
            p.sendMessage(Component.text("Refreshed current perk pool.", NamedTextColor.GREEN));
            openTab(p, Tab.PERKS);
            e.setCancelled(true);
        }));

        int slot = 9;
        for (PerkDefinition perk : plugin.getPerkRegistry().getAllPerks()) {
            if (perk.getTarget() != PerkTarget.HIDER && perk.getTarget() != PerkTarget.SEEKER) {
                continue;
            }
            if (slot >= 45) {
                break;
            }

            LoadoutRole role = perk.getTarget() == PerkTarget.HIDER ? LoadoutRole.HIDER : LoadoutRole.SEEKER;
            boolean disabled = loadoutManager.getDisabledPerks(role).contains(perk.getId());
            ItemStack stack = createPerkPolicyItem(perk, role, disabled,
                    List.of(
                            Component.text("Role: " + role.name(), NamedTextColor.GRAY),
                            Component.text("Status: " + (disabled ? "DISABLED" : "ENABLED"), disabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                            perk.getDescription().color(NamedTextColor.GRAY),
                            Component.text("Click to toggle", NamedTextColor.YELLOW)
                    ));

            inv.setItem(slot++, clickable(stack, (p, i, e, s) -> {
                loadoutManager.toggleDisabledPerk(role, perk.getId());
                loadoutManager.enforcePoliciesAndNotify();
                p.sendMessage(Component.text("Updated perk " + perk.getId() + " for " + role.name().toLowerCase() + ".", NamedTextColor.YELLOW));
                openTab(p, Tab.PERKS);
                e.setCancelled(true);
            }));
        }
    }

    private void fillPlayersTab(Player admin, FrameworkInventory inv) {
        UUID selectedTargetId = selectedTargetByAdmin.get(admin.getUniqueId());
        Player selectedTarget = selectedTargetId == null ? null : Bukkit.getPlayer(selectedTargetId);

        inv.setItem(0, clickable(createUtility(Material.PLAYER_HEAD, "Player Loadouts", NamedTextColor.AQUA,
                List.of(Component.text("Left click to select player", NamedTextColor.GRAY),
                        Component.text("Right click to open editor", NamedTextColor.GRAY))), (p, i, e, s) -> e.setCancelled(true)));

        inv.setItem(36, clickable(createActionButton("Toggle Hider Lock", selectedTarget, Material.IRON_DOOR), (p, i, e, s) -> {
            if (selectedTarget == null) {
                e.setCancelled(true);
                return;
            }
            boolean locked = !loadoutManager.isRoleLocked(selectedTarget.getUniqueId(), LoadoutRole.HIDER);
            loadoutManager.setRoleLocked(selectedTarget.getUniqueId(), LoadoutRole.HIDER, locked);
            p.sendMessage(Component.text("Hider lock for " + selectedTarget.getName() + ": " + locked, NamedTextColor.YELLOW));
            openTab(p, Tab.PLAYERS);
            e.setCancelled(true);
        }));

        inv.setItem(37, clickable(createActionButton("Toggle Seeker Lock", selectedTarget, Material.IRON_DOOR), (p, i, e, s) -> {
            if (selectedTarget == null) {
                e.setCancelled(true);
                return;
            }
            boolean locked = !loadoutManager.isRoleLocked(selectedTarget.getUniqueId(), LoadoutRole.SEEKER);
            loadoutManager.setRoleLocked(selectedTarget.getUniqueId(), LoadoutRole.SEEKER, locked);
            p.sendMessage(Component.text("Seeker lock for " + selectedTarget.getName() + ": " + locked, NamedTextColor.YELLOW));
            openTab(p, Tab.PLAYERS);
            e.setCancelled(true);
        }));

        inv.setItem(38, clickable(createActionButton("Reset Selected (Hider)", selectedTarget, Material.REDSTONE), (p, i, e, s) -> {
            if (selectedTarget != null) {
                loadoutManager.resetPlayerLoadout(selectedTarget.getUniqueId(), LoadoutRole.HIDER);
                loadoutManager.refreshRoleInventory(selectedTarget);
                p.sendMessage(Component.text("Reset hider loadout for " + selectedTarget.getName() + ".", NamedTextColor.YELLOW));
            }
            openTab(p, Tab.PLAYERS);
            e.setCancelled(true);
        }));

        inv.setItem(39, clickable(createActionButton("Reset Selected (Seeker)", selectedTarget, Material.REDSTONE), (p, i, e, s) -> {
            if (selectedTarget != null) {
                loadoutManager.resetPlayerLoadout(selectedTarget.getUniqueId(), LoadoutRole.SEEKER);
                loadoutManager.refreshRoleInventory(selectedTarget);
                p.sendMessage(Component.text("Reset seeker loadout for " + selectedTarget.getName() + ".", NamedTextColor.YELLOW));
            }
            openTab(p, Tab.PLAYERS);
            e.setCancelled(true);
        }));

        inv.setItem(40, clickable(createActionButton("Reset Selected (All)", selectedTarget, Material.TNT), (p, i, e, s) -> {
            if (selectedTarget != null) {
                loadoutManager.resetPlayerLoadout(selectedTarget.getUniqueId());
                loadoutManager.refreshRoleInventory(selectedTarget);
                p.sendMessage(Component.text("Reset full loadout for " + selectedTarget.getName() + ".", NamedTextColor.YELLOW));
            }
            openTab(p, Tab.PLAYERS);
            e.setCancelled(true);
        }));

        inv.setItem(41, clickable(createActionButton("Edit Selected (Hider)", selectedTarget, Material.CHEST), (p, i, e, s) -> {
            plugin.getLogger().info("Test1");
            if (selectedTarget != null) {
                plugin.getLogger().info("Test2");
                openPlayerEditor(p, selectedTarget, LoadoutRole.HIDER);
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                p.sendMessage(Component.text("Select a player first", NamedTextColor.RED));
            }
            e.setCancelled(true);
        }));

        inv.setItem(42, clickable(createActionButton("Edit Selected (Seeker)", selectedTarget, Material.CHEST), (p, i, e, s) -> {
            if (selectedTarget != null) {
                openPlayerEditor(p, selectedTarget, LoadoutRole.SEEKER);
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                p.sendMessage(Component.text("Select a player first", NamedTextColor.RED));
            }
            e.setCancelled(true);
        }));

        inv.setItem(43, clickable(createUtility(Material.LAVA_BUCKET, "Reset Everyone (All)", NamedTextColor.RED,
                List.of(Component.text("Bulk reset all known players", NamedTextColor.GRAY))), (p, i, e, s) -> {
            int h = loadoutManager.resetAllLoadouts(LoadoutRole.HIDER);
            int sk = loadoutManager.resetAllLoadouts(LoadoutRole.SEEKER);
            int affected = loadoutManager.enforcePoliciesAndNotify();
            p.sendMessage(Component.text("Bulk reset complete. Hider: " + h + ", Seeker: " + sk + ", online updated: " + affected + ".", NamedTextColor.RED));
            openTab(p, Tab.PLAYERS);
            e.setCancelled(true);
        }));

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int slot = 9;
        for (Player target : onlinePlayers) {
            if (slot >= 36) {
                break;
            }

            PlayerLoadout loadout = loadoutManager.getLoadout(target.getUniqueId());
            boolean selected = target.getUniqueId().equals(selectedTargetId);
            ItemStack head = createPlayerHead(target, loadout, selected);
            inv.setItem(slot++, clickable(head, (p, i, e, s) -> {
                selectedTargetByAdmin.put(p.getUniqueId(), target.getUniqueId());
                if (e.getClick() == ClickType.RIGHT) {
                    openPlayerEditor(p, target, LoadoutRole.HIDER);
                } else {
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    openTab(p, Tab.PLAYERS);
                }
                e.setCancelled(true);
            }));
        }
    }

    private void openPlayerEditor(Player admin, Player target, LoadoutRole role) {
        FrameworkInventory inv = new InventoryBuilder(plugin.getInventoryFramework())
                .id("admin_loadout_editor_" + admin.getUniqueId() + "_" + target.getUniqueId() + "_" + role.name().toLowerCase())
                .title("Edit " + target.getName() + " - " + role.name())
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        PlayerLoadout loadout = loadoutManager.getLoadout(target.getUniqueId());
        Set<LoadoutItemType> selectedItems = role == LoadoutRole.HIDER ? loadout.getHiderItems() : loadout.getSeekerItems();

        inv.setItem(0, clickable(createUtility(Material.ARROW, "Back", NamedTextColor.YELLOW,
                List.of(Component.text("Return to player tab", NamedTextColor.GRAY))), (p, i, e, s) -> {
            openTab(p, Tab.PLAYERS);
            e.setCancelled(true);
        }));

        inv.setItem(1, clickable(createUtility(Material.COMPASS, "Switch Role", NamedTextColor.YELLOW,
                List.of(Component.text("Current: " + role.name(), NamedTextColor.GRAY))), (p, i, e, s) -> {
            openPlayerEditor(p, target, role == LoadoutRole.HIDER ? LoadoutRole.SEEKER : LoadoutRole.HIDER);
            e.setCancelled(true);
        }));

        inv.setItem(4, clickable(createUtility(Material.BOOK, target.getName() + " Loadout", NamedTextColor.AQUA,
                List.of(
                        Component.text("Role: " + role.name(), NamedTextColor.GRAY),
                        Component.text("Items: " + selectedItems.size(), NamedTextColor.GRAY),
                        Component.text("Click any item to add/remove", NamedTextColor.YELLOW)
                )), (p, i, e, s) -> e.setCancelled(true)));

        int slot = 9;
        for (LoadoutItemType item : LoadoutItemType.values()) {
            if (role == LoadoutRole.HIDER && !item.isForHiders()) {
                continue;
            }
            if (role == LoadoutRole.SEEKER && !item.isForSeekers()) {
                continue;
            }
            if (slot >= 45) {
                break;
            }
            boolean allowed = loadoutManager.isItemAvailableForRole(role, item);
            boolean selected = selectedItems.contains(item);

            ItemStack stack = createLoadoutPolicyItem(item, role, selected, allowed,
                    List.of(
                            Component.text("Status: " + (selected ? "SELECTED" : "NOT SELECTED"), selected ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                            Component.text("Allowed by policy: " + (allowed ? "YES" : "NO"), allowed ? NamedTextColor.GREEN : NamedTextColor.RED),
                            Component.text(allowed ? "Click to toggle" : "Blocked by current policy", allowed ? NamedTextColor.YELLOW : NamedTextColor.RED)
                    ));

            inv.setItem(slot++, clickable(stack, (p, i, e, s) -> {
                if (!loadoutManager.isItemAvailableForRole(role, item)) {
                    p.sendMessage(Component.text("This item is blocked by current role policy.", NamedTextColor.RED));
                    openPlayerEditor(p, target, role);
                    e.setCancelled(true);
                    return;
                }

                PlayerLoadout targetLoadout = loadoutManager.getLoadout(target.getUniqueId());
                if (role == LoadoutRole.HIDER) {
                    if (targetLoadout.getHiderItems().contains(item)) {
                        targetLoadout.removeHiderItem(item);
                    } else {
                        targetLoadout.addHiderItemForced(item, loadoutManager.getItemCost(item));
                    }
                } else {
                    if (targetLoadout.getSeekerItems().contains(item)) {
                        targetLoadout.removeSeekerItem(item);
                    } else {
                        targetLoadout.addSeekerItemForced(item, loadoutManager.getItemCost(item));
                    }
                }
                loadoutManager.saveLoadout(target.getUniqueId());
                loadoutManager.sanitizePlayerLoadout(target.getUniqueId());
                loadoutManager.refreshRoleInventory(target);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.1f);
                openPlayerEditor(p, target, role);
                e.setCancelled(true);
            }));
        }
        inv.openForPlayer(admin);
    }

    private void fillPresetTab(Player admin, FrameworkInventory inv) {
        LoadoutRole role = presetRoleByAdmin.getOrDefault(admin.getUniqueId(), LoadoutRole.HIDER);
        boolean restricted = loadoutManager.isRoleRestrictedToAdminPresets(role);
        int forcedSlot = loadoutManager.getForcedRolePresetSlot(role);

        inv.setItem(0, clickable(createUtility(Material.BOOKSHELF, "Role Presets", NamedTextColor.AQUA,
                List.of(Component.text("Define preset loadouts for players", NamedTextColor.GRAY))), (p, i, e, s) -> e.setCancelled(true)));

        inv.setItem(1, clickable(createUtility(Material.COMPASS, "Role: " + role.name(), NamedTextColor.YELLOW,
                List.of(Component.text("Click to switch role", NamedTextColor.GRAY))), (p, i, e, s) -> {
            LoadoutRole next = role == LoadoutRole.HIDER ? LoadoutRole.SEEKER : LoadoutRole.HIDER;
            presetRoleByAdmin.put(p.getUniqueId(), next);
            openTab(p, Tab.PRESETS);
            e.setCancelled(true);
        }));

        inv.setItem(2, clickable(createUtility(restricted ? Material.REDSTONE_BLOCK : Material.LIME_CONCRETE,
                "Restrict Players: " + (restricted ? "ON" : "OFF"),
                restricted ? NamedTextColor.RED : NamedTextColor.GREEN,
                List.of(Component.text("When ON, players can only use admin presets", NamedTextColor.GRAY))), (p, i, e, s) -> {
            loadoutManager.setRoleRestrictedToAdminPresets(role, !restricted);
            int affected = loadoutManager.enforcePoliciesAndNotify();
            p.sendMessage(Component.text("Preset restriction for " + role.name().toLowerCase() + " is now " + (!restricted) + ". Updated " + affected + " online player(s).", NamedTextColor.YELLOW));
            openTab(p, Tab.PRESETS);
            e.setCancelled(true);
        }));

        inv.setItem(3, clickable(createUtility(forcedSlot > 0 ? Material.RED_BED : Material.GRAY_BED,
                forcedSlot > 0 ? "Forced Preset: #" + forcedSlot : "Forced Preset: NONE",
                forcedSlot > 0 ? NamedTextColor.RED : NamedTextColor.GRAY,
                List.of(Component.text("Shift+Left on preset = assign forced", NamedTextColor.GRAY),
                        Component.text("Click here to clear forced preset", NamedTextColor.GRAY))), (p, i, e, s) -> {
            if (forcedSlot > 0) {
                loadoutManager.setForcedRolePresetSlot(role, 0);
                int affected = loadoutManager.enforcePoliciesAndNotify();
                p.sendMessage(Component.text("Cleared forced preset for " + role.name().toLowerCase() + ". Updated " + affected + " online player(s).", NamedTextColor.YELLOW));
            }
            openTab(p, Tab.PRESETS);
            e.setCancelled(true);
        }));

        int[] presetSlots = {19, 20, 22, 24, 25};
        for (int slot = 1; slot <= PlayerLoadout.MAX_PRESETS; slot++) {
            int guiSlot = presetSlots[slot - 1];
            AdminRolePreset preset = loadoutManager.getAdminPreset(role, slot);
            boolean enabled = loadoutManager.isAdminPresetEnabled(role, slot);
            boolean forced = forcedSlot == slot;

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Items: " + preset.getItems().size(), NamedTextColor.GRAY));
            lore.add(Component.text("Status: " + (enabled ? "ENABLED" : "DISABLED"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (!preset.getItems().isEmpty()) {
                lore.add(Component.text("Preview:", NamedTextColor.AQUA));
                int shown = 0;
                for (LoadoutItemType previewItem : preset.getItems()) {
                    lore.add(Component.text("- " + humanize(previewItem.name()), NamedTextColor.GRAY));
                    shown++;
                    if (shown >= 3) {
                        break;
                    }
                }
                if (preset.getItems().size() > 3) {
                    lore.add(Component.text("+ " + (preset.getItems().size() - 3) + " more", NamedTextColor.DARK_GRAY));
                }
            }
            if (forced) {
                lore.add(Component.text("Currently forced for this role", NamedTextColor.RED));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Left: edit items", NamedTextColor.YELLOW));
            lore.add(Component.text("Shift+Left: set forced", NamedTextColor.GOLD));
            lore.add(Component.text("Shift+Right: enable/disable", NamedTextColor.YELLOW));
            lore.add(Component.text("Drop key: delete preset", NamedTextColor.RED));

            LoadoutItemType preview = preset.getItems().stream().findFirst().orElse(null);
            ItemStack stack = preview == null
                    ? createUtility(Material.CHEST, "Preset #" + slot, enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY, lore)
                    : getPreviewItemStack(preview);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Preset #" + slot + (forced ? " (Forced)" : ""), forced ? NamedTextColor.RED : (enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY), TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());

                meta.setEnchantmentGlintOverride(enabled || forced);

                stack.setItemMeta(meta);
            }
            if (preview != null) {
                CustomModelDataUtil.setCustomModelData(stack, resolveRuntimeItemId(preview), null);
            }

            int targetSlot = slot;
            inv.setItem(guiSlot, clickable(stack, (p, i, e, s) -> {
                if (e.getClick() == ClickType.DROP || e.getClick() == ClickType.CONTROL_DROP) {
                    loadoutManager.deleteAdminPreset(role, targetSlot);
                    int affected = loadoutManager.enforcePoliciesAndNotify();
                    p.sendMessage(Component.text("Deleted preset #" + targetSlot + ". Updated " + affected + " online player(s).", NamedTextColor.RED));
                    openTab(p, Tab.PRESETS);
                    e.setCancelled(true);
                    return;
                }
                if (e.getClick() == ClickType.SHIFT_RIGHT) {
                    loadoutManager.setAdminPresetEnabled(role, targetSlot, !enabled);
                    int affected = loadoutManager.enforcePoliciesAndNotify();
                    p.sendMessage(Component.text("Preset #" + targetSlot + " is now " + (!enabled ? "enabled" : "disabled") + ". Updated " + affected + " online player(s).", NamedTextColor.YELLOW));
                    openTab(p, Tab.PRESETS);
                    e.setCancelled(true);
                    return;
                }
                if (e.getClick() == ClickType.SHIFT_LEFT) {
                    if (!loadoutManager.isAdminPresetEnabled(role, targetSlot)) {
                        p.sendMessage(Component.text("Enable a preset before forcing it.", NamedTextColor.RED));
                    } else {
                        loadoutManager.setForcedRolePresetSlot(role, targetSlot);
                        int affected = loadoutManager.enforcePoliciesAndNotify();
                        p.sendMessage(Component.text("Forced preset #" + targetSlot + " for " + role.name().toLowerCase() + ". Updated " + affected + " online player(s).", NamedTextColor.RED));
                    }
                    openTab(p, Tab.PRESETS);
                    e.setCancelled(true);
                    return;
                }
                openPresetEditor(p, role, targetSlot);
                e.setCancelled(true);
            }));
        }
    }

    private void openPresetEditor(Player admin, LoadoutRole role, int presetSlot) {
        FrameworkInventory inv = new InventoryBuilder(plugin.getInventoryFramework())
                .id("admin_role_preset_editor_" + admin.getUniqueId() + "_" + role.name().toLowerCase() + "_" + presetSlot + "_items")
                .title("Preset #" + presetSlot + " - " + role.name() + " Items")
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        AdminRolePreset preset = loadoutManager.getAdminPreset(role, presetSlot);

        inv.setItem(0, clickable(createUtility(Material.ARROW, "Back", NamedTextColor.YELLOW,
                List.of(Component.text("Return to presets tab", NamedTextColor.GRAY))), (p, i, e, s) -> {
            openTab(p, Tab.PRESETS);
            e.setCancelled(true);
        }));

        inv.setItem(1, clickable(createUtility(Material.COMPASS, "Switch Role", NamedTextColor.YELLOW,
                List.of(Component.text("Current: " + role.name(), NamedTextColor.GRAY))), (p, i, e, s) -> {
            openPresetEditor(p, role == LoadoutRole.HIDER ? LoadoutRole.SEEKER : LoadoutRole.HIDER, presetSlot);
            e.setCancelled(true);
        }));

        inv.setItem(4, clickable(createUtility(Material.BOOK, "Preset #" + presetSlot + " Summary", NamedTextColor.AQUA,
                List.of(Component.text("Items: " + preset.getItems().size(), NamedTextColor.GRAY),
                        Component.text("Enabled for players: " + loadoutManager.isAdminPresetEnabled(role, presetSlot), NamedTextColor.GRAY))), (p, i, e, s) -> e.setCancelled(true)));

        int slot = 9;
        for (LoadoutItemType item : LoadoutItemType.values()) {
            if ((role == LoadoutRole.HIDER && !item.isForHiders()) || (role == LoadoutRole.SEEKER && !item.isForSeekers())) {
                continue;
            }
            if (slot >= 45) {
                break;
            }
            boolean selected = preset.getItems().contains(item);
            ItemStack stack = createLoadoutPolicyItem(item, role, selected, true,
                    List.of(Component.text(selected ? "In preset" : "Not in preset", selected ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                            Component.text("Click to toggle", NamedTextColor.YELLOW)));
            inv.setItem(slot++, clickable(stack, (p, i, e, s) -> {
                loadoutManager.toggleAdminPresetItem(role, presetSlot, item);
                loadoutManager.enforcePoliciesAndNotify();
                openPresetEditor(p, role, presetSlot);
                e.setCancelled(true);
            }));
        }

        renderPresetItemPreviewRow(inv, preset);

        plugin.getInventoryFramework().openInventory(admin, inv);
    }

    private void renderPresetItemPreviewRow(FrameworkInventory inv, AdminRolePreset preset) {
        int slot = 45;
        int shown = 0;
        for (LoadoutItemType itemType : new ArrayList<>(preset.getItems())) {
            if (shown >= 5) {
                break;
            }
            ItemStack stack = getPreviewItemStack(itemType);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(humanize(itemType.name()), NamedTextColor.GREEN, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(
                        Component.text("Included in preset", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
                ));
                stack.setItemMeta(meta);
            }
            CustomModelDataUtil.setCustomModelData(stack, resolveRuntimeItemId(itemType), null);
            inv.setItem(slot++, clickable(stack, (p, i, e, s) -> e.setCancelled(true)));
            shown++;
        }

        while (shown < 5) {
            ItemStack filler = createUtility(Material.GRAY_STAINED_GLASS_PANE, "Empty Preview", NamedTextColor.DARK_GRAY,
                    List.of(Component.text("No item", NamedTextColor.GRAY)));
            inv.setItem(slot++, clickable(filler, (p, i, e, s) -> e.setCancelled(true)));
            shown++;
        }
    }

    private void setTabButtons(Player admin, FrameworkInventory inv, Tab activeTab) {
        inv.setItem(45, tabButton(admin, activeTab, Tab.HIDER, Material.BLUE_CONCRETE, "Hider Loadouts"));
        inv.setItem(46, tabButton(admin, activeTab, Tab.SEEKER, Material.RED_CONCRETE, "Seeker Loadouts"));
        inv.setItem(47, tabButton(admin, activeTab, Tab.PERKS, Material.AMETHYST_SHARD, "Perks"));
        inv.setItem(48, tabButton(admin, activeTab, Tab.PLAYERS, Material.PLAYER_HEAD, "Player Loadouts"));
        inv.setItem(49, tabButton(admin, activeTab, Tab.PRESETS, Material.BOOKSHELF, "Presets"));

        boolean globalLocked = loadoutManager.isGlobalLoadoutLocked();
        inv.setItem(50, clickable(createUtility(globalLocked ? Material.BARRIER : Material.LIME_CONCRETE,
                "Global Lock: " + (globalLocked ? "ON" : "OFF"),
                globalLocked ? NamedTextColor.RED : NamedTextColor.GREEN,
                List.of(Component.text("Click to toggle global loadout lock", NamedTextColor.GRAY))), (p, i, e, s) -> {
            loadoutManager.setGlobalLoadoutLocked(!globalLocked);
            String status = !globalLocked ? "locked" : "unlocked";
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, !globalLocked ? 1.0f : 0.8f);
            p.sendMessage(Component.text("Global loadout is now " + status + ".", !globalLocked ? NamedTextColor.RED : NamedTextColor.GREEN));
            openTab(p, activeTab);
            e.setCancelled(true);
        }));

        inv.setItem(52, clickable(createUtility(Material.CLOCK, "Apply Changes", NamedTextColor.YELLOW,
                List.of(Component.text("Enforce immediately for online players", NamedTextColor.GRAY))), (p, i, e, s) -> {
            int affected = loadoutManager.enforcePoliciesAndNotify();
            p.sendMessage(Component.text("Applied policy changes. Updated " + affected + " online player(s).", NamedTextColor.GREEN));
            openTab(p, activeTab);
            e.setCancelled(true);
        }));

        inv.setItem(53, clickable(createUtility(Material.BARRIER, "Close", NamedTextColor.RED, List.of()), (p, i, e, s) -> {
            p.closeInventory();
            e.setCancelled(true);
        }));
    }

    private InventoryItem tabButton(Player admin, Tab activeTab, Tab tab, Material material, String name) {
        boolean active = activeTab == tab;
        ItemStack stack = createUtility(material, name, active ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
                List.of(Component.text(active ? "Current tab" : "Click to switch", active ? NamedTextColor.GREEN : NamedTextColor.GRAY)));

        if (active) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setEnchantmentGlintOverride(true);
                stack.setItemMeta(meta);
            }
        }

        return clickable(stack, (p, i, e, s) -> {
            openTab(admin, tab);
            e.setCancelled(true);
        });
    }

    private ItemStack createInfoItem(LoadoutRole role, LoadoutFilterMode mode, int entries) {
        return createUtility(Material.BOOK, role.name() + " Policy", NamedTextColor.AQUA,
                List.of(
                        Component.text("Mode: " + mode.name(), NamedTextColor.GRAY),
                        Component.text("Entries: " + entries, NamedTextColor.GRAY),
                        Component.text("Changes apply in real-time", NamedTextColor.YELLOW)
                ));
    }

    private ItemStack createPlayerHead(Player player, PlayerLoadout loadout, boolean selected) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof SkullMeta skullMeta)) {
            return item;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(player.getUniqueId());
        skullMeta.setOwningPlayer(offline);
        skullMeta.displayName(Component.text(player.getName(), selected ? NamedTextColor.GREEN : NamedTextColor.YELLOW, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Hider items: " + loadout.getHiderItems().size(), NamedTextColor.GRAY));
        lore.add(Component.text("Seeker items: " + loadout.getSeekerItems().size(), NamedTextColor.GRAY));

        lore.add(Component.text("Hider locked: " + loadout.isHiderLocked(), loadout.isHiderLocked() ? NamedTextColor.RED : NamedTextColor.GREEN));
        lore.add(Component.text("Seeker locked: " + loadout.isSeekerLocked(), loadout.isSeekerLocked() ? NamedTextColor.RED : NamedTextColor.GREEN));
        lore.add(Component.text("Left: select, Right: edit", NamedTextColor.YELLOW));
        skullMeta.lore(lore);
        item.setItemMeta(skullMeta);
        return item;
    }

    private ItemStack createActionButton(String name, Player target, Material material) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(target == null ? "Select a player first" : "Target: " + target.getName(), NamedTextColor.GRAY));
        ItemStack item = createUtility(material, name, target == null ? NamedTextColor.DARK_GRAY : NamedTextColor.YELLOW, lore);
        if (target != null && material == Material.IRON_DOOR) {
            boolean hiderDoor = name.contains("Hider");
            boolean lockActive = hiderDoor
                    ? loadoutManager.isRoleLocked(target.getUniqueId(), LoadoutRole.HIDER)
                    : loadoutManager.isRoleLocked(target.getUniqueId(), LoadoutRole.SEEKER);
            lore.add(Component.text("Status: " + (lockActive ? "LOCKED" : "UNLOCKED"), lockActive ? NamedTextColor.RED : NamedTextColor.GREEN));
            ItemMeta meta = item.getItemMeta();
            if (lockActive) {
                if (meta != null) {
                    meta.setEnchantmentGlintOverride(true);
                    meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
                    item.setItemMeta(meta);
                }
            } else {
                if (meta != null) {
                    meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
                    item.setItemMeta(meta);
                }
            }
        }
        return item;
    }

    private ItemStack createLoadoutPolicyItem(LoadoutItemType type, LoadoutRole role, boolean highlighted, boolean allowed, List<Component> extraLore) {
        ItemStack item = getPreviewItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        NamedTextColor color = highlighted ? NamedTextColor.GREEN : (allowed ? NamedTextColor.AQUA : NamedTextColor.RED);
        meta.displayName(Component.text(humanize(type.name()), color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Role: " + role.name(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.addAll(extraLore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        meta.lore(lore);


        meta.setEnchantmentGlintOverride(highlighted);


        item.setItemMeta(meta);

        CustomModelDataUtil.setCustomModelData(item, resolveRuntimeItemId(type), null);
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(false).build());
        return item;
    }

    private ItemStack createPerkPolicyItem(PerkDefinition perk, LoadoutRole role, boolean disabled, List<Component> extraLore) {
        ItemStack item = new ItemStack(perk.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        Component display = disabled
                ? perk.getDisplayName().color(NamedTextColor.RED).decorate(TextDecoration.STRIKETHROUGH)
                : perk.getDisplayName().decoration(TextDecoration.BOLD, true);
        meta.displayName(display.decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Perk ID: " + perk.getId(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Role: " + role.name(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.addAll(extraLore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getPreviewItemStack(LoadoutItemType type) {
        GameItem gameItem = SeekerItems.getItem(type.getItemId());
        if (gameItem == null) {
            gameItem = HiderItems.getItem(type.getItemId());
        }
        if (gameItem == null) {
            return new ItemStack(Material.BARRIER);
        }

        ItemStack stack = gameItem.createItem(plugin);
        return stack == null ? new ItemStack(Material.BARRIER) : stack.clone();
    }

    private String resolveRuntimeItemId(LoadoutItemType type) {
        if (type == LoadoutItemType.SPEED_BOOST) {
            return de.thecoolcraft11.hideAndSeek.items.hider.SpeedBoostItem.ID + "_0";
        }
        if (type == LoadoutItemType.KNOCKBACK_STICK) {
            return de.thecoolcraft11.hideAndSeek.items.hider.KnockbackStickItem.ID + "_1";
        }
        return type.getItemId();
    }

    private ItemStack createUtility(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(Component.text(name, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private InventoryItem clickable(ItemStack item, InventoryClickHandler clickHandler) {
        InventoryItem inventoryItem = new InventoryItem(item);
        inventoryItem.setClickHandler(clickHandler);
        inventoryItem.setAllowTakeout(false);
        inventoryItem.setAllowInsert(false);
        return inventoryItem;
    }

    private Tab tabByRole(LoadoutRole role) {
        return role == LoadoutRole.HIDER ? Tab.HIDER : Tab.SEEKER;
    }

    private String humanize(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.split("_")) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }

    private enum Tab {
        HIDER,
        SEEKER,
        PERKS,
        PLAYERS,
        PRESETS
    }

}

















