package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.client.network.packet.EntityPositionS2CPacket;
import net.minecraft.client.network.packet.VehicleMoveS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntity_horseFixMixin extends Entity
{

    @Shadow public abstract ActionResult interact(Entity entity_1, Hand hand_1);

    public PlayerEntity_horseFixMixin(EntityType<?> entityType_1, World world_1)
    {
        super(entityType_1, world_1);
    }

    @Inject(method = "stopRiding", at = @At("HEAD"))
    private void resetNavigation(CallbackInfo ci)
    {
        if (this.getVehicle() != null && CarpetSettings.horseWanderingFix)
        {
            if (getVehicle() instanceof MobEntity)
                ((MobEntity) getVehicle()).getNavigation().stop();
            if (!getEntityWorld().isClient)
            {
                ((ServerWorld) getEntityWorld()).method_14178().sendToNearbyPlayers(this, new EntityPositionS2CPacket(getVehicle()));
                // not sure if player at this point is actually considered to be next to the exiting vehicle
                ((ServerPlayerEntity)(Object)this).networkHandler.sendPacket(new EntityPositionS2CPacket(getVehicle()));
                ((ServerPlayerEntity)(Object)this).networkHandler.sendPacket(new VehicleMoveS2CPacket(getVehicle()));
            }
        }
    }
}
