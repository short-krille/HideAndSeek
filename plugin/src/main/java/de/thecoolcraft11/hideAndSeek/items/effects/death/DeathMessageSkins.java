package de.thecoolcraft11.hideAndSeek.items.effects.death;

import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.effects.death.impl.ClassicDeathMessages;
import de.thecoolcraft11.hideAndSeek.items.effects.death.impl.CreativeDeathMessages;
import de.thecoolcraft11.hideAndSeek.items.effects.death.impl.DramaticDeathMessages;
import de.thecoolcraft11.hideAndSeek.items.effects.death.impl.HumorousDeathMessages;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DeathMessageSkins {

    public static final String LOGICAL_ITEM_ID = "death_messages";

    private static final Map<String, Definition> DEFINITIONS = new ConcurrentHashMap<>();

    private DeathMessageSkins() {
    }

    public static void registerAll() {
        DeathMessageManager.clear();
        DEFINITIONS.clear();

        register("msg_classic", "Classic Messages", Material.WRITABLE_BOOK, ItemRarity.COMMON, 0, new ClassicDeathMessages());
        register("msg_creative", "Creative Messages", Material.ENCHANTED_BOOK, ItemRarity.UNCOMMON, -1, new CreativeDeathMessages());
        register("msg_dramatic", "Dramatic Messages", Material.REDSTONE_BLOCK, ItemRarity.RARE, -2, new DramaticDeathMessages());
        register("msg_humorous", "Humorous Messages", Material.EMERALD_BLOCK, ItemRarity.EPIC, -3, new HumorousDeathMessages());
    }

    private static void register(String id, String displayName, Material icon, ItemRarity rarity, int sortPriority, DeathMessageSkin skin) {
        DeathMessageManager.registerDeathMessageSkin(id, skin);
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

