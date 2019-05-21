package carpet.mixins;

import carpet.fakes.MobEntityInterface;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin implements MobEntityInterface
{
    @Shadow @Final protected GoalSelector targetSelector;
    @Shadow @Final protected GoalSelector goalSelector;
    public final Map<String, Goal> temporaryTasks = new HashMap<>();

    @Override
    public GoalSelector getAI(boolean target)
    {
        return target?targetSelector:goalSelector;
    }

    @Override
    public Map<String, Goal> getTemporaryTasks()
    {
        return temporaryTasks;
    }


}
