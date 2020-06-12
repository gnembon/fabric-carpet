package carpet.mixins;

import carpet.fakes.EntityInterface;
import carpet.script.EntityEventsGroup;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static carpet.script.CarpetEventServer.Event.PLAYER_CHANGES_DIMENSION;
import static carpet.script.CarpetEventServer.Event.PLAYER_DIES;
import static carpet.script.CarpetEventServer.Event.PLAYER_FINISHED_USING_ITEM;
import static carpet.script.CarpetEventServer.Event.STATISTICS;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntity_scarpetEventMixin extends PlayerEntity
{
    public ServerPlayerEntity_scarpetEventMixin(World world_1, GameProfile gameProfile_1)
    {
        super(world_1, gameProfile_1);
    }

    @Shadow protected abstract void consumeItem();

    @Shadow public boolean notInAnyWorld;

    @Redirect(method = "consumeItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;consumeItem()V"
    ))
    private void finishedUsingItem(PlayerEntity playerEntity)
    {
        if (PLAYER_FINISHED_USING_ITEM.isNeeded())
        {
            Hand hand = getActiveHand();
            ItemStack stack = getActiveItem().copy();
            // do vanilla
            super.consumeItem();
            PLAYER_FINISHED_USING_ITEM.onItemAction((ServerPlayerEntity) (Object)this, hand, stack);
        }
        else
        {
            // do vanilla
            super.consumeItem();
        }
    }

    @Inject(method = "increaseStat", at = @At("HEAD"))
    private void grabStat(Stat<?> stat, int amount, CallbackInfo ci)
    {
        STATISTICS.onPlayerStatistic((ServerPlayerEntity) (Object)this, stat, amount);
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeathEvent(DamageSource source, CallbackInfo ci)
    {
        ((EntityInterface)this).getEventContainer().onEvent(EntityEventsGroup.EntityEventType.ON_DEATH, this, source.name);
        if (PLAYER_DIES.isNeeded())
        {
            PLAYER_DIES.onPlayerEvent((ServerPlayerEntity) (Object)this);
        }
    }

    @Redirect(method = "method_14218", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerPlayerEntity;setSneaking(Z)V"
    ))
    private void setSneakingConditionally(ServerPlayerEntity serverPlayerEntity, boolean sneaking)
    {
        if (!((EntityInterface)serverPlayerEntity.getVehicle()).isPermanentVehicle()) // won't since that method makes sure its not null
            serverPlayerEntity.setSneaking(sneaking);
    }

    private Vec3d previousLocation;
    private DimensionType previousDimension;

    @Inject(method = "changeDimension", at = @At("HEAD"))
    private void logPreviousCoordinates(DimensionType newDimension, CallbackInfoReturnable<Entity> cir)
    {
        previousLocation = getPos();
        previousDimension = dimension;
    }

    @Inject(method = "changeDimension", at = @At("RETURN"))
    private void atChangeDimension(DimensionType newDimension, CallbackInfoReturnable<Entity> cir)
    {
        if (PLAYER_CHANGES_DIMENSION.isNeeded())
        {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object)this;
            Vec3d to = null;
            if (!notInAnyWorld || previousDimension != DimensionType.THE_END || newDimension != DimensionType.OVERWORLD)
            {
                to = getPos();
            }
            PLAYER_CHANGES_DIMENSION.onDimensionChange(player, previousLocation, to, previousDimension, newDimension);
        }
    }

}
