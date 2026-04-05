package de.thecoolcraft11.hideAndSeek.items.effects.win;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.entity.Player;

public interface WinSkin {
    void execute(Player player, boolean hidersWon, HideAndSeek plugin);
}
