package carpet.mixins;

import carpet.helpers.ParticleDisplay;
import carpet.utils.Messenger;
import carpet.utils.MobAI;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityInteraction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.AbstractTraderEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.GlobalPos;
import net.minecraft.util.Hand;
import net.minecraft.util.Timestamp;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.PointOfInterest;
import net.minecraft.village.PointOfInterestStorage;
import net.minecraft.village.PointOfInterestType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntity_aiMixin extends AbstractTraderEntity
{
    @Shadow protected abstract boolean hasSeenGolemRecently(long long_1);

    @Shadow protected abstract void sayNo();

    @Shadow protected abstract int getAvailableFood();

    @Shadow protected abstract void depleteFood(int int_1);

    @Shadow protected abstract boolean lacksFood();

    @Shadow public abstract void eatForBreeding();

    int totalFood;
    boolean hasBed;
    int displayAge;

    @Inject(method = "tick", at = @At("HEAD"))
    private void ontick(CallbackInfo ci)
    {
        if (MobAI.isTracking(this, MobAI.TrackingType.IRON_GOLEM_SPAWNING))
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
        else if (MobAI.isTracking(this, MobAI.TrackingType.VILLAGER_BREEDING))
        {
            if (age % 50 == 0 || age < 20)
            {
                totalFood = getAvailableFood() / 12;
                hasBed = this.brain.getOptionalMemory(MemoryModuleType.HOME).isPresent();
                displayAge = getBreedingAge();

            }
            if (Math.abs(displayAge) < 100 && displayAge !=0) displayAge = getBreedingAge();

            this.setCustomName(Messenger.c(
                    (hasBed?"eb ":"fb ")+"\u2616 ",//"\u263d ",
                    (totalFood>0?"eb ":"fb ")+"\u2668",(totalFood>0?"e ":"f ")+totalFood+" ",
                    (displayAge==0?"eb ":"fb ")+"\u2661",(displayAge==0?"e ":"f "+displayAge)
            ));
            this.setCustomNameVisible(true);
        }
    }

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void onInteract(PlayerEntity playerEntity_1, Hand hand_1, CallbackInfoReturnable<Boolean> cir)
    {
        if (MobAI.isTracking(this, MobAI.TrackingType.VILLAGER_BREEDING))
        {
            ItemStack itemStack_1 = playerEntity_1.getStackInHand(hand_1);
            if (itemStack_1.getItem() == Items.EMERALD)
            {
                GlobalPos bedPos = this.brain.getOptionalMemory(MemoryModuleType.HOME).orElse(null);
                if (bedPos == null || bedPos.getDimension() != dimension)
                {
                    sayNo();
                    ((ServerWorld) getEntityWorld()).spawnParticles(ParticleTypes.BARRIER, x, y + getStandingEyeHeight() + 1, z, 1, 0.1, 0.1, 0.1, 0.0);
                }
                else
                {

                    ParticleDisplay.drawParticleLine((ServerPlayerEntity) playerEntity_1, getPos(), new Vec3d(bedPos.getPos()).add(0.5, 0.5, 0.5), "dust 0 0 0 1", "happy_villager", 100, 0.2);
                }
            }
            else if (itemStack_1.getItem() == Items.ROTTEN_FLESH)
            {
                while(getAvailableFood() >= 12) eatForBreeding();

            }
            else if (itemStack_1.getItem() instanceof BedItem)
            {
                List<PointOfInterest> list_1 = ((ServerWorld) getEntityWorld()).getPointOfInterestStorage().get(
                        type -> type == PointOfInterestType.HOME,
                        getBlockPos(),
                        48, PointOfInterestStorage.OccupationStatus.ANY).collect(Collectors.toList());
                for (PointOfInterest poi : list_1)
                {
                    Vec3d pv = new Vec3d(poi.getPos()).add(0.5, 0.5, 0.5);
                    if (!poi.hasSpace())
                    {
                        ((ServerWorld) getEntityWorld()).spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                                pv.x, pv.y+1.5, pv.z,
                                50, 0.1, 0.3, 0.1, 0.0);
                    }
                    else if (method_20642((VillagerEntity)(Object)this, poi.getPos()))
                        ((ServerWorld) getEntityWorld()).spawnParticles(ParticleTypes.END_ROD,
                                pv.x, pv.y+1, pv.z,
                                50, 0.1, 0.3, 0.1, 0.0);
                    else
                        ((ServerWorld) getEntityWorld()).spawnParticles(ParticleTypes.BARRIER,
                                pv.x, pv.y+1, pv.z,
                                1, 0.1, 0.1, 0.1, 0.0);
                }
            }
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    private boolean method_20642(VillagerEntity villagerEntity_1, BlockPos blockPos_1) {
        Path path_1 = villagerEntity_1.getNavigation().findPathTo(blockPos_1, PointOfInterestType.HOME.method_21648());
        return path_1 != null && path_1.method_21655();
    }


    @Inject(method = "summonGolem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/Box;expand(DDD)Lnet/minecraft/util/math/Box;",
            shift = At.Shift.AFTER
    ))
    private void particleIt(long long_1, int int_1, CallbackInfo ci)
    {
        if (MobAI.isTracking(this, MobAI.TrackingType.IRON_GOLEM_SPAWNING))
        {
            ((ServerWorld) getEntityWorld()).spawnParticles(ParticleTypes.BARRIER, x, y+3, z, 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    public VillagerEntity_aiMixin(EntityType<? extends AbstractTraderEntity> entityType_1, World world_1)
    {
        super(entityType_1, world_1);
    }
}
