package carpet.mixins;

import carpet.utils.Messenger;
import carpet.utils.MobAI;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.AbstractTraderEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Timestamp;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntity_aiMixin extends AbstractTraderEntity
{
    @Shadow protected abstract boolean hasSeenGolemRecently(long long_1);

    @Inject(method = "tick", at = @At("HEAD"))
    private void ontick(CallbackInfo ci)
    {
        if (MobAI.isTracking(this, MobAI.TrackingType.IRON_GOLEM_SPAWNING) && !getEntityWorld().isClient())
        {
            long time;
            Optional<Long> optional_1 = this.brain.getOptionalMemory(MemoryModuleType.GOLEM_LAST_SEEN_TIME);
            if (!optional_1.isPresent()) {
                time = 0;
            } else {
                Long long_2 = optional_1.get();
                time = long_2+600 - getEntityWorld().getTime();
            }
            boolean recentlySeen = hasSeenGolemRecently(getEntityWorld().getTime());
            Optional<Timestamp> optional_11 = this.brain.getOptionalMemory(MemoryModuleType.LAST_SLEPT);
            Optional<Timestamp> optional_22 = this.brain.getOptionalMemory(MemoryModuleType.LAST_WORKED_AT_POI);
            boolean work = false;
            boolean sleep = false;
            boolean panic = this.brain.hasActivity(Activity.PANIC);
            long currentTime = this.world.getTime();
            if (optional_11.isPresent()) {
                sleep = (currentTime - optional_11.get().getTime()) < 24000L;
            }
            if (optional_22.isPresent()) {
                work = (currentTime - optional_22.get().getTime()) < 36000L;
            }

            this.setCustomName(Messenger.c(
                    (sleep?"eb ":"fb ")+"\u263d ",
                    (work?"eb ":"fb ")+"\u2692 ",//"\u26CF ",
                    (panic?"lb ":"fb ")+"\u2623 ",//"\u2622 \u2620 \u26A1 ",
                    (recentlySeen?"rb ":"lb ")+time ));
            this.setCustomNameVisible(true);
        }
    }

    @Inject(method = "summonGolem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/Box;expand(DDD)Lnet/minecraft/util/math/Box;",
            shift = At.Shift.AFTER
    ))
    private void particleIt(long long_1, int int_1, CallbackInfo ci)
    {
        if (MobAI.isTracking(this, MobAI.TrackingType.IRON_GOLEM_SPAWNING) && !getEntityWorld().isClient())
        {
            ((ServerWorld) getEntityWorld()).spawnParticles(ParticleTypes.BARRIER, x, y+3, z, 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    public VillagerEntity_aiMixin(EntityType<? extends AbstractTraderEntity> entityType_1, World world_1)
    {
        super(entityType_1, world_1);
    }
}
