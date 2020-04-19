package carpet.patches;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.sound.SoundEvents;

import java.util.EnumSet;

public class BlazeAttack extends Goal
{
    private final BlazeEntity blaze;
    private int field_7218;
    private int field_7217;
    private int field_19420;

    public BlazeAttack(BlazeEntity blaze) {
        this.blaze = blaze;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    public boolean canStart() {
        LivingEntity livingEntity = this.blaze.getTarget();
        return livingEntity != null && livingEntity.isAlive() && this.blaze.canTarget(livingEntity);
    }

    public void start() {
        this.field_7218 = 0;
    }

    public void stop() {
        this.field_19420 = 0;
        this.blaze.setAttacker(null);
    }

    public void tick() {
        --this.field_7217;
        LivingEntity livingEntity = this.blaze.getTarget();
        if (livingEntity != null) {
            boolean bl = this.blaze.getVisibilityCache().canSee(livingEntity);
            if (bl) {
                this.field_19420 = 0;
            } else {
                ++this.field_19420;
            }

            double d = this.blaze.squaredDistanceTo(livingEntity);
            if (d < this.method_6995() * this.method_6995() && bl) {

                if (d < 2.0 && this.blaze.getRandom().nextInt(20)==0)
                {
                    if (this.field_7217 <= 0)
                    {
                        this.field_7217 = 80+this.blaze.getRandom().nextInt(80);
                        this.blaze.tryAttack(livingEntity);
                    }
                }
                this.blaze.getLookControl().lookAt(livingEntity, 10.0F, 10.0F);
                if (this.blaze.getPos().squaredDistanceTo(livingEntity.getX(), this.blaze.getY(), livingEntity.getZ()) < 16.0)
                {
                    //if (this.blaze.getRandom().nextInt(15)==0)
                    //{
                        this.blaze.getMoveControl().strafeTo(0, -0.1f);
                        if (this.blaze.getRandom().nextInt(20)==0)
                        {
                            this.blaze.playSound(SoundEvents.ENTITY_GHAST_AMBIENT, 0.4f+this.blaze.getRandom().nextFloat()/4
                                    , 0.5f+this.blaze.getRandom().nextFloat()/8);
                        }
                    //}
                }
                else
                {
                    this.blaze.getMoveControl().moveTo(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), d/32+0.2D);

                }



            } else if (this.field_19420 < 5) {
                this.blaze.getMoveControl().moveTo(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), d/32+0.2D);
            }

            super.tick();
        }
    }

    private double method_6995() {
        return this.blaze.method_26825(EntityAttributes.GENERIC_FOLLOW_RANGE);
    }
}