package carpet.fakes;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;

import java.util.Map;

public interface MobEntityInterface
{
    GoalSelector getAI(boolean target);

    Map<String, Goal> getTemporaryTasks();
}
