package carpet.mixins;

import carpet.fakes.MobInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;

@Mixin(Mob.class)
public abstract class MobMixin implements MobInterface
{
    @Shadow @Final protected GoalSelector targetSelector;
    @Shadow @Final protected GoalSelector goalSelector;
    @Shadow private boolean persistenceRequired;
    public final Map<String, Goal> temporaryTasks = new HashMap<>();

    @Override
    public GoalSelector carpet$getAi(boolean target)
    {
        return target?targetSelector:goalSelector;
    }

    @Override
    public Map<String, Goal> carpet$getTemporaryTasks()
    {
        return temporaryTasks;
    }

    @Override
    public void carpet$setPersistence(boolean what)
    {
        persistenceRequired = what;
    }
}
