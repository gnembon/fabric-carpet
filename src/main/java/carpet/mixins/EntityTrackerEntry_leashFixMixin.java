package carpet.mixins;

import carpet.settings.CarpetSettings;
import net.minecraft.client.network.packet.EntityAttachS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.EntityTrackerEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntry_leashFixMixin
{
    @Shadow @Final private Entity entity;

    @Inject(method = "sendPackets", at = @At("RETURN"))
    private void sendLeashPackets(Consumer<Packet<?>> consumer_1, CallbackInfo ci)
    {
        if (CarpetSettings.leadFix)
        {
            if (entity instanceof MobEntity)
            {
                consumer_1.accept( new EntityAttachS2CPacket(entity, ((MobEntity) entity).getHoldingEntity()));
            }
        }
    }
}
