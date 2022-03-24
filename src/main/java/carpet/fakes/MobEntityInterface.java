package carpet.fakes;

import java.util.Map;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;

public interface MobEntityInterface
{
    GoalSelector getAI(boolean target);

    Map<String, Goal> getTemporaryTasks();

    void setPersistence(boolean what);
}
