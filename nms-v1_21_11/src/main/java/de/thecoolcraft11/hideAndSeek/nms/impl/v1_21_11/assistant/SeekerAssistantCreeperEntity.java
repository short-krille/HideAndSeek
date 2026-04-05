package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11.assistant;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class SeekerAssistantCreeperEntity extends net.minecraft.world.entity.monster.Creeper {

    private final Plugin plugin;
    private final AssistantSharedState sharedState;
    private final String skin;

    public SeekerAssistantCreeperEntity(Plugin plugin, UUID seekerId, Location origin, Level level, String skin) {
        super(EntityType.CREEPER, level);
        this.plugin = plugin;
        this.sharedState = new AssistantSharedState(origin);
        this.skin = skin;
    }

    @Override
    protected void registerGoals() {
    }

    public void injectGoals() {
        SeekerAssistantEntity.injectGoals(this, plugin, sharedState, skin);
    }
}
