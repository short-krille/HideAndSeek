package de.thecoolcraft11.hideAndSeek.items.effects;

import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.effects.impl.*;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class KillEffectSkins {

    public static final String LOGICAL_ITEM_ID = "kill_effect";

    private static final Map<String, Definition> DEFINITIONS = new ConcurrentHashMap<>();

    private KillEffectSkins() {
    }

    public static void registerAll() {
        KillEffectManager.clear();
        DEFINITIONS.clear();

        register("effect_energy_burst", "Energy Burst", Material.END_ROD, ItemRarity.UNCOMMON, 0, new EnergyBladeKillEffect());
        register("effect_ban_slam", "Ban Slam", Material.IRON_AXE, ItemRarity.RARE, -1, new BanHammerKillEffect());
        register("effect_cartoon_smack", "Cartoon Smack", Material.IRON_SHOVEL, ItemRarity.UNCOMMON, -2, new GiantSpatulaKillEffect());
        register("effect_inferno_nova", "Inferno Nova", Material.BLAZE_POWDER, ItemRarity.EPIC, -3, new InfernoBladeKillEffect());
        register("effect_frost_shatter", "Frost Shatter", Material.BLUE_ICE, ItemRarity.EPIC, -4, new FrostbiteKillEffect());
        register("effect_void_requiem", "Void Requiem", Material.ECHO_SHARD, ItemRarity.LEGENDARY, -5, new ShadowyReaperKillEffect());
        register("effect_ground_slam", "Ground Slam", Material.ANVIL, ItemRarity.EPIC, -6, new GroundSlamKillEffect());


        register("effect_vitality_wave", "Vitality Wave", Material.GOLDEN_APPLE, ItemRarity.RARE, -7, new VitalityWaveKillEffect());
        register("effect_score_crown", "Score Crown", Material.NETHER_STAR, ItemRarity.LEGENDARY, -8, new ScoreCrownKillEffect());
        register("effect_thunder_strike", "Thunder Strike", Material.LIGHTNING_ROD, ItemRarity.EPIC, -9, new ThunderStrikeKillEffect());
        register("effect_rainbow_blast", "Rainbow Blast", Material.FIREWORK_STAR, ItemRarity.RARE, -10, new RainbowBlastKillEffect());
        register("effect_phantom_slash", "Phantom Slash", Material.PHANTOM_MEMBRANE, ItemRarity.EPIC, -11, new PhantomSlashKillEffect());
        register("effect_bubble_pop", "Bubble Pop", Material.PRISMARINE_CRYSTALS, ItemRarity.UNCOMMON, -12, new BubblePopKillEffect());
        register("effect_wither_storm", "Wither Storm", Material.WITHER_SKELETON_SKULL, ItemRarity.LEGENDARY, -13, new WitherStormKillEffect());
    }

    private static void register(String id, String displayName, Material icon, ItemRarity rarity, int sortPriority, KillEffect effect) {
        KillEffectManager.registerKillEffect(id, effect);
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
