package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(LevelRenderer.class)
public class LevelRenderer_creativeNoClipMixin
{
    @Shadow @Final private Minecraft minecraft;

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"))
    private boolean canSeeWorld(boolean original)
    {
        return original || (CarpetSettings.creativeNoClip && minecraft.player.isCreative());
    }

}
