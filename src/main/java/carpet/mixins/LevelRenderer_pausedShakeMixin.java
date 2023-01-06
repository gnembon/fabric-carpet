package carpet.mixins;

import carpet.fakes.MinecraftClientInferface;
import carpet.helpers.TickSpeed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = LevelRenderer.class, priority = 69420)
public class LevelRenderer_pausedShakeMixin
{
    @Shadow @Final private Minecraft minecraft;

    float initial = -1234.0f;

    // require 0 is for optifine being a bitch as it usually is.
    @ModifyVariable(method = "renderLevel", argsOnly = true, require = 0, ordinal = 0, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"
    ))
    private float changeTickPhase(float previous)
    {
        initial = previous;
        if (!TickSpeed.process_entities)
            return ((MinecraftClientInferface)minecraft).getPausedTickDelta();
        return previous;
    }

    // require 0 is for optifine being a bitch as it usually is.
    @ModifyVariable(method = "renderLevel", argsOnly = true, require = 0, ordinal = 0 ,at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
            shift = At.Shift.BEFORE
    ))
    private float changeTickPhaseBack(float previous)
    {
        // this may not set with optifine bitch
        return initial==-1234.0f?previous:initial;
    }

}
