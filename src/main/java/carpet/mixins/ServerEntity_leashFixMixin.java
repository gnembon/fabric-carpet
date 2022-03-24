package carpet.mixins;

import carpet.CarpetSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

@Mixin(ServerEntity.class)
public class ServerEntity_leashFixMixin
{
    @Shadow @Final private Entity entity;

    @Inject(method = "sendPairingData", at = @At("RETURN"))
    private void sendLeashPackets(Consumer<Packet<?>> consumer_1, CallbackInfo ci)
    {
        if (CarpetSettings.leadFix)
        {
            if (entity instanceof Mob)
            {
                consumer_1.accept( new ClientboundSetEntityLinkPacket(entity, ((Mob) entity).getLeashHolder()));
            }
        }
    }
}
