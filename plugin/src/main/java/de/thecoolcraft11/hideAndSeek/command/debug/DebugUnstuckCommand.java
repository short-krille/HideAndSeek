package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.util.UnstuckManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DebugUnstuckCommand implements DebugSubcommand {
    private final UnstuckManager unstuckManager;

    public DebugUnstuckCommand(HideAndSeek plugin) {
        this.unstuckManager = plugin.getUnstuckManager();
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return DebugSubcommand.filterByPrefix(players, args[0]);
        }
        if (args.length == 2) {
            return DebugSubcommand.filterByPrefix(List.of("history", "nearby", "spawn"), args[1]);
        }
        return List.of();
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /has debug unstuck <player> [history|nearby|spawn]", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
            return true;
        }

        String method = args[1].toLowerCase();
        Location targetLocation;
        UnstuckManager.UnstuckMethod unstuckMethod = null;

        switch (method) {
            case "history" -> {

                targetLocation = findSafePositionFromHistory(target);
                if (targetLocation != null) {
                    unstuckMethod = UnstuckManager.UnstuckMethod.HISTORY;
                }
            }
            case "nearby" -> {

                targetLocation = findNearbySafePosition(target);
                if (targetLocation != null) {
                    unstuckMethod = UnstuckManager.UnstuckMethod.NEARBY;
                }
            }
            case "spawn" -> {

                targetLocation = unstuckManager.resolveWorldSpawnTarget(target);
                if (targetLocation != null) {
                    unstuckMethod = UnstuckManager.UnstuckMethod.SPAWN;
                }
            }
            default -> {
                sender.sendMessage(Component.text("Unknown unstuck method: " + method, NamedTextColor.RED));
                sender.sendMessage(Component.text("Valid methods: history, nearby, spawn", NamedTextColor.YELLOW));
                return true;
            }
        }

        if (targetLocation == null) {
            sender.sendMessage(Component.text("Could not find a safe position for unstuck method: " + method, NamedTextColor.RED));
            return true;
        }


        boolean teleported = target.teleport(targetLocation);
        if (!teleported) {
            sender.sendMessage(Component.text("Teleport failed for " + target.getName(), NamedTextColor.RED));
            return true;
        }


        unstuckManager.recordSuccessfulUnstuck(target.getUniqueId(), targetLocation, unstuckMethod);

        unstuckManager.applyCooldown(target.getUniqueId(), 0);


        targetLocation.getWorld().spawnParticle(Particle.PORTAL, targetLocation, 24, 0.3, 0.5, 0.3, 0.1);
        target.playSound(targetLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);


        sender.sendMessage(Component.text("Forcefully unstuck ", NamedTextColor.GREEN)
                .append(Component.text(target.getName(), NamedTextColor.AQUA))
                .append(Component.text(" using method: ", NamedTextColor.GREEN))
                .append(Component.text(method, NamedTextColor.GOLD)));

        target.sendMessage(Component.text("You were forcefully unstuck by ", NamedTextColor.YELLOW)
                .append(Component.text(sender.getName(), NamedTextColor.AQUA))
                .append(Component.text(" using method: ", NamedTextColor.YELLOW))
                .append(Component.text(method, NamedTextColor.GOLD)));

        return true;
    }

    private Location findSafePositionFromHistory(Player player) {


        UnstuckManager.UnstuckResult result = unstuckManager.tryFindSafePosition(player, HideAndSeek.getDataController().getRoundSpawnPoint());

        if (result.success() && result.location() != null && result.method() == UnstuckManager.UnstuckMethod.HISTORY) {
            return result.location();
        }


        return null;
    }

    private Location findNearbySafePosition(Player player) {

        UnstuckManager.UnstuckResult result = unstuckManager.tryFindSafePosition(player, HideAndSeek.getDataController().getRoundSpawnPoint());

        if (result.success() && result.location() != null && result.method() == UnstuckManager.UnstuckMethod.NEARBY) {
            return result.location();
        }


        return null;
    }
}



