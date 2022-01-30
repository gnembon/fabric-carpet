package carpet.mixins;

import carpet.helpers.ParticleDisplay;
import carpet.utils.Messenger;
import carpet.utils.MobAI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

@Mixin(Villager.class)
public abstract class Villager_aiMixin extends AbstractVillager
{
    @Shadow protected abstract void setUnhappy();

    @Shadow protected abstract int countFoodPointsInInventory();

    @Shadow public abstract void eatAndDigestFood();

    int totalFood;
    boolean hasBed;
    int displayAge;

    public Villager_aiMixin(EntityType<? extends AbstractVillager> entityType_1, Level world_1)
    {
        super(entityType_1, world_1);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void ontick(CallbackInfo ci)
    {
        if (MobAI.isTracking(this, MobAI.TrackingType.IRON_GOLEM_SPAWNING))
        {
            long time;
            Optional<? extends ExpirableValue<?>> last_seen = this.brain.getMemories().get(MemoryModuleType.GOLEM_DETECTED_RECENTLY);
            if (!last_seen.isPresent())
            {
                time = 0;
            }
            else
            {
                time = last_seen.get().getTimeToLive();
            }
            boolean recentlySeen = time > 0;
            Optional<Long> optional_11 = this.brain.getMemory(MemoryModuleType.LAST_SLEPT);
            //Optional<Timestamp> optional_22 = this.brain.getOptionalMemory(MemoryModuleType.LAST_WORKED_AT_POI);
            //boolean work = false;
            boolean sleep = false;
            boolean panic = this.brain.isActive(Activity.PANIC);
            long currentTime = this.level.getGameTime();
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
            if (tickCount % 50 == 0 || tickCount < 20)
            {
                totalFood = countFoodPointsInInventory() / 12;
                hasBed = this.brain.getMemory(MemoryModuleType.HOME).isPresent();
                displayAge = getAge();

            }
            if (Math.abs(displayAge) < 100 && displayAge !=0) displayAge = getAge();

            this.setCustomName(Messenger.c(
                    (hasBed?"eb ":"fb ")+"\u2616 ",//"\u263d ",
                    (totalFood>0?"eb ":"fb ")+"\u2668",(totalFood>0?"e ":"f ")+totalFood+" ",
                    (displayAge==0?"eb ":"fb ")+"\u2661",(displayAge==0?"e ":"f "+displayAge)
            ));
            this.setCustomNameVisible(true);
        }
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void onInteract(Player playerEntity_1, InteractionHand hand_1, CallbackInfoReturnable<InteractionResult> cir)
    {
        if (MobAI.isTracking(this, MobAI.TrackingType.VILLAGER_BREEDING))
        {
            ItemStack itemStack_1 = playerEntity_1.getItemInHand(hand_1);
            if (itemStack_1.getItem() == Items.EMERALD)
            {
                GlobalPos bedPos = this.brain.getMemory(MemoryModuleType.HOME).orElse(null);
                if (bedPos == null || bedPos.dimension() != level.dimension()) // get Dimension
                {
                    setUnhappy();
                    ((ServerLevel) getCommandSenderWorld()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState()), getX(), getY() + getEyeHeight() + 1, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
                }
                else
                {

                    ParticleDisplay.drawParticleLine((ServerPlayer) playerEntity_1, position(), Vec3.atCenterOf(bedPos.pos()), "dust 0 0 0 1", "happy_villager", 100, 0.2); // pos+0.5v
                }
            }
            else if (itemStack_1.getItem() == Items.ROTTEN_FLESH)
            {
                while(countFoodPointsInInventory() >= 12) eatAndDigestFood();

            }
            else if (itemStack_1.getItem() instanceof BedItem)
            {
                List<PoiRecord> list_1 = ((ServerLevel) getCommandSenderWorld()).getPoiManager().getInRange(
                        type -> type == PoiType.HOME,
                        blockPosition(),
                        48, PoiManager.Occupancy.ANY).collect(Collectors.toList());
                for (PoiRecord poi : list_1)
                {
                    Vec3 pv = Vec3.atCenterOf(poi.getPos());
                    if (!poi.hasSpace())
                    {
                        ((ServerLevel) getCommandSenderWorld()).sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                pv.x, pv.y+1.5, pv.z,
                                50, 0.1, 0.3, 0.1, 0.0);
                    }
                    else if (canReachHome((Villager)(Object)this, poi.getPos()))
                        ((ServerLevel) getCommandSenderWorld()).sendParticles(ParticleTypes.END_ROD,
                                pv.x, pv.y+1, pv.z,
                                50, 0.1, 0.3, 0.1, 0.0);
                    else
                        ((ServerLevel) getCommandSenderWorld()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState()),
                                pv.x, pv.y+1, pv.z,
                                1, 0.1, 0.1, 0.1, 0.0);
                }
            }
            cir.setReturnValue(InteractionResult.FAIL);
            cir.cancel();
        }
    }

    // stolen from VillagerBreedTask
    private boolean canReachHome(Villager villager, BlockPos pos) {
        Path path = villager.getNavigation().createPath(pos, PoiType.HOME.getValidRange());
        return path != null && path.canReach();
    }


    @Inject(method = "spawnGolemIfNeeded", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;",
            shift = At.Shift.AFTER
    ))
    private void particleIt(ServerLevel serverWorld, long l, int i, CallbackInfo ci)
    {
        if (MobAI.isTracking(this, MobAI.TrackingType.IRON_GOLEM_SPAWNING))
        {
            ((ServerLevel) getCommandSenderWorld()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.defaultBlockState()), getX(), getY()+3, getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        }
    }


}
