package carpet.mixins;

import carpet.helpers.ParticleDisplay;
import carpet.utils.Messenger;
import carpet.utils.MobAI;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Memory;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
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
public abstract class VillagerEntity_aiMixin extends MerchantEntity
{
    @Shadow protected abstract void sayNo();

    @Shadow protected abstract int getAvailableFood();

    @Shadow public abstract void eatForBreeding();

    int totalFood;
    boolean hasBed;
    int displayAge;

    public VillagerEntity_aiMixin(EntityType<? extends MerchantEntity> entityType_1, World world_1)
    {
        super(entityType_1, world_1);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void ontick(CallbackInfo ci)
    {
        if (MobAI.isTracking(this, MobAI.TrackingType.IRON_GOLEM_SPAWNING))
        {
            long time;
            Optional<? extends Memory<?>> last_seen = this.brain.getMemories().get(MemoryModuleType.GOLEM_DETECTED_RECENTLY);
            if (!last_seen.isPresent())
            {
                time = 0;
            }
            else
            {
                time = last_seen.get().getExpiry();
            }
            boolean recentlySeen = time > 0;
            Optional<Long> optional_11 = this.brain.getOptionalMemory(MemoryModuleType.LAST_SLEPT);
            //Optional<Timestamp> optional_22 = this.brain.getOptionalMemory(MemoryModuleType.LAST_WORKED_AT_POI);
            //boolean work = false;
            boolean sleep = false;
            boolean panic = this.brain.hasActivity(Activity.PANIC);
            long currentTime = this.world.getTime();
            if (optional_11.isPresent()) {
                sleep = (currentTime - optional_11.get()) < 24000L;
            }
            //if (optional_22.isPresent()) {
            //    work = (currentTime - optional_22.get().getTime()) < 36000L;
            //}

            this.setCustomName(Messenger.c(
                    (sleep?"eb ":"fb ")+"\u263d ",
                    //(work?"eb ":"fb ")+"\u2692 ",//"\u26CF ",
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
    private void onInteract(PlayerEntity playerEntity_1, Hand hand_1, CallbackInfoReturnable<ActionResult> cir)
    {
        if (MobAI.isTracking(this, MobAI.TrackingType.VILLAGER_BREEDING))
        {
            ItemStack itemStack_1 = playerEntity_1.getStackInHand(hand_1);
            if (itemStack_1.getItem() == Items.EMERALD)
            {
                GlobalPos bedPos = this.brain.getOptionalMemory(MemoryModuleType.HOME).orElse(null);
                if (bedPos == null || bedPos.getDimension() != world.getRegistryKey()) // get Dimension
                {
                    sayNo();
                    ((ServerWorld) getEntityWorld()).spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.getDefaultState()), getX(), getY() + getStandingEyeHeight() + 1, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
                }
                else
                {

                    ParticleDisplay.drawParticleLine((ServerPlayerEntity) playerEntity_1, getPos(), Vec3d.ofCenter(bedPos.getPos()), "dust 0 0 0 1", "happy_villager", 100, 0.2); // pos+0.5v
                }
            }
            else if (itemStack_1.getItem() == Items.ROTTEN_FLESH)
            {
                while(getAvailableFood() >= 12) eatForBreeding();

            }
            else if (itemStack_1.getItem() instanceof BedItem)
            {
                List<PointOfInterest> list_1 = ((ServerWorld) getEntityWorld()).getPointOfInterestStorage().getInCircle(
                        type -> type == PointOfInterestType.HOME,
                        getBlockPos(),
                        48, PointOfInterestStorage.OccupationStatus.ANY).collect(Collectors.toList());
                for (PointOfInterest poi : list_1)
                {
                    Vec3d pv = Vec3d.ofCenter(poi.getPos());
                    if (!poi.hasSpace())
                    {
                        ((ServerWorld) getEntityWorld()).spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                                pv.x, pv.y+1.5, pv.z,
                                50, 0.1, 0.3, 0.1, 0.0);
                    }
                    else if (canReachHome((VillagerEntity)(Object)this, poi.getPos()))
                        ((ServerWorld) getEntityWorld()).spawnParticles(ParticleTypes.END_ROD,
                                pv.x, pv.y+1, pv.z,
                                50, 0.1, 0.3, 0.1, 0.0);
                    else
                        ((ServerWorld) getEntityWorld()).spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.getDefaultState()),
                                pv.x, pv.y+1, pv.z,
                                1, 0.1, 0.1, 0.1, 0.0);
                }
            }
            cir.setReturnValue(ActionResult.FAIL);
            cir.cancel();
        }
    }

    // stolen from VillagerBreedTask
    private boolean canReachHome(VillagerEntity villager, BlockPos pos) {
        Path path = villager.getNavigation().findPathTo(pos, PointOfInterestType.HOME.getSearchDistance());
        return path != null && path.reachesTarget();
    }


    @Inject(method = "summonGolem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/Box;expand(DDD)Lnet/minecraft/util/math/Box;",
            shift = At.Shift.AFTER
    ))
    private void particleIt(ServerWorld serverWorld, long l, int i, CallbackInfo ci)
    {
        if (MobAI.isTracking(this, MobAI.TrackingType.IRON_GOLEM_SPAWNING))
        {
            ((ServerWorld) getEntityWorld()).spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.getDefaultState()), getX(), getY()+3, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        }
    }


}
