package de.thecoolcraft11.hideAndSeek.vote;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class VoteManager {
    private final HideAndSeek plugin;
    private final Random random = new Random();
    private final ConcurrentMap<UUID, GameModeEnum> gamemodeVotes = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, String> mapVotes = new ConcurrentHashMap<>();

    public VoteManager(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public boolean isGamemodeVotingEnabled() {
        var result = plugin.getSettingService().getSetting("game.vote_gamemode_enabled");
        Object value = result.isSuccess() ? result.getValue() : true;
        return value instanceof Boolean enabled ? enabled : true;
    }

    public boolean isMapVotingEnabled() {
        var result = plugin.getSettingService().getSetting("game.vote_map_enabled");
        Object value = result.isSuccess() ? result.getValue() : true;
        return value instanceof Boolean enabled ? enabled : true;
    }

    public boolean showVoteCounts() {
        var result = plugin.getSettingService().getSetting("game.vote_show_counts");
        Object value = result.isSuccess() ? result.getValue() : true;
        return value instanceof Boolean enabled ? enabled : true;
    }

    public boolean isVotingEnabled() {
        return isGamemodeVotingEnabled() || isMapVotingEnabled();
    }

    public boolean isLobbyPhase() {
        return "lobby".equalsIgnoreCase(plugin.getStateManager().getCurrentPhaseId());
    }

    public Optional<GameModeEnum> getGamemodeVote(UUID playerId) {
        return Optional.ofNullable(gamemodeVotes.get(playerId));
    }

    public Optional<String> getMapVote(UUID playerId) {
        return Optional.ofNullable(mapVotes.get(playerId));
    }

    public void castGamemodeVote(UUID playerId, GameModeEnum mode) {
        if (playerId == null || mode == null) {
            return;
        }
        gamemodeVotes.put(playerId, mode);
    }

    public void castMapVote(UUID playerId, String mapName) {
        if (playerId == null || mapName == null || mapName.isBlank()) {
            return;
        }
        mapVotes.put(playerId, mapName);
    }

    public void clearVotes(UUID playerId) {
        if (playerId == null) {
            return;
        }
        gamemodeVotes.remove(playerId);
        mapVotes.remove(playerId);
    }

    public void resetVotes() {
        gamemodeVotes.clear();
        mapVotes.clear();
    }

    public Set<UUID> getOnlineVoterIds() {
        Set<UUID> onlineIds = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            onlineIds.add(player.getUniqueId());
        }
        return onlineIds;
    }

    public Map<GameModeEnum, Long> countGamemodeVotes(Set<UUID> eligibleVoters) {
        Map<GameModeEnum, Long> counts = new EnumMap<>(GameModeEnum.class);
        for (Map.Entry<UUID, GameModeEnum> entry : gamemodeVotes.entrySet()) {
            if (!eligibleVoters.contains(entry.getKey())) {
                continue;
            }
            counts.merge(entry.getValue(), 1L, Long::sum);
        }
        return counts;
    }

    public Map<String, Long> countMapVotes(Set<UUID> eligibleVoters, Collection<String> eligibleMaps) {
        Map<String, Long> counts = new HashMap<>();
        Set<String> allowedMaps = new HashSet<>(eligibleMaps);

        for (Map.Entry<UUID, String> entry : mapVotes.entrySet()) {
            if (!eligibleVoters.contains(entry.getKey())) {
                continue;
            }
            if (!allowedMaps.contains(entry.getValue())) {
                continue;
            }
            counts.merge(entry.getValue(), 1L, Long::sum);
        }
        return counts;
    }

    public VotingResult resolveVotingResult(Set<UUID> eligibleVoters) {
        boolean anyVotes = false;
        GameModeEnum winningGamemode = null;

        if (isGamemodeVotingEnabled()) {
            Map<GameModeEnum, Long> modeCounts = countGamemodeVotes(eligibleVoters);
            if (!modeCounts.isEmpty()) {
                anyVotes = true;
                winningGamemode = pickRandomTopEntry(modeCounts);
            }
        }

        String winningMap = null;
        if (isMapVotingEnabled()) {
            Collection<String> eligibleMaps;
            if (isGamemodeVotingEnabled()) {
                eligibleMaps = winningGamemode == null
                        ? List.of()
                        : plugin.getMapManager().getAvailableMapsForMode(winningGamemode);
            } else {
                eligibleMaps = plugin.getMapManager().getMapsForVoting();
            }

            if (!eligibleMaps.isEmpty()) {
                Map<String, Long> mapCounts = countMapVotes(eligibleVoters, eligibleMaps);
                if (!mapCounts.isEmpty()) {
                    anyVotes = true;
                    winningMap = pickRandomTopEntry(mapCounts);
                }
            }
        }

        return new VotingResult(winningGamemode, winningMap, anyVotes);
    }

    private <T> T pickRandomTopEntry(Map<T, Long> counts) {
        if (counts.isEmpty()) {
            return null;
        }

        long highestVotes = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<T> topEntries = counts.entrySet().stream()
                .filter(entry -> entry.getValue() == highestVotes)
                .map(Map.Entry::getKey)
                .toList();

        return topEntries.get(random.nextInt(topEntries.size()));
    }
}


