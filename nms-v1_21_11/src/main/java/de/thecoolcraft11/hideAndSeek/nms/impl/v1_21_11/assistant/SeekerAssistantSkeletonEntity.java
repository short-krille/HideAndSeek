package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11.assistant;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class SeekerAssistantSkeletonEntity extends Skeleton {

    private static final String SKIN_GHOST_DRONE = "skin_ghost_drone";
    private static final String SKIN_STEEL_GOLEM = "skin_steel_golem";
    private final Plugin plugin;
    private final AssistantSharedState sharedState;
    private final String skin;

    public SeekerAssistantSkeletonEntity(Plugin plugin, UUID seekerId, Location origin, Level level, String skin) {
        super(EntityType.SKELETON, level);
        this.plugin = plugin;
        this.sharedState = new AssistantSharedState(origin);
        this.skin = skin;

        if (SKIN_STEEL_GOLEM.equals(skin)) equipArmor();
        if (SKIN_GHOST_DRONE.equals(skin)) equipDrone();
    }

    protected void registerGoals() {
    }

    public void injectGoals() {
        SeekerAssistantEntity.injectGoals(this, plugin, sharedState, skin);
    }

    private void equipArmor() {
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
    }

    private void equipDrone() {
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.SKELETON_SKULL));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));

        MobEffectInstance invis = new MobEffectInstance(MobEffects.INVISIBILITY, -1, 0, false, false);

        this.addEffect(invis);
        this.setInvisible(true);
    }
}


