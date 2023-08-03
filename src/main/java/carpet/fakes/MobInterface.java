package carpet.fakes;

import java.util.Map;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;

public interface MobInterface
{
    default GoalSelector carpet$getAi(boolean target) { throw new UnsupportedOperationException(); }

    default Map<String, Goal> carpet$getTemporaryTasks() { throw new UnsupportedOperationException(); }

    default void carpet$setPersistence(boolean what) { throw new UnsupportedOperationException(); }
}
