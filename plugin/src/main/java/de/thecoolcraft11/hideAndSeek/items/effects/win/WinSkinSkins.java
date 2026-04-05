package de.thecoolcraft11.hideAndSeek.items.effects.win;

import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.effects.win.impl.FireworkCelebrationWinSkin;
import de.thecoolcraft11.hideAndSeek.items.effects.win.impl.GoldenRainWinSkin;
import de.thecoolcraft11.hideAndSeek.items.effects.win.impl.StarburstVictoryWinSkin;
import de.thecoolcraft11.hideAndSeek.items.effects.win.impl.TriumphantAuraWinSkin;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WinSkinSkins {

    public static final String LOGICAL_ITEM_ID = "win_skin";

    private static final Map<String, Definition> DEFINITIONS = new ConcurrentHashMap<>();

    private WinSkinSkins() {
    }

    public static void registerAll() {
        WinSkinManager.clear();
        DEFINITIONS.clear();

        register("win_firework_celebration", "Firework Celebration", Material.FIREWORK_ROCKET, ItemRarity.UNCOMMON, 0, new FireworkCelebrationWinSkin());
        register("win_golden_rain", "Golden Rain", Material.GOLD_NUGGET, ItemRarity.RARE, -1, new GoldenRainWinSkin());
        register("win_triumphant_aura", "Triumphant Aura", Material.NETHER_STAR, ItemRarity.EPIC, -2, new TriumphantAuraWinSkin());
        register("win_starburst_victory", "Starburst Victory", Material.END_ROD, ItemRarity.LEGENDARY, -3, new StarburstVictoryWinSkin());
    }

    private static void register(String id, String displayName, Material icon, ItemRarity rarity, int sortPriority, WinSkin winSkin) {
        WinSkinManager.registerWinSkin(id, winSkin);
        ItemSkinSelectionService.registerVariantMetadata(LOGICAL_ITEM_ID, id, rarity);
        DEFINITIONS.put(id, new Definition(id, displayName, icon, rarity, sortPriority));
    }

    public static List<Definition> getDefinitions() {
        List<Definition> definitions = new ArrayList<>(DEFINITIONS.values());
        definitions.sort(Comparator.comparingInt(Definition::sortPriority).reversed().thenComparing(Definition::displayName));
        return definitions;
    }

    public static Definition getDefinition(String id) {
        return DEFINITIONS.get(id);
    }

    public record Definition(String id, String displayName, Material icon, ItemRarity rarity, int sortPriority) {
    }
}
