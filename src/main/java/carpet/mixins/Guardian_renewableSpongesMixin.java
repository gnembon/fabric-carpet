package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Guardian.class)
public abstract class Guardian_renewableSpongesMixin extends Monster
{
    protected Guardian_renewableSpongesMixin(EntityType<? extends Monster> entityType_1, Level world_1)
    {
        super(entityType_1, world_1);
    }

    @Override
    public void thunderHit(ServerLevel serverWorld, LightningBolt lightningEntity)
    {                                // isRemoved()
        if (!this.level().isClientSide && !this.isRemoved() && CarpetSettings.renewableSponges && !((Object)this instanceof ElderGuardian))
        {
            ElderGuardian elderGuardian = new ElderGuardian(EntityType.ELDER_GUARDIAN ,this.level());
            elderGuardian.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            elderGuardian.finalizeSpawn(serverWorld ,this.level().getCurrentDifficultyAt(elderGuardian.blockPosition()), EntitySpawnReason.CONVERSION, null);
            elderGuardian.setNoAi(this.isNoAi());
            
            if (this.hasCustomName())
            {
                elderGuardian.setCustomName(this.getCustomName());
                elderGuardian.setCustomNameVisible(this.isCustomNameVisible());
            }
            
            this.level().addFreshEntity(elderGuardian);
            this.discard(); // discard remove();
        }
        else
        {
            super.thunderHit(serverWorld, lightningEntity);
        }
    }
}
