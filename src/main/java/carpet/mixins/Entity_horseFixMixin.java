package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.client.network.packet.EntityPositionS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class Entity_horseFixMixin
{
    @Shadow private Entity vehicle;

    @Shadow public abstract World getEntityWorld();

    @Inject(method = "stopRiding", at = @At("HEAD"))
    private void resetNavigation(CallbackInfo ci)
    {
        if (vehicle != null && vehicle instanceof MobEntity && CarpetSettings.horseWanderingFix)
        {
            ((MobEntity) vehicle).getNavigation().stop();
            if (!getEntityWorld().isClient)
            {
                ((ServerWorld) getEntityWorld()).method_14178().sendToNearbyPlayers((Entity)(Object)this, new EntityPositionS2CPacket(vehicle));
            }
        }
    }

}
