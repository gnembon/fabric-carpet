package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GuardianEntity.class)
public abstract class GuardianEntityMixin extends HostileEntity
{
    protected GuardianEntityMixin(EntityType<? extends HostileEntity> entityType_1, World world_1)
    {
        super(entityType_1, world_1);
    }

    @Override
    public void onStruckByLightning(ServerWorld serverWorld, LightningEntity lightningEntity)
    {                                // isRemoved()
        if (!this.world.isClient && !this.isRemoved() && CarpetSettings.renewableSponges && !((Object)this instanceof ElderGuardianEntity))
        {
            ElderGuardianEntity elderGuardian = new ElderGuardianEntity(EntityType.ELDER_GUARDIAN ,this.world);
            elderGuardian.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.yaw, this.pitch);
            elderGuardian.initialize(serverWorld ,this.world.getLocalDifficulty(elderGuardian.getBlockPos()), SpawnReason.CONVERSION, (EntityData)null, (CompoundTag)null);
            elderGuardian.setAiDisabled(this.isAiDisabled());
            
            if (this.hasCustomName())
            {
                elderGuardian.setCustomName(this.getCustomName());
                elderGuardian.setCustomNameVisible(this.isCustomNameVisible());
            }
            
            this.world.spawnEntity(elderGuardian);
            this.discard(); // discard remove();
        }
        else
        {
            super.onStruckByLightning(serverWorld, lightningEntity);
        }
    }
}
