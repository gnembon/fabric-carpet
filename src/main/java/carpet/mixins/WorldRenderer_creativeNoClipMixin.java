package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class WorldRenderer_creativeNoClipMixin
{
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSpectator()Z"))
    private boolean canSeeWorld(ClientPlayerEntity clientPlayerEntity)
    {
        return clientPlayerEntity.isSpectator() || (CarpetSettings.creativeNoClip && clientPlayerEntity.isCreative());
    }

}
