package carpet.mixins;

import carpet.fakes.MinecraftClientInferface;
import carpet.helpers.TickSpeed;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldRenderer.class)
public class WorldRenderer_pausedShakeMixin
{
    @Shadow @Final private MinecraftClient client;

    float initial = 0.0f;

    @ModifyVariable(method = "render", argsOnly = true, ordinal = 0 ,at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;getEntities()Ljava/lang/Iterable;"
    ))
    private float changeTickPhase(float previous)
    {
        initial = previous;
        if (!TickSpeed.process_entities)
            return ((MinecraftClientInferface)client).getPausedTickDelta();
        return previous;
    }

    @ModifyVariable(method = "render", argsOnly = true, ordinal = 0 ,at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleManager;renderParticles(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/render/LightmapTextureManager;Lnet/minecraft/client/render/Camera;F)V",
            shift = At.Shift.BEFORE
    ))
    private float changeTickPhaseBack(float previous)
    {
        return initial;
    }

}
