package carpet.patches;

import carpet.fakes.BlazeInterface;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.MobEntityWithAi;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class BlazeWonderAroundFarGoal extends WanderAroundFarGoal
{
    public boolean isMyGoal;
    public Vec3d currentTarget;

    public BlazeWonderAroundFarGoal(MobEntityWithAi mob, double speed, float probabiliity)
    {
        super(mob, speed, probabiliity);
        isMyGoal = false;
        currentTarget = null;
    }

    @Override
    protected Vec3d getWanderTarget()
    {
        if (mob.getRandom().nextInt(4) == 0 )
        {
            Vec3d target = super.getWanderTarget();
            if (target != null)
            {
                isMyGoal = true;
                currentTarget = target;
            }
            return target;

        }
        List<BlazeEntity> others = mob.world.getEntities(
                BlazeEntity.class,
                mob.getBoundingBox().expand(32.0, 10.0, 32.0),
                b -> b != mob && ((BlazeInterface)b).getCurrentWanderingTarget() != null
                );
        if (others.size() == 0) {
            Vec3d target = super.getWanderTarget();
            if (target != null)
            {
                isMyGoal = true;
                currentTarget = target;
            }
            return target;
        }
        BlazeEntity otherBlaze = others.get(mob.getRandom().nextInt(others.size()));
        Vec3d otherTarget = ((BlazeInterface)otherBlaze).getCurrentWanderingTarget();
        isMyGoal = false;
        currentTarget = otherTarget;
        return otherTarget;
    }

    @Override
    public void start() {
        this.mob.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, isMyGoal?this.speed:2*speed);
    }

    @Override
    public void stop()
    {
        isMyGoal = true;
        currentTarget = null;
        super.stop();
    }
}
