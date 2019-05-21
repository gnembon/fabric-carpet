package carpet.fakes;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;

import java.util.Map;

public interface MobEntityInterface
{
    public GoalSelector getAI(boolean target);

    public Map<String, Goal> getTemporaryTasks();
}
