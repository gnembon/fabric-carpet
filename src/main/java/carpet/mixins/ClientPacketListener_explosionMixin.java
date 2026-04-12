package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListener_explosionMixin extends ClientCommonPacketListenerImpl
{

    private static Vec3 vec3dmem;
    private static long tickmem;
    // For disabling the explosion particles and sounds if explosions are stacking up

    protected ClientPacketListener_explosionMixin(final Minecraft minecraft, final Connection connection, final CommonListenerCookie commonListenerCookie)
    {
        super(minecraft, connection, commonListenerCookie);
    }


    @Inject(method = "handleExplosion", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"
    ), cancellable = true)
    private void onHandleExplosion(final ClientboundExplodePacket clientboundExplodePacket, final CallbackInfo ci)
    {

        if (CarpetSettings.optimizedTNT)
        {
            Vec3 vec3d = clientboundExplodePacket.center();
            long tick = minecraft.level.getGameTime();
            if (vec3dmem == null || !vec3dmem.equals(vec3d) || tickmem != tick) {
                vec3dmem = vec3d;
                tickmem = tick;
            } else {
                ci.cancel();
            }
        }
    }
}
