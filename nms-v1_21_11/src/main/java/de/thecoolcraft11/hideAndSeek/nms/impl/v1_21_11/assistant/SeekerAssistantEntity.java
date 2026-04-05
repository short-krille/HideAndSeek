package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11.assistant;

import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11.assistant.goals.AssistantAlertGoal;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11.assistant.goals.AssistantPathfindGoal;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11.assistant.goals.AssistantSniffGoal;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11.assistant.goals.AssistantWanderGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class SeekerAssistantEntity extends net.minecraft.world.entity.monster.zombie.Zombie {

    private final Plugin plugin;
    private final AssistantSharedState sharedState;
    private final String skin;

    public SeekerAssistantEntity(Plugin plugin, UUID seekerId, Location origin, Level level, String skin) {
        super(net.minecraft.world.entity.EntityType.ZOMBIE, level);
        this.plugin = plugin;
        this.sharedState = new AssistantSharedState(origin);
        this.skin = skin;
    }

    public static void injectGoals(PathfinderMob mob, Plugin plugin, AssistantSharedState sharedState, String assistantSkin) {
        mob.goalSelector.removeAllGoals(goal -> true);
        mob.targetSelector.removeAllGoals(goal -> true);

        mob.goalSelector.addGoal(1, new FloatGoal(mob));
        mob.goalSelector.addGoal(2, new AssistantSniffGoal(mob, plugin, sharedState));
        mob.goalSelector.addGoal(3, new AssistantPathfindGoal(mob, plugin, sharedState, assistantSkin));
        mob.goalSelector.addGoal(4, new AssistantAlertGoal(mob, plugin, sharedState));
        mob.goalSelector.addGoal(5, new AssistantWanderGoal(mob, plugin, sharedState));
    }

    @Override
    protected void registerGoals() {

    }

    public void injectGoals() {
        injectGoals(this, plugin, sharedState, skin);
    }
}
